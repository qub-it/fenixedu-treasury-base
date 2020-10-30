package org.fenixedu.treasury.domain.sibsonlinepaymentsgateway;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.commons.i18n.LocalizedString;

public class SibsBillingAddressBean {

    private String addressCountryCode;
    private LocalizedString addressCountryName;
    private String city;
    private String address;
    private String zipCode;
    
    public SibsBillingAddressBean() {
    }

    public String getUiFiscalPresentationValue() {
        final List<String> compounds = new ArrayList<>();

        if(StringUtils.isNotEmpty(getAddress())) {
            compounds.add(getAddress());
        }

        if(StringUtils.isNotEmpty(getZipCode())) {
            compounds.add(getZipCode());
        }

        if(StringUtils.isNotEmpty(getCity())) {
            compounds.add(getCity());
        }

        if(getAddressCountryName() != null) {
            compounds.add(getAddressCountryName().getContent());
        }

        return String.join(" ", compounds);
    }
    
    // *****************
    // GETTERS & SETTERS
    // *****************

    public String getAddressCountryCode() {
        return addressCountryCode;
    }

    public void setAddressCountryCode(String addressCountryCode) {
        this.addressCountryCode = addressCountryCode;
    }
    
    public LocalizedString getAddressCountryName() {
        return addressCountryName;
    }
    
    public void setAddressCountryName(LocalizedString addressCountryName) {
        this.addressCountryName = addressCountryName;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }
    
}
