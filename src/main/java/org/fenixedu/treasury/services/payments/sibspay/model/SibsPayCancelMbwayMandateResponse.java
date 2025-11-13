package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayAPIService;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayCancelMbwayMandateResponse {

    @JsonProperty("returnStatus")
    private SibsPayReturnStatus returnStatus = null;

    @JsonProperty("transactionID")
    private String transactionID = null;

    @JsonProperty("transactionSignature")
    private String transactionSignature = null;

    @JsonProperty("execution")
    private SibsPayExecution execution = null;

    @JsonProperty("signature")
    private String signature;

    @JsonIgnore
    private String requestLog;

    @JsonIgnore
    private String responseLog;

    public boolean isOperationSuccess() {
        return this.returnStatus != null && SibsPayAPIService.isOperationSuccess(this.returnStatus.getStatusCode());
    }

    public String getOperationStatusCode() {
        if (this.returnStatus == null) {
            return null;
        }

        return this.returnStatus.getStatusCode();
    }

    public String getOperationStatusMessage() {
        if (this.returnStatus == null) {
            return null;
        }

        return this.returnStatus.getStatusMsg();
    }

    public String getOperationStatusDescription() {
        if (this.returnStatus == null) {
            return null;
        }

        return this.returnStatus.getStatusDescription();
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

    public String getTransactionSignature() {
        return transactionSignature;
    }

    public void setTransactionSignature(String transactionSignature) {
        this.transactionSignature = transactionSignature;
    }

    public SibsPayExecution getExecution() {
        return execution;
    }

    public void setExecution(SibsPayExecution execution) {
        this.execution = execution;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
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
