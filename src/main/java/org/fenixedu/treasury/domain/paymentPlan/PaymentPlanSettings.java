package org.fenixedu.treasury.domain.paymentPlan;

import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.settings.TreasurySettings;

import jvstm.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPlanSettings extends PaymentPlanSettings_Base {

    public PaymentPlanSettings() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setActive(Boolean.FALSE);
        setTreasurySettings(TreasurySettings.getInstance());
    }

    private PaymentPlanSettings(LocalizedString installmentDescriptionFormat, Boolean interestCalculationOfDebitsInPlans,
            Product emolumentProduct, Integer numberOfPaymentPlansActives) {
        this();

        setInstallmentDescriptionFormat(installmentDescriptionFormat);
        setInterestCalculationOfDebitsInPlans(interestCalculationOfDebitsInPlans);
        setEmolumentProduct(emolumentProduct);

        setNumberOfPaymentPlansActives(numberOfPaymentPlansActives);

        checkRules();
    }

    private void checkRules() {
        if (getTreasurySettings() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.treasurySettings.required");
        }
        if (getInstallmentDescriptionFormat() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.InstallmentDescriptionFormat.required");
        }
        if (getInterestCalculationOfDebitsInPlans() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.InterestCalculationOfDebitsInPlans.required");
        }
        if (getEmolumentProduct() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.EmolumentProduct.required");
        }
        if (Boolean.TRUE.equals(getActive()) && getTreasurySettings().getPaymentPlanSettingsSet().stream()
                .anyMatch(p -> p != this && Boolean.TRUE.equals(p.getActive()))) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.only.one.can.be.active");
        }

        if (getNumberOfPaymentPlansActives() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.numberOfPaymentPlansActives.required");
        }

        if (getInstallmentDescriptionFormat().anyMatch(o -> !o.contains("${paymentPlanId}"))) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.installmentDescriptionFormat.payment.plan.id.required");
        }

        if (getInstallmentDescriptionFormat().anyMatch(o -> !o.contains("${installmentNumber}"))) {
            throw new TreasuryDomainException(
                    "error.PaymentPlanSettings.installmentDescriptionFormat.installment.number.required");
        }

    }

    @Atomic
    public static PaymentPlanSettings create(LocalizedString installmentDescriptionFormat,
            Boolean interestCalculationOfDebitsInPlans, Product emolumentProduct, Integer numberOfPaymentPlansActives) {
        return new PaymentPlanSettings(installmentDescriptionFormat, interestCalculationOfDebitsInPlans, emolumentProduct,
                numberOfPaymentPlansActives);
    }

    @Override
    @Atomic
    public void setActive(Boolean active) {
        super.setActive(active);
        checkRules();
    }

    /*
     * SERVICES
     */

    public static Stream<PaymentPlanSettings> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPlanSettingsSet().stream();
    }

    public static PaymentPlanSettings getActiveInstance() {
        return TreasurySettings.getInstance().getPaymentPlanSettingsSet().stream().filter(p -> Boolean.TRUE.equals(p.getActive()))
                .findFirst().orElse(null);
    }

    public Boolean isActive() {
        return Boolean.TRUE.equals(getActive());
    }
}
