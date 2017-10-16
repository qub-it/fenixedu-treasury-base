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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.CreditEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.DebitEntryBean;
import org.fenixedu.treasury.util.Constants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;;

public class SettlementEntry extends SettlementEntry_Base {

    public static final Comparator<SettlementEntry> COMPARATOR_BY_ENTRY_DATE_TIME = new Comparator<SettlementEntry>() {

        @Override
        public int compare(final SettlementEntry o1, final SettlementEntry o2) {
            int c = o1.getEntryDateTime().compareTo(o2.getEntryDateTime());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }

    };

    protected SettlementEntry() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
        setCloseDate(new DateTime());
    }

    @Override
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        this.setInvoiceEntry(null);

        super.delete();
    }

    protected SettlementEntry(final InvoiceEntry invoiceEntry, final SettlementNote finantialDocument, final BigDecimal amount,
            final String description, final DateTime entryDateTime, final boolean createInterestIfNeeded) {
        this();
        init(invoiceEntry, finantialDocument, amount, description, entryDateTime);

        if (invoiceEntry.isDebitNoteEntry()) {
            if (Constants.isEqual(invoiceEntry.getOpenAmount(), amount) && createInterestIfNeeded) {
                //Check if we need to create more interest for this debitEntry
                DebitEntry debitEntry = (DebitEntry) invoiceEntry;
                InterestRateBean undebitedInterestValue = debitEntry.calculateUndebitedInterestValue(entryDateTime.toLocalDate());
                if (Constants.isPositive(undebitedInterestValue.getInterestAmount())) {
                    debitEntry.createInterestRateDebitEntry(undebitedInterestValue, entryDateTime, Optional.<DebitNote> empty());
                }
            }
        }

        // TODO ANIL : Handle asynchronous events 
        // BennuSignalsServices.emitSignalForSettlement(finantialDocument);
    }

    @Override
    protected void init(final FinantialDocument finantialDocument, final FinantialEntryType finantialEntryType,
            final BigDecimal amount, String description, DateTime entryDateTime) {
        throw new RuntimeException("error.SettlementEntry.use.init.without.finantialEntryType");
    }

    protected void init(final InvoiceEntry invoiceEntry, final FinantialDocument finantialDocument, final BigDecimal amount,
            String description, final DateTime entryDateTime) {
        super.init(finantialDocument, FinantialEntryType.SETTLEMENT_ENTRY, amount, description, entryDateTime);
        setInvoiceEntry(invoiceEntry);
        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();

        if (!(getFinantialDocument() instanceof SettlementNote)) {
            throw new TreasuryDomainException("error.SettlementEntry.finantialDocument.not.settlement.note.type");
        }

        if (getInvoiceEntry().isCreditNoteEntry() && !Constants.isEqual(getAmount(), getTotalAmount())) {
            throw new TreasuryDomainException("error.SettlementEntry.creditNoteEntry.total.amount.not.equal");
        }
        
        if (!Constants.isPositive(getAmount())) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.amount.less.than.zero");
        }
    }

    public static Stream<SettlementEntry> findAll() {
        return FinantialDocumentEntry.findAll().filter(f -> f instanceof SettlementEntry).map(SettlementEntry.class::cast);
    }

    @Atomic
    public static SettlementEntry create(final InvoiceEntry invoiceEntry, final SettlementNote finantialDocument,
            final BigDecimal amount, final String description, final DateTime entryDateTime,
            final boolean createInterestIfNeeded) {
        return new SettlementEntry(invoiceEntry, finantialDocument, amount, description, entryDateTime, createInterestIfNeeded);
    }

    @Atomic
    public static SettlementEntry create(final DebitEntryBean debitEntryBean, final SettlementNote settlementNote,
            DateTime entryDate) {
        return new SettlementEntry(debitEntryBean.getDebitEntry(), settlementNote, debitEntryBean.getDebtAmount(),
                debitEntryBean.getDebitEntry().getDescription(), entryDate, true);
    }

    @Atomic
    public static SettlementEntry create(final CreditEntry creditEntry, final BigDecimal creditAmount, final String creditDescription, 
            final SettlementNote settlementNote, final DateTime entryDate) {
        return new SettlementEntry(creditEntry, settlementNote, creditAmount, creditDescription, entryDate, false);
    }

    @Override
    public BigDecimal getTotalAmount() {
        return this.getAmount();
    }

    @Override
    public BigDecimal getNetAmount() {
        return this.getAmount();
    }

    public String getInvoiceEntryAmountSignal() {
        if (this.getInvoiceEntry().isDebitNoteEntry()) {
            return "";
        } else {
            return "-";
        }
    }

}
