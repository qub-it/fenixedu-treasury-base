package org.fenixedu.treasury.domain.sibspay;

import org.fenixedu.commons.i18n.LocalizedString;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

public enum MbwayMandatePaymentScheduleState {
    SCHEDULED, EMAIL_SENT, PAYMENT_CHARGED, ANNULLED, ERROR, TRANSFERRED;

    public String getCode() {
        return name();
    }

    public LocalizedString getLocalizedName() {
        return treasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }

    public boolean isScheduled() {
        return this == SCHEDULED;
    }

    public boolean isEmailSent() {
        return this == EMAIL_SENT;
    }

    public boolean isPaymentCharged() {
        return this == PAYMENT_CHARGED;
    }

    public boolean isAnnulled() {
        return this == ANNULLED;
    }

    public boolean isError() {
        return this == ERROR;
    }
}
