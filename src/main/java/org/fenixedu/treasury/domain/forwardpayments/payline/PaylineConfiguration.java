package org.fenixedu.treasury.domain.forwardpayments.payline;


import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentController;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatformPaymentMode;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.GsonBuilder;

import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;
import pt.ist.fenixframework.Atomic;

public class PaylineConfiguration extends PaylineConfiguration_Base implements IForwardPaymentPlatformService {

    public PaylineConfiguration() {
        super();
    }

    protected PaylineConfiguration(FinantialInstitution finantialInstitution, String name, boolean active, String paymentURL,
            String paylineMerchantId, String paylineMerchantAccessKey, String paylineContractNumber) {
        this();

        this.init(finantialInstitution, name, active);

        setPaymentURL(paymentURL);
        setPaylineMerchantId(paylineMerchantId);
        setPaylineMerchantAccessKey(paylineMerchantAccessKey);
        setPaylineContractNumber(paylineContractNumber);

        DigitalPaymentPlatformPaymentMode.create(this, TreasurySettings.getInstance().getCreditCardPaymentMethod());

        checkRules();
    }

    private void checkRules() {
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

        ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();
        PaylineWebServiceResponse response = implementation.paylineGetWebPaymentDetails(forwardPayment);
        
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

        final ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(true, type, 
                response.getResultCode(),
                response.getResultLongMessage(), response.getJsonRequest(), response.getJsonResponse());

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

        if (Strings.isNullOrEmpty(justification)) {
            throw new TreasuryDomainException("label.ManageForwardPayments.postProcessPayment.justification.required");
        }

        if (Lists.newArrayList(ForwardPaymentStateType.CREATED, ForwardPaymentStateType.REQUESTED)
                .contains(paymentStatusBean.getStateType())) {
            // Do nothing
            return new PostProcessPaymentStatusBean(paymentStatusBean, previousState, false);
        }

        final boolean success = TRANSACTION_APPROVED_CODE.equals(paymentStatusBean.getStatusCode());

        if (!paymentStatusBean.isInvocationSuccess()) {
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

    @Override
    public List<ForwardPaymentStatusBean> verifyPaymentStatus(ForwardPaymentRequest forwardPayment) {
        return Collections.singletonList(paymentStatus(forwardPayment));
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

    private void saveReturnUrlChecksum(final ForwardPaymentRequest forwardPayment, final String returnControllerURL,
            final HttpSession session) {
        final String returnUrlToChecksum =
                String.format("%s%s/%s", TreasurySettings.getInstance().getForwardPaymentReturnDefaultURL(), returnControllerURL,
                        forwardPayment.getExternalId());

        forwardPayment
                .setReturnForwardPaymentUrlChecksum(GenericChecksumRewriter.calculateChecksum(returnUrlToChecksum, session));
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

        saveReturnUrlChecksum(forwardPayment, returnControllerURL, session);

        ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();
        PaylineWebServiceResponse response = implementation.paylineDoWebPayment(forwardPayment, returnControllerURL);

        final boolean success = TRANSACTION_APPROVED_CODE.equals(response.getResultCode());

        if (!success) {
            forwardPayment.reject("requestPayment", response.getResultCode(), response.getResultLongMessage(),
                    response.getJsonRequest(), response.getJsonResponse());

            return false;
        }

        final String code = response.getResultCode();
        final String longMessage = response.getResultLongMessage();

        forwardPayment.advanceToRequestState("doWebPayment", code, longMessage, response.getJsonRequest(), response.getJsonResponse());
        forwardPayment.setCheckoutId(response.getToken());
        forwardPayment.setRedirectUrl(response.getRedirectURL());

        return true;
    }

    private String json(final Object obj) {
        GsonBuilder builder = new GsonBuilder();
        builder.addSerializationExclusionStrategy(new ExclusionStrategy() {

            @Override
            public boolean shouldSkipField(FieldAttributes arg0) {
                return false;
            }

            @Override
            public boolean shouldSkipClass(final Class<?> clazz) {
                if (clazz == Class.class) {
                    return true;
                }

                return false;
            }
        });

        return builder.create().toJson(obj);
    }

    @Atomic
    public boolean processPayment(ForwardPaymentRequest forwardPayment, String action) {

        if (!isActionReturn(action)) {
            ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();
            PaylineWebServiceResponse response = implementation.paylineGetWebPaymentDetails(forwardPayment);
            String statusCode = response.getResultCode();
            String statusMessage =
                    treasuryBundle("label.PaylineImplementation.cancelled") + ": " + response.getResultLongMessage();
            forwardPayment.reject(action, statusCode, statusMessage, response.getJsonRequest(), response.getJsonResponse());

            return false;
        }

        ITreasuryPlatformDependentServices implementation = TreasuryPlataformDependentServicesFactory.implementation();
        PaylineWebServiceResponse response = implementation.paylineGetWebPaymentDetails(forwardPayment);

        String statusCode = response.getResultCode();
        String statusMessage = response.getResultLongMessage();
        final boolean success = TRANSACTION_APPROVED_CODE.equals(statusCode);

        if (!success) {
            forwardPayment.reject(action, statusCode, statusMessage, response.getJsonRequest(), response.getJsonResponse());
            return false;
        }

        final String transactionId = response.getTransactionId();
        final String authorizationNumber = response.getAuthorizationNumber();

        final DateTime transactionDate = response.getTransactionDate();
        final BigDecimal paidAmount = response.getPaymentAmount();

        forwardPayment.advanceToPaidState(statusCode, statusMessage, paidAmount, transactionDate, transactionId,
                authorizationNumber, response.getJsonRequest(), response.getJsonResponse(), null);

        return true;
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     * 
     */
    // @formatter:on

    public static PaylineConfiguration create(FinantialInstitution finantialInstitution, String name, boolean active,
            String paymentURL, String paylineMerchantId, String paylineMerchantAccessKey, String paylineContractNumber) {
        return new PaylineConfiguration(finantialInstitution, name, active, paymentURL, paylineMerchantId, paylineMerchantAccessKey,
                paylineContractNumber);
    }

    public static String getPresentationName() {
        return TreasuryConstants.treasuryBundle("label.PaylineConfiguration.presentationName");
    }
    
}
