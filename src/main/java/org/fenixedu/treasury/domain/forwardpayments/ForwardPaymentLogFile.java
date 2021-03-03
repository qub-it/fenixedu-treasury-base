package org.fenixedu.treasury.domain.forwardpayments;

import java.util.stream.Stream;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class ForwardPaymentLogFile extends ForwardPaymentLogFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "text/plain";

    public ForwardPaymentLogFile() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    private ForwardPaymentLogFile(final String fileName, final byte[] content) {
        this();

        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        services.createFile(this, fileName, CONTENT_TYPE, content);
    }

    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControlAPI.isBackOfficeMember(username);
    }

    public String getContentAsString() {
        if (getContent() != null) {
            return new String(getContent());
        }

        return null;
    }

    @Override
    public void delete() {
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        this.setDomainRoot(null);
        this.setForwardPaymentLogsForRequest(null);
        this.setForwardPaymentLogsForResponse(null);

        services.deleteFile(this);

        super.deleteDomainObject();
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

    public static ForwardPaymentLogFile createForException(final ForwardPaymentLog log, final byte[] content) {
        final ForwardPaymentLogFile logFile = new ForwardPaymentLogFile(
                String.format("exceptionLog_%s_%s.txt", new DateTime().toString("yyyyMMddHHmmss"), log.getExternalId()), content);
        logFile.setForwardPaymentLogsForException(log);

        return logFile;
    }

    public static Stream<ForwardPaymentLogFile> findAll() {
        return FenixFramework.getDomainRoot().getForwardPaymentLogFilesSet().stream();
    }

}
