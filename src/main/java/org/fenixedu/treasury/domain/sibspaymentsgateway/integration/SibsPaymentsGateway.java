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
package org.fenixedu.treasury.domain.sibspaymentsgateway.integration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.CheckoutResultBean;
import org.fenixedu.onlinepaymentsgateway.api.CustomerDataInputBean;
import org.fenixedu.onlinepaymentsgateway.api.MbCheckoutResultBean;
import org.fenixedu.onlinepaymentsgateway.api.MbPrepareCheckoutInputBean;
import org.fenixedu.onlinepaymentsgateway.api.MbWayCheckoutResultBean;
import org.fenixedu.onlinepaymentsgateway.api.MbWayPrepareCheckoutInputBean;
import org.fenixedu.onlinepaymentsgateway.api.OnlinePaymentServiceFactory;
import org.fenixedu.onlinepaymentsgateway.api.PaymentStateBean;
import org.fenixedu.onlinepaymentsgateway.api.PrepareCheckoutInputBean;
import org.fenixedu.onlinepaymentsgateway.api.SIBSInitializeServiceBean;
import org.fenixedu.onlinepaymentsgateway.api.SIBSOnlinePaymentsGatewayService;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.onlinepaymentsgateway.sibs.sdk.SibsEnvironmentMode;
import org.fenixedu.onlinepaymentsgateway.sibs.sdk.SibsResultCodeType;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentController;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatformPaymentMode;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsBillingAddressBean;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGatewayEnviromentMode;
import org.fenixedu.treasury.domain.sibspaymentsgateway.SibsPaymentsGatewayLog;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class SibsPaymentsGateway extends SibsPaymentsGateway_Base
        implements ISibsPaymentCodePoolService, IForwardPaymentPlatformService {

    private static final String ERROR_UNEXPECTED_NUMBER_TRANSACTIONS_BY_MERCHANT_TRANSACTION_ID =
            "error.SibsOnlinePaymentsGatewayForwardImplementation.paymentStatus.unexpected.number.transactions.by.merchantTransactionId";

    public SibsPaymentsGateway() {
        super();
    }

    public SibsPaymentsGateway(FinantialInstitution finantialInstitution, String name, boolean active, String entityReferenceCode,
            String sibsEntityId, String sibsEndpointUrl, String bearerToken, String aesKey) {
        this();

        this.init(finantialInstitution, name, active);

        setEntityReferenceCode(entityReferenceCode);
        setSibsEntityId(sibsEntityId);
        setSibsEndpointUrl(sibsEndpointUrl);
        setBearerToken(bearerToken);
        setAesKey(aesKey);

        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getCreditCardPaymentMethod());
        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getMbPaymentMethod());
        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getMbWayPaymentMethod());
        
        checkRules();
    }

    private void checkRules() {
    }

    public boolean isSendBillingDataInOnlinePayment() {
        return super.getSendBillingDataInOnlinePayment();
    }

    public String generateNewMerchantTransactionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public MbWayCheckoutResultBean generateMbwayReference(BigDecimal payableAmount, String merchantTransactionId,
            String phoneNumber) throws OnlinePaymentsGatewayCommunicationException {
        final SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        MbWayPrepareCheckoutInputBean inputBean =
                new MbWayPrepareCheckoutInputBean(payableAmount, merchantTransactionId, phoneNumber);

        inputBean.setAmount(payableAmount);
        inputBean.setMerchantTransactionId(merchantTransactionId);
        inputBean.setPhoneNumber(phoneNumber);

        MbWayCheckoutResultBean mbwayCheckoutResult = gatewayService.generateMbWayPayment(inputBean, null);

        return mbwayCheckoutResult;
    }

    @Override
    public String getPaymentURL(ForwardPaymentRequest request) {
        return getSibsEndpointUrl() + "/paymentWidgets.js?checkoutId=" + request.getSibsGatewayCheckoutId();
    }

    @Override
    public IForwardPaymentController getForwardPaymentController(ForwardPaymentRequest request) {
        return IForwardPaymentController.getForwardPaymentController(request);
    }

    @Override
    public String getLogosJspPage() {
        return "/WEB-INF/treasury/document/forwardpayments/forwardpayment/implementations/netcaixa/logos.jsp";
    }

    @Override
    public String getWarningBeforeRedirectionJspPage() {
        return null;
    }

    public static final String CONTROLLER_URL = "/treasury/document/forwardpayments/sibsonlinepaymentsgateway";
    private static final String RETURN_FORWARD_PAYMENT_URI = "/returnforwardpayment";
    public static final String RETURN_FORWARD_PAYMENT_URL = CONTROLLER_URL + RETURN_FORWARD_PAYMENT_URI;

    public String getReturnURL(final ForwardPaymentRequest forwardPayment) {
        return String.format("%s%s/%s", 
                TreasurySettings.getInstance().getForwardPaymentReturnDefaultURL(),
                RETURN_FORWARD_PAYMENT_URL, 
                forwardPayment.getExternalId());
    }

    @Atomic(mode = TxMode.READ)
    public ForwardPaymentStatusBean prepareCheckout(ForwardPaymentRequest forwardPayment, SibsBillingAddressBean addressBean) {
        String merchantTransactionId = generateNewMerchantTransactionId();

        FenixFramework.atomic(() -> {
            if (!StringUtils.isEmpty(forwardPayment.getSibsGatewayMerchantTransactionId())) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayForwardImplementation.sibsMerchantTransactionId.already.filled");
            }

            forwardPayment.setSibsGatewayMerchantTransactionId(merchantTransactionId);
        });

        try {
            DateTime requestSendDate = new DateTime();
            
            final CheckoutResultBean checkoutBean = prepareCheckout(forwardPayment.getDebtAccount(), merchantTransactionId,
                    forwardPayment.getPayableAmount(), getReturnURL(forwardPayment), addressBean);

            DateTime requestReceiveDate = new DateTime();

            final ForwardPaymentStateType stateType = translateForwardPaymentStateType(checkoutBean.getOperationResultType(), false);
            final ForwardPaymentStatusBean result = new ForwardPaymentStatusBean(checkoutBean.isOperationSuccess(), stateType,
                    checkoutBean.getPaymentGatewayResultCode(), checkoutBean.getPaymentGatewayResultDescription(),
                    checkoutBean.getRequestLog(), checkoutBean.getResponseLog());

            FenixFramework.atomic(() -> {
                forwardPayment.setSibsGatewayCheckoutId(checkoutBean.getCheckoutId());
            });

            FenixFramework.atomic(() -> {
                if (!result.isInvocationSuccess() || (result.getStateType() == ForwardPaymentStateType.REJECTED)) {
                    SibsPaymentsGatewayLog log = (SibsPaymentsGatewayLog) forwardPayment.reject("prepareCheckout", checkoutBean.getPaymentGatewayResultCode(),
                            checkoutBean.getPaymentGatewayResultDescription(), checkoutBean.getRequestLog(),
                            checkoutBean.getResponseLog());

                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);
                    
                } else {
                    SibsPaymentsGatewayLog log = (SibsPaymentsGatewayLog) forwardPayment.advanceToRequestState("prepareCheckout", 
                            checkoutBean.getPaymentGatewayResultCode(),
                            checkoutBean.getPaymentGatewayResultDescription(), 
                            checkoutBean.getRequestLog(),
                            checkoutBean.getResponseLog());

                    log.setOperationSuccess(result.isInvocationSuccess());
                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);
                }
            });

            result.defineSibsOnlinePaymentBrands(checkoutBean.getPaymentBrands());

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

    @Atomic(mode = TxMode.READ)
    private CheckoutResultBean prepareCheckout(DebtAccount debtAccount, String merchantTransactionId, BigDecimal payableAmount,
            String returnUrl, SibsBillingAddressBean billingAddressBean) throws OnlinePaymentsGatewayCommunicationException {
        final SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        final PrepareCheckoutInputBean bean = new PrepareCheckoutInputBean(payableAmount, merchantTransactionId, returnUrl,
                new DateTime(), new DateTime().plusDays(7));

        if (isSendBillingDataInOnlinePayment()) {
            String customerEmail = debtAccount.getCustomer().getEmail();

            bean.fillBillingData(/* debtAccount.getCustomer().getName() */ null, billingAddressBean.getAddressCountryCode(),
                    billingAddressBean.getCity(), billingAddressBean.getAddress(), billingAddressBean.getZipCode(),
                    customerEmail);
        }

        bean.setUseCreditCard(true);

        CheckoutResultBean resultBean = gatewayService.prepareOnlinePaymentCheckout(bean);

        return resultBean;
    }

    public ForwardPaymentStatusBean paymentStatusByCheckoutId(final ForwardPaymentRequest forwardPayment) {
        try {
            PaymentStateBean paymentStateBean = getPaymentStatusBySibsCheckoutId(forwardPayment.getSibsGatewayCheckoutId());

            String requestLog = paymentStateBean.getRequestLog();
            String responseLog = paymentStateBean.getResponseLog();

            final ForwardPaymentStateType type =
                    translateForwardPaymentStateType(paymentStateBean.getOperationResultType(), paymentStateBean.isPaid());

            final ForwardPaymentStatusBean statusBean = new ForwardPaymentStatusBean(paymentStateBean.isOperationSuccess(), type,
                    paymentStateBean.getPaymentGatewayResultCode(), paymentStateBean.getPaymentGatewayResultDescription(),
                    requestLog, responseLog);

            statusBean.editTransactionDetails(paymentStateBean.getTransactionId(), paymentStateBean.getPaymentDate(),
                    paymentStateBean.getAmount());

            return statusBean;
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
    public ForwardPaymentStatusBean paymentStatus(final ForwardPaymentRequest forwardPayment) {

        try {
            PaymentStateBean paymentStateBean = null;
            if (!StringUtils.isEmpty(forwardPayment.getSibsGatewayTransactionId())) {
                paymentStateBean = getPaymentStatusBySibsTransactionId(forwardPayment.getSibsGatewayTransactionId());
            } else {
                List<PaymentStateBean> paymentStateBeanList =
                        getPaymentTransactionsReportListByMerchantId(forwardPayment.getSibsGatewayMerchantTransactionId());
                if (paymentStateBeanList.size() != 1) {
                    throw new TreasuryDomainException(ERROR_UNEXPECTED_NUMBER_TRANSACTIONS_BY_MERCHANT_TRANSACTION_ID);
                }

                paymentStateBean = paymentStateBeanList.get(0);
            }

            final String requestLog = paymentStateBean.getRequestLog();
            final String responseLog = paymentStateBean.getResponseLog();

            final ForwardPaymentStateType type =
                    translateForwardPaymentStateType(paymentStateBean.getOperationResultType(), paymentStateBean.isPaid());

            final ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(paymentStateBean.isOperationSuccess(), type,
                    paymentStateBean.getPaymentGatewayResultCode(), paymentStateBean.getPaymentGatewayResultDescription(),
                    requestLog, responseLog);

            bean.editTransactionDetails(paymentStateBean.getTransactionId(), paymentStateBean.getPaymentDate(),
                    paymentStateBean.getAmount());

            return bean;
        } catch (final Exception e) {

            FenixFramework.atomic(() -> {
                String requestBody = null;
                String responseBody = null;

                if (e instanceof OnlinePaymentsGatewayCommunicationException) {
                    requestBody = ((OnlinePaymentsGatewayCommunicationException) e).getRequestLog();
                    responseBody = ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog();
                }

                if (!ERROR_UNEXPECTED_NUMBER_TRANSACTIONS_BY_MERCHANT_TRANSACTION_ID.equals(e.getMessage())) {
                    PaymentRequestLog log = forwardPayment.logException(e);
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

    private ForwardPaymentStateType translateForwardPaymentStateType(final SibsResultCodeType operationResultType,
            final boolean paid) {

        if (operationResultType == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayForwardImplementation.unknown.payment.state");
        }

        if (paid) {
            if (operationResultType != SibsResultCodeType.SUCCESSFUL_TRANSACTION
                    && operationResultType != SibsResultCodeType.SUCESSFUL_PROCESSED_TRANSACTION_FOR_REVIEW) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayForwardImplementation.payment.appears.paid.but.inconsistent.with.result.code");
            }

            return ForwardPaymentStateType.PAYED;
        } else if (operationResultType == SibsResultCodeType.PENDING_TRANSACTION) {
            return ForwardPaymentStateType.REQUESTED;
        }

        return ForwardPaymentStateType.REJECTED;
    }

    @Override
    public PostProcessPaymentStatusBean postProcessPayment(ForwardPaymentRequest forwardPayment, String justification,
            Optional<String> specificTransactionId) {
        if(specificTransactionId.isEmpty()) {
            ForwardPaymentStatusBean statusBean = new ForwardPaymentStatusBean(false, forwardPayment.getState(), "N/A", "N/A", null, null);
            return new PostProcessPaymentStatusBean(statusBean, forwardPayment.getState(), false);
        }
        
        if (!forwardPayment.getState().isInStateToPostProcessPayment()) {
            throw new TreasuryDomainException("error.ManageForwardPayments.forwardPayment.not.created.nor.requested",
                    String.valueOf(forwardPayment.getOrderNumber()));
        }

        try {
            DateTime requestSendDate = new DateTime();
            
            PaymentStateBean paymentStateBean = getPaymentStatusBySibsTransactionId(specificTransactionId.get());

            DateTime requestReceiveDate = new DateTime();

            String requestLog = paymentStateBean.getRequestLog();
            String responseLog = paymentStateBean.getResponseLog();

            ForwardPaymentStateType type =
                    translateForwardPaymentStateType(paymentStateBean.getOperationResultType(), paymentStateBean.isPaid());

            ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(paymentStateBean.isOperationSuccess(), type,
                    paymentStateBean.getPaymentGatewayResultCode(), paymentStateBean.getPaymentGatewayResultDescription(),
                    requestLog, responseLog);

            bean.editTransactionDetails(paymentStateBean.getTransactionId(), paymentStateBean.getPaymentDate(),
                    paymentStateBean.getAmount());
            
            if (Lists.newArrayList(ForwardPaymentStateType.CREATED, ForwardPaymentStateType.REQUESTED).contains(bean.getStateType())) {
                // Do nothing
                return new PostProcessPaymentStatusBean(bean, forwardPayment.getState(), false);
            }

            // First of all save sibsTransactionId
            FenixFramework.atomic(() -> {
                forwardPayment.setSibsGatewayTransactionId(bean.getTransactionId());
            });

            PostProcessPaymentStatusBean returnBean = new PostProcessPaymentStatusBean(bean, forwardPayment.getState(), bean.isInPayedState());
            
            if (bean.isInPayedState()) {
                FenixFramework.atomic(() -> {
                    SibsPaymentsGatewayLog log = (SibsPaymentsGatewayLog) forwardPayment.advanceToPaidState(bean.getStatusCode(), bean.getStatusMessage(),
                            bean.getPayedAmount(), bean.getTransactionDate(), bean.getTransactionId(), null,
                            bean.getRequestBody(), bean.getResponseBody(), "");

                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);
                    log.setSibsGatewayTransactionId(bean.getTransactionId());
                });

            } else {
                FenixFramework.atomic(() -> {
                    SibsPaymentsGatewayLog log = (SibsPaymentsGatewayLog) forwardPayment.reject("postProcessPayment", bean.getStatusCode(), bean.getStatusMessage(), bean.getRequestBody(),
                            bean.getResponseBody());

                    log.setRequestSendDate(requestSendDate);
                    log.setRequestReceiveDate(requestReceiveDate);
                    log.setSibsGatewayTransactionId(bean.getTransactionId());
                });

            }
            
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

    @Override
    public List<ForwardPaymentStatusBean> verifyPaymentStatus(ForwardPaymentRequest request) {
        // TODO Auto-generated method stub
        return null;
    }

    /* ISibsPaymentCodePoolService */

    @Override
    public SibsPaymentRequest createSibsPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries) {
        BigDecimal payableAmount = debitEntries.stream().map(DebitEntry::getOpenAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate now = new LocalDate();
        LocalDate validTo = debitEntries.stream().map(d -> d.getDueDate()).max(LocalDate::compareTo).orElse(now);
        
        if(validTo.isBefore(now)) {
            validTo = now;
        }
        
        return createPaymentRequest(debtAccount, debitEntries, validTo, payableAmount);
    }

    @Override
    public SibsPaymentRequest createSibsPaymentRequestWithInterests(DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            LocalDate interestsCalculationDate) {
        BigDecimal payableAmount = debitEntries.stream().map(d -> d.getOpenAmountWithInterestsAtDate(interestsCalculationDate))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDate now = new LocalDate();
        LocalDate validTo = debitEntries.stream().map(d -> d.getDueDate()).max(LocalDate::compareTo).orElse(now);
        
        if(validTo.isBefore(now)) {
            validTo = now;
        }
        
        return createPaymentRequest(debtAccount, debitEntries, validTo, payableAmount);
    }

    private void checkMaxActiveSibsPaymentRequests(Set<DebitEntry> debitEntries) {
        for (DebitEntry debitEntry : debitEntries) {
            long numActiveSibsPaymentRequests =
                    debitEntry.getPaymentRequestsSet().stream().filter(r -> r instanceof SibsPaymentRequest)
                            .map(SibsPaymentRequest.class::cast).filter(r -> r.getState() == PaymentReferenceCodeStateType.UNUSED
                                    || r.getState() == PaymentReferenceCodeStateType.USED)
                            .count();

            if (numActiveSibsPaymentRequests >= 2) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.with.active.payment.code",
                        debitEntry.getDescription());
            }
        }
    }

    private SibsPaymentRequest createPaymentRequest(DebtAccount debtAccount, Set<DebitEntry> debitEntries, LocalDate validTo,
            BigDecimal payableAmount) {
        if (!isActive()) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.paymentCodePool.not.active");
        }

        if (PaymentRequest.getReferencedCustomers(debitEntries).size() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.referencedCustomers.only.one.allowed");
        }
        
        checkMaxActiveSibsPaymentRequests(debitEntries);

        String merchantTransactionId = generateNewMerchantTransactionId();

        final SibsPaymentsGatewayLog log = createForSibsPaymentRequest(merchantTransactionId);

        try {

            FenixFramework.atomic(() -> {
                log.logRequestSendDate();
            });

            MbCheckoutResultBean checkoutResultBean = generateMBPaymentReference(payableAmount, new DateTime(),
                    new DateTime().plusMonths(getNumberOfMonthsToExpirePaymentReferenceCode()), merchantTransactionId);

            final String sibsReferenceId = checkoutResultBean.getTransactionId();
            FenixFramework.atomic(() -> {
                log.logRequestReceiveDateAndData(checkoutResultBean.getTransactionId(), checkoutResultBean.isOperationSuccess(),
                        false, checkoutResultBean.getPaymentGatewayResultCode(), checkoutResultBean.getOperationResultDescription());
                
                log.saveRequest(checkoutResultBean.getRequestLog());
                log.saveResponse(checkoutResultBean.getResponseLog());
                log.savePaymentTypeAndBrand(
                        checkoutResultBean.getPaymentType() != null ? checkoutResultBean.getPaymentType().name() : null, 
                        checkoutResultBean.getPaymentBrand() != null ? checkoutResultBean.getPaymentBrand().name() : null);
            });

            if (!checkoutResultBean.isOperationSuccess()) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.request.not.successful");
            }

            final String referenceCode = checkoutResultBean.getPaymentReference();

            if (StringUtils.isEmpty(referenceCode)) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.reference.not.empty");
            }

            if (SibsReferenceCode.findByReferenceCode(getEntityReferenceCode(), referenceCode).count() >= 1) {
                throw new TreasuryDomainException("error.PaymentReferenceCode.referenceCode.duplicated");
            }

            if (PaymentRequest.findBySibsGatewayMerchantTransactionId(merchantTransactionId).count() >= 1) {
                throw new TreasuryDomainException("error.PaymentReferenceCode.sibsMerchantTransaction.found.duplicated");
            }

            if (PaymentRequest.findBySibsGatewayTransactionId(sibsReferenceId).count() >= 1) {
                throw new TreasuryDomainException("error.PaymentReferenceCode.sibsReferenceId.found.duplicated");
            }

            SibsPaymentRequest sibsPaymentRequest = FenixFramework.atomic(() -> {
                SibsPaymentRequest request = SibsPaymentRequest.create(SibsPaymentsGateway.this, debtAccount, debitEntries, payableAmount,
                        referenceCode, merchantTransactionId, sibsReferenceId);
                log.setPaymentRequest(request);
                
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

    public PaymentStateBean handleWebhookNotificationRequest(String initializationVector, String authTag, String encryptedPayload)
            throws Exception {
        return gatewayService().handleNotificationRequest(initializationVector, authTag, encryptedPayload);
    }

    @Atomic(mode = TxMode.READ)
    public PaymentStateBean getPaymentStatusBySibsCheckoutId(final String checkoutId)
            throws OnlinePaymentsGatewayCommunicationException {
        try {
            return gatewayService().getPaymentStatusByCheckoutId(checkoutId);
        } catch (OnlinePaymentsGatewayCommunicationException e) {
            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Atomic(mode = TxMode.READ)
    public List<PaymentStateBean> getPaymentTransactionsReportListByMerchantId(String merchantTransactionId)
            throws OnlinePaymentsGatewayCommunicationException {
        return gatewayService().getPaymentTransactionsReportListByMerchantId(merchantTransactionId);
    }

    @Atomic(mode = TxMode.READ)
    public PaymentStateBean getPaymentStatusBySibsTransactionId(String transactionId)
            throws OnlinePaymentsGatewayCommunicationException {
        return gatewayService().getPaymentTransactionReportByTransactionId(transactionId);
    }
    
    public void edit(String name, String sibsEndpointUrl, String bearerToken, String aesKey,
            int numberOfMonthsToExpirePaymentReferenceCode, boolean sendBillingDataInOnlinePayment) {
        
        setName(name);
        setSibsEndpointUrl(sibsEndpointUrl);
        setBearerToken(bearerToken);
        setAesKey(aesKey);
        setNumberOfMonthsToExpirePaymentReferenceCode(numberOfMonthsToExpirePaymentReferenceCode);
        setSendBillingDataInOnlinePayment(sendBillingDataInOnlinePayment);
        
        checkRules();
    }

    @Override
    public void delete() {
        super.delete();
        
        super.deleteDomainObject();
    }

    @Atomic(mode = TxMode.READ)
    private MbCheckoutResultBean generateMBPaymentReference(BigDecimal payableAmount, DateTime validFrom, DateTime validTo,
            String merchantTransactionId) throws OnlinePaymentsGatewayCommunicationException {
        final SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        final MbPrepareCheckoutInputBean inputBean =
                new MbPrepareCheckoutInputBean(payableAmount, merchantTransactionId, validFrom, validTo);

        // Customer data will not be sent due to GDPR
        final CustomerDataInputBean customerInputBean = null;

        final MbCheckoutResultBean requestResult = gatewayService.generateMBPaymentReference(inputBean, customerInputBean);

        return requestResult;
    }

    @Atomic(mode = TxMode.WRITE)
    private void saveExceptionLog(final SibsPaymentsGatewayLog log, final Exception e,
            final boolean isOnlinePaymentsGatewayException) {
        log.logRequestReceiveDateAndData(null, false, false, null, null);
        log.logException(e);

        if (isOnlinePaymentsGatewayException) {
            log.saveRequest(((OnlinePaymentsGatewayCommunicationException) e).getRequestLog());
            log.saveResponse(((OnlinePaymentsGatewayCommunicationException) e).getResponseLog());
        }
    }

    private SIBSOnlinePaymentsGatewayService gatewayService() {
        final SIBSInitializeServiceBean initializeServiceBean =
                new SIBSInitializeServiceBean(getSibsEntityId(), getBearerToken(), getSibsEndpointUrl(), getEntityReferenceCode(),
                        getFinantialInstitution().getCurrency().getIsoCode(), translateEnviromentMode());

        initializeServiceBean.setAesKey(getAesKey());

        final SIBSOnlinePaymentsGatewayService gatewayService =
                OnlinePaymentServiceFactory.createSIBSOnlinePaymentGatewayService(initializeServiceBean);

        return gatewayService;
    }

    private SibsEnvironmentMode translateEnviromentMode() {
        if (getEnviromentMode() == SibsOnlinePaymentsGatewayEnviromentMode.PRODUCTION) {
            return SibsEnvironmentMode.PRODUCTION;
        } else if (getEnviromentMode() == SibsOnlinePaymentsGatewayEnviromentMode.TEST_MODE_EXTERNAL) {
            return SibsEnvironmentMode.TEST_MODE_EXTERNAL;
        } else if (getEnviromentMode() == SibsOnlinePaymentsGatewayEnviromentMode.TEST_MODE_INTERNAL) {
            return SibsEnvironmentMode.TEST_MODE_INTERNAL;
        }

        throw new RuntimeException("SibsOnlinePaymentsGateway.translateEnviromentMode() unkown environment mode");
    }

    @Override
    public SibsPaymentsGatewayLog log(PaymentRequest paymentRequest) {
        SibsPaymentsGatewayLog log = SibsPaymentsGatewayLog.create(paymentRequest, paymentRequest.getSibsGatewayMerchantTransactionId());
        log.setStateCode(paymentRequest.getCurrentState().getCode());
        log.setStateDescription(paymentRequest.getCurrentState().getLocalizedName());
        
        return log;
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<SibsPaymentsGateway> findAll() {
        return DigitalPaymentPlatform.findAll().filter(p -> p instanceof SibsPaymentsGateway)
                .map(SibsPaymentsGateway.class::cast);
    }

    public static Stream<SibsPaymentsGateway> find(FinantialInstitution finantialInstitution) {
        return finantialInstitution.getDigitalPaymentPlatformsSet().stream().filter(p -> p instanceof SibsPaymentsGateway)
                .map(SibsPaymentsGateway.class::cast);
    }

    public static Optional<SibsPaymentsGateway> findUniqueActive(FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(p -> p.isActive()).findAny();
    }

    public static SibsPaymentsGateway create(FinantialInstitution finantialInstitution, String name, boolean active,
            String entityReferenceCode, String sibsEntityId, String sibsEndpointUrl, String bearerToken, String aesKey) {
        return new SibsPaymentsGateway(finantialInstitution, name, active, entityReferenceCode, sibsEntityId, sibsEndpointUrl,
                bearerToken, aesKey);
    }

    @Atomic(mode = TxMode.WRITE)
    public static SibsPaymentsGatewayLog createForSibsPaymentRequest(String sibsGatewayMerchantTransactionId) {
        return SibsPaymentsGatewayLog.createForSibsPaymentRequest(sibsGatewayMerchantTransactionId);
    }

    public static boolean isMbwayServiceActive(FinantialInstitution finantialInstitution) {
        return finantialInstitution.getDigitalPaymentPlatformsSet().stream()
                .flatMap(p -> p.getDigitalPaymentPlatformPaymentModesSet().stream())
                .filter(p -> p.getPaymentMethod() == TreasurySettings.getInstance().getMbWayPaymentMethod())
                .anyMatch(p -> p.isActive());
    }

    public static String getPresentationName() {
        return TreasuryConstants.treasuryBundle("label.SibsPaymentsGateway.presentationName");
    }
    
}
