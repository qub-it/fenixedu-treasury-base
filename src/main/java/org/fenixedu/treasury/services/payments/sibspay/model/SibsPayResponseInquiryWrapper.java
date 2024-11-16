package org.fenixedu.treasury.services.payments.sibspay.model;

import java.math.BigDecimal;

import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayAPIService;
import org.joda.time.DateTime;

public class SibsPayResponseInquiryWrapper implements DigitalPlatformResultBean {

    private SibsPayResponseInquiry responseInquiry;

    private String requestLog;

    private String responseLog;

    public SibsPayResponseInquiryWrapper(SibsPayResponseInquiry responseInquiry, String requestLog, String responseLog) {
        this.responseInquiry = responseInquiry;
        this.requestLog = requestLog;
        this.responseLog = responseLog;
    }

    @Override
    public BigDecimal getAmount() {
        return this.responseInquiry.getAmount().getValue();
    }

    @Override
    public String getMerchantTransactionId() {
        return this.responseInquiry.getMerchant().getMerchantTransactionId();
    }

    @Override
    public String getPaymentBrand() {
        return this.responseInquiry.getPaymentMethod();
    }

    @Override
    public DateTime getPaymentDate() {
        return null;
    }

    @Override
    public String getPaymentResultCode() {
        return this.responseInquiry.getPaymentStatus();
    }

    @Override
    public String getPaymentResultDescription() {
        return this.responseInquiry.getPaymentStatus();
    }

    @Override
    public String getPaymentType() {
        return this.responseInquiry.getPaymentType();
    }

    @Override
    public String getTimestamp() {
        return null;
    }

    @Override
    public String getTransactionId() {
        return this.responseInquiry.getTransactionID();
    }

    public String getReferenceCode() {
        return this.responseInquiry.getPaymentReference().getReference();
    }

    @Override
    public boolean isOperationSuccess() {
        return SibsPayAPIService.isOperationSuccess(this.responseInquiry.getReturnStatus().getStatusCode());
    }

    public String getOperationStatusCode() {
        return this.responseInquiry.getReturnStatus().getStatusCode();
    }

    public String getOperationStatusMessage() {
        return this.responseInquiry.getReturnStatus().getStatusMsg();
    }

    public String getOperationStatusDescription() {
        return this.responseInquiry.getReturnStatus().getStatusDescription();
    }

    @Override
    public boolean isPaid() {
        // ANIL 2024-11-15 (#qubIT-Fenix-6101)
        //
        // The notification of annulment of SIBS payment requests are sent with the 
        // paymentStatus or #getPaymentResultCode() equal to "Success" .
        //
        // This is misleading as these transactions are not successful payments but
        // successful annulments. The payments were not being registered because the
        // transactionIds of annulement notifications are different from what is registered 
        // within the paymentRequest
        //
        // Also in the web controller SibsPayWebhookController, if the payment request
        // was not in creation or request state, it did nothing and returned OK to the
        // payment platform
        //
        // Consider only PURS and PREF as successful payments
        if (!SibsPayAPIService.isPaymentTypePurs(this.responseInquiry.getPaymentType())
                && !SibsPayAPIService.isPaymentTypePref(this.responseInquiry.getPaymentType())) {
            return false;
        }

        return SibsPayAPIService.isPaid(this.responseInquiry.getPaymentStatus());
    }

    public boolean isPending() {
        return SibsPayAPIService.isPending(this.responseInquiry.getPaymentStatus());
    }

    public boolean isDeclined() {
        return SibsPayAPIService.isDeclined(this.responseInquiry.getPaymentStatus());
    }

    public boolean isExpired() {
        return SibsPayAPIService.isExpired(this.responseInquiry.getPaymentStatus());
    }

    public String getRequestLog() {
        return this.requestLog;
    }

    public String getResponseLog() {
        return this.responseLog;
    }

}
