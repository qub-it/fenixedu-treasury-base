package org.fenixedu.treasury.domain.paymentPlan;

import java.util.List;

import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

public class WithoutNonConsecutiveInstallmentsOverdueValidator extends WithoutNonConsecutiveInstallmentsOverdueValidator_Base {

    public WithoutNonConsecutiveInstallmentsOverdueValidator() {
        super();
    }

    public WithoutNonConsecutiveInstallmentsOverdueValidator(Integer numberInstallments, Integer numberDaysToTakeEffect) {
        this();
        setNumberDaysToTakeEffect(numberDaysToTakeEffect);
        setNumberInstallments(numberInstallments);
    }

    public WithoutNonConsecutiveInstallmentsOverdueValidator create(Integer numberInstallments, Integer numberDaysToTakeEffect) {
        return new WithoutNonConsecutiveInstallmentsOverdueValidator(numberInstallments, numberDaysToTakeEffect);
    }

    @Override
    public String getDescription() {
        return TreasuryConstants.treasuryBundle(
                "org.fenixedu.treasury.domain.paymentPlan.WithoutNonConsecutiveInstallmentsOverdueValidator.description",
                String.valueOf(getNumberInstallments()), String.valueOf(getNumberDaysToTakeEffect()));
    }

    @Override
    public Boolean validate(LocalDate date, List<Installment> sortedInstallments) {
        return getNumberInstallments() > sortedInstallments.stream()
                .filter(inst -> inst.isOverdue(date) && inst.getDueDate().isBefore(date.minusDays(getNumberDaysToTakeEffect())))
                .count();
    }

    @Override
    protected PaymentPlanValidator clone() {
        return new WithoutNonConsecutiveInstallmentsOverdueValidator(getNumberInstallments(), getNumberDaysToTakeEffect());
    }
}
