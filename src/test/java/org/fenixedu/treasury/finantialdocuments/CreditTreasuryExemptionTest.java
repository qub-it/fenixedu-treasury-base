package org.fenixedu.treasury.finantialdocuments;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.fenixedu.treasury.base.BasicTreasuryUtils;
import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.AdhocCustomer;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.ProductGroup;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.VatExemptionReason;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.CreditNote;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxSettings;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.FixedTariff;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRateType;
import org.fenixedu.treasury.domain.tariff.InterestRateEntry;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.paymentplans.PaymentPlanTestsUtilities;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class CreditTreasuryExemptionTest {

    public static final String DEBT_PRODUCT = "PAGAMENTO";

    @BeforeClass
    public static void startUp() {
        BasicTreasuryUtils.startup(() -> {

            AdhocCustomer customer = AdhocCustomer.create(CustomerType.findByCode("ADHOC").findFirst().get(),
                    Customer.DEFAULT_FISCAL_NUMBER, "Cliente", "morada", "", "", "", "pt", "", List.of(getFinatialInstitution()));

            DebtAccount.create(getFinatialInstitution(), customer);

            TreasuryExemptionType.create("TreasuryExemptionType", BasicTreasuryUtils.ls("TreasuryExemptionType"),
                    TreasuryConstants.HUNDRED_PERCENT, true);

            Vat.findActiveUnique(VatType.findByCode("INT"), FinantialInstitution.findUnique().get(),
                    new LocalDate(2021, 1, 1).toDateTimeAtStartOfDay()).get().setTaxRate(new BigDecimal("0"));

            InterestRateType globalInterestRateType = GlobalInterestRateType.findUnique().get();

            InterestRateEntry globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2020, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("4.705"));
            globalInterestRate.setApplyPaymentMonth(false);

            globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2021, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("4.705"));
            globalInterestRate.setApplyPaymentMonth(false);

            globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2022, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("4.705"));
            globalInterestRate.setApplyPaymentMonth(false);

            globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2023, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("4.705"));
            globalInterestRate.setApplyPaymentMonth(false);

            globalInterestRate =
                    InterestRateEntry.findUniqueAppliedForDate(globalInterestRateType, new LocalDate(2024, 1, 1)).get();
            globalInterestRate.setRate(new BigDecimal("4.705"));
            globalInterestRate.setApplyPaymentMonth(false);

            createPenaltyTaxSettings();
            return null;
        });
    }

    @Test
    public void creditExemptionSimpleTest() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), Series.findByCode("INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntry = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("999.98"), debitEntry);

        assertEquals("Net amount is not equals", new BigDecimal("0.02"), debitEntry.getNetAmount());

        assertEquals("Net amount is not equals", new BigDecimal("999.98"), debitEntry.getNetExemptedAmount());

        assertEquals("Available net amount to credit is not equals", new BigDecimal("0.02"),
                debitEntry.getAvailableNetAmountForCredit());

        debitNote.closeDocument();

        CreditNote creditNote = CreditNote.create(debitNote,
                DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), Series.findByCode("INT")), date, null);

        CreditEntry creditEntry = CreditEntry.create(creditNote, debitEntry.getDescription(), product, vat,
                new BigDecimal("1000"), date, debitEntry, BigDecimal.ONE, Map.of(treasuryExemption, new BigDecimal("999.98")));

        assertEquals("Credit entry amount is not equals", new BigDecimal("1000"), creditEntry.getAmount());

        assertEquals("Credit entry net amount is not equals", new BigDecimal("0.02"), creditEntry.getNetAmount());

        assertEquals("Available net amount to exemption is not equals", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());

        assertEquals("Available net amount to credit is not equals", new BigDecimal("0.00"),
                debitEntry.getAvailableNetAmountForCredit());
    }

    @Test
    public void testAnnuledCreditEntryWithCreditOnExemption() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), Series.findByCode("INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntry = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("999.98"), debitEntry);

        debitNote.closeDocument();

        CreditNote creditNote = CreditNote.create(debitNote,
                DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), Series.findByCode("INT")), date, null);

        CreditEntry.create(creditNote, debitEntry.getDescription(), product, vat, new BigDecimal("1000"), date, debitEntry,
                BigDecimal.ONE, Map.of(treasuryExemption, new BigDecimal("999.98")));

        creditNote.anullDocument("credit annuled");

        assertEquals("Available amount to exemption is not equals", new BigDecimal("999.98"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());

        assertEquals("Available amount to credit is not equals", new BigDecimal("0.02"),
                debitEntry.getAvailableNetAmountForCredit());
    }

    @Test
    public void testTwoCreditsOnExemptionAmount() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), Series.findByCode("INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntry = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("999.98"), debitEntry);

        debitNote.closeDocument();

        {

            CreditNote creditNote = CreditNote.create(debitNote,
                    DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), Series.findByCode("INT")), date, null);

            CreditEntry.create(creditNote, debitEntry.getDescription(), product, vat, new BigDecimal("999.98"), date, debitEntry,
                    BigDecimal.ONE, Map.of(treasuryExemption, new BigDecimal("999.96")));

            assertEquals("Available amount to exemption is not equals", new BigDecimal("0.02"),
                    treasuryExemption.getAvailableNetExemptedAmountForCredit());

            assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                    debitEntry.getAvailableNetAmountForCredit());
        }

        {
            CreditNote creditNote = CreditNote.create(debitNote,
                    DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), Series.findByCode("INT")), date, null);

            CreditEntry.create(creditNote, debitEntry.getDescription(), product, vat, new BigDecimal("0.02"), date, debitEntry,
                    BigDecimal.ONE, Map.of(treasuryExemption, new BigDecimal("0.02")));

            assertEquals("Available amount to exemption is not equals", new BigDecimal("0.00"),
                    treasuryExemption.getAvailableNetExemptedAmountForCredit());

            assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                    debitEntry.getAvailableNetAmountForCredit());
        }

        {
            try {
                CreditNote creditNote = CreditNote.create(debitNote,
                        DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), Series.findByCode("INT")), date,
                        null);

                CreditEntry.create(creditNote, debitEntry.getDescription(), product, vat, new BigDecimal("0.01"), date,
                        debitEntry, BigDecimal.ONE, Collections.emptyMap());

                throw new RuntimeException("should throw a TreasuryException in overflowing the available amount for credit");

            } catch (TreasuryDomainException e) {
                assertEquals("error.CreditEntry.reated.debit.entry.invalid.total.credited.amount", e.getMessage());
            }
        }

    }

    @Test
    public void testDebitNoteAnnulmentWithNoSupportForCreditExemption() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(false);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), Series.findByCode("INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntry = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("999.98"), debitEntry);

        debitNote.closeDocument();

        {
            debitNote.anullDebitNoteWithCreditNote("test", true);

            assertEquals("Available amount to exemption is not equals", new BigDecimal("999.98"),
                    treasuryExemption.getAvailableNetExemptedAmountForCredit());

            assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                    debitEntry.getAvailableNetAmountForCredit());
        }
    }

    @Test
    public void testDebitNoteAnnulmentWithSupportForCreditExemption() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), Series.findByCode("INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntry = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("999.98"), debitEntry);

        debitNote.closeDocument();

        {
            debitNote.anullDebitNoteWithCreditNote("test", true);

            assertEquals("Available amount to exemption is not equals", new BigDecimal("0.00"),
                    treasuryExemption.getAvailableNetExemptedAmountForCredit());

            assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                    debitEntry.getAvailableNetAmountForCredit());
        }
    }

    @Test
    public void testDebitNoteWithTwoEntriesAnnulmentWithSupportForCreditExemption() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), Series.findByCode("INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntryOne = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("999.98"), debitEntryOne);

        DebitEntry debitEntryTwo = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description 2", BigDecimal.ONE, null, date, false, false, debitNote);

        debitNote.closeDocument();

        assertEquals("Available amount to exempt is not equals", new BigDecimal("999.98"), debitEntryOne
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available amount to exempt is not equals", BigDecimal.ZERO, debitEntryTwo
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        {
            debitNote.anullDebitNoteWithCreditNote("test", true);

            assertEquals("Available amount to exemption is not equals", new BigDecimal("0.00"),
                    treasuryExemption.getAvailableNetExemptedAmountForCredit());

            assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                    debitEntryOne.getAvailableNetAmountForCredit());

            assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                    debitEntryTwo.getAvailableNetAmountForCredit());

            assertEquals("Available amount to credit is not equals", BigDecimal.ZERO, debitEntryTwo
                    .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        }
    }

    @Test
    public void testDebitEntryCreditWithNoSupportForCreditExemption() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(false);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), Series.findByCode("INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntryOne = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("999.98"), debitEntryOne);

        DebitEntry debitEntryTwo = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description 2", BigDecimal.ONE, null, date, false, false, debitNote);

        debitNote.closeDocument();

        assertEquals("Available amount to exempt is not equals", new BigDecimal("999.98"), debitEntryOne
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available amount to exempt is not equals", BigDecimal.ZERO, debitEntryTwo
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        debitEntryOne.creditDebitEntry(new BigDecimal("0.01"), "reason", false,
                Map.of(treasuryExemption, new BigDecimal("498.98")));

        assertEquals("Available amount to exemption is not equals", new BigDecimal("999.98"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());

        assertEquals("Available amount to credit is not equals", new BigDecimal("0.01"),
                debitEntryOne.getAvailableNetAmountForCredit());

    }

    @Test
    public void testDebitEntryCreditWithSupportForCreditExemption() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), Series.findByCode("INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntryOne = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("999.98"), debitEntryOne);

        DebitEntry debitEntryTwo = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description 2", BigDecimal.ONE, null, date, false, false, debitNote);

        debitNote.closeDocument();

        assertEquals("Available amount to exempt is not equals", new BigDecimal("999.98"), debitEntryOne
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available amount to exempt is not equals", BigDecimal.ZERO, debitEntryTwo
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        debitEntryOne.creditDebitEntry(new BigDecimal("0.01"), "reason", false,
                Map.of(treasuryExemption, new BigDecimal("498.98")));

        assertEquals("Available amount to exemption is not equals", new BigDecimal("501.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());

        assertEquals("Available amount to credit is not equals", new BigDecimal("0.01"),
                debitEntryOne.getAvailableNetAmountForCredit());

    }

    @Test
    public void testDebitEntryCreditWithSupportForCreditExemptionAndFullExemptionInDebitEntry() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntryOne = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("1000"), debitEntryOne);

        debitNote.closeDocument();

        assertEquals("Available amount to exempt is not equals", new BigDecimal("1000.00"), debitEntryOne
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available amount to exempt is not equals", new BigDecimal("1000.00"),
                debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap(BigDecimal.ZERO).values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                debitEntryOne.getAvailableNetAmountForCredit());

        debitEntryOne.creditDebitEntry(BigDecimal.ZERO, "reason", false, Map.of(treasuryExemption, new BigDecimal("499.50")));

        assertEquals("Available amount to exemption is not equals", new BigDecimal("500.50"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());
    }

    @Test
    public void testDebitEntryPartialCreditWithSupportForCreditExemption() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntryOne = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("467.65"), debitEntryOne);

        debitNote.closeDocument();

        assertEquals("Net amount is not equal", new BigDecimal("532.35"), debitEntryOne.getNetAmount());

        assertEquals("Available exempted amount to credit is not equals", new BigDecimal("467.65"), debitEntryOne
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available exempted amount to credit is not equals", new BigDecimal("0"),
                debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap(new BigDecimal("0")).values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals(0, debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap(new BigDecimal("0")).size());

        assertEquals("Available amount to credit is not equals", new BigDecimal("532.35"),
                debitEntryOne.getAvailableNetAmountForCredit());

        debitEntryOne.creditDebitEntry(new BigDecimal("100.64"), "reason", false,
                debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap(new BigDecimal("100.64")));

        assertEquals("Available amount to credit is not equals", new BigDecimal("431.71"),
                debitEntryOne.getAvailableNetAmountForCredit());

        assertEquals("Available exemption amount for credit is not equals", new BigDecimal("379.24"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());

        assertEquals("Credited exemption amount is not equal", new BigDecimal("88.41"), debitEntryOne.getCreditEntriesSet()
                .iterator().next().getCreditTreasuryExemptionsSet().iterator().next().getCreditedNetExemptedAmount());

        debitEntryOne.creditDebitEntry(new BigDecimal("431.71"), "reason", false,
                debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap());

        assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                debitEntryOne.getAvailableNetAmountForCredit());

        assertEquals("Available exemption amount for credit is not equals", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());

        assertEquals(0, debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap().size());
    }

    @Test
    public void testTwoDebitEntryCreditWithSupportForCreditExemption() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), Series.findByCode("INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntryOne = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemptionOne =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("999.98"), debitEntryOne);

        DebitEntry debitEntryTwo = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description 2", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemption treasuryExemptionTwo =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("500"), debitEntryTwo);

        debitNote.closeDocument();

        assertEquals("Available amount to exempt is not equals", new BigDecimal("999.98"), debitEntryOne
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available amount to exempt is not equals", new BigDecimal("500.00"), debitEntryTwo
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        debitEntryOne.creditDebitEntry(new BigDecimal("0.01"), "reason", false,
                Map.of(treasuryExemptionOne, new BigDecimal("498.98")));

        debitEntryTwo.creditDebitEntry(new BigDecimal("400"), "reason", false,
                Map.of(treasuryExemptionTwo, new BigDecimal("100")));

        assertEquals("Available amount to exemption is not equals", new BigDecimal("501.00"),
                treasuryExemptionOne.getAvailableNetExemptedAmountForCredit());

        assertEquals("Available amount to credit is not equals", new BigDecimal("0.01"),
                debitEntryOne.getAvailableNetAmountForCredit());

        assertEquals("Available amount to exemption is not equals", new BigDecimal("400"),
                treasuryExemptionTwo.getAvailableNetExemptedAmountForCredit());

        assertEquals("Available amount to credit is not equals", new BigDecimal("100.00"),
                debitEntryTwo.getAvailableNetAmountForCredit());
    }

    @Test
    public void testDebitEntryCreditWithSupportForCreditExemptionAndFullExemptionInDebitEntryAndTestExemptionCreditCalculation() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntryOne = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("1000"), debitEntryOne);

        debitNote.closeDocument();

        assertEquals("Available amount to exempt is not equals", new BigDecimal("1000.00"), debitEntryOne
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                debitEntryOne.getAvailableNetAmountForCredit());

        debitEntryOne.creditDebitEntry(BigDecimal.ZERO, "reason", false, Map.of(treasuryExemption, new BigDecimal("499.50")));

        assertEquals("Available amount to exemption is not equals", new BigDecimal("500.50"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());

        // Request again the exemption amount to credit

        assertEquals(1, debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap().size());

        assertEquals(1, debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap(new BigDecimal("0")).size());

        assertEquals(new BigDecimal("500.50"),
                debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap(new BigDecimal("0")).values().iterator().next());

        debitEntryOne.creditDebitEntry(BigDecimal.ZERO, "reason", false, Map.of(treasuryExemption, new BigDecimal("500.50")));

        assertEquals(0, debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap().size());

        assertEquals(0, debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap(new BigDecimal("0")).size());
    }

    private static DebtAccount getDebtAccount() {
        return FenixFramework.getDomainRoot().getDebtAccountsSet().iterator().next();
    }

    private static FinantialInstitution getFinatialInstitution() {
        return FenixFramework.getDomainRoot().getFinantialInstitutionsSet().iterator().next();
    }

    private static void createPenaltyTaxSettings() {
        if (!Product.findUniqueByCode("TX_PEN_ATRASO_PAG").isPresent()) {
            Product.create(ProductGroup.findByCode("OTHER"), "TX_PEN_ATRASO_PAG",
                    PaymentPlanTestsUtilities.ls("Taxa de penalizacao por atraso no pagamento"),
                    PaymentPlanTestsUtilities.ls("Unidade"), true, false, 0, VatType.findByCode("INT"),
                    FinantialInstitution.findAll().collect(Collectors.toList()),
                    VatExemptionReason.findByCode("VatExemptionReason"));

            FinantialInstitution.findAll().flatMap(fi -> FinantialEntity.find(fi)).forEach(fe -> {
                FixedTariff.create(Product.findUniqueByCode("TX_PEN_ATRASO_PAG").get(), null, fe, new BigDecimal("20.00"),
                        new LocalDate(2010, 1, 1).toDateTimeAtStartOfDay(),
                        new LocalDate(2100, 12, 31).toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1),
                        DueDateCalculationType.DAYS_AFTER_CREATION, null, 0, false);
            });
        }

        FinantialInstitution.findAll().flatMap(fi -> FinantialEntity.find(fi)).forEach(fe -> {
            PaymentPenaltyTaxSettings settings =
                    PaymentPenaltyTaxSettings.create(fe, Product.findUniqueByCode("TX_PEN_ATRASO_PAG").get());

            settings.addTargetProducts(Product.findUniqueByCode(PaymentPlanTestsUtilities.DEBT_PRODUCT).get());
            settings.edit(true, settings.getPenaltyProduct(),
                    PaymentPlanTestsUtilities.ls("[Actos Administrativos] Pagamento em atraso - ${debitEntryDescription}"), false,
                    true, false);
        });

        InterestRateType.getDefaultInterestRateType().getInterestRateEntriesSet().forEach(entry -> entry.delete());
        InterestRateEntry.create(InterestRateType.getDefaultInterestRateType(), new LocalDate(1950, 1, 1),
                PaymentPlanTestsUtilities.ls("Juro oficial para o ano 2021"), new BigDecimal("4.705"), true, false);
    }
}
