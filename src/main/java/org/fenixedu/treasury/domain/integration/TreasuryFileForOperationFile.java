package org.fenixedu.treasury.domain.integration;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForOperationFile extends TreasuryFileForOperationFile_Base {
    
    public TreasuryFileForOperationFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
