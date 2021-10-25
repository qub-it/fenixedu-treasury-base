package org.fenixedu.treasury.finantialdocuments;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.base.BasicTreasuryUtils;
import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.AdhocCustomer;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.FiscalCountryRegion;
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
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxSettings;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxTreasuryEvent;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.FixedTariff;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRate;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.tariff.InterestType;
import org.fenixedu.treasury.domain.tariff.Tariff;
import org.fenixedu.treasury.paymentplans.PaymentPlanTestsUtilities;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.fasterxml.jackson.databind.introspect.TypeResolutionContext.Basic;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.District;
import pt.ist.standards.geographic.Municipality;
import pt.ist.standards.geographic.Planet;

@RunWith(FenixFrameworkRunner.class)

public class TreasuryExemptionTest {
    public static final String DEBT_PRODUCT = "DEBT";
    private DebitEntry debitEntry;
    private TreasuryEvent treasuryEvent;

    @BeforeClass
    public static void startUp() {
        BasicTreasuryUtils.startup(() -> {
            ProductGroup productGroup = ProductGroup.create("JUnitProductGroup", BasicTreasuryUtils.ls("JUnit ProductGroup"));

            Product product = Product.create(productGroup, DEBT_PRODUCT, BasicTreasuryUtils.ls("Dívida Pagamento"),
                    BasicTreasuryUtils.ls("Unidade"), true, false, 0, VatType.findByCode("INT"),
                    List.of(getFinatialInstitution()), VatExemptionReason.findByCode("M07"));

            Series series = Series.create(getFinatialInstitution(), "SERIES", BasicTreasuryUtils.ls("series"), false, false,
                    false, true, true);
            Vat.create(VatType.findByCode("INT"), getFinatialInstitution(), new BigDecimal("0"),
                    new LocalDate(2000, 1, 1).toDateTimeAtStartOfDay(), new LocalDate(2050, 1, 1).toDateTimeAtStartOfDay());

            AdhocCustomer customer = AdhocCustomer.create(CustomerType.findByCode("STUDENT").findFirst().get(),
                    Customer.DEFAULT_FISCAL_NUMBER, "Diogo", "morada", "", "", "", "pt", "", List.of(getFinatialInstitution()));

            DebtAccount debtAccount = DebtAccount.create(getFinatialInstitution(), customer);

            TreasuryExemptionType exemptionType = TreasuryExemptionType.create("TreasuryExemptionType",
                    BasicTreasuryUtils.ls("TreasuryExemptionType"), null, true);
            createPenaltyTaxSettings();
            return null;
        });
    }

    @Before
    public void before() {
        DateTime date = new LocalDate(2021, 9, 1).toDateTimeAtStartOfDay();
        LocalDate dueDate = new LocalDate(2021, 9, 30);

        DebitNote debitNote = DebitNote.create(getDebtAccount(), DocumentNumberSeries
                .find(FinantialDocumentType.findForDebitNote(), Series.findByCode(getFinatialInstitution(), "SERIES")), date);
        Vat vat = Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), date).get();

        debitEntry = DebitEntry.create(Optional.of(debitNote), getDebtAccount(), null, vat, new BigDecimal(1000), dueDate, null,
                Product.findUniqueByCode(DEBT_PRODUCT).get(), "debt description", BigDecimal.ONE, null, date);

        treasuryEvent = PaymentPenaltyTaxTreasuryEvent.checkAndCreatePaymentPenaltyTax(debitEntry, dueDate.plusDays(15))
                .getTreasuryEvent();
        debitEntry.setTreasuryEvent(treasuryEvent);

    }

    @Test
    public void oneExemptionPartialInPreparingDebtNote() {
        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(10), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(10), exemption.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", new BigDecimal(10), debitEntry.getExemptedAmount().setScale(0));

    }

    @Test
    public void oneExemptionInPreparingDebtNote() {
        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(1000), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(1000), exemption.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", new BigDecimal(1000), debitEntry.getExemptedAmount().setScale(0));

    }

    @Test(expected = TreasuryDomainException.class)
    public void oneExemptionWithAmountGreaterThanDebtAmountInPreparingDebtNote() {
        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(1001), debitEntry);
    }

    @Test(expected = TreasuryDomainException.class)
    public void oneExemptionPartialInAnnulDebtNote() {
        FenixFramework.atomic(() -> {
            debitEntry.getDebitNote().setState(FinantialDocumentStateType.ANNULED);
        });

        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(1000), debitEntry);
    }

    @Test
    public void oneExemptionPartialInCloseDebtNote() {
        FenixFramework.atomic(() -> {
            debitEntry.getDebitNote().setState(FinantialDocumentStateType.CLOSED);
        });

        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(100), exemption.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", BigDecimal.ZERO, debitEntry.getExemptedAmount().setScale(0));
        assertEquals("Credit Entry Exempted amount not equals", new BigDecimal(100),
                exemption.getCreditEntry().getTotalAmount().setScale(0));

    }

    @Test
    public void twoExemptionInPreparingDebtNote() {
        TreasuryExemption exemption1 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(100), exemption1.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", new BigDecimal(100), debitEntry.getExemptedAmount().setScale(0));

        TreasuryExemption exemption2 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(50), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(50), exemption2.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", new BigDecimal(150), debitEntry.getExemptedAmount().setScale(0));

    }

    @Test
    public void threeExemptionInPreparingDebtNote() {
        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 100", new BigDecimal(100), exemption.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 100", new BigDecimal(100),
                debitEntry.getExemptedAmount().setScale(0));

        exemption = TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                treasuryEvent, "reason", new BigDecimal(50), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 50", new BigDecimal(50), exemption.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 50", new BigDecimal(150),
                debitEntry.getExemptedAmount().setScale(0));

        exemption = TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                treasuryEvent, "reason", new BigDecimal(25), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 25", new BigDecimal(25), exemption.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 25", new BigDecimal(175),
                debitEntry.getExemptedAmount().setScale(0));
    }

    @Test
    public void twoExemptionInPreparingDebtNoteAndDeleteOne() {
        TreasuryExemption exemption1 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 100", new BigDecimal(100), exemption1.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 100", new BigDecimal(100),
                debitEntry.getExemptedAmount().setScale(0));

        TreasuryExemption exemption2 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(50), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 50", new BigDecimal(50), exemption2.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 50", new BigDecimal(150),
                debitEntry.getExemptedAmount().setScale(0));

        exemption1.revertExemption();
        assertEquals("Debit Entry Exempted amount not equals Delete E = 100", new BigDecimal(50),
                debitEntry.getExemptedAmount().setScale(0));

    }

    @Test(expected = TreasuryDomainException.class)
    public void exemptionInPreparingDebtNoteAndDeleteOneAfterClose() {
        TreasuryExemption exemption1 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals E = 100", new BigDecimal(100), exemption1.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals E = 100", new BigDecimal(100),
                debitEntry.getExemptedAmount().setScale(0));

        debitEntry.getDebitNote().closeDocument();
        assertEquals("DebitEntry closed", FinantialDocumentStateType.CLOSED, debitEntry.getDebitNote().getState());

        exemption1.revertExemption();

    }

    @Test(expected = TreasuryDomainException.class)
    public void exemptionInClosedDebtNoteAndDeleteOneAfterClose() {
        debitEntry.getDebitNote().closeDocument();

        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(100), exemption.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", BigDecimal.ZERO, debitEntry.getExemptedAmount().setScale(0));
        assertEquals("Credit Entry Exempted amount not equals", new BigDecimal(100),
                exemption.getCreditEntry().getTotalAmount().setScale(0));
        exemption.getCreditEntry().getCreditNote().closeDocument();
        exemption.revertExemption();
    }

    @Test
    public void twoExemptionInClosedDebtNote() {
        debitEntry.getDebitNote().closeDocument();

        TreasuryExemption exemption =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(100), debitEntry);

        assertEquals("Exemption Exempted amount not equals", new BigDecimal(100), exemption.getExemptedAmount());
        assertEquals("Debit Entry Exempted amount not equals", BigDecimal.ZERO, debitEntry.getExemptedAmount().setScale(0));
        assertEquals("Debit Entry Available amount not equals", new BigDecimal(900.00).setScale(2),
                debitEntry.getAvailableAmountForCredit());
        assertEquals("Credit Entry Exempted amount not equals", new BigDecimal(100),
                exemption.getCreditEntry().getTotalAmount().setScale(0));

        TreasuryExemption exemption1 =
                TreasuryExemption.create(TreasuryExemptionType.findByCode("TreasuryExemptionType").findFirst().get(),
                        treasuryEvent, "reason", new BigDecimal(100), debitEntry);

        assertEquals("1 Exemption Exempted amount not equals", new BigDecimal(100), exemption1.getExemptedAmount());
        assertEquals("1 Debit Entry Exempted amount not equals", BigDecimal.ZERO, debitEntry.getExemptedAmount().setScale(0));
        assertEquals("1 Debit Entry Available amount not equals", new BigDecimal(800.00).setScale(2),
                debitEntry.getAvailableAmountForCredit());
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
            Product.create(ProductGroup.findByCode("JUnitProductGroup"), "TX_PEN_ATRASO_PAG",
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
                    true);
        });

        GlobalInterestRate.findAll().forEach(i -> i.delete());
        GlobalInterestRate.create(new LocalDate(1950, 1, 1), PaymentPlanTestsUtilities.ls("Juro oficial para o ano 2021"),
                new BigDecimal("4.705"), true, false);
    }
}
