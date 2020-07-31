package org.fenixedu.treasury.domain.forwardpayments;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.util.Comparator;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.payments.integration.IPaymentRequestState;

public enum ForwardPaymentStateType implements IPaymentRequestState {
    
    CREATED,
    REQUESTED,
    AUTHENTICATED,
    AUTHORIZED,
    PAYED,
    REJECTED;
    
    public static final Comparator<ForwardPaymentStateType> COMPARE_BY_LOCALIZED_NAME = new Comparator<ForwardPaymentStateType>() {

        @Override
        public int compare(final ForwardPaymentStateType o1, final ForwardPaymentStateType o2) {
            final int c = o1.getLocalizedName().compareTo(o2.getLocalizedName());
            return c != 0 ? c : o1.compareTo(o2);
        }
    };
    
    public boolean isCreated() {
        return this == CREATED;
    }
    
    public boolean isRequested() {
        return this == REQUESTED;
    }
    
    public boolean isAuthenticated() {
        return this == AUTHENTICATED;
    }
    
    public boolean isAuthorized() {
        return this == AUTHORIZED;
    }
    
    public boolean isPayed() {
        return this == PAYED;
    }
    
    public boolean isRejected() {
        return this == REJECTED;
    }
    
    public boolean isInStateToPostProcessPayment() {
        return isCreated() || isRequested();
    }
    
    @Override
    public LocalizedString getLocalizedName() {
        return treasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }

    @Override
    public String getCode() {
        return name();
    }
}
