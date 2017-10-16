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

import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.Constants;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import pt.ist.fenixframework.Atomic;;

public class Vat extends Vat_Base {

    protected Vat() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    protected Vat(final VatType vatType, final FinantialInstitution finantialInstitution, final BigDecimal taxRate,
            final DateTime beginDate, final DateTime endDate) {
        this();
        setVatType(vatType);
        setFinantialInstitution(finantialInstitution);

        setTaxRate(taxRate);
        setBeginDate(beginDate);
        setEndDate(endDate);

        checkRules();
    }

    private void checkRules() {
        if (getTaxRate() == null) {
            throw new TreasuryDomainException("error.Vat.taxRate.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.Vat.finantialInstitution.required");
        }

        if (getVatType() == null) {
            throw new TreasuryDomainException("error.Vat.vatType.required");
        }

        if (getTaxRate().compareTo(BigDecimal.ZERO) < 0) {
            throw new TreasuryDomainException("error.Vat.taxRate.cannot.be.negative");
        }

        if (getBeginDate() == null) {
            throw new TreasuryDomainException("error.Vat.beginDate.required");
        }

        if (getEndDate() == null) {
            throw new TreasuryDomainException("error.Vat.endDate.required");
        }

        if (!getEndDate().isAfter(getBeginDate())) {
            throw new TreasuryDomainException("error.Vat.endDate.end.date.must.be.after.begin.date");
        }

        if (findActive(getFinantialInstitution(), getVatType(), getBeginDate(), getEndDate()).count() > 1) {
            throw new TreasuryDomainException("error.Vat.date.interval.overlap.with.another");
        }
    }

    @Atomic
    public void edit(final BigDecimal taxRate, final DateTime beginDate, final DateTime endDate) {
        setTaxRate(taxRate);

        setBeginDate(beginDate);
        setEndDate(endDate);

        checkRules();
    }

    public boolean isDeletable() {
        return getInvoiceEntriesSet().isEmpty();

    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.Vat.cannot.delete");
        }

        setDomainRoot(null);
        setVatType(null);

        setFinantialInstitution(null);

        deleteDomainObject();
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<Vat> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getVatsSet().stream();
    }

    public static Stream<Vat> find(final VatType vatType) {
        return vatType.getVatsSet().stream();
    }

    protected static Stream<Vat> findActive(final VatType vatType, final DateTime when) {
        return find(vatType).filter(v -> v.interval().contains(when));
    }

    protected static Stream<Vat> findActive(final FinantialInstitution finantialInstitution, final VatType vatType,
            final DateTime begin, final DateTime end) {
        final Interval interval = new Interval(begin, end);
        return find(vatType).filter(v -> v.getFinantialInstitution().equals(finantialInstitution))
                .filter(v -> v.interval().overlaps(interval));
    }

    public static Optional<Vat> findActiveUnique(final VatType vatType, final FinantialInstitution finantialInstiution,
            final DateTime when) {
        return findActive(vatType, when).filter(x -> x.getFinantialInstitution().equals(finantialInstiution)).findFirst();
    }

    @Atomic
    public static Vat create(final VatType vatType, final FinantialInstitution finantialInstitution, final BigDecimal taxRate,
            final DateTime beginDate, final DateTime endDate) {
        return new Vat(vatType, finantialInstitution, taxRate, beginDate, endDate);
    }

    /* -----
     * UTILS
     * -----
     */

    private Interval interval() {
        // HACK: org.joda.time.Interval does not allow open end dates so use this date in the future
        return new Interval(getBeginDate(), getEndDate() != null ? getEndDate() : Constants.INFINITY_DATE);
    }

    public boolean isActiveNow() {
        return isActive(new DateTime());
    }

    public boolean isActive(DateTime when) {
        return this.getBeginDate().isBefore(when) && (this.getEndDate() == null || this.getEndDate().isAfter(when));
    }

}
