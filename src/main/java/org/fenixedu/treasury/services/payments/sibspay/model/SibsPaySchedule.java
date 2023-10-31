package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Schedule
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPaySchedule {
    @JsonProperty("initialDate")
    private DateTime initialDate = null;

    @JsonProperty("finalDate")
    private DateTime finalDate = null;

    /**
     * Gets or Sets interval
     */
    public enum IntervalEnum {
        DAILY("DAILY"),

        WEEKLY("WEEKLY"),

        BIWEEKLY("BIWEEKLY"),

        MONTHLY("MONTHLY"),

        QUARTERLY("QUARTERLY"),

        SEMIANNUAL("SEMIANNUAL"),

        ANNUAL("ANNUAL");

        private String value;

        IntervalEnum(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }

        @JsonCreator
        public static IntervalEnum fromValue(String text) {
            for (IntervalEnum b : IntervalEnum.values()) {
                if (String.valueOf(b.value).equals(text)) {
                    return b;
                }
            }
            return null;
        }
    }

    @JsonProperty("interval")
    private IntervalEnum interval = IntervalEnum.DAILY;

    public SibsPaySchedule initialDate(DateTime initialDate) {
        this.initialDate = initialDate;
        return this;
    }

    /**
     * Get initialDate
     * 
     * @return initialDate
     **/

    public DateTime getInitialDate() {
        return initialDate;
    }

    public void setInitialDate(DateTime initialDate) {
        this.initialDate = initialDate;
    }

    public SibsPaySchedule finalDate(DateTime finalDate) {
        this.finalDate = finalDate;
        return this;
    }

    /**
     * Get finalDate
     * 
     * @return finalDate
     **/

    public DateTime getFinalDate() {
        return finalDate;
    }

    public void setFinalDate(DateTime finalDate) {
        this.finalDate = finalDate;
    }

    public SibsPaySchedule interval(IntervalEnum interval) {
        this.interval = interval;
        return this;
    }

    /**
     * Get interval
     * 
     * @return interval
     **/

    public IntervalEnum getInterval() {
        return interval;
    }

    public void setInterval(IntervalEnum interval) {
        this.interval = interval;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPaySchedule schedule = (SibsPaySchedule) o;
        return Objects.equals(this.initialDate, schedule.initialDate) && Objects.equals(this.finalDate, schedule.finalDate)
                && Objects.equals(this.interval, schedule.interval);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initialDate, finalDate, interval);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Schedule {\n");

        sb.append("    initialDate: ").append(toIndentedString(initialDate)).append("\n");
        sb.append("    finalDate: ").append(toIndentedString(finalDate)).append("\n");
        sb.append("    interval: ").append(toIndentedString(interval)).append("\n");
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
