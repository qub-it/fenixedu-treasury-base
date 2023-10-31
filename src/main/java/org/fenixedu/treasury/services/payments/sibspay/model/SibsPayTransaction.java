package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that defines a transaction.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayTransaction {
    @JsonProperty("transactionTimestamp")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ")
    private DateTime transactionTimestamp = null;

    @JsonProperty("description")
    private String description = null;

    @JsonProperty("moto")
    private Boolean moto = null;

    @JsonProperty("paymentType")
    private String paymentType = null;

    @JsonProperty("paymentMethod")
    private List<String> paymentMethod = new ArrayList<>();

    @JsonProperty("amount")
    private SibsPayAmount amount = null;

    @JsonProperty("paymentReference")
    private SibsPayPaymentReference paymentReference = null;

    public SibsPayTransaction transactionTimestamp(DateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
        return this;
    }

    /**
     * Timestamp of the transaction.
     * 
     * @return transactionTimestamp
     **/

    public DateTime getTransactionTimestamp() {
        return transactionTimestamp;
    }

    public void setTransactionTimestamp(DateTime transactionTimestamp) {
        this.transactionTimestamp = transactionTimestamp;
    }

    public SibsPayTransaction description(String description) {
        this.description = description;
        return this;
    }

    /**
     * Description of the transaction.
     * 
     * @return description
     **/

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public SibsPayTransaction moto(Boolean moto) {
        this.moto = moto;
        return this;
    }

    /**
     * Mail Order Telephone Order
     * 
     * @return moto
     **/

    public Boolean isMoto() {
        return moto;
    }

    public void setMoto(Boolean moto) {
        this.moto = moto;
    }

    public SibsPayTransaction paymentType(String paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    /**
     * Type of payment used by the client.
     * 
     * @return paymentType
     **/

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public SibsPayTransaction paymentMethod(List<String> paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    /**
     * Method of payment used by the client.
     * 
     * @return paymentMethod
     **/

    public List<String> getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(List<String> paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public SibsPayTransaction amount(SibsPayAmount amount) {
        this.amount = amount;
        return this;
    }

    /**
     * Get amount
     * 
     * @return amount
     **/

    public SibsPayAmount getAmount() {
        return amount;
    }

    public void setAmount(SibsPayAmount amount) {
        this.amount = amount;
    }

    public SibsPayTransaction paymentReference(SibsPayPaymentReference paymentReference) {
        this.paymentReference = paymentReference;
        return this;
    }

    /**
     * Get paymentReference
     * 
     * @return paymentReference
     **/

    public SibsPayPaymentReference getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(SibsPayPaymentReference paymentReference) {
        this.paymentReference = paymentReference;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayTransaction transaction = (SibsPayTransaction) o;
        return Objects.equals(this.transactionTimestamp, transaction.transactionTimestamp)
                && Objects.equals(this.description, transaction.description) && Objects.equals(this.moto, transaction.moto)
                && Objects.equals(this.paymentType, transaction.paymentType)
                && Objects.equals(this.paymentMethod, transaction.paymentMethod)
                && Objects.equals(this.amount, transaction.amount)
                && Objects.equals(this.paymentReference, transaction.paymentReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionTimestamp, description, moto, paymentType, paymentMethod, amount, paymentReference);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Transaction {\n");

        sb.append("    transactionTimestamp: ").append(toIndentedString(transactionTimestamp)).append("\n");
        sb.append("    description: ").append(toIndentedString(description)).append("\n");
        sb.append("    moto: ").append(toIndentedString(moto)).append("\n");
        sb.append("    paymentType: ").append(toIndentedString(paymentType)).append("\n");
        sb.append("    paymentMethod: ").append(toIndentedString(paymentMethod)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("    paymentReference: ").append(toIndentedString(paymentReference)).append("\n");
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
