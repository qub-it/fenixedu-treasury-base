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
import java.util.*;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
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
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanConfigurator;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanStateType;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class StandardBalanceTransferServiceForSAPAndSINGAP implements BalanceTransferService {

    protected DebtAccount fromDebtAccount;
    protected DebtAccount destinyDebtAccount;

    protected Map<DebitEntry, DebitEntry> debitEntriesConversionMap;
    protected Map<DebitEntry, SettlementEntry> settlementOfDebitEntryMap;

    protected Map<Installment, Installment> installmentsConversionMap;

    private Set<PaymentPlan> openPaymentPlans;

    public StandardBalanceTransferServiceForSAPAndSINGAP(DebtAccount fromDebtAccount, DebtAccount destinyDebtAccount) {
        this.fromDebtAccount = fromDebtAccount;
        this.destinyDebtAccount = destinyDebtAccount;
        this.debitEntriesConversionMap = new HashMap<>();
        this.installmentsConversionMap = new HashMap<>();
        this.settlementOfDebitEntryMap = new HashMap<>();
        this.openPaymentPlans = fromDebtAccount.getActivePaymentPlansSet();
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

        final BigDecimal initialGlobalBalance = fromDebtAccount.getCustomer().getGlobalBalance();

        final FinantialInstitution finantialInstitution = fromDebtAccount.getFinantialInstitution();
        final Currency currency = finantialInstitution.getCurrency();
        final DateTime now = new DateTime();

        final Set<DebitNote> pendingDebitNotes = Sets.newHashSet();
        for (final InvoiceEntry invoiceEntry : fromDebtAccount.getPendingInvoiceEntriesSet()) {
            FinantialEntity finantialEntity = invoiceEntry.getFinantialEntity();

            final DocumentNumberSeries documentNumberSeries =
                    DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);

            if (TreasuryDebtProcessMainService.isFinantialDocumentEntryAnnullmentActionBlocked(invoiceEntry)) {
                throw new TreasuryDomainException("error.DebitEntry.cannot.annul.or.credit.due.to.existing.active.debt.process");
            }

            if (invoiceEntry.isDebitNoteEntry()) {
                final DebitEntry debitEntry = (DebitEntry) invoiceEntry;
                if (debitEntry.getFinantialDocument() == null) {
                    final DebitNote debitNote =
                            DebitNote.create(invoiceEntry.getFinantialEntity(), fromDebtAccount, null, documentNumberSeries, now,
                                    now.toLocalDate(), null, Collections.emptyMap(), null, null);
                    debitNote.addDebitNoteEntries(Lists.newArrayList(debitEntry));
                }

                pendingDebitNotes.add((DebitNote) debitEntry.getFinantialDocument());
            }
        }

        for (final DebitNote debitNote : pendingDebitNotes) {
            transferDebitEntries(debitNote);
        }

        for (final InvoiceEntry invoiceEntry : fromDebtAccount.getPendingInvoiceEntriesSet()) {
            if (invoiceEntry.isCreditNoteEntry()) {
                transferCreditEntry((CreditEntry) invoiceEntry);
            }
        }

        final BigDecimal finalGlobalBalance = fromDebtAccount.getCustomer().getGlobalBalance();

        if (!isEqual(initialGlobalBalance, finalGlobalBalance)) {
            throw new TreasuryDomainException("error.BalanceTransferService.initial.and.final.global.balance",
                    currency.getValueFor(initialGlobalBalance), currency.getValueFor(finalGlobalBalance));
        }

        transferPaymentPlans();

        transferPaymentInvoiceEntriesGroups();

        transferActiveMbwayMandates();
    }

    private void transferActiveMbwayMandates() {
        this.fromDebtAccount.getMbwayMandatesSet().stream() //
                .filter(mandate -> mandate.getState().isActive() || mandate.getState()
                        .isWaitingAuthorization() || mandate.getState().isSuspended()).forEach(
                        mandate -> mandate.transferMandateToOtherDebtAccount(this.destinyDebtAccount, this.debitEntriesConversionMap,
                                this.installmentsConversionMap));
    }

    private void transferPaymentInvoiceEntriesGroups() {
        // Collect the groups from the previous debt account and consider only those that are payable
        this.fromDebtAccount.getPaymentInvoiceEntriesGroupsSet().stream()
                .forEach(g -> g.transferPaymentGroupToNewDebtAccount(this.destinyDebtAccount, this.debitEntriesConversionMap));
    }

    private void transferPaymentPlans() {
        for (PaymentPlan objectPaymentPlan : this.openPaymentPlans) {
            // Create destiny PaymentPlan
            PaymentPlan destinyPaymentPlan = new PaymentPlan();
            destinyPaymentPlan.setFinantialEntity(objectPaymentPlan.getFinantialEntity());
            destinyPaymentPlan.setCreationDate(objectPaymentPlan.getCreationDate());
            destinyPaymentPlan.setDebtAccount(destinyDebtAccount);
            destinyPaymentPlan.setReason(objectPaymentPlan.getReason());
            destinyPaymentPlan.setPaymentPlanId(
                    PaymentPlanConfigurator.findActives().iterator().next().getNumberGenerators().generateNumber());
            destinyPaymentPlan.setState(PaymentPlanStateType.OPEN);
            destinyPaymentPlan.getPaymentPlanValidatorsSet().addAll(objectPaymentPlan.getPaymentPlanValidatorsSet());
            destinyPaymentPlan.setEmolument(debitEntriesConversionMap.get(objectPaymentPlan.getEmolument()));

            for (Installment objectInstallment : objectPaymentPlan.getSortedOpenInstallments()) {
                //Create destiny Installment
                Installment destinyInstallment = Installment.create(objectInstallment.getDescription()
                                .map(des -> des.replace(objectPaymentPlan.getPaymentPlanId(), destinyPaymentPlan.getPaymentPlanId())),
                        objectInstallment.getDueDate(), destinyPaymentPlan);

                this.installmentsConversionMap.put(objectInstallment, destinyInstallment);

                for (InstallmentEntry objectInstallmentEntry : objectInstallment.getSortedOpenInstallmentEntries()) {
                    DebitEntry destinyDebitEntry = debitEntriesConversionMap.get(objectInstallmentEntry.getDebitEntry());
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

    protected void transferCreditEntry(final CreditEntry invoiceEntry) {
        FinantialEntity finantialEntity = invoiceEntry.getFinantialEntity();
        final Series defaultSeries = Series.findUniqueDefaultSeries(finantialEntity);
        final DocumentNumberSeries settlementNoteSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForSettlementNote(), defaultSeries);
        final DateTime now = new DateTime();

        final CreditEntry creditEntry = invoiceEntry;
        final BigDecimal creditOpenAmount = creditEntry.getOpenAmount();

        final String originNumber = creditEntry.getFinantialDocument() != null && invoiceEntry.getFinantialDocument()
                .isClosed() ? creditEntry.getFinantialDocument().getUiDocumentNumber() : "";
        final DebtAccount payorDebtAccount =
                creditEntry.getFinantialDocument() != null && ((Invoice) creditEntry.getFinantialDocument()).isForPayorDebtAccount() ? ((Invoice) creditEntry.getFinantialDocument()).getPayorDebtAccount() : null;
        DebitEntry regulationDebitEntry =
                createBalanceTransferDebit(invoiceEntry.getFinantialEntity(), fromDebtAccount, now, now.toLocalDate(),
                        originNumber, creditEntry.getProduct(), creditOpenAmount, payorDebtAccount, creditEntry.getDescription(),
                        null);

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {

            if (creditEntry.getFinantialDocument().isExportedInLegacyERP() || (creditEntry.getFinantialDocument()
                    .getCloseDate() != null && creditEntry.getFinantialDocument().getCloseDate()
                    .isBefore(ERP_INTEGRATION_START_DATE))) {
                regulationDebitEntry.getFinantialDocument().setExportedInLegacyERP(true);
                regulationDebitEntry.getFinantialDocument().setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
            }

        }

        regulationDebitEntry.getFinantialDocument().closeDocument();
        CreditEntry regulationCreditEntry =
                createBalanceTransferCredit(this.destinyDebtAccount, now, originNumber, creditEntry.getProduct(),
                        creditOpenAmount, payorDebtAccount, creditEntry.getDescription(), creditEntry.getFinantialEntity());

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {

            if (creditEntry.getFinantialDocument().isExportedInLegacyERP() || (creditEntry.getFinantialDocument()
                    .getCloseDate() != null && creditEntry.getFinantialDocument().getCloseDate()
                    .isBefore(ERP_INTEGRATION_START_DATE))) {
                regulationCreditEntry.getFinantialDocument().setExportedInLegacyERP(true);
                regulationCreditEntry.getFinantialDocument().setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
            }

        }

        final SettlementNote settlementNote =
                SettlementNote.create(invoiceEntry.getFinantialEntity(), this.fromDebtAccount, settlementNoteSeries, now, now,
                        null, null);

        if (creditEntry.getFinantialDocument().isPreparing()) {
            creditEntry.getFinantialDocument().closeDocument();
        }

        SettlementEntry.create(regulationDebitEntry, settlementNote, regulationDebitEntry.getOpenAmount(),
                regulationDebitEntry.getDescription(), now, false);
        SettlementEntry.create(creditEntry, settlementNote, creditOpenAmount, creditEntry.getDescription(), now, false);

        settlementNote.markAsUsedInBalanceTransfer();
        settlementNote.closeDocument();
    }

    protected void transferDebitEntries(final DebitNote objectDebitNote) {
        FinantialEntity finantialEntity = objectDebitNote.getFinantialEntity();
        final DocumentNumberSeries settlementNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(), finantialEntity);
        final DateTime now = new DateTime();

        if (objectDebitNote.isPreparing()) {
            anullPreparingDebitNote(objectDebitNote);
        } else if (objectDebitNote.isClosed()) {
            final DebtAccount payorDebtAccount =
                    objectDebitNote.isForPayorDebtAccount() ? objectDebitNote.getPayorDebtAccount() : null;

            final DebitNote destinyDebitNote =
                    DebitNote.create(objectDebitNote.getFinantialEntity(), this.destinyDebtAccount, payorDebtAccount,
                            objectDebitNote.getDocumentNumberSeries(), now, now.toLocalDate(),
                            objectDebitNote.getUiDocumentNumber(), Collections.emptyMap(), null, null);

            final SettlementNote settlementNote =
                    SettlementNote.create(objectDebitNote.getFinantialEntity(), this.fromDebtAccount, settlementNumberSeries, now,
                            now, null, null);
            for (final FinantialDocumentEntry objectEntry : objectDebitNote.getFinantialDocumentEntriesSet()) {
                if (!isPositive(((DebitEntry) objectEntry).getOpenAmount())) {
                    continue;
                }

                final DebitEntry debitEntry = (DebitEntry) objectEntry;

                final BigDecimal openAmount = debitEntry.getOpenAmount();
                final BigDecimal availableCreditAmount = debitEntry.getAvailableAmountWithVatForCredit();

                DebitEntry destinyDebitEntry = null;
                SettlementEntry destinySettlementEntry = null;

                if (!debitEntry.getFinantialDocument().getDocumentNumberSeries().getSeries()
                        .isRegulationSeries() && isGreaterOrEqualThan(availableCreditAmount, openAmount)) {

                    final BigDecimal openAmountWithoutVat = debitEntry.getCurrency().getValueWithScale(
                            TreasuryConstants.divide(openAmount, BigDecimal.ONE.add(rationalVatRate(debitEntry))));
                    final CreditEntry newCreditEntry =
                            debitEntry.createCreditEntry(now, debitEntry.getDescription(), null, null, openAmountWithoutVat, null,
                                    null, Collections.emptyMap());

                    if (newCreditEntry.getFinantialDocument().isPreparing()) {
                        newCreditEntry.getFinantialDocument().closeDocument();
                    }

                    destinySettlementEntry =
                            SettlementEntry.create(debitEntry, settlementNote, openAmount, debitEntry.getDescription(), now,
                                    false);
                    SettlementEntry.create(newCreditEntry, settlementNote, openAmount, newCreditEntry.getDescription(), now,
                            false);
                    destinyDebitEntry = createDestinyDebitEntry(destinyDebitNote, debitEntry);

                } else {

                    final CreditEntry regulationCreditEntry =
                            createBalanceTransferCredit(this.fromDebtAccount, now, objectDebitNote.getUiDocumentNumber(),
                                    debitEntry.getProduct(), openAmount, payorDebtAccount, null, debitEntry.getFinantialEntity());

                    if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {

                        if (objectDebitNote.isExportedInLegacyERP() || objectDebitNote.getCloseDate()
                                .isBefore(ERP_INTEGRATION_START_DATE)) {
                            regulationCreditEntry.getFinantialDocument().setExportedInLegacyERP(true);
                            regulationCreditEntry.getFinantialDocument().setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
                        }

                    }

                    if (regulationCreditEntry.getFinantialDocument().isPreparing()) {
                        regulationCreditEntry.getFinantialDocument().closeDocument();
                    }

                    destinySettlementEntry =
                            SettlementEntry.create(debitEntry, settlementNote, openAmount, debitEntry.getDescription(), now,
                                    false);

                    SettlementEntry.create(regulationCreditEntry, settlementNote, openAmount,
                            regulationCreditEntry.getDescription(), now, false);

                    final DebitEntry regulationDebitEntry =
                            createBalanceTransferDebit(debitEntry.getFinantialEntity(), this.destinyDebtAccount,
                                    debitEntry.getEntryDateTime(), debitEntry.getDueDate(),
                                    regulationCreditEntry.getFinantialDocument().getUiDocumentNumber(), debitEntry.getProduct(),
                                    openAmount, payorDebtAccount, debitEntry.getDescription(), debitEntry.getInterestRate());

                    destinyDebitEntry = regulationDebitEntry;

                    if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {

                        if (objectDebitNote.isExportedInLegacyERP() || objectDebitNote.getCloseDate()
                                .isBefore(ERP_INTEGRATION_START_DATE)) {
                            regulationDebitEntry.getFinantialDocument().setExportedInLegacyERP(true);
                            regulationDebitEntry.getFinantialDocument().setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
                        }

                    }
                    regulationDebitEntry.getFinantialDocument().closeDocument();
                }

                //paymentPlan
                this.debitEntriesConversionMap.put(debitEntry, destinyDebitEntry);
                this.settlementOfDebitEntryMap.put(debitEntry, destinySettlementEntry);
            }

            settlementNote.markAsUsedInBalanceTransfer();
            settlementNote.closeDocument();
        }
    }

    private DebitEntry createDestinyDebitEntry(final DebitNote destinyDebitNote, final DebitEntry debitEntry) {
        final BigDecimal openAmountWithoutVat =
                TreasuryConstants.divide(debitEntry.getOpenAmount(), BigDecimal.ONE.add(rationalVatRate(debitEntry)));

        final DebitEntry newDebitEntry =
                DebitEntry.create(debitEntry.getFinantialEntity(), this.destinyDebtAccount, debitEntry.getTreasuryEvent(),
                        debitEntry.getVat(), openAmountWithoutVat, debitEntry.getDueDate(), debitEntry.getPropertiesMap(),
                        debitEntry.getProduct(), debitEntry.getDescription(), debitEntry.getQuantity(),
                        debitEntry.getInterestRate(), debitEntry.getEntryDateTime(),
                        debitEntry.isAcademicalActBlockingSuspension(), debitEntry.isBlockAcademicActsOnDebt(), destinyDebitNote);

        return newDebitEntry;
    }

    private void anullPreparingDebitNote(final DebitNote objectDebitNote) {
        final DateTime now = new DateTime();
        final DebitNote newDebitNote = DebitNote.create(objectDebitNote.getFinantialEntity(), this.destinyDebtAccount,
                objectDebitNote.getPayorDebtAccount(), objectDebitNote.getDocumentNumberSeries(), now, now.toLocalDate(), "",
                Collections.emptyMap(), null, null);

        for (final FinantialDocumentEntry objectEntry : objectDebitNote.getFinantialDocumentEntriesSet()) {
            final DebitEntry debitEntry = (DebitEntry) objectEntry;
            final BigDecimal unitAmount = debitEntry.getAmount();

            if (!isPositive(unitAmount)) {
                continue;
            }

            if (debitEntry.getTreasuryEvent() != null) {
                debitEntry.annulOnEvent();
            }

            DebitEntry newDebitEntry =
                    DebitEntry.create(debitEntry.getFinantialEntity(), this.destinyDebtAccount, debitEntry.getTreasuryEvent(),
                            debitEntry.getVat(), unitAmount, debitEntry.getDueDate(), debitEntry.getPropertiesMap(),
                            debitEntry.getProduct(), debitEntry.getDescription(), debitEntry.getQuantity(),
                            debitEntry.getInterestRate(), debitEntry.getEntryDateTime(),
                            debitEntry.isAcademicalActBlockingSuspension(), debitEntry.isBlockAcademicActsOnDebt(), newDebitNote);

            debitEntry.getTreasuryExemptionsSet().forEach(treasuryExemption -> {
                TreasuryExemption.create(treasuryExemption.getTreasuryExemptionType(), treasuryExemption.getReason(),
                        treasuryExemption.getNetAmountToExempt(), newDebitEntry);

            });

            //paymentPlan
            debitEntriesConversionMap.put(debitEntry, newDebitEntry);

        }

        objectDebitNote.anullDebitNoteWithCreditNote(treasuryBundle("label.BalanceTransferService.annuled.reason"), false);
    }

    public static LocalizedString getPresentationName() {
        return TreasuryConstants.treasuryBundleI18N("label.StandardBalanceTransferServiceForSAPAndSINGAP.presentationName");
    }

    @Override
    public boolean isAutoTransferInSwitchDebtAccountsEnabled() {
        return false;
    }

    @Deprecated
    // TODO ANIL 2024-01-17 : This is to be discontinued
    private static CreditEntry createBalanceTransferCredit(final DebtAccount debtAccount, final DateTime documentDate,
            final String originNumber, final Product product, final BigDecimal amountWithVat, final DebtAccount payorDebtAccount,
            String entryDescription, FinantialEntity finantialEntity) {

        final FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        final Series regulationSeries = finantialInstitution.getRegulationSeries();
        final DocumentNumberSeries numberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), regulationSeries);
        final Vat transferVat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, documentDate).get();

        if (Strings.isNullOrEmpty(entryDescription)) {
            entryDescription = product.getName().getContent();
        }

        final CreditNote creditNote =
                CreditNote.create(finantialEntity, debtAccount, numberSeries, payorDebtAccount, documentDate, originNumber);

        final BigDecimal amountWithoutVat = Currency.getValueWithScale(TreasuryConstants.divide(amountWithVat,
                TreasuryConstants.divide(transferVat.getTaxRate(), TreasuryConstants.HUNDRED_PERCENT).add(BigDecimal.ONE)));

        CreditEntry entry =
                CreditEntry.create(finantialEntity, creditNote, entryDescription, product, transferVat, amountWithoutVat,
                        documentDate, BigDecimal.ONE);

        if (finantialInstitution.isToCloseCreditNoteWhenCreated()) {
            creditNote.closeDocument();
        }

        return entry;
    }

    @Deprecated
    // TODO ANIL 2024-01-17 : This is to be discontinued
    private static DebitEntry createBalanceTransferDebit(FinantialEntity finantialEntity, final DebtAccount debtAccount,
            final DateTime entryDate, final LocalDate dueDate, final String originNumber, final Product product,
            final BigDecimal amountWithVat, final DebtAccount payorDebtAccount, String entryDescription,
            final InterestRate interestRate) {

        final FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        final Series regulationSeries = finantialInstitution.getRegulationSeries();
        final DocumentNumberSeries numberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), regulationSeries);
        final Vat transferVat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, entryDate).get();

        if (Strings.isNullOrEmpty(entryDescription)) {
            entryDescription = product.getName().getContent();
        }

        final DebitNote debitNote = DebitNote.create(finantialEntity, debtAccount, payorDebtAccount, numberSeries, new DateTime(),
                new DateTime().toLocalDate(), originNumber, Collections.emptyMap(), null, null);

        final BigDecimal amountWithoutVat = Currency.getValueWithScale(TreasuryConstants.divide(amountWithVat,
                TreasuryConstants.divide(transferVat.getTaxRate(), TreasuryConstants.HUNDRED_PERCENT).add(BigDecimal.ONE)));

        return DebitEntry.create(finantialEntity, debtAccount, null, transferVat, amountWithoutVat, dueDate, Maps.newHashMap(),
                product, entryDescription, BigDecimal.ONE, interestRate, entryDate, false, false, debitNote);
    }

}
