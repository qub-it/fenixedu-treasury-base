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

import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.domain.settings.TreasurySettings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentPlanSettings extends PaymentPlanSettings_Base {

    public PaymentPlanSettings() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        super.setActive(Boolean.FALSE);
        setTreasurySettings(TreasurySettings.getInstance());
    }

    private PaymentPlanSettings(LocalizedString installmentDescriptionFormat, Boolean interestCalculationOfDebitsInPlans,
            Product emolumentProduct, Integer numberOfPaymentPlansActives, PaymentPlanNumberGenerator numberGenerator,
            PaymentCodePool paymentCodePool) {
        this();

        setInstallmentDescriptionFormat(installmentDescriptionFormat);
        setInterestCalculationOfDebitsInPlans(interestCalculationOfDebitsInPlans);
        setEmolumentProduct(emolumentProduct);

        setNumberOfPaymentPlansActives(numberOfPaymentPlansActives);
        setNumberGenerators(numberGenerator);
        setPaymentCodePool(paymentCodePool);
        checkRules();
    }

    private void checkRules() {
        if (getTreasurySettings() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.treasurySettings.required");
        }
        if (getInstallmentDescriptionFormat() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.InstallmentDescriptionFormat.required");
        }
        if (getInterestCalculationOfDebitsInPlans() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.InterestCalculationOfDebitsInPlans.required");
        }
        if (getEmolumentProduct() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.EmolumentProduct.required");
        }
        if (Boolean.TRUE.equals(getActive()) && getTreasurySettings().getPaymentPlanSettingsSet().stream()
                .anyMatch(p -> p != this && Boolean.TRUE.equals(p.getActive()))) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.only.one.can.be.active");
        }

        if (getNumberOfPaymentPlansActives() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.numberOfPaymentPlansActives.required");
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

        if (getPaymentCodePool() == null) {
            throw new TreasuryDomainException("error.PaymentPlanSettings.PaymentCodePool.required");
        }
    }

    @Atomic
    public static PaymentPlanSettings create(LocalizedString installmentDescriptionFormat,
            Boolean interestCalculationOfDebitsInPlans, Product emolumentProduct, Integer numberOfPaymentPlansActives,
            PaymentPlanNumberGenerator numberGenerator, PaymentCodePool paymentCodePool) {
        return new PaymentPlanSettings(installmentDescriptionFormat, interestCalculationOfDebitsInPlans, emolumentProduct,
                numberOfPaymentPlansActives, numberGenerator, paymentCodePool);
    }

    @Override
    @Atomic
    public void setActive(Boolean active) {
        super.setActive(active);
        checkRules();
    }

    /*
     * SERVICES
     */

    public static Stream<PaymentPlanSettings> findAll() {
        return FenixFramework.getDomainRoot().getPaymentPlanSettingsSet().stream();
    }

    public static PaymentPlanSettings getActiveInstance() {
        return TreasurySettings.getInstance().getPaymentPlanSettingsSet().stream().filter(p -> Boolean.TRUE.equals(p.getActive()))
                .findFirst().orElse(null);
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
        setPaymentCodePool(null);
        super.deleteDomainObject();
    }

    @Deprecated
    @Override
    public void setPaymentCodePool(PaymentCodePool paymentCodePool) {
        super.setPaymentCodePool(paymentCodePool);
    }

    @Deprecated
    @Override
    public PaymentCodePool getPaymentCodePool() {
        return super.getPaymentCodePool();
    }

}
