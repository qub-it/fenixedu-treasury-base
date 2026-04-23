package org.fenixedu.onlinepaymentsgateway.exceptions;

public class PaymentRequestNotFoundException extends OnlinePaymentsGatewayCommunicationException {

    public PaymentRequestNotFoundException(String requestLog, String responseLog) {
        super(requestLog, responseLog);
    }

    public PaymentRequestNotFoundException(String requestLog, String responseLog, String message, Throwable cause) {
        super(requestLog, responseLog, message, cause);
    }

    public PaymentRequestNotFoundException(String requestLog, String responseLog, String message) {
        super(requestLog, responseLog, message);
    }

    public PaymentRequestNotFoundException(String requestLog, String responseLog, Throwable cause) {
        super(requestLog, responseLog, cause);
    }

}
