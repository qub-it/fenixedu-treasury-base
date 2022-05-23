package org.fenixedu.treasury.domain.treasurydebtprocess;

import java.util.Set;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.joda.time.LocalDate;

public interface ITreasuryDebtProcess {

    String getProcessId();
    
    LocalizedString getDescription();
    
    String getObservations();
    
    Set<InvoiceEntry> getInvoiceEntriesSet();
    
    default DebtAccount getDebtAccount() {
        return getInvoiceEntriesSet().iterator().next().getDebtAccount();
    }
    
    LocalDate getEmissionDate();

    TreasuryDebtProcessState getProcessState();
    
    default boolean isProcessActive() {
        return getProcessState().isActive();
    }
}
