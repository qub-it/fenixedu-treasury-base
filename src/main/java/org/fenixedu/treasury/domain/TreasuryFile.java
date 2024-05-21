package org.fenixedu.treasury.domain;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.io.domain.IGenericFile;

public class TreasuryFile extends TreasuryFile_Base {

    public TreasuryFile() {
        super();
    }
    
    public TreasuryFile(final String fileName, final String contentType, final byte[] content) {
        this();
        
        this.init(fileName, fileName, content);
        this.setContentType(contentType);
    }
    
    @Override
    public boolean isAccessible(User arg0) {
        return false;
    }
    
    @Override
    public void delete() {
        
        super.delete();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on
    
    public static final TreasuryFile create(final String fileName, final String contentType, final byte[] content) {
        return new TreasuryFile(fileName, contentType, content);
    }

}
