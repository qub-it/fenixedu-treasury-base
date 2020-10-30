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

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.util.Locale;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class CustomerType extends CustomerType_Base {

    protected CustomerType() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected CustomerType(final String code, final LocalizedString name) {
        this();
        setCode(code);
        setName(name);

        checkRules();
    }

    private void checkRules() {
        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.CustomerType.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.CustomerType.name.required");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new TreasuryDomainException("error.CustomerType.code.duplicated");
        }

        getName().getLocales().stream().forEach(l -> findByName(getName().getContent(l)));
    }

    @Atomic
    public void edit(final String code, final LocalizedString name) {
        setCode(code);
        setName(name);

        checkRules();
    }

    public boolean isDeletable() {
        return getCustomersSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.CustomerType.cannot.delete");
        }

        setDomainRoot(null);

        deleteDomainObject();
    }

    public static Stream<CustomerType> findAll() {
        return FenixFramework.getDomainRoot().getCustomerTypesSet().stream();
    }

    public static Stream<CustomerType> findByCode(final String code) {
        return findAll().filter(ct -> ct.getCode().equalsIgnoreCase(code));
    }

    public static Stream<CustomerType> findByName(final String name) {
        return findAll().filter(ct -> ct.getName().equals(name));
    }

    @Atomic
    public static CustomerType create(final String code, final LocalizedString name) {
        return new CustomerType(code, name);
    }

    @Atomic
    public static void initializeCustomerType() {

        if (CustomerType.findAll().count() == 0) {
            CustomerType.create("CANDIDATE", treasuryBundleI18N("label.CustomerType.CANDIDATE"));
            CustomerType.create("STUDENT", treasuryBundleI18N("label.CustomerType.STUDENT"));
            CustomerType.create("ADHOC", treasuryBundleI18N("label.CustomerType.ADHOC"));
        }
    }

}
