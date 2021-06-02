package org.fenixedu.treasury.dto.PaymentPlans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

import org.fenixedu.commons.i18n.LocalizedString;
import org.joda.time.LocalDate;

public class InstallmentBean {

    private LocalDate dueDate;
    private LocalizedString description;
    Set<InstallmentEntryBean> installmentEntries;

    public InstallmentBean(LocalDate installmentDueDate, LocalizedString description) {
        super();
        this.dueDate = installmentDueDate;
        this.description = description;
        this.installmentEntries = new HashSet<>();
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

    public Set<InstallmentEntryBean> getInstallmentEntries() {
        return installmentEntries;
    }

    public void setInstallmentEntries(Set<InstallmentEntryBean> installmentEntries) {
        this.installmentEntries = installmentEntries;
    }

    public void addInstallmentEntries(InstallmentEntryBean installmentEntry) {
        this.installmentEntries.add(installmentEntry);
    }

    public BigDecimal getInstallmentAmount() {
        return installmentEntries.stream().map(i -> i.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,
                RoundingMode.HALF_UP);
    }

}
