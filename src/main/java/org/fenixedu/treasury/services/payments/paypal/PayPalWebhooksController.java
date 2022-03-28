/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.services.payments.paypal;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.paypal.PayPal;
import org.fenixedu.treasury.domain.paypal.PayPalLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Path("/paypal_callback")
public class PayPalWebhooksController {
    private static final Logger logger = LoggerFactory.getLogger(PayPalWebhooksController.class);

    private static final String NOTIFICATION_URI = "/";

    @POST
    @Path(PayPalWebhooksController.NOTIFICATION_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response notification(String body, @Context HttpServletRequest httpRequest, @Context HttpServletResponse response) {
        PayPalLog log = createLog();

        FenixFramework.atomic(() -> {
            log.saveRequest(body);
        });

        PayPalWebhookBean bean = new PayPalWebhookBean(body);

        FenixFramework.atomic(() -> {
            log.logRequestReceiveDateAndData(bean.getTransactionId(), "Notification", bean.getEvent_type(), bean.getAmount(),
                    bean.getResource_type(), bean.isOperationSuccess());
            log.setTransactionWithPayment(bean.isPaid());
        });

        try {


            Optional<ForwardPaymentRequest> forwardPaymentRequesteOptional = Optional.ofNullable(null);
            if (bean.getOrder() != null) {
                forwardPaymentRequesteOptional =
                        (Optional<ForwardPaymentRequest>) ForwardPaymentRequest
                                .findUniqueBySibsGatewayTransactionId(bean.getOrder().id());
            }

            if (forwardPaymentRequesteOptional.isPresent()) {
                ForwardPaymentRequest forwardPaymentRequest = forwardPaymentRequesteOptional.get();
                PayPal digitalPaymentPlatform = (PayPal) forwardPaymentRequest.getDigitalPaymentPlatform();

                FenixFramework.atomic(() -> {
                    log.setPaymentRequest(forwardPaymentRequest);
                });
                digitalPaymentPlatform.processForwardPaymentFromWebhook(log, bean);
            } else {

                FenixFramework.atomic(() -> {
                    log.logRequestReceiveDateAndData(bean.getTransactionId(), "Notification", "forwardPayment", bean.getAmount(),
                            bean.getOrder().status(), bean.isOperationSuccess());
                });
            }

            return Response.ok().build();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);

            if (log != null) {
                FenixFramework.atomic(() -> {
                    log.logException(e);
                });

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    final OnlinePaymentsGatewayCommunicationException oe = (OnlinePaymentsGatewayCommunicationException) e;
                    FenixFramework.atomic(() -> {
                        log.saveRequest(oe.getRequestLog());
                        log.saveResponse(oe.getResponseLog());
                    });
                }
            }

            return Response.serverError().build();
        }

    }

    @Atomic(mode = TxMode.WRITE)
    private PayPalLog createLog() {
        return PayPalLog.createLogForWebhookNotification();
    }

}
