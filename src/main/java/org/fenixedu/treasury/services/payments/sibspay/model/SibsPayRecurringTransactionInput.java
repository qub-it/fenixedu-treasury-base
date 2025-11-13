package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that defines a recurring transaction request.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayRecurringTransactionInput {
    @JsonProperty("validityDate")
    private DateTime validityDate = null;

    @JsonProperty("amountQualifier")
    private String amountQualifier = null;

    @JsonProperty("schedule")
    private SibsPaySchedule schedule = null;

    public SibsPayRecurringTransactionInput validityDate(DateTime validityDate) {
        this.validityDate = validityDate;
        return this;
    }

    /**
     * Date of the validity of the recurring transaction.
     * 
     * @return validityDate
     **/

    public DateTime getValidityDate() {
        return validityDate;
    }

    public void setValidityDate(DateTime validityDate) {
        this.validityDate = validityDate;
    }

    public SibsPayRecurringTransactionInput amountQualifier(String amountQualifier) {
        this.amountQualifier = amountQualifier;
        return this;
    }

    /**
     * Qualifier of the recurring transaction amount.
     * 
     * @return amountQualifier
     **/

    public String getAmountQualifier() {
        return amountQualifier;
    }

    public void setAmountQualifier(String amountQualifier) {
        this.amountQualifier = amountQualifier;
    }

    public SibsPayRecurringTransactionInput schedule(SibsPaySchedule schedule) {
        this.schedule = schedule;
        return this;
    }

    /**
     * Get schedule
     * 
     * @return schedule
     **/

    public SibsPaySchedule getSchedule() {
        return schedule;
    }

    public void setSchedule(SibsPaySchedule schedule) {
        this.schedule = schedule;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayRecurringTransactionInput recurringTransactionInput = (SibsPayRecurringTransactionInput) o;
        return Objects.equals(this.validityDate, recurringTransactionInput.validityDate)
                && Objects.equals(this.amountQualifier, recurringTransactionInput.amountQualifier)
                && Objects.equals(this.schedule, recurringTransactionInput.schedule);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validityDate, amountQualifier, schedule);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class RecurringTransactionInput {\n");

        sb.append("    validityDate: ").append(toIndentedString(validityDate)).append("\n");
        sb.append("    amountQualifier: ").append(toIndentedString(amountQualifier)).append("\n");
        sb.append("    schedule: ").append(toIndentedString(schedule)).append("\n");
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
