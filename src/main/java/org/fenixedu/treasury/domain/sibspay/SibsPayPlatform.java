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
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
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
import org.fenixedu.treasury.dto.sibspay.MbwayMandateBean;
import org.fenixedu.treasury.services.payments.sibspay.SibsPayAPIService;
import org.fenixedu.treasury.services.payments.sibspay.model.*;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.Duration;
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

    public SibsPayPlatform(FinantialInstitution finantialInstitution, FinantialEntity finantialEntity, String name,
            boolean active, String clientId, String bearerToken, Integer terminalId, String entityReferenceCode, String endpoint,
            String assetsEndpointUrl) {
        this();

        super.init(finantialInstitution, finantialEntity, name, active);

        super.setClientId(clientId);
        super.setTerminalId(terminalId);
        super.setBearerToken(bearerToken);
        super.setEntityReferenceCode(entityReferenceCode);
        super.setEndpointUrl(endpoint);
        super.setAssetsEndpointUrl(assetsEndpointUrl);

        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getCreditCardPaymentMethod());
        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getMbPaymentMethod());
        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getMbWayPaymentMethod());

        checkRules();
    }

    @Override
    public boolean isMbwayMandateSupported() {
        return true;
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

        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());

        MbwayRequest mbwayRequest = MbwayRequest.create(this, debtAccount, debitEntries, installments, phoneNumber, payableAmount,
                merchantTransactionId);

        // 1. Request checkout
        {
            PaymentRequestLog log = log(mbwayRequest, "createMbwayRequest", null, null, null, null);
            try {

                DateTime requestSendDate = new DateTime();

                SibsPayReturnCheckout sibsPayReturnCheckout =
                        sibsPayService.processSibsPaymentRequestOrMbwayCheckout(debtAccount, payableAmount, new DateTime(),
                                new DateTime().plusMinutes(3), merchantTransactionId);

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
                    final String message =
                            "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor." + (isOnlinePaymentsGatewayException ? "gateway.communication" : "unknown");

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

                SibsPayResponseInquiryWrapper responseInquiryWrapper =
                        sibsPayService.generateMbwayRequestTransaction(transactionId, transactionSignature, countryPrefix,
                                localPhoneNumber);

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
                    final String message =
                            "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor." + (isOnlinePaymentsGatewayException ? "gateway.communication" : "unknown");

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
            forwardPayment = FenixFramework.atomic(
                    () -> ForwardPaymentRequest.create(bean.getDigitalPaymentPlatform(), bean.getDebtAccount(), debitEntries,
                            installments, bean.getTotalAmountToPay(), successUrlFunction, insuccessUrlFunction));
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

            SibsPayAPIService sibsPayService =
                    new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(),
                            getTerminalId(), getEntityReferenceCode());

            final SibsPayReturnCheckout returnCheckout =
                    sibsPayService.processForwardPaymentCheckout(forwardPayment, bean.getAddressBean());

            DateTime requestReceiveDate = new DateTime();

            boolean isOperationSuccess = SibsPayAPIService.isOperationSuccess(returnCheckout.getReturnStatus().getStatusCode());
            final ForwardPaymentStateType stateType =
                    isOperationSuccess ? ForwardPaymentStateType.REQUESTED : ForwardPaymentStateType.REJECTED;

            final ForwardPaymentStatusBean result =
                    new ForwardPaymentStatusBean(isOperationSuccess, stateType, returnCheckout.getReturnStatus().getStatusCode(),
                            returnCheckout.getReturnStatus().getStatusDescription(), returnCheckout.getRequestLog(),
                            returnCheckout.getResponseLog());

            FenixFramework.atomic(() -> {
                forwardPayment.setTransactionId(returnCheckout.getTransactionID());
                forwardPayment.setFormContext(returnCheckout.getFormContext());

                PaymentRequestLog log =
                        log(forwardPayment, "createForwardPaymentRequest", result.getStatusCode(), result.getStatusMessage(),
                                result.getRequestBody(), result.getResponseBody());

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
            SibsPayAPIService sibsPayService =
                    new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(),
                            getTerminalId(), getEntityReferenceCode());

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
                PaymentRequestLog log =
                        log(forwardPayment, "postProcessPayment", bean.getStatusCode(), bean.getStatusMessage(), requestLog,
                                responseLog);

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
        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());

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

            final ForwardPaymentStatusBean bean =
                    new ForwardPaymentStatusBean(responseInquiryWrapper.isOperationSuccess(), paymentStateType,
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
        BigDecimal payableAmountDebitEntries =
                debitEntries.stream().map(d -> d.getOpenAmountWithInterestsAtDate(interestsCalculationDate))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
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

        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());
        DateTime sibsValidFrom = new DateTime();

        // Set validTo to 23:59:59
        DateTime sibsValidTo = calculateSibsValidTo(sibsValidFrom, validTo);

        List<PaymentRequestLog> logsList = new ArrayList<>();

        // 1. Prepare checkout for payment reference codes
        {
            final PaymentRequestLog log = createLogForSibsPaymentRequest(merchantTransactionId);
            logsList.add(log);

            try {

                SibsPayReturnCheckout sibsPayReturnCheckout =
                        sibsPayService.processSibsPaymentRequestOrMbwayCheckout(debtAccount, payableAmount, sibsValidFrom,
                                sibsValidTo, merchantTransactionId);

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
                    SibsPaymentRequest request =
                            SibsPaymentRequest.create(this, debtAccount, debitEntries, installments, payableAmount,
                                    getEntityReferenceCode(), referenceCode, merchantTransactionId, transactionId);

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

    private DateTime calculateSibsValidTo(DateTime sibsValidFrom, LocalDate validTo) {
        DateTime sibsValidTo = validTo.plusDays(1).toDateTimeAtStartOfDay().minusSeconds(1);

        // ANIL 2024-07-16
        //
        // If the period between sibsValidFrom and sibsValidTo, is only one hour, 
        // give a little more time to not return  error from SIBS

        if (getReferenceMinimumValidityInHours() != null && getReferenceMinimumValidityInHours() > 0) {
            Duration duration = new Duration(sibsValidFrom, sibsValidTo);

            if (duration.getStandardHours() < getReferenceMinimumValidityInHours()) {
                int incrementHours = ((int) getReferenceMinimumValidityInHours()) - (int) duration.getStandardHours();

                sibsValidTo = sibsValidTo.plusHours(incrementHours);
            }
        }

        return sibsValidTo;
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
        final DateTime paymentDate = bean.getPaymentDate() != null ? bean.getPaymentDate() : DateTime.now();

        return processPaymentReferenceCodeTransaction(log, bean, paymentDate);
    }

    // ANIL 2024-10-31 (#qubIT-Fenix-6029)
    //
    // Extends this method to receive a custom payment date
    //
    // Unfortunately SIBS SPG API does not respond with the real payment date. The payment date
    // is only transmitted through the webhook notification. So if the operator need to 
    // register the payment throught the consultation of SIBS SPG API, then he must enter the 
    // real payment date. Apparently this real payment date can be consulted in SIBS Backoffice Portal
    // or can be considered the date of the first webhook notification attempt by SIBS
    public PaymentTransaction processPaymentReferenceCodeTransaction(final PaymentRequestLog log, DigitalPlatformResultBean bean,
            DateTime customPaymentDate) {
        SibsPaymentRequest paymentRequest = (SibsPaymentRequest) log.getPaymentRequest();
        if (!bean.getTransactionId().equals(paymentRequest.getTransactionId())) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.transactionId.not.equal");
        }

        final BigDecimal paidAmount = bean.getAmount();

        FenixFramework.atomic(() -> {
            log.savePaymentTypeAndBrand(bean.getPaymentType(), bean.getPaymentBrand());
            log.savePaymentInfo(paidAmount, customPaymentDate);
        });

        if (paidAmount == null || !TreasuryConstants.isPositive(paidAmount)) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.amount");
        }

        if (customPaymentDate == null) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.payment.date");
        }

        String entityReferenceCode = this.getEntityReferenceCode();
        if (SibsPaymentCodeTransaction.isReferenceProcessingDuplicate(entityReferenceCode, paymentRequest.getReferenceCode(),
                customPaymentDate)) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }

        if (PaymentTransaction.isTransactionDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }

        return paymentRequest.processPayment(paidAmount, customPaymentDate, bean.getTransactionId(), null,
                bean.getMerchantTransactionId(), new DateTime(), null, true);
    }

    @Override
    public List<? extends DigitalPlatformResultBean> getPaymentTransactionsReportListByMerchantId(String merchantTransationId) {
        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());

        try {
            SibsPayResponseInquiryWrapper responseInquiryWrapper =
                    sibsPayService.getPaymentStatusByMerchantTransactionId(merchantTransationId);

            return List.of(responseInquiryWrapper);
        } catch (final Exception e) {
            FenixFramework.atomic(() -> {
                PaymentRequestLog log =
                        PaymentRequestLog.create((PaymentRequest) null, "getPaymentTransactionsReportListByMerchantId", null,
                                null);

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
        log.setStatusCode(bean.getPaymentResultCode());
        log.setStatusMessage(bean.getPaymentResultDescription());
        log.setInternalMerchantTransactionId(bean.getMerchantTransactionId());
        log.setExternalTransactionId(bean.getTransactionId());
        log.setOperationSuccess(bean.isOperationSuccess());
        log.setTransactionWithPayment(bean.isPaid());

        if (bean instanceof SibsPayResponseInquiryWrapper) {
            log.saveRequest(((SibsPayResponseInquiryWrapper) bean).getRequestLog());
            log.saveResponse(((SibsPayResponseInquiryWrapper) bean).getResponseLog());
        }
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
        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());

        try {
            // 1. First check the state of the sibsPaymentRequest in the platform
            SibsPayResponseInquiryWrapper responseInquiryWrapper =
                    sibsPayService.getPaymentStatusBySibsTransactionId(sibsPaymentRequest.getTransactionId());

            if (responseInquiryWrapper == null) {
                throw new IllegalStateException("unable to check the payment request status");
            }

            if (responseInquiryWrapper.isDeclined()) {
                // Just remove from the pending for annulment
                FenixFramework.atomic(() -> {
                    sibsPaymentRequest.setDigitalPaymentPlatformPendingForAnnulment(null);
                    log(sibsPaymentRequest, "annulPaymentRequestInPlatform", "DECLINED_REMOVE_FROM_PENDING",
                            "declined, remove from pending", "", "").setOperationSuccess(true);
                });

                return true;
            } else if (responseInquiryWrapper.isExpired()) {
                // Just remove from the pending for annulment
                FenixFramework.atomic(() -> {
                    sibsPaymentRequest.setDigitalPaymentPlatformPendingForAnnulment(null);
                    log(sibsPaymentRequest, "annulPaymentRequestInPlatform", "EXPIRED_REMOVE_FROM_PENDING",
                            "expired, remove from pending", "", "").setOperationSuccess(true);
                });

                return true;
            } else if (responseInquiryWrapper.isPaid()) {
                if (!sibsPaymentRequest.getPaymentTransactionsSet().isEmpty()) {
                    // The payment is processed, just remove from the pending for annulment
                    FenixFramework.atomic(() -> {
                        sibsPaymentRequest.setDigitalPaymentPlatformPendingForAnnulment(null);

                        log(sibsPaymentRequest, "annulPaymentRequestInPlatform", "PAID_REMOVE_FROM_PENDING",
                                "paid, remove from pending", "", "").setOperationSuccess(true);
                    });

                    return true;
                } else {
                    // Do not remove and report that it was not processed

                    FenixFramework.atomic(() -> {
                        log(sibsPaymentRequest, "annulPaymentRequestInPlatform", "PAID_CHECK", "paid, check pending payment", "",
                                "").setOperationSuccess(false);
                    });

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

                FenixFramework.atomic(() -> {
                    sibsPaymentRequest.setDigitalPaymentPlatformPendingForAnnulment(null);
                    log(sibsPaymentRequest, "annulPaymentRequestInPlatform", statusCode, statusMessage,
                            cancellationResponse.getRequestLog(), cancellationResponse.getResponseLog()).setOperationSuccess(
                            true);

                });

                return true;
            } else {
                // Unknown state

                FenixFramework.atomic(() -> {
                    log(sibsPaymentRequest, "annulPaymentRequestInPlatform", "UNKNOWN_STATE",
                            "unknown payment result code, please check: " + responseInquiryWrapper.getPaymentResultCode(), "",
                            "").setOperationSuccess(false);
                });

                return false;
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
    }

    @Override
    public int getMaximumLengthForAddressStreetFieldOne() {
        return SibsPayAPIService.MAX_STREET_LENGTH;
    }

    @Override
    public int getMaximumLengthForAddressCity() {
        return SibsPayAPIService.MAX_CITY_LENGTH;
    }

    @Override
    public int getMaximumLengthForPostalCode() {
        return SibsPayAPIService.MAX_POSTCODE_LENGTH;
    }

    /*
     * *****************
     * SIBS PAY MANDATES
     * *****************
     */

    @Override
    public MbwayMandate requestMbwayMandateAuthorization(DebtAccount debtAccount, String countryPrefix, String localPhoneNumber) {
        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());

        String merchantTransactionId = generateNewMerchantTransactionId();

        MbwayMandate mbwayMandate = createMbwayMandateObject(debtAccount, countryPrefix, localPhoneNumber, merchantTransactionId);

        try {

            SibsPayCreateMbwayMandateResponse response =
                    sibsPayService.createMbwayMandate(debtAccount, countryPrefix, localPhoneNumber, merchantTransactionId);

            FenixFramework.atomic(() -> {
                log(mbwayMandate, "requestMbwayMandateAuthorization", response.getOperationStatusCode(),
                        response.getOperationStatusMessage(), response.getRequestLog(), response.getResponseLog());

                if (response.isMandateCreationSuccess()) {
                    String mandateId = response.getMandate() != null ? response.getMandate().getMandateId() : null;

                    mbwayMandate.waitAuthorization(mandateId, response.getTransactionId());
                } else {
                    mbwayMandate.cancel("mandate creation not successful");
                }
            });

            return mbwayMandate;
        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                // Cancel because the request was not successful
                mbwayMandate.cancel("mandate creation not successful");
                logException(mbwayMandate, e, "requestMbwayMandateAuthorization", "error", "error", requestBody, responseBody);
            });

            throw new TreasuryDomainException(e, "error.SibsPayPlatform.requestMbwayMandateAuthorization");
        }
    }

    @Atomic
    private MbwayMandate createMbwayMandateObject(DebtAccount debtAccount, String countryPrefix, String localPhoneNumber,
            String merchantTransactionId) {
        return MbwayMandate.create(this, debtAccount, merchantTransactionId, countryPrefix, localPhoneNumber);
    }

    // ANIL (2025-04-16) (#qubIT-Fenix-5465)
    //
    // When there is the intent to cancel a mandate, it is important to mark
    // as cancelled in the FenixEdu, and only then cancel the mandate in the
    // digital payment platform
    @Atomic(mode = TxMode.READ)
    @Override
    public void cancelMbwayMandate(MbwayMandate mbwayMandate, String reason) {
        if (mbwayMandate.getState().isCanceled()) {
            // Already cancelled, just return
            return;
        }

        markMbwayMandateAsCancelled(mbwayMandate, reason);

        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());

        try {
            SibsPayCancelMbwayMandateResponse response =
                    sibsPayService.cancelMandate(mbwayMandate.getTransactionId(), mbwayMandate.getMerchantTransactionId(),
                            mbwayMandate.getCountryPrefix(), mbwayMandate.getLocalPhoneNumber());

            FenixFramework.atomic(() -> {
                log(mbwayMandate, "cancelMbwayMandate", response.getOperationStatusCode(), response.getOperationStatusMessage(),
                        response.getRequestLog(), response.getResponseLog());
            });

        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                logException(mbwayMandate, e, "cancelMbwayMandate", "error", "error", requestBody, responseBody);
            });

            throw new TreasuryDomainException(e, "error.SibsPayPlatform.cancelMbwayMandate");
        }
    }

    @Atomic
    private void markMbwayMandateAsCancelled(MbwayMandate mbwayMandate, String reason) {
        mbwayMandate.cancel(reason);
    }

    private static final int AUTHORIZATION_EXPIRE_TIME_IN_MINUTES = 10;

    @Atomic(mode = TxMode.READ)
    @Override
    public MbwayMandateBean checkMbwayMandateStateInDigitalPaymentPlatform(MbwayMandate mbwayMandate) {
        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());
        try {
            // First, check if the mandate exists
            String transactionId = mbwayMandate.getTransactionId();

            if (transactionId == null) {
                // we have to discover the transaction id with the merchant transaction id
                SibsPayResponseInquiryWrapper paymentStatusByMerchantTransactionId =
                        sibsPayService.getPaymentStatusByMerchantTransactionId(mbwayMandate.getMerchantTransactionId());

                if (paymentStatusByMerchantTransactionId != null) {
                    transactionId = paymentStatusByMerchantTransactionId.getTransactionId();
                }
            }

            if (transactionId == null) {
                // The transaction id is not present, almost certainly
                // the authorization request was not sent

                MbwayMandateBean bean = new MbwayMandateBean();
                bean.setMandateId(null);
                bean.setTransactionId(null);
                bean.setState(null);
                bean.setPlafond(null);

                return bean;
            }

            SibsPayGetInquiryMbwayMandateResponse inquiryMbwayMandateResponse =
                    sibsPayService.getInquiryMbwayMandate(transactionId, mbwayMandate.getCountryPrefix(),
                            mbwayMandate.getLocalPhoneNumber());

            if (inquiryMbwayMandateResponse.isOperationSuccess()) {
                MbwayMandateState currentMandateState = inquiryMbwayMandateResponse.getCurrentMandateState();

                MbwayMandateBean bean = new MbwayMandateBean();
                bean.setMandateId(inquiryMbwayMandateResponse.getMandate() != null ? inquiryMbwayMandateResponse.getMandate()
                        .getMandateId() : null);
                bean.setTransactionId(inquiryMbwayMandateResponse.getMandate() != null ? inquiryMbwayMandateResponse.getMandate()
                        .getTransactionId() : null);
                bean.setState(currentMandateState);
                bean.setPlafond(inquiryMbwayMandateResponse.getMandate()
                        .getAmountLimit() != null ? inquiryMbwayMandateResponse.getMandate().getAmountLimit().getValue() : null);

                bean.setExpirationDate(mbwayMandate.getExpirationDate());
                if (inquiryMbwayMandateResponse.getMandate().getMandateExpirationDate() != null) {
                    bean.setExpirationDate(parseLocalDate(inquiryMbwayMandateResponse.getMandate().getMandateExpirationDate()));
                }

                return bean;
            } else if (inquiryMbwayMandateResponse.isOperationErrorUnknownAuthPayment()) {
                // This might be the case where an authorization was issued
                // and is new or waiting authorization, and has passed too
                // much time to authorize

                MbwayMandateBean bean = new MbwayMandateBean();
                bean.setMandateId(null);
                bean.setTransactionId(transactionId);
                bean.setState(mbwayMandate.getState());
                bean.setPlafond(null);
                bean.setExpirationDate(null);

                // If it is the case then cancel the mandate
                if (mbwayMandate.getState().isCreated() || mbwayMandate.getState().isWaitingAuthorization()) {
                    if (new Duration(mbwayMandate.getRequestDate(), new DateTime()).toStandardMinutes()
                            .getMinutes() > AUTHORIZATION_EXPIRE_TIME_IN_MINUTES) {
                        bean.setState(MbwayMandateState.NOT_AUTHORIZED);

                        return bean;
                    }
                }

                return bean;
            } else {
                throw new OnlinePaymentsGatewayCommunicationException(inquiryMbwayMandateResponse.getRequestLog(),
                        inquiryMbwayMandateResponse.getResponseLog(), "not able to process the response");
            }
        } catch (final Exception e) {
            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                logException(mbwayMandate, e, "requestMbwayMandateAuthorization", "error", "error", requestBody, responseBody);
            });

            throw new TreasuryDomainException(e, "error.SibsPayPlatform.requestMbwayMandateAuthorization");
        }
    }

    @Atomic(mode = TxMode.READ)
    @Override
    public void updateMbwayMandateState(MbwayMandate mbwayMandate) {
        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());
        try {
            // First, check if the mandate exists
            SibsPayGetInquiryMbwayMandateResponse inquiryMbwayMandateResponse =
                    sibsPayService.getInquiryMbwayMandate(mbwayMandate.getTransactionId(), mbwayMandate.getCountryPrefix(),
                            mbwayMandate.getLocalPhoneNumber());

            executeUpdateMbwayMandateState(mbwayMandate, inquiryMbwayMandateResponse);
        } catch (final Exception e) {
            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                logException(mbwayMandate, e, "requestMbwayMandateAuthorization", "error", "error", requestBody, responseBody);
            });

            throw new TreasuryDomainException(e, "error.SibsPayPlatform.requestMbwayMandateAuthorization");
        }
    }

    @Atomic
    private void executeUpdateMbwayMandateState(MbwayMandate mbwayMandate,
            SibsPayGetInquiryMbwayMandateResponse inquiryMbwayMandateResponse)
            throws OnlinePaymentsGatewayCommunicationException {
        log(mbwayMandate, "updateMbwayMandateState", inquiryMbwayMandateResponse.getReturnStatus().getStatusCode(),
                inquiryMbwayMandateResponse.getReturnStatus().getStatusDescription(), inquiryMbwayMandateResponse.getRequestLog(),
                inquiryMbwayMandateResponse.getResponseLog());

        if (inquiryMbwayMandateResponse.isOperationSuccess()) {
            MbwayMandateState currentMandateState = inquiryMbwayMandateResponse.getCurrentMandateState();

            if (currentMandateState == null) {
                throw new IllegalArgumentException("it was not possible to infer the mandate state");
            }

            if (mbwayMandate.getState().isCreated()) {
                if (currentMandateState.isCanceled()) {
                    mbwayMandate.cancel("canceled authorization in the digital platform");
                } else if (currentMandateState.isExpired()) {
                    mbwayMandate.expire();
                } else {
                    throw new OnlinePaymentsGatewayCommunicationException(inquiryMbwayMandateResponse.getRequestLog(),
                            inquiryMbwayMandateResponse.getResponseLog(),
                            "not in the expected state [%s] [%s]".formatted(mbwayMandate.getState().name(),
                                    currentMandateState.name()));
                }
            } else if (mbwayMandate.getState().isWaitingAuthorization()) {
                if (currentMandateState.isActive()) {
                    mbwayMandate.authorize();
                } else if (currentMandateState.isCanceled()) {
                    mbwayMandate.cancel("canceled authorization in the digital platform");
                } else if (currentMandateState.isExpired()) {
                    mbwayMandate.expire();
                } else if (currentMandateState.isSuspended()) {
                    mbwayMandate.suspend();
                } else if (currentMandateState.isWaitingAuthorization()) {
                    // Do nothing
                } else if (currentMandateState.isNotAuthorized()) {
                    mbwayMandate.markAsNotAuthorized("not authorized in digital platform");
                } else {
                    throw new OnlinePaymentsGatewayCommunicationException(inquiryMbwayMandateResponse.getRequestLog(),
                            inquiryMbwayMandateResponse.getResponseLog(),
                            "not in the expected state [%s] [%s]".formatted(mbwayMandate.getState().name(),
                                    currentMandateState.name()));
                }
            } else if (mbwayMandate.getState().isActive()) {
                if (currentMandateState.isCanceled()) {
                    mbwayMandate.cancel("canceled authorization in the digital platform");
                } else if (currentMandateState.isExpired()) {
                    mbwayMandate.expire();
                } else if (currentMandateState.isSuspended()) {
                    mbwayMandate.suspend();
                } else if (currentMandateState.isActive()) {
                    // update plafond and expiration date

                    BigDecimal limitAmount = mbwayMandate.getPlafond();
                    if (inquiryMbwayMandateResponse.getMandate().getAmountLimit() != null) {
                        limitAmount = inquiryMbwayMandateResponse.getMandate().getAmountLimit().getValue();
                    }

                    LocalDate expirationDate = mbwayMandate.getExpirationDate();
                    if (inquiryMbwayMandateResponse.getMandate().getMandateExpirationDate() != null) {
                        expirationDate = parseLocalDate(inquiryMbwayMandateResponse.getMandate().getMandateExpirationDate());
                    }

                    mbwayMandate.updatePlafondAndExpirationDate(limitAmount, expirationDate);
                } else {
                    throw new OnlinePaymentsGatewayCommunicationException(inquiryMbwayMandateResponse.getRequestLog(),
                            inquiryMbwayMandateResponse.getResponseLog(),
                            "not in the expected state [%s] [%s]".formatted(mbwayMandate.getState().name(),
                                    currentMandateState.name()));
                }
            } else if (mbwayMandate.getState().isSuspended()) {
                if (currentMandateState.isCanceled()) {
                    mbwayMandate.cancel("canceled authorization in the digital platform");
                } else if (currentMandateState.isExpired()) {
                    mbwayMandate.expire();
                } else if (currentMandateState.isActive()) {
                    // reactivate the mandate
                    mbwayMandate.reactivate();
                } else if (currentMandateState.isSuspended()) {
                    // Do nothing
                } else {
                    throw new OnlinePaymentsGatewayCommunicationException(inquiryMbwayMandateResponse.getRequestLog(),
                            inquiryMbwayMandateResponse.getResponseLog(),
                            "not in the expected state [%s] [%s]".formatted(mbwayMandate.getState().name(),
                                    currentMandateState.name()));
                }
            } else if (mbwayMandate.getState().isCanceled() || mbwayMandate.getState().isExpired()) {
                if (currentMandateState.isCanceled() || currentMandateState.isExpired()) {
                    // Do nothing
                } else {
                    throw new OnlinePaymentsGatewayCommunicationException(inquiryMbwayMandateResponse.getRequestLog(),
                            inquiryMbwayMandateResponse.getResponseLog(),
                            "not in the expected state [%s] [%s]".formatted(mbwayMandate.getState().name(),
                                    currentMandateState.name()));
                }
            }
        } else if (inquiryMbwayMandateResponse.isOperationErrorUnknownAuthPayment()) {
            // This might be the case where an authorization was issued
            // and is new or waiting authorization, and has passed too
            // much time to authorize

            // If it is the case then cancel the mandate
            if (mbwayMandate.getState().isCreated() || mbwayMandate.getState().isWaitingAuthorization()) {
                if (new Duration(mbwayMandate.getRequestDate(), new DateTime()).toStandardMinutes()
                        .getMinutes() > AUTHORIZATION_EXPIRE_TIME_IN_MINUTES) {
                    mbwayMandate.markAsNotAuthorized("authorization time expired");
                }
            }
        } else {
            throw new OnlinePaymentsGatewayCommunicationException(inquiryMbwayMandateResponse.getRequestLog(),
                    inquiryMbwayMandateResponse.getResponseLog(), "not able to process the response");
        }
    }

    private static LocalDate parseLocalDate(String value) {
        if (value.length() > 10) {
            value = value.substring(0, 10);
        }

        return TreasuryConstants.parseLocalDate(value, "yyyy-MM-dd");
    }

    @Override
    @Atomic(mode = TxMode.READ)
    public MbwayRequest createMbwayRequest(MbwayMandatePaymentSchedule mbwayMandatePaymentSchedule, Set<DebitEntry> debitEntries,
            Set<Installment> installments) {
        MbwayMandate mbwayMandate = mbwayMandatePaymentSchedule.getMbwayMandate();

        final Function<DebitEntry, BigDecimal> getExtraAmount = (DebitEntry debitEntry) -> {
            PaymentPenaltyEntryBean penaltyTax =
                    PaymentPenaltyTaxTreasuryEvent.calculatePaymentPenaltyTax(debitEntry, LocalDate.now());

            BigDecimal penaltyTaxAmount = penaltyTax != null ? penaltyTax.getSettledAmount() : BigDecimal.ZERO;

            return debitEntry.getOpenAmountWithInterests().add(penaltyTaxAmount);
        };

        if (PaymentRequest.getReferencedCustomers(debitEntries, installments).size() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.referencedCustomers.only.one.allowed");
        }

        DebtAccount debtAccount = mbwayMandate.getDebtAccount();

        String phoneNumber = String.format("%s#%s", mbwayMandate.getCountryPrefix(), mbwayMandate.getLocalPhoneNumber());

        BigDecimal payableAmountDebitEntries =
                debitEntries.stream().map(e -> getExtraAmount.apply(e)).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(i -> i.getOpenAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        String merchantTransactionId = generateNewMerchantTransactionId();

        Optional<String> transactionIdOptional = Optional.empty();
        Optional<String> transactionSignatureOptional = Optional.empty();

        SibsPayAPIService sibsPayService =
                new SibsPayAPIService(getEndpointUrl(), getAssetsEndpointUrl(), getClientId(), getBearerToken(), getTerminalId(),
                        getEntityReferenceCode());

        MbwayRequest mbwayRequest =
                createMbwayRequestWithMandatePaymentSchedule(mbwayMandatePaymentSchedule, debitEntries, installments, debtAccount,
                        phoneNumber, payableAmount, merchantTransactionId);

        // 1. Request checkout
        {
            PaymentRequestLog log = log(mbwayRequest, "createMbwayRequest", null, null, null, null);
            try {

                DateTime requestSendDate = new DateTime();

                SibsPayReturnCheckout sibsPayReturnCheckout =
                        sibsPayService.processMbwayMandateCheckout(debtAccount, payableAmount, merchantTransactionId,
                                mbwayMandate.getMandateId());

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
                    final String message =
                            "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor." + (isOnlinePaymentsGatewayException ? "gateway.communication" : "unknown");

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

                SibsPayResponseInquiryWrapper responseInquiryWrapper =
                        sibsPayService.generateMbwayMandateRequestTransaction(transactionId, transactionSignature);

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
                    final String message =
                            "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor." + (isOnlinePaymentsGatewayException ? "gateway.communication" : "unknown");

                    throw new TreasuryDomainException(e, message);
                }
            }
        }

    }

    @Atomic
    private MbwayRequest createMbwayRequestWithMandatePaymentSchedule(MbwayMandatePaymentSchedule mbwayMandatePaymentSchedule,
            Set<DebitEntry> debitEntries, Set<Installment> installments, DebtAccount debtAccount, String phoneNumber,
            BigDecimal payableAmount, String merchantTransactionId) {
        MbwayRequest mbwayRequest = MbwayRequest.create(this, debtAccount, debitEntries, installments, phoneNumber, payableAmount,
                merchantTransactionId);

        mbwayRequest.setMbwayMandatePaymentSchedule(mbwayMandatePaymentSchedule);
        return mbwayRequest;
    }

    @Override
    public boolean isMbwayAuthorizedPaymentsActive() {
        return getMbwayAuthorizedPaymentsActive();
    }

    @Override
    public int getMaximumTimeForAuthorizationInMinutes() {
        return AUTHORIZATION_EXPIRE_TIME_IN_MINUTES;
    }

    @Override
    public void updateLastMbwayPaymentScheduleExecution() {
        setLastMbwayPaymentScheduleExecution(new DateTime());
    }

    /*
     * ********
     * SERVICES
     * ********
     */

    @Atomic(mode = TxMode.WRITE)
    private static PaymentRequestLog createLogForSibsPaymentRequest(String merchantTransactionId) {
        PaymentRequestLog log = PaymentRequestLog.create((PaymentRequest) null, "createSibsPaymentRequest",
                PaymentReferenceCodeStateType.UNUSED.getCode(), PaymentReferenceCodeStateType.UNUSED.getDescriptionI18N());

        log.setInternalMerchantTransactionId(merchantTransactionId);

        return log;
    }

    public static String getPresentationName() {
        return "SIBS Pay (SPG)";
    }

    public static SibsPayPlatform create(FinantialInstitution finantialInstitution, FinantialEntity finantialEntity, String name,
            boolean active, String clientId, String bearerToken, Integer terminalId, String entityReferenceCode,
            String endpointUrl, String assetsEndpointUrl) {
        return new SibsPayPlatform(finantialInstitution, finantialEntity, name, active, clientId, bearerToken, terminalId,
                entityReferenceCode, endpointUrl, assetsEndpointUrl);
    }

    public static Stream<SibsPayPlatform> findAllActive() {
        return DigitalPaymentPlatform.findAll().filter(p -> p instanceof SibsPayPlatform).filter(p -> p.isActive())
                .map(SibsPayPlatform.class::cast);
    }

}
