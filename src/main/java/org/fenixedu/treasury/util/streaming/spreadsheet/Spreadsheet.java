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
package org.fenixedu.treasury.util.streaming.spreadsheet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public interface Spreadsheet {

    public static final int ROWS_IN_MEMORY = 100;

    public ExcelSheet[] getSheets();

    public static byte[] buildSpreadsheetContent(final Spreadsheet spreadsheet, final IErrorsLog errorsLog) {

        final ByteArrayOutputStream output = new ByteArrayOutputStream();

        try {
            buildSpreadsheetContent(spreadsheet, errorsLog, output);
        } finally {
            try {
                output.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return output.toByteArray();
    }

    public static void buildSpreadsheetContent(final Spreadsheet spreadsheet, final IErrorsLog errorsLog,
            OutputStream outputStream) {
        final SXSSFWorkbook wb = new SXSSFWorkbook(ROWS_IN_MEMORY);

        for (ExcelSheet sheet : spreadsheet.getSheets()) {
            final Sheet sh = wb.createSheet(sheet.getName());
            final Row row = sh.createRow(0);

            final Font headerFont = wb.createFont();
            headerFont.setBold(true);

            if (row.getRowStyle() == null) {
                row.setRowStyle(wb.createCellStyle());
            }

            row.getRowStyle().setFont(headerFont);

            final String[] headers = sheet.getHeaders();
            for (int i = 0; i < headers.length; i++) {
                final Cell cell = row.createCell(i);
                cell.setCellType(Cell.CELL_TYPE_STRING);
                cell.setCellValue(headers[i]);
            }

            sheet.getRows().forEach(r -> r.writeCellValues(sh.createRow(sh.getLastRowNum() + 1), errorsLog));
        }

        try {
            File tempFile =
                    File.createTempFile("treasury-spreadsheet" + wb.hashCode() + "-" + System.currentTimeMillis(), ".xlsx");

            try (FileOutputStream output = new FileOutputStream(tempFile)) {
                wb.write(output);
            }

            try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
                IOUtils.copy(fileInputStream, outputStream);
            } finally {
                tempFile.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
        }
    }

}
