package org.fenixedu.treasury.domain.paymentcodes;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForSibsOutputFile extends TreasuryFileForSibsOutputFile_Base {
    
    public TreasuryFileForSibsOutputFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
