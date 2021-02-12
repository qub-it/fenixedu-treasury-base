package org.fenixedu.treasury.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentPlan.InstallmentEntry;
import org.joda.time.LocalDate;

public class InstallmentPaymenPlanBean implements ISettlementInvoiceEntryBean, ITreasuryBean, Serializable {

    private static final long serialVersionUID = 1L;

    private boolean isIncluded;

    private boolean isNotValid;

    private BigDecimal settledAmount;

    private Installment installment;

    public InstallmentPaymenPlanBean() {
    }

    public InstallmentPaymenPlanBean(Installment installment) {
        this.installment = installment;
        this.isIncluded = false;
        this.isNotValid = false;
        this.settledAmount = installment.getOpenAmount();
    }

    public Installment getInstallment() {
        return installment;
    }

    public void setInstallment(Installment installment) {
        this.installment = installment;
    }

    @Override
    public InvoiceEntry getInvoiceEntry() {
        return null;
    }

    @Override
    public String getDescription() {
        // TODO: This method should not deal with presentation
        String result = installment.getDescription().getContent() + ":<br>";

        for (InstallmentEntry element : installment.getSortedInstallmentEntries()) {
            result = result + "  - " + element.getDebitEntry().getDescription() + " <br>";
        }

        return result;
    }

    @Override
    public LocalDate getDueDate() {
        return installment.getDueDate();
    }

    @Override
    public BigDecimal getEntryAmount() {
        return installment.getTotalAmount();
    }

    @Override
    public BigDecimal getEntryOpenAmount() {
        return installment.getOpenAmount();
    }

    @Override
    public BigDecimal getSettledAmount() {
        return settledAmount;
    }

    @Override
    public void setSettledAmount(BigDecimal debtAmount) {
        this.settledAmount = debtAmount;

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
        return installment.getInstallmentEntriesSet().stream().map(entry -> entry.getDebitEntry().getDebitNote() != null
                && entry.getDebitEntry().getDebitNote().getPayorDebtAccount() != null ? entry.getDebitEntry().getDebitNote()
                        .getPayorDebtAccount().getCustomer() : entry.getDebitEntry().getDebtAccount().getCustomer())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean isForDebitEntry() {
        return false;
    }
    
    @Override
    public boolean isForInstallment() {
        return true;
    }
    
    @Override
    public boolean isForCreditEntry() {
        return false;
    }
    
    @Override
    public boolean isForPendingInterest() {
        return false;
    }
}
