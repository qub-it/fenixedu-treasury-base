package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Object that defines the predefined customer information.
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")

@JsonIgnoreProperties(ignoreUnknown = true)
public class SibsPayCustomerInfo {
    @JsonProperty("customerName")
    private String customerName = null;

    @JsonProperty("customerEmail")
    private String customerEmail = null;
    
    @JsonProperty("shippingAddress")
    private SibsPayAddress shippingAddress = null;

    @JsonProperty("billingAddress")
    private SibsPayAddress billingAddress = null;

    @JsonProperty("billingAddressSameAsShippingAddress")
    private Boolean billingAddressSameAsShippingAddress = null;

    public SibsPayCustomerInfo customerName(String customerName) {
        this.customerName = customerName;
        return this;
    }

    /**
     * Name of the customer.
     * 
     * @return customerName
     **/

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }
    
    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public SibsPayCustomerInfo shippingAddress(SibsPayAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
        return this;
    }

    /**
     * Get shippingAddress
     * 
     * @return shippingAddress
     **/

    public SibsPayAddress getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(SibsPayAddress shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public SibsPayCustomerInfo billingAddress(SibsPayAddress billingAddress) {
        this.billingAddress = billingAddress;
        return this;
    }

    /**
     * Get billingAddress
     * 
     * @return billingAddress
     **/

    public SibsPayAddress getBillingAddress() {
        return billingAddress;
    }

    public void setBillingAddress(SibsPayAddress billingAddress) {
        this.billingAddress = billingAddress;
    }

    public SibsPayCustomerInfo billingAddressSameAsShippingAddress(Boolean billingAddressSameAsShippingAddress) {
        this.billingAddressSameAsShippingAddress = billingAddressSameAsShippingAddress;
        return this;
    }

    /**
     * Flag that identifies that the billing address is the same as the shipping address
     * 
     * @return billingAddressSameAsShippingAddress
     **/

    public Boolean isBillingAddressSameAsShippingAddress() {
        return billingAddressSameAsShippingAddress;
    }

    public void setBillingAddressSameAsShippingAddress(Boolean billingAddressSameAsShippingAddress) {
        this.billingAddressSameAsShippingAddress = billingAddressSameAsShippingAddress;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayCustomerInfo customerInfo = (SibsPayCustomerInfo) o;
        return Objects.equals(this.customerName, customerInfo.customerName)
                && Objects.equals(this.shippingAddress, customerInfo.shippingAddress)
                && Objects.equals(this.billingAddress, customerInfo.billingAddress)
                && Objects.equals(this.billingAddressSameAsShippingAddress, customerInfo.billingAddressSameAsShippingAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerName, shippingAddress, billingAddress, billingAddressSameAsShippingAddress);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class CustomerInfo {\n");

        sb.append("    customerName: ").append(toIndentedString(customerName)).append("\n");
        sb.append("    shippingAddress: ").append(toIndentedString(shippingAddress)).append("\n");
        sb.append("    billingAddress: ").append(toIndentedString(billingAddress)).append("\n");
        sb.append("    billingAddressSameAsShippingAddress: ").append(toIndentedString(billingAddressSameAsShippingAddress))
                .append("\n");
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
