package org.fenixedu.treasury.domain.paymentcodes;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.IPaymentProcessorForInvoiceEntries;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGateway;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import pt.ist.fenixframework.Atomic;

public abstract class PaymentCodeTarget extends PaymentCodeTarget_Base implements IPaymentProcessorForInvoiceEntries {

    public PaymentCodeTarget() {
        super();
    }

    public abstract Set<SettlementNote> processPayment(final String username, final BigDecimal amountToPay,
            DateTime whenRegistered, String sibsTransactionId, String comments);

    public abstract String getDescription();

    public String getTargetPayorDescription() {
        if (getDebtAccount() != null) {
            return getDebtAccount().getCustomer().getBusinessIdentification() + "-" + getDebtAccount().getCustomer().getName();
        }
        return "----";
    }

    public abstract boolean isPaymentCodeFor(final TreasuryEvent event);

    @Override
    public BigDecimal getPayableAmount() {
        return getPaymentReferenceCode().getPayableAmount();
    }

    @Override
    public boolean isPaymentCodeTarget() {
        return true;
    }

    public boolean isMultipleEntriesPaymentCode() {
        return false;
    }

    public boolean isFinantialDocumentPaymentCode() {
        return false;
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

        // ######################################
        // 6. Create a SibsTransactionDetail
        // ######################################
        this.getPaymentReferenceCode().setState(PaymentReferenceCodeStateType.PROCESSED);

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

        // ######################################
        // 5. Create a SibsTransactionDetail
        // ######################################
        this.getPaymentReferenceCode().setState(PaymentReferenceCodeStateType.PROCESSED);

        return result;

    }

    @Atomic
    protected Set<SettlementNote> internalProcessPayment(final String username, final BigDecimal amount,
            final DateTime whenRegistered, final String sibsTransactionId, final String comments,
            Set<InvoiceEntry> invoiceEntriesToPay, Set<Installment> installmentsToPay) {

        Function<IPaymentProcessorForInvoiceEntries, Map<String, String>> additionalPropertiesMapFunction =
                (o) -> fillPaymentEntryPropertiesMap(sibsTransactionId);

        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            return internalProcessPaymentInNormalPaymentMixingLegacyInvoices(username, amount, whenRegistered, sibsTransactionId,
                    comments, invoiceEntriesToPay, installmentsToPay, additionalPropertiesMapFunction);
        } else {
            return internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(username, amount, whenRegistered,
                    sibsTransactionId, comments, invoiceEntriesToPay, installmentsToPay, additionalPropertiesMapFunction);
        }
    }

    @Override
    public String fillPaymentEntryMethodId() {
        // ANIL (2017-09-13) Required by used ERP at this date
        return String.format("COB PAG SERV %s", getPaymentReferenceCode().getPaymentCodePool().getEntityReferenceCode());
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(final String sibsTransactionId) {
        final Map<String, String> paymentEntryPropertiesMap = Maps.newHashMap();
        paymentEntryPropertiesMap.put("ReferenceCode", getPaymentReferenceCode().getReferenceCode());
        paymentEntryPropertiesMap.put("EntityReferenceCode",
                getPaymentReferenceCode().getPaymentCodePool().getEntityReferenceCode());

        if (!Strings.isNullOrEmpty(sibsTransactionId)) {
            paymentEntryPropertiesMap.put("SibsTransactionId", sibsTransactionId);
        }

        return paymentEntryPropertiesMap;
    }

    public static String PAYMENT_TYPE_DESCRIPTION() {
        return treasuryBundle("label.IPaymentProcessorForInvoiceEntries.paymentProcessorDescription.paymentReferenceCode");
    }

    @Override
    public abstract DocumentNumberSeries getDocumentSeriesInterestDebits();

    @Override
    public abstract DocumentNumberSeries getDocumentSeriesForPayments();

    public abstract LocalDate getDueDate();

    public abstract Set<Product> getReferencedProducts();

    @Override
    public Set<Customer> getReferencedCustomers() {
        return IPaymentProcessorForInvoiceEntries.getReferencedCustomers(getInvoiceEntriesSet(), Collections.emptySet());
    }

    @Override
    public PaymentMethod getPaymentMethod() {
        return getPaymentReferenceCode().getPaymentCodePool().getPaymentMethod();
    }

    @Override
    public DateTime getPaymentRequestDate() {
        return TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(this);
    }

    @Override
    public String getPaymentRequestStateDescription() {
        return getPaymentReferenceCode().getState().getDescriptionI18N().getContent();
    }

    @Override
    public String getPaymentTypeDescription() {
        return PAYMENT_TYPE_DESCRIPTION();
    }

    @Override
    public SibsOnlinePaymentsGateway getSibsOnlinePaymentsGateway() {
        return getPaymentReferenceCode().getPaymentCodePool().getSibsOnlinePaymentsGateway();
    }

    @Override
    public String getSibsOppwaMerchantTransactionId() {
        return getPaymentReferenceCode().getSibsMerchantTransactionId();
    }

    @Override
    public String getSibsOppwaTransactionId() {
        return getPaymentReferenceCode().getSibsReferenceId();
    }
}
