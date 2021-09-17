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
package org.fenixedu.treasury.domain.document;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.fenixedu.treasury.util.TreasuryConstants.DEFAULT_COUNTRY;
import static org.fenixedu.treasury.util.TreasuryConstants.isSameCountryCode;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.AdhocCustomer;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class ERPCustomerFieldsBean {

    public static final String MORADA_DESCONHECIDO = "Desconhecido";
    public static final String COD_POSTAL_OMISSAO = "0000-000";
    
    private static final int MAX_ADDRESS_DETAIL = 100;
    private static final int MAX_CITY = 50;
    private static final int MAX_ZIPCODE = 20;
    private static final int MAX_REGION = 50;

    private static final int MAX_FISCAL_NUM = 20;
    private static final int MAX_CONTACT = 50;
    private static final int MAX_NAME = 100;
    private static final int MAX_STREET_NAME = 90;

    private static final int MAX_TELEPHONE = 20;
    private static final int MAX_EMAIL = 60;
    
    private String customerId;

    private String customerBusinessId;
    private String customerFiscalCountry;
    private String customerNationality;
    private String customerAccountId;
    private String customerFiscalNumber;
    private String customerName;
    private String customerContact;
    private String customerStreetName;
    private String customerAddressDetail;
    private String customerCity;
    private String customerZipCode;
    private String customerRegion;
    private String customerCountry;
    
    private String customerTelephone;
    private String customerEmail;

    public ERPCustomerFieldsBean() {
    }

    private void checkRules() {

    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static ERPCustomerFieldsBean fillFromCustomer(final Customer customer) {
        final ERPCustomerFieldsBean bean = new ERPCustomerFieldsBean();

        // AccountID
        /*
         * Deve ser indicada a respectiva conta corrente do cliente no plano de
         * contas da contabilidade, caso esteja definida. Caso contr?rio dever?
         * ser preenchido com a designa??o ?Desconhecido?.
         */

        if (customer instanceof AdhocCustomer) {
            bean.setCustomerAccountId("ADHOC");
        } else {
            bean.setCustomerAccountId("STUDENT");
        }

        if (!validateFiscalNumber(customer.getFiscalCountry(), customer.getFiscalNumber())) {
            throw new TreasuryDomainException("error.ERPCustomerFieldsBean.invalid.fiscal.number");
        }

        final String fiscalCountry = customer.getFiscalCountry();
        final String fiscalNumber = customer.getFiscalNumber();
        final String name = customer.getName();
        final List<String> errorMessages = Lists.newArrayList();

        final String addressCountryCode = customer.getSaftBillingAddressCountry();
        final String street = customer.getSaftBillingAddressStreetName();
        final String address = customer.getSaftBillingAddressDetail();
        final String zipCode = customer.getSaftBillingAddressPostalCode();
        final String districtSubdivision = customer.getSaftBillingAddressCity();
        final String region = !isNullOrEmpty(customer.getSaftBillingAddressRegion()) ? customer
                .getSaftBillingAddressRegion() : districtSubdivision;

        if (!validateAddress(fiscalCountry, fiscalNumber, name, addressCountryCode, address, zipCode, districtSubdivision, region,
                errorMessages)) {
            throw new TreasuryDomainException("error.ERPCustomerFieldsBean.invalid.address");
        }

        convertAddress(bean, addressCountryCode, street, address, zipCode, districtSubdivision, region);

        // CompanyName
        if (customer.getName().length() > MAX_NAME) {
            throw new TreasuryDomainException("error.ERPCustomerFieldsBean.customerName.more.than.allowed",
                    String.valueOf(MAX_NAME));
        }

        bean.setCustomerName(customer.getName());

        // Contact
        bean.setCustomerContact(Splitter.fixedLength(MAX_CONTACT).splitToList(customer.getName()).get(0));

        bean.setCustomerId(customer.getCode());

        bean.setCustomerBusinessId(customer.getBusinessIdentification());

        if (!Strings.isNullOrEmpty(customer.getFiscalNumber()) && customer.getFiscalNumber().length() > MAX_FISCAL_NUM) {
            throw new TreasuryDomainException("error.ERPCustomerFieldsBean.fiscalNumber.more.than.allowed",
                    String.valueOf(MAX_FISCAL_NUM));
        }

        // CustomerTaxID
        bean.setCustomerFiscalNumber(customer.getFiscalNumber());
        bean.setCustomerFiscalCountry(customer.getFiscalCountry());
        bean.setCustomerNationality(customer.getNationalityCountryCode());

        if(StringUtils.isNotEmpty(customer.getPhoneNumber())) {
            bean.setCustomerTelephone(Splitter.fixedLength(MAX_TELEPHONE).splitToList(customer.getPhoneNumber()).get(0));
        }
        
        if(StringUtils.isNotEmpty(customer.getEmail())) {
            bean.setCustomerEmail(Splitter.fixedLength(MAX_EMAIL).splitToList(customer.getEmail()).get(0));
        }
        
        bean.checkRules();

        return bean;
    }

    private static boolean validateFiscalNumber(final String fiscalCountryCode, final String fiscalNumber) {
        return !Strings.isNullOrEmpty(fiscalNumber);
    }

    public static ERPCustomerFieldsBean fillFromDebitNote(final DebitNote debitNote) {
        return fillFromCustomer(debitNote.getDebtAccount().getCustomer());
    }

    public static ERPCustomerFieldsBean fillPayorFromDebitNote(final DebitNote debitNote) {
        final Customer payorCustomer = debitNote.getPayorDebtAccount().getCustomer();

        final String fiscalCountry = payorCustomer.getFiscalCountry();
        final String fiscalNumber = payorCustomer.getFiscalNumber();
        final String name = payorCustomer.getName();
        final List<String> errorMessages = Lists.newArrayList();

        final String region = !Strings.isNullOrEmpty(payorCustomer.getRegion()) ? payorCustomer.getRegion() : payorCustomer
                .getDistrictSubdivision();

        if (!validateAddress(fiscalCountry, fiscalNumber, name, payorCustomer.getAddressCountryCode(), payorCustomer.getAddress(),
                payorCustomer.getZipCode(), payorCustomer.getDistrictSubdivision(), region, errorMessages)) {
            throw new TreasuryDomainException("error.ERPCustomerFieldsBean.invalid.address");
        }

        return fillFromCustomer(payorCustomer);
    }

    public static ERPCustomerFieldsBean fillFromCreditNote(final CreditNote creditNote) {
        return fillFromCustomer(creditNote.getDebtAccount().getCustomer());
    }

    public static ERPCustomerFieldsBean fillPayorFromCreditNote(final CreditNote creditNote) {
        final Customer payorCustomer = creditNote.getPayorDebtAccount().getCustomer();

        final String fiscalCountry = payorCustomer.getFiscalCountry();
        final String fiscalNumber = payorCustomer.getFiscalNumber();
        final String name = payorCustomer.getName();
        final List<String> errorMessages = Lists.newArrayList();

        final String region = !Strings.isNullOrEmpty(payorCustomer.getRegion()) ? payorCustomer.getRegion() : payorCustomer
                .getDistrictSubdivision();

        if (!validateAddress(fiscalCountry, fiscalNumber, name, payorCustomer.getAddressCountryCode(), payorCustomer.getAddress(),
                payorCustomer.getZipCode(), payorCustomer.getDistrictSubdivision(), region, errorMessages)) {
            throw new TreasuryDomainException("error.ERPCustomerFieldsBean.invalid.address");
        }

        return fillFromCustomer(payorCustomer);
    }

    private static void convertAddress(final ERPCustomerFieldsBean bean, final String country, String street, String address,
            String zipCode, String city, String region) {
        
        street = !isNullOrEmpty(street) ? street : MORADA_DESCONHECIDO;
        address = !isNullOrEmpty(address) ? address : MORADA_DESCONHECIDO;
        zipCode = !isNullOrEmpty(zipCode) ? zipCode : null;
        city = !isNullOrEmpty(city) ? city : MORADA_DESCONHECIDO;
        region = !isNullOrEmpty(region) ? region : MORADA_DESCONHECIDO;
        
        if(Strings.isNullOrEmpty(zipCode) && isSameCountryCode(DEFAULT_COUNTRY, country)) {
            zipCode = COD_POSTAL_OMISSAO;
        }
        
        bean.setCustomerCountry(!Strings.isNullOrEmpty(country) ? country : MORADA_DESCONHECIDO);
        bean.setCustomerAddressDetail(address);
        bean.setCustomerCity(city);
        bean.setCustomerZipCode(zipCode);
        bean.setCustomerRegion(region);
        bean.setCustomerStreetName(Splitter.fixedLength(MAX_STREET_NAME).splitToList(street).get(0));
    }
    
    public static boolean validateAddress(final Invoice invoice, final List<String> errorMessages) {
        boolean validAddress = ERPCustomerFieldsBean.validateAddress(invoice.getDebtAccount().getCustomer(), errorMessages);

        if (invoice.isForPayorDebtAccount()) {
            validAddress =
                    ERPCustomerFieldsBean.validateAddress(invoice.getPayorDebtAccount().getCustomer(), errorMessages);
        }

        return validAddress;
    }

    public static boolean validateAddress(final Customer customer, final List<String> errorMessages) {

        final String addressCountryCode = customer.getSaftBillingAddressCountry();
        final String address = customer.getSaftBillingAddressDetail();
        final String zipCode = customer.getSaftBillingAddressPostalCode();
        final String districtSubdivision = customer.getSaftBillingAddressCity();
        final String region =
                !isNullOrEmpty(customer.getSaftBillingAddressRegion()) ? customer.getSaftBillingAddressRegion() : districtSubdivision;

        return validateAddress(customer.getFiscalCountry(), customer.getFiscalNumber(), customer.getName(),
                addressCountryCode, address, zipCode, districtSubdivision,
                region, errorMessages);
    }

    private static boolean validateAddress(final String fiscalCountry, final String fiscalNumber, final String name,
            final String addressCountryCode, final String address, final String zipCode, final String districtSubdivision,
            final String region, final List<String> errorMessages) {

        if (isNullOrEmpty(addressCountryCode)) {
            errorMessages.add(treasuryBundle("error.ERPCustomerFieldsBean.address.countryCode.not.filled", fiscalCountry,
                    fiscalNumber, name));
        }

        if (!isNullOrEmpty(addressCountryCode) && !isSameCountryCode(addressCountryCode, fiscalCountry)) {
            errorMessages.add(treasuryBundle("error.ERPCustomerFieldsBean.fiscal.country.not.equals.to.address",
                    addressCountryCode, fiscalCountry, fiscalCountry, fiscalNumber, name));
        }

        if (!isNullOrEmpty(address) && address.length() > MAX_ADDRESS_DETAIL) {
            errorMessages.add(treasuryBundle("error.ERPCustomerFieldsBean.addressDetail.more.than.allowed",
                    String.valueOf(MAX_ADDRESS_DETAIL), address, fiscalCountry, fiscalNumber, name));
        }

        if (!isNullOrEmpty(districtSubdivision) && districtSubdivision.length() > MAX_CITY) {
            errorMessages.add(treasuryBundle("error.ERPCustomerFieldsBean.city.more.than.allowed", String.valueOf(MAX_CITY),
                    districtSubdivision, fiscalCountry, fiscalNumber, name));
        }

        if (!isNullOrEmpty(zipCode) && zipCode.length() > MAX_ZIPCODE) {
            errorMessages.add(treasuryBundle("error.ERPCustomerFieldsBean.zipCode.more.than.allowed", String.valueOf(MAX_ZIPCODE),
                    zipCode, fiscalCountry, fiscalNumber, name));
        }

        if (!isNullOrEmpty(region) && region.length() > MAX_REGION) {
            errorMessages.add(treasuryBundle("error.ERPCustomerFieldsBean.region.more.than.allowed", String.valueOf(MAX_REGION),
                    region, fiscalCountry, fiscalNumber, name));
        }

        return errorMessages.isEmpty();
    }

    public static boolean checkIncompleteAddressForDebtAccountAndPayors(final DebtAccount debtAccount,
            final List<String> errorMessages) {
        final Set<Customer> referencedCustomers = Sets.newHashSet(debtAccount.getCustomer());

        for (final InvoiceEntry invoiceEntry : debtAccount.getActiveInvoiceEntries().collect(Collectors.toSet())) {
            if (invoiceEntry.getFinantialDocument() == null) {
                continue;
            }

            final Invoice invoice = (Invoice) invoiceEntry.getFinantialDocument();

            if (invoice.isForPayorDebtAccount()) {
                referencedCustomers.add(invoice.getPayorDebtAccount().getCustomer());
            }
        }

        boolean validAddress = true;
        for (final Customer customer : referencedCustomers) {
            validAddress &= ERPCustomerFieldsBean.validateAddress(customer, errorMessages);
        }

        return validAddress;
    }

    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on

    public String getCustomerBusinessId() {
        return customerBusinessId;
    }

    public void setCustomerBusinessId(String customerBusinessId) {
        this.customerBusinessId = customerBusinessId;
    }

    public String getCustomerFiscalCountry() {
        return customerFiscalCountry;
    }

    public void setCustomerFiscalCountry(String customerFiscalCountry) {
        this.customerFiscalCountry = customerFiscalCountry;
    }

    public String getCustomerNationality() {
        return customerNationality;
    }

    public void setCustomerNationality(String customerNationality) {
        this.customerNationality = customerNationality;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerAccountId() {
        return customerAccountId;
    }

    public void setCustomerAccountId(String customerAccountId) {
        this.customerAccountId = customerAccountId;
    }

    public String getCustomerFiscalNumber() {
        return customerFiscalNumber;
    }

    public void setCustomerFiscalNumber(String customerFiscalNumber) {
        this.customerFiscalNumber = customerFiscalNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerContact() {
        return customerContact;
    }

    public void setCustomerContact(String customerContact) {
        this.customerContact = customerContact;
    }

    public String getCustomerStreetName() {
        return customerStreetName;
    }

    public void setCustomerStreetName(String custometStreetName) {
        this.customerStreetName = custometStreetName;
    }

    public String getCustomerAddressDetail() {
        return customerAddressDetail;
    }

    public void setCustomerAddressDetail(String customerAddressDetail) {
        this.customerAddressDetail = customerAddressDetail;
    }

    public String getCustomerCity() {
        return customerCity;
    }

    public void setCustomerCity(String customerCity) {
        this.customerCity = customerCity;
    }

    public String getCustomerZipCode() {
        return customerZipCode;
    }

    public void setCustomerZipCode(String customerZipCode) {
        this.customerZipCode = customerZipCode;
    }

    public String getCustomerRegion() {
        return customerRegion;
    }

    public void setCustomerRegion(String customerRegion) {
        this.customerRegion = customerRegion;
    }

    public String getCustomerCountry() {
        return customerCountry;
    }

    public void setCustomerCountry(String customerCountry) {
        this.customerCountry = customerCountry;
    }
    
    public String getCustomerTelephone() {
        return customerTelephone;
    }
    
    public void setCustomerTelephone(String customerTelephone) {
        this.customerTelephone = customerTelephone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }
    
    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }
    
    
}
