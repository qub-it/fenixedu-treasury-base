package org.fenixedu.treasury.domain.sibspay;

import org.fenixedu.commons.i18n.LocalizedString;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

public enum MbwayMandatePaymentScheduleState {
    SCHEDULED, EMAIL_SENT, PAYMENT_CHARGED, ANNULLED;

    public String getCode() {
        return name();
    }

    public LocalizedString getLocalizedName() {
        return treasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }
}
