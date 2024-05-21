package org.fenixedu.treasury.services.integration.erp.tasks;

import org.fenixedu.bennu.scheduler.CronTask;
import org.fenixedu.bennu.scheduler.annotation.Task;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.integration.ERPExportOperation;
import org.fenixedu.treasury.services.integration.erp.ERPExporterManager;

import com.google.common.base.Strings;

import pt.ist.fenixframework.FenixFramework;

/**
 * Originally from treasury-ui but moved to this artifact because it is referenced in FenixEDUTreasuryPlatformDependentServices
 */
@Deprecated
@Task(englishTitle = "Export Single Doument to ERP Integration", readOnly = true)
public class ERPExportSingleDocumentsTask extends CronTask {

    private String externalId;

    public ERPExportSingleDocumentsTask(final String documentExternalId) {
        externalId = documentExternalId;
    }

    @Override
    public void runTask() throws Exception {

        if (Strings.isNullOrEmpty(externalId)) {
            taskLog("External ID empty, not exporting any document");
            return;
        }

        FinantialDocument document = FenixFramework.getDomainObject(externalId);
        if (document == null) {
            return;
        }
        
        if (document.isPreparing()) {
            taskLog("Ignored, trying to export a PREPARING document, oid: " + externalId);
            return;
        }
        
        if(document.isCreditNote()) {
            taskLog("Ignored, credit note is exported with settlement note, oid: " + externalId);
            return;
        }

        final FinantialInstitution finantialInstitution = document.getDocumentNumberSeries().getSeries().getFinantialInstitution();

        if (!finantialInstitution.getErpIntegrationConfiguration().getActive()) {
            return;
        }

        try {
            final ERPExportOperation exportOperation = ERPExporterManager.exportSingleDocument(document);
            
            if(exportOperation == null) {
                return;
            }
            
            taskLog(String.format("Exported document: %s => %s", document.getUiDocumentNumber(),
                    (exportOperation.getSuccess() ? "OK" : "NOK")));
        } catch (final Exception ex) {
            taskLog("Error exporting document: " + ex.getMessage());
            for (StackTraceElement el : ex.getStackTrace()) {
                taskLog(el.toString());
            }
        }
    }
    
}
