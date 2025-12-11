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

import static java.lang.String.join;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.SibsInputFile;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentCodeTransaction;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentRequest;
import org.fenixedu.treasury.domain.paymentcodes.SibsReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.SibsReportFile;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFile;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.joda.time.LocalDate;

import pt.ist.fenixframework.Atomic;

public class SIBSPaymentsImporter {

    static private final String PAYMENT_FILE_EXTENSION = "INP";

    public class ProcessResult {

        List<String> actionMessages = new ArrayList<String>();
        List<String> errorMessages = new ArrayList<String>();
        SibsReportFile reportFile;

        public List<String> getActionMessages() {
            return actionMessages;
        }

        public List<String> getErrorMessages() {
            return errorMessages;
        }

//        private final TreasuryBaseController baseController;
        private boolean processFailed = false;

        public void addStringMessage(String stringMessage) {
            actionMessages.add(stringMessage);
        }

        public void addMessage(String message, String... args) {
            actionMessages.add(treasuryBundle(message, args));
        }

        public void addError(String message, String... args) {
            errorMessages.add(treasuryBundle(message, args));
            reportFailure();
        }

        protected void reportFailure() {
            processFailed = true;
        }

        public boolean hasFailed() {
            return processFailed;
        }

        public void setReportFile(SibsReportFile reportFile) {
            this.reportFile = reportFile;
        }

        public SibsReportFile getReportFile() {
            return reportFile;
        }
    }

    public String readSibsEntityCode(String filename, byte[] content) {

        InputStream fileInputStream = null;
        try {
            fileInputStream = new ByteArrayInputStream(content);

            final SibsIncommingPaymentFile sibsFile = SibsIncommingPaymentFile.parse(filename, fileInputStream);
            final SIBSImportationFileDTO reportDTO = new SIBSImportationFileDTO(sibsFile);

            return reportDTO.getSibsEntityCode();
        } catch (Exception e) {
            throw new TreasuryDomainException(e, "error.manager.SIBS.getSibsEntityCode.invalid");
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public ProcessResult processSIBSPaymentFiles(SibsInputFile inputFile) throws IOException {
        // HACK:    Avoid concurrent and duplicated processing file
        synchronized (SIBSPaymentsImporter.class) {
            ProcessResult result = new ProcessResult();

            if (StringUtils.endsWithIgnoreCase(inputFile.getFilename(), PAYMENT_FILE_EXTENSION)) {
                result.addMessage("label.manager.SIBS.processingFile", inputFile.getFilename());
                try {
                    processFile(inputFile, result);
                } catch (FileNotFoundException e) {
                    throw new TreasuryDomainException("error.manager.SIBS.zipException", getMessage(e));
                } catch (IOException e) {
                    throw new TreasuryDomainException("error.manager.SIBS.IOException", getMessage(e));
                } catch (Exception e) {
                    throw new TreasuryDomainException("error.manager.SIBS.fileException", getMessage(e));
                } finally {
                }
            } else {
                throw new TreasuryDomainException("error.manager.SIBS.notSupportedExtension", inputFile.getFilename());
            }
            return result;
        }
    }

    protected String getMessage(Exception ex) {
        String message = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();

        message += "\n";
        for (StackTraceElement el : ex.getStackTrace()) {
            message = message + el.toString() + "\n";
        }
        return message;
    }

    private void processFile(SibsInputFile inputFile, ProcessResult processResult) throws IOException {
        processResult.addMessage("label.manager.SIBS.processingFile", inputFile.getFilename());

        InputStream fileInputStream = null;
        try {
            fileInputStream = inputFile.getStream();

            final String loggedUsername = org.fenixedu.treasury.util.TreasuryConstants.getAuthenticatedUsername();

            final SibsIncommingPaymentFile sibsFile = SibsIncommingPaymentFile.parse(inputFile.getFilename(), fileInputStream);

            processResult.addMessage("label.manager.SIBS.linesFound", String.valueOf(sibsFile.getDetailLines().size()));
            processResult.addMessage("label.manager.SIBS.startingProcess");

            processResult.addMessage("label.manager.SIBS.creatingReport");

            SibsReportFile reportFile = null;
            try {
                final SIBSImportationFileDTO reportDTO = new SIBSImportationFileDTO(sibsFile);
                reportFile = SibsReportFile.processSIBSIncommingFile(reportDTO);
                processResult.addMessage("label.manager.SIBS.reportCreated");
                processResult.setReportFile(reportFile);
            } catch (Exception ex) {
                ex.printStackTrace();
                processResult.addError("error.manager.SIBS.reportException", getMessage(ex));
            }

            if (reportFile == null) {
                processResult.addError("error.manager.SIBS.report.not.created");
                return;
            }

            for (final SibsIncommingPaymentFileDetailLine detailLine : sibsFile.getDetailLines()) {

                try {
                    final Set<SettlementNote> settlementNoteSet =
                            processCode(sibsFile.getHeader().getEntityCode(), detailLine, loggedUsername, processResult,
                                    inputFile.getFilename().replace("\\.inp", ""), sibsFile.getWhenProcessedBySibs(), reportFile);

                    if (settlementNoteSet != null && !settlementNoteSet.isEmpty()) {
                        String settlementsDescription = join(", ",
                                settlementNoteSet.stream().map(s -> settlementNoteDescription(s)).collect(Collectors.toSet()));
                        processResult.addStringMessage(String.format("%s [%s] => %s", detailLine.getCode(),
                                Currency.getValueWithScale(detailLine.getAmount()), settlementsDescription));
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    processResult.addError("error.manager.SIBS.processException", detailLine.getCode(), getMessage(e));
                }
            }

            if (processResult.hasFailed()) {
                processResult.addError("error.manager.SIBS.nonProcessedCodes");
            }

            inputFile.updateLastProcessExecutionDate();

            processResult.addMessage("label.manager.SIBS.done");

        } finally {
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        }
    }

    public ProcessResult processSIBSPaymentFiles(final SibsIncommingPaymentFile sibsFile) throws IOException {
        // HACK:    Avoid concurrent and duplicated processing file
        synchronized (SIBSPaymentsImporter.class) {
            ProcessResult result = new ProcessResult();

            if (StringUtils.endsWithIgnoreCase(sibsFile.getFilename(), PAYMENT_FILE_EXTENSION)) {
                result.addMessage("label.manager.SIBS.processingFile", sibsFile.getFilename());
                try {
                    processFile(sibsFile, result);
                } catch (Exception e) {
                    throw new TreasuryDomainException("error.manager.SIBS.fileException", getMessage(e));
                } finally {
                }
            } else {
                throw new TreasuryDomainException("error.manager.SIBS.notSupportedExtension", sibsFile.getFilename());
            }
            return result;
        }
    }

    private void processFile(SibsIncommingPaymentFile sibsFile, ProcessResult processResult) {
        final String responsibleUsername = org.fenixedu.treasury.util.TreasuryConstants.getAuthenticatedUsername();

        processResult.addMessage("label.manager.SIBS.linesFound", String.valueOf(sibsFile.getDetailLines().size()));
        processResult.addMessage("label.manager.SIBS.startingProcess");

        processResult.addMessage("label.manager.SIBS.creatingReport");

        SibsReportFile reportFile = null;
        try {
            final SIBSImportationFileDTO reportDTO = new SIBSImportationFileDTO(sibsFile);
            reportFile = SibsReportFile.processSIBSIncommingFile(reportDTO);
            processResult.addMessage("label.manager.SIBS.reportCreated");
            processResult.setReportFile(reportFile);

        } catch (Exception ex) {
            ex.printStackTrace();
            processResult.addError("error.manager.SIBS.reportException", getMessage(ex));
        }

        if (reportFile == null) {
            processResult.addError("error.manager.SIBS.report.not.created");
            return;
        }

        for (final SibsIncommingPaymentFileDetailLine detailLine : sibsFile.getDetailLines()) {

            try {
                final Set<SettlementNote> settlementNoteSet =
                        processCode(sibsFile.getHeader().getEntityCode(), detailLine, responsibleUsername, processResult,
                                sibsFile.getFilename().replace("\\.inp", ""), sibsFile.getWhenProcessedBySibs(), reportFile);

                if (!settlementNoteSet.isEmpty()) {
                    String settlementsDescription = join(", ",
                            settlementNoteSet.stream().map(s -> settlementNoteDescription(s)).collect(Collectors.toSet()));

                    processResult.addStringMessage(String.format("%s [%s] => %s", detailLine.getCode(),
                            Currency.getValueWithScale(detailLine.getAmount()), settlementsDescription));
                }
            } catch (Exception e) {
                e.printStackTrace();
                processResult.addError("error.manager.SIBS.processException", detailLine.getCode(), getMessage(e));
            }
        }

        if (processResult.hasFailed()) {
            processResult.addError("error.manager.SIBS.nonProcessedCodes");
        }

        processResult.addMessage("label.manager.SIBS.done");
    }

    private String settlementNoteDescription(SettlementNote s) {
        if (s.getAdvancedPaymentCreditNote() != null) {
            return String.format("%s (%s)", s.getUiDocumentNumber(), s.getAdvancedPaymentCreditNote().getUiDocumentNumber());
        } else {
            return s.getUiDocumentNumber();
        }
    }

    protected Set<SettlementNote> processCode(String sibsEntityCode, SibsIncommingPaymentFileDetailLine detailLine,
            String responsibleUsername, ProcessResult result, String sibsImportationFile, LocalDate whenProcessedBySibs,
            SibsReportFile reportFile) throws Exception {

        final SibsReferenceCode codeToProcess = getPaymentCode(sibsEntityCode, detailLine.getCode());

        if (codeToProcess == null) {
            result.addMessage("error.manager.SIBS.codeNotFound", sibsEntityCode, detailLine.getCode());
            return Collections.emptySet();
        }

        if (codeToProcess.isInAnnuledState()) {
            result.addMessage("warning.manager.SIBS.anulledCode", codeToProcess.getReferenceCode());
        }

        if (SibsPaymentCodeTransaction.isReferenceProcessingDuplicate(sibsEntityCode, codeToProcess.getReferenceCode(),
                detailLine.getWhenOccuredTransaction())) {
            result.addMessage("error.manager.SIBS.codeAlreadyProcessed.duplicated", codeToProcess.getReferenceCode());
            return Collections.emptySet();
        } else if (codeToProcess.isInPaidState()) {
            result.addMessage("warning.manager.SIBS.codeAlreadyProcessed", codeToProcess.getReferenceCode());
        }

        if (codeToProcess.getSibsPaymentRequest() == null) {
            result.addMessage("error.manager.SIBS.code.exists.but.not.attributed.to.any.target", detailLine.getCode());
            return Collections.emptySet();
        }

        if (codeToProcess.getSibsPaymentRequest().getDebitEntriesSet().isEmpty()
                && codeToProcess.getSibsPaymentRequest().getInstallmentsSet().isEmpty()) {
            result.addMessage("error.manager.SIBS.code.exists.but.not.attributed.to.any.target", detailLine.getCode());
            return Collections.emptySet();
        }

        if (codeToProcess.getSibsPaymentRequest().getReferencedCustomers().size() > 1) {
            result.addMessage("warning.manager.SIBS.referenced.multiple.payor.entities", codeToProcess.getReferenceCode());
        }

        return createSettlementNoteForPaymentReferenceCode(detailLine, sibsImportationFile, whenProcessedBySibs, reportFile,
                codeToProcess.getSibsPaymentRequest());
    }

    @Atomic
    private Set<SettlementNote> createSettlementNoteForPaymentReferenceCode(SibsIncommingPaymentFileDetailLine detailLine,
            String sibsImportationFilename, LocalDate whenProcessedBySibs, SibsReportFile reportFile,
            SibsPaymentRequest codeToProcess) {
        PaymentTransaction paymentTransaction = codeToProcess.processPayment(detailLine.getAmount(),
                detailLine.getWhenOccuredTransaction(), detailLine.getSibsTransactionId(), sibsImportationFilename, null,
                whenProcessedBySibs.toDateTimeAtStartOfDay(), reportFile, false);

        return paymentTransaction.getSettlementNotesSet();
    }

    public static SibsReferenceCode getPaymentCode(final String sibsEntityCode, final String code) {
        return SibsPaymentCodePool.find(sibsEntityCode).flatMap(d -> d.getSibsReferenceCodesSet().stream())
                .map(SibsReferenceCode.class::cast).filter(i -> code.equalsIgnoreCase(i.getReferenceCode())).findFirst()
                .orElse(null);
    }

}
