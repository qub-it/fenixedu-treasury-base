package org.fenixedu.treasury.domain.paypal;

import java.math.BigDecimal;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.joda.time.DateTime;

public class PayPalLog extends PayPalLog_Base {
    public static final String WEBHOOK_NOTIFICATION = "WEBHOOK_NOTIFICATION";

    public PayPalLog() {
        super();
    }

    public PayPalLog(String operationCode, String InvoiceId, String payerId) {
        setOperationCode(operationCode);
        setExtInvoiceId(InvoiceId);
        setExtCustomerId(payerId);
    }

    public PayPalLog(PaymentRequest paymentRequest, String statusCode, LocalizedString stateDescription) {
        this();
        setPaymentRequest(paymentRequest);
        setStatusCode(statusCode);
        setStateDescription(stateDescription);
    }

    public void savePaymentInfo(BigDecimal payedAmount, DateTime transactionDate) {
        setPaymentDate(transactionDate);
        setAmount(payedAmount);
    }

    public static PayPalLog createPaymentRequestLog(PaymentRequest paymentRequest, String code, LocalizedString localizedName) {
        return new PayPalLog(paymentRequest, code, localizedName);
    }

    public static PayPalLog createLogForWebhookNotification() {
        PayPalLog log = new PayPalLog(WEBHOOK_NOTIFICATION, "", "");
        log.setResponsibleUsername(WEBHOOK_NOTIFICATION);
        return log;
    }

    public void logRequestReceiveDateAndData(String transactionId, String string, String event_type, BigDecimal amount,
            String resource_type, boolean operationSuccess) {
        setRequestReceiveDate(new DateTime());
        setMeoWalletId(transactionId);
        setPaymentMethod(event_type);
        setAmount(amount);
        setStatusCode(resource_type);
        setOperationSuccess(operationSuccess);
    }
}
