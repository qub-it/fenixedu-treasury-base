package org.fenixedu.treasury.domain.paymentPlan.beans;

import java.math.BigDecimal;

import org.joda.time.LocalDate;

public class InstallmentBean {

    private LocalDate dueDate;
    private String description;
    private BigDecimal installmentAmmount;

    public InstallmentBean(LocalDate installmentDueDate, String description, BigDecimal installmentAmmount) {
        super();
        this.dueDate = installmentDueDate;
        this.description = description;
        this.installmentAmmount = installmentAmmount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getInstallmentAmmount() {
        return installmentAmmount;
    }

    public void setInstallmentAmmount(BigDecimal installmentAmmount) {
        this.installmentAmmount = installmentAmmount;
    }
}
