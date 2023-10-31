package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PaymentReference
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayPaymentReference {
    @JsonProperty("entity")
    private String entity = null;

    @JsonProperty("minAmount")
    private SibsPayAmount minAmount = null;

    @JsonProperty("maxAmount")
    private SibsPayAmount maxAmount = null;

    @JsonProperty("initialDatetime")
    private DateTime initialDatetime = null;

    @JsonProperty("finalDatetime")
    private DateTime finalDatetime = null;

    public SibsPayPaymentReference entity(String entity) {
        this.entity = entity;
        return this;
    }

    /**
     * Payment Reference Entity
     * 
     * @return entity
     **/

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public SibsPayPaymentReference minAmount(SibsPayAmount minAmount) {
        this.minAmount = minAmount;
        return this;
    }

    /**
     * Get minAmount
     * 
     * @return minAmount
     **/

    public SibsPayAmount getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(SibsPayAmount minAmount) {
        this.minAmount = minAmount;
    }

    public SibsPayPaymentReference maxAmount(SibsPayAmount maxAmount) {
        this.maxAmount = maxAmount;
        return this;
    }

    /**
     * Get maxAmount
     * 
     * @return maxAmount
     **/

    public SibsPayAmount getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(SibsPayAmount maxAmount) {
        this.maxAmount = maxAmount;
    }

    public SibsPayPaymentReference initialDatetime(DateTime initialDatetime) {
        this.initialDatetime = initialDatetime;
        return this;
    }

    /**
     * Timestamp for the payment reference start.
     * 
     * @return initialDatetime
     **/

    public DateTime getInitialDatetime() {
        return initialDatetime;
    }

    public void setInitialDatetime(DateTime initialDatetime) {
        this.initialDatetime = initialDatetime;
    }

    public SibsPayPaymentReference finalDatetime(DateTime finalDatetime) {
        this.finalDatetime = finalDatetime;
        return this;
    }

    /**
     * Timestamp for the payment reference end.
     * 
     * @return finalDatetime
     **/

    public DateTime getFinalDatetime() {
        return finalDatetime;
    }

    public void setFinalDatetime(DateTime finalDatetime) {
        this.finalDatetime = finalDatetime;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayPaymentReference paymentReference = (SibsPayPaymentReference) o;
        return Objects.equals(this.entity, paymentReference.entity) && Objects.equals(this.minAmount, paymentReference.minAmount)
                && Objects.equals(this.maxAmount, paymentReference.maxAmount)
                && Objects.equals(this.initialDatetime, paymentReference.initialDatetime)
                && Objects.equals(this.finalDatetime, paymentReference.finalDatetime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, minAmount, maxAmount, initialDatetime, finalDatetime);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PaymentReference {\n");

        sb.append("    entity: ").append(toIndentedString(entity)).append("\n");
        sb.append("    minAmount: ").append(toIndentedString(minAmount)).append("\n");
        sb.append("    maxAmount: ").append(toIndentedString(maxAmount)).append("\n");
        sb.append("    initialDatetime: ").append(toIndentedString(initialDatetime)).append("\n");
        sb.append("    finalDatetime: ").append(toIndentedString(finalDatetime)).append("\n");
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
