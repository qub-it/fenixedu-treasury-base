package org.fenixedu.treasury.domain.document;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.util.TreasuryConstants;

// 4.1.4.3.5 - Source Billing Options
public enum InvoiceSourceBillingType {
    P, I, M;
    
    public LocalizedString getDescriptionI18N() {
        return TreasuryConstants.treasuryBundleI18N(getClass().getName() + "." + name());
    }
}
