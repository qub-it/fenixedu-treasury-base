/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and ServiÃ§os Partilhados da
 * Universidade de Lisboa:
 *  - Copyright Â© 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright Â© 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com
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

package org.fenixedu.treasury.domain.tariff;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

import pt.ist.fenixframework.Atomic;;

public class GlobalInterestRate extends GlobalInterestRate_Base {

    protected GlobalInterestRate() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    protected void init(final int year, final LocalizedString description, final BigDecimal rate,
            final boolean applyPaymentMonth, final boolean applyInFirstWorkday) {
        setYear(year);
        setDescription(description);
        setRate(rate);
        setApplyPaymentMonth(applyPaymentMonth);
        setApplyInFirstWorkday(applyInFirstWorkday);

        checkRules();
    }

    private void checkRules() {
        if (findByYear(getYear()).count() > 1) {
            throw new TreasuryDomainException("error.GlobalInterestRate.year.duplicated");
        }
    }

    @Atomic
    public void edit(final int year, final LocalizedString description, final BigDecimal rate, final boolean applyPaymentMonth,
            final boolean applyInFirstWorkday) {
        setYear(year);
        setDescription(description);
        setRate(rate);
        setApplyPaymentMonth(applyPaymentMonth);
        setApplyInFirstWorkday(applyInFirstWorkday);

        checkRules();
    }
    
    public boolean isApplyPaymentMonth() {
        return super.getApplyPaymentMonth();
    }
    
    public boolean isApplyInFirstWorkday() {
        return super.getApplyInFirstWorkday();
    }

    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.GlobalInterestRate.cannot.delete");
        }

        setDomainRoot(null);

        deleteDomainObject();
    }

    @Atomic
    public static GlobalInterestRate create(final int year, final LocalizedString description, final BigDecimal rate,
            boolean applyPaymentMonth, boolean applyInFirstWorkday) {
        GlobalInterestRate globalInterestRate = new GlobalInterestRate();
        globalInterestRate.init(year, description, rate, applyPaymentMonth, applyInFirstWorkday);
        return globalInterestRate;
    }

    public static Stream<GlobalInterestRate> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getGlobalInterestRatesSet().stream();
    }

    public static Stream<GlobalInterestRate> findByYear(final int year) {
        return findAll().filter(i -> year == i.getYear());
    }

    public static Optional<GlobalInterestRate> findUniqueByYear(final int year) {
        return findByYear(year).findFirst();
    }

    public static Stream<GlobalInterestRate> findByDescription(final LocalizedString description) {
        return findAll().filter(i -> description.equals(i.getDescription()));
    }

    public static Stream<GlobalInterestRate> findByRate(final BigDecimal rate) {
        return findAll().filter(i -> rate.equals(i.getRate()));
    }

}
