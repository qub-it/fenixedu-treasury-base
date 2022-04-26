package org.fenixedu.treasury.domain.treasurydebtprocess;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.util.TreasuryConstants;

public enum TreasuryDebtProcessState {

    PREPARING, OPEN, CLOSED, ANNULLED;
    
    public LocalizedString getDescriptionI18N() {
        return TreasuryConstants.treasuryBundleI18N(getClass().getName() + "." + name());
    }

    public boolean isActive() {
        return this == OPEN;
    }
}
