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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

public class SibsIncommingPaymentFile {

    private static final String HEADER_REGISTER_TYPE = "0";

    private static final String DETAIL_REGISTER_TYPE = "2";

    private static final String FOOTER_REGISTER_TYPE = "9";

    private SibsIncommingPaymentFileHeader header;

    private List<SibsIncommingPaymentFileDetailLine> detailLines;

    private SibsIncommingPaymentFileFooter footer;

    private String filename;

    public SibsIncommingPaymentFile(String filename, SibsIncommingPaymentFileHeader header,
            SibsIncommingPaymentFileFooter footer, List<SibsIncommingPaymentFileDetailLine> detailLines) {
        this.filename = filename;
        this.header = header;
        this.footer = footer;
        this.detailLines = detailLines;

        checkIfDetailLinesTotalAmountMatchesFooterTotalAmount();
    }

    private void checkIfDetailLinesTotalAmountMatchesFooterTotalAmount() {
        BigDecimal totalEntriesAmount = BigDecimal.ZERO;
        for (final SibsIncommingPaymentFileDetailLine detailLine : getDetailLines()) {
            totalEntriesAmount = totalEntriesAmount.add(detailLine.getAmount());
        }

        if (!TreasuryConstants.isEqual(totalEntriesAmount, footer.getTransactionsTotalAmount())) {
            throw new RuntimeException("Footer total amount does not match detail lines total amount");
        }

    }
    
    public static SibsIncommingPaymentFile parse(String filename, InputStream stream) throws IOException {
        return parse(filename, IOUtils.toByteArray(stream));
    }
    
    public static SibsIncommingPaymentFile parse(final String filename, byte[] content) {
        SibsIncommingPaymentFileHeader header = null;
        SibsIncommingPaymentFileFooter footer = null;
        final List<SibsIncommingPaymentFileDetailLine> detailLines = new ArrayList<SibsIncommingPaymentFileDetailLine>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(content)));

        try {
            String line = reader.readLine();
            while (line != null) {

                if (isHeader(line)) {
                    header = SibsIncommingPaymentFileHeader.buildFrom(line);
                } else if (isDetail(line)) {
                    detailLines.add(SibsIncommingPaymentFileDetailLine.buildFrom(line));
                } else if (isFooter(line)) {
                    footer = SibsIncommingPaymentFileFooter.buildFrom(line);
                } else {
                    throw new RuntimeException("Unknown sibs incomming payment file line type");
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new SibsIncommingPaymentFile(filename, header, footer, detailLines);

    }

    private static boolean isFooter(String line) {
        return line.startsWith(FOOTER_REGISTER_TYPE);
    }

    private static boolean isHeader(String line) {
        return line.startsWith(HEADER_REGISTER_TYPE);
    }

    private static boolean isDetail(String line) {
        return line.startsWith(DETAIL_REGISTER_TYPE);
    }

    public List<SibsIncommingPaymentFileDetailLine> getDetailLines() {
        return Collections.unmodifiableList(detailLines);
    }

    public SibsIncommingPaymentFileFooter getFooter() {
        return footer;
    }

    public SibsIncommingPaymentFileHeader getHeader() {
        return header;
    }

    public String getFilename() {
        return filename;
    }

    public LocalDate getWhenProcessedBySibs() {
        return getHeader().getWhenProcessedBySibs();
    }

    public Integer getVersion() {
        return getHeader().getVersion();
    }

}
