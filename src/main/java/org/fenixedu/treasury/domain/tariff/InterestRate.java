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
//        if (getInterestType().isFixedAmount()) {
//            return calculateForFixedAmount(withAllInterestValues);
//        }
//
//        return calculateInterestAmount(withAllInterestValues, calculateEvents(paymentDate));
    }

    /*
     * Calculate the interest rate at certain date and disregard any payments made after that date.
     * This interest calculation is necessary in processes where the interest rate is calculated
     * in advance and fixed by agreement, and cannot change due to payments made after that.
     */
    public List<InterestRateBean> calculateAllInterestsByLockingAtDate(LocalDate lockDate) {
        return getInterestRateType().calculateAllInterestsByLockingAtDate(getDebitEntry(), lockDate);
//        if (getInterestType().isFixedAmount()) {
//            return calculateForFixedAmount(true);
//        }
//
//        return calculateInterestAmount(true, calculateEvents(lockDate, lockDate.toDateTimeAtStartOfDay()));
    }

//    private InterestRateBean calculateInterestAmount(boolean withAllInterestValues,
//            NavigableMap<LocalDate, InterestCalculationEvent> orderedEvents) {
//        InterestRateBean result = new InterestRateBean();
//
//        BigDecimal totalInterestAmount = BigDecimal.ZERO;
//        int totalOfDays = 0;
//
//        LocalDate key = orderedEvents.firstKey();
//        while (orderedEvents.higherKey(key) != null) {
//            LocalDate eventDate = orderedEvents.higherKey(key);
//            InterestCalculationEvent event = orderedEvents.get(key);
//
//            BigDecimal daysInYear = new BigDecimal(TreasuryConstants.numberOfDaysInYear(key.getYear()));
//            BigDecimal amountPerDay = TreasuryConstants.divide(event.amountToPay, daysInYear);
//            BigDecimal numberOfDays = new BigDecimal(Days.daysBetween(key, eventDate).getDays());
//            BigDecimal partialInterestAmount = event.interestRate.multiply(amountPerDay).multiply(numberOfDays);
//
//            result.addDetail(partialInterestAmount, key, eventDate.minusDays(1), amountPerDay, event.amountToPay,
//                    TreasuryConstants.defaultScale(event.interestRate).multiply(TreasuryConstants.HUNDRED_PERCENT).setScale(4,
//                            RoundingMode.HALF_UP));
//
//            totalInterestAmount = totalInterestAmount.add(partialInterestAmount);
//            totalOfDays += numberOfDays.intValue();
//
//            key = eventDate;
//        }
//
//        if (!withAllInterestValues) {
//            for (final Entry<LocalDate, BigDecimal> entry : createdInterestEntriesMap().entrySet()) {
//                result.addCreatedInterestEntry(entry.getKey(), entry.getValue());
//                totalInterestAmount = totalInterestAmount.subtract(entry.getValue());
//            }
//        }
//
//        if (TreasuryConstants.isNegative(totalInterestAmount)) {
//            totalInterestAmount = BigDecimal.ZERO;
//        }
//
//        result.setInterestAmount(getRelatedCurrency().getValueWithScale(totalInterestAmount));
//        result.setNumberOfDays(totalOfDays);
//
//        return result;
//    }

//    private TreeMap<LocalDate, BigDecimal> createdInterestEntriesMap() {
//        TreeMap<LocalDate, BigDecimal> result = new TreeMap<LocalDate, BigDecimal>();
//
//        for (DebitEntry interestDebitEntry : getDebitEntry().getInterestDebitEntriesSet()) {
//            if (interestDebitEntry.isAnnulled()) {
//                continue;
//            }
//
//            if (!TreasuryConstants.isPositive(interestDebitEntry.getAvailableAmountForCredit())) {
//                continue;
//            }
//
//            LocalDate interestEntryDateTime = interestDebitEntry.getEntryDateTime().toLocalDate();
//            result.putIfAbsent(interestEntryDateTime, BigDecimal.ZERO);
//            result.put(interestEntryDateTime,
//                    result.get(interestEntryDateTime).add(interestDebitEntry.getAvailableAmountForCredit()));
//        }
//
//        return result;
//    }

//    private NavigableMap<LocalDate, InterestCalculationEvent> calculateEvents(LocalDate paymentDate) {
//        return calculateEvents(paymentDate, null);
//    }

    /*
     * The ignorePaymentsAfterDate is not required. If set it will ignore all payments made after that
     * date. It is a way to lock the interest calculation at certain date, which is necessary
     * for processes which lock the interest to be paid
     */
//    private NavigableMap<LocalDate, InterestCalculationEvent> calculateEvents(LocalDate paymentDate, DateTime ignorePaymentsAfterDate) {
//        NavigableMap<LocalDate, BigDecimal> paymentsMap = createPaymentsMap(paymentDate, ignorePaymentsAfterDate);
//        LocalDate lastPayment = paymentsMap.lastKey();
//
//        final LocalDate firstDayToChargeInterests = calculateFirstDayToChargeInterests(lastPayment);
//        final LocalDate nextDayOfInterestsCharge =
//                calculateLastDayToChargeInterests(lastPayment, firstDayToChargeInterests).plusDays(1);
//
//        BigDecimal amountToPayAtFirstDay = amountInDebtAtDay(paymentsMap, firstDayToChargeInterests.minusDays(1));
//        BigDecimal interestRateAtFirstDay = interestRateValue(firstDayToChargeInterests);
//
//        NavigableMap<LocalDate, InterestCalculationEvent> result = new TreeMap<>();
//        result.put(firstDayToChargeInterests, new InterestCalculationEvent(amountToPayAtFirstDay, interestRateAtFirstDay));
//        result.put(nextDayOfInterestsCharge,
//                new InterestCalculationEvent(BigDecimal.ZERO, interestRateValue(nextDayOfInterestsCharge)));
//
//        paymentsMap.forEach((settlementPaymentDate, paidAmount) -> {
//            LocalDate eventDate = settlementPaymentDate.plusDays(1);
//            if (eventDate.isBefore(firstDayToChargeInterests)) {
//                return;
//            }
//
//            if (eventDate.isAfter(nextDayOfInterestsCharge)) {
//                return;
//            }
//
//            result.putIfAbsent(eventDate,
//                    new InterestCalculationEvent(amountInDebtAtDay(paymentsMap, eventDate), interestRateValue(eventDate)));
//        });
//
//        GlobalInterestRate.findAll().filter(r -> !r.getFirstDay().isBefore(firstDayToChargeInterests))
//                .filter(r -> !r.getFirstDay().isAfter(nextDayOfInterestsCharge)).forEach(r -> {
//                    LocalDate eventDate = r.getFirstDay();
//                    result.putIfAbsent(eventDate, new InterestCalculationEvent(amountInDebtAtDay(paymentsMap, eventDate),
//                            interestRateValue(eventDate)));
//                });
//
//        return result;
//    }

//    private LocalDate calculateFirstDayToChargeInterests(LocalDate lastPayment) {
//        LocalDate firstDayToChargeInterests =
//                applyOnFirstWorkdayIfNecessary(getDebitEntry().getDueDate().plusDays(numberOfDaysAfterDueDate()));
//
//        if (firstDayToChargeInterests.isBefore(lastPayment.minusYears(MAX_YEARS))) {
//            firstDayToChargeInterests = lastPayment.minusYears(MAX_YEARS).plusDays(1);
//        }
//
//        return firstDayToChargeInterests;
//    }
//
//    private LocalDate calculateLastDayToChargeInterests(LocalDate lastPayment, LocalDate firstDayToChargeInterests) {
//        LocalDate nextDayOfPaymentDate = lastPayment;
//        if (!isApplyPaymentMonth(lastPayment)) {
//            nextDayOfPaymentDate = nextDayOfPaymentDate.withDayOfMonth(1).minusDays(1);
//        }
//
//        if (isMaximumDaysToApplyPenaltyApplied()
//                && Days.daysBetween(firstDayToChargeInterests, nextDayOfPaymentDate).getDays() > getMaximumDaysToApplyPenalty()) {
//            nextDayOfPaymentDate = firstDayToChargeInterests.plusDays(getMaximumDaysToApplyPenalty() - 1);
//        }
//        return nextDayOfPaymentDate;
//    }
//
//    private BigDecimal amountInDebtAtDay(NavigableMap<LocalDate, BigDecimal> paymentsMap, LocalDate eventDate) {
//        BigDecimal amountToPay = getDebitEntry().getAmountWithVat();
//
//        for (Entry<LocalDate, BigDecimal> entry : paymentsMap.entrySet()) {
//            if (!entry.getKey().isBefore(eventDate)) {
//                break;
//            }
//
//            amountToPay = amountToPay.subtract(entry.getValue());
//        }
//
//        return amountToPay;
//    }
//
//    private NavigableMap<LocalDate, BigDecimal> createPaymentsMap(LocalDate paymentDate) {
//        return createPaymentsMap(paymentDate, null);
//    }

    /*
     * The ignorePaymentsAfterDate is not required. If set it will ignore all payments made after that
     * date. It is a way to lock the interest calculation at certain date, which is necessary
     * for processes which lock the interest to be paid
     */
//    private NavigableMap<LocalDate, BigDecimal> createPaymentsMap(LocalDate paymentDate, DateTime ignorePaymentsAfterDate) {
//        NavigableMap<LocalDate, BigDecimal> result = new TreeMap<>();
//
//        getDebitEntry().getSettlementEntriesSet().stream() //
//                .filter(s -> !s.isAnnulled()) //
//                .forEach(s -> {
//                    if (ignorePaymentsAfterDate != null
//                            && s.getSettlementNote().getPaymentDate().isAfter(ignorePaymentsAfterDate)) {
//                        return;
//                    }
//
//                    LocalDate settlementPaymentDate = s.getSettlementNote().getPaymentDate().toLocalDate();
//                    result.putIfAbsent(settlementPaymentDate, BigDecimal.ZERO);
//                    result.put(settlementPaymentDate, result.get(settlementPaymentDate).add(s.getAmount()));
//                });
//
//        result.putIfAbsent(paymentDate, BigDecimal.ZERO);
//        result.put(paymentDate, result.get(paymentDate).add(getDebitEntry().getOpenAmount()));
//
//        return result;
//    }
//
//    private boolean isApplyPaymentMonth(final LocalDate date) {
//        if (!getInterestType().isGlobalRate()) {
//            return false;
//        }
//
//        Optional<GlobalInterestRate> globalRate = GlobalInterestRate.findUniqueAppliedForDate(date);
//        if (!globalRate.isPresent()) {
//            throw new TreasuryDomainException("error.InterestRate.rate.not.defined.for.date",
//                    date.toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
//        }
//
//        return globalRate.get().isApplyPaymentMonth();
//    }
//
//    private BigDecimal interestRateValue(LocalDate date) {
//        if (getInterestType().isGlobalRate()) {
//            Optional<GlobalInterestRate> globalRate = GlobalInterestRate.findUniqueAppliedForDate(date);
//            if (!globalRate.isPresent()) {
//                throw new TreasuryDomainException("error.InterestRate.rate.not.defined.for.date",
//                        date.toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
//            }
//            return TreasuryConstants.divide(globalRate.get().getRate(), TreasuryConstants.HUNDRED_PERCENT);
//        }
//
//        return TreasuryConstants.divide(getRate(), TreasuryConstants.HUNDRED_PERCENT);
//    }
//
//    private int numberOfDaysAfterDueDate() {
//        if (getInterestType().isGlobalRate()) {
//            return 1;
//        }
//
//        return getNumberOfDaysAfterDueDate();
//    }

    private Currency getRelatedCurrency() {
        if (getTariff() != null) {
            return getTariff().getFinantialEntity().getFinantialInstitution().getCurrency();
        } else if (getDebitEntry() != null) {
            return getDebitEntry().getCurrency();
        }
        return null;
    }

//    private InterestRateBean calculateForFixedAmount(boolean withAllInterestValues) {
//        final InterestRateBean result = new InterestRateBean(getInterestType());
//        BigDecimal totalInterestAmount = this.getRelatedCurrency().getValueWithScale(getInterestFixedAmount());
//
//        if (!withAllInterestValues) {
//            for (final Entry<LocalDate, BigDecimal> entry : createdInterestEntriesMap().entrySet()) {
//                result.addCreatedInterestEntry(entry.getKey(), entry.getValue());
//                totalInterestAmount = totalInterestAmount.subtract(entry.getValue());
//            }
//        }
//
//        if (TreasuryConstants.isNegative(totalInterestAmount)) {
//            totalInterestAmount = BigDecimal.ZERO;
//        }
//
//        result.setInterestAmount(getRelatedCurrency().getValueWithScale(totalInterestAmount));
//        return result;
//    }
//
//    private LocalDate applyOnFirstWorkdayIfNecessary(final LocalDate date) {
//        boolean applyInFirstWorkday = isApplyInFirstWorkday();
//
//        if (getInterestType().isGlobalRate()) {
//            Optional<GlobalInterestRate> globalRate = GlobalInterestRate.findUniqueAppliedForDate(date);
//            if (!globalRate.isPresent()) {
//                throw new TreasuryDomainException("error.InterestRate.rate.not.defined.for.date",
//                        date.toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
//            }
//
//            applyInFirstWorkday = globalRate.get().getApplyInFirstWorkday();
//        }
//
//        if (applyInFirstWorkday && isSaturday(date)) {
//            return date.plusDays(2);
//        } else if (applyInFirstWorkday && isSunday(date)) {
//            return date.plusDays(1);
//        }
//
//        return date;
//    }

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

//    private boolean isSaturday(final LocalDate date) {
//        return date.getDayOfWeek() == DateTimeConstants.SATURDAY;
//    }
//
//    private boolean isSunday(final LocalDate date) {
//        return date.getDayOfWeek() == DateTimeConstants.SUNDAY;
//    }

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

//    private class InterestCalculationEvent {
//        private BigDecimal amountToPay;
//        private BigDecimal interestRate;
//
//        InterestCalculationEvent(BigDecimal amountToPay, BigDecimal interestRateAtEventDate) {
//            this.amountToPay = amountToPay;
//            this.interestRate = interestRateAtEventDate;
//        }
//    }
}
