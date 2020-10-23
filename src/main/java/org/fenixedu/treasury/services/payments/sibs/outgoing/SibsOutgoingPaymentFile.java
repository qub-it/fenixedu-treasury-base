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
package org.fenixedu.treasury.services.payments.sibs.outgoing;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.paymentcodes.SibsReferenceCode;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 * 
 * @author naat
 * 
 */
public class SibsOutgoingPaymentFile {

    private static final String DATE_FORMAT = "yyyyMMdd";

    private static final String NUMBER_FILLER = "0";

    private static final String LINE_TERMINATOR = "\r\n";

    private static class Header {

        private static final String HEADER_REGISTER_TYPE = "0";

        private static final String FILE_TYPE = "AEPS";

        private static final String OMISSION_SEQUENCE_NUMBER = "1";

        private static final String CURRENCY_CODE = "978";

        private static final int WHITE_SPACES_IN_HEADER = 3;

        private String sourceInstitutionId;

        private String destinationInstitutionId;

        private String entityCode;

        private DateTime lastSentPaymentFile;

        public Header(String sourceInstitutionId, String destinationInstitutionId, String entityCode) {
            this.sourceInstitutionId = sourceInstitutionId;
            this.destinationInstitutionId = destinationInstitutionId;
            this.entityCode = entityCode;
        }

        public Header(String sourceInstitutionId, String destinationInstitutionId, String entityCode,
                DateTime lastSuccessfulSentDate) {
            this.sourceInstitutionId = sourceInstitutionId;
            this.destinationInstitutionId = destinationInstitutionId;
            this.entityCode = entityCode;
            this.lastSentPaymentFile = lastSuccessfulSentDate;
        }

        public String render() {
            final StringBuilder header = new StringBuilder();
            header.append(HEADER_REGISTER_TYPE);
            header.append(FILE_TYPE);
            header.append(this.sourceInstitutionId);
            header.append(this.destinationInstitutionId);
            header.append(new LocalDate().toString(DATE_FORMAT));
            header.append(OMISSION_SEQUENCE_NUMBER);
            // last file's data if it was already sent
            header.append(lastSentPaymentFile != null ? lastSentPaymentFile.toString(DATE_FORMAT) : "00000000");
            header.append(OMISSION_SEQUENCE_NUMBER);
            header.append(this.entityCode);
            header.append(CURRENCY_CODE);
            header.append(StringUtils.leftPad("", WHITE_SPACES_IN_HEADER));
            header.append(LINE_TERMINATOR);

            return header.toString();
        }
    }

    private static class Footer {

        private static final String FOOTER_REGISTER_TYPE = "9";

        private static final int NUMBER_OF_LINES_DESCRIPTOR_LENGTH = 8;

        public static final int WHITE_SPACES_IN_FOOTER = 41;

        public Footer() {
        }

        public String render(int totalLines) {
            final StringBuilder footer = new StringBuilder();
            footer.append(FOOTER_REGISTER_TYPE);
            footer.append(StringUtils.leftPad(String.valueOf(totalLines), NUMBER_OF_LINES_DESCRIPTOR_LENGTH, NUMBER_FILLER));
            footer.append(StringUtils.leftPad("", WHITE_SPACES_IN_FOOTER));
            footer.append(LINE_TERMINATOR);

            return footer.toString();
        }
    }

    private static class Line {
        private static final String LINE_REGISTER_TYPE = "1";

        // Line Processing code (usually 80 but it can be 82)
        private static final String LINE_PROCESSING_CODE = "80";

        private static int DECIMAL_PLACES_FACTOR = 100;

        private static final int WHITE_SPACES_IN_LINE = 2;

        private static final int AMOUNT_LENGTH = 10;

        private String code;

        private BigDecimal minAmount;

        private BigDecimal maxAmount;

        private LocalDate startDate;

        private LocalDate endDate;

        public Line(String code, BigDecimal minAmount, BigDecimal maxAmount, LocalDate startDate, LocalDate endDate) {

            checkAmounts(code, minAmount, maxAmount);

            this.code = code;
            this.minAmount = minAmount;
            this.maxAmount = maxAmount;
            this.startDate = startDate;
            this.endDate = endDate;
        }

        private void checkAmounts(String code, BigDecimal minAmount, BigDecimal maxAmount) {
            if (minAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException(MessageFormat.format("Min amount for code {0} must be greater than zero", code));
            }

            if (maxAmount.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException(MessageFormat.format("Max amount for code {0} must be greater than zero", code));
            }

        }

        public String render() {
            final StringBuilder result = new StringBuilder();

            result.append(LINE_REGISTER_TYPE);
            result.append(LINE_PROCESSING_CODE);
            result.append(this.code);
            result.append(this.endDate.toString(DATE_FORMAT));
            result.append(leftPadAmount(this.maxAmount));
            result.append(this.startDate.toString(DATE_FORMAT));
            result.append(leftPadAmount(this.minAmount));
            result.append(StringUtils.leftPad("", WHITE_SPACES_IN_LINE));
            result.append(LINE_TERMINATOR);

            return result.toString();
        }

        private String leftPadAmount(final BigDecimal amount) {
            return StringUtils.leftPad(String.valueOf(amount.multiply(BigDecimal.valueOf(DECIMAL_PLACES_FACTOR)).longValue()),
                    AMOUNT_LENGTH, NUMBER_FILLER);
        }
    }

    private Header header;

    private List<Line> lines;

    private Footer footer;

    private Set<String> existingCodes;

    PrintedPaymentCodes associatedPaymentCodes;

    public SibsOutgoingPaymentFile(String sourceInstitutionId, String destinationInstitutionId, String entity) {
        this.header = new Header(sourceInstitutionId, destinationInstitutionId, entity);
        this.lines = new ArrayList<Line>();
        this.footer = new Footer();
        this.existingCodes = new HashSet<String>();
        this.associatedPaymentCodes = new PrintedPaymentCodes();
    }

    public SibsOutgoingPaymentFile(String sourceInstitutionId, String destinationInstitutionId, String entity,
            DateTime lastSuccessfulSentDate) {
        this.header = new Header(sourceInstitutionId, destinationInstitutionId, entity, lastSuccessfulSentDate);
        this.lines = new ArrayList<Line>();
        this.footer = new Footer();
        this.existingCodes = new HashSet<String>();
        this.associatedPaymentCodes = new PrintedPaymentCodes();
    }

    public void addAssociatedPaymentCode(final SibsReferenceCode paymentCode) {
        this.associatedPaymentCodes.addPaymentCode(paymentCode);
    }

    public PrintedPaymentCodes getAssociatedPaymentCodes() {
        return associatedPaymentCodes;
    }

    public void addLine(String code, BigDecimal minAmount, BigDecimal maxAmount, LocalDate validFrom, LocalDate validTo) {
        if (existingCodes.contains(code)) {
            throw new RuntimeException(MessageFormat.format("Code {0} is duplicated", code));
        }

        existingCodes.add(code);

        this.lines.add(new Line(code, minAmount, maxAmount, validFrom, validTo));
    }

    public String render() {
        final StringBuilder result = new StringBuilder();

        result.append(this.header.render());

        for (final Line line : this.lines) {
            result.append(line.render());
        }

        result.append(this.footer.render(this.lines.size()));

        return result.toString();
    }

    @Override
    public String toString() {
        return render();
    }

    public void save(final File destinationFile) {
        BufferedOutputStream outputStream = null;
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(destinationFile));
            outputStream.write(render().getBytes());
            outputStream.flush();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
