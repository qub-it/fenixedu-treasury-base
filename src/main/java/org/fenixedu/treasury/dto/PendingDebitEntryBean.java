package org.fenixedu.treasury.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.joda.time.LocalDate;

public class PendingDebitEntryBean implements ISettlementInvoiceEntryBean, ITreasuryBean, Serializable {

    private Product product;
    private BigDecimal amount;
    private LocalDate dueDate;

    public PendingDebitEntryBean(Product product, BigDecimal amount, LocalDate dueDate) {
        super();
        this.product = product;
        this.amount = amount;
        this.dueDate = dueDate;
    }

    public Product getProduct() {
        return this.product;
    }

    public void setProduct(Product emolumentProduct) {
        this.product = emolumentProduct;
    }

    @Override
    public InvoiceEntry getInvoiceEntry() {
        return null;
    }

    @Override
    public String getDescription() {
        return product.getName().getContent();
    }

    @Override
    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
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
        return false;
    }

    @Override
    public void setIncluded(boolean isIncluded) {

    }

    @Override
    public boolean isNotValid() {
        return false;
    }

    @Override
    public void setNotValid(boolean notValid) {

    }

    @Override
    public FinantialDocument getFinantialDocument() {
        return null;
    }

    @Override
    public Set<Customer> getPaymentCustomer() {
        return null;
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

    @Override
    public boolean isForPaymentPenalty() {
        return false;
    }

    @Override
    public boolean isForPendingDebitEntry() {
        return true;
    }

}
