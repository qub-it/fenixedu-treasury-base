package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.joda.time.LocalDate;

/**
 * Object that defines a Mandate
 */

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayMandate {

    @JsonProperty("mandateType")
    private String mandateType;

    @JsonProperty("aliasMBWAY")
    private String aliasMBWAY;

    @JsonProperty("customerName")
    private String customerName;

    @JsonProperty("mandateId")
    private String mandateId;

    @JsonProperty("mandateStatus")
    private String mandateStatus;

    @JsonProperty("mandateExpirationDate")
    private String mandateExpirationDate;

    @JsonProperty("amountLimit")
    private SibsPayAmount amountLimit;

    @JsonProperty("transactionId")
    private String transactionId;

    @JsonProperty("mandateIdentification")
    private String mandateIdentification;

    @JsonProperty("mandateAction")
    private String mandateAction;

    @JsonProperty("mandateActionStatus")
    private String mandateActionStatus;

    @JsonProperty("clientName")
    private String clientName;

    @JsonProperty("mandateAvailable")
    private Boolean mandateAvailable;

    @JsonProperty("termsAndConditions")
    private String termsAndConditions;

    @JsonProperty("mandateAmountLimit")
    private SibsPayAmount mandateAmountLimit;

    public String getMandateType() {
        return mandateType;
    }

    public SibsPayMandate MandateType(String mandateType) {
        this.mandateType = mandateType;
        return this;
    }

    public void setMandateType(String mandateType) {
        this.mandateType = mandateType;
    }

    public String getAliasMBWAY() {
        return aliasMBWAY;
    }

    public SibsPayMandate aliasMBWAY(String aliasMBWAY) {
        this.aliasMBWAY = aliasMBWAY;
        return this;
    }

    public void setAliasMBWAY(String aliasMBWAY) {
        this.aliasMBWAY = aliasMBWAY;
    }

    public String getCustomerName() {
        return customerName;
    }

    public SibsPayMandate customerName(String customerName) {
        this.customerName = customerName;
        return this;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getMandateId() {
        return mandateId;
    }

    public void setMandateId(String mandateId) {
        this.mandateId = mandateId;
    }

    public String getMandateStatus() {
        return mandateStatus;
    }

    public void setMandateStatus(String mandateStatus) {
        this.mandateStatus = mandateStatus;
    }

    public String getMandateExpirationDate() {
        return mandateExpirationDate;
    }

    public void setMandateExpirationDate(String mandateExpirationDate) {
        this.mandateExpirationDate = mandateExpirationDate;
    }

    public SibsPayAmount getAmountLimit() {
        return amountLimit;
    }

    public void setAmountLimit(SibsPayAmount amountLimit) {
        this.amountLimit = amountLimit;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public String getMandateIdentification() {
        return mandateIdentification;
    }

    public void setMandateIdentification(String mandateIdentification) {
        this.mandateIdentification = mandateIdentification;
    }

    public String getMandateAction() {
        return mandateAction;
    }

    public void setMandateAction(String mandateAction) {
        this.mandateAction = mandateAction;
    }

    public String getMandateActionStatus() {
        return mandateActionStatus;
    }

    public void setMandateActionStatus(String mandateActionStatus) {
        this.mandateActionStatus = mandateActionStatus;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public Boolean getMandateAvailable() {
        return mandateAvailable;
    }

    public void setMandateAvailable(Boolean mandateAvailable) {
        this.mandateAvailable = mandateAvailable;
    }

    public String getTermsAndConditions() {
        return termsAndConditions;
    }

    public void setTermsAndConditions(String termsAndConditions) {
        this.termsAndConditions = termsAndConditions;
    }

    public SibsPayAmount getMandateAmountLimit() {
        return mandateAmountLimit;
    }

    public void setMandateAmountLimit(SibsPayAmount mandateAmountLimit) {
        this.mandateAmountLimit = mandateAmountLimit;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SibsPayMandate mandate = (SibsPayMandate) o;
        return Objects.equals(this.mandateType, mandate.mandateType) //
                && Objects.equals(this.aliasMBWAY, mandate.aliasMBWAY) //
                && Objects.equals(this.customerName, mandate.customerName) //
                && Objects.equals(this.mandateId, mandate.mandateId) //
                && Objects.equals(this.mandateStatus, mandate.mandateStatus) //
                && Objects.equals(this.mandateExpirationDate, mandate.mandateExpirationDate) //
                && Objects.equals(this.amountLimit, mandate.amountLimit) //
                && Objects.equals(this.transactionId, mandate.transactionId)
                && Objects.equals(this.mandateIdentification, mandate.mandateIdentification)
                && Objects.equals(this.mandateAction, mandate.mandateAction)
                && Objects.equals(this.mandateActionStatus, mandate.mandateActionStatus)
                && Objects.equals(this.clientName, mandate.clientName)
                && Objects.equals(this.mandateAvailable, mandate.mandateAvailable)
                && Objects.equals(this.termsAndConditions, mandate.termsAndConditions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mandateType, this.aliasMBWAY, this.customerName, this.mandateId, this.mandateStatus,
                this.mandateExpirationDate, this.amountLimit, this.transactionId, this.mandateIdentification, this.mandateAction,
                this.mandateActionStatus, this.clientName, this.mandateAvailable, this.termsAndConditions);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Mandate {\n");

        sb.append("    mandateType: ").append(toIndentedString(this.mandateType)).append("\n");
        sb.append("    aliasMBWAY: ").append(toIndentedString(this.aliasMBWAY)).append("\n");
        sb.append("    customerName: ").append(toIndentedString(this.customerName)).append("\n");
        sb.append("    mandateId: ").append(toIndentedString(this.mandateId)).append("\n");
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