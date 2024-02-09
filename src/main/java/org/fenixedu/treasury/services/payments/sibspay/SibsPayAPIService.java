package org.fenixedu.treasury.services.payments.sibspay;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsBillingAddressBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayAddress;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayAmount;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayCancellationRequest;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayCancellationResponse;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayCustomer;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayCustomerInfo;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayMerchant;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayOriginalTransaction;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayPaymentReference;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayRequestCheckout;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayResponseInquiry;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayResponseInquiryWrapper;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayReturnCheckout;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayTransaction;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotification;
import org.glassfish.jersey.logging.LoggingFeature;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.google.common.base.Splitter;

public class SibsPayAPIService {

    private static final String STATUS_CODE_SUCCESS = "000";

    private static final String PAYMENT_RESULT_CODE_SUCCESS = "Success";
    private static final String PAYMENT_RESULT_CODE_PENDING = "Pending";
    private static final String PAYMENT_RESULT_CODE_EXPIRED = "Expired";
    private static final String PAYMENT_RESULT_CODE_DECLINED = "Declined";

    private static final Logger logger = LoggerFactory.getLogger(SibsPayAPIService.class);

    private String sibsEndpoint;
    private String sibsAssetsEndpoint;
    private String clientId;
    private String bearerToken;
    private Integer terminalId;
    private String sibsEntityCode;

    private Client client;
    private WebTarget webTarget;

    public SibsPayAPIService(String sibsEndpoint, String sibsAssetsEndpoint, String clientId, String bearerToken,
            Integer terminalId, String sibsEntityCode) {
        this.sibsEndpoint = sibsEndpoint;
        this.sibsAssetsEndpoint = sibsAssetsEndpoint;
        this.clientId = clientId;
        this.bearerToken = bearerToken;
        this.terminalId = terminalId;
        this.sibsEntityCode = sibsEntityCode;

        this.client = ClientBuilder.newClient()
                .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT, LoggingFeature.Verbosity.PAYLOAD_ANY)
                .property(LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT, "FINEST");

        this.client.register(LoggingFeature.class);

        this.webTarget = client.target(sibsEndpoint).path("sibs/spg/v2");
    }

    /* ***************
     * FORWARD PAYMENT
     * ***************
     */

    public SibsPayReturnCheckout processForwardPaymentCheckout(ForwardPaymentRequest forwardPayment,
            SibsBillingAddressBean billingAddressBean) throws OnlinePaymentsGatewayCommunicationException {

        String requestLog = null;
        String responseLog = null;
        try {
            ObjectMapper objectMapper = createObjectMapper();

            SibsPayRequestCheckout requestCheckout = createRequestCheckout(forwardPayment, billingAddressBean);

            requestLog = objectMapper.writeValueAsString(requestCheckout);

            logger.debug(requestLog);

            WebTarget checkoutWebTarget = this.webTarget.path("payments");

            Builder builder = checkoutWebTarget.request(MediaType.APPLICATION_JSON);

            builder.header("Authorization", this.bearerToken);
            builder.header("X-IBM-Client-Id", this.clientId);

            Response response = builder.post(Entity.entity(requestLog, MediaType.APPLICATION_JSON));

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, response.readEntity(String.class),
                        "unsuccessful request");
            }

            responseLog = response.readEntity(String.class);

            logger.debug(responseLog);

            SibsPayReturnCheckout returnCheckout = objectMapper.readValue(responseLog, SibsPayReturnCheckout.class);

            returnCheckout.setRequestLog(requestLog);
            returnCheckout.setResponseLog(responseLog);

            return returnCheckout;
        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        }

    }

    private SibsPayRequestCheckout createRequestCheckout(ForwardPaymentRequest forwardPayment,
            SibsBillingAddressBean billingAddressBean) {
        SibsPayRequestCheckout result = new SibsPayRequestCheckout();

        result.setMerchant(new SibsPayMerchant());

        result.getMerchant().setChannel("web");
        result.getMerchant().setMerchantTransactionId(forwardPayment.getMerchantTransactionId());
        result.getMerchant().setTerminalId(this.terminalId);

        result.setCustomer(new SibsPayCustomer());
        result.getCustomer().setCustomerInfo(new SibsPayCustomerInfo());

        result.getCustomer().getCustomerInfo().setCustomerName(forwardPayment.getDebtAccount().getCustomer().getName());
        SibsPayAddress address = new SibsPayAddress();
        String cityText = billingAddressBean.getCity();
        String zipCodeText = billingAddressBean.getZipCode();
        String addressText = billingAddressBean.getAddress();

        address.setCity(cityText != null ? Splitter.fixedLength(50).splitToList(cityText).get(0) : null);
        address.setCountry(billingAddressBean.getAddressCountryCode());
        address.setPostcode(zipCodeText != null ? Splitter.fixedLength(16).splitToList(zipCodeText).get(0) : null);
        address.setStreet1(addressText != null ? Splitter.fixedLength(50).splitToList(addressText).get(0) : null);

        // result.getCustomer().getCustomerInfo().setShippingAddress(address);
        result.getCustomer().getCustomerInfo().setBillingAddress(address);

        String email = TreasuryPlataformDependentServicesFactory.implementation()
                .getCustomerEmail(forwardPayment.getDebtAccount().getCustomer());
        result.getCustomer().getCustomerInfo().setCustomerEmail(email);

        SibsPayTransaction transaction = new SibsPayTransaction();
        transaction.setAmount(new SibsPayAmount());
        transaction.getAmount().setCurrency("EUR");
        transaction.getAmount().setValue(forwardPayment.getPayableAmount());
        transaction.setDescription("Online payment");
        transaction.setTransactionTimestamp(new DateTime());
        transaction.setMoto(false);
        transaction.setPaymentType("PURS");
        transaction.setPaymentMethod(List.of("CARD"));
        result.setTransaction(transaction);

        {
            SibsPayPaymentReference paymentReference = new SibsPayPaymentReference();
            paymentReference.setEntity(
                    forwardPayment.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode());
            paymentReference.setInitialDatetime(new DateTime());
            paymentReference.setFinalDatetime(new DateTime().plusDays(1));

            SibsPayAmount referenceAmount = new SibsPayAmount();

            referenceAmount.setCurrency("EUR");
            referenceAmount.setValue(forwardPayment.getPayableAmount());
            paymentReference.setMinAmount(referenceAmount);
            paymentReference.setMaxAmount(referenceAmount);

            transaction.setPaymentReference(paymentReference);
        }

        return result;
    }

    /* **************
     * PAYMENT STATUS
     * **************
     */

    public SibsPayResponseInquiryWrapper getPaymentStatusBySibsTransactionId(String transactionId)
            throws OnlinePaymentsGatewayCommunicationException {
        String responseLog = null;
        try {
            ObjectMapper objectMapper = createObjectMapper();

            WebTarget paymentStatusTarget = this.webTarget.path("payments").path(transactionId).path("status");

            Builder builder = paymentStatusTarget.request(MediaType.APPLICATION_JSON);

            builder.accept(MediaType.APPLICATION_JSON);
            builder.header("Authorization", this.bearerToken);
            builder.header("X-IBM-Client-Id", this.clientId);

            Response response = builder.get();

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(null, response.readEntity(String.class),
                        "unsuccessful request");
            }

            responseLog = response.readEntity(String.class);

            SibsPayResponseInquiry responseInquiry = objectMapper.readValue(responseLog, SibsPayResponseInquiry.class);

            return new SibsPayResponseInquiryWrapper(responseInquiry, null, responseLog);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(null, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(null, responseLog, e);
        }
    }

    public SibsPayResponseInquiryWrapper getPaymentStatusByMerchantTransactionId(String merchantTransactionId)
            throws OnlinePaymentsGatewayCommunicationException {
        String responseLog = null;
        try {
            ObjectMapper objectMapper = createObjectMapper();

            WebTarget paymentStatusTarget = this.webTarget.path("payments/status");

            paymentStatusTarget = paymentStatusTarget.queryParam("merchantTransactionId", merchantTransactionId);

            Builder builder = paymentStatusTarget.request(MediaType.APPLICATION_JSON);

            builder.accept(MediaType.APPLICATION_JSON);
            builder.header("Authorization", this.bearerToken);
            builder.header("X-IBM-Client-Id", this.clientId);

            Response response = builder.get();

            responseLog = response.readEntity(String.class);

            logger.debug(responseLog);

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(null, responseLog, "unsuccessful request");
            }

            SibsPayResponseInquiry responseInquiry = objectMapper.readValue(responseLog, SibsPayResponseInquiry.class);

            return new SibsPayResponseInquiryWrapper(responseInquiry, null, responseLog);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(null, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(null, responseLog, e);
        }
    }

    /* **********************
     * SIBS PAYMENT REFERENCE
     * ********************** 
     */

    public SibsPayReturnCheckout processSibsPaymentRequestOrMbwayCheckout(DebtAccount debtAccount, BigDecimal payableAmount,
            DateTime validFrom, DateTime validTo, String merchantTransactionId)
            throws OnlinePaymentsGatewayCommunicationException {

        String requestLog = null;
        String responseLog = null;
        try {
            ObjectMapper objectMapper = createObjectMapper();

            SibsPayRequestCheckout requestCheckout = createCheckoutForSibsPaymentRequestOrMbWayGeneration(debtAccount,
                    payableAmount, validFrom, validTo, merchantTransactionId);

            requestLog = objectMapper.writeValueAsString(requestCheckout);

            logger.debug(requestLog);

            WebTarget checkoutWebTarget = this.webTarget.path("payments");

            Builder builder = checkoutWebTarget.request(MediaType.APPLICATION_JSON);

            builder.header("Authorization", this.bearerToken);
            builder.header("X-IBM-Client-Id", this.clientId);

            Response response = builder.post(Entity.entity(requestLog, MediaType.APPLICATION_JSON));

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, response.readEntity(String.class),
                        "unsuccessful processSibsPaymentRequestCheckout");
            }

            responseLog = response.readEntity(String.class);

            logger.debug(responseLog);

            SibsPayReturnCheckout returnCheckout = objectMapper.readValue(responseLog, SibsPayReturnCheckout.class);

            returnCheckout.setRequestLog(requestLog);
            returnCheckout.setResponseLog(responseLog);

            return returnCheckout;
        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        }
    }

    public SibsPayResponseInquiryWrapper generateSibsPaymentRequestTransaction(String transactionId, String transactionSignature)
            throws OnlinePaymentsGatewayCommunicationException {

        String requestLog = "{}";
        String responseLog = null;
        try {
            ObjectMapper objectMapper = createObjectMapper();

            WebTarget checkoutWebTarget = this.webTarget.path("payments").path(transactionId).path("service-reference/generate");

            Builder builder = checkoutWebTarget.request(MediaType.APPLICATION_JSON);

            builder.header("Authorization", "Digest " + transactionSignature);
            builder.header("X-IBM-Client-Id", this.clientId);

            Response response = builder.post(Entity.entity(requestLog, MediaType.APPLICATION_JSON));

            responseLog = response.readEntity(String.class);
            logger.debug(responseLog);

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog,
                        "unsuccessful generateSibsPaymentRequestTransaction");
            }

            SibsPayResponseInquiry responseInquiry = objectMapper.readValue(responseLog, SibsPayResponseInquiry.class);

            return new SibsPayResponseInquiryWrapper(responseInquiry, requestLog, responseLog);

        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        }
    }

    public SibsPayCancellationResponse cancelTransaction(String merchantTransactionId, String originalTransactionId,
            BigDecimal amount) throws OnlinePaymentsGatewayCommunicationException {

        String requestLog = "{}";
        String responseLog = null;
        try {

            ObjectMapper objectMapper = createObjectMapper();

            WebTarget checkoutWebTarget = this.webTarget.path("payments").path(originalTransactionId).path("cancellation");

            Builder builder = checkoutWebTarget.request(MediaType.APPLICATION_JSON);

            builder.header("Authorization", this.bearerToken);
            builder.header("X-IBM-Client-Id", this.clientId);

            SibsPayCancellationRequest cancellationRequest = new SibsPayCancellationRequest();

            cancellationRequest.setMerchant(new SibsPayMerchant());
            cancellationRequest.getMerchant().setTerminalId(this.terminalId);
            cancellationRequest.getMerchant().setChannel("web");
            cancellationRequest.getMerchant().setMerchantTransactionId(merchantTransactionId);

            cancellationRequest.setTransaction(new SibsPayTransaction());
            cancellationRequest.getTransaction().setTransactionTimestamp(new DateTime());
            cancellationRequest.getTransaction().setPaymentMethod(null);
            cancellationRequest.getTransaction().setAmount(new SibsPayAmount());
            cancellationRequest.getTransaction().getAmount().setValue(amount);
            cancellationRequest.getTransaction().getAmount().setCurrency("EUR");

            cancellationRequest.setOriginalTransaction(new SibsPayOriginalTransaction());

            cancellationRequest.getOriginalTransaction().setId(originalTransactionId);
            cancellationRequest.getOriginalTransaction().setDatetime(new DateTime().toString("yyyy-MM-dd'T'HH:mm:ss.SSSZZ"));

            requestLog = objectMapper.writeValueAsString(cancellationRequest);

            logger.debug(requestLog);

            Response response = builder.post(Entity.entity(requestLog, MediaType.APPLICATION_JSON));

            responseLog = response.readEntity(String.class);
            logger.debug(responseLog);

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog,
                        "unsuccessful generateSibsPaymentRequestTransaction");
            }

            SibsPayCancellationResponse cancellationResponse =
                    objectMapper.readValue(responseLog, SibsPayCancellationResponse.class);

            cancellationResponse.setRequestLog(requestLog);
            cancellationResponse.setResponseLog(responseLog);

            return cancellationResponse;
        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        }
    }

    public SibsPayResponseInquiryWrapper generateMbwayRequestTransaction(String transactionId, String transactionSignature,
            String countryPrefix, String localPhoneNumber) throws OnlinePaymentsGatewayCommunicationException {

        String requestLog = String.format("{\"customerPhone\": \"%s#%s\"}", countryPrefix, localPhoneNumber);
        String responseLog = null;
        try {
            ObjectMapper objectMapper = createObjectMapper();

            WebTarget checkoutWebTarget = this.webTarget.path("payments").path(transactionId).path("mbway-id/purchase");

            Builder builder = checkoutWebTarget.request(MediaType.APPLICATION_JSON);

            builder.header("Authorization", "Digest " + transactionSignature);
            builder.header("X-IBM-Client-Id", this.clientId);

            Response response = builder.post(Entity.entity(requestLog, MediaType.APPLICATION_JSON));

            responseLog = response.readEntity(String.class);
            logger.debug(responseLog);

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog,
                        "unsuccessful generateSibsPaymentRequestTransaction");
            }

            SibsPayResponseInquiry responseInquiry = objectMapper.readValue(responseLog, SibsPayResponseInquiry.class);

            return new SibsPayResponseInquiryWrapper(responseInquiry, requestLog, responseLog);

        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        }
    }

    private SibsPayRequestCheckout createCheckoutForSibsPaymentRequestOrMbWayGeneration(DebtAccount debtAccount,
            BigDecimal payableAmount, DateTime validFrom, DateTime validTo, String merchantTransactionId) {

        SibsPayRequestCheckout result = new SibsPayRequestCheckout();

        result.setMerchant(new SibsPayMerchant());

        result.getMerchant().setChannel("web");
        result.getMerchant().setMerchantTransactionId(merchantTransactionId);
        result.getMerchant().setTerminalId(this.terminalId);

        result.setCustomer(new SibsPayCustomer());
        result.getCustomer().setCustomerInfo(new SibsPayCustomerInfo());

        result.getCustomer().getCustomerInfo().setCustomerName(debtAccount.getCustomer().getName());

        String email = TreasuryPlataformDependentServicesFactory.implementation().getCustomerEmail(debtAccount.getCustomer());
        result.getCustomer().getCustomerInfo().setCustomerEmail(email);

        SibsPayTransaction transaction = new SibsPayTransaction();
        result.setTransaction(transaction);

        SibsPayAmount amount = new SibsPayAmount();
        amount.setCurrency("EUR");
        amount.setValue(payableAmount);

        transaction.setAmount(amount);
        transaction.setDescription("Online payment");
        transaction.setTransactionTimestamp(new DateTime());
        transaction.setMoto(false);
        transaction.setPaymentType("PURS");
        transaction.setPaymentMethod(List.of("REFERENCE", "MBWAY"));

        SibsPayPaymentReference paymentReference = new SibsPayPaymentReference();
        transaction.setPaymentReference(paymentReference);

        paymentReference.setEntity(this.sibsEntityCode);
        paymentReference.setInitialDatetime(validFrom);
        paymentReference.setFinalDatetime(validTo);
        paymentReference.setMinAmount(amount);
        paymentReference.setMaxAmount(amount);

        return result;
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        JodaModule jodaModule = new JodaModule();
        objectMapper.registerModule(jodaModule);
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        objectMapper.setSerializationInclusion(Include.NON_NULL);
        return objectMapper;
    }

    public static boolean isOperationSuccess(String statusCode) {
        return STATUS_CODE_SUCCESS.equals(statusCode);
    }

    public static boolean isPaid(String paymentStatusCode) {
        return PAYMENT_RESULT_CODE_SUCCESS.equals(paymentStatusCode);
    }

    public static boolean isPending(String paymentStatusCode) {
        return PAYMENT_RESULT_CODE_PENDING.equals(paymentStatusCode);
    }

    public static boolean isExpired(String paymentStatusCode) {
        return PAYMENT_RESULT_CODE_EXPIRED.equals(paymentStatusCode);
    }

    public static boolean isDeclined(String paymentResultCode) {
        return PAYMENT_RESULT_CODE_DECLINED.equals(paymentResultCode);
    }

    public String getJsScriptURL(String checkoutId) {
        return this.sibsAssetsEndpoint + "/assets/js/widget.js?id=" + checkoutId;
    }

    public static SibsPayWebhookNotification deserializeWebhookNotification(String jsonBody)
            throws JsonParseException, JsonMappingException, IOException {
        ObjectMapper objectMapper = createObjectMapper();
        SibsPayWebhookNotification result = objectMapper.readValue(jsonBody, SibsPayWebhookNotification.class);

        return result;
    }
}
