package org.fenixedu.treasury.domain.paymentPlan.beans;

import java.math.BigDecimal;

import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;

public class InstallmentBean {

    private LocalDate dueDate;
    private LocalizedString description;
    private BigDecimal installmentAmmount;

    public InstallmentBean(LocalDate installmentDueDate, LocalizedString description, BigDecimal installmentAmmount) {
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

    public LocalizedString getDescription() {
        return description;
    }

    public void setDescription(LocalizedString description) {
        this.description = description;
    }

    public BigDecimal getInstallmentAmmount() {
        return installmentAmmount;
    }

    public void setInstallmentAmmount(BigDecimal installmentAmmount) {
        this.installmentAmmount = installmentAmmount;
    }
}
