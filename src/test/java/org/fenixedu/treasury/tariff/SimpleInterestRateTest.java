package org.fenixedu.treasury.tariff;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.document.*;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class SimpleInterestRateTest {

    @BeforeClass
    public static void beforeClass() {
        InterestRateTestsUtilities.startUp();
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                createDueDateExpiredDebitEntries();
                createTreasuryExemptionTypes();
                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static void createTreasuryExemptionTypes() {
        TreasuryExemptionType.create("TE1", new LocalizedString(Locale.ENGLISH, "Teste"), new BigDecimal("50"), true);
    }

    private static void createDueDateExpiredDebitEntries() {
        InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
    }

    @Test
    public void globalInterests_At_20230423_on_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("14.34");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void globalInterests_At_20230423_on_DebitEntry_With_DueDate_20200105_With_PartialPayment() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        SettlementNoteBean bean = new SettlementNoteBean(debitEntry.getDebtAccount(), false, false);

        bean.setFinantialEntity(debitEntry.getFinantialEntity());
        bean.setDocNumSeries(DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(), debitEntry.getFinantialEntity()));
        bean.getInvoiceEntryBean(debitEntry).setIncluded(true);
        bean.getPaymentEntries()
                .add(new SettlementNoteBean.PaymentEntryBean(new BigDecimal("25"), PaymentMethod.findAll().iterator().next(),
                        null));
        bean.setDate(new LocalDate(2021, 1, 5).toDateTimeAtStartOfDay());

        SettlementNote.createSettlementNote(bean);

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("11.59");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void globalInterests_At_20230423_on_DebitEntry_With_DueDate_20200105AndHalfExemptionWithCreditEntry() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(),
                        debitEntry.getFinantialEntity());

        DebitNote debitNoteForDebitEntry =
                DebitNote.createDebitNoteForDebitEntry(debitEntry, null, documentNumberSeries, new DateTime(), new LocalDate(),
                        null, null, null);

        debitNoteForDebitEntry.closeDocument(true);

        TreasuryExemptionType exemptionType = TreasuryExemptionType.findByCode("TE1").iterator().next();

        TreasuryExemption.create(exemptionType, "teste", new BigDecimal("50"), debitEntry);

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("7.17");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void globalInterests_At_20230423_on_DebitEntry_With_DueDate_20200105AndHalfExemptionWithCreditEntry_WithPaymentIn2021() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        SettlementNoteBean bean = new SettlementNoteBean(debitEntry.getDebtAccount(), false, false);

        bean.setFinantialEntity(debitEntry.getFinantialEntity());
        bean.setDocNumSeries(DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(), debitEntry.getFinantialEntity()));
        bean.getInvoiceEntryBean(debitEntry).setIncluded(true);
        bean.getPaymentEntries()
                .add(new SettlementNoteBean.PaymentEntryBean(new BigDecimal("25"), PaymentMethod.findAll().iterator().next(),
                        null));
        bean.setDate(new LocalDate(2021, 1, 5).toDateTimeAtStartOfDay());

        SettlementNote.createSettlementNote(bean);

        TreasuryExemptionType exemptionType = TreasuryExemptionType.findByCode("TE1").iterator().next();

        TreasuryExemption.create(exemptionType, "teste", new BigDecimal("50"), debitEntry);

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("4.42");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void globalInterests_At_20200104_on_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        List<InterestRateBean> interestRateBeansList = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 4));
        BigDecimal interestAmount =
                interestRateBeansList.stream().map(bean -> bean.getInterestAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedInterestAmount = new BigDecimal("0");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount, interestAmount),
                expectedInterestAmount, interestAmount);
    }

    @Test
    public void globalInterests_At_20200105_on_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        List<InterestRateBean> interestRateBeansList = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 5));
        BigDecimal interestAmount =
                interestRateBeansList.stream().map(bean -> bean.getInterestAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedInterestAmount = new BigDecimal("0");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount, interestAmount),
                expectedInterestAmount, interestAmount);
    }

    @Test
    public void globalInterests_With_Partial_Interest_Debit_Entry_Created() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        {
            InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 1)).iterator().next();

            // create interest rate of 13.98
            DebitEntry partialInterestRateDebitEntry =
                    debitEntry.createInterestRateDebitEntry(interestRateBean, new LocalDate(2023, 4, 1).toDateTimeAtStartOfDay(),
                            null);

            assertEquals(
                    String.format("Interest rate of 100 at 2023-04-01 is %s, but was calculated as %s", new BigDecimal("13.98"),
                            partialInterestRateDebitEntry.getTotalAmount()), new BigDecimal("13.98"),
                    partialInterestRateDebitEntry.getTotalAmount());
        }

        InterestRateBean interestRateBean =
                debitEntry.calculateUndebitedInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("0.36");

        assertEquals(String.format("Remaining interest rate of 100 is %s, but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void fixedInterests_At_20230423_On_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);

        debitEntry.getInterestRate().setInterestRateType(InterestRateType.findUniqueByCode("FIXED_AMOUNT").get());
        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("5.00");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void fixedInterests_At_20200104_On_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);

        debitEntry.getInterestRate().setInterestRateType(InterestRateType.findUniqueByCode("FIXED_AMOUNT").get());
        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        List<InterestRateBean> interestRateBeansList = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 4));
        BigDecimal interestAmount =
                interestRateBeansList.stream().map(bean -> bean.getInterestAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedInterestAmount = BigDecimal.ZERO;

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount, interestAmount),
                expectedInterestAmount, interestAmount);
    }

    @Test
    public void fixedInterests_At_20200105_On_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);

        debitEntry.getInterestRate().setInterestRateType(InterestRateType.findUniqueByCode("FIXED_AMOUNT").get());
        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        List<InterestRateBean> interestRateBeansList = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 5));
        BigDecimal interestAmount =
                interestRateBeansList.stream().map(bean -> bean.getInterestAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expectedInterestAmount = BigDecimal.ZERO;

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount, interestAmount),
                expectedInterestAmount, interestAmount);
    }

    @Test
    public void fixedInterests_At_20200106_On_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);

        debitEntry.getInterestRate().setInterestRateType(InterestRateType.findUniqueByCode("FIXED_AMOUNT").get());
        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 6)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("5.00");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void fixedInterests_With_Partial_Interest_Debit_Entry_Created() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));
        debitEntry.getInterestRate().setInterestRateType(InterestRateType.findUniqueByCode("FIXED_AMOUNT").get());
        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("3"));

        {
            InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 1)).iterator().next();

            // create interest rate of 13.98
            DebitEntry partialInterestRateDebitEntry =
                    debitEntry.createInterestRateDebitEntry(interestRateBean, new LocalDate(2023, 4, 1).toDateTimeAtStartOfDay(),
                            null);

            assertEquals(
                    String.format("Interest rate of 100 at 2023-04-01 is %s, but was calculated as %s", new BigDecimal("3.00"),
                            partialInterestRateDebitEntry.getTotalAmount()), new BigDecimal("3.00"),
                    partialInterestRateDebitEntry.getTotalAmount());
        }

        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));

        InterestRateBean interestRateBean =
                debitEntry.calculateUndebitedInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("2.00");

        assertEquals(String.format("Remaining interest rate of 100 is %s, but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void fixedInterests_With_Partial_Interest_Debit_Entry_Created_Check_Total_Interest_Amount() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));
        debitEntry.getInterestRate().setInterestRateType(InterestRateType.findUniqueByCode("FIXED_AMOUNT").get());
        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("3"));

        {
            InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 1)).iterator().next();

            // create interest rate of 13.98
            DebitEntry partialInterestRateDebitEntry =
                    debitEntry.createInterestRateDebitEntry(interestRateBean, new LocalDate(2023, 4, 1).toDateTimeAtStartOfDay(),
                            null);

            assertEquals(
                    String.format("Interest rate of 100 at 2023-04-01 is %s, but was calculated as %s", new BigDecimal("3.00"),
                            partialInterestRateDebitEntry.getTotalAmount()), new BigDecimal("3.00"),
                    partialInterestRateDebitEntry.getTotalAmount());
        }

        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23)).iterator().next();
        BigDecimal expectedInterestAmount = new BigDecimal("5.00");

        assertEquals(String.format("Total interest rate of 100 is %s, but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

}
