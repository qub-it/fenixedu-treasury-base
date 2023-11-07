package org.fenixedu.treasury.services.payments.sibspay;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
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
import org.fenixedu.treasury.domain.sibspay.SibsPayPlatform;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
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

        try {
            for (SibsPayPlatform configuration : SibsPayPlatform.findAllActive().collect(Collectors.toList())) {
                try {
                    String jsonBody = decrypt(configuration.getSecretKey(), iv, authTag, encryptedBody);
                    FenixFramework.atomic(() -> {
                        log.saveRequest(jsonBody);
                    });

                    SibsPayWebhookNotification webhookNotificationObj = SibsPayService.deserializeWebhookNotification(jsonBody);

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
            SibsPayWebhookNotificationWrapper webhookNotificationWrapper = webhookNotificationWrapperOptional.get();

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
                return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            }

            PaymentRequest paymentRequest = paymentRequestOptional.get();

            FenixFramework.atomic(() -> {
                log.setInternalMerchantTransactionId(paymentRequest.getMerchantTransactionId());
                log.setPaymentRequest(paymentRequest);
                log.setStateCode(paymentRequest.getCurrentState().getCode());
                log.setStateDescription(paymentRequest.getCurrentState().getLocalizedName());
            });

            if (!paymentRequest.isInCreatedState() && !paymentRequest.isInRequestedState()) {
                return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            }

            if (webhookNotificationWrapper.isPending()) {
                // Transaction is pending, ignore by returnig 200
                return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            } else if (webhookNotificationWrapper.isPaid()) {

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

                FenixFramework.atomic(() -> configurationToUse.rejectRequest(paymentRequest, log, webhookNotificationWrapper));

                return Response.ok(response(webhookNotificationWrapper), MediaType.APPLICATION_JSON).build();
            }

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

            cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(iv));

            // Decrypt
            byte[] bytes = cipher.doFinal(cipherText);

            // Return
            return new String(bytes, "UTF-8");
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | InvalidAlgorithmParameterException
                | IllegalBlockSizeException | BadPaddingException | UnsupportedEncodingException e) {
            throw new UnableToDecryptException(e);
        }

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
