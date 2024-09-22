package org.fenixedu.treasury.domain.sibspay;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import org.fenixedu.commons.i18n.LocalizedString;

public enum MbwayMandateState {

    CREATED, //
    WAITING_AUTHORIZATION, //
    ACTIVE, //
    SUSPENDED, //
    CANCELED, //
    EXPIRED, //
    NOT_AUTHORIZED, //
    TRANSFERRED;

    public boolean isCreated() {
        return this == CREATED;
    }

    public boolean isWaitingAuthorization() {
        return this == WAITING_AUTHORIZATION;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean isSuspended() {
        return this == SUSPENDED;
    }

    public boolean isCanceled() {
        return this == CANCELED;
    }

    public boolean isExpired() {
        return this == EXPIRED;
    }

    public boolean isNotAuthorized() {
        return this == NOT_AUTHORIZED;
    }

    public boolean isTransferred() {
        return this == TRANSFERRED;
    }

    public String getCode() {
        return name();
    }

    public LocalizedString getLocalizedName() {
        return treasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }

}
