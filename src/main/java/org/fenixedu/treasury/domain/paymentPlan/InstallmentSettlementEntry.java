package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.fenixframework.FenixFramework;

public class InstallmentSettlementEntry extends InstallmentSettlementEntry_Base {

    public InstallmentSettlementEntry() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    private InstallmentSettlementEntry(InstallmentEntry installmentEntry, SettlementEntry settlementEntry,
            BigDecimal debtAmount) {
        this();
        setSettlementEntry(settlementEntry);
        setInstallmentEntry(installmentEntry);
        setAmount(debtAmount);
        checkRules();
    }

    private void checkRules() {
        if (getSettlementEntry() == null) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.settlementEntry.required");
        }

        if (getAmount() == null || !TreasuryConstants.isPositive(getAmount())) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.amount.must.be.positive");
        }

        if (getInstallmentEntry() == null) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.installmentEntry.required");
        }

        // Verifiy is installment settlement entry is duplicated 
        // by checking if some instance exists for the same installmentEntry and settlementEntry
        if (find(super.getInstallmentEntry(), super.getSettlementEntry()).count() > 1) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.entry.is.duplicated");
        }

        // The sum of all installment settlement entries (of not annuled payment plans)
        // should not overflow settlement entry total amount
        BigDecimal sumOfInstallmentSettlementEntries = getSettlementEntry().getInstallmentSettlementEntriesSet().stream()
                .map(i -> i.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (TreasuryConstants.isGreaterThan(sumOfInstallmentSettlementEntries, getSettlementEntry().getAmount())) {
            throw new TreasuryDomainException(
                    "error.InstallmentSettlementEntry.sum.of.installmentSettlementEntries.exceed.settlement.entry.amount");
        }

    }

    public static InstallmentSettlementEntry create(InstallmentEntry installmentEntry, SettlementEntry settlementEntry,
            BigDecimal debtAmount) {
        return new InstallmentSettlementEntry(installmentEntry, settlementEntry, debtAmount);
    }
    
    public static Set<InstallmentSettlementEntry> settleInstallmentEntriesOfDebitEntry(SettlementEntry settlementEntry) {
        if(!settlementEntry.getInvoiceEntry().isDebitNoteEntry()) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.settleForDebitEntry.expecting.settlementEntry.forDebitEntry");
        }
        
        if(!settlementEntry.getInstallmentSettlementEntriesSet().isEmpty()) {
            throw new TreasuryDomainException("error.InstallmentSettlementEntry.settlementEntry.already.has.installmentSettlementEntries");
        }
        
        Set<InstallmentSettlementEntry> result = new HashSet<>();
        DebitEntry debitEntry = (DebitEntry) settlementEntry.getInvoiceEntry();
        
        BigDecimal rest = settlementEntry.getAmount();
        for (InstallmentEntry installmentEntry : debitEntry.getSortedOpenInstallmentEntries()) {
            if (!TreasuryConstants.isPositive(rest)) {
                break;
            }
            
            if(installmentEntry.isPaid()) {
                continue;
            }
            
            BigDecimal debtAmount =
                    rest.compareTo(installmentEntry.getOpenAmount()) > 0 ? installmentEntry.getOpenAmount() : rest;
            rest = rest.subtract(debtAmount);
            result.add(InstallmentSettlementEntry.create(installmentEntry, settlementEntry, debtAmount));

            installmentEntry.getInstallment().getPaymentPlan().tryClosePaymentPlanByPaidOff();
        }
        
        return result;
    }

    public static Stream<InstallmentSettlementEntry> find(InstallmentEntry installmentEntry, SettlementEntry settlementEntry) {
        return installmentEntry.getInstallmentSettlementEntriesSet().stream()
                .filter(i -> i.getSettlementEntry() == settlementEntry);
    }

    public static Optional<InstallmentSettlementEntry> findUnique(InstallmentEntry installmentEntry,
            SettlementEntry settlementEntry) {
        return find(installmentEntry, settlementEntry).findFirst();
    }

}
