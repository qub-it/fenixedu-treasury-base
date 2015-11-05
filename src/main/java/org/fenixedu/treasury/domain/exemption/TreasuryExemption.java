package org.fenixedu.treasury.domain.exemption;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.Constants;

import pt.ist.fenixframework.Atomic;

import com.google.common.base.Strings;

public class TreasuryExemption extends TreasuryExemption_Base {

    protected TreasuryExemption() {
        super();
        setBennu(Bennu.getInstance());
    }

    protected TreasuryExemption(final TreasuryExemptionType treasuryExemptionType, final TreasuryEvent treasuryEvent,
            final String reason, final BigDecimal valueToExempt, final DebitEntry debitEntry, final boolean creditIfNecessary) {
        this();

        if (debitEntry.getTreasuryExemption() != null) {
            throw new TreasuryDomainException("error.TreasuryExemption.debitEntry.already.exempted");
        }
        
        for (final CreditEntry creditEntry : debitEntry.getCreditEntriesSet()) {
            if(!creditEntry.isFromExemption()) {
                throw new TreasuryDomainException("error.TreasuryExemption.debitEntry.with.credit.not.from.exemption");
            }
        }

        setTreasuryExemptionType(treasuryExemptionType);
        setTreasuryEvent(treasuryEvent);
        setReason(reason);

        /*
         * For now percentages are not supported because they are complex to deal with
         */
        setExemptByPercentage(false);
        setValueToExempt(valueToExempt);

        setDebitEntry(debitEntry);
        setProduct(debitEntry.getProduct());

        checkRules();

        exemptEventIfWithDebtEntries(creditIfNecessary);
    }

    private void checkRules() {
        if (getTreasuryExemptionType() == null) {
            throw new TreasuryDomainException("error.TreasuryExemption.treasuryExemptionType.required");
        }

        if (getTreasuryEvent() == null) {
            throw new TreasuryDomainException("error.TreasuryExemption.treasuryEvent.required");
        }

        if (getValueToExempt() == null) {
            throw new TreasuryDomainException("error.TreasuryExemption.valueToExempt.required");
        }

        if (!Constants.isPositive(getValueToExempt())) {
            throw new TreasuryDomainException("error.TreasuryExemption.valueToExempt.positive.required");
        }

        if (getDebitEntry() == null) {
            throw new TreasuryDomainException("error.TreasuryExemption.debitEntry.required");
        }

        if (getProduct() == null) {
            throw new TreasuryDomainException("error.TreasuryExemption.product.required");
        }

        if (Strings.isNullOrEmpty(getReason())) {
            throw new TreasuryDomainException("error.TreasuryExemption.reason.empty");
        }

        if (getDebitEntry().isEventAnnuled()) {
            throw new TreasuryDomainException("error.TreasuryExemption.debit.entry.annuled.in.event");
        }
        
        if(Constants.isGreaterThan(getValueToExempt(), getDebitEntry().getAmountWithVat().add(getDebitEntry().getExemptedAmount()))) {
            throw new TreasuryDomainException("error.TreasuryExemption.valueToExempt.higher.than.debitEntry");
        }
    }

    public boolean isExemptByPercentage() {
        return super.getExemptByPercentage();
    }

    public BigDecimal getExemptedAmount(final TreasuryEvent treasuryEvent) {
        if (isExemptByPercentage()) {
            throw new TreasuryDomainException("error.TreasuryExemption.exempted.by.percentage.not.supported");
        }

        return getValueToExempt();
    }

    private void exemptEventIfWithDebtEntries(boolean creditIfNecessary) {
        if (!creditIfNecessary) {
            return;
        }

        // We're in conditions to create credit entries. But first
        // calculate the amount to exempt
        final BigDecimal amountToExempt = getValueToExempt().subtract(getTreasuryEvent().getExemptedAmount(getDebitEntry()));

        if (!Constants.isPositive(amountToExempt)) {
            return;
        }

        final BigDecimal amountToUse =
                Constants.isGreaterThan(getDebitEntry().getAmountWithVat(), amountToExempt) ? amountToExempt : getDebitEntry()
                        .getAmountWithVat();

        getDebitEntry().exempt(this, amountToUse);
    }

    private void revertExemptionIfPossible() {
        // TreasuryEvent is not charged. Do nothing...
        if (!getTreasuryEvent().isChargedWithDebitEntry()) {
            return;
        }

        if (!getDebitEntry().isProcessedInClosedDebitNote() || !getDebitEntry().isEventAnnuled()) {

            if (!getDebitEntry().revertExemptionIfPossible(this)) {
                throw new TreasuryDomainException(
                        "error.TreasuryExemption.delete.impossible.due.to.processed.debit.or.credit.entry");
            }
        }
    }

    private boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.TreasuryExemption.delete.impossible");
        }

        revertExemptionIfPossible();

        super.setBennu(null);

        super.setTreasuryExemptionType(null);
        super.setTreasuryEvent(null);
        super.setProduct(null);
        super.setDebitEntry(null);

        super.deleteDomainObject();
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<TreasuryExemption> find(final TreasuryExemptionType treasuryExemptionType) {
        return Bennu.getInstance().getTreasuryExemptionsSet().stream()
                .filter(t -> t.getTreasuryExemptionType() == treasuryExemptionType);
    }

    public static Stream<TreasuryExemption> find(final TreasuryEvent treasuryEvent) {
        return Bennu.getInstance().getTreasuryExemptionsSet().stream().filter(t -> t.getTreasuryEvent() == treasuryEvent);
    }

    protected static Stream<TreasuryExemption> find(final TreasuryEvent treasuryEvent, final Product product) {
        return find(treasuryEvent).filter(t -> t.getProduct() == product);
    }

    public static java.util.Optional<TreasuryExemption> findUnique(final TreasuryEvent treasuryEvent, final Product product) {
        return find(treasuryEvent, product).findFirst();
    }

    public static Stream<TreasuryExemption> findByDebtAccount(final DebtAccount debtAccount) {
        return Bennu.getInstance().getTreasuryExemptionsSet().stream()
                .filter(t -> t.getTreasuryEvent().getDebtAccount() == debtAccount);
    }

    @Atomic
    public static TreasuryExemption create(final TreasuryExemptionType treasuryExemptionType, final TreasuryEvent treasuryEvent,
            final String reason, final BigDecimal valueToExempt, final DebitEntry debitEntry, final boolean creditIfNecessary) {
        return new TreasuryExemption(treasuryExemptionType, treasuryEvent, reason, valueToExempt, debitEntry, creditIfNecessary);
    }

}
