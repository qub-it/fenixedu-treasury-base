package org.fenixedu.treasury.domain.forwardpayments.payline;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentController;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PaylineWebServiceClient;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PostProcessPaymentStatusBean;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatformPaymentMode;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Address;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Buyer;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Details;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.DoWebPaymentRequest;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.DoWebPaymentResponse;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.GetWebPaymentDetailsRequest;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.GetWebPaymentDetailsResponse;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Order;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.OrderDetail;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.Payment;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

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

        final GetWebPaymentDetailsRequest request = new GetWebPaymentDetailsRequest();
        request.setToken(forwardPayment.getPaylineToken());

        final GetWebPaymentDetailsResponse response = new PaylineWebServiceClient().getClient().getWebPaymentDetails(request);

        ForwardPaymentStateType type = null;

        String authorizationNumber = null;
        DateTime authorizationDate = null;

        String transactionId = null;
        DateTime transactionDate = null;
        BigDecimal payedAmount = null;

        final boolean success = TRANSACTION_APPROVED_CODE.equals(response.getResult().getCode());
        if (!success) {
            if (TRANSACTION_PENDING_FORM_FILL.equals(response.getResult().getCode())) {
                type = ForwardPaymentStateType.REQUESTED;
            } else {
                type = ForwardPaymentStateType.REJECTED;
            }
        } else {
            authorizationNumber = response.getAuthorization().getNumber();
            if (response.getAuthorization() != null && !Strings.isNullOrEmpty(response.getAuthorization().getDate())) {
                authorizationDate = DATE_TIME_PATTERN.parseDateTime(response.getAuthorization().getDate());
            }

            transactionId = response.getTransaction().getId();

            if (response.getPayment() != null && !Strings.isNullOrEmpty(response.getPayment().getAmount())) {
                payedAmount = new BigDecimal(response.getPayment().getAmount()).divide(new BigDecimal("100"));
            }

            if (response.getPayment() != null && !Strings.isNullOrEmpty(response.getTransaction().getDate())) {
                transactionDate = DATE_TIME_PATTERN.parseDateTime(response.getTransaction().getDate());
            }

            type = ForwardPaymentStateType.PAYED;
        }

        final ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(true, type, response.getResult().getCode(),
                response.getResult().getLongMessage(), json(request), json(response));

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
                forwardPayment.getDigitalPaymentPlatform().getForwardPaymentPlatformService().paymentStatus(forwardPayment);

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

    private static final int PAYLINE_MAX_PHONE_SIZE = 14;

    private static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");

    private static final String SECURITY_MODE = "SSL";
    private static final String PT = "PT";
    private static final String EURO_CURRENCY = "978";
    private static final String ACTION_AUTHORIZATION_AND_VALIDATION = "101";
    private static final String MODE_CPT = "CPT";
    private static final String TRANSACTION_APPROVED_CODE = "00000";
    private static final String TRANSACTION_PENDING_FORM_FILL = "02306";

    public static final String ACTION_RETURN_URL = "return";
    public static final String ACTION_CANCEL_URL = "cancel";
    public static final String LANG_PT = "pt";
    public static final String LANG_EN = "en";

    private String getReturnURL(ForwardPaymentRequest forwardPayment, String returnControllerURL) {
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

    public String getCancelURL(final ForwardPaymentRequest forwardPayment, final String returnControllerURL) {
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

        String formattedAmount =
                forwardPayment.getPayableAmount().multiply(new BigDecimal(100)).setScale(0, RoundingMode.HALF_EVEN).toString();

        final Payment paymentDetails = new Payment();
        paymentDetails.setAmount(formattedAmount);
        paymentDetails.setCurrency(EURO_CURRENCY);
        paymentDetails.setAction(ACTION_AUTHORIZATION_AND_VALIDATION);
        paymentDetails.setMode(MODE_CPT);

        paymentDetails.setContractNumber(
                ((PaylineConfiguration) forwardPayment.getDigitalPaymentPlatform()).getPaylineContractNumber());

        final Order order = new Order();
        order.setRef(String.valueOf(forwardPayment.getOrderNumber()));
        order.setAmount(formattedAmount);
        order.setCurrency(EURO_CURRENCY);
        order.setDate(TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(forwardPayment)
                .toString("dd/MM/yyyy HH:mm"));
        order.setCountry(PT);

        // fillOrderDetails(forwardPayment, order);

        final Customer customer = forwardPayment.getDebtAccount().getCustomer();

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
        request.setReturnURL(getReturnURL(forwardPayment, returnControllerURL));
        request.setCancelURL(getCancelURL(forwardPayment, returnControllerURL));

        final String languageToUse = "en".equals(I18N.getLocale().getLanguage()) ? LANG_EN : LANG_PT;
        request.setLanguageCode(languageToUse);

        request.setBuyer(buyerDetails);
        request.setSecurityMode(SECURITY_MODE);

        final DoWebPaymentResponse response = new PaylineWebServiceClient().getClient().doWebPayment(request);

        final boolean success = response != null && response.getResult() != null
                && TRANSACTION_APPROVED_CODE.equals(response.getResult().getCode());

        if (!success) {
            forwardPayment.reject("requestPayment", response.getResult().getCode(), response.getResult().getLongMessage(),
                    json(request), json(response));

            return false;
        }

        final String code = response.getResult().getCode();
        final String longMessage = response.getResult().getLongMessage();

        forwardPayment.advanceToRequestState("doWebPayment", code, longMessage, json(request), json(response));
        forwardPayment.setPaylineToken(response.getToken());
        forwardPayment.setPaylineRedirectUrl(response.getRedirectURL());

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
            final GetWebPaymentDetailsRequest request = new GetWebPaymentDetailsRequest();
            request.setToken(forwardPayment.getPaylineToken());

            final GetWebPaymentDetailsResponse response = new PaylineWebServiceClient().getClient().getWebPaymentDetails(request);
            String statusCode = response.getResult().getCode();
            String statusMessage =
                    treasuryBundle("label.PaylineImplementation.cancelled") + ": " + response.getResult().getLongMessage();
            forwardPayment.reject(action, statusCode, statusMessage, json(request), json(response));

            return false;
        }

        final GetWebPaymentDetailsRequest request = new GetWebPaymentDetailsRequest();
        request.setToken(forwardPayment.getPaylineToken());

        final GetWebPaymentDetailsResponse response = new PaylineWebServiceClient().getClient().getWebPaymentDetails(request);

        String statusCode = response.getResult().getCode();
        String statusMessage = response.getResult().getLongMessage();
        final boolean success = TRANSACTION_APPROVED_CODE.equals(statusCode);

        if (!success) {
            forwardPayment.reject(action, statusCode, statusMessage, json(request), json(response));
            return false;
        }

        final String transactionId = response.getTransaction().getId();
        final String authorizationNumber = response.getAuthorization().getNumber();

        final DateTime transactionDate = DATE_TIME_PATTERN.parseDateTime(response.getTransaction().getDate());
        final BigDecimal paidAmount = new BigDecimal(response.getPayment().getAmount()).divide(new BigDecimal("100"));

        forwardPayment.advanceToPaidState(statusCode, statusMessage, paidAmount, transactionDate, transactionId,
                authorizationNumber, json(request), json(response), null);

        return true;
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
