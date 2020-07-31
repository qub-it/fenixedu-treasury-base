package org.fenixedu.treasury.domain.payments;

import java.io.InputStream;

import org.apache.commons.beanutils.PropertyUtils;
import org.fenixedu.bennu.io.domain.GenericFile;
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

    @Override
    public byte[] getContent() {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(this, "treasuryFile");

            if(file != null) {
                return TreasuryPlataformDependentServicesFactory.implementation().getFileContent(this);
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileContent(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getSize() {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(this, "treasuryFile");

            if(file != null) {
                return TreasuryPlataformDependentServicesFactory.implementation().getFileSize(this);
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileSize(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DateTime getCreationDate() {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(this, "treasuryFile");

            if(file != null) {
                return TreasuryPlataformDependentServicesFactory.implementation().getFileCreationDate(this);
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileCreationDate(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFilename() {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(this, "treasuryFile");

            if(file != null) {
                return TreasuryPlataformDependentServicesFactory.implementation().getFilename(this);
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFilename(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getStream() {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(this, "treasuryFile");

            if(file != null) {
                return TreasuryPlataformDependentServicesFactory.implementation().getFileStream(this);
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileStream(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getContentType() {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(this, "treasuryFile");

            if(file != null) {
                return TreasuryPlataformDependentServicesFactory.implementation().getFileContentType(this);
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileContentType(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
