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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;

public abstract class Invoice extends Invoice_Base {

    protected Invoice() {
        super();
    }

    @Override
    protected void checkRules() {
        if (getDebtAccount() == getPayorDebtAccount()) {
            throw new TreasuryDomainException("error.Invoice.payor.same.as.debt.account");
        }

        if (getPayorDebtAccount() != null && !getPayorDebtAccount().getCustomer().isAdhocCustomer()) {
            throw new TreasuryDomainException("error.Invoice.payor.debt.account.not.adhoc.customer");
        }

        //check if all invoiceEntries are for the same debtaccount of invoice
        for (FinantialDocumentEntry entry : getFinantialDocumentEntriesSet()) {
            InvoiceEntry invoiceEntry = (InvoiceEntry) entry;
            if (!invoiceEntry.getDebtAccount().equals(this.getDebtAccount())) {
                throw new TreasuryDomainException("error.Invoice.debtaccount.mismatch.invoiceentries.debtaccount");
            }
        }
        super.checkRules();
    }

    @Override
    protected void init(final DebtAccount debtAccount, final DocumentNumberSeries documentNumberSeries,
            final DateTime documentDate) {
        if (debtAccount.getClosed()) {
            throw new TreasuryDomainException("error.Invoice.debtAccount.closed");
        }

        super.init(debtAccount, documentNumberSeries, documentDate);
    }

    protected void init(final DebtAccount debtAccount, final DebtAccount payorDebtAccount,
            final DocumentNumberSeries documentNumberSeries, final DateTime documentDate) {
        this.init(debtAccount, documentNumberSeries, documentDate);

        if (debtAccount == payorDebtAccount) {
            throw new TreasuryDomainException("error.Invoice.payor.same.as.debt.account");
        }

        setPayorDebtAccount(payorDebtAccount);
    }

    @Override
    public boolean isInvoice() {
        return true;
    }

    @Override
    public boolean isDeletable() {
        if (getState() != FinantialDocumentStateType.PREPARING) {
            return false;
        }
        return super.isDeletable();
    }

    @Override
    @Atomic
    public void delete(boolean deleteEntries) {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.Invoice.cannot.delete");
        }

        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        setPayorDebtAccount(null);
        super.delete(deleteEntries);
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<? extends Invoice> findAll() {
        return FinantialDocument.findAll().filter(x -> x instanceof Invoice).map(Invoice.class::cast);
    }

    public static Stream<? extends Invoice> find(final DebtAccount debtAccount) {
        return debtAccount.getFinantialDocumentsSet().stream().filter(x -> x instanceof Invoice).map(Invoice.class::cast);
    }

    public BigDecimal getTotalVatAmount() {
        BigDecimal vat = BigDecimal.ZERO;
        for (FinantialDocumentEntry entry : getFinantialDocumentEntriesSet()) {
            vat = vat.add(((InvoiceEntry) entry).getVatAmount());
        }
        return vat;
    }

    public Set<SettlementEntry> getRelatedSettlementEntries() {
        Set<SettlementEntry> result = new HashSet<SettlementEntry>();
        for (FinantialDocumentEntry entry : this.getFinantialDocumentEntriesSet()) {
            InvoiceEntry invoiceEntry = (InvoiceEntry) entry;
            if (invoiceEntry.getSettlementEntriesSet().size() > 0) {
                for (SettlementEntry settlementEntry : invoiceEntry.getSettlementEntriesSet()) {
                    if (settlementEntry.getFinantialDocument().isClosed()) {
                        result.add(settlementEntry);
                    }
                }
            }
        }
        return result;
    }

    public boolean hasValidSettlementEntries() {
        return this.getRelatedSettlementEntries().stream()
                .anyMatch(y -> y.getFinantialDocument().isClosed() || y.getFinantialDocument().isPreparing());
    }

    public InvoiceEntry getEntryInOrder(Integer lineNumber) {
        FinantialDocumentEntry entry = this.getFinantialDocumentEntriesSet().stream()
                .filter(x -> x.getEntryOrder().equals(lineNumber)).findFirst().orElse(null);
        if (entry != null) {
            return (InvoiceEntry) entry;
        }
        return null;
    }

    public boolean isTotalSettledWithoutPaymentEntries() {
        if (isAnnulled() || TreasuryConstants.isPositive(getOpenAmount())) {
            return false;
        }

        return !getRelatedSettlementEntries().stream()
                .map(e -> !((SettlementNote) e.getFinantialDocument()).getPaymentEntriesSet().isEmpty()).reduce((a, c) -> a || c)
                .orElse(false);
    }

    public boolean isForPayorDebtAccount() {
        return getPayorDebtAccount() != null && getPayorDebtAccount() != getDebtAccount();
    }

    @Override
    public Comparator<? extends FinantialDocumentEntry> getFinantialDocumentEntriesOrderComparator() {
        return InvoiceEntry.COMPARATOR_BY_ENTRY_ORDER_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION;
    }

    @Override
    public List<? extends FinantialDocumentEntry> getFinantialDocumentEntriesOrderedByTuitionInstallmentOrderAndDescription() {
        final List<InvoiceEntry> result = new ArrayList<>();

        getFinantialDocumentEntriesSet().stream().map(InvoiceEntry.class::cast).collect(Collectors.toCollection(() -> result));
        Collections.sort(result, InvoiceEntry.COMPARATOR_BY_ENTRY_ORDER_TUITION_INSTALLMENT_ORDER_AND_DESCRIPTION);

        if (result.size() != getFinantialDocumentEntriesSet().size()) {
            throw new RuntimeException("error");
        }

        return result;
    }

    public boolean isWithInvoiceEntriesWithCalculatedAmountsOverriden() {
        return getFinantialDocumentEntriesSet().stream().map(InvoiceEntry.class::cast)
                .anyMatch(ei -> Boolean.TRUE.equals(ei.getCalculatedAmountsOverriden()));
    }
}
