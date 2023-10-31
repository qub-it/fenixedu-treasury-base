package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that defines the status of the processed transaction.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayReturnStatus {
    @JsonProperty("statusCode")
    private String statusCode = null;

    @JsonProperty("statusMsg")
    private String statusMsg = null;

    @JsonProperty("statusDescription")
    private String statusDescription = null;

    @JsonProperty("execution")
    private SibsPayExecution execution = null;

    @JsonProperty("recurringTransaction")
    private SibsPayRecurringTransactionOutput recurringTransaction = null;

    public SibsPayReturnStatus statusCode(String statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    /**
     * Identifier code of the processed transaction status.
     * 
     * @return statusCode
     **/

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public SibsPayReturnStatus statusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
        return this;
    }

    /**
     * Message of the processed transaction status.
     * 
     * @return statusMsg
     **/

    public String getStatusMsg() {
        return statusMsg;
    }

    public void setStatusMsg(String statusMsg) {
        this.statusMsg = statusMsg;
    }

    public SibsPayReturnStatus statusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
        return this;
    }

    /**
     * Description of the processed transaction status.
     * 
     * @return statusDescription
     **/

    public String getStatusDescription() {
        return statusDescription;
    }

    public void setStatusDescription(String statusDescription) {
        this.statusDescription = statusDescription;
    }

    public SibsPayReturnStatus execution(SibsPayExecution execution) {
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

    public SibsPayReturnStatus recurringTransaction(SibsPayRecurringTransactionOutput recurringTransaction) {
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

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayReturnStatus returnStatus = (SibsPayReturnStatus) o;
        return Objects.equals(this.statusCode, returnStatus.statusCode) && Objects.equals(this.statusMsg, returnStatus.statusMsg)
                && Objects.equals(this.statusDescription, returnStatus.statusDescription)
                && Objects.equals(this.execution, returnStatus.execution)
                && Objects.equals(this.recurringTransaction, returnStatus.recurringTransaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusCode, statusMsg, statusDescription, execution, recurringTransaction);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ReturnStatus {\n");

        sb.append("    statusCode: ").append(toIndentedString(statusCode)).append("\n");
        sb.append("    statusMsg: ").append(toIndentedString(statusMsg)).append("\n");
        sb.append("    statusDescription: ").append(toIndentedString(statusDescription)).append("\n");
        sb.append("    execution: ").append(toIndentedString(execution)).append("\n");
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
