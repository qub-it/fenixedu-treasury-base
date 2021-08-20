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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxTreasuryEvent;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.dto.PaymentPenaltyEntryBean;
import org.fenixedu.treasury.dto.SettlementInterestEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.AddictionsCalculeTypeEnum;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.PaymentPlanBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPlanConfigurator extends PaymentPlanConfigurator_Base {

    // Debt < Interest < PenaltyTax
    private static final Comparator<ISettlementInvoiceEntryBean> COMPARE_SETTLEMENT_INVOICE_ENTRY_BEAN_PENALTY_TAX_AFTER =
            (s1, s2) -> {
                if (s1.isForPendingDebitEntry() || s2.isForPendingDebitEntry()) {
                    return s1.isForPendingDebitEntry() ? -1 : 1;
                }
                DebitEntry interestEntryS1 = getOriginDebitEntryFromInterestEntry(s1);
                DebitEntry penaltyTaxEntryS1 = interestEntryS1 == null ? getOriginDebitEntryFromPenaltyTaxEntry(s1) : null;
                DebitEntry debitEntryS1 =
                        penaltyTaxEntryS1 == null && interestEntryS1 == null ? (DebitEntry) s1.getInvoiceEntry() : null;

                DebitEntry interestEntryS2 = getOriginDebitEntryFromInterestEntry(s2);
                DebitEntry penaltyTaxEntryS2 = interestEntryS2 == null ? getOriginDebitEntryFromPenaltyTaxEntry(s2) : null;
                DebitEntry debitEntryS2 =
                        penaltyTaxEntryS2 == null && interestEntryS2 == null ? (DebitEntry) s2.getInvoiceEntry() : null;

                if (debitEntryS1 != null) {
                    if (debitEntryS2 != null) {
                        return compareDebitEntryDueDate(debitEntryS1, debitEntryS2);
                    }
                    if (interestEntryS2 != null) {
                        return debitEntryS1 == interestEntryS2 ? -1 : compareDebitEntryDueDate(debitEntryS1, interestEntryS2);
                    }
                    if (penaltyTaxEntryS2 != null) {
                        return debitEntryS1 == penaltyTaxEntryS2 ? -1 : compareDebitEntryDueDate(debitEntryS1, penaltyTaxEntryS2);
                    }
                }

                if (interestEntryS1 != null) {
                    if (debitEntryS2 != null) {
                        return interestEntryS1 == debitEntryS2 ? 1 : compareDebitEntryDueDate(interestEntryS1, debitEntryS2);
                    }
                    if (interestEntryS2 != null) {
                        return interestEntryS1 == interestEntryS2 ? (s1
                                .isForPendingInterest() ? 1 : -1) : compareDebitEntryDueDate(interestEntryS1, interestEntryS2);
                    }
                    if (penaltyTaxEntryS2 != null) {
                        return interestEntryS1 == penaltyTaxEntryS2 ? -1 : compareDebitEntryDueDate(interestEntryS1,
                                penaltyTaxEntryS2);
                    }
                }

                if (penaltyTaxEntryS1 != null) {
                    if (debitEntryS2 != null) {
                        return penaltyTaxEntryS1 == debitEntryS2 ? 1 : compareDebitEntryDueDate(penaltyTaxEntryS1, debitEntryS2);
                    }
                    if (interestEntryS2 != null) {
                        return penaltyTaxEntryS1 == interestEntryS2 ? 1 : compareDebitEntryDueDate(penaltyTaxEntryS1,
                                interestEntryS2);
                    }
                    if (penaltyTaxEntryS2 != null) {
                        return compareDebitEntryDueDate(penaltyTaxEntryS1, penaltyTaxEntryS2);
                    }
                }
                return s1.getDueDate().compareTo(s2.getDueDate());
            };

    //PenaltyTax<Debt<Interest
    private static final Comparator<ISettlementInvoiceEntryBean> COMPARE_SETTLEMENT_INVOICE_ENTRY_BEAN_PENALTY_TAX_BEFORE =
            (s1, s2) -> {
                if (s1.isForPendingDebitEntry() || s2.isForPendingDebitEntry()) {
                    return s1.isForPendingDebitEntry() ? -1 : 1;
                }
                DebitEntry interestEntryS1 = getOriginDebitEntryFromInterestEntry(s1);
                DebitEntry penaltyTaxEntryS1 = interestEntryS1 == null ? getOriginDebitEntryFromPenaltyTaxEntry(s1) : null;
                DebitEntry debitEntryS1 =
                        penaltyTaxEntryS1 == null && interestEntryS1 == null ? (DebitEntry) s1.getInvoiceEntry() : null;

                DebitEntry interestEntryS2 = getOriginDebitEntryFromInterestEntry(s2);
                DebitEntry penaltyTaxEntryS2 = interestEntryS2 == null ? getOriginDebitEntryFromPenaltyTaxEntry(s2) : null;
                DebitEntry debitEntryS2 =
                        penaltyTaxEntryS2 == null && interestEntryS2 == null ? (DebitEntry) s2.getInvoiceEntry() : null;

                if (debitEntryS1 != null) {
                    if (debitEntryS2 != null) {
                        return compareDebitEntryDueDate(debitEntryS1, debitEntryS2);
                    }
                    if (interestEntryS2 != null) {
                        return debitEntryS1 == interestEntryS2 ? -1 : compareDebitEntryDueDate(debitEntryS1, interestEntryS2);
                    }
                    if (penaltyTaxEntryS2 != null) {
                        return debitEntryS1 == penaltyTaxEntryS2 ? 1 : compareDebitEntryDueDate(debitEntryS1, penaltyTaxEntryS2);
                    }
                }

                if (interestEntryS1 != null) {
                    if (debitEntryS2 != null) {
                        return interestEntryS1 == debitEntryS2 ? 1 : compareDebitEntryDueDate(interestEntryS1, debitEntryS2);
                    }
                    if (interestEntryS2 != null) {
                        return interestEntryS1 == interestEntryS2 ? (s1
                                .isForPendingInterest() ? 1 : -1) : compareDebitEntryDueDate(interestEntryS1, interestEntryS2);
                    }
                    if (penaltyTaxEntryS2 != null) {
                        return interestEntryS1 == penaltyTaxEntryS2 ? 1 : compareDebitEntryDueDate(interestEntryS1,
                                penaltyTaxEntryS2);
                    }
                }

                if (penaltyTaxEntryS1 != null) {
                    if (debitEntryS2 != null) {
                        return penaltyTaxEntryS1 == debitEntryS2 ? -1 : compareDebitEntryDueDate(penaltyTaxEntryS1, debitEntryS2);
                    }
                    if (interestEntryS2 != null) {
                        return penaltyTaxEntryS1 == interestEntryS2 ? -1 : compareDebitEntryDueDate(penaltyTaxEntryS1,
                                interestEntryS2);
                    }
                    if (penaltyTaxEntryS2 != null) {
                        return compareDebitEntryDueDate(penaltyTaxEntryS1, penaltyTaxEntryS2);
                    }
                }
                return s1.getDueDate().compareTo(s2.getDueDate());
            };

    public PaymentPlanConfigurator() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        super.setActive(Boolean.FALSE);
        setTreasurySettings(TreasurySettings.getInstance());
    }

    public PaymentPlanConfigurator(LocalizedString name, LocalizedString installmentDescriptionFormat, Boolean usePaymentPenalty,
            AddictionsCalculeTypeEnum interestDistribuition, AddictionsCalculeTypeEnum paymentPenaltyDistribuition,
            Product emolumentProduct, PaymentPlanNumberGenerator numberGenerator) {
        this();

        setName(name);
        setInstallmentDescriptionFormat(installmentDescriptionFormat);
        setUsePaymentPenalty(usePaymentPenalty);
        setEmolumentProduct(emolumentProduct);
        setNumberGenerators(numberGenerator);
        setInterestDistribution(interestDistribuition);
        setPaymentPenaltyDistribution(paymentPenaltyDistribuition);

        checkRules();
    }

    private void checkRules() {
        if (getTreasurySettings() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.treasurySettings.required");
        }
        if (getInstallmentDescriptionFormat() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.InstallmentDescriptionFormat.required");
        }

        if (getEmolumentProduct() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.EmolumentProduct.required");
        }

        if (getInstallmentDescriptionFormat().anyMatch(o -> !o.contains("${paymentPlanId}"))) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.installmentDescriptionFormat.payment.plan.id.required");
        }

        if (getInstallmentDescriptionFormat().anyMatch(o -> !o.contains("${installmentNumber}"))) {
            throw new TreasuryDomainException(
                    "error.PaymentPlanSettings.installmentDescriptionFormat.installment.number.required");
        }
        if (getNumberGenerators() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.NumberGenerators.required");
        }

        if (getInterestDistribution() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.interestDistribution.required");
        }

        if (Boolean.TRUE.equals(getUsePaymentPenalty()) && getPaymentPenaltyDistribution() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.paymentPenaltyDistribution.required");
        }

    }

    public boolean isApplyInterest() {
        return Boolean.TRUE.equals(getApplyDebitEntryInterest());
    }

    @Override
    @Atomic
    public void setActive(Boolean active) {
        super.setActive(active);
    }

    public Boolean isActive() {
        return Boolean.TRUE.equals(getActive());
    }

    @Atomic
    public void delete() {
        if (getActive().booleanValue()) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.active.cannot.be.deleted");
        }

        setDomainRoot(null);
        setTreasurySettings(null);
        setEmolumentProduct(null);
        setNumberGenerators(null);

        super.deleteDomainObject();
    }

    public static Stream<PaymentPlanConfigurator> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPlanConfiguratorsSet().stream();
    }

    public static Stream<PaymentPlanConfigurator> findActives() {
        return findAll().filter(p -> Boolean.TRUE.equals(p.getActive()));
    }

    public Comparator<ISettlementInvoiceEntryBean> getComparator() {
        if (AddictionsCalculeTypeEnum.BEFORE_DEBIT_ENTRY == getPaymentPenaltyDistribution()) {
            return PaymentPlanConfigurator.COMPARE_SETTLEMENT_INVOICE_ENTRY_BEAN_PENALTY_TAX_BEFORE;
        } else {
            return PaymentPlanConfigurator.COMPARE_SETTLEMENT_INVOICE_ENTRY_BEAN_PENALTY_TAX_AFTER;
        }
    }

    public List<InstallmentBean> getInstallmentsBeansFor(PaymentPlanBean paymentPlanBean) {
        return getInstallmentsBeansFor(paymentPlanBean, null, null);
    }

    public List<InstallmentBean> getInstallmentsBeansFor(PaymentPlanBean paymentPlanBean, List<LocalDate> fixedDates,
            List<BigDecimal> fixedAmountList) {

        createInterestAndTaxBeans(paymentPlanBean);

        List<InstallmentBean> installments = createInstallmentsList(paymentPlanBean, fixedDates);
        List<BigDecimal> installmentsMaxAmount = createInstallmentMaxAmountList(paymentPlanBean, fixedAmountList);
        List<ISettlementInvoiceEntryBean> invoiceEntriesToBeTreated =
                paymentPlanBean.getSettlementInvoiceEntryBeans().stream().sorted(getComparator()).collect(Collectors.toList());

        ISettlementInvoiceEntryBean currentInvoiceEntryBean = pullNextCurrentInvoiceEntryBean(invoiceEntriesToBeTreated);

        for (int i = 0; i < installments.size(); i++) {
            InstallmentBean currentInstallmentBean = installments.get(i);
            BigDecimal installmentAmount = installmentsMaxAmount.get(i);
            BigDecimal restInstallmentMaxAmount = installmentAmount;
            while (currentInvoiceEntryBean != null && TreasuryConstants.isPositive(restInstallmentMaxAmount)) {
                BigDecimal installmentEntryAmount = getRestAmountOfBeanInPaymentPlan(currentInvoiceEntryBean, installments);

                if (!TreasuryConstants.isPositive(installmentEntryAmount)) {
                    currentInvoiceEntryBean = pullNextCurrentInvoiceEntryBean(invoiceEntriesToBeTreated);
                    continue;
                }

                if (TreasuryConstants.isGreaterThan(installmentEntryAmount, restInstallmentMaxAmount)) {
                    installmentEntryAmount = restInstallmentMaxAmount;
                }

                if (isInterestTodistributeByInstallmentEntryAmountAndCurrentInvoiceEntryHaveInterest(invoiceEntriesToBeTreated,
                        currentInvoiceEntryBean)) {
                    //get iterest referent at current invoice entry and that rest amount
                    ISettlementInvoiceEntryBean interestEntryBean = invoiceEntriesToBeTreated.get(0);
                    BigDecimal restAmountOfInterestEntryBean = getRestAmountOfBeanInPaymentPlan(interestEntryBean, installments);

                    if (TreasuryConstants.isGreaterThan(restAmountOfInterestEntryBean.add(installmentEntryAmount),
                            restInstallmentMaxAmount)) {
                        installmentEntryAmount = calculateDebitEntryAmountOnInstallment(restInstallmentMaxAmount,
                                currentInvoiceEntryBean.getEntryOpenAmount(), interestEntryBean.getSettledAmount());

                        //process interestEntryBean
                        BigDecimal interestEntryAmount = restInstallmentMaxAmount.subtract(installmentEntryAmount);
                        createorUpdateInstallmentEntryBean(currentInstallmentBean, interestEntryBean, interestEntryAmount);
                        restInstallmentMaxAmount = interestEntryAmount;
                    }
                }
                //CREATE InstallmentEntryBean for currentInvoiceEntryBean
                createorUpdateInstallmentEntryBean(currentInstallmentBean, currentInvoiceEntryBean, installmentEntryAmount);
                restInstallmentMaxAmount = restInstallmentMaxAmount.subtract(installmentEntryAmount);
            }
        }
        return installments;
    }

    private ISettlementInvoiceEntryBean pullNextCurrentInvoiceEntryBean(
            List<ISettlementInvoiceEntryBean> invoiceEntriesToBeTreated) {
        ISettlementInvoiceEntryBean currentInvoiceEntryBean =
                invoiceEntriesToBeTreated.isEmpty() ? null : invoiceEntriesToBeTreated.get(0);
        if (!invoiceEntriesToBeTreated.isEmpty()) {
            invoiceEntriesToBeTreated.remove(0);
        }
        return currentInvoiceEntryBean;
    }

    private boolean isInterestTodistributeByInstallmentEntryAmountAndCurrentInvoiceEntryHaveInterest(
            List<ISettlementInvoiceEntryBean> invoiceEntriesToBeTreated, ISettlementInvoiceEntryBean currentInvoiceEntryBean) {

        boolean isInterestTodistributeByInstallmentEntryAmount = isApplyInterest()
                && getInterestDistribution().isByInstallmentEntryAmount() && isDebitEntry(currentInvoiceEntryBean);

        //if current invoice entry have interest, than is next invoice bean to be treated
        boolean currentInvoiceEntryHaveInterest = !invoiceEntriesToBeTreated.isEmpty()
                && invoiceEntriesToBeTreated.get(0).isForPendingInterest() && currentInvoiceEntryBean.getInvoiceEntry()
                        .equals(getOriginDebitEntryFromInterestEntry(invoiceEntriesToBeTreated.get(0)));

        return isInterestTodistributeByInstallmentEntryAmount && currentInvoiceEntryHaveInterest;
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

            if (paymentPlanBean.getNbInstallments() == 1) {
                dates.add(paymentPlanBean.getStartDate());
            } else {
                double daysBetweenInstallments =
                        Days.daysBetween(paymentPlanBean.getStartDate(), paymentPlanBean.getEndDate()).getDays()
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

        //reset settlementInvoiceEntries
        paymentPlanBean.setSettlementInvoiceEntryBeans(paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                .filter(bean -> bean.isForDebitEntry() || bean.isForPendingDebitEntry()).collect(Collectors.toSet()));

        Set<ISettlementInvoiceEntryBean> newBeansSet = new HashSet<>(paymentPlanBean.getSettlementInvoiceEntryBeans());

        for (ISettlementInvoiceEntryBean currentInvoiceEntryBean : newBeansSet) {
            if (!isDebitEntry(currentInvoiceEntryBean)) {
                continue;
            }
            DebitEntry debitEntry = (DebitEntry) currentInvoiceEntryBean.getInvoiceEntry();
            //Interest
            if (isApplyInterest() && debitEntry.isApplyInterests()) {
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
            //PaymentPenalty Tax
            if (Boolean.TRUE.equals(getUsePaymentPenalty()) && isDebitEntry(currentInvoiceEntryBean)) {
                //Penalty Tax
                PaymentPenaltyEntryBean paymentPenaltyEntryBean = PaymentPenaltyTaxTreasuryEvent.calculatePaymentPenaltyTax(
                        debitEntry, paymentPlanBean.getCreationDate(), paymentPlanBean.getCreationDate());
                paymentPlanBean.addSettlementInvoiceEntryBean(paymentPenaltyEntryBean);
            }
        }
    }

    private InstallmentEntryBean createorUpdateInstallmentEntryBean(InstallmentBean currentInstallmentBean,
            ISettlementInvoiceEntryBean currentInvoiceEntryBean, BigDecimal installmentEntryAmount) {
        Optional<InstallmentEntryBean> installmentEntryBeanOptional = currentInstallmentBean.getInstallmentEntries().stream()
                .filter(bean -> bean.getInvoiceEntry() == currentInvoiceEntryBean).findFirst();
        InstallmentEntryBean installmentEntryBean = null;
        if (installmentEntryBeanOptional.isEmpty()) {
            installmentEntryBean = new InstallmentEntryBean(currentInvoiceEntryBean, installmentEntryAmount);
            currentInstallmentBean.addInstallmentEntries(installmentEntryBean);
        } else {
            installmentEntryBean = installmentEntryBeanOptional.get();
            installmentEntryBean.setAmount(installmentEntryBean.getAmount().add(installmentEntryAmount));
        }
        return installmentEntryBean;
    }

    private boolean isDebitEntry(ISettlementInvoiceEntryBean currentInvoiceEntryBean) {
        return currentInvoiceEntryBean.isForDebitEntry()
                && ((DebitEntry) currentInvoiceEntryBean.getInvoiceEntry()).getDebitEntry() == null
                && !(((DebitEntry) currentInvoiceEntryBean.getInvoiceEntry())
                        .getTreasuryEvent() instanceof PaymentPenaltyTaxTreasuryEvent);
    }

    protected static DebitEntry getOriginDebitEntryFromInterestEntry(ISettlementInvoiceEntryBean bean) {
        DebitEntry debitEntry = null;
        if (bean.isForPendingInterest()
                || (bean.isForDebitEntry() && ((DebitEntry) bean.getInvoiceEntry()).getDebitEntry() != null)) {
            debitEntry = bean.isForPendingInterest() ? ((SettlementInterestEntryBean) bean)
                    .getDebitEntry() : ((DebitEntry) bean.getInvoiceEntry()).getDebitEntry();
        }
        return debitEntry;
    }

    private static DebitEntry getOriginDebitEntryFromPenaltyTaxEntry(ISettlementInvoiceEntryBean bean) {
        DebitEntry debitEntry = null;
        if (bean.isForPaymentPenalty()
                || (bean.isForDebitEntry() && ((DebitEntry) bean.getInvoiceEntry()).getTreasuryEvent() != null
                        && ((DebitEntry) bean.getInvoiceEntry()).getTreasuryEvent() instanceof PaymentPenaltyTaxTreasuryEvent)) {
            debitEntry = bean.isForPaymentPenalty() ? ((PaymentPenaltyEntryBean) bean)
                    .getDebitEntry() : ((PaymentPenaltyTaxTreasuryEvent) ((DebitEntry) bean.getInvoiceEntry()).getTreasuryEvent())
                            .getOriginDebitEntry();
        }
        return debitEntry;
    }

    private static int compareDebitEntryDueDate(DebitEntry debitEntry1, DebitEntry debitEntry2) {
        return (debitEntry1.getDueDate().compareTo(debitEntry2.getDueDate()) * 10)
                + debitEntry1.getExternalId().compareTo(debitEntry2.getExternalId());
    }
}
