/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
 * 
 *
 * 
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
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
