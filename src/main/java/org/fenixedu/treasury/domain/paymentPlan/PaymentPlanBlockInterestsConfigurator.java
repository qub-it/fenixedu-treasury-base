package org.fenixedu.treasury.domain.paymentPlan;

import org.joda.time.LocalDate;

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

    @Override
    protected LocalDate getDateToUseToPenaltyTaxCalculation(LocalDate creationDate, LocalDate dueDate) {
        return creationDate;
    }

    @Override
    public boolean canChangeInstallmentsAmount() {
        return Boolean.TRUE.equals(getCanEditInstallmentAmount());
    }

}
