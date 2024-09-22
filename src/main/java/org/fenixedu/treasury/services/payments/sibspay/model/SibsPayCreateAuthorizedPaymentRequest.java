package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SibsPayCreateAuthorizedPaymentRequest {

    @JsonProperty("merchant")
    private SibsPayMerchant merchant = null;

    @JsonProperty("mandate")
    private SibsPayMandate mandate = null;

    @JsonProperty("info")
    private SibsPayInfo info = null;

    public SibsPayMerchant getMerchant() {
        return merchant;
    }

    public SibsPayCreateAuthorizedPaymentRequest merchant(SibsPayMerchant merchant) {
        this.merchant = merchant;
        return this;
    }

    public void setMerchant(SibsPayMerchant merchant) {
        this.merchant = merchant;
    }

    public SibsPayMandate getMandate() {
        return mandate;
    }

    public SibsPayCreateAuthorizedPaymentRequest mandate(SibsPayMandate mandate) {
        this.mandate = mandate;
        return this;
    }

    public void setMandate(SibsPayMandate mandate) {
        this.mandate = mandate;
    }

    public SibsPayInfo getInfo() {
        return info;
    }

    public SibsPayCreateAuthorizedPaymentRequest info(SibsPayInfo info) {
        this.info = info;
        return this;
    }

    public void setInfo(SibsPayInfo info) {
        this.info = info;
    }

}