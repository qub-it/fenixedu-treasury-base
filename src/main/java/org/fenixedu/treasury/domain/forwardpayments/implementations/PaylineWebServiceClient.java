package org.fenixedu.treasury.domain.forwardpayments.implementations;

import javax.xml.ws.BindingProvider;

import org.fenixedu.treasury.services.integration.forwardpayments.payline.WebPaymentAPI;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.WebPaymentAPI_Service;

import com.qubit.solution.fenixedu.bennu.webservices.services.client.BennuWebServiceClient;

public class PaylineWebServiceClient extends BennuWebServiceClient<WebPaymentAPI> {

    @Override
    protected BindingProvider getService() {
        BindingProvider bindingProvider = (BindingProvider) new WebPaymentAPI_Service().getWebPaymentAPI();
        return bindingProvider;
    }

}
