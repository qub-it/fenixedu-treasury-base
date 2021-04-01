package org.fenixedu.treasury.domain.payments;

import java.io.InputStream;

import org.apache.commons.lang.StringUtils;
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
        if(StringUtils.isNotEmpty(getFileId())) {
            return TreasuryPlataformDependentServicesFactory.implementation().getFileContent(getFileId());
        }
        
        return TreasuryPlataformDependentServicesFactory.implementation().getFileContent(this);
    }

    @Override
    public long getSize() {
        if(StringUtils.isNotEmpty(getFileId())) {
            return TreasuryPlataformDependentServicesFactory.implementation().getFileSize(getFileId());
        }
        
        return TreasuryPlataformDependentServicesFactory.implementation().getFileSize(this);
    }

    @Override
    public DateTime getCreationDate() {
        if(StringUtils.isNotEmpty(getFileId())) {
            return TreasuryPlataformDependentServicesFactory.implementation().getFileCreationDate(getFileId());
        }
        
        return TreasuryPlataformDependentServicesFactory.implementation().getFileCreationDate(this);
    }

    @Override
    public String getFilename() {
        if(StringUtils.isNotEmpty(getFileId())) {
            return TreasuryPlataformDependentServicesFactory.implementation().getFilename(getFileId());
        }
        
        return TreasuryPlataformDependentServicesFactory.implementation().getFilename(this);
    }

    @Override
    public InputStream getStream() {
        if(StringUtils.isNotEmpty(getFileId())) {
            return TreasuryPlataformDependentServicesFactory.implementation().getFileStream(getFileId());
        }
        
        return TreasuryPlataformDependentServicesFactory.implementation().getFileStream(this);
    }

    @Override
    public String getContentType() {
        if(StringUtils.isNotEmpty(getFileId())) {
            return TreasuryPlataformDependentServicesFactory.implementation().getFileContentType(getFileId());
        }
        
        return TreasuryPlataformDependentServicesFactory.implementation().getFileContentType(this);
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
