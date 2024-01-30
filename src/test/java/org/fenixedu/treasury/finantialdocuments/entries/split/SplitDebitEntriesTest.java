package org.fenixedu.treasury.finantialdocuments.entries.split;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.List;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
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
public class SplitDebitEntriesTest {

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
    public void testSplitDebitEntryWithoutDocumentWhenPayment() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);
        DebtAccount debtAccount = debitEntry.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntry).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("25.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("25.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals("Settled debit entry of 100 initially, should have amount of 25 after settlement", new BigDecimal("25.00"),
                debitEntry.getTotalAmount());

        DebitEntry splittedDebitEntry = debitEntry.getSplittedDebitEntriesSet().iterator().next();
        assertEquals("Splitted debit entry from debit entry of 100, should have amount of 75 after settlement",
                new BigDecimal("75.00"), splittedDebitEntry.getTotalAmount());

    }

    @Test
    public void testSplitDebitEntryInClosedDebitNote() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        DebtAccount debtAccount = debitEntry.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        DebitNote debitNote = DebitNote.create(debtAccount,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime());

        debitNote.addDebitNoteEntries(List.of(debitEntry));
        debitNote.closeDocument();

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntry).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("25.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("25.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals("Settled debit entry of 100 initially, should have amount of 100 after settlement", new BigDecimal("100.00"),
                debitEntry.getTotalAmount());

        assertEquals("Splitted debit entries must be zero", 0, debitEntry.getSplittedDebitEntriesSet().size());

    }

    @Test
    public void testSplitDebitEntryInPreparingDebitNote() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        DebtAccount debtAccount = debitEntry.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        DebitNote debitNote = DebitNote.create(debtAccount,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime());

        debitNote.addDebitNoteEntries(List.of(debitEntry));

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(false);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntry).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("25.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("25.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals("Settled debit entry of 100 initially, should have amount of 25 after settlement", new BigDecimal("25.00"),
                debitEntry.getTotalAmount());

        DebitEntry splittedDebitEntry = debitEntry.getSplittedDebitEntriesSet().iterator().next();
        assertEquals("Splitted debit entry from debit entry of 100, should have amount of 75 after settlement",
                new BigDecimal("75.00"), splittedDebitEntry.getTotalAmount());
    }

    @Test
    public void testSplitDebitEntryWithoutDocumentWhenPaymentWithSplitDisabledInFinantialInstitution() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);
        DebtAccount debtAccount = debitEntry.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(false);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntry).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("25.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("25.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals("Settled debit entry of 100 initially, should have amount of 100 after settlement", new BigDecimal("100.00"),
                debitEntry.getTotalAmount());

        assertEquals("Splitted debit entries must be zero", 0, debitEntry.getSplittedDebitEntriesSet().size());

    }

    @Test
    public void testSplitDebitEntryWithQuantityTwo() {
        DebtAccount debtAccount = InterestRateTestsUtilities.getDebtAccount();

        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), finantialInstitution, new DateTime()).get();
        TreasuryEvent treasuryEvent = null;
        DebitEntry debitEntry = DebitEntry.create(finantialEntity, debtAccount, treasuryEvent, vat, new BigDecimal("50.00"),
                new LocalDate(), null, Product.findUniqueByCode("PAGAMENTO").get(), "debt 1", new BigDecimal("2"), null,
                new DateTime(), false, false, null);

        DebitNote debitNote = DebitNote.create(debtAccount,
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get(),
                new DateTime());

        debitNote.addDebitNoteEntries(List.of(debitEntry));

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(false);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntry).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("25.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("25.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals("Settled debit entry of 100 initially, should have amount of 25 after settlement", new BigDecimal("25.00"),
                debitEntry.getTotalAmount());

        DebitEntry splittedDebitEntry = debitEntry.getSplittedDebitEntriesSet().iterator().next();
        assertEquals("Splitted debit entry from debit entry of 100, should have amount of 75 after settlement",
                new BigDecimal("75.00"), splittedDebitEntry.getTotalAmount());

        assertEquals("Splitted debit entry maintains quantity of 2", new BigDecimal("2"), splittedDebitEntry.getQuantity());

        assertEquals("Splitted debit entry unit amount is 37.5", new BigDecimal("37.5000"), splittedDebitEntry.getAmount());
    }

}
