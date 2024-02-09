package org.fenixedu.treasury.domain.sibspay;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentCodeTransaction;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxTreasuryEvent;
import org.fenixedu.treasury.domain.payments.IMbwayPaymentPlatformService;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatformPaymentMode;
import org.fenixedu.treasury.domain.payments.integration.StandardSibsPaymentExpiryStrategy;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibspaymentsgateway.MbwayRequest;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InstallmentPaymenPlanBean;
import org.fenixedu.treasury.dto.PaymentPenaltyEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayAPIService;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayCancellationResponse;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayResponseInquiryWrapper;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayReturnCheckout;
import org.fenixedu.treasury.services.payments.sibspay.model.SibsPayWebhookNotificationWrapper;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class SibsPayPlatform extends SibsPayPlatform_Base
        implements ISibsPaymentCodePoolService, IForwardPaymentPlatformService, IMbwayPaymentPlatformService {

    public SibsPayPlatform() {
        super();

        new StandardSibsPaymentExpiryStrategy(this);
    }

    public SibsPayPlatform(FinantialInstitution finantialInstitution, String name, boolean active, String clientId,
            String bearerToken, Integer terminalId, String entityReferenceCode, String endpoint, String assetsEndpointUrl) {
        this();

        super.setFinantialInstitution(finantialInstitution);
        super.setName(name);
        super.setActive(active);
        super.setClientId(clientId);
        super.setTerminalId(terminalId);
        super.setBearerToken(bearerToken);
        super.setEntityReferenceCode(entityReferenceCode);
        super.setEndpointUrl(endpoint);
        super.setAssetsEndpointUrl(assetsEndpointUrl);

        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getCreditCardPaymentMethod());
        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getMbPaymentMethod());
        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getMbWayPaymentMethod());
    }

    @Override
    public PaymentTransaction processMbwayTransaction(PaymentRequestLog log, DigitalPlatformResultBean bean) {
        MbwayRequest request = (MbwayRequest) log.getPaymentRequest();

        if (!bean.getTransactionId().equals(request.getTransactionId())) {
            throw new TreasuryDomainException(
                    "error.MbwayPaymentRequest.processMbwayTransaction.merchantTransactionId.not.equal");
        }

        final BigDecimal paidAmount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate() != null ? bean.getPaymentDate() : request.getRequestDate();

        FenixFramework.atomic(() -> {
            log.savePaymentTypeAndBrand(bean.getPaymentType(), bean.getPaymentBrand());
            log.savePaymentInfo(paidAmount, paymentDate);
        });

        if (paidAmount == null || !TreasuryConstants.isPositive(paidAmount)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.payment.date");
        }

        if (PaymentTransaction.isTransactionDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }

        try {
            return FenixFramework.atomic(() -> {
                final Set<SettlementNote> settlementNotes =
                        request.processPayment(paidAmount, paymentDate, bean.getTransactionId(), bean.getMerchantTransactionId());
                PaymentTransaction transaction =
                        PaymentTransaction.create(request, bean.getTransactionId(), paymentDate, paidAmount, settlementNotes);

                return transaction;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @Atomic(mode = TxMode.READ)
    public MbwayRequest createMbwayRequest(SettlementNoteBean settlementNoteBean, String countryPrefix, String localPhoneNumber) {
        DebtAccount debtAccount = settlementNoteBean.getDebtAccount();

        Set<DebitEntry> debitEntries =
                settlementNoteBean.getIncludedInvoiceEntryBeans().stream().filter(s -> s.getInvoiceEntry() != null)
                        .map(s -> s.getInvoiceEntry()).map(DebitEntry.class::cast).collect(Collectors.toSet());
        Set<Installment> installments =
                settlementNoteBean.getIncludedInvoiceEntryBeans().stream().filter(s -> s.isForInstallment())
                        .map(InstallmentPaymenPlanBean.class::cast).map(s -> s.getInstallment()).collect(Collectors.toSet());

        return createMbwayRequest(debtAccount, debitEntries, installments, countryPrefix, localPhoneNumber);
    }

    @Override
    @Atomic(mode = TxMode.READ)
    public MbwayRequest createMbwayRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries, Set<Installment> installments,
            String countryPrefix, String localPhoneNumber) {
        final Function<DebitEntry, BigDecimal> getExtraAmount = (DebitEntry debitEntry) -> {
            PaymentPenaltyEntryBean penaltyTax =
                    PaymentPenaltyTaxTreasuryEvent.calculatePaymentPenaltyTax(debitEntry, LocalDate.now());

            BigDecimal penaltyTaxAmount = penaltyTax != null ? penaltyTax.getSettledAmount() : BigDecimal.ZERO;

            return debitEntry.getOpenAmountWithInterests().add(penaltyTaxAmount);
        };

        if (PaymentRequest.getReferencedCustomers(debitEntries, installments).size() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.referencedCustomers.only.one.allowed");
        }

        if (StringUtils.isEmpty(countryPrefix)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.countryPrefix.required");
        }

        if (StringUtils.isEmpty(localPhoneNumber)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.required");
        }

        if (!countryPrefix.matches("\\d+")) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.countryPrefix.number.format.required");
        }

        if (!localPhoneNumber.matches("\\d+")) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.format.required");
        }

        String phoneNumber = String.format("%s#%s", countryPrefix, localPhoneNumber);

        BigDecimal payableAmountDebitEntries =
                debitEntries.stream().map(e -> getExtraAmount.apply(e)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(i -> i.getOpenAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        String merchantTransactionId = generateNewMerchantTransactionId();

        Optional<String> transactionIdOptional = Optional.empty();
        Optional<String> transactionSignatureOptional = Optional.empty();

        SibsPayAPIService sibsPayService = new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(),
                getBearerToken(), getTerminalId(), getEntityReferenceCode());

        MbwayRequest mbwayRequest = MbwayRequest.create(this, debtAccount, debitEntries, installments, phoneNumber, payableAmount,
                merchantTransactionId);

        // 1. Request checkout
        {
            PaymentRequestLog log = log(mbwayRequest, "createMbwayRequest", null, null, null, null);
            try {

                DateTime requestSendDate = new DateTime();

                SibsPayReturnCheckout sibsPayReturnCheckout = sibsPayService.processSibsPaymentRequestOrMbwayCheckout(debtAccount,
                        payableAmount, new DateTime(), new DateTime().plusMinutes(3), merchantTransactionId);

                DateTime requestReceiveDate = new DateTime();

                boolean isOperationSuccess =
                        SibsPayAPIService.isOperationSuccess(sibsPayReturnCheckout.getReturnStatus().getStatusCode());

                FenixFramework.atomic(() -> {
                    mbwayRequest.setTransactionId(sibsPayReturnCheckout.getTransactionID());

                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);
                    log.logRequestReceiveDateAndData(sibsPayReturnCheckout.getTransactionID(), isOperationSuccess, false,
                            sibsPayReturnCheckout.getReturnStatus().getStatusCode(),
                            sibsPayReturnCheckout.getReturnStatus().getStatusDescription());

                    log.saveRequest(sibsPayReturnCheckout.getRequestLog());
                    log.saveResponse(sibsPayReturnCheckout.getResponseLog());
                });

                if (!isOperationSuccess) {
                    throw new TreasuryDomainException("error.MbwayPaymentRequest.request.in.gateway.failed");
                }

                transactionIdOptional = Optional.ofNullable(sibsPayReturnCheckout.getTransactionID());
                transactionSignatureOptional = Optional.ofNullable(sibsPayReturnCheckout.getTransactionSignature());

            } catch (Exception e) {
                boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

                FenixFramework.atomic(() -> {

                    mbwayRequest.anull();

                    log.logRequestReceiveDateAndData(null, false, false, null, null);
                    log.logException(e);

                    if (isOnlinePaymentsGatewayException) {
                        OnlinePaymentsGatewayCommunicationException onlineException =
                                (OnlinePaymentsGatewayCommunicationException) e;
                        log.saveRequest(onlineException.getRequestLog());
                        log.saveResponse(onlineException.getResponseLog());
                    }
                });

                if (e instanceof TreasuryDomainException) {
                    throw (TreasuryDomainException) e;
                } else {
                    final String message = "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor."
                            + (isOnlinePaymentsGatewayException ? "gateway.communication" : "unknown");

                    throw new TreasuryDomainException(e, message);
                }
            }
        }

        // 2. generate mbway payment request
        {
            PaymentRequestLog log = log(mbwayRequest, "createMbwayRequest", null, null, null, null);

            try {

                if (transactionIdOptional.isEmpty() || StringUtils.isEmpty(transactionIdOptional.get())) {
                    throw new TreasuryDomainException("error.SibsPayPlatform.transactionId.required.to.generate.reference");
                }

                if (transactionSignatureOptional.isEmpty() || StringUtils.isEmpty(transactionSignatureOptional.get())) {
                    throw new TreasuryDomainException(
                            "error.SibsPayPlatform.transactionSignature.required.to.generate.mbway.request");
                }

                String transactionId = transactionIdOptional.get();
                String transactionSignature = transactionSignatureOptional.get();

                SibsPayResponseInquiryWrapper responseInquiryWrapper = sibsPayService
                        .generateMbwayRequestTransaction(transactionId, transactionSignature, countryPrefix, localPhoneNumber);

                FenixFramework.atomic(() -> {
                    log.logRequestReceiveDateAndData(transactionId, responseInquiryWrapper.isOperationSuccess(), false,
                            responseInquiryWrapper.getOperationStatusCode(),
                            responseInquiryWrapper.getOperationStatusDescription());

                    log.saveRequest(responseInquiryWrapper.getRequestLog());
                    log.saveResponse(responseInquiryWrapper.getResponseLog());
                });

                if (!responseInquiryWrapper.isOperationSuccess()) {
                    throw new TreasuryDomainException("error.MbwayPaymentRequest.request.in.gateway.failed");
                }

                return mbwayRequest;
            } catch (Exception e) {
                boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

                FenixFramework.atomic(() -> {
                    mbwayRequest.anull();

                    log.logRequestReceiveDateAndData(null, false, false, null, null);
                    log.logException(e);

                    if (isOnlinePaymentsGatewayException) {
                        OnlinePaymentsGatewayCommunicationException onlineException =
                                (OnlinePaymentsGatewayCommunicationException) e;
                        log.saveRequest(onlineException.getRequestLog());
                        log.saveResponse(onlineException.getResponseLog());
                    }
                });

                if (e instanceof TreasuryDomainException) {
                    throw (TreasuryDomainException) e;
                } else {
                    final String message = "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor."
                            + (isOnlinePaymentsGatewayException ? "gateway.communication" : "unknown");

                    throw new TreasuryDomainException(e, message);
                }
            }
        }

    }

    @Override
    @Atomic(mode = TxMode.READ)
    public ForwardPaymentRequest createForwardPaymentRequest(SettlementNoteBean bean,
            Function<ForwardPaymentRequest, String> successUrlFunction,
            Function<ForwardPaymentRequest, String> insuccessUrlFunction) {

        Set<DebitEntry> debitEntries =
                bean.getIncludedInvoiceEntryBeans().stream().map(ISettlementInvoiceEntryBean::getInvoiceEntry)
                        .filter(i -> i != null).map(DebitEntry.class::cast).collect(Collectors.toSet());

        Set<Installment> installments =
                bean.getIncludedInvoiceEntryBeans().stream().filter(i -> i instanceof InstallmentPaymenPlanBean && i.isIncluded())
                        .map(InstallmentPaymenPlanBean.class::cast).map(ib -> ib.getInstallment()).collect(Collectors.toSet());

        ForwardPaymentRequest forwardPayment = null;
        try {
            forwardPayment = FenixFramework
                    .atomic(() -> ForwardPaymentRequest.create(bean.getDigitalPaymentPlatform(), bean.getDebtAccount(),
                            debitEntries, installments, bean.getTotalAmountToPay(), successUrlFunction, insuccessUrlFunction));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        prepareCheckout(bean, forwardPayment);

        return forwardPayment;
    }

    @Atomic(mode = TxMode.READ)
    public void prepareCheckout(SettlementNoteBean bean, ForwardPaymentRequest forwardPayment) {
        String merchantTransactionId = generateNewMerchantTransactionId();

        FenixFramework.atomic(() -> {
            if (!StringUtils.isEmpty(forwardPayment.getMerchantTransactionId())) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayForwardImplementation.sibsMerchantTransactionId.already.filled");
            }

            forwardPayment.setMerchantTransactionId(merchantTransactionId);
        });

        try {
            DateTime requestSendDate = new DateTime();

            SibsPayAPIService sibsPayService = new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(),
                    getBearerToken(), getTerminalId(), getEntityReferenceCode());

            final SibsPayReturnCheckout returnCheckout =
                    sibsPayService.processForwardPaymentCheckout(forwardPayment, bean.getAddressBean());

            DateTime requestReceiveDate = new DateTime();

            boolean isOperationSuccess = SibsPayAPIService.isOperationSuccess(returnCheckout.getReturnStatus().getStatusCode());
            final ForwardPaymentStateType stateType =
                    isOperationSuccess ? ForwardPaymentStateType.REQUESTED : ForwardPaymentStateType.REJECTED;

            final ForwardPaymentStatusBean result = new ForwardPaymentStatusBean(isOperationSuccess, stateType,
                    returnCheckout.getReturnStatus().getStatusCode(), returnCheckout.getReturnStatus().getStatusDescription(),
                    returnCheckout.getRequestLog(), returnCheckout.getResponseLog());

            FenixFramework.atomic(() -> {
                forwardPayment.setTransactionId(returnCheckout.getTransactionID());
                forwardPayment.setFormContext(returnCheckout.getFormContext());

                PaymentRequestLog log = log(forwardPayment, "createForwardPaymentRequest", result.getStatusCode(),
                        result.getStatusMessage(), result.getRequestBody(), result.getResponseBody());

                log.setInternalMerchantTransactionId(forwardPayment.getMerchantTransactionId());
                log.setExternalTransactionId(forwardPayment.getTransactionId());
                log.setRequestSendDate(requestSendDate);
                log.setRequestReceiveDate(requestReceiveDate);
                log.setOperationSuccess(result.isOperationSuccess());

                if (result.getStateType() == ForwardPaymentStateType.REQUESTED) {
                    forwardPayment.advanceToRequestState();
                } else {
                    forwardPayment.reject();
                }
            });

        } catch (final Exception e) {
            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                logException(forwardPayment, e, "createForwardPaymentRequest", "error", "error", requestBody, responseBody);
                forwardPayment.reject();
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Override
    @Atomic(mode = TxMode.READ)
    public PostProcessPaymentStatusBean postProcessPayment(ForwardPaymentRequest forwardPayment, String justification,
            Optional<String> specificTransactionId) {
        if (specificTransactionId.isEmpty()) {
            return null;
        }

        try {
            SibsPayAPIService sibsPayService = new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(),
                    getBearerToken(), getTerminalId(), getEntityReferenceCode());

            DateTime requestSendDate = new DateTime();

            SibsPayResponseInquiryWrapper responseInquiryWrapper =
                    sibsPayService.getPaymentStatusBySibsTransactionId(specificTransactionId.get());

            DateTime requestReceiveDate = new DateTime();

            String requestLog = responseInquiryWrapper.getRequestLog();
            String responseLog = responseInquiryWrapper.getResponseLog();

            if (!responseInquiryWrapper.isOperationSuccess()) {
                throw new OnlinePaymentsGatewayCommunicationException(requestLog, responseLog,
                        String.format("%s - %s", responseInquiryWrapper.getOperationStatusMessage(),
                                responseInquiryWrapper.getOperationStatusDescription()));
            }

            ForwardPaymentStateType type = translateForwardPaymentStateType(responseInquiryWrapper);

            final ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(responseInquiryWrapper.isOperationSuccess(), type,
                    responseInquiryWrapper.getPaymentResultCode(), responseInquiryWrapper.getPaymentResultDescription(),
                    requestLog, responseLog);

            // README ANIL 2023-10-23: 
            // 
            // There is no payment date or transaction date, return by the API of SIBS
            // According to SIBS Onboarding team, the payment date in a credit card is considered
            // to be the date when the transaction is queried with the SIBS API. But the transaction
            // might be queried after, which will not give consistent results.
            //
            // We will assume that the payment date is the request date of the forward payment, which
            // is a reasonable assumption.

            bean.editTransactionDetails(responseInquiryWrapper.getTransactionId(), forwardPayment.getRequestDate(),
                    responseInquiryWrapper.getAmount());

            if (List.of(ForwardPaymentStateType.CREATED, ForwardPaymentStateType.REQUESTED).contains(bean.getStateType())) {
                // Do nothing
                return new PostProcessPaymentStatusBean(bean, forwardPayment.getState(), false);
            }

            PostProcessPaymentStatusBean returnBean =
                    new PostProcessPaymentStatusBean(bean, forwardPayment.getState(), bean.isInPayedState());

            FenixFramework.atomic(() -> {
                PaymentRequestLog log = log(forwardPayment, "postProcessPayment", bean.getStatusCode(), bean.getStatusMessage(),
                        requestLog, responseLog);

                log.setInternalMerchantTransactionId(forwardPayment.getMerchantTransactionId());
                log.setExternalTransactionId(bean.getTransactionId());
                log.setRequestSendDate(requestSendDate);
                log.setRequestReceiveDate(requestReceiveDate);
                log.setOperationSuccess(true);
                log.setTransactionWithPayment(bean.isInPayedState());

                if (!forwardPayment.isInRequestedState()) {
                    // Most probably this forward payment was processed in the webhook
                    // Nothing to process, just return
                    return;
                }

                if (bean.isInPayedState()) {
                    forwardPayment.advanceToPaidState(bean.getStatusCode(), bean.getPayedAmount(), bean.getTransactionDate(),
                            bean.getTransactionId(), justification);
                } else if (bean.isInRejectedState()) {
                    forwardPayment.reject();
                }
            });

            return returnBean;
        } catch (final Exception e) {
            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                logException(forwardPayment, e, "postProcessPayment", "error", "error", requestBody, responseBody);
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Override
    public PostProcessPaymentStatusBean processForwardPayment(ForwardPaymentRequest forwardPayment) {
        return postProcessPayment(forwardPayment, "", Optional.of(forwardPayment.getTransactionId()));
    }

    @Override
    public PostProcessPaymentStatusBean processForwardPaymentFromWebhook(PaymentRequestLog log, DigitalPlatformResultBean bean) {
        ForwardPaymentRequest forwardPaymentRequest = (ForwardPaymentRequest) log.getPaymentRequest();

        return postProcessPayment(forwardPaymentRequest, "", Optional.of(bean.getTransactionId()));
    }

    public static final String CONTROLLER_URL = "/treasury/document/forwardpayments/sibspayplatform";
    private static final String RETURN_FORWARD_PAYMENT_URL = CONTROLLER_URL + "/returnforwardpayment";

    public String getReturnURL(final ForwardPaymentRequest forwardPayment) {
        String forwardPaymentReturnDefaultURL = TreasurySettings.getInstance().getForwardPaymentReturnDefaultURL();
        return String.format("%s/%s/%s", forwardPaymentReturnDefaultURL, RETURN_FORWARD_PAYMENT_URL,
                forwardPayment.getExternalId());
    }

    public String generateNewMerchantTransactionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public String getPaymentURL(ForwardPaymentRequest request) {
        return new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                getEntityReferenceCode()).getJsScriptURL(request.getTransactionId());
    }

    @Override
    public String getLogosJspPage() {
        return null;
    }

    @Override
    public ForwardPaymentStatusBean paymentStatus(ForwardPaymentRequest request) {
        SibsPayAPIService sibsPayService = new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(),
                getBearerToken(), getTerminalId(), getEntityReferenceCode());

        try {
            SibsPayResponseInquiryWrapper responseInquiryWrapper = null;
            if (!StringUtils.isEmpty(request.getTransactionId())) {
                responseInquiryWrapper = sibsPayService.getPaymentStatusBySibsTransactionId(request.getTransactionId());
            } else {
                responseInquiryWrapper =
                        sibsPayService.getPaymentStatusByMerchantTransactionId(request.getMerchantTransactionId());
            }

            final String requestLog = responseInquiryWrapper.getRequestLog();
            final String responseLog = responseInquiryWrapper.getResponseLog();

            final ForwardPaymentStateType paymentStateType = translateForwardPaymentStateType(responseInquiryWrapper);

            final ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(responseInquiryWrapper.isOperationSuccess(),
                    paymentStateType, responseInquiryWrapper.getPaymentResultCode(),
                    responseInquiryWrapper.getPaymentResultDescription(), requestLog, responseLog);

            // README ANIL 2023-10-23: 
            // 
            // There is no payment date or transaction date, return by the API of SIBS
            // According to SIBS Onboarding team, the payment date in a credit card is considered
            // to be the date when the transaction is queried with the SIBS API. But the transaction
            // might be queried after, which will not give consistent results.
            //
            // We will assume that the payment date is the request date of the forward payment, which
            // is a reasonable assumption.

            bean.editTransactionDetails(responseInquiryWrapper.getTransactionId(), request.getRequestDate(),
                    responseInquiryWrapper.getAmount());

            return bean;
        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                logException(request, e, "paymentStatus", "error", "error", requestBody, responseBody);
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.gateway.communication");
        }
    }

    private ForwardPaymentStateType translateForwardPaymentStateType(SibsPayResponseInquiryWrapper responseInquiryWrapper) {
        if (responseInquiryWrapper.isPaid()) {
            return ForwardPaymentStateType.PAYED;
        } else if (responseInquiryWrapper.isPending()) {
            return ForwardPaymentStateType.REQUESTED;
        }

        return ForwardPaymentStateType.REJECTED;
    }

    @Override
    public SibsPaymentRequest createSibsPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments) {

        BigDecimal payableAmountDebitEntries =
                debitEntries.stream().map(DebitEntry::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(Installment::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        LocalDate validTo =
                getSibsPaymentExpiryStrategy().calculateSibsPaymentRequestExpiryDate(debitEntries, installments, false, null);

        return createSibsPaymentRequest(debtAccount, debitEntries, installments, validTo, payableAmount);
    }

    @Override
    public SibsPaymentRequest createSibsPaymentRequest(SettlementNoteBean settlementNoteBean) {
        DebtAccount debtAccount = settlementNoteBean.getDebtAccount();
        Set<DebitEntry> debitEntries =
                settlementNoteBean.getIncludedInvoiceEntryBeans().stream().filter(s -> s.getInvoiceEntry() != null)
                        .map(s -> s.getInvoiceEntry()).map(DebitEntry.class::cast).collect(Collectors.toSet());
        Set<Installment> installments =
                settlementNoteBean.getIncludedInvoiceEntryBeans().stream().filter(s -> s.isForInstallment())
                        .map(InstallmentPaymenPlanBean.class::cast).map(s -> s.getInstallment()).collect(Collectors.toSet());

        BigDecimal payableAmount = settlementNoteBean.getTotalAmountToPay();

        LocalDate validTo = getSibsPaymentExpiryStrategy().calculateSibsPaymentRequestExpiryDate(debitEntries, installments,
                settlementNoteBean.isLimitSibsPaymentRequestToCustomDueDate(),
                settlementNoteBean.getCustomSibsPaymentRequestDueDate());

        return createSibsPaymentRequest(debtAccount, debitEntries, installments, validTo, payableAmount);
    }

    @Override
    @Deprecated
    // TODO: Only used by PaymentReferenceCodeController.createPaymentCodeForSeveralDebitEntries() method. Replace with settlement note bean
    public SibsPaymentRequest createSibsPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, BigDecimal payableAmount) {

        LocalDate validTo =
                getSibsPaymentExpiryStrategy().calculateSibsPaymentRequestExpiryDate(debitEntries, installments, false, null);

        return createSibsPaymentRequest(debtAccount, debitEntries, installments, validTo, payableAmount);
    }

    @Override
    public SibsPaymentRequest createSibsPaymentRequestWithInterests(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, LocalDate interestsCalculationDate) {
        BigDecimal payableAmountDebitEntries = debitEntries.stream()
                .map(d -> d.getOpenAmountWithInterestsAtDate(interestsCalculationDate)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(Installment::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        LocalDate validTo =
                getSibsPaymentExpiryStrategy().calculateSibsPaymentRequestExpiryDate(debitEntries, installments, false, null);

        return createSibsPaymentRequest(debtAccount, debitEntries, installments, validTo, payableAmount);
    }

    private SibsPaymentRequest createSibsPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, LocalDate validTo, BigDecimal payableAmount) {
        if (!isActive()) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.paymentCodePool.not.active");
        }

        if (PaymentRequest.getReferencedCustomers(debitEntries, installments).size() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.referencedCustomers.only.one.allowed");
        }

        String merchantTransactionId = generateNewMerchantTransactionId();

        Optional<String> transactionIdOptional = Optional.empty();
        Optional<String> transactionSignatureOptional = Optional.empty();

        SibsPayAPIService sibsPayService = new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(),
                getBearerToken(), getTerminalId(), getEntityReferenceCode());
        DateTime sibsValidFrom = new DateTime();

        // Set validTo to 23:59:59
        DateTime sibsValidTo = validTo.plusDays(1).toDateTimeAtStartOfDay().minusSeconds(1);

        List<PaymentRequestLog> logsList = new ArrayList<>();

        // 1. Prepare checkout for payment reference codes
        {
            final PaymentRequestLog log = createLogForSibsPaymentRequest(merchantTransactionId);
            logsList.add(log);

            try {

                SibsPayReturnCheckout sibsPayReturnCheckout = sibsPayService.processSibsPaymentRequestOrMbwayCheckout(debtAccount,
                        payableAmount, sibsValidFrom, sibsValidTo, merchantTransactionId);

                boolean isOperationSuccess =
                        SibsPayAPIService.isOperationSuccess(sibsPayReturnCheckout.getReturnStatus().getStatusCode());

                FenixFramework.atomic(() -> {
                    log.logRequestReceiveDateAndData(sibsPayReturnCheckout.getTransactionID(), isOperationSuccess, false,
                            sibsPayReturnCheckout.getReturnStatus().getStatusCode(),
                            sibsPayReturnCheckout.getReturnStatus().getStatusDescription());

                    log.saveRequest(sibsPayReturnCheckout.getRequestLog());
                    log.saveResponse(sibsPayReturnCheckout.getResponseLog());
                });

                if (!isOperationSuccess) {
                    throw new TreasuryDomainException(
                            "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.request.not.successful");
                }

                transactionIdOptional = Optional.ofNullable(sibsPayReturnCheckout.getTransactionID());
                transactionSignatureOptional = Optional.ofNullable(sibsPayReturnCheckout.getTransactionSignature());

            } catch (final Exception e) {
                final boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

                saveExceptionLog(log, e, isOnlinePaymentsGatewayException);

                if (e instanceof TreasuryDomainException) {
                    throw (TreasuryDomainException) e;
                } else {
                    String message = "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.unknown";
                    if (isOnlinePaymentsGatewayException) {
                        message = "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.gateway.communication";
                    }

                    throw new TreasuryDomainException(e, message);
                }
            }
        }

        // 2. Generate payment reference
        {
            final PaymentRequestLog log = createLogForSibsPaymentRequest(merchantTransactionId);
            logsList.add(log);

            try {

                if (transactionIdOptional.isEmpty() || StringUtils.isEmpty(transactionIdOptional.get())) {
                    throw new TreasuryDomainException("error.SibsPayPlatform.transactionId.required.to.generate.reference");
                }

                if (transactionSignatureOptional.isEmpty() || StringUtils.isEmpty(transactionSignatureOptional.get())) {
                    throw new TreasuryDomainException(
                            "error.SibsPayPlatform.transactionSignature.required.to.generate.reference");
                }

                String transactionId = transactionIdOptional.get();
                String transactionSignature = transactionSignatureOptional.get();

                SibsPayResponseInquiryWrapper responseInquiryWrapper =
                        sibsPayService.generateSibsPaymentRequestTransaction(transactionId, transactionSignature);

                FenixFramework.atomic(() -> {
                    log.logRequestReceiveDateAndData(transactionId, responseInquiryWrapper.isOperationSuccess(), false,
                            responseInquiryWrapper.getOperationStatusCode(),
                            responseInquiryWrapper.getOperationStatusDescription());

                    log.saveRequest(responseInquiryWrapper.getRequestLog());
                    log.saveResponse(responseInquiryWrapper.getResponseLog());
                });

                if (!responseInquiryWrapper.isOperationSuccess()) {
                    throw new TreasuryDomainException(
                            "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.request.not.successful");
                }

                final String referenceCode = responseInquiryWrapper.getReferenceCode();

                if (StringUtils.isEmpty(referenceCode)) {
                    throw new TreasuryDomainException(
                            "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.reference.not.empty");
                }

                if (SibsReferenceCode.findByReferenceCode(getEntityReferenceCode(), referenceCode).count() >= 1) {
                    throw new TreasuryDomainException("error.PaymentReferenceCode.referenceCode.duplicated");
                }

                if (PaymentRequest.findBySibsGatewayTransactionId(transactionId).count() >= 1) {
                    throw new TreasuryDomainException("error.PaymentReferenceCode.sibsReferenceId.found.duplicated");
                }

                SibsPaymentRequest sibsPaymentRequest = FenixFramework.atomic(() -> {
                    SibsPaymentRequest request = SibsPaymentRequest.create(this, debtAccount, debitEntries, installments,
                            payableAmount, getEntityReferenceCode(), referenceCode, merchantTransactionId, transactionId);

                    request.setPaymentDueDate(validTo);
                    request.setExpiresDate(sibsValidTo);

                    logsList.forEach(l -> l.setPaymentRequest(request));

                    return request;
                });

                return sibsPaymentRequest;

            } catch (final Exception e) {
                final boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

                saveExceptionLog(log, e, isOnlinePaymentsGatewayException);

                if (e instanceof TreasuryDomainException) {
                    throw (TreasuryDomainException) e;
                } else {
                    String message = "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.unknown";
                    if (isOnlinePaymentsGatewayException) {
                        message = "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.gateway.communication";
                    }

                    throw new TreasuryDomainException(e, message);
                }
            }
        }

    }

    @Atomic(mode = TxMode.WRITE)
    private void saveExceptionLog(PaymentRequestLog log, final Exception e, final boolean isOnlinePaymentsGatewayException) {
        log.logException(e);

        if (isOnlinePaymentsGatewayException) {
            log.saveRequest(((OnlinePaymentsGatewayCommunicationException) e).getRequestLog());
            log.saveResponse(((OnlinePaymentsGatewayCommunicationException) e).getResponseLog());
        }
    }

    @Override
    public PaymentTransaction processPaymentReferenceCodeTransaction(PaymentRequestLog log, DigitalPlatformResultBean bean) {
        SibsPaymentRequest paymentRequest = (SibsPaymentRequest) log.getPaymentRequest();
        if (!bean.getTransactionId().equals(paymentRequest.getTransactionId())) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.transactionId.not.equal");
        }

        final BigDecimal paidAmount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate() != null ? bean.getPaymentDate() : DateTime.now();

        FenixFramework.atomic(() -> {
            log.savePaymentTypeAndBrand(bean.getPaymentType(), bean.getPaymentBrand());
            log.savePaymentInfo(paidAmount, paymentDate);
        });

        if (paidAmount == null || !TreasuryConstants.isPositive(paidAmount)) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.payment.date");
        }

        String entityReferenceCode = this.getEntityReferenceCode();
        if (SibsPaymentCodeTransaction.isReferenceProcessingDuplicate(entityReferenceCode, paymentRequest.getReferenceCode(),
                paymentDate)) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }

        if (PaymentTransaction.isTransactionDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }
        return paymentRequest.processPayment(paidAmount, paymentDate, bean.getTransactionId(), null,
                bean.getMerchantTransactionId(), new DateTime(), null, true);
    }

    @Override
    public List<? extends DigitalPlatformResultBean> getPaymentTransactionsReportListByMerchantId(String merchantTransationId) {
        SibsPayAPIService sibsPayService = new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(),
                getBearerToken(), getTerminalId(), getEntityReferenceCode());

        try {
            SibsPayResponseInquiryWrapper responseInquiryWrapper =
                    sibsPayService.getPaymentStatusByMerchantTransactionId(merchantTransationId);

            return List.of(responseInquiryWrapper);
        } catch (final Exception e) {
            FenixFramework.atomic(() -> {
                PaymentRequestLog log =
                        PaymentRequestLog.create(null, "getPaymentTransactionsReportListByMerchantId", null, null);

                log.setInternalMerchantTransactionId(merchantTransationId);
                log.logException(e);
                log.setOperationSuccess(false);

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    log.saveRequest(((OnlinePaymentsGatewayCommunicationException) e).getRequestLog());
                    log.saveResponse(((OnlinePaymentsGatewayCommunicationException) e).getResponseLog());
                }
            });

            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Override
    @Deprecated
    public PaymentRequestLog createLogForWebhookNotification() {
        throw new RuntimeException("deprecated");
    }

    @Override
    public void fillLogForWebhookNotification(PaymentRequestLog log, DigitalPlatformResultBean bean) {
        // TODO Auto-generated method stub

    }

    public void rejectRequest(PaymentRequest paymentRequest, PaymentRequestLog log,
            SibsPayWebhookNotificationWrapper webhookNotificationWrapper) {
        if (paymentRequest instanceof ForwardPaymentRequest) {
            ((ForwardPaymentRequest) paymentRequest).reject();
        } else if (paymentRequest instanceof SibsPaymentRequest) {
            ((SibsPaymentRequest) paymentRequest).anull();
        } else if (paymentRequest instanceof MbwayRequest) {
            ((MbwayRequest) paymentRequest).anull();
        } else {
            throw new RuntimeException("unknown payment request type");
        }
    }

    public void delete() {
        super.delete();
        super.deleteDomainObject();
    }

    @Override
    public boolean annulPaymentRequestInPlatform(SibsPaymentRequest sibsPaymentRequest) {
        SibsPayAPIService sibsPayService = new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(),
                getBearerToken(), getTerminalId(), getEntityReferenceCode());

        try {
            // 1. First check the state of the sibsPaymentRequest in the platform
            SibsPayResponseInquiryWrapper responseInquiryWrapper =
                    sibsPayService.getPaymentStatusBySibsTransactionId(sibsPaymentRequest.getTransactionId());

            if (responseInquiryWrapper == null) {
                throw new IllegalStateException("unable to check the payment request status");
            }

            if (responseInquiryWrapper.isDeclined()) {
                // Just remove from the pending for annulment
                FenixFramework.atomic(() -> sibsPaymentRequest.setDigitalPaymentPlatformPendingForAnnulment(null));

                log(sibsPaymentRequest, "annulPaymentRequestInPlatform", "DECLINED_REMOVE_FROM_PENDING",
                        "declined, remove from pending", "", "");

                return true;
            } else if (responseInquiryWrapper.isExpired()) {
                // Just remove from the pending for annulment
                FenixFramework.atomic(() -> sibsPaymentRequest.setDigitalPaymentPlatformPendingForAnnulment(null));

                log(sibsPaymentRequest, "annulPaymentRequestInPlatform", "EXPIRED_REMOVE_FROM_PENDING",
                        "expired, remove from pending", "", "");
                return true;
            } else if (responseInquiryWrapper.isPaid()) {
                if (!sibsPaymentRequest.getPaymentTransactionsSet().isEmpty()) {
                    // The payment is processed, just remove from the pending for annulment
                    FenixFramework.atomic(() -> sibsPaymentRequest.setDigitalPaymentPlatformPendingForAnnulment(null));

                    log(sibsPaymentRequest, "annulPaymentRequestInPlatform", "PAID_REMOVE_FROM_PENDING",
                            "paid, remove from pending", "", "");

                    return true;
                } else {
                    // Do not remove and report that it was not processed

                    log(sibsPaymentRequest, "annulPaymentRequestInPlatform", "PAID_CHECK", "paid, check pending payment", "", "");

                    return false;
                }
            } else if (responseInquiryWrapper.isPending()) {
                // Annul in the platform

                SibsPayCancellationResponse cancellationResponse =
                        sibsPayService.cancelTransaction(sibsPaymentRequest.getMerchantTransactionId(),
                                sibsPaymentRequest.getTransactionId(), sibsPaymentRequest.getPayableAmount());

                cancellationResponse.getReturnStatus().getStatusCode();

                String statusCode = cancellationResponse.getReturnStatus() != null ? cancellationResponse.getReturnStatus()
                        .getStatusCode() : "";
                String statusMessage = cancellationResponse.getReturnStatus() != null ? cancellationResponse.getReturnStatus()
                        .getStatusDescription() : "";

                log(sibsPaymentRequest, "annulPaymentRequestInPlatform", statusCode, statusMessage,
                        cancellationResponse.getRequestLog(), cancellationResponse.getResponseLog());

                FenixFramework.atomic(() -> sibsPaymentRequest.setDigitalPaymentPlatformPendingForAnnulment(null));

                return true;
            }

        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                logException(sibsPaymentRequest, e, "annulPaymentRequestInPlatform", "error", "error", requestBody, responseBody);
            });

            throw new TreasuryDomainException(e, "error.SibsPayPlatform.annulPaymentRequestInPlatform");
        }

        return false;
    }

    @Atomic(mode = TxMode.WRITE)
    private static PaymentRequestLog createLogForSibsPaymentRequest(String merchantTransactionId) {
        PaymentRequestLog log = PaymentRequestLog.create(null, "createSibsPaymentRequest",
                PaymentReferenceCodeStateType.UNUSED.getCode(), PaymentReferenceCodeStateType.UNUSED.getDescriptionI18N());

        log.setInternalMerchantTransactionId(merchantTransactionId);

        return log;
    }

    public static String getPresentationName() {
        return "SIBS Pay (SPG)";
    }

    public static SibsPayPlatform create(FinantialInstitution finantialInstitution, String name, boolean active, String clientId,
            String bearerToken, Integer terminalId, String entityReferenceCode, String endpointUrl, String assetsEndpointUrl) {
        return new SibsPayPlatform(finantialInstitution, name, active, clientId, bearerToken, terminalId, entityReferenceCode,
                endpointUrl, assetsEndpointUrl);
    }

    public static Stream<SibsPayPlatform> findAllActive() {
        return DigitalPaymentPlatform.findAll().filter(p -> p instanceof SibsPayPlatform).filter(p -> p.isActive())
                .map(SibsPayPlatform.class::cast);
    }

}
