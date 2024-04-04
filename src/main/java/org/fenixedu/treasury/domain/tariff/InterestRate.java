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
import java.util.List;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class InterestRate extends InterestRate_Base {

    private static final int MAX_YEARS = 5;

    protected InterestRate() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected InterestRate(final Tariff tariff, final DebitEntry debitEntry, final InterestRateType interestRateType,
            final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday, final int maximumDaysToApplyPenalty,
            final BigDecimal interestFixedAmount, final BigDecimal rate) {
        this();

        setTariff(tariff);
        setDebitEntry(debitEntry);
        setInterestRateType(interestRateType);
        //HACK: Override the numberOfDaysAfterDueDate
        setNumberOfDaysAfterDueDate(1);
        setApplyInFirstWorkday(applyInFirstWorkday);
        setMaximumDaysToApplyPenalty(maximumDaysToApplyPenalty);
        setInterestFixedAmount(interestFixedAmount);
        setRate(rate);

        checkRules();

        if (debitEntry != null && getInterestRateType() != null) {
            getInterestRateType().checkDebitEntryRequirementsForInterestCalculation(debitEntry);
        }
    }

    private void checkRules() {

        if (getTariff() == null && getDebitEntry() == null) {
            throw new TreasuryDomainException("error.InterestRate.product.or.debit.entry.required");
        }

        if (getTariff() != null && getDebitEntry() != null) {
            throw new TreasuryDomainException("error.InterestRate.product.or.debit.entry.only.one");
        }

        if (getInterestRateType() == null) {
            throw new TreasuryDomainException("error.InterestRate.interestRateType.required");
        }

        if (getInterestRateType().isInterestFixedAmountRequired() && getInterestFixedAmount() == null) {
            throw new TreasuryDomainException("error.InterestRate.interestFixedAmount.required");
        }
    }

    @Deprecated
    // TODO ANIL 2023-05-05: read the comments in ::getMaximumDaysToApplyPenaltyApplied
    public boolean isMaximumDaysToApplyPenaltyApplied() {
        return getMaximumDaysToApplyPenalty() > 0;
    }

    @Deprecated
    // TODO ANIL 2023-05-05: read the comments in ::applyInFirstWorkday
    public boolean isApplyInFirstWorkday() {
        return getApplyInFirstWorkday();
    }

    @Atomic
    public void edit(InterestRateType interestRateType, int numberOfDaysAfterDueDate, boolean applyInFirstWorkday,
            int maximumDaysToApplyPenalty, BigDecimal interestFixedAmount, BigDecimal rate) {

        setInterestRateType(interestRateType);
        //HACK: For now override the NumberOfDays - 01/07/2015
        setNumberOfDaysAfterDueDate(1);
        setApplyInFirstWorkday(applyInFirstWorkday);
        setMaximumDaysToApplyPenalty(maximumDaysToApplyPenalty);
        setInterestFixedAmount(interestFixedAmount);
        setRate(rate);

        checkRules();

        if (getDebitEntry() != null && getInterestRateType() != null) {
            getInterestRateType().checkDebitEntryRequirementsForInterestCalculation(getDebitEntry());
        }
    }

    public List<InterestRateBean> calculateInterests(LocalDate paymentDate, boolean withAllInterestValues) {
        return getInterestRateType().calculateInterests(getDebitEntry(), paymentDate, withAllInterestValues);
    }

    /*
     * Calculate the interest rate at certain date and disregard any payments made after that date.
     * This interest calculation is necessary in processes where the interest rate is calculated
     * in advance and fixed by agreement, and cannot change due to payments made after that.
     */
    public List<InterestRateBean> calculateAllInterestsByLockingAtDate(LocalDate lockDate) {
        return getInterestRateType().calculateAllInterestsByLockingAtDate(getDebitEntry(), lockDate);
    }

    private Currency getRelatedCurrency() {
        if (getTariff() != null) {
            return getTariff().getFinantialEntity().getFinantialInstitution().getCurrency();
        } else if (getDebitEntry() != null) {
            return getDebitEntry().getCurrency();
        }
        return null;
    }

    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.InterestRate.cannot.delete");
        }

        setDomainRoot(null);
        setTariff(null);
        setDebitEntry(null);
        setInterestRateType(null);
        deleteDomainObject();
    }

    @Override
    @Deprecated
    /*
     * TODO ANIL 2023-05-05: In the constructor and ::edit method it is being set as 1
     * Check if it is used and consider removal
     */
    public int getNumberOfDaysAfterDueDate() {
        // TODO Auto-generated method stub
        return super.getNumberOfDaysAfterDueDate();
    }

    @Override
    @Deprecated
    /*
     *  TODO ANIL 2023-05-05: read comments in ::NumberOfDaysAfterDueDate
     */
    public void setNumberOfDaysAfterDueDate(int numberOfDaysAfterDueDate) {
        // TODO Auto-generated method stub
        super.setNumberOfDaysAfterDueDate(numberOfDaysAfterDueDate);
    }

    @Override
    @Deprecated
    /*
     * TODO ANIL 2023-05-05: I think it is not being used. Check and consider removal
     */
    public int getMaximumDaysToApplyPenalty() {
        return super.getMaximumDaysToApplyPenalty();
    }

    @Override
    @Deprecated
    /*
     * TODO ANIL 2023-05-05: read comments of ::setMaximumDaysToApplyPenalty
     */
    public void setMaximumDaysToApplyPenalty(int maximumDaysToApplyPenalty) {
        // TODO Auto-generated method stub
        super.setMaximumDaysToApplyPenalty(maximumDaysToApplyPenalty);
    }

    @Override
    @Deprecated
    /*
     * TODO: ANIL 2023-05-05: I think it is not being used. The global interest rate type used it's own 
     * rate table entry configuration, and fixed amount does not use
     */
    public boolean getApplyInFirstWorkday() {
        // TODO Auto-generated method stub
        return super.getApplyInFirstWorkday();
    }

    @Override
    @Deprecated
    /*
     * TODO: ANIL 2023-05-05: read the comments of ::getApplyInFirstWorkday
     */
    public void setApplyInFirstWorkday(boolean applyInFirstWorkday) {
        // TODO Auto-generated method stub
        super.setApplyInFirstWorkday(applyInFirstWorkday);
    }

    @Override
    @Deprecated
    /*
     * TODO: ANIL 2023-05-05: I think it is not being used. The global interest rate type used it's own 
     * rate table entry configuration, and fixed amount does not use
     */
    public BigDecimal getRate() {
        return super.getRate();
    }

    @Override
    @Deprecated
    /*
     * TODO: ANIL 2023-05-05: read the comments of ::getRate
     */
    public void setRate(BigDecimal rate) {
        super.setRate(rate);
    }

    @Deprecated
    public InterestType getInterestType() {
        return super.getInterestType();
    }

    @Override
    @Deprecated
    public void setInterestType(InterestType interestType) {
        super.setInterestType(interestType);
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<InterestRate> findAll() {
        return FenixFramework.getDomainRoot().getInterestRatesSet().stream();
    }

    @Atomic
    public static InterestRate createForTariff(final Tariff tariff, final InterestRateType interestRateType,
            final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday, final int maximumDaysToApplyPenalty,
            final BigDecimal interestFixedAmount, final BigDecimal rate) {
        return new InterestRate(tariff, null, interestRateType, numberOfDaysAfterDueDate, applyInFirstWorkday,
                maximumDaysToApplyPenalty, interestFixedAmount, rate);
    }

    public String getUiFullDescription() {
        if (getInterestRateType().isInterestFixedAmountRequired()) {
            return this.getInterestRateType().getDescription().getContent() + "-"
                    + getRelatedCurrency().getValueFor(this.getInterestFixedAmount());
        }

        return this.getInterestRateType().getDescription().getContent();
    }

    @Atomic
    public static InterestRate createForDebitEntry(DebitEntry debitEntry, InterestRate interestRate) {
        if (interestRate != null) {
            return new InterestRate(null, debitEntry, interestRate.getInterestRateType(),
                    interestRate.getNumberOfDaysAfterDueDate(), interestRate.getApplyInFirstWorkday(),
                    interestRate.getMaximumDaysToApplyPenalty(), interestRate.getInterestFixedAmount(), interestRate.getRate());
        }
        return null;
    }

    @Atomic
    public static InterestRate createForDebitEntry(DebitEntry debitEntry, InterestRateType interestRateType,
            int numberOfDaysAfterDueDate, boolean applyInFirstWorkday, int maximumDaysToApplyPenalty,
            BigDecimal interestFixedAmount, BigDecimal rate) {
        return new InterestRate(null, debitEntry, interestRateType, numberOfDaysAfterDueDate, applyInFirstWorkday,
                maximumDaysToApplyPenalty, interestFixedAmount, rate);
    }

}
