package org.fenixedu.treasury.domain.payments.integration;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.joda.time.Days;
import org.joda.time.LocalDate;

public class StandardSibsPaymentExpiryStrategy extends StandardSibsPaymentExpiryStrategy_Base {

    public StandardSibsPaymentExpiryStrategy() {
        super();
    }

    public StandardSibsPaymentExpiryStrategy(DigitalPaymentPlatform digitalPaymentPlatform) {
        this();

        super.init(digitalPaymentPlatform);

        setUseEntriesMaximumDueDate(false);
        setMinimumExpiryDaysFromRequestDate(0);
        setMaximumExpiryDaysFromRequestDate(7);

        checkRules();
    }

    protected void checkRules() {
        super.checkRules();
    }

    @Override
    public LocalDate calculateSibsPaymentRequestExpiryDate(Set<DebitEntry> debitEntries, Set<Installment> installments,
            boolean limitSibsPaymentRequestToCustomDueDate, LocalDate customSibsPaymentRequestDueDate) {

        if (limitSibsPaymentRequestToCustomDueDate) {
            if (customSibsPaymentRequestDueDate == null) {
                throw new IllegalArgumentException(
                        "error.StandardSibsPaymentExpiryStrategy.customSibsPaymentRequestDueDate.required");
            }

            return customSibsPaymentRequestDueDate;
        }

        LocalDate validTo = null;

        Set<LocalDate> dueDatesSet = new HashSet<>();
        debitEntries.stream().map(db -> db.getDueDate()).collect(Collectors.toCollection(() -> dueDatesSet));
        installments.stream().map(i -> i.getDueDate()).collect(Collectors.toCollection(() -> dueDatesSet));

        if (Boolean.TRUE.equals(getUseEntriesMaximumDueDate())) {
            validTo = dueDatesSet.stream().max((o1, o2) -> o1.compareTo(o2)).get();
        } else {
            validTo = dueDatesSet.stream().min((o1, o2) -> o1.compareTo(o2)).get();
        }

        if (validTo.isBefore(new LocalDate())) {
            validTo = new LocalDate();
        }

        if (getMinimumExpiryDaysFromRequestDate() != null) {
            Days days = Days.daysBetween(new LocalDate(), validTo);

            if (days.getDays() < getMinimumExpiryDaysFromRequestDate()) {
                validTo = validTo.plusDays(getMinimumExpiryDaysFromRequestDate() - days.getDays());
            }
        }

        if (getMaximumExpiryDaysFromRequestDate() != null) {
            Days days = Days.daysBetween(new LocalDate(), validTo);

            if (days.getDays() > getMaximumExpiryDaysFromRequestDate()) {
                validTo = validTo.minusDays(days.getDays() - getMaximumExpiryDaysFromRequestDate());
            }
        }

        return validTo;
    }

}
