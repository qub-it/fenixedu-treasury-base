package org.fenixedu.treasury.dto.PaymentPlans;

import java.math.BigDecimal;

import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;

public class InstallmentEntryBean {

    ISettlementInvoiceEntryBean invoiceEntry;
    BigDecimal amount;

    public InstallmentEntryBean(ISettlementInvoiceEntryBean invoiceEntry, BigDecimal amount) {
        super();
        this.invoiceEntry = invoiceEntry;
        this.amount = amount;
    }

    public ISettlementInvoiceEntryBean getInvoiceEntry() {
        return invoiceEntry;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setInvoiceEntry(ISettlementInvoiceEntryBean invoiceEntry) {
        this.invoiceEntry = invoiceEntry;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return this.invoiceEntry.getDescription();
    }
}
