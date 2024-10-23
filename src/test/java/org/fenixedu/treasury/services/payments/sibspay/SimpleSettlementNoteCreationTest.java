package org.fenixedu.treasury.services.payments.sibspay;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
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
public class SimpleSettlementNoteCreationTest {

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
    public void testSimplePaymentWithAdvancePaymentCreditNote() {

        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), false);

        DebtAccount debtAccount = debitEntry.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        finantialInstitution.setSplitCreditEntriesWithSettledAmount(true);
        finantialInstitution.setSplitDebitEntriesWithSettledAmount(true);

        SettlementNoteBean bean = new SettlementNoteBean(debtAccount, false, false);
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();
        bean.setFinantialEntity(finantialEntity);
        bean.setAdvancePayment(true);

        bean.setDocNumSeries(
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(), finantialEntity));
        bean.setDate(new DateTime());

        bean.getDebitEntries().stream().filter(de -> de.getInvoiceEntry() == debitEntry).forEach(de -> {
            de.setIncluded(true);
            de.setSettledAmount(new BigDecimal("100"));
        });

        bean.getPaymentEntries().add(new PaymentEntryBean(new BigDecimal("150.00"), PaymentMethod.findByCode("NU"), null));

        SettlementNote settlementNote = SettlementNote.createSettlementNote(bean);

        assertEquals(new BigDecimal("50.00"), settlementNote.getAdvancedPaymentCreditNote().getTotalAmount());

    }

}
