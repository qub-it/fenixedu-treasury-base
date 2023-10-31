package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that defines the inquiry operation return fields
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayResponseInquiry {
    @JsonProperty("returnStatus")
    private SibsPayReturnStatus returnStatus = null;

    @JsonProperty("paymentStatus")
    private String paymentStatus = null;

    @JsonProperty("paymentMethod")
    private String paymentMethod = null;

    @JsonProperty("transactionID")
    private String transactionID = null;

    @JsonProperty("amount")
    private SibsPayAmount amount = null;

    @JsonProperty("merchant")
    private SibsPayMerchant merchant = null;

    @JsonProperty("paymentType")
    private String paymentType = null;

    @JsonProperty("paymentReference")
    private SibsPayPaymentInquiryReference paymentReference = null;

    @JsonProperty("token")
    private SibsPayTokenInquiry token = null;

    @JsonProperty("recurringTransaction")
    private SibsPayRecurringTransactionOutput recurringTransaction = null;

    @JsonProperty("execution")
    private SibsPayExecution execution = null;

    public SibsPayResponseInquiry returnStatus(SibsPayReturnStatus returnStatus) {
        this.returnStatus = returnStatus;
        return this;
    }

    /**
     * Get returnStatus
     * 
     * @return returnStatus
     **/

    public SibsPayReturnStatus getReturnStatus() {
        return returnStatus;
    }

    public void setReturnStatus(SibsPayReturnStatus returnStatus) {
        this.returnStatus = returnStatus;
    }

    public SibsPayResponseInquiry paymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
        return this;
    }

    /**
     * Get paymentStatus
     * 
     * @return paymentStatus
     **/

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public SibsPayResponseInquiry paymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
        return this;
    }

    /**
     * Get paymentMethod
     * 
     * @return paymentMethod
     **/

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public SibsPayResponseInquiry transactionID(String transactionID) {
        this.transactionID = transactionID;
        return this;
    }

    /**
     * Get transactionID
     * 
     * @return transactionID
     **/

    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public SibsPayResponseInquiry amount(SibsPayAmount amount) {
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

    public SibsPayResponseInquiry merchant(SibsPayMerchant merchant) {
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

    public SibsPayResponseInquiry paymentType(String paymentType) {
        this.paymentType = paymentType;
        return this;
    }

    /**
     * Get paymentType
     * 
     * @return paymentType
     **/

    public String getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(String paymentType) {
        this.paymentType = paymentType;
    }

    public SibsPayResponseInquiry paymentReference(SibsPayPaymentInquiryReference paymentReference) {
        this.paymentReference = paymentReference;
        return this;
    }

    /**
     * Get paymentReference
     * 
     * @return paymentReference
     **/

    public SibsPayPaymentInquiryReference getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(SibsPayPaymentInquiryReference paymentReference) {
        this.paymentReference = paymentReference;
    }

    public SibsPayResponseInquiry token(SibsPayTokenInquiry token) {
        this.token = token;
        return this;
    }

    /**
     * Get token
     * 
     * @return token
     **/

    public SibsPayTokenInquiry getToken() {
        return token;
    }

    public void setToken(SibsPayTokenInquiry token) {
        this.token = token;
    }

    public SibsPayResponseInquiry recurringTransaction(SibsPayRecurringTransactionOutput recurringTransaction) {
        this.recurringTransaction = recurringTransaction;
        return this;
    }

    /**
     * Get recurringTransaction
     * 
     * @return recurringTransaction
     **/

    public SibsPayRecurringTransactionOutput getRecurringTransaction() {
        return recurringTransaction;
    }

    public void setRecurringTransaction(SibsPayRecurringTransactionOutput recurringTransaction) {
        this.recurringTransaction = recurringTransaction;
    }

    public SibsPayResponseInquiry execution(SibsPayExecution execution) {
        this.execution = execution;
        return this;
    }

    /**
     * Get execution
     * 
     * @return execution
     **/

    public SibsPayExecution getExecution() {
        return execution;
    }

    public void setExecution(SibsPayExecution execution) {
        this.execution = execution;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayResponseInquiry responseInquiry = (SibsPayResponseInquiry) o;
        return Objects.equals(this.returnStatus, responseInquiry.returnStatus)
                && Objects.equals(this.paymentStatus, responseInquiry.paymentStatus)
                && Objects.equals(this.paymentMethod, responseInquiry.paymentMethod)
                && Objects.equals(this.transactionID, responseInquiry.transactionID)
                && Objects.equals(this.amount, responseInquiry.amount) && Objects.equals(this.merchant, responseInquiry.merchant)
                && Objects.equals(this.paymentType, responseInquiry.paymentType)
                && Objects.equals(this.paymentReference, responseInquiry.paymentReference)
                && Objects.equals(this.token, responseInquiry.token)
                && Objects.equals(this.recurringTransaction, responseInquiry.recurringTransaction)
                && Objects.equals(this.execution, responseInquiry.execution);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnStatus, paymentStatus, paymentMethod, transactionID, amount, merchant, paymentType,
                paymentReference, token, recurringTransaction, execution);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ResponseInquiry {\n");

        sb.append("    returnStatus: ").append(toIndentedString(returnStatus)).append("\n");
        sb.append("    paymentStatus: ").append(toIndentedString(paymentStatus)).append("\n");
        sb.append("    paymentMethod: ").append(toIndentedString(paymentMethod)).append("\n");
        sb.append("    transactionID: ").append(toIndentedString(transactionID)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("    merchant: ").append(toIndentedString(merchant)).append("\n");
        sb.append("    paymentType: ").append(toIndentedString(paymentType)).append("\n");
        sb.append("    paymentReference: ").append(toIndentedString(paymentReference)).append("\n");
        sb.append("    token: ").append(toIndentedString(token)).append("\n");
        sb.append("    recurringTransaction: ").append(toIndentedString(recurringTransaction)).append("\n");
        sb.append("    execution: ").append(toIndentedString(execution)).append("\n");
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
