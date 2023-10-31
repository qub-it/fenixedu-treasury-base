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
package org.fenixedu.treasury.services.payments.meowallet;

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.meowallet.MeoWallet;
import org.fenixedu.treasury.domain.meowallet.MeoWalletLog;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.fenixedu.treasury.dto.meowallet.MeoWalletCallbackBean;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Path("/wallet_callback")
public class MeoWalletWebhooksController {
    private static final Logger logger = LoggerFactory.getLogger(MeoWalletWebhooksController.class);

    private static final String NOTIFICATION_URI = "/";

    @POST
    @Path(MeoWalletWebhooksController.NOTIFICATION_URI)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response notification(String body, @Context HttpServletRequest httpRequest, @Context HttpServletResponse response) {
        MeoWalletLog log = createLog();

        FenixFramework.atomic(() -> log.saveRequest(body));

        Gson s = new GsonBuilder()
                .registerTypeAdapter(DateTime.class,
                        (JsonDeserializer<DateTime>) (json, typeOfT, context) -> new DateTime(json.getAsString()))
                .setPrettyPrinting().create();

        MeoWalletCallbackBean bean = s.fromJson(body, MeoWalletCallbackBean.class);

        FenixFramework.atomic(() -> {
            log.logRequestReceiveDateAndData(bean.getOperation_id(), "Notification", bean.getEvent(), bean.getAmount(),
                    bean.getOperation_status(), !MeoWallet.STATUS_FAIL.equals(bean.getOperation_status()));

            log.setExtInvoiceId(bean.getExt_invoiceid());
            log.setMeoWalletId(bean.getOperation_id());
            log.setTransactionWithPayment(MeoWallet.STATUS_COMPLETED.equals(bean.getOperation_status()));
        });

        try {

            Optional<SibsPaymentRequest> referenceCodeOptional = Optional.ofNullable(null);
            Optional<MbwayRequest> mbwayPaymentRequestOptional = Optional.ofNullable(null);
            Optional<ForwardPaymentRequest> forwardPaymentRequesteOptional = Optional.ofNullable(null);

            if (!StringUtils.isEmpty(bean.getExt_invoiceid())) {
                referenceCodeOptional =
                        SibsPaymentRequest.findBySibsGatewayMerchantTransactionId(bean.getExt_invoiceid()).findFirst();
                mbwayPaymentRequestOptional = MbwayRequest.findUniqueBySibsGatewayMerchantTransactionId(bean.getExt_invoiceid());
                forwardPaymentRequesteOptional = ForwardPaymentRequest.findUniqueByMerchantTransactionId(bean.getExt_invoiceid());
            }

            if (referenceCodeOptional.isPresent()) {
                MeoWallet digitalPaymentPlatform = (MeoWallet) referenceCodeOptional.get().getDigitalPaymentPlatform();

                if (!digitalPaymentPlatform.isActive()) {
                    throw new Exception("MeoWallet not active, payment will not be processed");
                }

                if (!digitalPaymentPlatform.getMeoWalletService().verifyCallback(body)) {
                    throw new Exception("callback not verified");
                }

                final SibsPaymentRequest paymentReferenceCode = referenceCodeOptional.get();
                FenixFramework.atomic(() -> {
                    log.setPaymentRequest(paymentReferenceCode);
                });

                digitalPaymentPlatform.processPaymentReferenceCodeTransaction(log, bean);

            } else if (mbwayPaymentRequestOptional.isPresent()) {
                MeoWallet digitalPaymentPlatform = (MeoWallet) mbwayPaymentRequestOptional.get().getDigitalPaymentPlatform();

                if (!digitalPaymentPlatform.isActive()) {
                    throw new Exception("MeoWallet not active, payment will not be processed");
                }

                if (!digitalPaymentPlatform.getMeoWalletService().verifyCallback(body)) {
                    throw new Exception("callback not verified");
                }

                MbwayRequest mbwayRequest = mbwayPaymentRequestOptional.get();
                FenixFramework.atomic(() -> {
                    log.setPaymentRequest(mbwayRequest);
                });

                digitalPaymentPlatform.processMbwayTransaction(log, bean);
            } else if (forwardPaymentRequesteOptional.isPresent()) {
                MeoWallet digitalPaymentPlatform = (MeoWallet) forwardPaymentRequesteOptional.get().getDigitalPaymentPlatform();

                if (!digitalPaymentPlatform.isActive()) {
                    throw new Exception("MeoWallet not active, payment will not be processed");
                }

                if (!digitalPaymentPlatform.getMeoWalletService().verifyCallback(body)) {
                    throw new Exception("callback not verified");
                }

                ForwardPaymentRequest forwardPaymentRequest = forwardPaymentRequesteOptional.get();
                FenixFramework.atomic(() -> {
                    log.setPaymentRequest(forwardPaymentRequest);
                });
                digitalPaymentPlatform.processForwardPaymentFromWebhook(log, bean);
            } else {
                FenixFramework.atomic(() -> {
                    log.logRequestReceiveDateAndData(bean.getTransactionId(), "Notification", bean.getPaymentType(),
                            bean.getAmount(), bean.getPaymentResultCode(),
                            !MeoWallet.STATUS_FAIL.equals(bean.getPaymentResultCode()));
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
    private MeoWalletLog createLog() {
        return MeoWalletLog.createLogForWebhookNotification();
    }

}
