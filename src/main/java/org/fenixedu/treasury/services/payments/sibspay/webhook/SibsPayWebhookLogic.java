package org.fenixedu.treasury.services.payments.sibspay.webhook;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.sibspay.MbwayMandate;
import org.fenixedu.treasury.domain.sibspay.SibsPayPlatform;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayAPIService;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotification;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotificationResponse;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotificationWrapper;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

public class SibsPayWebhookLogic {
    private static final String PROVIDER_NAME = "BC";

    private static final Logger logger = LoggerFactory.getLogger(SibsPayWebhookLogic.class);

    private final String encryptedBody;
    private final String iv;
    private final String authTag;

    private SibsPayPlatform configurationToUse;
    private SibsPayWebhookNotificationWrapper webhookNotificationWrapper;

    private PaymentRequestLog log;
    private boolean mockedUser;

    public SibsPayWebhookLogic(String encryptedBody, String iv, String authTag) {
        this.encryptedBody = encryptedBody;
        this.iv = iv;
        this.authTag = authTag;
    }

    private boolean decryptNotificationAndFindConfiguration() throws IOException {
        UnableToDecryptException savedUnableToDecryptException = null;

        for (SibsPayPlatform configuration : SibsPayPlatform.findAllActive().collect(Collectors.toList())) {
            try {
                String jsonBody = decrypt(configuration.getSecretKey(), this.iv, this.authTag, this.encryptedBody);

                FenixFramework.atomic(() -> log.saveRequest(jsonBody));

                SibsPayWebhookNotification webhookNotificationObj = SibsPayAPIService.deserializeWebhookNotification(jsonBody);

                if (StringUtils.isNotEmpty(webhookNotificationObj.getNotificationID())) {
                    this.webhookNotificationWrapper = new SibsPayWebhookNotificationWrapper(webhookNotificationObj);
                    this.configurationToUse = configuration;

                    return true;
                }
            } catch (UnableToDecryptException e) {
                // Continue to the other sibsPayPlatform
                savedUnableToDecryptException = e;
            }
        }

        logger.error("Unable to decrypt the encrypted body");
        if (savedUnableToDecryptException != null) {
            logger.error(savedUnableToDecryptException.getMessage());
        }

        return false;
    }

    public Response runWebhook() {
        this.log = createLog();

        FenixFramework.atomic(() -> log.saveWebhookNotificationData(iv, authTag, encryptedBody));

        // For each active SIBSPay configuration, try to decrypt, read json structure and get the notificationID
        try {
            boolean decryptedSuccessfully = decryptNotificationAndFindConfiguration();

            if (!decryptedSuccessfully) {
                throw new RuntimeException("the system was not able to decrypt or integration is down");
            }

            mockUserIfNecessary();

            FenixFramework.atomic(() -> {
                log.setExternalTransactionId(this.webhookNotificationWrapper.getTransactionId());
                log.setStatusCode(this.webhookNotificationWrapper.getOperationStatusCode());
                log.setStatusMessage(this.webhookNotificationWrapper.getOperationStatusMessage());
                log.setTransactionWithPayment(this.webhookNotificationWrapper.isPaid());
                log.setOperationSuccess(this.webhookNotificationWrapper.isOperationSuccess());

                log.savePaymentTypeAndBrand(this.webhookNotificationWrapper.getPaymentType(),
                        this.webhookNotificationWrapper.getPaymentBrand());
            });

            // Before considering this as payment request, check if it is an authorization mbway mandate
            if (isMandateActionAuthCreation()) {
                return runLogicForMandateActionAuthCreation();
            } else if (isMandateActionAuthSuspension()) {
                return runLogicForMandateActionAuthSuspension();
            } else if (isMandateActionAuthReactivation()) {
                return runLogicForMandateActionAuthReactivation();
            } else if (isMandateActionAuthLimitsUpdate()) {
                return runLogicForMandateActionAuthLimitUpdate();
            } else {
                // deal with payments
                return runLogicForPayment();
            }

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
            if (this.mockedUser) {
                Authenticate.unmock();

                logger.debug("Unmocked user");
            }
        }
    }

    private Response runLogicForMandateActionAuthSuspension() {
        String mandateId = this.webhookNotificationWrapper.getMandateId();

        if (StringUtils.isEmpty(mandateId)) {
            // IGNORE
            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        Optional<MbwayMandate> mbwayMandateOpt = MbwayMandate.findUniqueByMandateIdExcludingTransferred(mandateId);

        if (!mbwayMandateOpt.isPresent()) {
            // Could not find the mbwayMandate, return ok to dismiss the notification
            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        MbwayMandate mbwayMandate = mbwayMandateOpt.get();

        // If the mandate is waiting authorization, then update according to the result
        FenixFramework.atomic(() -> {
            if (isIsMandateActionStatusSuccess()) {
                mbwayMandate.suspend();
            } else {
                throw new IllegalArgumentException("how to deal with this kind of notification");
            }
        });

        // What else can we check?
        return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
    }

    private Response runLogicForMandateActionAuthReactivation() {
        String mandateId = this.webhookNotificationWrapper.getMandateId();

        if (StringUtils.isEmpty(mandateId)) {
            // IGNORE
            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        Optional<MbwayMandate> mbwayMandateOpt = MbwayMandate.findUniqueByMandateIdExcludingTransferred(mandateId);

        if (!mbwayMandateOpt.isPresent()) {
            // Could not find the mbwayMandate, return ok to dismiss the notification
            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        MbwayMandate mbwayMandate = mbwayMandateOpt.get();

        // If the mandate is waiting authorization, then update according to the result
        FenixFramework.atomic(() -> {
            if (isIsMandateActionStatusSuccess()) {
                mbwayMandate.reactivate();
            } else {
                throw new IllegalArgumentException("how to deal with this kind of notification");
            }
        });

        // What else can we check?
        return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
    }

    // runLogicForMandateActionAuthLimitUpdate
    private Response runLogicForMandateActionAuthLimitUpdate() {
        String mandateId = this.webhookNotificationWrapper.getMandateId();

        if (StringUtils.isEmpty(mandateId)) {
            // IGNORE
            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        Optional<MbwayMandate> mbwayMandateOpt = MbwayMandate.findUniqueByMandateIdExcludingTransferred(mandateId);

        if (!mbwayMandateOpt.isPresent()) {
            // Could not find the mbwayMandate, return ok to dismiss the notification
            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        MbwayMandate mbwayMandate = mbwayMandateOpt.get();

        // If the mandate is waiting authorization, then update according to the result
        FenixFramework.atomic(() -> {
            if (isIsMandateActionStatusSuccess()) {
                mbwayMandate.updatePlafondAndExpirationDate(this.webhookNotificationWrapper.getPlafond(),
                        this.webhookNotificationWrapper.getAuthorizationExpirationDate());
            } else {
                throw new IllegalArgumentException("how to deal with this kind of notification");
            }
        });

        // What else can we check?
        return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
    }

    private Response runLogicForPayment() {
        // Find payment request
        Optional<PaymentRequest> paymentRequestOptional = this.configurationToUse.getPaymentRequestsSet().stream()
                .filter(p -> this.webhookNotificationWrapper.getTransactionId().equals(p.getTransactionId())).findFirst();

        if (!paymentRequestOptional.isPresent()) {
            // Transaction not recognized, might be from other system, just return HTTP STATUS 200

            logger.info("Transaction not found, maybe was generated in other system: '%s' - '%s'. Return ok...".formatted(
                    this.webhookNotificationWrapper.getMerchantTransactionId(),
                    this.webhookNotificationWrapper.getTransactionId()));

            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        PaymentRequest paymentRequest = paymentRequestOptional.get();

        FenixFramework.atomic(() -> {
            this.log.setInternalMerchantTransactionId(paymentRequest.getMerchantTransactionId());
            this.log.setPaymentRequest(paymentRequest);
            this.log.setStateCode(paymentRequest.getCurrentState().getCode());
            this.log.setStateDescription(paymentRequest.getCurrentState().getLocalizedName());
        });

        // Check if notification is already processed as transaction
        if (this.webhookNotificationWrapper.isPaid()) {
            if (PaymentTransaction.isTransactionDuplicate(this.webhookNotificationWrapper.getTransactionId())) {
                // Mark this notification as duplicate and skip processing
                FenixFramework.atomic(() -> this.log.markAsDuplicatedTransaction());

                logger.debug(
                        "The transaction is duplicate. Nothing to do, return: " + this.webhookNotificationWrapper.getTransactionId());

                return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
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
            if (!(paymentRequest instanceof SibsPaymentRequest) && !paymentRequest.isInCreatedState() && !paymentRequest.isInRequestedState()) {
                throw new RuntimeException(
                        "The notification is a successful payment but the paymentRequest is already processed or annuled. Please check");
            }

            // ANIL 2024-11-14  (#qubIT-Fenix-6095)
            //
            // Check comments in the called method
            this.webhookNotificationWrapper.checkIfNotificationIsPaidAndPaymentReferenceIsAlsoInPaidStatus();
        }

        if (this.webhookNotificationWrapper.isPending()) {
            // Transaction is pending, ignore by returnig 200
            logger.debug(
                    "The notification is pending status. Nothing to do, return...: " + this.webhookNotificationWrapper.getTransactionId());

            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        } else if (this.webhookNotificationWrapper.isPaid()) {
            logger.debug("The notification paid. Registering payment: " + this.webhookNotificationWrapper.getTransactionId());

            if (paymentRequest instanceof ForwardPaymentRequest) {
                this.configurationToUse.processForwardPaymentFromWebhook(log, this.webhookNotificationWrapper);
                return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            } else if (paymentRequest instanceof SibsPaymentRequest) {
                this.configurationToUse.processPaymentReferenceCodeTransaction(log, this.webhookNotificationWrapper);
                return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            } else if (paymentRequest instanceof MbwayRequest) {
                this.configurationToUse.processMbwayTransaction(log, this.webhookNotificationWrapper);
                return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            }

            throw new RuntimeException("unknown payment request type");
        } else if (this.webhookNotificationWrapper.isExpired() || this.webhookNotificationWrapper.isDeclined()) {
            logger.debug(
                    "The notification is expired or declined. Reject payment request...: " + webhookNotificationWrapper.getTransactionId());

            FenixFramework.atomic(
                    () -> this.configurationToUse.rejectRequest(paymentRequest, this.log, this.webhookNotificationWrapper));

            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        logger.info(
                "Unknown state, nothing to do, return with successful response: " + webhookNotificationWrapper.getTransactionId());

        return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
    }

    private Response runLogicForMandateActionAuthCreation() {
        String mandateId = this.webhookNotificationWrapper.getMandateId();

        if (StringUtils.isEmpty(mandateId)) {
            // IGNORE
            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        Optional<MbwayMandate> mbwayMandateOpt = MbwayMandate.findUniqueByMandateIdExcludingTransferred(mandateId);

        if (!mbwayMandateOpt.isPresent()) {
            // Could not find the mbwayMandate, return ok to dismiss the notification
            return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
        }

        MbwayMandate mbwayMandate = mbwayMandateOpt.get();

        // If the mandate is waiting authorization, then update according to the result
        FenixFramework.atomic(() -> {
            if (isIsMandateActionStatusSuccess()) {
                mbwayMandate.authorize();
                mbwayMandate.updatePlafondAndExpirationDate(this.webhookNotificationWrapper.getPlafond(),
                        this.webhookNotificationWrapper.getAuthorizationExpirationDate());
            } else if (isMandateActionStatusRefusedOrRejected()) {
                mbwayMandate.markAsNotAuthorized("Received declined in the platform by a webhook notification");
            } else {
                throw new IllegalArgumentException("how to deal with this kind of notification");
            }
        });

        // What else can we check?
        return Response.ok(response(this.webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
    }

    private boolean isMandateActionStatusRefusedOrRejected() {
        return this.webhookNotificationWrapper.isMandateActionStatusRejected() || this.webhookNotificationWrapper.isMandateActionStatusRefused();
    }

    private boolean isIsMandateActionStatusSuccess() {
        return this.webhookNotificationWrapper.IsMandateActionStatusSuccess();
    }

    private boolean isMandateActionAuthCreation() {
        return this.webhookNotificationWrapper.IsMandateActionAuthCreation();
    }

    private boolean isMandateActionAuthSuspension() {
        return this.webhookNotificationWrapper.isMandateActionAuthSuspension();
    }

    private boolean isMandateActionAuthReactivation() {
        return this.webhookNotificationWrapper.isMandateActionAuthReactivation();
    }

    private boolean isMandateActionAuthLimitsUpdate() {
        return this.webhookNotificationWrapper.isMandateActionAuthLimitsUpdate();
    }

    private void mockUserIfNecessary() {
        boolean needToMockUser = StringUtils.isEmpty(
                TreasuryConstants.getAuthenticatedUsername()) && StringUtils.isNotEmpty(
                configurationToUse.getApplicationUsernameForAutomaticOperations());

        if (needToMockUser) {
            mockApplicationUser(configurationToUse.getApplicationUsernameForAutomaticOperations());
            this.mockedUser = true;

            logger.debug("Mocked user with " + configurationToUse.getApplicationUsernameForAutomaticOperations());
        }
    }

    private void mockApplicationUser(String username) {
        User user = User.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("user not found: " + username);
        }

        Authenticate.mock(user, "TODO: CHANGE ME");
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
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException |
                IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException | NoSuchProviderException e) {
            throw new UnableToDecryptException(e);
        }

    }

    public static Cipher getCipherInstance() throws NoSuchAlgorithmException, NoSuchProviderException, NoSuchPaddingException {
        return Cipher.getInstance("AES/GCM/NoPadding", PROVIDER_NAME);
    }

    @Atomic(mode = Atomic.TxMode.WRITE)
    private PaymentRequestLog createLog() {
        return new PaymentRequestLog("webhookNotification");
    }

    private class UnableToDecryptException extends Exception {
        public UnableToDecryptException(Throwable e) {
            super(e);
        }
    }

    private SibsPayWebhookNotificationResponse response(SibsPayWebhookNotificationWrapper webhookNotificationWrapper) {
        return new SibsPayWebhookNotificationResponse(200, "Success", webhookNotificationWrapper.getNotificationID());
    }

}
