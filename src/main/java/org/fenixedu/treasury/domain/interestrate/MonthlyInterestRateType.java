package org.fenixedu.treasury.domain.interestrate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.tariff.InterestRateEntry;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.joda.time.Months;

public class MonthlyInterestRateType extends MonthlyInterestRateType_Base {

    private static final int DEFAULT_MAXIMUM_MONTHS_TO_APPLY_INTEREST = 10;

    public MonthlyInterestRateType() {
        super();
    }

    public MonthlyInterestRateType(LocalizedString description) {
        this();

        super.init(description);
        super.setCode(UUID.randomUUID().toString());

        checkRules();
    }

    @Override
    public List<InterestRateBean> calculateInterests(DebitEntry debitEntry, LocalDate paymentDate,
            boolean withAllInterestValues) {

        DateTime entryDateTime = debitEntry.getEntryDateTime();

        // get the interest rate entry, valid at the debit entry date
        final InterestRateEntry rateEntry = InterestRateEntry.findUniqueAppliedForDate(this, entryDateTime.toLocalDate())
                .orElseThrow(() -> new TreasuryDomainException("error.InterestRate.rate.not.defined.for.date",
                        entryDateTime.toLocalDate().toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD)));

        MonthlyInterestRateConfig config = new MonthlyInterestRateConfig() {

            @Override
            public boolean isPostponePaymentLimitDateToFirstWorkDate() {
                // TODO ANIL 2023-06-16: Create a property for this
                // The property applyInFirstWorkday should be used in the cases where the
                // the first day to apply penalty should be in a work day, not in holiday or weekend

                return rateEntry.getApplyInFirstWorkday();
            }

            @Override
            public boolean isApplyPenaltyInFirstWorkday() {
                return rateEntry.getApplyInFirstWorkday();
            }

            @Override
            public int getMaximumMonthsToApplyInterests() {
                return DEFAULT_MAXIMUM_MONTHS_TO_APPLY_INTEREST;
            }

            @Override
            public BigDecimal getInterestsPercentage() {
                return rateEntry.getRate();
            }

            @Override
            public LocalDate getOverridenFirstDayToApplyInterests(int month) {
                return null;
            }

            public LocalizedString getInterestDebitEntryFormat() {
                return new LocalizedString(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale(),
                        "${debitEntryDescription} (in ${monthOfPenaltyDate})");
            };
        };

        return calculateInterests(debitEntry, paymentDate, withAllInterestValues, config);
    }

    @Override
    public List<InterestRateBean> calculateAllInterestsByLockingAtDate(DebitEntry debitEntry, LocalDate lockDate) {
        return calculateInterests(debitEntry, lockDate, true);
    }

    public List<InterestRateBean> calculateInterests(DebitEntry debitEntry, LocalDate paymentDate, boolean withAllInterestValues,
            MonthlyInterestRateConfig monthlyInterestRateConfig) {
        BigDecimal interestsPercentage = monthlyInterestRateConfig.getInterestsPercentage();
        boolean postponePaymentLimitDateToFirstWorkDate = monthlyInterestRateConfig.isPostponePaymentLimitDateToFirstWorkDate();
        boolean applyPenaltyInFirstWorkday = monthlyInterestRateConfig.isApplyPenaltyInFirstWorkday();
        int maximumMonthsToApplyInterests = monthlyInterestRateConfig.getMaximumMonthsToApplyInterests();

        List<LocalDate> monthsIterationList =
                getMonthlyDueDateIterations(debitEntry.getDueDate(), paymentDate, maximumMonthsToApplyInterests);

        return monthsIterationList.stream() //
                .map(monthIteration -> calculateInterestRateBean(debitEntry, paymentDate, withAllInterestValues, monthIteration,
                        interestsPercentage, postponePaymentLimitDateToFirstWorkDate, applyPenaltyInFirstWorkday,
                        monthlyInterestRateConfig.getOverridenFirstDayToApplyInterests(monthIteration.getMonthOfYear()),
                        monthlyInterestRateConfig.getInterestDebitEntryFormat())) //
                .filter(Objects::nonNull) //
                .collect(Collectors.toList());
    }

    private InterestRateBean calculateInterestRateBean(DebitEntry debitEntry, LocalDate paymentDate,
            boolean withAllInterestValues, LocalDate dueDate, BigDecimal interestsPercentage,
            boolean postponePaymentLimitDateToFirstWorkDate, boolean applyPenaltyInFirstWorkday,
            LocalDate overridenFirstDayToApplyPenalty, LocalizedString interestDebitEntryFormat) {

        LocalDate firstDayToApplyInterests = calculateFirstDateToApplyInterests(debitEntry,
                postponePaymentLimitDateToFirstWorkDate, applyPenaltyInFirstWorkday);

        LocalDate firstDayToApplyInterestsInMonth = overridenFirstDayToApplyPenalty;

        if (firstDayToApplyInterestsInMonth == null) {
            firstDayToApplyInterestsInMonth = calculateFirstDateToApplyInterests(debitEntry, dueDate,
                    postponePaymentLimitDateToFirstWorkDate, applyPenaltyInFirstWorkday);
        }

        if (firstDayToApplyInterestsInMonth.isAfter(paymentDate)) {
            return null;
        }

        BigDecimal interestAmount = Currency.getValueWithScale(debitEntry.getAmountInDebt(firstDayToApplyInterestsInMonth)
                .multiply(TreasuryConstants.divide(interestsPercentage, TreasuryConstants.HUNDRED_PERCENT)));

        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        String interestRateBeanDescription = services.availableLocales().stream().map(locale -> {
            Map<String, String> valueMap = new HashMap<String, String>();
            valueMap.put("debitEntryDescription", debitEntry.getDescription());
            valueMap.put("monthOfPenaltyDate", firstDayToApplyInterests.toString("MMMM", locale));

            return new LocalizedString(locale, StrSubstitutor.replace(interestDebitEntryFormat.getContent(locale), valueMap));
        }).reduce((a, c) -> a.append(c)).get().getContent(services.defaultLocale());

        InterestRateBean interestRateBean = new InterestRateBean(this);

        BigDecimal remainingInterestAmount = interestAmount;

        if (!withAllInterestValues) {
            BigDecimal totalCreatedInterestDebitAmount = debitEntry.getInterestDebitEntriesSet().stream() //
                    .filter(d -> !d.isAnnulled()) //
                    .filter(d -> d.getEntryDateTime().getMonthOfYear() == dueDate.getMonthOfYear()) //
                    .map(d -> d.getAvailableNetAmountForCredit().add(d.getNetExemptedAmount())) //
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            remainingInterestAmount = interestAmount.subtract(totalCreatedInterestDebitAmount);

            if (TreasuryConstants.isPositive(totalCreatedInterestDebitAmount)) {
                interestRateBean.addCreatedInterestEntry(firstDayToApplyInterests, totalCreatedInterestDebitAmount);
            }
        }

        interestRateBean.addDetail(interestAmount, firstDayToApplyInterestsInMonth, null, BigDecimal.ZERO,
                debitEntry.getNetAmount(), interestsPercentage);

        if (remainingInterestAmount.compareTo(BigDecimal.ZERO) < 0) {
            // negative
            remainingInterestAmount = BigDecimal.ZERO;
        }

        interestRateBean.setDescription(interestRateBeanDescription);
        interestRateBean.setInterestAmount(remainingInterestAmount);
        interestRateBean.setInterestDebitEntryDateTime(firstDayToApplyInterestsInMonth.toDateTimeAtStartOfDay());
        interestRateBean.setNumberOfDays(0);
        interestRateBean.setNumberOfMonths(1);

        return interestRateBean;
    }

    private List<LocalDate> getMonthlyDueDateIterations(LocalDate dueDate, LocalDate paymentDate,
            int maximumMonthsToApplyInterests) {
        int numberOfMonthsToCalculate = Months.monthsBetween(dueDate, paymentDate).getMonths() + 1;

        if (numberOfMonthsToCalculate > maximumMonthsToApplyInterests) {
            numberOfMonthsToCalculate = maximumMonthsToApplyInterests;
        }

        return IntStream.range(0, numberOfMonthsToCalculate) //
                .boxed() //
                .map(i -> dueDate.plusMonths(i)) //
                .collect(Collectors.toList());
    }

    public static LocalizedString getPresentationName() {
        return TreasuryConstants.treasuryBundleI18N("label.MonthlyInterestRateType.presentationName");
    }

}
