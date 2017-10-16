package org.fenixedu.treasury.domain.forwardpayments.implementations;

import org.fenixedu.treasury.domain.forwardpayments.ForwardPayment;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;

public interface IForwardPaymentImplementation {

    public String getPaymentURL(final ForwardPayment forwardPayment);

    public String getFormattedAmount(final ForwardPayment forwardPayment);
    
    public String getLogosJspPage();
    
    public String getWarningBeforeRedirectionJspPage();

    public ForwardPaymentStatusBean paymentStatus(final ForwardPayment forwardPayment);

    public PostProcessPaymentStatusBean postProcessPayment(final ForwardPayment forwardPayment, final String justification);
    
}
