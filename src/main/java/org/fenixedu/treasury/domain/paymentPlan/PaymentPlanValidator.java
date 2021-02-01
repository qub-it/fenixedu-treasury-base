package org.fenixedu.treasury.domain.paymentPlan;

import java.util.List;

import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public abstract class PaymentPlanValidator extends PaymentPlanValidator_Base {

    public PaymentPlanValidator() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    abstract public Boolean validate(LocalDate date, List<Installment> sortedInstallments);

    public static String getPresentationName(Class<? extends PaymentPlanValidator> clazz) {
        return TreasuryConstants.treasuryBundle(clazz.getCanonicalName() + ".presentationName");
    }

    abstract public String getDescription();

    public void delete() {
        setParentValidator(null);
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Override
    protected abstract PaymentPlanValidator clone();

    public void delete(boolean deleteChilds) {
        delete();
    }
}
