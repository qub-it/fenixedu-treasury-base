package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SibsPayWebhookNotification {

    @JsonProperty("returnStatus")
    private SibsPayReturnStatus returnStatus = null;

    @JsonProperty("paymentStatus")
    private String paymentStatus = null;

    @JsonProperty("paymentMethod")
    private String paymentMethod = null;

    @JsonProperty("transactionID")
    private String transactionID = null;

    @JsonProperty("amount")
    private SibsPayAmount amount = null;

    @JsonProperty("merchant")
    private SibsPayMerchant merchant = null;

    @JsonProperty("paymentType")
    private String paymentType = null;

    @JsonProperty("notificationID")
    private String notificationID = null;

    public SibsPayReturnStatus getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(SibsPayReturnStatus returnStatus) {
        this.returnStatus = returnStatus;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public SibsPayAmount getAmount() {
        return amount;
    }

    public void setAmount(SibsPayAmount amount) {
        this.amount = amount;
    }

    public SibsPayMerchant getMerchant() {
        return merchant;
    }

    public void setMerchant(SibsPayMerchant merchant) {
        this.merchant = merchant;
    }

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public String getNotificationID() {
        return notificationID;
    }

    public void setNotificationID(String notificationID) {
        this.notificationID = notificationID;
    }

}
