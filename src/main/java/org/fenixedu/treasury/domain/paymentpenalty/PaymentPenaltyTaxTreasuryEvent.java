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
package org.fenixedu.treasury.domain.paymentpenalty;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanStateType;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.tariff.DueDateCalculationType;
import org.fenixedu.treasury.domain.tariff.Tariff;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.dto.PaymentPenaltyEntryBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class PaymentPenaltyTaxTreasuryEvent extends PaymentPenaltyTaxTreasuryEvent_Base {

    public PaymentPenaltyTaxTreasuryEvent() {
        super();
    }

    protected PaymentPenaltyTaxTreasuryEvent(FinantialEntity finantialEntity, Product product, LocalizedString description,
            DebitEntry debitEntry) {
        this();

        super.init(finantialEntity, product, description);

        super.setDebtAccount(debitEntry.getDebtAccount());
        super.setOriginDebitEntry(debitEntry);

        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();

        if (super.getDebtAccount() == null) {
            throw new TreasuryDomainException("error.PaymentPenaltyTaxTreasuryEvent.debtAccount.required");
        }

        if (super.getOriginDebitEntry() == null) {
            throw new TreasuryDomainException("error.PaymentPenaltyTaxTreasuryEvent.originDebitEntry.required");
        }
    }

    @Override
    public String getERPIntegrationMetadata() {
        return "";
    }

    @Override
    public LocalDate getTreasuryEventDate() {
        return super.getOriginDebitEntry().getLastPaymentDate().toLocalDate();
    }

    @Override
    public String getDegreeCode() {
        if (super.getOriginDebitEntry().getTreasuryEvent() != null) {
            return super.getOriginDebitEntry().getTreasuryEvent().getDegreeCode();
        }

        return null;
    }

    @Override
    public String getDegreeName() {
        if (super.getOriginDebitEntry().getTreasuryEvent() != null) {
            return super.getOriginDebitEntry().getTreasuryEvent().getDegreeName();
        }

        return null;
    }

    @Override
    public String getExecutionYearName() {
        if (super.getOriginDebitEntry().getTreasuryEvent() != null) {
            return super.getOriginDebitEntry().getTreasuryEvent().getExecutionYearName();
        }

        return null;
    }

    @Override
    public void copyDebitEntryInformation(DebitEntry sourceDebitEntry, DebitEntry copyDebitEntry) {
    }

    @Override
    public Optional<Tariff> findMatchTariff(FinantialEntity finantialEntity, Product product, LocalDate when) {
        return Optional.empty();
    }

    // Services

    public static Stream<? extends PaymentPenaltyTaxTreasuryEvent> findAll() {
        return FenixFramework.getDomainRoot().getTreasuryEventsSet().stream()
                .filter(e -> e instanceof PaymentPenaltyTaxTreasuryEvent).map(PaymentPenaltyTaxTreasuryEvent.class::cast);
    }

    public static Stream<? extends PaymentPenaltyTaxTreasuryEvent> find(DebitEntry originDebitEntry) {
        return originDebitEntry.getPaymentPenaltyTaxTreasuryEventSet().stream();
    }

    public static PaymentPenaltyEntryBean calculatePaymentPenaltyTax(DebitEntry originDebitEntry, LocalDate lastPaymentDate) {
        return calculatePaymentPenaltyTax(originDebitEntry, lastPaymentDate, new LocalDate());
    }

    public static PaymentPenaltyEntryBean calculatePaymentPenaltyTax(DebitEntry originDebitEntry, LocalDate lastPaymentDate,
            LocalDate whenDebtCreationDate) {

        if (!shouldPenaltyBeCreatedForDebitEntry(originDebitEntry, lastPaymentDate)) {
            return null;
        }

        PaymentPenaltyTaxSettings settings =
                PaymentPenaltyTaxSettings.findActiveForOriginDebitEntry(originDebitEntry).findFirst().orElse(null);

        Tariff tariff = null;
        if (originDebitEntry.getTreasuryEvent() != null) {
            tariff = originDebitEntry.getTreasuryEvent()
                    .findMatchTariff(settings.getFinantialEntity(), settings.getPenaltyProduct(), lastPaymentDate).orElse(null);
        } else {
            tariff = Tariff.find(settings.getPenaltyProduct(), lastPaymentDate.toDateTimeAtStartOfDay())
                    .filter(t -> t.getFinantialEntity() == settings.getFinantialEntity())
                    .filter(t -> t.isBroadTariffForFinantialEntity()).findFirst().orElse(null);
        }

        if (tariff == null) {
            throw new TreasuryDomainException(
                    "error.PaymentPenaltyTaxTreasuryEvent.checkAndCreatePaymentPenaltyTax.tariff.not.found");
        }

        if (tariff.getFinantialEntity().getFinantialInstitution() != originDebitEntry.getDebtAccount()
                .getFinantialInstitution()) {
            throw new TreasuryDomainException(
                    "error.PaymentPenaltyTaxTreasuryEvent.checkAndCreatePaymentPenaltyTax.finantialInstitution.does.not.match");
        }

        BigDecimal totalAmount = tariff.amountToPay();
        LocalDate dueDate = tariff.dueDate(lastPaymentDate);

        LocalizedString emolumentDescription = settings.buildEmolumentDescription(originDebitEntry);
        return new PaymentPenaltyEntryBean(originDebitEntry, emolumentDescription.getContent(TreasuryConstants.DEFAULT_LANGUAGE),
                dueDate, totalAmount);
    }

    public static DebitEntry checkAndCreatePaymentPenaltyTax(DebitEntry originDebitEntry, LocalDate lastPaymentDate,
            DebitNote debitNote, boolean preventSibsPaymentRequest) {
        return checkAndCreatePaymentPenaltyTax(originDebitEntry, lastPaymentDate, new LocalDate(), debitNote,
                preventSibsPaymentRequest);
    }

    public static DebitEntry checkAndCreatePaymentPenaltyTax(DebitEntry originDebitEntry, LocalDate lastPaymentDate,
            LocalDate whenDebtCreationDate, DebitNote debitNote, boolean preventSibsPaymentRequest) {

        if (!shouldPenaltyBeCreatedForDebitEntry(originDebitEntry, lastPaymentDate)) {
            return null;
        }

        PaymentPenaltyTaxSettings settings =
                PaymentPenaltyTaxSettings.findActiveForOriginDebitEntry(originDebitEntry).findFirst().orElse(null);

        // Find one which is not charged
        PaymentPenaltyTaxTreasuryEvent paymentPenaltyTaxTreasuryEvent =
                find(originDebitEntry).filter(t -> !t.isChargedWithDebitEntry()).findFirst().orElse(null);

        if (paymentPenaltyTaxTreasuryEvent == null) {
            // There is none, create new treasury event
            LocalizedString emolumentDescription = settings.buildEmolumentDescription(originDebitEntry);
            paymentPenaltyTaxTreasuryEvent = new PaymentPenaltyTaxTreasuryEvent(settings.getFinantialEntity(),
                    settings.getPenaltyProduct(), emolumentDescription, originDebitEntry);
        }

        Tariff tariff = null;
        if (originDebitEntry.getTreasuryEvent() != null) {
            tariff = originDebitEntry.getTreasuryEvent()
                    .findMatchTariff(settings.getFinantialEntity(), settings.getPenaltyProduct(), whenDebtCreationDate)
                    .orElse(null);
        }

        if (tariff == null) {
            // Fallback to tariff which is only associated with finantial entity, without degree, degreeType, etc...
            tariff = Tariff.find(settings.getPenaltyProduct(), whenDebtCreationDate.toDateTimeAtStartOfDay())
                    .filter(t -> t.getFinantialEntity() == settings.getFinantialEntity())
                    .filter(t -> t.isBroadTariffForFinantialEntity()).findFirst().orElse(null);
        }

        if (tariff == null) {
            throw new TreasuryDomainException(
                    "error.PaymentPenaltyTaxTreasuryEvent.checkAndCreatePaymentPenaltyTax.tariff.not.found");
        }

        if (tariff.getFinantialEntity().getFinantialInstitution() != originDebitEntry.getDebtAccount()
                .getFinantialInstitution()) {
            throw new TreasuryDomainException(
                    "error.PaymentPenaltyTaxTreasuryEvent.checkAndCreatePaymentPenaltyTax.finantialInstitution.does.not.match");
        }

        DebtAccount debtAccount = originDebitEntry.getDebtAccount();
        FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        if (debitNote == null) {
            DocumentNumberSeries documentNumberSeries = DocumentNumberSeries
                    .findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), tariff.getFinantialEntity());

            debitNote = DebitNote.create(settings.getFinantialEntity(), debtAccount, null, documentNumberSeries,
                    whenDebtCreationDate.toDateTimeAtStartOfDay(), whenDebtCreationDate, null, Collections.emptyMap(), null,
                    null);
        }

        BigDecimal totalAmount = tariff.amountToPay();
        LocalDate dueDate = tariff.dueDate(lastPaymentDate);

        var effectiveWhen = lastPaymentDate;
        if (DueDateCalculationType.FIXED_DATE == tariff.getDueDateCalculationType() && dueDate.isBefore(lastPaymentDate)) {
            effectiveWhen = dueDate;
        }

        Vat vat = Vat.findActiveUnique(settings.getPenaltyProduct().getVatType(), finantialInstitution,
                effectiveWhen.toDateTimeAtStartOfDay()).get();

        DebitEntry penaltyDebitEntry = DebitEntry.create(tariff.getFinantialEntity(), debtAccount, paymentPenaltyTaxTreasuryEvent,
                vat, totalAmount, dueDate, Collections.emptyMap(), settings.getPenaltyProduct(),
                paymentPenaltyTaxTreasuryEvent.getDescription().getContent(TreasuryConstants.DEFAULT_LANGUAGE), BigDecimal.ONE,
                tariff.getInterestRate(), effectiveWhen.toDateTimeAtStartOfDay(), false, false, debitNote);

        {
            Map<String, String> map = penaltyDebitEntry.getPropertiesMap();

            map.put("LAST_PAYMENT_DATE", lastPaymentDate.toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));
            map.put("ORIGIN_DEBIT_ENTRY_DUE_DATE",
                    originDebitEntry.getDueDate().toString(TreasuryConstants.DATE_FORMAT_YYYY_MM_DD));

            penaltyDebitEntry.editPropertiesMap(map);
        }

        if (!preventSibsPaymentRequest && Boolean.TRUE.equals(settings.getCreatePaymentCode())) {
            Set<DebitEntry> debitEntriesSet = new HashSet<>();
            debitEntriesSet.add(penaltyDebitEntry);

            if (Boolean.TRUE.equals(settings.getIncludePendingInterestsToPaymentCode())) {
                debitEntriesSet.addAll(originDebitEntry.getInterestDebitEntriesSet().stream().filter(d -> d.isInDebt())
                        .collect(Collectors.toSet()));
            }

            ISibsPaymentCodePoolService.getDefaultDigitalPaymentPlatform(tariff.getFinantialEntity())
                    .createSibsPaymentRequest(debtAccount, debitEntriesSet, Collections.emptySet());
        }

        return penaltyDebitEntry;
    }

    private static boolean shouldPenaltyBeCreatedForDebitEntry(DebitEntry originDebitEntry, LocalDate lastPaymentDate) {

        if (PaymentPenaltyTaxSettings.findActiveForOriginDebitEntry(originDebitEntry).count() > 1) {
            throw new TreasuryDomainException(
                    "error.PaymentPenaltyTaxTreasuryEvent.more.than.one.configuration.active.for.origin.debit.entry");
        }

        PaymentPenaltyTaxSettings settings =
                PaymentPenaltyTaxSettings.findActiveForOriginDebitEntry(originDebitEntry).findFirst().orElse(null);

        if (settings == null) {
            return false;
        }

        if (originDebitEntry.getOpenPaymentPlan() != null) {
            return false;
        }

        boolean hasSettlementInValidPaymentPlan =
                originDebitEntry.getSettlementEntriesSet().stream().filter(se -> !se.getFinantialDocument().isAnnulled())
                        .filter(se -> se.getSettlementNote().getPaymentDate().toLocalDate().isEqual(lastPaymentDate))
                        .flatMap(se -> se.getInstallmentSettlementEntriesSet().stream())
                        .filter(ise -> !ise.getInstallmentEntry().getInstallment().getDueDate().isBefore(lastPaymentDate))
                        .anyMatch(ise -> ise.getInstallmentEntry().getInstallment().getPaymentPlan()
                                .getState() != PaymentPlanStateType.ANNULED);

        if (hasSettlementInValidPaymentPlan) {
            return false;
        }

        if (originDebitEntry.getInterestRate() == null
                && !Boolean.TRUE.equals(settings.getApplyPenaltyOnDebitsWithoutInterest())) {
            return false;
        }

        if (!originDebitEntry.getDueDate().isBefore(lastPaymentDate)) {
            return false;
        }

        if (find(originDebitEntry).anyMatch(t -> t.isChargedWithDebitEntry())) {
            return false;
        }

        return true;
    }

    public static Set<DebitEntry> checkAndCreatePaymentPenaltyTaxesFromSettlementNote(SettlementNote settlementNote) {

        Set<DebitEntry> result = new HashSet<>();
        LocalDate now = new LocalDate();

        for (SettlementEntry s : settlementNote.getSettlemetEntries().collect(Collectors.toSet())) {
            InvoiceEntry invoiceEntry = s.getInvoiceEntry();

            if (!(invoiceEntry instanceof DebitEntry)) {
                continue;
            }

            final DebitEntry d = (DebitEntry) invoiceEntry;
            if (d.isInDebt()) {
                continue;
            }
            DateTime lastPaymentDate = d.getLastPaymentDate();

            if (lastPaymentDate == null) {
                continue;
            }

            if (isDebitEntrySettledWithOwnCredit(s, settlementNote)) {
                continue;
            }

            if (TreasuryDebtProcessMainService.isInterestCreationWhenTotalSettledPrevented(invoiceEntry)) {
                continue;
            }

            DebitEntry penaltyDebitEntry = checkAndCreatePaymentPenaltyTax(d, lastPaymentDate.toLocalDate(), now, null, false);
            if (penaltyDebitEntry == null) {
                result.add(penaltyDebitEntry);
            }

        }

        return result;
    }

    private static boolean isDebitEntrySettledWithOwnCredit(SettlementEntry debitEntrySettlementEntry,
            SettlementNote settlementNote) {
        return settlementNote.getSettlemetEntries().filter(s -> s != debitEntrySettlementEntry)
                .filter(s -> s.getInvoiceEntry().isCreditNoteEntry()).anyMatch(
                        s -> ((CreditEntry) s.getInvoiceEntry()).getDebitEntry() == debitEntrySettlementEntry.getInvoiceEntry());
    }

}
