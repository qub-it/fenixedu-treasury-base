package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class Installment extends Installment_Base {

    public static final Comparator<? super Installment> COMPARE_BY_DUEDATE =
            (m1, m2) -> m1.getDueDate().compareTo(m2.getDueDate());

    public Installment() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public Installment(String description, LocalDate dueDate, PaymentPlan paymentPlan) {
        this();
        setDescription(description);
        setDueDate(dueDate);
        setPaymentPlan(paymentPlan);
        checkRules();
    }

    @Atomic
    public static Installment create(String description, LocalDate dueDate, PaymentPlan paymentPlan) {
        return new Installment(description, dueDate, paymentPlan);
    }

    private void checkRules() {
        if (getDescription() == null) {
            throw new TreasuryDomainException("error.Installment.description.required");
        }
        if (getDueDate() == null) {
            throw new TreasuryDomainException("error.Installment.dueDate.required");
        }

        if (getPaymentPlan() == null) {
            throw new TreasuryDomainException("error.Installment.paymentPlan.required");
        }
        if (getDueDate().isBefore(getPaymentPlan().getCreationDate().toLocalDate())) {
            throw new TreasuryDomainException("error.Installment.paymentPlan.must.be.after.paymentPlan.creationDate");
        }
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        getInstallmentEntriesSet().forEach(i -> i.delete());
        deleteDomainObject();
    }

    public BigDecimal getTotalAmount() {
        return getInstallmentEntriesSet().stream().map(i -> i.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getPayedAmount() {
        return getInstallmentEntriesSet().stream().map(i -> i.getPaidAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getOpenAmount() {
        return getTotalAmount().subtract(getPayedAmount());
    }

    public boolean isPayed() {
        return TreasuryConstants.isZero(getOpenAmount());
    }

    public List<InstallmentEntry> getSortedInstallmentEntries() {
        return super.getInstallmentEntriesSet().stream().sorted(InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR)
                .collect(Collectors.toList());
    }
}
