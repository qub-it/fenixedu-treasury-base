/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  (o) Redistributions of source code must retain the above
 *  copyright notice, this list of conditions and the following
 *  disclaimer.
 *
 *  (o) Redistributions in binary form must reproduce the
 *  above copyright notice, this list of conditions and the
 *  following disclaimer in the documentation and/or other
 *  materials provided with the distribution.
 *
 *  (o) Neither the name of Quorum Born IT nor the names of
 *  its contributors may be used to endorse or promote products
 *  derived from this software without specific prior written
 *  permission.
 *
 *  (o) Universidade de Lisboa and its respective subsidiary
 *  Serviços Centrais da Universidade de Lisboa (Departamento
 *  de Informática), hereby referred to as the Beneficiary,
 *  is the sole demonstrated end-user and ultimately the only
 *  beneficiary of the redistributed binary form and/or source
 *  code.
 *
 *  (o) The Beneficiary is entrusted with either the binary form,
 *  the source code, or both, and by accepting it, accepts the
 *  terms of this License.
 *
 *  (o) Redistribution of any binary form and/or source code is
 *  only allowed in the scope of the Universidade de Lisboa
 *  FenixEdu(™)’s implementation projects.
 *
 *  (o) This license and conditions of redistribution of source
 *  code/binary can oly be reviewed by the Steering Comittee of
 *  FenixEdu(™) <http://www.fenixedu.org/>.
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

import static org.fenixedu.treasury.util.TreasuryConstants.rationalVatRate;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.paymentcodes.MultipleEntriesPaymentCode;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import pt.ist.fenixframework.Atomic;

public class DebitNote extends DebitNote_Base {

    protected DebitNote(final DebtAccount debtAccount, final DocumentNumberSeries documentNumberSeries,
            final DateTime documentDate) {
        super();
        this.init(debtAccount, documentNumberSeries, documentDate);
    }

    protected DebitNote(final DebtAccount debtAccount, final DebtAccount payorDebtAccount,
            final DocumentNumberSeries documentNumberSeries, final DateTime documentDate) {
        super();

        this.init(debtAccount, payorDebtAccount, documentNumberSeries, documentDate);
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
            final String documentObservations, final String legacyERPCertificateDocumentReference) {

        if (isPreparing()) {
            setDocumentDate(documentDate.toDateTimeAtStartOfDay());
            setDocumentDueDate(documentDueDate);
        }

        setOriginDocumentNumber(originDocumentNumber);
        setDocumentObservations(documentObservations);
        setLegacyERPCertificateDocumentReference(legacyERPCertificateDocumentReference);

        checkRules();
    }

    @Override
    @Atomic
    public void closeDocument(boolean markDocumentToExport) {
        setDocumentDueDate(maxDebitEntryDueDate());

        super.closeDocument(markDocumentToExport);
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
    public static DebitNote create(final DebtAccount debtAccount, final DocumentNumberSeries documentNumberSeries,
            final DateTime documentDate) {
        DebitNote note = new DebitNote(debtAccount, documentNumberSeries, documentDate);
        note.setFinantialDocumentType(FinantialDocumentType.findForDebitNote());
        note.setOriginDocumentNumber("");
        note.setDocumentDueDate(documentDate.toLocalDate());
        note.checkRules();
        return note;
    }

    @Atomic
    public static DebitNote create(final DebtAccount debtAccount, final DebtAccount payorDebtAccount,
            final DocumentNumberSeries documentNumberSeries, final DateTime documentDate, final LocalDate documentDueDate,
            final String originNumber) {

        DebitNote note = new DebitNote(debtAccount, payorDebtAccount, documentNumberSeries, documentDate);
        note.setFinantialDocumentType(FinantialDocumentType.findForDebitNote());
        note.setOriginDocumentNumber(originNumber);
        note.setDocumentDueDate(documentDueDate);
        note.checkRules();

        return note;
    }

    @Atomic
    public static DebitNote createDebitNoteForDebitEntry(DebitEntry debitEntry, DebtAccount payorDebtAccount,
            DocumentNumberSeries documentNumberSeries, DateTime documentDate, LocalDate documentDueDate,
            String originDocumentNumber, String documentObservations) {
        final DebitNote debitNote = DebitNote.create(debitEntry.getDebtAccount(), payorDebtAccount, documentNumberSeries,
                documentDate, documentDueDate, originDocumentNumber);
        debitNote.setDocumentObservations(documentObservations);

        debitEntry.setFinantialDocument(debitNote);

        return debitNote;
    }

    public static DebitNote copyDebitNote(final DebitNote debitNoteToCopy, final boolean copyDocumentDate,
            final boolean copyCloseDate, final boolean applyExemptions) {
        final DebitNote result = DebitNote.create(debitNoteToCopy.getDebtAccount(), debitNoteToCopy.getPayorDebtAccount(),
                debitNoteToCopy.getDocumentNumberSeries(), copyDocumentDate ? debitNoteToCopy.getDocumentDate() : new DateTime(),
                debitNoteToCopy.getDocumentDueDate(), debitNoteToCopy.getOriginDocumentNumber());

        if (copyCloseDate) {
            result.setCloseDate(debitNoteToCopy.getCloseDate());
            result.setExportedInLegacyERP(debitNoteToCopy.isExportedInLegacyERP());
        }

        result.setAddress(debitNoteToCopy.getAddress());
        result.setDocumentObservations(result.getDocumentObservations());
        result.setLegacyERPCertificateDocumentReference(debitNoteToCopy.getLegacyERPCertificateDocumentReference());

        final Map<DebitEntry, DebitEntry> debitEntriesMap = Maps.newHashMap();

        for (final FinantialDocumentEntry finantialDocumentEntry : debitNoteToCopy.getFinantialDocumentEntriesSet()) {
            final DebitEntry sourceDebitEntry = (DebitEntry) finantialDocumentEntry;
            final boolean applyExemptionOnDebitEntry = applyExemptions && (sourceDebitEntry.getTreasuryExemption() != null
                    && TreasuryConstants.isPositive(sourceDebitEntry.getExemptedAmount()));

            final DebitEntry debitEntryCopy = DebitEntry.copyDebitEntry(sourceDebitEntry, result, applyExemptionOnDebitEntry);

            debitEntriesMap.put(sourceDebitEntry, debitEntryCopy);

        }

        if (applyExemptions) {
            for (final FinantialDocumentEntry finantialDocumentEntry : debitNoteToCopy.getFinantialDocumentEntriesSet()) {
                final DebitEntry sourceDebitEntry = (DebitEntry) finantialDocumentEntry;
                final boolean exemptionAppliedWithCreditNote = sourceDebitEntry.getTreasuryExemption() != null
                        && !TreasuryConstants.isPositive(sourceDebitEntry.getExemptedAmount());

                if (!exemptionAppliedWithCreditNote) {
                    continue;
                }

                if (result.isPreparing()) {
                    result.closeDocument();
                }

                final DebitEntry debitEntryCopy = debitEntriesMap.get(sourceDebitEntry);

                final TreasuryExemption treasuryExemptionToCopy = sourceDebitEntry.getTreasuryExemption();
                TreasuryExemption.create(treasuryExemptionToCopy.getTreasuryExemptionType(),
                        treasuryExemptionToCopy.getTreasuryEvent(), treasuryExemptionToCopy.getReason(),
                        treasuryExemptionToCopy.getValueToExempt(), debitEntryCopy);
            }
        }

        return result;
    }

    @Atomic
    public void addDebitNoteEntries(List<DebitEntry> debitEntries) {
        debitEntries.forEach(x -> this.addFinantialDocumentEntries(x));
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
            interest = interest.add(entry.calculateUndebitedInterestValue(whenToCalculate).getInterestAmount());
        }
        return interest;
    }

    @Atomic
    public void anullDebitNoteWithCreditNote(String reason, boolean anullGeneratedInterests) {

        if(getDebitEntriesSet().stream().anyMatch(d -> d.getOpenPaymentPlan() != null)) {
            throw new TreasuryDomainException("error.DebitNote.anullDebitNoteWithCreditNote.cannot.anull.debt.with.open.paymentPlan");
        }
        
        if (this.getFinantialDocumentEntriesSet().size() > 0 && this.isClosed()) {

            final DateTime now = new DateTime();

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
                for (final MultipleEntriesPaymentCode paymentCode : debitEntry.getPaymentCodesSet()) {
                    if (paymentCode.getPaymentReferenceCode().isNew() || paymentCode.getPaymentReferenceCode().isUsed()) {
                        paymentCode.getPaymentReferenceCode().anullPaymentReferenceCode();
                    }
                }

                debitEntry.clearInterestRate();

                // Also remove from treasury event
                if (debitEntry.getTreasuryEvent() != null) {
                    debitEntry.annulOnEvent();
                }

                for (final CreditEntry creditEntry : debitEntry.getCreditEntriesSet()) {
                    debitEntry.closeCreditEntryIfPossible(reason, now, creditEntry);
                }
            }

            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

            if (!Strings.isNullOrEmpty(loggedUsername)) {
                setAnnulledReason(reason + " - [" + loggedUsername + "]" + new DateTime().toString("YYYY-MM-dd HH:mm:ss"));
            } else {
                setAnnulledReason(reason + " - " + new DateTime().toString("YYYY-MM-dd HH:mm:ss"));
            }

        } else if (isPreparing()) {
            if (!getCreditNoteSet().isEmpty()) {
                throw new TreasuryDomainException("error.DebitNote.creditNote.not.empty");
            }

            for (DebitEntry debitEntry : this.getDebitEntriesSet()) {

                // Also remove from treasury event
                if (debitEntry.getTreasuryEvent() != null) {
                    debitEntry.annulOnEvent();
                }

                for (final MultipleEntriesPaymentCode paymentCode : debitEntry.getPaymentCodesSet()) {
                    if (paymentCode.getPaymentReferenceCode().isNew() || paymentCode.getPaymentReferenceCode().isUsed()) {
                        paymentCode.getPaymentReferenceCode().anullPaymentReferenceCode();
                    }
                }
            }

            this.setState(FinantialDocumentStateType.ANNULED);

            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

            if (!Strings.isNullOrEmpty(loggedUsername)) {
                setAnnulledReason(reason + " - [" + loggedUsername + "]" + new DateTime().toString("YYYY-MM-dd HH:mm:ss"));
            } else {
                setAnnulledReason(reason + " - " + new DateTime().toString("YYYY-MM-dd HH:mm:ss"));
            }
        } else {
            throw new TreasuryDomainException("error.DebitNote.cannot.anull.is.empty");
        }
    }

    @Atomic
    public void createEquivalentCreditNote(final DateTime documentDate, final String documentObservations,
            final boolean createForInterestRateEntries) {
        for (DebitEntry entry : this.getDebitEntriesSet()) {
            //Get the amount for credit without tax, and considering the credit quantity FOR ONE
            final BigDecimal amountForCreditWithoutVat = entry.getCurrency().getValueWithScale(
                    TreasuryConstants.divide(entry.getAvailableAmountForCredit(), BigDecimal.ONE.add(rationalVatRate(entry))));

            if (TreasuryConstants.isZero(amountForCreditWithoutVat) && entry.getTreasuryExemption() != null) {
                continue;
            }

            final CreditEntry creditEntry = entry.createCreditEntry(documentDate, entry.getDescription(), documentObservations,
                    amountForCreditWithoutVat, null, null);

            if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices() && isExportedInLegacyERP()) {
                creditEntry.getFinantialDocument().setExportedInLegacyERP(true);
                creditEntry.getFinantialDocument().setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
            }
        }

        if (!createForInterestRateEntries) {
            return;
        }

        for (final DebitEntry debitEntry : this.getDebitEntriesSet()) {
            for (DebitEntry interestEntry : debitEntry.getInterestDebitEntriesSet()) {
                final BigDecimal amountForCreditWithoutVat = interestEntry.getCurrency().getValueWithScale(TreasuryConstants
                        .divide(interestEntry.getAvailableAmountForCredit(), BigDecimal.ONE.add(rationalVatRate(interestEntry))));

                if (TreasuryConstants.isZero(amountForCreditWithoutVat) && interestEntry.getTreasuryExemption() != null) {
                    continue;
                }

                CreditEntry interestsCreditEntry = interestEntry.createCreditEntry(documentDate, interestEntry.getDescription(),
                        documentObservations, amountForCreditWithoutVat, null, null);

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
            for (final MultipleEntriesPaymentCode paymentCode : debitEntry.getPaymentCodesSet()) {
                if (paymentCode.getPaymentReferenceCode().isNew() || paymentCode.getPaymentReferenceCode().isUsed()) {
                    paymentCode.getPaymentReferenceCode().anullPaymentReferenceCode();
                }
            }
        }

        return updatingDebitNote;
    }

    // TODO: Debit entries are not copied well, it misses curricularCourse, evaluationSeason and
    // executionSemester
    private DebitNote anullAndCopyDebitNote(final String reason) {
        if (!isClosed()) {
            throw new TreasuryDomainException("error.DebitNote.anullAndCopyDebitNote.copy.only.on.closed.debit.note");
        }

        final DebitNote newDebitNote = DebitNote.create(getDebtAccount(), getDocumentNumberSeries(), new DateTime());

        newDebitNote.setOriginDocumentNumber(getOriginDocumentNumber());
        for (final FinantialDocumentEntry finantialDocumentEntry : getFinantialDocumentEntriesSet()) {
            final DebitEntry debitEntry = (DebitEntry) finantialDocumentEntry;

            // TODO Use DebitEntry.copyDebitEntry service
            DebitEntry newDebitEntry = DebitEntry.create(Optional.of(newDebitNote), debitEntry.getDebtAccount(),
                    debitEntry.getTreasuryEvent(), debitEntry.getVat(),
                    debitEntry.getAmount().add(debitEntry.getExemptedAmount()), debitEntry.getDueDate(),
                    debitEntry.getPropertiesMap(), debitEntry.getProduct(), debitEntry.getDescription(), debitEntry.getQuantity(),
                    debitEntry.getInterestRate(), debitEntry.getEntryDateTime());

            if (debitEntry.getTreasuryExemption() != null) {
                final TreasuryExemption treasuryExemption = debitEntry.getTreasuryExemption();
                TreasuryExemption.create(treasuryExemption.getTreasuryExemptionType(), debitEntry.getTreasuryEvent(),
                        treasuryExemption.getReason(), treasuryExemption.getValueToExempt(), newDebitEntry);
            }

            newDebitEntry.edit(newDebitEntry.getDescription(), newDebitEntry.getTreasuryEvent(), newDebitEntry.getDueDate(),
                    debitEntry.isAcademicalActBlockingSuspension(), debitEntry.isBlockAcademicActsOnDebt());
        }

        anullDebitNoteWithCreditNote(reason, false);

        return newDebitNote;
    }

    @Atomic
    public static DebitNote createInterestDebitNoteForDebitNote(final DebitNote debitNote,
            final DocumentNumberSeries documentNumberSeries, final LocalDate paymentDate, final String documentObservations) {
        DebitNote interestDebitNote;
        if (documentNumberSeries.getSeries().getCertificated()) {
            interestDebitNote =
                    DebitNote.createInterestDebitNoteForDebitNote(debitNote, documentNumberSeries, new DateTime(), paymentDate);
        } else {
            interestDebitNote = DebitNote.createInterestDebitNoteForDebitNote(debitNote, documentNumberSeries,
                    paymentDate.toDateTimeAtStartOfDay(), paymentDate);
        }
        interestDebitNote.setDocumentObservations(documentObservations);

        return interestDebitNote;
    }

    public static DebitNote createInterestDebitNoteForDebitNote(DebitNote debitNote, DocumentNumberSeries documentNumberSeries,
            DateTime documentDate, LocalDate paymentDate) {

        DebitNote interestDebitNote = DebitNote.create(debitNote.getDebtAccount(), documentNumberSeries, documentDate);
        for (DebitEntry entry : debitNote.getDebitEntriesSet()) {
            InterestRateBean calculateUndebitedInterestValue = entry.calculateUndebitedInterestValue(paymentDate);
            if (TreasuryConstants.isGreaterThan(calculateUndebitedInterestValue.getInterestAmount(), BigDecimal.ZERO)) {
                entry.createInterestRateDebitEntry(calculateUndebitedInterestValue, documentDate,
                        Optional.<DebitNote> of(interestDebitNote));
            }
        }

        if (TreasuryConstants.isEqual(interestDebitNote.getTotalAmount(), BigDecimal.ZERO)) {
            interestDebitNote.delete(true);
            throw new TreasuryDomainException(treasuryBundle("error.DebitNote.no.interest.to.generate"));
        }
        return interestDebitNote;
    }

    public static DebitEntry createBalanceTransferDebit(final DebtAccount debtAccount, final DateTime entryDate,
            final LocalDate dueDate, final String originNumber, final Product product, final BigDecimal amountWithVat,
            final DebtAccount payorDebtAccount, String entryDescription, final InterestRate interestRate) {
        final FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();
        final Series regulationSeries = finantialInstitution.getRegulationSeries();
        final DocumentNumberSeries numberSeries =
                DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), regulationSeries);
        final Vat transferVat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, entryDate).get();

        if (Strings.isNullOrEmpty(entryDescription)) {
            entryDescription = product.getName().getContent();
        }

        final DebitNote debitNote = DebitNote.create(debtAccount, payorDebtAccount, numberSeries, new DateTime(),
                new DateTime().toLocalDate(), originNumber);

        final BigDecimal amountWithoutVat = TreasuryConstants.divide(amountWithVat, BigDecimal.ONE.add(transferVat.getTaxRate()));
        return DebitEntry.create(Optional.of(debitNote), debtAccount, null, transferVat, amountWithoutVat, dueDate,
                Maps.newHashMap(), product, entryDescription, BigDecimal.ONE, interestRate, entryDate);
    }

}
