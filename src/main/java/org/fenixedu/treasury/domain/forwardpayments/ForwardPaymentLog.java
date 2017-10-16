package org.fenixedu.treasury.domain.forwardpayments;

import java.util.Comparator;

import org.fenixedu.bennu.core.domain.Bennu;
import org.joda.time.DateTime;

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
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }
    
    ForwardPaymentLog(final ForwardPayment forwardPayment, final ForwardPaymentStateType type, final DateTime whenOccured) {
        this();
        setForwardPayment(forwardPayment);
        setType(type);
        setWhenOccured(whenOccured);
        setOrderNumber(forwardPayment.getForwardPaymentLogsSet().size());
    }
    
}
