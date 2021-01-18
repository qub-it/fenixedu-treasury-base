package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;

public class PaymentPlanBean {

    private DebtAccount debtAccount;
    private List<DebitEntry> debitNotes;
    private BigDecimal debitAmount;
    private BigDecimal emulmentAmount;
    private BigDecimal interestAmount;
    private int nbInstallements;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String name;
    private List<InstallmentBean> installmentsBean;

    public PaymentPlanBean(DebtAccount debtAccount) {
        super();
        this.debitNotes = new ArrayList<DebitEntry>();
        this.debitAmount = BigDecimal.ZERO;
        this.interestAmount = BigDecimal.ZERO;
        this.emulmentAmount = BigDecimal.ZERO;
        this.debtAccount = debtAccount;
    }

    public void addPaymentPlanInformations(BigDecimal emulmentAmount, BigDecimal interestAmount, int nbInstallements,
            LocalDate startDate, LocalDate endDate, String description, String name) {
        this.emulmentAmount = emulmentAmount != null ? emulmentAmount : BigDecimal.ZERO;
        this.interestAmount = interestAmount != null ? interestAmount : BigDecimal.ZERO;
        this.nbInstallements = nbInstallements;
        this.startDate = startDate;
        this.endDate = endDate;
        this.name = name;
        this.description = description;
    }

    public List<InstallmentBean> getInstallmentsBean() {
        if (installmentsBean == null || isChangedInstallmentPlan()) {
            installmentsBean = createInstallmentsBean();
        }
        return installmentsBean;
    }

    private List<InstallmentBean> createInstallmentsBean() {
        List<InstallmentBean> result = new ArrayList<InstallmentBean>();

        LocalDate installmentDueDate = startDate;
        BigDecimal installmentAmmount = getInstallmentAmmount();
        BigDecimal restAmount = getTotalAmount();
        for (int i = 1; i <= nbInstallements; i++) {
            String installmentDescription = ConfiguracaoToDelete.getInstallmentDescription(i, name);
            if (i == nbInstallements) {
                installmentAmmount = restAmount;
                installmentDueDate = endDate;
            }
            InstallmentBean installmentBean = new InstallmentBean(installmentDueDate, installmentDescription, installmentAmmount);

            result.add(installmentBean);
            installmentDueDate = startDate.plusDays(getPlusDaysForInstallment(i));
            restAmount = restAmount.subtract(installmentAmmount);
        }

        return result;
    }

    private int getPlusDaysForInstallment(int i) {
        return Double.valueOf(((i) * getDaysBeweenInstallments())).intValue();
    }

    private double getDaysBeweenInstallments() {
        return Days.daysBetween(startDate, endDate).getDays() / (nbInstallements - 1.00);
    }

    private BigDecimal getInstallmentAmmount() {
        return debtAccount.getFinantialInstitution().getCurrency()
                .getValueWithScale(TreasuryConstants.divide(getTotalAmount(), new BigDecimal(nbInstallements)));
    }

    public BigDecimal getTotalAmount() {
        return debitAmount.add(emulmentAmount).add(interestAmount);
    }

    public List<DebitEntry> getDebitNotes() {
        return debitNotes;
    }

    public void addDebitNotes(DebitEntry entry) {
        debitNotes.add(entry);
    }

    public void removeDebitNotes(DebitEntry entry) {
        debitNotes.remove(entry);
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getEmulmentAmount() {
        return emulmentAmount;
    }

    public void setEmulmentAmount(BigDecimal emulmentAmount) {
        this.emulmentAmount = emulmentAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public int getNbInstallements() {
        return nbInstallements;
    }

    public void setNbInstallements(int nbInstallements) {
        this.nbInstallements = nbInstallements;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setInstallmentsBean(List<InstallmentBean> installmentsBean) {
        this.installmentsBean = installmentsBean;
    }

    public boolean isChangedInstallmentPlan() {
        if (installmentsBean == null) {
            return true;
        }
        int size = installmentsBean.size();
        boolean diffAmount = getTotalAmount().compareTo(getTotalInstallments()) != 0;
        boolean diffDescription = installmentsBean != null && !installmentsBean.get(0).getDescription().endsWith(getName());
        boolean diffNbInstallments = installmentsBean != null && size != getNbInstallements();
        boolean diffStartDate = installmentsBean != null && !installmentsBean.get(0).getDueDate().equals(getStartDate());
        boolean diffEndDate = installmentsBean != null && !installmentsBean.get(size - 1).getDueDate().equals(getEndDate());

        if (diffAmount || diffDescription || diffNbInstallments || diffStartDate || diffEndDate) {
            return true;
        }
        return false;

    }

    public BigDecimal getTotalInstallments() {
        if (installmentsBean == null) {
            return BigDecimal.ZERO;
        }

        return installmentsBean.stream().map(i -> i.getInstallmentAmmount()).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,
                RoundingMode.HALF_UP);
    }

    protected DebtAccount getDebtAccount() {
        return debtAccount;
    }

    protected void setDebtAccount(DebtAccount debtAccount) {
        this.debtAccount = debtAccount;
    }
}
