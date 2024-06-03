package org.fenixedu.treasury.domain.forwardpayments;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForPostForwardPaymentsReportFile extends TreasuryFileForPostForwardPaymentsReportFile_Base {
    
    public TreasuryFileForPostForwardPaymentsReportFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
