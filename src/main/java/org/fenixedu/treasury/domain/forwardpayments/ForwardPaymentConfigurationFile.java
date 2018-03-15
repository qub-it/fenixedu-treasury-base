package org.fenixedu.treasury.domain.forwardpayments;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.domain.accesscontrol.TreasuryAccessControl;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

public class ForwardPaymentConfigurationFile extends ForwardPaymentConfigurationFile_Base implements IGenericFile {
    
    public static final String CONTENT_TYPE = "application/octet-stream";
    
    protected ForwardPaymentConfigurationFile() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }
    
    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControl.getInstance().isManager(username);
    }

    public static ForwardPaymentConfigurationFile create(final String filename, final byte[] contents) {
        final ForwardPaymentConfigurationFile file = new ForwardPaymentConfigurationFile();
        
        TreasuryPlataformDependentServicesFactory.implementation().createFile(file, filename, CONTENT_TYPE, contents);
        
        return file;
    }
    
    @Override
    public void delete() {
        setDomainRoot(null);
        
        TreasuryPlataformDependentServicesFactory.implementation().deleteFile(this);
        
        deleteDomainObject();
    }
}
