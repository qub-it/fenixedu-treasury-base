package org.fenixedu.treasury.services.integration.erp.ERPExternalServiceImplementation;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.xml.ws.BindingProvider;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.integration.IntegrationOperationLogBean;
import org.fenixedu.treasury.generated.sources.saft.singap.siag.AuditFile;
import org.fenixedu.treasury.services.integration.erp.IERPExporter;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.fenixedu.treasury.services.integration.erp.IERPImporter;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS.StatusType;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationInput;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationOutput;
import org.fenixedu.treasury.services.integration.erp.singap.ArrayOfResposta;
import org.fenixedu.treasury.services.integration.erp.singap.Resposta;
import org.fenixedu.treasury.services.integration.erp.singap.Service;
import org.fenixedu.treasury.services.integration.erp.singap.ServiceSoap;
import org.fenixedu.treasury.services.integration.erp.singap.siag.SingapSiagExporter;
import org.fenixedu.treasury.services.integration.erp.singap.siag.SingapSiagImporter;

import com.qubit.solution.fenixedu.bennu.webservices.services.client.BennuWebServiceClient;
import com.sun.xml.ws.client.BindingProviderProperties;

import pt.ist.fenixframework.Atomic;

public class SINGAPExternalService extends BennuWebServiceClient<ServiceSoap> implements IERPExternalService {

    static {
        //HACK:only for "creation of webserviceclient-configuration"
        createStaticInitializer();
    }

    public SINGAPExternalService() {
    }

    @Atomic
    private static void createStaticInitializer() {
        new SINGAPExternalService();

    }

    @Override
    public ServiceSoap getClient() {
        final ServiceSoap client = super.getClient();

        return client;
    }

    @Override
    public DocumentsInformationOutput sendInfoOnline(final FinantialInstitution finantialInstutition,
            DocumentsInformationInput documentsInformation) {
        DocumentsInformationOutput output = new DocumentsInformationOutput();
        output.setDocumentStatus(new ArrayList<DocumentStatusWS>());
        final ServiceSoap client = getClient();

        final SOAPLoggingHandler loggingHandler = SOAPLoggingHandler.createLoggingHandler((BindingProvider) client);

        //Set Timeout for the client
        Map<String, Object> requestContext = ((BindingProvider) client).getRequestContext();
        requestContext.put(BindingProviderProperties.REQUEST_TIMEOUT, 15000); // Timeout in millis
        requestContext.put(BindingProviderProperties.CONNECT_TIMEOUT, 2000); // Timeout in millis

        ArrayOfResposta carregarSAFTON = client.carregarSAFTON(documentsInformation.getData());

        // 2016/09/27 Disable it for now
//        output.setSoapInboundMessage(loggingHandler.getInboundMessage());
//        output.setSoapOutboundMessage(loggingHandler.getOutboundMessage());
        output.setSoapInboundMessage("");
        output.setSoapOutboundMessage("");

        for (Resposta resposta : carregarSAFTON.getResposta()) {
            output.setRequestId(resposta.getChavePrimaria());
            DocumentStatusWS status = new DocumentStatusWS();
            status.setDocumentNumber(resposta.getChavePrimaria());
            status.setErrorDescription(String.format("[STATUS: %s] - %s", resposta.getStatus(), resposta.getMensagem()));
            status.setIntegrationStatus(covertToStatusType(resposta.getStatus()));
            output.getDocumentStatus().add(status);
        }

        return output;
    }

    @Override
    public String sendInfoOffline(DocumentsInformationInput documentsInformation) {
        Resposta carregarSAFT = getClient().carregarSAFTOFF(documentsInformation.getDataURI());
        return carregarSAFT.getChavePrimaria();
    }

    @Override
    public List<DocumentStatusWS> getIntegrationStatusFor(String finantialInstitution, List<String> documentsInformation) {
        List<DocumentStatusWS> statusList = new ArrayList<DocumentStatusWS>();

        for (String docId : documentsInformation) {
            Resposta verificaEstado = getClient().verificaEstado(docId);
            DocumentStatusWS docStatus = new DocumentStatusWS();
            docStatus.setDocumentNumber(docId);
            docStatus.setErrorDescription(verificaEstado.getMensagem());
            docStatus.setIntegrationStatus(covertToStatusType(verificaEstado.getStatus()));
            statusList.add(docStatus);
        }

        return statusList;
    }

    private StatusType covertToStatusType(String status) {
        if ("OK".equals(status)) {
            return StatusType.SUCCESS;
        } else {
            return StatusType.ERROR;
        }
    }

    @Override
    protected BindingProvider getService() {
        BindingProvider prov = (BindingProvider) new Service().getServiceSoap();
        return prov;
    }

    public UnaryOperator<AuditFile> getAuditFilePreProcessOperator() {

        return (AuditFile x) -> {
            return x;
        };
    }

    @Override
    public IERPExporter getERPExporter() {
        return new SingapSiagExporter();
    }

    @Override
    public IERPImporter getERPImporter(final InputStream inputStream) {
        return new SingapSiagImporter(inputStream);
    }

    @Override
    public byte[] downloadCertifiedDocumentPrint(final String finantialInstitution, final String finantialDocumentNumber,
            final String erpIdProcess) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public ReimbursementStateBean checkReimbursementState(SettlementNote reimbursementNote,
            final IntegrationOperationLogBean logBean) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public boolean isToSendCreditNoteWhenCreated() {
        return true;
    }

}