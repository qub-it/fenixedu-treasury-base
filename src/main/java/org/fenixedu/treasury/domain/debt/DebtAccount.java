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
        return getPaymentPlansSet().stream().filter(plan -> plan.isOpen()).collect(Collectors.toSet());
    }

    public Collection<? extends PaymentCodeTarget> getUsedPaymentCodeTargetOfPendingInstallments() {
        final Set<PaymentCodeTarget> result = Sets.newHashSet();
        for (final PaymentPlan paymentPlan : getActivePaymentPlansSet()) {
            result.addAll(paymentPlan.getInstallmentsSet().stream().flatMap(inst -> inst.getPaymentCodesSet().stream())
                    .filter(pay -> pay.getPaymentReferenceCode().isUsed()).collect(Collectors.toSet()));
        }
        return result;

    }
}
