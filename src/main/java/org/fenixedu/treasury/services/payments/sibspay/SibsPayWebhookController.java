package org.fenixedu.treasury.services.payments.sibspay;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.sibspay.SibsPayPlatform;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotification;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotificationResponse;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotificationWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Path("/sibspaywebhook")
public class SibsPayWebhookController {

    private static final String PROVIDER_NAME = "BC";

    private static final Logger logger = LoggerFactory.getLogger(SibsPayWebhookController.class);

    private static final String NOTIFICATION_URI = "/";

    @POST
    @Path(NOTIFICATION_URI)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response notification(String encryptedBody, @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse response) {

        final PaymentRequestLog log = createLog();

        String iv = httpRequest.getHeader("X-Initialization-Vector");
        String authTag = httpRequest.getHeader("X-Authentication-Tag");

        FenixFramework.atomic(() -> log.saveWebhookNotificationData(iv, authTag, encryptedBody));

        // For each active SIBSPay configuration, try to decrypt, read json structure and get the notificationID
        Optional<SibsPayPlatform> configurationToUseOptional = Optional.empty();
        Optional<SibsPayWebhookNotificationWrapper> webhookNotificationWrapperOptional = Optional.empty();

        boolean mockedUser = false;
        try {
            for (SibsPayPlatform configuration : SibsPayPlatform.findAllActive().collect(Collectors.toList())) {
                try {
                    String jsonBody = decrypt(configuration.getSecretKey(), iv, authTag, encryptedBody);
                    FenixFramework.atomic(() -> {
                        log.saveRequest(jsonBody);
                    });

                    SibsPayWebhookNotification webhookNotificationObj =
                            SibsPayAPIService.deserializeWebhookNotification(jsonBody);

                    if (StringUtils.isNotEmpty(webhookNotificationObj.getNotificationID())) {
                        webhookNotificationWrapperOptional =
                                Optional.of(new SibsPayWebhookNotificationWrapper(webhookNotificationObj));
                        configurationToUseOptional = Optional.of(configuration);

                        break;
                    }
                } catch (UnableToDecryptException e) {
                    // Continue to the other sibsPayPlatform
                }
            }

            if (configurationToUseOptional.isEmpty() || webhookNotificationWrapperOptional.isEmpty()) {
                // The system was not able to decrypt the encrypted body
                // throw exception to return HTTP 500
                throw new RuntimeException("the system was not able to decrypt or integration is down");
            }

            SibsPayPlatform configurationToUse = configurationToUseOptional.get();
            logger.debug("Using SibsPayPlatform: " + configurationToUse.getName());

            SibsPayWebhookNotificationWrapper webhookNotificationWrapper = webhookNotificationWrapperOptional.get();

            ITreasuryPlatformDependentServices treasuryServices = TreasuryPlataformDependentServicesFactory.implementation();

            boolean needToMockUser =
                    StringUtils.isEmpty(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername())
                            && StringUtils.isNotEmpty(configurationToUse.getApplicationUsernameForAutomaticOperations());

            if (needToMockUser) {
                treasuryServices.setCurrentApplicationUser(configurationToUse.getApplicationUsernameForAutomaticOperations());
                mockedUser = true;

                logger.debug("Mocked user with " + configurationToUse.getApplicationUsernameForAutomaticOperations());
            }

            FenixFramework.atomic(() -> {
                log.setExternalTransactionId(webhookNotificationWrapper.getTransactionId());
                log.setStatusCode(webhookNotificationWrapper.getOperationStatusCode());
                log.setStatusMessage(webhookNotificationWrapper.getOperationStatusMessage());
                log.setTransactionWithPayment(webhookNotificationWrapper.isPaid());
                log.setOperationSuccess(webhookNotificationWrapper.isOperationSuccess());

                log.savePaymentTypeAndBrand(webhookNotificationWrapper.getPaymentType(),
                        webhookNotificationWrapper.getPaymentBrand());
            });

            // Find payment request
            Optional<PaymentRequest> paymentRequestOptional = configurationToUse.getPaymentRequestsSet().stream()
                    .filter(p -> webhookNotificationWrapper.getTransactionId().equals(p.getTransactionId())).findFirst();

            if (!paymentRequestOptional.isPresent()) {
                // Transaction not recognized, might be from other system, just return HTTP STATUS 200

                logger.info("Transaction not found, maybe was generated in other system: '%s' - '%s'. Return ok...".formatted(
                        webhookNotificationWrapper.getMerchantTransactionId(), webhookNotificationWrapper.getTransactionId()));

                return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            }

            PaymentRequest paymentRequest = paymentRequestOptional.get();

            FenixFramework.atomic(() -> {
                log.setInternalMerchantTransactionId(paymentRequest.getMerchantTransactionId());
                log.setPaymentRequest(paymentRequest);
                log.setStateCode(paymentRequest.getCurrentState().getCode());
                log.setStateDescription(paymentRequest.getCurrentState().getLocalizedName());
            });

            // Check if notification is already processed as transaction
            if (webhookNotificationWrapper.isPaid()) {
                if (PaymentTransaction.isTransactionDuplicate(webhookNotificationWrapper.getTransactionId())) {
                    // Mark this notification as duplicate and skip processing
                    FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());

                    logger.debug("The transaction is duplicate. Nothing to do, return: "
                            + webhookNotificationWrapper.getTransactionId());

                    return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
                }

                // ANIL 2024-11-14 (#qubIT-Fenix-6095)
                //
                // For payments by credit card (ForwardPaymentRequest) or MB-WAY (MbwayRequest)
                // if the current state is not in created state or in requested state, then
                // the platform should respond with an error and analyse.
                // 
                // For payments by SIBS MB (SibsPaymentRequest), the paymentRequest might be
                // annuled when the notification is pending for processing. This might happen when the debt items were
                // annuled and the SibsPaymentRequest was also annuled.
                //
                // Another case is a different payment made by the customer, with the same reference code.
                //
                // In this case the payment must be allowed to be processed 
                if (!(paymentRequest instanceof SibsPaymentRequest) && !paymentRequest.isInCreatedState()
                        && !paymentRequest.isInRequestedState()) {
                    throw new RuntimeException(
                            "The notification is a successful payment but the paymentRequest is already processed or annuled. Please check");
                }

                // ANIL 2024-11-14  (#qubIT-Fenix-6095)
                //
                // Check comments in the called method
                webhookNotificationWrapper.checkIfNotificationIsPaidAndPaymentReferenceIsAlsoInPaidStatus();
            }

            if (webhookNotificationWrapper.isPending()) {
                // Transaction is pending, ignore by returnig 200
                logger.debug("The notification is pending status. Nothing to do, return...: "
                        + webhookNotificationWrapper.getTransactionId());

                return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            } else if (webhookNotificationWrapper.isPaid()) {
                logger.debug("The notification paid. Registering payment: " + webhookNotificationWrapper.getTransactionId());

                if (paymentRequest instanceof ForwardPaymentRequest) {
                    configurationToUse.processForwardPaymentFromWebhook(log, webhookNotificationWrapper);
                    return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
                } else if (paymentRequest instanceof SibsPaymentRequest) {
                    configurationToUse.processPaymentReferenceCodeTransaction(log, webhookNotificationWrapper);
                    return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
                } else if (paymentRequest instanceof MbwayRequest) {
                    configurationToUse.processMbwayTransaction(log, webhookNotificationWrapper);
                    return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
                }

                throw new RuntimeException("unknown payment request type");
            } else if (webhookNotificationWrapper.isExpired() || webhookNotificationWrapper.isDeclined()) {
                logger.debug("The notification is expired or declined. Reject payment request...: "
                        + webhookNotificationWrapper.getTransactionId());

                FenixFramework.atomic(() -> configurationToUse.rejectRequest(paymentRequest, log, webhookNotificationWrapper));

                return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            }

            logger.info("Unknown state, nothing to do, return with successful response: "
                    + webhookNotificationWrapper.getTransactionId());

            return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);

            FenixFramework.atomic(() -> log.logException(e));

            if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                final OnlinePaymentsGatewayCommunicationException oe = (OnlinePaymentsGatewayCommunicationException) e;

                FenixFramework.atomic(() -> {
                    log.saveRequest(oe.getRequestLog());
                    log.saveResponse(oe.getResponseLog());
                });
            }

            return Response.serverError().build();
        } finally {
            if (mockedUser) {
                TreasuryPlataformDependentServicesFactory.implementation().removeCurrentApplicationUser();

                logger.debug("Unmocked user");
            }
        }

    }

    private SibsPayWebhookNotificationResponse response(SibsPayWebhookNotificationWrapper webhookNotificationWrapper) {
        return new SibsPayWebhookNotificationResponse(200, "Success", webhookNotificationWrapper.getNotificationID());
    }

    private String decrypt(String aesSecretKey, String ivFromHttpHeader, String authTagFromHttpHeader, String encryptedBody)
            throws UnableToDecryptException {

        // Convert data to process

        byte[] key = Base64.getDecoder().decode(aesSecretKey);
        byte[] iv = Base64.getDecoder().decode(ivFromHttpHeader);
        byte[] authTag = Base64.getDecoder().decode(authTagFromHttpHeader);
        byte[] encryptedText = Base64.getDecoder().decode(encryptedBody);

        // Unlike other programming language, We have to append auth tag at the end of
        // encrypted text in Java

        byte[] cipherText = ArrayUtils.addAll(encryptedText, authTag);

        try {
            // Prepare decryption
            SecretKeySpec keySpec = new SecretKeySpec(key, 0, 32, "AES");
            Cipher cipher;

            cipher = getCipherInstance();
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

            // Decrypt
            byte[] bytes = cipher.doFinal(cipherText);

            // Return
            return new String(bytes, "UTF-8");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException | NoSuchProviderException e) {
            logger.error(e.getMessage());

            throw new UnableToDecryptException(e);
        }

    }

    protected static Cipher getCipherInstance() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
        return Cipher.getInstance("AES/GCM/NoPadding", PROVIDER_NAME);
    }

    @Atomic(mode = TxMode.WRITE)
    private PaymentRequestLog createLog() {
        return new PaymentRequestLog("webhookNotification");
    }

    private class UnableToDecryptException extends Exception {

        public UnableToDecryptException(Throwable e) {
            super(e);
        }
    }
}
