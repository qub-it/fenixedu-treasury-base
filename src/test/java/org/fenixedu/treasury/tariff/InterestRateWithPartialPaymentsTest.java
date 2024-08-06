package org.fenixedu.treasury.tariff;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.PaymentEntryBean;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class InterestRateWithPartialPaymentsTest {

    @BeforeClass
    public static void beforeClass() {
        InterestRateTestsUtilities.startUp();
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                createDueDateExpiredDebitEntries();
                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static void createDueDateExpiredDebitEntries() {
        InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
    }

    @Test
    public void globalInterests_At_20230423_on_DebitEntry_With_DueDate_20200105_And_Payment_At_20210210() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        createSettlementNote(debitEntry, new BigDecimal("50"), new LocalDate(2021, 2, 10));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("9.07");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void globalInterests_WithPartialPayment_And_With_Partial_Interest_Debit_Entry_Created() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        createSettlementNote(debitEntry, new BigDecimal("50"), new LocalDate(2021, 2, 10));

        {
            InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 1)).iterator().next();

            // create interest rate of 13.98
            DebitEntry partialInterestRateDebitEntry = debitEntry.createInterestRateDebitEntry(interestRateBean,
                    new LocalDate(2023, 4, 1).toDateTimeAtStartOfDay(), null);

            assertEquals(
                    String.format("Interest rate of 100 at 2023-04-01 is %s, but was calculated as %s", new BigDecimal("8.89"),
                            partialInterestRateDebitEntry.getTotalAmount()),
                    new BigDecimal("8.89"), partialInterestRateDebitEntry.getTotalAmount());
        }

        InterestRateBean interestRateBean =
                debitEntry.calculateUndebitedInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("0.18");

        assertEquals(String.format("Remaining interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    private void createSettlementNote(DebitEntry debitEntry, BigDecimal amount, LocalDate paymentDate) {
        SettlementNoteBean settlementNoteBean = new SettlementNoteBean(debitEntry.getDebtAccount(), false, true);
        FinantialInstitution finantialInstitution = debitEntry.getDebtAccount().getFinantialInstitution();

        settlementNoteBean.setDocNumSeries(DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get());
        settlementNoteBean.setDate(paymentDate.toDateTimeAtStartOfDay());
        settlementNoteBean.getDebitEntries().stream().filter(d -> d.getInvoiceEntry() == debitEntry).findFirst().get()
                .setIncluded(true);
        settlementNoteBean.getPaymentEntries().add(new PaymentEntryBean(amount, PaymentMethod.findByCode("NU"), null));

        SettlementNote.createSettlementNote(settlementNoteBean);
    }

    @Test
    public void fixedInterests_At_20230423_On_DebitEntry_With_DueDate_20200105_With_Partial_Payment() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);

        debitEntry.getInterestRate().setInterestRateType(InterestRateType.findUniqueByCode("FIXED_AMOUNT").get());
        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        createSettlementNote(debitEntry, new BigDecimal("50"), new LocalDate(2021, 2, 10));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("5.00");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

}
