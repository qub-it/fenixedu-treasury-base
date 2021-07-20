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
package org.fenixedu.treasury.domain;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class PaymentMethod extends PaymentMethod_Base {

    protected PaymentMethod() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected PaymentMethod(final String code, final LocalizedString name, final boolean availableForPaymentInApplication) {
        this();
        setCode(code);
        setName(name);
        setAvailableForPaymentInApplication(availableForPaymentInApplication);

        checkRules();
    }

    private void checkRules() {
        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.PaymentMethod.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.PaymentMethod.name.required");
        }

        findByCode(getCode());
        getName().getLocales().stream().forEach(l -> findByName(getName().getContent(l)));
    }
    
    public boolean isAvailableForPaymentInApplication() {
        return getAvailableForPaymentInApplication();
    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final boolean availableForPaymentInApplication) {
        setCode(code);
        setName(name);
        setAvailableForPaymentInApplication(availableForPaymentInApplication);

        checkRules();
    }

    public boolean isDeletable() {
        return getPaymentCodePoolPaymentMethodSet().isEmpty() && getPaymentEntriesSet().isEmpty()
                && getReimbursementEntriesSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.PaymentMethod.cannot.delete");
        }

        setDomainRoot(null);

        deleteDomainObject();
    }

    @Atomic
    public static void initializePaymentMethod() {
        if (PaymentMethod.findAll().count() == 0) {
            PaymentMethod.create("NU", treasuryBundleI18N("label.PaymentMethod.MON"), true);
            PaymentMethod.create("TB", treasuryBundleI18N("label.PaymentMethod.WTR"), true);
            PaymentMethod.create("MB", treasuryBundleI18N("label.PaymentMethod.ELE"), true);
            PaymentMethod.create("CD", treasuryBundleI18N("label.PaymentMethod.CCR"), true);
            PaymentMethod.create("CH", treasuryBundleI18N("label.PaymentMethod.CH"), true);
        }
    }

    public static Stream<PaymentMethod> findAll() {
        return FenixFramework.getDomainRoot().getPaymentMethodsSet().stream();
    }
    
    public static Stream<PaymentMethod> findAvailableForPaymentInApplication() {
        return findAll().filter(l -> l.isAvailableForPaymentInApplication());
    }

    public static PaymentMethod findByCode(final String code) {
        PaymentMethod result = null;

        for (final PaymentMethod it : findAll().collect(Collectors.toList())) {
            if (!it.getCode().equalsIgnoreCase(code)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.PaymentMethod.duplicated.code");
            }

            result = it;
        }

        return result;
    }

    public static PaymentMethod findByName(final String name) {
        PaymentMethod result = null;

        for (final PaymentMethod it : findAll().collect(Collectors.toList())) {

            if (!LocalizedStringUtil.isEqualToAnyLocaleIgnoreCase(it.getName(), name)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.PaymentMethod.duplicated.name");
            }

            result = it;
        }

        return result;
    }

    @Atomic
    public static PaymentMethod create(final String code, final LocalizedString name, boolean availableForPaymentInApplication) {
        return new PaymentMethod(code, name, availableForPaymentInApplication);
    }

}
