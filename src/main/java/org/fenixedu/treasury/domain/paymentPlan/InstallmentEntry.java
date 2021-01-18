package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class InstallmentEntry extends InstallmentEntry_Base {

    public static final Comparator<? super InstallmentEntry> COMPARE_BY_DEBIT_ENTRY_COMPARATOR = (m1, m2) -> {
        return DebitEntry.COMPARE_DEBIT_ENTRY_IN_SAME_PAYMENT_PLAN.compare(m1.getDebitEntry(), m2.getDebitEntry());
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
        if (!isPaymentPlanEmolument() &&  (getDebitEntry().getDebitNote() == null || !getDebitEntry().getDebitNote().isClosed())) {
            throw new TreasuryDomainException("error.InstallmentEntry.debitEntry.require.closed.debitNote");
        }
    }

    private boolean isPaymentPlanEmolument() {
		return getInstallment().getPaymentPlan().getEmolument() == getDebitEntry();
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
        Installment installment = getInstallment();
        PaymentPlan paymentPlan = installment.getPaymentPlan();

        /*
         *  totalBeforePlan  = totalDebitEntry - totalDebitInPlan
         *  debitPaid = totalPaid - totalBeforePlan
         */

        BigDecimal totalDebitEntry = getDebitEntry().getTotalAmount();

        BigDecimal totalDebitInPlan = paymentPlan.getTotalDebitEntry(getDebitEntry());

        BigDecimal totalPaid = getDebitEntry().getPayedAmount();

        BigDecimal totalBeforePlan = totalDebitEntry.subtract(totalDebitInPlan);

        BigDecimal debitPaid = totalPaid.subtract(totalBeforePlan);

        List<InstallmentEntry> installmentEntries =
                paymentPlan.getInstallmentsSet().stream().filter(inst -> inst.getDueDate().isBefore(installment.getDueDate()))
                        .flatMap(inst -> inst.getInstallmentEntriesSet().stream())
                        .filter(ent -> ent.getDebitEntry().equals(getDebitEntry())).collect(Collectors.toList());

        BigDecimal totalAmountBefore =
                installmentEntries.stream().map(i -> i.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal rest = debitPaid.subtract(totalAmountBefore);

        if (TreasuryConstants.isLessOrEqualThan(rest, BigDecimal.ZERO)) {
            return BigDecimal.ZERO;
        }
        if (TreasuryConstants.isLessOrEqualThan(rest, getAmount())) {
            return rest;
        }
        return getAmount();
    }

    public BigDecimal getOpenAmount() {
        return getAmount().subtract(getPaidAmount()).setScale(2, RoundingMode.HALF_UP);
    }

}
