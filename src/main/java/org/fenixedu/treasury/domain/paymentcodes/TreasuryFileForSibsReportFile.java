package org.fenixedu.treasury.domain.paymentcodes;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForSibsReportFile extends TreasuryFileForSibsReportFile_Base {
    
    public TreasuryFileForSibsReportFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
