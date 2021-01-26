package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.Comparator;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class InstallmentEntry extends InstallmentEntry_Base {

    public static final Comparator<? super InstallmentEntry> COMPARE_BY_DEBIT_ENTRY_COMPARATOR = (m1, m2) -> {
        return (m1.getInstallment().getDueDate().compareTo(m2.getInstallment().getDueDate()) * 100)
                + (DebitEntry.COMPARE_DEBIT_ENTRY_IN_SAME_PAYMENT_PLAN.compare(m1.getDebitEntry(), m2.getDebitEntry()) * 10)
                + m1.getExternalId().compareTo(m2.getExternalId());
    };

    public InstallmentEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    private InstallmentEntry(DebitEntry debitEntry, BigDecimal debitAmount, Installment installment) {
        this();
        setDebitEntry(debitEntry);
        setAmount(debitAmount);
        setInstallment(installment);
        checkRules();
    }

    private void checkRules() {
        if (getDebitEntry() == null) {
            throw new TreasuryDomainException("error.InstallmentEntry.debitEntry.required");
        }
        if (getAmount() == null || !TreasuryConstants.isPositive(getAmount())) {
            throw new TreasuryDomainException("error.InstallmentEntry.getAmount.must.be.positive");
        }
        if (getInstallment() == null) {
            throw new TreasuryDomainException("error.InstallmentEntry.installment.required");
        }
        if (getDebitEntry().getDebitNote() == null || !getDebitEntry().getDebitNote().isClosed()) {
            throw new TreasuryDomainException("error.InstallmentEntry.debitEntry.require.closed.debitNote");
        }
    }

    @Atomic
    public static InstallmentEntry create(DebitEntry debitEntry, BigDecimal debitAmount, Installment installment) {
        return new InstallmentEntry(debitEntry, debitAmount, installment);
    }

    @Atomic
    public void delete() {
        setDomainRoot(null);
        setDebitEntry(null);
        deleteDomainObject();
    }

    public BigDecimal getPaidAmount() {
        return getInstallmentSettlementEntriesSet().stream().map(i -> i.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    public BigDecimal getOpenAmount() {
        return getCurrency().getValueWithScale(getAmount().subtract(getPaidAmount()));
    }

    private Currency getCurrency() {
        return getDebitEntry().getCurrency();
    }

    public boolean isPaid() {
        return TreasuryConstants.isZero(getOpenAmount());
    }
}
