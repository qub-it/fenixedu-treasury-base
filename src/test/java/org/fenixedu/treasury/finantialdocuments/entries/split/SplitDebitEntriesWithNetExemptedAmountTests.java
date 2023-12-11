package org.fenixedu.treasury.finantialdocuments.entries.split;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.PaymentEntryBean;
import org.fenixedu.treasury.tariff.InterestRateTestsUtilities;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class SplitDebitEntriesWithNetExemptedAmountTests {

    @BeforeClass
    public static void startUp() {
        InterestRateTestsUtilities.startUp();
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                TreasuryExemptionType tet1 =
                        TreasuryExemptionType.create("TET1", ls("Treasury Exemption 1"), new BigDecimal("12.21"), true);

                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }

    @Test
    public void testSplitDebitEntryWithoutDocumentWhenPayment() {
        TreasuryExemptionType tet1 = TreasuryExemptionType.findByCode("TET1").findFirst().get();

        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        TreasuryExemption.create(tet1, "Test", new BigDecimal("98.00"), debitEntry);

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
            de.setSettledAmount(new BigDecimal("1.50"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("1.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(bean);

        assertEquals("Settled debit entry of 2 initially, should have amount of 1.5 after settlement", new BigDecimal("1.50"),
                debitEntry.getTotalAmount());

        assertEquals("Settled debit entry of unit amount 100 initially, should have unit amount of 99.50 after settlement",
                new BigDecimal("99.50"), debitEntry.getAmount());

        assertEquals("Settled debit entry of net amount 2 initially, should have net amount of 1.50 after settlement",
                new BigDecimal("1.50"), debitEntry.getNetAmount());

        assertEquals("Settled debit entry of net exempted amount 98 initially, should have net amount of 98 after settlement",
                new BigDecimal("98.00"), debitEntry.getNetExemptedAmount());

        DebitEntry splittedDebitEntry = debitEntry.getSplittedDebitEntriesSet().iterator().next();
        assertEquals("Splitted debit entry from debit entry of 2, should have amount of 0.5 after settlement",
                new BigDecimal("0.50"), splittedDebitEntry.getTotalAmount());

    }

}
