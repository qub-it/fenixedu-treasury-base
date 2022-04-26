package org.fenixedu.treasury.domain.treasurydebtprocess;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.InvoiceEntry;

public interface ITreasuryDebtProcessService {

    LocalizedString getServiceDescription();
    
    public boolean isBlockingPaymentInFrontend(InvoiceEntry invoiceEntry);
    
    public boolean isBlockingPaymentInBackoffice(InvoiceEntry invoiceEntry);

    LocalizedString getBlockingPaymentReasonForFrontend(InvoiceEntry invoiceEntry);
    
    LocalizedString getBlockingPaymentReasonForBackoffice(InvoiceEntry invoiceEntry);
}
