package org.fenixedu.treasury.domain.sibspaymentsgateway;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

public class SibsPaymentsGatewayLog extends SibsPaymentsGatewayLog_Base {

    public static final String REQUEST_PAYMENT_CODE = "sibsMbPaymentRequest";
    public static final String MBWAY_REQUEST_PAYMENT = "mbwayPaymentRequest";
    public static final String WEBHOOK_NOTIFICATION = "WEBHOOK_NOTIFICATION";

    public static final String OCTECT_STREAM_CONTENT_TYPE = "application/octet-stream";

    public SibsPaymentsGatewayLog() {
        super();
    }

    protected SibsPaymentsGatewayLog(String operationCode) {
        this();

        setOperationCode(operationCode);

        checkRules();
    }

    protected SibsPaymentsGatewayLog(String operationCode, String sibsGatewayMerchantTransactionId) {
        this();

        setOperationCode(operationCode);
        setSibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId);

        checkRules();
    }

    private void checkRules() {

    }

    public boolean isExceptionOccured() {
        return super.getExceptionOccured();
    }

    public boolean isOperationSuccess() {
        return super.getOperationSuccess();
    }

    public void savePaymentInfo(BigDecimal amount, DateTime paymentDate) {
        setAmount(amount);
        setPaymentDate(paymentDate);
    }

    public void markAsDuplicatedTransaction() {
        setSibsTransactionDuplicated(true);
    }

    public void logRequestSendDate() {
        setRequestSendDate(new DateTime());
    }

    public void logRequestReceiveDateAndData(String transactionId, boolean operationSuccess, boolean transactionPaid,
            String operationResultCode, String operationResultDescription) {
        setRequestReceiveDate(new DateTime());
        setSibsGatewayTransactionId(transactionId);
        setOperationSuccess(operationSuccess);
        setTransactionWithPayment(transactionPaid);
        setStatusCode(operationResultCode);
        setStatusMessage(operationResultDescription);
    }

    public void saveWebhookNotificationData(String notificationInitializationVector, String notificationAuthenticationTag,
            String notificationEncryptedPayload) {
        final ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();

        setNotificationInitializationVector(notificationInitializationVector);
        setNotificationAuthTag(notificationAuthenticationTag);

        if (notificationEncryptedPayload != null) {
            final String notificationEncryptedPayloadFileId = implementation.createFile(
                    String.format("sibsOnlinePaymentsGatewayLog-notificationEncryptedPayload-%s.txt", getExternalId()),
                    OCTECT_STREAM_CONTENT_TYPE, notificationEncryptedPayload.getBytes());

            setNotificationEncryptedPayloadFileId(notificationEncryptedPayloadFileId);
        }
    }

    public void saveMerchantTransactionId(String merchantTransactionId) {
        setSibsGatewayMerchantTransactionId(merchantTransactionId);
    }
    
    public void saveTransactionId(String transactionId) {
        setSibsGatewayTransactionId(transactionId);
    }
    
    public void saveReferenceId(String referenceId) {
        setSibsGatewayReferenceId(referenceId);
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<SibsPaymentsGatewayLog> findAll() {
        return PaymentRequestLog.findAll().filter(p -> p instanceof SibsPaymentsGatewayLog).map(SibsPaymentsGatewayLog.class::cast);
    }
    
    public static SibsPaymentsGatewayLog createForSibsPaymentRequest(String sibsGatewayMerchantTransactionId) {
        SibsPaymentsGatewayLog log = new SibsPaymentsGatewayLog(REQUEST_PAYMENT_CODE, sibsGatewayMerchantTransactionId);
        log.setStateCode(PaymentReferenceCodeStateType.UNUSED.getCode());
        log.setStateDescription(PaymentReferenceCodeStateType.UNUSED.getDescriptionI18N());
        
        return log;
    }

    public static SibsPaymentsGatewayLog createForMbwayPaymentRequest(String sibsGatewayMerchantTransactionId) {
        SibsPaymentsGatewayLog log = new SibsPaymentsGatewayLog(MBWAY_REQUEST_PAYMENT, sibsGatewayMerchantTransactionId);
        log.setStateCode(PaymentReferenceCodeStateType.UNUSED.getCode());
        log.setStateDescription(PaymentReferenceCodeStateType.UNUSED.getDescriptionI18N());

        return log;
    }

    public static SibsPaymentsGatewayLog createLogForWebhookNotification() {
        return new SibsPaymentsGatewayLog(WEBHOOK_NOTIFICATION);
    }

    public static SibsPaymentsGatewayLog create(PaymentRequest paymentRequest, String sibsGatewayMerchantTransactionId) {
        SibsPaymentsGatewayLog log = new SibsPaymentsGatewayLog();
        
        log.setPaymentRequest(paymentRequest);
        log.setSibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId);
        
        return log;
    }

}
