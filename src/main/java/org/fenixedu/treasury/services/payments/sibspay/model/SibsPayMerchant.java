package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Object that defines a Merchant.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayMerchant {
    @JsonProperty("terminalId")
    private Integer terminalId = null;

    @JsonProperty("channel")
    private String channel = null;

    @JsonProperty("merchantTransactionId")
    private String merchantTransactionId = null;

    public SibsPayMerchant terminalId(Integer terminalId) {
        this.terminalId = terminalId;
        return this;
    }

    /**
     * Number of the merchant pos Id.
     * 
     * @return terminalId
     **/
    public Integer getTerminalId() {
        return terminalId;
    }

    public void setTerminalId(Integer terminalId) {
        this.terminalId = terminalId;
    }

    public SibsPayMerchant channel(String channel) {
        this.channel = channel;
        return this;
    }

    /**
     * Type of channel used by the merchant.
     * 
     * @return channel
     **/

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public SibsPayMerchant merchantTransactionId(String merchantTransactionId) {
        this.merchantTransactionId = merchantTransactionId;
        return this;
    }

    /**
     * Unique id used by the merchant.
     * 
     * @return merchantTransactionId
     **/

    public String getMerchantTransactionId() {
        return merchantTransactionId;
    }

    public void setMerchantTransactionId(String merchantTransactionId) {
        this.merchantTransactionId = merchantTransactionId;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayMerchant merchant = (SibsPayMerchant) o;
        return Objects.equals(this.terminalId, merchant.terminalId) && Objects.equals(this.channel, merchant.channel)
                && Objects.equals(this.merchantTransactionId, merchant.merchantTransactionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(terminalId, channel, merchantTransactionId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Merchant {\n");

        sb.append("    terminalId: ").append(toIndentedString(terminalId)).append("\n");
        sb.append("    channel: ").append(toIndentedString(channel)).append("\n");
        sb.append("    merchantTransactionId: ").append(toIndentedString(merchantTransactionId)).append("\n");
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
