package org.fenixedu.treasury.domain.tariff;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class GlobalInterestRateType extends GlobalInterestRateType_Base {

    public static final String DEFAULT_CODE = "GLOBAL_RATE";
    private static final int MAX_YEARS = 5;

    public GlobalInterestRateType() {
        super();
    }

    public GlobalInterestRateType(LocalizedString description) {
        this();

        super.init(description);
        super.setCode(DEFAULT_CODE);

        checkRules();
    }

    protected void checkRules() {
        super.checkRules();

        if (findAll().count() > 1) {
            throw new TreasuryDomainException("error.GlobalInterestRateType.already.exists");
        }
    }

    @Override
    public List<InterestRateBean> calculateInterests(DebitEntry debitEntry, LocalDate paymentDate,
            boolean withAllInterestValues) {
        return calculateInterestAmount(debitEntry, withAllInterestValues, calculateEvents(debitEntry, paymentDate));
    }

    @Override
    public List<InterestRateBean> calculateAllInterestsByLockingAtDate(DebitEntry debitEntry, LocalDate lockDate) {
        return calculateInterestAmount(debitEntry, true,
                calculateEvents(debitEntry, lockDate, lockDate.toDateTimeAtStartOfDay()));
    }

    private List<InterestRateBean> calculateInterestAmount(DebitEntry debitEntry, boolean withAllInterestValues,
            NavigableMap<LocalDate, InterestCalculationEvent> orderedEvents) {
        InterestRateBean result = new InterestRateBean();

        BigDecimal totalInterestAmount = BigDecimal.ZERO;
        int totalOfDays = 0;

        LocalDate key = orderedEvents.firstKey();
        while (orderedEvents.higherKey(key) != null) {
            LocalDate eventDate = orderedEvents.higherKey(key);
            InterestCalculationEvent event = orderedEvents.get(key);

            BigDecimal daysInYear = new BigDecimal(TreasuryConstants.numberOfDaysInYear(key.getYear()));
            BigDecimal amountPerDay = TreasuryConstants.divide(event.amountToPay, daysInYear);
            BigDecimal numberOfDays = new BigDecimal(Days.daysBetween(key, eventDate).getDays());
            BigDecimal partialInterestAmount = event.interestRate.multiply(amountPerDay).multiply(numberOfDays);

            result.addDetail(partialInterestAmount, key, eventDate.minusDays(1), amountPerDay, event.amountToPay,
                    TreasuryConstants.defaultScale(event.interestRate).multiply(TreasuryConstants.HUNDRED_PERCENT)
                            .setScale(4, RoundingMode.HALF_UP));

            totalInterestAmount = totalInterestAmount.add(partialInterestAmount);
            totalOfDays += numberOfDays.intValue();

            key = eventDate;
        }

        if (!withAllInterestValues) {
            for (final Entry<LocalDate, BigDecimal> entry : createdInterestEntriesMap(debitEntry).entrySet()) {
                result.addCreatedInterestEntry(entry.getKey(), entry.getValue());
                totalInterestAmount = totalInterestAmount.subtract(entry.getValue());
            }
        }

        if (TreasuryConstants.isNegative(totalInterestAmount)) {
            totalInterestAmount = BigDecimal.ZERO;
        }

        result.setInterestAmount(Currency.getValueWithScale(totalInterestAmount));
        result.setNumberOfDays(totalOfDays);
        result.setDescription(TreasuryConstants.treasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE,
                "label.InterestRateBean.interest.designation", debitEntry.getDescription()));

        return Collections.singletonList(result);
    }

    private NavigableMap<LocalDate, InterestCalculationEvent> calculateEvents(DebitEntry debitEntry, LocalDate paymentDate) {
        return calculateEvents(debitEntry, paymentDate, null);
    }

    /*
     * The ignorePaymentsAfterDate is not required. If set it will ignore all payments made after that
     * date. It is a way to lock the interest calculation at certain date, which is necessary
     * for processes which lock the interest to be paid
     */
    private NavigableMap<LocalDate, InterestCalculationEvent> calculateEvents(DebitEntry debitEntry, LocalDate paymentDate,
            DateTime ignorePaymentsAfterDate) {
        NavigableMap<LocalDate, BigDecimal> paymentsMap = createPaymentsMap(debitEntry, paymentDate, ignorePaymentsAfterDate);
        LocalDate lastPayment = paymentsMap.lastKey();

        final LocalDate firstDayToChargeInterests = calculateFirstDayToChargeInterests(debitEntry, lastPayment);
        final LocalDate nextDayOfInterestsCharge =
                calculateLastDayToChargeInterests(debitEntry, lastPayment, firstDayToChargeInterests).plusDays(1);

        BigDecimal amountToPayAtFirstDay = amountInDebtAtDay(debitEntry, paymentsMap, firstDayToChargeInterests);
        BigDecimal interestRateAtFirstDay = interestRateValue(firstDayToChargeInterests);

        NavigableMap<LocalDate, InterestCalculationEvent> result = new TreeMap<>();
        result.put(firstDayToChargeInterests, new InterestCalculationEvent(amountToPayAtFirstDay, interestRateAtFirstDay));
        result.put(nextDayOfInterestsCharge,
                new InterestCalculationEvent(BigDecimal.ZERO, interestRateValue(nextDayOfInterestsCharge)));

        paymentsMap.forEach((settlementPaymentDate, paidAmount) -> {
            LocalDate eventDate = settlementPaymentDate.plusDays(1);
            if (eventDate.isBefore(firstDayToChargeInterests)) {
                return;
            }

            if (eventDate.isAfter(nextDayOfInterestsCharge)) {
                return;
            }

            result.putIfAbsent(eventDate, new InterestCalculationEvent(amountInDebtAtDay(debitEntry, paymentsMap, eventDate),
                    interestRateValue(eventDate)));
        });

        getInterestRateEntriesSet().stream().filter(r -> !r.getStartDate().isBefore(firstDayToChargeInterests))
                .filter(r -> !r.getStartDate().isAfter(nextDayOfInterestsCharge)).forEach(r -> {
                    LocalDate eventDate = r.getStartDate();
                    result.putIfAbsent(eventDate, new InterestCalculationEvent(amountInDebtAtDay(debitEntry, paymentsMap, eventDate),
                            interestRateValue(eventDate)));
                });

        return result;
    }

    private NavigableMap<LocalDate, BigDecimal> createPaymentsMap(DebitEntry debitEntry, LocalDate paymentDate) {
        return createPaymentsMap(debitEntry, paymentDate, null);
    }

    /*
     * The ignorePaymentsAfterDate is not required. If set it will ignore all payments made after that
     * date. It is a way to lock the interest calculation at certain date, which is necessary
     * for processes which lock the interest to be paid
     */
    private NavigableMap<LocalDate, BigDecimal> createPaymentsMap(DebitEntry debitEntry, LocalDate paymentDate,
            DateTime ignorePaymentsAfterDate) {
        NavigableMap<LocalDate, BigDecimal> result = new TreeMap<>();

        // ANIL 2025-11-03 (#qubIT-Fenix-7720)
        // The debit entry amount to calculate interest must not
        // take into account the amount that was exempted by credit entry

        Set<CreditEntry> creditEntriesByExemption =
                debitEntry.getTreasuryExemptionsSet().stream().filter(te -> te.getCreditEntry() != null)
                        .filter(te -> !te.getCreditEntry().isAnnulled()).map(te -> te.getCreditEntry())
                        .collect(Collectors.toSet());

        debitEntry.getSettlementEntriesSet().stream() //
                .filter(s -> !s.isAnnulled()) //
                .forEach(se -> {
                    if (ignorePaymentsAfterDate != null && se.getSettlementNote().getPaymentDate()
                            .isAfter(ignorePaymentsAfterDate)) {
                        return;
                    }

                    // ANIL 2025-11-03 (#qubIT-Fenix-7720)
                    BigDecimal amountToDiscountFromCreditEntriesByExemption =
                            se.getSettlementNote().getSettlemetEntriesSet().stream()
                                    .filter(sce -> creditEntriesByExemption.contains(sce.getInvoiceEntry()))
                                    .map(sce -> sce.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

                    LocalDate settlementPaymentDate = se.getSettlementNote().getPaymentDate().toLocalDate();
                    BigDecimal settledAmount =
                            se.getAmount().subtract(amountToDiscountFromCreditEntriesByExemption).max(BigDecimal.ZERO);

                    if (TreasuryConstants.isPositive(settledAmount)) {
                        result.merge(settlementPaymentDate, settledAmount, BigDecimal::add);
                    }
                });

        result.merge(paymentDate, debitEntry.getOpenAmount(), BigDecimal::add);

        return result;
    }

    private LocalDate calculateFirstDayToChargeInterests(DebitEntry debitEntry, LocalDate lastPayment) {
        LocalDate firstDayToChargeInterests =
                applyOnFirstWorkdayIfNecessary(debitEntry, debitEntry.getDueDate().plusDays(numberOfDaysAfterDueDate()));

        // ??? Despacho n.º 5621/2015 » ... » Decreto-lei 73/99 ???
        if (firstDayToChargeInterests.isBefore(lastPayment.minusYears(MAX_YEARS))) {
            firstDayToChargeInterests = lastPayment.minusYears(MAX_YEARS).plusDays(1);
        }

        return firstDayToChargeInterests;
    }

    private LocalDate calculateLastDayToChargeInterests(DebitEntry debitEntry, LocalDate lastPayment,
            LocalDate firstDayToChargeInterests) {
        InterestRate interestRate = debitEntry.getInterestRate();

        LocalDate nextDayOfPaymentDate = lastPayment;
        if (!isApplyPaymentMonth(lastPayment)) {
            nextDayOfPaymentDate = nextDayOfPaymentDate.withDayOfMonth(1).minusDays(1);
        }

        if (interestRate.isMaximumDaysToApplyPenaltyApplied() && Days.daysBetween(firstDayToChargeInterests, nextDayOfPaymentDate)
                .getDays() > interestRate.getMaximumDaysToApplyPenalty()) {
            nextDayOfPaymentDate = firstDayToChargeInterests.plusDays(interestRate.getMaximumDaysToApplyPenalty() - 1);
        }
        return nextDayOfPaymentDate;
    }

    private BigDecimal amountInDebtAtDay(DebitEntry debitEntry, NavigableMap<LocalDate, BigDecimal> paymentsMap,
            LocalDate eventDate) {
        Set<CreditEntry> creditEntriesByExemption =
                debitEntry.getTreasuryExemptionsSet().stream().filter(te -> te.getCreditEntry() != null)
                        .filter(te -> !te.getCreditEntry().isAnnulled()).map(te -> te.getCreditEntry())
                        .collect(Collectors.toSet());

        BigDecimal sumOfCreditEntriesByExemption =
                creditEntriesByExemption.stream().map(CreditEntry::getAmountWithVat).reduce(BigDecimal.ZERO, BigDecimal::add);

        // ANIL 2025-11-03 (#qubIT-Fenix-7720)
        // Subtract the amount exempted by credit entry

        BigDecimal amountToPay = debitEntry.getAmountWithVat().subtract(sumOfCreditEntriesByExemption);

        for (Entry<LocalDate, BigDecimal> entry : paymentsMap.entrySet()) {
            if (!entry.getKey().isBefore(eventDate)) {
                break;
            }

            amountToPay = amountToPay.subtract(entry.getValue()).max(BigDecimal.ZERO);
        }

        return amountToPay;
    }

    private int numberOfDaysAfterDueDate() {
        return 1;
    }

    private LocalDate applyOnFirstWorkdayIfNecessary(DebitEntry debitEntry, final LocalDate date) {
        Optional<InterestRateEntry> globalRate = InterestRateEntry.findUniqueAppliedForDate(this, date);
        if (!globalRate.isPresent()) {
            throw new TreasuryDomainException("error.InterestRate.rate.not.defined.for.date",
                    date.toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
        }

        boolean applyInFirstWorkday = globalRate.get().getApplyInFirstWorkday();

        if (applyInFirstWorkday && isSaturday(date)) {
            return date.plusDays(2);
        } else if (applyInFirstWorkday && isSunday(date)) {
            return date.plusDays(1);
        }

        return date;
    }

    private boolean isApplyPaymentMonth(final LocalDate date) {
        Optional<InterestRateEntry> globalRate = InterestRateEntry.findUniqueAppliedForDate(this, date);
        if (!globalRate.isPresent()) {
            throw new TreasuryDomainException("error.InterestRate.rate.not.defined.for.date",
                    date.toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
        }

        return Boolean.TRUE.equals(globalRate.get().getApplyPaymentMonth());
    }

    private BigDecimal interestRateValue(LocalDate date) {
        Optional<InterestRateEntry> globalRate = InterestRateEntry.findUniqueAppliedForDate(this, date);
        if (!globalRate.isPresent()) {
            throw new TreasuryDomainException("error.InterestRate.rate.not.defined.for.date",
                    date.toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
        }

        return TreasuryConstants.divide(globalRate.get().getRate(), TreasuryConstants.HUNDRED_PERCENT);
    }

    private class InterestCalculationEvent {
        private BigDecimal amountToPay;
        private BigDecimal interestRate;

        InterestCalculationEvent(BigDecimal amountToPay, BigDecimal interestRateAtEventDate) {
            this.amountToPay = amountToPay;
            this.interestRate = interestRateAtEventDate;
        }
    }

    // 
    // SERVICES
    //

    public static LocalizedString getPresentationName() {
        return TreasuryConstants.treasuryBundleI18N("label.GlobalInterestRateType.default.description");
    }

    public static GlobalInterestRateType create(LocalizedString description) {
        return new GlobalInterestRateType(description);
    }

    public static Stream<GlobalInterestRateType> findAll() {
        return FenixFramework.getDomainRoot().getInterestRateTypesSet().stream()
                .filter(type -> type instanceof GlobalInterestRateType).map(GlobalInterestRateType.class::cast);
    }

    public static Optional<GlobalInterestRateType> findUnique() {
        return findAll().findFirst();
    }
}
