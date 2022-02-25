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

import java.util.Comparator;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.util.LocalizedStringUtil;
import org.fenixedu.treasury.util.TreasuryBootstrapper;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.District;
import pt.ist.standards.geographic.Municipality;

public class FinantialInstitution extends FinantialInstitution_Base {

    public static final Comparator<FinantialInstitution> COMPARATOR_BY_NAME = (o1, o2) -> {
        int c = o1.getName().compareTo(o2.getName());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    protected FinantialInstitution() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setInvoiceRegistrationMode(InvoiceRegistrationMode.ERP_INTEGRATION);
    }

    protected FinantialInstitution(final FiscalCountryRegion fiscalCountryRegion, final Currency currency, final String code,
            final String fiscalNumber, final String companyId, final String name, final String companyName, final String address,
            final Country country, final District district, final Municipality municipality, final String locality,
            final String zipCode) {
        this();
        setFiscalCountryRegion(fiscalCountryRegion);
        setCode(code);
        setFiscalNumber(fiscalNumber);
        setCompanyId(companyId);
        setName(name);
        setCompanyName(companyName);
        setAddress(address);
        setCountry(country);
        setDistrict(district);
        setMunicipality(municipality);
        setLocality(locality);
        setZipCode(zipCode);
        setCurrency(currency);

        ERPConfiguration.createEmpty(this);
        TreasuryBootstrapper.bootstrapFinantialInstitution(this);

        checkRules();
    }

    private void checkRules() {
        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.FinantialInstitution.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getFiscalNumber())) {
            throw new TreasuryDomainException("error.FinantialInstitution.fiscalNumber.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.FinantialInstitution.name.required");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new TreasuryDomainException("error.FinantialInstitution.code.duplicated");
        }
        if (findByName(getName()).count() > 1) {
            throw new TreasuryDomainException("error.FinantialInstitution.name.duplicated");
        }

//        IFiscalContributor.findByFiscalNumber(getFiscalNumber());
    }

    @Atomic
    public void editContacts(final String email, final String telephoneContact, final String webAddress) {
        setEmail(email);
        setTelephoneContact(telephoneContact);
        setWebAddress(webAddress);
    }

    public String getComercialRegistrationCode() {
        return this.getFiscalNumber() + " " + this.getAddress();
    }

    @Atomic
    public void edit(final FiscalCountryRegion fiscalCountryRegion, final Currency currency, final String code,
            final String fiscalNumber, final String companyId, final String name, final String companyName, final String address,
            final Country country, final District district, final Municipality municipality, final String locality,
            final String zipCode) {
        setFiscalCountryRegion(fiscalCountryRegion);
        setCurrency(currency);
        setCode(code);
        setFiscalNumber(fiscalNumber);
        setCompanyId(companyId);
        setName(name);
        setCompanyName(companyName);
        setAddress(address);
        setCountry(country);
        setDistrict(district);
        setMunicipality(municipality);
        setLocality(locality);
        setZipCode(zipCode);

        checkRules();
    }

    public boolean isDeletable() {
        if (this.getFinantialEntitiesSet().stream().anyMatch(x -> x.isDeletable() == false)) {
            return false;
        }

        if (this.getDebtAccountsSet().stream().anyMatch(x -> x.isDeletable() == false)) {
            return false;
        }

        if (this.getSeriesSet().stream().anyMatch(x -> x.isDeletable() == false)) {
            return false;
        }

        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.FinantialInstitution.cannot.delete");
        }

        setDomainRoot(null);
        setCurrency(null);
        setCountry(null);
        setDistrict(null);
        setMunicipality(null);
        setFiscalCountryRegion(null);

        for (DebtAccount debt : getDebtAccountsSet()) {
            this.removeDebtAccounts(debt);
            debt.delete();
        }

        for (FinantialEntity entity : getFinantialEntitiesSet()) {
            this.removeFinantialEntities(entity);
            entity.delete();
        }

        for (Product p : getAvailableProductsSet()) {
            this.removeAvailableProducts(p);
        }

        for (Series s : getSeriesSet()) {
            this.removeSeries(s);
            s.delete();
        }

        for (Vat vat : getVatsSet()) {
            vat.delete();
        }

        deleteDomainObject();
    }

    @Atomic
    public void markSeriesAsDefault(final Series series) {
        for (final Series s : getSeriesSet()) {
            s.setDefaultSeries(false);
        }

        series.setDefaultSeries(true);
    }

    public Set<FinantialDocument> getExportableDocuments(DateTime fromDate, DateTime toDate) {
        Set<FinantialDocument> result = new HashSet<FinantialDocument>();
        for (Series series : this.getSeriesSet()) {
            for (DocumentNumberSeries documentNumberSeries : series.getDocumentNumberSeriesSet()) {
                result.addAll(documentNumberSeries.getFinantialDocumentsSet().stream()
                        .filter(x -> x.getDocumentDate().isAfter(fromDate) && x.getDocumentDate().isBefore(toDate))
                        .collect(Collectors.toSet()));
            }
        }

        return result;
    }

    public Vat getActiveVat(VatType vatType, DateTime when) {
        return this.getVatsSet().stream().filter(x -> x.isActive(when) && x.getVatType().equals(vatType)).findFirst()
                .orElse(null);
    }

    public String getUiCompleteAddress() {

        final StringBuilder sb = new StringBuilder();

        if (!Strings.isNullOrEmpty(getAddress())) {
            sb.append(getAddress()).append(", ");
        }

        if (!Strings.isNullOrEmpty(getZipCode())) {
            sb.append(getZipCode()).append(", ");
        }

        if (!Strings.isNullOrEmpty(getLocality())) {
            sb.append(getLocality()).append(", ");
        }

        if (getMunicipality() != null) {
            sb.append(getMunicipality().name).append(", ");
        }

        if (getCountry() != null) {
            sb.append(getCountry().alpha2).append(", ");
        }

        if (sb.length() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }

        return sb.toString();
    }

    public boolean isToCloseCreditNoteWhenCreated() {
        return getErpIntegrationConfiguration() != null && getErpIntegrationConfiguration().isToCloseCreditNoteWhenCreated();
    }
    
    public boolean isInvoiceRegistrationByErpIntegration() {
        return getInvoiceRegistrationMode() == InvoiceRegistrationMode.ERP_INTEGRATION;
    }

    public boolean isInvoiceRegistrationByTreasuryCertification() {
        return getInvoiceRegistrationMode() == InvoiceRegistrationMode.TREASURY_CERTIFICATION;
    }

    // ********
    // SERVICES
    // ********

    public static Stream<FinantialInstitution> findAll() {
        return FenixFramework.getDomainRoot().getFinantialInstitutionsSet().stream();
    }

    public static Optional<FinantialInstitution> findUnique() {
        final Set<FinantialInstitution> all = FenixFramework.getDomainRoot().getFinantialInstitutionsSet();
        return all.size() != 1 ? Optional.empty() : Optional.of(all.iterator().next());
    }

    public static Stream<FinantialInstitution> findByCode(final String code) {
        return findAll().filter(fi -> fi.getCode().equalsIgnoreCase(code));
    }

    public static Stream<FinantialInstitution> findByName(final String name) {
        return findAll().filter(fi -> fi.getName().equalsIgnoreCase(name));
    }

    @Atomic
    public static FinantialInstitution create(final FiscalCountryRegion fiscalCountryRegion, final Currency currency,
            final String code, final String fiscalNumber, final String companyId, final String name, final String companyName,
            final String address, final Country country, final District district, final Municipality municipality,
            final String locality, final String zipCode) {
        return new FinantialInstitution(fiscalCountryRegion, currency, code, fiscalNumber, companyId, name, companyName, address,
                country, district, municipality, locality, zipCode);
    }

    public static Optional<FinantialInstitution> findUniqueByFiscalCode(String fiscalNumber) {
        return findAll().filter(x -> fiscalNumber.equals(x.getFiscalNumber())).findFirst();
    }

}
