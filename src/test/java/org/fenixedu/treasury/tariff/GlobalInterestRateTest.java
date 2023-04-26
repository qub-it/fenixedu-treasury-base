package org.fenixedu.treasury.tariff;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class GlobalInterestRateTest {

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
    public void interests_At_20230423_on_DebitEntry_With_DueDate_20200105() {
        DebitEntry debitEntry = DebitEntry.findAll().iterator().next();

        InterestRateBean interestRateBean = debitEntry.calculateAllInterestValue(new LocalDate(2023, 4, 23));
        BigDecimal expectedInterestAmount = new BigDecimal("14.34");

        assertEquals(String.format("Interest rate of 100 is %s but was calculated as %s", expectedInterestAmount,
                interestRateBean.getInterestAmount()), expectedInterestAmount, interestRateBean.getInterestAmount());
    }

}
