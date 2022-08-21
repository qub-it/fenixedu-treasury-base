package org.fenixedu.treasury.domain.document;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import org.fenixedu.commons.i18n.LocalizedString;

// 4.1.4.3.5 - Source Billing Options
public enum InvoiceSourceBillingType {
    P, I, M;
    
    public LocalizedString getDescriptionI18N() {
        return treasuryBundleI18N(getClass().getName() + "." + name());
    }
}
