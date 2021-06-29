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
package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.fenixedu.treasury.domain.tariff.GlobalInterestRate;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentEntryBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;

public class PaymentPlanCalculateInstallmentsInterestsConfigurator
        extends PaymentPlanCalculateInstallmentsInterestsConfigurator_Base {

    public PaymentPlanCalculateInstallmentsInterestsConfigurator() {
        super();
    }

    @Override
    public boolean isInterestBlocked() {
        return true;
    }

    @Override
    public boolean canChangeInstallmentsAmount() {
        return false;
    };

    @Override
    protected BigDecimal getInterestAmountOfCurrentInvoiceEntryBeanToInstallment(
            PaymentPlanInstallmentCreationBean installmentsCreationBean, ISettlementInvoiceEntryBean currentInvoiceEntryBean,
            BigDecimal amount, LocalDate fromDate, LocalDate toDate, boolean isLastInstallmentOfCurrInvoiceEntryBean) {
        /**
         * Calculate interestAmount before payment plan request date
         *
         * Calculate interestAmount after payment plan start date
         *
         * return interestAmountBeforePaymentPlan + interestAmountAfterPaymentPlan
         */

        BigDecimal interestAmountBeforePaymentPlan = BigDecimal.ZERO;

        if (fromDate.isBefore(installmentsCreationBean.getRequestDate())) {
            interestAmountBeforePaymentPlan =
                    super.getInterestAmountOfCurrentInvoiceEntryBeanToInstallment(installmentsCreationBean,
                            currentInvoiceEntryBean, amount, fromDate, toDate, isLastInstallmentOfCurrInvoiceEntryBean);
        }

        LocalDate startDate = currentInvoiceEntryBean.getDueDate()
                .isAfter(installmentsCreationBean.getPaymentPlanStartDate()) ? currentInvoiceEntryBean
                        .getDueDate() : installmentsCreationBean.getPaymentPlanStartDate();

        BigDecimal interestAmountAfterPaymentPlan = calculateInterestValue(installmentsCreationBean, amount, startDate, toDate);

        return interestAmountBeforePaymentPlan.add(interestAmountAfterPaymentPlan);
    }

    @Override
    protected BigDecimal getInterestAmountToPaymentPlan(PaymentPlanInstallmentCreationBean installmentsCreationBean,
            ISettlementInvoiceEntryBean currentInvoiceEntryBean) {
        //get Installments with invoice entry bean
        BigDecimal totalInterestAmount = BigDecimal.ZERO;
        List<InstallmentBean> collect = installmentsCreationBean.getInstallmentBeansWithInvoiceEntryBean(currentInvoiceEntryBean)
                .collect(Collectors.toList());

        /**
         * sum interestAmount of each installment
         */

        boolean isLastinstallment = false;
        for (int i = 0; i < collect.size(); i++) {
            if (i == collect.size() - 1) {
                isLastinstallment = true;
            }
            InstallmentBean installment = collect.get(i);

            InstallmentEntryBean installmentEntryBean = installment.getInstallmentEntries().stream()
                    .filter(entryBean -> entryBean.getInvoiceEntry() == currentInvoiceEntryBean).findFirst().get();

            BigDecimal installmentInterestAmount = getInterestAmountOfCurrentInvoiceEntryBeanToInstallment(
                    installmentsCreationBean, currentInvoiceEntryBean, installmentEntryBean.getAmount(),
                    installmentEntryBean.getInvoiceEntry().getDueDate(), installment.getDueDate(), isLastinstallment);
            totalInterestAmount = totalInterestAmount.add(installmentInterestAmount);

        }

        return totalInterestAmount;
    }

    @Override
    protected LocalDate getDateToUseToPenaltyTaxCalculation(LocalDate creationDate, LocalDate installmentDate) {
        return installmentDate;
    }

    private BigDecimal calculateInterestValue(PaymentPlanInstallmentCreationBean installmentsCreationBean, BigDecimal amount,
            LocalDate fromDate, LocalDate toDate) {
        BigDecimal daysBetween = new BigDecimal(Days.daysBetween(fromDate, toDate).getDays());
        BigDecimal daysInYear =
                new BigDecimal(TreasuryConstants.numberOfDaysInYear(installmentsCreationBean.getRequestDate().getYear()));
        BigDecimal amountPerDay = TreasuryConstants.divide(amount, daysInYear);
        Optional<GlobalInterestRate> findUniqueAppliedForDate =
                GlobalInterestRate.findUniqueAppliedForDate(installmentsCreationBean.getRequestDate());
        if (findUniqueAppliedForDate.isEmpty() || !TreasuryConstants.isPositive(daysBetween)) {
            return BigDecimal.ZERO;
        }
        BigDecimal interestRate =
                TreasuryConstants.divide(findUniqueAppliedForDate.get().getRate(), TreasuryConstants.HUNDRED_PERCENT);

        // interestRate * (amount / daysInYear) * (days Between fromDate and toDate)
        return interestRate.multiply(amountPerDay).multiply(daysBetween);
    }

    @Override
    public boolean isApplyInterest() {
        return true;
    }
}
