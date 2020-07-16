package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.FinantialDocumentEntry;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class FinantialDocumentPaymentCode extends FinantialDocumentPaymentCode_Base {

    @Override
    // Check: The only invocation is PaymentReferenceCode::processPayment which is already Atomic
    // @Atomic
    public Set<SettlementNote> processPayment(final String username, final BigDecimal amountToPay, final DateTime whenRegistered,
            final String sibsTransactionId, final String comments) {

        final Set<InvoiceEntry> invoiceEntriesToPay = getInvoiceEntriesSet().stream()
                .sorted((x, y) -> y.getOpenAmount().compareTo(x.getOpenAmount())).collect(Collectors.toSet());

        return internalProcessPayment(username, amountToPay, whenRegistered, sibsTransactionId, comments, invoiceEntriesToPay);
    }

    @Override
    public boolean isFinantialDocumentPaymentCode() {
        return true;
    }

    @Override
    public String getDescription() {
        final StringBuilder builder = new StringBuilder();
        for (FinantialDocumentEntry entry : this.getFinantialDocument().getFinantialDocumentEntriesSet()) {
            builder.append(entry.getDescription()).append("\n");
        }
        return builder.toString();
    }

    @Override
    public boolean isPaymentCodeFor(final TreasuryEvent event) {
        if (this.getFinantialDocument().isDebitNote()) {
            DebitNote debitNote = (DebitNote) this.getFinantialDocument();
            return debitNote.getDebitEntries().anyMatch(x -> x.getTreasuryEvent() != null && x.getTreasuryEvent().equals(event));
        }
        return false;

    }

    protected FinantialDocumentPaymentCode() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(final FinantialDocument finantialDocument, final PaymentReferenceCode paymentReferenceCode,
            final java.lang.Boolean valid) {
        setFinantialDocument(finantialDocument);
        setDebtAccount(finantialDocument.getDebtAccount());
        setPaymentReferenceCode(paymentReferenceCode);
        setValid(valid);
        checkRules();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getFinantialDocument() == null) {
            throw new TreasuryDomainException("error.FinantialDocumentPaymentCode.finantialDocument.required");
        }

        if (getPaymentReferenceCode() == null) {
            throw new TreasuryDomainException("error.FinantialDocumentPaymentCode.paymentReferenceCode.required");
        }

        if (getDebtAccount() == null) {
            throw new TreasuryDomainException("error.FinantialDocumentPaymentCode.debtAccount.required");
        }

        // Ensure that there is only one active reference code
        final long activePaymentCodesOnFinantialDocumentCount =
                FinantialDocumentPaymentCode.findNewByFinantialDocument(this.getFinantialDocument()).count()
                        + FinantialDocumentPaymentCode.findUsedByFinantialDocument(this.getFinantialDocument()).count();

        if (activePaymentCodesOnFinantialDocumentCount > 1) {
            throw new TreasuryDomainException("error.FinantialDocumentPaymentCode.finantial.with.active.payment.code");
        }

        for (final FinantialDocumentEntry finantialDocumentEntry : getFinantialDocument().getFinantialDocumentEntriesSet()) {
            if (!(finantialDocumentEntry instanceof DebitEntry)) {
                continue;
            }

            final DebitEntry debitEntry = (DebitEntry) finantialDocumentEntry;

            if (MultipleEntriesPaymentCode.findNewByDebitEntry(debitEntry).count() > 0
                    || MultipleEntriesPaymentCode.findUsedByDebitEntry(debitEntry).count() > 0) {
                throw new TreasuryDomainException("error.FinantialDocumentPaymentCode.debit.entry.with.active.payment.code",
                        debitEntry.getDescription());
            }
        }

        //CHANGE_ME In order to validate UNIQUE restrictions
        //if (findByFinantialDocument(getFinantialDocument().count()>1)
        //{
        //  throw new TreasuryDomainException("error.FinantialDocumentPaymentCode.finantialDocument.duplicated");
        //} 
        //if (findByPaymentReferenceCode(getPaymentReferenceCode().count()>1)
        //{
        //  throw new TreasuryDomainException("error.FinantialDocumentPaymentCode.paymentReferenceCode.duplicated");
        //} 
        //if (findByValid(getValid().count()>1)
        //{
        //  throw new TreasuryDomainException("error.FinantialDocumentPaymentCode.valid.duplicated");
        //} 
    }

    @Atomic
    public void edit(final FinantialDocument finantialDocument, final PaymentReferenceCode paymentReferenceCode,
            final java.lang.Boolean valid) {
        setFinantialDocument(finantialDocument);
        setPaymentReferenceCode(paymentReferenceCode);
        setValid(valid);
        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        //add more logical tests for checking deletion rules
        //if (getXPTORelation() != null)
        //{
        //    blockers.add(BundleUtil.getString(Bundle.APPLICATION, "error.FinantialDocumentPaymentCode.cannot.be.deleted"));
        //}
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.FinantialDocumentPaymentCode.cannot.delete");
        }
        deleteDomainObject();
    }

    private boolean isDeletable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Atomic
    public static FinantialDocumentPaymentCode create(final FinantialDocument finantialDocument,
            final PaymentReferenceCode paymentReferenceCode, final java.lang.Boolean valid) {
        FinantialDocumentPaymentCode finantialDocumentPaymentCode = new FinantialDocumentPaymentCode();
        finantialDocumentPaymentCode.init(finantialDocument, paymentReferenceCode, valid);
        paymentReferenceCode.setState(PaymentReferenceCodeStateType.USED);
        return finantialDocumentPaymentCode;
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<FinantialDocumentPaymentCode> findAll() {
        Set<FinantialDocumentPaymentCode> entries = new HashSet<FinantialDocumentPaymentCode>();
        for(FinantialInstitution finantialInstitution : FinantialInstitution.findAll().collect(Collectors.toSet())) {
            findAll(finantialInstitution).collect(Collectors.toCollection(() -> entries));
        }
        
        return entries.stream();
    }
    
    public static Stream<FinantialDocumentPaymentCode> findAll(final FinantialInstitution finantialInstitution) {
        Set<FinantialDocumentPaymentCode> entries = new HashSet<FinantialDocumentPaymentCode>();
        for (PaymentCodePool pool : finantialInstitution.getPaymentCodePoolsSet()) {
            for (PaymentReferenceCode code : pool.getPaymentReferenceCodesSet()) {
                if (code.getTargetPayment() != null && code.getTargetPayment() instanceof FinantialDocumentPaymentCode) {
                    entries.add((FinantialDocumentPaymentCode) code.getTargetPayment());
                }
            }
        }
        return entries.stream();
    }

    public static Stream<FinantialDocumentPaymentCode> findByFinantialDocument(final FinantialInstitution finantialInstitution,
            final FinantialDocument finantialDocument) {
        return finantialDocument.getPaymentCodesSet().stream().filter(i -> finantialDocument.equals(i.getFinantialDocument()));
//        return findAll(finantialInstitution).filter(i -> finantialDocument.equals(i.getFinantialDocument()));
    }

    public static Stream<FinantialDocumentPaymentCode> findNewByFinantialDocument(final FinantialDocument finantialDocument) {
        return findByFinantialDocument(finantialDocument.getDebtAccount().getFinantialInstitution(), finantialDocument)
                .filter(p -> p.getPaymentReferenceCode().isNew());
    }

    public static Stream<FinantialDocumentPaymentCode> findUsedByFinantialDocument(final FinantialDocument finantialDocument) {
        return findByFinantialDocument(finantialDocument.getDebtAccount().getFinantialInstitution(), finantialDocument)
                .filter(p -> p.getPaymentReferenceCode().isUsed());
    }

    public static Stream<FinantialDocumentPaymentCode> findByValid(final FinantialInstitution finantialInstitution,
            final java.lang.Boolean valid) {
        return findAll(finantialInstitution).filter(i -> valid.equals(i.getValid()));
    }

    @Override
    public DocumentNumberSeries getDocumentSeriesForPayments() {
        return this.getPaymentReferenceCode().getPaymentCodePool().getDocumentSeriesForPayments();
    }

    @Override
    public DocumentNumberSeries getDocumentSeriesInterestDebits() {
        return DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(),
                this.getPaymentReferenceCode().getPaymentCodePool().getDocumentSeriesForPayments().getSeries());
    }

    @Override
    public LocalDate getDueDate() {
        return getFinantialDocument().getDocumentDueDate();
    }

    @Override
    public Set<InvoiceEntry> getInvoiceEntriesSet() {
        final Set<InvoiceEntry> result = Sets.newHashSet();

        for (final FinantialDocumentEntry entry : getFinantialDocument().getFinantialDocumentEntriesSet()) {
            if (!(entry instanceof InvoiceEntry)) {
                continue;
            }

            result.add((InvoiceEntry) entry);
        }

        return result;
    }

    @Override
    public Set<Product> getReferencedProducts() {
        return getFinantialDocument().getFinantialDocumentEntriesSet().stream().map(DebitEntry.class::cast)
                .map(d -> d.getProduct()).collect(Collectors.toSet());
    }

}
