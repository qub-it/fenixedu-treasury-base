/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.dto.PaymentPlans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanConfigurator;
import org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator.PaymentPlanValidator;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.SettlementDebitEntryBean;
import org.joda.time.LocalDate;

public class PaymentPlanBean {

    private DebtAccount debtAccount;
    private String paymentPlanId;
    private LocalDate creationDate;
    private String reason;
    private BigDecimal emolumentAmount;
    private int nbInstallments;
    private LocalDate startDate;
    private LocalDate endDate;
    private PaymentPlanValidator paymentPlanValidator;
    private PaymentPlanConfigurator paymentPlanConfigurator;
    private List<InstallmentBean> installmentsBean;
    private String interestChangeReason;

    private Set<ISettlementInvoiceEntryBean> settlementInvoiceEntryBeans;
    private List<? extends ISettlementInvoiceEntryBean> allDebits;
    private boolean isChanged;
    private boolean withInitialValues;

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
                    SettlementDebitEntryBean debitEntryBean = new SettlementDebitEntryBean((DebitEntry) debitEntry);
                    debitEntryBean.setSettledAmount(((DebitEntry) debitEntry).getOpenAmountWithInterestsAtDate(creationDate));
                    return debitEntryBean;
                }).collect(Collectors.toList());
    }

    public void updateDebitEntriesSettleAmountInPaymentPlan() {
        for (ISettlementInvoiceEntryBean iSettlementInvoiceEntryBean : this.allDebits) {
            if (!iSettlementInvoiceEntryBean.isForDebitEntry()) {
                continue;
            }

            SettlementDebitEntryBean debitEntryBean = (SettlementDebitEntryBean) iSettlementInvoiceEntryBean;
            debitEntryBean.setSettledAmount(debitEntryBean.getDebitEntry().getOpenAmountWithInterestsAtDate(this.creationDate));
        }
    }

    public List<InstallmentBean> getInstallmentsBean() {
        if (installmentsBean == null || isChanged) {
            createInstallmentsBean(null, null);
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
        return getInstallmentsBean().stream().map(i -> i.getInstallmentAmount()).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
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
        if (fixedDates != null && !fixedDates.isEmpty()) {
            setStartDate(fixedDates.get(0));
            setEndDate(fixedDates.get(fixedDates.size() - 1));
        }
        installmentsBean = paymentPlanConfigurator.getInstallmentsBeansFor(this, fixedDates, fixedAmounts);
        isChanged = false;
    }

    public List<? extends ISettlementInvoiceEntryBean> getAllDebits() {
        return allDebits;
    }

    public void setAllDebits(List<? extends ISettlementInvoiceEntryBean> allDebits) {
        this.allDebits = allDebits;
    }

    public String getInterestChangeReason() {
        return interestChangeReason;
    }

    public void setInterestChangeReason(String interestChangeReason) {
        this.interestChangeReason = interestChangeReason;
    }
}
