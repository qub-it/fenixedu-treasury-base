/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
 * 
 *
 * 
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.treasury.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.FiscalCodeValidation;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class AdhocCustomer extends AdhocCustomer_Base {

    protected AdhocCustomer() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    @Override
    public boolean isAdhocCustomer() {
        return true;
    }

    @Override
    public boolean isActive() {
        return getDebtAccountsSet().stream().filter(d -> !d.getClosed()).count() > 0;
    }

    @Override
    public Customer getActiveCustomer() {
        return this;
    }

    protected AdhocCustomer(final CustomerType customerType, final String fiscalNumber, final String name, final String address,
            final String districtSubdivision, final String zipCode, final String addressCountryCode, final String countryCode,
            final String identificationNumber) {
        this();
        setCustomerType(customerType);
        setCode(getExternalId());
        setFiscalNumber(fiscalNumber);
        setName(name);
        setAddress(address);
        setDistrictSubdivision(districtSubdivision);
        setZipCode(zipCode);
        setAddressCountryCode(addressCountryCode);
        setCountryCode(countryCode);
        setIdentificationNumber(identificationNumber);
        
        if(!FiscalCodeValidation.isValidFiscalNumber(getCountryCode(), getFiscalNumber())) {
            throw new TreasuryDomainException("error.Customer.fiscal.information.invalid");
        }
        
        checkRules();
    }

    @Override
    protected void checkRules() {
        super.checkRules();

    }

    @Override
    public String getPaymentReferenceBaseCode() {
        return this.getCode();
    }

    @Atomic
    public void edit(final CustomerType customerType, final String name, final String address, final String districtSubdivision,
            final String zipCode, final String addressCountryCode, final String identificationNumber, final List<FinantialInstitution> newFinantialInstitutions) {
        registerFinantialInstitutions(newFinantialInstitutions);
        
        setCustomerType(customerType);
        setName(name);
        setAddress(address);
        setDistrictSubdivision(districtSubdivision);
        setZipCode(zipCode);
        setAddressCountryCode(addressCountryCode);
        setIdentificationNumber(identificationNumber);

        checkRules();
    }

    @Override
    public Set<? extends TreasuryEvent> getTreasuryEventsSet() {
        return Sets.newHashSet();
    }

    @Override
    public boolean isUiOtherRelatedCustomerActive() {
        return false;
    }

    @Override
    public String uiRedirectToActiveCustomer(final String url) {
        return url + "/" + getExternalId();
    }

    @Override
    public boolean isDeletable() {
        return getDebtAccountsSet().stream().allMatch(da -> da.isDeletable());
    }

    @Override
    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.AdhocCustomer.cannot.delete");
        }

        setDomainRoot(null);
        setCustomerType(null);

        for (DebtAccount deb : getDebtAccountsSet()) {
            deb.delete();
        }

        deleteDomainObject();
    }

    @Override
    public String getFirstNames() {
        throw new RuntimeException("not supported");
    }

    @Override
    public String getLastNames() {
        throw new RuntimeException("not supported");
    }

    @Override
    public String getBusinessIdentification() {
        return this.getIdentificationNumber();
    }

    @Override
    public String getDistrict() {
        return getDistrictSubdivision();
    }

    @Override
    public String getNationalityCountryCode() {
        return null;
    }

    @Override
    public String getFiscalCountry() {
        return getCountryCode();
    }

    @Override
    public String getEmail() {
        throw new RuntimeException("not supported");
    }

    @Override
    public String getPhoneNumber() {
        throw new RuntimeException("not supported");
    }

    public BigDecimal getGlobalBalance() {
        BigDecimal globalBalance = BigDecimal.ZERO;
        for (final DebtAccount debtAccount : getDebtAccountsSet()) {
            globalBalance = globalBalance.add(debtAccount.getTotalInDebt());
        }

        return globalBalance;
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static CustomerType getDefaultCustomerType() {
        return CustomerType.findByCode("ADHOC").findFirst().orElse(null);
    }

    @Atomic
    public static AdhocCustomer create(final CustomerType customerType, final String fiscalNumber, final String name,
            final String address, final String districtSubdivision, final String zipCode, final String addressCountryCode,
            final String countryCode, final String identificationNumber) {
        return new AdhocCustomer(customerType, fiscalNumber, name, address, districtSubdivision, zipCode, addressCountryCode,
                countryCode, identificationNumber);
    }

    public static Stream<AdhocCustomer> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getCustomersSet().stream().filter(x -> x instanceof AdhocCustomer)
                .map(AdhocCustomer.class::cast);
    }
    
    public static Stream<AdhocCustomer> findByFiscalNumber(final String fiscalNumber) {
        return findAll().filter(i -> fiscalNumber.equalsIgnoreCase(i.getFiscalNumber()));
    }

    public static Stream<AdhocCustomer> findByName(final String name) {
        return findAll().filter(i -> name.equalsIgnoreCase(i.getName()));
    }

    public static Stream<AdhocCustomer> findByAddress(final String address) {
        return findAll().filter(i -> address.equalsIgnoreCase(i.getAddress()));
    }

    public static Stream<AdhocCustomer> findByDistrictSubdivision(final String districtSubdivision) {
        return findAll().filter(i -> districtSubdivision.equalsIgnoreCase(i.getDistrictSubdivision()));
    }

    public static Stream<AdhocCustomer> findByZipCode(final String zipCode) {
        return findAll().filter(i -> zipCode.equalsIgnoreCase(i.getZipCode()));
    }

    public static Stream<AdhocCustomer> findByCountryCode(final String countryCode) {
        return findAll().filter(i -> countryCode.equalsIgnoreCase(i.getCountryCode()));
    }

    public static Stream<AdhocCustomer> findByCode(final String code) {
        return findAll().filter(i -> code.equalsIgnoreCase(i.getCode()));
    }

}
