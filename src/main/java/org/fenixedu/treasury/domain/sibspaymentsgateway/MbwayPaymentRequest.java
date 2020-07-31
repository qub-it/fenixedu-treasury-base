package org.fenixedu.treasury.domain.sibspaymentsgateway;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.MbWayCheckoutResultBean;
import org.fenixedu.onlinepaymentsgateway.api.PaymentStateBean;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibspaymentsgateway.integration.SibsPaymentsGateway;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class MbwayPaymentRequest extends MbwayPaymentRequest_Base {

    public MbwayPaymentRequest() {
        super();
    }

    protected MbwayPaymentRequest(DigitalPaymentPlatform platform, DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            BigDecimal payableAmount, String phoneNumber, String sibsGatewayMerchantTransactionId,
            String sibsGatewayTransactionId) {
        this();

        this.init(platform, debtAccount, debitEntries, payableAmount, TreasurySettings.getInstance().getMbWayPaymentMethod());

        setPhoneNumber(phoneNumber);
        setSibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId);
        setSibsGatewayTransactionId(sibsGatewayTransactionId);

        setState(PaymentReferenceCodeStateType.USED);

        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();
        
        if (StringUtils.isEmpty(getPhoneNumber())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phoneNumber.required");
        }

        if (StringUtils.isEmpty(getSibsGatewayMerchantTransactionId())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsMerchantTransaction.required");
        }
    }

    @Atomic
    private Set<SettlementNote> processPayment(final BigDecimal paidAmount, DateTime paymentDate, String sibsTransactionId,
            String comments) {
        Function<PaymentRequest, Map<String, String>> additionalPropertiesMapFunction =
                (o) -> fillPaymentEntryPropertiesMap(sibsTransactionId);

        this.setState(PaymentReferenceCodeStateType.PROCESSED);

        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            return internalProcessPaymentInNormalPaymentMixingLegacyInvoices(paidAmount, paymentDate, sibsTransactionId, comments,
                    additionalPropertiesMapFunction);
        } else {
            return internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(paidAmount, paymentDate, sibsTransactionId,
                    comments, additionalPropertiesMapFunction);
        }
    }

    @Atomic(mode = TxMode.READ)
    public PaymentTransaction processMbwayTransaction(SibsPaymentsGatewayLog log, PaymentStateBean bean) {
        if (!bean.getMerchantTransactionId().equals(getSibsGatewayMerchantTransactionId())) {
            throw new TreasuryDomainException(
                    "error.MbwayPaymentRequest.processMbwayTransaction.merchantTransactionId.not.equal");
        }

        final BigDecimal paidAmount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate();

        FenixFramework.atomic(() -> {
            log.savePaymentInfo(paidAmount, paymentDate);
        });

        if (paidAmount == null || !TreasuryConstants.isPositive(paidAmount)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.payment.date");
        }

        if (PaymentTransaction.isTransactionDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> {
                log.markAsDuplicatedTransaction();
            });
            return null;
        }

        try {
            return FenixFramework.atomic(() -> {
                final Set<SettlementNote> settlementNotes =
                        processPayment(paidAmount, paymentDate, bean.getTransactionId(), bean.getMerchantTransactionId());
                PaymentTransaction transaction = PaymentTransaction.create(this, bean.getTransactionId(), paymentDate, paidAmount, settlementNotes);
                return transaction;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(String sibsTransactionId) {
        final Map<String, String> result = new HashMap<>();

        result.put("SibsTransactionId", sibsTransactionId);

        return result;
    }

    @Override
    public String fillPaymentEntryMethodId() {
        return null;
    }
    
    @Override
    public PaymentReferenceCodeStateType getCurrentState() {
        return super.getState();
    }

    @Override
    public boolean isInCreatedState() {
        return false;
    }

    @Override
    public boolean isInRequestedState() {
        return getState() == PaymentReferenceCodeStateType.USED;
    }

    @Override
    public boolean isInPaidState() {
        return getState() == PaymentReferenceCodeStateType.PROCESSED;
    }

    @Override
    public boolean isInAnnuledState() {
        return getState() == PaymentReferenceCodeStateType.ANNULLED;
    }

    /* ************ */
    /* * SERVICES * */
    /* ************ */

    public static Stream<MbwayPaymentRequest> findAll() {
        return PaymentRequest.findAll().filter(p -> p instanceof MbwayPaymentRequest).map(MbwayPaymentRequest.class::cast);
    }

    public static Stream<MbwayPaymentRequest> findBySibsGatewayMerchantTransactionId(
            String sibsGatewayMerchantTransactionId) {
        return PaymentRequest.findBySibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId)
                .filter(p -> p instanceof MbwayPaymentRequest).map(MbwayPaymentRequest.class::cast);
    }

    public static Optional<MbwayPaymentRequest> findUniqueBySibsGatewayMerchantTransactionId(
            String sibsGatewayMerchantTransactionId) {
        return findBySibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId).findAny();
    }
    
    public static Stream<? extends PaymentRequest> findBySibsGatewayTransactionId(String sibsGatewayTransactionId) {
        return PaymentRequest.findBySibsGatewayTransactionId(sibsGatewayTransactionId)
                .filter(p -> p instanceof MbwayPaymentRequest).map(MbwayPaymentRequest.class::cast);
    }

    public static Optional<? extends PaymentRequest> findUniqueBySibsGatewayTransactionId(String sibsGatewayTransactionId) {
        return findBySibsGatewayTransactionId(sibsGatewayTransactionId).findAny();
    }

    @Atomic(mode = TxMode.READ)
    public static MbwayPaymentRequest create(SibsPaymentsGateway sibsOnlinePaymentsGateway, DebtAccount debtAccount,
            Set<DebitEntry> debitEntries, String countryPrefix, String localPhoneNumber) {

        if (getReferencedCustomers(debitEntries).size() > 1) {
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

        BigDecimal payableAmount =
                debitEntries.stream().map(e -> e.getOpenAmountWithInterests()).reduce(BigDecimal.ZERO, BigDecimal::add);

        String merchantTransactionId = sibsOnlinePaymentsGateway.generateNewMerchantTransactionId();

        SibsPaymentsGatewayLog log = createLog(merchantTransactionId);

        try {
            FenixFramework.atomic(() -> {
                log.logRequestSendDate();
            });

            final MbWayCheckoutResultBean checkoutResultBean =
                    sibsOnlinePaymentsGateway.generateMbwayReference(payableAmount, merchantTransactionId, phoneNumber);

            FenixFramework.atomic(() -> {
                log.logRequestReceiveDateAndData(checkoutResultBean.getTransactionId(), checkoutResultBean.isOperationSuccess(),
                        false, checkoutResultBean.getPaymentGatewayResultCode(),
                        checkoutResultBean.getOperationResultDescription());
                log.saveRequest(checkoutResultBean.getRequestLog());
                log.saveResponse(checkoutResultBean.getResponseLog());
            });

            if (!checkoutResultBean.isOperationSuccess()) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.request.not.successful");
            }

            MbwayPaymentRequest mbwayPaymentRequest = createMbwayPaymentRequest(sibsOnlinePaymentsGateway, debtAccount,
                    debitEntries, phoneNumber, payableAmount, merchantTransactionId, checkoutResultBean.getTransactionId());

            FenixFramework.atomic(() -> {
                log.setPaymentRequest(mbwayPaymentRequest);
                log.setStateCode(mbwayPaymentRequest.getState().name());
                log.setStateDescription(mbwayPaymentRequest.getState().getDescriptionI18N());
            });

            return mbwayPaymentRequest;

        } catch (Exception e) {
            boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

            FenixFramework.atomic(() -> {

                log.logRequestReceiveDateAndData(null, false, false, null, null);
                log.logException(e);

                if (isOnlinePaymentsGatewayException) {
                    OnlinePaymentsGatewayCommunicationException onlineException = (OnlinePaymentsGatewayCommunicationException) e;
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

    @Atomic(mode = TxMode.WRITE)
    private static SibsPaymentsGatewayLog createLog(String sibsGatewayMerchantTransactionId) {
        return SibsPaymentsGatewayLog.createForMbwayPaymentRequest(sibsGatewayMerchantTransactionId);
    }

    @Atomic(mode = TxMode.WRITE)
    private static MbwayPaymentRequest createMbwayPaymentRequest(SibsPaymentsGateway sibsOnlinePaymentsGateway,
            DebtAccount debtAccount, Set<DebitEntry> debitEntries, String phoneNumber, BigDecimal payableAmount,
            String sibsGatewayMerchantTransactionId, String sibsGatewayTransactionId) {
        return new MbwayPaymentRequest(sibsOnlinePaymentsGateway, debtAccount, debitEntries, payableAmount, phoneNumber,
                sibsGatewayMerchantTransactionId, sibsGatewayTransactionId);
    }

}
