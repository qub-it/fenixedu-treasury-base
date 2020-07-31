package org.fenixedu.treasury.domain.sibsonlinepaymentsgateway;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.Collections;
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
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.IPaymentProcessorForInvoiceEntries;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class MbwayPaymentRequest extends MbwayPaymentRequest_Base implements IPaymentProcessorForInvoiceEntries {

    public MbwayPaymentRequest() {
        super();

        setCreationDate(new DateTime());
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected MbwayPaymentRequest(SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway, DebtAccount debtAccount,
            Set<InvoiceEntry> invoiceEntries, Set<Installment> installments, BigDecimal payableAmount, String phoneNumber,
            String sibsMerchantTransactionId, String sibsReferenceId) {
        this();

        setSibsOnlinePaymentsGateway(sibsOnlinePaymentsGateway);
        setDebtAccount(debtAccount);
        getInvoiceEntriesSet().addAll(invoiceEntries);
        getInstallmentsSet().addAll(installments);
        setPayableAmount(payableAmount);
        setPhoneNumber(phoneNumber);
        setSibsMerchantTransactionId(sibsMerchantTransactionId);
        setSibsReferenceId(sibsReferenceId);
        setState(PaymentReferenceCodeStateType.USED);

        checkRules();
    }

    private void checkRules() {

        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.domainRoot.required");
        }

        if (getCreationDate() == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.creationDate.required");
        }

        if (getSibsOnlinePaymentsGateway() == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsOnlinePaymentsGateway.required");
        }

        if (getDebtAccount() == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.debtAccount.required");
        }

        if (getInvoiceEntriesSet().isEmpty() && getInstallmentsSet().isEmpty()) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.invoiceEntriesSet.required");
        }

        if (getPayableAmount() == null || !TreasuryConstants.isPositive(getPayableAmount())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.payableAmount.required");
        }

        if (StringUtils.isEmpty(getPhoneNumber())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phoneNumber.required");
        }

        if (StringUtils.isEmpty(getSibsMerchantTransactionId())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsMerchantTransaction.required");
        }

        if (findBySibsMerchantTransactionId(getSibsMerchantTransactionId()).count() > 1) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsMerchantTransactionId.not.unique");
        }

        checkParametersAreValid(getDebtAccount(), getInvoiceEntriesSet());
    }

    @Override
    public Set<SettlementNote> internalProcessPaymentInNormalPaymentMixingLegacyInvoices(final String username,
            final BigDecimal amount, final DateTime paymentDate, final String sibsTransactionId, final String comments,
            Set<InvoiceEntry> invoiceEntriesToPay, Set<Installment> installmentsToPay,
            Function<IPaymentProcessorForInvoiceEntries, Map<String, String>> fillPaymentEntryPropertiesMapFunction) {

        Set<SettlementNote> result =
                IPaymentProcessorForInvoiceEntries.super.internalProcessPaymentInNormalPaymentMixingLegacyInvoices(username,
                        amount, paymentDate, sibsTransactionId, comments, invoiceEntriesToPay, installmentsToPay,
                        fillPaymentEntryPropertiesMapFunction);

        this.setState(PaymentReferenceCodeStateType.PROCESSED);

        return result;
    }

    @Override
    public Set<SettlementNote> internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(final String username,
            final BigDecimal amount, final DateTime paymentDate, final String sibsTransactionId, final String comments,
            final Set<InvoiceEntry> invoiceEntriesToPay, Set<Installment> installmentsToPay,
            Function<IPaymentProcessorForInvoiceEntries, Map<String, String>> fillPaymentEntryPropertiesMapFunction) {

        Set<SettlementNote> result =
                IPaymentProcessorForInvoiceEntries.super.internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(username,
                        amount, paymentDate, sibsTransactionId, comments, invoiceEntriesToPay, installmentsToPay,
                        fillPaymentEntryPropertiesMapFunction);

        this.setState(PaymentReferenceCodeStateType.PROCESSED);

        return result;

    }

    @Atomic
    private Set<SettlementNote> processPayment(final String username, final BigDecimal amount, final DateTime paymentDate,
            final String sibsTransactionId, final String comments) {
        Function<IPaymentProcessorForInvoiceEntries, Map<String, String>> additionalPropertiesMapFunction =
                (o) -> fillPaymentEntryPropertiesMap(sibsTransactionId);

        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            return internalProcessPaymentInNormalPaymentMixingLegacyInvoices(username, amount, paymentDate, sibsTransactionId,
                    comments, getInvoiceEntriesSet(), getInstallmentsSet(), additionalPropertiesMapFunction);
        } else {
            return internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(username, amount, paymentDate, sibsTransactionId,
                    comments, getInvoiceEntriesSet(), getInstallmentsSet(), additionalPropertiesMapFunction);
        }
    }

    @Atomic(mode = TxMode.READ)
    public void processMbwayTransaction(final SibsOnlinePaymentsGatewayLog log, PaymentStateBean bean) {
        if (!bean.getMerchantTransactionId().equals(getSibsMerchantTransactionId())) {
            throw new TreasuryDomainException(
                    "error.MbwayPaymentRequest.processMbwayTransaction.merchantTransactionId.not.equal");
        }

        FenixFramework.atomic(() -> {
            final SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway = getSibsOnlinePaymentsGateway();
            final DebtAccount debtAccount = getDebtAccount();

            log.associateSibsOnlinePaymentGatewayAndDebtAccount(sibsOnlinePaymentsGateway, debtAccount);
        });

        final BigDecimal amount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate();

        FenixFramework.atomic(() -> {
            log.savePaymentInfo(amount, paymentDate);
        });

        if (amount == null || !TreasuryConstants.isPositive(amount)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.payment.date");
        }

        if (MbwayTransaction.isTransactionProcessingDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> {
                log.markAsDuplicatedTransaction();
            });
        } else {

            FenixFramework.atomic(() -> {
                final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

                final Set<SettlementNote> settlementNotes =
                        processPayment(StringUtils.isNotEmpty(loggedUsername) ? loggedUsername : "unknown", amount, paymentDate,
                                bean.getTransactionId(), bean.getMerchantTransactionId());
                MbwayTransaction.create(this, bean.getTransactionId(), amount, paymentDate, settlementNotes);

                log.markSettlementNotesCreated(settlementNotes);
            });
        }
    }

    @Override
    public DocumentNumberSeries getDocumentSeriesForPayments() {
        return getSibsOnlinePaymentsGateway().getMbwayDocumentSeries();
    }

    @Override
    public DocumentNumberSeries getDocumentSeriesInterestDebits() {
        return DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), getDocumentSeriesForPayments().getSeries());
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(String sibsTransactionId) {
        final Map<String, String> result = new HashMap<>();

        result.put("SibsTransactionId", sibsTransactionId);

        return result;
    }

    @Override
    public Set<Customer> getReferencedCustomers() {
        return IPaymentProcessorForInvoiceEntries.getReferencedCustomers(getInvoiceEntriesSet(), getInstallmentsSet());
    }

    @Override
    public DateTime getPaymentRequestDate() {
        return getCreationDate();
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return getSibsOnlinePaymentsGateway().getMbwayPaymentMethod();
    }

    @Override
    public String getPaymentRequestStateDescription() {
        return getState().getDescriptionI18N().getContent();
    }

    @Override
    public String getPaymentTypeDescription() {
        return PAYMENT_TYPE_DESCRIPTION();
    }

    @Override
    public String fillPaymentEntryMethodId() {
        return "";
    }

    @Override
    public boolean isMbwayRequest() {
        return true;
    }

    public static String PAYMENT_TYPE_DESCRIPTION() {
        return treasuryBundle("label.IPaymentProcessorForInvoiceEntries.paymentProcessorDescription.mbwayPaymentRequest");
    }

    /* ************ */
    /* * SERVICES * */
    /* ************ */

    @Atomic(mode = TxMode.READ)
    public static MbwayPaymentRequest create(SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway, DebtAccount debtAccount,
            Set<InvoiceEntry> invoiceEntries, Set<Installment> installments, String countryPrefix, String localPhoneNumber) {
        throw new RuntimeException("not supported");
    }

    private static void checkParametersAreValid(final DebtAccount debtAccount, final Set<InvoiceEntry> invoiceEntries) {
        for (final InvoiceEntry invoiceEntry : invoiceEntries) {
            final DebitEntry debitEntry = (DebitEntry) invoiceEntry;

            // Ensure all debit entries are the same debt account
            if (debitEntry.getDebtAccount() != debtAccount) {
                throw new TreasuryDomainException("error.MbwayPaymentRequest.debit.entry.not.same.debt.account");
            }

            // Ensure debit entries have payable amount
            if (!TreasuryConstants.isGreaterThan(debitEntry.getOpenAmount(), BigDecimal.ZERO)) {
                throw new TreasuryDomainException("error.MbwayPaymentRequest.debit.entry.open.amount.must.be.greater.than.zero");
            }
        }

        if (IPaymentProcessorForInvoiceEntries.getReferencedCustomers(invoiceEntries, Collections.emptySet()).size() > 1) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.referencedCustomers.only.one.allowed");
        }

        SettlementNote.checkMixingOfInvoiceEntriesExportedInLegacyERP(invoiceEntries);
    }

    @Atomic(mode = TxMode.WRITE)
    private static MbwayPaymentRequest createMbwayPaymentRequest(SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway,
            DebtAccount debtAccount, Set<InvoiceEntry> invoiceEntries, Set<Installment> installments, String phoneNumber,
            BigDecimal payableAmount, String merchantTransactionId, String sibsReferenceId) {
        return new MbwayPaymentRequest(sibsOnlinePaymentsGateway, debtAccount, invoiceEntries, installments, payableAmount,
                phoneNumber, merchantTransactionId, sibsReferenceId);
    }

    @Atomic(mode = TxMode.WRITE)
    private static SibsOnlinePaymentsGatewayLog createLog(final SibsOnlinePaymentsGateway sibsGateway,
            final DebtAccount debtAccount) {
        return SibsOnlinePaymentsGatewayLog.createLogForRequestPaymentCode(sibsGateway, debtAccount);
    }

    // ############
    // # SERVICES #
    // ############

    public static Stream<MbwayPaymentRequest> findAll() {
        return FenixFramework.getDomainRoot().getMbwayPaymentRequestsSet().stream();
    }

    public static Stream<MbwayPaymentRequest> findBySibsMerchantTransactionId(final String sibsMerchantTransactionId) {
        return findAll().filter(r -> r.getSibsMerchantTransactionId().equals(sibsMerchantTransactionId));
    }

    public static Stream<MbwayPaymentRequest> findBySibsReferenceId(final String sibsReferenceId) {
        return findAll().filter(r -> r.getSibsReferenceId().equals(sibsReferenceId));
    }

    public static Optional<MbwayPaymentRequest> findUniqueBySibsReferenceId(final String sibsReferenceId) {
        return findBySibsReferenceId(sibsReferenceId).findFirst();
    }

    public static Optional<MbwayPaymentRequest> findUniqueBySibsMerchantTransactionId(final String sibsMerchantTransactionId) {
        return findBySibsMerchantTransactionId(sibsMerchantTransactionId).findFirst();
    }

    @Override
    public String getSibsOppwaMerchantTransactionId() {
        return getSibsMerchantTransactionId();
    }

    @Override
    public String getSibsOppwaTransactionId() {
        return getSibsReferenceId();
    }

}
