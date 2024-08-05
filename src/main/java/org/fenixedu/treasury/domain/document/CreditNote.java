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
package org.fenixedu.treasury.domain.document;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;

public class CreditNote extends CreditNote_Base {

    public CreditNote() {
        super();
    }

    protected CreditNote(FinantialEntity finantialEntity, DebtAccount debtAccount, DocumentNumberSeries documentNumberSeries,
            DateTime documentDate, DebitNote debitNote) {
        super();

        init(finantialEntity, debtAccount, documentNumberSeries, documentDate, debitNote);
        checkRules();
    }

    protected void init(FinantialEntity finantialEntity, DebtAccount debtAccount, DocumentNumberSeries documentNumberSeries,
            DateTime documentDate, DebitNote debitNote) {
        super.init(finantialEntity, debtAccount, documentNumberSeries, documentDate);

        this.setDebitNote(debitNote);

        if (debitNote != null) {
            this.setPayorDebtAccount(debitNote.getPayorDebtAccount());
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

        if (getDebitNote() != null && getPayorDebtAccount() != getDebitNote().getPayorDebtAccount()) {
            throw new TreasuryDomainException("error.CreditNote.with.payorDebtAccount.different.from.debit.note");
        }

        if (getDebitNote() != null && getDebitNote().getDocumentNumberSeries().getSeries().isRegulationSeries()) {
            throw new TreasuryDomainException("error.CreditNote.debit.note.cannot.be.from.regulation.series");
        }

        if (getDebitNote() != null && getDocumentNumberSeries().getSeries().isRegulationSeries()) {
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
    @Atomic
    public void delete(boolean deleteEntries) {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.CreditNote.cannot.delete");
        }

        setFinantialEntity(null);
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

    @Override
    public void closeDocument(boolean markDocumentToExport) {
        if (!TreasuryConstants.isPositive(getTotalNetAmount())) {
            throw new TreasuryDomainException("error.CreditNote.close.document.with.netAmount.zero.not.supported");
        }

        super.closeDocument(markDocumentToExport);

        TreasuryPlataformDependentServicesFactory.implementation().certifyDocument(this);
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
    public void updateCreditNote(String originDocumentNumber, String documentObservations, String documentTermsAndConditions) {
        setOriginDocumentNumber(originDocumentNumber);
        setDocumentObservations(documentObservations);
        setDocumentTermsAndConditions(documentTermsAndConditions);
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

    /**
     * This method will annul a credit note if the state is preparing
     * 
     * @param reason
     */
    @Atomic
    public void anullDocument(final String reason) {

        if (Strings.isNullOrEmpty(reason)) {
            throw new TreasuryDomainException("error.CreditNote.anullDocument.reason.required");
        }

        if (TreasuryDebtProcessMainService.isFinantialDocumentAnnullmentActionBlocked(this)) {
            throw new TreasuryDomainException("error.CreditNote.cannot.annull.or.credit.due.to.existing.active.debt.process");
        }

        if (this.isPreparing()) {

            if (getCreditEntries().anyMatch(ce -> ce.isFromExemption())) {
                throw new TreasuryDomainException("error.CreditNote.entry.from.exemption.cannot.be.annuled");
            }

            if (getCreditEntries().anyMatch(ce -> !ce.getSettlementEntriesSet().isEmpty())) {
                throw new TreasuryDomainException("error.CreditNote.cannot.delete.has.settlemententries");
            }

            setState(FinantialDocumentStateType.ANNULED);
            setAnnulledReason(reason);
            setAnnullmentDate(new DateTime());

            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
            setAnnullmentResponsible(!Strings.isNullOrEmpty(loggedUsername) ? loggedUsername : "unknown");

            TreasuryPlataformDependentServicesFactory.implementation().annulCertifiedDocument(this);
        } else {
            throw new TreasuryDomainException(treasuryBundle("error.FinantialDocumentState.invalid.state.change.request"));
        }

        checkRules();
    }

    public void annulCertifiedCreditNote(String reason) {
        if (!isCertifiedCreditNoteAnnulable()) {
            throw new TreasuryDomainException(
                    "error.CreditNote.annulCertifiedCreditNote.not.possible.due.existing.active.settlements");
        }

        // Activate the debit entries in treasury event
        getCreditEntriesSet().stream() //
                .filter(ce -> ce.getDebitEntry() != null) //
                .filter(ce -> !ce.getDebitEntry().isAnnulled()) //
                .map(ce -> ce.getDebitEntry()) //
                .filter(de -> de.getTreasuryEvent() != null && !de.isIncludedInEvent()) //
                .forEach(de -> de.revertEventAnnuled());

        this.setState(FinantialDocumentStateType.ANNULED);

        setAnnulledReason(reason);
        setAnnullmentDate(new DateTime());

        final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
        setAnnullmentResponsible(!Strings.isNullOrEmpty(loggedUsername) ? loggedUsername : "unknown");

        TreasuryPlataformDependentServicesFactory.implementation().annulCertifiedDocument(this);
    }

    public boolean isCertifiedCreditNoteAnnulable() {
        if (!isClosed()) {
            return false;
        }

        boolean withAllSettlementNotesAnnuled = getCreditEntriesSet().stream()
                .allMatch(de -> de.getSettlementEntriesSet().stream().allMatch(se -> se.getFinantialDocument().isAnnulled()));

        return withAllSettlementNotesAnnuled;
    }

    public void updateCertificationOriginDocumentReference(String certificationOriginDocumentReference) {
        if (!isPreparing()) {
            throw new TreasuryDomainException("error.CreditNote.updateCertificationOriginDocumentReference.is.not.preparing");
        }

        if (getDebitNote() != null) {
            throw new TreasuryDomainException(
                    "error.CreditNote.updateCertificationOriginDocumentReference.is.associated.with.debitNote");
        }

        super.setCertificationOriginDocumentReference(certificationOriginDocumentReference);
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<CreditNote> findAll() {
        return Invoice.findAll().filter(i -> i instanceof CreditNote).map(CreditNote.class::cast);
    }

    public static Stream<CreditNote> find(final DebtAccount debtAccount) {
        return debtAccount.getFinantialDocumentsSet().stream().filter(x -> x instanceof CreditNote).map(CreditNote.class::cast);
    }

    @Atomic
    public static CreditNote create(DebitNote debitNote, DocumentNumberSeries documentNumberSeries, DateTime documentDate,
            String originNumber) {
        if (!debitNote.isClosed()) {
            throw new TreasuryDomainException("error.CreditNote.debitnote.not.closed");
        }

        DebtAccount debtAccount = debitNote.getDebtAccount();

        FinantialEntity finantialEntity = debitNote.getFinantialEntity();

        if (Boolean.TRUE.equals(debtAccount.getFinantialInstitution().getSeriesByFinantialEntity())) {
            if (documentNumberSeries.getSeries().getFinantialEntity() != finantialEntity) {
                throw new TreasuryDomainException("error.CreditNote.documentNumberSeries.finantialEntity.mismatch");
            }
        } else {
            if (documentNumberSeries.getSeries().getFinantialInstitution() != debtAccount.getFinantialInstitution()) {
                throw new TreasuryDomainException("error.CreditNote.documentNumberSeries.finantialInstitution.mismatch");
            }

            if (documentNumberSeries.getSeries().getFinantialEntity() != null) {
                throw new TreasuryDomainException("error.CreditNote.documentNumberSeries.finantialInstitution.mismatch");
            }
        }

        CreditNote note = new CreditNote(finantialEntity, debtAccount, documentNumberSeries, documentDate, debitNote);
        note.setOriginDocumentNumber(originNumber);
        note.checkRules();

        return note;
    }

    @Atomic
    public static CreditNote create(FinantialEntity finantialEntity, DebtAccount debtAccount,
            DocumentNumberSeries documentNumberSeries, DebtAccount payorDebtAccount, DateTime documentDate, String originNumber) {
        CreditNote note = new CreditNote(finantialEntity, debtAccount, documentNumberSeries, documentDate, null);

        if (Boolean.TRUE.equals(debtAccount.getFinantialInstitution().getSeriesByFinantialEntity())) {
            if (documentNumberSeries.getSeries().getFinantialEntity() != finantialEntity) {
                throw new TreasuryDomainException("error.CreditNote.documentNumberSeries.finantialEntity.mismatch");
            }
        } else {
            if (documentNumberSeries.getSeries().getFinantialInstitution() != debtAccount.getFinantialInstitution()) {
                throw new TreasuryDomainException("error.CreditNote.documentNumberSeries.finantialInstitution.mismatch");
            }

            if (documentNumberSeries.getSeries().getFinantialEntity() != null) {
                throw new TreasuryDomainException("error.CreditNote.documentNumberSeries.finantialInstitution.mismatch");
            }
        }

        note.setPayorDebtAccount(payorDebtAccount);
        note.setOriginDocumentNumber(originNumber);

        note.checkRules();

        return note;
    }

    public static CreditNote createForImportation(FinantialEntity finantialEntity, DebtAccount debtAccount,
            DocumentNumberSeries documentNumberSeries, DateTime documentDate, DebitNote debitNote, String originNumber) {
        CreditNote note = new CreditNote(finantialEntity, debtAccount, documentNumberSeries, documentDate, debitNote);

        note.setOriginDocumentNumber(originNumber);
        note.checkRules();

        return note;
    }

}
