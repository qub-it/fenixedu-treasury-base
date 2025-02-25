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

import static org.fenixedu.treasury.services.integration.erp.sap.SAPExporter.ERP_INTEGRATION_START_DATE;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.bennu.signals.BennuSignalsServices;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.reimbursement.ReimbursementProcessStatusType;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentEntry;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentSettlementEntry;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.treasurydebtprocess.ITreasuryDebtProcess;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InstallmentPaymenPlanBean;
import org.fenixedu.treasury.dto.SettlementCreditEntryBean;
import org.fenixedu.treasury.dto.SettlementDebitEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.PaymentEntryBean;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class SettlementNote extends SettlementNote_Base {

    public static Comparator<SettlementNote> COMPARE_BY_PAYMENT_DATE = (s1, s2) -> {
        int c = s1.getPaymentDate().compareTo(s2.getPaymentDate());

        if (c != 0) {
            return c;
        }

        c = s1.getDocumentDate().compareTo(s2.getDocumentDate());

        if (c != 0) {
            return c;
        }

        return s1.getExternalId().compareTo(s2.getExternalId());
    };

    protected SettlementNote() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected SettlementNote(FinantialEntity finantialEntity, DebtAccount debtAccount, DocumentNumberSeries documentNumberSeries,
            DateTime documentDate, DateTime paymentDate, String originDocumentNumber, String finantialTransactionReference) {
        this();
        init(finantialEntity, debtAccount, documentNumberSeries, documentDate, paymentDate, originDocumentNumber,
                finantialTransactionReference);
    }

    protected void init(FinantialEntity finantialEntity, DebtAccount debtAccount, DocumentNumberSeries documentNumberSeries,
            DateTime documentDate, DateTime paymentDate, String originDocumentNumber, String finantialTransactionReference) {

        setFinantialTransactionReference(finantialTransactionReference);
        setOriginDocumentNumber(originDocumentNumber);

        if (paymentDate == null) {
            setPaymentDate(documentDate);
        } else {
            setPaymentDate(paymentDate);
        }

        super.init(finantialEntity, debtAccount, documentNumberSeries, documentDate);

        checkRules();
    }

    @Override
    public boolean isSettlementNote() {
        return true;
    }

    public boolean isReimbursement() {
        return getDocumentNumberSeries().getFinantialDocumentType() == FinantialDocumentType.findForReimbursementNote();
    }

    public BigDecimal checkDiferenceInAmount() {
        BigDecimal result = this.getTotalDebitAmount().subtract(this.getTotalCreditAmount());

        if (this.getAdvancedPaymentCreditNote() != null) {
            result = result.add(this.getAdvancedPaymentCreditNote().getTotalAmount());
        }
        if (TreasuryConstants.isZero(this.getTotalReimbursementAmount())) {
            return this.getTotalPayedAmount().subtract(result);
        } else {
            return this.getTotalReimbursementAmount().add(result);
        }
    }

    @Override
    public void checkRules() {
        super.checkRules();

        if (getPaymentDate().isAfter(getDocumentDate())) {
            throw new TreasuryDomainException("error.SettlementNote.invalid.payment.date.after.document.date");
        }

        if (!getDocumentNumberSeries().getFinantialDocumentType().getType().equals(FinantialDocumentTypeEnum.SETTLEMENT_NOTE)
                && !getDocumentNumberSeries().getFinantialDocumentType().getType()
                        .equals(FinantialDocumentTypeEnum.REIMBURSEMENT_NOTE)) {
            throw new TreasuryDomainException("error.FinantialDocument.finantialDocumentType.invalid");
        }

        if (isClosed() && isReimbursement() && getCurrentReimbursementProcessStatus() == null) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementNote.current.status.invalid");
        }

        if (isClosed()) {
            for (final SettlementEntry settlementEntry : getSettlemetEntriesSet()) {
                if (!settlementEntry.getInvoiceEntry().isCreditNoteEntry()) {
                    continue;
                }

                if (!settlementEntry.getInvoiceEntry().getFinantialDocument().isClosed()) {
                    throw new TreasuryDomainException("error.SettlementNote.settlement.entry.for.credit.entry.not.closed");
                }
            }
        }

        // Ensure the settlement entries do not settle the same invoice entry twice or
        // more
        {
            final Map<InvoiceEntry, LongAdder> map = new HashMap<>();
            getSettlemetEntriesSet().forEach(se -> {
                map.putIfAbsent(se.getInvoiceEntry(), new LongAdder());
                map.get(se.getInvoiceEntry()).increment();
            });

            for (Entry<InvoiceEntry, LongAdder> entry : map.entrySet()) {
                if (entry.getValue().intValue() > 1) {
                    throw new TreasuryDomainException("error.SettlementNote.checkRules.invoiceEntries.not.unique");
                }
            }
        }

        if (!TreasurySettings.getInstance().getCanRegisterPaymentWithMultipleMethods() && getPaymentEntriesSet().size() > 1) {
            throw new TreasuryDomainException("error.SettlementNote.only.one.payment.method.is.supported");
        }

        // Check that the "not reimbursement settlement note" which has credit entries
        // also has debit entries
        {
            boolean hasCreditEntries = getSettlemetEntriesSet().stream().anyMatch(se -> se.getInvoiceEntry().isCreditNoteEntry());
            boolean hasDebitEntries = getSettlemetEntriesSet().stream().anyMatch(se -> se.getInvoiceEntry().isDebitNoteEntry());

            if (!isReimbursement() && hasCreditEntries && !hasDebitEntries) {
                throw new TreasuryDomainException(
                        "error.SettlementNote.settlementNote.not.reimbursement.but.has.only.credits.settled");
            }
        }
    }

    public void markAsUsedInBalanceTransfer() {
        setUsedInBalanceTransfer(true);
    }

    @Atomic
    public void edit(final FinantialDocumentType finantialDocumentType, final DebtAccount debtAccount,
            final DocumentNumberSeries documentNumberSeries, final Currency currency, final java.lang.String documentNumber,
            final org.joda.time.DateTime documentDate, final DateTime paymentDate, final java.lang.String originDocumentNumber,
            final org.fenixedu.treasury.domain.document.FinantialDocumentStateType state) {
        setFinantialDocumentType(finantialDocumentType);
        setDebtAccount(debtAccount);
        setDocumentNumberSeries(documentNumberSeries);
        setCurrency(currency);
        setDocumentNumber(documentNumber);
        setDocumentDate(documentDate);
        setDocumentDueDate(documentDate.toLocalDate());
        setOriginDocumentNumber(originDocumentNumber);
        setState(state);
        setPaymentDate(paymentDate);
        checkRules();
    }

    @Atomic
    public void updateSettlementNote(java.lang.String originDocumentNumber, String documentObservations,
            String documentTermsAndConditions) {
        setOriginDocumentNumber(originDocumentNumber);
        setDocumentObservations(documentObservations);
        setDocumentTermsAndConditions(documentTermsAndConditions);

        checkRules();
    }

    @Override
    public boolean isDeletable() {
        // We can only "delete" a settlement note if is in "Preparing"
        if (this.isPreparing()) {
            // if is preparing, the AdvancedPaymentCreditNote if exists, must be deletable
            if (getAdvancedPaymentCreditNote() != null) {
                return getAdvancedPaymentCreditNote().isDeletable();
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean isAdvancePaymentSetByUser() {
        return getAdvancePaymentSetByUser();
    }

    public boolean isReimbursementPending() {
        if (!isReimbursement()) {
            return false;
        }

        if (getCurrentReimbursementProcessStatus() == null) {
            return false;
        }

        return getCurrentReimbursementProcessStatus().isInitialStatus();
    }

    public boolean isReimbursementConcluded() {
        if (!isReimbursement()) {
            return false;
        }

        final ReimbursementProcessStatusType currentStatus = getCurrentReimbursementProcessStatus();
        if (currentStatus == null) {
            return false;
        }

        return currentStatus.isFinalStatus() && !currentStatus.isRejectedStatus();
    }

    public boolean isReimbursementRejected() {
        if (!isReimbursement()) {
            return false;
        }

        final ReimbursementProcessStatusType currentStatus = getCurrentReimbursementProcessStatus();
        if (currentStatus == null) {
            return false;
        }

        return currentStatus.isFinalStatus() && currentStatus.isRejectedStatus();
    }

    public boolean isUsedInBalanceTransfer() {
        return getUsedInBalanceTransfer();
    }

    @Override
    @Atomic
    public void delete(boolean deleteEntries) {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.SettlementNote.cannot.delete");
        }

        for (PaymentEntry paymentEntry : getPaymentEntriesSet()) {
            this.removePaymentEntries(paymentEntry);
            if (deleteEntries) {
                paymentEntry.delete();
            } else {
                paymentEntry.setSettlementNote(null);
            }
        }
        for (ReimbursementEntry entry : getReimbursementEntriesSet()) {
            this.removeReimbursementEntries(entry);
            if (deleteEntries) {
                entry.delete();
            } else {
                entry.setSettlementNote(null);
            }
        }

        if (getAdvancedPaymentCreditNote() != null) {
            getAdvancedPaymentCreditNote().delete(true);
        }
        super.delete(deleteEntries);
    }

    @Atomic
    public void processSettlementNoteCreation(SettlementNoteBean bean) {
        processDebitEntries(bean);
        processCreditEntries(bean);

        if (bean.isReimbursementNote()) {
            processReimbursementEntries(bean);
        } else {
            processPaymentEntries(bean);
        }

        processAdvancePayments(bean);
        setAdvancePaymentSetByUser(bean.isAdvancePayment());

        if (isReimbursement()) {
            if (getSettlemetEntries().anyMatch(se -> !se.getInvoiceEntry().isCreditNoteEntry())) {
                throw new TreasuryDomainException("error.SettlementNote.reimbursement.invoice.entry.not.from.credit.note");
            }
        }
    }

    private void processAdvancePayments(SettlementNoteBean bean) {
        if (bean.isReimbursementNote()) {
            return;
        }

        if (!bean.isAdvancePayment()) {
            return;
        }

        final BigDecimal debitSum =
                bean.isReimbursementNote() ? bean.getDebtAmountWithVat().negate() : bean.getDebtAmountWithVat();
        final BigDecimal paymentSum = bean.getPaymentAmount();

        final BigDecimal availableAmount = paymentSum.subtract(debitSum);

        if (!TreasuryConstants.isPositive(availableAmount)) {
            return;
        }

        if (bean.getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification()) {
            ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
            // Instead of creating an advanced payment credit note, 
            // create a debit note with the excess payment and settle it
            final String comments = String.format("%s [%s]",
                    treasuryBundleI18N("label.SettlementNote.excessPayment").getContent(services.defaultLocale()),
                    getPaymentDate().toString(TreasuryConstants.DATE_FORMAT));

            createExcessPaymentDebitNote(bean, availableAmount, comments, getUiDocumentNumber());
        } else {
            final String comments = String.format("%s [%s]",
                    treasuryBundleI18N("label.SettlementNote.advancedpayment").getContent(TreasuryConstants.DEFAULT_LANGUAGE),
                    getPaymentDate().toString(TreasuryConstants.DATE_FORMAT));

            createAdvancedPaymentCreditNote(availableAmount, comments, getExternalId());
        }
    }

    private void processReimbursementEntries(SettlementNoteBean bean) {
        for (PaymentEntryBean paymentEntryBean : bean.getPaymentEntries()) {
            ReimbursementEntry.create(this, paymentEntryBean.getPaymentMethod(), paymentEntryBean.getPaymentAmount(),
                    paymentEntryBean.getPaymentMethodId());
        }
    }

    private void processPaymentEntries(SettlementNoteBean bean) {
        for (PaymentEntryBean paymentEntryBean : bean.getPaymentEntries()) {
            PaymentEntry.create(paymentEntryBean.getPaymentMethod(), this, paymentEntryBean.getPaymentAmount(),
                    paymentEntryBean.getPaymentMethodId(), Maps.newHashMap());
        }
    }

    private void processCreditEntries(SettlementNoteBean bean) {
        boolean splitCreditEntriesWithSettledAmount =
                getDebtAccount().getFinantialInstitution().getSplitCreditEntriesWithSettledAmount();

        for (SettlementCreditEntryBean creditEntryBean : bean.getCreditEntries()) {
            if (creditEntryBean.isIncluded()) {
                CreditEntry creditEntry = creditEntryBean.getCreditEntry();

                final BigDecimal creditAmountWithVat = creditEntryBean.getSettledAmount();

                if (bean.isReimbursementNote()) {
                    if (ReimbursementUtils.isCreditNoteForReimbursementMustBeClosedWithDebitNoteAndCreatedNew(creditEntry)) {
                        creditEntry = ReimbursementUtils.closeWithDebitNoteAndCreateNewCreditNoteForReimbursement(creditEntry,
                                creditAmountWithVat);
                    }
                }

                if (creditEntry.getFinantialDocument().isPreparing()) {
                    if (splitCreditEntriesWithSettledAmount
                            && TreasuryConstants.isLessThan(creditAmountWithVat, creditEntry.getOpenAmount())) {
                        creditEntry.splitCreditEntry(creditEntry.getOpenAmount().subtract(creditAmountWithVat));
                    }

                    creditEntry.getFinantialDocument().closeDocument();
                }

                final String creditDescription = creditEntryBean.getCreditEntry().getDescription();
                SettlementEntry.create(creditEntry, creditAmountWithVat, creditDescription, this, bean.getDate());
            }
        }
    }

    private void processDebitEntries(SettlementNoteBean bean) {
        boolean splitDebitEntriesWithSettledAmount =
                bean.getDebtAccount().getFinantialInstitution().getSplitDebitEntriesWithSettledAmount();

        BigDecimal paymentEntriesAmount =
                bean.getPaymentEntries().stream().map(p -> p.getPaymentAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditsAmount = bean.getCreditEntries().stream().filter(c -> c.isIncluded())
                .map(c -> c.getCreditAmountWithVat()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal restAmountToUse = paymentEntriesAmount.add(creditsAmount);

        List<DebitEntry> untiedDebitEntries = new ArrayList<DebitEntry>();
        for (SettlementDebitEntryBean debitEntryBean : bean.getDebitEntriesByType(SettlementDebitEntryBean.class)) {
            if (!debitEntryBean.isIncluded()) {
                continue;
            }

            if (!TreasuryConstants.isPositive(restAmountToUse)) {
                debitEntryBean.setSettledAmount(BigDecimal.ZERO);
                debitEntryBean.setIncluded(false);
                continue;
            }

            DebitEntry debitEntry = debitEntryBean.getDebitEntry();

            if (splitDebitEntriesWithSettledAmount
                    && (debitEntry.getFinantialDocument() == null || debitEntry.getFinantialDocument().isPreparing())
                    && TreasuryConstants.isLessThan(debitEntryBean.getSettledAmount(), debitEntry.getOpenAmount())) {
                debitEntry.splitDebitEntry(debitEntry.getOpenAmount().subtract(debitEntryBean.getSettledAmount()),
                        "partial payment (system)");
            } else if (splitDebitEntriesWithSettledAmount && debitEntry.getFinantialDocument() != null
                    && debitEntry.getFinantialDocument().isPreparing()) {
                // ANIL 2024-08-07 (#qubIT-Fenix-5710)
                //
                // Check if there are other debit entries, which are not being settled 
                // but need to be separated from this current settled debit note
                // 
                // 1) Collect the debit entries not being settled and are not zero in total amount
                // 2) If the collection is not empty, create an identical debit note
                // 3) Transfer the not settled debit entries to the new debit note

                Set<DebitEntry> settlingDebitEntriesTotallySet =
                        bean.getDebitEntriesByType(SettlementDebitEntryBean.class).stream() //
                                .filter(d -> d.isIncluded()) //
                                .filter(d -> TreasuryConstants.isEqual(d.getSettledAmount(), d.getDebitEntry().getOpenAmount())) //
                                .map(d -> d.getDebitEntry()) //
                                .collect(Collectors.toSet());

                DebitNote settlingDebitNote = debitEntry.getDebitNote();

                List<DebitEntry> notSettlingDebitEntriesSet = settlingDebitNote.getDebitEntriesSet().stream() //
                        .filter(d -> d != debitEntry) //
                        .filter(d -> !settlingDebitEntriesTotallySet.contains(d)) //
                        .filter(d -> TreasuryConstants.isPositive(d.getTotalAmount())) //
                        .collect(Collectors.toList());

                if (!notSettlingDebitEntriesSet.isEmpty()) {
                    DebitNote newDebitNote = DebitNote.create(settlingDebitNote.getFinantialEntity(), this.getDebtAccount(),
                            settlingDebitNote.getPayorDebtAccount(), settlingDebitNote.getDocumentNumberSeries(),
                            settlingDebitNote.getDocumentDate(), settlingDebitNote.getDocumentDueDate(),
                            settlingDebitNote.getOriginDocumentNumber(), settlingDebitNote.getPropertiesMap(),
                            settlingDebitNote.getDocumentObservations(), settlingDebitNote.getDocumentTermsAndConditions());

                    newDebitNote.setCloseDate(settlingDebitNote.getCloseDate());
                    newDebitNote.setLegacyERPCertificateDocumentReference(
                            settlingDebitNote.getLegacyERPCertificateDocumentReference());

                    newDebitNote.addDebitNoteEntries(notSettlingDebitEntriesSet);
                }
            }

            if (debitEntry.getFinantialDocument() == null) {
                untiedDebitEntries.add(debitEntry);
            } else if (!debitEntry.getFinantialDocument().isClosed()) {
                debitEntry.getFinantialDocument().closeDocument();
            }

            if (TreasuryConstants.isLessThan(restAmountToUse, debitEntryBean.getSettledAmount())) {
                debitEntryBean.setSettledAmount(restAmountToUse);
            }

            restAmountToUse = restAmountToUse.subtract(debitEntryBean.getSettledAmount());

            SettlementEntry settlementEntry =
                    SettlementEntry.create(debitEntry, debitEntryBean.getSettledAmount(), this, bean.getDate());

            InstallmentSettlementEntry.settleInstallmentEntriesOfDebitEntry(settlementEntry);
        }

        for (InstallmentPaymenPlanBean installmentPaymenPlanBean : bean.getDebitEntriesByType(InstallmentPaymenPlanBean.class)) {
            if (!installmentPaymenPlanBean.isIncluded()) {
                continue;
            }

            if (!TreasuryConstants.isPositive(restAmountToUse)) {
                installmentPaymenPlanBean.setSettledAmount(BigDecimal.ZERO);
                installmentPaymenPlanBean.setIncluded(false);
                continue;
            }

            if (TreasuryConstants.isLessThan(restAmountToUse, installmentPaymenPlanBean.getSettledAmount())) {
                installmentPaymenPlanBean.setSettledAmount(restAmountToUse);
            }

            restAmountToUse = restAmountToUse.subtract(installmentPaymenPlanBean.getSettledAmount());

            BigDecimal restToPay = installmentPaymenPlanBean.getSettledAmount();

            for (InstallmentEntry installmentEntry : installmentPaymenPlanBean.getInstallment()
                    .getSortedOpenInstallmentEntries()) {
                if (TreasuryConstants.isZero(restToPay)) {
                    break;
                }

                BigDecimal debtAmount =
                        restToPay.compareTo(installmentEntry.getOpenAmount()) > 0 ? installmentEntry.getOpenAmount() : restToPay;
                restToPay = restToPay.subtract(debtAmount);

                if (!TreasuryConstants.isPositive(debtAmount)) {
                    continue;
                }

                if (installmentEntry.getDebitEntry().getFinantialDocument() == null) {
                    untiedDebitEntries.add(installmentEntry.getDebitEntry());
                } else if (!installmentEntry.getDebitEntry().getFinantialDocument().isClosed()) {
                    installmentEntry.getDebitEntry().getFinantialDocument().closeDocument();
                }

                SettlementEntry settlementEntry = getSettlementEntryByDebitEntry(installmentEntry.getDebitEntry());
                if (settlementEntry == null) {
                    settlementEntry = SettlementEntry.create(installmentEntry.getDebitEntry(), debtAmount, this, bean.getDate());
                } else {
                    settlementEntry.setAmount(settlementEntry.getAmount().add(debtAmount));
                }

                InstallmentSettlementEntry.create(installmentEntry, settlementEntry, debtAmount);
            }

            installmentPaymenPlanBean.getInstallment().getPaymentPlan().tryClosePaymentPlanByPaidOff();
        }

        if (untiedDebitEntries.size() != 0) {
            DocumentNumberSeries debitNoteSeries = DocumentNumberSeries
                    .findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), bean.getFinantialEntity());

            DebitNote debitNote = DebitNote.create(bean.getFinantialEntity(), bean.getDebtAccount(), null, debitNoteSeries,
                    bean.getDate(), bean.getDate().toLocalDate(), null, Collections.emptyMap(), null, null);

            debitNote.addDebitNoteEntries(untiedDebitEntries);
            debitNote.closeDocument();
        }
    }

    public SettlementEntry getSettlementEntryByDebitEntry(DebitEntry debitEntry) {
        return getSettlemetEntriesSet().stream().filter(se -> se.getInvoiceEntry().equals(debitEntry)).findFirst().orElse(null);
    }

    public Stream<SettlementEntry> getSettlemetEntries() {
        return this.getFinantialDocumentEntriesSet().stream().map(SettlementEntry.class::cast);
    }

    public Set<SettlementEntry> getSettlemetEntriesSet() {
        return getSettlemetEntries().collect(Collectors.toSet());
    }

    @Override
    public Set<FinantialDocument> findRelatedDocuments(Set<FinantialDocument> documentsBaseList,
            Boolean includeAnulledDocuments) {

        documentsBaseList.add(this);

        for (SettlementEntry entry : getSettlemetEntriesSet()) {
            if (entry.getInvoiceEntry() != null && entry.getInvoiceEntry().getFinantialDocument() != null) {
                if (includeAnulledDocuments == true || this.isAnnulled() == false) {
                    if (documentsBaseList.contains(entry.getInvoiceEntry().getFinantialDocument()) == false) {
                        documentsBaseList.addAll(entry.getInvoiceEntry().getFinantialDocument()
                                .findRelatedDocuments(documentsBaseList, includeAnulledDocuments));
                    }
                }
            }
        }
        return documentsBaseList;

    }

    private void _anullDocument(String anulledReason, boolean markDocumentToExport) {
        if (this.isPreparing()) {
            this.delete(true);
        } else if (this.isClosed()) {
            if (isExportedInLegacyERP()) {
                throw new TreasuryDomainException("error.SettlementNote.cannot.anull.settlement.exported.in.legacy.erp");
            }

            if (getAdvancedPaymentCreditNote() != null && getAdvancedPaymentCreditNote().hasValidSettlementEntries()) {
                throw new TreasuryDomainException("error.SettlementNote.cannot.anull.settlement.due.to.advanced.payment.settled");
            }

            if (getExcessPaymentDebitNote() != null
                    && getExcessPaymentDebitNote().getCreditNoteSet().iterator().next().hasValidSettlementEntries()) {
                throw new TreasuryDomainException("error.SettlementNote.cannot.anull.settlement.due.to.advanced.payment.settled");
            }

            if (isUsedInBalanceTransfer()) {
                throw new TreasuryDomainException("error.SettlementNote.cannot.anull.settlement.due.to.balance.transfer");
            }

            setState(FinantialDocumentStateType.ANNULED);
            setAnnulledReason(anulledReason);
            setAnnullmentDate(new DateTime());

            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
            setAnnullmentResponsible(!Strings.isNullOrEmpty(loggedUsername) ? loggedUsername : "unknown");

            // Settlement note can never free entries
            if (markDocumentToExport) {
                this.markDocumentToExport();
            }

            if (this.getAdvancedPaymentCreditNote() != null) {
                this.getAdvancedPaymentCreditNote().anullDocument(anulledReason);
            }

            if (this.getExcessPaymentDebitNote() != null) {
                // Settle excess debit and credit
                DateTime now = new DateTime();

                SettlementNote excessCloseSettlementNote = create(getFinantialEntity(), getDebtAccount(),
                        getDocumentNumberSeries(), now, now, getUiDocumentNumber(), null);
                DebitEntry excessDebitEntry = this.getExcessPaymentDebitNote().getDebitEntriesSet().iterator().next();
                CreditEntry excessCreditEntry = this.getExcessPaymentDebitNote().getCreditNoteSet().iterator().next()
                        .getCreditEntriesSet().iterator().next();

                SettlementEntry.create(excessDebitEntry, excessDebitEntry.getTotalAmount(), excessCloseSettlementNote, now);
                SettlementEntry.create(excessCreditEntry, excessCreditEntry.getTotalAmount(), excessCreditEntry.getDescription(),
                        excessCloseSettlementNote, now);

                excessCloseSettlementNote.closeDocument();
            }

            checkRules();

            TreasuryPlataformDependentServicesFactory.implementation().annulCertifiedDocument(this);
        } else {
            throw new TreasuryDomainException(treasuryBundle("error.FinantialDocumentState.invalid.state.change.request"));
        }
    }

    @Atomic
    /*
     * This method check if there is some debt process blocking the annullment
     */
    public void anullDocument(String anulledReason, boolean markDocumentToExport) {
        if (TreasuryDebtProcessMainService.isFinantialDocumentAnnullmentActionBlocked(this)) {
            throw new TreasuryDomainException("error.SettlementNote.cannot.annull.due.to.existing.active.debt.process");
        }

        _anullDocument(anulledReason, markDocumentToExport);
    }

    /* Does not verify if the settlement note is blocking due to some debt process */
    public void anullDocumentFromDebtProcess(String anulledReason, boolean markDocumentToExport,
            ITreasuryDebtProcess debtProcess) {
        _anullDocument(anulledReason, markDocumentToExport);
    }

    public BigDecimal getTotalDebitAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (SettlementEntry entry : this.getSettlemetEntriesSet()) {
            if (entry.getInvoiceEntry().isDebitNoteEntry()) {
                total = total.add(entry.getTotalAmount());
            }
        }
        return total;
    }

    @Override
    public void closeDocument(boolean markDocumentToExport) {

        // Validate the settlement entries can be used, since multiple entries to the
        // same settlement Note
        for (SettlementEntry settlementEntry : getSettlemetEntriesSet()) {
            if (TreasuryConstants.isGreaterThan(settlementEntry.getAmount(), settlementEntry.getInvoiceEntry().getOpenAmount())) {
                throw new TreasuryDomainException("error.SettlementNote.invalid.settlement.entry.amount.for.invoice.entry");
            }
        }

        if (!TreasuryConstants.isZero(checkDiferenceInAmount())) {
            throw new TreasuryDomainException("error.SettlementNote.invalid.amounts.in.settlement.note");
        }

        if (getReferencedCustomers().size() > 1) {
            throw new TreasuryDomainException("error.SettlementNote.referencedCustomers.only.one.allowed");
        }

        if (this.getAdvancedPaymentCreditNote() != null) {
            this.getAdvancedPaymentCreditNote().closeDocument();
        }

        if (isReimbursement()) {
            processReimbursementStateChange(ReimbursementProcessStatusType.findUniqueByInitialStatus().get(),
                    String.valueOf(getDocumentDate().getYear()), new DateTime());
        }

        super.closeDocument(markDocumentToExport);

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            // Mark this settlement note if there is at least one invoice exported in legacy
            // ERP
            boolean atLeastOneInvoiceEntryExportedInLegacyERP = getSettlemetEntries()
                    .filter(s -> s.getInvoiceEntry().getFinantialDocument().isExportedInLegacyERP()).count() > 0;

            if (atLeastOneInvoiceEntryExportedInLegacyERP) {
                if (!isExportedInLegacyERP()) {
                    setExportedInLegacyERP(true);
                    setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
                }

                getSettlemetEntries().forEach(s -> {
                    if (s.getCloseDate() == null || !s.getCloseDate().isBefore(ERP_INTEGRATION_START_DATE.minusSeconds(1))) {
                        s.setCloseDate(ERP_INTEGRATION_START_DATE.minusSeconds(1));
                    }
                });

                if (getAdvancedPaymentCreditNote() != null && !getAdvancedPaymentCreditNote().isExportedInLegacyERP()) {
                    getAdvancedPaymentCreditNote().setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
                    getAdvancedPaymentCreditNote().setExportedInLegacyERP(true);
                }
            }
        }

        checkRules();

        TreasuryPlataformDependentServicesFactory.implementation().certifyDocument(this);

        BennuSignalsServices.emitSignalForSettlement(this);
    }

    @Atomic
    @Deprecated
    public void processReimbursementStateChange(final ReimbursementProcessStatusType reimbursementStatus,
            final String exerciseYear, final DateTime reimbursementStatusDate) {

        if (reimbursementStatus == null) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementStatus");
        }

        if (!isReimbursement()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.settlementNote");
        }

        if (!isClosed() && !reimbursementStatus.isInitialStatus()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementNote.state");
        }

        if (!reimbursementStatus.isInitialStatus() && getCurrentReimbursementProcessStatus() == null) {
            throw new TreasuryDomainException("error.SettlementNote.currentReimbursementProcessStatus.invalid");
        }

        if (getCurrentReimbursementProcessStatus() != null
                && !reimbursementStatus.isAfter(getCurrentReimbursementProcessStatus())) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementNote.next.status.invalid");
        }

        if (getCurrentReimbursementProcessStatus() != null && getCurrentReimbursementProcessStatus().isFinalStatus()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementNote.current.status.is.final");
        }

        setCurrentReimbursementProcessStatus(reimbursementStatus);

        if (getCurrentReimbursementProcessStatus() == null) {
            throw new TreasuryDomainException("error.SettlementNote.currentReimbursementProcessStatus.invalid");
        }
    }

    public BigDecimal getTotalCreditAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (SettlementEntry entry : this.getSettlemetEntriesSet()) {
            if (entry.getInvoiceEntry().isCreditNoteEntry()) {
                total = total.add(entry.getTotalAmount());
            }
        }
        return total;
    }

    public BigDecimal getTotalPayedAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (PaymentEntry entry : this.getPaymentEntriesSet()) {
            total = total.add(entry.getPayedAmount());
        }
        return total;
    }

    public BigDecimal getTotalReimbursementAmount() {
        BigDecimal total = BigDecimal.ZERO;
        for (ReimbursementEntry reimbursementEntry : getReimbursementEntriesSet()) {
            total = total.add(reimbursementEntry.getReimbursedAmount());
        }
        return total;
    }

    @Override
    public BigDecimal getTotalAmount() {
        return this.getTotalDebitAmount().subtract(this.getTotalDebitAmount());
    }

    @Override
    public BigDecimal getTotalNetAmount() {
        throw new TreasuryDomainException("error.SettlementNote.totalNetAmount.not.available");
    }

    private void createExcessPaymentDebitNote(SettlementNoteBean bean, BigDecimal availableAmount, String comments,
            String originDocumentNumber) {
        FinantialInstitution finantialInstitution = getDebtAccount().getFinantialInstitution();

        if (getReferencedCustomers().size() > 1) {
            throw new TreasuryDomainException("error.SettlementNote.referencedCustomers.only.one.allowed");
        }

        DebtAccount payorDebtAccount = null;
        if (!getReferencedCustomers().isEmpty()) {
            final Customer payorCustomer = getReferencedCustomers().iterator().next();
            if (DebtAccount.findUnique(this.getDebtAccount().getFinantialInstitution(), payorCustomer).isPresent()) {
                if (DebtAccount.findUnique(this.getDebtAccount().getFinantialInstitution(), payorCustomer)
                        .get() != getDebtAccount()) {
                    payorDebtAccount =
                            DebtAccount.findUnique(this.getDebtAccount().getFinantialInstitution(), payorCustomer).get();
                }
            }
        }

        // TODO: Resolve the issue of converting the VAT
        // Find the highest vat amount settled
//        InvoiceEntry invoiceEntry = getSettlemetEntries().map(se -> se.getInvoiceEntry())
//                .sorted((v1, v2) -> -1 * v1.getVat().getTaxRate().compareTo(v2.getVat().getTaxRate())).findFirst().get();
//
//        BigDecimal amount = Currency
//                .getValueWithScale(TreasuryConstants.divide(availableAmount, BigDecimal.ONE.add(rationalVatRate(invoiceEntry))));
        BigDecimal amount = availableAmount;

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), this.getDocumentNumberSeries().getSeries());
        DateTime now = new DateTime();
        DebitNote debitNote = DebitNote.create(getFinantialEntity(), getDebtAccount(), payorDebtAccount, documentNumberSeries,
                now, now.toLocalDate(), originDocumentNumber, Collections.emptyMap(), null, null);

        Product advancePaymentProduct = TreasurySettings.getInstance().getAdvancePaymentProduct();
        Vat vat = Vat.findActiveUnique(advancePaymentProduct.getVatType(), finantialInstitution, now).get();
        DebitEntry debitEntry = DebitEntry.create(getFinantialEntity(), getDebtAccount(), null, vat, amount, now.toLocalDate(),
                new HashMap<>(), advancePaymentProduct, comments, BigDecimal.ONE, null, now, false, false, debitNote);

        if (!TreasuryConstants.isEqual(debitEntry.getTotalAmount(), availableAmount)) {
            throw new RuntimeException(
                    "error.SettlementNote.createExcessPaymentDebitNote.debitEntry.totalAmount.not.equal.to.availableAmount");
        }

        debitNote.closeDocument();
        setExcessPaymentDebitNote(debitNote);

        SettlementEntry.create(debitEntry, availableAmount, this, bean.getDate());
    }

    public void createAdvancedPaymentCreditNote(BigDecimal availableAmount, String comments, String originDocumentNumber) {
        // Create the CreditNote for this amount and
        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), this.getDocumentNumberSeries().getSeries());

        if (getReferencedCustomers().size() > 1) {
            throw new TreasuryDomainException("error.SettlementNote.referencedCustomers.only.one.allowed");
        }

        DebtAccount payorDebtAccount = null;
        if (!getReferencedCustomers().isEmpty()) {
            final Customer payorCustomer = getReferencedCustomers().iterator().next();
            if (DebtAccount.findUnique(this.getDebtAccount().getFinantialInstitution(), payorCustomer).isPresent()) {
                if (DebtAccount.findUnique(this.getDebtAccount().getFinantialInstitution(), payorCustomer)
                        .get() != getDebtAccount()) {
                    payorDebtAccount =
                            DebtAccount.findUnique(this.getDebtAccount().getFinantialInstitution(), payorCustomer).get();
                }
            }
        }

        AdvancedPaymentCreditNote creditNote = AdvancedPaymentCreditNote.createCreditNoteForAdvancedPayment(getFinantialEntity(),
                documentNumberSeries, this.getDebtAccount(), availableAmount, this.getDocumentDate(), comments,
                originDocumentNumber, payorDebtAccount);

        this.setAdvancedPaymentCreditNote(creditNote);
    }

    public boolean hasAdvancedPayment() {
        return getAdvancedPaymentCreditNote() != null;
    }

    @Override
    protected boolean isDocumentEmpty() {
        if (this.getAdvancedPaymentCreditNote() != null) {
            return this.getAdvancedPaymentCreditNote().isDocumentEmpty() && this.getFinantialDocumentEntriesSet().isEmpty();
        }
        return this.getFinantialDocumentEntriesSet().isEmpty();
    }

    public Set<Customer> getReferencedCustomers() {
        final Set<Customer> result = Sets.newHashSet();

        for (final SettlementEntry settlementEntry : getSettlemetEntriesSet()) {
            final Invoice invoice = (Invoice) settlementEntry.getInvoiceEntry().getFinantialDocument();

            if (invoice.isForPayorDebtAccount()) {
                result.add(invoice.getPayorDebtAccount().getCustomer());
            } else {
                result.add(invoice.getDebtAccount().getCustomer());
            }
        }

        if (getAdvancedPaymentCreditNote() != null) {
            if (getAdvancedPaymentCreditNote().isForPayorDebtAccount()) {
                result.add(getAdvancedPaymentCreditNote().getPayorDebtAccount().getCustomer());
            } else {
                result.add(getAdvancedPaymentCreditNote().getDebtAccount().getCustomer());
            }
        }

        return result;
    }

    @Override
    public Comparator<? extends FinantialDocumentEntry> getFinantialDocumentEntriesOrderComparator() {
        return SettlementEntry.COMPARATOR_BY_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION;
    }

    @Override
    public List<? extends FinantialDocumentEntry> getFinantialDocumentEntriesOrderedByTuitionInstallmentOrderAndDescription() {
        final List<SettlementEntry> result = new ArrayList<>();

        getFinantialDocumentEntriesSet().stream().map(SettlementEntry.class::cast).collect(Collectors.toCollection(() -> result));
        Collections.sort(result, SettlementEntry.COMPARATOR_BY_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION);

        if (result.size() != getFinantialDocumentEntriesSet().size()) {
            throw new RuntimeException("error");
        }

        return result;
    }

    public void updateOverrideCertificationDateWithCloseDate(boolean overrideCertificationDateWithCloseDate, DateTime closeDate) {
        super.updateOverrideCertificationDateWithCloseDate(overrideCertificationDateWithCloseDate, closeDate);

        for (SettlementEntry settlementEntry : getSettlemetEntriesSet()) {
            settlementEntry.setCloseDate(closeDate);
        }
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static SettlementNote create(FinantialEntity finantialEntity, DebtAccount debtAccount,
            DocumentNumberSeries documentNumberSeries, DateTime documentDate, DateTime paymentDate, String originDocumentNumber,
            String finantialTransactionReference) {

        if (Boolean.TRUE.equals(debtAccount.getFinantialInstitution().getSeriesByFinantialEntity())) {
            if (documentNumberSeries.getSeries().getFinantialEntity() != finantialEntity) {
                throw new TreasuryDomainException("error.SettlementNote.documentNumberSeries.finantialEntity.mismatch");
            }
        } else {
            if (documentNumberSeries.getSeries().getFinantialInstitution() != debtAccount.getFinantialInstitution()) {
                throw new TreasuryDomainException("error.SettlementNote.documentNumberSeries.finantialInstitution.mismatch");
            }

            if (documentNumberSeries.getSeries().getFinantialEntity() != null) {
                throw new TreasuryDomainException("error.SettlementNote.documentNumberSeries.finantialInstitution.mismatch");
            }
        }

        SettlementNote settlementNote = new SettlementNote(finantialEntity, debtAccount, documentNumberSeries, documentDate,
                paymentDate, originDocumentNumber, finantialTransactionReference);

        return settlementNote;
    }

    // ANIL 2024-08-02
    //
    // Add this method factory to not validate the document number series against the finantial entity
    // or finantial institution
    public static SettlementNote createForImportation(FinantialEntity finantialEntity, DebtAccount debtAccount,
            DocumentNumberSeries documentNumberSeries, DateTime documentDate, DateTime paymentDate, String originDocumentNumber,
            String finantialTransactionReference) {
        SettlementNote settlementNote = new SettlementNote(finantialEntity, debtAccount, documentNumberSeries, documentDate,
                paymentDate, originDocumentNumber, finantialTransactionReference);

        return settlementNote;
    }

    @Atomic
    public static SettlementNote createSettlementNote(SettlementNoteBean bean) {
        DateTime documentDate = new DateTime();
        SettlementNoteBean copy = SettlementNoteBean.copyForSettlementNoteCreation(bean);

        SettlementNote settlementNote = SettlementNote.create(copy.getFinantialEntity(), copy.getDebtAccount(),
                copy.getDocNumSeries(), documentDate, copy.getDate(), copy.getOriginDocumentNumber(),
                !Strings.isNullOrEmpty(copy.getFinantialTransactionReference()) ? copy.getFinantialTransactionReferenceYear()
                        + "/" + copy.getFinantialTransactionReference() : "");

        for (ISettlementInvoiceEntryBean virtualbean : copy.getVirtualDebitEntries()) {
            if (virtualbean.isIncluded() && virtualbean.getVirtualPaymentEntryHandler() != null) {
                virtualbean.getVirtualPaymentEntryHandler().execute(copy, virtualbean);

            }
        }

        settlementNote.processSettlementNoteCreation(copy);
        settlementNote.closeDocument();

        if (settlementNote.getExcessPaymentDebitNote() != null) {
            settlementNote.getExcessPaymentDebitNote().setOriginDocumentNumber(settlementNote.getUiDocumentNumber());
            String comments = treasuryBundleI18N("label.SettlementNote.excessPayment")
                    .getContent(TreasuryPlataformDependentServicesFactory.implementation().defaultLocale());
            settlementNote.getExcessPaymentDebitNote().anullDebitNoteWithCreditNote(comments, true);

            CreditNote excessCreditNote = settlementNote.getExcessPaymentDebitNote().getCreditNoteSet().iterator().next();

            if (excessCreditNote.isPreparing()) {
                excessCreditNote.closeDocument();
            }
        }

        return settlementNote;
    }

    public static Stream<SettlementNote> findAll() {
        return FenixFramework.getDomainRoot().getFinantialDocumentsSet().stream().filter(x -> x instanceof SettlementNote)
                .map(SettlementNote.class::cast);
    }

    public static Stream<SettlementNote> findByFinantialDocumentType(final FinantialDocumentType finantialDocumentType) {
        return finantialDocumentType.getFinantialDocumentsSet().stream().filter(x -> x instanceof SettlementNote)
                .map(SettlementNote.class::cast);
    }

    public static Stream<SettlementNote> findByDebtAccount(final DebtAccount debtAccount) {
        return debtAccount.getFinantialDocumentsSet().stream().filter(x -> x instanceof SettlementNote)
                .map(SettlementNote.class::cast);
    }

    public static Stream<SettlementNote> findByDocumentNumberSeries(final DocumentNumberSeries documentNumberSeries) {
        return documentNumberSeries.getFinantialDocumentsSet().stream().filter(x -> x instanceof SettlementNote)
                .map(SettlementNote.class::cast);
    }

    public static Stream<SettlementNote> findByCurrency(final Currency currency) {
        return currency.getFinantialDocumentsSet().stream().filter(x -> x instanceof SettlementNote)
                .map(SettlementNote.class::cast);
    }

    public static Stream<SettlementNote> findByDocumentNumber(final java.lang.String documentNumber) {
        return findAll().filter(i -> documentNumber.equalsIgnoreCase(i.getDocumentNumber()));
    }

    public static Stream<SettlementNote> findByDocumentDate(final org.joda.time.DateTime documentDate) {
        return findAll().filter(i -> documentDate.equals(i.getDocumentDate()));
    }

    public static Stream<SettlementNote> findByDocumentDueDate(final org.joda.time.DateTime documentDueDate) {
        return findAll().filter(i -> documentDueDate.equals(i.getDocumentDueDate()));
    }

    public static Stream<SettlementNote> findByOriginDocumentNumber(final java.lang.String originDocumentNumber) {
        return findAll().filter(i -> originDocumentNumber.equalsIgnoreCase(i.getOriginDocumentNumber()));
    }

    public static Stream<SettlementNote> findByState(final FinantialDocumentStateType state) {
        return findAll().filter(i -> state.equals(i.getState()));
    }

    public static void checkMixingOfInvoiceEntriesExportedInLegacyERP(final Set<? extends InvoiceEntry> invoiceEntries) {
        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            return;
        }

        // Find at least one that is exported in legacy ERP
        final boolean atLeastOneExportedInLegacyERP = invoiceEntries.stream().filter(i -> i.getFinantialDocument() != null)
                .filter(i -> i.getFinantialDocument().isExportedInLegacyERP()).count() > 0;

        if (atLeastOneExportedInLegacyERP) {
            boolean notExportedInLegacyERP = invoiceEntries.stream()
                    .anyMatch(i -> i.getFinantialDocument() == null || !i.getFinantialDocument().isExportedInLegacyERP());

            if (notExportedInLegacyERP) {
                throw new TreasuryDomainException("error.SettlementNote.debit.entry.mixed.exported.in.legacy.erp.not.allowed");
            }
        }

    }

    public static void checkMixingOfInvoiceEntriesExportedInLegacyERP(final List<ISettlementInvoiceEntryBean> invoiceEntryBeans) {
        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            return;
        }

        // Find at least one that is exported in legacy ERP
        final boolean atLeastOneExportedInLegacyERP = invoiceEntryBeans.stream().filter(i -> i.getInvoiceEntry() != null)
                .filter(i -> i.getInvoiceEntry().getFinantialDocument() != null)
                .filter(i -> i.getInvoiceEntry().getFinantialDocument().isExportedInLegacyERP()).count() > 0;

        if (atLeastOneExportedInLegacyERP) {
            // Ensure all debit entries has finantial documents and exported in legacy erp
            boolean notExportedInLegacyERP = invoiceEntryBeans.stream()
                    .anyMatch(i -> i.getInvoiceEntry() == null || i.getInvoiceEntry().getFinantialDocument() == null
                            || !i.getInvoiceEntry().getFinantialDocument().isExportedInLegacyERP());

            if (notExportedInLegacyERP) {
                throw new TreasuryDomainException("error.SettlementNote.debit.entry.mixed.exported.in.legacy.erp.not.allowed");
            }
        }
    }

}
