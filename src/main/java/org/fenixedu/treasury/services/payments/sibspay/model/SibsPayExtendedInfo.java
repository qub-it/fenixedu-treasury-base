package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Key value tuple.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayExtendedInfo {
    @JsonProperty("key")
    private String key = null;

    @JsonProperty("value")
    private String value = null;

    public SibsPayExtendedInfo key(String key) {
        this.key = key;
        return this;
    }

    /**
     * Get key
     * 
     * @return key
     **/

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public SibsPayExtendedInfo value(String value) {
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
        SibsPayExtendedInfo extendedInfo = (SibsPayExtendedInfo) o;
        return Objects.equals(this.key, extendedInfo.key) && Objects.equals(this.value, extendedInfo.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class ExtendedInfo {\n");

        sb.append("    key: ").append(toIndentedString(key)).append("\n");
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
