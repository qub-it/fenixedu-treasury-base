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

import static org.fenixedu.treasury.util.TreasuryConstants.DEFAULT_LANGUAGE;

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
import org.fenixedu.treasury.domain.paymentcodes.SibsConfiguration;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.fenixedu.treasury.util.LocalizedStringUtil;
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
        
        SibsConfiguration.create(this, null, null, null);
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
        
        if(!Strings.isNullOrEmpty(getAddress())) {
            sb.append(getAddress()).append(", ");
        }
        
        if(!Strings.isNullOrEmpty(getZipCode())) {
            sb.append(getZipCode()).append(", ");
        }

        if(!Strings.isNullOrEmpty(getLocality())) {
            sb.append(getLocality()).append(", ");
        }

        if(getMunicipality() != null) {
            sb.append(getMunicipality().name).append(", ");
        }
        
        if(getCountry() != null) {
            sb.append(getCountry().alpha2).append(", ");
        }
        
        if(sb.length() > 0) {
            sb.delete(sb.length() - 2, sb.length());
        }
        
        return sb.toString();
    }

    public static Optional<FinantialInstitution> findUniqueByFiscalCode(String fiscalNumber) {
        return findAll().filter(x -> fiscalNumber.equals(x.getFiscalNumber())).findFirst();
    }
    
}
