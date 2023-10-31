package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.ArrayList;
import java.util.List;

/**
 * Object that defines a customer.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

public class SibsPayCustomer {
    @JsonProperty("customerInfo")
    private SibsPayCustomerInfo customerInfo = null;

    @JsonProperty("extendedInfo")
    private List<SibsPayExtendedInfo> extendedInfo = null;

    public SibsPayCustomer customerInfo(SibsPayCustomerInfo customerInfo) {
        this.customerInfo = customerInfo;
        return this;
    }

    /**
     * Get customerInfo
     * 
     * @return customerInfo
     **/
    public SibsPayCustomerInfo getCustomerInfo() {
        return customerInfo;
    }

    public void setCustomerInfo(SibsPayCustomerInfo customerInfo) {
        this.customerInfo = customerInfo;
    }

    public SibsPayCustomer extendedInfo(List<SibsPayExtendedInfo> extendedInfo) {
        this.extendedInfo = extendedInfo;
        return this;
    }

    public SibsPayCustomer addExtendedInfoItem(SibsPayExtendedInfo extendedInfoItem) {
        if (this.extendedInfo == null) {
            this.extendedInfo = new ArrayList<SibsPayExtendedInfo>();
        }
        this.extendedInfo.add(extendedInfoItem);
        return this;
    }

    /**
     * Key Value tuple array.
     * 
     * @return extendedInfo
     **/
    public List<SibsPayExtendedInfo> getExtendedInfo() {
        return extendedInfo;
    }

    public void setExtendedInfo(List<SibsPayExtendedInfo> extendedInfo) {
        this.extendedInfo = extendedInfo;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayCustomer customer = (SibsPayCustomer) o;
        return Objects.equals(this.customerInfo, customer.customerInfo)
                && Objects.equals(this.extendedInfo, customer.extendedInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerInfo, extendedInfo);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Customer {\n");

        sb.append("    customerInfo: ").append(toIndentedString(customerInfo)).append("\n");
        sb.append("    extendedInfo: ").append(toIndentedString(extendedInfo)).append("\n");
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
