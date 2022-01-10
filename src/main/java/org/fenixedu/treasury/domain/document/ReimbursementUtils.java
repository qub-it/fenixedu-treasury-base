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

import static org.fenixedu.treasury.util.TreasuryConstants.divide;
import static org.fenixedu.treasury.util.TreasuryConstants.rationalVatRate;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.Optional;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Maps;

public class ReimbursementUtils {

    public static boolean isInReimbursementCreditsRestrictionModeOfSAP(CreditNote creditNote) {
        return !creditNote.getDebtAccount().getFinantialInstitution().isToCloseCreditNoteWhenCreated();
    }

    public static boolean isInReimbursementCreditsRestrictionModeOfSAP(CreditEntry creditEntry) {
        return !creditEntry.getDebtAccount().getFinantialInstitution().isToCloseCreditNoteWhenCreated();
    }

    public static boolean isCreditNoteSettledWithPayment(final CreditNote creditNote) {
        return creditNote.getCreditEntries().flatMap(c -> c.getSettlementEntriesSet().stream())
                .filter(se -> !((SettlementNote) se.getFinantialDocument()).isReimbursement()).count() > 0;
    }

    public static boolean isCreditNoteForReimbursementMustBeClosedWithDebitNoteAndCreatedNew(final CreditEntry creditEntry) {
        if (!isInReimbursementCreditsRestrictionModeOfSAP(creditEntry)) {
            return false;
        }
        
        final CreditNote creditNote = (CreditNote) creditEntry.getFinantialDocument();

        if (creditNote.isAnnulled()) {
            throw new TreasuryDomainException("error.ReimbursementUtils.creditNote.annulled");
        }

        if (creditNote.isPreparing()) {
            if (creditNote.getCreditEntries().flatMap(c -> c.getSettlementEntriesSet().stream()).count() > 0) {
                throw new TreasuryDomainException("error.ReimbursementUtils.creditNote.with.settlement.entries.already");
            }

            return false;
        }

        if (creditNote.isAdvancePayment()) {
            return false;
        }

        if (creditNote.isExportedInLegacyERP()) {
            return true;
        }
        
        if(creditNote.getDocumentNumberSeries().getSeries().isRegulationSeries() && 
                creditNote.getCloseDate().isBefore(SAPExporter.ERP_INTEGRATION_START_DATE)) {
            // ANIL 2017-05-04: This is applied to advanced payments in legacy ERP converted 
            // to regulation series with specific product
            return true;
        }

        if (creditNote.getCloseDate().isBefore(SAPExporter.ERP_INTEGRATION_START_DATE)) {
            throw new TreasuryDomainException(
                    "error.ReimbursementUtils.creditNote.marked.as.exportedInLegacyERP.but.close.date.not.conformant");
        }

        return isCreditNoteSettledWithPayment(creditNote);
    }

    public static CreditEntry closeWithDebitNoteAndCreateNewCreditNoteForReimbursement(final CreditEntry originalCreditEntry,
            final BigDecimal amountToReimburseWithVat) {
        final DateTime now = new DateTime();
        final DebtAccount debtAccount = originalCreditEntry.getDebtAccount();
        final FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        final CreditNote originalCreditNote = (CreditNote) originalCreditEntry.getFinantialDocument();
        final Series series = Series.findUniqueDefault(finantialInstitution).get();

        final DebtAccount payorDebtAccount = originalCreditNote.getPayorDebtAccount();

        final DocumentNumberSeries debitNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), series);
        final DocumentNumberSeries creditNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), series);
        final DocumentNumberSeries settlementNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForSettlementNote(), series);

        if (TreasuryConstants.isGreaterThan(amountToReimburseWithVat, originalCreditEntry.getOpenAmount())) {
            throw new TreasuryDomainException("error.ReimbursementUtils.amountToReimburse.greater.than.open.amount.of.credit");
        }

        if (originalCreditNote.isPreparing()) {
            originalCreditNote.closeDocument();
        }

        final Vat vat = originalCreditEntry.getVat();
        final BigDecimal amountToReimburseWithoutVat =
                divide(amountToReimburseWithVat, BigDecimal.ONE.add(rationalVatRate(originalCreditEntry)));

        final DebitNote compensationDebitNote = DebitNote.create(debtAccount, payorDebtAccount, debitNumberSeries, now,
                new LocalDate(), originalCreditNote.getUiDocumentNumber());
        final DebitEntry compensationDebitEntry = DebitEntry.create(Optional.of(compensationDebitNote), debtAccount, null, vat,
                amountToReimburseWithoutVat, new LocalDate(), Maps.newHashMap(), originalCreditEntry.getProduct(),
                treasuryBundle("label.ReimbursementUtils.compensation.debit.entry.description",
                        originalCreditEntry.getDescription()),
                BigDecimal.ONE, null, now);

        compensationDebitNote.closeDocument();

        settlementCompensation(originalCreditEntry, amountToReimburseWithVat, now, debtAccount, settlementNumberSeries,
                compensationDebitEntry);

        final CreditNote creditNoteToReimburse = CreditNote.create(debtAccount, creditNumberSeries, now, compensationDebitNote,
                originalCreditNote.getUiDocumentNumber());
        final CreditEntry creditEntryToReimburse = compensationDebitEntry.createCreditEntry(now,
                originalCreditEntry.getDescription(), originalCreditNote.getDocumentObservations(),
                originalCreditNote.getDocumentTermsAndConditions(), amountToReimburseWithoutVat, null, creditNoteToReimburse);

        return creditEntryToReimburse;
    }

    private static void settlementCompensation(final CreditEntry originalCreditEntry, final BigDecimal amountToReimburseWithVat,
            final DateTime now, final DebtAccount debtAccount, final DocumentNumberSeries settlementNumberSeries,
            final DebitEntry compensationDebitEntry) {
        final SettlementNote compensationSettlementNote =
                SettlementNote.create(debtAccount, settlementNumberSeries, now, now, null, null);

        SettlementEntry.create(compensationDebitEntry, compensationSettlementNote, amountToReimburseWithVat,
                compensationDebitEntry.getDescription(), now, false);
        SettlementEntry.create(originalCreditEntry, compensationSettlementNote, amountToReimburseWithVat,
                originalCreditEntry.getDescription(), now, false);
        
        compensationSettlementNote.closeDocument();
    }

}
