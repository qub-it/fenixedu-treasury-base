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

import static com.google.common.base.Strings.isNullOrEmpty;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.ERPExportOperation;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.fenixedu.treasury.util.FiscalCodeValidation;
import org.fenixedu.treasury.util.TreasuryConstants;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public abstract class Customer extends Customer_Base {

    public static final String DEFAULT_FISCAL_NUMBER = "999999990";
    public static final int MAX_CODE_LENGHT = 20;

    public static final int MAX_NAME_LENGTH = 100;

    public static final Comparator<Customer> COMPARE_BY_NAME_IGNORE_CASE = (o1, o2) -> {
        int c = o1.getName().compareToIgnoreCase(o2.getName());
        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    protected Customer() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCode(getExternalId());
    }

    public abstract String getFiscalNumber();

    public abstract String getName();

    public abstract String getFirstNames();

    public abstract String getLastNames();

    public abstract String getIdentificationNumber();

    public abstract String getNationalityCountryCode();

    public abstract String getPaymentReferenceBaseCode();

    public abstract String getBusinessIdentification();

    public abstract String getEmail();

    public abstract String getPhoneNumber();

    public abstract BigDecimal getGlobalBalance();

    public abstract String getUsername();

    public abstract Set<Customer> getAllCustomers();

    public boolean isDeletable() {
        return false;
    }

    public boolean isPersonCustomer() {
        return false;
    }

    public boolean isAdhocCustomer() {
        return false;
    }

    public abstract boolean isActive();

    public abstract Customer getActiveCustomer();

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.Customer.cannot.delete");
        }

        setCustomerType(null);
        setDomainRoot(null);

        deleteDomainObject();
    }

    public void checkRules() {
        if (Strings.isNullOrEmpty(getCode())) {
            throw new TreasuryDomainException("error.Customer.code.required");
        }

        if (Strings.isNullOrEmpty(getName())) {
            throw new TreasuryDomainException("error.Customer.name.required");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new TreasuryDomainException("error.Customer.code.duplicated");
        }

        if (this.getCode().length() > Customer.MAX_CODE_LENGHT) {
            throw new TreasuryDomainException("error.Customer.code.maxlength");
        }

        if (getName().length() > Customer.MAX_NAME_LENGTH) {
            throw new TreasuryDomainException("error.Customer.name.maxlength");
        }

        if (Strings.isNullOrEmpty(getFiscalNumber().trim())) {
            throw new TreasuryDomainException("error.Customer.fiscalNumber.required");
        }

        if (Strings.isNullOrEmpty(super.getAddressCountryCode())) {
            throw new TreasuryDomainException("error.Customer.addressCountryCode.required");
        }

        if (getCustomerType() == null) {
            throw new TreasuryDomainException("error.Customer.customerType.required");
        }

        if (!TreasuryConstants.isDefaultCountry(getAddressCountryCode()) || !DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber())) {
            final Set<Customer> customers = findByFiscalInformation(getAddressCountryCode(), getFiscalNumber())
                    .filter(c -> c.isActive()).collect(Collectors.<Customer> toSet());

            if (customers.size() > 1) {
                final Customer self = this;
                final Set<String> otherCustomers =
                        customers.stream().filter(c -> c != self).map(c -> c.getName()).collect(Collectors.<String> toSet());

                throw new TreasuryDomainException("error.Customer.customer.with.fiscal.information.exists",
                        Joiner.on(", ").join(otherCustomers));
            }
        }

    }

    public String getShortName() {
        return TreasuryConstants.firstAndLastWords(getName());
    }

    public static Stream<? extends Customer> findAll() {
        return FenixFramework.getDomainRoot().getCustomersSet().stream();
    }

    public static Stream<? extends Customer> find(final FinantialInstitution institution) {
        return institution.getDebtAccountsSet().stream().map(debtAccount -> debtAccount.getCustomer());
    }

    public static Stream<? extends Customer> findByCode(final java.lang.String code) {
        return findAll().filter(i -> code.equalsIgnoreCase(i.getCode()));
    }

    public static Stream<? extends Customer> findByFiscalInformation(final String fiscalCountryCode, final String fiscalNumber) {
        if (Strings.isNullOrEmpty(fiscalCountryCode)) {
            throw new TreasuryDomainException("error.Customer.findByFiscalCountryAndNumber.fiscalCountryCode.required");
        }

        if (Strings.isNullOrEmpty(fiscalNumber)) {
            throw new TreasuryDomainException("error.Customer.findByFiscalCountryAndNumber.fiscalNumber.required");
        }

        return findAll().filter(c -> !Strings.isNullOrEmpty(c.getAddressCountryCode())
                && lowerCase(c.getAddressCountryCode()).equals(lowerCase(fiscalCountryCode))
                && !Strings.isNullOrEmpty(c.getFiscalNumber()) && c.getFiscalNumber().equals(fiscalNumber));
    }

    public boolean matchesMultiFilter(String searchText) {
        if (searchText == null) {
            return false;
        }

        //Use the # to filter for Business Identification (Student, Candidacy, professor, etc...)
        if (searchText.startsWith("#") && searchText.length() > 1) {
            String codeToSearch = searchText.replace("#", "");
            return getBusinessIdentification() != null && getBusinessIdentification().equals(codeToSearch);
        }

        final String searchFieldClear =
                Normalizer.normalize(searchText.toLowerCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");

        final String nameClear =
                Normalizer.normalize(getName().toLowerCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");

        return TreasuryConstants.matchNames(nameClear, searchFieldClear)
                || getIdentificationNumber() != null && getIdentificationNumber().contains(searchFieldClear)
                || getFiscalNumber() != null && getFiscalNumber().toLowerCase().contains(searchFieldClear)
                || getCode() != null && getCode().contains(searchFieldClear)
                || getBusinessIdentification() != null && getBusinessIdentification().contains(searchFieldClear)
                || getUsername() != null && getUsername().contains(searchFieldClear);
    }

    public Set<FinantialInstitution> getFinantialInstitutions() {
        return getDebtAccountsSet().stream().map(x -> x.getFinantialInstitution()).collect(Collectors.toSet());
    }

    public DebtAccount getDebtAccountFor(FinantialInstitution institution) {
        return getDebtAccountsSet().stream().filter(x -> x.getFinantialInstitution().equals(institution)).findFirst()
                .orElse(null);
    }

    @Atomic
    public void registerFinantialInstitutions(List<FinantialInstitution> newFinantialInstitutions) {

        Set<FinantialInstitution> actualInstitutions = Sets.newHashSet(getFinantialInstitutions());

        for (FinantialInstitution newInst : newFinantialInstitutions) {
            if (actualInstitutions.contains(newInst)) {
                this.getDebtAccountFor(newInst).reopenDebtAccount();
            } else {
                DebtAccount.create(newInst, this);
            }
        }

        for (FinantialInstitution actualInst : actualInstitutions) {
            if (newFinantialInstitutions.contains(actualInst)) {
            } else {
                DebtAccount account = getDebtAccountFor(actualInst);
                account.closeDebtAccount();
            }
        }
    }

    public boolean isFiscalCodeValid() {
        return FiscalCodeValidation.isValidFiscalNumber(getAddressCountryCode(), getFiscalNumber());
    }

    public boolean isFiscalValidated() {
        return FiscalCodeValidation.isValidationAppliedToFiscalCountry(getAddressCountryCode());
    }

    public boolean isAbleToChangeFiscalNumber() {
        if (!Strings.isNullOrEmpty(getErpCustomerId())) {
            return false;
        }

        if (isWithFinantialDocumentsCertified()) {
            return false;
        }

        if (isWithFinantialDocumentsIntegratedInERP()) {
            return false;
        }

        if (isFiscalValidated() && isFiscalCodeValid()) {
            return false;
        }

        return true;
    }

    protected boolean isWithFinantialDocumentsCertified() {
        for (final DebtAccount debtAccount : getDebtAccountsSet()) {

            for (final FinantialDocument finantialDocument : debtAccount.getFinantialDocumentsSet()) {
                if (TreasuryPlataformDependentServicesFactory.implementation().hasCertifiedDocument(finantialDocument)) {
                    return true;
                }
            }

            /* Payor debt account associated invoices */
            for (Invoice invoice : debtAccount.getInvoiceSet()) {
                if (TreasuryPlataformDependentServicesFactory.implementation().hasCertifiedDocument(invoice)) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean isWithFinantialDocumentsIntegratedInERP() {
        return isCustomerWithFinantialDocumentsIntegratedInERP(this);
    }

    private boolean isCustomerWithFinantialDocumentsIntegratedInERP(Customer customer) {
        for (final DebtAccount debtAccount : customer.getDebtAccountsSet()) {

            for (final FinantialDocument finantialDocument : debtAccount.getFinantialDocumentsSet()) {

                if (!Strings.isNullOrEmpty(finantialDocument.getErpCertificateDocumentReference())) {
                    return true;
                }

                for (final ERPExportOperation erpExportOperation : finantialDocument.getErpExportOperationsSet()) {
                    if (erpExportOperation.getSuccess()) {
                        return true;
                    }
                }
            }

            /* Payor debt account associated invoices */
            for (Invoice invoice : debtAccount.getInvoiceSet()) {

                if (!Strings.isNullOrEmpty(invoice.getErpCertificateDocumentReference())) {
                    return true;
                }

                for (final ERPExportOperation erpExportOperation : invoice.getErpExportOperationsSet()) {
                    if (erpExportOperation.getSuccess()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean isCustomerWithFinantialDocumentsIntegratedInPreviousERP() {
        boolean checkedInAllFinantialInstitutions = true;

        for (DebtAccount debtAccount : getDebtAccountsSet()) {
            final FinantialInstitution institution = debtAccount.getFinantialInstitution();

            if (institution.getErpIntegrationConfiguration() == null) {
                checkedInAllFinantialInstitutions = false;
                continue;
            }

            if (Strings.isNullOrEmpty(institution.getErpIntegrationConfiguration().getImplementationClassName())) {
                checkedInAllFinantialInstitutions = false;
                break;
            }

            final IERPExternalService erpService =
                    institution.getErpIntegrationConfiguration().getERPExternalServiceImplementation();

            if (erpService == null) {
                checkedInAllFinantialInstitutions = false;
                break;
            }

            if (erpService.getERPExporter().isCustomerWithFinantialDocumentsIntegratedInPreviousERP(this)) {
                throw new TreasuryDomainException("error.Customer.changeFiscalNumber.documents.integrated.in.previous.erp");
            }
        }

        return !checkedInAllFinantialInstitutions;
    }

    public String getUiCompleteAddress() {
        final List<String> addressCompoundList = Lists.newArrayList();

        if (!Strings.isNullOrEmpty(getAddress())) {
            addressCompoundList.add(getAddress());
        }

        if (!Strings.isNullOrEmpty(getZipCode())) {
            addressCompoundList.add(getZipCode());
        }

        if (!Strings.isNullOrEmpty(getDistrictSubdivision())) {
            addressCompoundList.add(getDistrictSubdivision());
        }

        if (!Strings.isNullOrEmpty(getRegion())) {
            addressCompoundList.add(getRegion());
        }

        if (!Strings.isNullOrEmpty(getAddressCountryCode())) {
            addressCompoundList.add(getAddressCountryCode());
        }

        return String.join(", ", addressCompoundList);
    }

    public abstract Set<? extends TreasuryEvent> getTreasuryEventsSet();

    public abstract boolean isUiOtherRelatedCustomerActive();

    public abstract String uiRedirectToActiveCustomer(final String url);

    public String getUiFiscalNumber() {
        final String fiscalCountry = !Strings.isNullOrEmpty(getAddressCountryCode()) ? getAddressCountryCode() : "";
        final String fiscalNumber = !Strings.isNullOrEmpty(getFiscalNumber()) ? getFiscalNumber() : "";

        return (fiscalCountry + " " + fiscalNumber).trim();
    }

    public abstract LocalizedString getIdentificationTypeDesignation();

    public abstract String getIdentificationTypeCode();

    public abstract String getIban();

    public boolean isIbanDefined() {
        return !isNullOrEmpty(getIban());
    }

    // @formatter:off
    /* ****************************
     * BEGIN OF SAFT ADDRESS FIELDS
     * ****************************
     */
    // @formatter:on

    public String getSaftBillingAddressCountry() {
        return getAddressCountryCode();
    }

    public String getSaftBillingAddressStreetName() {
        return getAddress();
    }

    public String getSaftBillingAddressDetail() {
        return getAddress();
    }

    public String getSaftBillingAddressCity() {
        return getDistrictSubdivision();
    }

    public String getSaftBillingAddressPostalCode() {
        return getZipCode();
    }

    public String getSaftBillingAddressRegion() {
        return getRegion();
    }

    // @formatter:off
    /* **************************
     * END OF SAFT ADDRESS FIELDS
     * **************************
     */
    // @formatter:on

    protected static String lowerCase(final String value) {
        if (value == null) {
            return null;
        }

        return value.toLowerCase();
    }

    public int nextFinantialDocumentNumber() {
        int number = 0;
        if (getFinantialDocumentCounter() != null) {
            number = getFinantialDocumentCounter();
        }
        number += 1;
        setFinantialDocumentCounter(number);
        return number;
    }

    public int nextFinantialDocumentEntryNumber() {
        int number = 0;
        if (getFinantialDocumentEntryCounter() != null) {
            number = getFinantialDocumentEntryCounter();
        }
        number += 1;
        setFinantialDocumentEntryCounter(number);
        return number;
    }
}
