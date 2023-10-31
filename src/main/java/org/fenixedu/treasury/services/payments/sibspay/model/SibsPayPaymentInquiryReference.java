package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PaymentInquiryReference
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayPaymentInquiryReference {
    @JsonProperty("reference")
    private String reference = null;

    @JsonProperty("entity")
    private String entity = null;

    @JsonProperty("paymentEntity")
    private String paymentEntity = null;

    @JsonProperty("amount")
    private SibsPayAmount amount = null;

    @JsonProperty("status")
    private String status = null;

    @JsonProperty("expireDate")
    private DateTime expireDate = null;

    public SibsPayPaymentInquiryReference reference(String reference) {
        this.reference = reference;
        return this;
    }

    /**
     * Payment Reference
     * 
     * @return reference
     **/

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public SibsPayPaymentInquiryReference entity(String entity) {
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

    public SibsPayPaymentInquiryReference paymentEntity(String paymentEntity) {
        this.paymentEntity = paymentEntity;
        return this;
    }

    /**
     * Payment Reference Entity
     * 
     * @return paymentEntity
     **/

    public String getPaymentEntity() {
        return paymentEntity;
    }

    public void setPaymentEntity(String paymentEntity) {
        this.paymentEntity = paymentEntity;
    }

    public SibsPayPaymentInquiryReference amount(SibsPayAmount amount) {
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

    public SibsPayPaymentInquiryReference status(String status) {
        this.status = status;
        return this;
    }

    /**
     * Payment Reference Status
     * 
     * @return status
     **/

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public DateTime getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(DateTime expireDate) {
        this.expireDate = expireDate;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayPaymentInquiryReference paymentInquiryReference = (SibsPayPaymentInquiryReference) o;
        return Objects.equals(this.reference, paymentInquiryReference.reference)
                && Objects.equals(this.entity, paymentInquiryReference.entity)
                && Objects.equals(this.paymentEntity, paymentInquiryReference.paymentEntity)
                && Objects.equals(this.amount, paymentInquiryReference.amount)
                && Objects.equals(this.status, paymentInquiryReference.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, entity, paymentEntity, amount, status);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PaymentInquiryReference {\n");

        sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
        sb.append("    entity: ").append(toIndentedString(entity)).append("\n");
        sb.append("    paymentEntity: ").append(toIndentedString(paymentEntity)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("    status: ").append(toIndentedString(status)).append("\n");
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
