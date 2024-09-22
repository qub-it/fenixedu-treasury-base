package org.fenixedu.treasury.services.payments.sibspay;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Base64;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.sibspay.MbwayMandate;
import org.fenixedu.treasury.domain.sibspay.SibsPayPlatform;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotification;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotificationResponse;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotificationWrapper;
import org.fenixedu.treasury.services.payments.sibspay.webhook.SibsPayWebhookLogic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

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
