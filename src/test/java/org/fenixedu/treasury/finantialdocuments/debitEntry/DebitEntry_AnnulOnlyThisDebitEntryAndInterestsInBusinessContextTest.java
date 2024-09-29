package org.fenixedu.treasury.finantialdocuments.debitEntry;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentStateType;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Series;
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
public class DebitEntry_AnnulOnlyThisDebitEntryAndInterestsInBusinessContextTest {

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
    public void annulSimpleTest() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries = DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(),
                Series.findByCode(getFinatialInstitution(), "INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntry = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        debitEntry.annulOnlyThisDebitEntryAndInterestsInBusinessContext("test");

        assertEquals(FinantialDocumentStateType.ANNULED, debitEntry.getDebitNote().getState());

        // Ensure it is idempotent

        debitEntry.annulOnlyThisDebitEntryAndInterestsInBusinessContext("test");

        assertEquals(FinantialDocumentStateType.ANNULED, debitEntry.getDebitNote().getState());
    }

    @Test
    public void annulClosedTest() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries = DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(),
                Series.findByCode(getFinatialInstitution(), "INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();

        DebitEntry debitEntry = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        debitNote.closeDocument();

        debitEntry.annulOnlyThisDebitEntryAndInterestsInBusinessContext("test");

        assertEquals(FinantialDocumentStateType.CLOSED, debitEntry.getDebitNote().getState());

        assertEquals(1, debitEntry.getCreditEntriesSet().size());

        assertEquals(new BigDecimal("1000.00"), debitEntry.getCreditEntriesSet().iterator().next().getTotalAmount());

        assertEquals(new BigDecimal("1000.00"), debitEntry.getDebitNote().getCreditNoteSet().iterator().next().getTotalAmount());

        // Ensure it is idempotent

        debitEntry.annulOnlyThisDebitEntryAndInterestsInBusinessContext("test");

        assertEquals(FinantialDocumentStateType.CLOSED, debitEntry.getDebitNote().getState());

        assertEquals(1, debitEntry.getCreditEntriesSet().size());

        assertEquals(new BigDecimal("1000.00"), debitEntry.getCreditEntriesSet().iterator().next().getTotalAmount());

        assertEquals(new BigDecimal("1000.00"), debitEntry.getDebitNote().getCreditNoteSet().iterator().next().getTotalAmount());
    }

    @Test
    public void annulWithExemptionClosedTest() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries = DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(),
                Series.findByCode(getFinatialInstitution(), "INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();

        DebitEntry debitEntry = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        debitNote.closeDocument();

        debitEntry.annulOnlyThisDebitEntryAndInterestsInBusinessContext("test");

        assertEquals(FinantialDocumentStateType.CLOSED, debitEntry.getDebitNote().getState());

        assertEquals(1, debitEntry.getCreditEntriesSet().size());

        assertEquals(new BigDecimal("1000.00"), debitEntry.getCreditEntriesSet().iterator().next().getTotalAmount());

        assertEquals(new BigDecimal("1000.00"), debitEntry.getDebitNote().getCreditNoteSet().iterator().next().getTotalAmount());

        // Ensure it is idempotent

        debitEntry.annulOnlyThisDebitEntryAndInterestsInBusinessContext("test");

        assertEquals(FinantialDocumentStateType.CLOSED, debitEntry.getDebitNote().getState());

        assertEquals(1, debitEntry.getCreditEntriesSet().size());

        assertEquals(new BigDecimal("1000.00"), debitEntry.getCreditEntriesSet().iterator().next().getTotalAmount());

        assertEquals(new BigDecimal("1000.00"), debitEntry.getDebitNote().getCreditNoteSet().iterator().next().getTotalAmount());
    }

    @Test
    public void testAnnulDebitEntryCreditWithSupportForCreditExemptionAndFullExemptionInDebitEntry() {

        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        finantialEntity.getFinantialInstitution().setSupportCreditTreasuryExemptions(true);

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries = DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(),
                Series.findByCode(getFinatialInstitution(), "INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        Product product = Product.findUniqueByCode(DEBT_PRODUCT).get();
        DebitEntry debitEntryOne = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description", BigDecimal.ONE, null, date, false, false, debitNote);

        TreasuryExemptionType treasuryExemptionType = TreasuryExemptionType.findByCode("TreasuryExemptionType").iterator().next();

        TreasuryExemption treasuryExemption =
                TreasuryExemption.create(treasuryExemptionType, "reason", new BigDecimal("1000"), debitEntryOne);

        DebitEntry debitEntryTwo = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate,
                null, product, "debt description 2", BigDecimal.ONE, null, date, false, false, debitNote);

        debitNote.closeDocument();

        assertEquals("Available amount to exempt is not equals", new BigDecimal("1000.00"), debitEntryOne
                .calculateDefaultNetExemptedAmountsToCreditMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available amount to exempt is not equals", new BigDecimal("1000.00"),
                debitEntryOne.calculateDefaultNetExemptedAmountsToCreditMap(BigDecimal.ZERO).values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        assertEquals("Available amount to credit is not equals", new BigDecimal("0.00"),
                debitEntryOne.getAvailableNetAmountForCredit());

        debitEntryOne.annulOnlyThisDebitEntryAndInterestsInBusinessContext("test");

        assertEquals("Available amount to exemption is not equals", new BigDecimal("0.00"),
                treasuryExemption.getAvailableNetExemptedAmountForCredit());

        assertEquals(1, debitEntryOne.getCreditEntriesSet().size());

        debitEntryOne.annulOnlyThisDebitEntryAndInterestsInBusinessContext("test");

        assertEquals(1, debitEntryOne.getCreditEntriesSet().size());

        assertEquals(0, debitEntryTwo.getCreditEntriesSet().size());

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
