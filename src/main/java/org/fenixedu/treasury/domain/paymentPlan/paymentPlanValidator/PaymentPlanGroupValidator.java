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
package org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.FenixFramework;

public class PaymentPlanGroupValidator extends PaymentPlanGroupValidator_Base {

    public static enum ConjunctionWidget {
        AND(Boolean.TRUE), OR(Boolean.FALSE);

        private Boolean conjuction;

        ConjunctionWidget(Boolean conjuction) {
            this.conjuction = conjuction;
        }

        public Boolean getConjuction() {
            return conjuction;
        }
    }

    public PaymentPlanGroupValidator() {
        super();
        setConjunction(Boolean.TRUE);
    }

    protected PaymentPlanGroupValidator(Set<PaymentPlanValidator> childs, Boolean isConjunction) {
        this();
        getChildValidatorsSet().addAll(childs);
        setConjunction(isConjunction);
    }

    public static PaymentPlanGroupValidator create(Set<PaymentPlanValidator> childs, Boolean isConjunction) {
        return new PaymentPlanGroupValidator(childs, isConjunction);
    }

    @Override
    public Boolean validate(LocalDate date, List<Installment> sortedInstallments) {
        if (getParentValidator() == null || Boolean.TRUE.equals(getConjunction())) {
            // validator1 AND validator2
            return getChildValidatorsSet().stream().allMatch(v -> v.validate(date, sortedInstallments));
        } else {
            // validator1 OR validator2
            return getChildValidatorsSet().stream().anyMatch(v -> v.validate(date, sortedInstallments));
        }
    }

    @Override
    public String getDescription() {
        if (getConjunction()) {
            return TreasuryConstants.treasuryBundle(
                    "org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator.PaymentPlanGroupValidator.GroupofrulesAND");
        } else {
            return TreasuryConstants.treasuryBundle(
                    "org.fenixedu.treasury.domain.paymentPlan.paymentPlanValidator.PaymentPlanGroupValidator.GroupofrulesOR");
        }
    }

    @Override
    public void delete() {
        if (!getPaymentPlansSet().isEmpty()) {
            throw new IllegalArgumentException(TreasuryConstants
                    .treasuryBundle("PaymentPlanGroupValidator.Cannot.delete.PaymentPlanValidator.withAssocietedPlans"));
        } else if (getParentValidator() == null) {
            getChildValidatorsSet().forEach(v -> v.delete(true));
            setDomainRoot(null);
            deleteDomainObject();
        } else {
            getChildValidatorsSet().forEach(v -> v.setParentValidator(getParentValidator()));
            setParentValidator(null);
            setDomainRoot(null);
            deleteDomainObject();
        }
    }

    @Override
    public void delete(boolean deleteChilds) {
        if (!getPaymentPlansSet().isEmpty()) {
            throw new IllegalArgumentException(TreasuryConstants
                    .treasuryBundle("PaymentPlanGroupValidator.Cannot.delete.PaymentPlanValidator.withAssocietedPlans"));
        } else if (getParentValidator() == null) {
            getChildValidatorsSet().forEach(v -> v.delete(true));
            setDomainRoot(null);
            deleteDomainObject();
        } else {
            if (deleteChilds) {
                getChildValidatorsSet().forEach(v -> v.delete(deleteChilds));
            } else {
                getChildValidatorsSet().forEach(v -> v.setParentValidator(getParentValidator()));
            }
            setParentValidator(null);
            setDomainRoot(null);
            deleteDomainObject();
        }
    }

    public static Set<PaymentPlanGroupValidator> findRootGroupValidators() {
        return FenixFramework.getDomainRoot().getPaymentPlanValidatorsSet().stream()
                .filter(PaymentPlanGroupValidator.class::isInstance).map(PaymentPlanGroupValidator.class::cast)
                .filter(p -> p.getParentValidator() == null).collect(Collectors.toSet());
    }

    public static Set<PaymentPlanGroupValidator> findActiveGroupValidators() {
        return findRootGroupValidators().stream().filter(p -> Boolean.TRUE.equals(p.getActive())).collect(Collectors.toSet());
    }

    public static PaymentPlanGroupValidator create(LocalizedString newName, Boolean active, PaymentPlanGroupValidator base) {
        PaymentPlanGroupValidator result = new PaymentPlanGroupValidator();
        result.setName(newName);
        result.setActive(active);
        result.setConjunction(Boolean.TRUE);

        for (PaymentPlanValidator validator : base.getChildValidatorsSet()) {
            result.addChildValidators(validator.clone());
        }

        return result;
    }

    @Override
    protected PaymentPlanValidator clone() {
        PaymentPlanGroupValidator result = new PaymentPlanGroupValidator();

        result.setConjunction(getConjunction());

        for (PaymentPlanValidator validator : getChildValidatorsSet()) {
            result.addChildValidators(validator.clone());
        }
        return result;
    }

}
