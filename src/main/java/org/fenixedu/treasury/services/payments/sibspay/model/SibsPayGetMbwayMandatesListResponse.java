package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SibsPayGetMbwayMandatesListResponse {

    @JsonProperty("returnStatus")
    private SibsPayReturnStatus returnStatus = null;

    @JsonProperty("mandates")
    private List<SibsPayMandate> mandates;

    @JsonProperty("moreElementsIndicator")
    private boolean moreElementsIndicator;

    @JsonProperty("execution")
    private SibsPayExecution execution = null;

    @JsonIgnore
    private String requestLog;

    @JsonIgnore
    private String responseLog;

    public SibsPayReturnStatus getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(SibsPayReturnStatus returnStatus) {
        this.returnStatus = returnStatus;
    }

    public List<SibsPayMandate> getMandates() {
        return mandates;
    }

    public void setMandates(List<SibsPayMandate> mandates) {
        this.mandates = mandates;
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

}