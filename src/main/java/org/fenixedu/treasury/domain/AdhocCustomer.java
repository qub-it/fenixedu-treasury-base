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
package org.fenixedu.treasury.domain;

import static org.fenixedu.treasury.util.FiscalCodeValidation.isValidFiscalNumber;
import static org.fenixedu.treasury.util.TreasuryConstants.isDefaultCountry;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.dto.AdhocCustomerBean;
import org.fenixedu.treasury.util.FiscalCodeValidation;
import org.fenixedu.treasury.util.TreasuryConstants;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class AdhocCustomer extends AdhocCustomer_Base {

    private static final int SAFT_CUSTOMER_COMPANY_NAME_MAX_LENGTH = 100;

    protected AdhocCustomer() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
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
            final String districtSubdivision, final String region, final String zipCode, final String addressCountryCode,
            final String identificationNumber, final List<FinantialInstitution> finantialInstitutions) {
        this();
        setCustomerType(customerType);
        setFiscalNumber(fiscalNumber);
        setName(name);
        setAddress(address);
        setRegion(region);
        setDistrictSubdivision(districtSubdivision);
        setZipCode(zipCode);

        setAddressCountryCode(addressCountryCode.toUpperCase());

        setIdentificationNumber(identificationNumber);

        if (TreasuryConstants.isDefaultCountry(getAddressCountryCode())
                && !FiscalCodeValidation.isValidFiscalNumber(getAddressCountryCode(), getFiscalNumber())) {
            throw new TreasuryDomainException("error.Customer.fiscal.information.invalid");
        }

        registerFinantialInstitutions(finantialInstitutions);

        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();

        if (getName().length() > SAFT_CUSTOMER_COMPANY_NAME_MAX_LENGTH) {
            throw new TreasuryDomainException("error.AdhocCustomer.name.exceeds.max.length",
                    String.valueOf(SAFT_CUSTOMER_COMPANY_NAME_MAX_LENGTH));
        }

        // ANIL 2024-05-21
        //
        // Validate only of the customer is active
        if (isActive()) {
            if (!TreasuryConstants.isDefaultCountry(getAddressCountryCode())
                    || !DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber())) {
                final Set<Customer> customers = findByFiscalInformation(getAddressCountryCode(), getFiscalNumber()) //
                        .filter(c -> c.isAdhocCustomer()) //
                        .filter(c -> c.isActive()) //
                        .collect(Collectors.<Customer> toSet());

                if (customers.size() > 1) {
                    final Customer self = this;
                    final Set<String> otherCustomers =
                            customers.stream().filter(c -> c != self).map(c -> c.getName()).collect(Collectors.<String> toSet());

                    throw new TreasuryDomainException("error.Customer.customer.with.fiscal.information.exists",
                            Joiner.on(", ").join(otherCustomers));
                }
            }
        }

    }

    @Override
    public String getPaymentReferenceBaseCode() {
        return this.getCode();
    }

    @Atomic
    public void edit(final CustomerType customerType, final String name, final String address, final String districtSubdivision,
            final String region, final String zipCode, final String identificationNumber,
            final List<FinantialInstitution> newFinantialInstitutions) {
        registerFinantialInstitutions(newFinantialInstitutions);

        setCustomerType(customerType);
        setName(name);
        setAddress(address);
        setDistrictSubdivision(districtSubdivision);
        setRegion(region);
        setZipCode(zipCode);
        setIdentificationNumber(identificationNumber);

        checkRules();
    }

    @Atomic
    public void changeFiscalNumber(AdhocCustomerBean bean) {
        if (!Strings.isNullOrEmpty(getErpCustomerId())) {
            throw new TreasuryDomainException("warning.Customer.changeFiscalNumber.maybe.integrated.in.erp");
        }

        final String oldFiscalCountry = getAddressCountryCode();
        final String oldFiscalNumber = getFiscalNumber();

        final boolean changeFiscalNumberConfirmed = bean.isChangeFiscalNumberConfirmed();
        final boolean withFinantialDocumentsIntegratedInERP = isWithFinantialDocumentsIntegratedInERP();

        // 2023-02-12 ANIL: The platform no longer check if there are logs with ERP client
        // This was removed because it hinders the tests in quality or development servers, due to
        // lack of log files
        //
        // Checking if there is an operation with success should be enough
        final boolean customerInformationMaybeIntegratedWithSuccess = false;
        final boolean customerWithFinantialDocumentsIntegratedInPreviousERP =
                isCustomerWithFinantialDocumentsIntegratedInPreviousERP();

        if (!bean.isChangeFiscalNumberConfirmed()) {
            throw new TreasuryDomainException("message.Customer.changeFiscalNumber.confirmation");
        }

        final String addressCountryCode = bean.getAddressCountryCode();
        final String fiscalNumber = bean.getFiscalNumber();

        if (Strings.isNullOrEmpty(addressCountryCode)) {
            throw new TreasuryDomainException("error.Customer.countryCode.required");
        }

        if (Strings.isNullOrEmpty(fiscalNumber)) {
            throw new TreasuryDomainException("error.Customer.fiscalNumber.required");
        }

        // Check if fiscal information is different from current information
        if (lowerCase(addressCountryCode).equals(lowerCase(getAddressCountryCode())) && fiscalNumber.equals(getFiscalNumber())) {
            throw new TreasuryDomainException("error.Customer.already.with.fiscal.information");
        }

        if (isFiscalValidated() && isFiscalCodeValid()) {
            throw new TreasuryDomainException("error.Customer.changeFiscalNumber.already.valid");
        }

        if (customerInformationMaybeIntegratedWithSuccess) {
            throw new TreasuryDomainException("warning.Customer.changeFiscalNumber.maybe.integrated.in.erp");
        }

        if (withFinantialDocumentsIntegratedInERP) {
            throw new TreasuryDomainException("error.Customer.changeFiscalNumber.documents.integrated.erp");
        }

        if (!FiscalCodeValidation.isValidFiscalNumber(addressCountryCode, fiscalNumber)) {
            throw new TreasuryDomainException("error.Customer.fiscal.information.invalid");
        }

        setAddressCountryCode(addressCountryCode);
        setFiscalNumber(fiscalNumber);

        setAddress(bean.getAddress());
        setDistrictSubdivision(bean.getDistrictSubdivision());
        setRegion(bean.getRegion());
        setZipCode(bean.getZipCode());

        checkRules();

        FiscalDataUpdateLog.create(this, oldFiscalCountry, oldFiscalNumber, changeFiscalNumberConfirmed,
                withFinantialDocumentsIntegratedInERP, customerInformationMaybeIntegratedWithSuccess,
                customerWithFinantialDocumentsIntegratedInPreviousERP);
    }

    @Override
    public boolean isFiscalCodeValid() {
        return !TreasuryConstants.isDefaultCountry(getAddressCountryCode())
                || isValidFiscalNumber(getAddressCountryCode(), getFiscalNumber());
    }

    @Override
    public boolean isFiscalValidated() {
        return TreasuryConstants.isDefaultCountry(getAddressCountryCode());
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
    public String getNationalityCountryCode() {
        return null;
    }

    @Override
    public String getEmail() {
        return null;
    }

    @Override
    public String getPhoneNumber() {
        return null;
    }

    @Override
    public BigDecimal getGlobalBalance() {
        BigDecimal globalBalance = BigDecimal.ZERO;
        for (final DebtAccount debtAccount : getDebtAccountsSet()) {
            globalBalance = globalBalance.add(debtAccount.getTotalInDebt());
        }

        return globalBalance;
    }

    @Override
    public String getUsername() {
        return null;
    }

    @Override
    public Set<Customer> getAllCustomers() {
        return Sets.newHashSet(this);
    }

    @Override
    public LocalizedString getIdentificationTypeDesignation() {
        return null;
    }

    @Override
    public String getIdentificationTypeCode() {
        return null;
    }

    @Override
    public String getIban() {
        return null;
    }

    public boolean isAddressValid() {
        boolean valid = true;

        valid &= !Strings.isNullOrEmpty(this.getAddressCountryCode());
        valid &= !Strings.isNullOrEmpty(this.getAddress());
        valid &= !Strings.isNullOrEmpty(this.getDistrictSubdivision());

        if (isDefaultCountry(this.getAddressCountryCode())) {
            valid &= !Strings.isNullOrEmpty(this.getZipCode());
            valid &= !Strings.isNullOrEmpty(this.getRegion());
        }

        if (isDefaultCountry(this.getAddressCountryCode()) && !Strings.isNullOrEmpty(this.getZipCode())) {
            valid &= this.getZipCode().matches("\\d{4}-\\d{3}");
        }

        return valid;
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
            final String address, final String districtSubdivision, final String region, final String zipCode,
            final String addressCountryCode, final String identificationNumber,
            final List<FinantialInstitution> finantialInstitutions) {
        return new AdhocCustomer(customerType, fiscalNumber, name, address, districtSubdivision, region, zipCode,
                addressCountryCode, identificationNumber, finantialInstitutions);
    }

    // ANIL 2024-03-11: Bypass checkrules in order to import customers with duplicated fiscal number
    public static AdhocCustomer createForImportation(String code, CustomerType customerType, String fiscalNumber, String name,
            String address, String districtSubdivision, String region, String zipCode, String addressCountryCode,
            String identificationNumber, List<FinantialInstitution> finantialInstitutions) {
        AdhocCustomer result = new AdhocCustomer();

        result.setCode(code);
        result.setCustomerType(customerType);
        result.setFiscalNumber(fiscalNumber);
        result.setName(name);
        result.setAddress(address);
        result.setRegion(region);
        result.setDistrictSubdivision(districtSubdivision);
        result.setZipCode(zipCode);

        result.setAddressCountryCode(addressCountryCode.toUpperCase());

        result.setIdentificationNumber(identificationNumber);

        result.registerFinantialInstitutions(finantialInstitutions);

        return result;
    }

    public static Stream<AdhocCustomer> findAll() {
        return FenixFramework.getDomainRoot().getCustomersSet().stream().filter(x -> x instanceof AdhocCustomer)
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
        return findAll().filter(i -> countryCode.equalsIgnoreCase(i.getAddressCountryCode()));
    }

    public static Stream<AdhocCustomer> findByCode(final String code) {
        return findAll().filter(i -> code.equalsIgnoreCase(i.getCode()));
    }

}
