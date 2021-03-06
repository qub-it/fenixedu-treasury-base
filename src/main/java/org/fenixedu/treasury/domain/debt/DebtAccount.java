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
package org.fenixedu.treasury.domain.debt;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.balancetransfer.BalanceTransferService;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlan;
import org.fenixedu.treasury.domain.paymentcodes.FinantialDocumentPaymentCode;
import org.fenixedu.treasury.domain.paymentcodes.MultipleEntriesPaymentCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentCodeTarget;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class DebtAccount extends DebtAccount_Base {

    public static final Comparator<DebtAccount> COMPARATOR_BY_CUSTOMER_NAME_IGNORE_CASE = (o1, o2) -> {
        int c = Customer.COMPARE_BY_NAME_IGNORE_CASE.compare(o1.getCustomer(), o2.getCustomer());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final Comparator<DebtAccount> COMPARATOR_BY_FINANTIAL_INSTITUTION_NAME = (o1, o2) -> {
        int c = FinantialInstitution.COMPARATOR_BY_NAME.compare(o1.getFinantialInstitution(), o2.getFinantialInstitution());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public DebtAccount() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected DebtAccount(final FinantialInstitution finantialInstitution, final Customer customer) {
        this();
        setCustomer(customer);
        setFinantialInstitution(finantialInstitution);

        checkRules();
    }

    private void checkRules() {
        if (getCustomer() == null) {
            throw new TreasuryDomainException("error.DebtAccount.customer.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.DebtAccount.finantialInstitution.required");
        }
    }

    public BigDecimal getTotalInDebt() {
        BigDecimal amount = BigDecimal.ZERO;
        for (InvoiceEntry entry : this.getPendingInvoiceEntriesSet()) {
            if (entry.isDebitNoteEntry()) {
                amount = amount.add(entry.getOpenAmount());
            } else if (entry.isCreditNoteEntry()) {
                amount = amount.subtract(entry.getOpenAmount());
            }
        }

        return getFinantialInstitution().getCurrency().getValueWithScale(amount);
    }

    public BigDecimal getTotalInDebtForAllDebtAccountsOfSameFinantialInstitution() {
        BigDecimal result = BigDecimal.ZERO;
        for (final Customer customer : getCustomer().getAllCustomers()) {
            if (DebtAccount.findUnique(getFinantialInstitution(), customer).isPresent()) {
                result = result.add(DebtAccount.findUnique(getFinantialInstitution(), customer).get().getTotalInDebt());
            }
        }

        return result;
    }

    public Set<PaymentCodeTarget> getUsedPaymentCodeTargetOfPendingInvoiceEntries() {
        final Set<PaymentCodeTarget> result = Sets.newHashSet();
        for (final InvoiceEntry invoiceEntry : getPendingInvoiceEntriesSet()) {
            if (!invoiceEntry.isDebitNoteEntry()) {
                continue;
            }

            result.addAll(MultipleEntriesPaymentCode.findUsedByDebitEntry((DebitEntry) invoiceEntry).collect(Collectors.toSet()));

            if (invoiceEntry.getFinantialDocument() != null) {
                result.addAll(FinantialDocumentPaymentCode.findUsedByFinantialDocument(invoiceEntry.getFinantialDocument())
                        .collect(Collectors.<PaymentCodeTarget> toSet()));
            }
        }

        return result;
    }

    public boolean isClosed() {
        return getClosed();
    }

    @Atomic
    public void transferBalanceForActiveDebtAccount() {
        if (getCustomer().isActive()) {
            throw new TreasuryDomainException("error.DebtAccount.transfer.from.must.not.be.active");
        }

        Optional<DebtAccount> activeDebtAccount =
                DebtAccount.findUnique(getFinantialInstitution(), getCustomer().getActiveCustomer());

        if (!activeDebtAccount.isPresent()) {
            throw new TreasuryDomainException("error.DebtAccount.active.debt.account.not.found");
        }

        transferBalance(activeDebtAccount.get());
    }

    @Atomic
    public void transferBalance(final DebtAccount destinyDebtAccount) {
        new BalanceTransferService(this, destinyDebtAccount).transferBalance();
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<DebtAccount> findAll() {
        return FenixFramework.getDomainRoot().getDebtAccountsSet().stream();
    }

    public static Stream<DebtAccount> find(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getDebtAccountsSet().stream();
    }

    public static Stream<DebtAccount> findActiveAdhoc(final FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(x -> x.getCustomer().isAdhocCustomer()).filter(x -> x.getCustomer().isActive());
    }

    public static Stream<DebtAccount> find(final Customer customer) {
        return customer.getDebtAccountsSet().stream();
    }

    public static Optional<DebtAccount> findUnique(final FinantialInstitution finantialInstitution, final Customer customer) {
        return Optional.ofNullable(customer.getDebtAccountFor(finantialInstitution));
    }

    public static SortedSet<DebtAccount> findActiveAdhocDebtAccountsSortedByCustomerName(
            final FinantialInstitution finantialInstitution) {
        final SortedSet<DebtAccount> result = Sets.newTreeSet(COMPARATOR_BY_CUSTOMER_NAME_IGNORE_CASE);
        result.addAll(DebtAccount.findActiveAdhoc(finantialInstitution).collect(Collectors.toSet()));

        return result;
    }

    @Atomic
    public static DebtAccount create(final FinantialInstitution finantialInstitution, final Customer customer) {
        //Find if already exists
        DebtAccount existing = DebtAccount.findUnique(finantialInstitution, customer).orElse(null);
        if (existing != null) {
            existing.setClosed(false);
            return existing;
        }
        return new DebtAccount(finantialInstitution, customer);
    }

    public Stream<? extends InvoiceEntry> pendingInvoiceEntries() {
        return this.getInvoiceEntrySet().stream().filter(x -> x.isPendingForPayment());
    }

    public Set<? extends InvoiceEntry> getPendingInvoiceEntriesSet() {
        return pendingInvoiceEntries().collect(Collectors.<InvoiceEntry> toSet());
    }

    @Atomic
    public void closeDebtAccount() {
        this.setClosed(true);
    }

    @Atomic
    public void reopenDebtAccount() {
        this.setClosed(false);
    }

    private Set<SettlementNote> getSettlementNoteSet() {
        return this.getFinantialDocumentsSet().stream().filter(x -> x.isSettlementNote()).map(SettlementNote.class::cast)
                .collect(Collectors.toSet());
    }

    public String obtainUITotalInDebt() {
        return this.getFinantialInstitution().getCurrency().getValueFor(this.getTotalInDebt());
    }

    public boolean isDeletable() {
        return this.getFinantialDocumentsSet().isEmpty() && getInvoiceEntrySet().isEmpty() && getInvoiceSet().isEmpty()
                && getPayorDebitEntriesSet().isEmpty() && getForwardPaymentsSet().isEmpty()
                && getPaymentCodeTargetsSet().isEmpty() && getTreasuryEventsSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.DebtAccount.cannot.delete");
        }

        setDomainRoot(null);
        setCustomer(null);
        setFinantialInstitution(null);

        deleteDomainObject();
    }

    public BigDecimal calculatePendingInterestAmount() {
        return calculatePendingInterestAmount(new DateTime().toLocalDate());
    }

    public BigDecimal calculateTotalPendingInterestAmountForAllDebtAccountsOfSameFinantialInstitution() {
        BigDecimal result = BigDecimal.ZERO;

        for (Customer customer : getCustomer().getAllCustomers()) {
            if (DebtAccount.findUnique(getFinantialInstitution(), customer).isPresent()) {
                result = result
                        .add(DebtAccount.findUnique(getFinantialInstitution(), customer).get().calculatePendingInterestAmount());
            }
        }

        return result;
    }

    private BigDecimal calculatePendingInterestAmount(LocalDate whenToCalculate) {
        BigDecimal interestAmount = BigDecimal.ZERO;
        for (InvoiceEntry entry : this.getPendingInvoiceEntriesSet()) {
            if (entry.isDebitNoteEntry()) {
                interestAmount = interestAmount
                        .add(((DebitEntry) entry).calculateUndebitedInterestValue(whenToCalculate).getInterestAmount());
            }
        }
        return interestAmount;
    }

    public Stream<InvoiceEntry> getActiveInvoiceEntries() {
        return this.getInvoiceEntrySet().stream().filter(x -> x.getFinantialDocument() == null
                || x.getFinantialDocument() != null && x.getFinantialDocument().isAnnulled() == false);
    }

    public boolean hasPreparingDocuments() {
        return getFinantialDocumentsSet().stream().anyMatch(ie -> ie.isPreparing());
    }

    public boolean hasPreparingDebitNotes() {
        return getPendingInvoiceEntriesSet().stream().anyMatch(
                ie -> ie.isDebitNoteEntry() && ie.getFinantialDocument() != null && ie.getFinantialDocument().isPreparing());
    }

    public boolean hasPreparingCreditNotes() {
        return getPendingInvoiceEntriesSet().stream().anyMatch(
                ie -> ie.isCreditNoteEntry() && ie.getFinantialDocument() != null && ie.getFinantialDocument().isPreparing());
    }

    public boolean hasPreparingSettlementNotes() {
        return getPendingInvoiceEntriesSet().stream().anyMatch(ie -> ie.getSettlementEntriesSet().stream()
                .anyMatch(se -> se.getFinantialDocument() != null && se.getFinantialDocument().isPreparing()));
    }

    public Set<PaymentPlan> getActivePaymentPlansSet() {
        return getPaymentPlansSet().stream().filter(plan -> plan.getState().isOpen()).collect(Collectors.toSet());

    }

    public Collection<? extends PaymentCodeTarget> getUsedPaymentCodeTargetOfPendingInstallments() {
        final Set<PaymentCodeTarget> result = Sets.newHashSet();
        for (final PaymentPlan paymentPlan : getActivePaymentPlansSet()) {
            result.addAll(paymentPlan.getInstallmentsSet().stream().flatMap(inst -> inst.getPaymentCodesSet().stream())
                    .filter(pay -> pay.getPaymentReferenceCode().isUsed()).collect(Collectors.toSet()));
        }
        return result;

    }

    public Set<PaymentPlan> getPaymentPlansNotCompliantSet(LocalDate when) {
        return getActivePaymentPlansSet().stream().filter(plan -> !plan.isCompliant(when)).collect(Collectors.toSet());
    }

    public Set<PaymentPlan> getPaymentPlansNotCompliantSet() {
        return getPaymentPlansNotCompliantSet(LocalDate.now());
    }

}
