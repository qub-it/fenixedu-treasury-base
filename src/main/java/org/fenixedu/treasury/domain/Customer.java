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
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.dto.AdhocCustomerBean;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.fenixedu.treasury.util.FiscalCodeValidation;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public abstract class Customer extends Customer_Base {

    public static final String DEFAULT_FISCAL_NUMBER = "999999990";
    public static final int MAX_CODE_LENGHT = 20;

    public static final Comparator<Customer> COMPARE_BY_NAME_IGNORE_CASE = new Comparator<Customer>() {

        @Override
        public int compare(final Customer o1, final Customer o2) {
            int c = o1.getName().compareToIgnoreCase(o2.getName());
            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    protected Customer() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public abstract String getCode();

    public abstract String getFiscalNumber();

    public abstract String getName();

    public abstract String getFirstNames();

    public abstract String getLastNames();

    public abstract String getIdentificationNumber();

    public abstract String getNationalityCountryCode();

    public abstract String getFiscalCountry();

    public abstract String getPaymentReferenceBaseCode();

    public abstract String getBusinessIdentification();

    public abstract String getEmail();

    public abstract String getPhoneNumber();

    public abstract BigDecimal getGlobalBalance();

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
            throw new TreasuryDomainException("error.Customer.code.maxlenght");
        }
        
        if(Strings.isNullOrEmpty(getFiscalNumber().trim())) {
            throw new TreasuryDomainException("error.Customer.fiscalNumber.required");
        }

        if(Strings.isNullOrEmpty(super.getAddressCountryCode())) {
            throw new TreasuryDomainException("error.Customer.addressCountryCode.required");
        }

        if(getCustomerType() == null) {
            throw new TreasuryDomainException("error.Customer.customerType.required");
        }
        
        if (!TreasuryConstants.isDefaultCountry(getFiscalCountry()) || !DEFAULT_FISCAL_NUMBER.equals(getFiscalNumber())) {
            final Set<Customer> customers = findByFiscalInformation(getFiscalCountry(), getFiscalNumber())
                    .filter(c -> c.isActive()).collect(Collectors.<Customer> toSet());
            
            if (customers.size() > 1) {
                final Customer self = this;
                final Set<String> otherCustomers = customers.stream().filter(c -> c != self).map(c -> c.getName()).collect(Collectors.<String> toSet());

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

        return findAll().filter(c -> !Strings.isNullOrEmpty(c.getFiscalCountry())
                && lowerCase(c.getFiscalCountry()).equals(lowerCase(fiscalCountryCode))
                && !Strings.isNullOrEmpty(c.getFiscalNumber()) 
                && c.getFiscalNumber().equals(fiscalNumber));
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
                || getBusinessIdentification() != null && getBusinessIdentification().contains(searchFieldClear);
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
        if(!Strings.isNullOrEmpty(getErpCustomerId())) {
            return false;
        }
        
        if(isWithFinantialDocumentsIntegratedInERP()) {
            return false;
        }
        
        if(isFiscalValidated() && isFiscalCodeValid()) {
            return false;
        }
        
        return true;
    }

    public boolean isWithFinantialDocumentsIntegratedInERP() {
        boolean checkedInAllFinantialInstitutions = true;
        
        for (DebtAccount debtAccount : getDebtAccountsSet()) {
            final FinantialInstitution institution = debtAccount.getFinantialInstitution();

            if(institution.getErpIntegrationConfiguration() == null) {
                checkedInAllFinantialInstitutions = false;
                break;
            }
            
            if(Strings.isNullOrEmpty(institution.getErpIntegrationConfiguration().getImplementationClassName())) {
                checkedInAllFinantialInstitutions = false;
                break;
            }
            
            final IERPExternalService erpService = institution.getErpIntegrationConfiguration().getERPExternalServiceImplementation();
            
            if(erpService == null) {
                checkedInAllFinantialInstitutions = false;
                break;
            }
            
            if(erpService.getERPExporter().isCustomerWithFinantialDocumentsIntegratedInERP(this)) {
                return true;
            }
        }
        
        return !checkedInAllFinantialInstitutions;
    }

    public boolean isCustomerInformationMaybeIntegratedWithSuccess() {
        boolean checkedInAllFinantialInstitutions = true;

        for (DebtAccount debtAccount : getDebtAccountsSet()) {
            final FinantialInstitution institution = debtAccount.getFinantialInstitution();

            if(institution.getErpIntegrationConfiguration() == null) {
                checkedInAllFinantialInstitutions = false;
                break;
            }
            
            if(Strings.isNullOrEmpty(institution.getErpIntegrationConfiguration().getImplementationClassName())) {
                checkedInAllFinantialInstitutions = false;
                break;
            }
            
            final IERPExternalService erpService = institution.getErpIntegrationConfiguration().getERPExternalServiceImplementation();
            
            if(erpService == null) {
                checkedInAllFinantialInstitutions = false;
                break;
            }
            
            if(erpService.getERPExporter().isCustomerMaybeIntegratedWithSuccess(this)) {
                return true;
            }
        }
        
        return !checkedInAllFinantialInstitutions;
    }
    
    public boolean isCustomerWithFinantialDocumentsIntegratedInPreviousERP() {
        boolean checkedInAllFinantialInstitutions = true;
        
        for (DebtAccount debtAccount : getDebtAccountsSet()) {
            final FinantialInstitution institution = debtAccount.getFinantialInstitution();

            if(institution.getErpIntegrationConfiguration() == null) {
                checkedInAllFinantialInstitutions = false;
                continue;
            }
            
            if(Strings.isNullOrEmpty(institution.getErpIntegrationConfiguration().getImplementationClassName())) {
                checkedInAllFinantialInstitutions = false;
                break;
            }
            
            final IERPExternalService erpService = institution.getErpIntegrationConfiguration().getERPExternalServiceImplementation();
            
            if(erpService == null) {
                checkedInAllFinantialInstitutions = false;
                break;
            }
            
            if(erpService.getERPExporter().isCustomerWithFinantialDocumentsIntegratedInPreviousERP(this)) {
                throw new TreasuryDomainException("error.Customer.changeFiscalNumber.documents.integrated.in.previous.erp");
            }
        }
        
        return !checkedInAllFinantialInstitutions;
    }
    
    public String getUiCompleteAddress() {
        final List<String> addressCompoundList = Lists.newArrayList();
        
        if(!Strings.isNullOrEmpty(getAddress())) {
            addressCompoundList.add(getAddress());
        }
        
        if(!Strings.isNullOrEmpty(getZipCode())) {
            addressCompoundList.add(getZipCode());
        }
        
        if(!Strings.isNullOrEmpty(getDistrictSubdivision())) {
            addressCompoundList.add(getDistrictSubdivision());
        }
        
        if(!Strings.isNullOrEmpty(getRegion())) {
            addressCompoundList.add(getRegion());
        }
        
        if(!Strings.isNullOrEmpty(getAddressCountryCode())) {
            addressCompoundList.add(getAddressCountryCode());
        }
        
        return String.join(", ", addressCompoundList);
    }
    
    public abstract Set<? extends TreasuryEvent> getTreasuryEventsSet();

    public abstract boolean isUiOtherRelatedCustomerActive();

    public abstract String uiRedirectToActiveCustomer(final String url);

    public String getUiFiscalNumber() {
        final String fiscalCountry = !Strings.isNullOrEmpty(getFiscalCountry()) ? getFiscalCountry() : "";
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

}
