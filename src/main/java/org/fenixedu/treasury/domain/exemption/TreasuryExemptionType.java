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
package org.fenixedu.treasury.domain.exemption;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.LocalizedStringUtil;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TreasuryExemptionType extends TreasuryExemptionType_Base {

    public static final Comparator<? super TreasuryExemptionType> COMPARE_BY_NAME = new Comparator<TreasuryExemptionType>() {
        @Override
        public int compare(final TreasuryExemptionType o1, final TreasuryExemptionType o2) {
            int c = o1.getName().getContent().compareTo(o2.getName().getContent());
            return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
        }
    };

    protected TreasuryExemptionType() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TreasuryExemptionType(final String code, final LocalizedString name, final BigDecimal defaultExemptionPercentage,
            final boolean active) {
        this();
        setCode(code);
        setName(name);
        setDefaultExemptionPercentage(defaultExemptionPercentage);
        setActive(active);

        checkRules();
    }

    private void checkRules() {
        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.Currency.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.Currency.name.required");
        }

        if (getDefaultExemptionPercentage() == null) {
            throw new TreasuryDomainException("error.TreasuryExemptionType.defaultExemptionPercentage.required");
        }

        if (findByCode(getCode()).count() > 1) {
            throw new TreasuryDomainException("error.TreasuryExemptionType.code.duplicated");
        }

        if (!TreasuryConstants.isPositive(getDefaultExemptionPercentage())
                || TreasuryConstants.isGreaterThan(getDefaultExemptionPercentage(), TreasuryConstants.HUNDRED_PERCENT)) {
            throw new TreasuryDomainException("error.TreasuryExemptionType.defaultExemptionPercentage.invalid");
        }
    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final BigDecimal discountRate, final boolean active) {
        setCode(code);
        setName(name);
        setDefaultExemptionPercentage(discountRate);
        setActive(active);

        checkRules();
    }

    public boolean isDeletable() {
        return getTreasuryExemptionsSet().isEmpty();
    }

    public boolean isActive() {
        return super.getActive();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.TreasuryExemptionType.cannot.delete");
        }
        setDomainRoot(null);
        deleteDomainObject();
    }

    @Atomic
    public static TreasuryExemptionType create(final String code, final LocalizedString name,
            final BigDecimal defaultExemptionPercentage, final boolean active) {
        return new TreasuryExemptionType(code, name, defaultExemptionPercentage, active);
    }

    public static Stream<TreasuryExemptionType> findAll() {
        return FenixFramework.getDomainRoot().getTreasuryExemptionTypesSet().stream();
    }

    public static Stream<TreasuryExemptionType> findByCode(final String code) {
        return findAll().filter(i -> code.equalsIgnoreCase(i.getCode()));
    }

    public static Stream<TreasuryExemptionType> findByDebtAccount(DebtAccount debtAccount) {
        return Stream.empty();
    }

}
