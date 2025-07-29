package org.fenixedu.treasury.domain.payments;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.treasurydebtprocess.TreasuryDebtProcessMainService;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PaymentInvoiceEntriesGroup extends PaymentInvoiceEntriesGroup_Base {

    public PaymentInvoiceEntriesGroup() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
        setResponsible(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());
    }

    protected PaymentInvoiceEntriesGroup(FinantialEntity finantialEntity, DebtAccount debtAccount,
            Set<? extends InvoiceEntry> invoiceEntrySet, String groupKey) {
        this();

        setFinantialEntity(finantialEntity);
        setDebtAccount(debtAccount);

        invoiceEntrySet.forEach(e -> {
            if (TreasuryDebtProcessMainService.isBlockingPaymentInBackoffice(
                    e) || TreasuryDebtProcessMainService.isBlockingPaymentInFrontend(e)) {
                throw new TreasuryDomainException("error.PaymentInvoiceEntriesGroup.invoiceEntry.blocked.for.payment",
                        e.getDescription());
            }
        });

        getInvoiceEntriesSet().addAll(invoiceEntrySet);

        if (StringUtils.isEmpty(groupKey)) {
            groupKey = getCreationDate().toString("yyyyMMddHHmmss");
        }

        setGroupKey(groupKey.trim());

        checkRules();
    }

    public void checkRules() {

        if (getDomainRoot() == null) {
            throw new IllegalStateException("error.PaymentInvoiceEntriesGroup.domainRoot.required");
        }

        if (getDebtAccount() == null) {
            throw new IllegalStateException("error.PaymentInvoiceEntriesGroup.debtAccount.required");
        }

        if (getFinantialEntity() == null) {
            throw new IllegalStateException("error.PaymentInvoiceEntriesGroup.finantialEntity.required");
        }

        if (getInvoiceEntriesSet().isEmpty()) {
            throw new IllegalStateException("error.PaymentInvoiceEntriesGroup.invoiceEntries.required");
        }

        if (getInvoiceEntriesSet().stream().map(e -> e.getFinantialEntity()).distinct().count() != 1) {
            throw new TreasuryDomainException("error.PaymentInvoiceEntriesGroup.different.finantialEntities.from.invoiceEntries");
        }

        if (getInvoiceEntriesSet().stream().anyMatch(i -> i.getDebtAccount() != getDebtAccount())) {
            throw new TreasuryDomainException("error.PaymentInvoiceEntriesGroup.different.debtAccounts.from.invoiceEntries");
        }

        if (getReferencedCustomers().size() != 1) {
            throw new TreasuryDomainException("error.PaymentInvoiceEntriesGroup.referencedCustomers.only.one.allowed");
        }

        if (getDebtAccount().getPaymentInvoiceEntriesGroupsSet().stream().filter(g -> g.getGroupKey().equals(getGroupKey()))
                .count() > 1) {
            throw new TreasuryDomainException("error.PaymentInvoiceEntriesGroup.groupKey.already.exists");
        }
    }

    public void delete() {
        setDomainRoot(null);
        setDebtAccount(null);
        setFinantialEntity(null);

        getInvoiceEntriesSet().clear();

        super.deleteDomainObject();
    }

    public Set<Customer> getReferencedCustomers() {
        return InvoiceEntry.getReferencedCustomers(getInvoiceEntriesSet());
    }

    public void editRemarks(String remarks) {
        super.setRemarks(remarks);

        checkRules();
    }

    @Override
    public void addInvoiceEntries(InvoiceEntry invoiceEntry) {
        super.addInvoiceEntries(invoiceEntry);

        checkRules();
    }

    public void transferPaymentGroupToNewDebtAccount(DebtAccount destinyDebtAccount,
            Map<DebitEntry, DebitEntry> debitEntriesTransferMap) {
        // Collect the open debit entries from other debt account
        Set<DebitEntry> newDebitEntrySet = getInvoiceEntriesSet().stream() //
                .filter(d -> debitEntriesTransferMap.containsKey(d)) //
                .map(d -> debitEntriesTransferMap.get(d)) //
                .filter(d -> TreasuryConstants.isPositive(d.getOpenAmount())) //
                .collect(Collectors.toSet());

        if (newDebitEntrySet.isEmpty()) {
            return;
        }

        PaymentInvoiceEntriesGroup group = PaymentInvoiceEntriesGroup.findUniqueByGroupKey(destinyDebtAccount,
                StringUtils.isNotEmpty(getGroupKey()) ? getGroupKey() : UUID.randomUUID().toString()).orElseGet(
                () -> PaymentInvoiceEntriesGroup.create(getFinantialEntity(), destinyDebtAccount, newDebitEntrySet,
                        getGroupKey()));

        newDebitEntrySet.forEach(d -> group.addInvoiceEntries(d));
    }


    /* SERVICES */

    public static PaymentInvoiceEntriesGroup create(FinantialEntity finantialEntity, DebtAccount debtAccount,
            Set<? extends InvoiceEntry> invoiceEntrySet, String groupKey) {

        // Annul payment reference codes
        invoiceEntrySet.stream().filter(i -> i.isDebitNoteEntry()) //
                .map(i -> (DebitEntry) i) //
                .flatMap(i -> i.getActiveSibsPaymentRequests().stream()) //
                .forEach(i -> i.anull());

        return new PaymentInvoiceEntriesGroup(finantialEntity, debtAccount, invoiceEntrySet, groupKey);
    }

    public static Stream<PaymentInvoiceEntriesGroup> findAll() {
        return FenixFramework.getDomainRoot().getPaymentInvoiceEntriesGroupsSet().stream();
    }

    public static Stream<PaymentInvoiceEntriesGroup> find(DebtAccount debtAccount) {
        return debtAccount.getPaymentInvoiceEntriesGroupsSet().stream();
    }

    public static Stream<PaymentInvoiceEntriesGroup> find(InvoiceEntry invoiceEntry) {
        return invoiceEntry.getPaymentInvoiceEntriesGroupsSet().stream();
    }

    public static Stream<PaymentInvoiceEntriesGroup> findByGroupKey(DebtAccount debtAccount, String groupKey) {
        return find(debtAccount).filter(d -> groupKey.equals(d.getGroupKey()));
    }

    public static Stream<PaymentInvoiceEntriesGroup> findContainingAllInvoiceEntries(Set<? extends InvoiceEntry> invoiceEntriesSet) {
        if (invoiceEntriesSet == null || invoiceEntriesSet.isEmpty()) {
            return Stream.empty();
        }

        DebtAccount debtAccount = invoiceEntriesSet.iterator().next().getDebtAccount();

        return find(debtAccount).filter(i -> i.getInvoiceEntriesSet().contains(invoiceEntriesSet));
    }

    public static Optional<PaymentInvoiceEntriesGroup> findUniqueByGroupKey(DebtAccount debtAccount, String groupKey) {
        return findByGroupKey(debtAccount, groupKey).findFirst();
    }

    public static Set<InvoiceEntry> getAllPaymentRelatableInvoiceEntries(InvoiceEntry invoiceEntry) {
        return invoiceEntry.getPaymentInvoiceEntriesGroupsSet().stream().flatMap(g -> g.getInvoiceEntriesSet().stream())
                .collect(Collectors.toSet());
    }

}
