package org.fenixedu.treasury.domain.paymentPlan;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class PaymentPlanGroupValidator extends PaymentPlanGroupValidator_Base {

    public static enum ConjunctionWidget {
        AND(Boolean.TRUE), OR(Boolean.FALSE);

        private Boolean conjuction;

        ConjunctionWidget(Boolean conjuction) {
            this.conjuction = conjuction;
        }

        public Boolean getConjuction() {
            return conjuction;
        }
    }

    public PaymentPlanGroupValidator() {
        super();
        setConjunction(Boolean.TRUE);
    }

    protected PaymentPlanGroupValidator(Set<PaymentPlanValidator> childs, Boolean isConjunction) {
        this();
        getChildValidatorsSet().addAll(childs);
        setConjunction(isConjunction);
    }

    public static PaymentPlanGroupValidator create(Set<PaymentPlanValidator> childs, Boolean isConjunction) {
        return new PaymentPlanGroupValidator(childs, isConjunction);
    }

    @Override
    public Boolean validate(LocalDate date, List<Installment> sortedInstallments) {
        if (getParentValidator() == null || Boolean.TRUE.equals(getConjunction())) {
            // validator1 AND validator2
            return getChildValidatorsSet().stream().allMatch(v -> v.validate(date, sortedInstallments));
        } else {
            // validator1 OR validator2
            return getChildValidatorsSet().stream().anyMatch(v -> v.validate(date, sortedInstallments));
        }
    }

    @Override
    public String getDescription() {
        if (getConjunction()) {
            return TreasuryConstants
                    .treasuryBundle("org.fenixedu.treasury.domain.paymentPlan.PaymentPlanGroupValidator.GroupofrulesAND");
        } else {
            return TreasuryConstants
                    .treasuryBundle("org.fenixedu.treasury.domain.paymentPlan.PaymentPlanGroupValidator.GroupofrulesOR");
        }
    }

    @Override
    public void delete() {
        if (!getPaymentPlansSet().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete Payment Plan Validator with associeted plans");
        } else if (getParentValidator() == null) {
            getChildValidatorsSet().forEach(v -> v.delete(true));
            setDomainRoot(null);
            deleteDomainObject();
        } else {
            getChildValidatorsSet().forEach(v -> v.setParentValidator(getParentValidator()));
            setParentValidator(null);
            setDomainRoot(null);
            deleteDomainObject();
        }
    }

    @Override
    public void delete(boolean deleteChilds) {
        if (!getPaymentPlansSet().isEmpty()) {
            throw new IllegalArgumentException("Cannot delete Payment Plan Validator with associeted plans");
        } else if (getParentValidator() == null) {
            getChildValidatorsSet().forEach(v -> v.delete(true));
            setDomainRoot(null);
            deleteDomainObject();
        } else {
            if (deleteChilds) {
                getChildValidatorsSet().forEach(v -> v.delete(deleteChilds));
            } else {
                getChildValidatorsSet().forEach(v -> v.setParentValidator(getParentValidator()));
            }
            setParentValidator(null);
            setDomainRoot(null);
            deleteDomainObject();
        }
    }

    public static Set<PaymentPlanGroupValidator> findRootGroupValidators() {
        return FenixFramework.getDomainRoot().getPaymentPlanValidatorsSet().stream()
                .filter(PaymentPlanGroupValidator.class::isInstance).map(PaymentPlanGroupValidator.class::cast)
                .filter(p -> p.getParentValidator() == null).collect(Collectors.toSet());
    }

    public static Set<PaymentPlanGroupValidator> findActiveGroupValidators() {
        return findRootGroupValidators().stream().filter(p -> Boolean.TRUE.equals(p.getActive())).collect(Collectors.toSet());
    }

    public static PaymentPlanGroupValidator create(LocalizedString newName, Boolean active, PaymentPlanGroupValidator base) {
        PaymentPlanGroupValidator result = new PaymentPlanGroupValidator();
        result.setName(newName);
        result.setActive(active);
        result.setConjunction(Boolean.TRUE);

        for (PaymentPlanValidator validator : base.getChildValidatorsSet()) {
            result.addChildValidators(validator.clone());
        }

        return result;
    }

    @Override
    protected PaymentPlanValidator clone() {
        PaymentPlanGroupValidator result = new PaymentPlanGroupValidator();

        result.setConjunction(getConjunction());

        for (PaymentPlanValidator validator : getChildValidatorsSet()) {
            result.addChildValidators(validator.clone());
        }
        return result;
    }

}
