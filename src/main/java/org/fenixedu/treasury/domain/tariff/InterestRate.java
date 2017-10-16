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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.util.Constants;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.Months;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;;

public class InterestRate extends InterestRate_Base {

    private static final int MAX_YEARS = 5;

    protected InterestRate() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    protected InterestRate(final Tariff tariff, final DebitEntry debitEntry, final InterestType interestType,
            final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday, final int maximumDaysToApplyPenalty,
            final int maximumMonthsToApplyPenalty, final BigDecimal interestFixedAmount, final BigDecimal rate) {
        this();

        setTariff(tariff);
        setDebitEntry(debitEntry);
        setInterestType(interestType);
        //HACK: Override the numberOfDaysAfterDueDate
        setNumberOfDaysAfterDueDate(1);
//        setNumberOfDaysAfterDueDate(numberOfDaysAfterDueDate);
        setApplyInFirstWorkday(applyInFirstWorkday);
        setMaximumDaysToApplyPenalty(maximumDaysToApplyPenalty);
        setMaximumMonthsToApplyPenalty(maximumMonthsToApplyPenalty);
        setInterestFixedAmount(interestFixedAmount);
        setRate(rate);

        checkRules();
    }

    private void checkRules() {

        if (getTariff() == null && getDebitEntry() == null) {
            throw new TreasuryDomainException("error.InterestRate.product.or.debit.entry.required");
        }

        if (getTariff() != null && getDebitEntry() != null) {
            throw new TreasuryDomainException("error.InterestRate.product.or.debit.entry.only.one");
        }

        if (getInterestType() == null) {
            throw new TreasuryDomainException("error.InterestRate.interestType.required");
        }

        if ((getInterestType().isDaily() || getInterestType().isMonthly()) && getRate() == null) {
            throw new TreasuryDomainException("error.InterestRate.rate.required");
        }

        if (getInterestType().isFixedAmount() && getInterestFixedAmount() == null) {
            throw new TreasuryDomainException("error.InterestRate.interestFixedAmount.required");
        }
    }

    public boolean isMaximumDaysToApplyPenaltyApplied() {
        return getMaximumDaysToApplyPenalty() > 0;
    }

    public boolean isMaximumMonthsToApplyPenaltyApplied() {
        return getMaximumMonthsToApplyPenalty() > 0;
    }

    public boolean isApplyInFirstWorkday() {
        return getApplyInFirstWorkday();
    }

    @Atomic
    public void edit(final InterestType interestType, final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday,
            final int maximumDaysToApplyPenalty, final int maximumMonthsToApplyPenalty, final BigDecimal interestFixedAmount,
            final BigDecimal rate) {

        setInterestType(interestType);
        //HACK: For now override the NumberOfDays - 01/07/2015
        setNumberOfDaysAfterDueDate(1);
        setApplyInFirstWorkday(applyInFirstWorkday);
        setMaximumDaysToApplyPenalty(maximumDaysToApplyPenalty);
        setMaximumMonthsToApplyPenalty(maximumMonthsToApplyPenalty);
        setInterestFixedAmount(interestFixedAmount);
        setRate(rate);

        checkRules();
    }

    public InterestRateBean calculateInterest(final Map<LocalDate, BigDecimal> amountInDebtMap,
            final Map<LocalDate, BigDecimal> createdInterestEntries, final LocalDate dueDate, final LocalDate paymentDate) {

        TreeMap<LocalDate, BigDecimal> sortedMap = new TreeMap<LocalDate, BigDecimal>();
        sortedMap.putAll(amountInDebtMap);
        
        if(getInterestType().isGlobalRate() && !GlobalInterestRate.findUniqueByYear(dueDate.getYear()).get().isApplyPaymentMonth() ) {
            final LocalDate lastDayForInterestCalculation = paymentDate.withField(DateTimeFieldType.dayOfMonth(), 1).minusDays(1);
            
            for (final LocalDate localDate : Sets.newTreeSet(sortedMap.keySet())) {
                if(localDate.isAfter(lastDayForInterestCalculation)) {
                    sortedMap.remove(localDate);
                }
            }
            
            sortedMap.put(lastDayForInterestCalculation, BigDecimal.ZERO);
        } else {
            sortedMap.put(paymentDate, BigDecimal.ZERO);
        }

        sortedMap = splitDatesWithYearsSpan(sortedMap);
        
        if (getInterestType().isFixedAmount()) {
            return calculedForFixedAmount();
        }

        if (getInterestType().isDaily() || getInterestType().isGlobalRate()) {
            return calculateDaily(createdInterestEntries, dueDate, paymentDate, sortedMap);
        }

        if (getInterestType().isMonthly()) {
            return calculateMonthly(createdInterestEntries, dueDate, paymentDate, sortedMap);
        }

        throw new RuntimeException("unknown interest type formula");
    }

    private TreeMap<LocalDate, BigDecimal> splitDatesWithYearsSpan(final TreeMap<LocalDate, BigDecimal> sortedMap) {
        
        final TreeMap<LocalDate, BigDecimal> result = new TreeMap<LocalDate, BigDecimal>();
        LocalDate startDate = null;
        
        for (final Entry<LocalDate, BigDecimal> entry : sortedMap.entrySet()) {
            if(startDate == null) {
                result.put(entry.getKey(), entry.getValue());
                startDate = entry.getKey();
                continue;
            }
            
            if(startDate.getYear() == entry.getKey().getYear()) {
                result.put(entry.getKey(), entry.getValue());
                startDate = entry.getKey();
                continue;
            }
            
            if(startDate.getYear() == entry.getKey().getYear() - 1) {
                result.put(entry.getKey(), entry.getValue());
                startDate = entry.getKey();
                continue;                                
            }
            
            for(LocalDate a = startDate.plusYears(1), b = entry.getKey(); a.getYear() < b.getYear();) {
                result.put(Constants.firstDayInYear(a.getYear()), sortedMap.get(startDate));
                a = a.plusYears(1);
            }
            
            result.put(entry.getKey(), entry.getValue());
            startDate = entry.getKey();
        }
        
        return result;
    }

    private InterestRateBean calculateMonthly(final Map<LocalDate, BigDecimal> createdInterestEntries, final LocalDate dueDate,
            final LocalDate paymentDate, final TreeMap<LocalDate, BigDecimal> sortedMap) {
        LocalDate dueDateToUse = dueDate;
        if(dueDate.isBefore(paymentDate.minusYears(MAX_YEARS))) {
            dueDateToUse = paymentDate.minusYears(MAX_YEARS);
        }

        final InterestRateBean result = new InterestRateBean(getInterestType());

        BigDecimal totalInterestAmount = BigDecimal.ZERO;
        int totalOfMonths = 0;

        LocalDate startDate = dueDateToUse.plusMonths(1).withDayOfMonth(1);

        // Iterate over amountInDebtMap and calculate amountToPay
        BigDecimal amountInDebt = null;
        for (final Entry<LocalDate, BigDecimal> entry : sortedMap.entrySet()) {

            if (entry.getKey().isAfter(paymentDate)) {
                break;
            }

            if (entry.getKey().isBefore(startDate)) {
                amountInDebt = entry.getValue();
                continue;
            }

            final LocalDate endDate = entry.getKey();
            final int numberOfMonths = Months.monthsBetween(startDate, endDate).getMonths();
            final BigDecimal amountByRate = Constants.divide(getRate(), Constants.HUNDRED_PERCENT).multiply(amountInDebt);
            final BigDecimal partialInterestAmount = amountByRate.multiply(new BigDecimal(numberOfMonths));

            if (Constants.isPositive(partialInterestAmount)) {
                result.addDetail(partialInterestAmount, startDate, endDate, amountByRate, amountInDebt);
            }

            totalInterestAmount = totalInterestAmount.add(partialInterestAmount);
            totalOfMonths += numberOfMonths;

            amountInDebt = entry.getValue();
            startDate = endDate;
        }

        if (createdInterestEntries != null) {
            final TreeMap<LocalDate, BigDecimal> interestSortedMap = new TreeMap<LocalDate, BigDecimal>();
            interestSortedMap.putAll(createdInterestEntries);

            for (final Entry<LocalDate, BigDecimal> entry : createdInterestEntries.entrySet()) {
                result.addCreatedInterestEntry(entry.getKey(), entry.getValue());

                totalInterestAmount = totalInterestAmount.subtract(entry.getValue());
            }
        }

        result.setInterestAmount(getRelatedCurrency().getValueWithScale(totalInterestAmount));
        result.setNumberOfMonths(totalOfMonths);

        return result;
    }

    private InterestRateBean calculateDaily(final Map<LocalDate, BigDecimal> createdInterestEntries, final LocalDate dueDate,
            final LocalDate paymentDate, final TreeMap<LocalDate, BigDecimal> sortedMap) {
        LocalDate dueDateToUse = dueDate;
        if(dueDate.isBefore(paymentDate.minusYears(MAX_YEARS))) {
            dueDateToUse = paymentDate.minusYears(MAX_YEARS);
        }

        final InterestRateBean result = new InterestRateBean(getInterestType());

        BigDecimal totalInterestAmount = BigDecimal.ZERO;
        int totalOfDays = 0;

        LocalDate startDate = applyOnFirstWorkdayIfNecessary(dueDateToUse.plusDays(numberOfDaysAfterDueDate(dueDateToUse.getYear())));

        // Iterate over amountInDebtMap and calculate amountToPay
        BigDecimal amountInDebt = BigDecimal.ZERO;
        for (final Entry<LocalDate, BigDecimal> entry : sortedMap.entrySet()) {

            if (entry.getKey().isAfter(paymentDate)) {
                break;
            }

            if (entry.getKey().isBefore(startDate)) {
                amountInDebt = entry.getValue();
                continue;
            }

            final LocalDate endDate = entry.getKey();

            int numberOfDays = 0;
            BigDecimal partialInterestAmount;
            if (startDate.getYear() != endDate.getYear()) {
                boolean reachedMaxDays = false;

                int firstYearDays = Days.daysBetween(startDate, Constants.lastDayInYear(startDate.getYear())).getDays() + 1;
                int secondYearDays = Days.daysBetween(Constants.firstDayInYear(endDate.getYear()), endDate).getDays() + 1;

                {
                    if (isMaximumDaysToApplyPenaltyApplied() && totalOfDays + firstYearDays >= getMaximumDaysToApplyPenalty()) {
                        firstYearDays = getMaximumDaysToApplyPenalty() - totalOfDays;
                        reachedMaxDays = true;
                    }

                    final BigDecimal amountPerDay =
                            Constants.divide(amountInDebt, new BigDecimal(Constants.numberOfDaysInYear(startDate.getYear())));

                    final BigDecimal rate = interestRate(startDate.getYear());

                    partialInterestAmount =
                            Constants.divide(rate, Constants.HUNDRED_PERCENT).multiply(amountPerDay)
                                    .multiply(new BigDecimal(firstYearDays));

                    numberOfDays += firstYearDays;

                    if (Constants.isPositive(partialInterestAmount)) {
                        result.addDetail(partialInterestAmount, startDate, Constants.lastDayInYear(startDate.getYear()),
                                amountPerDay, amountInDebt);
                    }
                }

                if (!reachedMaxDays) {

                    if (isMaximumDaysToApplyPenaltyApplied()
                            && totalOfDays + firstYearDays + secondYearDays >= getMaximumDaysToApplyPenalty()) {
                        secondYearDays = getMaximumDaysToApplyPenalty() - totalOfDays - firstYearDays;
                    }

                    final BigDecimal amountPerDay =
                            Constants.divide(amountInDebt, new BigDecimal(Constants.numberOfDaysInYear(endDate.getYear())));

                    final BigDecimal rate = interestRate(endDate.getYear());

                    final BigDecimal secondInterestAmount =
                            Constants.divide(rate, Constants.HUNDRED_PERCENT).multiply(amountPerDay)
                                    .multiply(new BigDecimal(secondYearDays));

                    if (Constants.isPositive(partialInterestAmount)) {
                        result.addDetail(secondInterestAmount, Constants.firstDayInYear(endDate.getYear()), endDate,
                                amountPerDay, amountInDebt);
                    }

                    partialInterestAmount = partialInterestAmount.add(secondInterestAmount);

                    numberOfDays += secondYearDays;
                }

            } else {
                numberOfDays = Days.daysBetween(startDate, endDate).getDays() + 1;

                if (isMaximumDaysToApplyPenaltyApplied() && totalOfDays + numberOfDays >= getMaximumDaysToApplyPenalty()) {
                    numberOfDays = getMaximumDaysToApplyPenalty() - totalOfDays;
                }

                final BigDecimal amountPerDay =
                        Constants.divide(amountInDebt, new BigDecimal(Constants.numberOfDaysInYear(startDate.getYear())));

                final BigDecimal rate = interestRate(startDate.getYear());
                partialInterestAmount =
                        Constants.divide(rate, Constants.HUNDRED_PERCENT).multiply(amountPerDay)
                                .multiply(new BigDecimal(numberOfDays));

                if (Constants.isPositive(partialInterestAmount)) {
                    result.addDetail(partialInterestAmount, startDate, endDate, amountPerDay, amountInDebt);
                }
            }

            totalInterestAmount = totalInterestAmount.add(partialInterestAmount);
            totalOfDays += numberOfDays;

            amountInDebt = entry.getValue();
            startDate = endDate.plusDays(1);

            if (isMaximumDaysToApplyPenaltyApplied() && totalOfDays >= getMaximumDaysToApplyPenalty()) {
                break;
            }
        }

        if (createdInterestEntries != null) {
            final TreeMap<LocalDate, BigDecimal> interestSortedMap = new TreeMap<LocalDate, BigDecimal>();
            interestSortedMap.putAll(createdInterestEntries);

            for (final Entry<LocalDate, BigDecimal> entry : createdInterestEntries.entrySet()) {
                result.addCreatedInterestEntry(entry.getKey(), entry.getValue());

                totalInterestAmount = totalInterestAmount.subtract(entry.getValue());
            }
        }

        result.setInterestAmount(getRelatedCurrency().getValueWithScale(totalInterestAmount));
        result.setNumberOfDays(totalOfDays);

        return result;
    }

    private int numberOfDaysAfterDueDate(int year) {
        if(getInterestType().isGlobalRate()) {
            return 1;
        }
        
        return getNumberOfDaysAfterDueDate();
    }

    private Currency getRelatedCurrency() {
        if (getTariff() != null) {
            return getTariff().getFinantialEntity().getFinantialInstitution().getCurrency();
        } else if (getDebitEntry() != null) {
            return getDebitEntry().getCurrency();
        }
        return null;
    }

    private BigDecimal interestRate(final int year) {
        if (getInterestType().isGlobalRate()) {
            if (!GlobalInterestRate.findUniqueByYear(year).isPresent()) {
                throw new TreasuryDomainException("error.InterestRate.global.interest.rate.not.found", String.valueOf(year));
            };

            return GlobalInterestRate.findUniqueByYear(year).get().getRate();
        }

        return getRate();
    }

    private InterestRateBean calculedForFixedAmount() {
        final InterestRateBean result = new InterestRateBean(getInterestType());
        result.setInterestAmount(this.getRelatedCurrency().getValueWithScale(getInterestFixedAmount()));

        return result;
    }

    private LocalDate applyOnFirstWorkdayIfNecessary(final LocalDate date) {
        boolean applyInFirstWorkday = isApplyInFirstWorkday();
        
        if (getInterestType().isGlobalRate()) {
            if (!GlobalInterestRate.findUniqueByYear(date.getYear()).isPresent()) {
                throw new TreasuryDomainException("error.InterestRate.global.interest.rate.not.found", String.valueOf(date.getYear()));
            };

            applyInFirstWorkday = GlobalInterestRate.findUniqueByYear(date.getYear()).get().getApplyInFirstWorkday();
        }
        
        if (applyInFirstWorkday && isSaturday(date)) {
            return date.plusDays(2);
        } else if (applyInFirstWorkday && isSunday(date)) {
            return date.plusDays(1);
        }

        return date;
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
        deleteDomainObject();
    }

    private boolean isSaturday(final LocalDate date) {
        return date.getDayOfWeek() == DateTimeConstants.SATURDAY;
    }

    private boolean isSunday(final LocalDate date) {
        return date.getDayOfWeek() == DateTimeConstants.SUNDAY;
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<InterestRate> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getInterestRatesSet().stream();
    }

    @Atomic
    public static InterestRate createForTariff(final Tariff tariff, final InterestType interestType,
            final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday, final int maximumDaysToApplyPenalty,
            final int maximumMonthsToApplyPenalty, final BigDecimal interestFixedAmount, final BigDecimal rate) {
        return new InterestRate(tariff, null, interestType, numberOfDaysAfterDueDate, applyInFirstWorkday,
                maximumDaysToApplyPenalty, maximumMonthsToApplyPenalty, interestFixedAmount, rate);
    }

    public String getUiFullDescription() {
        //HACK: This should be moved to the Presentation Layer, but here is easier
        switch (this.getInterestType()) {
        case DAILY:
            return this.getInterestType().getDescriptionI18N().getContent() + "-" + this.getRate() + "% (Max. Dias="
                    + this.getMaximumDaysToApplyPenalty() + ", Aplica 1º Dia Útil=" + this.getApplyInFirstWorkday()
                    + ", Dias após Vencimento=" + this.getNumberOfDaysAfterDueDate() + ")";
        case FIXED_AMOUNT:
            return this.getInterestType().getDescriptionI18N().getContent() + "-"
                    + getRelatedCurrency().getValueFor(this.getInterestFixedAmount());
        case GLOBAL_RATE:
            return this.getInterestType().getDescriptionI18N().getContent();
        case MONTHLY:
            return this.getInterestType().getDescriptionI18N().getContent() + "-" + this.getRate() + "% (Max. Meses="
                    + this.getMaximumMonthsToApplyPenalty() + ")";
        default:
            return this.getInterestType().getDescriptionI18N().getContent();
        }
    }

    @Atomic
    public static InterestRate createForDebitEntry(DebitEntry debitEntry, InterestRate interestRate) {
        if (interestRate != null) {
            return new InterestRate(null, debitEntry, interestRate.getInterestType(), interestRate.getNumberOfDaysAfterDueDate(),
                    interestRate.getApplyInFirstWorkday(), interestRate.getMaximumDaysToApplyPenalty(),
                    interestRate.getMaximumMonthsToApplyPenalty(), interestRate.getInterestFixedAmount(), interestRate.getRate());
        }
        return null;
    }

    @Atomic
    public static InterestRate createForDebitEntry(final DebitEntry debitEntry, final InterestType interestType,
            final int numberOfDaysAfterDueDate, final boolean applyInFirstWorkday, final int maximumDaysToApplyPenalty,
            final int maximumMonthsToApplyPenalty, final BigDecimal interestFixedAmount, final BigDecimal rate) {
        return new InterestRate(null, debitEntry, interestType, numberOfDaysAfterDueDate, applyInFirstWorkday,
                maximumDaysToApplyPenalty, maximumMonthsToApplyPenalty, interestFixedAmount, rate);
    }
}
