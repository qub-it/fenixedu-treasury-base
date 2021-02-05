package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.LocalDate;

public class PaymentPlanBean {

    private DebtAccount debtAccount;
    private List<DebitEntry> debitEntries;
    private BigDecimal debitAmount;
    private BigDecimal emolumentAmount;
    private BigDecimal interestAmount;
    private int nbInstallments;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private String name;
    private List<InstallmentBean> installmentsBean;
    private DateTime creationDate;
    private PaymentPlanValidator paymentPlanValidator;

    public PaymentPlanBean(DebtAccount debtAccount, DateTime creationDate) {
        super();
        this.debitEntries = new ArrayList<DebitEntry>();
        this.debitAmount = BigDecimal.ZERO;
        this.interestAmount = BigDecimal.ZERO;
        this.emolumentAmount = BigDecimal.ZERO;
        this.debtAccount = debtAccount;
        this.creationDate = creationDate;
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
        for (int i = 1; i <= nbInstallments; i++) {
            String installmentDescription = Installment.installmentDescription(i, name);
            if (i == nbInstallments) {
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
        return Days.daysBetween(startDate, endDate).getDays() / (nbInstallments - 1.00);
    }

    private BigDecimal getInstallmentAmmount() {
        return debtAccount.getFinantialInstitution().getCurrency()
                .getValueWithScale(TreasuryConstants.divide(getTotalAmount(), new BigDecimal(nbInstallments)));
    }

    public BigDecimal getTotalAmount() {
        return debitAmount.add(emolumentAmount).add(interestAmount);
    }

    public List<DebitEntry> getDebitEntries() {
        return debitEntries;
    }

    public void addDebitEntry(DebitEntry entry) {
        debitEntries.add(entry);
    }

    public void removeDebitEntry(DebitEntry entry) {
        debitEntries.remove(entry);
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getEmolumentAmount() {
        return emolumentAmount;
    }

    public void setEmolumentAmount(BigDecimal emolumentAmount) {
        this.emolumentAmount = emolumentAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public int getNbInstallments() {
        return nbInstallments;
    }

    public void setNbInstallments(int nbInstallments) {
        this.nbInstallments = nbInstallments;
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
        boolean diffNbInstallments = installmentsBean != null && size != getNbInstallments();
        boolean diffStartDate = installmentsBean != null && !installmentsBean.get(0).getDueDate().equals(getStartDate());
        boolean diffEndDate = installmentsBean != null && !installmentsBean.get(size - 1).getDueDate().equals(getEndDate());

        if (diffAmount || diffDescription || diffNbInstallments || diffStartDate || diffEndDate) {
            return true;
        }
        return false;

    }

    public BigDecimal getTotalInstallments() {
        if (installmentsBean == null) {
            installmentsBean = createInstallmentsBean();
        }

        return installmentsBean.stream().map(i -> i.getInstallmentAmmount()).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,
                RoundingMode.HALF_UP);
    }

    public DebtAccount getDebtAccount() {
        return debtAccount;
    }

    public void setDebtAccount(DebtAccount debtAccount) {
        this.debtAccount = debtAccount;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public PaymentPlanValidator getPaymentPlanValidator() {
        return paymentPlanValidator;
    }

    public void setPaymentPlanValidator(PaymentPlanValidator paymentPlanValidator) {
        this.paymentPlanValidator = paymentPlanValidator;
    }

    public void calculeTotalAndInterestsAmount() {
        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = BigDecimal.ZERO;

        for (DebitEntry debitEntry : getDebitEntries()) {
            debitAmount = debitAmount.add(debitEntry.getAmountInDebt(LocalDate.now()));
            interestAmount = interestAmount.add(debitEntry.getPendingInterestAmount());
        }
        setDebitAmount(debitAmount);
        setInterestAmount(interestAmount);
    }

    public BigDecimal getMaxInterestAmount() {
        return debitEntries.stream().map(d -> d.getPendingInterestAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

    }

}
