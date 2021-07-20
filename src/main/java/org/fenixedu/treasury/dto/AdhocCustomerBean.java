/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.dto;

import static org.fenixedu.treasury.util.TreasuryConstants.isDefaultCountry;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.util.TreasuryConstants;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.Planet;

public class AdhocCustomerBean implements ITreasuryBean {

    private CustomerType customerType;
    private String code;
    private String fiscalNumber;
    private String identificationNumber;
    private String name;
    private String address;
    private String districtSubdivision;
    private String region;
    private String zipCode;
    private String addressCountryCode;
    private List<FinantialInstitution> finantialInstitutions;

    private List<TreasuryTupleDataSourceBean> finantialInstitutionsDataSource;
    private List<TreasuryTupleDataSourceBean> customerTypesDataSource;
    private List<TreasuryTupleDataSourceBean> countryCodesDataSource;
    
    private boolean changeFiscalNumberConfirmed;
    private boolean addressCountryDefault;

    public AdhocCustomerBean() {
        this.setFinantialInstitutionsDataSource(FinantialInstitution.findAll().collect(Collectors.toList()));
        this.setCustomerTypesDataSource(CustomerType.findAll().collect(Collectors.toList()));
        this.setCountryCodesDataSource(Lists.newArrayList(Planet.getEarth().getPlaces()));
        this.update();
    }

    public AdhocCustomerBean(Customer customer) {
        this();
        this.setCustomerType(customer.getCustomerType());
        this.code = customer.getCode();
        this.setFiscalNumber(customer.getFiscalNumber());
        this.setIdentificationNumber(customer.getIdentificationNumber());
        this.setName(customer.getName());
        this.setAddress(customer.getAddress());
        this.setDistrictSubdivision(customer.getDistrictSubdivision());
        this.setRegion(customer.getRegion());
        this.setZipCode(customer.getZipCode());
        this.setAddressCountryCode(customer.getAddressCountryCode());
        this.setFinantialInstitutions(customer.getDebtAccountsSet().stream().filter(x -> x.getClosed() == false)
                .map(x -> x.getFinantialInstitution()).collect(Collectors.toList()));
        
        this.update();
    }

    public boolean isAddressValid() {
        boolean valid = true;
        
        valid &= !Strings.isNullOrEmpty(this.getAddressCountryCode());
        valid &= !Strings.isNullOrEmpty(this.getAddress());
        valid &= !Strings.isNullOrEmpty(this.getDistrictSubdivision());
        
        if(isDefaultCountry(this.getAddressCountryCode())) {
            valid &= !Strings.isNullOrEmpty(this.getZipCode());
            valid &= !Strings.isNullOrEmpty(this.getRegion());
        }
        
        if(isDefaultCountry(this.getAddressCountryCode()) && !Strings.isNullOrEmpty(this.getZipCode())) {
            valid &= this.getZipCode().matches("\\d{4}-\\d{3}");
        }
        
        return valid;
    }
    
    public String getCode() {
        return code;
    }

    public java.lang.String getFiscalNumber() {
        return fiscalNumber;
    }

    public void setFiscalNumber(java.lang.String value) {
        fiscalNumber = value;
    }

    public java.lang.String getIdentificationNumber() {
        return identificationNumber;
    }

    public void setIdentificationNumber(java.lang.String value) {
        identificationNumber = value;
    }

    public java.lang.String getName() {
        return name;
    }

    public void setName(java.lang.String value) {
        name = value;
    }

    public java.lang.String getAddress() {
        return address;
    }

    public void setAddress(java.lang.String value) {
        address = value;
    }

    public java.lang.String getDistrictSubdivision() {
        return districtSubdivision;
    }

    public void setDistrictSubdivision(java.lang.String value) {
        districtSubdivision = value;
    }
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }

    public java.lang.String getZipCode() {
        return zipCode;
    }

    public void setZipCode(java.lang.String value) {
        zipCode = value;
    }
    
    public String getAddressCountryCode() {
        return addressCountryCode;
    }
    
    public void setAddressCountryCode(String addressCountryCode) {
        this.addressCountryCode = addressCountryCode;
    }

    public CustomerType getCustomerType() {
        return customerType;
    }

    public void setCustomerType(CustomerType customerType) {
        this.customerType = customerType;
    }
    
    public boolean isChangeFiscalNumberConfirmed() {
        return changeFiscalNumberConfirmed;
    }
    
    public void setChangeFiscalNumberConfirmed(final boolean value) {
        changeFiscalNumberConfirmed = value;
    }

    public void update() {
        this.addressCountryDefault = TreasuryConstants.DEFAULT_COUNTRY.toUpperCase().equals(getAddressCountryCode());
    }
    
    public boolean isAddressCountryDefault() {
        return this.addressCountryDefault;
    }

    public List<FinantialInstitution> getFinantialInstitutions() {
        return finantialInstitutions;
    }

    public void setFinantialInstitutions(List<FinantialInstitution> finantialInstitutions) {
        this.finantialInstitutions = finantialInstitutions;
    }

    public List<TreasuryTupleDataSourceBean> getFinantialInstitutionsDataSource() {
        return finantialInstitutionsDataSource;
    }

    public void setFinantialInstitutionsDataSource(List<FinantialInstitution> finantialInstitutionsDataSource) {
        this.finantialInstitutionsDataSource = finantialInstitutionsDataSource.stream().map(x -> {
            TreasuryTupleDataSourceBean inst = new TreasuryTupleDataSourceBean();
            inst.setId(x.getExternalId());
            inst.setText(x.getName());
            return inst;
        }).collect(Collectors.toList());
    }

    public List<TreasuryTupleDataSourceBean> getCustomerTypesDataSource() {
        return customerTypesDataSource;
    }

    public void setCustomerTypesDataSource(List<CustomerType> customerTypesDataSource) {
        this.customerTypesDataSource = customerTypesDataSource.stream().map(customerType -> {
            TreasuryTupleDataSourceBean customerTypeDataSource = new TreasuryTupleDataSourceBean();
            customerTypeDataSource.setId(customerType.getExternalId());
            customerTypeDataSource.setText(customerType.getName().getContent());
            return customerTypeDataSource;
        }).collect(Collectors.toList());
    }
    
    public List<TreasuryTupleDataSourceBean> getCountryCodesDataSource() {
        return countryCodesDataSource;
    }
    
    public void setCountryCodesDataSource(final List<Country> countries) {
        this.countryCodesDataSource = countries.stream().map(c -> new TreasuryTupleDataSourceBean(c.alpha2, c.getLocalizedName(I18N.getLocale()))).collect(Collectors.toList());
        
        Collections.sort(this.countryCodesDataSource, TreasuryTupleDataSourceBean.COMPARE_BY_TEXT);
    }
}
