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

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentCodeTransaction;
import org.joda.time.DateTime;

public class SibsIncommingPaymentFileDetailLine {

    private DateTime whenOccuredTransaction;

    private BigDecimal amount;

    private String sibsTransactionId;

    private String code;

    private static final int[] FIELD_SIZES = new int[] { 1, 2, 4, 8, 12, 10, 5, 2, 10, 5, 15, 9, 1, 1, 12, 3 };

    public static SibsIncommingPaymentFileDetailLine buildFrom(String rawLine) {
        final String[] fields = splitLine(rawLine);
        return new SibsIncommingPaymentFileDetailLine(getWhenOccuredTransactionFrom(fields), getAmountFrom(fields),
                getSibsTransactionIdFrom(fields), getCodeFrom(fields));
    }

    public SibsIncommingPaymentFileDetailLine(DateTime whenOccuredTransactionFrom, BigDecimal amountFrom,
            String sibsTransactionIdFrom, String codeFrom) {
        this.whenOccuredTransaction = whenOccuredTransactionFrom;
        this.amount = amountFrom;
        this.sibsTransactionId = sibsTransactionIdFrom;
        this.code = codeFrom;
    }

    private static String getCodeFrom(String[] fields) {
        return fields[11];
    }

    private static String getSibsTransactionIdFrom(String[] fields) {
        return fields[9];
    }

    private static BigDecimal getAmountFrom(String[] fields) {
        return BigDecimal.valueOf(Double.parseDouble(fields[5].substring(0, 8) + "." + fields[5].substring(8)));
    }

    private static DateTime getWhenOccuredTransactionFrom(String[] fields) {
        try {
            return new DateTime(new SimpleDateFormat(SibsPaymentCodeTransaction.DATE_TIME_FORMAT).parse(fields[4]));
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getSibsTransactionId() {
        return sibsTransactionId;
    }

    public void setSibsTransactionId(String sibsTransactionId) {
        this.sibsTransactionId = sibsTransactionId;
    }

    public DateTime getWhenOccuredTransaction() {
        return whenOccuredTransaction;
    }

    public void setWhenOccuredTransaction(DateTime whenOccuredTransaction) {
        this.whenOccuredTransaction = whenOccuredTransaction;
    }
}
