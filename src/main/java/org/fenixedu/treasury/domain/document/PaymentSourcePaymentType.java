package org.fenixedu.treasury.domain.document;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.util.TreasuryConstants;

// 4.4.4.9.5 - SourcePayment
public enum PaymentSourcePaymentType {
    P, I, M;
    
    public LocalizedString getDescriptionI18N() {
        return TreasuryConstants.treasuryBundleI18N(getClass().getName() + "." + name());
    }    
}
