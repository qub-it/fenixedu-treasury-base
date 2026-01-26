package org.fenixedu.treasury.services.payments.sibspay;

import org.fenixedu.treasury.services.payments.sibspay.webhook.SibsPayWebhookLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
