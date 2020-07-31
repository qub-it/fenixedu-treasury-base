package org.fenixedu.treasury.domain.forwardpayments;

import java.util.Comparator;
import java.util.stream.Stream;

import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class ForwardPaymentLog extends ForwardPaymentLog_Base {
    
    public static final Comparator<ForwardPaymentLog> COMPARATOR_BY_ORDER = new Comparator<ForwardPaymentLog>() {

        @Override
        public int compare(final ForwardPaymentLog o1, final ForwardPaymentLog o2) {
            int c = - Integer.compare(o1.getOrderNumber(), o2.getOrderNumber());
            
            return c != 0 ? c : (- o1.getExternalId().compareTo(o2.getExternalId()));
        }
    };
    
    private ForwardPaymentLog() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    ForwardPaymentLog(final ForwardPayment forwardPayment, final ForwardPaymentStateType type, final DateTime whenOccured) {
        this();
        setForwardPayment(forwardPayment);
        setType(type);
        setWhenOccured(whenOccured);
        setOrderNumber(forwardPayment.getForwardPaymentLogsSet().size());
    }
    
    public void delete() {
        setForwardPayment(null);
        setDomainRoot(null);

        if(getRequestLogFile() != null) {
            getRequestLogFile().delete();
        }
        
        if(getResponseLogFile() != null) {
            getResponseLogFile().delete();
        }
        
        deleteDomainObject();
    }

    
    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on
    
    public static Stream<ForwardPaymentLog> findAll() {
        return FenixFramework.getDomainRoot().getForwardPaymentLogsSet().stream();
    }
    
}
