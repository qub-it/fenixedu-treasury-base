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
