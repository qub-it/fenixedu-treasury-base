package org.fenixedu.treasury.domain.treasurydebtprocess;

import java.util.Set;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;

public interface ITreasuryDebtProcessService {

    LocalizedString getServiceDescription();
    
    boolean isBlockingPaymentInFrontend(InvoiceEntry invoiceEntry);
    
    boolean isBlockingPaymentInBackoffice(InvoiceEntry invoiceEntry);

    LocalizedString getBlockingPaymentReasonForFrontend(InvoiceEntry invoiceEntry);
    
    LocalizedString getBlockingPaymentReasonForBackoffice(InvoiceEntry invoiceEntry);
    
    Set<? extends ITreasuryDebtProcess> getDebtProcesses(InvoiceEntry invoiceEntry);
    
    boolean isInterestCreationWhenTotalSettledPrevented(InvoiceEntry invoiceEntry);
    
    boolean isSettlementAnnullmentActionBlocked(SettlementNote settlementNote);
    
    Set<? extends ITreasuryDebtProcess> getDebtProcesses(SettlementNote settlementNote);
}
