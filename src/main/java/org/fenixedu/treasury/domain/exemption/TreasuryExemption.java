package org.fenixedu.treasury.domain.exemption;

import static org.fenixedu.treasury.util.TreasuryConstants.isGreaterThan;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TreasuryExemption extends TreasuryExemption_Base {

    public TreasuryExemption() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TreasuryExemption(final TreasuryExemptionType treasuryExemptionType, final TreasuryEvent treasuryEvent,
            final String reason, final BigDecimal valueToExempt, final DebitEntry debitEntry) {
        this();

        if (debitEntry.getTreasuryExemption() != null) {
            throw new TreasuryDomainException("error.TreasuryExemption.debitEntry.already.exempted");
        }

        for (final CreditEntry creditEntry : debitEntry.getCreditEntriesSet()) {
            if (!creditEntry.getFinantialDocument().isAnnulled() && !creditEntry.isFromExemption()) {
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

        exemptEventIfWithDebtEntries();
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

        if (!TreasuryConstants.isPositive(getValueToExempt())) {
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

        if (TreasuryConstants.isGreaterThan(getValueToExempt(),
                getDebitEntry().getAmountWithVat().add(getDebitEntry().getExemptedAmount()))) {
            throw new TreasuryDomainException("error.TreasuryExemption.valueToExempt.higher.than.debitEntry");
        }
    }

    public boolean isExemptByPercentage() {
        return super.getExemptByPercentage();
    }

    public BigDecimal getExemptedAmount() {
        if (isExemptByPercentage()) {
            throw new TreasuryDomainException("error.TreasuryExemption.exempted.by.percentage.not.supported");
        }

        return getValueToExempt();
    }

    private void exemptEventIfWithDebtEntries() {
        // We're in conditions to create credit entries. But first
        // calculate the amount to exempt
        final BigDecimal amountToExempt = getValueToExempt().subtract(exemptedAmount(getDebitEntry()));

        if (!TreasuryConstants.isPositive(amountToExempt)) {
            return;
        }

        final BigDecimal amountWithVat = getDebitEntry().getAmountWithVat();
        final BigDecimal amountToUse = isGreaterThan(amountWithVat, amountToExempt) ? amountToExempt : amountWithVat;

        getDebitEntry().exempt(this, amountToUse);
    }

    private BigDecimal exemptedAmount(final DebitEntry debitEntry) {
        BigDecimal result = debitEntry.getExemptedAmount();

        result = result.add(debitEntry.getCreditEntriesSet().stream().map(l -> l.getAmountWithVat()).reduce((a, b) -> a.add(b))
                .orElse(BigDecimal.ZERO));

        return result;
    }

    private void revertExemptionIfPossible() {
        if(getDebitEntry().isAnnulled()) {
            return;
        }
        
        if(getDebitEntry().isProcessedInClosedDebitNote()) {
            throw new TreasuryDomainException("error.TreasuryExemption.delete.impossible.due.to.processed.debit.or.credit.entry");
        }
        
        if (!getDebitEntry().revertExemptionIfPossible(this)) {
            throw new TreasuryDomainException("error.TreasuryExemption.delete.impossible.due.to.processed.debit.or.credit.entry");
        }
    }

    private boolean isDeletable() {
        return getDebitEntry().isAnnulled() || 
                getDebitEntry().getFinantialDocument() == null || 
                getDebitEntry().getFinantialDocument().isPreparing();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.TreasuryExemption.delete.impossible");
        }

        revertExemptionIfPossible();

        super.setDomainRoot(null);

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

    public static Stream<TreasuryExemption> findAll() {
        return FenixFramework.getDomainRoot().getTreasuryExemptionsSet().stream();
    }

    public static Stream<TreasuryExemption> find(final TreasuryExemptionType treasuryExemptionType) {
        return FenixFramework.getDomainRoot().getTreasuryExemptionsSet().stream()
                .filter(t -> t.getTreasuryExemptionType() == treasuryExemptionType);
    }

    public static Stream<TreasuryExemption> find(final TreasuryEvent treasuryEvent) {
        return FenixFramework.getDomainRoot().getTreasuryExemptionsSet().stream().filter(t -> t.getTreasuryEvent() == treasuryEvent);
    }

    protected static Stream<TreasuryExemption> find(final TreasuryEvent treasuryEvent, final Product product) {
        return find(treasuryEvent).filter(t -> t.getProduct() == product);
    }

    public static java.util.Optional<TreasuryExemption> findUnique(final TreasuryEvent treasuryEvent, final Product product) {
        return find(treasuryEvent, product).findFirst();
    }

    public static Stream<TreasuryExemption> findByDebtAccount(final DebtAccount debtAccount) {
        return FenixFramework.getDomainRoot().getTreasuryExemptionsSet().stream()
                .filter(t -> t.getDebitEntry().getDebtAccount() == debtAccount);
    }

    @Atomic
    public static TreasuryExemption create(final TreasuryExemptionType treasuryExemptionType, final TreasuryEvent treasuryEvent,
            final String reason, final BigDecimal valueToExempt, final DebitEntry debitEntry) {
        if(TreasuryConstants.isGreaterThan(debitEntry.getQuantity(), BigDecimal.ONE)) {
            throw new TreasuryDomainException("error.TreasuryExemption.not.possible.to.exempt.debit.entry.with.more.than.one.in.quantity");
        }
        
        return new TreasuryExemption(treasuryExemptionType, treasuryEvent, reason, valueToExempt, debitEntry);
    }

}
