/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  (o) Redistributions of source code must retain the above
 *  copyright notice, this list of conditions and the following
 *  disclaimer.
 *
 *  (o) Redistributions in binary form must reproduce the
 *  above copyright notice, this list of conditions and the
 *  following disclaimer in the documentation and/or other
 *  materials provided with the distribution.
 *
 *  (o) Neither the name of Quorum Born IT nor the names of
 *  its contributors may be used to endorse or promote products
 *  derived from this software without specific prior written
 *  permission.
 *
 *  (o) Universidade de Lisboa and its respective subsidiary
 *  Serviços Centrais da Universidade de Lisboa (Departamento
 *  de Informática), hereby referred to as the Beneficiary,
 *  is the sole demonstrated end-user and ultimately the only
 *  beneficiary of the redistributed binary form and/or source
 *  code.
 *
 *  (o) The Beneficiary is entrusted with either the binary form,
 *  the source code, or both, and by accepting it, accepts the
 *  terms of this License.
 *
 *  (o) Redistribution of any binary form and/or source code is
 *  only allowed in the scope of the Universidade de Lisboa
 *  FenixEdu(™)’s implementation projects.
 *
 *  (o) This license and conditions of redistribution of source
 *  code/binary can oly be reviewed by the Steering Comittee of
 *  FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.util;

import static org.fenixedu.treasury.domain.Currency.EURO_CODE;
import static org.fenixedu.treasury.domain.FiscalCountryRegion.findByRegionCode;

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
import org.fenixedu.treasury.domain.FiscalYear;
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

    private static void initializeFiscalYear(FinantialInstitution finantialInstitution) {
        int lastYear = LocalDate.now().getYear() + 10;
        for (int year = LocalDate.now().getYear() - 10; year <= lastYear; year++) {
            FiscalYear.create(finantialInstitution, year, new LocalDate(year, 12, 31));
        }
    }

    private static void initializePaymentPlanConfigurations() {
        LocalizedString name = TreasuryConstants.treasuryBundleI18N("TreasuryBootstrapper.paymentPlan.WITHOUT_VALIDATION");
        PaymentPlanGroupValidator.create(name, Boolean.TRUE);

        LocalizedString numberGeneratorName =
                TreasuryConstants.treasuryBundleI18N("TreasuryBootstrapper.paymentPlan.DEFAULT_GENERATOR");
        PaymentPlanNumberGenerator numberGenerator = PaymentPlanNumberGenerator.create(numberGeneratorName, "PP", 1);
        
        LocalizedString paymentPlanConfiguratorName =
                TreasuryConstants.treasuryBundleI18N("TreasuryBootstrapper.paymentPlan.CONFIGURATOR");
        LocalizedString installmentDescriptionFormat =
                TreasuryConstants.treasuryBundleI18N("TreasuryBootstrapper.paymentPlan.DESCRIPTION");

        PaymentPlanConfigurator configurator =
                new PaymentPlanConfigurator(paymentPlanConfiguratorName, installmentDescriptionFormat, Boolean.FALSE,
                        AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, null, Product.findUniqueByCode("PAYMENT_PLAN_EMOL").get(),
                        numberGenerator, true);
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
        initializeFiscalYear(finantialInstitution);

    }

    private static void initializeFiscalRegion(String countryCode) {
        FiscalCountryRegion.create("PT", TreasuryConstants.treasuryBundleI18N("label.FiscalCountryRegion.PT"));
        FiscalCountryRegion.create("PT_MA", TreasuryConstants.treasuryBundleI18N("label.FiscalCountryRegion.PT_MA"));
        FiscalCountryRegion.create("PT_AZ", TreasuryConstants.treasuryBundleI18N("label.FiscalCountryRegion.PT_AZ"));
    }

    private static void initializeCustomerType() {
        CustomerType.create("ADHOC", TreasuryConstants.treasuryBundleI18N("label.CustomerType.ADHOC"));
    }

    private static void initializeProductGroup() {
        ProductGroup.create("EMOLUMENT", TreasuryConstants.treasuryBundleI18N("label.productGroup.emolument"));
        ProductGroup.create("OTHER", TreasuryConstants.treasuryBundleI18N("label.ProductGroup.Other"));
    }

    private static void initializeVatType() {
        VatType.create("RED", TreasuryConstants.treasuryBundleI18N("label.VatType.RED"));
        VatType.create("INT", TreasuryConstants.treasuryBundleI18N("label.VatType.INT"));
        VatType.create("NOR", TreasuryConstants.treasuryBundleI18N("label.VatType.NOR"));
        VatType.create("ISE", TreasuryConstants.treasuryBundleI18N("label.VatType.ISE"));
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
        PaymentMethod.create("NU", TreasuryConstants.treasuryBundleI18N("label.PaymentMethod.MON"), true, false);
        PaymentMethod.create("TB", TreasuryConstants.treasuryBundleI18N("label.PaymentMethod.WTR"), true, false);
        PaymentMethod.create("MB", TreasuryConstants.treasuryBundleI18N("label.PaymentMethod.MB"), true, false);
        PaymentMethod.create("CD", TreasuryConstants.treasuryBundleI18N("label.PaymentMethod.CCR"), true, false);
        PaymentMethod.create("CH", TreasuryConstants.treasuryBundleI18N("label.PaymentMethod.CH"), true, false);
        PaymentMethod.create("MW", TreasuryConstants.treasuryBundleI18N("label.PaymentMethod.MW"), true, false);
        PaymentMethod.create("CC", TreasuryConstants.treasuryBundleI18N("label.PaymentMethod.CC"), true, false);
    }

    private static void initializeCurrency() {
        Currency.create("EUR", TreasuryConstants.treasuryBundleI18N("label.Currency.EUR"), "EUR", "€");
    }

    private static void initializeFinantialDocumentType() {
        FinantialDocumentType.createForCreditNote("NA",
                TreasuryConstants.treasuryBundleI18N("label.FinantialDocumentType.CreditNote"), "NA", true);
        FinantialDocumentType.createForDebitNote("ND",
                TreasuryConstants.treasuryBundleI18N("label.FinantialDocumentType.DebitNote"), "ND", true);
        FinantialDocumentType.createForSettlementNote("NP",
                TreasuryConstants.treasuryBundleI18N("label.FinantialDocumentType.SettlementNote"),
                "NP", true);
        FinantialDocumentType.createForReimbursementNote("NR",
                TreasuryConstants.treasuryBundleI18N("label.FinantialDocumentType.ReimbursementNote"), "NR", true);
    }

    private static void initializeGlobalInterestRate() {
        // Check global InterestRate since 1995 till now
        int year = 1995;
        while (year <= new LocalDate().getYear()) {
            if (!GlobalInterestRate.findUniqueByYear(year).isPresent()) {
                GlobalInterestRate.create(new LocalDate(year, 1, 1),
                        TreasuryConstants.treasuryBundleI18N("label.interest.for.year", String.valueOf(year)), BigDecimal.ZERO,
                        true, false);
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
        instance.setCreditCardPaymentMethod(PaymentMethod.findByCode("CC"));

    }

    private static void initializeReimbursementProcessStatusType() {
        ReimbursementProcessStatusType.create("PENDING",
                TreasuryConstants.treasuryBundleI18N("TreasuryBootstrapper.ReimbursementProcessStatusType.PENDING")
                        .getContent(Locale.getDefault()),
                1, true, false, false);
        ReimbursementProcessStatusType.create("ANNULED",
                TreasuryConstants.treasuryBundleI18N("TreasuryBootstrapper.ReimbursementProcessStatusType.ANNULED")
                        .getContent(Locale.getDefault()),
                2, false, true, true);
        ReimbursementProcessStatusType.create("CONCLUDED",
                TreasuryConstants.treasuryBundleI18N("TreasuryBootstrapper.ReimbursementProcessStatusType.CONCLUDED")
                        .getContent(Locale.getDefault()),
                3, false, true, false);
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
                TreasuryConstants.treasuryBundleI18N("label.finantialEntity.name", finantialInstitution.getCode()));
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
        Series.create(finantialInstitution, "INT", TreasuryConstants.treasuryBundleI18N("label.internal.serie"), false, true,
                false, true, true);
        Series.create(finantialInstitution, "LEG", TreasuryConstants.treasuryBundleI18N("label.legacy.serie"), false, true, true,
                false, true);
        Series.create(finantialInstitution, "REG", TreasuryConstants.treasuryBundleI18N("label.reg.serie"), true, true, false,
                false, false);
        Series.create(finantialInstitution, "EXT", TreasuryConstants.treasuryBundleI18N("label.external.serie"), true, true,
                false, false, false);
    }

    private static void initializeProducts() {
        Product.create(ProductGroup.findByCode("OTHER"), "INTEREST", TreasuryConstants.treasuryBundleI18N("label.interest"),
                TreasuryConstants.treasuryBundleI18N("label.unit"), true, false, 0, VatType.findByCode("ISE"), null, null);

        Product.create(ProductGroup.findByCode("OTHER"), "PAGAMENTO",
                TreasuryConstants.treasuryBundleI18N("label.advancedPayment"), TreasuryConstants.treasuryBundleI18N("label.unit"),
                true, false, 0, VatType.findByCode("ISE"), null, null);

        Product.create(ProductGroup.findByCode("EMOLUMENT"), "PAYMENT_PLAN_EMOL",
                TreasuryConstants.treasuryBundleI18N("label.paymentPlanEmolumentProduct"),
                TreasuryConstants.treasuryBundleI18N("label.unit"), true, false, 0, VatType.findByCode("ISE"), null, null);
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

        DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), regulationSeries).editReplacingPrefix(true, "NY");
        DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), regulationSeries).editReplacingPrefix(true, "NZ");
    }

    private static DateTime fromDate(int year, int month, int day) {
        return new DateTime(year, month, day, 0, 0, 0);
    }

    private static DateTime toDate(int year, int month, int day) {
        return new DateTime(year, month, day, 23, 59, 59);
    }
}
