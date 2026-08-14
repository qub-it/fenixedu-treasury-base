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

import com.qubit.terra.framework.services.ServiceProvider;
import com.qubit.terra.framework.services.fileSupport.FileDescriptor;
import com.qubit.terra.framework.services.fileSupport.FileManager;
import org.apache.commons.lang3.StringUtils;
import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.domain.TreasuryFile;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibs.SIBSImportationFileDTO;
import org.fenixedu.treasury.services.payments.sibs.SIBSPaymentsImporter.ProcessResult;
import org.fenixedu.treasury.util.streaming.spreadsheet.ExcelSheet;
import org.fenixedu.treasury.util.streaming.spreadsheet.Spreadsheet;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.stream.Stream;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

public class SibsReportFile extends SibsReportFile_Base implements IGenericFile {

    public static final Comparator<SibsReportFile> COMPARATOR_BY_CREATION_DATE = (o1, o2) -> {
        int c = o1.getCreationDate().compareTo(o2.getCreationDate());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static final String CONTENT_TYPE = "text/plain";
    public static final String FILE_EXTENSION = ".idm";

    protected SibsReportFile() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    protected SibsReportFile(String sibsEntityCode, final DateTime whenProcessedBySibs, final BigDecimal transactionsTotalAmount,
            final BigDecimal totalCost, final String displayName, final String fileName, final byte[] content) {
        this();
        this.init(sibsEntityCode, whenProcessedBySibs, transactionsTotalAmount, totalCost, displayName, fileName, content);

        checkRules();
    }

    protected void init(String sibsEntityCode, DateTime whenProcessedBySibs, BigDecimal transactionsTotalAmount,
            BigDecimal totalCost, String displayName, String fileName, byte[] content) {

        FileManager fileManager = ServiceProvider.getService(FileManager.class);

        FileDescriptor fileDescriptor = fileManager.createFile(fileName, content.length, CONTENT_TYPE, content);
        setFileDescriptorId(fileDescriptor.getId());

        setSibsEntityCode(sibsEntityCode);
        setWhenProcessedBySibs(whenProcessedBySibs);
        setTransactionsTotalAmount(transactionsTotalAmount);
        setTotalCost(totalCost);

        checkRules();
    }

    private void checkRules() {
    }

    @Atomic
    public void edit(final DateTime whenProcessedBySibs, final BigDecimal transactionsTotalAmount, final BigDecimal totalCost) {
        setWhenProcessedBySibs(whenProcessedBySibs);
        setTransactionsTotalAmount(transactionsTotalAmount);
        setTotalCost(totalCost);

        checkRules();
    }

    public boolean isDeletable() {
        return getReferenceCodesSet().isEmpty() && getSibsTransactionsSet().isEmpty();
    }

    @Override
    @Atomic
    public void delete() {
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        FileManager fileManager = ServiceProvider.getService(FileManager.class);

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.SibsReportFile.cannot.delete");
        }

        setDomainRoot(null);

        if (StringUtils.isNotEmpty(getFileDescriptorId())) {
            fileManager.delete(getFileDescriptorId());
        }

        if (getTreasuryFile() != null) {
            services.deleteFile(this);
        }

        super.deleteDomainObject();
    }

    public static Stream<SibsReportFile> findAll() {
        return FenixFramework.getDomainRoot().getSibsReportFilesSet().stream();
    }

    public static Stream<SibsReportFile> findByWhenProcessedBySibs(final LocalDate whenProcessedBySibs) {
        return findAll().filter(i -> whenProcessedBySibs.equals(i.getWhenProcessedBySibs()));
    }

    public static Stream<SibsReportFile> findByTransactionsTotalAmount(final BigDecimal transactionsTotalAmount) {
        return findAll().filter(i -> transactionsTotalAmount.equals(i.getTransactionsTotalAmount()));
    }

    public static Stream<SibsReportFile> findByTotalCost(final BigDecimal totalCost) {
        return findAll().filter(i -> totalCost.equals(i.getTotalCost()));
    }

    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControlAPI.isBackOfficeMember(username);
    }

    @Atomic
    public void updateLogMessages(ProcessResult result) {
        StringBuilder build = new StringBuilder();
        for (String s : result.getErrorMessages()) {
            build.append(s + "\n");
        }
        this.setErrorLog(build.toString());
        build = new StringBuilder();
        for (String s : result.getActionMessages()) {
            build.append(s + "\n");
        }
        this.setInfoLog(build.toString());
    }

    @Atomic
    public static SibsReportFile create(String sibsEntityCode, DateTime whenProcessedBySibs, BigDecimal transactionsTotalAmount,
            BigDecimal totalCost, String displayName, String fileName, byte[] content) {
        return new SibsReportFile(sibsEntityCode, whenProcessedBySibs, transactionsTotalAmount, totalCost, displayName, fileName,
                content);

    }

    protected static byte[] buildContentFor(final SIBSImportationFileDTO reportFileDTO) {

        Stream<SibsSpreadsheetRowReportBean> lines =
                reportFileDTO.getLines().stream().map(l -> new SibsSpreadsheetRowReportBean(l));

        return Spreadsheet.buildSpreadsheetContent(new Spreadsheet() {

            @Override
            public ExcelSheet[] getSheets() {
                return new ExcelSheet[] { ExcelSheet.create(treasuryBundle("label.SibsReportFile.spreadsheet.name"),
                        SibsSpreadsheetRowReportBean.SPREADSHEET_HEADERS, lines) };
            }
        }, null);

    }

    protected static String filenameFor(final SIBSImportationFileDTO reportFileDTO) {
        final String date = new DateTime().toString("yyyyMMddHHmm");
        return "Relatorio-SIBS-" + date + ".xlsx";
    }

    protected static String displayNameFor(final SIBSImportationFileDTO reportFileDTO) {
        final String date = new DateTime().toString("yyyyMMddHHmm");
        return "Relatorio-SIBS-" + date;
    }

    @Atomic
    public static SibsReportFile processSIBSIncommingFile(final SIBSImportationFileDTO reportDTO) {
        byte[] content = buildContentFor(reportDTO);

        SibsReportFile result = SibsReportFile.create(reportDTO.getSibsEntityCode(), reportDTO.getWhenProcessedBySibs(),
                reportDTO.getTransactionsTotalAmount(), reportDTO.getTotalCost(), displayNameFor(reportDTO),
                filenameFor(reportDTO), content);

        return result;
    }

    // 2026-08-14 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public String getFileId() {
        return super.getFileId();
    }

    // 2026-08-14 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public void setFileId(String fileId) {
        super.setFileId(fileId);
    }

    // 2026-08-14 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public TreasuryFile getTreasuryFile() {
        return super.getTreasuryFile();
    }

    // 2026-08-14 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public void setTreasuryFile(TreasuryFile treasuryFile) {
        super.setTreasuryFile(treasuryFile);
    }

    @Override
    public byte[] getContent() {
        return getFileDescriptor().getContent();
    }

    @Override
    public long getSize() {
        return getFileDescriptor().getSize();
    }

    @Override
    public String getFilename() {
        return getFileDescriptor().getName();
    }

    @Override
    public String getContentType() {
        return getFileDescriptor().getContentType();
    }

    @Override
    public InputStream getStream() {
        return getFileDescriptor().getReadStream();
    }

    private FileDescriptor getFileDescriptor() {
        return ServiceProvider.getService(FileManager.class).getFileDescriptor(getFileDescriptorId());
    }
}
