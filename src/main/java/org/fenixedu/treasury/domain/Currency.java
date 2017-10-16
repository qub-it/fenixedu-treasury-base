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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;


import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.Constants;
import org.fenixedu.treasury.util.LocalizedStringUtil;

import pt.ist.fenixframework.Atomic;

public class Currency extends Currency_Base {

    public static String EURO_CODE = "EUR";

    @Atomic
    public static void initializeCurrency() {
        if (Currency.findAll().count() == 0) {
            Currency.create("EUR",
            		org.fenixedu.treasury.util.Constants.bundleI18N("label.Currency.EUR"),
            		org.fenixedu.treasury.util.Constants.bundle("label.Currency.EUR"), "€");
        }
    }

    protected Currency() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
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
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getCurrenciesSet().stream();
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

    public String getValueFor(BigDecimal value) {
        return getValueWithScale(value) + " " + this.getSymbol();
    }

    public String getValueFor(BigDecimal value, int decimalsPlaces) {
        return getValueWithScale(value, decimalsPlaces) + " " + this.getSymbol();
    }

    public BigDecimal getValueWithScale(BigDecimal amount) {
        return getValueWithScale(amount, 2);
    }

    public BigDecimal getValueWithScale(BigDecimal amount, int decimalPlaces) {
        return amount.setScale(decimalPlaces, RoundingMode.HALF_EVEN);
    }

}
