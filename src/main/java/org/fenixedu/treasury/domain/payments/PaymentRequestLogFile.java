package org.fenixedu.treasury.domain.payments;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class PaymentRequestLogFile extends PaymentRequestLogFile_Base implements IGenericFile {
    
    public static final String CONTENT_TYPE = "application/octet-stream";

    public PaymentRequestLogFile() {
        super();

        this.setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }
    
    protected PaymentRequestLogFile(String filename, byte[] content) {
        this();
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        services.createFile(this, filename, CONTENT_TYPE, content);
    }

    @Override
    public boolean isAccessible(String username) {
        return false;
    }

    public String getContentAsString() {
        if (getContent() != null) {
            return new String(getContent());
        }

        return null;
    }

    @Override
    public void delete() {
        setDomainRoot(null);
    }

    // @formatter:off
    /*
     * 
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on
    
    public static PaymentRequestLogFile create(String filename, byte[] content) {
        return new PaymentRequestLogFile(filename, content);
    }
}
