package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object that encapsulates technical execution information.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayExecution {
    @JsonProperty("startTime")
    private DateTime startTime = null;

    @JsonProperty("endTime")
    private DateTime endTime = null;

    public SibsPayExecution startTime(DateTime startTime) {
        this.startTime = startTime;
        return this;
    }

    /**
     * Time of the acceptance of the request by the API.
     * 
     * @return startTime
     **/

    public DateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(DateTime startTime) {
        this.startTime = startTime;
    }

    public SibsPayExecution endTime(DateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    /**
     * Time of the response to the request by the API.
     * 
     * @return endTime
     **/

    public DateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(DateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayExecution execution = (SibsPayExecution) o;
        return Objects.equals(this.startTime, execution.startTime) && Objects.equals(this.endTime, execution.endTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Execution {\n");

        sb.append("    startTime: ").append(toIndentedString(startTime)).append("\n");
        sb.append("    endTime: ").append(toIndentedString(endTime)).append("\n");
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
