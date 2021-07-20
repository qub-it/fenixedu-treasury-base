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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.ERPCustomerFieldsBean;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.document.reimbursement.ReimbursementProcessStateLog;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.domain.integration.ERPExportOperation;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.integration.erp.ERPExternalServiceImplementation.ReimbursementStateBean;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

import pt.ist.fenixframework.Atomic;

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
        final IERPExporter erpExporter = finantialDocument.getDebtAccount().getFinantialInstitution()
                .getErpIntegrationConfiguration().getERPExternalServiceImplementation().getERPExporter();

        return erpExporter.exportFinantialDocumentToXML(finantialInstitution, Lists.newArrayList(finantialDocument));
    }

    public static List<ERPExportOperation> exportPendingDocumentsForFinantialInstitution(
            final FinantialInstitution finantialInstitution) {

        final IERPExporter erpExporter =
                finantialInstitution.getErpIntegrationConfiguration().getERPExternalServiceImplementation().getERPExporter();

        if (!finantialInstitution.getErpIntegrationConfiguration().getActive()) {
            return Lists.newArrayList();
        }

        final List<FinantialDocument> sortedDocuments =
                filterDocumentsToExport(finantialInstitution.getFinantialDocumentsPendingForExportationSet().stream());

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

                result.add(
                        erpExporter.exportFinantialDocumentToIntegration(finantialInstitution, Collections.singletonList(doc)));
            }

            return result;
        }

        // return Lists.newArrayList(erpExporter.exportFinantialDocumentToIntegration(finantialInstitution, sortedDocuments));
        return Lists.newArrayList();
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

        final List<FinantialDocument> sortedDocuments = filterDocumentsToExport(debtAccount.getFinantialDocumentsSet().stream());

        if (sortedDocuments.isEmpty()) {
            return Lists.newArrayList();
        }

        final IERPExporter erpExporter = debtAccount.getFinantialInstitution().getErpIntegrationConfiguration()
                .getERPExternalServiceImplementation().getERPExporter();

        if (finantialInstitution.getErpIntegrationConfiguration().getExportOnlyRelatedDocumentsPerExport()) {
            final List<ERPExportOperation> result = Lists.newArrayList();

            int i = 0;
            while (!sortedDocuments.isEmpty()) {
                final FinantialDocument doc = sortedDocuments.iterator().next();

                //remove the related documents from the original Set
                sortedDocuments.remove(doc);

                result.add(
                        erpExporter.exportFinantialDocumentToIntegration(finantialInstitution, Collections.singletonList(doc)));

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

    public static ERPExportOperation exportSingleDocument(final FinantialDocument finantialDocument) {
        final List<FinantialDocument> documentsToExport =
                filterDocumentsToExport(Collections.singletonList(finantialDocument).stream());

        if (documentsToExport.isEmpty()) {
            return null;
        }

        final FinantialInstitution finantialInstitution = finantialDocument.getDebtAccount().getFinantialInstitution();
        final ERPConfiguration erpIntegrationConfiguration = finantialInstitution.getErpIntegrationConfiguration();

        final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();

        return erpExporter.exportFinantialDocumentToIntegration(finantialInstitution, documentsToExport);
    }

    public static ERPExportOperation exportSettlementNote(final SettlementNote settlementNote) {
        List<FinantialDocument> documentsToExport = filterDocumentsToExport(Collections.singletonList(settlementNote).stream());

        if (documentsToExport.isEmpty()) {
            return null;
        }

        for (final SettlementEntry settlementEntry : settlementNote.getSettlemetEntriesSet()) {
            if (settlementEntry.getInvoiceEntry().isDebitNoteEntry()) {
                documentsToExport.add(settlementEntry.getInvoiceEntry().getFinantialDocument());
            }
        }

        documentsToExport = filterDocumentsToExport(documentsToExport.stream());

        final FinantialInstitution finantialInstitution = settlementNote.getDebtAccount().getFinantialInstitution();
        final ERPConfiguration erpIntegrationConfiguration = finantialInstitution.getErpIntegrationConfiguration();

        final IERPExporter erpExporter = erpIntegrationConfiguration.getERPExternalServiceImplementation().getERPExporter();

        if (finantialInstitution.getErpIntegrationConfiguration().getExportOnlyRelatedDocumentsPerExport()) {

            ERPExportOperation settlementExportOperation = null;

            while (!documentsToExport.isEmpty()) {
                final FinantialDocument doc = documentsToExport.iterator().next();

                //remove the related documents from the original Set
                documentsToExport.remove(doc);

                ERPExportOperation exportOperation =
                        erpExporter.exportFinantialDocumentToIntegration(finantialInstitution, Collections.singletonList(doc));
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
        final List<FinantialDocument> documentsToExport =
                filterDocumentsToExport(eRPExportOperation.getFinantialDocumentsSet().stream());

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

        if (reimbursementStateBean.getReimbursementProcessStatus().isRejectedStatus()) {
            throw new TreasuryDomainException(
                    "error.ERPExporterManager.reimbursementStatus.rejected.please.check.rejection.and.contact.support.if.needed");
        }

        ReimbursementProcessStateLog stateLog = ReimbursementProcessStateLog.create(reimbursementNote,
                reimbursementStateBean.getReimbursementProcessStatus(), UUID.randomUUID().toString(),
                reimbursementStateBean.getReimbursementStateDate(), reimbursementStateBean.getExerciseYear());

        reimbursementNote.processReimbursementStateChange(reimbursementStateBean.getReimbursementProcessStatus(),
                reimbursementStateBean.getExerciseYear(), reimbursementStateBean.getReimbursementStateDate());

        return stateLog;
    }

    // @formatter:off
    public static List<FinantialDocument> filterDocumentsToExport(
            final Stream<? extends FinantialDocument> finantialDocumentsStream) {
        
        List<? extends FinantialDocument> tempList = finantialDocumentsStream
                .filter(d -> d.isDocumentToExport())
                .filter(d -> !d.isCreditNote())
                .filter(d -> d.isAnnulled() || d.isClosed())
                .filter(d -> d.isDocumentSeriesNumberSet())
                .filter(x -> x.getCloseDate() != null)
                .filter(x -> x.isDebitNote() || (x.isSettlementNote() && !x.getCloseDate().isBefore(SAPExporter.ERP_INTEGRATION_START_DATE)))
                // TODO Anil Review comparator COMPARE_BY_DOCUMENT_TYPE which is buggy, for now do not sort
                // .sorted(COMPARE_BY_DOCUMENT_TYPE)
                .collect(Collectors.<FinantialDocument> toList());

        if(TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            // If there is restriction on mixing payments exported in legacy ERP, then filter documents exported in legacy ERP
            tempList = tempList.stream()
                    .filter(d -> !d.isExportedInLegacyERP())
                    .filter(d -> !d.getCloseDate().isBefore(SAPExporter.ERP_INTEGRATION_START_DATE))
                    .collect(Collectors.<FinantialDocument> toList());
        }
        
        final List<FinantialDocument> result = Lists.newArrayList();

        // TODO: Put first debit notes and then settlement notes
        result.addAll(tempList.stream().filter(d -> d.isDebitNote()).collect(Collectors.<FinantialDocument> toList()));
        result.addAll(tempList.stream().filter(d -> d.isSettlementNote()).collect(Collectors.<FinantialDocument> toList()));
        
        if(tempList.size() != result.size()) {
            throw new RuntimeException("error");
        }
        
        return result;
    }
    // @formatter:on

}
