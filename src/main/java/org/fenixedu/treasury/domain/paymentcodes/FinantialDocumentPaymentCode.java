/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
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
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class FinantialDocumentPaymentCode extends FinantialDocumentPaymentCode_Base {

    @Override
    // Check: The only invocation is PaymentReferenceCode::processPayment which is
    // already Atomic
    // @Atomic
    public Set<SettlementNote> processPayment(final String username, final BigDecimal amountToPay, final DateTime whenRegistered,
            final String sibsTransactionId, final String comments) {

        final Set<InvoiceEntry> invoiceEntriesToPay = getInvoiceEntriesSet().stream()
                .sorted((x, y) -> y.getOpenAmount().compareTo(x.getOpenAmount())).collect(Collectors.toSet());

        return internalProcessPayment(username, amountToPay, whenRegistered, sibsTransactionId, comments, invoiceEntriesToPay,
                Collections.emptySet());
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
        // CHANGE_ME add more busines validations
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

        // CHANGE_ME In order to validate UNIQUE restrictions
        // if (findByFinantialDocument(getFinantialDocument().count()>1)
        // {
        // throw new
        // TreasuryDomainException("error.FinantialDocumentPaymentCode.finantialDocument.duplicated");
        // }
        // if (findByPaymentReferenceCode(getPaymentReferenceCode().count()>1)
        // {
        // throw new
        // TreasuryDomainException("error.FinantialDocumentPaymentCode.paymentReferenceCode.duplicated");
        // }
        // if (findByValid(getValid().count()>1)
        // {
        // throw new
        // TreasuryDomainException("error.FinantialDocumentPaymentCode.valid.duplicated");
        // }
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

        // add more logical tests for checking deletion rules
        // if (getXPTORelation() != null)
        // {
        // blockers.add(BundleUtil.getString(Bundle.APPLICATION,
        // "error.FinantialDocumentPaymentCode.cannot.be.deleted"));
        // }
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
        for (FinantialInstitution finantialInstitution : FinantialInstitution.findAll().collect(Collectors.toSet())) {
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

    @Override
    public Set<Installment> getInstallmentsSet() {
        return Collections.emptySet();
    }

}
