package org.fenixedu.treasury.domain.forwardpayments;

import java.util.stream.Stream;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;;

public class PostForwardPaymentsReportFile extends PostForwardPaymentsReportFile_Base implements IGenericFile {
    
    private PostForwardPaymentsReportFile(final DateTime postForwardPaymentsExecutionDate, 
            final DateTime beginDate, final DateTime endDate,
            final String filename, final byte[] content) {
        super();

        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
        setPostForwardPaymentsExecutionDate(postForwardPaymentsExecutionDate);
        setBeginDate(beginDate);
        setEndDate(endDate);
        
        TreasuryPlataformDependentServicesFactory.implementation().createFile(this, filename, filename, content);
    }

    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControlAPI.isBackOfficeMember(username);
    }

    @Override
    public void delete() {
        setDomainRoot(null);
        
    	TreasuryPlataformDependentServicesFactory.implementation().deleteFile(this);
        
        deleteDomainObject();
    }

    
    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on
    
    public static Stream<PostForwardPaymentsReportFile> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getPostForwardPaymentsReportFilesSet().stream();
    }

    @Atomic
    public static PostForwardPaymentsReportFile create(final DateTime postForwardPaymentsExecutionDate, final DateTime beginDate, final DateTime endDate, 
            final String filename, final byte[] content) {
        return new PostForwardPaymentsReportFile(postForwardPaymentsExecutionDate, beginDate, endDate, filename, content);
    }

}
