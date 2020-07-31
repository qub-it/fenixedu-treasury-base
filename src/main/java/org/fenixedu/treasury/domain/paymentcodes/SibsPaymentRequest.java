package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.PaymentStateBean;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibspaymentsgateway.SibsPaymentsGatewayLog;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class SibsPaymentRequest extends SibsPaymentRequest_Base {

    public SibsPaymentRequest() {
        super();
    }

    protected SibsPaymentRequest(SibsReferenceCode sibsReferenceCode, DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, BigDecimal payableAmount) {
        this();
        
        // Double check if sibsReferenceCode is not associated to other sibsPaymentRequest
        if(sibsReferenceCode.getSibsPaymentRequest() != null) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.sibsReferenceCode.not.free");
        }
        
        this.init(sibsReferenceCode.getDigitalPaymentPlatform(), debtAccount, debitEntries, installments, payableAmount,
                TreasurySettings.getInstance().getMbPaymentMethod());

        setEntityReferenceCode(sibsReferenceCode.getEntityReferenceCode());
        setReferenceCode(sibsReferenceCode.getReferenceCode());
        setSibsReferenceCode(sibsReferenceCode);

        setState(PaymentReferenceCodeStateType.USED);

        checkRules();
    }

    protected SibsPaymentRequest(DigitalPaymentPlatform platform, DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, BigDecimal payableAmount, String referenceCode,
            String merchantTransactionId, String transactionId) {

        this();
        this.init(platform, debtAccount, debitEntries, installments, payableAmount,
                TreasurySettings.getInstance().getMbPaymentMethod());

        setEntityReferenceCode(platform.castToSibsPaymentCodePoolService().getEntityReferenceCode());
        setReferenceCode(referenceCode);
        setMerchantTransactionId(merchantTransactionId);
        setTransactionId(transactionId);

        setState(PaymentReferenceCodeStateType.USED);

        if (StringUtils.isEmpty(getMerchantTransactionId())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsMerchantTransaction.required");
        }

        if (StringUtils.isEmpty(getTransactionId())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsMerchantTransaction.required");
        }

        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();

        if (!getDigitalPaymentPlatform().isSibsPaymentCodeServiceSupported()) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.digitalPaymentPlatform.not.supports.sibs.service");
        }
        
        if(getDigitalPaymentPlatform() instanceof SibsPaymentCodePool && getSibsReferenceCode() == null) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.sibsReferenceCode.required");
        }
        
        if(StringUtils.isEmpty(getEntityReferenceCode())) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.entityReferenceCode.required");
        }
        
        if(StringUtils.isEmpty(getReferenceCode())) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.referenceCode.required");
        }
        
        // For now ensure only one sibsPaymentRequest for the same entityCode and referenceCode exists
        // Later we may remove this restriction
        if(find(getEntityReferenceCode(), getReferenceCode()).count() > 1) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.request.already.exists.for.entityCode.and.referenceCode");
        }
    }

    @Override
    public void setSibsReferenceCode(SibsReferenceCode sibsReferenceCode) {
        // Double check if sibsReferenceCode is not associated to other sibsPaymentRequest
        if(sibsReferenceCode.getSibsPaymentRequest() != null && sibsReferenceCode.getSibsPaymentRequest() != this) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.sibsReferenceCode.associated.to.other.sibsPaymentRequest");
        }
        
        super.setSibsReferenceCode(sibsReferenceCode);
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

    public void anull() {
        setState(PaymentReferenceCodeStateType.ANNULLED);
    }

    public String getDescription() {
        List<String> descriptions =
                getOrderedDebitEntries().stream().map(DebitEntry::getDescription).collect(Collectors.toList());
        descriptions
                .addAll(getOrderedInstallments().stream().map(i -> i.getDescription().getContent()).collect(Collectors.toList()));
        return String.join("\n", descriptions);
    }

    public String getFormattedCode() {
        final StringBuilder result = new StringBuilder();
        int i = 1;
        for (char character : getReferenceCode().toCharArray()) {
            result.append(character);
            if (i % 3 == 0) {
                result.append(" ");
            }
            i++;
        }

        return result.charAt(result.length() - 1) == ' ' ? result.deleteCharAt(result.length() - 1).toString() : result
                .toString();
    }

    @Atomic
    public PaymentTransaction processPayment(BigDecimal paidAmount, DateTime paymentDate, String sibsTransactionId,
            String sibsImportationFilename, String sibsMerchantTransactionId, DateTime whenProcessedBySibs,
            SibsReportFile sibsReportFile, boolean checkSibsTransactionIdDuplication) {
        String entityReferenceCode = getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode();

        if (SibsPaymentCodeTransaction.isReferenceProcessingDuplicate(entityReferenceCode, getReferenceCode(), paymentDate)) {
            return null;
        }

        if (checkSibsTransactionIdDuplication
                && SibsPaymentCodeTransaction.isSibsGatewayReferenceProcessingDuplicate(sibsTransactionId)) {
            throw new RuntimeException("Duplicate transaction id: " + sibsTransactionId);
        }

        if (getState() == PaymentReferenceCodeStateType.UNUSED || getState() == PaymentReferenceCodeStateType.USED) {
            setState(PaymentReferenceCodeStateType.PROCESSED);
        }

        Set<SettlementNote> noteSet = new HashSet<>();

        SibsPaymentCodeTransaction transaction = SibsPaymentCodeTransaction.create(sibsReportFile, this, paymentDate, paidAmount,
                sibsTransactionId, whenProcessedBySibs, noteSet);

        Function<PaymentRequest, Map<String, String>> additionalPropertiesMapFunction =
                (o) -> fillPaymentEntryPropertiesMap(sibsTransactionId);

        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            noteSet.addAll(internalProcessPaymentInNormalPaymentMixingLegacyInvoices(paidAmount, paymentDate, sibsTransactionId,
                    sibsImportationFilename, additionalPropertiesMapFunction));
        } else {
            noteSet.addAll(internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(paidAmount, paymentDate,
                    sibsTransactionId, sibsImportationFilename, additionalPropertiesMapFunction));
        }

        for (SettlementNote settlementNote : noteSet) {
            String observations = "";
            if (!StringUtils.isEmpty(sibsMerchantTransactionId)) {
                observations = sibsMerchantTransactionId;
            } else if (!StringUtils.isEmpty(sibsImportationFilename)) {
                observations = String.format("%s [%s]", sibsImportationFilename, getReferenceCode());
            }

            settlementNote.setDocumentObservations(observations);
            if (settlementNote.getAdvancedPaymentCreditNote() != null) {
                settlementNote.getAdvancedPaymentCreditNote().setDocumentObservations(observations);
            }
        }

        transaction.getSettlementNotesSet().addAll(noteSet);

        return transaction;
    }

    @Atomic(mode = TxMode.READ)
    public PaymentTransaction processPaymentReferenceCodeTransaction(final SibsPaymentsGatewayLog log, PaymentStateBean bean) {
        if (!bean.getMerchantTransactionId().equals(getMerchantTransactionId())) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.merchantTransactionId.not.equal");
        }

        FenixFramework.atomic(() -> {
            log.setPaymentRequest(this);
        });

        final BigDecimal paidAmount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate();

        FenixFramework.atomic(() -> {
            log.savePaymentInfo(paidAmount, paymentDate);
        });

        if (paidAmount == null || !TreasuryConstants.isPositive(paidAmount)) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.payment.date");
        }

        String entityReferenceCode = getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode();
        if (SibsPaymentCodeTransaction.isReferenceProcessingDuplicate(entityReferenceCode, getReferenceCode(), paymentDate)) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }

        if (PaymentTransaction.isTransactionDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }

        if (SibsPaymentCodeTransaction.isSibsGatewayReferenceProcessingDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> log.markAsDuplicatedTransaction());
            return null;
        }

        PaymentTransaction paymentTransaction = processPayment(paidAmount, paymentDate, bean.getTransactionId(), null, bean.getMerchantTransactionId(),
                new DateTime(), null, true);
        
        if(paymentTransaction != null) {
            FenixFramework.atomic(() -> log.setPaymentTransaction(paymentTransaction));
        }
        
        return paymentTransaction;
    }

    @Override
    public String fillPaymentEntryMethodId() {
        // ANIL (2017-09-13) Required by used ERP at this date
        return String.format("COB PAG SERV %s",
                ((ISibsPaymentCodePoolService) getDigitalPaymentPlatform()).getEntityReferenceCode());
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(final String sibsTransactionId) {
        String entityReferenceCode = this.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode();
        final Map<String, String> paymentEntryPropertiesMap = new HashMap<>();

        paymentEntryPropertiesMap.put("ReferenceCode", getReferenceCode());
        paymentEntryPropertiesMap.put("EntityReferenceCode", entityReferenceCode);

        if (!Strings.isNullOrEmpty(sibsTransactionId)) {
            paymentEntryPropertiesMap.put("SibsTransactionId", sibsTransactionId);
        }

        return paymentEntryPropertiesMap;
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<SibsPaymentRequest> findAll() {
        return PaymentRequest.findAll().filter(p -> p instanceof SibsPaymentRequest).map(SibsPaymentRequest.class::cast);
    }

    public static Stream<SibsPaymentRequest> find(String entityReferenceCode, String referenceCode) {
        return findAll()
                .filter(p -> entityReferenceCode
                        .equals(p.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode()))
                .filter(p -> referenceCode.equals(p.getReferenceCode()));
    }

    public static Stream<SibsPaymentRequest> findBySibsGatewayMerchantTransactionId(String sibsGatewayMerchantTransactionId) {
        return PaymentRequest.findBySibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId)
                .filter(p -> p instanceof SibsPaymentRequest).map(SibsPaymentRequest.class::cast);
    }

    public static Stream<SibsPaymentRequest> findBySibsGatewayTransactionId(String sibsGatewayTransactionId) {
        return PaymentRequest.findBySibsGatewayTransactionId(sibsGatewayTransactionId)
                .filter(p -> p instanceof SibsPaymentRequest).map(SibsPaymentRequest.class::cast);
    }

    public static Optional<SibsPaymentRequest> findUniqueBySibsGatewayTransactionId(String sibsGatewayTransactionId) {
        return findBySibsGatewayTransactionId(sibsGatewayTransactionId).findAny();
    }

    public static Stream<SibsPaymentRequest> find(final DebitEntry debitEntry) {
        return debitEntry.getPaymentRequestsSet().stream().filter(r -> r instanceof SibsPaymentRequest)
                .map(SibsPaymentRequest.class::cast);
    }
    
    public static Stream<SibsPaymentRequest> find(Installment installment) {
        return installment.getPaymentRequestsSet().stream().filter(r -> r instanceof SibsPaymentRequest)
                .map(SibsPaymentRequest.class::cast);
    }

    public static Stream<SibsPaymentRequest> findWithDebitEntries(final Set<DebitEntry> debitEntries) {
        final Set<SibsPaymentRequest> paymentCodes = debitEntries.stream().flatMap(d -> find(d)).collect(Collectors.toSet());

        final Set<SibsPaymentRequest> result = Sets.newHashSet();
        for (SibsPaymentRequest code : paymentCodes) {
            if (!Sets.symmetricDifference(code.getDebitEntriesSet(), debitEntries).isEmpty()) {
                continue;
            }

            result.add(code);
        }

        return result.stream();
    }

    public static Stream<SibsPaymentRequest> findCreatedByDebitEntry(final DebitEntry debitEntry) {
        return find(debitEntry).filter(p -> p.isInCreatedState());
    }

    public static Stream<SibsPaymentRequest> findRequestedByDebitEntry(final DebitEntry debitEntry) {
        return find(debitEntry).filter(p -> p.isInRequestedState());
    }

    public static Stream<SibsPaymentRequest> findCreatedByInstallment(Installment installment) {
        return find(installment).filter(p -> p.isInCreatedState());
    }

    public static Stream<SibsPaymentRequest> findRequestedByInstallment(Installment installment) {
        return find(installment).filter(p -> p.isInRequestedState());
    }

    public static Stream<SibsPaymentRequest> findCreatedByDebitEntriesSet(final Set<DebitEntry> debitEntries) {
        return findWithDebitEntries(debitEntries).filter(p -> p.isInCreatedState());
    }

    public static Stream<SibsPaymentRequest> findRequestedByDebitEntriesSet(final Set<DebitEntry> debitEntries) {
        return findWithDebitEntries(debitEntries).filter(p -> p.isInRequestedState());
    }

    public static SibsPaymentRequest create(SibsReferenceCode sibsReferenceCode, DebtAccount debtAccount,
            Set<DebitEntry> debitEntries, Set<Installment> installments, BigDecimal payableAmount) {
        return new SibsPaymentRequest(sibsReferenceCode, debtAccount, debitEntries, installments, payableAmount);
    }

    public static SibsPaymentRequest create(DigitalPaymentPlatform platform, DebtAccount debtAccount,
            Set<DebitEntry> debitEntries, Set<Installment> installments, BigDecimal payableAmount, String referenceCode,
            String sibsGatewayMerchantTransactionId, String sibsGatewayTransactionId) {
        return new SibsPaymentRequest(platform, debtAccount, debitEntries, installments, payableAmount, referenceCode,
                sibsGatewayMerchantTransactionId, sibsGatewayTransactionId);
    }

}
