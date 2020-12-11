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
package org.fenixedu.treasury.domain.paymentcodes;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import org.apache.poi.ss.usermodel.Row;
import org.fenixedu.treasury.services.payments.sibs.SIBSImportationLineDTO;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.fenixedu.treasury.util.streaming.spreadsheet.IErrorsLog;
import org.fenixedu.treasury.util.streaming.spreadsheet.SpreadsheetRow;

public class SibsSpreadsheetRowReportBean implements SpreadsheetRow {

    // @formatter:off
    public static String[] SPREADSHEET_HEADERS = { 
            treasuryBundle("label.SibsReportFile.whenProcessedBySibs"),
            treasuryBundle("label.SibsReportFile.filename"),
            treasuryBundle("label.SibsReportFile.transactionsTotalAmount"),
            treasuryBundle("label.SibsReportFile.totalCost"),
            treasuryBundle("label.SibsReportFile.fileVersion"),
            treasuryBundle("label.SibsReportFile.sibsTransactionId"),
            treasuryBundle("label.SibsReportFile.transactionTotalAmount"),
            treasuryBundle("label.SibsReportFile.paymentCode"),
            treasuryBundle("label.SibsReportFile.transactionWhenRegistered"),
            treasuryBundle("label.SibsReportFile.studentNumber"),
            treasuryBundle("label.SibsReportFile.personName"),
            treasuryBundle("label.SibsReportFile.description")
            /* TODO: Appears to be empty. Check if it is needed
            ,
            Constants.bundle("label.SibsReportFile.transactionDescription"),
            Constants.bundle("label.SibsReportFile.transactionAmount") 
            */ };
    // @formatter:off
    
    private SIBSImportationLineDTO line;

    public SibsSpreadsheetRowReportBean(final SIBSImportationLineDTO line) {
        this.line = line;
    }
    
    @Override
    public void writeCellValues(final Row row, final IErrorsLog errorsLog) {
        int i = 0;
        
        try {
            row.createCell(i++).setCellValue(line.getWhenProcessedBySibs().toString("yyyy-MM-dd HH:mm:ss"));
            row.createCell(i++).setCellValue(line.getFilename());
            row.createCell(i++).setCellValue(line.getTransactionsTotalAmount().toPlainString());
            row.createCell(i++).setCellValue(line.getTotalCost().toPlainString());
            row.createCell(i++).setCellValue(line.getFileVersion());
            row.createCell(i++).setCellValue(line.getSibsTransactionId());
            row.createCell(i++).setCellValue(line.getTransactionTotalAmount().toPlainString());
            row.createCell(i++).setCellValue(line.getCode());
            row.createCell(i++).setCellValue(line.getTransactionWhenRegistered().toString("yyyy-MM-dd HH:mm:ss"));
            row.createCell(i++).setCellValue(line.getStudentNumber());
            row.createCell(i++).setCellValue(line.getPersonName());
            row.createCell(i++).setCellValue(line.getDescription());
            
            return;
        } catch (final Exception e) {
            e.printStackTrace();
            row.createCell(i++).setCellValue(treasuryBundle("error.SibsSpreadsheetRowReportBean.report.generation.verify.line"));
        }
        
    }

}
