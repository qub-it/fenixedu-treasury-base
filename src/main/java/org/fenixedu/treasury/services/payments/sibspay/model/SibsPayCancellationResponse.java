package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayCancellationResponse {

    @JsonProperty("merchant")
    private SibsPayMerchant merchant = null;

    @JsonProperty("returnStatus")
    private SibsPayReturnStatus returnStatus = null;

    @JsonProperty("transactionID")
    private String transactionID = null;

    @JsonProperty("amount")
    private SibsPayAmount amount = null;

    @JsonProperty("transactionTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    private DateTime transactionTimestamp = null;

    @JsonProperty("transactionRecipientId")
    private String transactionRecipientId = null;

    @JsonProperty("execution")
    private SibsPayExecution execution = null;

    @JsonProperty("paymentStatus")
    private String paymentStatus = null;

    @JsonIgnore
    private String requestLog;

    @JsonIgnore
    private String responseLog;

    public SibsPayMerchant getMerchant() {
        return merchant;
    }

    public void setMerchant(SibsPayMerchant merchant) {
        this.merchant = merchant;
    }

    public SibsPayReturnStatus getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(SibsPayReturnStatus returnStatus) {
        this.returnStatus = returnStatus;
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

    public DateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(DateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public String getTransactionRecipientId() {
        return transactionRecipientId;
    }

    public void setTransactionRecipientId(String transactionRecipientId) {
        this.transactionRecipientId = transactionRecipientId;
    }

    public SibsPayExecution getExecution() {
        return execution;
    }

    public void setExecution(SibsPayExecution execution) {
        this.execution = execution;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getRequestLog() {
        return requestLog;
    }

    public void setRequestLog(String requestLog) {
        this.requestLog = requestLog;
    }

    public String getResponseLog() {
        return responseLog;
    }

    public void setResponseLog(String responseLog) {
        this.responseLog = responseLog;
    }

}
