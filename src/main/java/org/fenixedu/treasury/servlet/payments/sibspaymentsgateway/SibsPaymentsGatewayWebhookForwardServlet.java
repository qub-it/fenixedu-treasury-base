package org.fenixedu.treasury.servlet.payments.sibspaymentsgateway;

import com.qubit.terra.framework.services.ServiceProvider;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGatewayEnviromentMode;
import org.fenixedu.treasury.domain.sibspaymentsgateway.integration.SibsPaymentsGateway;
import pt.ist.fenixframework.FenixFramework;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@WebServlet("/treasury/document/payments/onlinepaymentsgateway/notification")
public class SibsPaymentsGatewayWebhookForwardServlet extends HttpServlet {

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        boolean forwardToJaxRsSibsPaymentsGatewayWebhookController = checkForwardToJaxRsSibsPaymentsGatewayWebhookController();

        String target =
                forwardToJaxRsSibsPaymentsGatewayWebhookController ? "/api/sibspaymentsgatewaywebhook" : "/treasury/document/payments/onlinepaymentsgateway/deprecated/notification";

        forwardToWebhookController(req, resp, target);
    }

    private boolean checkForwardToJaxRsSibsPaymentsGatewayWebhookController() {
        Optional<SibsPaymentsGateway> gateway = SibsPaymentsGateway.findAll().filter(g -> g.isActive()).findFirst();

        if (gateway.isPresent()) {
            return gateway.get().getEnviromentMode() == SibsOnlinePaymentsGatewayEnviromentMode.TEST_MODE_INTERNAL;
        }

        return false;
    }

    private void forwardToWebhookController(HttpServletRequest req, HttpServletResponse resp, String target)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = req.getRequestDispatcher(target);

        // optional: keep original query string
        if (req.getQueryString() != null && !req.getQueryString().isEmpty()) {
            target = target + "?" + req.getQueryString();
        }

        dispatcher.forward(req, resp);
    }

}
