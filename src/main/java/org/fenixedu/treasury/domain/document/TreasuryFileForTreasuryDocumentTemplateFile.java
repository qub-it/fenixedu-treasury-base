package org.fenixedu.treasury.domain.document;

import org.fenixedu.bennu.core.domain.User;

public class TreasuryFileForTreasuryDocumentTemplateFile extends TreasuryFileForTreasuryDocumentTemplateFile_Base {
    
    public TreasuryFileForTreasuryDocumentTemplateFile() {
        /* This is a replacement for existings treasury files. It should not be created for new files */

        throw new RuntimeException("error");
    }

    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
}
