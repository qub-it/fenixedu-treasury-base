package org.fenixedu.treasury.finantialdocuments.entries.split;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.*;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.*;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
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

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

@RunWith(FenixFrameworkRunner.class)
public class SplitCreditEntriesWithExemptionTest {

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

        finantialInstitution.setSupportCreditTreasuryExemptions(true);
        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(false);

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), finantialInstitution, new DateTime()).get();
        Product pagamento = Product.findUniqueByCode("PAGAMENTO").get();

        DebitEntry debitEntry =
                DebitEntry.create(finantialEntity, debtAccount, null, vat, new BigDecimal(100), new LocalDate(), Map.of(),
                        pagamento, "Debit entry", BigDecimal.ONE, null, new DateTime(), false, false, null);

        TreasuryExemptionType exemptionType =
                TreasuryExemptionType.create("TE", new LocalizedString(Locale.getDefault(), "Exemption type"),
                        new BigDecimal("50"), true);

        TreasuryExemption.create(exemptionType, "test", new BigDecimal("50"), debitEntry);

        payDebitEntry(debitEntry);

        debitEntry.getDebitNote().anullDebitNoteWithCreditNote("test", true);

        CreditEntry creditEntry = debitEntry.getCreditEntriesSet().iterator().next();

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, true, false);

        bean.setFinantialEntity(FinantialEntity.findAll().iterator().next());
        bean.setDocNumSeries(
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForReimbursementNote(), finantialEntity));
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

        assertEquals("Split credit entry from credit entry of 100, should have amount of 75 after settlement",
                new BigDecimal("25.00"), splittedCreditEntry.getTotalAmount());

    }

    private void payDebitEntry(DebitEntry debitEntry) {
        SettlementNoteBean bean = new SettlementNoteBean(debitEntry.getDebtAccount(), false, false);

        DocumentNumberSeries docNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(),
                        debitEntry.getFinantialEntity());

        bean.setDocNumSeries(docNumberSeries);
        bean.setFinantialEntity(debitEntry.getFinantialEntity());

        bean.getInvoiceEntryBean(debitEntry).setIncluded(true);
        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("50"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);
    }

}
