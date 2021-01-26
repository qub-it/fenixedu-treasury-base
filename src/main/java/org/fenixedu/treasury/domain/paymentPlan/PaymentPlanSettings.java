package org.fenixedu.treasury.domain.paymentPlan;

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

	private PaymentPlanSettings(LocalizedString installmentDescriptionFormat,
			Boolean interestCalculationOfDebitsInPlans, Product emolumentProduct) {
		setInstallmentDescriptionFormat(installmentDescriptionFormat);
		setInterestCalculationOfDebitsInPlans(interestCalculationOfDebitsInPlans);
		setEmolumentProduct(emolumentProduct);
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
	}

	@Atomic
	public static PaymentPlanSettings create(LocalizedString installmentDescriptionFormat,
			Boolean interestCalculationOfDebitsInPlans, Product emolumentProduct) {
		return new PaymentPlanSettings(installmentDescriptionFormat, interestCalculationOfDebitsInPlans,
				emolumentProduct);
	}

	@Atomic
	public void active(Boolean active) {
		setActive(active);
		checkRules();
	}

	public static PaymentPlanSettings getActiveInstance() {
		return TreasurySettings.getInstance().getPaymentPlanSettingsSet().stream()
				.filter(p -> Boolean.TRUE.equals(p.getActive())).findFirst().orElse(null);
	}

	public Boolean isActive() {
		return Boolean.TRUE.equals(getActive());
	}
}
