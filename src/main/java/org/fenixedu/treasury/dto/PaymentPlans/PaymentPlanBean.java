package org.fenixedu.treasury.dto.PaymentPlans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanConfigurator;
import org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator.PaymentPlanValidator;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
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
    private String reason;
    private String paymentPlanId;
    private List<InstallmentBean> installmentsBean;
    private LocalDate creationDate;

    private PaymentPlanValidator paymentPlanValidator;
    private PaymentPlanConfigurator paymentPlanConfigurator;
    private boolean isChanged;
    private boolean withInitialValues;
    private Set<ISettlementInvoiceEntryBean> settlementInvoiceEntryBeans;

    public PaymentPlanBean(DebtAccount debtAccount, LocalDate creationDate) {
        super();
        this.debitEntries = new ArrayList<DebitEntry>();
        this.settlementInvoiceEntryBeans = new HashSet<ISettlementInvoiceEntryBean>();
        this.debitAmount = BigDecimal.ZERO;
        this.interestAmount = BigDecimal.ZERO;
        this.emolumentAmount = BigDecimal.ZERO;
        this.debtAccount = debtAccount;
        this.creationDate = creationDate;
        this.isChanged = false;
        this.withInitialValues = true;
    }

    public List<InstallmentBean> getInstallmentsBean() {
        if (installmentsBean == null || isChanged) {
            installmentsBean = paymentPlanConfigurator.getInstallmentsBeansFor(this);
            isChanged = false;
        }
        return installmentsBean;
    }

//    private List<InstallmentBean> createInstallmentsBean() {
//        List<InstallmentBean> result = new ArrayList<InstallmentBean>();
//
//        LocalDate installmentDueDate = startDate;
//        BigDecimal installmentAmmount = getInstallmentAmmount();
//        BigDecimal restAmount = getTotalAmount();
//        for (int i = 1; i <= nbInstallments; i++) {
//            LocalizedString installmentDescription = getPaymentPlanConfigurator().getInstallmentDescription(i, paymentPlanId);
//            if (i == nbInstallments) {
//                installmentAmmount = restAmount;
//                installmentDueDate = endDate;
//            }
//            InstallmentBean installmentBean = new InstallmentBean(installmentDueDate, installmentDescription, installmentAmmount);
//
//            result.add(installmentBean);
//            installmentDueDate = startDate.plusDays(getPlusDaysForInstallment(i));
//            restAmount = restAmount.subtract(installmentAmmount);
//        }
//
//        return result;
//    }

    public BigDecimal getTotalAmount() {
        return debitAmount.add(emolumentAmount).add(interestAmount);
    }

    public List<DebitEntry> getDebitEntries() {
        return debitEntries;
    }

    public void addDebitEntry(DebitEntry entry) {
        debitEntries.add(entry);
        this.isChanged = true;
    }

    public void removeDebitEntry(DebitEntry entry) {
        debitEntries.remove(entry);
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
        this.isChanged = true;
    }

    public BigDecimal getEmolumentAmount() {
        return emolumentAmount;
    }

    public void setEmolumentAmount(BigDecimal emolumentAmount) {
        this.emolumentAmount = emolumentAmount;
        this.isChanged = true;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
        this.isChanged = true;
    }

    public int getNbInstallments() {
        return nbInstallments;
    }

    public void setNbInstallments(int nbInstallments) {
        this.nbInstallments = nbInstallments;
        this.isChanged = true;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        this.isChanged = true;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        this.isChanged = true;
    }

    public String getPaymentPlanId() {
        return paymentPlanId;
    }

    public void setPaymentPlanId(String paymentPlanId) {
        this.paymentPlanId = paymentPlanId;
        this.isChanged = true;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
        this.isChanged = true;
    }

    public void setInstallmentsBean(List<InstallmentBean> installmentsBean) {
        this.installmentsBean = installmentsBean;
    }

    public boolean isChangedInstallmentPlan() {
        return isChanged;
    }

    public BigDecimal getTotalInstallments() {
        if (installmentsBean == null) {
            installmentsBean = paymentPlanConfigurator.getInstallmentsBeansFor(this);
        }

        return installmentsBean.stream().map(i -> i.getInstallmentAmount()).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2,
                RoundingMode.HALF_UP);
    }

    public DebtAccount getDebtAccount() {
        return debtAccount;
    }

    public void setDebtAccount(DebtAccount debtAccount) {
        this.debtAccount = debtAccount;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public PaymentPlanValidator getPaymentPlanValidator() {
        return paymentPlanValidator;
    }

    public void setPaymentPlanValidator(PaymentPlanValidator paymentPlanValidator) {
        this.paymentPlanValidator = paymentPlanValidator;
        this.isChanged = true;
    }

    public void calculeTotalAndInterestsAmount(LocalDate date) {
        BigDecimal debitAmount = BigDecimal.ZERO;
        BigDecimal interestAmount = BigDecimal.ZERO;

        for (DebitEntry debitEntry : getDebitEntries()) {
            debitAmount = debitAmount.add(debitEntry.getAmountInDebt(LocalDate.now()));
            interestAmount = interestAmount.add(debitEntry.getPendingInterestAmount(date));
        }
        setDebitAmount(debitAmount);
        setInterestAmount(interestAmount);
    }

    public BigDecimal getMaxInterestAmount() {
        return debitEntries.stream().map(d -> d.getPendingInterestAmount(creationDate)).reduce(BigDecimal.ZERO, BigDecimal::add);

    }

    public boolean isWithInitialValues() {
        return withInitialValues;
    }

    public void setWithInitialValues(boolean withInitialValues) {
        this.withInitialValues = withInitialValues;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public PaymentPlanConfigurator getPaymentPlanConfigurator() {
        return paymentPlanConfigurator;
    }

    public void setPaymentPlanConfigurator(PaymentPlanConfigurator paymentPlanConfigurator) {
        this.paymentPlanConfigurator = paymentPlanConfigurator;
        this.paymentPlanId = paymentPlanConfigurator.getNumberGenerators().getNextNumberPreview();

    }

    public Set<ISettlementInvoiceEntryBean> getSettlementInvoiceEntryBeans() {
        return settlementInvoiceEntryBeans;
    }

    public void setSettlementInvoiceEntryBeans(Set<ISettlementInvoiceEntryBean> settlementInvoiceEntryBeans) {
        this.settlementInvoiceEntryBeans = settlementInvoiceEntryBeans;
    }

    public void addSettlementInvoiceEntryBean(ISettlementInvoiceEntryBean settlementInvoiceEntryBeans) {
        this.settlementInvoiceEntryBeans.add(settlementInvoiceEntryBeans);
    }

    public void removeSettlementInvoiceEntryBean(ISettlementInvoiceEntryBean settlementInvoiceEntryBeans) {
        this.settlementInvoiceEntryBeans.remove(settlementInvoiceEntryBeans);
    }

    public void createInstallmentsBean(List<LocalDate> dates) {
        installmentsBean = paymentPlanConfigurator.getInstallmentsBeansFor(this, dates);
    }

}
