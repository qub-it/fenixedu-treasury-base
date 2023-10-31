package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that defines the checkout operation request fields
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayRequestCheckout {
    
    @JsonProperty("merchant")
    private SibsPayMerchant merchant = null;

    @JsonProperty("customer")
    private SibsPayCustomer customer = null;

    @JsonProperty("transaction")
    private SibsPayTransaction transaction = null;

    @JsonProperty("info")
    private SibsPayInfo info = null;

    @JsonProperty("originalTransaction")
    private SibsPayOriginalTransaction originalTransaction = null;

    @JsonProperty("tokenisation")
    private SibsPayTokenisation tokenisation = null;

    @JsonProperty("recurringTransaction")
    private SibsPayRecurringTransactionInput recurringTransaction = null;

    public SibsPayRequestCheckout merchant(SibsPayMerchant merchant) {
        this.merchant = merchant;
        return this;
    }

    /**
     * Get merchant
     * 
     * @return merchant
     **/
    public SibsPayMerchant getMerchant() {
        return merchant;
    }

    public void setMerchant(SibsPayMerchant merchant) {
        this.merchant = merchant;
    }

    public SibsPayRequestCheckout customer(SibsPayCustomer customer) {
        this.customer = customer;
        return this;
    }

    /**
     * Get customer
     * 
     * @return customer
     **/

    public SibsPayCustomer getCustomer() {
        return customer;
    }

    public void setCustomer(SibsPayCustomer customer) {
        this.customer = customer;
    }

    public SibsPayRequestCheckout transaction(SibsPayTransaction transaction) {
        this.transaction = transaction;
        return this;
    }

    /**
     * Get transaction
     * 
     * @return transaction
     **/

    public SibsPayTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(SibsPayTransaction transaction) {
        this.transaction = transaction;
    }

    public SibsPayRequestCheckout info(SibsPayInfo info) {
        this.info = info;
        return this;
    }

    /**
     * Get info
     * 
     * @return info
     **/

    public SibsPayInfo getInfo() {
        return info;
    }

    public void setInfo(SibsPayInfo info) {
        this.info = info;
    }

    public SibsPayRequestCheckout originalTransaction(SibsPayOriginalTransaction originalTransaction) {
        this.originalTransaction = originalTransaction;
        return this;
    }

    /**
     * Get originalTransaction
     * 
     * @return originalTransaction
     **/

    public SibsPayOriginalTransaction getOriginalTransaction() {
        return originalTransaction;
    }

    public void setOriginalTransaction(SibsPayOriginalTransaction originalTransaction) {
        this.originalTransaction = originalTransaction;
    }

    public SibsPayRequestCheckout tokenisation(SibsPayTokenisation tokenisation) {
        this.tokenisation = tokenisation;
        return this;
    }

    /**
     * Get tokenisation
     * 
     * @return tokenisation
     **/

    public SibsPayTokenisation getTokenisation() {
        return tokenisation;
    }

    public void setTokenisation(SibsPayTokenisation tokenisation) {
        this.tokenisation = tokenisation;
    }

    public SibsPayRequestCheckout recurringTransaction(SibsPayRecurringTransactionInput recurringTransaction) {
        this.recurringTransaction = recurringTransaction;
        return this;
    }

    /**
     * Get recurringTransaction
     * 
     * @return recurringTransaction
     **/

    public SibsPayRecurringTransactionInput getRecurringTransaction() {
        return recurringTransaction;
    }

    public void setRecurringTransaction(SibsPayRecurringTransactionInput recurringTransaction) {
        this.recurringTransaction = recurringTransaction;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayRequestCheckout requestCheckout = (SibsPayRequestCheckout) o;
        return Objects.equals(this.merchant, requestCheckout.merchant) && Objects.equals(this.customer, requestCheckout.customer)
                && Objects.equals(this.transaction, requestCheckout.transaction)
                && Objects.equals(this.info, requestCheckout.info)
                && Objects.equals(this.originalTransaction, requestCheckout.originalTransaction)
                && Objects.equals(this.tokenisation, requestCheckout.tokenisation)
                && Objects.equals(this.recurringTransaction, requestCheckout.recurringTransaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merchant, customer, transaction, info, originalTransaction, tokenisation, recurringTransaction);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class RequestCheckout {\n");

        sb.append("    merchant: ").append(toIndentedString(merchant)).append("\n");
        sb.append("    customer: ").append(toIndentedString(customer)).append("\n");
        sb.append("    transaction: ").append(toIndentedString(transaction)).append("\n");
        sb.append("    info: ").append(toIndentedString(info)).append("\n");
        sb.append("    originalTransaction: ").append(toIndentedString(originalTransaction)).append("\n");
        sb.append("    tokenisation: ").append(toIndentedString(tokenisation)).append("\n");
        sb.append("    recurringTransaction: ").append(toIndentedString(recurringTransaction)).append("\n");
        sb.append("}");
        return sb.toString();
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(java.lang.Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
