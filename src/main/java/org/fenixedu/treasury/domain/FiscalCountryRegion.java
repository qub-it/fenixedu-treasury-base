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

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;

public class FiscalCountryRegion extends FiscalCountryRegion_Base {

    protected FiscalCountryRegion() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    protected FiscalCountryRegion(final String fiscalCode, final LocalizedString name) {
        this();
        setFiscalCode(fiscalCode);
        setName(name);

        checkRules();
    }

    @Atomic
    public static void initializeFiscalRegion() {
        if (FiscalCountryRegion.findAll().count() == 0) {
            FiscalCountryRegion.create(
                    "PT",
                    org.fenixedu.treasury.util.Constants.bundleI18N("label.FiscalCountryRegion.PT"));
            FiscalCountryRegion.create(
                    "PT_MA",
                    org.fenixedu.treasury.util.Constants.bundleI18N("label.FiscalCountryRegion.PT_MA"));
            FiscalCountryRegion.create(
                    "PT_AZ",
                    org.fenixedu.treasury.util.Constants.bundleI18N("label.FiscalCountryRegion.PT_AZ"));
        }
    }

    private void checkRules() {
        if (LocalizedStringUtil.isTrimmedEmpty(getFiscalCode())) {
            throw new TreasuryDomainException("error.FiscalCountryRegion.fiscalCode.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.FiscalCountryRegion.name.required");
        }

        findByRegionCode(getFiscalCode());

        getName().getLocales().stream().forEach(l -> findByName(getName().getContent(l)));
    }

    @Atomic
    public void edit(final String fiscalCode, final LocalizedString name) {
        setFiscalCode(fiscalCode);
        setName(name);

        checkRules();
    }

    public boolean isDeletable() {
        return getFinantialInstitutionsSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.FiscalCountryRegion.cannot.delete");
        }

        setDomainRoot(null);

        deleteDomainObject();
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<FiscalCountryRegion> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getFiscalCountryRegionsSet().stream();
    }

    public static FiscalCountryRegion findByRegionCode(final String fiscalCode) {
        FiscalCountryRegion result = null;

        for (final FiscalCountryRegion it : findAll().collect(Collectors.toList())) {
            if (!it.getFiscalCode().equalsIgnoreCase(fiscalCode)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.FiscalCountryRegion.duplicated.fiscalCode");
            }

            result = it;
        }

        return result;
    }

    public static FiscalCountryRegion findByName(final String name) {
        FiscalCountryRegion result = null;

        for (final FiscalCountryRegion it : findAll().collect(Collectors.toList())) {

            if (!LocalizedStringUtil.isEqualToAnyLocaleIgnoreCase(it.getName(), name)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.FiscalCountryRegion.duplicated.name");
            }

            result = it;
        }

        return result;
    }

    @Atomic
    public static FiscalCountryRegion create(final String fiscalCode, final LocalizedString name) {
        return new FiscalCountryRegion(fiscalCode, name);
    }

}
