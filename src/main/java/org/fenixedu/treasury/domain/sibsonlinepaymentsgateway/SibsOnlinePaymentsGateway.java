package org.fenixedu.treasury.domain.sibsonlinepaymentsgateway;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
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
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentConfiguration;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class SibsOnlinePaymentsGateway extends SibsOnlinePaymentsGateway_Base {

    private static final int DEFAULT_NUM_MONTHS_PAYMENT_REFERENCE_CODE_EXPIRATION = 12;

    public SibsOnlinePaymentsGateway() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected SibsOnlinePaymentsGateway(final PaymentCodePool paymentCodePool,
            final ForwardPaymentConfiguration forwardPaymentConfiguration, final String sibsEntityId,
            final String sibsEndpointUrl, final String merchantTransactionIdPrefix, final String bearerToken, final String aesKey,
            final PaymentMethod mbwayPaymentMethod, final DocumentNumberSeries mbwayDocumentSeries, final boolean mbwayActive) {
        this();

        setPaymentCodePool(paymentCodePool);
        setForwardPaymentConfiguration(forwardPaymentConfiguration);
        setSibsEntityId(sibsEntityId);
        setSibsEndpointUrl(sibsEndpointUrl);
        setMerchantTransactionIdPrefix(merchantTransactionIdPrefix);
        setBearerToken(bearerToken);
        setAesKey(aesKey);

        setMbwayPaymentMethod(mbwayPaymentMethod);
        setMbwayDocumentSeries(mbwayDocumentSeries);
        setMbwayActive(mbwayActive);
        setNumberOfMonthsToExpirePaymentReferenceCode(DEFAULT_NUM_MONTHS_PAYMENT_REFERENCE_CODE_EXPIRATION);

        checkRules();
    }

    private void checkRules() {

        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.domainRoot.required");
        }

        if (getPaymentCodePool() == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.paymentCodePool.required");
        }

        if (getForwardPaymentConfiguration() == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.forwardPaymentConfiguration.required");
        }

        if (getPaymentCodePool().getFinantialInstitution() != getForwardPaymentConfiguration().getFinantialInstitution()) {
            throw new TreasuryDomainException(
                    "error.SibsOnlinePaymentsGateway.pool.and.forward.configuration.not.from.same.finantial.institution");
        }

        if (Strings.isNullOrEmpty(getSibsEntityId())) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.sibsEntityId.required");
        }

        if (Strings.isNullOrEmpty(getSibsEndpointUrl())) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.sibsEndpointUrl.required");
        }

        if (Strings.isNullOrEmpty(getMerchantTransactionIdPrefix())) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.merchantTransactionIdPrefix.required");
        }

        if (getMbwayPaymentMethod() == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.mbwayPaymentMethod.required");
        }

        if (getMbwayDocumentSeries() == null) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.mbwayDocumentSeries.required");
        }
    }

    @Atomic
    public void edit(final String sibsEndpointUrl, final String bearerToken, final String aesKey,
            final PaymentMethod mbwayPaymentMethod, final DocumentNumberSeries mbwayDocumentSeries, final boolean mbwayActive,
            final int numberOfMonthsToExpirePaymentReferenceCode) {
        setSibsEndpointUrl(sibsEndpointUrl);
        setBearerToken(bearerToken);
        setAesKey(aesKey);

        setMbwayPaymentMethod(mbwayPaymentMethod);
        setMbwayDocumentSeries(mbwayDocumentSeries);
        setMbwayActive(mbwayActive);
        setNumberOfMonthsToExpirePaymentReferenceCode(numberOfMonthsToExpirePaymentReferenceCode);
    }
    
    public void delete() {
        if(!getMbwayPaymentRequestsSet().isEmpty()) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.delete.not.possible.due.to.existing.requests");
        }
        
        if(!getSibsOnlinePaymentsGatewayLogsSet().isEmpty()) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGateway.delete.not.possible.due.to.existing.requests");
        }
        
        super.setDomainRoot(null);
        super.setForwardPaymentConfiguration(null);
        super.setMbwayDocumentSeries(null);
        super.setMbwayPaymentMethod(null);
        super.setPaymentCodePool(null);
        
        super.deleteDomainObject();
    }
    
    public boolean isSendBillingDataInOnlinePayment() {
        return getSendBillingDataInOnlinePayment();
    }

    public String generateNewMerchantTransactionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Atomic(mode = TxMode.READ)
    public PaymentStateBean getPaymentStatusBySibsCheckoutId(final String checkoutId)
            throws OnlinePaymentsGatewayCommunicationException {
        final SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        try {
            return gatewayService.getPaymentStatusByCheckoutId(checkoutId);
        } catch (OnlinePaymentsGatewayCommunicationException e) {
            throw new TreasuryDomainException(e,
                    "error.SibsOnlinePaymentsGateway.getPaymentStatusBySibsTransactionId.communication.error");
        }
    }

    @Atomic(mode = TxMode.READ)
    public PaymentStateBean getPaymentStatusBySibsTransactionId(final String transactionId)
            throws OnlinePaymentsGatewayCommunicationException {
        final SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        return gatewayService.getPaymentTransactionReportByTransactionId(transactionId);
    }

    @Atomic(mode = TxMode.READ)
    public List<PaymentStateBean> getPaymentTransactionsReportListByMerchantId(final String merchantId)
            throws OnlinePaymentsGatewayCommunicationException {
        final SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        return gatewayService.getPaymentTransactionsReportListByMerchantId(merchantId);
    }

    @Atomic(mode = TxMode.READ)
    public CheckoutResultBean prepareCheckout(final DebtAccount debtAccount, final String merchantTransactionId, 
            final BigDecimal amount, final String returnUrl,
            final SibsBillingAddressBean billingAddressBean)
            throws OnlinePaymentsGatewayCommunicationException {
        final SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        final PrepareCheckoutInputBean bean = new PrepareCheckoutInputBean(amount, merchantTransactionId, returnUrl,
                new DateTime(), new DateTime().plusDays(7));
        
        if(isSendBillingDataInOnlinePayment()) {
            String customerEmail = debtAccount.getCustomer().getEmail();
            
            bean.fillBillingData(
                    /* debtAccount.getCustomer().getName() */ null, 
                    billingAddressBean.getAddressCountryCode(), 
                    limitTextSize(billingAddressBean.getCity(), SECURE_3D_MAX_CITY_SIZE),
                    limitTextSize(billingAddressBean.getAddress(), SECURE_3D_MAX_ADDRESS_SIZE), 
                    limitTextSize(billingAddressBean.getZipCode(), SECURE_3D_MAX_ZIP_CODE),
                    customerEmail);
        }
        
        bean.setUseCreditCard(true);

        CheckoutResultBean resultBean = gatewayService.prepareOnlinePaymentCheckout(bean);

        return resultBean;
    }
    
    private static final int SECURE_3D_MAX_CITY_SIZE = 50;
    private static final int SECURE_3D_MAX_ADDRESS_SIZE = 50;
    private static final int SECURE_3D_MAX_ZIP_CODE = 16;

    private String limitTextSize(String text, int maxSize) {
        if(text == null) {
            return null;
        }
        
        return text.substring(0, Integer.min(text.length(), maxSize));
    }

    @Atomic(mode = TxMode.READ)
    public MbCheckoutResultBean generateMBPaymentReference(final BigDecimal amount, final DateTime validFrom,
            final DateTime validTo, final String merchantTransactionId) throws OnlinePaymentsGatewayCommunicationException {
        final SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        final MbPrepareCheckoutInputBean inputBean =
                new MbPrepareCheckoutInputBean(amount, merchantTransactionId, validFrom, validTo);

        // Customer data will not be sent due to GDPR
        final CustomerDataInputBean customerInputBean = null;

        final MbCheckoutResultBean requestResult = gatewayService.generateMBPaymentReference(inputBean, customerInputBean);

        return requestResult;
    }

    public MbWayCheckoutResultBean generateMbwayReference(final BigDecimal amount, final String merchantTransactionId,
            final String phoneNumber) throws OnlinePaymentsGatewayCommunicationException {
        final SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        MbWayPrepareCheckoutInputBean inputBean = new MbWayPrepareCheckoutInputBean(amount, merchantTransactionId, phoneNumber);

        inputBean.setAmount(amount);
        inputBean.setMerchantTransactionId(merchantTransactionId);
        inputBean.setPhoneNumber(phoneNumber);

        MbWayCheckoutResultBean mbwayCheckoutResult = gatewayService.generateMbWayPayment(inputBean, null);

        return mbwayCheckoutResult;
    }

    public PaymentStateBean handleWebhookNotificationRequest(final String initializationVector, final String authTag,
            final String encryptedPayload) throws Exception {
        SIBSOnlinePaymentsGatewayService gatewayService = gatewayService();

        PaymentStateBean notificationBean =
                gatewayService.handleNotificationRequest(initializationVector, authTag, encryptedPayload);

        return notificationBean;
    }

    private SIBSOnlinePaymentsGatewayService gatewayService() {
        final SIBSInitializeServiceBean initializeServiceBean = new SIBSInitializeServiceBean(getSibsEntityId(), getBearerToken(),
                getSibsEndpointUrl(), getPaymentCodePool().getEntityReferenceCode(),
                getPaymentCodePool().getFinantialInstitution().getCurrency().getIsoCode(), translateEnviromentMode());

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

    /* ************/
    /* * SERVICES */
    /* ************/

    public static SibsOnlinePaymentsGateway create(final PaymentCodePool paymentCodePool,
            final ForwardPaymentConfiguration forwardPaymentConfiguration, final String sibsEntityId,
            final String sibsEndpointUrl, final String merchantIdPrefix, final String bearerToken, final String aesKey,
            final PaymentMethod mbwayPaymentMethod, final DocumentNumberSeries mbwayDocumentSeries, final boolean mbwayActive) {
        return new SibsOnlinePaymentsGateway(paymentCodePool, forwardPaymentConfiguration, sibsEntityId, sibsEndpointUrl,
                merchantIdPrefix, bearerToken, aesKey, mbwayPaymentMethod, mbwayDocumentSeries, mbwayActive);
    }

    public static Stream<SibsOnlinePaymentsGateway> findAll() {
        return FenixFramework.getDomainRoot().getSibsOnlinePaymentsGatewaySet().stream();
    }

    public static Stream<SibsOnlinePaymentsGateway> findByMerchantIdPrefix(final String merchantIdPrefix) {
        return findAll().filter(e -> merchantIdPrefix.toLowerCase().equals(e.getMerchantTransactionIdPrefix().toLowerCase()));
    }

    public static boolean isMbwayServiceActive(final FinantialInstitution finantialInstitution) {
        Optional<ForwardPaymentConfiguration> optional = ForwardPaymentConfiguration.findUniqueActive(finantialInstitution);

        return optional.isPresent() && optional.get().getSibsOnlinePaymentsGateway() != null
                && Boolean.TRUE.equals(optional.get().getSibsOnlinePaymentsGateway().getMbwayActive());
    }

}
