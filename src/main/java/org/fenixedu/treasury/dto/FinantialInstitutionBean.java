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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.FiscalCountryRegion;

import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.District;
import pt.ist.standards.geographic.TreasuryGeographicInfoLoader;
import pt.ist.standards.geographic.Municipality;

public class FinantialInstitutionBean implements ITreasuryBean, Serializable {

    private static final long serialVersionUID = 1L;

    private String fiscalNumber;

    private Country country;

    private District district;

    private Municipality municipality;

    private FiscalCountryRegion fiscalcountryregion;

    private List<TreasuryTupleDataSourceBean> countries;

    private List<TreasuryTupleDataSourceBean> districts;

    private List<TreasuryTupleDataSourceBean> municipalities;

    private List<TreasuryTupleDataSourceBean> fiscalcountryregions;
    private List<TreasuryTupleDataSourceBean> currenciesDataSource;

    private Currency currency;

    private String code;
    private String companyId;
    private String name;
    private String companyName;
    private String address;
    private String email;
    private String telephone;
    private String webAddress;
    private String locality;
    private String zipCode;

    public FinantialInstitutionBean() {
        this.updateModelLists();
        this.setCurrenciesDataSource(Currency.findAll().collect(Collectors.toList()));
        this.setFiscalcountryregions(FiscalCountryRegion.findAll().collect(Collectors.toList()));
    }

    public FinantialInstitutionBean(FinantialInstitution finantialInstitution) {
        this.code = finantialInstitution.getCode();
        this.address = finantialInstitution.getAddress();
        this.companyId = finantialInstitution.getCompanyId();
        this.companyName = finantialInstitution.getCompanyName();
        this.country = finantialInstitution.getCountry();
        this.district = finantialInstitution.getDistrict();
        this.fiscalcountryregion = finantialInstitution.getFiscalCountryRegion();
        this.fiscalNumber = finantialInstitution.getFiscalNumber();
        this.locality = finantialInstitution.getLocality();
        this.municipality = finantialInstitution.getMunicipality();
        this.name = finantialInstitution.getName();
        this.zipCode = finantialInstitution.getZipCode();
        this.setCurrency(finantialInstitution.getCurrency());
        this.setEmail(finantialInstitution.getEmail());
        this.setWebAddress(finantialInstitution.getWebAddress());
        this.setTelephone(finantialInstitution.getTelephoneContact());
        this.setCurrenciesDataSource(Currency.findAll().collect(Collectors.toList()));
        this.setFiscalcountryregions(FiscalCountryRegion.findAll().collect(Collectors.toList()));
        this.updateModelLists();
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public District getDistrict() {
        return district;
    }

    public void setDistrict(District district) {
        this.district = district;
    }

    public Municipality getMunicipality() {
        return municipality;
    }

    public void setMunicipality(Municipality municipality) {
        this.municipality = municipality;
    }

    public List<TreasuryTupleDataSourceBean> getCountries() {
        return countries;
    }

    public void setCountries(List<Country> countries) {
        this.countries = countries.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();

            tuple.setId(x.exportAsString());
            tuple.setText(x.getLocalizedName(I18N.getLocale()));
            return tuple;
        }).collect(Collectors.toList());
    }

    public List<TreasuryTupleDataSourceBean> getDistricts() {
        return districts;
    }

    public void setDistricts(List<District> districts) {
        this.districts = districts.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();

            tuple.setId(x.exportAsString());
            tuple.setText(x.getLocalizedName(I18N.getLocale()));
            return tuple;
        }).collect(Collectors.toList());
    }

    public List<TreasuryTupleDataSourceBean> getMunicipalities() {
        return municipalities;
    }

    public void setMunicipalities(List<Municipality> municipalities) {
        this.municipalities = municipalities.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();

            tuple.setId(x.exportAsString());
            tuple.setText(x.getLocalizedName(I18N.getLocale()));
            return tuple;
        }).collect(Collectors.toList());
    }

    public String getFiscalNumber() {
        return fiscalNumber;
    }

    public void setFiscalNumber(String fiscalNumber) {
        this.fiscalNumber = fiscalNumber;
    }

    public List<TreasuryTupleDataSourceBean> getFiscalcountryregions() {
        return fiscalcountryregions;
    }

    public void setFiscalcountryregions(List<FiscalCountryRegion> fiscalcountryregions) {
        this.fiscalcountryregions = fiscalcountryregions.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();

            tuple.setId(x.getExternalId());
            tuple.setText(x.getName().getContent());
            return tuple;
        }).collect(Collectors.toList());
    }

    public FiscalCountryRegion getFiscalcountryregion() {
        return fiscalcountryregion;
    }

    public void setFiscalcountryregion(FiscalCountryRegion fiscalcountryregion) {
        this.fiscalcountryregion = fiscalcountryregion;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCompanyId() {
        return companyId;
    }

    public void setCompanyId(String companyId) {
        this.companyId = companyId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getLocality() {
        return locality;
    }

    public void setLocality(String locality) {
        this.locality = locality;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public void updateModelLists() {

        this.setCountries(TreasuryGeographicInfoLoader.getInstance().findAllCountries().sorted((s1, s2) -> {
            return s1.getLocalizedName(I18N.getLocale()).compareTo(s2.getLocalizedName(I18N.getLocale()));
        }).collect(Collectors.toList()));

        if (this.getCountry() != null) {
            this.setDistricts(this.getCountry().getPlaces().stream().sorted((s1, s2) -> {
                return s1.getLocalizedName(I18N.getLocale()).compareTo(s2.getLocalizedName(I18N.getLocale()));
            }).collect(Collectors.toList()));
            this.setMunicipalities(new ArrayList<Municipality>());
        }
        if (this.getDistrict() != null) {
            this.setMunicipalities(this.getDistrict().getPlaces().stream().sorted((s1, s2) -> {
                return s1.getLocalizedName(I18N.getLocale()).compareTo(s2.getLocalizedName(I18N.getLocale()));
            }).collect(Collectors.toList()));
        }

    }

    public List<TreasuryTupleDataSourceBean> getCurrenciesDataSource() {
        return currenciesDataSource;
    }

    public void setCurrenciesDataSource(List<Currency> currencies) {
        this.currenciesDataSource = currencies.stream().map(x -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setText(x.getIsoCode() + " - " + x.getSymbol());
            tuple.setId(x.getExternalId());
            return tuple;
        }).collect(Collectors.toList());
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getWebAddress() {
        return webAddress;
    }

    public void setWebAddress(String webAddress) {
        this.webAddress = webAddress;
    }

}
