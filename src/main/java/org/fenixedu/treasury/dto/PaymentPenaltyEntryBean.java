package org.fenixedu.treasury.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.joda.time.LocalDate;

public class PaymentPenaltyEntryBean implements ISettlementInvoiceEntryBean, ITreasuryBean, Serializable {

    private DebitEntry originDebitEntry;

    private boolean isIncluded;

    private boolean isNotValid;

    private String description;

    private LocalDate dueDate;

    private BigDecimal amount;

    public PaymentPenaltyEntryBean(DebitEntry originDebitEntry, String description, LocalDate dueDate, BigDecimal amount) {
        super();
        this.originDebitEntry = originDebitEntry;
        this.description = description;
        this.dueDate = dueDate;
        this.amount = amount;
    }

    @Override
    public InvoiceEntry getInvoiceEntry() {
        return null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public LocalDate getDueDate() {
        return dueDate;
    }

    @Override
    public BigDecimal getEntryAmount() {
        return amount;
    }

    @Override
    public BigDecimal getEntryOpenAmount() {
        return amount;
    }

    @Override
    public BigDecimal getSettledAmount() {
        return amount;
    }

    @Override
    public void setSettledAmount(BigDecimal debtAmount) {
        amount = debtAmount;
    }

    @Override
    public Vat getVat() {
        return null;
    }

    @Override
    public BigDecimal getVatRate() {
        return null;
    }

    @Override
    public boolean isIncluded() {
        return isIncluded;
    }

    @Override
    public void setIncluded(boolean isIncluded) {
        this.isIncluded = isIncluded;
    }

    @Override
    public boolean isNotValid() {
        return isNotValid;
    }

    @Override
    public void setNotValid(boolean notValid) {
        this.isNotValid = notValid;
    }

    @Override
    public FinantialDocument getFinantialDocument() {
        return null;
    }

    @Override
    public Set<Customer> getPaymentCustomer() {
        return originDebitEntry.getFinantialDocument() != null
                && ((Invoice) originDebitEntry.getFinantialDocument()).getPayorDebtAccount() != null ? Collections.singleton(
                        ((Invoice) originDebitEntry.getFinantialDocument()).getPayorDebtAccount().getCustomer()) : Collections
                                .singleton(originDebitEntry.getDebtAccount().getCustomer());
    }

    @Override
    public boolean isForDebitEntry() {
        return false;
    }

    @Override
    public boolean isForInstallment() {
        return false;
    }

    @Override
    public boolean isForCreditEntry() {
        return false;
    }

    @Override
    public boolean isForPendingInterest() {
        return false;
    }

//    @Override
//    public boolean isForPaymentPenalty() {
//        return true;
//    }

}