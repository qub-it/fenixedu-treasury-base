package org.fenixedu.treasury.services.payments.sibspay.model;

import java.math.BigDecimal;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that defines an amount.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayAmount {

    @JsonProperty("value")
    private BigDecimal value = null;

    @JsonProperty("currency")
    private String currency = null;

    public SibsPayAmount value(BigDecimal value) {
        this.value = value;
        return this;
    }

    /**
     * Get value
     * 
     * @return value
     **/

    public BigDecimal getValue() {
        return value;
    }

    public void setValue(BigDecimal value) {
        this.value = value;
    }

    public SibsPayAmount currency(String currency) {
        this.currency = currency;
        return this;
    }

    /**
     * Currency used in the transaction.
     * 
     * @return currency
     **/
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayAmount amount = (SibsPayAmount) o;
        return Objects.equals(this.value, amount.value) && Objects.equals(this.currency, amount.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, currency);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Amount {\n");

        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("    currency: ").append(toIndentedString(currency)).append("\n");
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
