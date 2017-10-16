package org.fenixedu.treasury.domain.forwardpayments;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

public class ForwardPaymentLogFile extends ForwardPaymentLogFile_Base implements IGenericFile {

    private ForwardPaymentLogFile() {
        super();
    }

    private ForwardPaymentLogFile(final String fileName, final byte[] content) {
        this();
        
        TreasuryPlataformDependentServicesFactory.implementation().createFile(this, fileName, fileName, content);
    }

    @Override
    public boolean isAccessible(final String username) {
        throw new RuntimeException("not implemented");
    }
    
    public String getContentAsString() {
        if(getContent() != null) {
            return new String(getContent());
        }
        
        return null;
    }

    @Override
    public void delete() {
        TreasuryPlataformDependentServicesFactory.implementation().deleteFile(this);
        
        deleteDomainObject();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static ForwardPaymentLogFile createForRequestBody(final ForwardPaymentLog log, final byte[] content) {
        final ForwardPaymentLogFile logFile = new ForwardPaymentLogFile(
                String.format("requestBody_%s_%s.txt", new DateTime().toString("yyyyMMddHHmmss"), log.getExternalId()), content);
        logFile.setForwardPaymentLogsForRequest(log);

        return logFile;
    }

    public static ForwardPaymentLogFile createForResponseBody(final ForwardPaymentLog log, final byte[] content) {
        final ForwardPaymentLogFile logFile = new ForwardPaymentLogFile(
                String.format("responseBody_%s_%s.txt", new DateTime().toString("yyyyMMddHHmmss"), log.getExternalId()), content);
        logFile.setForwardPaymentLogsForResponse(log);

        return logFile;
    }

}
