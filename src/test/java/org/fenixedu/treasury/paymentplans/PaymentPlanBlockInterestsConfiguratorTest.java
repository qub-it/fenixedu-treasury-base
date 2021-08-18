package org.fenixedu.treasury.paymentplans;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.document.DebitEntry;
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
public class PaymentPlanBlockInterestsConfiguratorTest {

    private static final String BLOCKING_CONFIGURATOR_NO_INT_NO_TAX = "blockingConfigurator No_Int No_Tax";
    private static final String BLOCKING_CONFIGURATOR_INT_AFTER_NO_TAX = "blockingConfigurator Int_After No_Tax";
    private static final String BLOCKING_CONFIGURATOR_INT_DIST_NO_TAX = "blockingConfigurator Int_Dist No_Tax";

    @BeforeClass
    public static void beforeClass() {
        PaymentPlanTestsUtilities.startUp();
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                createConfigurators();
                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }
    }

    private static void createConfigurators() {
        PaymentPlanTestsUtilities.createBlockingConfigurator(BLOCKING_CONFIGURATOR_NO_INT_NO_TAX, Boolean.FALSE,
                AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, null, Boolean.FALSE);
        PaymentPlanTestsUtilities.createBlockingConfigurator(BLOCKING_CONFIGURATOR_INT_AFTER_NO_TAX, Boolean.TRUE,
                AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, null, Boolean.FALSE);
        PaymentPlanTestsUtilities.createBlockingConfigurator(BLOCKING_CONFIGURATOR_INT_DIST_NO_TAX, Boolean.TRUE,
                AddictionsCalculeTypeEnum.BY_INSTALLMENT_ENTRY_AMOUNT, null, Boolean.FALSE);

//        createBlockingConfigurator("Juros Bloquados J-D", Boolean.TRUE, AddictionsCalculeTypeEnum.BY_INSTALLMENT_ENTRY_AMOUNT,
//                null, Boolean.FALSE);
//        createBlockingConfigurator("Juros Bloquados J-F / T-F", Boolean.TRUE, AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY,
//                AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, Boolean.TRUE);
    }

    @Test
    public void blockingConfiguratorNoInterestNoTax_I3_D1_100() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_NO_INT_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("100"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.34")));
        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void blockingConfiguratorNoInterestNoTax_I3_D1_99_99() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_NO_INT_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("99.99"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void blockingConfiguratorInterestAfterAndNoApplyPenaltyTax_WithoutInterestValue() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_AFTER_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("100"), new LocalDate(2021, 5, 1), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.34")));
        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void blockingConfiguratorInterestAfterAndNoApplyPenaltyTax_WithInterestValue_I3_D1_100() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_AFTER_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("100"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("0.79"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.60")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.60")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("32.80")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.79")));

        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void blockingConfiguratorInterestAfterAndNoApplyPenaltyTax_WithInterestValue_I3_D1_99_99() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_AFTER_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("99.99"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("0.79"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.59")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.59")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("32.81")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.79")));

        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void blockingConfiguratorInterestAfterAndNoApplyPenaltyTax_WithCustomInterestValue() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_AFTER_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("100"), new LocalDate(2021, 3, 1), true);

        ISettlementInvoiceEntryBean debitEntryBean = PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry,
                paymentPlanBean.getCreationDate(), new BigDecimal("100.50"));
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("0.79"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.50")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.50")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.00")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.50")));

        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void blockingConfiguratorInterestDistAndNoApplyPenaltyTax_WithOneDebt() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIST_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("100"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("0.79"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.34")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.26")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.34")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.26")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.32")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.27")));

        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void blockingConfiguratorInterestDistAndNoApplyPenaltyTax_WithOneDebtThanNotApplyInterest() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIST_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("100"), new LocalDate(2021, 2, 28), false);

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.34")));

        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void blockingConfiguratorInterestDistAndNoApplyPenaltyTax_WithTwoDebt() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIST_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry1 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("100"), new LocalDate(2021, 2, 28), true);
        DebitEntry debitEntry2 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("50"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean1 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry1, paymentPlanBean.getCreationDate(), null);
        ISettlementInvoiceEntryBean debitEntryBean2 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry2, paymentPlanBean.getCreationDate(), null);

        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean1);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean2);

        ISettlementInvoiceEntryBean interestEntryBean1 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry1, new BigDecimal("0.79"));
        ISettlementInvoiceEntryBean interestEntryBean2 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry2, new BigDecimal("0.39"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("50.00")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.39")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("50.00")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.39")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.01")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("50.00")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("0.39")));
        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

//    @Test
//    public void blockingConfiguratorInterestAfterAndNoApplyPenaltyTax_WithInterestValueIsGreaterThenDebtAmount() {
//        String methodName = new Object() {
//        }.getClass().getEnclosingMethod().getName();
//
//        PaymentPlanBean paymentPlanBean = createPaymentPlanBean(methodName, "blockingConfigurator Int_After No_Tax", 3,
//                new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));
//
//        Set<ISettlementInvoiceEntryBean> list = new HashSet<>();
//        list.add(createDebitEntryBean(new BigDecimal("1"), new LocalDate(2021, 3, 1), paymentPlanBean.getCreationDate(), true,
//                new BigDecimal("100.50")));
//        paymentPlanBean.setSettlementInvoiceEntryBeans(list);
//
//        // Results
//        List<InstallmentBean> installmentsBeans =
//                paymentPlanBean.getPaymentPlanConfigurator().getInstallmentsBeansFor(paymentPlanBean);
//        assertInstallment(paymentPlanBean, installmentsBeans.get(0), "33.50", new String[] { "33.50" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(1), "33.50", new String[] { "33.50" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(2), "33.50", new String[] { "33.00", "0.50" });
//    }
//
//    @Test
//    public void blockingConfiguratorInterestAfterAndNoApplyPenaltyTax_WithInterestValueOnOneDebt() {
//        String methodName = new Object() {
//        }.getClass().getEnclosingMethod().getName();
//
//        PaymentPlanBean paymentPlanBean = createPaymentPlanBean(methodName, "blockingConfigurator Int_After No_Tax", 3,
//                new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));
//
//        Set<ISettlementInvoiceEntryBean> list = new HashSet<>();
//        list.add(
//                createDebitEntryBean(new BigDecimal("100"), new LocalDate(2021, 2, 28), paymentPlanBean.getCreationDate(), true));
//        list.add(createDebitEntryBean(new BigDecimal("50"), new LocalDate(2021, 5, 1), paymentPlanBean.getCreationDate(), true));
//        paymentPlanBean.setSettlementInvoiceEntryBeans(list);
//
//        // Results
//        List<InstallmentBean> installmentsBeans =
//                paymentPlanBean.getPaymentPlanConfigurator().getInstallmentsBeansFor(paymentPlanBean);
//        assertInstallment(paymentPlanBean, installmentsBeans.get(0), "33.60", new String[] { "33.60" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(1), "33.60", new String[] { "33.60" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(2), "33.59", new String[] { "32.80", "0.79" });
//    }
//
//    @Test
//    public void blockingConfiguratorInterestAfterAndNoApplyPenaltyTax_WithoutInterestValueOnOneDebt() {
//        String methodName = new Object() {
//        }.getClass().getEnclosingMethod().getName();
//
//        PaymentPlanBean paymentPlanBean = createPaymentPlanBean(methodName, "blockingConfigurator Int_After No_Tax", 3,
//                new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));
//
//        Set<ISettlementInvoiceEntryBean> list = new HashSet<>();
//        list.add(
//                createDebitEntryBean(new BigDecimal("100"), new LocalDate(2021, 2, 28), paymentPlanBean.getCreationDate(), true));
//        list.add(
//                createDebitEntryBean(new BigDecimal("50"), new LocalDate(2021, 2, 28), paymentPlanBean.getCreationDate(), false));
//        paymentPlanBean.setSettlementInvoiceEntryBeans(list);
//
//        // Results
//        List<InstallmentBean> installmentsBeans =
//                paymentPlanBean.getPaymentPlanConfigurator().getInstallmentsBeansFor(paymentPlanBean);
//        assertInstallment(paymentPlanBean, installmentsBeans.get(0), "33.60", new String[] { "33.60" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(1), "33.60", new String[] { "33.60" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(2), "33.59", new String[] { "32.80", "0.79" });
//    }
//
//    @Test
//    public void blockingConfiguratorInterestAfterAndNoApplyPenaltyTax_WithInterestValueOnTwoDebt() {
//        String methodName = new Object() {
//        }.getClass().getEnclosingMethod().getName();
//
//        PaymentPlanBean paymentPlanBean = createPaymentPlanBean(methodName, "blockingConfigurator Int_After No_Tax", 3,
//                new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));
//
//        Set<ISettlementInvoiceEntryBean> list = new HashSet<>();
//        list.add(
//                createDebitEntryBean(new BigDecimal("100"), new LocalDate(2021, 2, 28), paymentPlanBean.getCreationDate(), true));
//        list.add(createDebitEntryBean(new BigDecimal("50"), new LocalDate(2021, 2, 28), paymentPlanBean.getCreationDate(), true));
//        paymentPlanBean.setSettlementInvoiceEntryBeans(list);
//
//        // Results
//        List<InstallmentBean> installmentsBeans =
//                paymentPlanBean.getPaymentPlanConfigurator().getInstallmentsBeansFor(paymentPlanBean);
//        assertInstallment(paymentPlanBean, installmentsBeans.get(0), "33.60", new String[] { "33.60" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(1), "33.60", new String[] { "33.60" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(2), "33.59", new String[] { "32.80", "0.79" });
//    }
//
//    @Test
//    public void blockingConfiguratorInterestAfterAndNoApplyPenaltyTax_WithoutInterestValueOnTwoDebt() {
//        String methodName = new Object() {
//        }.getClass().getEnclosingMethod().getName();
//
//        PaymentPlanBean paymentPlanBean = createPaymentPlanBean(methodName, "blockingConfigurator Int_After No_Tax", 3,
//                new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));
//
//        Set<ISettlementInvoiceEntryBean> list = new HashSet<>();
//        list.add(createDebitEntryBean(new BigDecimal("100"), new LocalDate(2021, 2, 28), paymentPlanBean.getCreationDate(),
//                false));
//        list.add(
//                createDebitEntryBean(new BigDecimal("50"), new LocalDate(2021, 2, 28), paymentPlanBean.getCreationDate(), false));
//        paymentPlanBean.setSettlementInvoiceEntryBeans(list);
//
//        // Results
//        List<InstallmentBean> installmentsBeans =
//                paymentPlanBean.getPaymentPlanConfigurator().getInstallmentsBeansFor(paymentPlanBean);
//        assertInstallment(paymentPlanBean, installmentsBeans.get(0), "33.60", new String[] { "33.60" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(1), "33.60", new String[] { "33.60" });
//        assertInstallment(paymentPlanBean, installmentsBeans.get(2), "33.59", new String[] { "32.80", "0.79" });
//    }

}
