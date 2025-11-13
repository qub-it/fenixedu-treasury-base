package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayAPIService;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayCreateMbwayMandateResponse {

    @JsonProperty("returnStatus")
    private SibsPayReturnStatus returnStatus = null;

    @JsonProperty("transactionId")
    private String transactionId = null;

    @JsonProperty("transactionSignature")
    private String transactionSignature = null;

    @JsonProperty("execution")
    private SibsPayExecution execution = null;

    @JsonProperty("mandate")
    private SibsPayMandate mandate;

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

    public boolean isMandateCreationSuccess() {
        return isOperationSuccess();
    }

    public SibsPayReturnStatus getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(SibsPayReturnStatus returnStatus) {
        this.returnStatus = returnStatus;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
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

    public SibsPayMandate getMandate() {
        return mandate;
    }

    public void setMandate(SibsPayMandate mandate) {
        this.mandate = mandate;
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