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
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.MultipleEntriesPaymentCode;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.MbwayPaymentRequest;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;

public abstract class InvoiceEntry extends InvoiceEntry_Base {

    public static final Comparator<InvoiceEntry> COMPARE_BY_DUE_DATE = (o1, o2) -> {
        int c = o1.getDueDate().compareTo(o2.getDueDate());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<InvoiceEntry> COMPARE_BY_ENTRY_DATE = (o1, o2) -> {
        int c = o1.getEntryDateTime().compareTo(o2.getEntryDateTime());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<InvoiceEntry> COMPARE_BY_AMOUNT_AND_DUE_DATE = (o1, o2) -> {
        int c = -o1.getOpenAmount().compareTo(o2.getOpenAmount());

        if (c != 0) {
            return c;
        }

        c = o1.getDueDate().compareTo(o2.getDueDate());

        if (c != 0) {
            return c;
        }

        return o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<InvoiceEntry> COMPARATOR_BY_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION = (o1, o2) -> {
        if (o1.getProduct().getTuitionInstallmentOrder() != 0 && o2.getProduct().getTuitionInstallmentOrder() != 0) {
            int c1 = Integer.compare(o1.getProduct().getTuitionInstallmentOrder(), o2.getProduct().getTuitionInstallmentOrder());

            return c1 != 0 ? c1 : o1.getExternalId().compareTo(o2.getExternalId());

        } else if (o1.getProduct().getTuitionInstallmentOrder() != 0 && o2.getProduct().getTuitionInstallmentOrder() == 0) {
            return -1;
        } else if (o1.getProduct().getTuitionInstallmentOrder() == 0 && o2.getProduct().getTuitionInstallmentOrder() != 0) {
            return 1;
        }

        final int c2 = o1.getDescription().compareTo(o2.getDescription());

        return c2 != 0 ? c2 : o1.getExternalId().compareTo(o2.getExternalId());
    };

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);

        if (getFinantialDocument() != null && !getFinantialDocument().isPreparing()) {
            blockers.add(treasuryBundle("error.invoiceentry.cannot.be.deleted.document.is.not.preparing"));
        }

        if (!getSettlementEntriesSet().isEmpty()) {
            blockers.add(treasuryBundle("error.invoiceentry.cannot.be.deleted.settlemententries.is.not.empty"));
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

    @Deprecated
    @Override
    public void addPaymentCodes(MultipleEntriesPaymentCode paymentCodes) {
        super.addPaymentCodes(paymentCodes);
    }

    @Deprecated
    @Override
    public void removePaymentCodes(MultipleEntriesPaymentCode paymentCodes) {
        // TODO Auto-generated method stub
        super.removePaymentCodes(paymentCodes);
    }

    @Deprecated
    @Override
    public Set<MultipleEntriesPaymentCode> getPaymentCodesSet() {
        // TODO Auto-generated method stub
        return super.getPaymentCodesSet();
    }

    @Deprecated
    @Override
    public void addMbwayPaymentRequests(MbwayPaymentRequest mbwayPaymentRequests) {
        // TODO Auto-generated method stub
        super.addMbwayPaymentRequests(mbwayPaymentRequests);
    }

    @Deprecated
    @Override
    public void removeMbwayPaymentRequests(MbwayPaymentRequest mbwayPaymentRequests) {
        // TODO Auto-generated method stub
        super.removeMbwayPaymentRequests(mbwayPaymentRequests);
    }

    @Deprecated
    @Override
    public Set<MbwayPaymentRequest> getMbwayPaymentRequestsSet() {
        // TODO Auto-generated method stub
        return super.getMbwayPaymentRequestsSet();
    }

}
