package org.fenixedu.treasury.domain.payments;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import pt.ist.fenixframework.FenixFramework;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PaymentInvoiceEntriesGroup extends PaymentInvoiceEntriesGroup_Base {

    public PaymentInvoiceEntriesGroup() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected PaymentInvoiceEntriesGroup(DebtAccount debtAccount, Set<InvoiceEntry> invoiceEntrySet) {
        this();

        setDebtAccount(debtAccount);

        getInvoiceEntriesSet().addAll(invoiceEntrySet);
    }

    public void checkRules() {

    }

    public void edit(Set<InvoiceEntry> invoiceEntrySet) {
        getInvoiceEntriesSet().clear();
        getInvoiceEntriesSet().addAll(invoiceEntrySet);
    }

    public static PaymentInvoiceEntriesGroup create(DebtAccount debtAccount, Set<InvoiceEntry> invoiceEntrySet) {
        return new PaymentInvoiceEntriesGroup(debtAccount, invoiceEntrySet);
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

    public static Set<InvoiceEntry> getAllPaymentRelatableInvoiceEntries(InvoiceEntry invoiceEntry) {
        return invoiceEntry.getPaymentInvoiceEntriesGroupsSet().stream().flatMap(g -> g.getInvoiceEntriesSet().stream())
                .collect(Collectors.toSet());
    }

}
