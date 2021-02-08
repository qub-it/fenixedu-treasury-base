package org.fenixedu.treasury.domain.paymentPlan;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.util.ArrayList;
import java.util.List;

import org.fenixedu.commons.i18n.LocalizedString;

public enum PaymentPlanStateType {
    CLOSED, ANNULED, OPEN, ACTIVE, TRANSFERRED;

    public boolean isOpen() {
        return this == OPEN;
    }

    public boolean isClosed() {
        return this == CLOSED;
    }

    public boolean isAnnuled() {
        return this == ANNULED;
    }

    public LocalizedString getDescriptionI18N() {
        return treasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }

    public static List<PaymentPlanStateType> findAll() {
        List<PaymentPlanStateType> result = new ArrayList<PaymentPlanStateType>();
        result.add(CLOSED);
        result.add(ANNULED);
        result.add(OPEN);
        result.add(TRANSFERRED);
        return result;
    }
}
