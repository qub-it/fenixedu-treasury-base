package org.fenixedu.treasury.services.integration.erp;

import java.io.InputStream;
import java.util.List;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.integration.IntegrationOperationLogBean;
import org.fenixedu.treasury.services.integration.erp.ERPExternalServiceImplementation.ReimbursementStateBean;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationInput;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationOutput;

public interface IERPExternalService {

    public DocumentsInformationOutput sendInfoOnline(final FinantialInstitution finantialInstitution,
            DocumentsInformationInput documentsInformation);

    public String sendInfoOffline(DocumentsInformationInput documentsInformation);

    public List<DocumentStatusWS> getIntegrationStatusFor(String finantialInstiution, List<String> documentInformaton);

    public byte[] downloadCertifiedDocumentPrint(final String finantialInstitution, final String finantialDocumentNumber,
            final String erpIdProcess);

    public IERPExporter getERPExporter();

    public IERPImporter getERPImporter(final InputStream inputStream);

    public ReimbursementStateBean checkReimbursementState(final SettlementNote reimbursementNote,
            final IntegrationOperationLogBean logBean);

    public default boolean isToSendCreditNoteWhenCreated() {
        return false;
    }
}