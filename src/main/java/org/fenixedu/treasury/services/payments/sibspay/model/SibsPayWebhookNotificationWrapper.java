package org.fenixedu.treasury.services.payments.sibspay.model;

import java.math.BigDecimal;

import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayService;
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
        return null;
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

    @Override
    public boolean isOperationSuccess() {
        return SibsPayService.isOperationSuccess(this.webhookNotification.getReturnStatus().getStatusCode());
    }

    @Override
    public boolean isPaid() {
        return SibsPayService.isPaid(this.getPaymentResultCode());
    }

    public boolean isPending() {
        return SibsPayService.isPending(this.getPaymentResultCode());
    }

    public boolean isExpired() {
        return SibsPayService.isExpired(this.getPaymentResultCode());
    }

}
