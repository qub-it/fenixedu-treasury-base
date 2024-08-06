package org.fenixedu.treasury.finantialdocuments.entries.split;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.CreditNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.PaymentEntryBean;
import org.fenixedu.treasury.tariff.InterestRateTestsUtilities;
import org.joda.time.DateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class SplitCreditEntriesTest {

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
    public void testSplitCreditEntryWithDocumentPreparingWhenPayment() {
        DebtAccount debtAccount = InterestRateTestsUtilities.getDebtAccount();

        Set<InvoiceEntry> existingInvoiceEntriesFromOtherTests = new HashSet<>(debtAccount.getInvoiceEntrySet());

        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        DocumentNumberSeries creditNoteNumberSeries = DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForCreditNote(), debtAccount.getFinantialInstitution()).get();

        CreditNote creditNote =
                CreditNote.create(finantialEntity, debtAccount, creditNoteNumberSeries, null, new DateTime(), null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), finantialInstitution, new DateTime()).get();

        CreditEntry creditEntry = CreditEntry.create(finantialEntity, creditNote, "Credit note",
                Product.findUniqueByCode("PAGAMENTO").get(), vat, new BigDecimal("100.00"), new DateTime(), BigDecimal.ONE);

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(false);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, true, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForReimbursementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getCreditEntries().stream().filter(de -> de.getInvoiceEntry() == creditEntry).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("25.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("25.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals("Settled credit entry of 100 initially, should have amount of 25 after settlement", new BigDecimal("25.00"),
                creditEntry.getTotalAmount());

        CreditEntry splittedCreditEntry = debtAccount.getPendingInvoiceEntriesSet().stream() //
                .filter(e -> e.isCreditNoteEntry() && e != creditEntry) //
                .filter(e -> !existingInvoiceEntriesFromOtherTests.contains(e)) //
                .map(CreditEntry.class::cast).findFirst().get();

        assertEquals("Splitted credit entry from credit entry of 100, should have amount of 75 after settlement",
                new BigDecimal("75.00"), splittedCreditEntry.getTotalAmount());

    }

    @Test
    public void testSplitCreditEntryWithDocumentClosedWhenPayment() {
        DebtAccount debtAccount = InterestRateTestsUtilities.getDebtAccount();
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        Set<InvoiceEntry> existingInvoiceEntriesFromOtherTests = new HashSet<>(debtAccount.getInvoiceEntrySet());

        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        DocumentNumberSeries creditNoteNumberSeries = DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForCreditNote(), debtAccount.getFinantialInstitution()).get();
        CreditNote creditNote =
                CreditNote.create(finantialEntity, debtAccount, creditNoteNumberSeries, null, new DateTime(), null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), finantialInstitution, new DateTime()).get();

        CreditEntry creditEntry = CreditEntry.create(finantialEntity, creditNote, "Credit note",
                Product.findUniqueByCode("PAGAMENTO").get(), vat, new BigDecimal("100.00"), new DateTime(), BigDecimal.ONE);

        creditNote.closeDocument();

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(false);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, true, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForReimbursementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getCreditEntries().stream().filter(de -> de.getInvoiceEntry() == creditEntry).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("25.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("25.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals("Settled credit entry of 100 initially, should have amount of 100 after settlement",
                new BigDecimal("100.00"), creditEntry.getTotalAmount());

        long numberOfCreditEntries = debtAccount.getInvoiceEntrySet().stream() //
                .filter(e -> e.isCreditNoteEntry()) //
                .filter(e -> !existingInvoiceEntriesFromOtherTests.contains(e)) //
                .count();

        assertEquals("Credit entries in debt account should be 1", 1, numberOfCreditEntries);
    }

    @Test
    public void testSplitDebitEntryWithoutDocumentWhenPaymentWithSplitDisabledInFinantialInstitution() {
        DebtAccount debtAccount = InterestRateTestsUtilities.getDebtAccount();

        Set<InvoiceEntry> existingInvoiceEntriesFromOtherTests = new HashSet<>(debtAccount.getInvoiceEntrySet());

        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        DocumentNumberSeries creditNoteNumberSeries = DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForCreditNote(), debtAccount.getFinantialInstitution()).get();

        CreditNote creditNote =
                CreditNote.create(finantialEntity, debtAccount, creditNoteNumberSeries, null, new DateTime(), null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), finantialInstitution, new DateTime()).get();

        CreditEntry creditEntry = CreditEntry.create(finantialEntity, creditNote, "Credit note",
                Product.findUniqueByCode("PAGAMENTO").get(), vat, new BigDecimal("100.00"), new DateTime(), BigDecimal.ONE);

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(false);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(false);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, true, false);

        bean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForReimbursementNote(), finantialInstitution).get());
        bean.setDate(new DateTime());

        bean.getCreditEntries().stream().filter(de -> de.getInvoiceEntry() == creditEntry).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("25.00"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("25.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals("Settled credit entry of 100 initially, should have amount of 100 after settlement",
                new BigDecimal("100.00"), creditEntry.getTotalAmount());

        long numberOfCreditEntries = debtAccount.getInvoiceEntrySet().stream() //
                .filter(e -> e.isCreditNoteEntry()) //
                .filter(e -> !existingInvoiceEntriesFromOtherTests.contains(e)) //
                .count();

        assertEquals("Credit entries in debt account should be 1", 1, numberOfCreditEntries);
    }

}
