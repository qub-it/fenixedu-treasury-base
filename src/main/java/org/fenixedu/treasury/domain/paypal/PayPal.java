package org.fenixedu.treasury.domain.paypal;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatformPaymentMode;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InstallmentPaymenPlanBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.paypal.core.PayPalEnvironment;
import com.paypal.core.PayPalHttpClient;
import com.paypal.http.HttpClient;
import com.paypal.http.HttpResponse;
import com.paypal.http.serializer.Json;
import com.paypal.orders.AmountBreakdown;
import com.paypal.orders.AmountWithBreakdown;
import com.paypal.orders.ApplicationContext;
import com.paypal.orders.Item;
import com.paypal.orders.Money;
import com.paypal.orders.Order;
import com.paypal.orders.OrderRequest;
import com.paypal.orders.OrdersCaptureRequest;
import com.paypal.orders.OrdersCreateRequest;
import com.paypal.orders.OrdersGetRequest;
import com.paypal.orders.PurchaseUnitRequest;

import fr.opensagres.xdocreport.document.json.JSONObject;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PayPal extends PayPal_Base implements IForwardPaymentPlatformService {

    public PayPal() {
        super();
    }

    private PayPal(FinantialInstitution finantialInstitution, String name, boolean active, String endpointUrl, String accountId,
            String secret, String mode) {

        this();
        this.init(finantialInstitution, name, active);

        setEndpointUrl(endpointUrl);
        setAccountId(accountId);
        setSecret(secret);
        setMode(mode);

        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getCreditCardPaymentMethod());

        checkRules();
    }

    private void checkRules() {
    }

    public static PayPal create(FinantialInstitution finantialInstitution, String name, boolean active, String endpointUrl,
            String accountId, String secret, String mode) {
        return new PayPal(finantialInstitution, name, active, endpointUrl, accountId, secret, mode);
    }

    public static String getPresentationName() {
        return TreasuryConstants.treasuryBundle("label.PayPal.presentationName");
    }

    @Override
    public ForwardPaymentRequest createForwardPaymentRequest(SettlementNoteBean bean,
            Function<ForwardPaymentRequest, String> successUrlFunction,
            Function<ForwardPaymentRequest, String> insuccessUrlFunction) {
        Set<DebitEntry> debitEntries =
                bean.getIncludedInvoiceEntryBeans().stream().map(ISettlementInvoiceEntryBean::getInvoiceEntry)
                        .filter(i -> i != null).map(DebitEntry.class::cast).collect(Collectors.toSet());

        Set<Installment> installments =
                bean.getIncludedInvoiceEntryBeans().stream().filter(i -> i instanceof InstallmentPaymenPlanBean && i.isIncluded())
                        .map(InstallmentPaymenPlanBean.class::cast).map(ib -> ib.getInstallment()).collect(Collectors.toSet());

        ForwardPaymentRequest forwardPaymentRequest =
                ForwardPaymentRequest.create(bean.getDigitalPaymentPlatform(), bean.getDebtAccount(), debitEntries, installments,
                        bean.getTotalAmountToPay(), successUrlFunction, insuccessUrlFunction);

        prepareCheckout(forwardPaymentRequest);

        return forwardPaymentRequest;
    }

    private ForwardPaymentStatusBean prepareCheckout(ForwardPaymentRequest forwardPayment) {
        String merchantTransactionId = UUID.randomUUID().toString().replace("-", "");

        FenixFramework.atomic(() -> {
            if (!StringUtils.isEmpty(forwardPayment.getMerchantTransactionId())) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayForwardImplementation.sibsMerchantTransactionId.already.filled");
            }

            forwardPayment.setMerchantTransactionId(merchantTransactionId);
        });

        try {
            DateTime requestSendDate = new DateTime();

            List<Item> items = new ArrayList<>();
            forwardPayment.getDebitEntriesSet().stream()
                    .forEach(d -> items.add(new Item().name(d.getDescription())
                            .unitAmount(new Money().currencyCode(d.getCurrency().getIsoCode())
                                    .value(Currency.getValueWithScale(d.getOpenAmountWithInterests()).toString()))
                            .quantity(d.getQuantity().toString())));

            forwardPayment.getInstallmentsSet().stream().forEach(d -> items.add(new Item().name(d.getDescription().getContent())
                    .unitAmount(new Money()
                            .currencyCode(d.getSortedOpenInstallmentEntries().get(0).getDebitEntry().getCurrency().getCode())
                            .value(Currency.getValueWithScale(d.getOpenAmount()).toString()))
                    .quantity("1")));



            OrderRequest orderRequest = new OrderRequest();
            orderRequest.checkoutPaymentIntent("CAPTURE");

            ApplicationContext applicationContext = new ApplicationContext().brandName("FenixEdu")
                    .landingPage("LOGIN").cancelUrl(forwardPayment.getForwardPaymentInsuccessUrl())
                    .returnUrl(forwardPayment.getForwardPaymentSuccessUrl()).userAction("PAY_NOW")
                    .shippingPreference("NO_SHIPPING");
            orderRequest.applicationContext(applicationContext);

            List<PurchaseUnitRequest> purchaseUnitRequests = new ArrayList<PurchaseUnitRequest>();
            PurchaseUnitRequest purchaseUnitRequest = new PurchaseUnitRequest();
            purchaseUnitRequest
                    .amountWithBreakdown(new AmountWithBreakdown().currencyCode(items.get(0).unitAmount().currencyCode())
                            .value(forwardPayment.getPayableAmount().toString())
                            .amountBreakdown(new AmountBreakdown()
                                    .itemTotal(new Money().currencyCode(items.get(0).unitAmount().currencyCode())
                                            .value(forwardPayment.getPayableAmount().toString()))));

            purchaseUnitRequest.items(items);
            purchaseUnitRequest.customId(merchantTransactionId);

            purchaseUnitRequests.add(purchaseUnitRequest);
            orderRequest.purchaseUnits(purchaseUnitRequests);
            orderRequest.processingInstruction("ORDER_COMPLETE_ON_PAYMENT_APPROVAL");

            OrdersCreateRequest request = new OrdersCreateRequest();
            request.header("prefer", "return=representation");
            request.requestBody(orderRequest);

            HttpResponse<Order> response = getClient().execute(request);

            Order order = response.result();
            String links = order.links().stream().filter(link -> "approve".equals(link.rel())).findFirst().get().href();
            FenixFramework.atomic(() -> {
                forwardPayment.setCheckoutId(order.id());
                forwardPayment.setRedirectUrl(links);
            });
            DateTime requestReceiveDate = new DateTime();
            String requestLog = "";
//             new JSONObject(new Json().serialize(request)).toString(4);
            String responseLog = new JSONObject(new Json().serialize(response.result())).toString(4);

            final ForwardPaymentStateType stateType = translateForwardPaymentStateType(order.status());
            final ForwardPaymentStatusBean result =
                    new ForwardPaymentStatusBean(true, stateType, order.status(), order.status(), requestLog, responseLog);

            FenixFramework.atomic(() -> {
                if (!result.isInvocationSuccess() || (result.getStateType() == ForwardPaymentStateType.REJECTED)) {
                    PayPalLog log = (PayPalLog) log(forwardPayment, "prepareCheckout", order.status(),
                            requestLog, responseLog);
                    forwardPayment.reject();
                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);

                } else {
                    PayPalLog log = (PayPalLog) forwardPayment.advanceToRequestState("prepareCheckout", order.status(),
                            order.status(), requestLog, responseLog);

                    log.setOperationSuccess(result.isInvocationSuccess());
                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);
                }
            });

            return result;

        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                PaymentRequestLog log = forwardPayment.logException(e);
                if (!StringUtils.isEmpty(requestBody)) {
                    log.saveRequest(requestBody);

                }

                if (!StringUtils.isEmpty(responseBody)) {
                    log.saveResponse(responseBody);
                }
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Override
    public PaymentRequestLog log(PaymentRequest paymentRequest, String statusCode, String statusMessage, String requestBody,
            String responseBody) {
        final PayPalLog log = PayPalLog.createPaymentRequestLog(paymentRequest, paymentRequest.getCurrentState().getCode(),
                paymentRequest.getCurrentState().getLocalizedName());

        log.setExtInvoiceId(paymentRequest.getMerchantTransactionId());
        log.setMeoWalletId(paymentRequest.getTransactionId());

        log.setStatusCode(statusCode);
        log.setStatusMessage(statusMessage);

        if (!Strings.isNullOrEmpty(requestBody)) {
            log.saveRequest(requestBody);
        }

        if (!Strings.isNullOrEmpty(responseBody)) {
            log.saveResponse(responseBody);
        }

        return log;
    }

    private ForwardPaymentStateType translateForwardPaymentStateType(String status) {
        if (status == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayForwardImplementation.unknown.payment.state");
        }

        if (status.equals("COMPLETED")) {
            return ForwardPaymentStateType.PAYED;
        } else if (status.equals("VOIDED")) {
            return ForwardPaymentStateType.REJECTED;
        }

        return ForwardPaymentStateType.REQUESTED;
    }

    @Override
    public String getPaymentURL(ForwardPaymentRequest request) {
        return request.getRedirectUrl();
    }

    @Override
    public String getLogosJspPage() {
        return null;
    }

    @Override
    public ForwardPaymentStatusBean paymentStatus(ForwardPaymentRequest paymentRequest) {
        try {
            OrdersGetRequest request = new OrdersGetRequest(paymentRequest.getCheckoutId());
            HttpResponse<Order> response = getClient().execute(request);
            Order order = response.result();

            final ForwardPaymentStateType stateType =
                    translateForwardPaymentStateType(order.status());
            String requestLog = "";
//            String requestLog = new JSONObject(new Json().serialize(request)).toString(4);
            String responseLog = new JSONObject(new Json().serialize(response.result())).toString(4);

            final ForwardPaymentStatusBean result = new ForwardPaymentStatusBean(true, stateType,
                    order.status(), order.status(), requestLog, responseLog);

            result.editTransactionDetails(order.id(),
                    DateTime.parse(order.updateTime() == null ? order.createTime() : order.updateTime()),
                    order.purchaseUnits().stream()
                    .map(unit -> new BigDecimal(unit.amountWithBreakdown().value())).reduce(BigDecimal.ZERO, BigDecimal::add));

            return result;
        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                if (!"ERRO".equals(e.getMessage())) {
                    PaymentRequestLog log = paymentRequest.logException(e);
                    log.setOperationCode("paymentStatus");

                    if (!StringUtils.isEmpty(requestBody)) {
                        log.saveRequest(requestBody);
                    }

                    if (!StringUtils.isEmpty(responseBody)) {
                        log.saveResponse(responseBody);
                    }
                }
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    private HttpClient getClient() {
        PayPalEnvironment environment = new PayPalEnvironment.Sandbox(getAccountId(), getSecret());
        return new PayPalHttpClient(environment);
    }

    @Override
    public PostProcessPaymentStatusBean postProcessPayment(ForwardPaymentRequest forwardPayment, String justification,
            Optional<String> specificTransactionId) {
        if (specificTransactionId.isEmpty()) {
            ForwardPaymentStatusBean statusBean =
                    new ForwardPaymentStatusBean(false, forwardPayment.getState(), "N/A", "N/A", null, null);
            return new PostProcessPaymentStatusBean(statusBean, forwardPayment.getState(), false);
        }

        try {
            DateTime requestSendDate = new DateTime();
            OrdersGetRequest request = new OrdersGetRequest(forwardPayment.getCheckoutId());
            HttpResponse<Order> response = getClient().execute(request);
            Order order = response.result();

            Order resultOrder = null;
            if ("APPROVED".equals(order.status())) {
                OrdersCaptureRequest captureRequest = new OrdersCaptureRequest(order.id());
                captureRequest.requestBody(new OrderRequest());
                HttpResponse<Order> captureResponse = getClient().execute(captureRequest);
                resultOrder = captureResponse.result();
            }
            DateTime requestReceiveDate = new DateTime();

            final ForwardPaymentStateType stateType = translateForwardPaymentStateType(resultOrder.status());
            String requestLog = "";
//            String requestLog = new JSONObject(new Json().serialize(request)).toString(4);
            String responseLog = new JSONObject(new Json().serialize(response.result())).toString(4);

            final ForwardPaymentStatusBean result =
                    new ForwardPaymentStatusBean(true, stateType, resultOrder.status(), resultOrder.status(), requestLog,
                            responseLog);

            result.editTransactionDetails(resultOrder.id(),
                    DateTime.parse(order.updateTime() == null ? order.createTime() : order.updateTime()),
                    resultOrder.purchaseUnits().stream()
                    .map(unit -> new BigDecimal(unit.amountWithBreakdown().value())).reduce(BigDecimal.ZERO, BigDecimal::add));

            if (Lists.newArrayList(ForwardPaymentStateType.CREATED, ForwardPaymentStateType.REQUESTED)
                    .contains(result.getStateType())) {
                // Do nothing
                return new PostProcessPaymentStatusBean(result, forwardPayment.getState(), false);
            }

            // First of all save sibsTransactionId

            PostProcessPaymentStatusBean returnBean =
                    new PostProcessPaymentStatusBean(result, forwardPayment.getState(), result.isInPayedState());

            returnBean.getForwardPaymentStatusBean().defineSibsOnlinePaymentBrands("TODO");
            String orderId = resultOrder.id();
            String payerId = resultOrder.payer() != null ? resultOrder.payer().payerId() : "";
            PayPalLog log = FenixFramework.atomic(() -> {
                PayPalLog log2 = new PayPalLog("processPaymentStatus", orderId, payerId);
                log2.setRequestSendDate(requestSendDate);
                log2.setRequestReceiveDate(requestReceiveDate);
                log2.setPaymentRequest(forwardPayment);
                return log2;
            });

            processPaymentStatus(log, forwardPayment, returnBean);

            return returnBean;
        } catch (final Exception e) {
            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                PaymentRequestLog log = forwardPayment.logException(e);
                if (!StringUtils.isEmpty(requestBody)) {
                    log.saveRequest(requestBody);
                }

                if (!StringUtils.isEmpty(responseBody)) {
                    log.saveResponse(responseBody);
                }
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Atomic
    private void processPaymentStatus(PayPalLog log, ForwardPaymentRequest forwardPayment,
            PostProcessPaymentStatusBean returnBean) {
        ForwardPaymentStatusBean result = returnBean.getForwardPaymentStatusBean();

        log.setStateCode(result.getStatusCode());
        log.setStateDescription(result.getStateType().getLocalizedName());
        log.setStatusMessage(result.getStatusMessage());

        if (forwardPayment.getState().isPayed() || forwardPayment.getState().isRejected()) {
            log.setTransactionWithPayment(forwardPayment.getState().isPayed());
            log.setOperationCode("processDuplicated");
            log.setOperationSuccess(true);
            return;
        }

        forwardPayment.setTransactionId(result.getTransactionId());
        if (result.isInPayedState()) {
            PaymentTransaction paymentTransaction = forwardPayment.advanceToPaidState(result.getStatusCode(),
                    result.getPayedAmount(), result.getTransactionDate(), result.getTransactionId(), null);

            log.setOperationCode("advanceToPaidState");
            log.setOperationSuccess(true);
            log.setTransactionWithPayment(true);
            log.setPaymentTransaction(paymentTransaction);
            log.saveRequest(result.getRequestBody());
            log.saveResponse(result.getResponseBody());

            log.setMeoWalletId(result.getTransactionId());
            log.savePaymentInfo(result.getPayedAmount(), result.getTransactionDate());
            log.setPaymentMethod(result.getSibsOnlinePaymentBrands());

        } else if (result.isInRejectedState()) {
            forwardPayment.reject();

            log.setOperationCode("postProcessPayment");
            log.setOperationSuccess(false);
            log.setTransactionWithPayment(false);
            log.setMeoWalletId(result.getTransactionId());
        }
    }
    @Override
    public List<? extends DigitalPlatformResultBean> getPaymentTransactionsReportListByMerchantId(String merchantTransationId) {
//        final MeoWalletLog log = MeoWalletLog.createForTransationReport(merchantTransationId);
//        try {
//            List<MeoWalletPaymentBean> resultCheckoutBean = new ArrayList<>();
//            FenixFramework.atomic(() -> log.setRequestSendDate(DateTime.now()));
//            resultCheckoutBean = getMeoWalletService().getPaymentTransactionReportByMerchantTransactionId(merchantTransationId);
//
//            String request = resultCheckoutBean.isEmpty() ? "" : resultCheckoutBean.get(0).getRequestLog();
//            String response = resultCheckoutBean.isEmpty() ? "" : resultCheckoutBean.get(0).getResponseLog();
//            String operationId = resultCheckoutBean.isEmpty() ? "" : resultCheckoutBean.get(0).getId();
//            FenixFramework.atomic(() -> {
//                log.setRequestReceiveDate(DateTime.now());
//                log.saveRequest(request);
//                log.saveResponse(response);
//                log.setMeoWalletId(operationId);
//            });
//
//            return resultCheckoutBean;
//        } catch (Exception e) {
//            FenixFramework.atomic(() -> log.logException(e));
//            throw new RuntimeException(e);
//        }
        return null;
    }

    @Override
    public PostProcessPaymentStatusBean processForwardPayment(ForwardPaymentRequest forwardPayment) {
        return postProcessPayment(forwardPayment, "", Optional.of(forwardPayment.getCheckoutId()));
    }

    @Override
    public PostProcessPaymentStatusBean processForwardPaymentFromWebhook(PaymentRequestLog paymentRequestLog,
            DigitalPlatformResultBean digitalPlatformResultBean) {

//        ForwardPaymentRequest forwardPayment = (ForwardPaymentRequest) paymentRequestLog.getPaymentRequest();
//        PayPalLog log = (PayPalLog) paymentRequestLog;
//        MeoWalletCallbackBean bean = (MeoWalletCallbackBean) digitalPlatformResultBean;
//        try {
//
//            final ForwardPaymentStateType stateType = translateForwardPaymentStateType(bean.getOperation_status());
//
//            final ForwardPaymentStatusBean result =
//                    new ForwardPaymentStatusBean(true, stateType, bean.getOperation_status(), bean.getOperation_status(), "", "");
//
//            result.editTransactionDetails(bean.getOperation_id(), bean.getModified_date(), bean.getAmount());
//            if (Lists.newArrayList(ForwardPaymentStateType.CREATED, ForwardPaymentStateType.REQUESTED)
//                    .contains(result.getStateType())) {
//                // Do nothing
//                return new PostProcessPaymentStatusBean(result, forwardPayment.getState(), false);
//            }
//
//            PostProcessPaymentStatusBean returnBean =
//                    new PostProcessPaymentStatusBean(result, forwardPayment.getState(), result.isInPayedState());
//            returnBean.getForwardPaymentStatusBean().defineSibsOnlinePaymentBrands(bean.getMethod());
//
//            processPaymentStatus(log, forwardPayment, returnBean);
//
//            return returnBean;
//        } catch (final Exception e) {
//            FenixFramework.atomic(() -> {
//                String requestBody = null;
//                String responseBody = null;
//
//                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
//                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
//                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
//                }
//
//                PaymentRequestLog log2 = forwardPayment.logException(e);
//                if (!StringUtils.isEmpty(requestBody)) {
//                    log.saveRequest(requestBody);
//                }
//
//                if (!StringUtils.isEmpty(responseBody)) {
//                    log.saveResponse(responseBody);
//                }
//            });
//
//            throw new TreasuryDomainException(e,
//                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
//        }
        return null;
    }
}
