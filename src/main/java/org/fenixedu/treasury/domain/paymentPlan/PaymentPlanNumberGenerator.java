package org.fenixedu.treasury.domain.paymentPlan;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

public class PaymentPlanNumberGenerator extends PaymentPlanNumberGenerator_Base {

    public PaymentPlanNumberGenerator() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    public PaymentPlanNumberGenerator(LocalizedString name, String prefix, Integer initialValue) {
        this();
        setName(name);
        setPrefix(prefix);
        setInitialValue(initialValue);
        checkRules();
    }

    public static PaymentPlanNumberGenerator create(LocalizedString name, String prefix, Integer initialValue) {
        return new PaymentPlanNumberGenerator(name, prefix, initialValue);
    }

    private void checkRules() {
        if (getInitialValue() == null) {
            throw new TreasuryDomainException("error.PaymentPlanNumberGenerator.initialValue.required");
        }

        if (getPrefix() == null) {
            throw new TreasuryDomainException("error.PaymentPlanNumberGenerator.prefix.required");
        }

        if (getName() == null) {
            throw new TreasuryDomainException("error.PaymentPlanNumberGenerator.name.required");
        }
    }

    protected String getPrefixToGenerateNumber() {
        return StringUtils.isEmpty(getPrefix()) ? "" : getPrefix();
    }

    public String generateNumber() {
        setActualValue(getActualValue() == null ? getInitialValue() : getActualValue() + 1);
        return getPrefixToGenerateNumber() + getActualValue();
    }

    public String getNextNumberPreview() {
        return getPrefixToGenerateNumber() + (getActualValue() == null ? getInitialValue() : getActualValue() + 1);
    }

    public void delete() {
        if (!getPaymentPlanSettingsSet().isEmpty()) {
            throw new TreasuryDomainException("error.PaymentPlanNumberGenerator.in.settings.cannot.be.deleted");
        }
        setDomainRoot(null);
        super.deleteDomainObject();
    }

}
