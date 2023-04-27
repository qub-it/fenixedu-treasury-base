package org.fenixedu.treasury.paymentplans;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.ProductGroup;
import org.fenixedu.treasury.domain.VatExemptionReason;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxSettings;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.FixedTariff;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRateType;
import org.fenixedu.treasury.domain.tariff.InterestRateEntry;
import org.fenixedu.treasury.domain.tariff.InterestRateType;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.AddictionsCalculeTypeEnum;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.PaymentPlanBean;
import org.joda.time.LocalDate;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@RunWith(FenixFrameworkRunner.class)
public class PaymentPlansWithPenaltyTaxTest {

    private static final String CONFIGURATOR_INT_AFTER_TAX_BEFORE = "CONFIGURATOR_INT_AFTER_TAX_BEFORE";
    private static final String CONFIGURATOR_INT_AFTER_TAX_AFTER = "CONFIGURATOR_INT_AFTER_TAX_AFTER";
    private static final String CONFIGURATOR_INT_DIST_TAX_BEFORE = "CONFIGURATOR_INT_DIST_TAX_BEFORE";
    private static final String CONFIGURATOR_INT_DIST_TAX_AFTER = "CONFIGURATOR_INT_DIST_TAX_AFTER";

    @BeforeClass
    public static void beforeClass() {
        PaymentPlanTestsUtilities.startUp();
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                createPenaltyTaxAndConfigurators();
                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static void createPenaltyTaxAndConfigurators() {
        createPenaltyTaxSettings();

        PaymentPlanTestsUtilities.createBlockingConfigurator(CONFIGURATOR_INT_AFTER_TAX_BEFORE, Boolean.TRUE,
                AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, AddictionsCalculeTypeEnum.BEFORE_DEBIT_ENTRY, Boolean.TRUE);

        PaymentPlanTestsUtilities.createBlockingConfigurator(CONFIGURATOR_INT_AFTER_TAX_AFTER, Boolean.TRUE,
                AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, Boolean.TRUE);

        PaymentPlanTestsUtilities.createBlockingConfigurator(CONFIGURATOR_INT_DIST_TAX_BEFORE, Boolean.TRUE,
                AddictionsCalculeTypeEnum.BY_INSTALLMENT_ENTRY_AMOUNT, AddictionsCalculeTypeEnum.BEFORE_DEBIT_ENTRY,
                Boolean.TRUE);

        PaymentPlanTestsUtilities.createBlockingConfigurator(CONFIGURATOR_INT_DIST_TAX_AFTER, Boolean.TRUE,
                AddictionsCalculeTypeEnum.BY_INSTALLMENT_ENTRY_AMOUNT, AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, Boolean.TRUE);
    }

    private static void createPenaltyTaxSettings() {
        if (!Product.findUniqueByCode("TX_PEN_ATRASO_PAG").isPresent()) {
            Product.create(ProductGroup.findByCode("EMOLUMENT"), "TX_PEN_ATRASO_PAG",
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
                    false, false);
        });

        InterestRateType globalInterestRateType = TreasurySettings.getInstance().getAvailableInterestRateTypesSet().stream()
                .filter(type -> type instanceof GlobalInterestRateType).findFirst().get();
        
        globalInterestRateType.getInterestRateEntriesSet().forEach(e -> e.delete());
        
        InterestRateEntry.create(globalInterestRateType, new LocalDate(1950, 1, 1), PaymentPlanTestsUtilities.ls("Juro oficial para o ano 2021"),
                new BigDecimal("4.705"), true, false);
    }

    @Test
    public void interestsAfter_penaltyBefore_I2_D1_1000() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, CONFIGURATOR_INT_AFTER_TAX_BEFORE, 2,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("1000.00"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean1 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("7.99"));

        ISettlementInvoiceEntryBean penaltyTaxEntryBean1 =
                PaymentPlanTestsUtilities.createPenaltyTaxEntryBean(debitEntry, new BigDecimal("20.00"));
        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 5, 1), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(penaltyTaxEntryBean1, new BigDecimal("20.00")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("494.00")));
            expectedInstallmentsBean.add(installmentBean);
        }

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("506.00")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("7.99")));
            expectedInstallmentsBean.add(installmentBean);
        }

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void interestsAfter_penaltyAfter_I2_D1_1000() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, CONFIGURATOR_INT_AFTER_TAX_AFTER, 2,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("1000.00"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean1 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("7.99"));

        ISettlementInvoiceEntryBean penaltyTaxEntryBean1 =
                PaymentPlanTestsUtilities.createPenaltyTaxEntryBean(debitEntry, new BigDecimal("20.00"));
        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 5, 1), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("514.00")));
            expectedInstallmentsBean.add(installmentBean);
        }

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("486.00")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("7.99")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(penaltyTaxEntryBean1, new BigDecimal("20.00")));
            expectedInstallmentsBean.add(installmentBean);
        }

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void interestsDist_penaltyBefore_I2_D1_1000() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, CONFIGURATOR_INT_DIST_TAX_BEFORE, 2,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("1000.00"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean1 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("7.99"));

        ISettlementInvoiceEntryBean penaltyTaxEntryBean1 =
                PaymentPlanTestsUtilities.createPenaltyTaxEntryBean(debitEntry, new BigDecimal("20.00"));
        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 5, 1), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(penaltyTaxEntryBean1, new BigDecimal("20.00")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("490.08")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("3.92")));
            expectedInstallmentsBean.add(installmentBean);
        }

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("509.92")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("4.07")));
            expectedInstallmentsBean.add(installmentBean);
        }

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void interestsDist_penaltyAfter_I2_D1_1000() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, CONFIGURATOR_INT_DIST_TAX_AFTER, 2,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("1000.00"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean1 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("7.99"));

        ISettlementInvoiceEntryBean penaltyTaxEntryBean1 =
                PaymentPlanTestsUtilities.createPenaltyTaxEntryBean(debitEntry, new BigDecimal("20.00"));
        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 5, 1), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("509.93")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("4.07")));
            expectedInstallmentsBean.add(installmentBean);
        }

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("490.07")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("3.92")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(penaltyTaxEntryBean1, new BigDecimal("20.00")));
            expectedInstallmentsBean.add(installmentBean);
        }

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void interestsAfter_penaltyBefore_I5_D1_1000_D2_1_27() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, CONFIGURATOR_INT_AFTER_TAX_BEFORE, 5,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 8, 30), new LocalDate(2021, 12, 30));

        DebitEntry debitEntry1 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("1000.00"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean1 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry1, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean1);

        DebitEntry debitEntry2 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("1.27"), new LocalDate(2021, 3, 1), false);

        ISettlementInvoiceEntryBean debitEntryBean2 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry2, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean2);

        ISettlementInvoiceEntryBean interestEntryBean1 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry1, new BigDecimal("7.99"));

        ISettlementInvoiceEntryBean penaltyTaxEntryBean1 =
                PaymentPlanTestsUtilities.createPenaltyTaxEntryBean(debitEntry1, new BigDecimal("20.00"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 8, 30), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(penaltyTaxEntryBean1, new BigDecimal("20.00")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("185.85")));
            expectedInstallmentsBean.add(installmentBean);
        }

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 9, 29), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("205.85")));
            expectedInstallmentsBean.add(installmentBean);
        }

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 10, 30), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("205.85")));
            expectedInstallmentsBean.add(installmentBean);
        }

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 11, 29), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("205.85")));
            expectedInstallmentsBean.add(installmentBean);
        }

        {
            InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 12, 30), PaymentPlanTestsUtilities.ls(""));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("196.60")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("7.99")));
            installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("1.27")));
            expectedInstallmentsBean.add(installmentBean);
        }

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

}
