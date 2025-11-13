package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that defines the tokenization field
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayTokenisation {
    @JsonProperty("tokenisationRequest")
    private SibsPayTokenisationRequest tokenisationRequest = null;

    @JsonProperty("paymentTokens")
    private List<SibsPayPaymentTokenItem> paymentTokens = null;

    public SibsPayTokenisation tokenisationRequest(SibsPayTokenisationRequest tokenisationRequest) {
        this.tokenisationRequest = tokenisationRequest;
        return this;
    }

    /**
     * Get tokenisationRequest
     * 
     * @return tokenisationRequest
     **/

    public SibsPayTokenisationRequest getTokenisationRequest() {
        return tokenisationRequest;
    }

    public void setTokenisationRequest(SibsPayTokenisationRequest tokenisationRequest) {
        this.tokenisationRequest = tokenisationRequest;
    }

    public SibsPayTokenisation paymentTokens(List<SibsPayPaymentTokenItem> paymentTokens) {
        this.paymentTokens = paymentTokens;
        return this;
    }

    public SibsPayTokenisation addPaymentTokensItem(SibsPayPaymentTokenItem paymentTokensItem) {
        if (this.paymentTokens == null) {
            this.paymentTokens = new ArrayList<SibsPayPaymentTokenItem>();
        }
        this.paymentTokens.add(paymentTokensItem);
        return this;
    }

    /**
     * Get paymentTokens
     * 
     * @return paymentTokens
     **/
    public List<SibsPayPaymentTokenItem> getPaymentTokens() {
        return paymentTokens;
    }

    public void setPaymentTokens(List<SibsPayPaymentTokenItem> paymentTokens) {
        this.paymentTokens = paymentTokens;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayTokenisation tokenisation = (SibsPayTokenisation) o;
        return Objects.equals(this.tokenisationRequest, tokenisation.tokenisationRequest)
                && Objects.equals(this.paymentTokens, tokenisation.paymentTokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tokenisationRequest, paymentTokens);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Tokenisation {\n");

        sb.append("    tokenisationRequest: ").append(toIndentedString(tokenisationRequest)).append("\n");
        sb.append("    paymentTokens: ").append(toIndentedString(paymentTokens)).append("\n");
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
