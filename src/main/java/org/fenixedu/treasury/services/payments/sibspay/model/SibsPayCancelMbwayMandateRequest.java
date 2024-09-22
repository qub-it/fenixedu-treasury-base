package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SibsPayCancelMbwayMandateRequest {

    @JsonProperty("merchant")
    private SibsPayMerchant merchant = null;

    @JsonProperty("info")
    private SibsPayInfo info = null;

    public SibsPayMerchant getMerchant() {
        return merchant;
    }

    public SibsPayCancelMbwayMandateRequest merchant(SibsPayMerchant merchant) {
        this.merchant = merchant;
        return this;
    }

    public void setMerchant(SibsPayMerchant merchant) {
        this.merchant = merchant;
    }
    
    public SibsPayInfo getInfo() {
        return info;
    }

    public SibsPayCancelMbwayMandateRequest info(SibsPayInfo info) {
        this.info = info;
        return this;
    }

    public void setInfo(SibsPayInfo info) {
        this.info = info;
    }
}
