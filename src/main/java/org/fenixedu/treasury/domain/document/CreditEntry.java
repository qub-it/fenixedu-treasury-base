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

    protected void init(final FinantialDocument finantialDocument, final Product product, final Vat vat, final BigDecimal amountWithoutVat,
            String description, BigDecimal quantity, final DateTime entryDateTime, final DebitEntry debitEntry,
            final boolean fromExemption) {
        super.init(finantialDocument, finantialDocument.getDebtAccount(), product, FinantialEntryType.CREDIT_ENTRY, vat, amountWithoutVat,
                description, quantity, entryDateTime);
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
            if (TreasuryConstants.isGreaterThan(this.getDebitEntry().getTotalCreditedAmount(), this.getDebitEntry().getTotalAmount())) {
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

        final BigDecimal newOpenAmountWithoutVatDividedByQuantity =
                divide(divide(getOpenAmount(), BigDecimal.ONE.add(rationalVatRate(this))), getQuantity());

        setAmount(newOpenAmountWithoutVatDividedByQuantity.subtract(remainingAmountWithoutVatDividedByQuantity));
        recalculateAmountValues();

        final CreditEntry newCreditEntry = create(newCreditNote, getDescription(), getProduct(), getVat(),
                remainingAmountWithoutVatDividedByQuantity, getEntryDateTime(), getDebitEntry(), getQuantity());
        newCreditEntry.setFromExemption(isFromExemption());

        if(TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices() && getFinantialDocument().isExportedInLegacyERP()) {
            newCreditEntry.getFinantialDocument().setExportedInLegacyERP(true);
            newCreditEntry.getFinantialDocument().setCloseDate(SAPExporter.ERP_INTEGRATION_START_DATE.minusSeconds(1));
        }
        
        return newCreditEntry;
    }

}
