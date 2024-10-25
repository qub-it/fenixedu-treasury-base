package org.fenixedu.treasury.seriesbyfinantialentity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.List;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
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
public class DocumentsWithSeriesByFinantialInstitutionTest {

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

        DebtAccount debtAccount = debitEntryOne.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        finantialInstitution.setSeriesByFinantialEntity(false);

        DebitNote.createDebitNoteForDebitEntry(debitEntryOne, null, DocumentNumberSeries
                .findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), debitEntryOne.getFinantialEntity()),
                new DateTime(), new LocalDate(), null, null, null);

        debitEntryOne.getDebitNote().addDebitNoteEntries(List.of(debitEntryTwo));

        debitEntryOne.getDebitNote().closeDocument();

        assertEquals(null, debitEntryOne.getDebitNote().getDocumentNumberSeries().getSeries().getFinantialEntity());

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);
        bean.setFinantialEntity(FinantialEntity.findAll().iterator().next());

        bean.setDocNumSeries(DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(),
                debitEntryOne.getFinantialEntity()));
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntryOne).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("100.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("100.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote settlementNote = SettlementNote.createSettlementNote(bean);

        assertEquals(null, settlementNote.getDocumentNumberSeries().getSeries().getFinantialEntity());
    }

}
