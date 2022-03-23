package org.fenixedu.treasury.domain;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.util.TreasuryConstants;

public enum InvoiceRegistrationMode {

    ERP_INTEGRATION, TREASURY_CERTIFICATION;

    public LocalizedString getDescriptionI18N() {
        return TreasuryConstants.treasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }

}
