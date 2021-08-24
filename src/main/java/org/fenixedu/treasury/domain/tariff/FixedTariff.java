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

// TODO: Delete all instances of this entity, replace with academic tariff
/**
 * The FixedTariff is useful for unit tests
 */
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
