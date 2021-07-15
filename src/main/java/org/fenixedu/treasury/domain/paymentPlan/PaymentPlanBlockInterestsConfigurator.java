package org.fenixedu.treasury.domain.paymentPlan;

public class PaymentPlanBlockInterestsConfigurator extends PaymentPlanBlockInterestsConfigurator_Base {

    public PaymentPlanBlockInterestsConfigurator() {
        super();
    }

    @Override
    public boolean isApplyInterest() {
        return Boolean.TRUE.equals(getApplyDebitEntryInterest());
    }

    @Override
    public boolean isInterestBlocked() {
        return !isApplyInterest();
    }

}
