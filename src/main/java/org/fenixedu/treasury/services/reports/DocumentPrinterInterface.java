package org.fenixedu.treasury.services.reports;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.FinantialDocument;

import java.util.List;

public interface DocumentPrinterInterface {

    void init();
    byte[] printDebtAccountPaymentPlan(DebtAccount debtAccount, String outputMimeType);
    byte[] printDebitNotesPaymentPlan(DebtAccount debtAccount, List<DebitNote> documents, String outputMimeType);
    byte[] printFinantialDocument(FinantialDocument document, String outputMimeType);
}
