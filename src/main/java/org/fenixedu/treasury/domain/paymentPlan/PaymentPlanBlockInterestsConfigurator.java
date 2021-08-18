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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxTreasuryEvent;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.dto.PaymentPenaltyEntryBean;
import org.fenixedu.treasury.dto.SettlementInterestEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.PaymentPlanBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;

public class PaymentPlanBlockInterestsConfigurator extends PaymentPlanBlockInterestsConfigurator_Base {

    public PaymentPlanBlockInterestsConfigurator() {
        super();
    }

    @Override
    public List<InstallmentBean> getInstallmentsBeansFor(PaymentPlanBean paymentPlanBean, List<LocalDate> fixedDates,
            List<BigDecimal> fixedAmountList) {

        resetSettlementInvoiceEntryBeansForChoosenDebitEntries(paymentPlanBean);
        createInterestAndTaxBeans(paymentPlanBean);

        List<InstallmentBean> installments = createInstallmentsList(paymentPlanBean, fixedDates);
        List<BigDecimal> installmentsMaxAmount = createInstallmentMaxAmountList(paymentPlanBean, fixedAmountList);
        List<ISettlementInvoiceEntryBean> invoiceEntriesToBeTreated =
                paymentPlanBean.getSettlementInvoiceEntryBeans().stream().sorted(getComparator()).collect(Collectors.toList());

        ISettlementInvoiceEntryBean currentInvoiceEntryBean =
                invoiceEntriesToBeTreated.isEmpty() ? null : invoiceEntriesToBeTreated.get(0);
        if (!invoiceEntriesToBeTreated.isEmpty()) {
            invoiceEntriesToBeTreated.remove(0);
        }

        for (int i = 0; i < installments.size(); i++) {
            InstallmentBean currentInstallmentBean = installments.get(i);
            BigDecimal installmentAmount = installmentsMaxAmount.get(i);
            BigDecimal restInstallmentMaxAmount = installmentAmount;
            while (currentInvoiceEntryBean != null && TreasuryConstants.isPositive(restInstallmentMaxAmount)) {
                BigDecimal installmentEntryAmount = getRestAmountOfBeanInPaymentPlan(currentInvoiceEntryBean, installments);

                if (!TreasuryConstants.isPositive(installmentEntryAmount)) {
                    currentInvoiceEntryBean = invoiceEntriesToBeTreated.isEmpty() ? null : invoiceEntriesToBeTreated.get(0);
                    if (!invoiceEntriesToBeTreated.isEmpty()) {
                        invoiceEntriesToBeTreated.remove(0);
                    }
                    continue;
                }

                if (TreasuryConstants.isGreaterThan(installmentEntryAmount, restInstallmentMaxAmount)) {
                    installmentEntryAmount = restInstallmentMaxAmount;
                }

                if (isApplyInterest() && getInterestDistribution().isByInstallmentEntryAmount()
                        && isDebitEntry(currentInvoiceEntryBean) && !invoiceEntriesToBeTreated.isEmpty()
                        && invoiceEntriesToBeTreated.get(0).isForPendingInterest() && currentInvoiceEntryBean.getInvoiceEntry()
                                .equals(getOriginDebitEntryFromInterestEntry(invoiceEntriesToBeTreated.get(0)))) {

                    SettlementInterestEntryBean interestEntryBean =
                            (SettlementInterestEntryBean) invoiceEntriesToBeTreated.get(0);
                    BigDecimal restAmountOfInterestEntryBean = getRestAmountOfBeanInPaymentPlan(interestEntryBean, installments);
                    if (TreasuryConstants.isGreaterThan(restAmountOfInterestEntryBean.add(installmentEntryAmount),
                            restInstallmentMaxAmount)) {
                        installmentEntryAmount = calculateDebitEntryAmountOnInstallment(restInstallmentMaxAmount,
                                currentInvoiceEntryBean.getEntryOpenAmount(), interestEntryBean.getSettledAmount());

                        BigDecimal interestEntryAmount = restInstallmentMaxAmount.subtract(installmentEntryAmount);

                        createInstallmentEntryBean(currentInstallmentBean, interestEntryBean, interestEntryAmount);

                        restInstallmentMaxAmount = interestEntryAmount;
                    }
                }
                //CREATE InstallmentEntryBean for currentInvoiceEntryBean
                createInstallmentEntryBean(currentInstallmentBean, currentInvoiceEntryBean, installmentEntryAmount);
                restInstallmentMaxAmount = restInstallmentMaxAmount.subtract(installmentEntryAmount);
            }
        }

        return installments;
    }

    /**
     * 
     * IA = Rest amount of installment
     * TD = Total of Debt
     * TI = Total of interest amount of debt
     * D = debt amount on installment
     * I = interest amount on installment
     * 
     * IA = D + I
     * 
     * I = D / TD * TI
     * 
     * IA = D + ( D / TD * TI)
     * IA = (D (TD + TI)) / TD
     * 
     * assuming that all variables are positive
     * 
     * IA * TD = D ( TD + TI)
     * D = (IA * TD) / (TD + TI)
     */
    private BigDecimal calculateDebitEntryAmountOnInstallment(BigDecimal restInstallmentMaxAmount, BigDecimal totalDebtAmount,
            BigDecimal totalInterestAmount) {
        BigDecimal IAxTD = restInstallmentMaxAmount.multiply(totalDebtAmount);
        BigDecimal TDplusTI = totalDebtAmount.add(totalInterestAmount);
        BigDecimal D = TreasuryConstants.divide(IAxTD, TDplusTI);
        return Currency.getValueWithScale(D);
    }

    private BigDecimal getRestAmountOfBeanInPaymentPlan(ISettlementInvoiceEntryBean currInvoiceEntryBean,
            Collection<InstallmentBean> installments) {
        BigDecimal total = currInvoiceEntryBean.isForDebitEntry() ? currInvoiceEntryBean
                .getEntryOpenAmount() : currInvoiceEntryBean.getSettledAmount();

        Stream<InstallmentEntryBean> installmentEntryBeansWithInvoiceEntryBean =
                installments.stream().flatMap(inst -> inst.getInstallmentEntries().stream())
                        .filter(entry -> entry.getInvoiceEntry() == currInvoiceEntryBean);;

        BigDecimal used = installmentEntryBeansWithInvoiceEntryBean.map(entry -> entry.getAmount()).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        return total.subtract(used);
    }

    private List<BigDecimal> createInstallmentMaxAmountList(PaymentPlanBean paymentPlanBean, List<BigDecimal> fixedAmountList) {

        if (fixedAmountList != null) {
            return new ArrayList<>(fixedAmountList);
        }

        BigDecimal totalBeansAmount = paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                .map(bean -> bean.isForDebitEntry() ? bean.getEntryOpenAmount() : bean.getSettledAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal installmentAmount = Currency.getValueWithScale(
                TreasuryConstants.divide(totalBeansAmount, new BigDecimal(paymentPlanBean.getNbInstallments())));

        List<BigDecimal> result = new ArrayList<>();
        for (int i = 0; i < paymentPlanBean.getNbInstallments() - 1; i++) {
            result.add(installmentAmount);
            totalBeansAmount = totalBeansAmount.subtract(installmentAmount);
        }
        result.add(Currency.getValueWithScale(totalBeansAmount));
        return result;
    }

    private List<InstallmentBean> createInstallmentsList(PaymentPlanBean paymentPlanBean, List<LocalDate> dates) {
        if (dates == null) {
            /**
             * Create installment dates with days between installments, end date - start date / number of installments
             */
            dates = new ArrayList<>();

            double daysBetweenInstallments = paymentPlanBean.getNbInstallments() == 1 ? 0 : Days
                    .daysBetween(paymentPlanBean.getStartDate(), paymentPlanBean.getEndDate()).getDays()
                    / (paymentPlanBean.getNbInstallments() - 1.00);

            LocalDate installmentDueDate = paymentPlanBean.getStartDate();
            for (int i = 1; i <= paymentPlanBean.getNbInstallments(); i++) {
                if (i == paymentPlanBean.getNbInstallments()) {
                    installmentDueDate = paymentPlanBean.getEndDate();
                }
                dates.add(installmentDueDate);
                installmentDueDate =
                        paymentPlanBean.getStartDate().plusDays(Double.valueOf(((i) * daysBetweenInstallments)).intValue());
            }
        }

        List<InstallmentBean> result = new ArrayList<InstallmentBean>();
        for (int installmentNumber = 1; installmentNumber <= paymentPlanBean.getNbInstallments(); installmentNumber++) {
            Map<String, String> values = new HashMap<>();
            values.put("installmentNumber", "" + installmentNumber);
            values.put("paymentPlanId", paymentPlanBean.getPaymentPlanId());

            LocalizedString installmentDescription = new LocalizedString();
            for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
                installmentDescription = installmentDescription.with(locale,
                        StrSubstitutor.replace(getInstallmentDescriptionFormat().getContent(locale), values));
            }

            result.add(new InstallmentBean(dates.get(installmentNumber - 1), installmentDescription));
        }
        return result;
    }

    private void createInterestAndTaxBeans(PaymentPlanBean paymentPlanBean) {
        Set<ISettlementInvoiceEntryBean> newBeansSet = new HashSet<>(paymentPlanBean.getSettlementInvoiceEntryBeans());

        for (ISettlementInvoiceEntryBean currentInvoiceEntryBean : newBeansSet) {
            DebitEntry debitEntry = (DebitEntry) currentInvoiceEntryBean.getInvoiceEntry();
            //Interest
            if (isApplyInterest() && debitEntry.isApplyInterests() && isDebitEntry(currentInvoiceEntryBean)) {
                BigDecimal interestEntryAmout =
                        currentInvoiceEntryBean.getSettledAmount().subtract(currentInvoiceEntryBean.getEntryOpenAmount());
                if (TreasuryConstants.isPositive(interestEntryAmout)) {
                    InterestRateBean interestRateBean = new InterestRateBean();
                    interestRateBean.setDescription(TreasuryConstants.treasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE,
                            "label.InterestRateBean.interest.designation", debitEntry.getDescription()));
                    interestRateBean.setInterestAmount(interestEntryAmout);
                    SettlementInterestEntryBean interestEntryBean = new SettlementInterestEntryBean(debitEntry, interestRateBean);
                    paymentPlanBean.addSettlementInvoiceEntryBean(interestEntryBean);
                }
            }

            if (Boolean.TRUE.equals(getUsePaymentPenalty()) && isDebitEntry(currentInvoiceEntryBean)) {
                //Penalty Tax
                PaymentPenaltyEntryBean paymentPenaltyEntryBean = PaymentPenaltyTaxTreasuryEvent.calculatePaymentPenaltyTax(
                        debitEntry, paymentPlanBean.getCreationDate(), paymentPlanBean.getCreationDate());
                paymentPlanBean.addSettlementInvoiceEntryBean(paymentPenaltyEntryBean);
            }
        }
    }

    /**
     * 
     * IA = Rest amount of installment
     * TD = Total of Debt
     * TI = Total of interest amount of debt
     * D = debt amount on installment
     * I = interest amount on installment
     * 
     * IA = D + I
     * 
     * I = D / TD * TI
     * 
     * IA = D + ( D / TD * TI)
     * IA = (D (TD + TI)) / TD
     * 
     * assuming that all variables are positive
     * 
     * IA * TD = D ( TD + TI)
     * D = (IA * TD) / (TD + TI)
     */

    @Override
    public boolean isApplyInterest() {
        return Boolean.TRUE.equals(getApplyDebitEntryInterest());
    }

    @Override
    public boolean isInterestBlocked() {
        return !isApplyInterest();
    }

    @Override
    protected LocalDate getDateToUseToPenaltyTaxCalculation(LocalDate creationDate, LocalDate dueDate) {
        return creationDate;
    }

    @Override
    public boolean canChangeInstallmentsAmount() {
        return Boolean.TRUE.equals(getCanEditInstallmentAmount());
    }

}
