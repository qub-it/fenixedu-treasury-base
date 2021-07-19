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
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
import org.fenixedu.treasury.dto.SettlementDebitEntryBean;
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

public abstract class PaymentPlanConfigurator extends PaymentPlanConfigurator_Base {

    private static final int MAX_LOOP = 10;

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

    public abstract boolean isApplyInterest();

    public abstract boolean isInterestBlocked();

    public abstract boolean canChangeInstallmentsAmount();

    protected abstract LocalDate getDateToUseToPenaltyTaxCalculation(LocalDate creationDate, LocalDate dueDate);

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

        //Remove PaymentPenaltyTax and Interest beans, created in the algorithm
        resetSettlementInvoiceEntryBeansForChoosenDebitEntries(paymentPlanBean);

        int loopNumber = 0;
        PaymentPlanInstallmentCreationBean installmentsCreationBean = null;

        do {
            //@formatter:off
            /**
             * PaymentPlanBean -> contains selected debitEntries and number of installments
             * FixedDates -> list of fixed dates for installments chosen manually, can be null to be calculated
             * FixedAmount -> List of installments amount chosen manually, can be null to be calculated
             *
             * PaymentPlanInstallmentCreationBean -> saves: PaymentPlanBean, list of installments, list of installments amount and
             * list of invoice entries to be treated
             *
             * EG:
             * For this example is used InterestDistribution = AFTER_DEBIT_ENTRY and not exists PaymentPenaltyTax and Interest are blocked at payment plan request date
             *
             * loopNumber := 0
             *
             * Payment contains : [DebitEntry1 := 100.00, DebitEntry2 := 50], number of installments := 5
             * Creating PaymentPlanInstallmentCreationBean with state:
             * -- installmentsMaxAmount := [30,30,30,30,30], because (100 + 50) / 5 = 30
             * -- installments := [[description1],[description2],[description3],[description4],[description5]]
             *
             * Calling fillInstallmentsWithInvoiceEntries:
             * - Calculate PaymentPenaltyTax and Interest for each debit entry (EG: Interests for debitEntry1 := 5, interest for
             * debitEntry2 := 2.54)
             * - Add amount of PaymentPenaltyTax and Interest in last installment amount (EG: installmentsMaxAmount :=
             * [30,30,30,30,37.54]
             * - Distribute debit entries, PaymentPenaltyTax and Interests in the list of installments in
             * PaymentPlanInstallmentCreationBean whit state:
             * We will try to fit the maximum amount of installment
             * -- installmentsMaxAmount := [30,30,30,30,37.54]
             * -- installments := [[description1,[DebitEntry1 :=30]],   -> rest of DE1 := 100-30 =70 -> Max amount was 30
             *                     [description2,[DebitEntry1 :=30]],   -> rest of DE1 := 70-30 =40
             *                     [description3,[DebitEntry1 :=30]],   -> rest of DE1 := 40-30 =10
             *                     [description4,[DebitEntry1 :=10,InterestDE1:=5,DebitEntry2 :=15]],   -> rest of DE1 := 10-10 =0, rest of InterestDE1 := 5-5 =0, rest of DE2 := 50 - 15 = 35
             *                     [description5,[DebitEntry2 :=35,InterestDE2:=2.54]]]   -> rest of DE2 := 35 - 35 = 0, rest of InterestDE1 := 2.54-2.54 = 0 -> Max amount was 37.54
             * Check ending condition (EG: condition fails, because diffFirstLastAmountGreaterOrEqualThanNbInstallmentsInCents fails -> ABS(37.54 - 30) > 0.05 )
             *
             * loopNumber := 1
             *
             * Payment contains : [DebitEntry1 := 100.00, DebitEntry2 := 50, InterestDE1 := 5, InterestDE2 := 2.54], number of installments := 5
             * Creating PaymentPlanInstallmentCreationBean with state:
             * -- installmentsMaxAmount := [31.5,31.5,31.5,31.5,31.54], because (100 + 50 + 5 + 2.54) / 5 = 31.5 (+0.04)
             * -- installments := [[description1],[description2],[description3],[description4],[description5]] -> reset state for new calculation step
             * -- Payment contains : [DebitEntry1 := 100.00, DebitEntry2 := 50], number of installments := 5 -> reset state for new calculation step
             *
             * Calling fillInstallmentsWithInvoiceEntries:
             * - Calculate PaymentPenaltyTax and Interest for each debit entry (EG: Interests for debitEntry1 := 5, interest for
             * debitEntry2 := 2.54)
             * - Add amount of PaymentPenaltyTax and Interest in last installment amount (EG: installmentsMaxAmount :=
             * [31.5,31.5,31.5,31.5,31.54]
             * - Distribute debit entries, PaymentPenaltyTax and Interests in the list of installments in
             * PaymentPlanInstallmentCreationBean whit state:
             * We will try to fit the maximum amount of installment
             * -- installmentsMaxAmount := [31.5,31.5,31.5,31.5,31.54]
             * -- installments := [[description1,[DebitEntry1 :=31.5]],   -> rest of DE1 := 100-31.5 =68.5 -> Max amount was 31.5
             *                     [description2,[DebitEntry1 :=31.5]],   -> rest of DE1 := 68.5-31.5 =37
             *                     [description3,[DebitEntry1 :=31.5]],   -> rest of DE1 := 37-31.5 = 5.5
             *                     [description4,[DebitEntry1 :=5.5,InterestDE1:=5,DebitEntry2 :=21]],   -> rest of DE1 := 5.5-5.5 =0, rest of InterestDE1 := 5-5 =0, rest of DE2 := 50 - 21 = 29
             *                     [description5,[DebitEntry2 :=29,InterestDE2:=2.54]]]   -> rest of DE2 := 29 - 29 = 0, rest of InterestDE1 := 2.54-2.54 = 0 -> Max amount was 31.54        *
             * Check ending condition (EG: condition success, because diffFirstLastAmountGreaterOrEqualThanNbInstallmentsInCents success -> ABS(31.54 - 31.5) < 0.05 )
             */
            //@formatter:on

            //Reset PaymentPlanInstallmentCreationBean
            installmentsCreationBean = new PaymentPlanInstallmentCreationBean(paymentPlanBean, fixedDates, fixedAmountList);

            // fill Installments with invoice entries
            fillInstallmentsWithInvoiceEntries(installmentsCreationBean);

            loopNumber++;
        } while ((fixedAmountList == null || fixedAmountList.isEmpty()) && loopNumber < MAX_LOOP
                && installmentsCreationBean.diffFirstLastAmountGreaterOrEqualThanNbInstallmentsInCents());

        /**
         * And extra interest warning to paymentPlanBean
         * EG: Exists a Debit entry than is a interest with 0.5 and for payment plan the calculated interest for that debit entry
         * are 0.4 will be created a warning with that debit entry and 0.1 of extra interest amount
         */
        installmentsCreationBean.fillExtraInterestWarning();

        return installmentsCreationBean.getInstallments();
    }

    private void fillInstallmentsWithInvoiceEntries(PaymentPlanInstallmentCreationBean installmentsCreationBean) {
        ISettlementInvoiceEntryBean currentInvoiceEntryBean = installmentsCreationBean.getNextInvoiceEntryBean();
        for (int i = 0; i < installmentsCreationBean.getInstallments().size(); i++) {
            InstallmentBean currentInstallmentBean = installmentsCreationBean.getInstallmentBean(i);
            BigDecimal installmentAmount = installmentsCreationBean.getInstallmentAmount(i);
            BigDecimal restInstallmentMaxAmount = installmentAmount;

            while (currentInvoiceEntryBean != null && TreasuryConstants.isPositive(restInstallmentMaxAmount)) {
                if (Boolean.TRUE.equals(getUsePaymentPenalty() && isDebitEntry(currentInvoiceEntryBean))) {
                    currentInvoiceEntryBean = processPaymentPenaltyEntryBean(installmentsCreationBean, currentInvoiceEntryBean);
                }

                BigDecimal installmentEntryAmount =
                        getRestAmountOfBeanInPaymentPlan(currentInvoiceEntryBean, installmentsCreationBean);
                if (!TreasuryConstants.isPositive(installmentEntryAmount)) {
                    /**
                     * IF rest amount of current bean is less or equal than zero
                     * THEN: change the bean no next bean and remove it from list
                     */
                    currentInvoiceEntryBean = installmentsCreationBean.getNextInvoiceEntryBean();
                    continue;
                }

                boolean isLastInstallmentOfCurrInvoiceEntryBean = true;
                if (TreasuryConstants.isGreaterThan(installmentEntryAmount, restInstallmentMaxAmount)
                        && !installmentsCreationBean.isLastInstallmentOfPaymentPLan(currentInstallmentBean)) {
                    /**
                     * IF installmentEntryAmount greater than restInstallmentAmmount
                     * THEN:
                     * limit installmentEntryAmount to restInstallmentAmount
                     * set isLastInstallmentOfCurrInvoiceEntryBean to false;
                     */
                    installmentEntryAmount = restInstallmentMaxAmount;
                    isLastInstallmentOfCurrInvoiceEntryBean = false;
                }

                if (isApplyInterest() && getInterestDistribution().isByInstallmentEntryAmount()
                        && isDebitEntry(currentInvoiceEntryBean)) {
                    // BEAN IS DEBIT ENTRY AND INTEREST IS AFTER DEBIT ENTRY

                    restInstallmentMaxAmount = processInterestInstallmentEntryByInstallmentEntryAmount(installmentsCreationBean,
                            currentInstallmentBean, currentInvoiceEntryBean, installmentEntryAmount, restInstallmentMaxAmount,
                            isLastInstallmentOfCurrInvoiceEntryBean);
                }

                //CREATE InstallmentEntryBean for currentInvoiceEntryBean
                createInstallmentEntryBean(currentInstallmentBean, currentInvoiceEntryBean, installmentEntryAmount);
                restInstallmentMaxAmount = restInstallmentMaxAmount.subtract(installmentEntryAmount);

                if (isApplyInterest() && getInterestDistribution().isAfterDebitEntry() && isDebitEntry(currentInvoiceEntryBean)
                        && !TreasuryConstants.isPositive(
                                getRestAmountOfBeanInPaymentPlan(currentInvoiceEntryBean, installmentsCreationBean))) {
                    // BEAN IS DEBIT ENTRY AND INTEREST IS AFTER DEBIT ENTRY
                    restInstallmentMaxAmount = createInterestInstallmentEntryAfterDebitEntry(installmentsCreationBean,
                            currentInvoiceEntryBean, currentInstallmentBean, restInstallmentMaxAmount);
                }

            }

        }

    }

    private BigDecimal createInterestInstallmentEntryAfterDebitEntry(PaymentPlanInstallmentCreationBean installmentsCreationBean,
            ISettlementInvoiceEntryBean currentInvoiceEntryBean, InstallmentBean currentInstallmentBean,
            BigDecimal restInstallmentAmount) {
        BigDecimal interestEntryAmout = getInterestAmountToPaymentPlan(installmentsCreationBean, currentInvoiceEntryBean);

        if (TreasuryConstants.isPositive(interestEntryAmout)) {
            processInterestInstallmentEntryBeanInInstallmentBean(installmentsCreationBean, currentInvoiceEntryBean,
                    currentInstallmentBean, restInstallmentAmount, interestEntryAmout, null);
            restInstallmentAmount = restInstallmentAmount.subtract(Currency.getValueWithScale(interestEntryAmout));
        }
        return restInstallmentAmount;
    }

    private BigDecimal processInterestInstallmentEntryByInstallmentEntryAmount(
            PaymentPlanInstallmentCreationBean installmentsCreationBean, InstallmentBean currentInstallmentBean,
            ISettlementInvoiceEntryBean currentInvoiceEntryBean, BigDecimal installmentEntryAmount,
            BigDecimal restInstallmentAmount, boolean isLastInstallmentOfCurrInvoiceEntryBean) {
        /**
         * IF current bean is debit entry and interestDistribuition is by installment entry amount
         * THEN:
         * 1 - calculate installmentEntryAmount to this and respective interest are equal at installmentEntryAmount
         * 2 - update isLastInstallmentOfCurrInvoiceEntryBean
         * 3 - calculate debit entry interest amount for amount previous calculated.
         * 4 - set interest amount in payment plan and current installment
         * 5 - update restInstallmentAmount
         * [1 and 2 exists to all installments except last one]
         */

        if (!installmentsCreationBean.isLastInstallmentOfPaymentPLan(currentInstallmentBean)) {
            BigDecimal backupInstallmentEntryAmount = installmentEntryAmount;
            installmentEntryAmount = getDebtAmountToInstallmentEntryAmount(installmentsCreationBean, currentInstallmentBean,
                    currentInvoiceEntryBean, installmentEntryAmount, restInstallmentAmount,
                    isLastInstallmentOfCurrInvoiceEntryBean);
            isLastInstallmentOfCurrInvoiceEntryBean = isLastInstallmentOfCurrInvoiceEntryBean
                    && TreasuryConstants.isEqual(installmentEntryAmount, backupInstallmentEntryAmount);
        }

        BigDecimal interestAmout = getInterestAmountOfCurrentInvoiceEntryBeanToInstallment(installmentsCreationBean,
                currentInvoiceEntryBean, installmentEntryAmount, currentInvoiceEntryBean.getDueDate(),
                currentInstallmentBean.getDueDate(), isLastInstallmentOfCurrInvoiceEntryBean);

        if (TreasuryConstants.isPositive(interestAmout)) {
            processInterestInstallmentEntryBeanInInstallmentBean(installmentsCreationBean, currentInvoiceEntryBean,
                    currentInstallmentBean, restInstallmentAmount, interestAmout, installmentEntryAmount);
            return restInstallmentAmount.subtract(Currency.getValueWithScale(interestAmout));
        }
        return restInstallmentAmount;
    }

    private ISettlementInvoiceEntryBean processPaymentPenaltyEntryBean(
            PaymentPlanInstallmentCreationBean installmentsCreationBean, ISettlementInvoiceEntryBean currentInvoiceEntryBean) {
        PaymentPenaltyEntryBean paymentPenaltyEntryBean =
                createIfNotExistsPaymentPenaltyEntryBean((SettlementDebitEntryBean) currentInvoiceEntryBean, installmentsCreationBean);

        if (paymentPenaltyEntryBean != null) {
            /**
             * paymentPenaltyEntryBean was created
             *
             * update last installment amount
             *
             * paymentPenaltyEntryBean = penaltyTax1
             * currentInvoiceEntryBean = debitEntry1
             * invoice entries to be treated list = debitEntry2
             *
             * IF paymentPenaltyDistribuition is before debit entry
             * THEN:
             * currentInvoiceEntryBean = penaltyTax1
             * invoice entries to be treated list = debitEntry1, debitEntry2
             * ELSE:
             * currentInvoiceEntryBean = debitEntry1
             * invoice entries to be treated list = penaltyTax1, debitEntry2
             */

            installmentsCreationBean.addAmountToLastInstallmentMaxAmount(paymentPenaltyEntryBean.getSettledAmount());
            if (getPaymentPenaltyDistribution().isBeforeDebitEntry()) {
                // tax < debt then add debt to list and continue with tax
                installmentsCreationBean.addInvoiceEntryBeanToBeTreatedAndSort(currentInvoiceEntryBean);
                return paymentPenaltyEntryBean;
            }

            if (getPaymentPenaltyDistribution().isAfterDebitEntry()) {
                // debt < tax then add tax to be dealt with next
                installmentsCreationBean.addInvoiceEntryBeanToBeTreatedAndSort(paymentPenaltyEntryBean);
                return currentInvoiceEntryBean;
            }
        }
        return currentInvoiceEntryBean;
    }

    private void processInterestInstallmentEntryBeanInInstallmentBean(PaymentPlanInstallmentCreationBean installmentsCreationBean,
            ISettlementInvoiceEntryBean currentInvoiceEntryBean, InstallmentBean currentInstallmentBean,
            BigDecimal restInstallmentAmount, BigDecimal interestAmout, BigDecimal invoiceInstallmentEntryAmount) {
        /**
         * Calculate interestInstallmentEntryAmount
         * Calculate totalInstallment IF isInstallmentInterest THEN installmentEntryAmount + interestInstallmentEntryAmount ELSE
         * interestInstallmentEntryAmount
         *
         * limit interestInstallmentEntryAmount to restInstallmentAmount if is greater and is not in last installment
         *
         * createInterestInstallmentEntryBean
         *
         * update last installment amount
         *
         */

        BigDecimal interestInstallmentEntryAmount = Currency.getValueWithScale(interestAmout);
        BigDecimal totalInstallmentAmount = invoiceInstallmentEntryAmount != null ? invoiceInstallmentEntryAmount
                .add(interestInstallmentEntryAmount) : interestInstallmentEntryAmount;

        if (TreasuryConstants.isGreaterThan(totalInstallmentAmount, restInstallmentAmount)
                && !installmentsCreationBean.isLastInstallmentOfPaymentPLan(currentInstallmentBean)) {
            interestInstallmentEntryAmount = invoiceInstallmentEntryAmount != null ? restInstallmentAmount
                    .subtract(invoiceInstallmentEntryAmount) : restInstallmentAmount;
        }

        BigDecimal interestAmountToAddAtLastInstallment =
                createInterestInstallmentEntryBeanInInstallmentBean(installmentsCreationBean, currentInvoiceEntryBean,
                        currentInstallmentBean, interestInstallmentEntryAmount, Currency.getValueWithScale(interestAmout));

        installmentsCreationBean.addAmountToLastInstallmentMaxAmount(interestAmountToAddAtLastInstallment);
    }

    private boolean isDebitEntry(ISettlementInvoiceEntryBean currentInvoiceEntryBean) {
        return currentInvoiceEntryBean.isForDebitEntry()
                && ((DebitEntry) currentInvoiceEntryBean.getInvoiceEntry()).getDebitEntry() == null
                && !(((DebitEntry) currentInvoiceEntryBean.getInvoiceEntry())
                        .getTreasuryEvent() instanceof PaymentPenaltyTaxTreasuryEvent);
    }

    private InstallmentEntryBean createInstallmentEntryBean(InstallmentBean currentInstallmentBean,
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

    private PaymentPenaltyEntryBean createIfNotExistsPaymentPenaltyEntryBean(SettlementDebitEntryBean currInvoiceEntryBean,
            PaymentPlanInstallmentCreationBean installmentsCreationBean) {

        Optional<ISettlementInvoiceEntryBean> paymentPenaltyEntryOptional =
                installmentsCreationBean.getPaymentPenaltyEntryFormPaymentPlan(currInvoiceEntryBean.getDebitEntry());

        if (paymentPenaltyEntryOptional.isPresent()) {
            //is treated
            return null;
        }

        PaymentPenaltyEntryBean paymentPenaltyEntryBean = PaymentPenaltyTaxTreasuryEvent.calculatePaymentPenaltyTax(
                currInvoiceEntryBean.getDebitEntry(),
                getDateToUseToPenaltyTaxCalculation(installmentsCreationBean.getRequestDate(), currInvoiceEntryBean.getDueDate()),
                installmentsCreationBean.getRequestDate());
        if (paymentPenaltyEntryBean != null) {
            installmentsCreationBean.addPaymentPlanSettlementInvoiceEntryBean(paymentPenaltyEntryBean);
        }
        return paymentPenaltyEntryBean;
    }

    private BigDecimal getRestAmountOfBeanInPaymentPlan(ISettlementInvoiceEntryBean currInvoiceEntryBean,
            PaymentPlanInstallmentCreationBean installmentsCreationBean) {
        BigDecimal total = currInvoiceEntryBean.isForDebitEntry() ? currInvoiceEntryBean
                .getEntryOpenAmount() : currInvoiceEntryBean.getSettledAmount();

        Stream<InstallmentEntryBean> installmentEntryBeansWithInvoiceEntryBean =
                installmentsCreationBean.getInstallmentEntryBeansWithInvoiceEntryBean(currInvoiceEntryBean);

        BigDecimal used = installmentEntryBeansWithInvoiceEntryBean.map(entry -> entry.getAmount()).reduce(BigDecimal.ZERO,
                BigDecimal::add);

        return total.subtract(used);
    }

    private BigDecimal getDebtAmountToInstallmentEntryAmount(PaymentPlanInstallmentCreationBean installmentsCreationBean,
            InstallmentBean currentInstallmentBean, ISettlementInvoiceEntryBean currentInvoiceEntryBean,
            BigDecimal installmentEntryAmount, BigDecimal currentInstallmentAmount, boolean isLastinstallment) {
        /**
         * Start with current invoice entry bean amount, respective interest and subTotal = sum of each one
         *
         * IF subTotal is greater than currentInstallmentAmount and calculated interest are positive
         * THEN:
         * create a list of subTotals
         * Do:(inside then block)
         * update currentInvoiceEntryAmount with difference between subTotal and installment amount to converge to ideal current
         * invoice entry Amount
         * calculate interest amount of new currentInvoiceEntryAmount and respective subTotal
         * WHILE: subTotal and currentInstallmentAmount are not equals OR a same subTotal exists on list to avoid infinite loop
         *
         * FINALY:
         * Return round amount of the ideal current invoice entry Amount
         */

        BigDecimal currentInvoiceEntryBeanAmount = installmentEntryAmount;
        BigDecimal calculatedInterestAmount = getInterestAmountOfCurrentInvoiceEntryBeanToInstallment(installmentsCreationBean,
                currentInvoiceEntryBean, currentInvoiceEntryBeanAmount, currentInvoiceEntryBean.getDueDate(),
                currentInstallmentBean.getDueDate(), isLastinstallment);

        BigDecimal subTotal = Currency.getValueWithScale(currentInvoiceEntryBeanAmount)
                .add(Currency.getValueWithScale(calculatedInterestAmount));

        if (TreasuryConstants.isGreaterThan(subTotal, currentInstallmentAmount)
                && TreasuryConstants.isPositive(calculatedInterestAmount)) {
            List<BigDecimal> subTotalList = new ArrayList<>();
            do {
                //update currentInvoiceEntryAmount with difference between subTotal and installment amount to converge to ideal current
                currentInvoiceEntryBeanAmount =
                        currentInvoiceEntryBeanAmount.subtract(subTotal.subtract(currentInstallmentAmount));

                calculatedInterestAmount = getInterestAmountOfCurrentInvoiceEntryBeanToInstallment(installmentsCreationBean,
                        currentInvoiceEntryBean, currentInvoiceEntryBeanAmount, currentInvoiceEntryBean.getDueDate(),
                        currentInstallmentBean.getDueDate(), false);

                subTotal = Currency.getValueWithScale(currentInvoiceEntryBeanAmount)
                        .add(Currency.getValueWithScale(calculatedInterestAmount));
                if (subTotalList.contains(subTotal)
                        && TreasuryConstants.isGreaterOrEqualThan(subTotal, currentInstallmentAmount)) {
                    break;
                }
                subTotalList.add(subTotal);
            } while (!TreasuryConstants.isEqual(subTotal, currentInstallmentAmount));
        }
        return Currency.getValueWithScale(currentInvoiceEntryBeanAmount);
    }

    protected BigDecimal getInterestAmountOfCurrentInvoiceEntryBeanToInstallment(
            PaymentPlanInstallmentCreationBean installmentsCreationBean, ISettlementInvoiceEntryBean currentInvoiceEntryBean,
            BigDecimal amount, LocalDate fromDate, LocalDate toDate, boolean isLastInstallmentOfCurrInvoiceEntryBean) {

        return getInterestAmountOfCurrentInvoiceEntryBeanToInstallmentBeforePlan(installmentsCreationBean,
                currentInvoiceEntryBean, amount, fromDate, toDate, isLastInstallmentOfCurrInvoiceEntryBean);
    }

    private BigDecimal getInterestAmountOfCurrentInvoiceEntryBeanToInstallmentBeforePlan(
            PaymentPlanInstallmentCreationBean installmentsCreationBean, ISettlementInvoiceEntryBean currentInvoiceEntryBean,
            BigDecimal amount, LocalDate fromDate, LocalDate toDate, boolean isLastInstallmentOfCurrInvoiceEntryBean) {

        if (!currentInvoiceEntryBean.isForDebitEntry()
                || !((DebitEntry) currentInvoiceEntryBean.getInvoiceEntry()).isApplyInterests()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalInterestAmountBeforePaymentPlan =
                getTotalInterestAmountOfInvoiceEntryBeforePaymentPlan(installmentsCreationBean, currentInvoiceEntryBean);

        if (isLastInstallmentOfCurrInvoiceEntryBean) {
            /**
             * calculate total installment amount of all previous installments where the invoice entry is present
             *
             * return totalInterestAmountBeforePaymentPlan - totalPreviousInstallmentsAmount
             * to avoid loose money with rounding
             */
            BigDecimal totalAmountOfCurrentInvoiceEntryInPreviousInstallments = BigDecimal.ZERO;
            List<InstallmentBean> listOfInstallments = installmentsCreationBean
                    .getInstallmentBeansWithInvoiceEntryBean(currentInvoiceEntryBean).collect(Collectors.toList());
            for (InstallmentBean installment : listOfInstallments) {
                if (installment.getDueDate().equals(toDate)) {
                    continue;
                }
                InstallmentEntryBean installmentEntryBean = installment.getInstallmentEntries().stream()
                        .filter(entryBean -> entryBean.getInvoiceEntry() == currentInvoiceEntryBean).findFirst().get();

                BigDecimal interestAmountOfCurrentInvoiceEntryBeanToInstallmentBeforePlan =
                        getInterestAmountOfCurrentInvoiceEntryBeanToInstallmentBeforePlan(installmentsCreationBean,
                                currentInvoiceEntryBean, installmentEntryBean.getAmount(), fromDate, installment.getDueDate(),
                                false);
                totalAmountOfCurrentInvoiceEntryInPreviousInstallments =
                        Currency.getValueWithScale(totalAmountOfCurrentInvoiceEntryInPreviousInstallments
                                .add(interestAmountOfCurrentInvoiceEntryBeanToInstallmentBeforePlan));
            }
            return Currency.getValueWithScale(
                    totalInterestAmountBeforePaymentPlan.subtract(totalAmountOfCurrentInvoiceEntryInPreviousInstallments), 20);
        } else {
            // return a rate of totalInterestAmountBeforePaymentPlan respective to amount of this installment
            return Currency.getValueWithScale(TreasuryConstants.divide(amount, currentInvoiceEntryBean.getEntryOpenAmount())
                    .multiply(totalInterestAmountBeforePaymentPlan), 20);
        }
    }

    private BigDecimal getTotalInterestAmountOfInvoiceEntryBeforePaymentPlan(
            PaymentPlanInstallmentCreationBean installmentsCreationBean, ISettlementInvoiceEntryBean currentInvoiceEntryBean) {

        DebitEntry debitEntry = (DebitEntry) currentInvoiceEntryBean.getInvoiceEntry();

        BigDecimal interestAmountOfDebitEntryBean =
                currentInvoiceEntryBean.getSettledAmount().subtract(currentInvoiceEntryBean.getEntryOpenAmount());

        BigDecimal totalInterestDebitEntriesInPlan = installmentsCreationBean
                .getInterestInvoiceEntryBeanOfDebitEntry((DebitEntry) currentInvoiceEntryBean.getInvoiceEntry())
                .filter(bean -> bean.isForDebitEntry()).map(bean -> bean.getSettledAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalOfInterestInPlan = totalInterestDebitEntriesInPlan.add(interestAmountOfDebitEntryBean);

        BigDecimal allInterestAmountBeforePlan =
                debitEntry.calculateAllInterestValue(installmentsCreationBean.getRequestDate()).getInterestAmount();

        BigDecimal totalInterestPaid = debitEntry.getInterestDebitEntriesSet().stream()
                .filter(interest -> !interest.isAnnulled() && !interest.isInDebt())
                .map(interest -> interest.getAvailableAmountForCredit()).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal expectedInterestBeforeInPlan = allInterestAmountBeforePlan.subtract(totalInterestPaid);

        if (TreasuryConstants.isGreaterThan(totalOfInterestInPlan, expectedInterestBeforeInPlan)) {
            return expectedInterestBeforeInPlan;
        } else {
            return totalOfInterestInPlan;
        }
    }

    private BigDecimal createInterestInstallmentEntryBeanInInstallmentBean(
            PaymentPlanInstallmentCreationBean installmentsCreationBean, ISettlementInvoiceEntryBean currentInvoiceEntryBean,
            InstallmentBean currentInstallmentBean, BigDecimal interestEntryAmout, BigDecimal totalInterestEntryAmount) {
        /**
         * Get List of interest beans of debit entry
         *
         * start to create of installmentEntries by Interest debit Entries
         *
         * when is a InterestEntryBean add totalInterestEntryAmount to interestAmount and create installmentEntry with
         * interestEntryAmout
         *
         */

        List<ISettlementInvoiceEntryBean> interestsBeansList = installmentsCreationBean
                .getInterestInvoiceEntryBeanOfDebitEntry((DebitEntry) currentInvoiceEntryBean.getInvoiceEntry())
                .collect(Collectors.toList());

        for (ISettlementInvoiceEntryBean settlementInvoiceEntryBean : interestsBeansList) {
            if (!TreasuryConstants.isPositive(interestEntryAmout)) {
                break;
            }

            if (settlementInvoiceEntryBean.isForDebitEntry()) {
                BigDecimal settlementRestAmout =
                        getRestAmountOfBeanInPaymentPlan(settlementInvoiceEntryBean, installmentsCreationBean);
                if (!TreasuryConstants.isPositive(settlementRestAmout)) {
                    continue;
                }
                if (TreasuryConstants.isGreaterThan(settlementRestAmout, interestEntryAmout)) {
                    //remove difference of settlementRestAmout and interestEntryAmout for InterestEntryBean creation
                    totalInterestEntryAmount =
                            totalInterestEntryAmount.subtract(settlementRestAmout.subtract(interestEntryAmout));
                    settlementRestAmout = interestEntryAmout;
                }

                createInstallmentEntryBean(currentInstallmentBean, settlementInvoiceEntryBean, settlementRestAmout);

                interestEntryAmout = interestEntryAmout.subtract(settlementRestAmout);
                totalInterestEntryAmount = totalInterestEntryAmount.subtract(settlementRestAmout);

            } else {
                //add totalInterestEntryAmount to existent InterestEntryBean
                ((SettlementInterestEntryBean) settlementInvoiceEntryBean).getInterest()
                        .setInterestAmount(settlementInvoiceEntryBean.getSettledAmount().add(totalInterestEntryAmount));
                createInstallmentEntryBean(currentInstallmentBean, settlementInvoiceEntryBean, interestEntryAmout);
                return interestEntryAmout;
            }
        }
        /**
         * If InterestEntryBean not exists and totalInterestEntryAmount is positive
         * THEN:
         * create InterestEntryBean with totalInterestEntryAmount
         * create installment with interestEntryAmout and add to currentInstallmentBean
         */
        if (TreasuryConstants.isPositive(totalInterestEntryAmount)) {
            ISettlementInvoiceEntryBean createdInterestEntryBean = createInterestEntryBean(installmentsCreationBean,
                    (DebitEntry) currentInvoiceEntryBean.getInvoiceEntry(), totalInterestEntryAmount);
            if (TreasuryConstants.isPositive(interestEntryAmout)) {
                createInstallmentEntryBean(currentInstallmentBean, createdInterestEntryBean,
                        Currency.getValueWithScale(interestEntryAmout));
            }
        }
        return interestEntryAmout;
    }

    private ISettlementInvoiceEntryBean createInterestEntryBean(PaymentPlanInstallmentCreationBean installmentsCreationBean,
            DebitEntry debitEntry, BigDecimal interestEntryAmout) {
        if (TreasuryConstants.isPositive(interestEntryAmout)) {
            InterestRateBean interestRateBean = new InterestRateBean();
            interestRateBean.setDescription(TreasuryConstants.treasuryBundle(TreasuryConstants.DEFAULT_LANGUAGE,
                    "label.InterestRateBean.interest.designation", debitEntry.getDescription()));
            interestRateBean.setInterestAmount(interestEntryAmout);
            SettlementInterestEntryBean interestEntryBean = new SettlementInterestEntryBean(debitEntry, interestRateBean);
            installmentsCreationBean.addPaymentPlanSettlementInvoiceEntryBean(interestEntryBean);
            installmentsCreationBean.addInvoiceEntryBeanToBeTreatedAndSort(interestEntryBean);
            return interestEntryBean;
        } else {
            return null;
        }
    }

    protected BigDecimal getInterestAmountToPaymentPlan(PaymentPlanInstallmentCreationBean installmentsCreationBean,
            ISettlementInvoiceEntryBean currentInvoiceEntryBean) {
        return getTotalInterestAmountOfInvoiceEntryBeforePaymentPlan(installmentsCreationBean, currentInvoiceEntryBean);
    }

    protected void resetSettlementInvoiceEntryBeansForChoosenDebitEntries(PaymentPlanBean paymentPlanBean) {
        paymentPlanBean.setSettlementInvoiceEntryBeans(paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                .filter(bean -> bean.isForDebitEntry() || bean.isForPendingDebitEntry()).collect(Collectors.toSet()));
    }
    //*******************
    // Comparators Utilities
    //*******************

    private static DebitEntry getOriginDebitEntryFromInterestEntry(ISettlementInvoiceEntryBean bean) {
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

    //bean created to not modify the payment plan bean
    protected class PaymentPlanInstallmentCreationBean {
        private PaymentPlanBean paymentPlanBean;
        private List<InstallmentBean> installments;
        private List<BigDecimal> installmentsMaxAmount;
        private List<ISettlementInvoiceEntryBean> invoiceEntriesToBeTreated;

        public PaymentPlanInstallmentCreationBean(PaymentPlanBean paymentPlanBean, List<LocalDate> fixedDates,
                List<BigDecimal> fixedAmountList) {
            this.paymentPlanBean = paymentPlanBean;
            //clean extra interest warning
            paymentPlanBean.setExtraInterestWarning(new LinkedHashMap<>());

            installments = createInstallmentsList(paymentPlanBean, fixedDates);
            installmentsMaxAmount = createInstallmentMaxAmountList(paymentPlanBean, fixedAmountList);

            resetSettlementInvoiceEntryBeansForChoosenDebitEntries(paymentPlanBean);
            invoiceEntriesToBeTreated = paymentPlanBean.getSettlementInvoiceEntryBeans().stream().sorted(getComparator())
                    .collect(Collectors.toList());

            if (invoiceEntriesToBeTreated.isEmpty()) {
                throw new TreasuryDomainException(
                        TreasuryConstants.treasuryBundle("label.paymentPlanInstalllmentCreation.invoiceEntries.required"));
            }
        }

        private List<BigDecimal> createInstallmentMaxAmountList(PaymentPlanBean paymentPlanBean,
                List<BigDecimal> fixedAmountList) {

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

        public ISettlementInvoiceEntryBean getNextInvoiceEntryBean() {
            ISettlementInvoiceEntryBean iSettlementInvoiceEntryBean =
                    invoiceEntriesToBeTreated.isEmpty() ? null : invoiceEntriesToBeTreated.get(0);
            if (!invoiceEntriesToBeTreated.isEmpty()) {
                invoiceEntriesToBeTreated.remove(0);
            }
            return iSettlementInvoiceEntryBean;
        }

        //**********************
        //Getteres and Setters
        //**********************
        public List<InstallmentBean> getInstallments() {
            return installments;
        }

        public BigDecimal getInstallmentAmount(int i) {
            return installmentsMaxAmount.get(i);
        }

        public InstallmentBean getInstallmentBean(int i) {
            return installments.get(i);
        }

        public boolean diffFirstLastAmountGreaterOrEqualThanNbInstallmentsInCents() {
            BigDecimal nbInstallmentsInCents = Currency.getValueWithScale(TreasuryConstants
                    .divide(new BigDecimal(paymentPlanBean.getNbInstallments()), TreasuryConstants.HUNDRED_PERCENT));

            BigDecimal amountFirstInstallment = installments.get(0).getInstallmentAmount();
            BigDecimal amountLastInstallment = installments.get(paymentPlanBean.getNbInstallments() - 1).getInstallmentAmount();
            BigDecimal diffFirstLast = amountFirstInstallment.subtract(amountLastInstallment).abs();
            return TreasuryConstants.isGreaterOrEqualThan(diffFirstLast, nbInstallmentsInCents);
        }

        public void addAmountToLastInstallmentMaxAmount(BigDecimal amount) {
            BigDecimal installmentsAmount = installmentsMaxAmount.stream().reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal settlementInvoiceEntriesAmount = paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                    .map(entry -> entry.isForDebitEntry() ? entry.getEntryOpenAmount() : entry.getSettledAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (TreasuryConstants.isGreaterThan(settlementInvoiceEntriesAmount, installmentsAmount)) {
                installmentsMaxAmount.set(installmentsMaxAmount.size() - 1,
                        installmentsMaxAmount.get(installmentsMaxAmount.size() - 1).add(amount));
            }
        }

        private boolean isInterestOf(ISettlementInvoiceEntryBean settlementInvoiceEntry, DebitEntry invoiceEntry) {
            boolean isInterestEntry = settlementInvoiceEntry.isForDebitEntry()
                    && ((DebitEntry) settlementInvoiceEntry.getInvoiceEntry()).getDebitEntry() == invoiceEntry;
            boolean isPendingInterestEntry = settlementInvoiceEntry.isForPendingInterest()
                    && ((SettlementInterestEntryBean) settlementInvoiceEntry).getDebitEntry() == invoiceEntry;
            return isPendingInterestEntry || isInterestEntry;
        }

        public Stream<ISettlementInvoiceEntryBean> getInterestInvoiceEntryBeanOfDebitEntry(DebitEntry debitEntry) {
            return paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                    .filter(settlementInvoiceEntry -> isInterestOf(settlementInvoiceEntry, debitEntry)).sorted(getComparator());
        }

        public Stream<InstallmentEntryBean> getInstallmentEntryBeansWithInvoiceEntryBean(
                ISettlementInvoiceEntryBean currInvoiceEntryBean) {
            return installments.stream().flatMap(inst -> inst.getInstallmentEntries().stream())
                    .filter(entry -> entry.getInvoiceEntry() == currInvoiceEntryBean);
        }

        public void addPaymentPlanSettlementInvoiceEntryBean(ISettlementInvoiceEntryBean settlementInvoiceEntryBean) {
            paymentPlanBean.addSettlementInvoiceEntryBean(settlementInvoiceEntryBean);
        }

        public LocalDate getRequestDate() {
            return paymentPlanBean.getCreationDate();
        }

        public Optional<ISettlementInvoiceEntryBean> getPaymentPenaltyEntryFormPaymentPlan(DebitEntry debitEntry) {
            return paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                    .filter(bean -> bean.isForPaymentPenalty() && ((PaymentPenaltyEntryBean) bean).getDebitEntry() == debitEntry)
                    .findFirst();
        }

        public void addInvoiceEntryBeanToBeTreatedAndSort(ISettlementInvoiceEntryBean invoiceEntryBean) {
            invoiceEntriesToBeTreated.add(invoiceEntryBean);
            invoiceEntriesToBeTreated.sort(getComparator());
        }

        public LocalDate getPaymentPlanStartDate() {
            return paymentPlanBean.getStartDate();
        }

        public Stream<InstallmentBean> getInstallmentBeansWithInvoiceEntryBean(
                ISettlementInvoiceEntryBean currentInvoiceEntryBean) {
            return installments.stream().filter(installment -> installment.getInstallmentEntries().stream()
                    .anyMatch(installmentEntryBean -> installmentEntryBean.getInvoiceEntry() == currentInvoiceEntryBean));
        }

        public void fillExtraInterestWarning() {
            Map<SettlementDebitEntryBean, BigDecimal> result = new LinkedHashMap<>();

            List<ISettlementInvoiceEntryBean> debitEntryList = paymentPlanBean.getSettlementInvoiceEntryBeans().stream()
                    .filter(bean -> isDebitEntry(bean)).sorted(getComparator()).collect(Collectors.toList());

            for (ISettlementInvoiceEntryBean iSettlementInvoiceEntryBean : debitEntryList) {
                SettlementDebitEntryBean debitEntry = (SettlementDebitEntryBean) iSettlementInvoiceEntryBean;

                BigDecimal totalInterestsBeans = getInterestInvoiceEntryBeanOfDebitEntry(debitEntry.getDebitEntry())
                        .map(bean -> bean.getSettledAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalInterests =
                        Currency.getValueWithScale(getInterestAmountToPaymentPlan(this, iSettlementInvoiceEntryBean));

                BigDecimal diffInterest = totalInterestsBeans.subtract(totalInterests);
                if (TreasuryConstants.isPositive(diffInterest)) {
                    result.put(debitEntry, diffInterest);
                }
            }
            paymentPlanBean.setExtraInterestWarning(result);
        }

        public boolean isLastInstallmentOfPaymentPLan(InstallmentBean currentInstallmentBean) {
            return installments.get(installments.size() - 1) == currentInstallmentBean;
        }
    }
}
