package org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator;

import java.util.List;

import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

public class WithoutConsecutiveInstallmentsOverdueValidator extends WithoutConsecutiveInstallmentsOverdueValidator_Base {

    public WithoutConsecutiveInstallmentsOverdueValidator() {
        super();

    }

    public WithoutConsecutiveInstallmentsOverdueValidator(Integer numberInstallments, Integer numberDaysToTakeEffect) {
        this();
        setNumberDaysToTakeEffect(numberDaysToTakeEffect);
        setNumberInstallments(numberInstallments);
    }

    public WithoutConsecutiveInstallmentsOverdueValidator create(Integer numberInstallments, Integer numberDaysToTakeEffect) {
        return new WithoutConsecutiveInstallmentsOverdueValidator(numberInstallments, numberDaysToTakeEffect);
    }

    @Override
    public String getDescription() {
        return TreasuryConstants.treasuryBundle(
                "org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator.WithoutConsecutiveInstallmentsOverdueValidator.description",
                String.valueOf(getNumberInstallments()), String.valueOf(getNumberDaysToTakeEffect()));
    }

    @Override
    public Boolean validate(LocalDate date, List<Installment> sortedInstallments) {
        int count = 0;
        for (Installment installment : sortedInstallments) {
            if (installment.isOverdue(date) && installment.getDueDate().isBefore(date.minusDays(getNumberDaysToTakeEffect()))) {
                count++;
                if (count == getNumberInstallments()) {
                    return false;
                }
            } else {
                count = 0;
            }
        }
        return true;
    }

    @Override
    protected PaymentPlanValidator clone() {
        return new WithoutConsecutiveInstallmentsOverdueValidator(getNumberInstallments(), getNumberDaysToTakeEffect());
    }

}
