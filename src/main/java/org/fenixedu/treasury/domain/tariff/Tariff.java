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
package org.fenixedu.treasury.domain.tariff;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public abstract class Tariff extends Tariff_Base {

    protected Tariff() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(final FinantialEntity finantialEntity, final Product product, final DateTime beginDate,
            final DateTime endDate, final DueDateCalculationType dueDateCalculationType, final LocalDate fixedDueDate,
            final int numberOfDaysAfterCreationForDueDate, final boolean applyInterests, final InterestType interestType,
            final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday, final int maximumDaysToApplyPenalty,
            final BigDecimal interestFixedAmount, final BigDecimal rate) {
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
                    maximumDaysToApplyPenalty, interestFixedAmount, rate);
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
    
    public abstract BigDecimal amountToPay();

    /**
     * Return if the tariff is specificed without any additional parameters for matching like degreeType, degree, cycle, and so on...
     * @return
     */
    public abstract boolean isBroadTariffForFinantialEntity();
    
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
        return FenixFramework.getDomainRoot().getTariffsSet().stream();
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
