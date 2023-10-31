package org.fenixedu.treasury.services.payments.sibspay.model;

import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.HashMap;
import java.util.Map;

/**
 * Address
 */
@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen",
        date = "2023-08-30T20:20:13.375686+01:00[Europe/Lisbon]")
public class SibsPayAddress {

    @JsonProperty("street1")
    private String street1 = null;

    @JsonProperty("street2")
    private String street2 = null;

    @JsonProperty("city")
    private String city = null;

    @JsonProperty("postcode")
    private String postcode = null;

    @JsonProperty("country")
    private String country = null;

    public SibsPayAddress street1(String street1) {
        this.street1 = street1;
        return this;
    }

    /**
     * Get street1
     * 
     * @return street1
     **/

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public SibsPayAddress street2(String street2) {
        this.street2 = street2;
        return this;
    }

    /**
     * Get street2
     * 
     * @return street2
     **/

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public SibsPayAddress city(String city) {
        this.city = city;
        return this;
    }

    /**
     * Get city
     * 
     * @return city
     **/

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public SibsPayAddress postcode(String postcode) {
        this.postcode = postcode;
        return this;
    }

    /**
     * Get postcode
     * 
     * @return postcode
     **/

    public String getPostcode() {
        return postcode;
    }

    public void setPostcode(String postcode) {
        this.postcode = postcode;
    }

    public SibsPayAddress country(String country) {
        this.country = country;
        return this;
    }

    /**
     * Get country
     * 
     * @return country
     **/

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    @Override
    public boolean equals(java.lang.Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SibsPayAddress address = (SibsPayAddress) o;
        return Objects.equals(this.street1, address.street1) && Objects.equals(this.street2, address.street2)
                && Objects.equals(this.city, address.city) && Objects.equals(this.postcode, address.postcode)
                && Objects.equals(this.country, address.country) && super.equals(o);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street1, street2, city, postcode, country, super.hashCode());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class Address {\n");
        sb.append("    ").append(toIndentedString(super.toString())).append("\n");
        sb.append("    street1: ").append(toIndentedString(street1)).append("\n");
        sb.append("    street2: ").append(toIndentedString(street2)).append("\n");
        sb.append("    city: ").append(toIndentedString(city)).append("\n");
        sb.append("    postcode: ").append(toIndentedString(postcode)).append("\n");
        sb.append("    country: ").append(toIndentedString(country)).append("\n");
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
