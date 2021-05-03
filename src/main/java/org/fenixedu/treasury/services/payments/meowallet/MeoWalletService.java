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

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.MediaType;

import org.fenixedu.onlinepaymentsgateway.api.SIBSOnlinePaymentsGatewayService;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.dto.meowallet.MeoWalletCallbackBean;
import org.fenixedu.treasury.dto.meowallet.MeoWalletCheckoutBean;
import org.fenixedu.treasury.dto.meowallet.MeoWalletPaymentBean;
import org.fenixedu.treasury.dto.meowallet.MeoWalletPaymentItemBean;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.joda.time.DateTime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.qubit.terra.framework.tools.gson.serializers.DateTimeSerializer;

public class MeoWalletService {

    private static final String PAYMENT_PATH = "payment";
    private static final String MB_PAY_PATH = "mb/pay";
    private static final String WS_ACCESS_TOKEN_HEADER = "Authorization";
    private static final String WS_ACCESS_TOKEN_VALUE_PREFIX = "WalletPT ";
    private static final String CHECKOUT_PATH = "checkout";
    private static final String OPERATION_PATH = "operations";

    private Feature feature;
    private Client client;
    private WebTarget webTargetBase;
    private String token;

    public MeoWalletService(String endpointUrl, String token) {
        this.feature = new LoggingFeature(java.util.logging.Logger.getLogger(SIBSOnlinePaymentsGatewayService.class.getName()),
                Level.FINEST, (Verbosity) null, (Integer) null);
        this.client = ClientBuilder.newBuilder().register(this.feature).build();
        if (endpointUrl != null) {
            this.webTargetBase = this.client.target(endpointUrl);
        }
        this.token = token;
    }

    public void closeClient() {

        this.client.close();
    }

    public MeoWalletPaymentBean generateMbwayReference(MeoWalletPaymentBean payment)
            throws OnlinePaymentsGatewayCommunicationException {

        String requestLog = getGson().toJson(payment);
        String responseLog = processPost(PAYMENT_PATH, requestLog);
        try {
            MeoWalletPaymentBean result = getGson().fromJson(responseLog, MeoWalletPaymentBean.class);
            if (result.getId() == null) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog);
            }
            result.setRequestLog(requestLog);
            result.setResponseLog(responseLog);
            return result;

        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        }
    }

    public MeoWalletPaymentBean generateMBPaymentReference(MeoWalletPaymentBean payment)
            throws OnlinePaymentsGatewayCommunicationException, IOException {

        String requestLog = getGson().toJson(payment);
        String responseLog = processPost(MB_PAY_PATH, requestLog);

        try {
            MeoWalletPaymentBean result = getGson().fromJson(responseLog, MeoWalletPaymentBean.class);
            result.setRequestLog(requestLog);
            result.setResponseLog(responseLog);
            return result;
        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        }
    }

    public MeoWalletCheckoutBean prepareOnlinePaymentCheckout(MeoWalletCheckoutBean checkoutBean)
            throws OnlinePaymentsGatewayCommunicationException {

        String requestLog = getGson().toJson(checkoutBean);
        String responseLog = processPost(CHECKOUT_PATH, requestLog);
        System.out.println(responseLog);
        try {
            MeoWalletCheckoutBean result = getGson().fromJson(responseLog, MeoWalletCheckoutBean.class);
            result.setRequestLog(requestLog);
            result.setResponseLog(responseLog);
            return result;
        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        }
    }

    public MeoWalletCheckoutBean getForwardPaymentTransactionReportByTransactionId(String id)
            throws OnlinePaymentsGatewayCommunicationException {
        String path = CHECKOUT_PATH + "/" + id;
        String requestLog = id;
        String responseLog = processGetService(path);

        try {
            MeoWalletCheckoutBean result = getGson().fromJson(responseLog, MeoWalletCheckoutBean.class);
            result.setRequestLog(requestLog);
            result.setResponseLog(responseLog);
            return result;

        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        }
    }

    public MeoWalletPaymentBean getCallbackReportByTransactionId(String id) throws OnlinePaymentsGatewayCommunicationException {
        String path = OPERATION_PATH + "/" + id;
        String requestLog = id;
        String responseLog = processGetService(path);

        try {
            MeoWalletPaymentBean result = getGson().fromJson(responseLog, MeoWalletPaymentBean.class);
            result.setRequestLog(requestLog);
            result.setResponseLog(responseLog);
            return result;

        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        }
    }

    public void verifyCallback(MeoWalletCallbackBean bean) {
        String path = "callback/verify";
        String requestLog = getGson().toJson(bean);
        processPost(path, requestLog);
    }

    public List<MeoWalletPaymentBean> getPaymentTransactionReportByMerchantTransactionId(String merchantTransationId)
            throws OnlinePaymentsGatewayCommunicationException {
        String requestLog = merchantTransationId;
        String path = OPERATION_PATH;

        String responseLog = processGetService(path, new String[] { "ext_invoiceid", merchantTransationId });
        try {
            List<MeoWalletPaymentBean> list = new ArrayList<>();
            Gson gson = getGson();
            JsonArray jsonArray = gson.fromJson(responseLog, JsonObject.class).getAsJsonArray("elements");
            for (JsonElement jsonElement : jsonArray) {
                MeoWalletPaymentBean result = gson.fromJson(jsonElement.getAsJsonObject(), MeoWalletPaymentBean.class);
                result.setRequestLog(requestLog);
                result.setResponseLog(responseLog);
                list.add(result);
            }
            return list;

        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        }
    }

    private Gson getGson() {
        return new GsonBuilder().registerTypeAdapter(DateTime.class, new DateTimeSerializer()).setPrettyPrinting().create();
    }

    private String processGetService(String path, String... queryParams) {
        WebTarget target = this.webTargetBase.path(path);
        for (int i = 0; i < queryParams.length; i += 2) {
            target = target.queryParam(queryParams[i], queryParams[i + 1]);
        }
        Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

        String responseLog = builder.header(WS_ACCESS_TOKEN_HEADER, WS_ACCESS_TOKEN_VALUE_PREFIX + token).get(String.class);

        return responseLog;
    }

    private String processPost(String path, String requestLog) {
        WebTarget target = this.webTargetBase.path(path);

        Builder builder = target.request(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON);

        String responseLog = builder.header(WS_ACCESS_TOKEN_HEADER, WS_ACCESS_TOKEN_VALUE_PREFIX + token)
                .post(Entity.json(requestLog)).readEntity(String.class);

        return responseLog;
    }

}
