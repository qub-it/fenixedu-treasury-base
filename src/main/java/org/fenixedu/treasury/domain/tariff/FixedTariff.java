/**
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.dto.FixedTariffInterestRateBean;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
// TODO: Delete all instances of this entity and then remove from domain
public class FixedTariff extends FixedTariff_Base {

    protected FixedTariff(final FinantialEntity finantialEntity, final Product product, final DateTime beginDate,
            final DateTime endDate, final BigDecimal amount, final DueDateCalculationType dueDateCalculationType,
            final LocalDate fixedDueDate, final int numberOfDaysAfterCreationForDueDate, final boolean applyInterests,
            final InterestType interestType, final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday,
            final int maximumDaysToApplyPenalty, final BigDecimal interestFixedAmount,
            final BigDecimal rate) {
        super();

        init(finantialEntity, product, beginDate, endDate, amount, dueDateCalculationType, fixedDueDate,
                numberOfDaysAfterCreationForDueDate, applyInterests, interestType, numberOfDaysAfterDueDate, applyInFirstWorkday,
                maximumDaysToApplyPenalty, interestFixedAmount, rate);
    }

    @Override
    protected void init(final FinantialEntity finantialEntity, final Product product, final DateTime beginDate,
            final DateTime endDate, final DueDateCalculationType dueDateCalculationType, final LocalDate fixedDueDate,
            final int numberOfDaysAfterCreationForDueDate, final boolean applyInterests, final InterestType interestType,
            final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday, final int maximumDaysToApplyPenalty,
            final BigDecimal interestFixedAmount, final BigDecimal rate) {
        throw new RuntimeException("error.FixedTariff.use.init.with.amount");
    }

    protected void init(final FinantialEntity finantialEntity, final Product product, final DateTime beginDate,
            final DateTime endDate, final BigDecimal amount, final DueDateCalculationType dueDateCalculationType,
            LocalDate fixedDueDate, int numberOfDaysAfterCreationForDueDate, boolean applyInterests, InterestType interestType,
            int numberOfDaysAfterDueDate, boolean applyInFirstWorkday, int maximumDaysToApplyPenalty,
            BigDecimal interestFixedAmount, BigDecimal rate) {
        super.init(finantialEntity, product, beginDate, endDate, dueDateCalculationType, fixedDueDate,
                numberOfDaysAfterCreationForDueDate, applyInterests, interestType, numberOfDaysAfterDueDate, applyInFirstWorkday,
                maximumDaysToApplyPenalty, interestFixedAmount, rate);

        setAmount(amount);
        checkRules();
    }

    protected FixedTariff() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(final Product product, final InterestRate interestRate, final FinantialEntity finantialEntity,
            final BigDecimal amount, final DateTime beginDate, final DateTime endDate,
            final DueDateCalculationType dueDateCalculationType, final LocalDate fixedDueDate,
            final int numberOfDaysAfterCreationForDueDate, final boolean applyInterests) {
        setProduct(product);
        setInterestRate(interestRate);
        setFinantialEntity(finantialEntity);
        setAmount(amount);
        setBeginDate(beginDate);
        setEndDate(endDate);
        setDueDateCalculationType(dueDateCalculationType);
        setFixedDueDate(fixedDueDate);
        setNumberOfDaysAfterCreationForDueDate(numberOfDaysAfterCreationForDueDate);
        setApplyInterests(applyInterests);
        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();
        if (getProduct() == null) {
            throw new TreasuryDomainException("error.FixedTariff.product.required");
        }

        if (getFinantialEntity() == null) {
            throw new TreasuryDomainException("error.FixedTariff.finantialEntity.required");
        }

        if (this.getAmount() == null || this.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TreasuryDomainException("error.FixedTariff.amount.invalid");
        }
    }

    @Atomic
    public void edit(final Product product, final FinantialEntity finantialEntity, final BigDecimal amount,
            final DateTime beginDate, final DateTime endDate, final DueDateCalculationType dueDateCalculationType,
            final LocalDate fixedDueDate, final int numberOfDaysAfterCreationForDueDate, final boolean applyInterests,
            final FixedTariffInterestRateBean rateBean) {
        setProduct(product);
        setFinantialEntity(finantialEntity);
        setAmount(amount);
        setBeginDate(beginDate);
        setEndDate(endDate);
        setDueDateCalculationType(dueDateCalculationType);
        setFixedDueDate(fixedDueDate);
        setNumberOfDaysAfterCreationForDueDate(numberOfDaysAfterCreationForDueDate);
        setApplyInterests(applyInterests);

        if (applyInterests) {
            if (getInterestRate() == null) {
                InterestRate rate =
                        InterestRate.createForTariff(this, rateBean.getInterestType(), rateBean.getNumberOfDaysAfterDueDate(),
                                rateBean.getApplyInFirstWorkday(), rateBean.getMaximumDaysToApplyPenalty(),
                                rateBean.getInterestFixedAmount(), rateBean.getRate());
                setInterestRate(rate);
            } else {
                InterestRate rate = getInterestRate();
                rate.setApplyInFirstWorkday(rateBean.getApplyInFirstWorkday());
                rate.setInterestFixedAmount(rateBean.getInterestFixedAmount());
                rate.setInterestType(rateBean.getInterestType());
                rate.setMaximumDaysToApplyPenalty(rateBean.getMaximumDaysToApplyPenalty());
                rate.setNumberOfDaysAfterDueDate(rateBean.getNumberOfDaysAfterDueDate());
                rate.setRate(rateBean.getRate());
            }
        } else {
            getInterestRate().delete();
        }

        checkRules();
    }

    @Override
    public boolean isDeletable() {
        return super.isDeletable();
    }

    @Override
    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.FixedTariff.cannot.delete");
        }

        super.delete();
    }
    
    @Override
    public BigDecimal amountToPay() {
        return getAmount();
    }
    
    @Override
    public boolean isBroadTariffForFinantialEntity() {
        return true;
    }
    
    @Atomic
    public static FixedTariff create(final Product product, final InterestRate interestRate,
            final FinantialEntity finantialEntity, final BigDecimal amount, final DateTime beginDate, final DateTime endDate,
            final DueDateCalculationType dueDateCalculationType, final LocalDate fixedDueDate,
            final int numberOfDaysAfterCreationForDueDate, final boolean applyInterests) {
        FixedTariff fixedTariff = new FixedTariff();
        fixedTariff.init(product, interestRate, finantialEntity, amount, beginDate, endDate, dueDateCalculationType,
                fixedDueDate, numberOfDaysAfterCreationForDueDate, applyInterests);
        return fixedTariff;
    }

    public static Stream<FixedTariff> findAll(FinantialInstitution institution) {
        Set<FixedTariff> result = new HashSet<FixedTariff>();
        FenixFramework.getDomainRoot().getFinantialInstitutionsSet()
                .forEach(x -> x.getFinantialEntitiesSet().stream().forEach(y -> result.addAll(y.getFixedTariffSet())));
        return result.stream();
    }

    public static Stream<FixedTariff> findByProduct(final FinantialInstitution institution, final Product product) {
        return findAll(institution).filter(i -> product.equals(i.getProduct()));
    }

    public static Stream<FixedTariff> findByInterestRate(final FinantialInstitution institution, final InterestRate InterestRate) {
        return findAll(institution).filter(i -> InterestRate.equals(i.getInterestRate()));
    }

    public static Stream<FixedTariff> findByFinantialEntity(final FinantialInstitution institution,
            final FinantialEntity finantialEntity) {
        return findAll(institution).filter(i -> finantialEntity.equals(i.getFinantialEntity()));
    }

    public static Stream<FixedTariff> findByAmount(final FinantialInstitution institution, final BigDecimal amount) {
        return findAll(institution).filter(i -> amount.equals(i.getAmount()));
    }

    public static Stream<FixedTariff> findByBeginDate(final FinantialInstitution institution, final DateTime beginDate) {
        return findAll(institution).filter(i -> beginDate.equals(i.getBeginDate()));
    }

    public static Stream<FixedTariff> findByEndDate(final FinantialInstitution institution, final DateTime endDate) {
        return findAll(institution).filter(i -> endDate.equals(i.getEndDate()));
    }

    public static Stream<FixedTariff> findByDueDateCalculationType(final FinantialInstitution institution,
            final org.fenixedu.treasury.domain.tariff.DueDateCalculationType dueDateCalculationType) {
        return findAll(institution).filter(i -> dueDateCalculationType.equals(i.getDueDateCalculationType()));
    }

    public static Stream<FixedTariff> findByFixedDueDate(final FinantialInstitution institution,
            final org.joda.time.LocalDate fixedDueDate) {
        return findAll(institution).filter(i -> fixedDueDate.equals(i.getFixedDueDate()));
    }

    public static Stream<FixedTariff> findByNumberOfDaysAfterCreationForDueDate(final FinantialInstitution institution,
            final int numberOfDaysAfterCreationForDueDate) {
        return findAll(institution)
                .filter(i -> numberOfDaysAfterCreationForDueDate == i.getNumberOfDaysAfterCreationForDueDate());
    }

    public static Stream<FixedTariff> findByApplyInterests(final FinantialInstitution institution, final boolean applyInterests) {
        return findAll(institution).filter(i -> applyInterests == i.getApplyInterests());
    }

    public LocalDate calculateDueDate(DebitNote finantialDocument) {
        if (this.getDueDateCalculationType().equals(DueDateCalculationType.DAYS_AFTER_CREATION)) {
            if (finantialDocument != null) {
                return finantialDocument.getDocumentDueDate().plusDays(this.getNumberOfDaysAfterCreationForDueDate());
            } else {
                return new DateTime().plusDays(this.getNumberOfDaysAfterCreationForDueDate()).toLocalDate();
            }
        } else if (this.getDueDateCalculationType().equals(DueDateCalculationType.FIXED_DATE)) {
            return this.getFixedDueDate();
        } else if (finantialDocument != null) {
            if (finantialDocument.getDocumentDueDate() != null) {
                return finantialDocument.getDocumentDueDate();
            } else {
                return finantialDocument.getDocumentDate().toLocalDate();
            }
        } else {
            return new LocalDate();
        }
    }

}
