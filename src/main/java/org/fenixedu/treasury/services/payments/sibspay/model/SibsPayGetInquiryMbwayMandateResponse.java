package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SibsPayGetInquiryMbwayMandateResponse {

    @JsonProperty("returnStatus")
    private SibsPayReturnStatus returnStatus = null;

    @JsonProperty("mandate")
    private SibsPayMandate mandate;

    @JsonProperty("moreElementsIndicator")
    private boolean moreElementsIndicator;

    @JsonProperty("execution")
    private SibsPayExecution execution = null;

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

}
