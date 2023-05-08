package org.fenixedu.treasury.tariff;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.Optional;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.dto.InterestRateBean;
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
    public void globalInterests_At_20230423_on_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23));
        BigDecimal expectedInterestAmount = new BigDecimal("14.34");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void globalInterests_At_20200104_on_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 4));
        BigDecimal expectedInterestAmount = new BigDecimal("0");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void globalInterests_At_20200105_on_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 5));
        BigDecimal expectedInterestAmount = new BigDecimal("0");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

    @Test
    public void globalInterests_With_Partial_Interest_Debit_Entry_Created() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        {
            InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 1));

            // create interest rate of 13.98
            DebitEntry partialInterestRateDebitEntry = debitEntry.createInterestRateDebitEntry(interestRateBean,
                    new LocalDate(2023, 4, 1).toDateTimeAtStartOfDay(), Optional.ofNullable(null));

            assertEquals(
                    String.format("Interest rate of 100 at 2023-04-01 is %s, but was calculated as %s", new BigDecimal("13.98"),
                            partialInterestRateDebitEntry.getTotalAmount()),
                    new BigDecimal("13.98"), partialInterestRateDebitEntry.getTotalAmount());
        }

        InterestRateBean interestRateBean = debitEntry.calculateUndebitedInterestValue(new LocalDate(2023, 4, 23));
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

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23));
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

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 4));
        BigDecimal expectedInterestAmount = BigDecimal.ZERO;

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }
    
    @Test
    public void fixedInterests_At_20200105_On_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);

        debitEntry.getInterestRate().setInterestRateType(InterestRateType.findUniqueByCode("FIXED_AMOUNT").get());
        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 5));
        BigDecimal expectedInterestAmount = BigDecimal.ZERO;

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }
    
    @Test
    public void fixedInterests_At_20200106_On_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry =
                InterestRateTestsUtilities.createDebitEntry(new BigDecimal("100.00"), new LocalDate(2020, 1, 5), true);

        debitEntry.getInterestRate().setInterestRateType(InterestRateType.findUniqueByCode("FIXED_AMOUNT").get());
        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));
        debitEntry.setDueDate(new LocalDate(2020, 1, 5));

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2020, 1, 6));
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
            InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 1));

            // create interest rate of 13.98
            DebitEntry partialInterestRateDebitEntry = debitEntry.createInterestRateDebitEntry(interestRateBean,
                    new LocalDate(2023, 4, 1).toDateTimeAtStartOfDay(), Optional.ofNullable(null));

            assertEquals(
                    String.format("Interest rate of 100 at 2023-04-01 is %s, but was calculated as %s", new BigDecimal("3.00"),
                            partialInterestRateDebitEntry.getTotalAmount()),
                    new BigDecimal("3.00"), partialInterestRateDebitEntry.getTotalAmount());
        }

        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));
        
        InterestRateBean interestRateBean = debitEntry.calculateUndebitedInterestValue(new LocalDate(2023, 4, 23));
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
            InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 1));

            // create interest rate of 13.98
            DebitEntry partialInterestRateDebitEntry = debitEntry.createInterestRateDebitEntry(interestRateBean,
                    new LocalDate(2023, 4, 1).toDateTimeAtStartOfDay(), Optional.ofNullable(null));

            assertEquals(
                    String.format("Interest rate of 100 at 2023-04-01 is %s, but was calculated as %s", new BigDecimal("3.00"),
                            partialInterestRateDebitEntry.getTotalAmount()),
                    new BigDecimal("3.00"), partialInterestRateDebitEntry.getTotalAmount());
        }

        debitEntry.getInterestRate().setInterestFixedAmount(new BigDecimal("5"));
        
        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23));
        BigDecimal expectedInterestAmount = new BigDecimal("5.00");

        assertEquals(String.format("Total interest rate of 100 is %s, but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }
    
}
