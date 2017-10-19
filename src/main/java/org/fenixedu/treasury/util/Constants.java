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
package org.fenixedu.treasury.util;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.springframework.util.StringUtils;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class Constants {

    private static final int SCALE = 20;

    public static final BigDecimal HUNDRED_PERCENT = new BigDecimal("100.00");

    public static final String BUNDLE = "resources.FenixeduTreasuryResources";

    // HACK: org.joda.time.Interval does not allow open end dates so use this date in the future
    public static final DateTime INFINITY_DATE = new DateTime().plusYears(500);

    public static final BigDecimal DEFAULT_QUANTITY = BigDecimal.ONE;

    public static final Locale DEFAULT_LANGUAGE = new Locale("PT");
    public static final String DEFAULT_COUNTRY = "PT";

    private static final int ORIGIN_DOCUMENT_LIMIT = 30;

    public static final String DATE_FORMAT = "dd/MM/yyyy";

    public static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy/MM/dd";

    public static final String DATE_TIME_FORMAT_YYYY_MM_DD = "yyyy/MM/dd HH:mm:ss";

    public static final String STANDARD_DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";

    // @formatter:off
    /* *************
     * COUNTRY UTILS
     * *************
     */
    // @formatter:on

    public static boolean isForeignLanguage(final Locale language) {
        return !language.getLanguage().equals(DEFAULT_LANGUAGE.getLanguage());
    }

    public static boolean isDefaultCountry(final String country) {
        if (Strings.isNullOrEmpty(country)) {
            return false;
        }

        return DEFAULT_COUNTRY.equals(country.toUpperCase());
    }

    // @formatter:off
    /**************
     * MATH UTILS *
     **************/
    // @formatter:on

    public static boolean isNegative(final BigDecimal value) {
        return !isZero(value) && !isPositive(value);
    }

    public static boolean isZero(final BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) == 0;
    }

    public static boolean isPositive(final BigDecimal value) {
        return BigDecimal.ZERO.compareTo(value) < 0;
    }

    public static boolean isGreaterThan(final BigDecimal v1, final BigDecimal v2) {
        return v1.compareTo(v2) > 0;
    }

    public static boolean isGreaterOrEqualThan(final BigDecimal v1, final BigDecimal v2) {
        return v1.compareTo(v2) >= 0;
    }

    public static boolean isEqual(final BigDecimal v1, final BigDecimal v2) {
        return v1.compareTo(v2) == 0;
    }

    public static boolean isLessThan(final BigDecimal v1, final BigDecimal v2) {
        return v1.compareTo(v2) < 0;
    }

    public static boolean isLessOrEqualThan(final BigDecimal v1, final BigDecimal v2) {
        return v1.compareTo(v2) <= 0;
    }

    public static BigDecimal defaultScale(final BigDecimal v) {
        return v.setScale(20, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal divide(final BigDecimal a, BigDecimal b) {
        return a.divide(b, SCALE, RoundingMode.HALF_EVEN);
    }

    public static BigDecimal rationalRatRate(final InvoiceEntry entry) {
        return divide(entry.getVatRate(), BigDecimal.valueOf(100));
    }
    
    // @formatter:off
    /**************
     * DATE UTILS *
     **************/
    // @formatter:on

    public static int numberOfDaysInYear(final int year) {

        if (new LocalDate(year, 1, 1).year().isLeap()) {
            return 366;
        }

        return 365;
    }

    public static LocalDate lastDayInYear(final int year) {
        return new LocalDate(year, 12, 31);
    }

    public static LocalDate firstDayInYear(final int year) {
        return new LocalDate(year, 1, 1);
    }

    // @formatter:off
    /****************
     * STRING UTILS *
     ****************/
    // @formatter:on

    public static boolean stringNormalizedContains(final String text, final String compound) {
        final String textNormalized = Normalizer.normalize(text.toLowerCase(), Normalizer.Form.NFC);
        final String compoundNormalized = Normalizer.normalize(compound.toLowerCase(), Normalizer.Form.NFC);
        
        return textNormalized.contains(compoundNormalized);
    }
    
    public static boolean matchNames(final String value, final String searchTerms) {
        final List<String> valuesArray = Lists.<String> newArrayList(value.split("\\s+"));
        final List<String> searchTermsArray = Lists.<String> newArrayList(searchTerms.split("\\s+"));
        
        if(valuesArray.isEmpty() && !searchTermsArray.isEmpty()) {
            return false;
        }
        
        for (final String term : searchTermsArray) {
            if(!valuesArray.contains(term)) {
                return false;
            }
        }
        
        return true;
    }

    // @formatter:off
    /**********
     * BUNDLE *
     **********/
    // @formatter:on

    public static String bundle(final String key, final String... args) {
    	return TreasuryPlataformDependentServicesFactory.implementation().bundle(BUNDLE, key, args);
    }

    public static LocalizedString bundleI18N(final String key, final String... args) {
    	return TreasuryPlataformDependentServicesFactory.implementation().bundleI18N(BUNDLE, key, args);
    }

    // @formatter:off
    /********
     * SIBS *
     ********/
    // @formatter:on

    public static final String sibsTransactionUniqueIdentifier(final String paymentCode, final DateTime whenOccured) {
        return String.format("%s%s", paymentCode, whenOccured.toString("yyyyMMddHHmm"));
    }

    public static boolean isOriginDocumentNumberValid(String originDocumentNumber) {
        if (Strings.isNullOrEmpty(originDocumentNumber)) {
            return true;
        }

        return originDocumentNumber.length() <= ORIGIN_DOCUMENT_LIMIT;
    }
    
    
    // @formatter:off
    /***********
     * JSON UTILS
     *********** 
     */
    // @formatter:on

    public static String propertiesMapToJson(final Map<String, String> propertiesMap) {
        final GsonBuilder builder = new GsonBuilder();

        final Gson gson = builder.create();
        final Type stringStringMapType = new TypeToken<Map<String, String>>() {
        }.getType();

        return gson.toJson(propertiesMap, stringStringMapType);
    }

    public static Map<String, String> propertiesJsonToMap(final String propertiesMapJson) {
        if (StringUtils.isEmpty(propertiesMapJson)) {
            return new HashMap<String, String>();
        }
        
        final GsonBuilder builder = new GsonBuilder();

        final Gson gson = builder.create();
        final Type stringStringMapType = new TypeToken<Map<String, String>>() {
        }.getType();

        return gson.fromJson(propertiesMapJson, stringStringMapType);
    }

    
}






























