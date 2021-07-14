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
}
