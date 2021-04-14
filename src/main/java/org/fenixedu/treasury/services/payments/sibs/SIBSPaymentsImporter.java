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
package org.fenixedu.treasury.services.payments.sibs;

import static java.lang.String.join;
import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

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
import org.fenixedu.treasury.domain.paymentcodes.SibsTransactionDetail;
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

            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

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
        final String responsibleUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

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
