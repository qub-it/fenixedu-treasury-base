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
