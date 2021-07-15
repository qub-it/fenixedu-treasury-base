package org.fenixedu.treasury.dto.PaymentPlans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanConfigurator;
import org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator.PaymentPlanValidator;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.DebitEntryBean;
import org.joda.time.LocalDate;

public class PaymentPlanBean {

    private DebtAccount debtAccount;
    private BigDecimal emolumentAmount;
    private int nbInstallments;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String paymentPlanId;
    private List<InstallmentBean> installmentsBean;
    private LocalDate creationDate;
    private List<? extends ISettlementInvoiceEntryBean> allDebits;

    private PaymentPlanValidator paymentPlanValidator;
    private PaymentPlanConfigurator paymentPlanConfigurator;
    private boolean isChanged;
    private boolean withInitialValues;
    private Set<ISettlementInvoiceEntryBean> settlementInvoiceEntryBeans;
    private Map<DebitEntryBean, BigDecimal> extraInterestWarning;

    public PaymentPlanBean(DebtAccount debtAccount, LocalDate creationDate) {
        super();
        this.settlementInvoiceEntryBeans = new HashSet<ISettlementInvoiceEntryBean>();
        this.emolumentAmount = BigDecimal.ZERO;
        this.debtAccount = debtAccount;
        this.creationDate = creationDate;
        this.isChanged = false;
        this.withInitialValues = true;

        allDebits = debtAccount.getPendingInvoiceEntriesSet().stream()
                .filter(f -> f.isDebitNoteEntry() && !((DebitEntry) f).isInOpenPaymentPlan()).map((debitEntry) -> {
                    DebitEntryBean debitEntryBean = new DebitEntryBean((DebitEntry) debitEntry);
                    debitEntryBean.setSettledAmount(debitEntry.getOpenAmountWithInterests());
                    return debitEntryBean;
                }).collect(Collectors.toList());
    }

    public List<InstallmentBean> getInstallmentsBean() {
        if (installmentsBean == null || isChanged) {
            installmentsBean = paymentPlanConfigurator.getInstallmentsBeansFor(this);
            isChanged = false;
        }
        return installmentsBean;
    }

    public BigDecimal getEmolumentAmount() {
        return emolumentAmount;
    }

    public void setEmolumentAmount(BigDecimal emolumentAmount) {
        this.emolumentAmount = emolumentAmount;
        this.isChanged = true;
    }

    public int getNbInstallments() {
        return nbInstallments;
    }

    public void setChanged(boolean isChanged) {
        this.isChanged = isChanged;
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

    public void createInstallmentsBean(List<LocalDate> fixedDates, List<BigDecimal> fixedAmounts) {
        installmentsBean = paymentPlanConfigurator.getInstallmentsBeansFor(this, fixedDates, fixedAmounts);
    }

    public List<? extends ISettlementInvoiceEntryBean> getAllDebits() {
        return allDebits;
    }

    public void setAllDebits(List<? extends ISettlementInvoiceEntryBean> allDebits) {
        this.allDebits = allDebits;
    }

    public void setExtraInterestWarning(Map<DebitEntryBean, BigDecimal> result) {
        this.extraInterestWarning = result;
    }

    public Map<DebitEntryBean, BigDecimal> getExtraInterestWarning() {
        return this.extraInterestWarning;
    }
}
