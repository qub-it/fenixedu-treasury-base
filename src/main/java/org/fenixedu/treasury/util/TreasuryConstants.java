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
package org.fenixedu.treasury.util;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class TreasuryConstants {

    private static final int SCALE = 20;

    public static final BigDecimal HUNDRED_PERCENT = new BigDecimal("100.00");

    public static final String BUNDLE = "resources.FenixeduTreasuryResources";

    // HACK: org.joda.time.Interval does not allow open end dates so use this date in the future
    public static final DateTime INFINITY_DATE = new DateTime().plusYears(500);

    public static final BigDecimal DEFAULT_QUANTITY = BigDecimal.ONE;

    @Deprecated
    // TODO: Replace with a solution provided by the platform
    public static final Locale DEFAULT_LANGUAGE = new Locale("PT");

    @Deprecated
    // TODO: Replace with a solution provided by the platform
    public static final String DEFAULT_COUNTRY = "PT";

    private static final int ORIGIN_DOCUMENT_LIMIT = 100;

    public static final String DATE_FORMAT = "dd/MM/yyyy";

    public static final String DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    public static final String DATE_FORMAT_YYYY_MM_DD = "yyyy/MM/dd";

    public static final String DATE_TIME_FORMAT_YYYY_MM_DD = "yyyy/MM/dd HH:mm:ss";

    public static final String STANDARD_DATE_FORMAT_YYYY_MM_DD = "yyyy-MM-dd";

    // @formatter:off
    /* **************
     * DEFAULT VALUES
     * **************
     */
    // @formatter:on

    public static LocalizedString getDefaultProductUnitDescription() {
        return treasuryBundleI18N("label.TreasuryConstants.default.product.unit.description");
    }

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

        return isSameCountryCode(DEFAULT_COUNTRY, country);
    }

    public static boolean isSameCountryCode(final String leftCountryCode, final String rightCountryCode) {
        return lowerCase(leftCountryCode).equals(lowerCase(rightCountryCode));
    }

    private static String lowerCase(final String value) {
        if (value == null) {
            return null;
        }

        return value.toLowerCase();
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

    public static BigDecimal rationalVatRate(final InvoiceEntry entry) {
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

    public static boolean isDateBetween(final LocalDate beginDate, final LocalDate endDate, final LocalDate when) {
        return new Interval(beginDate.toDateTimeAtStartOfDay(),
                endDate != null ? endDate.toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1) : INFINITY_DATE)
                        .contains(when.toDateTimeAtStartOfDay());
    }

    public static boolean isDateBetween(final LocalDate beginDate, final LocalDate endDate, final DateTime when) {
        return new Interval(beginDate.toDateTimeAtStartOfDay(),
                endDate != null ? endDate.toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1) : INFINITY_DATE).contains(when);
    }

    public static DateTime parseDateTime(String strValue, String pattern) {
        DateTimeFormatter dateTimePattern = DateTimeFormat.forPattern(pattern);

        return dateTimePattern.parseDateTime(strValue);
    }

    public static LocalDate parseLocalDate(String strValue, String pattern) {
        DateTimeFormatter dateTimePattern = DateTimeFormat.forPattern(pattern);

        return dateTimePattern.parseLocalDate(strValue);
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

        if (valuesArray.isEmpty() && !searchTermsArray.isEmpty()) {
            return false;
        }

        for (final String term : searchTermsArray) {
            if (!valuesArray.stream().anyMatch(str -> str.contains(term))) {
                return false;
            }
        }

        return true;
    }

    public static String firstAndLastWords(final String value) {
        if (Strings.isNullOrEmpty(value)) {
            return value;
        }

        final List<String> wordsList = Splitter.onPattern("\\s+").splitToList(value.trim());

        if (wordsList.size() == 1) {
            return value;
        }

        return wordsList.get(0) + " " + wordsList.get(wordsList.size() - 1);
    }

    public static String json(final Object obj) {
        GsonBuilder builder = new GsonBuilder();
        builder.addSerializationExclusionStrategy(new ExclusionStrategy() {

            @Override
            public boolean shouldSkipField(FieldAttributes arg0) {
                return false;
            }

            @Override
            public boolean shouldSkipClass(final Class<?> clazz) {
                if (clazz == Class.class) {
                    return true;
                }

                return false;
            }
        });

        return builder.create().toJson(obj);
    }

    // @formatter:off
    /**********
     * BUNDLE *
     **********/
    // @formatter:on

    public static String treasuryBundle(final String key, final String... args) {
        return TreasuryPlataformDependentServicesFactory.implementation().bundle(TreasuryConstants.BUNDLE, key, args);
    }

    public static String treasuryBundle(final Locale locale, final String key, final String... args) {
        return TreasuryPlataformDependentServicesFactory.implementation().bundle(locale, TreasuryConstants.BUNDLE, key, args);
    }

    public static LocalizedString treasuryBundleI18N(final String key, final String... args) {
        return TreasuryPlataformDependentServicesFactory.implementation().bundleI18N(TreasuryConstants.BUNDLE, key, args);
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
