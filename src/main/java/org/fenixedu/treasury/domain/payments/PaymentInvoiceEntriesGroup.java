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
import org.joda.time.DateTime;
import pt.ist.fenixframework.FenixFramework;

import java.util.Set;
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

        if (getInvoiceEntriesSet().size() < 2) {
            throw new IllegalStateException("error.PaymentInvoiceEntriesGroup.invoiceEntries.at.least.two.required");
        }

        if (getInvoiceEntriesSet().stream().map(e -> e.getFinantialEntity()).distinct().count() != 1) {
            throw new TreasuryDomainException("error.PaymentInvoiceEntriesGroup.different.finantialEntities.from.invoiceEntries");
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


    /* SERVICES */

    public static PaymentInvoiceEntriesGroup create(FinantialEntity finantialEntity, DebtAccount debtAccount,
            Set<? extends InvoiceEntry> invoiceEntrySet, String groupKey) {
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

    public static Set<InvoiceEntry> getAllPaymentRelatableInvoiceEntries(InvoiceEntry invoiceEntry) {
        return invoiceEntry.getPaymentInvoiceEntriesGroupsSet().stream().flatMap(g -> g.getInvoiceEntriesSet().stream())
                .collect(Collectors.toSet());
    }

}
