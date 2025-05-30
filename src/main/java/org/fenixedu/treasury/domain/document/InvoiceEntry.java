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

import static org.fenixedu.treasury.util.TreasuryConstants.rationalVatRate;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.Sets;
import org.fenixedu.treasury.domain.*;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.exemption.TreasuryExemption;
import org.fenixedu.treasury.domain.paymentcodes.MultipleEntriesPaymentCode;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.MbwayPaymentRequest;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public abstract class InvoiceEntry extends InvoiceEntry_Base {

    public static final int UNIT_PRICE_SCALE = 4;

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

    public static final Comparator<InvoiceEntry> COMPARATOR_BY_ENTRY_ORDER_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION =
            (o1, o2) -> {
                if (o1.getEntryOrder() != null && o2.getEntryOrder() != null) {
                    int c = o1.getEntryOrder().compareTo(o2.getEntryOrder());

                    if (c != 0) {
                        return c;
                    }
                } else if (o1.getEntryOrder() != null && o2.getEntryOrder() == null) {
                    return -1;
                } else if (o1.getEntryOrder() == null && o2.getEntryOrder() != null) {
                    return 1;
                }

                return COMPARATOR_BY_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION.compare(o1, o2);
            };

    public static final int MAX_DESCRIPTION_LENGTH = 200;

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
    protected void init(FinantialEntity finantialEntity, DebtAccount debtAccount, FinantialDocument finantialDocument,
            FinantialEntryType finantialEntryType, BigDecimal amount, String description, DateTime entryDateTime) {
        throw new RuntimeException("error.InvoiceEntry.use.init.with.product");
    }

    protected void init(FinantialEntity finantialEntity, final FinantialDocument finantialDocument, final DebtAccount debtAccount,
            final Product product, final FinantialEntryType finantialEntryType, final Vat vat, final BigDecimal unitAmount,
            String description, BigDecimal quantity, DateTime entryDateTime) {
        super.init(finantialEntity, debtAccount, finantialDocument, finantialEntryType, unitAmount, description, entryDateTime);

        if (debtAccount.getClosed()) {
            throw new TreasuryDomainException("error.InvoiceEntry.debtAccount.closed");
        }

        this.setCalculatedAmountsOverriden(false);
        this.setQuantity(quantity);
        this.setNetExemptedAmount(BigDecimal.ZERO);
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

        if (TreasuryConstants.isNegative(getNetAmount())) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.netAmount.less.than.zero");
        }

        if (TreasuryConstants.isNegative(getNetExemptedAmount())) {
            throw new TreasuryDomainException("error.FinantialDocumentEntry.netExemptedAmount.less.than.zero");
        }

        if (checkAmountValues() == false) {
            throw new TreasuryDomainException("error.InvoiceEntry.amount.invalid.consistency");
        }
    }

    private boolean checkAmountValues() {
        if (getNetAmount() != null && getVatAmount() != null && getAmountWithVat() != null) {
            BigDecimal netAmount = calculateNetAmount();
            BigDecimal vatAmount = Currency.getValueWithScale(getNetAmount().multiply(rationalVatRate(this)));
            BigDecimal amountWithVat =
                    Currency.getValueWithScale(getNetAmount().multiply(BigDecimal.ONE.add(rationalVatRate(this))));

            //Compare the re-calculated values with the original ones
            return netAmount.compareTo(getNetAmount()) == 0 && vatAmount.compareTo(getVatAmount()) == 0
                    && amountWithVat.compareTo(getTotalAmount()) == 0;
        }

        return TreasuryConstants.isLessOrEqualThan(getNetExemptedAmount(),
                Currency.getValueWithScale(getQuantity().multiply(getAmount())));
    }

    protected void recalculateAmountValues() {
        if (this.getVatRate() == null) {
            this.setVatRate(super.getVat().getTaxRate());
        }

        // Net Amount with scale of 2
        BigDecimal netAmount = calculateNetAmount();
        setNetAmount(netAmount);

        // Check: The rounding mode used was HALF_EVEN which was giving incorrect result
        // For example 12.5 by 5% IVA is 0.63 but with RoundingMode.HALF_EVEN give 0.62
        //
        // Changed to HALF_UP, also in Currency::getValueWithScale method
        // Also read the comments in Currency::getValueWithScale method
        BigDecimal rationalVatRate = TreasuryConstants.rationalVatRate(this);

        // Vat Amount with scale of 2
        BigDecimal vatAmount = Currency.getValueWithScale(netAmount.multiply(rationalVatRate));

        // Amount with VAT with scale of 2, assuming summing two numbers with scale of 2, will result 
        // in a number with scale of 2
        BigDecimal amountWithVat = netAmount.add(vatAmount);

        setVatAmount(vatAmount);

        // Set scale of 2, just in case something goes wrong
        setAmountWithVat(Currency.getValueWithScale(amountWithVat));

        // Ensure the netAmount, vatAmount and amountWithVat have at most the decimal places for cents
        // defined in the currency
        //
        // TODO: First ensure in all instances that the following verifications are checked
        // then move to InvoiceEntry::checkRules
        if (getNetAmount().scale() > getDebtAccount().getFinantialInstitution().getCurrency().getDecimalPlacesForCents()) {
            throw new IllegalStateException("The netAmount has scale above the currency decimal places for cents");
        }

        if (getVatAmount().scale() > getDebtAccount().getFinantialInstitution().getCurrency().getDecimalPlacesForCents()) {
            throw new IllegalStateException("The vatAmount has scale above the currency decimal places for cents");
        }

        if (getAmountWithVat().scale() > getDebtAccount().getFinantialInstitution().getCurrency().getDecimalPlacesForCents()) {
            throw new IllegalStateException("The amountWithVat has scale above the currency decimal places for cents");
        }
    }

    /**
     * This method is overriden in DebitEntry, that take into account the netExemptedAmount
     * 
     * @return
     */
    private BigDecimal calculateNetAmount() {
        BigDecimal netAmount = Currency.getValueWithScale(getQuantity().multiply(getAmount()));
        netAmount = netAmount.subtract(getNetExemptedAmount());

        return netAmount;
    }

    public static Stream<? extends InvoiceEntry> findAll() {
        return FinantialDocumentEntry.findAll().filter(f -> f instanceof InvoiceEntry).map(InvoiceEntry.class::cast);
    }

    public Invoice getInvoice() {
        return (Invoice) getFinantialDocument();
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

    public void overrideCalculatedAmounts(BigDecimal netAmount, BigDecimal vatRate, BigDecimal vatAmount,
            BigDecimal amountWithVat) {
        setCalculatedAmountsOverriden(true);

        if (!TreasuryConstants.isPositive(netAmount)) {
            throw new TreasuryDomainException("error.DebitEntry.overrideCalculatedAmounts.invalid.netAmount");
        }

        if (TreasuryConstants.isLessThan(vatRate, BigDecimal.ZERO)
                || TreasuryConstants.isGreaterThan(vatRate, TreasuryConstants.HUNDRED_PERCENT)) {
            throw new TreasuryDomainException("error.DebitEntry.overrideCalculatedAmounts.invalid.vatRate");
        }

        if (TreasuryConstants.isNegative(vatAmount)) {
            throw new TreasuryDomainException("error.DebitEntry.overrideCalculatedAmounts.invalid.vatAmount");
        }

        if (!TreasuryConstants.isPositive(amountWithVat)) {
            throw new TreasuryDomainException("error.DebitEntry.overrideCalculatedAmounts.invalid.amountWithVat");
        }

        setNetAmount(netAmount);
        setVatRate(vatRate);
        setVatAmount(vatAmount);
        setAmountWithVat(amountWithVat);

        // Ensure the netAmount, vatAmount and amountWithVat have at most the decimal places for cents
        // defined in the currency
        //
        // TODO: First ensure in all instances that the following verifications are checked
        // then move to InvoiceEntry::checkRules
        if (getNetAmount().scale() > getDebtAccount().getFinantialInstitution().getCurrency().getDecimalPlacesForCents()) {
            throw new IllegalStateException("The netAmount has scale above the currency decimal places for cents");
        }

        if (getVatAmount().scale() > getDebtAccount().getFinantialInstitution().getCurrency().getDecimalPlacesForCents()) {
            throw new IllegalStateException("The vatAmount has scale above the currency decimal places for cents");
        }

        if (getAmountWithVat().scale() > getDebtAccount().getFinantialInstitution().getCurrency().getDecimalPlacesForCents()) {
            throw new IllegalStateException("The amountWithVat has scale above the currency decimal places for cents");
        }
    }

    public void disableOverrideCalculatedAmounts() {
        setCalculatedAmountsOverriden(false);
        recalculateAmountValues();
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

    @Override
    public void addToFinantialDocument(FinantialDocument finantialDocument) {
        super.addToFinantialDocument(finantialDocument);

        getPaymentInvoiceEntriesGroupsSet().stream().forEach(g -> g.checkRules());
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

    // Used in screens to display negative amounts for credit entries
    public BigDecimal getUiTotalAmount() {
        return getTotalAmount();
    }

    // Used in screens to display negative amounts for credit entries
    public BigDecimal getUiOpenAmount() {
        return getOpenAmount();
    }

    // Used in screens to display negative amounts for credit entries
    public BigDecimal getUiOpenAmountWithInterests() {
        return getOpenAmountWithInterests();
    }

    // used in screens to display negative amounts for credit entries
    public BigDecimal getUiNetExemptedAmount() {
        return getNetExemptedAmount();
    }

    public Set<SettlementEntry> getNotAnnuledSettlementEntries() {
        return getSettlementEntriesSet().stream().filter(se -> !se.getSettlementNote().isAnnulled()).collect(Collectors.toSet());
    }

    public Set<SettlementNote> getNotAnnuledSettlementNotes() {
        return getNotAnnuledSettlementEntries().stream().map(se -> se.getSettlementNote()).collect(Collectors.toSet());
    }

    // ANIL 2024-06-04
    //
    // It is not being possible in reporting tool, to descent at third level
    // of relations
    //
    // Declare to present the payment entry method and paid amount
    // 
    // Also for reimbursement entries

    public List<PaymentEntry> getPaymentEntries() {
        return getNotAnnuledSettlementEntries().stream().map(se -> se.getSettlementNote())
                .flatMap(s -> s.getPaymentEntriesSet().stream()).sorted(Comparator.comparing(PaymentEntry::getExternalId))
                .collect(Collectors.toList());
    }

    public List<ReimbursementEntry> getReimbursementEntries() {
        return getNotAnnuledSettlementEntries().stream().map(se -> se.getSettlementNote())
                .flatMap(s -> s.getReimbursementEntriesSet().stream())
                .sorted(Comparator.comparing(ReimbursementEntry::getExternalId)).collect(Collectors.toList());
    }

    public abstract TreasuryEvent getTreasuryEvent();

    public abstract Set<TreasuryExemption> getAssociatedTreasuryExemptions();

    /* This is used in reports */
    public DateTime getUiLastDateThatExemptOrChangeAmountsToZero() {
        DateTime result = null;

        if (TreasuryPlataformDependentServicesFactory.implementation().getCertifiedDocumentDate(getFinantialDocument()) != null) {
            result = TreasuryPlataformDependentServicesFactory.implementation().getCertifiedDocumentDate(getFinantialDocument())
                    .toDateTimeAtStartOfDay();
        }

        if (result == null && !getAssociatedTreasuryExemptions().isEmpty()) {
            // If exempted totally, fetch the last registered exemption and return the responsible
            TreasuryExemption t = getAssociatedTreasuryExemptions().stream()
                    .sorted(TreasuryExemption.COMPARE_BY_CREATION_DATE.reversed()).findFirst().get();

            result = TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(t);
        }

        if (result == null && isDebitNoteEntry()) {
            result = ((DebitEntry) this).getLastAmountsChangeDate() != null ? ((DebitEntry) this)
                    .getLastAmountsChangeDate() : null;
        }

        if (result == null) {
            result = TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(this);
        }

        return result;
    }

    public static Set<Customer> getReferencedCustomers(Set<InvoiceEntry> invoiceEntrySet) {
        final Set<Customer> result = Sets.newHashSet();

        for (final InvoiceEntry entry : invoiceEntrySet) {
            if (entry.getFinantialDocument() != null && ((Invoice) entry.getFinantialDocument()).isForPayorDebtAccount()) {
                result.add(((Invoice) entry.getFinantialDocument()).getPayorDebtAccount().getCustomer());
                continue;
            }

            result.add(entry.getDebtAccount().getCustomer());
        }

        return result;
    }

}
