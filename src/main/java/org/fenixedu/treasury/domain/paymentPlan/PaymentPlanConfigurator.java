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

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
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
import org.fenixedu.treasury.dto.SettlementNoteBean.DebitEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean.InterestEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentBean;
import org.fenixedu.treasury.dto.PaymentPlans.InstallmentEntryBean;
import org.fenixedu.treasury.dto.PaymentPlans.PaymentPlanBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public abstract class PaymentPlanConfigurator extends PaymentPlanConfigurator_Base {

    private static final int MAX_LOOP = 10;
    public static final Comparator<ISettlementInvoiceEntryBean> COMPARE_SETTLEMENT_INVOICE_ENTRY_BEAN = (s1, s2) -> {
        if (s1.isForPendingDebitEntry() || s2.isForPendingDebitEntry()) {
            return s1.isForPendingDebitEntry() ? -1 : 1;
        }
        /**
         * Divida1
         * Taxa1
         * Juro1
         * Divida2
         * Taxa2
         * Juro2
         */
        String descriçãoS1 = s1.getDescription();
        String descriçãoS2 = s2.getDescription();

        DebitEntry debitEntryS1 = null;
        DebitEntry interestEntryS1 = null;
        DebitEntry penaltyTaxEntryS1 = null;

        if (s1.isForPendingInterest() || (s1.isForDebitEntry() && ((DebitEntry) s1.getInvoiceEntry()).getDebitEntry() != null)) {
            interestEntryS1 = s1.isForPendingInterest() ? ((InterestEntryBean) s1)
                    .getDebitEntry() : ((DebitEntry) s1.getInvoiceEntry()).getDebitEntry();
        } else if (s1.isForPaymentPenalty()
                || (s1.isForDebitEntry() && ((DebitEntry) s1.getInvoiceEntry()).getTreasuryEvent() != null
                        && ((DebitEntry) s1.getInvoiceEntry()).getTreasuryEvent() instanceof PaymentPenaltyTaxTreasuryEvent)) {
            penaltyTaxEntryS1 = s1.isForPaymentPenalty() ? ((PaymentPenaltyEntryBean) s1)
                    .getDebitEntry() : ((PaymentPenaltyTaxTreasuryEvent) ((DebitEntry) s1.getInvoiceEntry()).getTreasuryEvent())
                            .getOriginDebitEntry();
        } else {
            debitEntryS1 = (DebitEntry) s1.getInvoiceEntry();
        }

        DebitEntry debitEntryS2 = null;
        DebitEntry interestEntryS2 = null;
        DebitEntry penaltyTaxEntryS2 = null;

        if (s2.isForPendingInterest() || (s2.isForDebitEntry() && ((DebitEntry) s2.getInvoiceEntry()).getDebitEntry() != null)) {
            interestEntryS2 = s2.isForPendingInterest() ? ((InterestEntryBean) s2)
                    .getDebitEntry() : ((DebitEntry) s2.getInvoiceEntry()).getDebitEntry();
        } else if (s2.isForPaymentPenalty()
                || (s2.isForDebitEntry() && ((DebitEntry) s2.getInvoiceEntry()).getTreasuryEvent() != null
                        && ((DebitEntry) s2.getInvoiceEntry()).getTreasuryEvent() instanceof PaymentPenaltyTaxTreasuryEvent)) {
            penaltyTaxEntryS2 = s2.isForPaymentPenalty() ? ((PaymentPenaltyEntryBean) s2)
                    .getDebitEntry() : ((PaymentPenaltyTaxTreasuryEvent) ((DebitEntry) s2.getInvoiceEntry()).getTreasuryEvent())
                            .getOriginDebitEntry();
        } else {
            debitEntryS2 = (DebitEntry) s2.getInvoiceEntry();
        }

        if (debitEntryS1 != null) {
            if (debitEntryS2 != null) {
                return compareDebitEntryDueDate(debitEntryS1, debitEntryS2);
            } else {
                DebitEntry debitAux = interestEntryS2 != null ? interestEntryS2 : penaltyTaxEntryS2;
                return debitEntryS1 == debitAux ? -1 : compareDebitEntryDueDate(debitEntryS1, debitAux);
            }
        }

        if (interestEntryS1 != null) {
            if (interestEntryS2 != null) {
                return interestEntryS1 == interestEntryS2 ? (s1.isForPendingInterest() ? 1 : -1) : compareDebitEntryDueDate(
                        interestEntryS1, interestEntryS2);
            } else {
                DebitEntry debitAux = debitEntryS2 != null ? debitEntryS2 : penaltyTaxEntryS2;
                return interestEntryS1 == debitAux ? 1 : compareDebitEntryDueDate(interestEntryS1, debitAux);
            }

        }

        if (penaltyTaxEntryS1 != null) {
            if (penaltyTaxEntryS2 != null) {
                return compareDebitEntryDueDate(penaltyTaxEntryS1, penaltyTaxEntryS2);
            } else if (debitEntryS2 != null) {
                return penaltyTaxEntryS1 == debitEntryS2 ? 1 : compareDebitEntryDueDate(penaltyTaxEntryS1, debitEntryS2);
            } else {
                return penaltyTaxEntryS1 == interestEntryS2 ? -1 : compareDebitEntryDueDate(penaltyTaxEntryS1, interestEntryS2);
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

    private static int compareDebitEntryDueDate(DebitEntry debitEntry1, DebitEntry debitEntry2) {
        return (debitEntry1.getDueDate().compareTo(debitEntry2.getDueDate()) * 10)
                + debitEntry1.getExternalId().compareTo(debitEntry2.getExternalId());
    }

    protected PaymentPlanConfigurator(LocalizedString name, LocalizedString installmentDescriptionFormat,
            Boolean usePaymentPenalty, Boolean divideInterestsByInstallments, Boolean dividePaymentPenaltyInstallments,
            Product emolumentProduct, PaymentPlanNumberGenerator numberGenerator) {
        this();

        setName(name);
        setInstallmentDescriptionFormat(installmentDescriptionFormat);
        setUsePaymentPenalty(usePaymentPenalty);
        setDivideInterestsByAllInstallments(divideInterestsByInstallments);
        setDividePaymentPenaltyByAllInstallments(dividePaymentPenaltyInstallments);
        setEmolumentProduct(emolumentProduct);
        setNumberGenerators(numberGenerator);

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
    }

    public List<InstallmentBean> getInstallmentsBeansFor(PaymentPlanBean paymentPlanBean) {
        return getInstallmentsBeansFor(paymentPlanBean, null);
    }

    public List<InstallmentBean> getInstallmentsBeansFor(PaymentPlanBean paymentPlanBean, List<LocalDate> dates) {
        List<InstallmentBean> installments = createInstallments(paymentPlanBean, dates);
        paymentPlanBean.setSettlementInvoiceEntryBeans(paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                .filter(bean -> bean.isForDebitEntry() || bean.isForPendingDebitEntry()).collect(Collectors.toSet()));

        List<ISettlementInvoiceEntryBean> invoiceEntries = getInvoiceEntryBeans(paymentPlanBean, getPredicateToDebitEntry());
        BigDecimal amount = getSumAmountOf(invoiceEntries, installments, paymentPlanBean);
        BigDecimal installmentAmmount = getInstallmentAmount(amount, paymentPlanBean.getNbInstallments());
        fillInstallmentsWithInvoiceEntries(installments, installmentAmmount, amount, invoiceEntries, paymentPlanBean);

        if (!isDivideInterest() || !isDividePenaltyTax()) {
            int i = 0;
            while (diffFirstLastAmountGreaterOrEqualThanNbInstallmentsInCents(installments, paymentPlanBean) && i < MAX_LOOP) {
                installments = createInstallments(paymentPlanBean, dates);
                invoiceEntries = getInvoiceEntryBeans(paymentPlanBean, getPredicateToDebitEntry());
                amount = getSumAmountOf(invoiceEntries, installments, paymentPlanBean);
                installmentAmmount = getInstallmentAmount(amount, paymentPlanBean.getNbInstallments());
                fillInstallmentsWithInvoiceEntries(installments, installmentAmmount, amount, invoiceEntries, paymentPlanBean);
                i++;
            }
        }
        if (isDivideInterest()) {
            invoiceEntries = getInvoiceEntryBeans(paymentPlanBean, getPredicateToInterestEntry());
            amount = getSumAmountOf(invoiceEntries, installments, paymentPlanBean);
            installmentAmmount = getInstallmentAmount(amount, paymentPlanBean.getNbInstallments());
            fillInstallmentsWithInvoiceEntries(installments, installmentAmmount, amount, invoiceEntries, paymentPlanBean);
        }

        if (isDividePenaltyTax()) {
            invoiceEntries = getInvoiceEntryBeans(paymentPlanBean, getPredicateToPenaltyTaxEntry());
            amount = getSumAmountOf(invoiceEntries, installments, paymentPlanBean);
            installmentAmmount = getInstallmentAmount(amount, paymentPlanBean.getNbInstallments());
            fillInstallmentsWithInvoiceEntries(installments, installmentAmmount, amount, invoiceEntries, paymentPlanBean);
        }

        return installments;
    }

    protected void fillInstallmentsWithInvoiceEntries(List<InstallmentBean> result, BigDecimal installmentAmmount,
            BigDecimal restAmount, List<ISettlementInvoiceEntryBean> invoiceEntries, PaymentPlanBean paymentPlanBean) {

        if (invoiceEntries.isEmpty()) {
            return;
        }
        ISettlementInvoiceEntryBean bean = invoiceEntries.get(0);
        invoiceEntries.remove(0);

        for (InstallmentBean installmentBean : result) {
            if (installmentBean == result.get(result.size() - 1)) {
                installmentAmmount = restAmount;
            }
            BigDecimal restInstallmentAmmount = installmentAmmount;
            while (TreasuryConstants.isPositive(restInstallmentAmmount)) {
                BigDecimal installmentEntryAmount = getRestAmountOf(bean, result, paymentPlanBean);
                if (!TreasuryConstants.isPositive(installmentEntryAmount)) {
                    bean = invoiceEntries.isEmpty() ? null : invoiceEntries.get(0);
                    if (!invoiceEntries.isEmpty()) {
                        invoiceEntries.remove(0);
                    }
                    continue;
                }
                if (TreasuryConstants.isGreaterThan(installmentEntryAmount, restInstallmentAmmount)) {
                    installmentEntryAmount = restInstallmentAmmount;
                }
                installmentBean.addInstallmentEntries(new InstallmentEntryBean(bean, installmentEntryAmount));

                restInstallmentAmmount = restInstallmentAmmount.subtract(installmentEntryAmount);

                if (bean.isForDebitEntry() && !TreasuryConstants.isPositive(getRestAmountOf(bean, result, paymentPlanBean))) {
                    InterestEntryBean interestEntryBean =
                            updateRelatedInterests((DebitEntryBean) bean, paymentPlanBean, result, installmentBean.getDueDate());
                    if (interestEntryBean != null && !isDivideInterest()) {
                        if (!invoiceEntries.contains(interestEntryBean)) {
                            invoiceEntries.add(0, interestEntryBean);
                        }
                        restAmount = getSumAmountOf(invoiceEntries, result, paymentPlanBean).add(installmentEntryAmount);
                    }
                    if (Boolean.TRUE.equals(getUsePaymentPenalty())) {
                        PaymentPenaltyEntryBean paymentPenaltyEntryBean =
                                updateRelatedPenaltyTax((DebitEntryBean) bean, paymentPlanBean, installmentBean.getDueDate());
                        if (paymentPenaltyEntryBean != null && !isDividePenaltyTax()) {
                            if (!invoiceEntries.contains(paymentPenaltyEntryBean)) {
                                invoiceEntries.add(0, paymentPenaltyEntryBean);
                            }
                            restAmount = getSumAmountOf(invoiceEntries, result, paymentPlanBean).add(installmentEntryAmount);
                        }
                    }
                    if (installmentBean == result.get(result.size() - 1)
                            && !TreasuryConstants.isEqual(restAmount, restInstallmentAmmount)) {
                        restInstallmentAmmount = getSumAmountOf(invoiceEntries, result, paymentPlanBean);
                    }

                }
            }
            restAmount = restAmount.subtract(installmentAmmount);
        }
    }

    protected PaymentPenaltyEntryBean updateRelatedPenaltyTax(DebitEntryBean bean, PaymentPlanBean paymentPlanBean,
            LocalDate dueDate) {
        PaymentPenaltyEntryBean calculatePaymentPenaltyTax = PaymentPenaltyTaxTreasuryEvent
                .calculatePaymentPenaltyTax(bean.getDebitEntry(), dueDate, paymentPlanBean.getCreationDate());
        if (calculatePaymentPenaltyTax != null) {
            paymentPlanBean.addSettlementInvoiceEntryBean(calculatePaymentPenaltyTax);
        }
        return calculatePaymentPenaltyTax;
    }

    protected InterestEntryBean updateRelatedInterests(DebitEntryBean bean, PaymentPlanBean paymentPlanBean,
            List<InstallmentBean> result, LocalDate lastInstallmentDueDate) {

        InterestEntryBean interestEntryBean =
                (InterestEntryBean) paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                        .filter(interestbean -> interestbean.isForPendingInterest()
                                && ((InterestEntryBean) interestbean).getDebitEntry() == bean.getInvoiceEntry())
                        .findFirst().orElse(null);

        BigDecimal interestBeforePaymentPlan = bean.getSettledAmount().subtract(bean.getEntryOpenAmount());

        if (TreasuryConstants.isPositive(interestBeforePaymentPlan)) {
            if (interestEntryBean == null) {
                InterestRateBean interestRateBean = new InterestRateBean();
                interestRateBean.setDescription(treasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE,
                        "label.InterestRateBean.interest.designation", bean.getDebitEntry().getDescription()));

                interestRateBean.setInterestAmount(interestBeforePaymentPlan);
                interestEntryBean = new InterestEntryBean(bean.getDebitEntry(), interestRateBean);
                paymentPlanBean.addSettlementInvoiceEntryBean(interestEntryBean);
            } else {
                interestEntryBean.getInterest().setInterestAmount(interestBeforePaymentPlan);
            }
            return interestEntryBean;
        } else if (interestEntryBean != null) {
            paymentPlanBean.removeSettlementInvoiceEntryBean(interestEntryBean);
        }
        return null;

    }

    protected BigDecimal getRestAmountOf(ISettlementInvoiceEntryBean bean, List<InstallmentBean> result,
            PaymentPlanBean paymentPlanBean) {
        BigDecimal total = bean.isForDebitEntry() ? bean.getEntryOpenAmount() : bean.getSettledAmount();

        BigDecimal used = result.stream().flatMap(inst -> inst.getInstallmentEntries().stream())
                .filter(entry -> entry.getInvoiceEntry() == bean).map(entry -> entry.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return total.subtract(used);
    }

    public boolean isApplyInterest() {
        return true;
    }

    public boolean isInterestBlocked() {
        return false;
    };

    public boolean isDivideInterest() {
        return Boolean.TRUE.equals(getDivideInterestsByAllInstallments());
    }

    public boolean isDividePenaltyTax() {
        return Boolean.TRUE.equals(getDividePaymentPenaltyByAllInstallments());
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

    /*
     * SERVICES
     */

    public static Stream<PaymentPlanConfigurator> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPlanConfiguratorsSet().stream();
    }

    public static Stream<PaymentPlanConfigurator> findActives() {
        return findAll().filter(p -> Boolean.TRUE.equals(p.getActive()));
    }

    protected LocalizedString getInstallmentDescription(int installmentNumber, String paymentPlanId) {
        Map<String, String> values = new HashMap<>();
        values.put("installmentNumber", "" + installmentNumber);
        values.put("paymentPlanId", paymentPlanId);

        LocalizedString ls = new LocalizedString();
        for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            ls = ls.with(locale, StrSubstitutor.replace(getInstallmentDescriptionFormat().getContent(locale), values));
        }

        return ls;
    }

    protected int getPlusDaysForInstallment(PaymentPlanBean paymentPlanBean, int i) {
        double daysBeweenInstallments = paymentPlanBean.getNbInstallments() == 1 ? 0 : Days
                .daysBetween(paymentPlanBean.getStartDate(), paymentPlanBean.getEndDate()).getDays()
                / (paymentPlanBean.getNbInstallments() - 1.00);
        return Double.valueOf(((i) * daysBeweenInstallments)).intValue();
    }

    protected BigDecimal getInstallmentAmount(BigDecimal totalAmount, int nbInstallments) {
        return Currency.getValueWithScale(TreasuryConstants.divide(totalAmount, new BigDecimal(nbInstallments)));
    }

    protected List<InstallmentBean> createInstallments(PaymentPlanBean paymentPlanBean, List<LocalDate> dates) {
        if (dates == null) {
            dates = getDates(paymentPlanBean);
        }

        List<InstallmentBean> result = new ArrayList<InstallmentBean>();
        for (int i = 1; i <= paymentPlanBean.getNbInstallments(); i++) {
            LocalizedString installmentDescription =
                    paymentPlanBean.getPaymentPlanConfigurator().getInstallmentDescription(i, paymentPlanBean.getPaymentPlanId());
            result.add(new InstallmentBean(dates.get(i - 1), installmentDescription));
        }
        return result;

    }

    protected List<LocalDate> getDates(PaymentPlanBean paymentPlanBean) {
        List<LocalDate> result = new ArrayList<>();
        LocalDate installmentDueDate = paymentPlanBean.getStartDate();
        for (int i = 1; i <= paymentPlanBean.getNbInstallments(); i++) {
            if (i == paymentPlanBean.getNbInstallments()) {
                installmentDueDate = paymentPlanBean.getEndDate();
            }
            result.add(installmentDueDate);
            installmentDueDate = paymentPlanBean.getStartDate().plusDays(getPlusDaysForInstallment(paymentPlanBean, i));
        }
        return result;
    }

    protected List<ISettlementInvoiceEntryBean> getInvoiceEntryBeans(PaymentPlanBean paymentPlanBean,
            Predicate<? super ISettlementInvoiceEntryBean> predicate) {
        return paymentPlanBean.getSettlementInvoiceEntryBeans().stream().filter(predicate)
                .sorted(COMPARE_SETTLEMENT_INVOICE_ENTRY_BEAN).collect(Collectors.toList());
    }

    protected BigDecimal getSumAmountOf(List<ISettlementInvoiceEntryBean> invoiceEntries, List<InstallmentBean> installments,
            PaymentPlanBean paymentPlanBean) {
        return invoiceEntries.stream().map(bean -> getRestAmountOf(bean, installments, paymentPlanBean)).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    protected Predicate<? super ISettlementInvoiceEntryBean> getPredicateToDebitEntry() {
        return bean -> {
            if (isDivideInterest()) {
                if (bean.isForPendingInterest()) {
                    return false;
                }
                if (bean.isForDebitEntry() && ((DebitEntry) bean.getInvoiceEntry()).getDebitEntry() != null) {
                    return false;
                }
            }
            if (isDividePenaltyTax()) {
                if (bean.isForPaymentPenalty()) {
                    return false;
                }
                if (bean.isForDebitEntry() && ((DebitEntry) bean.getInvoiceEntry()).getTreasuryEvent() != null
                        && ((DebitEntry) bean.getInvoiceEntry()).getTreasuryEvent() instanceof PaymentPenaltyTaxTreasuryEvent) {
                    return false;
                }
            }
            return true;
        };
    }

    protected Predicate<? super ISettlementInvoiceEntryBean> getPredicateToInterestEntry() {
        return bean -> {
            if (isDivideInterest()) {
                if (bean.isForPendingInterest()) {
                    return true;
                }
                if (bean.isForDebitEntry() && ((DebitEntry) bean.getInvoiceEntry()).getDebitEntry() != null) {
                    return true;
                }
            }
            return false;
        };
    }

    protected Predicate<? super ISettlementInvoiceEntryBean> getPredicateToPenaltyTaxEntry() {
        return bean -> {
            if (isDividePenaltyTax()) {
                if (bean.isForPaymentPenalty()) {
                    return true;
                }
                if (bean.isForDebitEntry() && ((DebitEntry) bean.getInvoiceEntry()).getTreasuryEvent() != null
                        && ((DebitEntry) bean.getInvoiceEntry()).getTreasuryEvent() instanceof PaymentPenaltyTaxTreasuryEvent) {
                    return true;
                }
            }
            return false;
        };
    }

    protected boolean diffFirstLastAmountGreaterOrEqualThanNbInstallmentsInCents(List<InstallmentBean> installments,
            PaymentPlanBean paymentPlanBean) {
        BigDecimal nbInstallmentsInCents = Currency.getValueWithScale(
                TreasuryConstants.divide(new BigDecimal(paymentPlanBean.getNbInstallments()), TreasuryConstants.HUNDRED_PERCENT));
        BigDecimal amountFirstInstallment = installments.get(0).getInstallmentAmount();
        BigDecimal amountLastInstallment = installments.get(paymentPlanBean.getNbInstallments() - 1).getInstallmentAmount();
        BigDecimal diffFirstLast = amountFirstInstallment.subtract(amountLastInstallment).abs();
        return TreasuryConstants.isGreaterOrEqualThan(diffFirstLast, nbInstallmentsInCents);
    }
}
