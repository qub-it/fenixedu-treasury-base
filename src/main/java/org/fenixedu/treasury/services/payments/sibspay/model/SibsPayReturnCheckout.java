package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that defines the checkout operation return fields
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayReturnCheckout {
    @JsonProperty("returnStatus")
    private SibsPayReturnStatus returnStatus = null;

    @JsonProperty("transactionID")
    private String transactionID = null;

    @JsonProperty("transactionSignature")
    private String transactionSignature = null;

    @JsonProperty("amount")
    private SibsPayAmount amount = null;

    @JsonProperty("merchant")
    private SibsPayMerchant merchant = null;

    @JsonProperty("paymentMethodList")
    private List<String> paymentMethodList = null;

    @JsonProperty("tokenList")
    private List<SibsPayToken> tokenList = null;

    @JsonProperty("formContext")
    private String formContext = null;

    @JsonProperty("execution")
    private SibsPayExecution execution = null;

    @JsonProperty("expiry")
    private DateTime expiry;

    @JsonProperty("mandate")
    private SibsPayMandate mandate;

    @JsonIgnore
    private String requestLog;

    @JsonIgnore
    private String responseLog;

    public SibsPayReturnCheckout returnStatus(SibsPayReturnStatus returnStatus) {
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

    public SibsPayReturnCheckout transactionID(String transactionID) {
        this.transactionID = transactionID;
        return this;
    }

    /**
     * Unique identify of the transaction.
     * 
     * @return transactionID
     **/

    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public SibsPayReturnCheckout transactionSignature(String transactionSignature) {
        this.transactionSignature = transactionSignature;
        return this;
    }

    /**
     * Get transactionSignature
     * 
     * @return transactionSignature
     **/

    public String getTransactionSignature() {
        return transactionSignature;
    }

    public void setTransactionSignature(String transactionSignature) {
        this.transactionSignature = transactionSignature;
    }

    public SibsPayReturnCheckout amount(SibsPayAmount amount) {
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

    public SibsPayReturnCheckout merchant(SibsPayMerchant merchant) {
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

    public SibsPayReturnCheckout paymentMethodList(List<String> paymentMethodList) {
        this.paymentMethodList = paymentMethodList;
        return this;
    }

    public SibsPayReturnCheckout addPaymentMethodListItem(String paymentMethodListItem) {
        if (this.paymentMethodList == null) {
            this.paymentMethodList = new ArrayList<String>();
        }
        this.paymentMethodList.add(paymentMethodListItem);
        return this;
    }

    /**
     * Methods of payment accepted by the merchant.
     * 
     * @return paymentMethodList
     **/

    public List<String> getPaymentMethodList() {
        return paymentMethodList;
    }

    public void setPaymentMethodList(List<String> paymentMethodList) {
        this.paymentMethodList = paymentMethodList;
    }

    public SibsPayReturnCheckout tokenList(List<SibsPayToken> tokenList) {
        this.tokenList = tokenList;
        return this;
    }

    public SibsPayReturnCheckout addTokenListItem(SibsPayToken tokenListItem) {
        if (this.tokenList == null) {
            this.tokenList = new ArrayList<SibsPayToken>();
        }
        this.tokenList.add(tokenListItem);
        return this;
    }

    /**
     * Payment tokens.
     * 
     * @return tokenList
     **/
    public List<SibsPayToken> getTokenList() {
        return tokenList;
    }

    public void setTokenList(List<SibsPayToken> tokenList) {
        this.tokenList = tokenList;
    }

    public SibsPayReturnCheckout formContext(String formContext) {
        this.formContext = formContext;
        return this;
    }

    /**
     * Form context base64 encoded.
     * 
     * @return formContext
     **/

    public String getFormContext() {
        return formContext;
    }

    public void setFormContext(String formContext) {
        this.formContext = formContext;
    }

    public SibsPayReturnCheckout execution(SibsPayExecution execution) {
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

    public DateTime getExpiry() {
        return expiry;
    }

    public void setExpiry(DateTime expiry) {
        this.expiry = expiry;
    }

    public SibsPayMandate getMandate() {
        return mandate;
    }

    public void setMandate(SibsPayMandate mandate) {
        this.mandate = mandate;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayReturnCheckout returnCheckout = (SibsPayReturnCheckout) o;
        return Objects.equals(this.returnStatus, returnCheckout.returnStatus)
                && Objects.equals(this.transactionID, returnCheckout.transactionID)
                && Objects.equals(this.transactionSignature, returnCheckout.transactionSignature)
                && Objects.equals(this.amount, returnCheckout.amount) && Objects.equals(this.merchant, returnCheckout.merchant)
                && Objects.equals(this.paymentMethodList, returnCheckout.paymentMethodList)
                && Objects.equals(this.tokenList, returnCheckout.tokenList)
                && Objects.equals(this.formContext, returnCheckout.formContext)
                && Objects.equals(this.execution, returnCheckout.execution)
                && Objects.equals(this.mandate, returnCheckout.mandate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(returnStatus, transactionID, transactionSignature, amount, merchant, paymentMethodList, tokenList,
                formContext, execution, mandate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ReturnCheckout {\n");

        sb.append("    returnStatus: ").append(toIndentedString(returnStatus)).append("\n");
        sb.append("    transactionID: ").append(toIndentedString(transactionID)).append("\n");
        sb.append("    transactionSignature: ").append(toIndentedString(transactionSignature)).append("\n");
        sb.append("    amount: ").append(toIndentedString(amount)).append("\n");
        sb.append("    merchant: ").append(toIndentedString(merchant)).append("\n");
        sb.append("    paymentMethodList: ").append(toIndentedString(paymentMethodList)).append("\n");
        sb.append("    tokenList: ").append(toIndentedString(tokenList)).append("\n");
        sb.append("    formContext: ").append(toIndentedString(formContext)).append("\n");
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

    public String getRequestLog() {
        return requestLog;
    }

    public void setRequestLog(String requestLog) {
        this.requestLog = requestLog;
    }

    public String getResponseLog() {
        return responseLog;
    }

    public void setResponseLog(String responseLog) {
        this.responseLog = responseLog;
    }
}
