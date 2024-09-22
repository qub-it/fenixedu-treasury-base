package org.fenixedu.treasury.domain.sibspay;

import org.fenixedu.commons.i18n.LocalizedString;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

public enum MbwayMandatePaymentScheduleState {
    SCHEDULED, NOTIFICATION_SENT, PAYMENT_CHARGED, CANCELED, ERROR, TRANSFERRED;

    public String getCode() {
        return name();
    }

    public LocalizedString getLocalizedName() {
        return treasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }

    public boolean isScheduled() {
        return this == SCHEDULED;
    }

    public boolean isNotificationSent() {
        return this == NOTIFICATION_SENT;
    }

    public boolean isPaymentCharged() {
        return this == PAYMENT_CHARGED;
    }

    public boolean isCanceled() {
        return this == CANCELED;
    }

    public boolean isError() {
        return this == ERROR;
    }
}
