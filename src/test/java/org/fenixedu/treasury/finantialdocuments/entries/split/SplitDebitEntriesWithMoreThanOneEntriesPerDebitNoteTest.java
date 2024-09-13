package org.fenixedu.treasury.finantialdocuments.entries.split;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

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
public class SplitDebitEntriesWithMoreThanOneEntriesPerDebitNoteTest {

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

        DebitNote.createDebitNoteForDebitEntry(debitEntryOne, null,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime(), new LocalDate(), null, null, null);

        debitEntryOne.getDebitNote().addDebitNoteEntries(List.of(debitEntryTwo));

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);
        bean.setFinantialEntity(FinantialEntity.findAll().iterator().next());

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

        DebtAccount debtAccount = debitEntryOne.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        DebitNote.createDebitNoteForDebitEntry(debitEntryOne, null,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime(), new LocalDate(), null, null, null);

        debitEntryOne.getDebitNote().addDebitNoteEntries(List.of(debitEntryTwo));

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(false);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);
        bean.setFinantialEntity(FinantialEntity.findAll().iterator().next());

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

        assertEquals(debitEntryOne.getDebitNote(), debitEntryTwo.getDebitNote());

        assertEquals(debitEntryOne.getDebitNote().getDebitEntriesSet().size(), 2);
        assertEquals(debitEntryTwo.getDebitNote().getDebitEntriesSet().size(), 2);

        assertEquals(debitEntryOne.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);
        assertEquals(debitEntryTwo.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);

    }

    @Test
    public void testSplitDebitEntryPaymentWithThreeEntriesInDebitNote() {
        DebitEntry debitEntryOne =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        DebitEntry debitEntryTwo =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("99.00"), new LocalDate(2020, 1, 5), false);

        DebitEntry debitEntryThree =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("98.00"), new LocalDate(2020, 1, 5), false);

        DebtAccount debtAccount = debitEntryOne.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        DebitNote.createDebitNoteForDebitEntry(debitEntryOne, null,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime(), new LocalDate(), null, null, null);

        debitEntryOne.getDebitNote().addDebitNoteEntries(List.of(debitEntryTwo, debitEntryThree));

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);
        bean.setFinantialEntity(FinantialEntity.findAll().iterator().next());

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

        assertEquals(1, debitEntryOne.getDebitNote().getDebitEntriesSet().size());
        assertEquals(2, debitEntryTwo.getDebitNote().getDebitEntriesSet().size());
        assertEquals(2, debitEntryThree.getDebitNote().getDebitEntriesSet().size());

        assertEquals(debitEntryOne.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);
        assertEquals(debitEntryTwo.getDebitNote().getState(), FinantialDocumentStateType.PREPARING);
        assertEquals(debitEntryThree.getDebitNote().getState(), FinantialDocumentStateType.PREPARING);

    }

    @Test
    public void testSplitDebitEntryPaymentWithThreeEntriesInDebitNoteAndPartialSettlement() {
        DebitEntry debitEntryOne =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        DebitEntry debitEntryTwo =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("99.00"), new LocalDate(2020, 1, 5), false);

        DebitEntry debitEntryThree =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("98.00"), new LocalDate(2020, 1, 5), false);

        DebtAccount debtAccount = debitEntryOne.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        DebitNote.createDebitNoteForDebitEntry(debitEntryOne, null,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime(), new LocalDate(), null, null, null);

        debitEntryOne.getDebitNote().addDebitNoteEntries(List.of(debitEntryTwo, debitEntryThree));

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);
        bean.setFinantialEntity(FinantialEntity.findAll().iterator().next());

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntryOne).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("100.00"));
        });

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntryTwo).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("49.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("149.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);
        bean.setFinantialEntity(FinantialEntity.findAll().iterator().next());

        assertEquals(new BigDecimal("49.00"), debitEntryTwo.getTotalAmount());

        // The debit note of debitEntryOne must be different than debitNoteTwo

        assertNotEquals(debitEntryOne.getDebitNote(), debitEntryTwo.getDebitNote());
        assertNotEquals(debitEntryTwo.getDebitNote(), debitEntryThree.getDebitNote());

        assertEquals(1, debitEntryOne.getDebitNote().getDebitEntriesSet().size());
        assertEquals(1, debitEntryTwo.getDebitNote().getDebitEntriesSet().size());
        assertEquals(2, debitEntryThree.getDebitNote().getDebitEntriesSet().size());

        assertEquals(debitEntryThree.getDebitNote(), debitEntryTwo.getSplittedDebitEntriesSet().iterator().next().getDebitNote());

        assertEquals(1, debitEntryTwo.getSplittedDebitEntriesSet().size());

        assertEquals(new BigDecimal("50.00"), debitEntryTwo.getSplittedDebitEntriesSet().iterator().next().getOpenAmount());

        assertEquals(debitEntryOne.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);
        assertEquals(debitEntryTwo.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);
        assertEquals(debitEntryThree.getDebitNote().getState(), FinantialDocumentStateType.PREPARING);
    }

    @Test
    public void testSplitDebitEntryPaymentWithThreeEntriesInDebitNoteAndPartialSettlementAndTotalSettlementOtherDebitNote() {
        DebitEntry debitEntryOne =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        DebitEntry debitEntryTwo =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("99.00"), new LocalDate(2020, 1, 5), false);

        DebitEntry debitEntryThree =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("98.00"), new LocalDate(2020, 1, 5), false);

        DebitEntry debitEntryFour =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("97.00"), new LocalDate(2020, 1, 5), false);

        DebtAccount debtAccount = debitEntryOne.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        DebitNote.createDebitNoteForDebitEntry(debitEntryOne, null,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime(), new LocalDate(), null, null, null);

        debitEntryOne.getDebitNote().addDebitNoteEntries(List.of(debitEntryTwo, debitEntryThree));

        DebitNote.createDebitNoteForDebitEntry(debitEntryFour, null,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime(), new LocalDate(), null, null, null);

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);
        bean.setFinantialEntity(FinantialEntity.findAll().iterator().next());

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        assertEquals(4, bean.getDebitEntries().size());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntryOne).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("100.00"));
        });

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntryTwo).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("49.00"));
        });

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntryFour).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("97.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("246.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals(new BigDecimal("49.00"), debitEntryTwo.getTotalAmount());

        // The debit note of debitEntryOne must be different than debitNoteTwo

        assertNotEquals(debitEntryOne.getDebitNote(), debitEntryTwo.getDebitNote());
        assertNotEquals(debitEntryTwo.getDebitNote(), debitEntryThree.getDebitNote());

        assertEquals(1, debitEntryOne.getDebitNote().getDebitEntriesSet().size());
        assertEquals(1, debitEntryTwo.getDebitNote().getDebitEntriesSet().size());
        assertEquals(2, debitEntryThree.getDebitNote().getDebitEntriesSet().size());

        assertEquals(debitEntryThree.getDebitNote(), debitEntryTwo.getSplittedDebitEntriesSet().iterator().next().getDebitNote());

        assertEquals(1, debitEntryTwo.getSplittedDebitEntriesSet().size());

        assertEquals(new BigDecimal("50.00"), debitEntryTwo.getSplittedDebitEntriesSet().iterator().next().getOpenAmount());

        assertEquals(debitEntryOne.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);
        assertEquals(debitEntryTwo.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);
        assertEquals(debitEntryThree.getDebitNote().getState(), FinantialDocumentStateType.PREPARING);

        assertEquals(debitEntryFour.getDebitNote().getState(), FinantialDocumentStateType.CLOSED);
        assertEquals(1, debitEntryFour.getDebitNote().getDebitEntriesSet().size());
        assertEquals(new BigDecimal("97.00"), debitEntryFour.getTotalAmount());
        assertEquals(new BigDecimal("0.00"), debitEntryFour.getOpenAmount());
        assertEquals(0, debitEntryFour.getSplittedDebitEntriesSet().size());

    }

}
