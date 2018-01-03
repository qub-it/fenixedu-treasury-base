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
package org.fenixedu.treasury.domain.tariff;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;;

public abstract class Tariff extends Tariff_Base {

    protected Tariff() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    public abstract LocalizedString getUiTariffDescription();

    protected void init(final FinantialEntity finantialEntity, final Product product, final DateTime beginDate,
            final DateTime endDate, final DueDateCalculationType dueDateCalculationType, final LocalDate fixedDueDate,
            final int numberOfDaysAfterCreationForDueDate, final boolean applyInterests, final InterestType interestType,
            final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday, final int maximumDaysToApplyPenalty,
            final int maximumMonthsToApplyPenalty, final BigDecimal interestFixedAmount, final BigDecimal rate) {
        setFinantialEntity(finantialEntity);
        setProduct(product);

        setBeginDate(beginDate);
        setEndDate(endDate);
        setDueDateCalculationType(dueDateCalculationType);
        setFixedDueDate(fixedDueDate);
        setNumberOfDaysAfterCreationForDueDate(numberOfDaysAfterCreationForDueDate);
        setApplyInterests(applyInterests);
        if (getApplyInterests()) {
            InterestRate.createForTariff(this, interestType, numberOfDaysAfterDueDate, applyInFirstWorkday,
                    maximumDaysToApplyPenalty, maximumMonthsToApplyPenalty, interestFixedAmount, rate);
        }
    }

    protected void checkRules() {
        if(getFinantialEntity() == null) {
            throw new TreasuryDomainException("error.Tariff.finantialEntity.required");
        }
        
        if (getProduct() == null) {
            throw new TreasuryDomainException("error.Tariff.product.required");
        }

        if (getBeginDate() == null) {
            throw new TreasuryDomainException("error.Tariff.beginDate.required");
        }

        if (getEndDate() != null && !getEndDate().isAfter(getBeginDate())) {
            throw new TreasuryDomainException("error.Tariff.endDate.must.be.after.beginDate");
        }

        if (getDueDateCalculationType() == null) {
            throw new TreasuryDomainException("error.Tariff.dueDateCalculationType.required");
        }

        if ((getDueDateCalculationType().isFixedDate() || getDueDateCalculationType().isBestOfFixedDateAndDaysAfterCreation())
                && getFixedDueDate() == null) {
            throw new TreasuryDomainException("error.Tariff.fixedDueDate.required");
        }

        if (getFixedDueDate() != null
                && getFixedDueDate().toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1).isBefore(getBeginDate())) {
            throw new TreasuryDomainException("error.Tariff.fixedDueDate.must.be.after.or.equal.beginDate");
        }

        if ((getDueDateCalculationType().isDaysAfterCreation()
                || getDueDateCalculationType().isBestOfFixedDateAndDaysAfterCreation())
                && getNumberOfDaysAfterCreationForDueDate() < 0) {
            throw new TreasuryDomainException("error.Tariff.numberOfDaysAfterCreationForDueDate.must.be.positive");
        }

        if (isApplyInterests()) {
            if (getInterestRate() == null || getInterestRate().getInterestType() == null) {
                throw new TreasuryDomainException("error.Tariff.interestRate.required");
            }

            if (getInterestRate().getInterestType().isDaily()) {
                if (getInterestRate().getRate() == null || !isPositive(getInterestRate().getRate())) {
                    throw new TreasuryDomainException("error.Tariff.interestRate.invalid");
                }
                if (getInterestRate().getNumberOfDaysAfterDueDate() <= 0) {
                    throw new TreasuryDomainException("error.Tariff.interestRate.numberofdaysafterduedate.invalid");
                }
                if (getInterestRate().getMaximumDaysToApplyPenalty() < 0) {
                    throw new TreasuryDomainException("error.Tariff.interestRate.maximumdaystoapplypenalty.invalid");
                }
            }
            if (getInterestRate().getInterestType().isMonthly()) {
                if (getInterestRate().getRate() == null || BigDecimal.ZERO.compareTo(getInterestRate().getRate()) >= 0) {
                    throw new TreasuryDomainException("error.Tariff.interestRate.invalid");
                }

                if (getInterestRate().getMaximumMonthsToApplyPenalty() < 0) {
                    throw new TreasuryDomainException("error.Tariff.interestRate.maximummonthstoapplypenalty.invalid");
                }
            }

            if (getInterestRate().getInterestType() == InterestType.FIXED_AMOUNT) {
                if (BigDecimal.ZERO.compareTo(getInterestRate().getInterestFixedAmount()) >= 0) {
                    throw new TreasuryDomainException("error.Tariff.interestRate.interestfixedamount.invalid");
                }
            }
        }
    }

    protected Interval getInterval() {
        return new Interval(getBeginDate(), getEndDate());
    }

    public boolean isEndDateDefined() {
        return getEndDate() != null;
    }

    public boolean isActive(final DateTime when) {
        return new Interval(getBeginDate(), getEndDate()).contains(when);
    }

    public boolean isActive(final Interval dateInterval) {
        return new Interval(getBeginDate(), getEndDate()).overlaps(dateInterval);
    }

    public boolean isApplyInterests() {
        return getApplyInterests();
    }

    @Atomic
    public void edit(final DateTime beginDate, final DateTime endDate) {
        super.setBeginDate(beginDate);
        super.setEndDate(endDate);

        checkRules();
    }

    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.Tariff.cannot.delete");
        }

        setDomainRoot(null);
        setProduct(null);
        setFinantialEntity(null);

        if (getInterestRate() != null) {
            getInterestRate().delete();
        }

        super.deleteDomainObject();
    }

    // @formatter: off
    /************
     * UTILS *
     ************/
    // @formatter: on

    protected boolean isNegative(final BigDecimal value) {
        return !isZero(value) && !isPositive(value);
    }

    protected boolean isZero(final BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) == 0;
    }

    protected boolean isPositive(final BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) < 0;
    }

    protected boolean isGreaterThan(final BigDecimal v1, final BigDecimal v2) {
        return v1.compareTo(v2) > 0;
    }

    public LocalDate dueDate(final LocalDate requestDate) {

        if (getDueDateCalculationType().isFixedDate()) {
            return getFixedDueDate();
        }

        if (getDueDateCalculationType().isBestOfFixedDateAndDaysAfterCreation()) {
            final LocalDate daysAfterCreation = requestDate.plusDays(getNumberOfDaysAfterCreationForDueDate());

            if (daysAfterCreation.isAfter(getFixedDueDate())) {
                return daysAfterCreation;
            } else {
                return getFixedDueDate();
            }
        }

        return requestDate.plusDays(getNumberOfDaysAfterCreationForDueDate());
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<? extends Tariff> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getTariffsSet().stream();
    }

    public static Stream<? extends Tariff> find(final Product product) {
        return product.getTariffSet().stream();
    }

    public static Stream<? extends Tariff> find(final Product product, final DateTime when) {
        return find(product).filter(t -> t.isActive(when));
    }

    public static Stream<? extends Tariff> findInInterval(final Product product, final DateTime start, final DateTime end) {
        final Interval interval = new Interval(start, end);
        return find(product).filter(t -> t.isActive(interval));
    }

}
