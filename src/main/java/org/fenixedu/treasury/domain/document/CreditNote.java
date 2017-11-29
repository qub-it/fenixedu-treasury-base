/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
 * 
 *
 * 
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.treasury.domain.document;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.Constants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;

public class CreditNote extends CreditNote_Base {

    public CreditNote() {
        super();
    }

    protected CreditNote(final DebtAccount debtAccount, final DocumentNumberSeries documentNumberSeries,
            final DateTime documentDate, DebitNote debitNote) {
        super();

        init(debtAccount, documentNumberSeries, documentDate, debitNote);
        checkRules();
    }

    protected void init(DebtAccount debtAccount, DocumentNumberSeries documentNumberSeries, DateTime documentDate,
            DebitNote debitNote) {
        super.init(debtAccount, documentNumberSeries, documentDate);

        this.setDebitNote(debitNote);

        if (debitNote != null) {
            this.setPayorDebtAccount(debitNote.getPayorDebtAccount());
        }

        if (!getCreditEntriesSet().isEmpty() && getCreditEntriesSet().size() > 1) {
            throw new TreasuryDomainException("error.CreditNote.with.unexpected.credit.entries");
        }
    }

    @Override
    protected void checkRules() {
        if (!getDocumentNumberSeries().getFinantialDocumentType().getType().equals(FinantialDocumentTypeEnum.CREDIT_NOTE)) {
            throw new TreasuryDomainException("error.CreditNote.finantialDocumentType.invalid");
        }

        if (getDebitNote() != null && !getDebitNote().getDebtAccount().equals(getDebtAccount())) {
            throw new TreasuryDomainException("error.CreditNote.invalid.debtaccount.with.debitnote");
        }

        if (getDebitNote() != null && !getDebitNote().isClosed()) {
            throw new TreasuryDomainException("error.CreditNote.debitnote.not.closed");
        }

        if (getDebitNote() != null && getPayorDebtAccount() != getDebitNote().getPayorDebtAccount()) {
            throw new TreasuryDomainException("error.CreditNote.with.payorDebtAccount.different.from.debit.note");
        }

        if (getDebitNote() != null && getDebitNote().getDocumentNumberSeries().getSeries().isRegulationSeries()) {
            throw new TreasuryDomainException("error.CreditNote.debit.note.cannot.be.from.regulation.series");
        }

        super.checkRules();
    }

    @Override
    public boolean isCreditNote() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        return true;
    }

    public boolean isAdvancePayment() {
        return false;
    }

    public boolean isRelatedToReimbursement() {
        if (!isClosed()) {
            return false;
        }

        final Set<SettlementEntry> settlementEntries = new HashSet<SettlementEntry>();
        for (FinantialDocumentEntry entry : this.getFinantialDocumentEntriesSet()) {
            for (SettlementEntry settlementEntry : ((InvoiceEntry) entry).getSettlementEntriesSet()) {
                settlementEntries.add(settlementEntry);
            }
        }

        if (settlementEntries.isEmpty()) {
            return false;
        }

        if (settlementEntries.size() != 1) {
            throw new TreasuryDomainException("error.CreditNote.isRelatedToReimbursement.settlement.entries.not.one.check");
        }

        final SettlementNote settlementNote =
                (SettlementNote) getRelatedSettlementEntries().iterator().next().getFinantialDocument();

        return settlementNote.isReimbursement();
    }

    @Override
    public void closeDocument(boolean markDocumentToExport) {
        super.closeDocument(markDocumentToExport);

        if (!getCreditEntriesSet().isEmpty() && getCreditEntriesSet().size() > 1) {
            throw new TreasuryDomainException("error.CreditNote.with.unexpected.credit.entries");
        }
    }

    @Override
    @Atomic
    public void delete(boolean deleteEntries) {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.CreditNote.cannot.delete");
        }

        setDebitNote(null);

        super.delete(deleteEntries);
    }

    public Stream<? extends CreditEntry> getCreditEntries() {
        return CreditEntry.find(this);
    }

    public Set<? extends CreditEntry> getCreditEntriesSet() {
        return this.getCreditEntries().collect(Collectors.<CreditEntry> toSet());
    }

    public BigDecimal getDebitAmount() {
        return BigDecimal.ZERO;
    }

    public BigDecimal getCreditAmount() {
        return this.getTotalAmount();
    }

    @Override
    // Ensure payor debt account of associated debit note is returned
    public DebtAccount getPayorDebtAccount() {
        if (super.getPayorDebtAccount() == null && getDebitNote() != null) {
            return getDebitNote().getPayorDebtAccount();
        }

        return super.getPayorDebtAccount();
    };

    public void editPayorDebtAccount(final DebtAccount payorDebtAccount) {
        if (!isPreparing()) {
            throw new TreasuryDomainException("error.CreditNote.edit.not.possible.on.closed.document");
        }

        setPayorDebtAccount(payorDebtAccount);

        checkRules();
    }

    /* Method is not used anywhere */
    @Atomic
    private void deprecatedEdit(final DebitNote debitNote, final DebtAccount payorDebtAccount,
            final FinantialDocumentType finantialDocumentType, final DebtAccount debtAccount,
            final DocumentNumberSeries documentNumberSeries, final Currency currency, final String documentNumber,
            final org.joda.time.DateTime documentDate, final org.joda.time.LocalDate documentDueDate,
            final String originDocumentNumber, final org.fenixedu.treasury.domain.document.FinantialDocumentStateType state) {
        if (!isPreparing()) {
            throw new TreasuryDomainException("error.CreditNote.edit.not.possible.on.closed.document");
        }

        setDebitNote(debitNote);
        setFinantialDocumentType(finantialDocumentType);
        setDebtAccount(debtAccount);
        editPayorDebtAccount(payorDebtAccount);
        setDocumentNumberSeries(documentNumberSeries);
        setCurrency(currency);
        setDocumentNumber(documentNumber);
        setDocumentDate(documentDate);
        setDocumentDueDate(documentDueDate);
        setOriginDocumentNumber(originDocumentNumber);
        setState(state);
        checkRules();
    }

    @Atomic
    public void updateCreditNote(String originDocumentNumber, String documentObservations) {
        setOriginDocumentNumber(originDocumentNumber);
        setDocumentObservations(documentObservations);

        checkRules();
    }

    @Override
    public BigDecimal getOpenAmount() {
        if (this.isAnnulled()) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = BigDecimal.ZERO;
        for (CreditEntry entry : getCreditEntriesSet()) {
            amount = amount.add(entry.getOpenAmount());
        }
        return getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(amount);
    }

    @Override
    public BigDecimal getOpenAmountWithInterests() {
        if (this.getState().isPreparing() || this.getState().isClosed()) {
            return getOpenAmount();
        } else {
            return BigDecimal.ZERO;
        }
    }

    @Override
    public Set<FinantialDocument> findRelatedDocuments(Set<FinantialDocument> documentsBaseList,
            Boolean includeAnulledDocuments) {
        documentsBaseList.add(this);

        for (CreditEntry entry : getCreditEntriesSet()) {
            if (entry.getDebitEntry() != null && entry.getDebitEntry().getFinantialDocument() != null
                    && !entry.getDebitEntry().getFinantialDocument().isPreparing()) {
                if (includeAnulledDocuments == true || this.isAnnulled() == false) {
                    if (documentsBaseList.contains(entry.getDebitEntry().getFinantialDocument()) == false) {
                        documentsBaseList.addAll(entry.getDebitEntry().getFinantialDocument()
                                .findRelatedDocuments(documentsBaseList, includeAnulledDocuments));
                    }
                }
            }
        }

        for (CreditEntry entry : getCreditEntriesSet()) {
            for (SettlementEntry settlementEntry : entry.getSettlementEntriesSet()) {
                if (settlementEntry.getFinantialDocument() != null && !settlementEntry.getFinantialDocument().isPreparing()) {
                    if (includeAnulledDocuments == true || settlementEntry.getFinantialDocument().isAnnulled() == false) {
                        if (documentsBaseList.contains(settlementEntry.getFinantialDocument()) == false) {
                            documentsBaseList.addAll(settlementEntry.getFinantialDocument()
                                    .findRelatedDocuments(documentsBaseList, includeAnulledDocuments));
                        }
                    }
                }
            }

        }

        return documentsBaseList;
    }

    @Atomic
    public void anullDocument(final String reason) {

        if (Strings.isNullOrEmpty(reason)) {
            throw new TreasuryDomainException("error.CreditNote.anullDocument.reason.required");
        }

        if (this.isPreparing()) {

            if (getCreditEntries().anyMatch(ce -> ce.isFromExemption())) {
                throw new TreasuryDomainException("error.CreditNote.entry.from.exemption.cannot.be.annuled");
            }

            if (getCreditEntries().anyMatch(ce -> !ce.getSettlementEntriesSet().isEmpty())) {
                throw new TreasuryDomainException("error.CreditNote.cannot.delete.has.settlemententries");
            }

            setState(FinantialDocumentStateType.ANNULED);

        	final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

            if (!Strings.isNullOrEmpty(loggedUsername)) {
                setAnnulledReason(reason + " - [" + loggedUsername + "] "
                        + new DateTime().toString("YYYY-MM-dd HH:mm:ss"));
            } else {
                setAnnulledReason(reason + " - " + new DateTime().toString("YYYY-MM-dd HH:mm:ss"));
            }

        } else {
            throw new TreasuryDomainException(Constants.bundle("error.FinantialDocumentState.invalid.state.change.request"));
        }

        checkRules();
    }

    public CreditNote anullReimbursementCreditNoteAndCopy(final String annuledReason) {
        if (!isClosed()) {
            throw new TreasuryDomainException(
                    "error.CreditNote.anullReimbursementCreditNoteAndCopy.copy.only.on.closed.credit.note");
        }

        if (!isRelatedToReimbursement()) {
            throw new TreasuryDomainException("error.CreditNote.creditNote.not.from.reimbursement");
        }

        if (isAdvancePayment()) {
            throw new TreasuryDomainException("error.CreditNote.annulment.over.advance.payment.not.possible");
        }

        if (ReimbursementUtils.isCreditNoteSettledWithPayment(this)) {
            throw new TreasuryDomainException("error.CreditNote.annulment.over.credit.with.payments.not.possible");
        }

        setState(FinantialDocumentStateType.ANNULED);
        setAnnulledReason(annuledReason);

        final CreditNote creditNote =
                create(getDebtAccount(), getDocumentNumberSeries(), new DateTime(), getDebitNote(), getOriginDocumentNumber());

        for (final CreditEntry creditEntry : getCreditEntriesSet()) {
            CreditEntry.create(creditNote, creditEntry.getDescription(), creditEntry.getProduct(), creditEntry.getVat(),
                    creditEntry.getAmount(), new DateTime(), creditEntry.getDebitEntry(), creditEntry.getQuantity());
        }

        return creditNote;
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<? extends CreditNote> findAll() {
        return Invoice.findAll().filter(i -> i instanceof CreditNote).map(CreditNote.class::cast);
    }

    @Atomic
    public static CreditNote create(final DebtAccount debtAccount, final DocumentNumberSeries documentNumberSeries,
            final DateTime documentDate, DebitNote debitNote, String originNumber) {
        CreditNote note = new CreditNote(debtAccount, documentNumberSeries, documentDate, debitNote);
        note.setOriginDocumentNumber(originNumber);
        note.checkRules();
        return note;
    }

    public static CreditEntry createBalanceTransferCredit(final DebtAccount debtAccount, final DateTime documentDate,
            final String originNumber, final Product product, final BigDecimal amountWithVat, final DebtAccount payorDebtAccount,
            String entryDescription) {

        final FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        final Series regulationSeries = finantialInstitution.getRegulationSeries();
        final DocumentNumberSeries numberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), regulationSeries);
        final Vat transferVat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, documentDate).get();

        if (Strings.isNullOrEmpty(entryDescription)) {
            entryDescription = product.getName().getContent();
        }

        final CreditNote creditNote = create(debtAccount, numberSeries, documentDate, null, originNumber);

        final BigDecimal amountWithoutVat = Constants.divide(amountWithVat, BigDecimal.ONE.add(transferVat.getTaxRate()));
        CreditEntry entry = CreditEntry.create(creditNote, entryDescription, product, transferVat, amountWithoutVat, documentDate,
                null, BigDecimal.ONE);

        creditNote.editPayorDebtAccount(payorDebtAccount);

        return entry;
    }

}
