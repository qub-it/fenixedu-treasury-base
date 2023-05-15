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

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

public class PaymentPlanNumberGenerator extends PaymentPlanNumberGenerator_Base {

    public PaymentPlanNumberGenerator() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    public PaymentPlanNumberGenerator(LocalizedString name, String prefix, Integer initialValue) {
        this();
        setName(name);
        setPrefix(prefix);
        setInitialValue(initialValue);
        checkRules();
    }

    public static PaymentPlanNumberGenerator create(LocalizedString name, String prefix, Integer initialValue) {
        return new PaymentPlanNumberGenerator(name, prefix, initialValue);
    }

    private void checkRules() {
        if (getInitialValue() == null) {
            throw new TreasuryDomainException("error.PaymentPlanNumberGenerator.initialValue.required");
        }

        if (getPrefix() == null) {
            throw new TreasuryDomainException("error.PaymentPlanNumberGenerator.prefix.required");
        }

        if (getName() == null) {
            throw new TreasuryDomainException("error.PaymentPlanNumberGenerator.name.required");
        }
    }

    protected String getPrefixToGenerateNumber() {
        return StringUtils.isEmpty(getPrefix()) ? "" : getPrefix();
    }

    public String generateNumber() {
        setActualValue(getActualValue() == null ? getInitialValue() : getActualValue() + 1);
        return getPrefixToGenerateNumber() + getActualValue();
    }

    public String getNextNumberPreview() {
        return getPrefixToGenerateNumber() + (getActualValue() == null ? getInitialValue() : getActualValue() + 1);
    }

    public void delete() {
        if (!getPaymentPlanSettingsSet().isEmpty()) {
            throw new TreasuryDomainException("error.PaymentPlanNumberGenerator.in.settings.cannot.be.deleted");
        }
        setDomainRoot(null);
        super.deleteDomainObject();
    }

}
