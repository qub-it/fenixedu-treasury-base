package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Token
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayToken {
    @JsonProperty("tokenName")
    private String tokenName = null;

    @JsonProperty("tokenType")
    private String tokenType = null;

    @JsonProperty("value")
    private String value = null;

    @JsonProperty("maskedPAN")
    private String maskedPAN = null;

    @JsonProperty("expireDate")
    private String expireDate = null;

    public SibsPayToken tokenName(String tokenName) {
        this.tokenName = tokenName;
        return this;
    }

    /**
     * Get tokenName
     * 
     * @return tokenName
     **/

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public SibsPayToken tokenType(String tokenType) {
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

    public SibsPayToken value(String value) {
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

    public SibsPayToken maskedPAN(String maskedPAN) {
        this.maskedPAN = maskedPAN;
        return this;
    }

    /**
     * Get maskedPAN
     * 
     * @return maskedPAN
     **/

    public String getMaskedPAN() {
        return maskedPAN;
    }

    public void setMaskedPAN(String maskedPAN) {
        this.maskedPAN = maskedPAN;
    }

    public SibsPayToken expireDate(String expireDate) {
        this.expireDate = expireDate;
        return this;
    }

    /**
     * Get expireDate
     * 
     * @return expireDate
     **/

    public String getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(String expireDate) {
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
        SibsPayToken token = (SibsPayToken) o;
        return Objects.equals(this.tokenName, token.tokenName) && Objects.equals(this.tokenType, token.tokenType)
                && Objects.equals(this.value, token.value) && Objects.equals(this.maskedPAN, token.maskedPAN)
                && Objects.equals(this.expireDate, token.expireDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenName, tokenType, value, maskedPAN, expireDate);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Token {\n");

        sb.append("    tokenName: ").append(toIndentedString(tokenName)).append("\n");
        sb.append("    tokenType: ").append(toIndentedString(tokenType)).append("\n");
        sb.append("    value: ").append(toIndentedString(value)).append("\n");
        sb.append("    maskedPAN: ").append(toIndentedString(maskedPAN)).append("\n");
        sb.append("    expireDate: ").append(toIndentedString(expireDate)).append("\n");
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
