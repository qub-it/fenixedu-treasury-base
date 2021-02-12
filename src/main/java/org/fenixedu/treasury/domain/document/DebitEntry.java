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

import static org.fenixedu.treasury.util.TreasuryConstants.rationalVatRate;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.event.TreasuryEvent.TreasuryEventKeys;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentEntry;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentSettlementEntry;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlan;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanSettings;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import pt.ist.fenixframework.Atomic;

public class DebitEntry extends DebitEntry_Base {

    public static final Comparator<DebitEntry> COMPARE_BY_OPEN_AMOUNT_WITH_VAT = (o1, o2) -> {
        final int c = o1.getAmountWithVat().compareTo(o2.getAmountWithVat());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<DebitEntry> COMPARE_BY_DUE_DATE = (o1, o2) -> {
        int c = o1.getDueDate().compareTo(o2.getDueDate());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<DebitEntry> COMPARE_BY_EVENT_ANNULED_AND_BY_DATE = (o1, o2) -> {

        if (!o1.isEventAnnuled() && o2.isEventAnnuled()) {
            return -1;
        } else if (o1.isEventAnnuled() && !o2.isEventAnnuled()) {
            return 1;
        }

        int c = o1.getEntryDateTime().compareTo(o2.getEntryDateTime());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<DebitEntry> COMPARE_BY_EVENT_ANNULED_AND_DUE_DATE = (o1, o2) -> {
        if (!o1.isEventAnnuled() && o2.isEventAnnuled()) {
            return 1;
        } else if (o1.isEventAnnuled() && !o2.isEventAnnuled()) {
            return -1;
        }

        int c = o1.getDueDate().compareTo(o2.getDueDate());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<DebitEntry> COMPARE_DEBIT_ENTRY_IN_SAME_PAYMENT_PLAN = (m1, m2) -> {
        boolean m1IsInterest = m1.getDebitEntry() != null;
        boolean m1IsEmolument = m1.getEmolumentPaymentPlan() != null && m1.getEmolumentPaymentPlan().getState().isOpen();

        boolean m2IsInterest = m2.getDebitEntry() != null;
        boolean m2IsEmolument = m2.getEmolumentPaymentPlan() != null && m2.getEmolumentPaymentPlan().getState().isOpen();

        if (m1IsInterest && m2IsInterest) {
            return (m1.getDebitEntry().getDueDate().compareTo(m2.getDebitEntry().getDueDate()) * 100)
                    + (m1.getDueDate().compareTo(m2.getDueDate()) * 10) + m1.getExternalId().compareTo(m2.getExternalId());
        }
        if (m1IsInterest && !m2IsInterest) {
            return -1;
        }
        if (m1IsEmolument && m2IsInterest) {
            return 1;
        }
        if (m1IsEmolument && m2IsEmolument) {
            return (m1.getDueDate().compareTo(m2.getDueDate()) * 10) + m1.getExternalId().compareTo(m2.getExternalId());
        }
        if (m1IsEmolument && !(m2IsInterest || m2IsEmolument)) {
            return -1;
        }
        if (m2IsInterest || m2IsEmolument) {
            return 1;
        }
        return (m1.getDueDate().compareTo(m2.getDueDate()) * 100) + (m1.getDescription().compareTo(m2.getDescription()) * 10)
                + m1.getExternalId().compareTo(m2.getExternalId());
    };

    public static Comparator<DebitEntry> COMPARE_BY_EXTERNAL_ID = (o1, o2) -> o1.getExternalId().compareTo(o2.getExternalId());

    protected DebitEntry(final DebitNote debitNote, final DebtAccount debtAccount, final TreasuryEvent treasuryEvent,
            final Vat vat, final BigDecimal amount, final LocalDate dueDate, final Map<String, String> propertiesMap,
            final Product product, final String description, final BigDecimal quantity, final InterestRate interestRate,
            final DateTime entryDateTime) {
        init(debitNote, debtAccount, treasuryEvent, product, vat, amount, dueDate, propertiesMap, description, quantity,
                interestRate, entryDateTime);
    }

    @Override
    public boolean isDebitNoteEntry() {
        return true;
    }

    public boolean isDeletable() {
        final Collection<String> blockers = Lists.newArrayList();

        checkForDeletionBlockers(blockers);

        return blockers.isEmpty();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        getInterestDebitEntriesSet().stream().forEach(ide -> ide.checkForDeletionBlockers(blockers));
        if (!getCreditEntriesSet().isEmpty()) {
            blockers.add(BundleUtil.getString(TreasuryConstants.BUNDLE, "error.DebitEntry.cannot.delete.has.creditentries"));
        }

    }

    @Override
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        if (getTreasuryExemption() != null) {
            getTreasuryExemption().delete();
        }

        if (this.getInterestRate() != null) {
            InterestRate oldRate = this.getInterestRate();
            this.setInterestRate(null);
            oldRate.delete();
        }
        this.setDebitEntry(null);
        this.setTreasuryEvent(null);

        this.getPaymentCodesSet().clear();

        super.delete();
    }

    @Override
    protected void init(final FinantialDocument finantialDocument, final DebtAccount debtAccount, final Product product,
            final FinantialEntryType finantialEntryType, final Vat vat, final BigDecimal amount, String description,
            BigDecimal quantity, DateTime entryDateTime) {
        throw new RuntimeException("error.CreditEntry.use.init.without.finantialEntryType");
    }

    protected void init(final DebitNote debitNote, final DebtAccount debtAccount, final TreasuryEvent treasuryEvent,
            final Product product, final Vat vat, final BigDecimal amount, final LocalDate dueDate,
            final Map<String, String> propertiesMap, final String description, final BigDecimal quantity,
            final InterestRate interestRate, final DateTime entryDateTime) {
        super.init(debitNote, debtAccount, product, FinantialEntryType.DEBIT_ENTRY, vat, amount, description, quantity,
                entryDateTime);

        setTreasuryEvent(treasuryEvent);
        setDueDate(dueDate);
        setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));
        setExemptedAmount(BigDecimal.ZERO);
        setInterestRate(interestRate);

        /*
         * This property has academic significance but is meaningless in treasury scope
         * It is false by default but can be set with
         * markAcademicalActBlockingSuspension service method
         */
        setAcademicalActBlockingSuspension(false);
        setBlockAcademicActsOnDebt(false);

        checkRules();
    }

    public InterestRateBean calculateAllInterestValue(final LocalDate whenToCalculate) {
        if (this.getInterestRate() == null) {
            return new InterestRateBean();
        }

        if (!toCalculateInterests(whenToCalculate)) {
            return new InterestRateBean();
        }

        return this.getInterestRate().calculateInterests(whenToCalculate, true);
    }

    public InterestRateBean calculateUndebitedInterestValue(final LocalDate whenToCalculate) {
        if (!this.isApplyInterests()) {
            return new InterestRateBean();
        }

        if (PaymentPlanSettings.getActiveInstance() != null
                && !PaymentPlanSettings.getActiveInstance().getInterestCalculationOfDebitsInPlans() && isInOpenPaymentPlan()
                && !getOpenPaymentPlan().isCompliant(whenToCalculate)) {
            return new InterestRateBean();
        }

        if (!toCalculateInterests(whenToCalculate)) {
            return new InterestRateBean();
        }

        InterestRateBean calculateInterest = getInterestRate().calculateInterests(whenToCalculate, false);

        calculateInterest.setDescription(treasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE,
                "label.InterestRateBean.interest.designation", getDescription()));

        return calculateInterest;
    }

    public boolean isApplyInterests() {
        return this.getInterestRate() != null;
    }

    private boolean toCalculateInterests(final LocalDate whenToCalculate) {
        return !whenToCalculate.isBefore(getDueDate().plusDays(getInterestRate().getNumberOfDaysAfterDueDate()));
    }

    @Override
    protected void checkRules() {
        super.checkRules();

        if (getFinantialDocument() != null && !(getFinantialDocument() instanceof DebitNote)) {
            throw new TreasuryDomainException("error.DebitEntry.finantialDocument.not.debit.entry.type");
        }

        if (getDebtAccount() == null) {
            throw new TreasuryDomainException("error.DebitEntry.debtAccount.required");
        }

        if (getDueDate() == null) {
            throw new TreasuryDomainException("error.DebitEntry.dueDate.required");
        }

        if (this.getEntryDateTime() != null && this.getDueDate().isBefore(this.getEntryDateTime().toLocalDate())) {
            throw new TreasuryDomainException("error.DebitEntry.dueDate.invalid");
        }

        if (Strings.isNullOrEmpty(getDescription())) {
            throw new TreasuryDomainException("error.DebitEntry.description.required");
        }

        // If it exempted then it must be on itself or with credit entry but not both
        if (isPositive(getExemptedAmount())
                && CreditEntry.findActive(getTreasuryEvent(), getProduct()).filter(c -> c.getDebitEntry() == this).count() > 0) {
            throw new TreasuryDomainException(
                    "error.DebitEntry.exemption.cannot.be.on.debit.entry.and.with.credit.entry.at.same.time");
        }

        if (getTreasuryEvent() != null && getProduct().isTransferBalanceProduct()) {
            throw new TreasuryDomainException("error.DebitEntry.transferBalanceProduct.cannot.be.associated.to.academic.event");
        }

        if (isBlockAcademicActsOnDebt() && isAcademicalActBlockingSuspension()) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.suspend.and.also.block.academical.acts.on.debt");
        }

    }

    @Override
    public boolean isFinantialDocumentRequired() {
        return false;
    }

    public boolean isEventAnnuled() {
        return isAnnulled() || getEventAnnuled();
    }

    public boolean isIncludedInEvent() {
        return !isEventAnnuled();
    }

    public BigDecimal getPendingInterestAmount() {
        return getPendingInterestAmount(new LocalDate());
    }

    public BigDecimal getPendingInterestAmount(LocalDate whenToCalculate) {
        return calculateUndebitedInterestValue(whenToCalculate).getInterestAmount();
    }

    public boolean isInDebt() {
        return TreasuryConstants.isPositive(getOpenAmount());
    }

    public boolean isDueDateExpired(final LocalDate when) {
        return getDueDate().isBefore(when);
    }

    @Atomic
    public DebitEntry createInterestRateDebitEntry(final InterestRateBean interest, final DateTime when,
            final Optional<DebitNote> debitNote) {
        Product product = TreasurySettings.getInstance().getInterestProduct();

        if (product == null) {
            throw new TreasuryDomainException("error.SettlementNote.need.interest.product");
        }

        FinantialInstitution finantialInstitution = this.getDebtAccount().getFinantialInstitution();
        Vat vat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, when).orElse(null);

        // entry description for Interest Entry
        String entryDescription = interest.getDescription();
        if (Strings.isNullOrEmpty(entryDescription)) {
            // default entryDescription
            entryDescription = product.getName().getContent() + "-" + this.getDescription();
        }

        DebitEntry interestEntry = _create(debitNote, getDebtAccount(), getTreasuryEvent(), vat, interest.getInterestAmount(),
                when.toLocalDate(), TreasuryConstants.propertiesJsonToMap(getPropertiesJsonMap()), product, entryDescription,
                BigDecimal.ONE, null, when);

        addInterestDebitEntries(interestEntry);

        return interestEntry;
    }

    public void edit(final String description, final TreasuryEvent treasuryEvent, LocalDate dueDate,
            final boolean academicalActBlockingSuspension, final boolean blockAcademicActsOnDebt) {

        this.setDescription(description);
        this.setTreasuryEvent(treasuryEvent);
        this.setDueDate(dueDate);

        this.setAcademicalActBlockingSuspension(academicalActBlockingSuspension);
        this.setBlockAcademicActsOnDebt(blockAcademicActsOnDebt);

        checkRules();
    }

    public boolean isAcademicalActBlockingSuspension() {
        return getAcademicalActBlockingSuspension();
    }

    public boolean isBlockAcademicActsOnDebt() {
        return getBlockAcademicActsOnDebt();
    }

    public boolean exempt(final TreasuryExemption treasuryExemption, final BigDecimal amountWithVat) {
        if (treasuryExemption.getTreasuryEvent() != getTreasuryEvent()) {
            throw new RuntimeException("wrong call");
        }

        if (treasuryExemption.getProduct() != getProduct()) {
            throw new RuntimeException("wrong call");
        }

        if (isEventAnnuled()) {
            throw new RuntimeException("error.DebitEntry.is.event.annuled.cannot.be.exempted");
        }

        final BigDecimal amountWithoutVat = TreasuryConstants.divide(amountWithVat, BigDecimal.ONE.add(rationalVatRate(this)));

        if (isProcessedInClosedDebitNote()) {
            // If there is at least one credit entry then skip...
            if (!getCreditEntriesSet().isEmpty()) {
                return false;
            }

            final DateTime now = new DateTime();

            final String reason = treasuryBundle("label.TreasuryExemption.credit.entry.exemption.description", getDescription(),
                    treasuryExemption.getTreasuryExemptionType().getName().getContent());

            final CreditEntry creditEntryFromExemption =
                    createCreditEntry(now, getDescription(), null, amountWithoutVat, treasuryExemption, null);

            closeCreditEntryIfPossible(reason, now, creditEntryFromExemption);

        } else {
            BigDecimal originalAmount = getAmount();
            if (TreasuryConstants.isPositive(getExemptedAmount())) {
                originalAmount = originalAmount.add(getExemptedAmount());
                setExemptedAmount(BigDecimal.ZERO);
            }

            setAmount(originalAmount.subtract(amountWithoutVat));
            setExemptedAmount(amountWithoutVat);

            recalculateAmountValues();

            if (getTreasuryEvent() != null) {
                getTreasuryEvent().invokeSettlementCallbacks();
            }

        }

        checkRules();

        return true;
    }

    public CreditEntry createCreditEntry(final DateTime documentDate, final String description, final String documentObservations,
            final BigDecimal amountForCreditWithoutVat, final TreasuryExemption treasuryExemption, CreditNote creditNote) {
        final DebitNote finantialDocument = (DebitNote) this.getFinantialDocument();

        if (finantialDocument == null) {
            throw new TreasuryDomainException("error.DebitEntry.createCreditEntry.requires.finantial.document");
        }

        final DocumentNumberSeries documentNumberSeries = DocumentNumberSeries.find(FinantialDocumentType.findForCreditNote(),
                finantialDocument.getDocumentNumberSeries().getSeries());

        if (creditNote == null) {
            creditNote = CreditNote.create(this.getDebtAccount(), documentNumberSeries, documentDate, finantialDocument,
                    finantialDocument.getUiDocumentNumber());
        }

        if (!Strings.isNullOrEmpty(documentObservations)) {
            creditNote.setDocumentObservations(documentObservations);
        }

        if (!TreasuryConstants.isPositive(amountForCreditWithoutVat)) {
            throw new TreasuryDomainException("error.DebitEntry.createCreditEntry.amountForCredit.not.positive");
        }

        if (treasuryExemption != null) {
            return CreditEntry.createFromExemption(treasuryExemption, creditNote, description, amountForCreditWithoutVat,
                    new DateTime(), this);
        } else {
            return CreditEntry.create(creditNote, description, getProduct(), getVat(), amountForCreditWithoutVat, documentDate,
                    this, BigDecimal.ONE);
        }
    }

    public void closeCreditEntryIfPossible(final String reason, final DateTime now, final CreditEntry creditEntry) {
        final DocumentNumberSeries documentNumberSeriesSettlementNote = DocumentNumberSeries.find(
                FinantialDocumentType.findForSettlementNote(), this.getFinantialDocument().getDocumentNumberSeries().getSeries());

        if (!creditEntry.getFinantialDocument().isPreparing()) {
            return;
        }

        if (!TreasuryConstants.isPositive(getOpenAmount())) {
            return;
        }

        if (TreasuryConstants.isLessThan(getOpenAmount(), creditEntry.getOpenAmount())) {
            // split credit entry
            creditEntry.splitCreditEntry(creditEntry.getOpenAmount().subtract(getOpenAmount()));
        }

        creditEntry.getFinantialDocument().closeDocument();

        final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

        final String reasonDescription =
                treasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE, "label.TreasuryEvent.credit.by.annulAllDebitEntries.reason");

        final SettlementNote settlementNote =
                SettlementNote.create(this.getDebtAccount(), documentNumberSeriesSettlementNote, now, now, "", null);
        settlementNote
                .setDocumentObservations(reason + " - [" + loggedUsername + "] " + new DateTime().toString("YYYY-MM-dd HH:mm"));

        SettlementEntry.create(creditEntry, settlementNote, creditEntry.getOpenAmount(),
                reasonDescription + ": " + creditEntry.getDescription(), now, false);
        SettlementEntry debitSettlementEntry = SettlementEntry.create(this, settlementNote, creditEntry.getOpenAmount(), reasonDescription + ": " + getDescription(),
                now, false);
        
        InstallmentSettlementEntry.settleInstallmentEntriesOfDebitEntry(debitSettlementEntry)
        
        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()
                && getFinantialDocument().isExportedInLegacyERP() != creditEntry.getFinantialDocument().isExportedInLegacyERP()) {
            throw new TreasuryDomainException("error.DebitEntry.closeCreditEntryIfPossible.exportedInLegacyERP.not.same");
        }

        if (((Invoice) getFinantialDocument()).getPayorDebtAccount() != ((Invoice) getFinantialDocument())
                .getPayorDebtAccount()) {
            throw new TreasuryDomainException("error.DebitEntry.closeCreditEntryIfPossible.payorDebtAccount.not.same");
        }

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()
                && getFinantialDocument().isExportedInLegacyERP()) {
            settlementNote.setExportedInLegacyERP(true);
            settlementNote.setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
        }

        settlementNote.closeDocument();
    }

    public boolean isExportedInERPAndInRestrictedPaymentMixingLegacyInvoices() {
        return TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices() && getFinantialDocument() != null
                && getFinantialDocument().isExportedInLegacyERP();
    }

    public BigDecimal getAmountInDebt(final LocalDate paymentDate) {
        final Set<SettlementEntry> entries = new TreeSet<SettlementEntry>(SettlementEntry.COMPARATOR_BY_ENTRY_DATE_TIME);

        entries.addAll(getSettlementEntriesSet());

        BigDecimal amountToPay = getAmountWithVat();
        for (final SettlementEntry settlementEntry : entries) {
            if (!settlementEntry.isAnnulled()) {
                if (settlementEntry.getEntryDateTime().toLocalDate().isAfter(paymentDate)) {
                    break;
                }

                amountToPay = amountToPay.subtract(settlementEntry.getAmount());
            }
        }

        return amountToPay;
    }

    public boolean revertExemptionIfPossible(final TreasuryExemption treasuryExemption) {
        if (isAnnulled()) {
            return false;
        }

        if (isProcessedInClosedDebitNote()) {
            return false;
        }

        if (!treasuryExemption.getDebitEntry().getCreditEntriesSet().isEmpty()) {
            return false;
        }

        setAmount(getAmount().add(getExemptedAmount()));
        setExemptedAmount(BigDecimal.ZERO);

        recalculateAmountValues();

        checkRules();

        return true;
    }

    @Atomic
    public void markAcademicalActBlockingSuspension() {
        setAcademicalActBlockingSuspension(true);
    }

    @Atomic
    public void markBlockAcademicActsOnDebt() {
        setBlockAcademicActsOnDebt(true);
    }

    @Atomic
    public void annulOnEvent() {
        setEventAnnuled(true);
    }

    @Atomic
    public void revertEventAnnuled() {
        setEventAnnuled(false);
    }

    public DateTime getLastSettlementDate() {
        Optional<SettlementNote> settlementNote = getSettlementEntriesSet().stream()
                .filter(s -> !s.getFinantialDocument().isAnnulled()).map(s -> ((SettlementNote) s.getFinantialDocument()))
                .max(Comparator.comparing(SettlementNote::getPaymentDate));
        if (!settlementNote.isPresent()) {
            return null;
        }

        return settlementNote.get().getPaymentDate();
    }

    public BigDecimal getExemptedAmountWithVat() {
        return getExemptedAmount().multiply(BigDecimal.ONE.add(rationalVatRate(this)));
    }

    /**
     * Differs from getLastSettlementDate in obtaining payment date only from
     * settlement notes with payment entries
     *
     * @return
     */

    public DateTime getLastPaymentDate() {
        Optional<SettlementNote> settlementNote = getSettlementEntriesSet().stream()
                .filter(s -> !s.getFinantialDocument().isAnnulled()
                        && !((SettlementNote) s.getFinantialDocument()).getPaymentEntriesSet().isEmpty())
                .map(s -> ((SettlementNote) s.getFinantialDocument())).max(Comparator.comparing(SettlementNote::getPaymentDate));

        if (!settlementNote.isPresent()) {
            return null;
        }

        return settlementNote.get().getPaymentDate();
    }

    public void editAmount(final BigDecimal amount) {
        if (isProcessedInClosedDebitNote()) {
            throw new TreasuryDomainException("error.DebitEntry.editAmount.cannot.edit.amount.due.to.closed.in.debit.note");
        }

        if (isAnnulled()) {
            throw new TreasuryDomainException("error.DebitEntry.editAmount.cannot.edit.amount.due.to.annuled.state");
        }

        setAmount(amount);
        recalculateAmountValues();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static DebitEntry copyDebitEntry(final DebitEntry debitEntryToCopy, final DebitNote debitNoteToAssociate,
            final boolean applyExemption) {

        final Map<String, String> propertiesMap = Maps.newHashMap(
                debitEntryToCopy.getPropertiesMap() != null ? debitEntryToCopy.getPropertiesMap() : Maps.newHashMap());
        propertiesMap.put(TreasuryEvent.TreasuryEventKeys.COPIED_FROM_DEBIT_ENTRY_ID.getDescriptionI18N()
                .getContent(TreasuryConstants.DEFAULT_LANGUAGE), debitEntryToCopy.getExternalId());
        propertiesMap.put(
                TreasuryEvent.TreasuryEventKeys.COPY_DEBIT_ENTRY_RESPONSIBLE.getDescriptionI18N()
                        .getContent(TreasuryConstants.DEFAULT_LANGUAGE),
                Authenticate.getUser() != null ? Authenticate.getUser().getUsername() : "");

        final DebitEntry result = DebitEntry.create(Optional.ofNullable(debitNoteToAssociate), debitEntryToCopy.getDebtAccount(),
                debitEntryToCopy.getTreasuryEvent(), debitEntryToCopy.getVat(),
                debitEntryToCopy.getAmount().add(debitEntryToCopy.getExemptedAmount()), debitEntryToCopy.getDueDate(),
                propertiesMap, debitEntryToCopy.getProduct(), debitEntryToCopy.getDescription(), debitEntryToCopy.getQuantity(),
                debitEntryToCopy.getInterestRate(), debitEntryToCopy.getEntryDateTime());

        result.edit(result.getDescription(), result.getTreasuryEvent(), result.getDueDate(),
                debitEntryToCopy.getAcademicalActBlockingSuspension(), debitEntryToCopy.getBlockAcademicActsOnDebt());

        // We could copy eventAnnuled property, but in most cases we want to create an
        // active debit entry
        result.setEventAnnuled(false);

        // Interest relation must be done outside because the origin
        // debit entry of debitEntryToCopy will may be annuled
        // result.setDebitEntry(debitEntryToCopy.getDebitEntry());

        result.setPayorDebtAccount(debitEntryToCopy.getPayorDebtAccount());

        if (applyExemption && debitEntryToCopy.getTreasuryExemption() != null) {
            final TreasuryExemption treasuryExemptionToCopy = debitEntryToCopy.getTreasuryExemption();
            TreasuryExemption.create(treasuryExemptionToCopy.getTreasuryExemptionType(),
                    treasuryExemptionToCopy.getTreasuryEvent(), treasuryExemptionToCopy.getReason(),
                    treasuryExemptionToCopy.getValueToExempt(), result);
        }

        return result;
    }

    public static Stream<? extends DebitEntry> findAll() {
        return FinantialDocumentEntry.findAll().filter(f -> f instanceof DebitEntry).map(DebitEntry.class::cast);
    }

    public static Stream<? extends DebitEntry> find(final Customer customer) {
        return customer.getDebtAccountsSet().stream().flatMap(d -> find(d));
    }

    public static Stream<? extends DebitEntry> find(final DebtAccount debtAccount) {
        return debtAccount.getInvoiceEntrySet().stream().filter(i -> i.isDebitNoteEntry()).map(DebitEntry.class::cast);
    }

    public static Stream<? extends DebitEntry> find(final DebitNote debitNote) {
        return debitNote.getFinantialDocumentEntriesSet().stream().filter(f -> f instanceof DebitEntry)
                .map(DebitEntry.class::cast);
    }

    public static Stream<? extends DebitEntry> find(final TreasuryEvent treasuryEvent) {
        return treasuryEvent.getDebitEntriesSet().stream();
    }

    public static Stream<? extends DebitEntry> findActive(final DebtAccount debtAccount, final Product product) {
        return find(debtAccount).filter(d -> d.getProduct() == product && !d.isEventAnnuled());
    }

    public static Stream<? extends DebitEntry> findActive(final TreasuryEvent treasuryEvent) {
        return find(treasuryEvent).filter(d -> !d.isEventAnnuled());
    }

    public static Stream<? extends DebitEntry> findActive(final TreasuryEvent treasuryEvent, final Product product) {
        return findActive(treasuryEvent).filter(d -> d.getProduct() == product);
    }

    public static Stream<? extends DebitEntry> findActiveByDescription(final TreasuryEvent treasuryEvent,
            final String description, final boolean trimmed) {
        return findActive(treasuryEvent).filter(d -> (!trimmed && d.getDescription().equals(description))
                || (trimmed && d.getDescription().trim().equals(description)));
    }

    public static Stream<? extends DebitEntry> findEventAnnuled(final TreasuryEvent treasuryEvent) {
        return find(treasuryEvent).filter(d -> d.isEventAnnuled());
    }

    public static Stream<? extends DebitEntry> findEventAnnuled(final TreasuryEvent treasuryEvent, final Product product) {
        return findEventAnnuled(treasuryEvent).filter(d -> d.getProduct() == product);
    }

    public static BigDecimal payedAmount(final TreasuryEvent treasuryEvent) {
        return findActive(treasuryEvent).map(d -> d.getPayedAmount()).reduce((x, y) -> x.add(y)).orElse(BigDecimal.ZERO);
    }

    public static BigDecimal remainingAmountToPay(final TreasuryEvent treasuryEvent) {
        return findActive(treasuryEvent).map(d -> d.getOpenAmount()).reduce((x, y) -> x.add(y)).orElse(BigDecimal.ZERO);
    }

    public static DebitEntry create(Optional<DebitNote> debitNote, DebtAccount debtAccount, TreasuryEvent treasuryEvent, Vat vat,
            BigDecimal amount, LocalDate dueDate, Map<String, String> propertiesMap, Product product, String description,
            BigDecimal quantity, InterestRate interestRate, DateTime entryDateTime) {

        if (!isDebitEntryCreationAllowed(debtAccount, debitNote, product)) {
            throw new TreasuryDomainException("error.DebitEntry.customer.not.active");
        }

        return _create(debitNote, debtAccount, treasuryEvent, vat, amount, dueDate, propertiesMap, product, description, quantity,
                interestRate, entryDateTime);
    }

    public static DebitEntry createForImportationPurpose(Optional<DebitNote> debitNote, DebtAccount debtAccount,
            TreasuryEvent treasuryEvent, Vat vat, BigDecimal amount, LocalDate dueDate, Map<String, String> propertiesMap,
            Product product, String description, BigDecimal quantity, InterestRate interestRate, DateTime entryDateTime) {

        return _create(debitNote, debtAccount, treasuryEvent, vat, amount, dueDate, propertiesMap, product, description, quantity,
                interestRate, entryDateTime);
    }

    private static boolean isDebitEntryCreationAllowed(final DebtAccount debtAccount, Optional<DebitNote> debitNote,
            Product product) {
        if (debtAccount.getCustomer().isActive()) {
            return true;
        }

        if (debitNote.isPresent() && debitNote.get().getDocumentNumberSeries().getSeries().isRegulationSeries()) {
            return true;
        }

        return false;
    }

    private static DebitEntry _create(final Optional<DebitNote> debitNote, final DebtAccount debtAccount,
            final TreasuryEvent treasuryEvent, final Vat vat, final BigDecimal amount, final LocalDate dueDate,
            final Map<String, String> propertiesMap, final Product product, final String description, final BigDecimal quantity,
            final InterestRate interestRate, final DateTime entryDateTime) {

        final DebitEntry entry = new DebitEntry(debitNote.orElse(null), debtAccount, treasuryEvent, vat, amount, dueDate,
                propertiesMap, product, description, quantity, null, entryDateTime);

        if (interestRate != null) {
            InterestRate.createForDebitEntry(entry, interestRate);
        }

        entry.recalculateAmountValues();
        return entry;
    }

    public void changeInterestRate(InterestRate oldInterestRate) {
        if (this.getInterestRate() != null && this.getInterestRate() != oldInterestRate) {
            oldInterestRate.delete();
        }

        checkRules();
    }

    public BigDecimal getTotalCreditedAmount() {
        BigDecimal totalCreditedAmount = BigDecimal.ZERO;
        for (CreditEntry credits : this.getCreditEntriesSet()) {
            if (credits.getFinantialDocument() == null || !credits.getFinantialDocument().isAnnulled()) {
                totalCreditedAmount = totalCreditedAmount.add(credits.getTotalAmount());
            }
        }
        return this.getCurrency().getValueWithScale(totalCreditedAmount);
    }

    public BigDecimal getAvailableAmountForCredit() {
        return this.getCurrency().getValueWithScale(this.getTotalAmount().subtract(getTotalCreditedAmount()));
    }

    @Override
    public BigDecimal getOpenAmountWithInterests() {
        if (isAnnulled()) {
            return BigDecimal.ZERO;
        }

        if (TreasuryConstants.isEqual(getOpenAmount(), BigDecimal.ZERO)) {
            return getOpenAmount();
        } else {
            return getOpenAmount().add(getPendingInterestAmount());
        }
    }

    @Atomic
    public void clearInterestRate() {
        if (this.getInterestRate() != null) {
            this.getInterestRate().delete();
        }
    }

    public String getERPIntegrationMetadata() {
        String degreeCode = getDegreeCode();
        String executionYear = getExecutionYearName();

        return "{\"" + TreasuryEventKeys.DEGREE_CODE + "\":\"" + degreeCode + "\",\"" + TreasuryEventKeys.EXECUTION_YEAR + "\":\""
                + executionYear + "\"}";
    }

    public String getExecutionYearName() {
        String executionYear = "";
        if (getTreasuryEvent() != null && !Strings.isNullOrEmpty(getTreasuryEvent().getExecutionYearName())) {
            executionYear = getTreasuryEvent().getExecutionYearName();
        } else if (getPropertiesMap() != null) {
            if (getPropertiesMap().containsKey(TreasuryEventKeys.EXECUTION_YEAR)) {
                executionYear = getPropertiesMap().get(TreasuryEventKeys.EXECUTION_YEAR);
            } else if (getPropertiesMap().containsKey(TreasuryEventKeys.EXECUTION_YEAR.getDescriptionI18N().getContent())) {
                executionYear = getPropertiesMap().get(TreasuryEventKeys.EXECUTION_YEAR.getDescriptionI18N().getContent());
            }
        }
        return executionYear;
    }

    public String getDegreeCode() {
        String degreeCode = "";
        if (getTreasuryEvent() != null && !Strings.isNullOrEmpty(getTreasuryEvent().getDegreeCode())) {
            degreeCode = getTreasuryEvent().getDegreeCode();
        } else if (getPropertiesMap() != null) {
            if (getPropertiesMap().containsKey(TreasuryEventKeys.DEGREE_CODE)) {
                degreeCode = getPropertiesMap().get(TreasuryEventKeys.DEGREE_CODE);
            } else if (getPropertiesMap().containsKey(TreasuryEventKeys.DEGREE_CODE.getDescriptionI18N().getContent())) {
                degreeCode = getPropertiesMap().get(TreasuryEventKeys.DEGREE_CODE.getDescriptionI18N().getContent());
            }
        }
        return degreeCode;
    }

    public DebitNote getDebitNote() {
        return (DebitNote) getFinantialDocument();
    }

    @Atomic
    public void annulDebitEntry(final String reason) {
        if (isAnnulled()) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.is.already.annuled");
        }

        if (getFinantialDocument() != null) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.with.finantial.document");
        }

        if (Strings.isNullOrEmpty(reason)) {
            throw new TreasuryDomainException("error.DebitEntry.annul.debit.entry,requires.reason");
        }

        final DebitNote debitNote = DebitNote.create(getDebtAccount(), DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForDebitNote(), getDebtAccount().getFinantialInstitution()).get(),
                new DateTime());

        setFinantialDocument(debitNote);

        debitNote.anullDebitNoteWithCreditNote(reason, false);
    }

    @Atomic
    public void creditDebitEntry(BigDecimal amountToCreditWithVat, String reason, boolean closeWithOtherDebitEntriesOfDebitNote) {

        if (isAnnulled()) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.credit.is.already.annuled");
        }

        if (getFinantialDocument() == null || getFinantialDocument().isPreparing()) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.credit.without.or.preparing.finantial.document");
        }

        if (Strings.isNullOrEmpty(reason)) {
            throw new TreasuryDomainException("error.DebitEntry.credit.debit.entry.requires.reason");
        }

        if (!TreasuryConstants.isPositive(amountToCreditWithVat)) {
            throw new TreasuryDomainException("error.DebitEntry.credit.debit.entry.amountToCreditWithVat.must.be.positive");
        }

        if (!TreasuryConstants.isLessOrEqualThan(amountToCreditWithVat, getAvailableAmountForCredit())) {
            throw new TreasuryDomainException(
                    "error.DebitEntry.credit.debit.entry.amountToCreditWithVat.must.be.less.or.equal.than.amountAvailableForCredit");
        }

        final BigDecimal amountForCreditWithoutVat =
                TreasuryConstants.divide(amountToCreditWithVat, BigDecimal.ONE.add(rationalVatRate(this)));;

        final DateTime now = new DateTime();
        final CreditEntry creditEntry = createCreditEntry(now, getDescription(), null, amountForCreditWithoutVat, null, null);

        // Close creditEntry with debitEntry if it is possible
        closeCreditEntryIfPossible(reason, now, creditEntry);

        if (closeWithOtherDebitEntriesOfDebitNote) {
            // With the remaining credit amount, close with other debit entries of same
            // debit note
            final Supplier<Boolean> openCreditEntriesExistsFunc =
                    () -> getCreditEntriesSet().stream().filter(c -> TreasuryConstants.isPositive(c.getOpenAmount())).count() > 0;
            final Supplier<Boolean> openDebitEntriesExistsFunc = () -> getDebitNote().getDebitEntriesSet().stream()
                    .filter(d -> TreasuryConstants.isPositive(d.getOpenAmount())).count() > 0;

            while (openCreditEntriesExistsFunc.get() && openDebitEntriesExistsFunc.get()) {
                final CreditEntry openCreditEntry = getCreditEntriesSet().stream()
                        .filter(c -> TreasuryConstants.isPositive(c.getOpenAmount())).findFirst().get();

                // Find debit entry with open amount equal or higher than the open credit
                DebitEntry openDebitEntry = getDebitNote().getDebitEntriesSet().stream()
                        .filter(d -> TreasuryConstants.isPositive(d.getOpenAmount()))
                        .filter(d -> TreasuryConstants.isGreaterOrEqualThan(d.getOpenAmount(), openCreditEntry.getOpenAmount()))
                        .findFirst().orElse(null);

                if (openDebitEntry == null) {
                    openDebitEntry = getDebitNote().getDebitEntriesSet().stream()
                            .filter(d -> TreasuryConstants.isPositive(d.getOpenAmount())).findFirst().orElse(null);
                }

                openDebitEntry.closeCreditEntryIfPossible(reason, now, openCreditEntry);
            }
        }
    }

    @Atomic
    public void removeFromDocument() {
        if (getFinantialDocument() == null || !getFinantialDocument().isPreparing()) {
            throw new TreasuryDomainException("error.DebitEntry.removeFromDocument.invalid.state");
        }

        setFinantialDocument(null);
    }

    public boolean isInOpenPaymentPlan() {
        return getOpenPaymentPlan() != null;
    }

    public PaymentPlan getOpenPaymentPlan() {
        if (getEmolumentPaymentPlan() != null && getEmolumentPaymentPlan().getState().isOpen()) {
            return getEmolumentPaymentPlan();
        }
        return getInstallmentEntrySet().stream().filter(i -> i.getInstallment().getPaymentPlan().getState().isOpen())
                .map(inst -> inst.getInstallment().getPaymentPlan()).findFirst().orElse(null);

    }

    /**
     * Return all installments of open payment plans
     * @return
     */
    // TODO Rename method imply the returning value installments entries that are not paid, of open payment plan
    public List<InstallmentEntry> getSortedOpenInstallmentEntries() {
        return getInstallmentEntrySet().stream().filter(i -> i.getInstallment().getPaymentPlan().getState().isOpen())
                .sorted(InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR).collect(Collectors.toList());
    }
}
