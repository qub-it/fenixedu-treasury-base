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
package org.fenixedu.treasury.services.payments.sibs;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFile;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.joda.time.DateTime;

public class SIBSImportationFileDTO {

    protected DateTime whenProcessedBySibs;
    protected String filename;
    protected BigDecimal transactionsTotalAmount;
    protected BigDecimal totalCost;
    protected Integer fileVersion;
    private String sibsEntityCode;

    protected List<SIBSImportationLineDTO> lines;

    public SIBSImportationFileDTO(final SibsIncommingPaymentFile sibsIncomingPaymentFile) {
        setWhenProcessedBySibs(sibsIncomingPaymentFile.getHeader().getWhenProcessedBySibs().toDateTimeAtMidnight());
        setFilename(sibsIncomingPaymentFile.getFilename());
        setTransactionsTotalAmount(sibsIncomingPaymentFile.getFooter().getTransactionsTotalAmount());
        setTotalCost(sibsIncomingPaymentFile.getFooter().getTotalCost());
        setFileVersion(sibsIncomingPaymentFile.getHeader().getVersion());
        setSibsEntityCode(sibsIncomingPaymentFile.getHeader().getEntityCode());

        setLines(generateLines(sibsIncomingPaymentFile));
    }

    protected List<SIBSImportationLineDTO> generateLines(final SibsIncommingPaymentFile sibsIncomingPaymentFile) {

        ArrayList<SIBSImportationLineDTO> result = new ArrayList<SIBSImportationLineDTO>();
        for (SibsIncommingPaymentFileDetailLine dto : sibsIncomingPaymentFile.getDetailLines()) {
            result.add(new SIBSImportationLineDTO(SIBSImportationFileDTO.this, dto));
        }
        return result;
    }

    public List<SIBSImportationLineDTO> getLines() {
        return lines;
    }

    public void setLines(List<SIBSImportationLineDTO> lines) {
        this.lines = lines;
    }

    public DateTime getWhenProcessedBySibs() {
        return whenProcessedBySibs;
    }

    public void setWhenProcessedBySibs(final DateTime whenProcessedBySibs) {
        this.whenProcessedBySibs = whenProcessedBySibs;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public BigDecimal getTransactionsTotalAmount() {
        return transactionsTotalAmount;
    }

    public void setTransactionsTotalAmount(BigDecimal transactionsTotalAmount) {
        this.transactionsTotalAmount = transactionsTotalAmount;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(BigDecimal totalCost) {
        this.totalCost = totalCost;
    }

    public Integer getFileVersion() {
        return fileVersion;
    }

    public void setFileVersion(final Integer fileVersion) {
        this.fileVersion = fileVersion;
    }

    public String getSibsEntityCode() {
        return sibsEntityCode;
    }
    
    public void setSibsEntityCode(String sibsEntityId) {
        this.sibsEntityCode = sibsEntityId;
    }
}
