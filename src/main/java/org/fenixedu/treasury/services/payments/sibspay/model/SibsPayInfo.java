package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.List;

/**
 * Object that defines the transaction additional information
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayInfo {
    @JsonProperty("deviceInfo")
    private SibsPayDeviceinfo deviceInfo = null;

    @JsonProperty("customerInfo")
    private List<SibsPayExtendedInfo> customerInfo = null;

    public SibsPayInfo deviceInfo(SibsPayDeviceinfo deviceInfo) {
        this.deviceInfo = deviceInfo;
        return this;
    }

    /**
     * Get deviceInfo
     * 
     * @return deviceInfo
     **/

    public SibsPayDeviceinfo getDeviceInfo() {
        return deviceInfo;
    }

    public void setDeviceInfo(SibsPayDeviceinfo deviceInfo) {
        this.deviceInfo = deviceInfo;
    }

    public SibsPayInfo customerInfo(List<SibsPayExtendedInfo> customerInfo) {
        this.customerInfo = customerInfo;
        return this;
    }

    public SibsPayInfo addCustomerInfoItem(SibsPayExtendedInfo customerInfoItem) {
        if (this.customerInfo == null) {
            this.customerInfo = new ArrayList<SibsPayExtendedInfo>();
        }
        this.customerInfo.add(customerInfoItem);
        return this;
    }

    /**
     * Key Value tuple array.
     * 
     * @return customerInfo
     **/

    public List<SibsPayExtendedInfo> getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(List<SibsPayExtendedInfo> customerInfo) {
        this.customerInfo = customerInfo;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayInfo info = (SibsPayInfo) o;
        return Objects.equals(this.deviceInfo, info.deviceInfo) && Objects.equals(this.customerInfo, info.customerInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceInfo, customerInfo);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Info {\n");

        sb.append("    deviceInfo: ").append(toIndentedString(deviceInfo)).append("\n");
        sb.append("    customerInfo: ").append(toIndentedString(customerInfo)).append("\n");
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
