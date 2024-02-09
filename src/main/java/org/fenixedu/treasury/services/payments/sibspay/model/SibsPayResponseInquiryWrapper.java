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
