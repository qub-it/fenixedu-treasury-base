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
package org.fenixedu.treasury.domain.paymentPlan.beans;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentPlan.PaymentPlanSettings;
import org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator.PaymentPlanValidator;
import org.fenixedu.treasury.util.TreasuryConstants;
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
    private String reason;
    private String paymentPlanId;
    private List<InstallmentBean> installmentsBean;
    private LocalDate creationDate;

    private PaymentPlanValidator paymentPlanValidator;
    private boolean isChanged;
    private boolean withInitialValues;

    public PaymentPlanBean(DebtAccount debtAccount, LocalDate creationDate) {
        super();
        this.debitEntries = new ArrayList<DebitEntry>();
        this.debitAmount = BigDecimal.ZERO;
        this.interestAmount = BigDecimal.ZERO;
        this.emolumentAmount = BigDecimal.ZERO;
        this.debtAccount = debtAccount;
        this.creationDate = creationDate;
        this.paymentPlanId = PaymentPlanSettings.getActiveInstance().getNumberGenerators().getNextNumberPreview();
        this.isChanged = false;
        this.withInitialValues = true;
    }

    public List<InstallmentBean> getInstallmentsBean() {
        if (installmentsBean == null || isChanged) {
            installmentsBean = createInstallmentsBean();
            isChanged = false;
        }
        return installmentsBean;
    }

    private List<InstallmentBean> createInstallmentsBean() {
        List<InstallmentBean> result = new ArrayList<InstallmentBean>();

        LocalDate installmentDueDate = startDate;
        BigDecimal installmentAmmount = getInstallmentAmmount();
        BigDecimal restAmount = getTotalAmount();
        for (int i = 1; i <= nbInstallments; i++) {
            LocalizedString installmentDescription = Installment.installmentDescription(i, paymentPlanId);
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
}
