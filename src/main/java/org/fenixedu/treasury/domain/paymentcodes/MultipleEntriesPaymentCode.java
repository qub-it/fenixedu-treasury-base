package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.IPaymentProcessorForInvoiceEntries;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentEntry;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class MultipleEntriesPaymentCode extends MultipleEntriesPaymentCode_Base {

    public static final int MAX_PAYMENT_CODES_FOR_DEBIT_ENTRY = 2;

    public MultipleEntriesPaymentCode() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }
    
    protected MultipleEntriesPaymentCode(final Set<DebitEntry> debitNoteEntries, Set<Installment> installments,
            final PaymentReferenceCode paymentReferenceCode, final boolean valid) {
        this();
        init(debitNoteEntries, installments, paymentReferenceCode, valid);
    }

    protected void init(final Set<DebitEntry> debitNoteEntries, Set<Installment> installments,
            final PaymentReferenceCode paymentReferenceCode, final boolean valid) {
        getInvoiceEntriesSet().addAll(debitNoteEntries);
        getInstallmentsSet().addAll(installments);
        DebtAccount debtAccount = null;
        if (!debitNoteEntries.isEmpty()) {
            debtAccount = debitNoteEntries.iterator().next().getDebtAccount();
        }
        if (!installments.isEmpty() && debtAccount == null) {
            debtAccount = installments.iterator().next().getPaymentPlan().getDebtAccount();
        }
        setDebtAccount(debtAccount);

        setPaymentReferenceCode(paymentReferenceCode);
        setValid(valid);
        checkRules();
    }

    private void checkRules() {
        if (getPaymentReferenceCode() == null) {
            throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.paymentReferenceCode.required");
        }

        // Is associated with one debit entry
        if (getInvoiceEntriesSet().isEmpty() && getInstallmentsSet().isEmpty()) {
            throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debitEntries.and.installments.empty");
        }

        final DebtAccount debtAccount = getDebtAccount();
        for (final InvoiceEntry invoiceEntry : getInvoiceEntriesSet()) {
            final DebitEntry debitEntry = (DebitEntry) invoiceEntry;

            // Ensure all debit entries are the same debt account
            if (debitEntry.getDebtAccount() != debtAccount) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.not.same.debt.account");
            }

            // Ensure debit entries have payable amount
            if (!TreasuryConstants.isGreaterThan(debitEntry.getOpenAmount(), BigDecimal.ZERO)) {
                throw new TreasuryDomainException(
                        "error.MultipleEntriesPaymentCode.debit.entry.open.amount.must.be.greater.than.zero");
            }

            // Ensure that there is only one payment code active reference for debit entry
            boolean hasFinantialDocumentPaymentCode = debitEntry.getFinantialDocument() != null
                    && FinantialDocumentPaymentCode.findNewByFinantialDocument(debitEntry.getFinantialDocument()).count() > 0;

            hasFinantialDocumentPaymentCode |= debitEntry.getFinantialDocument() != null
                    && FinantialDocumentPaymentCode.findUsedByFinantialDocument(debitEntry.getFinantialDocument()).count() > 0;

            if (hasFinantialDocumentPaymentCode) {
                throw new TreasuryDomainException(
                        "error.MultipleEntriesPaymentCode.debit.entry.finantial.with.active.payment.code",
                        debitEntry.getFinantialDocument().getUiDocumentNumber());
            }

            final long activePaymentCodesOnDebitEntryCount = MultipleEntriesPaymentCode.findNewByDebitEntry(debitEntry).count()
                    + MultipleEntriesPaymentCode.findUsedByDebitEntry(debitEntry).count();

            if (activePaymentCodesOnDebitEntryCount > MAX_PAYMENT_CODES_FOR_DEBIT_ENTRY) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.with.active.payment.code",
                        debitEntry.getDescription());
            }
        }

        if (getReferencedCustomers().size() > 1) {
            throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.referencedCustomers.only.one.allowed");
        }

        SettlementNote.checkMixingOfInvoiceEntriesExportedInLegacyERP(getInvoiceEntriesSet());
    }

    @Override
    public boolean isMultipleEntriesPaymentCode() {
        return true;
    }

    @Override
    public DocumentNumberSeries getDocumentSeriesForPayments() {
        return this.getPaymentReferenceCode().getPaymentCodePool().getDocumentSeriesForPayments();
    }

    @Override
    public String getDescription() {
        final StringBuilder builder = new StringBuilder();
        for (FinantialDocumentEntry entry : getOrderedInvoiceEntries()) {
            builder.append(entry.getDescription()).append(" <br>");
        }
        for (Installment entry : getInstallmentsSet()) {
            builder.append(entry.getDescription()).append(" <br>");
        }
        return builder.toString();
    }

    @Override
    public Set<SettlementNote> processPayment(final String username, final BigDecimal amountToPay, final DateTime whenRegistered,
            final String sibsTransactionId, final String comments) {
        return internalProcessPayment(username, amountToPay, whenRegistered, sibsTransactionId, comments, getInvoiceEntriesSet(),
                getInstallmentsSet());
    }

    @Override
    public Set<Customer> getReferencedCustomers() {
        return IPaymentProcessorForInvoiceEntries.getReferencedCustomers(getInvoiceEntriesSet(), getInstallmentsSet());
    }

    public TreeSet<DebitEntry> getOrderedInvoiceEntries() {
        final TreeSet<DebitEntry> result = new TreeSet<DebitEntry>(DebitEntry.COMPARE_BY_EXTERNAL_ID);
        result.addAll(getInvoiceEntriesSet().stream().map(DebitEntry.class::cast).collect(Collectors.<DebitEntry> toSet()));

        return result;
    }

    @Override
    public DocumentNumberSeries getDocumentSeriesInterestDebits() {
        return DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(),
                this.getPaymentReferenceCode().getPaymentCodePool().getDocumentSeriesForPayments().getSeries());
    }

    @Override
    public boolean isPaymentCodeFor(final TreasuryEvent event) {
        Set<InvoiceEntry> invoiceEntriesSet = getInvoiceEntriesSet();
        invoiceEntriesSet.addAll(getInstallmentsSet().stream()
                .flatMap(i -> i.getInstallmentEntriesSet().stream().map(e -> e.getDebitEntry())).collect(Collectors.toSet()));
        return invoiceEntriesSet.stream().map(DebitEntry.class::cast)
                .anyMatch(x -> x.getTreasuryEvent() != null && x.getTreasuryEvent().equals(event));
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        // add more logical tests for checking deletion rules
        // if (getXPTORelation() != null)
        // {
        // blockers.add(BundleUtil.getString(Bundle.APPLICATION,
        // "error.MultipleEntriesPaymentCode.cannot.be.deleted"));
        // }
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.cannot.delete");
        }

        deleteDomainObject();
    }

    private boolean isDeletable() {
        return false;
    }

    @Override
    public LocalDate getDueDate() {
        Set<LocalDate> map = getInvoiceEntriesSet().stream().map(InvoiceEntry::getDueDate).collect(Collectors.toSet());
        map.addAll(getInstallmentsSet().stream().map(Installment::getDueDate).collect(Collectors.toSet()));
        return map.stream().sorted().findFirst().orElse(null);

    }

    @Atomic
    public static MultipleEntriesPaymentCode create(final Set<DebitEntry> debitNoteEntries, Set<Installment> installments,
            final PaymentReferenceCode paymentReferenceCode, final boolean valid) {
        return new MultipleEntriesPaymentCode(debitNoteEntries, installments, paymentReferenceCode, valid);
    }

    @Override
    public Set<Product> getReferencedProducts() {
        Set<InvoiceEntry> invoiceEntriesSet = getInvoiceEntriesSet();
        invoiceEntriesSet.addAll(getInstallmentsSet().stream()
                .flatMap(i -> i.getInstallmentEntriesSet().stream().map(e -> e.getDebitEntry())).collect(Collectors.toSet()));
        return invoiceEntriesSet.stream().map(d -> d.getProduct()).collect(Collectors.toSet());
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<MultipleEntriesPaymentCode> findAll() {
        Set<MultipleEntriesPaymentCode> entries = new HashSet<MultipleEntriesPaymentCode>();

        for (FinantialInstitution finantialInstitution : FinantialInstitution.findAll().collect(Collectors.toSet())) {
            findAll(finantialInstitution).collect(Collectors.toCollection(() -> entries));
        }

        return entries.stream();
    }

    public static Stream<MultipleEntriesPaymentCode> findAll(FinantialInstitution finantialInstitution) {
        Set<MultipleEntriesPaymentCode> entries = new HashSet<MultipleEntriesPaymentCode>();
        for (PaymentCodePool pool : finantialInstitution.getPaymentCodePoolsSet()) {
            for (PaymentReferenceCode code : pool.getPaymentReferenceCodesSet()) {
                if (code.getTargetPayment() != null && code.getTargetPayment() instanceof MultipleEntriesPaymentCode) {
                    entries.add((MultipleEntriesPaymentCode) code.getTargetPayment());
                }
            }
        }
        return entries.stream();
    }

    public static Stream<MultipleEntriesPaymentCode> find(final DebitEntry debitEntry) {
        return debitEntry.getPaymentCodesSet().stream();
    }

    public static Stream<MultipleEntriesPaymentCode> findWithDebitEntries(final Set<DebitEntry> debitEntries) {
        final Set<MultipleEntriesPaymentCode> paymentCodes =
                debitEntries.stream().map(d -> d.getPaymentCodesSet()).flatMap(p -> p.stream()).collect(Collectors.toSet());

        final Set<MultipleEntriesPaymentCode> result = Sets.newHashSet();
        for (final MultipleEntriesPaymentCode code : paymentCodes) {
            if (!Sets.symmetricDifference(code.getInvoiceEntriesSet(), debitEntries).isEmpty()) {
                continue;
            }

            result.add(code);
        }

        return result.stream();
    }

    public static Stream<MultipleEntriesPaymentCode> findByValid(FinantialInstitution finantialInstitution, final boolean valid) {
        return findAll(finantialInstitution).filter(i -> valid == i.getValid());
    }

    public static Stream<MultipleEntriesPaymentCode> findNewByDebitEntry(final DebitEntry debitEntry) {
        return find(debitEntry).filter(p -> p.getPaymentReferenceCode().isNew());
    }

    public static Stream<MultipleEntriesPaymentCode> findUsedByDebitEntry(final DebitEntry debitEntry) {
        return find(debitEntry).filter(p -> p.getPaymentReferenceCode().isUsed());
    }

    public static Stream<MultipleEntriesPaymentCode> findNewByDebitEntriesSet(final Set<DebitEntry> debitEntries) {
        return findWithDebitEntries(debitEntries).filter(p -> p.getPaymentReferenceCode().isNew());
    }

    public static Stream<MultipleEntriesPaymentCode> findUsedByDebitEntriesSet(final Set<DebitEntry> debitEntries) {
        return findWithDebitEntries(debitEntries).filter(p -> p.getPaymentReferenceCode().isUsed());
    }

}
