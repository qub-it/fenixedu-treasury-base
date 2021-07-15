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
package org.fenixedu.treasury.domain.debt.balancetransfer;

import static org.fenixedu.treasury.services.integration.erp.sap.SAPExporter.ERP_INTEGRATION_START_DATE;
import static org.fenixedu.treasury.util.TreasuryConstants.isEqual;
import static org.fenixedu.treasury.util.TreasuryConstants.isGreaterOrEqualThan;
import static org.fenixedu.treasury.util.TreasuryConstants.isPositive;
import static org.fenixedu.treasury.util.TreasuryConstants.rationalVatRate;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.CreditNote;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentEntry;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.Series;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentEntry;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentSettlementEntry;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlan;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanSettings;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanStateType;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class BalanceTransferService {

    private DebtAccount objectDebtAccount;
    private DebtAccount destinyDebtAccount;

    private Map<DebitEntry, DebitEntry> conversionMap;
    private Map<DebitEntry, SettlementEntry> settlementOfDebitEntryMap;

    private Set<PaymentPlan> openPaymentPlans;

    public BalanceTransferService(final DebtAccount objectDebtAccount, final DebtAccount destinyDebtAccount) {
        this.objectDebtAccount = objectDebtAccount;
        this.destinyDebtAccount = destinyDebtAccount;
        this.conversionMap = new HashMap<>();
        this.settlementOfDebitEntryMap = new HashMap<>();
        this.openPaymentPlans = objectDebtAccount.getActivePaymentPlansSet();
    }

    @Atomic
    public void transferBalance() {
        // Change open payment plans temporarly, to annul debit entries
        this.openPaymentPlans.forEach(p -> {
            //update object payment Plan
            p.setState(PaymentPlanStateType.TRANSFERRED);
            p.setStateReason(treasuryBundle("label.BalanceTransferService.paymentPlan.reason",
                    destinyDebtAccount.getCustomer().getFiscalNumber()));
        });

        final BigDecimal initialGlobalBalance = objectDebtAccount.getCustomer().getGlobalBalance();

        final FinantialInstitution finantialInstitution = objectDebtAccount.getFinantialInstitution();
        final Currency currency = finantialInstitution.getCurrency();
        final DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForDebitNote(), finantialInstitution).get();
        final DateTime now = new DateTime();

        final Set<DebitNote> pendingDebitNotes = Sets.newHashSet();
        for (final InvoiceEntry invoiceEntry : objectDebtAccount.getPendingInvoiceEntriesSet()) {

            if (invoiceEntry.isDebitNoteEntry()) {
                final DebitEntry debitEntry = (DebitEntry) invoiceEntry;
                if (debitEntry.getFinantialDocument() == null) {
                    final DebitNote debitNote = DebitNote.create(objectDebtAccount, documentNumberSeries, now);
                    debitNote.addDebitNoteEntries(Lists.newArrayList(debitEntry));
                }

                pendingDebitNotes.add((DebitNote) debitEntry.getFinantialDocument());
            }
        }

        for (final DebitNote debitNote : pendingDebitNotes) {
            transferDebitEntries(debitNote);
        }

        for (final InvoiceEntry invoiceEntry : objectDebtAccount.getPendingInvoiceEntriesSet()) {
            if (invoiceEntry.isCreditNoteEntry()) {
                transferCreditEntry((CreditEntry) invoiceEntry);
            }
        }

        final BigDecimal finalGlobalBalance = objectDebtAccount.getCustomer().getGlobalBalance();

        if (!isEqual(initialGlobalBalance, finalGlobalBalance)) {
            throw new TreasuryDomainException("error.BalanceTransferService.initial.and.final.global.balance",
                    currency.getValueFor(initialGlobalBalance), currency.getValueFor(finalGlobalBalance));
        }

        transferPaymentPlans();
    }

    private void transferPaymentPlans() {
        for (PaymentPlan objectPaymentPlan : this.openPaymentPlans) {
            // Create destiny PaymentPlan
            PaymentPlan destinyPaymentPlan = new PaymentPlan();
            destinyPaymentPlan.setCreationDate(objectPaymentPlan.getCreationDate());
            destinyPaymentPlan.setDebtAccount(destinyDebtAccount);
            destinyPaymentPlan.setReason(objectPaymentPlan.getReason());
            destinyPaymentPlan.setPaymentPlanId(PaymentPlanSettings.getActiveInstance().getNumberGenerators().generateNumber());
            destinyPaymentPlan.setState(PaymentPlanStateType.OPEN);
            destinyPaymentPlan.getPaymentPlanValidatorsSet().addAll(objectPaymentPlan.getPaymentPlanValidatorsSet());
            destinyPaymentPlan.setEmolument(conversionMap.get(objectPaymentPlan.getEmolument()));

            for (Installment objectInstallment : objectPaymentPlan.getSortedOpenInstallments()) {
                //Create destiny Installment
                Installment destinyInstallment =
                        Installment.create(
                                objectInstallment.getDescription()
                                        .map(des -> des.replace(objectPaymentPlan.getPaymentPlanId(),
                                                destinyPaymentPlan.getPaymentPlanId())),
                                objectInstallment.getDueDate(), destinyPaymentPlan);
                for (InstallmentEntry objectInstallmentEntry : objectInstallment.getSortedOpenInstallmentEntries()) {
                    DebitEntry destinyDebitEntry = conversionMap.get(objectInstallmentEntry.getDebitEntry());
                    if (destinyDebitEntry.getDebitNote() != null && !destinyDebitEntry.getDebitNote().isClosed()) {
                        destinyDebitEntry.getDebitNote().closeDocument();
                    }

                    //Create destiny InstallmentEntry
                    InstallmentEntry.create(destinyDebitEntry, objectInstallmentEntry.getOpenAmount(), destinyInstallment);

                    //Create SettlementEntry for object InstallmentEntry
                    SettlementEntry settlementEntry = settlementOfDebitEntryMap.get(objectInstallmentEntry.getDebitEntry());
                    if (settlementEntry != null) {
                        InstallmentSettlementEntry.create(objectInstallmentEntry, settlementEntry,
                                objectInstallmentEntry.getOpenAmount());
                    }
                }
            }

            destinyPaymentPlan.createPaymentReferenceCode();

            //Add Revision to Payment Plan
            objectPaymentPlan.addPaymentPlanRevisions(destinyPaymentPlan);

            //Validate Rules
            objectPaymentPlan.checkRules();
            destinyPaymentPlan.checkRules();
        }
    }

    private void transferCreditEntry(final CreditEntry invoiceEntry) {
        final FinantialInstitution finantialInstitution = objectDebtAccount.getFinantialInstitution();
        final Series defaultSeries = Series.findUniqueDefault(finantialInstitution).get();
        final DocumentNumberSeries settlementNoteSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForSettlementNote(), defaultSeries);
        final DateTime now = new DateTime();

        final CreditEntry creditEntry = invoiceEntry;
        final BigDecimal creditOpenAmount = creditEntry.getOpenAmount();

        final String originNumber = creditEntry.getFinantialDocument() != null
                && invoiceEntry.getFinantialDocument().isClosed() ? creditEntry.getFinantialDocument().getUiDocumentNumber() : "";
        final DebtAccount payorDebtAccount =
                creditEntry.getFinantialDocument() != null && ((Invoice) creditEntry.getFinantialDocument())
                        .isForPayorDebtAccount() ? ((Invoice) creditEntry.getFinantialDocument()).getPayorDebtAccount() : null;
        DebitEntry regulationDebitEntry = DebitNote.createBalanceTransferDebit(objectDebtAccount, now, now.toLocalDate(),
                originNumber, creditEntry.getProduct(), creditOpenAmount, payorDebtAccount, creditEntry.getDescription(), null);

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {

            if (creditEntry.getFinantialDocument().isExportedInLegacyERP()
                    || (creditEntry.getFinantialDocument().getCloseDate() != null
                            && creditEntry.getFinantialDocument().getCloseDate().isBefore(ERP_INTEGRATION_START_DATE))) {
                regulationDebitEntry.getFinantialDocument().setExportedInLegacyERP(true);
                regulationDebitEntry.getFinantialDocument().setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
            }

        }

        regulationDebitEntry.getFinantialDocument().closeDocument();
        CreditEntry regulationCreditEntry = CreditNote.createBalanceTransferCredit(destinyDebtAccount, now, originNumber,
                creditEntry.getProduct(), creditOpenAmount, payorDebtAccount, creditEntry.getDescription());

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {

            if (creditEntry.getFinantialDocument().isExportedInLegacyERP()
                    || (creditEntry.getFinantialDocument().getCloseDate() != null
                            && creditEntry.getFinantialDocument().getCloseDate().isBefore(ERP_INTEGRATION_START_DATE))) {
                regulationCreditEntry.getFinantialDocument().setExportedInLegacyERP(true);
                regulationCreditEntry.getFinantialDocument().setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
            }

        }

        final SettlementNote settlementNote =
                SettlementNote.create(objectDebtAccount, settlementNoteSeries, now, now, null, null);

        if (creditEntry.getFinantialDocument().isPreparing()) {
            creditEntry.getFinantialDocument().closeDocument();
        }

        SettlementEntry.create(regulationDebitEntry, settlementNote, regulationDebitEntry.getOpenAmount(),
                regulationDebitEntry.getDescription(), now, false);
        SettlementEntry.create(creditEntry, settlementNote, creditOpenAmount, creditEntry.getDescription(), now, false);

        settlementNote.markAsUsedInBalanceTransfer();
        settlementNote.closeDocument();
    }

    private void transferDebitEntries(final DebitNote objectDebitNote) {
        final FinantialInstitution finantialInstitution = objectDebitNote.getDebtAccount().getFinantialInstitution();
        final DocumentNumberSeries settlementNumberSeries =
                DocumentNumberSeries.findUniqueDefault(FinantialDocumentType.findForSettlementNote(), finantialInstitution).get();
        final DateTime now = new DateTime();

        if (objectDebitNote.isPreparing()) {
            anullPreparingDebitNote(objectDebitNote);
        } else if (objectDebitNote.isClosed()) {
            final DebtAccount payorDebtAccount =
                    objectDebitNote.isForPayorDebtAccount() ? objectDebitNote.getPayorDebtAccount() : null;

            final DebitNote destinyDebitNote = DebitNote.create(destinyDebtAccount, payorDebtAccount,
                    objectDebitNote.getDocumentNumberSeries(), now, now.toLocalDate(), objectDebitNote.getUiDocumentNumber());

            final SettlementNote settlementNote =
                    SettlementNote.create(objectDebtAccount, settlementNumberSeries, now, now, null, null);
            for (final FinantialDocumentEntry objectEntry : objectDebitNote.getFinantialDocumentEntriesSet()) {
                if (!isPositive(((DebitEntry) objectEntry).getOpenAmount())) {
                    continue;
                }

                final DebitEntry debitEntry = (DebitEntry) objectEntry;

                final BigDecimal openAmount = debitEntry.getOpenAmount();
                final BigDecimal availableCreditAmount = debitEntry.getAvailableAmountForCredit();

                DebitEntry destinyDebitEntry = null;
                SettlementEntry destinySettlementEntry = null;

                if (!debitEntry.getFinantialDocument().getDocumentNumberSeries().getSeries().isRegulationSeries()
                        && isGreaterOrEqualThan(availableCreditAmount, openAmount)) {

                    final BigDecimal openAmountWithoutVat = debitEntry.getCurrency().getValueWithScale(
                            TreasuryConstants.divide(openAmount, BigDecimal.ONE.add(rationalVatRate(debitEntry))));
                    final CreditEntry newCreditEntry = debitEntry.createCreditEntry(now, debitEntry.getDescription(), null, null,
                            openAmountWithoutVat, null, null);

                    newCreditEntry.getFinantialDocument().closeDocument();

                    destinySettlementEntry = SettlementEntry.create(debitEntry, settlementNote, openAmount,
                            debitEntry.getDescription(), now, false);
                    SettlementEntry.create(newCreditEntry, settlementNote, openAmount, newCreditEntry.getDescription(), now,
                            false);
                    destinyDebitEntry = createDestinyDebitEntry(destinyDebitNote, debitEntry);

                } else {

                    final CreditEntry regulationCreditEntry = CreditNote.createBalanceTransferCredit(objectDebtAccount, now,
                            objectDebitNote.getUiDocumentNumber(), debitEntry.getProduct(), openAmount, payorDebtAccount, null);

                    if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {

                        if (objectDebitNote.isExportedInLegacyERP()
                                || objectDebitNote.getCloseDate().isBefore(ERP_INTEGRATION_START_DATE)) {
                            regulationCreditEntry.getFinantialDocument().setExportedInLegacyERP(true);
                            regulationCreditEntry.getFinantialDocument().setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
                        }

                    }

                    regulationCreditEntry.getFinantialDocument().closeDocument();

                    destinySettlementEntry = SettlementEntry.create(debitEntry, settlementNote, openAmount,
                            debitEntry.getDescription(), now, false);

                    SettlementEntry.create(regulationCreditEntry, settlementNote, openAmount,
                            regulationCreditEntry.getDescription(), now, false);

                    final DebitEntry regulationDebitEntry = DebitNote.createBalanceTransferDebit(destinyDebtAccount,
                            debitEntry.getEntryDateTime(), debitEntry.getDueDate(),
                            regulationCreditEntry.getFinantialDocument().getUiDocumentNumber(), debitEntry.getProduct(),
                            openAmount, payorDebtAccount, debitEntry.getDescription(), debitEntry.getInterestRate());

                    destinyDebitEntry = regulationDebitEntry;

                    if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {

                        if (objectDebitNote.isExportedInLegacyERP()
                                || objectDebitNote.getCloseDate().isBefore(ERP_INTEGRATION_START_DATE)) {
                            regulationDebitEntry.getFinantialDocument().setExportedInLegacyERP(true);
                            regulationDebitEntry.getFinantialDocument().setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
                        }

                    }
                    regulationDebitEntry.getFinantialDocument().closeDocument();
                }

                //paymentPlan
                conversionMap.put(debitEntry, destinyDebitEntry);
                settlementOfDebitEntryMap.put(debitEntry, destinySettlementEntry);
            }

            settlementNote.markAsUsedInBalanceTransfer();
            settlementNote.closeDocument();
        }
    }

    private DebitEntry createDestinyDebitEntry(final DebitNote destinyDebitNote, final DebitEntry debitEntry) {
        final BigDecimal openAmountWithoutVat =
                TreasuryConstants.divide(debitEntry.getOpenAmount(), BigDecimal.ONE.add(rationalVatRate(debitEntry)));

        final DebitEntry newDebitEntry = DebitEntry.create(Optional.of(destinyDebitNote), destinyDebtAccount,
                debitEntry.getTreasuryEvent(), debitEntry.getVat(), openAmountWithoutVat, debitEntry.getDueDate(),
                debitEntry.getPropertiesMap(), debitEntry.getProduct(), debitEntry.getDescription(), debitEntry.getQuantity(),
                debitEntry.getInterestRate(), debitEntry.getEntryDateTime());

        newDebitEntry.edit(newDebitEntry.getDescription(), newDebitEntry.getTreasuryEvent(), newDebitEntry.getDueDate(),
                debitEntry.isAcademicalActBlockingSuspension(), debitEntry.isBlockAcademicActsOnDebt());

        return newDebitEntry;
    }

    private void anullPreparingDebitNote(final DebitNote objectDebitNote) {
        final DateTime now = new DateTime();
        final DebitNote newDebitNote = DebitNote.create(destinyDebtAccount, objectDebitNote.getPayorDebtAccount(),
                objectDebitNote.getDocumentNumberSeries(), now, now.toLocalDate(), "");

        for (final FinantialDocumentEntry objectEntry : objectDebitNote.getFinantialDocumentEntriesSet()) {
            final DebitEntry debitEntry = (DebitEntry) objectEntry;
            final BigDecimal amountWithExemptedAmount = debitEntry.getAmount().add(debitEntry.getExemptedAmount());

            if (!isPositive(amountWithExemptedAmount)) {
                continue;
            }

            if (debitEntry.getTreasuryEvent() != null) {
                debitEntry.annulOnEvent();
            }

            DebitEntry newDebitEntry = DebitEntry.create(Optional.of(newDebitNote), destinyDebtAccount,
                    debitEntry.getTreasuryEvent(), debitEntry.getVat(), amountWithExemptedAmount, debitEntry.getDueDate(),
                    debitEntry.getPropertiesMap(), debitEntry.getProduct(), debitEntry.getDescription(), debitEntry.getQuantity(),
                    debitEntry.getInterestRate(), debitEntry.getEntryDateTime());

            if (debitEntry.getTreasuryExemption() != null) {
                final TreasuryExemption treasuryExemption = debitEntry.getTreasuryExemption();
                TreasuryExemption.create(treasuryExemption.getTreasuryExemptionType(), debitEntry.getTreasuryEvent(),
                        treasuryExemption.getReason(), treasuryExemption.getValueToExempt(), newDebitEntry);

            }

            newDebitEntry.edit(newDebitEntry.getDescription(), newDebitEntry.getTreasuryEvent(), newDebitEntry.getDueDate(),
                    debitEntry.isAcademicalActBlockingSuspension(), debitEntry.isBlockAcademicActsOnDebt());

            //paymentPlan
            conversionMap.put(debitEntry, newDebitEntry);

        }

        objectDebitNote.anullDebitNoteWithCreditNote(treasuryBundle("label.BalanceTransferService.annuled.reason"), false);
    }

}
