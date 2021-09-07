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

import static org.fenixedu.treasury.util.TreasuryConstants.divide;
import static org.fenixedu.treasury.util.TreasuryConstants.rationalVatRate;

import java.math.BigDecimal;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

public class CreditEntry extends CreditEntry_Base {

    protected CreditEntry(final FinantialDocument finantialDocument, final Product product, final Vat vat,
            final BigDecimal amountWithoutVat, String description, BigDecimal quantity, final DateTime entryDateTime,
            final DebitEntry debitEntry, final boolean fromExemption) {
        init(finantialDocument, product, vat, amountWithoutVat, description, quantity, entryDateTime, debitEntry, fromExemption);

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

    protected void init(final FinantialDocument finantialDocument, final Product product, final Vat vat,
            final BigDecimal amountWithoutVat, String description, BigDecimal quantity, final DateTime entryDateTime,
            final DebitEntry debitEntry, final boolean fromExemption) {
        super.init(finantialDocument, finantialDocument.getDebtAccount(), product, FinantialEntryType.CREDIT_ENTRY, vat,
                amountWithoutVat, description, quantity, entryDateTime);
        this.setDebitEntry(debitEntry);
        this.setFromExemption(fromExemption);
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
        if (getFromExemption() == true && getDebitEntry() == null) {
            throw new TreasuryDomainException("error.CreditEntry.from.exemption.requires.debit.entry");
        }

        if (getDebitEntry() != null && getDebitEntry().getProduct() != getProduct()) {
            throw new TreasuryDomainException("error.CreditEntry.product.must.be.the.same.as.debit.entry");
        }

        /* If it is from exemption then ensure that there is no credit entries 
         * from exemption created.
         */

        if (getFromExemption() == true && getDebitEntry().getCreditEntriesSet().size() > 1) {
            throw new TreasuryDomainException("error.CreditEntry.from.exemption.at.most.one.per.debit.entry");
        }

        if (this.getDebitEntry() != null) {
            if (TreasuryConstants.isGreaterThan(this.getDebitEntry().getTotalCreditedAmount(),
                    this.getDebitEntry().getTotalAmount())) {
                throw new TreasuryDomainException("error.CreditEntry.reated.debit.entry.invalid.total.credited.amount");
            }
        }

        if (Strings.isNullOrEmpty(getDescription())) {
            throw new TreasuryDomainException("error.CreditEntry.description.required");
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
        return DebitEntry.find(treasuryEvent).map(d -> d.getCreditEntriesSet()).reduce((a, b) -> Sets.union(a, b))
                .orElse(Sets.newHashSet()).stream();
    }

    public static Stream<? extends CreditEntry> findActive(final TreasuryEvent treasuryEvent) {
        return DebitEntry.findActive(treasuryEvent).map(d -> d.getCreditEntriesSet()).reduce((a, b) -> Sets.union(a, b))
                .orElse(Sets.newHashSet()).stream();
    }

    public static Stream<? extends CreditEntry> findActive(final TreasuryEvent treasuryEvent, final Product product) {
        return DebitEntry.findActive(treasuryEvent, product).map(d -> d.getCreditEntriesSet()).reduce((a, b) -> Sets.union(a, b))
                .orElse(Sets.newHashSet()).stream();
    }

    public static CreditEntry create(FinantialDocument finantialDocument, String description, Product product, Vat vat,
            BigDecimal amount, final DateTime entryDateTime, final DebitEntry debitEntry, BigDecimal quantity) {
        CreditEntry cr =
                new CreditEntry(finantialDocument, product, vat, amount, description, quantity, entryDateTime, debitEntry, false);
        return cr;
    }

    public static CreditEntry createFromExemption(final TreasuryExemption treasuryExemption,
            final FinantialDocument finantialDocument, final String description, final BigDecimal amount,
            final DateTime entryDateTime, final DebitEntry debitEntry) {

        if (treasuryExemption == null) {
            throw new TreasuryDomainException("error.CreditEntry.createFromExemption.requires.treasuryExemption");
        }

        final CreditEntry cr = new CreditEntry(finantialDocument, debitEntry.getProduct(), debitEntry.getVat(), amount,
                description, BigDecimal.ONE, entryDateTime, debitEntry, true);

        cr.recalculateAmountValues();

        return cr;
    }

    @Override
    public BigDecimal getOpenAmountWithInterests() {
        return getOpenAmount();
    }

    public CreditNote getCreditNote() {
        return (CreditNote) getFinantialDocument();
    }

    public CreditEntry splitCreditEntry(final BigDecimal remainingAmount) {
        if (!TreasuryConstants.isLessThan(remainingAmount, getOpenAmount())) {
            throw new TreasuryDomainException("error.CreditEntry.splitCreditEntry.remainingAmount.less.than.open.amount");
        }

        if (!getFinantialDocument().isPreparing()) {
            throw new TreasuryDomainException("error.CreditEntry.splitCreditEntry.finantialDocument.not.preparing");
        }

        final Currency currency = getDebtAccount().getFinantialInstitution().getCurrency();

        final BigDecimal remainingAmountWithoutVatDividedByQuantity = currency
                .getValueWithScale(divide(divide(remainingAmount, BigDecimal.ONE.add(rationalVatRate(this))), getQuantity()));

        final CreditNote newCreditNote = CreditNote.create(this.getDebtAccount(),
                getFinantialDocument().getDocumentNumberSeries(), getFinantialDocument().getDocumentDate(),
                ((CreditNote) getFinantialDocument()).getDebitNote(), getFinantialDocument().getOriginDocumentNumber());
        newCreditNote.setDocumentObservations(getFinantialDocument().getDocumentObservations());
        newCreditNote.setDocumentTermsAndConditions(getFinantialDocument().getDocumentTermsAndConditions());

        final BigDecimal newOpenAmountWithoutVatDividedByQuantity =
                divide(divide(getOpenAmount(), BigDecimal.ONE.add(rationalVatRate(this))), getQuantity());

        setAmount(newOpenAmountWithoutVatDividedByQuantity.subtract(remainingAmountWithoutVatDividedByQuantity));
        recalculateAmountValues();

        final CreditEntry newCreditEntry = create(newCreditNote, getDescription(), getProduct(), getVat(),
                remainingAmountWithoutVatDividedByQuantity, getEntryDateTime(), getDebitEntry(), getQuantity());
        newCreditEntry.setFromExemption(isFromExemption());

        if (TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()
                && getFinantialDocument().isExportedInLegacyERP()) {
            newCreditEntry.getFinantialDocument().setExportedInLegacyERP(true);
            newCreditEntry.getFinantialDocument().setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
        }

        return newCreditEntry;
    }

}
