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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class Currency extends Currency_Base {

    public static String EURO_CODE = "EUR";

    protected Currency() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected Currency(final String code, final LocalizedString name, final String isoCode, final String symbol) {
        this();
        setCode(code);
        setName(name);
        setIsoCode(isoCode);
        setSymbol(symbol);

        checkRules();
    }

    private void checkRules() {
        if (LocalizedStringUtil.isTrimmedEmpty(getCode())) {
            throw new TreasuryDomainException("error.Currency.code.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getName())) {
            throw new TreasuryDomainException("error.Currency.name.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getIsoCode())) {
            throw new TreasuryDomainException("error.Currency.isoCode.required");
        }

        if (LocalizedStringUtil.isTrimmedEmpty(getSymbol())) {
            throw new TreasuryDomainException("error.Currency.symbol.required");
        }

        findByCode(getCode());
        getName().getLocales().stream().forEach(l -> findByName(getName().getContent(l)));

    }

    @Atomic
    public void edit(final String code, final LocalizedString name, final String isoCode, final String symbol) {
        setCode(code);
        setName(name);
        setIsoCode(isoCode);
        setSymbol(symbol);

        checkRules();
    }

    public boolean isDeletable() {
        return getFinantialDocumentsSet().isEmpty() && getFinantialInstitutionsSet().isEmpty() && getInvoiceEntrySet().isEmpty()
                && getTreasurySettings() == null;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.Currency.cannot.delete");
        }

        setDomainRoot(null);

        deleteDomainObject();
    }

    public static Stream<Currency> findAll() {
        return FenixFramework.getDomainRoot().getCurrenciesSet().stream();
    }

    public static Currency findByCode(final String code) {
        Currency result = null;

        for (final Currency it : findAll().collect(Collectors.toList())) {
            if (!it.getCode().equalsIgnoreCase(code)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.Currency.duplicated.code");
            }

            result = it;
        }

        return result;
    }

    public static Currency findByName(final String name) {
        Currency result = null;

        for (final Currency it : findAll().collect(Collectors.toList())) {

            if (!LocalizedStringUtil.isEqualToAnyLocaleIgnoreCase(it.getName(), name)) {
                continue;
            }

            if (result != null) {
                throw new TreasuryDomainException("error.Currency.duplicated.name");
            }

            result = it;
        }

        return result;
    }

    @Atomic
    public static Currency create(final String code, final LocalizedString name, final String isoCode, final String symbol) {
        return new Currency(code, name, isoCode, symbol);
    }

    @Deprecated
    /* TODO: Rename method to describe the value returned has currency symbol 
     */
    public String getValueFor(BigDecimal value) {
        return getValueWithScale(value) + " " + this.getSymbol();
    }

    @Deprecated
    /* TODO: Rename method to describe the value returned has currency symbol
     */
    public String getValueFor(BigDecimal value, int decimalsPlaces) {
        return getValueWithScale(value, decimalsPlaces) + " " + this.getSymbol();
    }

    public static BigDecimal getValueWithScale(BigDecimal amount) {
        return getValueWithScale(amount, 2);
    }

    public static BigDecimal getValueWithScale(BigDecimal amount, int decimalPlaces) {
        return amount.setScale(decimalPlaces, RoundingMode.HALF_EVEN);
    }

}
