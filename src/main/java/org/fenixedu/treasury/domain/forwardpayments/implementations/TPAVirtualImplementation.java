package org.fenixedu.treasury.domain.forwardpayments.implementations;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPayment;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentConfiguration;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentStateType;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.gson.GsonBuilder;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class TPAVirtualImplementation implements IForwardPaymentImplementation {

    static final String A030 = "A030";
    static final String TPA_VIRTUAL_ID_FIELD = "A001";
    static final String REFERENCE_CODE_FIELD = "C007";
    static final String CURRENCY_FIELD = "A105";
    static final String AMOUNT_FIELD = "A061";
    static final String PAN_FIELD = "C003";
    static final String EXPIRATION_FIELD = "C004";
    static final String OPERATION_STATUS_FIELD = "C016";
    static final String SECURE_HASH_CODE_FIELD = "C013";
    static final String A037 = "A037";

    public static final String AUTHENTICATION_REQUEST_MESSAGE = "H3D0";
    static final String AUTHENTICATION_RESPONSE_MESSAGE = "MH05";
    static final String C016_AUTHENTICATION_REGISTERED_CODE = "01";
    static final String C016_AUTHORIZATION_ACCEPTED_CODE = "02";
    static final String C016_PAYMENT_ACCEPTED_CODE = "03";
    static final String C016_AUTHORIZATION_CANCELLED = "04";
    static final String C016_UNABLE_TO_CONTACT_HOST_RESPONSE_CODE = "99";

    static final String A038 = "A038";
    static final String A038_SUCCESS = "000";

    static final String M020 = "M020";
    static final String M120 = "M120";

    static final String C016 = "C016";
    static final String C016_POS_RESP = "0X";
    static final String C016_NEG_RESP = "99";
    static final int C016_AUTHENTICATED_STATE = 1;
    static final int C016_AUTHORIZED = 2;
    static final int C016_PAYED = 3;
    static final int C016_AUTHORIZED_CANCELLED = 4;

    static final String C026 = "C026";

    static final String M001 = "M001";
    static final String M101 = "M101";

    static final String M002 = "M002";
    static final String M102 = "M102";

    static final String A077 = "A077";
    static final String A078 = "A078";
    static final String A085 = "A085";
    static final String A086 = "A086";

    public static final String EURO_CODE = "9782";

    @Override
    public IForwardPaymentController getForwardPaymentController(final ForwardPayment forwardPayment) {
        return IForwardPaymentController.getForwardPaymentController(forwardPayment);
    }

    @Atomic(mode = TxMode.WRITE)
    public boolean processPayment(final ForwardPayment forwardPayment, Map<String, String> responseMap) {
        if (!forwardPayment.getForwardPaymentConfiguration().isActive()) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.not.active");
        }

        LinkedHashMap<String, String> requestMap = null;

        if (!isAuthenticationResponseMessage(responseMap)) {
            final String errorMessage = errorMessage(responseMap);
            forwardPayment.reject(responseMap.get(C016), errorMessage, null, json(responseMap));
            return false;
        }

        final TPAInvocationUtil tpa = new TPAInvocationUtil(forwardPayment);
        requestMap = Maps.newLinkedHashMap();
        responseMap = tpa.postPaymentStatus(requestMap);

        if (!isPaymentStatusSuccess(responseMap)) {
            final String responseCode = responseCode(responseMap);
            forwardPayment.reject(responseCode, errorMessage(responseMap), json(requestMap), json(responseMap));
            return false;
        }

        int resultCode = Integer.parseInt(responseMap.get(C016));

        if (resultCode == C016_AUTHENTICATED_STATE) {

            requestMap = Maps.newLinkedHashMap();
            responseMap = tpa.postAuthorizationRequest(requestMap);

            if (!isAuthorizationSuccess(responseMap)) {
                final String responseCode = responseCode(responseMap);
                forwardPayment.reject(responseCode, errorMessage(responseMap), json(requestMap), json(responseMap));
                return false;
            }

            forwardPayment.advanceToAuthenticatedState(responseMap.get(C016),
                    treasuryBundle("label.TPAVirtualImplementation.authenticated"), json(requestMap), json(responseMap));

            resultCode = Integer.parseInt(responseMap.get(C016));
        }

        if (resultCode == C016_AUTHORIZED) {
            requestMap = Maps.newLinkedHashMap();
            responseMap = tpa.postPaymentStatus(requestMap);

            if (!isPaymentStatusSuccess(responseMap)) {
                final String responseCode = responseCode(responseMap);
                forwardPayment.reject(responseCode, errorMessage(responseMap), json(requestMap), json(responseMap));
                return false;
            }

            forwardPayment.advanceToAuthorizedState(String.valueOf(resultCode),
                    treasuryBundle("label.TPAVirtualImplementation.authorized"), json(requestMap), json(responseMap));

            final DateTime authorizationDate = authorizationSibsDate(responseMap);
            forwardPayment.setAuthorizationDate(authorizationDate);

            requestMap = Maps.newLinkedHashMap();
            responseMap = tpa.postPayment(authorizationDate, requestMap);

            if (!isPaymentSuccess(responseMap)) {
                final String responseCode = responseCode(responseMap);
                forwardPayment.reject(responseCode, errorMessage(responseMap), json(requestMap), json(responseMap));
                return false;
            } else {
                final String responseCode = responseCode(responseMap);
                forwardPayment.log(responseCode, errorMessage(responseMap), json(requestMap), json(responseMap));
            }

            resultCode = Integer.parseInt(responseMap.get(C016));
        }

        if (resultCode == C016_PAYED) {
            requestMap = Maps.newLinkedHashMap();
            responseMap = tpa.postPaymentStatus(requestMap);

            if (!isPaymentStatusSuccess(responseMap)) {
                final String responseCode = responseCode(responseMap);
                forwardPayment.reject(responseCode, errorMessage(responseMap), json(requestMap), json(responseMap));
                return false;
            }

            final String transactionId = transactionId(responseMap);
            final DateTime transactionDate = forwardPayment.getAuthorizationDate();
            final BigDecimal payedAmount = payedAmount(responseMap);

            forwardPayment.advanceToPayedState(String.valueOf(resultCode), treasuryBundle("label.TPAVirtualImplementation.payed"),
                    payedAmount, transactionDate, transactionId, null, json(requestMap), json(responseMap), null);

            return true;
        }

        forwardPayment.reject(String.valueOf(resultCode), errorMessage(responseMap), json(requestMap), json(responseMap));

        return false;
    }

    @Override
    public ForwardPaymentStatusBean paymentStatus(final ForwardPayment forwardPayment) {
        if (!forwardPayment.getForwardPaymentConfiguration().isActive()) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.not.active");
        }

        final LinkedHashMap<String, String> requestMap = Maps.newLinkedHashMap();
        final TPAInvocationUtil tpa = new TPAInvocationUtil(forwardPayment);
        final Map<String, String> responseMap = tpa.postPaymentStatus(requestMap);

        final String responseCode = responseCode(responseMap);

        if (!isPaymentStatusSuccess(responseMap)) {
            return new ForwardPaymentStatusBean(isPaymentStatusSuccess(responseMap), null, responseCode,
                    errorMessage(responseMap), json(requestMap), json(responseMap));
        }

        ForwardPaymentStateType type = null;

        DateTime authorizationDate = null;

        String transactionId = null;
        DateTime transactionDate = null;
        BigDecimal payedAmount = null;

        int resultCode = Integer.parseInt(responseMap.get(C016));

        if (resultCode == C016_AUTHENTICATED_STATE) {
            type = ForwardPaymentStateType.AUTHENTICATED;
        } else if (resultCode == C016_AUTHORIZED) {
            type = ForwardPaymentStateType.AUTHORIZED;
            authorizationDate = authorizationSibsDate(responseMap);
        } else if (resultCode == C016_PAYED) {
            type = ForwardPaymentStateType.PAYED;
            authorizationDate = authorizationSibsDate(responseMap);

            transactionId = transactionId(responseMap);
            transactionDate = forwardPayment.getAuthorizationDate();
            payedAmount = payedAmount(responseMap);
        } else if (resultCode == C016_AUTHORIZED_CANCELLED) {
            type = ForwardPaymentStateType.REJECTED;
        }

        final ForwardPaymentStatusBean bean = new ForwardPaymentStatusBean(isPaymentStatusSuccess(responseMap), type,
                responseCode, errorMessage(responseMap), json(requestMap), json(responseMap));

        bean.editAuthorizationDetails(null, authorizationDate);
        bean.editTransactionDetails(transactionId, transactionDate, payedAmount);

        return bean;
    }

    private String errorMessage(final Map<String, String> responseData) {

        if (responseData.containsKey(A086) && !Strings.isNullOrEmpty(responseData.get(A086))) {
            return responseData.get(A086);
        }

        final StringBuilder sb = new StringBuilder();
        if (responseData.containsKey(A085) && !Strings.isNullOrEmpty(responseData.get(A085))) {
            sb.append(String.format("[%s: %s]", A085, responseData.get(A085)));
        }

        if (responseData.containsKey(A077) && !Strings.isNullOrEmpty(responseData.get(A077))) {
            sb.append(String.format("[%s: %s] ", A077, responseData.get(A077)));
        }

        if (responseData.containsKey(A078) && !Strings.isNullOrEmpty(responseData.get(A078))) {
            sb.append(String.format("[%s: %s] ", A078, responseData.get(A078)));
        }

        if (sb.length() == 0) {
            sb.append(treasuryBundle("label.TPAVirtualImplementation.error.not.available"));
        }

        return sb.toString();
    }

    public Map<String, String> mapAuthenticationRequest(final ForwardPayment forwardPayment) {
        final TPAInvocationUtil tpa = new TPAInvocationUtil(forwardPayment);
        return tpa.mapAuthenticationRequest();
    }

    private BigDecimal payedAmount(Map<String, String> responseMap) {
        if (!Strings.isNullOrEmpty(responseMap.get(AMOUNT_FIELD))) {
            return new BigDecimal(responseMap.get(AMOUNT_FIELD));
        }

        return null;
    }

    private String transactionId(Map<String, String> responseMap) {
        if (!Strings.isNullOrEmpty(responseMap.get(C026))) {
            return responseMap.get(C026);
        }

        return null;
    }

    private DateTime authorizationSibsDate(final Map<String, String> responseMap) {
        if (!responseMap.containsKey(A037)) {
            return null;
        }

        return DateTimeFormat.forPattern("YYYYMMddHHmmss").parseDateTime(responseMap.get(A037));
    }

    private boolean isResponseSuccess(final Map<String, String> responseMap) {
        if (responseMap.get(A038) == null) {
            return false;
        }

        try {
            return Integer.parseInt(responseMap.get(A038)) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isPaymentStatusSuccess(final Map<String, String> responseMap) {
        return isResponseSuccess(responseMap) && responseMap.get(A030) != null && M120.equals(responseMap.get(A030));
    }

    private boolean isAuthorizationSuccess(Map<String, String> responseMap) {
        return isResponseSuccess(responseMap) && responseMap.containsKey(A030) && M101.equals(responseMap.get(A030));
    }

    private boolean isPaymentSuccess(final Map<String, String> responseMap) {
        return isResponseSuccess(responseMap) && responseMap.containsKey(A030) && M102.equals(responseMap.get(A030));
    }

    private boolean isAuthenticationResponseMessage(Map<String, String> responseMap) {
        return responseMap.containsKey(A030) && AUTHENTICATION_RESPONSE_MESSAGE.equals(responseMap.get(A030));
    }

    private String responseCode(final Map<String, String> responseMap) {
        if (!responseMap.containsKey(A038)) {
            return null;
        }

        return responseMap.get(A038);
    }

    @Override
    public String getPaymentURL(final ForwardPayment forwardPayment) {
        return forwardPayment.getForwardPaymentConfiguration().getPaymentURL();
    }

    public String getReturnURL(final ForwardPayment forwardPayment) {
        return String.format("%s/%s", forwardPayment.getForwardPaymentConfiguration().getReturnURL(),
                forwardPayment.getExternalId());
    }

    @Override
    public String getFormattedAmount(final ForwardPayment forwardPayment) {
        return forwardPayment.getAmount().toString();
    }

    private String json(final Object obj) {
        GsonBuilder builder = new GsonBuilder();
        return builder.create().toJson(obj);
    }

    @Override
    public String getLogosJspPage(final ForwardPaymentConfiguration forwardPaymentConfiguration) {
        return "implementations/tpavirtual/logos.jsp";
    }

    @Override
    public String getWarningBeforeRedirectionJspPage() {
        return null;
    }

    @Override
    @Atomic
    public PostProcessPaymentStatusBean postProcessPayment(final ForwardPayment forwardPayment, final String justification,
            final Optional<String> specificTransactionId) {
        if (true) {
            throw new TreasuryDomainException("label.ManageForwardPayments.postProcessPayment.not.supported.yet");
        }

        final ForwardPaymentStateType previousState = forwardPayment.getCurrentState();
        final ForwardPaymentStatusBean paymentStatusBean =
                forwardPayment.getForwardPaymentConfiguration().implementation().paymentStatus(forwardPayment);

        if (!forwardPayment.getCurrentState().isInStateToPostProcessPayment() || !paymentStatusBean.isInPayedState()) {
            throw new TreasuryDomainException("label.ManageForwardPayments.forwardPayment.not.created.nor.payed.in.platform");
        }

        if (Strings.isNullOrEmpty(justification)) {
            throw new TreasuryDomainException("label.ManageForwardPayments.postProcessPayment.justification.required");
        }

        final TPAInvocationUtil tpa = new TPAInvocationUtil(forwardPayment);
        final LinkedHashMap<String, String> requestMap = Maps.newLinkedHashMap();
        final Map<String, String> responseMap = tpa.postPaymentStatus(requestMap);

        if (!isPaymentStatusSuccess(responseMap)) {
            final String responseCode = responseCode(responseMap);
            forwardPayment.reject(responseCode, errorMessage(responseMap), json(requestMap), json(responseMap));

            return new PostProcessPaymentStatusBean(paymentStatusBean, previousState, false);
        }

        int resultCode = Integer.parseInt(responseMap.get(C016));

        if (resultCode != C016_PAYED) {
            throw new TreasuryDomainException("label.ManageForwardPayments.forwardPayment.not.created.nor.payed.in.platform");
        }

        final String transactionId = paymentStatusBean.getTransactionId();
        final DateTime transactionDate = paymentStatusBean.getAuthorizationDate();
        final BigDecimal payedAmount = paymentStatusBean.getPayedAmount();

        forwardPayment.advanceToPayedState(String.valueOf(resultCode), treasuryBundle("label.TPAVirtualImplementation.payed"),
                payedAmount, transactionDate, transactionId, null, json(requestMap), json(responseMap), justification);

        return new PostProcessPaymentStatusBean(paymentStatusBean, previousState, true);
    }

    @Override
    public String getImplementationCode() {
        return "TPAVIRTUAL";
    }

    @Override
    public List<ForwardPaymentStatusBean> verifyPaymentStatus(ForwardPayment forwardPayment) {
        return Collections.singletonList(paymentStatus(forwardPayment));
    }

}
