package org.fenixedu.treasury.services.payments.sibspay.model;

import org.joda.time.DateTime;

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

    @JsonProperty("transactionDateTime")
    private DateTime transactionDateTime = null;

    @JsonProperty("amount")
    private SibsPayAmount amount = null;

    @JsonProperty("merchant")
    private SibsPayMerchant merchant = null;

    @JsonProperty("paymentType")
    private String paymentType = null;

    @JsonProperty("paymentReference")
    private SibsPayPaymentInquiryReference paymentReference = null;

    @JsonProperty("token")
    private SibsPayToken token = null;

    // financialOperation

    // mbwayMandate

    // installmentPlan

    // customer

    // merchantInitiatedTransaction

    // threeDSecure

    private String terminalBrand;

    private String wrapperType;

    private String internalTransactionId;

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

    public DateTime getTransactionDateTime() {
        return transactionDateTime;
    }

    public void setTransactionDateTime(DateTime transactionDateTime) {
        this.transactionDateTime = transactionDateTime;
    }

    public SibsPayToken getToken() {
        return token;
    }

    public void setToken(SibsPayToken token) {
        this.token = token;
    }

    public SibsPayPaymentInquiryReference getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(SibsPayPaymentInquiryReference paymentReference) {
        this.paymentReference = paymentReference;
    }

    public String getTerminalBrand() {
        return terminalBrand;
    }

    public void setTerminalBrand(String terminalBrand) {
        this.terminalBrand = terminalBrand;
    }

    public String getWrapperType() {
        return wrapperType;
    }

    public void setWrapperType(String wrapperType) {
        this.wrapperType = wrapperType;
    }

    public String getInternalTransactionId() {
        return internalTransactionId;
    }

    public void setInternalTransactionId(String internalTransactionId) {
        this.internalTransactionId = internalTransactionId;
    }

}
