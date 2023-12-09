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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.event.TreasuryEvent.TreasuryEventKeys;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPayment;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentEntry;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentSettlementEntry;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlan;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.tariff.InterestRate;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

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

        this.setSplittingOriginDebitEntry(null);
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
        setNetExemptedAmount(BigDecimal.ZERO);
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

    @Override
    protected BigDecimal calculateNetAmount() {
        BigDecimal netAmount = Currency.getValueWithScale(getQuantity().multiply(getAmount()));
        netAmount = netAmount.subtract(getNetExemptedAmount());

        return netAmount;
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
    public DebitEntry createInterestRateDebitEntry(final InterestRateBean interest, final DateTime when,
            final Optional<DebitNote> debitNote) {
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
                    createCreditEntry(now, getDescription(), null, null, netExemptedAmount, treasuryExemption, null);

            closeCreditEntryIfPossible(reason, now, creditEntryFromExemption);

            creditEntryFromExemption.getFinantialDocument().setDocumentObservations(String.format("[%s] - %s",
                    treasuryExemption.getTreasuryExemptionType().getName().getContent(), treasuryExemption.getReason()));

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
            CreditNote creditNote) {
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

            creditNote = CreditNote.create(this.getDebtAccount(), documentNumberSeries, documentDate, debitNote,
                    debitNote.getUiDocumentNumber());
        }

        if (treasuryExemption != null && Boolean.TRUE.equals(getCalculatedAmountsOverriden())) {
            throw new TreasuryDomainException("error.DebitEntry.exempt.not.possible.due.to.overriden.calculated.amounts");
        }

        if (Boolean.TRUE.equals(getCalculatedAmountsOverriden())
                && TreasuryConstants.isLessThan(netAmountForCredit, getNetAmount())) {
            throw new TreasuryDomainException(
                    "error.DebitEntry.createCreditEntry.for.overriden.calculated.amounts.only.possible.to.credit.by.full.amount");
        }

        if (!Strings.isNullOrEmpty(documentObservations)) {
            creditNote.setDocumentObservations(documentObservations);
        }

        if (!Strings.isNullOrEmpty(documentTermsAndConditions)) {
            creditNote.setDocumentTermsAndConditions(documentTermsAndConditions);
        }

        if (!TreasuryConstants.isPositive(netAmountForCredit)) {
            throw new TreasuryDomainException("error.DebitEntry.createCreditEntry.amountForCredit.not.positive");
        }

        CreditEntry creditEntry = null;
        if (treasuryExemption != null) {
            creditEntry = CreditEntry.createFromExemption(treasuryExemption, creditNote, description, netAmountForCredit,
                    new DateTime(), this, BigDecimal.ONE);
        } else {
            creditEntry = CreditEntry.create(creditNote, description, getProduct(), getVat(), netAmountForCredit, documentDate,
                    this, BigDecimal.ONE);

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
        DocumentNumberSeries documentNumberSeriesSettlementNote = DocumentNumberSeries.find(
                FinantialDocumentType.findForSettlementNote(), this.getFinantialDocument().getDocumentNumberSeries().getSeries());

        // ANIL 2023-11-29: Ensure the associated finantial document is in closed state

        if (!getDebitNote().isClosed()) {
            throw new IllegalStateException("error.DebitEntry.closeCreditEntryIfPossible.invalid.debitNote.state");
        }

        boolean splitCreditEntriesWithSettledAmount =
                getDebtAccount().getFinantialInstitution().getSplitCreditEntriesWithSettledAmount();

        boolean isToCloseCreditNoteWhenCreated = getDebtAccount().getFinantialInstitution().isToCloseCreditNoteWhenCreated();
        boolean isInvoiceRegistrationByTreasuryCertification =
                getDebtAccount().getFinantialInstitution().isInvoiceRegistrationByTreasuryCertification();

        if (isInvoiceRegistrationByTreasuryCertification
                && this.getFinantialDocument().getDocumentNumberSeries().getSeries().isLegacy()) {
            documentNumberSeriesSettlementNote = DocumentNumberSeries.find(FinantialDocumentType.findForSettlementNote(),
                    Series.findUniqueDefault(this.getDebtAccount().getFinantialInstitution()).get());
        }

        if (creditEntry.getFinantialDocument().isAnnulled()) {
            throw new TreasuryDomainException("error.DebitEntry.closeCreditEntryIfPossible.creditEntry.is.annulled");
        }

        if (!isInvoiceRegistrationByTreasuryCertification && !creditEntry.getFinantialDocument().isPreparing()
                && !isToCloseCreditNoteWhenCreated) {
            return;
        }

        BigDecimal minimumOpenAmount = creditEntry.getOpenAmount();

        if (TreasuryConstants.isLessThan(this.getOpenAmount(), creditEntry.getOpenAmount())) {
            minimumOpenAmount = this.getOpenAmount();
        }

        if (!TreasuryConstants.isPositive(minimumOpenAmount)) {
            return;
        }

        if (splitCreditEntriesWithSettledAmount && TreasuryConstants.isLessThan(minimumOpenAmount, creditEntry.getOpenAmount())
                && creditEntry.getFinantialDocument().isPreparing()) {
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
                SettlementNote.create(this.getDebtAccount(), documentNumberSeriesSettlementNote, now, now, "", null);
        settlementNote
                .setDocumentObservations(reason + " - [" + loggedUsername + "] " + new DateTime().toString("YYYY-MM-dd HH:mm"));

        SettlementEntry.create(creditEntry, settlementNote, minimumOpenAmount,
                reasonDescription + ": " + creditEntry.getDescription(), now, false);

        SettlementEntry debitSettlementEntry = SettlementEntry.create(this, settlementNote, minimumOpenAmount,
                reasonDescription + ": " + getDescription(), now, false);

        InstallmentSettlementEntry.settleInstallmentEntriesOfDebitEntry(debitSettlementEntry);

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
        Optional<SettlementNote> settlementNote = getSettlementEntriesSet().stream()
                .filter(s -> !s.getFinantialDocument().isAnnulled()).map(s -> ((SettlementNote) s.getFinantialDocument()))
                .max(Comparator.comparing(SettlementNote::getPaymentDate));
        if (!settlementNote.isPresent()) {
            return null;
        }

        return settlementNote.get().getPaymentDate();
    }

    @Override
    @Deprecated
    // TODO deprecated - this should be renamed as netExemptedAmount         
    public BigDecimal getExemptedAmount() {
        return super.getExemptedAmount();
    }

    @Override
    @Deprecated
    // TODO deprecated - this should be renamed as netExemptedAmount         
    public void setExemptedAmount(BigDecimal exemptedAmount) {
        super.setExemptedAmount(exemptedAmount);
    }

    // TODO: Replace exemptedAmount this by this
    public BigDecimal getNetExemptedAmount() {
        return super.getExemptedAmount();
    }

    // TODO: Replace exemptedAmount this by this
    public void setNetExemptedAmount(BigDecimal exemptedAmount) {
        super.setExemptedAmount(exemptedAmount);
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
                    .filter(se -> ((CreditEntry) se.getInvoiceEntry()).getDebitEntry() == this).anyMatch(se -> TreasuryConstants
                            .isGreaterOrEqualThan(se.getAmount(), debitEntrySettlementEntry.getAmount()))) {
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

    public void changeInterestRate(InterestRate oldInterestRate) {
        if (this.getInterestRate() != null && this.getInterestRate() != oldInterestRate) {
            oldInterestRate.delete();
        }

        checkRules();
    }

    @Deprecated
    // TODO: Replace by getTotalCreditedAmountWithVat
    public BigDecimal getTotalCreditedAmount() {
        BigDecimal totalCreditedAmount = BigDecimal.ZERO;
        for (CreditEntry credits : this.getCreditEntriesSet()) {
            if (credits.getFinantialDocument() == null || !credits.getFinantialDocument().isAnnulled()) {
                totalCreditedAmount = totalCreditedAmount.add(credits.getTotalAmount());
            }
        }
        return this.getCurrency().getValueWithScale(totalCreditedAmount);
    }

    public BigDecimal getTotalCreditedAmountWithVat() {
        return getTotalCreditedAmount();
    }

    @Deprecated
    // TODO: Replace by getAvailableAmountWithVatForCredit
    public BigDecimal getAvailableAmountForCredit() {
        return this.getCurrency().getValueWithScale(this.getTotalAmount().subtract(getTotalCreditedAmount()));
    }

    public BigDecimal getAvailableAmountWithVatForCredit() {
        return getAvailableAmountForCredit();
    }

    public BigDecimal getAvailableNetAmountForCredit() {
        BigDecimal creditedNetAmount = getCreditEntriesSet().stream()
                .filter(c -> c.getFinantialDocument() == null || !c.getFinantialDocument().isAnnulled())
                .map(CreditEntry::getNetAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        // Scale should not be necessary. Assume the netAmount is scaled to Currency scale
        return this.getNetAmount().subtract(creditedNetAmount);
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

    public Set<SibsPaymentRequest> getSibsPaymentRequests() {
        return getSibsPaymentRequestsSet();
    }

    public Set<SibsPaymentRequest> getSibsPaymentRequestsSet() {
        return getPaymentRequestsSet().stream().filter(r -> r instanceof SibsPaymentRequest).map(SibsPaymentRequest.class::cast)
                .collect(Collectors.toSet());
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
            throw new TreasuryDomainException("error.DebitEntry.annul.debit.entry.requires.reason");
        }

        if (TreasuryDebtProcessMainService.isFinantialDocumentEntryAnnullmentActionBlocked(this)) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.or.credit.due.to.existing.active.debt.process");
        }

        final DebitNote debitNote = DebitNote.create(getDebtAccount(), DocumentNumberSeries
                .findUniqueDefault(FinantialDocumentType.findForDebitNote(), getDebtAccount().getFinantialInstitution()).get(),
                new DateTime());

        setFinantialDocument(debitNote);

        debitNote.anullDebitNoteWithCreditNote(reason, true);
    }

    /**
     * The purpose of this method is to avoid rewriting the logic
     * of checking if the debit entry is in finantial document or not
     * and to annul the interest debit entries
     */
    public void annulOnlyThisDebitEntryAndInterestsInBusinessContext(String reason) {
        // Ensure interests are annuled
        getInterestDebitEntriesSet().stream().forEach(d -> d.annulOnlyThisDebitEntryAndInterestsInBusinessContext(reason));

        annulOnEvent();
        if (isAnnulled() || !TreasuryConstants.isPositive(getAvailableNetAmountForCredit())) {
            return;
        }

        if (getFinantialDocument() != null) {
            if (getFinantialDocument().isPreparing()) {
                removeFromDocument();
                annulDebitEntry(reason);
            } else {
                creditDebitEntry(getAvailableNetAmountForCredit(), reason, false);
                annulOnEvent();
            }
        } else {
            annulDebitEntry(reason);
        }
    }

    @Atomic
    public void creditDebitEntry(BigDecimal netAmountForCredit, String reason, boolean closeWithOtherDebitEntriesOfDebitNote) {

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

        if (!TreasuryConstants.isPositive(netAmountForCredit)) {
            throw new TreasuryDomainException("error.DebitEntry.credit.debit.entry.amountToCreditWithVat.must.be.positive");
        }

        if (!TreasuryConstants.isLessOrEqualThan(netAmountForCredit, getAvailableNetAmountForCredit())) {
            throw new TreasuryDomainException(
                    "error.DebitEntry.credit.debit.entry.amountToCreditWithVat.must.be.less.or.equal.than.amountAvailableForCredit");
        }

        final DateTime now = new DateTime();
        final CreditEntry creditEntry = createCreditEntry(now, getDescription(), null, null, netAmountForCredit, null, null);

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

    public Set<SibsPaymentRequest> getActiveSibsPaymentRequestsOfPendingDebitEntries() {
        return getPaymentRequestsSet().stream() //
                .filter(p -> p instanceof SibsPaymentRequest) //
                .map(SibsPaymentRequest.class::cast) //
                .filter(p -> p.getState() == PaymentReferenceCodeStateType.UNUSED
                        || p.getState() == PaymentReferenceCodeStateType.USED) //
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
    public FinantialEntity getAssociatedFinantialEntity() {
        if (getTreasuryEvent() != null) {
            return getTreasuryEvent().getAssociatedFinantialEntity();
        }

        return null;
    }

    // TODO: Review the calculation of remainingUnitAmount, which might be losing
    // precision
    public DebitEntry splitDebitEntry(BigDecimal withAmountWithVatOfNewDebitEntry) {
        if (!TreasuryConstants.isLessThan(withAmountWithVatOfNewDebitEntry, getOpenAmount())) {
            throw new IllegalStateException("error.DebitEntry.splitDebitEntry.remainingAmount.less.than.open.amount");
        }

        if (getFinantialDocument() != null && !getFinantialDocument().isPreparing()) {
            throw new IllegalStateException("error.DebitEntry.splitDebitEntry.finantialDocument.not.preparing");
        }

        if (getFinantialDocument() != null && getFinantialDocument().isExportedInLegacyERP()) {
            throw new IllegalStateException(
                    "error.DebitEntry.splitDebitEntry.not.supported.for.finantialDocument.exportedInLegacyERP");
        }

        annulAllActiveSibsPaymentRequests();

        BigDecimal oldNetAmount = getNetAmount();
        BigDecimal oldAmountWithVat = getAmountWithVat();

        Optional<DebitNote> newDebitNote = Optional.empty();

        if (getDebitNote() != null) {
            newDebitNote = Optional.of(DebitNote.create(this.getDebtAccount(), getDebitNote().getPayorDebtAccount(),
                    getDebitNote().getDocumentNumberSeries(), getDebitNote().getDocumentDate(),
                    getDebitNote().getDocumentDueDate(), getDebitNote().getOriginDocumentNumber()));

            newDebitNote.get().setDocumentObservations(getFinantialDocument().getDocumentObservations());
            newDebitNote.get().setDocumentTermsAndConditions(getFinantialDocument().getDocumentTermsAndConditions());
            newDebitNote.get().editPropertiesMap(getDebitNote().getPropertiesMap());
            newDebitNote.get().setCloseDate(getDebitNote().getCloseDate());
            newDebitNote.get()
                    .setLegacyERPCertificateDocumentReference(getDebitNote().getLegacyERPCertificateDocumentReference());
        }

        // TODO: Check if precision is lost in cents
        BigDecimal withUnitAmountOfNewDebitEntry =
                Currency.getValueWithScale(TreasuryConstants.divide(TreasuryConstants.divide(withAmountWithVatOfNewDebitEntry,
                        BigDecimal.ONE.add(TreasuryConstants.rationalVatRate(this))), getQuantity()));

        // TODO: the amountPerUnit should be truncated to fewer decimal places?
        BigDecimal openUnitAmountOfThisDebitEntry = TreasuryConstants.divide(
                TreasuryConstants.divide(getOpenAmount(), BigDecimal.ONE.add(TreasuryConstants.rationalVatRate(this))),
                getQuantity());

        setAmount(openUnitAmountOfThisDebitEntry.subtract(withUnitAmountOfNewDebitEntry));
        recalculateAmountValues();

        DebitEntry newDebitEntry = _create(newDebitNote, getDebtAccount(), getTreasuryEvent(), getVat(),
                withUnitAmountOfNewDebitEntry, getDueDate(), getPropertiesMap(), getProduct(), getDescription(), getQuantity(),
                getInterestRate(), getEntryDateTime(), isAcademicalActBlockingSuspension(), isBlockAcademicActsOnDebt());

        newDebitEntry.setSplittingOriginDebitEntry(this);

        // TODO: ANIL 2023-12-07: This is wrong, the amount of the new installment entries
        // must be proportional with the amount of the new debit entry open amount and the installment entry amount
        //
//        if (getInstallmentEntrySet().stream()
//                .anyMatch(installmentEntry -> installmentEntry.getInstallment().getPaymentPlan().getState().isOpen())) {
//
//            getInstallmentEntrySet().stream()
//                    .filter(installmentEntry -> installmentEntry.getInstallment().getPaymentPlan().getState().isOpen())
//                    .forEach(installmentEntry -> {
//                        InstallmentEntry.create(newDebitEntry, newDebitEntry.getOpenAmount(),
//                                installmentEntry.getInstallment();
//                    });
//        }

        // TODO: ANIL 2023-12-07: For now just throw an exception if open payment plans are envolved
        if (getOpenPaymentPlan() != null) {
            throw new IllegalStateException("error.DebitEntry.splitDebitEntry.not.supported.for.open.payment.plans");
        }

        // Ensure the netAmount before and after are equals
        if (oldNetAmount.compareTo(getNetAmount().add(newDebitEntry.getNetAmount())) != 0) {
            throw new IllegalStateException("error.DebitEntry.splitDebitEntry.netAmount.before.after.not.equal");
        }

        // Ensure the amountWithVat before and after are equals
        if (oldAmountWithVat.compareTo(getAmountWithVat().add(newDebitEntry.getAmountWithVat())) != 0) {
            throw new IllegalStateException("error.DebitEntry.splitDebitEntry.amountWithVat.before.after.not.equal");
        }

        return newDebitEntry;
    }

    private void annulAllActiveSibsPaymentRequests() {
        getPaymentRequestsSet().stream() //
                .filter(request -> request.isInCreatedState() || request.isInRequestedState()) //
                .filter(request -> request instanceof SibsPaymentRequest)
                .forEach(request -> ((SibsPaymentRequest) request).anull());
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    @Deprecated
    // TODO ANIL 2023-12-07: This method is to old and has some errors. It is only used with
    // the copy of debit note. It needs a review
    static DebitEntry _copyDebitEntry(final DebitEntry debitEntryToCopy, final DebitNote debitNoteToAssociate,
            final boolean applyExemption) {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        String loggedUsername = services.getLoggedUsername();

        final Map<String, String> propertiesMap = Maps.newHashMap(
                debitEntryToCopy.getPropertiesMap() != null ? debitEntryToCopy.getPropertiesMap() : Maps.newHashMap());
        propertiesMap.put(TreasuryEvent.TreasuryEventKeys.COPIED_FROM_DEBIT_ENTRY_ID.getDescriptionI18N()
                .getContent(TreasuryConstants.DEFAULT_LANGUAGE), debitEntryToCopy.getExternalId());
        propertiesMap.put(TreasuryEvent.TreasuryEventKeys.COPY_DEBIT_ENTRY_RESPONSIBLE.getDescriptionI18N()
                .getContent(TreasuryConstants.DEFAULT_LANGUAGE), StringUtils.isNotEmpty(loggedUsername) ? loggedUsername : "");

        final DebitEntry result = DebitEntry.create(Optional.ofNullable(debitNoteToAssociate), debitEntryToCopy.getDebtAccount(),
                debitEntryToCopy.getTreasuryEvent(), debitEntryToCopy.getVat(),
                debitEntryToCopy.getAmount().add(debitEntryToCopy.getNetExemptedAmount()), debitEntryToCopy.getDueDate(),
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

        if (applyExemption) {
            debitEntryToCopy.getTreasuryExemptionsSet().forEach(exemption -> {
                TreasuryExemption.create(exemption.getTreasuryExemptionType(), exemption.getReason(),
                        exemption.getNetAmountToExempt(), result);
            });
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

    public static DebitEntry create(Optional<DebitNote> debitNote, DebtAccount debtAccount, TreasuryEvent treasuryEvent, Vat vat,
            BigDecimal amount, LocalDate dueDate, Map<String, String> propertiesMap, Product product, String description,
            BigDecimal quantity, InterestRate interestRate, DateTime entryDateTime, boolean academicalActBlockingSuspension,
            boolean blockAcademicActsOnDebt) {

        DebitEntry result = create(debitNote, debtAccount, treasuryEvent, vat, amount, dueDate, propertiesMap, product,
                description, quantity, interestRate, entryDateTime);

        result.edit(description, treasuryEvent, dueDate, academicalActBlockingSuspension, blockAcademicActsOnDebt);

        return result;
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

        return entry;
    }

    private static DebitEntry _create(Optional<DebitNote> debitNote, DebtAccount debtAccount, TreasuryEvent treasuryEvent,
            Vat vat, BigDecimal amount, LocalDate dueDate, Map<String, String> propertiesMap, Product product, String description,
            BigDecimal quantity, InterestRate interestRate, DateTime entryDateTime, boolean academicalActBlockingSuspension,
            boolean blockAcademicActsOnDebt) {

        final DebitEntry entry = new DebitEntry(debitNote.orElse(null), debtAccount, treasuryEvent, vat, amount, dueDate,
                propertiesMap, product, description, quantity, null, entryDateTime);

        if (interestRate != null) {
            InterestRate.createForDebitEntry(entry, interestRate);
        }

        entry.edit(description, treasuryEvent, dueDate, academicalActBlockingSuspension, blockAcademicActsOnDebt);

        return entry;
    }
}
