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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;

public class DebitNote extends DebitNote_Base {

    protected DebitNote(FinantialEntity finantialEntity, DebtAccount debtAccount, DebtAccount payorDebtAccount,
            DocumentNumberSeries documentNumberSeries, DateTime documentDate) {
        super();

        setFinantialDocumentType(FinantialDocumentType.findForDebitNote());

        super.init(finantialEntity, debtAccount, payorDebtAccount, documentNumberSeries, documentDate);
    }

    @Override
    public boolean isDebitNote() {
        return true;
    }

    @Override
    protected void checkRules() {
        if (!getDocumentNumberSeries().getFinantialDocumentType().getType().equals(FinantialDocumentTypeEnum.DEBIT_NOTE)) {
            throw new TreasuryDomainException("error.DebitNote.finantialDocumentType.invalid");
        }

        if (getPayorDebtAccount() != null
                && !getPayorDebtAccount().getFinantialInstitution().equals(getDebtAccount().getFinantialInstitution())) {
            throw new TreasuryDomainException("error.DebitNote.finantialinstitution.mismatch");
        }

        super.checkRules();
    }

    @Override
    @Atomic
    public void delete(boolean deleteEntries) {
        super.delete(deleteEntries);
    }

    @Override
    public BigDecimal getOpenAmount() {
        if (this.isAnnulled()) {
            return BigDecimal.ZERO;
        }
        BigDecimal amount = BigDecimal.ZERO;
        for (DebitEntry entry : getDebitEntriesSet()) {
            amount = amount.add(entry.getOpenAmount());
        }
        return getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(amount);
    }

    @Override
    public BigDecimal getOpenAmountWithInterests() {
        if (this.getState().isPreparing() || this.getState().isClosed()) {
            if (TreasuryConstants.isEqual(getOpenAmount(), BigDecimal.ZERO)) {
                return BigDecimal.ZERO;
            } else {
                return getDebtAccount().getFinantialInstitution().getCurrency()
                        .getValueWithScale(getOpenAmount().add(getPendingInterestAmount()));
            }
        } else {
            return BigDecimal.ZERO;
        }
    }

    public Stream<? extends DebitEntry> getDebitEntries() {
        return DebitEntry.find(this);
    }

    public Set<? extends DebitEntry> getDebitEntriesSet() {
        return this.getDebitEntries().collect(Collectors.<DebitEntry> toSet());
    }

    public BigDecimal getDebitAmount() {
        return this.getTotalAmount();
    }

    public BigDecimal getCreditAmount() {
        return BigDecimal.ZERO;
    }

    @Atomic
    public void edit(final LocalDate documentDate, LocalDate documentDueDate, final String originDocumentNumber,
            final String documentObservations, final String documentTermsAndConditions,
            final String legacyERPCertificateDocumentReference) {

        if (isPreparing()) {
            setDocumentDate(documentDate.toDateTimeAtStartOfDay());
            setDocumentDueDate(documentDueDate);
        }

        setOriginDocumentNumber(originDocumentNumber);
        setDocumentObservations(documentObservations);
        setDocumentTermsAndConditions(documentTermsAndConditions);
        setLegacyERPCertificateDocumentReference(legacyERPCertificateDocumentReference);

        checkRules();
    }

    @Override
    @Atomic
    public void closeDocument(boolean markDocumentToExport) {
        setDocumentDueDate(maxDebitEntryDueDate());

        // if this document is not a manually issued document, then
        // recalculate all debit entries that the calculated amounts
        // were overriden. This is to not allow normal invoices with
        // overriden amounts
        if (!isManuallyIssuedDocument()) {
            getDebitEntriesSet().stream().filter(de -> Boolean.TRUE.equals(de.getCalculatedAmountsOverriden()))
                    .forEach(de -> de.disableOverrideCalculatedAmounts());
        }

        // VAT RECALCULATION
        //
        // TODO ANIL 2022-11-18: For now comment the following code, until we have a decision about this
//        if (getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification()) {
//            // Recalculate the vat rates for all debit entries
//            // This is done to avoid the case where a debit note
//            // is closed after some time of being created
//            // and in the meantime the VAT rate changes
//            getDebitEntriesSet().forEach(de -> {
//                de.setVat(
//                        Vat.findActiveUnique(de.getVat().getVatType(), getDebtAccount().getFinantialInstitution(), new DateTime())
//                                .get());
//                de.setVatRate(null);
//                de.recalculateAmountValues();
//            });
//        }

        super.closeDocument(markDocumentToExport);

        TreasuryPlataformDependentServicesFactory.implementation().certifyDocument(this);
    }

    public boolean isManuallyIssuedDocument() {
        return Boolean.TRUE.equals(getCertificationCopyFromCertifiedDocument())
                && getCertificationOriginalDocumentInvoiceSourceBillingType() == InvoiceSourceBillingType.M
                && Boolean.TRUE.equals(getCertificationOriginalDocumentManual());
    }

    private LocalDate maxDebitEntryDueDate() {
        final LocalDate maxDate =
                getDebitEntries().max(DebitEntry.COMPARE_BY_DUE_DATE).map(DebitEntry::getDueDate).orElse(getDocumentDueDate());

        return maxDate.isAfter(getDocumentDate().toLocalDate()) ? maxDate : getDocumentDate().toLocalDate();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<DebitNote> findAll() {
        return FinantialDocument.findAll().filter(x -> x instanceof DebitNote).map(DebitNote.class::cast);
    }

    public static Stream<DebitNote> find(final DebtAccount debtAccount) {
        return debtAccount.getFinantialDocumentsSet().stream().filter(x -> x instanceof DebitNote).map(DebitNote.class::cast);
    }

    @Atomic
    public static DebitNote create(FinantialEntity finantialEntity, DebtAccount debtAccount, DebtAccount payorDebtAccount,
            DocumentNumberSeries documentNumberSeries, DateTime documentDate, LocalDate documentDueDate, String originNumber,
            Map<String, String> propertiesMap, String documentObservations, String documentTermsAndConditions) {

        DebitNote note = new DebitNote(finantialEntity, debtAccount, payorDebtAccount, documentNumberSeries, documentDate);

        note.setOriginDocumentNumber(originNumber);
        note.setDocumentDueDate(documentDueDate);

        note.setDocumentObservations(documentObservations);
        note.setDocumentTermsAndConditions(documentTermsAndConditions);
        note.editPropertiesMap(propertiesMap);

        note.checkRules();

        return note;
    }

    @Atomic
    public static DebitNote createDebitNoteForDebitEntry(DebitEntry debitEntry, DebtAccount payorDebtAccount,
            DocumentNumberSeries documentNumberSeries, DateTime documentDate, LocalDate documentDueDate,
            String originDocumentNumber, String documentObservations, String documentTermsAndConditions) {
        if (debitEntry.getFinantialDocument() != null) {
            throw new IllegalStateException(
                    "error.DebitNote.createDebitNoteForDebitEntry.debitEntry.already.is.attached.to.finantialDocument");
        }

        final DebitNote debitNote = DebitNote.create(debitEntry.getFinantialEntity(), debitEntry.getDebtAccount(),
                payorDebtAccount, documentNumberSeries, documentDate, documentDueDate, originDocumentNumber,
                Collections.emptyMap(), null, null);

        debitEntry.setFinantialDocument(debitNote);

        return debitNote;
    }

    @Atomic
    // TODO ANIL 2024-08-07
    //
    // This receive a Set instead of a List
    public void addDebitNoteEntries(List<DebitEntry> debitEntries) {
        if (!isPreparing()) {
            throw new IllegalStateException("debit note is not in preparing state");
        }

        debitEntries.forEach(d -> {
            if (d.getFinantialDocument() != null && !d.getFinantialDocument().isPreparing()) {
                throw new IllegalArgumentException("debit entry with finantial document that is not in preparing state");
            }

            if (d.getFinantialDocument() != null && d.getDebitNote().getPayorDebtAccount() != getPayorDebtAccount()) {
                throw new IllegalArgumentException("debit entry with preparing debit note, but payor debt account mismatch");
            }

            this.addFinantialDocumentEntries(d);
        });
        checkRules();
    }

    @Override
    public Set<FinantialDocument> findRelatedDocuments(Set<FinantialDocument> documentsBaseList,
            Boolean includeAnulledDocuments) {
        documentsBaseList.add(this);

        for (DebitEntry entry : getDebitEntriesSet()) {
            for (CreditEntry creditEntry : entry.getCreditEntriesSet()) {
                if (creditEntry.getFinantialDocument() != null && !creditEntry.getFinantialDocument().isPreparing()) {
                    if (includeAnulledDocuments == true || this.isAnnulled() == false) {
                        if (documentsBaseList.contains(creditEntry.getFinantialDocument()) == false) {
                            documentsBaseList.addAll(creditEntry.getFinantialDocument().findRelatedDocuments(documentsBaseList,
                                    includeAnulledDocuments));
                        }
                    }
                }
            }
        }

        for (DebitEntry entry : getDebitEntriesSet()) {
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

    public BigDecimal getPendingInterestAmount() {
        return getPendingInterestAmount(new LocalDate());
    }

    public BigDecimal getPendingInterestAmount(LocalDate whenToCalculate) {
        BigDecimal interest = BigDecimal.ZERO;
        for (DebitEntry entry : this.getDebitEntriesSet()) {
            List<InterestRateBean> undebitedInterestRateBeansList = entry.calculateUndebitedInterestValue(whenToCalculate);

            BigDecimal interestAmount = undebitedInterestRateBeansList.stream().map(bean -> bean.getInterestAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            interest = interest.add(interestAmount);
        }
        return interest;
    }

    @Atomic
    public void anullDebitNoteWithCreditNote(String reason, boolean anullGeneratedInterests) {

        if (getDebitEntriesSet().stream().anyMatch(d -> d.getOpenPaymentPlan() != null)) {
            throw new TreasuryDomainException(
                    "error.DebitNote.anullDebitNoteWithCreditNote.cannot.anull.debt.with.open.paymentPlan");
        }

        if (TreasuryDebtProcessMainService.isFinantialDocumentAnnullmentActionBlocked(this)) {
            throw new TreasuryDomainException("error.DebitNote.cannot.annull.or.credit.due.to.existing.active.debt.process");
        }

        if (this.getFinantialDocumentEntriesSet().size() > 0 && this.isClosed()) {

            final DateTime now = new DateTime();

            if (anullGeneratedInterests) {
                //Annul open interest debit entry
                getDebitEntries().flatMap(entry -> entry.getInterestDebitEntriesSet().stream()).filter(
                        interest -> interest.getFinantialDocument() == null || interest.getFinantialDocument().isPreparing())
                        .forEach(interest -> {
                            if (interest.getFinantialDocument() == null) {
                                interest.annulDebitEntry(reason);
                            } else {
                                interest.getDebitNote().anullDebitNoteWithCreditNote(reason, false);
                            }
                        });
            }

            //1. criar nota de acerto
            //2. percorrer os itens de divida, criar correspondente item de acerto com o valor "aberto"
            //2.1 verificar se existiram "juros" gerados correspondentes
            //2.2 Libertar o tipo de juro a aplicar para não continuar a "calcular juro"
            //3. fechar nota de acerto
            //4. criar settlement note
            //5. adicionar itens de divida com cada valor open amount
            //5.1 adicionar itens de dívida com cada valor open amount dos juros
            //6. adicionar itens de acerto por cada valor open amount de item de divida
            //7. fechar settlement note

            // No final podem sobrar itens de acerto com valor pendente de utilizacao, que representam os valores ja pagos nos itens de dividas correspondentes
            createEquivalentCreditNote(now, reason, anullGeneratedInterests);

            //Clear the InterestRate for DebitEntry
            for (final DebitEntry debitEntry : this.getDebitEntriesSet()) {

                // Annul payment reference codes
                for (SibsPaymentRequest paymentCode : debitEntry.getSibsPaymentRequests()) {
                    if (paymentCode.isInCreatedState() || paymentCode.isInRequestedState()) {
                        paymentCode.anull();
                    }
                }

                debitEntry.clearInterestRate();

                // Also remove from treasury event
                if (debitEntry.getTreasuryEvent() != null) {
                    debitEntry.annulOnEvent();
                }

                debitEntry.getCreditEntriesSet().stream().filter(e -> !e.isAnnulled()).forEach(c -> {
                    debitEntry.closeCreditEntryIfPossible(reason, now, c);
                });
            }

            setAnnulledReason(reason);
            setAnnullmentDate(new DateTime());

            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
            setAnnullmentResponsible(!Strings.isNullOrEmpty(loggedUsername) ? loggedUsername : "unknown");

        } else if (isPreparing()) {
            if (!getCreditNoteSet().isEmpty()) {
                throw new TreasuryDomainException("error.DebitNote.creditNote.not.empty");
            }

            if (anullGeneratedInterests) {
                //Annul open interest debit entry
                getDebitEntries().flatMap(entry -> entry.getInterestDebitEntriesSet().stream())
                        .filter(interest -> !interest.isAnnulled()
                                && TreasuryConstants.isPositive(interest.getAvailableNetAmountForCredit()))
                        .forEach(interest -> interest.annulOnlyThisDebitEntryAndInterestsInBusinessContext(reason));
            }

            for (DebitEntry debitEntry : this.getDebitEntriesSet()) {

                // Also remove from treasury event
                if (debitEntry.getTreasuryEvent() != null) {
                    debitEntry.annulOnEvent();
                }

                for (SibsPaymentRequest paymentCode : debitEntry.getSibsPaymentRequests()) {
                    if (paymentCode.isInCreatedState() || paymentCode.isInRequestedState()) {
                        paymentCode.anull();
                    }
                }
            }

            this.setState(FinantialDocumentStateType.ANNULED);
            setAnnulledReason(reason);
            setAnnullmentDate(new DateTime());

            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
            setAnnullmentResponsible(!Strings.isNullOrEmpty(loggedUsername) ? loggedUsername : "unknown");
        } else {
            throw new TreasuryDomainException("error.DebitNote.cannot.anull.is.empty");
        }
    }

    @Atomic
    private void createEquivalentCreditNote(final DateTime documentDate, final String reason,
            final boolean createForInterestRateEntries) {
        FinantialInstitution finantialInstitution = getDebtAccount().getFinantialInstitution();

        boolean isToCloseCreditNoteWhenCreated = getDebtAccount().getFinantialInstitution().isToCloseCreditNoteWhenCreated();
        boolean isInvoiceRegistrationByTreasuryCertification =
                getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification();
        CreditNote creditNote = null;

        if (isInvoiceRegistrationByTreasuryCertification) {
            DocumentNumberSeries documentNumberSeries = inferCreditNoteDocumentNumberSeries();
            creditNote = CreditNote.create(this, documentNumberSeries, documentDate, getUiDocumentNumber());
        }

        for (DebitEntry entry : this.getDebitEntriesSet()) {
            //Get the amount for credit without tax, and considering the credit quantity FOR ONE
            BigDecimal amountForCreditWithoutVat = entry.getAvailableNetAmountForCredit();

            if (TreasuryConstants.isZero(amountForCreditWithoutVat) && !entry.getTreasuryExemptionsSet().isEmpty()) {
                continue;
            }

            Map<TreasuryExemption, BigDecimal> creditExemptionsMap =
                    entry.calculateDefaultNetExemptedAmountsToCreditMap(amountForCreditWithoutVat);

            final CreditEntry creditEntry = entry.createCreditEntry(documentDate, entry.getDescription(),
                    finantialInstitution.isInvoiceRegistrationByTreasuryCertification() ? null : reason, null,
                    amountForCreditWithoutVat, null, creditNote, creditExemptionsMap);

            creditEntry.setInternalComments(reason);

            if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices() && isExportedInLegacyERP()) {
                creditEntry.getFinantialDocument().setExportedInLegacyERP(true);
                creditEntry.getFinantialDocument().setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
            }
        }

        if (isInvoiceRegistrationByTreasuryCertification && isToCloseCreditNoteWhenCreated) {
            creditNote.closeDocument();
        }

        if (!createForInterestRateEntries) {
            return;
        }

        for (final DebitEntry debitEntry : this.getDebitEntriesSet()) {
            for (DebitEntry interestEntry : debitEntry.getInterestDebitEntriesSet()) {
                if (!interestEntry.getFinantialDocument().isClosed()) {
                    continue;
                }

                final BigDecimal amountForCreditWithoutVat = interestEntry.getAvailableNetAmountForCredit();

                if (TreasuryConstants.isZero(amountForCreditWithoutVat) && !interestEntry.getTreasuryExemptionsSet().isEmpty()) {
                    continue;
                }

                Map<TreasuryExemption, BigDecimal> creditExemptionsMap =
                        interestEntry.calculateDefaultNetExemptedAmountsToCreditMap(amountForCreditWithoutVat);

                CreditEntry interestsCreditEntry = interestEntry.createCreditEntry(documentDate, interestEntry.getDescription(),
                        finantialInstitution.isInvoiceRegistrationByTreasuryCertification() ? null : reason, null,
                        amountForCreditWithoutVat, null, null, creditExemptionsMap);

                interestsCreditEntry.setInternalComments(reason);

                if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()
                        && interestEntry.getFinantialDocument() != null
                        && interestEntry.getFinantialDocument().isExportedInLegacyERP()) {
                    interestsCreditEntry.getFinantialDocument().setExportedInLegacyERP(true);
                    interestsCreditEntry.getFinantialDocument()
                            .setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
                }
            }
        }
    }

    public void createCreditNote(String reason, Map<DebitEntry, BigDecimal> creditDebitEntriesMap,
            Map<TreasuryExemption, BigDecimal> creditTreasuryExemptionsMap) {
        FinantialInstitution finantialInstitution = getDebtAccount().getFinantialInstitution();

        DateTime now = new DateTime();

        DocumentNumberSeries documentNumberSeries = inferCreditNoteDocumentNumberSeries();
        CreditNote creditNoteWithCreditedNetAmountOnDebitEntries =
                CreditNote.create(this, documentNumberSeries, now, getUiDocumentNumber());

        CreditNote creditNoteWithOnlyCreditedNetExemptedAmountOnTreasuryExemptions =
                CreditNote.create(this, documentNumberSeries, now, getUiDocumentNumber());

        creditDebitEntriesMap.forEach((debitEntry, creditNetAmount) -> {
            Map<TreasuryExemption, BigDecimal> creditExemptionsMap =
                    creditTreasuryExemptionsMap.entrySet().stream().filter(e -> e.getKey().getDebitEntry() == debitEntry)
                            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

            CreditNote creditNoteToAggregate = null;
            if (TreasuryConstants.isPositive(creditNetAmount)) {
                creditNoteToAggregate = creditNoteWithCreditedNetAmountOnDebitEntries;
            } else {
                creditNoteToAggregate = creditNoteWithOnlyCreditedNetExemptedAmountOnTreasuryExemptions;
            }

            CreditEntry creditEntry = debitEntry.createCreditEntry(now, debitEntry.getDescription(),
                    finantialInstitution.isInvoiceRegistrationByTreasuryCertification() ? null : reason, null, creditNetAmount,
                    null, creditNoteToAggregate, creditExemptionsMap);
            creditEntry.setInternalComments(reason);

        });

        boolean isToCloseCreditNoteWhenCreated = getDebtAccount().getFinantialInstitution().isToCloseCreditNoteWhenCreated();
        boolean isInvoiceRegistrationByTreasuryCertification =
                getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification();

        if (!creditNoteWithCreditedNetAmountOnDebitEntries.isDocumentEmpty() && isInvoiceRegistrationByTreasuryCertification
                && isToCloseCreditNoteWhenCreated) {
            creditNoteWithCreditedNetAmountOnDebitEntries.closeDocument();
        }

        creditDebitEntriesMap.keySet().forEach(debitEntry -> {

            if (debitEntry.getTreasuryEvent() != null) {
                if (!TreasuryConstants.isPositive(debitEntry.getAvailableNetAmountForCredit())
                        && !TreasuryConstants.isPositive(debitEntry.getEffectiveNetExemptedAmount())) {
                    debitEntry.annulOnEvent();
                }
            }

            for (SibsPaymentRequest paymentCode : debitEntry.getSibsPaymentRequestsSet()) {
                if (paymentCode.isInCreatedState() || paymentCode.isInRequestedState()) {
                    paymentCode.anull();
                }
            }

            debitEntry.getCreditEntriesSet().stream().filter(c -> !c.isAnnulled())
                    .forEach(c -> debitEntry.closeCreditEntryIfPossible(reason, now, c));
        });

        if (creditNoteWithCreditedNetAmountOnDebitEntries.isDocumentEmpty()) {
            creditNoteWithCreditedNetAmountOnDebitEntries.delete(false);
        }

        if (creditNoteWithOnlyCreditedNetExemptedAmountOnTreasuryExemptions.isDocumentEmpty()) {
            creditNoteWithOnlyCreditedNetExemptedAmountOnTreasuryExemptions.delete(false);
        }

    }

    /**
     * Use this method to get the credit note document number series, to be used in annulments, credits and so on
     * 
     * This method was created in the treasury certification scope, due to importation of other certified documents
     * 
     * @return
     */
    public DocumentNumberSeries inferCreditNoteDocumentNumberSeries() {
        boolean isInvoiceRegistrationByTreasuryCertification =
                getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification();

        if (isInvoiceRegistrationByTreasuryCertification && getDocumentNumberSeries().getSeries().isLegacy()) {
            // This is relevant for treasury certification
            //
            // Avoid using the same document number series for documents from other
            // software applications, documents issued manually or recovered documents

            return DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(),
                    Series.findUniqueDefault(getDebtAccount().getFinantialInstitution()).get());
        } else {
            return DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), getDocumentNumberSeries().getSeries());
        }
    }

    public void annulCertifiedDebitNote(String reason) {
        if (!isCertifiedDebitNoteAnnulable()) {
            throw new TreasuryDomainException(
                    "error.DebitNote.annulCertifiedDebitNote.not.possible.due.existing.active.credits.or.settlements");
        }

        for (DebitEntry debitEntry : this.getDebitEntriesSet()) {

            // Also remove from treasury event
            if (debitEntry.getTreasuryEvent() != null) {
                debitEntry.annulOnEvent();
            }

            for (SibsPaymentRequest paymentCode : debitEntry.getSibsPaymentRequests()) {
                if (paymentCode.isInCreatedState() || paymentCode.isInRequestedState()) {
                    paymentCode.anull();
                }
            }
        }

        this.setState(FinantialDocumentStateType.ANNULED);

        setAnnulledReason(reason);
        setAnnullmentDate(new DateTime());

        final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
        setAnnullmentResponsible(!Strings.isNullOrEmpty(loggedUsername) ? loggedUsername : "unknown");

        TreasuryPlataformDependentServicesFactory.implementation().annulCertifiedDocument(this);
    }

    public boolean isCertifiedDebitNoteAnnulable() {
        if (!isClosed()) {
            return false;
        }

        boolean withAllCreditNotesAnnuled = getCreditNoteSet().stream().allMatch(c -> c.isAnnulled());
        boolean withAllSettlementNotesAnnuled = getDebitEntriesSet().stream()
                .allMatch(de -> de.getSettlementEntriesSet().stream().allMatch(se -> se.getFinantialDocument().isAnnulled()));
        boolean withAllInterestsAnnuled = getDebitEntriesSet().stream()
                .allMatch(de -> de.getInterestDebitEntriesSet().stream().allMatch(i -> i.isAnnulled()));

        return withAllCreditNotesAnnuled && withAllSettlementNotesAnnuled && withAllInterestsAnnuled;
    }

    public Set<CreditEntry> getRelatedCreditEntriesSet() {
        Set<CreditEntry> result = new HashSet<CreditEntry>();
        for (DebitEntry debit : this.getDebitEntriesSet()) {
            result.addAll(debit.getCreditEntriesSet());
        }
        return result;
    }

    @Atomic
    public DebitNote updatePayorDebtAccount(final DebtAccount payorDebtAccount) {
        if (!isPreparing() && !isClosed()) {
            throw new TreasuryDomainException("error.DebitNote.updatePayorDebtAccount.not.preparing.nor.closed");
        }

        if (getPayorDebtAccount() == payorDebtAccount) {
            throw new TreasuryDomainException("error.DebitNote.updatePayorDebtAccount.payor.not.changed");
        }

        if (payorDebtAccount == getDebtAccount()) {
            throw new TreasuryDomainException("error.DebitNote.updatePayorDebtAccount.payor.same.as.debt.account");
        }

        if (getDebitEntriesSet().stream().anyMatch(entry -> entry.isInOpenPaymentPlan())) {
            throw new TreasuryDomainException("error.DebitNote.updatePayorDebtAccount.debitEntry.in.active.paymentPlan");
        }

        if (isClosed()) {
            // Check if debit entries has settlement entries
            for (final DebitEntry debitEntry : this.getDebitEntriesSet()) {
                if (debitEntry.getSettlementEntriesSet().stream().filter(s -> !s.isAnnulled()).count() > 0) {
                    throw new TreasuryDomainException("error.DebitNote.updatePayorDebtAccount.debit.entries.has.settlements");
                }
            }
        }

        final DebitNote updatingDebitNote = isPreparing() ? this : anullAndCopyDebitNote(
                treasuryBundle("label.DebitNote.updatePayorDebtAccount.anull.reason"));

        updatingDebitNote.setPayorDebtAccount(payorDebtAccount);

        for (DebitEntry debitEntry : this.getDebitEntriesSet()) {
            for (PaymentRequest paymentCode : debitEntry.getSibsPaymentRequests()) {
                if (paymentCode.isInCreatedState() || paymentCode.isInRequestedState()) {
                    ((SibsPaymentRequest) paymentCode).anull();
                }
            }
        }

        return updatingDebitNote;
    }

    private DebitNote anullAndCopyDebitNote(final String reason) {
        if (!isClosed()) {
            throw new TreasuryDomainException("error.DebitNote.anullAndCopyDebitNote.copy.only.on.closed.debit.note");
        }

        final DebitNote newDebitNote = DebitNote.create(getFinantialEntity(), getDebtAccount(), null, getDocumentNumberSeries(),
                new DateTime(), new LocalDate(), getOriginDocumentNumber(), Collections.emptyMap(), null, null);

        newDebitNote.setOriginDocumentNumber(getOriginDocumentNumber());
        for (final FinantialDocumentEntry finantialDocumentEntry : getFinantialDocumentEntriesSet()) {
            final DebitEntry debitEntry = (DebitEntry) finantialDocumentEntry;

            DebitEntry newDebitEntry = DebitEntry.create(debitEntry.getFinantialEntity(), debitEntry.getDebtAccount(),
                    debitEntry.getTreasuryEvent(), debitEntry.getVat(),
                    debitEntry.getAmount().add(debitEntry.getNetExemptedAmount()), debitEntry.getDueDate(),
                    debitEntry.getPropertiesMap(), debitEntry.getProduct(), debitEntry.getDescription(), debitEntry.getQuantity(),
                    debitEntry.getInterestRate(), debitEntry.getEntryDateTime(), debitEntry.isAcademicalActBlockingSuspension(),
                    debitEntry.isBlockAcademicActsOnDebt(), newDebitNote);

            DebitEntry.applyAdditionalRelationConnectionsOfDebitEntry(debitEntry, newDebitEntry);

            debitEntry.getTreasuryExemptionsSet()
                    .forEach(treasuryExemption -> TreasuryExemption.create(treasuryExemption.getTreasuryExemptionType(),
                            treasuryExemption.getReason(), treasuryExemption.getNetAmountToExempt(), newDebitEntry));
        }

        anullDebitNoteWithCreditNote(reason, false);

        return newDebitNote;
    }

    @Atomic
    public void updateAllDueDates(LocalDate newDueDate) {

        this.setDocumentDueDate(newDueDate);

        this.getDebitEntries().forEach(entry -> {
            entry.updateDueDate(newDueDate);
        });
        checkRules();
    }

    /**
     * This method is an helper to create open credit note, for recovery or integration purposes.
     * Created in the scope of treasury certification
     */
    public void createOpenCreditNoteForIntegrationOrRecovery() {
        if (isAnnulled()) {
            throw new IllegalStateException("error.DebitNote.createOpenCreditNoteForIntegrationOrRecovery.document.is.annuled");
        }

        if (!TreasuryConstants.isPositive(getAvailableNetAmountForCredit())) {
            throw new IllegalStateException(
                    "error.DebitNote.createOpenCreditNoteForIntegrationOrRecovery.available.amount.for.credit.is.not.positive");
        }

        if (!getDocumentNumberSeries().getSeries().isLegacy()) {
            throw new IllegalStateException("error.DebitNote.createOpenCreditNoteForIntegrationOrRecovery.series.is.not.legacy");
        }

        if (!getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification()) {
            throw new IllegalStateException(
                    "error.DebitNote.createOpenCreditNoteForIntegrationOrRecovery.invoice.registration.not.treasury.certification");
        }

        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(), getDocumentNumberSeries().getSeries());

        DateTime documentDate = new DateTime();
        CreditNote creditNote = CreditNote.create(this, documentNumberSeries, documentDate, getUiDocumentNumber());

        for (DebitEntry entry : this.getDebitEntriesSet()) {
            // Get the amount for credit without tax, and considering the credit quantity FOR ONE
            final BigDecimal amountForCreditWithoutVat = entry.getAvailableNetAmountForCredit();

            if (!TreasuryConstants.isPositive(amountForCreditWithoutVat)) {
                continue;
            }

            Map<TreasuryExemption, BigDecimal> creditExemptionsMap = entry.calculateDefaultNetExemptedAmountsToCreditMap();
            entry.createCreditEntry(documentDate, entry.getDescription(), null, null, amountForCreditWithoutVat, null, creditNote,
                    creditExemptionsMap);
        }
    }

    public BigDecimal getAvailableNetAmountForCredit() {
        return getDebitEntriesSet().stream().map(de -> de.getAvailableNetAmountForCredit()).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    public BigDecimal getAvailableNetExemptedAmountForCredit() {
        return getDebitEntriesSet().stream().flatMap(de -> de.getTreasuryExemptionsSet().stream())
                .filter(te -> te.getCreditEntry() == null).map(te -> te.getAvailableNetExemptedAmountForCredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}
