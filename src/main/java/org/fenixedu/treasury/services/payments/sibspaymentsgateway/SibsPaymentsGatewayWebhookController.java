package org.fenixedu.treasury.services.payments.sibspaymentsgateway;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.onlinepaymentsgateway.api.PaymentStateBean;
import org.fenixedu.onlinepaymentsgateway.api.SIBSOnlinePaymentsGatewayService;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.onlinepaymentsgateway.sibs.sdk.PaymentType;
import org.fenixedu.onlinepaymentsgateway.util.Decryption;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.payments.IMbwayPaymentPlatformService;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.fenixedu.treasury.domain.sibspaymentsgateway.SibsPaymentsGatewayLog;
import org.fenixedu.treasury.domain.sibspaymentsgateway.integration.SibsPaymentsGateway;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Optional;

@Path("/sibspaymentsgatewaywebhook")
public class SibsPaymentsGatewayWebhookController {

    private static final String RISK_MANAGEMENT_TIMEOUT = "100.380.501";

    private static final String NOTIFICATION_URI = "/";

    private static final Logger logger = LoggerFactory.getLogger(SibsPaymentsGatewayWebhookController.class);

    @POST
    @Path(NOTIFICATION_URI)
    @Consumes(MediaType.TEXT_PLAIN)
    public Response notification(String notificationEncryptedPayload, @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse response) {

        final SibsPaymentsGatewayLog log = createLog();

        boolean mockedUser = false;
        try {

            String notificationInitializationVector =
                    SIBSOnlinePaymentsGatewayService.notificationInitializationVector(httpRequest);
            String notificationAuthenticationTag = SIBSOnlinePaymentsGatewayService.notificationAuthenticationTag(httpRequest);

            FenixFramework.atomic(
                    () -> log.saveWebhookNotificationData(notificationInitializationVector, notificationAuthenticationTag,
                            notificationEncryptedPayload));

            if (isTestPayloadForWebhookActivation(notificationInitializationVector, notificationAuthenticationTag,
                    notificationEncryptedPayload)) {
                return Response.ok().build();
            }

            // TODO: The gateway should be chosen by the one that allows the decription of encryped payload

            SibsPaymentsGateway gateway = SibsPaymentsGateway.findUniqueActive(FinantialInstitution.findAll().iterator().next())
                    .orElseThrow(() -> new TreasuryDomainException(
                            "error.OnlinePaymentsGatewayWebhooksController.gateway.is.not.active"));

            if (!gateway.isActive()) {
                throw new TreasuryDomainException("error.OnlinePaymentsGatewayWebhooksController.gateway.is.not.active");
            }

            PaymentStateBean bean =
                    gateway.handleWebhookNotificationRequest(notificationInitializationVector, notificationAuthenticationTag,
                            notificationEncryptedPayload);

            FenixFramework.atomic(() -> {
                log.logRequestReceiveDateAndData(bean.getTransactionId(), bean.isOperationSuccess(), bean.isPaid(),
                        bean.getPaymentGatewayResultCode(), bean.getPaymentGatewayResultDescription());
                log.saveRequest(bean.getRequestLog());
            });

            if (!"PAYMENT".equals(bean.getNotificationType())) {
                // Not payment, ignore
                return Response.ok().build();
            }

            FenixFramework.atomic(() -> {
                log.saveMerchantTransactionId(bean.getMerchantTransactionId());
                log.saveTransactionId(bean.getTransactionId());
                log.saveReferenceId(bean.getReferencedId());
            });

            if (PaymentType.PA.name().equals(bean.getPaymentType()) && "SIBS_MULTIBANCO".equals(bean.getPaymentBrand())) {
                // Sibs reference code request
                return Response.ok().build();
            }

            // Find payment code
            final Optional<SibsPaymentRequest> referenceCodeOptional =
                    bean.getReferencedId() != null ? SibsPaymentRequest.findUniqueBySibsGatewayTransactionId(
                            bean.getReferencedId()) : Optional.empty();

            final Optional<MbwayRequest> mbwayPaymentRequestOptional =
                    MbwayRequest.findUniqueBySibsGatewayMerchantTransactionId(bean.getMerchantTransactionId());

            Optional<ForwardPaymentRequest> forwardPaymentRequestOptional =
                    ForwardPaymentRequest.findUniqueByMerchantTransactionId(bean.getMerchantTransactionId());

            if (referenceCodeOptional.isPresent()) {
                if (!bean.isOperationSuccess()) {
                    // TODO Review what to do, for now just return
                    // Operation with insucess, return ok just to not retry the transaction
                    return Response.ok().build();
                }

                if (!PaymentType.RC.name().equals(bean.getPaymentType())) {
                    throw new TreasuryDomainException(
                            "error.OnlinePaymentsGatewayWebhooksController.unrecognized.payment.type.for.payment.reference.code");
                }

                if (!bean.isPaid()) {
                    throw new TreasuryDomainException(
                            "error.OnlinePaymentsGatewayWebhooksController.notificationBean.not.paid.check");
                }

                mockedUser = mockUserIfNeeded(referenceCodeOptional.get().getDigitalPaymentPlatform());

                final ISibsPaymentCodePoolService paymentReferenceCodeService =
                        referenceCodeOptional.get().getDigitalPaymentPlatform().castToSibsPaymentCodePoolService();

                final SibsPaymentRequest paymentReferenceCode = referenceCodeOptional.get();
                FenixFramework.atomic(() -> log.setPaymentRequest(paymentReferenceCode));

                paymentReferenceCodeService.processPaymentReferenceCodeTransaction(log, bean);
            } else if (mbwayPaymentRequestOptional.isPresent()) {
                if (!bean.isOperationSuccess()) {
                    // TODO Review what to do, for now just return
                    // Operation with insucess, return ok just to not retry the transaction
                    return Response.ok().build();
                }

                if (!PaymentType.DB.name().equals(bean.getPaymentType())) {
                    throw new TreasuryDomainException(
                            "error.OnlinePaymentsGatewayWebhooksController.unrecognized.payment.type.for.mbway.payment.request");
                }

                mockedUser = mockUserIfNeeded(mbwayPaymentRequestOptional.get().getDigitalPaymentPlatform());

                MbwayRequest mbwayRequest = mbwayPaymentRequestOptional.get();
                FenixFramework.atomic(() -> log.setPaymentRequest(mbwayRequest));

                if (bean.isPaid()) {
                    final IMbwayPaymentPlatformService mbwayPaymentRequest =
                            mbwayPaymentRequestOptional.get().getDigitalPaymentPlatform().castToMbwayPaymentPlatformService();

                    mbwayPaymentRequest.processMbwayTransaction(log, bean);
                }

                return Response.ok().build();
            } else if (forwardPaymentRequestOptional.isPresent()) {
                IForwardPaymentPlatformService digitalPaymentPlatform =
                        (IForwardPaymentPlatformService) forwardPaymentRequestOptional.get().getDigitalPaymentPlatform();

                mockedUser = mockUserIfNeeded(forwardPaymentRequestOptional.get().getDigitalPaymentPlatform());

                ForwardPaymentRequest forwardPaymentRequest = forwardPaymentRequestOptional.get();
                FenixFramework.atomic(() -> log.setPaymentRequest(forwardPaymentRequest));

                if (bean.getResult() != null && RISK_MANAGEMENT_TIMEOUT.equals(bean.getResult().getCode())) {
                    /* The "risk management transaction timeout" is causing forward payment requests to
                     * become rejected, and consequently not accepting payment. For now ignore these notifications
                     */
                    return Response.ok().build();
                }

                digitalPaymentPlatform.processForwardPaymentFromWebhook(log, bean);

                return Response.ok().build();
            } else {
                throw new TreasuryDomainException(
                        "error.OnlinePaymentsGatewayWebhooksController.notificationBean.paymentReferenceCode.not.found.by.referenceId");
            }

            return Response.ok().build();
        } catch (Exception e) {
            FenixFramework.atomic(() -> log.logException(e));

            if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                final OnlinePaymentsGatewayCommunicationException oe = (OnlinePaymentsGatewayCommunicationException) e;
                FenixFramework.atomic(() -> {
                    log.saveRequest(oe.getRequestLog());
                    log.saveResponse(oe.getResponseLog());
                });
            }

            logger.error(e.getLocalizedMessage(), e);

            throw new RuntimeException(e);
        } finally {
            if (mockedUser) {
                Authenticate.unmock();

                logger.debug("Unmocked user");
            }
        }
    }

    private boolean isTestPayloadForWebhookActivation(String notificationInitializationVector,
            String notificationAuthenticationTag, String notificationEncryptedPayload) {
        try {

            // TODO: The gateway should be chosen by the one that allows the decription of encryped payload
            SibsPaymentsGateway gateway =
                    SibsPaymentsGateway.findUniqueActive(FinantialInstitution.findAll().iterator().next()).get();

            String aesKey = gateway.getAesKey();

            Decryption notification = new Decryption(aesKey, notificationInitializationVector, notificationAuthenticationTag,
                    notificationEncryptedPayload);

            String decryptedPayload = notification.decryptPayload();

            ObjectMapper mapper = new ObjectMapper();

            Map<String, String> map = mapper.readValue(decryptedPayload, new TypeReference<Map<String, String>>() {
            });

            if (map.containsKey("type") && map.containsKey("action")) {
                String typeValue = map.get("type");
                String actionValue = map.get("action");

                return "test".equals(typeValue) && "webhook activation".equals(actionValue);
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean mockUserIfNeeded(DigitalPaymentPlatform digitalPaymentPlatform) {
        boolean needToMockUser = StringUtils.isEmpty(TreasuryConstants.getAuthenticatedUsername()) && StringUtils.isNotEmpty(
                digitalPaymentPlatform.getApplicationUsernameForAutomaticOperations());

        if (needToMockUser) {
            mockApplicationUser(digitalPaymentPlatform.getApplicationUsernameForAutomaticOperations());
            logger.debug("Mocked user with " + digitalPaymentPlatform.getApplicationUsernameForAutomaticOperations());

            return true;
        }

        return false;
    }

    private void mockApplicationUser(String username) {
        User user = User.findByUsername(username);

        if (user == null) {
            throw new IllegalArgumentException("user not found: " + username);
        }

        Authenticate.mock(user, "TODO: CHANGE ME");
    }

    @Atomic(mode = Atomic.TxMode.WRITE)
    private SibsPaymentsGatewayLog createLog() {
        return SibsPaymentsGatewayLog.createLogForWebhookNotification();
    }
}
