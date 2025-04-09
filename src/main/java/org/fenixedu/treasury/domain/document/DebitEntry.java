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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.log.DebitEntryChangeAmountsLog;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.event.TreasuryEvent.TreasuryEventKeys;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPayment;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentEntry;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentSettlementEntry;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlan;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;
//import sun.text.normalizer.ICUBinary.Authenticate;

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
            return (m1.getDebitEntry().getDueDate().compareTo(m2.getDebitEntry().getDueDate()) * 100) + (m1.getDueDate()
                    .compareTo(m2.getDueDate()) * 10) + m1.getExternalId().compareTo(m2.getExternalId());
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
        return (m1.getDueDate().compareTo(m2.getDueDate()) * 100) + (m1.getDescription()
                .compareTo(m2.getDescription()) * 10) + m1.getExternalId().compareTo(m2.getExternalId());
    };

    public static Comparator<DebitEntry> COMPARE_BY_EXTERNAL_ID = (o1, o2) -> o1.getExternalId().compareTo(o2.getExternalId());

    protected DebitEntry(FinantialEntity finantialEntity, DebtAccount debtAccount, TreasuryEvent treasuryEvent, Vat vat,
            BigDecimal amount, LocalDate dueDate, Map<String, String> propertiesMap, Product product, String description,
            BigDecimal quantity, InterestRate interestRate, DateTime entryDateTime, DebitNote debitNote) {
        init(finantialEntity, debtAccount, treasuryEvent, product, vat, amount, dueDate, propertiesMap, description, quantity,
                interestRate, entryDateTime, debitNote);

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

    public Set<DebitEntry> getInterestDebitEntriesIncludingFromSplittingOriginDebitEntriesAncestorsSet() {
        Set<DebitEntry> result = new HashSet<>(getInterestDebitEntriesSet());

        if (getSplittingOriginDebitEntry() != null) {
            result.addAll(
                    getSplittingOriginDebitEntry().getInterestDebitEntriesIncludingFromSplittingOriginDebitEntriesAncestorsSet());
        }

        return result;
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        getInterestDebitEntriesSet().stream().forEach(ide -> ide.checkForDeletionBlockers(blockers));
        if (!getCreditEntriesSet().isEmpty()) {
            blockers.add(TreasuryConstants.treasuryBundle("error.DebitEntry.cannot.delete.has.creditentries"));
        }

    }

    @Override
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        if (!getTreasuryExemptionsSet().isEmpty()) {
            getTreasuryExemptionsSet().forEach(exemption -> {
                // This is to avoid calling delete directly
                exemption.setDebitEntry(null);
                exemption.delete();
            });
        }

        if (this.getInterestRate() != null) {
            InterestRate oldRate = this.getInterestRate();
            this.setInterestRate(null);
            oldRate.delete();
        }

        getDebitEntryChangeAmountsLogsSet().forEach(log -> log.setDebitEntry(null));

        this.setSplittingOriginDebitEntry(null);
        this.setDebitEntry(null);
        this.setTreasuryEvent(null);

        this.getPaymentCodesSet().clear();

        super.delete();
    }

    @Override
    protected void init(FinantialEntity finantialEntity, final FinantialDocument finantialDocument, final DebtAccount debtAccount,
            final Product product, final FinantialEntryType finantialEntryType, final Vat vat, final BigDecimal amount,
            String description, BigDecimal quantity, DateTime entryDateTime) {
        throw new RuntimeException("error.CreditEntry.use.init.without.finantialEntryType");
    }

    protected void init(FinantialEntity finantialEntity, final DebtAccount debtAccount, final TreasuryEvent treasuryEvent,
            final Product product, final Vat vat, final BigDecimal amount, final LocalDate dueDate,
            final Map<String, String> propertiesMap, final String description, final BigDecimal quantity,
            final InterestRate interestRate, final DateTime entryDateTime, final DebitNote debitNote) {
        super.init(finantialEntity, debitNote, debtAccount, product, FinantialEntryType.DEBIT_ENTRY, vat, amount, description,
                quantity, entryDateTime);

        setTreasuryEvent(treasuryEvent);
        setDueDate(dueDate);
        setPropertiesJsonMap(TreasuryConstants.propertiesMapToJson(propertiesMap));
        setInterestRate(interestRate);

        /*
         * This property has academic significance but is meaningless in treasury scope
         * It is false by default but can be set with
         * markAcademicalActBlockingSuspension service method
         */
        setAcademicalActBlockingSuspension(false);
        setBlockAcademicActsOnDebt(false);

        recalculateAmountValues();

        checkRules();
    }

    public List<InterestRateBean> calculateAllInterestValue(final LocalDate whenToCalculate) {
        if (this.getInterestRate() == null) {
            return Collections.emptyList();
        }

        if (!this.isApplyInterests()) {
            return Collections.emptyList();
        }

        if (!toCalculateInterests(whenToCalculate)) {
            return Collections.emptyList();
        }

        return this.getInterestRate().calculateInterests(whenToCalculate, true);
    }

    public List<InterestRateBean> calculateAllInterestsByLockingAtDate(LocalDate lockDate) {
        if (this.getInterestRate() == null) {
            return Collections.emptyList();
        }

        if (!this.isApplyInterests()) {
            return Collections.emptyList();
        }

        if (!toCalculateInterests(lockDate)) {
            return Collections.emptyList();
        }

        return this.getInterestRate().calculateAllInterestsByLockingAtDate(lockDate);
    }

    public List<InterestRateBean> calculateUndebitedInterestValue(final LocalDate whenToCalculate) {
        if (!this.isApplyInterests()) {
            return Collections.emptyList();
        }

        if (isInOpenPaymentPlan() && getOpenPaymentPlan().isCompliant(whenToCalculate)) {
            return Collections.emptyList();
        }

        if (!toCalculateInterests(whenToCalculate)) {
            return Collections.emptyList();
        }

        List<InterestRateBean> interestRateBeansList = getInterestRate().calculateInterests(whenToCalculate, false);

        return interestRateBeansList;
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
        return calculateUndebitedInterestValue(whenToCalculate).stream().map(bean -> bean.getInterestAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public boolean isInDebt() {
        return TreasuryConstants.isPositive(getOpenAmount());
    }

    public boolean isDueDateExpired(final LocalDate when) {
        return getDueDate().isBefore(when);
    }

    @Atomic
    public DebitEntry createInterestRateDebitEntry(final InterestRateBean interest, final DateTime when, DebitNote debitNote) {
        Product product = TreasurySettings.getInstance().getInterestProduct();

        if (product == null) {
            throw new TreasuryDomainException("error.SettlementNote.need.interest.product");
        }

        FinantialInstitution finantialInstitution = this.getDebtAccount().getFinantialInstitution();
        Vat vat = Vat.findActiveUnique(product.getVatType(), finantialInstitution, new DateTime()).orElse(null);

        // entry description for Interest Entry
        String entryDescription = interest.getDescription();
        if (Strings.isNullOrEmpty(entryDescription)) {
            // default entryDescription
            entryDescription = product.getName().getContent() + "-" + this.getDescription();
        }

        DebitEntry interestEntry =
                _create(getFinantialEntity(), getDebtAccount(), getTreasuryEvent(), vat, interest.getInterestAmount(),
                        when.toLocalDate(), TreasuryConstants.propertiesJsonToMap(getPropertiesJsonMap()), product,
                        entryDescription, BigDecimal.ONE, null, when, false, false, debitNote);

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

    // TRUE - Do not block academical acts when dueDate is expired
    // FALSE - Block academical acts when dueDate is expired
    public boolean isAcademicalActBlockingSuspension() {
        return getAcademicalActBlockingSuspension();
    }

    // This is the opposite of academicalActBlockingSuspension()
    // TRUE - Block academical acts when dueDate is expired
    // FALSE - Do not block academical acts when dueDate is expired
    public boolean getAcademicalActBlockingAfterDueDate() {
        return !isAcademicalActBlockingSuspension();
    }

    // This is the opposite of academicalActBlockingSuspension()
    // TRUE - Block academical acts when dueDate is expired
    // FALSE - Do not block academical acts when dueDate is expired
    public boolean isAcademicalActBlockingAfterDueDate() {
        return getAcademicalActBlockingAfterDueDate();
    }

    // This is the opposite of academicalActBlockingSuspension()
    // TRUE - Block academical acts when dueDate is expired
    // FALSE - Do not block academical acts when dueDate is expired
    public void setAcademicalActBlockingAfterDueDate(boolean value) {
        setAcademicalActBlockingSuspension(!value);
    }

    public boolean isBlockAcademicActsOnDebt() {
        return getBlockAcademicActsOnDebt();
    }

    public boolean exempt(final TreasuryExemption treasuryExemption) {
        if (treasuryExemption.getTreasuryEvent() != getTreasuryEvent()) {
            throw new RuntimeException("wrong call");
        }

        if (treasuryExemption.getProduct() != getProduct()) {
            throw new RuntimeException("wrong call");
        }

        if (isEventAnnuled()) {
            throw new RuntimeException("error.DebitEntry.is.event.annuled.cannot.be.exempted");
        }

        if (TreasuryDebtProcessMainService.isFinantialDocumentEntryAnnullmentActionBlocked(this)) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.or.credit.due.to.existing.active.debt.process");
        }

        if (TreasuryConstants.isGreaterThan(treasuryExemption.getNetExemptedAmount(), getAvailableNetAmountForCredit())) {
            throw new TreasuryDomainException("error.DebitEntry.exemptedAmount.cannot.be.greater.than.availableAmount");
        }

        if (Boolean.TRUE.equals(getCalculatedAmountsOverriden())) {
            throw new TreasuryDomainException("error.DebitEntry.exempt.not.possible.due.to.overriden.calculated.amounts");
        }

        if (isProcessedInClosedDebitNote()) {
            BigDecimal netExemptedAmount = treasuryExemption.getNetExemptedAmount();

            DateTime now = new DateTime();

            String reason = TreasuryConstants.treasuryBundle("label.TreasuryExemption.credit.entry.exemption.description",
                    getDescription(), treasuryExemption.getTreasuryExemptionType().getName().getContent());

            final CreditEntry creditEntryFromExemption =
                    createCreditEntry(now, getDescription(), null, null, netExemptedAmount, treasuryExemption, null,
                            Collections.emptyMap());

            closeCreditEntryIfPossible(reason, now, creditEntryFromExemption);

            creditEntryFromExemption.getFinantialDocument().setDocumentObservations(
                    String.format("[%s] - %s", treasuryExemption.getTreasuryExemptionType().getName().getContent(),
                            treasuryExemption.getReason()));

        } else {
            BigDecimal netExemptedAmount = treasuryExemption.getNetExemptedAmount();
            setNetExemptedAmount(getNetExemptedAmount().add(netExemptedAmount));

            // Record the old netAmount
            BigDecimal oldNetAmount = getNetAmount();

            // Recalculate the netAmount and amountWithVat
            recalculateAmountValues();

            // Compare the oldNetAmount is the sum of netAmount and netExemptedAmount
            if (!TreasuryConstants.isEqual(oldNetAmount, getNetAmount().add(netExemptedAmount))) {
                throw new IllegalStateException(
                        "The sum of netAmount and netExemptedAmount is not equal to the netAmount before applying the exemption");
            }

            // Ensure the netExemptedAmount have at most the decimal places for cents
            // defined in the currency
            //
            // TODO: First ensure in all instances that the following verifications are checked
            // Then move to DebitEntry::checkRules
            if (getNetExemptedAmount().scale() > getDebtAccount().getFinantialInstitution().getCurrency()
                    .getDecimalPlacesForCents()) {
                throw new IllegalStateException("The netExemptedAmount has scale above the currency decimal places for cents");
            }

            if (getTreasuryEvent() != null) {
                getTreasuryEvent().invokeSettlementCallbacks(treasuryExemption);
            }

        }

        checkRules();

        return true;
    }

    // The credit entry created here, assume that the Quantity is ONE. Therefore the amountForCreditWithoutVat must be
    // with the correct decimal scale
    public CreditEntry createCreditEntry(DateTime documentDate, String description, String documentObservations,
            String documentTermsAndConditions, BigDecimal netAmountForCredit, TreasuryExemption treasuryExemption,
            CreditNote creditNote, Map<TreasuryExemption, BigDecimal> creditExemptionsMap) {
        FinantialInstitution finantialInstitution = getDebtAccount().getFinantialInstitution();

        boolean isToCloseCreditNoteWhenCreated = getDebtAccount().getFinantialInstitution().isToCloseCreditNoteWhenCreated();
        boolean isInvoiceRegistrationByTreasuryCertification =
                getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification();

        final DebitNote debitNote = (DebitNote) this.getFinantialDocument();

        if (debitNote == null) {
            throw new TreasuryDomainException("error.DebitEntry.createCreditEntry.requires.finantial.document");
        }

        if (creditNote != null && !creditNote.isPreparing()) {
            throw new TreasuryDomainException("error.DebitEntry.createCreditEntry.creditNote.is.not.preparing");
        }

        if (creditNote == null) {
            DocumentNumberSeries documentNumberSeries = debitNote.inferCreditNoteDocumentNumberSeries();

            creditNote = CreditNote.create(debitNote, documentNumberSeries, documentDate, debitNote.getUiDocumentNumber());
        }

        if (treasuryExemption != null && Boolean.TRUE.equals(getCalculatedAmountsOverriden())) {
            throw new TreasuryDomainException("error.DebitEntry.exempt.not.possible.due.to.overriden.calculated.amounts");
        }

        if (Boolean.TRUE.equals(getCalculatedAmountsOverriden()) && TreasuryConstants.isLessThan(netAmountForCredit,
                getNetAmount())) {
            throw new TreasuryDomainException(
                    "error.DebitEntry.createCreditEntry.for.overriden.calculated.amounts.only.possible.to.credit.by.full.amount");
        }

        if (!Strings.isNullOrEmpty(documentObservations)) {
            creditNote.setDocumentObservations(documentObservations);
        }

        if (!Strings.isNullOrEmpty(documentTermsAndConditions)) {
            creditNote.setDocumentTermsAndConditions(documentTermsAndConditions);
        }

        if (finantialInstitution.isSupportForCreditExemptionsActive()) {
            if (!TreasuryConstants.isPositive(netAmountForCredit) && !TreasuryConstants.isPositive(
                    creditExemptionsMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))) {
                throw new TreasuryDomainException("error.DebitEntry.createCreditEntry.amountForCredit.not.positive");
            }
        } else {
            if (!TreasuryConstants.isPositive(netAmountForCredit)) {
                throw new TreasuryDomainException("error.DebitEntry.createCreditEntry.amountForCredit.not.positive");
            }
        }

        CreditEntry creditEntry = null;
        if (treasuryExemption != null) {
            creditEntry = CreditEntry.createFromExemption(treasuryExemption, creditNote, description, netAmountForCredit,
                    new DateTime(), BigDecimal.ONE);
        } else {
            if (finantialInstitution.isSupportForCreditExemptionsActive()) {
                creditEntry = CreditEntry.create(creditNote, description, getProduct(), getVat(),
                        netAmountForCredit.add(creditExemptionsMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)),
                        documentDate, this, BigDecimal.ONE, creditExemptionsMap);
            } else {
                creditEntry =
                        CreditEntry.create(creditNote, description, getProduct(), getVat(), netAmountForCredit, documentDate,
                                this, BigDecimal.ONE, Collections.emptyMap());
            }

            if (Boolean.TRUE.equals(getCalculatedAmountsOverriden())) {
                // Create a credit entry with the exact amounts as this debit entry
                creditEntry.overrideCalculatedAmounts(getNetAmount(), getVatRate(), getVatAmount(), getAmountWithVat());
            }
        }

        if (!isInvoiceRegistrationByTreasuryCertification && isToCloseCreditNoteWhenCreated) {
            creditNote.closeDocument();
        }

        return creditEntry;
    }

    // ANIL 2023-11-29 
    //
    // I changed the visibility of this method to 'package protected', it is only
    // called by DebitNote#anullDebitNoteWithCreditNote and this class. If it is needed in some script, just copy the
    // method and use it. The visibility is important to not be called by other method

    void closeCreditEntryIfPossible(final String reason, final DateTime now, final CreditEntry creditEntry) {
        FinantialInstitution finantialInstitution = getDebtAccount().getFinantialInstitution();
        FinantialEntity finantialEntity = creditEntry.getFinantialEntity();

        DocumentNumberSeries documentNumberSeriesSettlementNote =
                DocumentNumberSeries.find(FinantialDocumentType.findForSettlementNote(),
                        this.getFinantialDocument().getDocumentNumberSeries().getSeries());

        // ANIL 2023-11-29: Ensure the associated finantial document is in closed state

        if (!getDebitNote().isClosed()) {
            throw new IllegalStateException("error.DebitEntry.closeCreditEntryIfPossible.invalid.debitNote.state");
        }

        boolean splitCreditEntriesWithSettledAmount =
                getDebtAccount().getFinantialInstitution().getSplitCreditEntriesWithSettledAmount();

        boolean isToCloseCreditNoteWhenCreated = getDebtAccount().getFinantialInstitution().isToCloseCreditNoteWhenCreated();
        boolean isInvoiceRegistrationByTreasuryCertification =
                getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification();

        if (isInvoiceRegistrationByTreasuryCertification) {
            documentNumberSeriesSettlementNote =
                    DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForSettlementNote(), finantialEntity);
        }

        if (creditEntry.getFinantialDocument().isAnnulled()) {
            throw new TreasuryDomainException("error.DebitEntry.closeCreditEntryIfPossible.creditEntry.is.annulled");
        }

        if (!isInvoiceRegistrationByTreasuryCertification && !creditEntry.getFinantialDocument()
                .isPreparing() && !isToCloseCreditNoteWhenCreated) {
            return;
        }

        BigDecimal minimumOpenAmount = creditEntry.getOpenAmount();

        if (TreasuryConstants.isLessThan(this.getOpenAmount(), creditEntry.getOpenAmount())) {
            minimumOpenAmount = this.getOpenAmount();
        }

        if (!TreasuryConstants.isPositive(minimumOpenAmount)) {
            return;
        }

        if (splitCreditEntriesWithSettledAmount && TreasuryConstants.isLessThan(minimumOpenAmount,
                creditEntry.getOpenAmount()) && creditEntry.getFinantialDocument().isPreparing()) {
            // split credit entry
            creditEntry.splitCreditEntry(creditEntry.getOpenAmount().subtract(minimumOpenAmount));
        }

        if (creditEntry.getFinantialDocument().isPreparing()) {
            creditEntry.getFinantialDocument().closeDocument();
        }

        final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

        final String reasonDescription = TreasuryConstants.treasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE,
                "label.TreasuryEvent.credit.by.annulAllDebitEntries.reason");

        final SettlementNote settlementNote =
                SettlementNote.create(getFinantialEntity(), this.getDebtAccount(), documentNumberSeriesSettlementNote, now, now,
                        "", null);

        if (!finantialInstitution.isInvoiceRegistrationByTreasuryCertification()) {
            settlementNote.setDocumentObservations(
                    reason + " - [" + loggedUsername + "] " + new DateTime().toString("YYYY-MM-dd HH:mm"));
        }

        SettlementEntry creditSettlementEntry = SettlementEntry.create(creditEntry, settlementNote, minimumOpenAmount,
                reasonDescription + ": " + creditEntry.getDescription(), now, false);
        creditSettlementEntry.setInternalComments(reason);

        SettlementEntry debitSettlementEntry =
                SettlementEntry.create(this, settlementNote, minimumOpenAmount, reasonDescription + ": " + getDescription(), now,
                        false);
        debitSettlementEntry.setInternalComments(reason);

        InstallmentSettlementEntry.settleInstallmentEntriesOfDebitEntry(debitSettlementEntry);

        if (TreasurySettings.getInstance()
                .isRestrictPaymentMixingLegacyInvoices() && getFinantialDocument().isExportedInLegacyERP() != creditEntry.getFinantialDocument()
                .isExportedInLegacyERP()) {
            throw new TreasuryDomainException("error.DebitEntry.closeCreditEntryIfPossible.exportedInLegacyERP.not.same");
        }

        if (((Invoice) getFinantialDocument()).getPayorDebtAccount() != ((Invoice) getFinantialDocument()).getPayorDebtAccount()) {
            throw new TreasuryDomainException("error.DebitEntry.closeCreditEntryIfPossible.payorDebtAccount.not.same");
        }

        if (TreasurySettings.getInstance()
                .isRestrictPaymentMixingLegacyInvoices() && getFinantialDocument().isExportedInLegacyERP()) {
            settlementNote.setExportedInLegacyERP(true);
            settlementNote.setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
        }

        settlementNote.closeDocument();
    }

    public boolean isExportedInERPAndInRestrictedPaymentMixingLegacyInvoices() {
        return TreasurySettings.getInstance()
                .isRestrictPaymentMixingLegacyInvoices() && getFinantialDocument() != null && getFinantialDocument().isExportedInLegacyERP();
    }

    public BigDecimal getAmountInDebt(final LocalDate paymentDate) {
        final Set<SettlementEntry> entries = new TreeSet<SettlementEntry>(SettlementEntry.COMPARATOR_BY_ENTRY_DATE_TIME);

        entries.addAll(getSettlementEntriesSet());

        BigDecimal amountToPay = getAmountWithVat();
        for (final SettlementEntry settlementEntry : entries) {
            // ANIL 2024-10-10 (qubIT-Fenix-5932)
            //
            // The only settlements that must be considered are those closed (and not open)
            // Also only before the paymentDate . The return value must be the amount in debt at the start of day
            if (settlementEntry.getFinantialDocument() != null && settlementEntry.getFinantialDocument().isClosed()) {

                if (!settlementEntry.getEntryDateTime().toLocalDate().isBefore(paymentDate)) {
                    break;
                }

                amountToPay = amountToPay.subtract(settlementEntry.getAmount());
            }
        }

        return amountToPay;
    }

    public void revertExemptionIfPossibleInPreparingState(TreasuryExemption treasuryExemption) {
        if (isAnnulled()) {
            throw new TreasuryDomainException("error.TreasuryExemption.delete.impossible.due.to.processed.debit.or.credit.entry");
        }

        if (isProcessedInClosedDebitNote()) {
            throw new TreasuryDomainException("error.TreasuryExemption.delete.impossible.due.to.processed.debit.or.credit.entry");
        }

        // TODO: This check can be removed?
        if (!getCreditEntriesSet().isEmpty()) {
            throw new TreasuryDomainException("error.TreasuryExemption.delete.impossible.due.to.processed.debit.or.credit.entry");
        }

        BigDecimal netExemptedAmount = treasuryExemption.getNetExemptedAmount();
        setNetExemptedAmount(getNetExemptedAmount().subtract(netExemptedAmount));

        recalculateAmountValues();

        // Ensure the netExemptedAmount have at most the decimal places for cents
        // defined in the currency
        //
        // TODO: First ensure in all instances that the following verifications are checked
        // Then move to DebitEntry::checkRules
        if (getNetExemptedAmount().scale() > getDebtAccount().getFinantialInstitution().getCurrency()
                .getDecimalPlacesForCents()) {
            throw new IllegalStateException("The netExemptedAmount has scale above the currency decimal places for cents");
        }

        checkRules();
    }

    // This method is just a helper to be used in interface
    // to create exemptions
    public BigDecimal getUiPossibleMaximumAmountToExempt() {
        return getNetAmount().min(getAvailableNetAmountForCredit());
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
        Optional<SettlementNote> settlementNote =
                getSettlementEntriesSet().stream().filter(s -> !s.getFinantialDocument().isAnnulled())
                        .map(s -> ((SettlementNote) s.getFinantialDocument()))
                        .max(Comparator.comparing(SettlementNote::getPaymentDate));
        if (!settlementNote.isPresent()) {
            return null;
        }

        return settlementNote.get().getPaymentDate();
    }

    public boolean isTotallyExempted() {
        return TreasuryConstants.isPositive(getNetExemptedAmount()) && !TreasuryConstants.isPositive(getNetAmount());
    }

    /**
     * Differs from getLastSettlementDate in obtaining payment date only from
     * settlement notes that do not credit this debitEntry
     *
     * @return
     */

    public DateTime getLastPaymentDate() {
        Set<SettlementNote> settlementNotesToConsiderSet = new HashSet<SettlementNote>();
        for (SettlementEntry debitEntrySettlementEntry : getSettlementEntriesSet()) {
            SettlementNote note = (SettlementNote) debitEntrySettlementEntry.getFinantialDocument();

            // Do not consider annuled settlement notes
            if (note.isAnnulled()) {
                continue;
            }

            // Do not consider settlements with own credits
            if (note.getSettlemetEntriesSet().stream().filter(se -> se != debitEntrySettlementEntry)
                    .filter(se -> se.getInvoiceEntry().isCreditNoteEntry())
                    .filter(se -> ((CreditEntry) se.getInvoiceEntry()).getDebitEntry() == this).anyMatch(
                            se -> TreasuryConstants.isGreaterOrEqualThan(se.getAmount(),
                                    debitEntrySettlementEntry.getAmount()))) {
                continue;
            }

            settlementNotesToConsiderSet.add(note);
        }

        Optional<SettlementNote> settlementNote =
                settlementNotesToConsiderSet.stream().max(Comparator.comparing(SettlementNote::getPaymentDate));

        if (!settlementNote.isPresent()) {
            return null;
        }

        return settlementNote.get().getPaymentDate();
    }

    @Deprecated
    // TODO Used in SubsequentTuitionServiceExtension. Evaluate and remove usage
    // and remove this method
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

    public boolean isAllowEditAmounts() {
        if (isAnnulled()) {
            return false;
        }

        if (getFinantialDocument() != null && !getFinantialDocument().isPreparing()) {
            return false;
        }

        if (!getTreasuryExemptionsSet().isEmpty()) {
            return false;
        }

        if (TreasuryDebtProcessMainService.getDebtProcesses(this).stream().anyMatch(process -> process.isProcessActive())) {
            return false;
        }

        if (getOpenPaymentPlan() != null) {
            return false;
        }

        if (getCreditEntriesSet().stream().anyMatch(c -> !c.isAnnulled())) {
            return false;
        }

        if (Boolean.TRUE.equals(getCalculatedAmountsOverriden())) {
            return false;
        }

        if (getFinantialDocument() != null && getFinantialDocument().getDocumentNumberSeries().getSeries().isRegulationSeries()) {
            return false;
        }

        if (getFinantialDocument() != null && getFinantialDocument().getDocumentNumberSeries().getSeries().isLegacy()) {
            return false;
        }

        return true;
    }

    public void editAmounts(BigDecimal unitAmount, BigDecimal quantity, String reason) {
        if (isProcessedInClosedDebitNote()) {
            throw new TreasuryDomainException("error.DebitEntry.editAmount.cannot.edit.amount.due.to.closed.in.debit.note");
        }

        if (isAnnulled()) {
            throw new TreasuryDomainException("error.DebitEntry.editAmount.cannot.edit.amount.due.to.annuled.state");
        }

        if (!getTreasuryExemptionsSet().isEmpty()) {
            throw new TreasuryDomainException("error.DebitEntry.editAmount.cannot.edit.amount.due.to.existing.exemptions");
        }

        if (TreasuryDebtProcessMainService.getDebtProcesses(this).stream().anyMatch(process -> process.isProcessActive())) {
            throw new TreasuryDomainException("error.DebitEntry.editAmount.cannot.edit.amount.due.to.existing.processes");
        }

        if (getOpenPaymentPlan() != null) {
            throw new TreasuryDomainException("error.DebitEntry.editAmount.cannot.edit.amount.due.to.existing.open.paymentPlan");
        }

        if (getCreditEntriesSet().stream().anyMatch(c -> !c.isAnnulled())) {
            throw new TreasuryDomainException("error.DebitEntry.editAmount.cannot.edit.amount.due.to.existing.creditEntries");
        }

        if (!isAllowEditAmounts()) {
            throw new TreasuryDomainException("error.DebitEntry.editAmount.cannot.edit.amount");
        }

        DebitEntryChangeAmountsLog.log(this, "editAmounts", reason);

        setAmount(unitAmount);
        setQuantity(quantity);

        recalculateAmountValues();

        // ANIL 2025-04-09 (#qubIT-Fenix-6814)
        getSibsPaymentRequests().stream().filter(r -> r.isInCreatedState() || r.isInRequestedState()).forEach(r -> r.anull());

        checkRules();
    }

    public void changeInterestRate(InterestRate oldInterestRate) {
        if (this.getInterestRate() != null && this.getInterestRate() != oldInterestRate) {
            oldInterestRate.delete();
        }

        checkRules();
    }

    public BigDecimal getTotalCreditedAmountWithVat() {
        BigDecimal totalCreditedAmount = BigDecimal.ZERO;
        for (CreditEntry creditEntry : this.getCreditEntriesSet()) {
            if (creditEntry.getFinantialDocument() == null || !creditEntry.getFinantialDocument().isAnnulled()) {
                totalCreditedAmount = totalCreditedAmount.add(creditEntry.getTotalAmount());
            }
        }
        return Currency.getValueWithScale(totalCreditedAmount);
    }

    public BigDecimal getAvailableAmountWithVatForCredit() {
        return Currency.getValueWithScale(this.getTotalAmount().subtract(getTotalCreditedAmountWithVat()));
    }

    public BigDecimal getTotalCreditedNetAmount() {
        BigDecimal creditedNetAmount = getCreditEntriesSet().stream() //
                .filter(c -> !c.isAnnulled()) //
                .map(CreditEntry::getNetAmount) //
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return creditedNetAmount;
    }

    public BigDecimal getAvailableNetAmountForCredit() {
        return this.getNetAmount().subtract(getTotalCreditedNetAmount());
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

    public BigDecimal getOpenAmountWithInterestsAtDate(LocalDate date) {
        if (isAnnulled()) {
            return BigDecimal.ZERO;
        }

        if (TreasuryConstants.isEqual(getOpenAmount(), BigDecimal.ZERO)) {
            return getOpenAmount();
        } else {
            return getOpenAmount().add(getPendingInterestAmount(date));
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

        return "{\"" + TreasuryEventKeys.DEGREE_CODE + "\":\"" + degreeCode + "\",\"" + TreasuryEventKeys.EXECUTION_YEAR + "\":\"" + executionYear + "\"}";
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

    public Set<SibsPaymentRequest> getSibsPaymentRequests() {
        return getSibsPaymentRequestsSet();
    }

    public Set<SibsPaymentRequest> getSibsPaymentRequestsSet() {
        return getPaymentRequestsSet().stream().filter(r -> r instanceof SibsPaymentRequest).map(SibsPaymentRequest.class::cast)
                .collect(Collectors.toSet());
    }

    @Atomic
    public void annulDebitEntry(final String reason) {
        annulDebitEntry(reason, true);
    }

    /* ANIL 2025-03-03 (#qubIT-Fenix-6662)
     *
     * There are some cases, like tuition recalculation, in which
     * we cannot annul the interests, if the installment must be
     * annuled and created again
     */
    @Atomic
    public void annulDebitEntry(final String reason, boolean annulInterests) {
        if (isAnnulled()) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.is.already.annuled");
        }

        if (getFinantialDocument() != null) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.with.finantial.document");
        }

        if (Strings.isNullOrEmpty(reason)) {
            throw new TreasuryDomainException("error.DebitEntry.annul.debit.entry.requires.reason");
        }

        if (TreasuryDebtProcessMainService.isFinantialDocumentEntryAnnullmentActionBlocked(this)) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.or.credit.due.to.existing.active.debt.process");
        }

        DocumentNumberSeries defaultDocumentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), getFinantialEntity());
        final DebitNote debitNote =
                DebitNote.create(getFinantialEntity(), getDebtAccount(), null, defaultDocumentNumberSeries, new DateTime(),
                        new LocalDate(), null, Collections.emptyMap(), null, null);

        setFinantialDocument(debitNote);

        debitNote.anullDebitNoteWithCreditNote(reason, annulInterests);
    }

    public void annulOnlyThisDebitEntryAndInterestsInBusinessContext(String reason) {
        annulOnlyThisDebitEntryAndInterestsInBusinessContext(reason, true);
    }

    /* ANIL 2025-03-03 (#qubIT-Fenix-6662)
     *
     * There are some cases, like tuition recalculation, in which
     * we cannot annul the interests, if the installment must be
     * annuled and created again
     */

    /**
     * The purpose of this method is to avoid rewriting the logic
     * of checking if the debit entry is in finantial document or not
     * and to annul the interest debit entries
     */
    public void annulOnlyThisDebitEntryAndInterestsInBusinessContext(String reason, boolean annulInterests) {
        if (annulInterests) {
            // Ensure interests are annuled
            getInterestDebitEntriesSet().stream()
                    .forEach(d -> d.annulOnlyThisDebitEntryAndInterestsInBusinessContext(reason, annulInterests));
        }

        annulOnEvent();

        // ANIL 2024-09-26 (#qubIT-Fenix-5852)
        //
        // Before this date, it was being applied this condition besides isAnnulled() :
        //
        //  if(isAnnulled() || !TreasuryConstants.isPositive(getAvailableNetAmountForCredit())) {
        //      return true;
        //  }
        //
        // which does not mark as annuled the debit entry and does not
        // create the necessary credit entries for exemptions, if it is closed

        if (isAnnulled()) {
            return;
        }

        if (getFinantialDocument() != null) {
            if (getFinantialDocument().isPreparing()) {
                removeFromDocument();

                annulDebitEntry(reason);
            } else {
                Map<TreasuryExemption, BigDecimal> calculateAllNetExemptedAmountsToCreditMap =
                        calculateDefaultNetExemptedAmountsToCreditMap();

                BigDecimal totalNetExemptedAmountToCredit =
                        calculateAllNetExemptedAmountsToCreditMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
                if (!TreasuryConstants.isPositive(getAvailableNetAmountForCredit()) && !TreasuryConstants.isPositive(
                        totalNetExemptedAmountToCredit)) {
                    // Nothing to credit, just return
                    return;
                }

                creditDebitEntry(getAvailableNetAmountForCredit(), reason, false, calculateAllNetExemptedAmountsToCreditMap);
            }
        } else {
            annulDebitEntry(reason);
        }
    }

    public Map<TreasuryExemption, BigDecimal> calculateDefaultNetExemptedAmountsToCreditMap() {
        return calculateDefaultNetExemptedAmountsToCreditMap(getAvailableNetAmountForCredit());
    }

    public Map<TreasuryExemption, BigDecimal> calculateDefaultNetExemptedAmountsToCreditMap(BigDecimal netAmountToCredit) {
        if (!TreasuryConstants.isLessOrEqualThan(netAmountToCredit, getAvailableNetAmountForCredit())) {
            throw new IllegalArgumentException("netAmountToCredit is less than availableNetAmountForCredit");
        }

        // ANIL 2024-09-26 (#qubIT-Fenix-5852)
        // 
        // If the netAmountToCredit and the getAvailableNetAmountForCredit() are both zero,
        // then the ratio should be one

        BigDecimal calculatedRatio = BigDecimal.ONE;

        // Avoid divide by zero exception
        if (TreasuryConstants.isPositive(getAvailableNetAmountForCredit())) {
            calculatedRatio = TreasuryConstants.divide(netAmountToCredit, getAvailableNetAmountForCredit());
        }

        BigDecimal ratio = calculatedRatio;

        // ANIL 2024-09-26 (#qubIT-Fenix-5852) **README**
        // 
        // If the multiplication by ratio is not positive, then discard

        Map<TreasuryExemption, BigDecimal> result = getTreasuryExemptionsSet().stream() //
                .filter(te -> te.getCreditEntry() == null) //
                .filter(te -> TreasuryConstants.isPositive(te.getAvailableNetExemptedAmountForCredit())) //
                .filter(te -> TreasuryConstants.isPositive(
                        Currency.getValueWithScale(te.getAvailableNetExemptedAmountForCredit().multiply(ratio)))) // **README**
                .collect(Collectors.toMap(te -> te,
                        te -> Currency.getValueWithScale(te.getAvailableNetExemptedAmountForCredit().multiply(ratio))));

        return result;
    }

    public BigDecimal getCreditedNetExemptedAmount() {
        return getCreditEntriesSet().stream().filter(ce -> !ce.isAnnulled()).map(CreditEntry::getNetExemptedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getEffectiveNetExemptedAmount() {
        return getNetExemptedAmount().subtract(getCreditedNetExemptedAmount());
    }

    @Atomic
    public void creditDebitEntry(BigDecimal netAmountForCredit, String reason, boolean closeWithOtherDebitEntriesOfDebitNote,
            Map<TreasuryExemption, BigDecimal> creditExemptionsMap) {
        if (isAnnulled()) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.credit.is.already.annuled");
        }

        if (getFinantialDocument() == null || getFinantialDocument().isPreparing()) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.credit.without.or.preparing.finantial.document");
        }

        if (Strings.isNullOrEmpty(reason)) {
            throw new TreasuryDomainException("error.DebitEntry.credit.debit.entry.requires.reason");
        }

        if (TreasuryDebtProcessMainService.isFinantialDocumentEntryAnnullmentActionBlocked(this)) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.or.credit.due.to.existing.active.debt.process");
        }

        if (!getDebtAccount().getFinantialInstitution().isSupportForCreditExemptionsActive()) {
            if (!TreasuryConstants.isPositive(netAmountForCredit)) {
                throw new TreasuryDomainException("error.DebitEntry.credit.debit.entry.amountToCreditWithVat.must.be.positive");
            }
        }

        if (!TreasuryConstants.isLessOrEqualThan(netAmountForCredit, getAvailableNetAmountForCredit())) {
            throw new TreasuryDomainException(
                    "error.DebitEntry.credit.debit.entry.amountToCreditWithVat.must.be.less.or.equal.than.amountAvailableForCredit");
        }

        final DateTime now = new DateTime();
        final CreditEntry creditEntry =
                createCreditEntry(now, getDescription(), null, null, netAmountForCredit, null, null, creditExemptionsMap);

        creditEntry.setInternalComments(reason);

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
                final CreditEntry openCreditEntry =
                        getCreditEntriesSet().stream().filter(c -> TreasuryConstants.isPositive(c.getOpenAmount())).findFirst()
                                .get();

                // Find debit entry with open amount equal or higher than the open credit
                DebitEntry openDebitEntry =
                        getDebitNote().getDebitEntriesSet().stream().filter(d -> TreasuryConstants.isPositive(d.getOpenAmount()))
                                .filter(d -> TreasuryConstants.isGreaterOrEqualThan(d.getOpenAmount(),
                                        openCreditEntry.getOpenAmount())).findFirst().orElse(null);

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

        if (Boolean.TRUE.equals(getCalculatedAmountsOverriden())) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.remove.from.document.due.to.calculated.amounts.overriden");
        }

        setFinantialDocument(null);
        setEntryOrder(null);
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
     *
     * @return
     */
    // TODO Rename method imply the returning value installments entries that are not paid, of open payment plan
    public List<InstallmentEntry> getSortedOpenInstallmentEntries() {
        return getInstallmentEntrySet().stream().filter(i -> i.getInstallment().getPaymentPlan().getState().isOpen())
                .sorted(InstallmentEntry.COMPARE_BY_DEBIT_ENTRY_COMPARATOR).collect(Collectors.toList());
    }

    @Deprecated
    @Override
    public void addForwardPayments(ForwardPayment forwardPayments) {
        super.addForwardPayments(forwardPayments);
    }

    @Deprecated
    @Override
    public void removeForwardPayments(ForwardPayment forwardPayments) {
        super.removeForwardPayments(forwardPayments);
    }

    @Deprecated
    @Override
    public Set<ForwardPayment> getForwardPaymentsSet() {
        return super.getForwardPaymentsSet();
    }

    public void updateDueDate(LocalDate newDueDate) {
        setDueDate(newDueDate);
        checkRules();
    }

    // TODO ANIL 2024-01-23: Replace this method to #getActiveSibsPaymentRequests
    @Deprecated
    public Set<SibsPaymentRequest> getActiveSibsPaymentRequestsOfPendingDebitEntries() {
        return getActiveSibsPaymentRequests();
    }

    public Set<SibsPaymentRequest> getActiveSibsPaymentRequests() {
        return getPaymentRequestsSet().stream() //
                .filter(p -> p instanceof SibsPaymentRequest) //
                .map(SibsPaymentRequest.class::cast) //
                .filter(p -> p.getState() == PaymentReferenceCodeStateType.UNUSED || p.getState() == PaymentReferenceCodeStateType.USED) //
                .filter(p -> p.getExpiresDate() == null || !p.getExpiresDate().isBeforeNow()) //
                .collect(Collectors.toSet());
    }

    // @formatter:off
    /*
     * ********************************
     * FINANTIAL ENTITY RELATED METHODS
     * ********************************
     */
    // @formatter:on

    @Override
    public FinantialEntity getFinantialEntity() {
        if (super.getFinantialEntity() != null) {
            return super.getFinantialEntity();
        }

        if (getTreasuryEvent() != null) {
            return getTreasuryEvent().getAssociatedFinantialEntity();
        } else if (getDebitEntry() != null) {
            return getDebitEntry().getFinantialEntity();
        }

        return null;
    }

    // ANIL 2023-12-10: This consumers allows to establish the relation connections when
    // splitting a debit entry. This should be registered in other modules than treasury
    private static List<BiConsumer<DebitEntry, DebitEntry>> CONNECT_RELATIONS_WHEN_SPLITTING_DEBIT_ENTRY_CONSUMERS_LIST =
            new ArrayList<>();

    public static void registerConnectRelationsWhenSplittingDebitEntryConsumer(BiConsumer<DebitEntry, DebitEntry> consumer) {
        CONNECT_RELATIONS_WHEN_SPLITTING_DEBIT_ENTRY_CONSUMERS_LIST.add(consumer);
    }

    public DebitEntry splitDebitEntry(BigDecimal withAmountWithVatOfNewDebitEntry, String splitDebitEntryReason) {
        if (getFinantialDocument() != null && !getFinantialDocument().isPreparing()) {
            throw new IllegalStateException("error.DebitEntry.splitDebitEntry.finantialDocument.not.preparing");
        }

        if (!TreasuryConstants.isLessThan(withAmountWithVatOfNewDebitEntry, getTotalAmount())) {
            throw new IllegalStateException("error.DebitEntry.splitDebitEntry.remainingAmount.less.than.open.amount");
        }

        if (getFinantialDocument() != null && getFinantialDocument().isExportedInLegacyERP()) {
            throw new IllegalStateException(
                    "error.DebitEntry.splitDebitEntry.not.supported.for.finantialDocument.exportedInLegacyERP");
        }

        final BigDecimal totalAmount = getTotalAmount();

        DebitEntryChangeAmountsLog.log(this, "splitDebitEntry", splitDebitEntryReason);

        annulAllActiveSibsPaymentRequests();

        // README ANIL 2023-12-26
        //
        // This method will create a new debit entry the amount argument
        // supplied, which is the total amount with vat.
        // The argument is totalAmountWithVat, because this will
        // be used in payments, when a partial payment occurs, and the
        // payment amounts are with vat

        // The problem with this method is the loss of precision,
        // which is a problem in payments (payments cannot be hindered)

        // The precision can be lost in the following ways:
        //
        // 1. The calculation of between unit amount and quantity
        // 2. The partition of exemptions
        // 3. The calculation of vat amount

        // The rule of thumb is: 
        // 1) the amountWithVat supplied in the argument
        // must be the final result of the new debit entry
        //
        // 2) The restAmountWithVat, resulted from the totalAmount
        // subtracted with the argument supplied withAmountWithVatOfNewDebitEntry

        // The calculations must find a way to accomodate the 
        // resulting amountWithVat, in both debts

        BigDecimal totalAmountRatio = TreasuryConstants.divide(withAmountWithVatOfNewDebitEntry, getTotalAmount());

        BigDecimal amountWithVatOfCurrentDebitEntry = getTotalAmount().subtract(withAmountWithVatOfNewDebitEntry);

        BigDecimal netAmountOfNewDebitEntry = Currency.getValueWithScale(
                TreasuryConstants.divide(withAmountWithVatOfNewDebitEntry,
                        BigDecimal.ONE.add(TreasuryConstants.rationalVatRate(this))));

        BigDecimal netAmountOfCurrentDebitEntry = Currency.getValueWithScale(
                TreasuryConstants.divide(amountWithVatOfCurrentDebitEntry,
                        BigDecimal.ONE.add(TreasuryConstants.rationalVatRate(this))));

        Map<TreasuryExemption, BigDecimal> exemptionsNetAmountForNewDebitEntryMap = getTreasuryExemptionsSet().stream().collect(
                Collectors.toMap(Function.identity(),
                        e -> Currency.getValueWithScale(e.getNetExemptedAmount().multiply(totalAmountRatio))));

        Map<TreasuryExemption, BigDecimal> exemptionsNetAmountForCurrentDebitEntryMap = getTreasuryExemptionsSet().stream()
                .collect(Collectors.toMap(Function.identity(),
                        e -> e.getNetExemptedAmount().subtract(exemptionsNetAmountForNewDebitEntryMap.get(e))));

        BigDecimal unitAmountForNewDebitEntry = TreasuryConstants.divide(netAmountOfNewDebitEntry.add(
                        exemptionsNetAmountForNewDebitEntryMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)), getQuantity())
                .setScale(InvoiceEntry.UNIT_PRICE_SCALE, RoundingMode.HALF_UP);

        BigDecimal unitAmountForCurrentDebitEntry = TreasuryConstants.divide(netAmountOfCurrentDebitEntry.add(
                        exemptionsNetAmountForCurrentDebitEntryMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)),
                getQuantity()).setScale(InvoiceEntry.UNIT_PRICE_SCALE, RoundingMode.HALF_UP);

        // Find the amount that is being lost, which is will cents, and call it delta

        DebitNote oldDebitNote = getDebitNote();

        if (getDebitNote() != null) {
            DebitNote newDebitNote = DebitNote.create(getDebitNote().getFinantialEntity(), this.getDebtAccount(),
                    getDebitNote().getPayorDebtAccount(), getDebitNote().getDocumentNumberSeries(),
                    getDebitNote().getDocumentDate(), getDebitNote().getDocumentDueDate(),
                    getDebitNote().getOriginDocumentNumber(), getDebitNote().getPropertiesMap(),
                    getFinantialDocument().getDocumentObservations(), getFinantialDocument().getDocumentTermsAndConditions());

            newDebitNote.setCloseDate(getDebitNote().getCloseDate());
            newDebitNote.setLegacyERPCertificateDocumentReference(getDebitNote().getLegacyERPCertificateDocumentReference());

            newDebitNote.addDebitNoteEntries(List.of(this));
        }

        DebitEntry newDebitEntry =
                _create(getFinantialEntity(), getDebtAccount(), getTreasuryEvent(), getVat(), unitAmountForNewDebitEntry,
                        getDueDate(), getPropertiesMap(), getProduct(), getDescription(), getQuantity(), getInterestRate(),
                        getEntryDateTime(), isAcademicalActBlockingSuspension(), isBlockAcademicActsOnDebt(), oldDebitNote);

        newDebitEntry.setSplittingOriginDebitEntry(this);

        // Apply exemptions and track the amounts of exemption that must the applied
        for (TreasuryExemption treasuryExemption : getTreasuryExemptionsSet()) {
            BigDecimal netAmountToExempt = exemptionsNetAmountForNewDebitEntryMap.get(treasuryExemption);

            if (TreasuryConstants.isPositive(netAmountToExempt)) {
                TreasuryExemption.create(treasuryExemption.getTreasuryExemptionType(), treasuryExemption.getReason(),
                        netAmountToExempt, newDebitEntry);
            }
        }

        newDebitEntry.setNetAmount(netAmountOfNewDebitEntry);
        newDebitEntry.setAmountWithVat(withAmountWithVatOfNewDebitEntry);

        // Reapply the exemptions but with the new amount
        for (TreasuryExemption treasuryExemption : getTreasuryExemptionsSet()) {
            BigDecimal netAmountToExempt = exemptionsNetAmountForCurrentDebitEntryMap.get(treasuryExemption);

            if (TreasuryConstants.isPositive(netAmountToExempt)) {
                String exemptionReason = treasuryExemption.getReason();
                TreasuryExemptionType treasuryExemptionType = treasuryExemption.getTreasuryExemptionType();

                treasuryExemption.revertExemption();
                TreasuryExemption.create(treasuryExemptionType, exemptionReason, netAmountToExempt, this);
            }
        }

        setAmount(unitAmountForCurrentDebitEntry);
        setNetAmount(netAmountOfCurrentDebitEntry);
        setVatAmount(amountWithVatOfCurrentDebitEntry.subtract(netAmountOfCurrentDebitEntry));
        setAmountWithVat(amountWithVatOfCurrentDebitEntry);

        // The method InvoiceEntry::recalculateAmountValues will not be called

        applyAdditionalRelationConnectionsOfDebitEntry(this, newDebitEntry);

        // Ensure the amountWithVat before and after are equals
        if (totalAmount.compareTo(getAmountWithVat().add(newDebitEntry.getAmountWithVat())) != 0) {
            throw new IllegalStateException("error.DebitEntry.splitDebitEntry.amountWithVat.before.after.not.equal");
        }

        Collector<InstallmentEntry, ?, Map<PaymentPlan, Set<InstallmentEntry>>> collector =
                Collectors.toMap(ie -> ie.getInstallment().getPaymentPlan(), (InstallmentEntry ie) -> Set.of(ie), (l1, l2) -> {
                    HashSet<InstallmentEntry> s1 = new HashSet<>(l1);
                    s1.addAll(l2);
                    return s1;
                });

        final Map<PaymentPlan, Set<InstallmentEntry>> installmentsMap = getInstallmentEntrySet().stream().collect(collector);

        Comparator<InstallmentEntry> installmentEntryComparator =
                ((Comparator<InstallmentEntry>) (ie1, ie2) -> Installment.COMPARE_BY_DUEDATE.compare(ie1.getInstallment(),
                        ie2.getInstallment())).thenComparing(InstallmentEntry::getExternalId);

        installmentsMap.keySet().forEach(paymentPlan -> {
            List<InstallmentEntry> installmentEntriesList =
                    installmentsMap.get(paymentPlan).stream().sorted(installmentEntryComparator).collect(Collectors.toList());

            BigDecimal currentTotalAmount = getTotalAmount();
            for (InstallmentEntry installmentEntry : installmentEntriesList) {

                if (TreasuryConstants.isLessOrEqualThan(installmentEntry.getAmount(), currentTotalAmount)) {
                    currentTotalAmount = currentTotalAmount.subtract(installmentEntry.getAmount());
                } else if (TreasuryConstants.isPositive(currentTotalAmount)) {
                    BigDecimal diffForNewDebitEntry = installmentEntry.getAmount().subtract(currentTotalAmount);

                    installmentEntry.setAmount(currentTotalAmount);
                    InstallmentEntry.create(newDebitEntry, diffForNewDebitEntry, installmentEntry.getInstallment());

                    currentTotalAmount = BigDecimal.ZERO;
                } else {
                    installmentEntry.setDebitEntry(newDebitEntry);
                }
            }
        });

        // ANIL 2025-04-03 (#qubIT-Fenix-6786)
        //
        // The new partitioned new debit entry must be in the same payment groups
        // of the old debit entry
        getPaymentInvoiceEntriesGroupsSet().forEach(g -> g.addInvoiceEntries(newDebitEntry));

        return newDebitEntry;
    }

    static void applyAdditionalRelationConnectionsOfDebitEntry(DebitEntry oldDebitEntry, DebitEntry newDebitEntry) {
        CONNECT_RELATIONS_WHEN_SPLITTING_DEBIT_ENTRY_CONSUMERS_LIST.forEach(
                consumer -> consumer.accept(oldDebitEntry, newDebitEntry));
    }

    private void annulAllActiveSibsPaymentRequests() {
        getPaymentRequestsSet().stream() //
                .filter(request -> request.isInCreatedState() || request.isInRequestedState()) //
                .filter(request -> request instanceof SibsPaymentRequest)
                .forEach(request -> ((SibsPaymentRequest) request).anull());
    }

    @Override
    public void setNetExemptedAmount(BigDecimal netExemptedAmount) {
        super.setNetExemptedAmount(netExemptedAmount);
        super.setExemptedAmount(netExemptedAmount);
    }

    @Override
    @Deprecated
    public BigDecimal getExemptedAmount() {
        return super.getExemptedAmount();
    }

    @Override
    @Deprecated
    public void setExemptedAmount(BigDecimal exemptedAmount) {
        super.setExemptedAmount(exemptedAmount);
    }

    public Set<DebitEntry> getAllSplittedDebitEntriesSet() {
        final Set<DebitEntry> result = new HashSet<>();
        // To collect all splitted debit entries that are part of this debit entry

        // 1. Reach to the root debit entry

        DebitEntry root = this;
        while (root.getSplittingOriginDebitEntry() != null) {
            root = root.getSplittingOriginDebitEntry();
        }

        result.add(root);

        // 2. From root navigate to each descendent and add to the set
        final BiConsumer<DebitEntry, BiConsumer> func = (debitEntry, consumer) -> {
            result.add(debitEntry);
            debitEntry.getSplittedDebitEntriesSet().stream().forEach(child -> consumer.accept(child, consumer));
        };

        func.accept(root, func);

        return result;
    }

    @Override
    public Set<TreasuryExemption> getAssociatedTreasuryExemptions() {
        return getTreasuryExemptionsSet();
    }

    public DateTime getLastAmountsChangeDate() {
        return getDebitEntryChangeAmountsLogsSet().stream().sorted(DebitEntryChangeAmountsLog.COMPARE_BY_CHANGE_DATE.reversed())
                .findFirst().map(DebitEntryChangeAmountsLog::getChangeDate).orElse(null);
    }

    public String getLastAmountsChangeResponsible() {
        return getDebitEntryChangeAmountsLogsSet().stream().sorted(DebitEntryChangeAmountsLog.COMPARE_BY_CHANGE_DATE.reversed())
                .findFirst().map(DebitEntryChangeAmountsLog::getResponsible).orElse(null);
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

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
        return findActive(treasuryEvent).filter(
                d -> (!trimmed && d.getDescription().equals(description)) || (trimmed && d.getDescription().trim()
                        .equals(description)));
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

//    @Deprecated
//    // TODO ANIL 2023-12-28: Replace with the extended version of this method
//    public static DebitEntry create(Optional<DebitNote> debitNote, DebtAccount debtAccount, TreasuryEvent treasuryEvent, Vat vat,
//            BigDecimal amount, LocalDate dueDate, Map<String, String> propertiesMap, Product product, String description,
//            BigDecimal quantity, InterestRate interestRate, DateTime entryDateTime) {
//
//        if (!isDebitEntryCreationAllowed(debtAccount, debitNote, product)) {
//            throw new TreasuryDomainException("error.DebitEntry.customer.not.active");
//        }
//
//        return _create(debitNote, debtAccount, treasuryEvent, vat, amount, dueDate, propertiesMap, product, description, quantity,
//                interestRate, entryDateTime, false, false, Optional.empty());
//    }

    public static DebitEntry create(FinantialEntity finantialEntity, DebtAccount debtAccount, TreasuryEvent treasuryEvent,
            Vat vat, BigDecimal amount, LocalDate dueDate, Map<String, String> propertiesMap, Product product, String description,
            BigDecimal quantity, InterestRate interestRate, DateTime entryDateTime, boolean academicalActBlockingSuspension,
            boolean blockAcademicActsOnDebt, DebitNote debitNote) {

        if (!isDebitEntryCreationAllowed(debtAccount, debitNote, product)) {
            throw new TreasuryDomainException("error.DebitEntry.customer.not.active");
        }

        return _create(finantialEntity, debtAccount, treasuryEvent, vat, amount, dueDate, propertiesMap, product, description,
                quantity, interestRate, entryDateTime, academicalActBlockingSuspension, blockAcademicActsOnDebt, debitNote);
    }

    public static DebitEntry createForImportationPurpose(FinantialEntity finantialEntity, DebtAccount debtAccount,
            TreasuryEvent treasuryEvent, Vat vat, BigDecimal amount, LocalDate dueDate, Map<String, String> propertiesMap,
            Product product, String description, BigDecimal quantity, InterestRate interestRate, DateTime entryDateTime,
            DebitNote debitNote) {

        return _create(finantialEntity, debtAccount, treasuryEvent, vat, amount, dueDate, propertiesMap, product, description,
                quantity, interestRate, entryDateTime, false, false, debitNote);
    }

    private static boolean isDebitEntryCreationAllowed(DebtAccount debtAccount, DebitNote debitNote, Product product) {
        if (debtAccount.getCustomer().isActive()) {
            return true;
        }

        if (debitNote != null && debitNote.getDocumentNumberSeries().getSeries().isRegulationSeries()) {
            return true;
        }

        return false;
    }

    private static DebitEntry _create(FinantialEntity finantialEntity, DebtAccount debtAccount, TreasuryEvent treasuryEvent,
            Vat vat, BigDecimal amount, LocalDate dueDate, Map<String, String> propertiesMap, Product product, String description,
            BigDecimal quantity, InterestRate interestRate, DateTime entryDateTime, boolean academicalActBlockingSuspension,
            boolean blockAcademicActsOnDebt, DebitNote debitNote) {

        if (finantialEntity == null && treasuryEvent != null) {
            finantialEntity = treasuryEvent.getAssociatedFinantialEntity();
        }

        final DebitEntry entry =
                new DebitEntry(finantialEntity, debtAccount, treasuryEvent, vat, amount, dueDate, propertiesMap, product,
                        description, quantity, null, entryDateTime, debitNote);

        if (interestRate != null) {
            InterestRate.createForDebitEntry(entry, interestRate);
        }

        entry.edit(description, treasuryEvent, dueDate, academicalActBlockingSuspension, blockAcademicActsOnDebt);

        return entry;
    }
}
