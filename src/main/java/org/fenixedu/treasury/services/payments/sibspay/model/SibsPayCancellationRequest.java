package org.fenixedu.treasury.services.payments.sibspay.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayCancellationRequest {

    @JsonProperty("merchant")
    private SibsPayMerchant merchant = null;

    @JsonProperty("transaction")
    private SibsPayTransaction transaction = null;

    @JsonProperty("originalTransaction")
    private SibsPayOriginalTransaction originalTransaction = null;

    public SibsPayMerchant getMerchant() {
        return merchant;
    }

    public void setMerchant(SibsPayMerchant merchant) {
        this.merchant = merchant;
    }

    public SibsPayTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(SibsPayTransaction transaction) {
        this.transaction = transaction;
    }

    public SibsPayOriginalTransaction getOriginalTransaction() {
        return originalTransaction;
    }

    public void setOriginalTransaction(SibsPayOriginalTransaction originalTransaction) {
        this.originalTransaction = originalTransaction;
    }

}
