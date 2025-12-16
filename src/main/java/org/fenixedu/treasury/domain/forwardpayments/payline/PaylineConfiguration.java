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
package org.fenixedu.treasury.domain.forwardpayments.payline;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpSession;

import com.google.common.base.Strings;
import org.apache.commons.validator.routines.EmailValidator;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentController;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PaylineWebServiceClient;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatformPaymentMode;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InstallmentPaymenPlanBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;
import org.fenixedu.treasury.services.integration.FenixEDUTreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.*;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.collect.Lists;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;
import pt.ist.fenixframework.Atomic;

public class PaylineConfiguration extends PaylineConfiguration_Base implements IForwardPaymentPlatformService {

    public PaylineConfiguration() {
        super();
    }

    protected PaylineConfiguration(FinantialInstitution finantialInstitution, FinantialEntity finantialEntity, String name,
            boolean active, String paymentURL, String paylineMerchantId, String paylineMerchantAccessKey,
            String paylineContractNumber) {
        this();

        this.init(finantialInstitution, finantialEntity, name, active);

        setPaymentURL(paymentURL);
        setPaylineMerchantId(paylineMerchantId);
        setPaylineMerchantAccessKey(paylineMerchantAccessKey);
        setPaylineContractNumber(paylineContractNumber);

        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getCreditCardPaymentMethod());

        checkRules();
    }

    @Override
    public IForwardPaymentController getForwardPaymentController(final ForwardPaymentRequest forwardPayment) {
        return IForwardPaymentController.getForwardPaymentController(forwardPayment);
    }

    @Override
    public String getPaymentURL(ForwardPaymentRequest request) {
        throw new RuntimeException("not applied");
    }

    @Override
    public String getLogosJspPage() {
        return "implementations/payline/logos.jsp";
    }

    @Override
    public String getWarningBeforeRedirectionJspPage() {
        return "implementations/payline/warning.jsp";
    }

    @Override
    public ForwardPaymentStatusBean paymentStatus(ForwardPaymentRequest forwardPayment) {
        if (!forwardPayment.getDigitalPaymentPlatform().isActive()) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.not.active");
        }

        PaylineWebServiceResponse response = paylineGetWebPaymentDetails(forwardPayment);

        ForwardPaymentStateType type = null;

        String authorizationNumber = null;
        DateTime authorizationDate = null;

        String transactionId = null;
        DateTime transactionDate = null;
        BigDecimal payedAmount = null;

        final boolean success = TRANSACTION_APPROVED_CODE.equals(response.getResultCode());
        if (!success) {
            if (TRANSACTION_PENDING_FORM_FILL.equals(response.getResultCode())) {
                type = ForwardPaymentStateType.REQUESTED;
            } else {
                type = ForwardPaymentStateType.REJECTED;
            }
        } else {
            authorizationNumber = response.getAuthorizationNumber();
            authorizationDate = response.getAuthorizationDate();

            transactionId = response.getTransactionId();
            payedAmount = response.getPaymentAmount();
            transactionDate = response.getTransactionDate();
            type = ForwardPaymentStateType.PAYED;
        }

        final ForwardPaymentStatusBean bean =
                new ForwardPaymentStatusBean(true, type, response.getResultCode(), response.getResultLongMessage(),
                        response.getJsonRequest(), response.getJsonResponse());

        bean.editAuthorizationDetails(authorizationNumber, authorizationDate);
        bean.editTransactionDetails(transactionId, transactionDate, payedAmount);

        return bean;
    }

    @Override
    @Atomic
    public PostProcessPaymentStatusBean postProcessPayment(final ForwardPaymentRequest forwardPayment, final String justification,
            final Optional<String> specificTransactionId) {

        final ForwardPaymentStateType previousState = forwardPayment.getCurrentState();

        final ForwardPaymentStatusBean paymentStatusBean =
                forwardPayment.getDigitalPaymentPlatform().castToForwardPaymentPlatformService().paymentStatus(forwardPayment);

        if (!forwardPayment.getState().isInStateToPostProcessPayment()) {
            throw new TreasuryDomainException("error.ManageForwardPayments.forwardPayment.not.created.nor.requested",
                    String.valueOf(forwardPayment.getOrderNumber()));
        }

        if (Lists.newArrayList(ForwardPaymentStateType.CREATED, ForwardPaymentStateType.REQUESTED)
                .contains(paymentStatusBean.getStateType())) {
            // Do nothing
            return new PostProcessPaymentStatusBean(paymentStatusBean, previousState, false);
        }

        final boolean success = TRANSACTION_APPROVED_CODE.equals(paymentStatusBean.getStatusCode());

        if (!paymentStatusBean.isOperationSuccess()) {
            throw new TreasuryDomainException("error.ManageForwardPayments.postProcessPayment.invocation.unsucessful",
                    String.valueOf(forwardPayment.getOrderNumber()));
        }

        if (!success) {
            forwardPayment.reject("postProcessPayment", paymentStatusBean.getStatusCode(), paymentStatusBean.getStatusMessage(),
                    paymentStatusBean.getRequestBody(), paymentStatusBean.getResponseBody());

            return new PostProcessPaymentStatusBean(paymentStatusBean, previousState, false);
        }

        forwardPayment.advanceToPaidState(paymentStatusBean.getStatusCode(), paymentStatusBean.getStatusMessage(),
                paymentStatusBean.getPayedAmount(), paymentStatusBean.getTransactionDate(), paymentStatusBean.getTransactionId(),
                paymentStatusBean.getAuthorizationNumber(), paymentStatusBean.getRequestBody(),
                paymentStatusBean.getResponseBody(), justification);

        return new PostProcessPaymentStatusBean(paymentStatusBean, previousState, true);
    }

    public void edit(String name, boolean active, String paymentURL, String paylineMerchantId, String paylineMerchantAccessKey,
            String paylineContractNumber) {

        setName(name);
        setActive(active);
        setPaymentURL(paymentURL);
        setPaylineMerchantId(paylineMerchantId);
        setPaylineMerchantAccessKey(paylineMerchantAccessKey);
        setPaylineContractNumber(paylineContractNumber);

        checkRules();
    }

    @Override
    public void delete() {
        super.delete();

        super.deleteDomainObject();
    }

    /*
     * Payline Implementation
     */

    private static final String TRANSACTION_APPROVED_CODE = "00000";
    private static final String TRANSACTION_PENDING_FORM_FILL = "02306";

    public static final String ACTION_RETURN_URL = "return";
    public static final String ACTION_CANCEL_URL = "cancel";
    public static final String LANG_PT = "pt";
    public static final String LANG_EN = "en";

    public static String getReturnURL(ForwardPaymentRequest forwardPayment, String returnControllerURL) {
        return String.format("%s%s/%s/%s/%s", TreasurySettings.getInstance().getForwardPaymentReturnDefaultURL(),
                returnControllerURL, forwardPayment.getExternalId(), ACTION_RETURN_URL,
                forwardPayment.getReturnForwardPaymentUrlChecksum());
    }

    private void saveReturnUrlChecksum(ForwardPaymentRequest forwardPayment, String returnControllerURL, HttpSession session) {
        final String returnUrlToChecksum =
                String.format("%s%s/%s", TreasurySettings.getInstance().getForwardPaymentReturnDefaultURL(), returnControllerURL,
                        forwardPayment.getExternalId());

        String urlChecksum = GenericChecksumRewriter.calculateChecksum(returnUrlToChecksum, session);
        forwardPayment.setReturnForwardPaymentUrlChecksum(urlChecksum);
    }

    public static String getCancelURL(final ForwardPaymentRequest forwardPayment, final String returnControllerURL) {
        return String.format("%s%s/%s/%s/%s", TreasurySettings.getInstance().getForwardPaymentReturnDefaultURL(),
                returnControllerURL, forwardPayment.getExternalId(), ACTION_CANCEL_URL,
                forwardPayment.getReturnForwardPaymentUrlChecksum());
    }

    public boolean isActionReturn(final String action) {
        return ACTION_RETURN_URL.equals(action);
    }

    public boolean isActionCancel(final String action) {
        return ACTION_CANCEL_URL.equals(action);
    }

    @Atomic
    public boolean doWebPayment(ForwardPaymentRequest forwardPayment, String returnControllerURL, HttpSession session) {
        if (!forwardPayment.getDigitalPaymentPlatform().isActive()) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.not.active");
        }

        String returnUrl;
        String cancelUrl;

        // The return url can be different between Spring and OMNIS, 
        // and session is needed and not needed respectively
        if (session != null) {
            // Spring
            saveReturnUrlChecksum(forwardPayment, returnControllerURL, session);
            returnUrl = PaylineConfiguration.getReturnURL(forwardPayment, returnControllerURL);
            cancelUrl = PaylineConfiguration.getCancelURL(forwardPayment, returnControllerURL);
        } else {
            // OMNIS
            returnUrl = forwardPayment.getForwardPaymentSuccessUrl();
            cancelUrl = forwardPayment.getForwardPaymentInsuccessUrl();
        }

        PaylineWebServiceResponse response = paylineDoWebPayment(forwardPayment, returnUrl, cancelUrl);

        final boolean success = TRANSACTION_APPROVED_CODE.equals(response.getResultCode());

        if (!success) {
            forwardPayment.reject("requestPayment", response.getResultCode(), response.getResultLongMessage(),
                    response.getJsonRequest(), response.getJsonResponse());

            return false;
        }

        final String code = response.getResultCode();
        final String longMessage = response.getResultLongMessage();

        forwardPayment.advanceToRequestState("doWebPayment", code, longMessage, response.getJsonRequest(),
                response.getJsonResponse());
        forwardPayment.setCheckoutId(response.getToken());
        forwardPayment.setRedirectUrl(response.getRedirectURL());

        return true;
    }

    public boolean processPayment(ForwardPaymentRequest forwardPayment, String action) {

        if (!isActionReturn(action)) {
            PaylineWebServiceResponse response = paylineGetWebPaymentDetails(forwardPayment);
            String statusCode = response.getResultCode();
            String statusMessage =
                    treasuryBundle("label.PaylineImplementation.cancelled") + ": " + response.getResultLongMessage();
            reject(forwardPayment, action, statusCode, statusMessage, response.getJsonRequest(), response.getJsonResponse());

            return false;
        }

        PaylineWebServiceResponse response = paylineGetWebPaymentDetails(forwardPayment);

        String statusCode = response.getResultCode();
        String statusMessage = response.getResultLongMessage();
        final boolean success = TRANSACTION_APPROVED_CODE.equals(statusCode);

        if (!success) {
            reject(forwardPayment, action, statusCode, statusMessage, response.getJsonRequest(), response.getJsonResponse());
            return false;
        }

        final String transactionId = response.getTransactionId();
        final String authorizationNumber = response.getAuthorizationNumber();

        final DateTime transactionDate = response.getTransactionDate();
        final BigDecimal paidAmount = response.getPaymentAmount();

        advanceToPaidState(forwardPayment, statusCode, statusMessage, paidAmount, transactionDate, transactionId,
                authorizationNumber, response.getJsonRequest(), response.getJsonResponse(), null);

        return true;
    }

    @Atomic
    private PaymentRequestLog reject(ForwardPaymentRequest forwardPayment, String operationCode, String statusCode,
            String errorMessage, String requestBody, String responseBody) {
        return forwardPayment.reject(operationCode, statusCode, errorMessage, requestBody, responseBody);
    }

    @Atomic
    private PaymentRequestLog advanceToPaidState(ForwardPaymentRequest forwardPayment, String statusCode, String statusMessage,
            BigDecimal paidAmount, DateTime transactionDate, String transactionId, String authorizationNumber, String requestBody,
            String responseBody, String justification) {
        return forwardPayment.advanceToPaidState(statusCode, statusMessage, paidAmount, transactionDate, transactionId,
                authorizationNumber, requestBody, responseBody, null);
    }

    @Override
    public int getMaximumLengthForAddressStreetFieldOne() {
        return 50;
    }

    @Override
    public int getMaximumLengthForAddressCity() {
        return 50;
    }

    @Override
    public int getMaximumLengthForPostalCode() {
        return 16;
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     *
     */
    // @formatter:on

    public static PaylineConfiguration create(FinantialInstitution finantialInstitution, FinantialEntity finantialEntity,
            String name, boolean active, String paymentURL, String paylineMerchantId, String paylineMerchantAccessKey,
            String paylineContractNumber) {
        return new PaylineConfiguration(finantialInstitution, finantialEntity, name, active, paymentURL, paylineMerchantId,
                paylineMerchantAccessKey, paylineContractNumber);
    }

    public static String getPresentationName() {
        return TreasuryConstants.treasuryBundle("label.PaylineConfiguration.presentationName");
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
                createForwardPaymentRequest(bean, successUrlFunction, insuccessUrlFunction, debitEntries, installments);

        doWebPayment(forwardPaymentRequest, null /*forwardPaymentRequest.getForwardPaymentSuccessUrl()*/, null);

        return forwardPaymentRequest;
    }

    @Atomic
    private ForwardPaymentRequest createForwardPaymentRequest(SettlementNoteBean bean,
            Function<ForwardPaymentRequest, String> successUrlFunction,
            Function<ForwardPaymentRequest, String> insuccessUrlFunction, Set<DebitEntry> debitEntries,
            Set<Installment> installments) {
        ForwardPaymentRequest forwardPaymentRequest =
                ForwardPaymentRequest.create(bean.getDigitalPaymentPlatform(), bean.getDebtAccount(), debitEntries, installments,
                        bean.getTotalAmountToPay(), successUrlFunction, insuccessUrlFunction);

        return forwardPaymentRequest;
    }

    @Override
    public List<? extends DigitalPlatformResultBean> getPaymentTransactionsReportListByMerchantId(String merchantTransationId) {
        return Collections.emptyList();
    }

    @Override
    public PostProcessPaymentStatusBean processForwardPayment(ForwardPaymentRequest forwardPayment) {
        return postProcessPayment(forwardPayment, "", null);
    }

    @Override
    public PostProcessPaymentStatusBean processForwardPaymentFromWebhook(PaymentRequestLog log, DigitalPlatformResultBean bean) {
        throw new RuntimeException("not implemented");
    }

    private PaylineWebServiceResponse paylineGetWebPaymentDetails(ForwardPaymentRequest forwardPaymentRequest) {
        final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");

        GetWebPaymentDetailsRequest request = new GetWebPaymentDetailsRequest();
        request.setToken(forwardPaymentRequest.getCheckoutId());

        GetWebPaymentDetailsResponse response = new PaylineWebServiceClient().getClient().getWebPaymentDetails(request);

        PaylineWebServiceResponse result = new PaylineWebServiceResponse();

        if (response.getResult() != null) {
            result.setResultCode(response.getResult().getCode());
            result.setResultLongMessage(response.getResult().getLongMessage());
        }

        if (response.getAuthorization() != null) {
            result.setAuthorizationNumber(response.getAuthorization().getNumber());
        }

        if (response.getAuthorization() != null && !Strings.isNullOrEmpty(response.getAuthorization().getDate())) {
            result.setAuthorizationDate(DATE_TIME_PATTERN.parseDateTime(response.getAuthorization().getDate()));
        }

        if (response.getPayment() != null && !Strings.isNullOrEmpty(response.getPayment().getAmount())) {
            result.setPaymentAmount(new BigDecimal(response.getPayment().getAmount()).divide(new BigDecimal("100")));
        }

        if (response.getTransaction() != null) {
            result.setTransactionId(response.getTransaction().getId());
        }

        if (response.getTransaction() != null && !Strings.isNullOrEmpty(response.getTransaction().getDate())) {
            result.setTransactionDate(DATE_TIME_PATTERN.parseDateTime(response.getTransaction().getDate()));
        }

        // ANIL 2024-09-19 (#qubIT-Fenix-5824)
        //
        // In development environment, it might throw a error due to
        // the JDK module java.xml . Check the task for more details

        result.setJsonRequest(TreasuryConstants.json(request));
        result.setJsonResponse(TreasuryConstants.json(response));

        return result;
    }

    private PaylineWebServiceResponse paylineDoWebPayment(ForwardPaymentRequest forwardPaymentRequest, String returnUrl,
            String cancelUrl) {
        final int PAYLINE_MAX_PHONE_SIZE = 14;

        final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");

        final String SECURITY_MODE = "SSL";
        final String PT = "PT";
        final String EURO_CURRENCY = "978";
        final String ACTION_AUTHORIZATION_AND_VALIDATION = "101";
        final String MODE_CPT = "CPT";
        final String TRANSACTION_APPROVED_CODE = "00000";
        final String TRANSACTION_PENDING_FORM_FILL = "02306";

        final String ACTION_RETURN_URL = "return";
        final String ACTION_CANCEL_URL = "cancel";
        final String LANG_PT = "pt";
        final String LANG_EN = "en";

        String formattedAmount =
                forwardPaymentRequest.getPayableAmount().multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN)
                        .toString();

        final Payment paymentDetails = new Payment();
        paymentDetails.setAmount(formattedAmount);
        paymentDetails.setCurrency(EURO_CURRENCY);
        paymentDetails.setAction(ACTION_AUTHORIZATION_AND_VALIDATION);
        paymentDetails.setMode(MODE_CPT);

        paymentDetails.setContractNumber(
                ((PaylineConfiguration) forwardPaymentRequest.getDigitalPaymentPlatform()).getPaylineContractNumber());

        final Order order = new Order();
        order.setRef(String.valueOf(forwardPaymentRequest.getOrderNumber()));
        order.setAmount(formattedAmount);
        order.setCurrency(EURO_CURRENCY);
        order.setDate(FenixEDUTreasuryPlatformDependentServices.getVersioningCreationDate(forwardPaymentRequest)
                .toString("dd/MM/yyyy HH:mm"));
        order.setCountry(PT);

        // fillOrderDetails(forwardPayment, order);

        final Customer customer = forwardPaymentRequest.getDebtAccount().getCustomer();

        final Buyer buyerDetails = new Buyer();
        buyerDetails.setFirstName(customer.getFirstNames());
        buyerDetails.setLastName(customer.getLastNames());

        if (!Strings.isNullOrEmpty(customer.getEmail()) && EmailValidator.getInstance().isValid(customer.getEmail())) {
            buyerDetails.setEmail(customer.getEmail());
        }

        if (!Strings.isNullOrEmpty(customer.getPhoneNumber())) {
            String phone = customer.getPhoneNumber().replaceAll("[^\\d]", "");

            if (phone.length() > PAYLINE_MAX_PHONE_SIZE) {
                phone = phone.substring(0, PAYLINE_MAX_PHONE_SIZE);
            }

            buyerDetails.setMobilePhone(phone);

        }

        // fillAddress(customer, buyerDetails);

        final DoWebPaymentRequest request = new DoWebPaymentRequest();

        request.setPayment(paymentDetails);
        request.setOrder(order);
        request.setReturnURL(returnUrl);
        request.setCancelURL(cancelUrl);

        final String languageToUse = "en".equals(I18N.getLocale().getLanguage()) ? LANG_EN : LANG_PT;
        request.setLanguageCode(languageToUse);

        request.setBuyer(buyerDetails);
        request.setSecurityMode(SECURITY_MODE);

        final DoWebPaymentResponse response = new PaylineWebServiceClient().getClient().doWebPayment(request);

        PaylineWebServiceResponse result = new PaylineWebServiceResponse();

        if (response.getResult() != null) {
            result.setResultCode(response.getResult().getCode());
            result.setResultLongMessage(response.getResult().getLongMessage());
        }

        result.setToken(response.getToken());
        result.setRedirectURL(response.getRedirectURL());

        result.setJsonRequest(TreasuryConstants.json(request));
        result.setJsonResponse(TreasuryConstants.json(response));

        return result;
    }

    private void fillAddress(final Customer customer, final Buyer buyerDetails) {
        final Address address = new Address();
        address.setStreet1(customer.getAddress());
        address.setZipCode(customer.getZipCode());
        address.setCountry(customer.getAddressCountryCode());
        buyerDetails.setShippingAdress(address);
    }

    private void fillOrderDetails(final ForwardPaymentRequest forwardPayment, final Order order) {
        final Currency currency = forwardPayment.getDebtAccount().getFinantialInstitution().getCurrency();
        final Details details = new Details();
        for (final DebitEntry debitEntry : forwardPayment.getDebitEntriesSet()) {
            final OrderDetail orderDetail = new OrderDetail();
            orderDetail.setRef(debitEntry.getExternalId());
            orderDetail.setPrice(Currency.getValueWithScale(debitEntry.getOpenAmount()).multiply(new BigDecimal(100))
                    .setScale(0, RoundingMode.HALF_EVEN).toString());
            orderDetail.setQuantity("1");
            orderDetail.setComment(debitEntry.getDescription());
            details.getDetails().add(orderDetail);
        }

        order.setDetails(details);
    }

}
