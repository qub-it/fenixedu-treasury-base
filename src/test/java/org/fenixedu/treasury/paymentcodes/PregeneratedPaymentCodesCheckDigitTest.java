package org.fenixedu.treasury.paymentcodes;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.AdhocCustomer;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRateType;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.tariff.InterestRateTestsUtilities;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PregeneratedPaymentCodesCheckDigitTest {

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
    public void testPregeneration() {
        DebtAccount debtAccount = createDebtAccount("one");

        DebitEntry debitEntryOne = createDebitEntry(debtAccount, new BigDecimal("100.00"), new LocalDate(2024, 1, 5), false);

        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        FinantialEntity finantialEntity = debitEntryOne.getFinantialEntity();

        LocalDate validFrom = new LocalDate(2024, 1, 1);
        LocalDate validTo = new LocalDate(2030, 12, 31);

        SibsPaymentCodePool sibsPaymentCodePool =
                SibsPaymentCodePool.create(finantialInstitution, finantialEntity, "Check digit pool", true, "12345", 1000000,
                        2000000, BigDecimal.ZERO, new BigDecimal("99999.99"), validFrom, validTo, true, true, null, null);

        assertEquals(1000000l, sibsPaymentCodePool.getNextReferenceCode());

        SibsReferenceCode pregenerateSibsReferenceCode =
                sibsPaymentCodePool.pregenerateSibsReferenceCode(debtAccount, new BigDecimal("100.00"));

        assertEquals(1000001l, sibsPaymentCodePool.getNextReferenceCode());

        assertNotNull(pregenerateSibsReferenceCode);

        assertEquals(debtAccount, pregenerateSibsReferenceCode.getPregeneratedReferenceDebtAccount());

        assertEquals(1, sibsPaymentCodePool.getSibsReferenceCodesSet().size());

        SibsPaymentRequest sibsPaymentRequestOne =
                sibsPaymentCodePool.createSibsPaymentRequest(debtAccount, Set.of(debitEntryOne), Collections.emptySet());

        assertEquals(1000001l, sibsPaymentCodePool.getNextReferenceCode());

        assertEquals(pregenerateSibsReferenceCode, sibsPaymentRequestOne.getSibsReferenceCode());

        assertEquals(null, sibsPaymentRequestOne.getSibsReferenceCode().getPregeneratedReferenceDebtAccount());

        assertEquals(1, sibsPaymentCodePool.getSibsReferenceCodesSet().size());

        sibsPaymentRequestOne.anull();

        SibsPaymentRequest sibsPaymentRequestTwo =
                sibsPaymentCodePool.createSibsPaymentRequest(debtAccount, Set.of(debitEntryOne), Collections.emptySet());

        assertEquals(1000002l, sibsPaymentCodePool.getNextReferenceCode());

        assertNotEquals(pregenerateSibsReferenceCode, sibsPaymentRequestTwo.getSibsReferenceCode());

        assertEquals(2, sibsPaymentCodePool.getSibsReferenceCodesSet().size());
    }

    @Test
    public void testPregenerationTwoDifferentDebtAccounts() {
        DebtAccount debtAccountOne = createDebtAccount("one");

        DebitEntry debitEntryOne = createDebitEntry(debtAccountOne, new BigDecimal("100.00"), new LocalDate(2024, 1, 5), false);

        FinantialInstitution finantialInstitution = debtAccountOne.getFinantialInstitution();
        FinantialEntity finantialEntity = debitEntryOne.getFinantialEntity();

        LocalDate validFrom = new LocalDate(2024, 1, 1);
        LocalDate validTo = new LocalDate(2030, 12, 31);

        SibsPaymentCodePool sibsPaymentCodePool =
                SibsPaymentCodePool.create(finantialInstitution, finantialEntity, "Check digit pool", true, "12346", 1000000,
                        2000000, BigDecimal.ZERO, new BigDecimal("99999.99"), validFrom, validTo, true, true, null, null);

        assertEquals(1000000l, sibsPaymentCodePool.getNextReferenceCode());

        DebtAccount debtAccountTwo = createDebtAccount("two");

        SibsReferenceCode pregenerateSibsReferenceCode =
                sibsPaymentCodePool.pregenerateSibsReferenceCode(debtAccountTwo, new BigDecimal("100.00"));

        assertEquals(1000001l, sibsPaymentCodePool.getNextReferenceCode());

        assertEquals(0, debtAccountOne.getPregeneratedSibsReferenceCodesSet().size());

        SibsPaymentRequest sibsPaymentRequestOne =
                sibsPaymentCodePool.createSibsPaymentRequest(debtAccountOne, Set.of(debitEntryOne), Collections.emptySet());

        assertEquals(1000002l, sibsPaymentCodePool.getNextReferenceCode());

        assertNotEquals(pregenerateSibsReferenceCode, sibsPaymentRequestOne.getSibsReferenceCode());

        assertEquals(1, debtAccountTwo.getPregeneratedSibsReferenceCodesSet().size());

        assertEquals(2, sibsPaymentCodePool.getSibsReferenceCodesSet().size());

    }

    @Test
    public void testPregenerationDifferentAmounts() {
        DebtAccount debtAccountOne = createDebtAccount("one");
        DebitEntry debitEntryOne = createDebitEntry(debtAccountOne, new BigDecimal("100.00"), new LocalDate(2024, 1, 5), false);

        FinantialInstitution finantialInstitution = debtAccountOne.getFinantialInstitution();
        FinantialEntity finantialEntity = debitEntryOne.getFinantialEntity();

        LocalDate validFrom = new LocalDate(2024, 1, 1);
        LocalDate validTo = new LocalDate(2030, 12, 31);

        SibsPaymentCodePool sibsPaymentCodePool =
                SibsPaymentCodePool.create(finantialInstitution, finantialEntity, "Check digit pool", true, "12347", 1000000,
                        2000000, BigDecimal.ZERO, new BigDecimal("99999.99"), validFrom, validTo, true, true, null, null);

        assertEquals(1000000l, sibsPaymentCodePool.getNextReferenceCode());

        SibsReferenceCode pregenerateSibsReferenceCode =
                sibsPaymentCodePool.pregenerateSibsReferenceCode(debtAccountOne, new BigDecimal("99.99"));

        assertEquals(1000001l, sibsPaymentCodePool.getNextReferenceCode());

        assertEquals(1, debtAccountOne.getPregeneratedSibsReferenceCodesSet().size());

        SibsPaymentRequest sibsPaymentRequestOne =
                sibsPaymentCodePool.createSibsPaymentRequest(debtAccountOne, Set.of(debitEntryOne), Collections.emptySet());

        assertEquals(1000002l, sibsPaymentCodePool.getNextReferenceCode());

        assertNotEquals(pregenerateSibsReferenceCode, sibsPaymentRequestOne.getSibsReferenceCode());

        assertEquals(1, debtAccountOne.getPregeneratedSibsReferenceCodesSet().size());

        assertEquals(2, sibsPaymentCodePool.getSibsReferenceCodesSet().size());

    }

    @Test
    public void testPregenerationDifferentDatesFuture() {
        DebtAccount debtAccountOne = createDebtAccount("one");
        DebitEntry debitEntryOne = createDebitEntry(debtAccountOne, new BigDecimal("100.00"), new LocalDate(2024, 1, 5), false);

        FinantialInstitution finantialInstitution = debtAccountOne.getFinantialInstitution();
        FinantialEntity finantialEntity = debitEntryOne.getFinantialEntity();

        LocalDate validFrom = new LocalDate(2025, 1, 1);
        LocalDate validTo = new LocalDate(2030, 12, 31);

        SibsPaymentCodePool sibsPaymentCodePool =
                SibsPaymentCodePool.create(finantialInstitution, finantialEntity, "Check digit pool", true, "12348", 1000000,
                        2000000, BigDecimal.ZERO, new BigDecimal("99999.99"), validFrom, validTo, true, true, null, null);

        assertEquals(1000000l, sibsPaymentCodePool.getNextReferenceCode());

        SibsReferenceCode pregenerateSibsReferenceCode =
                sibsPaymentCodePool.pregenerateSibsReferenceCode(debtAccountOne, new BigDecimal("100.00"));

        assertEquals(1000001l, sibsPaymentCodePool.getNextReferenceCode());

        assertEquals(1, debtAccountOne.getPregeneratedSibsReferenceCodesSet().size());

        sibsPaymentCodePool.setValidFrom(new LocalDate(2024, 1, 1));

        SibsPaymentRequest sibsPaymentRequestOne =
                sibsPaymentCodePool.createSibsPaymentRequest(debtAccountOne, Set.of(debitEntryOne), Collections.emptySet());

        assertEquals(1000002l, sibsPaymentCodePool.getNextReferenceCode());

        assertNotEquals(pregenerateSibsReferenceCode, sibsPaymentRequestOne.getSibsReferenceCode());

        assertEquals(1, debtAccountOne.getPregeneratedSibsReferenceCodesSet().size());

        assertEquals(2, sibsPaymentCodePool.getSibsReferenceCodesSet().size());

    }

    @Test
    public void testPregenerationDifferentDatesPast() {
        DebtAccount debtAccountOne = createDebtAccount("one");
        DebitEntry debitEntryOne = createDebitEntry(debtAccountOne, new BigDecimal("100.00"), new LocalDate(2024, 1, 5), false);

        FinantialInstitution finantialInstitution = debtAccountOne.getFinantialInstitution();
        FinantialEntity finantialEntity = debitEntryOne.getFinantialEntity();

        LocalDate validFrom = new LocalDate(2024, 1, 1);
        LocalDate validTo = new LocalDate(2024, 1, 10);

        SibsPaymentCodePool sibsPaymentCodePool =
                SibsPaymentCodePool.create(finantialInstitution, finantialEntity, "Check digit pool", true, "12349", 1000000,
                        2000000, BigDecimal.ZERO, new BigDecimal("99999.99"), validFrom, validTo, true, true, null, null);

        assertEquals(1000000l, sibsPaymentCodePool.getNextReferenceCode());

        SibsReferenceCode pregenerateSibsReferenceCode =
                sibsPaymentCodePool.pregenerateSibsReferenceCode(debtAccountOne, new BigDecimal("100.00"));

        assertEquals(1000001l, sibsPaymentCodePool.getNextReferenceCode());

        assertEquals(1, debtAccountOne.getPregeneratedSibsReferenceCodesSet().size());

        sibsPaymentCodePool.setValidTo(new LocalDate(2030, 1, 1));

        SibsPaymentRequest sibsPaymentRequestOne =
                sibsPaymentCodePool.createSibsPaymentRequest(debtAccountOne, Set.of(debitEntryOne), Collections.emptySet());

        assertEquals(1000002l, sibsPaymentCodePool.getNextReferenceCode());

        assertNotEquals(pregenerateSibsReferenceCode, sibsPaymentRequestOne.getSibsReferenceCode());

        assertEquals(1, debtAccountOne.getPregeneratedSibsReferenceCodesSet().size());

        assertEquals(2, sibsPaymentCodePool.getSibsReferenceCodesSet().size());

    }

    public static DebtAccount createDebtAccount(String name) {
        FinantialInstitution finantialInstitution = FinantialInstitution.findAll().iterator().next();

        AdhocCustomer create = AdhocCustomer.create(CustomerType.findByCode("ADHOC").findFirst().get(),
                Customer.DEFAULT_FISCAL_NUMBER, name, "morada", "", "", "", "pt", "", List.of(finantialInstitution));

        return DebtAccount.create(finantialInstitution, create);
    }

    public static final String DEBT_PRODUCT = "PAGAMENTO";

    public static DebitEntry createDebitEntry(DebtAccount debtAccount, BigDecimal debitAmount, LocalDate dueDate,
            boolean applyInterest) {
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), finantialInstitution, dueDate.toDateTimeAtStartOfDay()).get();
        TreasuryEvent treasuryEvent = null;
        DebitEntry debitEntry = DebitEntry.create(finantialEntity, debtAccount, treasuryEvent, vat, debitAmount, dueDate, null,
                Product.findUniqueByCode(DEBT_PRODUCT).get(), "debt " + debitAmount, BigDecimal.ONE, null,
                dueDate.toDateTimeAtStartOfDay(), false, false, null);
        if (applyInterest) {
            final int numberOfDaysAfterDueDate = 1;
            final boolean applyInFirstWorkday = false;
            final int maximumDaysToApplyPenalty = 0;
            final BigDecimal rate = null;

            InterestRateType globalInterestRateType = GlobalInterestRateType.findUnique().get();

            InterestRate interestRate = InterestRate.createForDebitEntry(debitEntry, globalInterestRateType,
                    numberOfDaysAfterDueDate, applyInFirstWorkday, maximumDaysToApplyPenalty, BigDecimal.ZERO, rate);
            debitEntry.changeInterestRate(interestRate);
        }
        return debitEntry;
    }

}
