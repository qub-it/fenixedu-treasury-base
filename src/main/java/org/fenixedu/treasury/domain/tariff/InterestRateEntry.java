package org.fenixedu.treasury.domain.tariff;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class InterestRateEntry extends InterestRateEntry_Base {

    public static final Comparator<? super InterestRateEntry> FIRST_DATE_COMPARATOR = (o1, o2) -> {
        final int c = o1.getStartDate().compareTo(o2.getStartDate());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public InterestRateEntry() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public InterestRateEntry(InterestRateType interestRateType) {
        this();

        setInterestRateType(interestRateType);
    }

    public InterestRateEntry(InterestRateType interestRateType, LocalDate startDate, LocalizedString description, BigDecimal rate,
            boolean applyPaymentMonth, boolean applyInFirstWorkday) {
        this(interestRateType);

        super.setStartDate(startDate);
        super.setDescription(description);
        super.setRate(rate);
        super.setApplyPaymentMonth(applyPaymentMonth);
        super.setApplyInFirstWorkday(applyInFirstWorkday);

        checkRules();
    }

    private void checkRules() {
        if (getStartDate() == null) {
            throw new TreasuryDomainException("error.InterestRateEntry.startDate.required");
        }

        if (findByStartDate(getInterestRateType(), getStartDate()).count() > 1) {
            throw new TreasuryDomainException("error.InterestRateEntry.startDate.duplicated");
        }

        if (getRate() == null) {
            throw new TreasuryDomainException("error.InterestRateEntry.rate.with.valid.value.required");
        }

        if (getApplyPaymentMonth() == null) {
            throw new TreasuryDomainException("error.InterestRateEntry.applyPaymentMonth.required");
        }

        if (getApplyInFirstWorkday() == null) {
            throw new TreasuryDomainException("error.InterestRateEntry.applyInFirstWorkday.required");
        }

        if (StringUtils.isEmpty(getDescription().getContent())) {
            throw new TreasuryDomainException("error.InterestRateEntry.description.required");
        }

        if (TreasuryConstants.isLessThan(getRate(), BigDecimal.ZERO)
                || TreasuryConstants.isGreaterThan(getRate(), TreasuryConstants.HUNDRED_PERCENT)) {
            throw new TreasuryDomainException("error.InterestRateEntry.rate.with.valid.value.required");
        }
    }

    public void edit(LocalDate startDate, LocalizedString description, BigDecimal rate, boolean applyPaymentMonth,
            boolean applyInFirstWorkday) {
        setStartDate(startDate);
        setDescription(description);
        setRate(rate);
        setApplyPaymentMonth(applyPaymentMonth);
        setApplyInFirstWorkday(applyInFirstWorkday);

        checkRules();
    }

    public void delete() {
        setDomainRoot(null);
        setInterestRateType(null);

        deleteDomainObject();
    }
    
    public Integer getYear() {
        return getStartDate().getYear();
    }
    
    
    public static Stream<InterestRateEntry> findByYear(InterestRateType interestRateType, int year) {
        return interestRateType.getInterestRateEntriesSet().stream().filter(entry -> entry.getYear() == year);
    }

    public static Stream<InterestRateEntry> findByStartDate(InterestRateType interestRateType, LocalDate date) {
        return interestRateType.getInterestRateEntriesSet().stream().filter(r -> r.getStartDate().equals(date));
    }

    public static Optional<InterestRateEntry> findUniqueByStartDate(InterestRateType interestRateType, LocalDate date) {
        return findByStartDate(interestRateType, date).findFirst();
    }

    public static Optional<InterestRateEntry> findUniqueAppliedForDate(InterestRateType interestRateType, LocalDate date) {
        return interestRateType.getInterestRateEntriesSet().stream().filter(r -> !r.getStartDate().isAfter(date))
                .sorted(FIRST_DATE_COMPARATOR.reversed()).findFirst();
    }

    public static InterestRateEntry create(InterestRateType interestRateType, LocalDate startDate, LocalizedString description,
            BigDecimal rate, boolean applyPaymentMonth, boolean applyInFirstWorkday) {
        return new InterestRateEntry(interestRateType, startDate, description, rate, applyPaymentMonth, applyInFirstWorkday);
    }

}
