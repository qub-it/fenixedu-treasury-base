package org.fenixedu.treasury.paymentplans;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.base.TreasuryPlatformDependentServicesForTests;
import org.fenixedu.treasury.domain.AdhocCustomer;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.FiscalCountryRegion;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.ProductGroup;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.VatExemptionReason;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanConfigurator;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanNumberGenerator;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRate;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.tariff.InterestType;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.dto.PaymentPenaltyEntryBean;
import org.fenixedu.treasury.dto.SettlementDebitEntryBean;
import org.fenixedu.treasury.dto.SettlementInterestEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.AddictionsCalculeTypeEnum;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.PaymentPlanBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.District;
import pt.ist.standards.geographic.Municipality;
import pt.ist.standards.geographic.Planet;

public class PaymentPlanTestsUtilities {
    public static final String DEBT_PRODUCT = "DEBT";

    public static void startUp() {
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                createFinantialInstitution();
                createEmolumentProduct();
                createDebtProduct();
                createDebtAccount();
                createTreasurySettings();

                TreasuryPlataformDependentServicesFactory.registerImplementation(new TreasuryPlatformDependentServicesForTests());

                Vat.create(VatType.findByCode("INT"), getFinatialInstitution(), new BigDecimal("0"),
                        new LocalDate(2000, 1, 1).toDateTimeAtStartOfDay(), new LocalDate(2050, 1, 1).toDateTimeAtStartOfDay());
                GlobalInterestRate.create(new LocalDate(1950, 1, 1), ls("Juro oficial para o ano 2021"), new BigDecimal("4.705"),
                        false, false);
                PaymentPlanNumberGenerator.create(ls("Gerador"), "Plano-", 0);

                FinantialDocumentType.createForSettlementNote("NP", ls("Nota de liquidação"), "NP", true);
                FinantialDocumentType.createForDebitNote("ND", ls("Nota de dívida"), "ND", true);
                Series.create(getFinatialInstitution(), "INT", ls("Série Interna"), false, true, false, true, true);

                PaymentMethod.create("MB", ls("multibanco"), true);

                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    public static void runTests(PaymentPlanBean paymentPlanBean, List<InstallmentBean> expectedInstallments) {
        List<InstallmentBean> resultInstallmentsBeans =
                paymentPlanBean.getPaymentPlanConfigurator().getInstallmentsBeansFor(paymentPlanBean);

        for (int i = 0; i < expectedInstallments.size(); i++) {
            assertInstallment(paymentPlanBean, expectedInstallments.get(i), resultInstallmentsBeans.get(i));
        }

    }

    public static void runTests(PaymentPlanBean paymentPlanBean, List<InstallmentBean> expectedInstallments,
            List<LocalDate> fixedDates, List<BigDecimal> fixedAmounts) {
        List<InstallmentBean> resultInstallmentsBeans =
                paymentPlanBean.getPaymentPlanConfigurator().getInstallmentsBeansFor(paymentPlanBean, fixedDates, fixedAmounts);

        for (int i = 0; i < expectedInstallments.size(); i++) {
            assertInstallment(paymentPlanBean, expectedInstallments.get(i), resultInstallmentsBeans.get(i));
        }

    }

    private static void assertInstallment(PaymentPlanBean paymentPlanBean, InstallmentBean expectedInstallmentBean,
            InstallmentBean resultInstallmentAmount) {
        assertEquals(
                String.format("Installment amount %s on %s", resultInstallmentAmount.getDescription().getContent(),
                        paymentPlanBean.getReason()),
                expectedInstallmentBean.getInstallmentAmount(), resultInstallmentAmount.getInstallmentAmount());

        assertEquals(String.format("Installment date %s on %s", resultInstallmentAmount.getDescription().getContent(),
                paymentPlanBean.getReason()), expectedInstallmentBean.getDueDate(), resultInstallmentAmount.getDueDate());

        List<InstallmentEntryBean> expectedInstallmentsEntries =
                expectedInstallmentBean.getSortedInstallmentEntries(paymentPlanBean.getPaymentPlanConfigurator().getComparator());

        List<InstallmentEntryBean> resultInstallmentsEntries =
                resultInstallmentAmount.getSortedInstallmentEntries(paymentPlanBean.getPaymentPlanConfigurator().getComparator());

        for (int i = 0; i < expectedInstallmentsEntries.size(); i++) {
            InstallmentEntryBean expectedInstallmentsEntryBean = expectedInstallmentsEntries.get(i);
            InstallmentEntryBean resultInstallmentsEntryBean = resultInstallmentsEntries.get(i);

            assertEquals(
                    String.format("Entry amount %s - %d> %s", resultInstallmentAmount.getDescription().getContent(), i,
                            paymentPlanBean.getReason()),
                    expectedInstallmentsEntryBean.getAmount(), resultInstallmentsEntryBean.getAmount());

            if (expectedInstallmentsEntryBean.getInvoiceEntry().isForDebitEntry()
                    || expectedInstallmentsEntryBean.getInvoiceEntry().isForPendingDebitEntry()) {
                assertEquals(
                        String.format("Entry entry %s - %d> %s", resultInstallmentAmount.getDescription().getContent(), i,
                                paymentPlanBean.getReason()),
                        expectedInstallmentsEntryBean.getInvoiceEntry().getInvoiceEntry(),
                        resultInstallmentsEntryBean.getInvoiceEntry().getInvoiceEntry());
            }
            if (expectedInstallmentsEntryBean.getInvoiceEntry().isForPendingInterest()) {
                SettlementInterestEntryBean expectedInterestBean =
                        (SettlementInterestEntryBean) expectedInstallmentsEntryBean.getInvoiceEntry();
                SettlementInterestEntryBean resultInterestBean =
                        (SettlementInterestEntryBean) resultInstallmentsEntryBean.getInvoiceEntry();
                assertEquals(
                        String.format("Entry entry %s - %d> %s", resultInstallmentAmount.getDescription().getContent(), i,
                                paymentPlanBean.getReason()),
                        expectedInterestBean.getDebitEntry(), resultInterestBean.getDebitEntry());
            }
            if (expectedInstallmentsEntryBean.getInvoiceEntry().isForPaymentPenalty()) {
                PaymentPenaltyEntryBean expectedPenaltyBean =
                        (PaymentPenaltyEntryBean) expectedInstallmentsEntryBean.getInvoiceEntry();
                PaymentPenaltyEntryBean resultPenaltyBean =
                        (PaymentPenaltyEntryBean) resultInstallmentsEntryBean.getInvoiceEntry();
                assertEquals(
                        String.format("Entry entry %s - %d> %s", resultInstallmentAmount.getDescription().getContent(), i,
                                paymentPlanBean.getReason()),
                        expectedPenaltyBean.getDebitEntry(), resultPenaltyBean.getDebitEntry());
            }
        }
    }

    public static PaymentPlanConfigurator getConfigurator(String string) {
        return FenixFramework.getDomainRoot().getPaymentPlanConfiguratorsSet().stream()
                .filter(configurator -> configurator.getName().anyMatch(s -> s.equals(string))).findAny().orElse(null);
    }

    public static DebtAccount getDebtAccount() {
        return FenixFramework.getDomainRoot().getDebtAccountsSet().iterator().next();
    }

//    public ISettlementInvoiceEntryBean createDebitEntryBean(BigDecimal debitAmount, LocalDate dueDate, LocalDate planRequestDate,
//            boolean aplyInterest) {
//        return createDebitEntryBean(debitAmount, dueDate, planRequestDate, aplyInterest, debitAmount);
//    }

    public static ISettlementInvoiceEntryBean createDebitEntryBean(DebitEntry debitEntry, LocalDate planRequestDate,
            BigDecimal customSettledAmount) {
        SettlementDebitEntryBean settlementDebitEntryBean = new SettlementDebitEntryBean(debitEntry);
        if (customSettledAmount == null) {
            settlementDebitEntryBean.setSettledAmount(debitEntry.getOpenAmountWithInterestsAtDate(planRequestDate));
        } else {
            settlementDebitEntryBean.setSettledAmount(customSettledAmount);
        }
        return settlementDebitEntryBean;
    }

    public static ISettlementInvoiceEntryBean createInterestEntryBean(DebitEntry debitEntry, BigDecimal bigDecimal) {
        InterestRateBean interestRateBean = new InterestRateBean();
        interestRateBean.setInterestAmount(bigDecimal);
        ISettlementInvoiceEntryBean interestEntryBean = new SettlementInterestEntryBean(debitEntry, interestRateBean);
        return interestEntryBean;
    }
    
    public static ISettlementInvoiceEntryBean createPenaltyTaxEntryBean(DebitEntry debitEntry, BigDecimal amount) {
        return new PaymentPenaltyEntryBean(debitEntry, "Penalty Tax For: " + debitEntry.getDescription(), new LocalDate(), amount);
    }

    public static DebitEntry createDebitEntry(BigDecimal debitAmount, LocalDate dueDate, boolean aplyInterest) {
        Vat vat =
                Vat.findActiveUnique(VatType.findByCode("INT"), getFinatialInstitution(), dueDate.toDateTimeAtStartOfDay()).get();
        TreasuryEvent treasuryEvent = null;
        DebitEntry debitEntry = DebitEntry.create(Optional.empty(), getDebtAccount(), treasuryEvent, vat, debitAmount, dueDate,
                null, Product.findUniqueByCode(DEBT_PRODUCT).get(), "debt " + debitAmount, BigDecimal.ONE, null,
                dueDate.toDateTimeAtStartOfDay());
        if (aplyInterest) {
            final int numberOfDaysAfterDueDate = 1;
            final boolean applyInFirstWorkday = false;
            final int maximumDaysToApplyPenalty = 0;
            final BigDecimal rate = null;

            InterestRate interestRate = InterestRate.createForDebitEntry(debitEntry, InterestType.GLOBAL_RATE,
                    numberOfDaysAfterDueDate, applyInFirstWorkday, maximumDaysToApplyPenalty, BigDecimal.ZERO, rate);
            debitEntry.changeInterestRate(interestRate);
        }
        return debitEntry;
    }

    public static PaymentPlanBean createPaymentPlanBean(String planID, String configurator, int nbInstallments,
            LocalDate requestDate, LocalDate StartDate, LocalDate EndDate) {
        PaymentPlanBean paymentPlanBean = new PaymentPlanBean(getDebtAccount(), requestDate);
        paymentPlanBean.setNbInstallments(nbInstallments);
        paymentPlanBean.setStartDate(StartDate);
        paymentPlanBean.setEndDate(EndDate);
        paymentPlanBean.setReason(planID);
        paymentPlanBean.setPaymentPlanConfigurator(getConfigurator(configurator));
        return paymentPlanBean;
    }

    public static PaymentPlanConfigurator createBlockingConfigurator(String name, Boolean applyDebitEntryInterest,
            AddictionsCalculeTypeEnum interestDistribution, AddictionsCalculeTypeEnum paymentPenaltyDistribution,
            Boolean usePaymentPenalty) {

        PaymentPlanConfigurator configurator = new PaymentPlanConfigurator();
        configurator.setName(ls(name));
        configurator.setActive(true);
        configurator.setInstallmentDescriptionFormat(new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE,
                "${installmentNumber}º prestação do plano de pagamento: ${paymentPlanId}"));
        configurator.setEmolumentProduct(Product.findUniqueByCode("EMULUMENTO").get());
        configurator.setNumberGenerators(FenixFramework.getDomainRoot().getPaymentPlanNumberGeneratorsSet().iterator().next());

        configurator.setApplyDebitEntryInterest(applyDebitEntryInterest);
        configurator.setInterestDistribution(interestDistribution);
        configurator.setPaymentPenaltyDistribution(paymentPenaltyDistribution);
        configurator.setUsePaymentPenalty(usePaymentPenalty);

        return configurator;
    }

    public static Product createEmolumentProduct() {
        return Product.create(ProductGroup.create("JUnitProductGroup", ls("JUnit ProductGroup")), "EMULUMENTO",
                ls("Adiantamento de Pagamento"), ls("Unidade"), true, false, 0, VatType.create("INT", ls("INT")),
                List.of(getFinatialInstitution()), VatExemptionReason.create("VatExemptionReason", ls("VatExemptionReason")));
    }

    public static Product createDebtProduct() {
        return Product.create(ProductGroup.findByCode("JUnitProductGroup"), DEBT_PRODUCT, ls("interest de Pagamento"),
                ls("Unidade"), true, false, 0, VatType.findByCode("INT"), List.of(getFinatialInstitution()),
                VatExemptionReason.findByCode("VatExemptionReason"));
    }

    public static void createTreasurySettings() {
        TreasurySettings instance = TreasurySettings.getInstance();
        instance.setInterestProduct(createInterestProduct());
        instance.setNumberOfPaymentPlansActivesPerStudent(999);
    }

    public static Product createInterestProduct() {
        return Product.create(ProductGroup.findByCode("JUnitProductGroup"), "Interest", ls("interest de Pagamento"),
                ls("Unidade"), true, false, 0, VatType.findByCode("INT"), List.of(getFinatialInstitution()),
                VatExemptionReason.findByCode("VatExemptionReason"));
    }

    public static void createDebtAccount() {
        CustomerType customerType = CustomerType.create("CT", ls("CustomerType"));
        AdhocCustomer create = AdhocCustomer.create(customerType, Customer.DEFAULT_FISCAL_NUMBER, "Diogo", "morada", "", "", "",
                "pt", "", List.of(getFinatialInstitution()));
        DebtAccount.create(getFinatialInstitution(), create);
    }

    public static void createFinantialInstitution() {
        Country country = new Country(Planet.getEarth(), "portugal", "pt", "ptr", "1");
        District district = new District(country, "lisboa", "lisboa");
        FinantialInstitution finantialInstitution = FinantialInstitution.create(FiscalCountryRegion.create("PT", ls("portugal")),
                Currency.create("EUR", ls("Euro"), "EUR", "€"), "FinantialInstitution", "123456789", "companyId",
                "Finantial Institution", "company name", "address", country, district,
                new Municipality(district, "lisboa", "lisboa"), "", "");

        FinantialEntity.create(finantialInstitution, "FINANTIAL_ENTITY", ls("Entidade Financeira"));
    }

    public static FinantialInstitution getFinatialInstitution() {
        return FenixFramework.getDomainRoot().getFinantialInstitutionsSet().iterator().next();
    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }
}
