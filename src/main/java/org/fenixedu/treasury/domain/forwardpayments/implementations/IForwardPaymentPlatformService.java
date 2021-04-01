package org.fenixedu.treasury.domain.forwardpayments.implementations;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;

public interface IForwardPaymentPlatformService {

    public IForwardPaymentController getForwardPaymentController(ForwardPaymentRequest request);

    public String getPaymentURL(ForwardPaymentRequest request);

    default public boolean isLogosPageDefined() {
        return !StringUtils.isEmpty(getLogosJspPage());
    }
    
    public String getLogosJspPage();

    public String getWarningBeforeRedirectionJspPage();

    public ForwardPaymentStatusBean paymentStatus(ForwardPaymentRequest request);

    public PostProcessPaymentStatusBean postProcessPayment(ForwardPaymentRequest forwardPayment, String justification,
            Optional<String> specificTransactionId);

    public List<ForwardPaymentStatusBean> verifyPaymentStatus(final ForwardPaymentRequest request);

}