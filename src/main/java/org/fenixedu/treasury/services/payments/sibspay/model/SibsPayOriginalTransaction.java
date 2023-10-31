package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * OriginalTransaction
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayOriginalTransaction {
    @JsonProperty("id")
    private String id = null;

    @JsonProperty("datetime")
    private String datetime = null;

    @JsonProperty("recipientId")
    private String recipientId = null;

    public SibsPayOriginalTransaction id(String id) {
        this.id = id;
        return this;
    }

    /**
     * Get id
     * 
     * @return id
     **/

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SibsPayOriginalTransaction datetime(String datetime) {
        this.datetime = datetime;
        return this;
    }

    /**
     * Get datetime
     * 
     * @return datetime
     **/

    public String getDatetime() {
        return datetime;
    }

    public void setDatetime(String datetime) {
        this.datetime = datetime;
    }

    public SibsPayOriginalTransaction recipientId(String recipientId) {
        this.recipientId = recipientId;
        return this;
    }

    /**
     * Get recipientId
     * 
     * @return recipientId
     **/

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayOriginalTransaction originalTransaction = (SibsPayOriginalTransaction) o;
        return Objects.equals(this.id, originalTransaction.id) && Objects.equals(this.datetime, originalTransaction.datetime)
                && Objects.equals(this.recipientId, originalTransaction.recipientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, datetime, recipientId);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class OriginalTransaction {\n");

        sb.append("    id: ").append(toIndentedString(id)).append("\n");
        sb.append("    datetime: ").append(toIndentedString(datetime)).append("\n");
        sb.append("    recipientId: ").append(toIndentedString(recipientId)).append("\n");
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
