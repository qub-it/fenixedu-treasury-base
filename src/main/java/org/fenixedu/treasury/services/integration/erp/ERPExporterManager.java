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
package org.fenixedu.treasury.services.integration.erp;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.CreditNote;
import org.fenixedu.treasury.domain.document.ERPCustomerFieldsBean;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.document.reimbursement.ReimbursementProcessStateLog;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.domain.integration.ERPExportOperation;
import org.fenixedu.treasury.domain.integration.IntegrationOperationLogBean;
import org.fenixedu.treasury.domain.integration.OperationFile;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.ERPExternalServiceImplementation.ReimbursementStateBean;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationInput;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationOutput;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

public class ERPExporterManager {

    private static ERPExporterManager _INSTANCE = null;

    public static final ERPExporterManager getInstance() {
        if (_INSTANCE == null) {
            _INSTANCE = new ERPExporterManager();
        }

        return _INSTANCE;
    }

    private static Logger logger = LoggerFactory.getLogger(ERPExporterManager.class);

    private static final int WAIT_TRANSACTION_TO_FINISH_MS = 500;

    public static final Comparator<FinantialDocument> COMPARE_BY_DOCUMENT_TYPE = new Comparator<FinantialDocument>() {
        @Override
        public int compare(FinantialDocument o1, FinantialDocument o2) {
            if (o1.getFinantialDocumentType().equals(o2.getFinantialDocumentType())) {
                return o1.getUiDocumentNumber().compareTo(o2.getUiDocumentNumber());
            } else {
                if (o1.isDebitNote()) {
                    return -2;
                } else if (o1.isCreditNote()) {
                    return -1;
                } else if (o1.isSettlementNote()) {
                    return 1;
                }
            }
            return 0;
        }
    };

    public static String saftEncoding(final FinantialInstitution finantialInstitution) {
        final IERPExporter erpExporter =
                finantialInstitution.getErpIntegrationConfiguration().getERPExternalServiceImplementation().getERPExporter();

        return erpExporter.saftEncoding();
    }

    @Atomic
    public static String exportFinantialDocumentToXML(final FinantialDocument finantialDocument) {
        final FinantialInstitution finantialInstitution = finantialDocument.getDebtAccount().getFinantialInstitution();
        ERPConfiguration erpIntegrationConfiguration =
                finantialDocument.getDebtAccount().getFinantialInstitution().getErpIntegrationConfiguration();
        final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();

        if (TreasuryPlataformDependentServicesFactory.implementation()
                .getSaftExporterConfiguration(erpIntegrationConfiguration) != null) {
            ISaftExporterConfiguration saftExporterConfiguration = TreasuryPlataformDependentServicesFactory.implementation()
                    .getSaftExporterConfiguration(erpIntegrationConfiguration);
            List<FinantialDocument> documentsToExport = new ArrayList<>();
            documentsToExport.add(finantialDocument);
            documentsToExport = erpExporter.processCreditNoteSettlementsInclusion(documentsToExport);
            documentsToExport.stream()
                    .forEach(document -> validateDocumentWithERPConfiguration(document, erpIntegrationConfiguration));

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                saftExporterConfiguration.generateSaftForFinantialDocuments(documentsToExport, true, baos);

                return new String(baos.toByteArray(), saftExporterConfiguration.getEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            return erpExporter.exportFinantialDocumentToXML(finantialInstitution, Lists.newArrayList(finantialDocument));
        }

    }

    public static List<ERPExportOperation> exportPendingDocumentsForFinantialInstitution(
            final FinantialInstitution finantialInstitution) {
        return exportPendingDocumentsForFinantialInstitution(finantialInstitution, null);
    }

    public static List<ERPExportOperation> exportPendingDocumentsForFinantialInstitution(
            final FinantialInstitution finantialInstitution, Runnable runnable) {

        final IERPExporter erpExporter =
                finantialInstitution.getErpIntegrationConfiguration().getERPExternalServiceImplementation().getERPExporter();

        if (!finantialInstitution.getErpIntegrationConfiguration().getActive()) {
            return Lists.newArrayList();
        }

        Stream<FinantialDocument> stream = finantialInstitution.getFinantialDocumentsPendingForExportationSet().stream();

        if (isUsingSaftConfiguration(finantialInstitution)) {
            stream = applyAdditionalFilterForSaftConfiguration(stream);
        }

        final List<FinantialDocument> sortedDocuments = erpExporter.filterDocumentsToExport(stream);

        if (sortedDocuments.isEmpty()) {
            return Lists.newArrayList();
        }

        if (finantialInstitution.getErpIntegrationConfiguration().getExportOnlyRelatedDocumentsPerExport()) {
            final List<ERPExportOperation> result = Lists.newArrayList();

            while (!sortedDocuments.isEmpty()) {
                final FinantialDocument doc = sortedDocuments.iterator().next();

                //remove the related documents from the original Set
                sortedDocuments.remove(doc);

                // Limit exportation of documents in which customer has invalid addresses
                final Customer customer = doc.getDebtAccount().getCustomer();
                final List<String> errorMessages = Lists.newArrayList();
                if (!ERPCustomerFieldsBean.validateAddress(customer, errorMessages)) {
                    if (!doc.getErpExportOperationsSet().isEmpty()) {
                        continue;
                    }
                }
                if (runnable != null) {
                    runnable.run();
                }

                result.add(exportSingleDocument(doc));
            }

            return result;
        }

        // return Lists.newArrayList(erpExporter.exportFinantialDocumentToIntegration(finantialInstitution, sortedDocuments));
        return Lists.newArrayList();
    }

    private static boolean isUsingSaftConfiguration(final FinantialInstitution finantialInstitution) {
        return TreasuryPlataformDependentServicesFactory.implementation()
                .getSaftExporterConfiguration(finantialInstitution.getErpIntegrationConfiguration()) != null;
    }

    private static Stream<FinantialDocument> applyAdditionalFilterForSaftConfiguration(Stream<FinantialDocument> stream) {
        // TODO Auto-generated method stub
        return stream;
    }

    public static List<ReimbursementProcessStateLog> updatePendingReimbursementNotes(
            final FinantialInstitution finantialInstitution) {

        final List<SettlementNote> settlementNotes = FinantialDocument.find(FinantialDocumentType.findForReimbursementNote())
                .map(SettlementNote.class::cast).filter(s -> s.getDebtAccount().getFinantialInstitution() == finantialInstitution)
                .filter(s -> !s.isDocumentToExport()).filter(s -> s.isReimbursementPending()).collect(Collectors.toList());

        for (final SettlementNote note : settlementNotes) {

            try {
                ReimbursementProcessStateLog log = updateReimbursementState(note);

                logger.info("Reimbursement update %s => %s", note.getUiDocumentNumber(),
                        log.getReimbursementProcessStatusType().getCode());
            } catch (final Exception e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        }

        return null;
    }

    private static final int LIMIT = 200;

    public static List<ERPExportOperation> exportPendingDocumentsForDebtAccount(final DebtAccount debtAccount) {
        final FinantialInstitution finantialInstitution = debtAccount.getFinantialInstitution();

        final IERPExporter erpExporter = debtAccount.getFinantialInstitution().getErpIntegrationConfiguration()
                .getERPExternalServiceImplementation().getERPExporter();

        final List<FinantialDocument> sortedDocuments =
                erpExporter.filterDocumentsToExport(debtAccount.getFinantialDocumentsSet().stream());

        if (sortedDocuments.isEmpty()) {
            return Lists.newArrayList();
        }

        if (finantialInstitution.getErpIntegrationConfiguration().getExportOnlyRelatedDocumentsPerExport()) {
            final List<ERPExportOperation> result = Lists.newArrayList();

            int i = 0;
            while (!sortedDocuments.isEmpty()) {
                final FinantialDocument doc = sortedDocuments.iterator().next();

                //remove the related documents from the original Set
                sortedDocuments.remove(doc);

                result.add(exportSingleDocument(doc));

                /* For now limit to 200 finantial documents */
                if (++i >= LIMIT) {
                    System.out.println("ERPExporterManager: Limit " + LIMIT + " finantial documents.");
                    break;
                }
            }

            return result;
        }

        return Lists.newArrayList();
    }

    public static void scheduleSingleDocument(final FinantialDocument finantialDocument) {
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        services.scheduleDocumentForExportation(finantialDocument);
    }

    @Atomic(mode = TxMode.WRITE)
    public static ERPExportOperation exportSingleDocument(final FinantialDocument finantialDocument) {
        // ALTERAR
        final FinantialInstitution finantialInstitution = finantialDocument.getDebtAccount().getFinantialInstitution();
        final ERPConfiguration erpIntegrationConfiguration = finantialInstitution.getErpIntegrationConfiguration();

        final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();

        List<FinantialDocument> documentsToExport =
                erpExporter.filterDocumentsToExport(Collections.singletonList(finantialDocument).stream());

        if (documentsToExport.isEmpty()) {
            return null;
        }

        if (TreasuryPlataformDependentServicesFactory.implementation()
                .getSaftExporterConfiguration(erpIntegrationConfiguration) != null) {
            ISaftExporterConfiguration saftExporterConfiguration = TreasuryPlataformDependentServicesFactory.implementation()
                    .getSaftExporterConfiguration(erpIntegrationConfiguration);

            checkForUnsetDocumentSeriesNumberInDocumentsToExport(documentsToExport);

            if (!finantialInstitution.getErpIntegrationConfiguration().isIntegratedDocumentsExportationEnabled()) {
                // Filter documents already exported
                documentsToExport = documentsToExport.stream().filter(x -> x.isDocumentToExport()).collect(Collectors.toList());
            }

            final IntegrationOperationLogBean logBean = new IntegrationOperationLogBean();
            final ERPExportOperation operation =
                    ERPExportOperation.createSaftExportOperation(null, finantialInstitution, new DateTime());

            documentsToExport.forEach(document -> operation.addFinantialDocuments(document));
            try {
                documentsToExport = erpExporter.processCreditNoteSettlementsInclusion(documentsToExport);
                documentsToExport.stream()
                        .forEach(document -> validateDocumentWithERPConfiguration(document, erpIntegrationConfiguration));

                logBean.appendIntegrationLog(treasuryBundle("label.ERPExporter.starting.finantialdocuments.integration"));

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                saftExporterConfiguration.generateSaftForFinantialDocuments(documentsToExport, false, baos);

                logBean.appendIntegrationLog(treasuryBundle("label.ERPExporter.erp.xml.content.generated"));

                byte[] contents = baos.toByteArray();
                saveSaftContentToOperationFile(contents, operation);

                boolean success = sendDocumentsInformationToIntegration(finantialInstitution, contents, logBean);
                operation.getFinantialDocumentsSet().addAll(documentsToExport);
                operation.setSuccess(success);

            } catch (Exception ex) {
                writeError(operation, logBean, ex);
            } finally {
                logBean.appendIntegrationLog(treasuryBundle("label.ERPExporter.finished.finantialdocuments.integration"));
                operation.appendLog(logBean.getErrorLog(), logBean.getIntegrationLog(), logBean.getSoapInboundMessage(),
                        logBean.getSoapOutboundMessage());
            }

            return operation;
        } else {
            return erpExporter.exportFinantialDocumentToIntegration(finantialInstitution, documentsToExport);
        }
    }

    private static boolean validateDocumentWithERPConfiguration(FinantialDocument document,
            ERPConfiguration erpIntegrationConfiguration) {

        if (!erpIntegrationConfiguration.isIntegratedDocumentsExportationEnabled() && !document.isDocumentToExport()) {
            throw new TreasuryDomainException("error.ERPExporter.document.already.exported", document.getUiDocumentNumber());
        }
        if (!erpIntegrationConfiguration.isCreditsOfLegacyDebitWithoutLegacyInvoiceExportEnabled() && document.isCreditNote()) {
            CreditNote creditNote = (CreditNote) document;
            if (!creditNote.isAdvancePayment() && !creditNote.isExportedInLegacyERP() && creditNote.getDebitNote() != null
                    && creditNote.getDebitNote().isExportedInLegacyERP()
                    && StringUtils.isEmpty(creditNote.getDebitNote().getLegacyERPCertificateDocumentReference())) {
                throw new TreasuryDomainException(
                        "error.ERPExporter.credit.note.of.legacy.debit.note.without.legacyERPCertificateDocumentReference",
                        creditNote.getDebitNote().getUiDocumentNumber(), creditNote.getUiDocumentNumber());
            }
        }
        return true;
    }

    public static ERPExportOperation exportSettlementNote(final SettlementNote settlementNote) {
        final FinantialInstitution finantialInstitution = settlementNote.getDebtAccount().getFinantialInstitution();
        final ERPConfiguration erpIntegrationConfiguration = finantialInstitution.getErpIntegrationConfiguration();
        final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();

        List<FinantialDocument> documentsToExport =
                erpExporter.filterDocumentsToExport(Collections.singletonList(settlementNote).stream());

        if (documentsToExport.isEmpty()) {
            return null;
        }

        for (final SettlementEntry settlementEntry : settlementNote.getSettlemetEntriesSet()) {
            if (settlementEntry.getInvoiceEntry().isDebitNoteEntry()) {
                documentsToExport.add(settlementEntry.getInvoiceEntry().getFinantialDocument());
            }
        }

        documentsToExport = erpExporter.filterDocumentsToExport(documentsToExport.stream());

        if (finantialInstitution.getErpIntegrationConfiguration().getExportOnlyRelatedDocumentsPerExport()) {

            ERPExportOperation settlementExportOperation = null;

            while (!documentsToExport.isEmpty()) {
                final FinantialDocument doc = documentsToExport.iterator().next();

                //remove the related documents from the original Set
                documentsToExport.remove(doc);

                ERPExportOperation exportOperation = exportSingleDocument(doc);
                if (settlementNote == doc) {
                    settlementExportOperation = exportOperation;
                }
            }

            return settlementExportOperation;
        } else {
            // Review
            return null;
        }
    }

    public static void requestPendingDocumentStatus(FinantialInstitution finantialInstitution) {
        final IERPExporter erpExporter =
                finantialInstitution.getErpIntegrationConfiguration().getERPExternalServiceImplementation().getERPExporter();
        erpExporter.requestPendingDocumentStatus(finantialInstitution);
    }

    public static ERPExportOperation retryExportToIntegration(final ERPExportOperation eRPExportOperation) {
        FinantialInstitution finantialInstitution = eRPExportOperation.getFinantialInstitution();
        ERPConfiguration erpIntegrationConfiguration = finantialInstitution.getErpIntegrationConfiguration();
        IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();

        final List<FinantialDocument> documentsToExport =
                erpExporter.filterDocumentsToExport(eRPExportOperation.getFinantialDocumentsSet().stream());

        return exportSingleDocument(documentsToExport.iterator().next());
    }

    public static byte[] downloadCertifiedDocumentPrint(final FinantialDocument finantialDocument) {
        final FinantialInstitution finantialInstitution = finantialDocument.getDebtAccount().getFinantialInstitution();

        final IERPExporter erpExporter =
                finantialInstitution.getErpIntegrationConfiguration().getERPExternalServiceImplementation().getERPExporter();

        if (!finantialInstitution.getErpIntegrationConfiguration().getActive()) {
            throw new TreasuryDomainException("error.ERPExporterManager.integration.not.active");
        }

        return erpExporter.downloadCertifiedDocumentPrint(finantialDocument);
    }

    public static ReimbursementProcessStateLog updateReimbursementState(final SettlementNote reimbursementNote) {
        final FinantialInstitution finantialInstitution = reimbursementNote.getDebtAccount().getFinantialInstitution();

        final IERPExporter erpExporter =
                finantialInstitution.getErpIntegrationConfiguration().getERPExternalServiceImplementation().getERPExporter();

        if (!finantialInstitution.getErpIntegrationConfiguration().getActive()) {
            throw new TreasuryDomainException("error.ERPExporterManager.integration.not.active");
        }

        if (!reimbursementNote.isReimbursement()) {
            throw new RuntimeException("error");
        }

        final ReimbursementStateBean reimbursementStateBean = erpExporter.checkReimbursementState(reimbursementNote);
        if (reimbursementStateBean == null) {
            throw new TreasuryDomainException("error.ERPExporterManager.reimbursementStatusBean.null");
        }

        if (reimbursementStateBean.getReimbursementProcessStatus() == null) {
            throw new TreasuryDomainException("error.ERPExporterManager.reimbursementStatus.unknown");
        }

        ReimbursementProcessStateLog stateLog = ReimbursementProcessStateLog.create(reimbursementNote,
                reimbursementStateBean.getReimbursementProcessStatus(), UUID.randomUUID().toString(),
                reimbursementStateBean.getReimbursementStateDate(), reimbursementStateBean.getExerciseYear());

        erpExporter.processReimbursementStateChange(reimbursementNote, reimbursementStateBean.getReimbursementProcessStatus(),
                reimbursementStateBean.getExerciseYear(), reimbursementStateBean.getReimbursementStateDate());

        return stateLog;
    }

    // SERVICE

    public static void checkForUnsetDocumentSeriesNumberInDocumentsToExport(List<? extends FinantialDocument> documents) {
        for (final FinantialDocument finantialDocument : documents) {
            if (!finantialDocument.isDocumentSeriesNumberSet()) {
                throw new TreasuryDomainException("error.ERPExporter.document.without.number.series");
            }
        }
    }

    // SERVICE
    @Atomic
    public static OperationFile saveSaftContentToOperationFile(byte[] content, ERPExportOperation operation) {
        String fileName = operation.getFinantialInstitution().getFiscalNumber() + "_"
                + operation.getExecutionDate().toString("ddMMyyyy_hhmm") + ".xml";
        OperationFile binaryStream = new OperationFile(fileName, content);
        if (operation.getFile() != null) {
            operation.getFile().delete();
        }
        operation.setFile(binaryStream);

        return binaryStream;
    }

    private static boolean sendDocumentsInformationToIntegration(final FinantialInstitution institution, byte[] contents,
            final IntegrationOperationLogBean logBean) throws MalformedURLException {
        boolean success = true;
        ERPConfiguration erpIntegrationConfiguration = institution.getErpIntegrationConfiguration();
        if (erpIntegrationConfiguration == null) {
            throw new TreasuryDomainException("error.ERPExporter.invalid.erp.configuration");
        }

        if (erpIntegrationConfiguration.getActive() == false) {
            logBean.appendErrorLog(treasuryBundle("info.ERPExporter.configuration.inactive"));
            return false;
        }

        final IERPExternalService service = erpIntegrationConfiguration.getERPExternalServiceImplementation();
        logBean.appendIntegrationLog(treasuryBundle("info.ERPExporter.sending.inforation"));

        DocumentsInformationInput input = new DocumentsInformationInput();
        if (contents.length <= erpIntegrationConfiguration.getMaxSizeBytesToExportOnline()) {
            input.setData(contents);
            DocumentsInformationOutput sendInfoOnlineResult = service.sendInfoOnline(institution, input);

            logBean.appendIntegrationLog(
                    treasuryBundle("info.ERPExporter.sucess.sending.inforation.online", sendInfoOnlineResult.getRequestId()));
            logBean.setErpOperationId(sendInfoOnlineResult.getRequestId());

            //if we have result in online situation, then check the information of integration STATUS
            for (DocumentStatusWS status : sendInfoOnlineResult.getDocumentStatus()) {
                final FinantialDocument document =
                        FinantialDocument.findByUiDocumentNumber(institution, status.getDocumentNumber());

                boolean integratedWithSuccess = status.isIntegratedWithSuccess();

                if (isToIgnoreWsDocument(institution, status)) {
                    // Product or Customer
                    if (integratedWithSuccess) {
                        final String message =
                                treasuryBundle("info.ERPExporter.sucess.integrating.document", status.getDocumentNumber());
                        logBean.appendIntegrationLog(message);
                    } else {
                        success = false;
                        logBean.appendIntegrationLog(treasuryBundle("info.ERPExporter.error.integrating.document",
                                status.getDocumentNumber(), status.getErrorDescription()));
                        logBean.appendErrorLog(treasuryBundle("info.ERPExporter.error.integrating.document",
                                status.getDocumentNumber(), status.getErrorDescription()));
                    }
                } else {
                    // Finantial Document or something else
                    if (document != null && integratedWithSuccess) {
                        final String message =
                                treasuryBundle("info.ERPExporter.sucess.integrating.document", document.getUiDocumentNumber());
                        logBean.appendIntegrationLog(message);
                        document.clearDocumentToExportAndSaveERPCertificationData(message, new LocalDate(),
                                status.getSapDocumentNumber());
                    } else {
                        success = false;
                        logBean.appendIntegrationLog(treasuryBundle("info.ERPExporter.error.integrating.document",
                                status.getDocumentNumber(), status.getErrorDescription()));
                        logBean.appendErrorLog(treasuryBundle("info.ERPExporter.error.integrating.document",
                                status.getDocumentNumber(), status.getErrorDescription()));
                    }
                }
            }

            for (final String m : sendInfoOnlineResult.getOtherMessages()) {
                logBean.appendIntegrationLog(m);
            }

            for (final String m : sendInfoOnlineResult.getOtherErrorMessages()) {
                logBean.appendErrorLog(m);
            }

            logBean.defineSoapInboundMessage(sendInfoOnlineResult.getSoapInboundMessage());
            logBean.defineSoapOutboundMessage(sendInfoOnlineResult.getSoapOutboundMessage());

        } else {
            throw new TreasuryDomainException(
                    "error.ERPExporter.sendDocumentsInformationToIntegration.maxSizeBytesToExportOnline.exceeded");
        }

        return success;
    }

    private static boolean isToIgnoreWsDocument(final FinantialInstitution finantialInstitution, final DocumentStatusWS status) {
        String documentNumber = status.getDocumentNumber();
        if (documentNumber != null) {
            Optional<Product> product = Product.findUniqueByCode(documentNumber);
            if (product.isPresent()) {
                return true;
            }

            Stream<? extends Customer> customers = Customer.findByCode(documentNumber);
            if (customers.findAny().isPresent()) {
                return true;
            }
        }

        return false;
    }

    private static void writeError(final ERPExportOperation operation, final IntegrationOperationLogBean logBean,
            final Throwable t) {
        final StringWriter out = new StringWriter();
        final PrintWriter writer = new PrintWriter(out);
        t.printStackTrace(writer);

        logBean.appendErrorLog(out.toString());

        operation.setProcessed(true);
    }

    public static String exportsCustomersToXML(FinantialInstitution finantialInstitution) {
        final ERPConfiguration erpIntegrationConfiguration = finantialInstitution.getErpIntegrationConfiguration();
        final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();

        if (TreasuryPlataformDependentServicesFactory.implementation()
                .getSaftExporterConfiguration(erpIntegrationConfiguration) != null) {

            ISaftExporterConfiguration saftExporterConfiguration = TreasuryPlataformDependentServicesFactory.implementation()
                    .getSaftExporterConfiguration(erpIntegrationConfiguration);

            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                saftExporterConfiguration
                        .generateSaftForCustomers(Customer.find(finantialInstitution).collect(Collectors.toSet()), true, baos);
                return new String(baos.toByteArray(), saftExporterConfiguration.getEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

        } else {
            return erpExporter.exportsCustomersToXML(finantialInstitution);
        }

    }

    public static String exportsProductsToXML(FinantialInstitution finantialInstitution) {
        final ERPConfiguration erpIntegrationConfiguration = finantialInstitution.getErpIntegrationConfiguration();
        final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();

        if (TreasuryPlataformDependentServicesFactory.implementation()
                .getSaftExporterConfiguration(erpIntegrationConfiguration) != null) {

            ISaftExporterConfiguration saftExporterConfiguration = TreasuryPlataformDependentServicesFactory.implementation()
                    .getSaftExporterConfiguration(erpIntegrationConfiguration);
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                saftExporterConfiguration.generateSaftForProducts(finantialInstitution.getAvailableProductsSet(), true, baos);
                return new String(baos.toByteArray(), saftExporterConfiguration.getEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            return erpExporter.exportsProductsToXML(finantialInstitution);
        }

    }

    public static String exportPendingDocumentsForFinantialInstitutionToXML(FinantialInstitution finantialInstitution) {
        final ERPConfiguration erpIntegrationConfiguration = finantialInstitution.getErpIntegrationConfiguration();
        final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();

        if (TreasuryPlataformDependentServicesFactory.implementation()
                .getSaftExporterConfiguration(erpIntegrationConfiguration) != null) {

            ISaftExporterConfiguration saftExporterConfiguration = TreasuryPlataformDependentServicesFactory.implementation()
                    .getSaftExporterConfiguration(erpIntegrationConfiguration);
            try {
                final List<FinantialDocument> sortedDocuments = erpExporter
                        .filterDocumentsToExport(finantialInstitution.getFinantialDocumentsPendingForExportationSet().stream());

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                saftExporterConfiguration.generateSaftForFinantialDocuments(sortedDocuments, true, baos);
                return new String(baos.toByteArray(), saftExporterConfiguration.getEncoding());
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new RuntimeException("Operation.not.supported");
        }

    }

    @Atomic(mode = TxMode.WRITE)
    public static ERPExportOperation exportCustomersToIntegration(FinantialInstitution institution) {
        final ERPConfiguration erpIntegrationConfiguration = institution.getErpIntegrationConfiguration();

        if (TreasuryPlataformDependentServicesFactory.implementation()
                .getSaftExporterConfiguration(erpIntegrationConfiguration) != null) {

            final IntegrationOperationLogBean logBean = new IntegrationOperationLogBean();
            final ERPExportOperation operation = ERPExportOperation.createSaftExportOperation(null, institution, new DateTime());
            try {
                logBean.appendIntegrationLog(treasuryBundle("label.ERPExporter.starting.products.integration"));

                ISaftExporterConfiguration saftExporterConfiguration = TreasuryPlataformDependentServicesFactory.implementation()
                        .getSaftExporterConfiguration(erpIntegrationConfiguration);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                saftExporterConfiguration
                        .generateSaftForCustomers(Customer.find(institution).collect(Collectors.toSet()), false, baos);

                logBean.appendIntegrationLog(treasuryBundle("label.ERPExporter.erp.xml.content.generated"));

                byte[] contents = baos.toByteArray();
                saveSaftContentToOperationFile(contents, operation);

                boolean success = sendDocumentsInformationToIntegration(institution, contents, logBean);
                logBean.appendIntegrationLog(treasuryBundle("label.ERPExporter.finished.products.integration"));

                operation.setSuccess(success);
            } catch (Exception ex) {
                writeError(operation, logBean, ex);
            } finally {
                operation.appendLog(logBean.getErrorLog(), logBean.getIntegrationLog(), logBean.getSoapInboundMessage(),
                        logBean.getSoapOutboundMessage());
            }

            return operation;
        } else {
            final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();
            return erpExporter.exportCustomersToIntegration(institution);
        }
    }

    @Atomic(mode = TxMode.WRITE)
    public static ERPExportOperation exportProductsToIntegration(FinantialInstitution institution) {
        final ERPConfiguration erpIntegrationConfiguration = institution.getErpIntegrationConfiguration();

        if (TreasuryPlataformDependentServicesFactory.implementation()
                .getSaftExporterConfiguration(erpIntegrationConfiguration) != null) {

            final IntegrationOperationLogBean logBean = new IntegrationOperationLogBean();
            final ERPExportOperation operation = ERPExportOperation.createSaftExportOperation(null, institution, new DateTime());
            try {
                logBean.appendIntegrationLog(treasuryBundle("label.ERPExporter.starting.products.integration"));

                ISaftExporterConfiguration saftExporterConfiguration = TreasuryPlataformDependentServicesFactory.implementation()
                        .getSaftExporterConfiguration(erpIntegrationConfiguration);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                saftExporterConfiguration.generateSaftForProducts(institution.getAvailableProductsSet(), false, baos);

                logBean.appendIntegrationLog(treasuryBundle("label.ERPExporter.erp.xml.content.generated"));

                byte[] contents = baos.toByteArray();
                saveSaftContentToOperationFile(contents, operation);

                boolean success = sendDocumentsInformationToIntegration(institution, contents, logBean);
                logBean.appendIntegrationLog(treasuryBundle("label.ERPExporter.finished.products.integration"));

                operation.setSuccess(success);
            } catch (Exception ex) {
                writeError(operation, logBean, ex);
            } finally {
                operation.appendLog(logBean.getErrorLog(), logBean.getIntegrationLog(), logBean.getSoapInboundMessage(),
                        logBean.getSoapOutboundMessage());
            }

            return operation;
        } else {

            final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();
            return erpExporter.exportProductsToIntegration(institution);

        }

    }

}
