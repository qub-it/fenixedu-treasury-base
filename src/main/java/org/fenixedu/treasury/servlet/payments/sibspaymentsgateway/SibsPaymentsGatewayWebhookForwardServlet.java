package org.fenixedu.treasury.servlet.payments.sibspaymentsgateway;

import com.qubit.terra.framework.services.ServiceProvider;
import com.qubit.terra.framework.services.logging.Log;
import com.qubit.terra.framework.services.logging.LogContext;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGatewayEnviromentMode;
import org.fenixedu.treasury.domain.sibspaymentsgateway.integration.SibsPaymentsGateway;
import pt.ist.fenixframework.FenixFramework;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

@WebServlet("/treasury/document/payments/onlinepaymentsgateway/notification")
public class SibsPaymentsGatewayWebhookForwardServlet extends HttpServlet {

    private static LogContext LOG_CONTEXT = LogContext.forContext(SibsPaymentsGatewayWebhookForwardServlet.class.getSimpleName());

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
            return Boolean.TRUE.equals(gateway.get().getForwardWebhooksToNewController());
        }

        return false;
    }

    private void forwardToWebhookController(HttpServletRequest req, HttpServletResponse resp, String target)
            throws ServletException, IOException {
        Log.info(LOG_CONTEXT, "Forwarding SIBS notification to %s".formatted(target));

        RequestDispatcher dispatcher = req.getRequestDispatcher(target);

        // optional: keep original query string
        if (req.getQueryString() != null && !req.getQueryString().isEmpty()) {
            target = target + "?" + req.getQueryString();
        }

        dispatcher.forward(req, resp);
    }

}
