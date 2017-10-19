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
package org.fenixedu.treasury.domain.exemption;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;;

public class TreasuryExemptionType extends TreasuryExemptionType_Base {

    public static final Comparator<? super TreasuryExemptionType> COMPARE_BY_NAME = new Comparator<TreasuryExemptionType>() {
        @Override
        public int compare(final TreasuryExemptionType o1, final TreasuryExemptionType o2) {
            int c = o1.getName().getContent().compareTo(o2.getName().getContent());
            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    protected TreasuryExemptionType() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    protected TreasuryExemptionType(final String code, final LocalizedString name, final BigDecimal defaultExemptionPercentage,
            final boolean active) {
        this();
        setCode(code);
        setName(name);
        setDefaultExemptionPercentage(defaultExemptionPercentage);
        setActive(active);

        checkRules();
    }
    
    private void checkRules() {
        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.Currency.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.Currency.name.required");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new TreasuryDomainException("error.TreasuryExemptionType.code.duplicated");
        }
    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final BigDecimal discountRate, final boolean active) {
        setCode(code);
        setName(name);
        setDefaultExemptionPercentage(discountRate);
        setActive(active);

        checkRules();
    }

    public boolean isDeletable() {
        return getTreasuryExemptionsSet().isEmpty();
    }
    public boolean isActive() {
        return super.getActive();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.TreasuryExemptionType.cannot.delete");
        }
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Atomic
    public static TreasuryExemptionType create(final String code, final LocalizedString name,
            final BigDecimal defaultExemptionPercentage, final boolean active) {
        return new TreasuryExemptionType(code, name, defaultExemptionPercentage, active);
    }

    public static Stream<TreasuryExemptionType> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getTreasuryExemptionTypesSet().stream();
    }

    public static Stream<TreasuryExemptionType> findByCode(final String code) {
        return findAll().filter(i -> code.equalsIgnoreCase(i.getCode()));
    }

    public static Stream<TreasuryExemptionType> findByDebtAccount(DebtAccount debtAccount) {
        return Stream.empty();
    }

}
