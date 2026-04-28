package org.fenixedu.onlinepaymentsgateway.exceptions;

public class PaymentRequestTimeoutException extends OnlinePaymentsGatewayCommunicationException {
    public PaymentRequestTimeoutException(String requestLog, String responseLog) {
        super(requestLog, responseLog);
    }

    public PaymentRequestTimeoutException(String requestLog, String responseLog, String message, Throwable cause) {
        super(requestLog, responseLog, message, cause);
    }

    public PaymentRequestTimeoutException(String requestLog, String responseLog, String message) {
        super(requestLog, responseLog, message);
    }

    public PaymentRequestTimeoutException(String requestLog, String responseLog, Throwable cause) {
        super(requestLog, responseLog, cause);
    }
}
