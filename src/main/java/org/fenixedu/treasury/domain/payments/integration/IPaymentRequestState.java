package org.fenixedu.treasury.domain.payments.integration;

import org.fenixedu.commons.i18n.LocalizedString;

public interface IPaymentRequestState {

    public String getCode();
    public LocalizedString getLocalizedName();
    
}
