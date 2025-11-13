package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.StringUtils;
import org.fenixedu.treasury.domain.sibspay.MbwayMandateState;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayAPIService;

import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayGetInquiryMbwayMandateResponse {

    @JsonProperty("returnStatus")
    private SibsPayReturnStatus returnStatus = null;

    @JsonProperty("mandate")
    private SibsPayMandate mandate;

    @JsonProperty("moreElementsIndicator")
    private boolean moreElementsIndicator;

    @JsonProperty("execution")
    private SibsPayExecution execution = null;

    @JsonIgnore
    private String requestLog;

    @JsonIgnore
    private String responseLog;

    public boolean isOperationSuccess() {
        return this.returnStatus != null && SibsPayAPIService.isOperationSuccess(this.returnStatus.getStatusCode());
    }

    public boolean isOperationErrorUnknownAuthPayment() {
        return this.returnStatus != null && SibsPayAPIService.isUnknownMbwayAuthorizedPayment(this.returnStatus.getStatusCode());
    }

    public MbwayMandateState getCurrentMandateState() {
        if (getMandate() != null && StringUtils.isNotEmpty(getMandate().getMandateStatus())) {
            return switch (this.mandate.getMandateStatus()) {
            case "ACTV" -> MbwayMandateState.ACTIVE;
            case "SSPN" -> MbwayMandateState.SUSPENDED;
            case "EXPR" -> MbwayMandateState.EXPIRED;
            case "CNCL" -> MbwayMandateState.CANCELED;
            default -> throw new IllegalArgumentException("Unexpected value: " + this.mandate.getMandateStatus());
            };
        }

        return null;
    }

    public SibsPayReturnStatus getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(SibsPayReturnStatus returnStatus) {
        this.returnStatus = returnStatus;
    }

    public SibsPayMandate getMandate() {
        return mandate;
    }

    public void setMandates(SibsPayMandate mandate) {
        this.mandate = mandate;
    }

    public boolean isMoreElementsIndicator() {
        return moreElementsIndicator;
    }

    public void setMoreElementsIndicator(boolean moreElementsIndicator) {
        this.moreElementsIndicator = moreElementsIndicator;
    }

    public SibsPayExecution getExecution() {
        return execution;
    }

    public void setExecution(SibsPayExecution execution) {
        this.execution = execution;
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
