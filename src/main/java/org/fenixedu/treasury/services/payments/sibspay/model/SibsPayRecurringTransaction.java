package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that defines the recurring transaction
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayRecurringTransaction {
    @JsonProperty("validityDate")
    private String validityDate = null;

    public SibsPayRecurringTransaction validityDate(String validityDate) {
        this.validityDate = validityDate;
        return this;
    }

    /**
     * Get validityDate
     * 
     * @return validityDate
     **/

    public String getValidityDate() {
        return validityDate;
    }

    public void setValidityDate(String validityDate) {
        this.validityDate = validityDate;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayRecurringTransaction recurringTransaction = (SibsPayRecurringTransaction) o;
        return Objects.equals(this.validityDate, recurringTransaction.validityDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(validityDate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class RecurringTransaction {\n");

        sb.append("    validityDate: ").append(toIndentedString(validityDate)).append("\n");
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
