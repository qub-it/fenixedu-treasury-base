package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SibsPayRequestCheckoutMandate {

    @JsonProperty("mandateId")
    private String mandateId;

    public String getMandateId() {
        return mandateId;
    }

    public void setMandateId(String mandateId) {
        this.mandateId = mandateId;
    }
}
