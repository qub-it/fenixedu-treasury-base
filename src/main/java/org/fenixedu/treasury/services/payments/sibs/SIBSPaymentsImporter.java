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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.SibsInputFile;
import org.fenixedu.treasury.domain.paymentcodes.SibsReportFile;
import org.fenixedu.treasury.domain.paymentcodes.SibsTransactionDetail;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFile;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFileDetailLine;
import org.joda.time.YearMonthDay;

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

        public void addMessage(String message, String... args) {
            actionMessages.add(org.fenixedu.treasury.util.Constants.bundle(message, args));
        }

        public void addError(String message, String... args) {
            errorMessages.add(org.fenixedu.treasury.util.Constants.bundle(message, args));
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

//    private PaymentReferenceCode getPaymentReferenceCode(final FinantialInstitution finantialInstitution, final String code,
//            ProcessResult result) {
//
//        return PaymentReferenceCode.findByReferenceCode(code, finantialInstitution).findFirst().orElse(null);
//    }

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
                final SIBSImportationFileDTO reportDTO =
                        new SIBSImportationFileDTO(sibsFile, inputFile.getFinantialInstitution());
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
                    final SettlementNote settlementNote =
                            processCode(detailLine, loggedUsername, processResult, inputFile.getFinantialInstitution(),
                                    inputFile.getFilename().replace("\\.inp", ""), sibsFile.getWhenProcessedBySibs(), reportFile);

                    if (settlementNote != null) {
                        processResult.addMessage(detailLine.getCode() + " ["
                                + inputFile.getFinantialInstitution().getCurrency().getValueFor(detailLine.getAmount()) + "] => "
                                + settlementNote.getUiDocumentNumber());
//                        if (settlementNote.getAdvancedPaymentCreditNote() != null) {
//                            processResult.addMessage("label.manager.SIBS.advancedPayment.registered",
//                                    settlementNote.getUiDocumentNumber());
//                        }
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

    public ProcessResult processSIBSPaymentFiles(final SibsIncommingPaymentFile sibsFile,
            final FinantialInstitution finantialInstitution) throws IOException {
        // HACK:    Avoid concurrent and duplicated processing file
        synchronized (SIBSPaymentsImporter.class) {
            ProcessResult result = new ProcessResult();

            if (StringUtils.endsWithIgnoreCase(sibsFile.getFilename(), PAYMENT_FILE_EXTENSION)) {
                result.addMessage("label.manager.SIBS.processingFile", sibsFile.getFilename());
                try {
                    processFile(sibsFile, finantialInstitution, result);
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

    private void processFile(SibsIncommingPaymentFile sibsFile, final FinantialInstitution finantialInstitution,
            ProcessResult processResult) {
        final String responsibleUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

        processResult.addMessage("label.manager.SIBS.linesFound", String.valueOf(sibsFile.getDetailLines().size()));
        processResult.addMessage("label.manager.SIBS.startingProcess");

        processResult.addMessage("label.manager.SIBS.creatingReport");

        SibsReportFile reportFile = null;
        try {
            final SIBSImportationFileDTO reportDTO = new SIBSImportationFileDTO(sibsFile, finantialInstitution);
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
                final SettlementNote settlementNote = processCode(detailLine, responsibleUsername, processResult, finantialInstitution,
                        sibsFile.getFilename().replace("\\.inp", ""), sibsFile.getWhenProcessedBySibs(), reportFile);

                if (settlementNote != null) {
                    processResult.addMessage(
                            detailLine.getCode() + " [" + finantialInstitution.getCurrency().getValueFor(detailLine.getAmount())
                                    + "] => " + settlementNote.getUiDocumentNumber());
//                    if (settlementNote.getAdvancedPaymentCreditNote() != null) {
//                        processResult.addMessage("label.manager.SIBS.advancedPayment.registered",
//                                settlementNote.getUiDocumentNumber());
//                    }
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

    @Atomic
    protected SettlementNote processCode(SibsIncommingPaymentFileDetailLine detailLine, final String responsibleUsername, ProcessResult result,
            FinantialInstitution finantialInstitution, final String sibsImportationFile, YearMonthDay whenProcessedBySibs,
            final SibsReportFile reportFile) throws Exception {

        final PaymentReferenceCode paymentCode = getPaymentCode(detailLine.getCode(), finantialInstitution);

        if (paymentCode == null) {
            result.addMessage("error.manager.SIBS.codeNotFound", detailLine.getCode());
            return null;
        }

        final PaymentReferenceCode codeToProcess = getPaymentCodeToProcess(paymentCode, result);

        if (codeToProcess.getState() == PaymentReferenceCodeStateType.ANNULLED) {
            result.addMessage("warning.manager.SIBS.anulledCode", codeToProcess.getReferenceCode());
        }

        if (!codeToProcess.isNew()) {
            if (SibsTransactionDetail.isReferenceProcessingDuplicate(codeToProcess.getReferenceCode(),
                    codeToProcess.getPaymentCodePool().getEntityReferenceCode(), detailLine.getWhenOccuredTransaction())) {
                result.addMessage("error.manager.SIBS.codeAlreadyProcessed.duplicated", codeToProcess.getReferenceCode());
                return null;
            } else {
                if (codeToProcess.isProcessed()) {
                    result.addMessage("warning.manager.SIBS.codeAlreadyProcessed", codeToProcess.getReferenceCode());
                }
            }
        }
        
        if(codeToProcess.getTargetPayment() == null) {
            result.addMessage("error.manager.SIBS.code.exists.but.not.attributed.to.any.target", detailLine.getCode());
            return null;
        }
        
        if(codeToProcess.getTargetPayment().getReferencedCustomers().size() > 1) {
            result.addMessage("warning.manager.SIBS.referenced.multiple.payor.entities", codeToProcess.getReferenceCode());
        }

        final SettlementNote settlementNote = codeToProcess.processPayment(responsibleUsername, detailLine.getAmount(),
                detailLine.getWhenOccuredTransaction(), detailLine.getSibsTransactionId(), sibsImportationFile,
                whenProcessedBySibs.toLocalDate().toDateTimeAtStartOfDay(), reportFile);

        if (settlementNote != null) {
            //Add the new SettlementNote to the TargetPayment
            codeToProcess.getTargetPayment().addSettlementNotes(settlementNote);
        }

        return settlementNote;
    }

    /**
     * Copied from head
     */
    private PaymentReferenceCode getPaymentCodeToProcess(final PaymentReferenceCode paymentCode, ProcessResult result) {

        final PaymentReferenceCode codeToProcess;
        codeToProcess = paymentCode;
        return codeToProcess;
    }

    /**
     * Copied from head
     */
    private PaymentReferenceCode getPaymentCode(final String code, FinantialInstitution finantialInstitution) {

        return PaymentReferenceCode.findByReferenceCode(code, finantialInstitution)
                .filter(x -> x.getPaymentCodePool().getActive() == true).findFirst().orElse(null);
    }

}
