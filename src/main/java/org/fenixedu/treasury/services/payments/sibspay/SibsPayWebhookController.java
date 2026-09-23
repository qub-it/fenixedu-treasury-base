package org.fenixedu.treasury.services.payments.sibspay;

import org.fenixedu.treasury.services.payments.sibspay.webhook.SibsPayWebhookLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/sibspaywebhook")
public class SibsPayWebhookController {

    private static final String PROVIDER_NAME = "BC";

    private static final Logger logger = LoggerFactory.getLogger(SibsPayWebhookController.class);

    private static final String NOTIFICATION_URI = "/";

    @POST
    @Path(NOTIFICATION_URI)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response notification(String encryptedBody, @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse response) {

        String iv = httpRequest.getHeader("X-Initialization-Vector");
        String authTag = httpRequest.getHeader("X-Authentication-Tag");

        return new SibsPayWebhookLogic(encryptedBody, iv, authTag).runWebhook();
    }

}
