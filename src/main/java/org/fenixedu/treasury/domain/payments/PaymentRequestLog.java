package org.fenixedu.treasury.domain.payments;

import java.io.UnsupportedEncodingException;
import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class PaymentRequestLog extends PaymentRequestLog_Base {

    public static final Comparator<PaymentRequestLog> COMPARE_BY_CREATION_DATE = 
            (o1, o2) -> o1.getCreationDate().compareTo(o2.getCreationDate()) * 10 + o1.getExternalId().compareTo(o2.getExternalId());
    
    public PaymentRequestLog() {
        super();

        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
        setResponsibleUsername(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());
    }

    protected PaymentRequestLog(PaymentRequest request, String stateCode, LocalizedString stateDescription) {
        this();

        setPaymentRequest(request);
        setStateCode(stateCode);
        setStateDescription(stateDescription);

        checkRules();
    }

    private void checkRules() {
    }

    public void saveRequest(String requestBody) {
        if(requestBody == null) {
            return;
        }
        
        try {
            String filename = String.format("request_%s_%s.txt", new DateTime().toString("yyyyMMddHHmmss"), getExternalId());
            setRequestLogFile(PaymentRequestLogFile.create(filename, requestBody.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveResponse(String responseBody) {
        if(responseBody == null) {
            return;
        }
        
        try {
            String filename = String.format("response_%s_%s.txt", new DateTime().toString("yyyyMMddHHmmss"), getExternalId());
            setResponseLogFile(PaymentRequestLogFile.create(filename, responseBody.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void logException(Exception e) {
        String exceptionLog = String.format("%s\n%s", e.getLocalizedMessage(), ExceptionUtils.getFullStackTrace(e));
        setExceptionOccured(true);

        try {
            String filename = String.format("exception_%s_%s.txt", new DateTime().toString("yyyyMMddHHmmss"), getExternalId());
            setExceptionLogFile(PaymentRequestLogFile.create(filename, exceptionLog.getBytes("UTF-8")));
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void editAuthorizationData(String authorizationId, DateTime authorizationDate) {
        setAuthorizationId(authorizationId);
        setAuthorizationDate(authorizationDate);
    }

    // @formatter:off
    /*
     * 
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<? extends PaymentRequestLog> findAll() {
        return FenixFramework.getDomainRoot().getPaymentRequestLogsSet().stream();
    }
    
    public static PaymentRequestLog create(PaymentRequest request, String stateCode, LocalizedString stateDescription) {
        return new PaymentRequestLog(request, stateCode, stateDescription);
    }

}
