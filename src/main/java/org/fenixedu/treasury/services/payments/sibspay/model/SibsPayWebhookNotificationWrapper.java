package org.fenixedu.treasury.services.payments.sibspay.model;

import java.math.BigDecimal;

import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayAPIService;
import org.joda.time.DateTime;

public class SibsPayWebhookNotificationWrapper implements DigitalPlatformResultBean {

    private SibsPayWebhookNotification webhookNotification;

    public SibsPayWebhookNotificationWrapper(SibsPayWebhookNotification webhookNotification) {
        this.webhookNotification = webhookNotification;
    }

    @Override
    public BigDecimal getAmount() {
        return this.webhookNotification.getAmount().getValue();
    }

    @Override
    public String getMerchantTransactionId() {
        return null;
    }

    @Override
    public String getPaymentBrand() {
        return this.webhookNotification.getPaymentMethod();
    }

    @Override
    public DateTime getPaymentDate() {
        return this.webhookNotification.getTransactionDateTime();
    }

    @Override
    public String getPaymentResultCode() {
        return this.webhookNotification.getPaymentStatus();
    }

    @Override
    public String getPaymentResultDescription() {
        return null;
    }

    @Override
    public String getPaymentType() {
        return this.webhookNotification.getPaymentMethod();
    }

    @Override
    public String getTimestamp() {
        return null;
    }

    @Override
    public String getTransactionId() {
        return this.webhookNotification.getTransactionID();
    }

    public String getOperationStatusCode() {
        return this.webhookNotification.getReturnStatus().getStatusCode();
    }

    public String getOperationStatusMessage() {
        return this.webhookNotification.getReturnStatus().getStatusMsg();
    }

    public String getOperationStatusDescription() {
        return this.webhookNotification.getReturnStatus().getStatusDescription();
    }

    public String getNotificationID() {
        return this.webhookNotification.getNotificationID();
    }

    @Override
    public boolean isOperationSuccess() {
        return SibsPayAPIService.isOperationSuccess(this.webhookNotification.getReturnStatus().getStatusCode());
    }

    @Override
    public boolean isPaid() {
        return SibsPayAPIService.isPaid(this.getPaymentResultCode());
    }

    public boolean isPending() {
        return SibsPayAPIService.isPending(this.getPaymentResultCode());
    }

    public boolean isExpired() {
        return SibsPayAPIService.isExpired(this.getPaymentResultCode());
    }

    public boolean isDeclined() {
        return SibsPayAPIService.isDeclined(this.getPaymentResultCode());
    }

}
