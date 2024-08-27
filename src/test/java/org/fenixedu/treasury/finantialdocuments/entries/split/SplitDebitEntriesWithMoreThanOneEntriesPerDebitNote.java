package org.fenixedu.treasury.finantialdocuments.entries.split;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.List;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentStateType;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.PaymentEntryBean;
import org.fenixedu.treasury.tariff.InterestRateTestsUtilities;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class SplitDebitEntriesWithMoreThanOneEntriesPerDebitNote {

    @BeforeClass
    public static void startUp() {
        InterestRateTestsUtilities.startUp();
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    @Test
    public void testSplitDebitEntryPaymentWithTwoEntriesInDebitNote() {
        DebitEntry debitEntryOne =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        DebitEntry debitEntryTwo =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        debitEntryOne.getDebitNote().addDebitNoteEntries(List.of(debitEntryTwo));

        DebtAccount debtAccount = debitEntryOne.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntryOne).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("100.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("100.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        // The debit note of debitEntryOne must be different than debitNoteTwo

        assertNotEquals(debitEntryOne.getDebitNote(), debitEntryTwo.getDebitNote());

        assertEquals(debitEntryOne.getDebitNote().getDebitEntriesSet().size(), 1);
        assertEquals(debitEntryTwo.getDebitNote().getDebitEntriesSet().size(), 1);

        assertEquals(debitEntryOne.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);
        assertEquals(debitEntryTwo.getDebitNote().getState(), FinantialDocumentStateType.PREPARING);

    }

    @Test
    public void testNotSplitDebitEntryPaymentWithTwoEntriesInDebitNote() {
        DebitEntry debitEntryOne =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        DebitEntry debitEntryTwo =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        debitEntryOne.getDebitNote().addDebitNoteEntries(List.of(debitEntryTwo));

        DebtAccount debtAccount = debitEntryOne.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(false);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntryOne).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("100.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("100.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        // The debit note of debitEntryOne must be different than debitNoteTwo

        assertNotEquals(debitEntryOne.getDebitNote(), debitEntryTwo.getDebitNote());

        assertEquals(debitEntryOne.getDebitNote().getDebitEntriesSet().size(), 1);
        assertEquals(debitEntryTwo.getDebitNote().getDebitEntriesSet().size(), 1);

        assertEquals(debitEntryOne.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);
        assertEquals(debitEntryTwo.getDebitNote().getState(), FinantialDocumentStateType.PREPARING);

    }

}
