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
import static org.fenixedu.treasury.util.TreasuryConstants.isPositive;
import static org.fenixedu.treasury.util.TreasuryConstants.rationalVatRate;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;

public abstract class InvoiceEntry extends InvoiceEntry_Base {

    public static final Comparator<InvoiceEntry> COMPARE_BY_DUE_DATE = new Comparator<InvoiceEntry>() {

        @Override
        public int compare(final InvoiceEntry o1, final InvoiceEntry o2) {
            int c = o1.getDueDate().compareTo(o2.getDueDate());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    public static final Comparator<InvoiceEntry> COMPARE_BY_ENTRY_DATE = new Comparator<InvoiceEntry>() {

        @Override
        public int compare(final InvoiceEntry o1, final InvoiceEntry o2) {
            int c = o1.getEntryDateTime().compareTo(o2.getEntryDateTime());

            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    public static final Comparator<InvoiceEntry> COMPARE_BY_AMOUNT_AND_DUE_DATE = new Comparator<InvoiceEntry>() {

        @Override
        public int compare(final InvoiceEntry o1, final InvoiceEntry o2) {
            int c = -o1.getOpenAmount().compareTo(o2.getOpenAmount());

            if (c != 0) {
                return c;
            }

            c = o1.getDueDate().compareTo(o2.getDueDate());

            if (c != 0) {
                return c;
            }

            return o1.getExternalId().compareTo(o2.getExternalId());
        }
    };
    
    public static final Comparator<InvoiceEntry> COMPARATOR_BY_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION = new Comparator<InvoiceEntry>() {

        @Override
        public int compare(InvoiceEntry o1, InvoiceEntry o2) {
            if(o1.getProduct().getTuitionInstallmentOrder() != 0 && o2.getProduct().getTuitionInstallmentOrder() != 0) {
                int c = Integer.compare(o1.getProduct().getTuitionInstallmentOrder(), o2.getProduct().getTuitionInstallmentOrder());
                
                return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
                
            } else if(o1.getProduct().getTuitionInstallmentOrder() != 0 && o2.getProduct().getTuitionInstallmentOrder() == 0) {
                return -1;
            } else if(o1.getProduct().getTuitionInstallmentOrder() == 0 && o2.getProduct().getTuitionInstallmentOrder() != 0) {
                return 1;
            }
            
            final int c = o1.getDescription().compareTo(o2.getDescription());
            
            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
        
    };

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        if (getFinantialDocument() != null && !getFinantialDocument().isPreparing()) {
            blockers.add(
                    treasuryBundle("error.invoiceentry.cannot.be.deleted.document.is.not.preparing"));
        }

        if (!getSettlementEntriesSet().isEmpty()) {
            blockers.add(treasuryBundle(
                    "error.invoiceentry.cannot.be.deleted.settlemententries.is.not.empty"));
        }

    }

    public boolean isDebitNoteEntry() {
        return false;
    }

    public boolean isCreditNoteEntry() {
        return false;
    }

    public boolean isProcessedInDebitNote() {
        return getFinantialDocument() != null;
    }

    public boolean isProcessedInClosedDebitNote() {
        return isProcessedInDebitNote() && getFinantialDocument().isClosed();
    }

    @Override
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        this.setCurrency(null);
        this.setDebtAccount(null);
        this.setVat(null);
        this.setProduct(null);
        super.delete();
    }

    @Override
    protected void init(FinantialDocument finantialDocument, FinantialEntryType finantialEntryType, BigDecimal amount,
            String description, DateTime entryDateTime) {
        throw new RuntimeException("error.InvoiceEntry.use.init.with.product");
    }

    protected void init(final FinantialDocument finantialDocument, final DebtAccount debtAccount, final Product product,
            final FinantialEntryType finantialEntryType, final Vat vat, final BigDecimal amount, String description,
            BigDecimal quantity, DateTime entryDateTime) {
        super.init(finantialDocument, finantialEntryType, amount, description, entryDateTime);

        if (debtAccount.getClosed()) {
            throw new TreasuryDomainException("error.InvoiceEntry.debtAccount.closed");
        }

        this.setQuantity(quantity);
        this.setCurrency(debtAccount.getFinantialInstitution().getCurrency());
        this.setDebtAccount(debtAccount);
        this.setProduct(product);
        this.setVat(vat);
    }

    @Override
    protected void checkRules() {
        super.checkRules();

        if (getQuantity() == null) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.quantity.required");
        }

        if (getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.quantity.less.than.zero");
        }

        if (getFinantialDocument() != null && !(getFinantialDocument() instanceof Invoice)) {
            throw new TreasuryDomainException("error.InvoiceEntry.finantialDocument.not.invoice.type");
        }

        if (getProduct() == null) {
            throw new TreasuryDomainException("error.InvoiceEntry.product.required");
        }

        if (getDebtAccount() == null) {
            throw new TreasuryDomainException("error.InvoiceEntry.debtAccount.required");
        }

        if (getCurrency() == null) {
            throw new TreasuryDomainException("error.InvoiceEntry.currency.required");
        }

        if (getVat() == null) {
            throw new TreasuryDomainException("error.InvoiceEntry.vat.required");
        }

        if (getFinantialDocument() != null && getFinantialDocument().getDebtAccount() != this.getDebtAccount()) {
            throw new TreasuryDomainException("error.InvoiceEntry.invalidDebtAccount");
        }

        if (checkAmountValues() == false) {
            throw new TreasuryDomainException("error.InvoiceEntry.amount.invalid.consistency");
        }

    }

    protected boolean checkAmountValues() {
        if (getNetAmount() != null && getVatAmount() != null && getAmountWithVat() != null) {
            BigDecimal netAmount = getCurrency().getValueWithScale(getQuantity().multiply(getAmount()));
            BigDecimal vatAmount = getCurrency().getValueWithScale(getNetAmount().multiply(rationalVatRate(this)));
            BigDecimal amountWithVat =
                    getCurrency().getValueWithScale(getNetAmount().multiply(BigDecimal.ONE.add(rationalVatRate(this))));

            //Compare the re-calculated values with the original ones
            return netAmount.compareTo(getNetAmount()) == 0 && vatAmount.compareTo(getVatAmount()) == 0
                    && amountWithVat.compareTo(getTotalAmount()) == 0;
        }
        return true;
    }

    @Atomic
    protected void recalculateAmountValues() {
        if (this.getVatRate() == null) {
            this.setVatRate(super.getVat().getTaxRate());
        }
        setNetAmount(getCurrency().getValueWithScale(getQuantity().multiply(getAmount())));

        // TODO
        // Check: The rounding mode used was HALF_EVEN which was giving incorrect result
        // For example 12.5 by 5% IVA is 0.63 but with RoundingMode.HALF_EVEN give 0.62
        //
        BigDecimal rationalVatRate = rationalVatRate(this);
        BigDecimal netAmount = getNetAmount();
        BigDecimal vatAmount = netAmount.multiply(rationalVatRate);
        BigDecimal amountWithVat = netAmount.multiply(BigDecimal.ONE.add(rationalVatRate));
        
        setVatAmount(vatAmount.setScale(2, RoundingMode.HALF_UP));
        setAmountWithVat(amountWithVat.setScale(2, RoundingMode.HALF_UP));
    }

    public static Stream<? extends InvoiceEntry> findAll() {
        return FinantialDocumentEntry.findAll().filter(f -> f instanceof InvoiceEntry).map(InvoiceEntry.class::cast);
    }

    public boolean isPendingForPayment() {
        if (this.getFinantialDocument() != null && this.getFinantialDocument().getState().isAnnuled()) {
            return false;
        }
        return this.getOpenAmount().compareTo(BigDecimal.ZERO) != 0;
    }

    public boolean hasPreparingSettlementEntries() {
        return getSettlementEntriesSet().stream().anyMatch(se -> se.getFinantialDocument().isPreparing());
    }

    @Override
    public BigDecimal getTotalAmount() {
        return this.getAmountWithVat();
    }

    public BigDecimal getPayedAmount() {
        BigDecimal amount = BigDecimal.ZERO;
        for (SettlementEntry entry : this.getSettlementEntriesSet()) {
            if (entry.getFinantialDocument() != null && entry.getFinantialDocument().isClosed()) {
                amount = amount.add(entry.getTotalAmount());
            }
        }
        return amount;
    }

    public BigDecimal getOpenAmount() {
        if (isAnnulled()) {
            return BigDecimal.ZERO;
        }

        final BigDecimal openAmount = this.getAmountWithVat().subtract(getPayedAmount());

        return getCurrency().getValueWithScale(isPositive(openAmount) ? openAmount : BigDecimal.ZERO);
    }

    public abstract BigDecimal getOpenAmountWithInterests();

    public abstract LocalDate getDueDate();

    // @formatter:off
    /* *********************
     * ERP INTEGRATION UTILS
     * *********************
     */
    // @formatter:on

    public BigDecimal openAmountAtDate(final DateTime when) {
        final Currency currency = getDebtAccount().getFinantialInstitution().getCurrency();

        if (isAnnulled()) {
            return BigDecimal.ZERO;
        }

        final BigDecimal openAmount = getAmountWithVat().subtract(payedAmountAtDate(when));

        return currency.getValueWithScale(isPositive(openAmount) ? openAmount : BigDecimal.ZERO);
    }

    public BigDecimal payedAmountAtDate(final DateTime when) {
        BigDecimal amount = BigDecimal.ZERO;
        for (final SettlementEntry entry : getSettlementEntriesSet()) {
            if (entry.getEntryDateTime().isAfter(when)) {
                continue;
            }

            if (entry.getFinantialDocument() != null && entry.getFinantialDocument().isClosed()) {
                amount = amount.add(entry.getTotalAmount());
            }
        }

        return amount;
    }

}
