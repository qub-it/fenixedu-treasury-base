package org.fenixedu.treasury.util;

import static org.fenixedu.treasury.domain.Currency.EURO_CODE;
import static org.fenixedu.treasury.domain.FiscalCountryRegion.findByRegionCode;
import static org.fenixedu.treasury.domain.document.FinantialDocumentType.findForCreditNote;
import static org.fenixedu.treasury.domain.document.FinantialDocumentType.findForDebitNote;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.fenixedu.commons.i18n.LocalizedString;
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
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.document.reimbursement.ReimbursementProcessStatusType;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanConfigurator;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanNumberGenerator;
import org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator.PaymentPlanGroupValidator;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRate;
import org.fenixedu.treasury.dto.PaymentPlans.AddictionsCalculeTypeEnum;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.District;
import pt.ist.standards.geographic.Municipality;

public class TreasuryBootstrapper {

    private static final Object COUNTRY_PT = "PT";


    public static void bootstrap(String institutionName, String institutionInitials, String countryCode) {
        initializeFiscalRegion(countryCode);
        initializeCustomerType();
        initializeProductGroup();
        initializeVatType();
        initializeVatExemption(countryCode);
        initializePaymentMethod();
        initializeCurrency();
        initializeFinantialDocumentType();
        initializeGlobalInterestRate();
        initializeReimbursementProcessStatusType();
        initializeProducts();
        initializeTreasurySettings();
        initializePaymentPlanConfigurations();

        // On create of finantialInstitution will be called TreasuryBootstrap.bootstrapFinantialInstitution(FinantialInstitution finantialInstitution)
        initializeFinantialInstituition(institutionName, institutionInitials, countryCode);

    }

    private static void initializePaymentPlanConfigurations() {
        LocalizedString name = new LocalizedString(new Locale("pt", "PT"), "Sem validação").with(Locale.UK, "Without validation");
        PaymentPlanGroupValidator.create(name, Boolean.TRUE);

        LocalizedString numberGeneratorName =
                new LocalizedString(new Locale("pt", "PT"), "Gerador por omissão").with(Locale.UK, "Default generator");
        PaymentPlanNumberGenerator numberGenerator = PaymentPlanNumberGenerator.create(numberGeneratorName, "PP", 1);
        
        LocalizedString paymentPlanConfiguratorName =
                new LocalizedString(new Locale("pt", "PT"), "Configurador por omissão").with(Locale.UK, "Default configurator");
        LocalizedString installmentDescriptionFormat =
                new LocalizedString(new Locale("pt", "PT"),
                        "${installmentNumber}ª prestação do plano de pagamento: ${paymentPlanId}").with(Locale.UK,
                                "${installmentNumber} installment of payment plan: ${paymentPlanId}");

        PaymentPlanConfigurator configurator =
                new PaymentPlanConfigurator(paymentPlanConfiguratorName, installmentDescriptionFormat, Boolean.FALSE,
                        AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, null, Product.findUniqueByCode("PAYMENT_PLAN_EMOL").get(),
                        numberGenerator);
        configurator.setApplyDebitEntryInterest(Boolean.TRUE);
        configurator.setCanIncreaseInterestAmount(Boolean.FALSE);
        configurator.setActive(Boolean.TRUE);
        
    }

    public static void bootstrapFinantialInstitution(FinantialInstitution finantialInstitution) {
        initializeFinantialEntity(finantialInstitution);
        initializeVat(finantialInstitution);
        initializeSeries(finantialInstitution);
        initializeFinantialIntitutionProducts(finantialInstitution);
        initializeRegulationDocumentNumberSeries(finantialInstitution);
    }

    private static void initializeFiscalRegion(String countryCode) {
        FiscalCountryRegion.create("PT", treasuryBundleI18N("label.FiscalCountryRegion.PT"));
        FiscalCountryRegion.create("PT_MA", treasuryBundleI18N("label.FiscalCountryRegion.PT_MA"));
        FiscalCountryRegion.create("PT_AZ", treasuryBundleI18N("label.FiscalCountryRegion.PT_AZ"));
    }

    private static void initializeCustomerType() {
        CustomerType.create("ADHOC", treasuryBundleI18N("label.CustomerType.ADHOC"));
    }

    private static void initializeProductGroup() {
        ProductGroup.create("EMOLUMENT", treasuryBundleI18N("label.productGroup.emolument"));
        ProductGroup.create("OTHER", treasuryBundleI18N("label.ProductGroup.Other"));
    }

    private static void initializeVatType() {
        VatType.create("RED", treasuryBundleI18N("label.VatType.RED"));
        VatType.create("INT", treasuryBundleI18N("label.VatType.INT"));
        VatType.create("NOR", treasuryBundleI18N("label.VatType.NOR"));
        VatType.create("ISE", treasuryBundleI18N("label.VatType.ISE"));
    }

    private static void initializeVatExemption(String countryCode) {
        if (COUNTRY_PT.equals(countryCode)) {
            String[] codes = new String[] { "M01", "M02", "M03", "M04", "M05", "M06", "M07", "M08", "M09", "M10", "M11", "M12",
                    "M13", "M14", "M15", "M16", "M99" };

            for (String code : codes) {
                if (code.equals("M07")) {
                    VatExemptionReason.create(code,
                            new LocalizedString(Locale.getDefault(), "Isento Artigo 9.º do CIVA (Ou similar)"));
                } else if (code.equals("M01")) {
                    VatExemptionReason.create(code,
                            new LocalizedString(Locale.getDefault(), "Artigo 16.º n.º 6 alínea c) do CIVA (Ou similar)"));
                } else if (code.equals("M02")) {
                    VatExemptionReason.create(code,
                            new LocalizedString(Locale.getDefault(), "Artigo 6.º do Decreto‐Lei n.º 198/90, de 19 de Junho"));
                } else if (code.equals("M03")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(), "Exigibilidade de caixa"));
                } else if (code.equals("M04")) {
                    VatExemptionReason.create(code,
                            new LocalizedString(Locale.getDefault(), "Isento Artigo 13.º do CIVA (Ou similar)"));
                } else if (code.equals("M05")) {
                    VatExemptionReason.create(code,
                            new LocalizedString(Locale.getDefault(), "Isento Artigo 14.º do CIVA (Ou similar)"));
                } else if (code.equals("M06")) {
                    VatExemptionReason.create(code,
                            new LocalizedString(Locale.getDefault(), "Isento Artigo 15.º do CIVA (Ou similar)"));
                } else if (code.equals("M08")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(), "IVA - Autoliquidação"));
                } else if (code.equals("M09")) {
                    VatExemptionReason.create(code,
                            new LocalizedString(Locale.getDefault(), "IVA ‐ Não confere direito a dedução"));
                } else if (code.equals("M10")) {
                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(), "IVA - Regime de isenção"));
                } else if (code.equals("M16")) {
                    VatExemptionReason.create(code,
                            new LocalizedString(Locale.getDefault(), "Isento Artigo 14.º do RITI (Ou similar)"));
                } else if (code.equals("M99")) {
                    VatExemptionReason.create(code,
                            new LocalizedString(Locale.getDefault(), "Não sujeito; não tributado (Ou similar)"));
                } else {

                    VatExemptionReason.create(code, new LocalizedString(Locale.getDefault(), code));
                }
            }
        }
    }

    private static void initializePaymentMethod() {
        PaymentMethod.create("NU", treasuryBundleI18N("label.PaymentMethod.MON"), true);
        PaymentMethod.create("TB", treasuryBundleI18N("label.PaymentMethod.WTR"), true);
        PaymentMethod.create("MB", treasuryBundleI18N("label.PaymentMethod.ELE"), true);
        PaymentMethod.create("CD", treasuryBundleI18N("label.PaymentMethod.CCR"), true);
        PaymentMethod.create("CH", treasuryBundleI18N("label.PaymentMethod.CH"), true);
        PaymentMethod.create("MW", treasuryBundleI18N("label.PaymentMethod.MW"), true);

    }

    private static void initializeCurrency() {
        Currency.create("EUR", treasuryBundleI18N("label.Currency.EUR"), "EUR", "€");
    }

    private static void initializeFinantialDocumentType() {
        FinantialDocumentType.createForCreditNote("NA", treasuryBundleI18N("label.FinantialDocumentType.CreditNote"), "NA", true);
        FinantialDocumentType.createForDebitNote("ND", treasuryBundleI18N("label.FinantialDocumentType.DebitNote"), "ND", true);
        FinantialDocumentType.createForSettlementNote("NP", treasuryBundleI18N("label.FinantialDocumentType.SettlementNote"),
                "NP", true);
        FinantialDocumentType.createForReimbursementNote("NR",
                treasuryBundleI18N("label.FinantialDocumentType.ReimbursementNote"), "NR", true);
    }

    private static void initializeGlobalInterestRate() {
        // Check global InterestRate since 1995 till now
        int year = 1995;
        while (year <= new LocalDate().getYear()) {
            if (!GlobalInterestRate.findUniqueByYear(year).isPresent()) {
                GlobalInterestRate.create(new LocalDate(year, 1, 1),
                        treasuryBundleI18N("label.interest.for.year", String.valueOf(year)), BigDecimal.ZERO, true, false);
            }
            year++;
        }
    }

    private static void initializeTreasurySettings() {
        TreasurySettings instance = TreasurySettings.getInstance();
        instance.edit(Currency.findByCode(EURO_CODE), Product.findUniqueByCode("INTEREST").get(),
                Product.findUniqueByCode("PAGAMENTO").get(), 1, Boolean.FALSE);
        instance.setMbPaymentMethod(PaymentMethod.findByCode("MB"));
        instance.setMbWayPaymentMethod(PaymentMethod.findByCode("MW"));
        instance.setCreditCardPaymentMethod(PaymentMethod.findByCode("CD"));

    }

    private static void initializeReimbursementProcessStatusType() {
        ReimbursementProcessStatusType.create("PENDING", "Reembolso pendente", 1, true, false, false);
        ReimbursementProcessStatusType.create("ANNULED", "Reembolso anulado", 2, false, true, true);
        ReimbursementProcessStatusType.create("CONCLUDED", "Reembolso concluído", 3, false, true, false);
    }

    private static FinantialInstitution initializeFinantialInstituition(String institutionName, String institutionInitials,
            String countryCode) {
        FiscalCountryRegion fiscalCountryRegion = findByRegionCode(countryCode);;
        Currency currency = Currency.findByCode(EURO_CODE);
        String code = institutionInitials;
        String fiscalNumber = Customer.DEFAULT_FISCAL_NUMBER;
        String companyId = null;
        String name = institutionName;
        String companyName = institutionName;
        String address = null;
        Country country = null;
        District district = null;
        Municipality municipality = null;
        String locality = null;
        String zipCode = null;

        return FinantialInstitution.create(fiscalCountryRegion, currency, code, fiscalNumber, companyId, name, companyName,
                address, country, district, municipality, locality, zipCode);
    }

    private static void initializeFinantialEntity(FinantialInstitution finantialInstitution) {
        FinantialEntity.create(finantialInstitution, finantialInstitution.getCode() + "_" + "ACADEMIC",
                treasuryBundleI18N("label.finantialEntity.name", finantialInstitution.getCode()));
    }

    private static void initializeVat(FinantialInstitution finantialInstitution) {
        if (finantialInstitution.getVatsSet().stream().anyMatch(x -> x.getVatType().getCode().equals("ISE")) == false) {
            Vat.create(VatType.findByCode("ISE"), finantialInstitution, BigDecimal.ZERO, fromDate(1980, 1, 1),
                    toDate(2100, 12, 31));
        }

        if (finantialInstitution.getVatsSet().stream().anyMatch(x -> x.getVatType().getCode().equals("RED")) == false) {
            VatType type = VatType.findByCode("RED");

            Vat.create(type, finantialInstitution, BigDecimal.valueOf(8), fromDate(1986, 1, 1), toDate(1992, 3, 23));
            Vat.create(type, finantialInstitution, BigDecimal.valueOf(5), fromDate(1992, 3, 24), toDate(2010, 6, 30));
            Vat.create(type, finantialInstitution, BigDecimal.valueOf(6), fromDate(2011, 7, 1), toDate(2100, 12, 31));
        }

        if (finantialInstitution.getVatsSet().stream().anyMatch(x -> x.getVatType().getCode().equals("INT")) == false) {
            VatType type = VatType.findByCode("INT");

            Vat.create(type, finantialInstitution, BigDecimal.valueOf(12), fromDate(1996, 7, 1), toDate(2010, 6, 30));
            Vat.create(type, finantialInstitution, BigDecimal.valueOf(13), fromDate(2011, 7, 1), toDate(2100, 12, 31));
        }

        if (finantialInstitution.getVatsSet().stream().anyMatch(x -> x.getVatType().getCode().equals("NOR")) == false) {
            VatType taxaNormal = VatType.findByCode("NOR");

            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(16), fromDate(1986, 1, 1), toDate(1988, 1, 31));
            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(17), fromDate(1988, 2, 1), toDate(1992, 3, 23));
            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(16), fromDate(1992, 3, 24), toDate(1994, 12, 31));
            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(17), fromDate(1995, 1, 1), toDate(1996, 6, 30));
            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(17), fromDate(1996, 7, 1), toDate(2002, 6, 4));
            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(19), fromDate(2002, 6, 5), toDate(2005, 6, 30));
            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(21), fromDate(2005, 7, 1), toDate(2008, 6, 30));
            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(20), fromDate(2008, 7, 1), toDate(2010, 6, 30));
            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(21), fromDate(2010, 7, 1), toDate(2010, 12, 31));
            Vat.create(taxaNormal, finantialInstitution, BigDecimal.valueOf(23), fromDate(2011, 1, 1), toDate(2100, 12, 31));
        }
    }

    private static void initializeSeries(FinantialInstitution finantialInstitution) {
        Series.create(finantialInstitution, "INT", treasuryBundleI18N("label.internal.serie"), false, true, false, true, true);
        Series.create(finantialInstitution, "LEG", treasuryBundleI18N("label.legacy.serie"), false, true, true, false, true);
        Series.create(finantialInstitution, "REG", treasuryBundleI18N("label.reg.serie"), true, true, false, false, false);
        Series.create(finantialInstitution, "EXT", treasuryBundleI18N("label.external.serie"), true, true, false, false, false);
    }

    private static void initializeProducts() {
        Product.create(ProductGroup.findByCode("OTHER"), "INTEREST", treasuryBundleI18N("label.interest"),
                treasuryBundleI18N("label.unit"), true, false, 0, VatType.findByCode("ISE"), null, null);

        Product.create(ProductGroup.findByCode("OTHER"), "PAGAMENTO", treasuryBundleI18N("label.advancedPayment"),
                treasuryBundleI18N("label.unit"), true, false, 0, VatType.findByCode("ISE"), null, null);

        Product.create(ProductGroup.findByCode("EMOLUMENT"), "PAYMENT_PLAN_EMOL",
                treasuryBundleI18N("label.paymentPlanEmolumentProduct"),
                treasuryBundleI18N("label.unit"), true, false, 0, VatType.findByCode("ISE"), null, null);
    }

    private static void initializeFinantialIntitutionProducts(FinantialInstitution finantialInstitution) {
        if (Product.findUniqueByCode("INTEREST").isPresent()) {
            Product product = Product.findUniqueByCode("INTEREST").get();
            List<FinantialInstitution> finantialInstitutions = new ArrayList<>(product.getFinantialInstitutionsSet());
            finantialInstitutions.add(finantialInstitution);
            product.updateFinantialInstitutions(finantialInstitutions);
            product.setVatExemptionReason(VatExemptionReason.findByCode("M01"));
        }

        if (Product.findUniqueByCode("PAGAMENTO").isPresent()) {
            Product product = Product.findUniqueByCode("PAGAMENTO").get();
            List<FinantialInstitution> finantialInstitutions = new ArrayList<>(product.getFinantialInstitutionsSet());
            finantialInstitutions.add(finantialInstitution);
            product.updateFinantialInstitutions(finantialInstitutions);
            product.setVatExemptionReason(VatExemptionReason.findByCode("M01"));

        }

        if (Product.findUniqueByCode("PAYMENT_PLAN_EMOL").isPresent()) {
            Product product = Product.findUniqueByCode("PAYMENT_PLAN_EMOL").get();
            List<FinantialInstitution> finantialInstitutions = new ArrayList<>(product.getFinantialInstitutionsSet());
            finantialInstitutions.add(finantialInstitution);
            product.updateFinantialInstitutions(finantialInstitutions);
            product.setVatExemptionReason(VatExemptionReason.findByCode("M07"));

        }
    }

    private static void initializeRegulationDocumentNumberSeries(final FinantialInstitution finantialInstitution) {
        final Series regulationSeries = Series.findByCode(finantialInstitution, "REG");

        finantialInstitution.setRegulationSeries(regulationSeries);

        DocumentNumberSeries.find(findForDebitNote(), regulationSeries).editReplacingPrefix(true, "NY");
        DocumentNumberSeries.find(findForCreditNote(), regulationSeries).editReplacingPrefix(true, "NZ");
    }

    private static DateTime fromDate(int year, int month, int day) {
        return new DateTime(year, month, day, 0, 0, 0);
    }

    private static DateTime toDate(int year, int month, int day) {
        return new DateTime(year, month, day, 23, 59, 59);
    }
}
