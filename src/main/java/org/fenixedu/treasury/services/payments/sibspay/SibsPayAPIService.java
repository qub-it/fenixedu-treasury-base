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

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsBillingAddressBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibspay.model.*;
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

    private static final String STATUS_CODE_UNKNOWN_MBWAY_AUTH_PAYMENT = "E0507";

    private static final String PAYMENT_RESULT_CODE_SUCCESS = "Success";
    private static final String PAYMENT_RESULT_CODE_PENDING = "Pending";
    private static final String PAYMENT_RESULT_CODE_EXPIRED = "Expired";
    private static final String PAYMENT_RESULT_CODE_DECLINED = "Declined";

    private static final String PAYMENT_TYPE_PURS = "PURS";
    private static final String PAYMENT_TYPE_PREF = "PREF";

    private static final String MANDATE_ACTION_AUTH_CREATION = "CRTN";
    private static final String MANDATE_ACTION_AUTH_SUSPENSION = "SSPN";
    private static final String MANDATE_ACTION_AUTH_REACTIVATION = "RCTV";
    private static final String MANDATE_ACTION_AUTH_LIMITS_UPDATE = "LMUP";
    private static final String MANDATE_ACTION_AUTH_CANCEL = "CNCL";

    private static final String MANDATE_ACTION_STATUS_SUCCESS = "SCCS";
    private static final String MANDATE_ACTION_STATUS_REJECTED = "RJCT";
    private static final String MANDATE_ACTION_STATUS_REFUSED = "RFSD";

    private static final Logger logger = LoggerFactory.getLogger(SibsPayAPIService.class);

    public static final int MAX_CITY_LENGTH = 35;
    public static final int MAX_POSTCODE_LENGTH = 16;
    public static final int MAX_STREET_LENGTH = 50;

    private static final Object PAYMENT_TYPE_AUTH = "AUTH";

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

        // ANIL 2025-05-04 (#qubIT-Fenix-6885)
        //
        // The path should be totally retrieved from database until the /payments
        // Instead of client.target(sibsEndpoint).path("sibs/spg/v2") , just invoke
        // client.target(sibsEndpoint)

        // The development path is https://spg.qly.site1.sibs.pt/api/v2/payments
        // The production path is https://api.sibspayments.com/api/v2/payments

        // The sibsEndpoint parameter should be configured in SibsPayPlatform object with:
        //
        // https://spg.qly.site1.sibs.pt/api/v2 (FOR DEVELOPMENT)
        // https://api.sibspayments.com/api/v2 (FOR PRODUCTION)

        this.webTarget = client.target(sibsEndpoint);
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

        // ANIL 2025-02-28 (#qubIT-Fenix-6683)
        //
        // THe customer name must be limited to 45 characters

        result.getCustomer().getCustomerInfo()
                .setCustomerName(limitCustomerName(forwardPayment.getDebtAccount().getCustomer().getName()));
        SibsPayAddress address = new SibsPayAddress();
        String cityText = billingAddressBean.getCity();
        String zipCodeText = billingAddressBean.getZipCode();
        String addressText = billingAddressBean.getAddress();

        address.setCity(cityText != null ? Splitter.fixedLength(MAX_CITY_LENGTH).splitToList(cityText).get(0) : null);
        address.setCountry(billingAddressBean.getAddressCountryCode());
        address.setPostcode(zipCodeText != null ? Splitter.fixedLength(MAX_POSTCODE_LENGTH).splitToList(zipCodeText).get(0) : null);
        address.setStreet1(addressText != null ? Splitter.fixedLength(MAX_STREET_LENGTH).splitToList(addressText).get(0) : null);

        // result.getCustomer().getCustomerInfo().setShippingAddress(address);
        result.getCustomer().getCustomerInfo().setBillingAddress(address);

        String email = getEmail(forwardPayment.getDebtAccount().getCustomer());
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

            SibsPayRequestCheckout requestCheckout =
                    createCheckoutForSibsPaymentRequestOrMbWayGeneration(debtAccount, payableAmount, validFrom, validTo,
                            merchantTransactionId);

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

        // ANIL 2025-02-28 (#qubIT-Fenix-6683)
        //
        // THe customer name must be limited to 45 characters

        result.getCustomer().getCustomerInfo().setCustomerName(limitCustomerName(debtAccount.getCustomer().getName()));

        String email = getEmail(debtAccount.getCustomer());

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

    private String getEmail(Customer customer) {
        if(!StringUtils.isBlank(customer.getPersonalEmail())) {
            return customer.getPersonalEmail();
        }

        return TreasuryPlataformDependentServicesFactory.implementation().getCustomerEmail(customer);
    }

    private static final int MAX_NAME_SIZE = 45;

    // ANIL 2025-02-28 (#qubIT-Fenix-6683)
    //
    // The customer name must be limited to 45 characters
    private String limitCustomerName(String name) {
        if (StringUtils.isEmpty(name)) {
            return name;
        }

        if (name.length() <= MAX_NAME_SIZE) {
            return name;
        }

        // Just return the first and the last name
        String[] compounds = name.split("\\s+");

        if (compounds.length == 0) {
            // Strange case but check for the sake of it, return the original name

            return name;
        } else if (compounds.length == 1) {
            // Limit the name to maximum of MAX_NAME_SIZE
            return compounds[0].substring(0, Integer.min(compounds[0].length(), MAX_NAME_SIZE));
        } else {
            // Return the first and the last name
            String result = compounds[0] + " " + compounds[compounds.length - 1];

            // But also limit to MAX_NAME_SIZE
            return result.substring(0, Integer.min(result.length(), MAX_NAME_SIZE));
        }
    }

    /*
     * *****************
     * SIBS PAY MANDATES
     * *****************
     */

    public SibsPayCreateMbwayMandateResponse createMbwayMandate(DebtAccount debtAccount, String countryPrefix,
            String localPhoneNumber, String merchantTransactionId) throws OnlinePaymentsGatewayCommunicationException {

        SibsPayCreateMbwayMandateRequest request = new SibsPayCreateMbwayMandateRequest();

        request.setMerchant(new SibsPayMerchant());

        request.getMerchant().setChannel("web");
        request.getMerchant().setMerchantTransactionId(merchantTransactionId);
        request.getMerchant().setTerminalId(this.terminalId);

        request.setMandate(new SibsPayMandate());
        request.getMandate().setMandateType("SUBSCRIPTION");

        String mbwayId = String.format("%s#%s", countryPrefix, localPhoneNumber);
        request.getMandate().setAliasMBWAY(mbwayId);

        request.getMandate().setCustomerName(debtAccount.getCustomer().getName());

        String requestLog = null;
        String responseLog = null;

        try {

            ObjectMapper objectMapper = createObjectMapper();

            requestLog = objectMapper.writeValueAsString(request);

            logger.debug(requestLog);

            WebTarget checkoutWebTarget = this.webTarget.path("mbway-mandates/creation");

            Builder builder = checkoutWebTarget.request(MediaType.APPLICATION_JSON);

            builder.header("Authorization", this.bearerToken);
            builder.header("X-IBM-Client-Id", this.clientId);

            Response response = builder.post(Entity.entity(requestLog, MediaType.APPLICATION_JSON));

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, response.readEntity(String.class),
                        "unsuccessful createMbwayMandate");
            }

            responseLog = response.readEntity(String.class);
            logger.debug(responseLog);

            SibsPayCreateMbwayMandateResponse returnValue =
                    objectMapper.readValue(responseLog, SibsPayCreateMbwayMandateResponse.class);

            return returnValue;
        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        }

    }

    public SibsPayGetMbwayMandatesListResponse getMbwayMandatesList() throws OnlinePaymentsGatewayCommunicationException {
        String requestLog = "{}";
        String responseLog = null;

        try {

            ObjectMapper objectMapper = createObjectMapper();

            logger.debug(requestLog);

            WebTarget checkoutWebTarget = this.webTarget.path("mbway-mandates/list");

            Builder builder = checkoutWebTarget.request(MediaType.APPLICATION_JSON);

            builder.header("Authorization", this.bearerToken);
            builder.header("X-IBM-Client-Id", this.clientId);

            Response response = builder.post(Entity.entity(requestLog, MediaType.APPLICATION_JSON));

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, response.readEntity(String.class),
                        "unsuccessful getMbwayMandatesList");
            }

            responseLog = response.readEntity(String.class);
            logger.debug(responseLog);

            SibsPayGetMbwayMandatesListResponse returnValue =
                    objectMapper.readValue(responseLog, SibsPayGetMbwayMandatesListResponse.class);

            return returnValue;
        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        }
    }

    public SibsPayGetInquiryMbwayMandateResponse getInquiryMbwayMandate(String transactionId, String countryPrefix,
            String localPhoneNumber) throws OnlinePaymentsGatewayCommunicationException {
        String requestLog = "{}";
        String responseLog = null;

        ObjectMapper objectMapper = createObjectMapper();

        try {
            logger.debug(requestLog);

            WebTarget checkoutWebTarget = this.webTarget.path("mbway-mandates").path(transactionId).path("inquiry");

            Builder builder = checkoutWebTarget.request(MediaType.APPLICATION_JSON);

            builder.header("Authorization", this.bearerToken);
            builder.header("X-IBM-Client-Id", this.clientId);
            builder.header("Mbway-ID", "%s#%s".formatted(countryPrefix, localPhoneNumber));

            Response response = builder.post(Entity.entity(requestLog, MediaType.APPLICATION_JSON));

            responseLog = response.readEntity(String.class);
            logger.debug(responseLog);

            SibsPayGetInquiryMbwayMandateResponse returnValue =
                    objectMapper.readValue(responseLog, SibsPayGetInquiryMbwayMandateResponse.class);

            if (returnValue == null) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog,
                        "unsuccessful getInquiryMbwayMandate");
            }

            returnValue.setRequestLog(requestLog);
            returnValue.setResponseLog(responseLog);

            return returnValue;
        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);

            try {
                SibsPayGetInquiryMbwayMandateResponse returnValue =
                        objectMapper.readValue(responseLog, SibsPayGetInquiryMbwayMandateResponse.class);

                returnValue.setRequestLog(requestLog);
                returnValue.setResponseLog(responseLog);

                return returnValue;
            } catch (IOException e) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
            }
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        }
    }

    public SibsPayReturnCheckout processMbwayMandateCheckout(DebtAccount debtAccount, BigDecimal payableAmount,
            String merchantTransactionId, String mandateId) throws OnlinePaymentsGatewayCommunicationException {

        String requestLog = null;
        String responseLog = null;
        try {
            ObjectMapper objectMapper = createObjectMapper();

            SibsPayRequestCheckout requestCheckout =
                    createCheckoutForMbWayMandateGeneration(debtAccount, payableAmount, merchantTransactionId, mandateId);

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

    private SibsPayRequestCheckout createCheckoutForMbWayMandateGeneration(DebtAccount debtAccount, BigDecimal payableAmount,
            String merchantTransactionId, String mandateId) {

        SibsPayRequestCheckout result = new SibsPayRequestCheckout();

        result.setMerchant(new SibsPayMerchant());

        result.getMerchant().setChannel("web");
        result.getMerchant().setMerchantTransactionId(merchantTransactionId);
        result.getMerchant().setTerminalId(this.terminalId);

        result.setCustomer(new SibsPayCustomer());
        result.getCustomer().setCustomerInfo(new SibsPayCustomerInfo());

        result.getCustomer().getCustomerInfo().setCustomerName(debtAccount.getCustomer().getName());

        String email = getEmail(debtAccount.getCustomer());
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
        transaction.setPaymentMethod(List.of("MANDATE", "MBWAY"));

        result.setMandate(new SibsPayRequestCheckoutMandate());

        result.getMandate().setMandateId(mandateId);

        return result;
    }

    public SibsPayResponseInquiryWrapper generateMbwayMandateRequestTransaction(String transactionId, String transactionSignature)
            throws OnlinePaymentsGatewayCommunicationException {

        String requestLog = "{ \"mandate\" : { \"mandateCreation\": false, \"useMBWAYMandate\": true } }";
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

    public SibsPayCancelMbwayMandateResponse cancelMandate(String transactionId, String merchantTransactionId, String countryPrefix, String localPhoneNumber)
            throws OnlinePaymentsGatewayCommunicationException {
        SibsPayCancelMbwayMandateRequest request = new SibsPayCancelMbwayMandateRequest();

        request.setMerchant(new SibsPayMerchant());

        request.getMerchant().setChannel("web");
        request.getMerchant().setMerchantTransactionId(merchantTransactionId);
        request.getMerchant().setTerminalId(this.terminalId);

        String requestLog = null;
        String responseLog = null;

        try {
            ObjectMapper objectMapper = createObjectMapper();

            requestLog = objectMapper.writeValueAsString(request);

            logger.debug(requestLog);

            WebTarget checkoutWebTarget = this.webTarget.path("mbway-mandates/%s/cancel".formatted(transactionId));

            Builder builder = checkoutWebTarget.request(MediaType.APPLICATION_JSON);

            builder.header("Authorization", this.bearerToken);
            builder.header("X-IBM-Client-Id", this.clientId);
            builder.header("Mbway-ID", "%s#%s".formatted(countryPrefix, localPhoneNumber));

            Response response = builder.post(Entity.entity(requestLog, MediaType.APPLICATION_JSON));

            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, response.readEntity(String.class),
                        "unsuccessful cancelMbwayMandate");
            }

            responseLog = response.readEntity(String.class);
            logger.debug(responseLog);

            SibsPayCancelMbwayMandateResponse returnValue =
                    objectMapper.readValue(responseLog, SibsPayCancelMbwayMandateResponse.class);

            returnValue.setRequestLog(requestLog);
            returnValue.setResponseLog(responseLog);

            return returnValue;
        } catch (WebApplicationException var23) {
            responseLog = var23.getResponse().readEntity(String.class);
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, var23);
        } catch (JsonProcessingException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        } catch (IOException e) {
            throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog, e);
        }
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

    public static boolean isAuthorizationAccepted(String statusCode) {
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

    public static boolean isPaymentTypePurs(String paymentTypeCode) {
        return PAYMENT_TYPE_PURS.equals(paymentTypeCode);
    }

    public static boolean isPaymentTypePref(String paymentTypeCode) {
        return PAYMENT_TYPE_PREF.equals(paymentTypeCode);
    }

    public static boolean isUnknownMbwayAuthorizedPayment(String paymentResultCode) {
        return STATUS_CODE_UNKNOWN_MBWAY_AUTH_PAYMENT.equals(paymentResultCode);
    }

    public static boolean isPaymentTypeAuth(String paymentType) {
        return PAYMENT_TYPE_AUTH.equals(paymentType);
    }

    public static boolean isMandateActionAuthCreation(String mandateAction) {
        return MANDATE_ACTION_AUTH_CREATION.equals(mandateAction);
    }

    public static boolean isMandateActionAuthSuspension(String mandateAction) {
        return MANDATE_ACTION_AUTH_SUSPENSION.equals(mandateAction);
    }

    public static boolean isMandateActionAuthReactivation(String mandateAction) {
        return MANDATE_ACTION_AUTH_REACTIVATION.equals(mandateAction);
    }

    public static boolean isMandateActionAuthLimitsUpdate(String mandateAction) {
        return MANDATE_ACTION_AUTH_LIMITS_UPDATE.equals(mandateAction);
    }

    public static boolean isMandateActionAuthCancel(String mandateAction) {
        return MANDATE_ACTION_AUTH_CANCEL.equals(mandateAction);
    }

    public static boolean isMandateActionStatusSuccess(String mandateActionStatus) {
        return MANDATE_ACTION_STATUS_SUCCESS.equals(mandateActionStatus);
    }

    public static boolean isMandateActionStatusRejected(String mandateActionStatus) {
        return MANDATE_ACTION_STATUS_REJECTED.equals(mandateActionStatus);
    }

    public static boolean isMandateActionStatusRefused(String mandateActionStatus) {
        return MANDATE_ACTION_STATUS_REFUSED.equals(mandateActionStatus);
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
