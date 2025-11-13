package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token value tuple.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayPaymentTokenItem {
    @JsonProperty("tokenType")
    private String tokenType = null;

    @JsonProperty("value")
    private String value = null;

    public SibsPayPaymentTokenItem tokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }

    /**
     * Get tokenType
     * 
     * @return tokenType
     **/

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public SibsPayPaymentTokenItem value(String value) {
        this.value = value;
        return this;
    }

    /**
     * Get value
     * 
     * @return value
     **/

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayPaymentTokenItem paymentTokenItem = (SibsPayPaymentTokenItem) o;
        return Objects.equals(this.tokenType, paymentTokenItem.tokenType) && Objects.equals(this.value, paymentTokenItem.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenType, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class PaymentTokenItem {\n");

        sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
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
