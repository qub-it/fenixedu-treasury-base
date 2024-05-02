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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.CreditTreasuryExemption;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

public class CreditEntry extends CreditEntry_Base {

    protected CreditEntry(FinantialEntity finantialEntity, final FinantialDocument finantialDocument, final Product product,
            final Vat vat, final BigDecimal unitAmount, String description, BigDecimal quantity, final DateTime entryDateTime,
            final DebitEntry debitEntry, final TreasuryExemption treasuryExemption,
            Map<TreasuryExemption, BigDecimal> creditExemptionsMap) {
        init(finantialEntity, finantialDocument, product, vat, unitAmount, description, quantity, entryDateTime, debitEntry,
                treasuryExemption, creditExemptionsMap);
    }

    @Override
    public boolean isCreditNoteEntry() {
        return true;
    }

    @Override
    protected void init(final FinantialDocument finantialDocument, final DebtAccount debtAccount, final Product product,
            final FinantialEntryType finantialEntryType, final Vat vat, final BigDecimal amount, String description,
            BigDecimal quantity, final DateTime entryDateTime) {
        throw new RuntimeException("error.CreditEntry.use.init.without.finantialEntryType");
    }

    protected void init(FinantialEntity finantialEntity, FinantialDocument finantialDocument, Product product, final Vat vat,
            BigDecimal unitAmount, String description, BigDecimal quantity, DateTime entryDateTime, DebitEntry debitEntry,
            TreasuryExemption treasuryExemption, Map<TreasuryExemption, BigDecimal> creditExemptionsMap) {
        super.init(finantialDocument, finantialDocument.getDebtAccount(), product, FinantialEntryType.CREDIT_ENTRY, vat,
                unitAmount, description, quantity, entryDateTime);

        super.setFinantialEntity(finantialEntity);
        this.setDebitEntry(debitEntry);
        this.setFromExemption(treasuryExemption != null);
        this.setTreasuryExemption(treasuryExemption);

        if (creditExemptionsMap != null) {
            BigDecimal netExemptedAmount = creditExemptionsMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            creditExemptionsMap.forEach((exemptionToCredit, creditedNetExemptedAmount) -> CreditTreasuryExemption.create(this,
                    exemptionToCredit, creditedNetExemptedAmount));

            super.setNetExemptedAmount(netExemptedAmount);
        }

        recalculateAmountValues();

        checkRules();
    }

    @Override
    protected void checkRules() {
        super.checkRules();

        if (getFinantialDocument() != null && !(getFinantialDocument() instanceof CreditNote)) {
            throw new TreasuryDomainException("error.CreditEntry.finantialDocument.not.credit.entry.type");
        }

        // If from exemption then ensure debit entry is not null and the product is the same
        if (getFromExemption() && getDebitEntry() == null) {
            throw new TreasuryDomainException("error.CreditEntry.from.exemption.requires.debit.entry");
        }

        if (getDebitEntry() != null && getDebitEntry().getProduct() != getProduct()) {
            if (getFinantialDocument() == null || !getFinantialDocument().getDocumentNumberSeries().getSeries().isLegacy()) {
                throw new TreasuryDomainException("error.CreditEntry.product.must.be.the.same.as.debit.entry");
            }
        }

        /* If it is from exemption then ensure that there is no credit entries
         * from exemption created.
         */

        if (getFromExemption() && getTreasuryExemption() == null) {
            throw new TreasuryDomainException("error.CreditEntry.from.exemption.at.most.one.per.debit.entry");
        }

        if (this.getDebitEntry() != null) {
            if (TreasuryConstants.isGreaterThan(this.getDebitEntry().getTotalCreditedAmountWithVat(),
                    this.getDebitEntry().getTotalAmount())) {
                throw new TreasuryDomainException("error.CreditEntry.reated.debit.entry.invalid.total.credited.amount");
            }
        }

        if (Strings.isNullOrEmpty(getDescription())) {
            throw new TreasuryDomainException("error.CreditEntry.description.required");
        }

        if (getDebitEntry() != null) {
            if (!TreasuryConstants.isEqual(getNetExemptedAmount(),
                    getCreditedExemptionsMap().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add))) {
                throw new TreasuryDomainException(
                        "error.CreditEntry.creditExemptionsTotalAmount.mismatch.with.netExemptedAmount");
            }
        }

        if (getTreasuryExemption() != null && TreasuryConstants.isPositive(getNetExemptedAmount())) {
            throw new TreasuryDomainException(
                    "error.CreditEntry.netExemptedAmount.not.supported.with.creditEntry.with.exemption");
        }
    }

    public boolean isFromExemption() {
        return getFromExemption();
    }

    @Override
    public void delete() {
        this.setDebitEntry(null);
        super.delete();
    }

    public void edit(final String description) {
        if (isFromExemption()) {
            throw new TreasuryDomainException("error.CreditEntry.cannot.edit.due.to.exemption.origin");
        }

        this.setDescription(description);

        this.checkRules();
    }

    public static Stream<CreditEntry> findAll() {
        return FinantialDocumentEntry.findAll().filter(f -> f instanceof CreditEntry).map(CreditEntry.class::cast);
    }

    @Override
    public LocalDate getDueDate() {
        return getEntryDateTime().toLocalDate();
    }

    public static Stream<? extends CreditEntry> find(final CreditNote creditNote) {
        return creditNote.getFinantialDocumentEntriesSet().stream().filter(f -> f instanceof CreditEntry)
                .map(CreditEntry.class::cast);
    }

    public static Stream<? extends CreditEntry> find(final TreasuryEvent treasuryEvent) {
        return Stream.concat(DebitEntry.find(treasuryEvent).flatMap(d -> d.getCreditEntriesSet().stream()),
                treasuryEvent.getCreditEntriesSet().stream());
    }

    public static Stream<? extends CreditEntry> findActive(final TreasuryEvent treasuryEvent) {
        return Stream.concat(
                DebitEntry.findActive(treasuryEvent)
                        .flatMap(d -> d.getCreditEntriesSet().stream().filter(ce -> !ce.isAnnulled())),
                treasuryEvent.getCreditEntriesSet().stream().filter(ce -> !ce.isAnnulled()));
    }

    public static Stream<? extends CreditEntry> findActive(final TreasuryEvent treasuryEvent, final Product product) {
        return Stream.concat(
                DebitEntry.findActive(treasuryEvent, product)
                        .flatMap(d -> d.getCreditEntriesSet().stream().filter(ce -> !ce.isAnnulled())),
                treasuryEvent.getCreditEntriesSet().stream().filter(ce -> ce.getProduct() == product)
                        .filter(ce -> !ce.isAnnulled()));
    }

    public static CreditEntry create(FinantialDocument finantialDocument, String description, Product product, Vat vat,
            BigDecimal unitAmount, DateTime entryDateTime, DebitEntry debitEntry, BigDecimal quantity,
            Map<TreasuryExemption, BigDecimal> creditExemptionsMap) {
        if (TreasuryDebtProcessMainService.isFinantialDocumentEntryAnnullmentActionBlocked(debitEntry)) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.or.credit.due.to.existing.active.debt.process");
        }

        if (debitEntry == null) {
            throw new TreasuryDomainException("error.CreditEntry.debitEntry.required");
        }

        CreditEntry cr = new CreditEntry(debitEntry.getFinantialEntity(), finantialDocument, product, vat, unitAmount,
                description, quantity, entryDateTime, debitEntry, null, creditExemptionsMap);
        return cr;
    }

    public static CreditEntry create(FinantialEntity finantialEntity, FinantialDocument finantialDocument, String description,
            Product product, Vat vat, BigDecimal unitAmount, DateTime entryDateTime, BigDecimal quantity) {
        CreditEntry cr = new CreditEntry(finantialEntity, finantialDocument, product, vat, unitAmount, description, quantity,
                entryDateTime, null, null, null);

        return cr;
    }

    public static CreditEntry createFromExemption(final TreasuryExemption treasuryExemption,
            final FinantialDocument finantialDocument, final String description, final BigDecimal unitAmount,
            final DateTime entryDateTime, BigDecimal quantity) {
        DebitEntry debitEntry = treasuryExemption.getDebitEntry();

        if (TreasuryDebtProcessMainService.isFinantialDocumentEntryAnnullmentActionBlocked(debitEntry)) {
            throw new TreasuryDomainException("error.DebitEntry.cannot.annul.or.credit.due.to.existing.active.debt.process");
        }

        if (treasuryExemption == null) {
            throw new TreasuryDomainException("error.CreditEntry.createFromExemption.requires.treasuryExemption");
        }

        final CreditEntry cr = new CreditEntry(debitEntry.getFinantialEntity(), finantialDocument, debitEntry.getProduct(),
                debitEntry.getVat(), unitAmount, description, quantity, entryDateTime, debitEntry, treasuryExemption, null);

        return cr;
    }

    @Override
    public BigDecimal getOpenAmountWithInterests() {
        return getOpenAmount();
    }

    public CreditNote getCreditNote() {
        return (CreditNote) getFinantialDocument();
    }

    // TODO: Review the calculation of remainingUnitAmount, which might be losing
    // precision
    public CreditEntry splitCreditEntry(BigDecimal amountWithVatOfNewCreditEntry) {
        if (!TreasuryConstants.isLessThan(amountWithVatOfNewCreditEntry, getOpenAmount())) {
            throw new TreasuryDomainException("error.CreditEntry.splitCreditEntry.remainingAmount.less.than.open.amount");
        }

        if (!getFinantialDocument().isPreparing()) {
            throw new TreasuryDomainException("error.CreditEntry.splitCreditEntry.finantialDocument.not.preparing");
        }

        BigDecimal oldNetAmount = getNetAmount();
        BigDecimal oldAmountWithVat = getAmountWithVat();

        CreditNote newCreditNote = CreditNote.create(this.getDebtAccount(), getCreditNote().getDocumentNumberSeries(),
                getCreditNote().getDocumentDate(), getCreditNote().getDebitNote(), getCreditNote().getOriginDocumentNumber());

        newCreditNote.setDocumentObservations(getCreditNote().getDocumentObservations());
        newCreditNote.setDocumentTermsAndConditions(getCreditNote().getDocumentTermsAndConditions());
        newCreditNote.editPropertiesMap(getCreditNote().getPropertiesMap());
        newCreditNote.setCertificationOriginDocumentReference(getCreditNote().getCertificationOriginDocumentReference());
        newCreditNote.setCloseDate(getCreditNote().getCloseDate());

        // TODO: Check if precision is lost in cents
        BigDecimal unitAmountOfNewCreditEntry =
                Currency.getValueWithScale(TreasuryConstants.divide(TreasuryConstants.divide(amountWithVatOfNewCreditEntry,
                        BigDecimal.ONE.add(TreasuryConstants.rationalVatRate(this))), getQuantity()));

        // TODO: the amountPerUnit should be truncated to fewer decimal places?
        BigDecimal openUnitAmountOfThisCreditEntry = TreasuryConstants.divide(
                TreasuryConstants.divide(getOpenAmount(), BigDecimal.ONE.add(TreasuryConstants.rationalVatRate(this))),
                getQuantity());

        BigDecimal newOpenUnitAmountOfThisCreditEntry = openUnitAmountOfThisCreditEntry.subtract(unitAmountOfNewCreditEntry);
        BigDecimal ratioBetweenOldAndNewAmounts =
                TreasuryConstants.divide(newOpenUnitAmountOfThisCreditEntry, openUnitAmountOfThisCreditEntry);

        Map<TreasuryExemption, BigDecimal> exemptionsMapForCurrentCreditEntry =
                getCreditedExemptionsMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> Currency
                        .getValueWithScale(TreasuryConstants.defaultScale(e.getValue()).multiply(ratioBetweenOldAndNewAmounts))));

        Map<TreasuryExemption, BigDecimal> exemptionsMapForNewCreditEntry =
                getCreditedExemptionsMap().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(),
                        e -> e.getValue().subtract(exemptionsMapForCurrentCreditEntry.get(e.getKey()))));

        setAmount(newOpenUnitAmountOfThisCreditEntry);
        setNetExemptedAmount(exemptionsMapForCurrentCreditEntry.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        resetCreditedNetExemptionAmounts(exemptionsMapForCurrentCreditEntry);

        recalculateAmountValues();

        CreditEntry newCreditEntry = null;

        if (getDebitEntry() != null) {
            newCreditEntry = create(newCreditNote, getDescription(), getProduct(), getVat(), unitAmountOfNewCreditEntry,
                    getEntryDateTime(), getDebitEntry(), getQuantity(), exemptionsMapForNewCreditEntry);
        } else {
            newCreditEntry = create(getFinantialEntity(), newCreditNote, getDescription(), getProduct(), getVat(),
                    unitAmountOfNewCreditEntry, getEntryDateTime(), getQuantity());
        }

        newCreditEntry.setFromExemption(isFromExemption());

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()
                && getFinantialDocument().isExportedInLegacyERP()) {
            newCreditEntry.getFinantialDocument().setExportedInLegacyERP(true);
            newCreditEntry.getFinantialDocument().setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
        }

        // Ensure the netAmount before and after are equals
        if (oldNetAmount.compareTo(getNetAmount().add(newCreditEntry.getNetAmount())) != 0) {
            throw new IllegalStateException("error.CreditEntry.splitCreditEntry.netAmount.before.after.not.equal");
        }

        // Ensure the amountWithVat before and after are equals
        if (oldAmountWithVat.compareTo(getAmountWithVat().add(newCreditEntry.getAmountWithVat())) != 0) {
            throw new IllegalStateException("error.CreditEntry.splitCreditEntry.amountWithVat.before.after.not.equal");
        }

        return newCreditEntry;
    }

    private void resetCreditedNetExemptionAmounts(Map<TreasuryExemption, BigDecimal> exemptionsMap) {
        exemptionsMap.forEach((exemption, netExemptedAmount) -> getCreditTreasuryExemptionsSet().stream()
                .filter(e -> e.getTreasuryExemption() == exemption).findFirst()
                .ifPresent(cte -> cte.editCreditedNetExemptedAmount(netExemptedAmount)));
    }

    @Override
    public FinantialEntity getFinantialEntity() {
        if (super.getFinantialEntity() != null) {
            return super.getFinantialEntity();
        }

        if (getDebitEntry() != null) {
            return getDebitEntry().getFinantialEntity();
        }

        return null;
    }

    // Used in screens to display negative amounts for credit entries
    @Override
    public BigDecimal getUiTotalAmount() {
        if (Boolean.TRUE.equals(TreasurySettings.getInstance().getDisplayNegativeAmountsForCreditEntries())) {
            return super.getUiTotalAmount().negate();
        }

        return super.getUiTotalAmount();
    }

    // Used in screens to display negative amounts for credit entries
    @Override
    public BigDecimal getUiOpenAmount() {
        if (Boolean.TRUE.equals(TreasurySettings.getInstance().getDisplayNegativeAmountsForCreditEntries())) {
            return super.getUiOpenAmount().negate();
        }

        return super.getUiOpenAmount();
    }

    // Used in screens to display negative amounts for credit entries
    @Override
    public BigDecimal getUiOpenAmountWithInterests() {
        if (Boolean.TRUE.equals(TreasurySettings.getInstance().getDisplayNegativeAmountsForCreditEntries())) {
            return super.getUiOpenAmountWithInterests().negate();
        }

        return super.getUiOpenAmountWithInterests();
    }

    @Override
    public BigDecimal getUiNetExemptedAmount() {
        if (Boolean.TRUE.equals(TreasurySettings.getInstance().getDisplayNegativeAmountsForCreditEntries())) {
            return super.getUiNetExemptedAmount().negate();
        }

        return super.getUiNetExemptedAmount();
    }

    @Override
    public TreasuryEvent getTreasuryEvent() {
        if (super.getTreasuryEvent() != null) {
            return super.getTreasuryEvent();
        }

        return super.getDebitEntry() != null ? super.getDebitEntry().getTreasuryEvent() : null;
    }

    public Map<TreasuryExemption, BigDecimal> getCreditedExemptionsMap() {
        return getCreditTreasuryExemptionsSet().stream().collect(Collectors.toMap(CreditTreasuryExemption::getTreasuryExemption,
                CreditTreasuryExemption::getCreditedNetExemptedAmount));
    }
}
