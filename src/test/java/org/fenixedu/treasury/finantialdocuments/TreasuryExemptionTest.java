package org.fenixedu.treasury.finantialdocuments;

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
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxSettings;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxTreasuryEvent;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.FixedTariff;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRateType;
import org.fenixedu.treasury.domain.tariff.InterestRateEntry;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.paymentplans.PaymentPlanTestsUtilities;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class TreasuryExemptionTest {
    public static final String DEBT_PRODUCT = "PAGAMENTO";
    private DebitEntry debitEntry;
    private TreasuryEvent treasuryEvent;

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

    @Before
    public void before() {
        FinantialEntity finantialEntity = FinantialEntity.findAll().iterator().next();

        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DocumentNumberSeries documentNumberSeries = DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(),
                Series.findByCode(getFinatialInstitution(), "INT"));
        DebitNote debitNote = DebitNote.create(finantialEntity, getDebtAccount(), null, documentNumberSeries, date,
                date.toLocalDate(), null, Collections.emptyMap(), null, null);

        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        this.debitEntry = DebitEntry.create(finantialEntity, getDebtAccount(), null, vat, new BigDecimal(1000), dueDate, null,
                Product.findUniqueByCode(DEBT_PRODUCT).get(), "debt description", BigDecimal.ONE, null, date, false, false,
                debitNote);

        this.treasuryEvent = PaymentPenaltyTaxTreasuryEvent
                .checkAndCreatePaymentPenaltyTax(this.debitEntry, dueDate.plusDays(15), null, false).getTreasuryEvent();

        this.debitEntry.setTreasuryEvent(this.treasuryEvent);
    }

    @Test
    public void oneExemptionPartialInPreparingDebtNote() {
        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(10), this.debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(10), exemption.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", new BigDecimal(10),
                this.debitEntry.getNetExemptedAmount().setScale(0));

    }

    @Test
    public void oneExemptionInPreparingDebtNote() {
        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(1000), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(1000), exemption.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", new BigDecimal(1000),
                debitEntry.getNetExemptedAmount().setScale(0));

    }

    @Test(expected = TreasuryDomainException.class)
    public void oneExemptionWithAmountGreaterThanDebtAmountInPreparingDebtNote() {
        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(1001), debitEntry);
    }

    @Test(expected = TreasuryDomainException.class)
    public void oneExemptionPartialInAnnulDebtNote() {
        FenixFramework.atomic(() -> {
            debitEntry.getDebitNote().setState(FinantialDocumentStateType.ANNULED);
        });

        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(1000), debitEntry);
    }

    @Test
    public void oneExemptionPartialInCloseDebtNote() {
        FenixFramework.atomic(() -> {
            debitEntry.getDebitNote().setState(FinantialDocumentStateType.CLOSED);
        });

        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(100), exemption.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", BigDecimal.ZERO, debitEntry.getNetExemptedAmount().setScale(0));
        assertEquals("Credit Entry Exempted amount not equals", new BigDecimal(100),
                exemption.getCreditEntry().getTotalAmount().setScale(0));

    }

    @Test
    public void twoExemptionInPreparingDebtNote() {
        TreasuryExemption exemption1 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(100), exemption1.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", new BigDecimal(100),
                debitEntry.getNetExemptedAmount().setScale(0));

        TreasuryExemption exemption2 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(50), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(50), exemption2.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", new BigDecimal(150),
                debitEntry.getNetExemptedAmount().setScale(0));

    }

    @Test
    public void threeExemptionInPreparingDebtNote() {
        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 100", new BigDecimal(100), exemption.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 100", new BigDecimal(100),
                debitEntry.getNetExemptedAmount().setScale(0));

        exemption = TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                "reason", new BigDecimal(50), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 50", new BigDecimal(50), exemption.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 50", new BigDecimal(150),
                debitEntry.getNetExemptedAmount().setScale(0));

        exemption = TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                "reason", new BigDecimal(25), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 25", new BigDecimal(25), exemption.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 25", new BigDecimal(175),
                debitEntry.getNetExemptedAmount().setScale(0));
    }

    @Test
    public void twoExemptionInPreparingDebtNoteAndDeleteOne() {
        TreasuryExemption exemption1 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 100", new BigDecimal(100), exemption1.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 100", new BigDecimal(100),
                debitEntry.getNetExemptedAmount().setScale(0));

        TreasuryExemption exemption2 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(50), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 50", new BigDecimal(50), exemption2.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 50", new BigDecimal(150),
                debitEntry.getNetExemptedAmount().setScale(0));

        exemption1.revertExemption();
        assertEquals("Debit Entry Exempted amount not equals Delete E = 100", new BigDecimal(50),
                debitEntry.getNetExemptedAmount().setScale(0));

    }

    @Test(expected = TreasuryDomainException.class)
    public void exemptionInPreparingDebtNoteAndDeleteOneAfterClose() {
        TreasuryExemption exemption1 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 100", new BigDecimal(100), exemption1.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 100", new BigDecimal(100),
                debitEntry.getNetExemptedAmount().setScale(0));

        debitEntry.getDebitNote().closeDocument();
        assertEquals("DebitEntry closed", FinantialDocumentStateType.CLOSED, debitEntry.getDebitNote().getState());

        exemption1.revertExemption();

    }

    @Test(expected = TreasuryDomainException.class)
    public void exemptionInClosedDebtNoteAndDeleteOneAfterClose() {
        debitEntry.getDebitNote().closeDocument();

        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(100), exemption.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", BigDecimal.ZERO, debitEntry.getNetExemptedAmount().setScale(0));
        assertEquals("Credit Entry Exempted amount not equals", new BigDecimal(100),
                exemption.getCreditEntry().getTotalAmount().setScale(0));
        exemption.getCreditEntry().getCreditNote().closeDocument();
        exemption.revertExemption();
    }

    @Test
    public void twoExemptionInClosedDebtNote() {
        debitEntry.getDebitNote().closeDocument();

        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(100), exemption.getNetExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", BigDecimal.ZERO, debitEntry.getNetExemptedAmount().setScale(0));
        assertEquals("Debit Entry Available amount not equals", new BigDecimal(900.00).setScale(2),
                debitEntry.getAvailableAmountWithVatForCredit());
        assertEquals("Credit Entry Exempted amount not equals", new BigDecimal(100),
                exemption.getCreditEntry().getTotalAmount().setScale(0));

        TreasuryExemption exemption1 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(), "reason",
                        new BigDecimal(100), debitEntry);

        assertEquals("1 Exemption Exempted amount not equals", new BigDecimal(100), exemption1.getNetExemptedAmount());
        assertEquals("1 Debit Entry Exempted amount not equals", BigDecimal.ZERO, debitEntry.getNetExemptedAmount().setScale(0));
        assertEquals("1 Debit Entry Available amount not equals", new BigDecimal(800.00).setScale(2),
                debitEntry.getAvailableAmountWithVatForCredit());
        assertEquals("1 Credit Entry Exempted amount not equals", new BigDecimal(100),
                exemption1.getCreditEntry().getTotalAmount().setScale(0));
//        assertEquals("1 Credit Entry Exempted state not equals", FinantialDocumentStateType.PREPARING,
//                exemption1.getCreditEntry().getCreditNote().getState());

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
