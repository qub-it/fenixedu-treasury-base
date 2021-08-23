package org.fenixedu.treasury.paymentplans;

import java.lang.reflect.UndeclaredThrowableException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.fenixedu.treasury.base.FenixFrameworkRunner;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
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
public class PaymentPlanConfiguratorTest {

    private static final String BLOCKING_CONFIGURATOR_NO_INT_NO_TAX = "No_Int No_Tax";
    private static final String BLOCKING_CONFIGURATOR_INT_AFTER_NO_TAX = "Int_After No_Tax";
    private static final String BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX = "Int_Dist No_Tax";

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
        PaymentPlanTestsUtilities.createBlockingConfigurator(BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, Boolean.TRUE,
                AddictionsCalculeTypeEnum.BY_INSTALLMENT_ENTRY_AMOUNT, null, Boolean.FALSE);

//        createBlockingConfigurator("Juros Bloquados J-D", Boolean.TRUE, AddictionsCalculeTypeEnum.BY_INSTALLMENT_ENTRY_AMOUNT,
//                null, Boolean.FALSE);
//        createBlockingConfigurator("Juros Bloquados J-F / T-F", Boolean.TRUE, AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY,
//                AddictionsCalculeTypeEnum.AFTER_DEBIT_ENTRY, Boolean.TRUE);
    }

    @Test
    public void noInterestNoTax_I3_D1_100() {
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
    public void noInterestNoTax_I3_D1_99_99() {
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
    public void interestAfter_WithoutInterestValue() {
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
    public void interestAfter_DebtNoApplyInterest() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_AFTER_NO_TAX, 3,
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
    public void interestAfter_Inst3_D1_100() {
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
    public void interestAfter_Inst3_D1_99_99() {
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
    public void interestAfter_FixedDates_Inst3_D1_99_99() {
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

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.59")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("32.81")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.79")));

        expectedInstallmentsBean.add(installmentBean);

        List<LocalDate> fixedDates = List.of(new LocalDate(2021, 6, 1), new LocalDate(2022, 5, 30), new LocalDate(2022, 6, 1));
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, fixedDates, null);
    }

    @Test
    public void interestAfter_FixedAmmounts_Inst3_D1_99_99() {
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
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("5")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("5")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("89.99")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.79")));

        expectedInstallmentsBean.add(installmentBean);

        List<BigDecimal> fixedAmount = List.of(new BigDecimal(5), new BigDecimal(5), new BigDecimal(90.78));
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, null, fixedAmount);
    }

    @Test
    public void interestAfter_FixedAmmountsAndDates_I3_D1_99_99() {
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
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("5")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("5")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("89.99")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.79")));

        expectedInstallmentsBean.add(installmentBean);

        List<BigDecimal> fixedAmount = List.of(new BigDecimal(5), new BigDecimal(5), new BigDecimal(90.78));
        List<LocalDate> fixedDates = List.of(new LocalDate(2021, 6, 1), new LocalDate(2022, 5, 30), new LocalDate(2022, 6, 1));
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, fixedDates, fixedAmount);
    }

    @Test
    public void interestAfter_WithCustomInterestValueInst3_D1_100() {
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
    public void interestDivided_Inst3_D1_100() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 3,
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
    public void interestDivided_FixedDates_Inst3_D1_99_99() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 3,
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
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.26")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.26")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.27")));

        expectedInstallmentsBean.add(installmentBean);

        List<LocalDate> fixedDates = List.of(new LocalDate(2021, 6, 1), new LocalDate(2022, 5, 30), new LocalDate(2022, 6, 1));
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, fixedDates, null);
    }

    @Test
    public void interestDivided_FixedAmmounts_Inst3_D1_99_99() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 3,
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
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("4.96")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.04")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("4.96")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.04")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("90.07")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.71")));

        expectedInstallmentsBean.add(installmentBean);

        List<BigDecimal> fixedAmount = List.of(new BigDecimal(5), new BigDecimal(5), new BigDecimal(90.78));
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, null, fixedAmount);
    }

    @Test
    public void interestDivided_FixedAmmountsAndDates_Inst3_D1_99_99() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 3,
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
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("4.96")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.04")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("4.96")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.04")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("90.07")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.71")));

        expectedInstallmentsBean.add(installmentBean);

        List<BigDecimal> fixedAmount = List.of(new BigDecimal(5), new BigDecimal(5), new BigDecimal(90.78));
        List<LocalDate> fixedDates = List.of(new LocalDate(2021, 6, 1), new LocalDate(2022, 5, 30), new LocalDate(2022, 6, 1));
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, fixedDates, fixedAmount);
    }

    @Test
    public void interestDivided_WithOneDebt_InterestCreatedBefore() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("100"), new LocalDate(2021, 2, 28), true);

        DebitEntry interestEntry =
                debitEntry.createInterestRateDebitEntry(debitEntry.calculateUndebitedInterestValue(new LocalDate(2021, 4, 1)),
                        new LocalDate(2021, 4, 1).toDateTimeAtStartOfDay(), Optional.empty());

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);

        ISettlementInvoiceEntryBean debitInterestEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(interestEntry, paymentPlanBean.getCreationDate(), null);

        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitInterestEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("0.39"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.34")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitInterestEntryBean, new BigDecimal("0.26")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.34")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitInterestEntryBean, new BigDecimal("0.14")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.12")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.32")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.27")));

        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean);
    }

    @Test
    public void interestDivided_WithOneDebtThanNotApplyInterest() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 3,
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
    public void interestDivided_WithTwoDebt_Inst3_D1_100_D2_50() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 3,
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

    //TODO
    public void interestDivided_FixedDates_Inst5_D1_99_99_D2_247_36() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 5,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry1 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("99.99"), new LocalDate(2021, 2, 28), true);

        DebitEntry debitEntry2 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("247.36"), new LocalDate(2021, 1, 1), true);

        ISettlementInvoiceEntryBean debitEntryBean1 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry1, paymentPlanBean.getCreationDate(), null);
        ISettlementInvoiceEntryBean debitEntryBean2 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry2, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean1);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean2);

        ISettlementInvoiceEntryBean interestEntryBean1 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry1, new BigDecimal("0.79"));

        ISettlementInvoiceEntryBean interestEntryBean2 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry2, new BigDecimal("3.79"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("69.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("1.06")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 28), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("69.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("1.06")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 29), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("69.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("1.06")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("39.37")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("0.61")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("30.17")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.24")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("69.82")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.55")));

        expectedInstallmentsBean.add(installmentBean);

        List<LocalDate> fixedDates = List.of(new LocalDate(2021, 6, 1), new LocalDate(2022, 5, 28), new LocalDate(2022, 5, 29),
                new LocalDate(2022, 5, 30), new LocalDate(2022, 6, 1));
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, fixedDates, null);
    }

    @Test
    public void interestDivided_FixedAmmounts_Inst5_D1_99_99_D2_247_36() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 5,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry1 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("99.99"), new LocalDate(2021, 2, 28), true);

        DebitEntry debitEntry2 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("247.36"), new LocalDate(2021, 1, 1), true);

        ISettlementInvoiceEntryBean debitEntryBean1 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry1, paymentPlanBean.getCreationDate(), null);
        ISettlementInvoiceEntryBean debitEntryBean2 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry2, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean1);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean2);

        ISettlementInvoiceEntryBean interestEntryBean1 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry1, new BigDecimal("0.79"));

        ISettlementInvoiceEntryBean interestEntryBean2 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry2, new BigDecimal("3.79"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("49.25")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("0.75")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 8, 31), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("69.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("1.06")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2021, 11, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("128.78")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("1.98")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("10.16")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.08")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 3, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("69.84")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.55")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("19.99")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.16")));
        expectedInstallmentsBean.add(installmentBean);

        List<BigDecimal> fixedAmount = List.of(new BigDecimal(50), new BigDecimal(70.39), new BigDecimal(141),
                new BigDecimal(70.39), new BigDecimal(20.15));
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, null, fixedAmount);
    }

    @Test
    public void interestDivided_FixedAmmountsAndDates_Inst3_D1_99_99_D2_247_36() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 5,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry1 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("99.99"), new LocalDate(2021, 2, 28), true);

        DebitEntry debitEntry2 =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("247.36"), new LocalDate(2021, 1, 1), true);

        ISettlementInvoiceEntryBean debitEntryBean1 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry1, paymentPlanBean.getCreationDate(), null);
        ISettlementInvoiceEntryBean debitEntryBean2 =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry2, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean1);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean2);

        ISettlementInvoiceEntryBean interestEntryBean1 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry1, new BigDecimal("0.79"));

        ISettlementInvoiceEntryBean interestEntryBean2 =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry2, new BigDecimal("3.79"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("49.25")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("0.75")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 28), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("69.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("1.06")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 29), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean2, new BigDecimal("128.78")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean2, new BigDecimal("1.98")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("10.16")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.08")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("69.84")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.55")));
        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean1, new BigDecimal("19.99")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean1, new BigDecimal("0.16")));
        expectedInstallmentsBean.add(installmentBean);

        List<BigDecimal> fixedAmount = List.of(new BigDecimal(50), new BigDecimal(70.39), new BigDecimal(141),
                new BigDecimal(70.39), new BigDecimal(20.15));
        List<LocalDate> fixedDates = List.of(new LocalDate(2021, 6, 1), new LocalDate(2022, 5, 28), new LocalDate(2022, 5, 29),
                new LocalDate(2022, 5, 30), new LocalDate(2022, 6, 1));
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, fixedDates, fixedAmount);
    }

    public void interestDivided_InterestGreaterThanDebt_Inst3_D1_99_99_D2_247_36() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("99.99"), new LocalDate(2021, 2, 28), true);

        SettlementNote settlementNote =
                SettlementNote.create(paymentPlanBean.getDebtAccount(), null, new LocalDate(2021, 5, 1).toDateTimeAtStartOfDay(),
                        new LocalDate(2021, 5, 1).toDateTimeAtStartOfDay(), null, null);

        SettlementEntry.create(debitEntry, new BigDecimal("99.90"), settlementNote,
                new LocalDate(2021, 5, 1).toDateTimeAtStartOfDay());
        settlementNote.closeDocument();

        ISettlementInvoiceEntryBean debitEntryBean =
                PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry, paymentPlanBean.getCreationDate(), null);
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("0.79"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("0.03")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.26")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("0.03")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.26")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("0.03")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("0.27")));

        expectedInstallmentsBean.add(installmentBean);

        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, null, null);
    }

    public void interestDivided_InterestGreaterThanDebtInterest_Inst3_D1_99_99_D2_247_36() {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();

        PaymentPlanBean paymentPlanBean =
                PaymentPlanTestsUtilities.createPaymentPlanBean(methodName, BLOCKING_CONFIGURATOR_INT_DIVIDED_NO_TAX, 3,
                        new LocalDate(2021, 5, 1), new LocalDate(2021, 6, 1), new LocalDate(2022, 6, 1));

        DebitEntry debitEntry =
                PaymentPlanTestsUtilities.createDebitEntry(new BigDecimal("99.99"), new LocalDate(2021, 2, 28), true);

        ISettlementInvoiceEntryBean debitEntryBean = PaymentPlanTestsUtilities.createDebitEntryBean(debitEntry,
                paymentPlanBean.getCreationDate(), new BigDecimal("2.50"));
        paymentPlanBean.addSettlementInvoiceEntryBean(debitEntryBean);

        ISettlementInvoiceEntryBean interestEntryBean =
                PaymentPlanTestsUtilities.createInterestEntryBean(debitEntry, new BigDecimal("3.00"));

        List<InstallmentBean> expectedInstallmentsBean = new ArrayList<>();

        InstallmentBean installmentBean = new InstallmentBean(new LocalDate(2021, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("1.00")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 5, 30), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("1.00")));

        expectedInstallmentsBean.add(installmentBean);

        installmentBean = new InstallmentBean(new LocalDate(2022, 6, 1), PaymentPlanTestsUtilities.ls(""));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(debitEntryBean, new BigDecimal("33.33")));
        installmentBean.addInstallmentEntries(new InstallmentEntryBean(interestEntryBean, new BigDecimal("1.00")));

        expectedInstallmentsBean.add(installmentBean);
        PaymentPlanTestsUtilities.runTests(paymentPlanBean, expectedInstallmentsBean, null, null);
    }

}
