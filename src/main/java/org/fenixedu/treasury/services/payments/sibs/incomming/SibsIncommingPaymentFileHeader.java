/**
 * Copyright © 2002 Instituto Superior Técnico
 *
 * This file is part of FenixEdu Academic.
 *
 * FenixEdu Academic is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Academic is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Academic.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.treasury.services.payments.sibs.incomming;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.joda.time.LocalDate;
import org.joda.time.YearMonthDay;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public class SibsIncommingPaymentFileHeader {

    private static final String DATE_FORMAT = "yyyyMMdd";

    private static final int[] FIELD_SIZES = new int[] { 1, 4, 8, 8, 9, 9, 5, 3, 2, 4, 47 };

    private LocalDate whenProcessedBySibs;

    private Integer version;

    private String entityCode;

    public SibsIncommingPaymentFileHeader(LocalDate whenProcessedBySibs, Integer version, String entityCode) {
        this.whenProcessedBySibs = whenProcessedBySibs;
        this.version = version;
        this.entityCode = entityCode;
    }

    public static SibsIncommingPaymentFileHeader buildFrom(String rawLine) {
        final String[] fields = splitLine(rawLine);
        return new SibsIncommingPaymentFileHeader(getWhenProcessedBySibsFrom(fields), getVersionFrom(fields),
                getEntityCodeFrom(fields));
    }

    private static String getEntityCodeFrom(String[] fields) {
        return fields[6];
    }

    private static LocalDate getWhenProcessedBySibsFrom(String[] fields) {
        String dateStringValue = fields[4].substring(0, DATE_FORMAT.length());
        return DateTimeFormat.forPattern(DATE_FORMAT).parseLocalDate(dateStringValue);
    }

    private static Integer getVersionFrom(String[] fields) {
        return Integer.valueOf(fields[5].substring(DATE_FORMAT.length()));
    }

    private final static String[] splitLine(final String line) {
        int lastIndex = 0;
        final String[] result = new String[FIELD_SIZES.length];
        for (int i = 0; i < FIELD_SIZES.length; i++) {
            result[i] = line.substring(lastIndex, lastIndex + FIELD_SIZES[i]);
            lastIndex += FIELD_SIZES[i];
        }
        return result;
    }

    public LocalDate getWhenProcessedBySibs() {
        return whenProcessedBySibs;
    }

    public Integer getVersion() {
        return version;
    }

    public String getEntityCode() {
        return entityCode;
    }

    public void setEntityCode(String entityCode) {
        this.entityCode = entityCode;
    }

}
