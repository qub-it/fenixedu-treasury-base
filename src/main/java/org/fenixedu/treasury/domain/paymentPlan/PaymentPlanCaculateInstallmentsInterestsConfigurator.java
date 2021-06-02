package org.fenixedu.treasury.domain.paymentPlan;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.tariff.GlobalInterestRate;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.DebitEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.InterestEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.PaymentPlanBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;

public class PaymentPlanCaculateInstallmentsInterestsConfigurator
        extends PaymentPlanCaculateInstallmentsInterestsConfigurator_Base {

    public PaymentPlanCaculateInstallmentsInterestsConfigurator() {
        super();
    }

    @Override
    protected InterestEntryBean updateRelatedInterests(DebitEntryBean bean, PaymentPlanBean paymentPlanBean,
            List<InstallmentBean> result, LocalDate lastInstallmentDueDate) {
        InterestEntryBean interestEntryBean =
                (InterestEntryBean) paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                        .filter(interestbean -> interestbean.isForPendingInterest()
                                && ((InterestEntryBean) interestbean).getDebitEntry() == bean.getInvoiceEntry())
                        .findFirst().orElse(null);

        DebitEntry debitEntry = bean.getDebitEntry();
        if (!debitEntry.isApplyInterests()) {
            return null;
        }

        BigDecimal calculatedInterests = bean.getSettledAmount().subtract(bean.getEntryOpenAmount());
        if (debitEntry.getInterestRate().getInterestType().isFixedAmount()) {
            if (TreasuryConstants.isPositive(calculatedInterests)) {
                return null;
            }

            calculatedInterests = debitEntry.calculateUndebitedInterestValue(lastInstallmentDueDate).getInterestAmount();
        }
        if (debitEntry.getInterestRate().getInterestType().isGlobalRate()) {
            LocalDate startDate = debitEntry.getDueDate().isAfter(paymentPlanBean.getCreationDate()) ? debitEntry
                    .getDueDate() : paymentPlanBean.getCreationDate();

            for (InstallmentBean installment : result) {
                InstallmentEntryBean installmentEntryBean = installment.getInstallmentEntries().stream()
                        .filter(entryBean -> entryBean.getInvoiceEntry() == bean).findFirst().orElse(null);
                if (installmentEntryBean == null) {
                    continue;
                }
                if (!installment.getDueDate().isAfter(startDate)) {
                    continue;
                }

                BigDecimal interest = calculateInterestValue(installmentEntryBean.getAmount(), startDate,
                        installment.getDueDate(), paymentPlanBean);
                calculatedInterests = calculatedInterests.add(interest);
            }
        }
        if (TreasuryConstants.isPositive(calculatedInterests)) {
            if (interestEntryBean == null) {
                InterestRateBean interestRateBean = new InterestRateBean();
                interestRateBean.setDescription(TreasuryConstants.treasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE,
                        "label.InterestRateBean.interest.designation", debitEntry.getDescription()));
                interestRateBean.setInterestAmount(calculatedInterests);
                interestEntryBean = new InterestEntryBean(debitEntry, interestRateBean);
                paymentPlanBean.addSettlementInvoiceEntryBean(interestEntryBean);
            } else {
                interestEntryBean.getInterest().setInterestAmount(calculatedInterests);
            }
            return interestEntryBean;
        } else if (interestEntryBean != null) {
            paymentPlanBean.removeSettlementInvoiceEntryBean(interestEntryBean);
        }

        return null;

    }

    private BigDecimal calculateInterestValue(BigDecimal amount, LocalDate creationDate, LocalDate dueDate,
            PaymentPlanBean paymentPlanBean) {
        BigDecimal daysBetween = new BigDecimal(Days.daysBetween(creationDate, dueDate).getDays());
        BigDecimal daysInYear = new BigDecimal(TreasuryConstants.numberOfDaysInYear(paymentPlanBean.getCreationDate().getYear()));
        BigDecimal amountPerDay = TreasuryConstants.divide(amount, daysInYear);
        Optional<GlobalInterestRate> findUniqueAppliedForDate =
                GlobalInterestRate.findUniqueAppliedForDate(paymentPlanBean.getCreationDate());
        if (findUniqueAppliedForDate.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal interestRate =
                TreasuryConstants.divide(findUniqueAppliedForDate.get().getRate(), TreasuryConstants.HUNDRED_PERCENT);
        return Currency.getValueWithScale(interestRate.multiply(amountPerDay).multiply(daysBetween));
    }

    @Override
    public boolean isInterestBlocked() {
        return Boolean.TRUE.equals(getBlockInterestBeforePaymentPlanDate());
    }
}
