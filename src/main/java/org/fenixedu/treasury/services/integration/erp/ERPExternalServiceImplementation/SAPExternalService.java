package org.fenixedu.treasury.services.integration.erp.ERPExternalServiceImplementation;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.ws.BindingProvider;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.CreditNote;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.document.reimbursement.ReimbursementProcessStatusType;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.domain.integration.IntegrationOperationLogBean;
import org.fenixedu.treasury.services.integration.erp.IERPExporter;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.fenixedu.treasury.services.integration.erp.IERPImporter;
import org.fenixedu.treasury.services.integration.erp.ERPExternalServiceImplementation.ReimbursementStateBean;
import org.fenixedu.treasury.services.integration.erp.ERPExternalServiceImplementation.SOAPLoggingHandler;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS.StatusType;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationInput;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationOutput;
import org.fenixedu.treasury.services.integration.erp.sap.SAPExporter;
import org.fenixedu.treasury.services.integration.erp.sap.SAPImporter;
import org.fenixedu.treasury.services.integration.erp.sap.ZULWSFATURACAOCLIENTESBLK;
import org.fenixedu.treasury.services.integration.erp.sap.ZULWSFATURACAOCLIENTESBLK_Service;
import org.fenixedu.treasury.services.integration.erp.sap.ZulfwscustomersReturn1S;
import org.fenixedu.treasury.services.integration.erp.sap.ZulwsDocumentosInput;
import org.fenixedu.treasury.services.integration.erp.sap.ZulwsDocumentosOutput;
import org.fenixedu.treasury.services.integration.erp.sap.ZulwsReembolsosInput;
import org.fenixedu.treasury.services.integration.erp.sap.ZulwsReembolsosOutput;
import org.fenixedu.treasury.services.integration.erp.sap.ZulwsdocumentStatusWs1;
import org.fenixedu.treasury.services.integration.erp.sap.ZulwsfaturacaoClientesIn;
import org.fenixedu.treasury.services.integration.erp.sap.ZulwsfaturacaoClientesOut;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.qubit.solution.fenixedu.bennu.webservices.services.client.BennuWebServiceClient;
import com.sun.xml.ws.client.BindingProviderProperties;
import com.sun.xml.ws.fault.ServerSOAPFaultException;

public class SAPExternalService extends BennuWebServiceClient<ZULWSFATURACAOCLIENTESBLK> implements IERPExternalService {

    private static final String S_KEY = "S";

    @Override
    public DocumentsInformationOutput sendInfoOnline(final FinantialInstitution finantialInstitution,
            DocumentsInformationInput documentsInformation) {
        DocumentsInformationOutput output = new DocumentsInformationOutput();
        output.setDocumentStatus(new ArrayList<DocumentStatusWS>());
        final ZULWSFATURACAOCLIENTESBLK client = getClient();

        final SOAPLoggingHandler loggingHandler = SOAPLoggingHandler.createLoggingHandler((BindingProvider) client);

        //Set Timeout for the client
        Map<String, Object> requestContext = ((BindingProvider) client).getRequestContext();
        requestContext.put(BindingProviderProperties.REQUEST_TIMEOUT, 15000); // Timeout in millis
        requestContext.put(BindingProviderProperties.CONNECT_TIMEOUT, 2000); // Timeout in millis

        ZulwsfaturacaoClientesIn auditFile = new ZulwsfaturacaoClientesIn();
        auditFile.setFinantialInstitution(documentsInformation.getFinantialInstitution());
        auditFile.setData(documentsInformation.getData());

        ZulwsfaturacaoClientesOut zulwsfaturacaoClientesOut = client.zulfmwsFaturacaoClientes(auditFile);

        output.setRequestId(zulwsfaturacaoClientesOut.getRequestId());

        boolean hasSettlementFailed = hasSettlementFailed(finantialInstitution, zulwsfaturacaoClientesOut);
        boolean isSomeDocAssociatedWithReimbursementFailed =
                isSomeDocAssociatedWithReimbursementFailed(finantialInstitution, zulwsfaturacaoClientesOut);

        for (ZulwsdocumentStatusWs1 item : zulwsfaturacaoClientesOut.getDocumentStatus().getItem()) {
            final DocumentStatusWrapper itemWrapper = new DocumentStatusWrapper(finantialInstitution, item);

            DocumentStatusWS status = new DocumentStatusWS();
            status.setDocumentNumber(itemWrapper.getDocumentNumber());
            status.setErrorDescription(
                    String.format("[STATUS: %s] - %s", itemWrapper.getIntegrationStatus(), itemWrapper.getErrorDescription()));
            status.setIntegrationStatus(
                    convertToStatusType(itemWrapper, hasSettlementFailed, isSomeDocAssociatedWithReimbursementFailed));
            status.setSapDocumentNumber(itemWrapper.getSapDocumentNumber());

            output.getDocumentStatus().add(status);
        }

        for (final ZulfwscustomersReturn1S item : zulwsfaturacaoClientesOut.getCustomers().getItem()) {
            String erpCustomerId = item.getCustomerIdSap();
            String integrationStatus = item.getIntegrationStatus();
            String returnMsg = item.getReturnMsg();
            String fenixCustomerId = item.getCustomerId();

            final String otherMessage = String.format("%s (SAP nº %s): [%s] %s",
                    treasuryBundle("label.SAPExternalService.customer.integration.result"),
                    !Strings.isNullOrEmpty(erpCustomerId) ? erpCustomerId : "", integrationStatus, returnMsg);

            output.getOtherMessages().add(otherMessage);

            if (S_KEY.equals(integrationStatus)) {
                saveErpCustomerId(output, erpCustomerId, fenixCustomerId);
            } else {
                output.getOtherErrorMessages().add(otherMessage);
            }
        }

        output.setSoapInboundMessage(loggingHandler.getInboundMessage());
        output.setSoapOutboundMessage(loggingHandler.getOutboundMessage());

        return output;
    }

    private void saveErpCustomerId(final DocumentsInformationOutput output, final String erpCustomerId,
            final String fenixCustomerId) {
        if (Strings.isNullOrEmpty(fenixCustomerId)) {
            final String message = String.format("%s %s", treasuryBundle("label.SAPExternalService.customer.integration.result"),
                    treasuryBundle("label.SAPExternalService.warning.customer.id.empty"));

            output.getOtherMessages().add(message);
            return;
        }

        if (Strings.isNullOrEmpty(erpCustomerId) || Strings.isNullOrEmpty(erpCustomerId.trim())) {
            String message = String.format("%s %s", treasuryBundle("label.SAPExternalService.customer.integration.result"),
                    treasuryBundle("label.SAPExternalService.warning.erp.customer.id.empty"));

            output.getOtherMessages().add(message);
            return;
        }

        Optional<? extends Customer> customerOptional = Customer.findByCode(fenixCustomerId).findAny();

        if (!customerOptional.isPresent()) {
            final String message = String.format("%s %s", treasuryBundle("label.SAPExternalService.customer.integration.result"),
                    treasuryBundle(("label.SAPExternalService.warning.customer.with.code.not.found"), fenixCustomerId));

            output.getOtherMessages().add(message);
            return;
        }

        Customer customer = customerOptional.get();

        if (customer.getErpCustomerId() != null && customer.getErpCustomerId().equals(erpCustomerId)) {
            final String message =
                    String.format("%s %s", treasuryBundle("label.SAPExternalService.customer.integration.result"), treasuryBundle(
                            "label.SAPExternalService.info.customer.with.erp.id.already.set", customer.getCode(), erpCustomerId));

            output.getOtherMessages().add(message);
            return;
        }

        if (customer.getErpCustomerId() != null && !customer.getErpCustomerId().equals(erpCustomerId)) {
            final String message = String.format("%s %s", treasuryBundle("label.SAPExternalService.customer.integration.result"),
                    treasuryBundle("label.SAPExternalService.warning.erp.customer.id.not.equal", customer.getCode(),
                            erpCustomerId, customer.getErpCustomerId()));

            output.getOtherMessages().add(message);
            return;
        }

        if (customer.getErpCustomerId() == null) {
            final String message = String.format("%s %s", treasuryBundle("label.SAPExternalService.customer.integration.result"),
                    treasuryBundle("label.SAPExternalService.info.customer.with.erp.id.set", customer.getCode(), erpCustomerId));

            output.getOtherMessages().add(message);

            customer.setErpCustomerId(erpCustomerId);
        }
    }

    private boolean isSomeDocAssociatedWithReimbursementFailed(final FinantialInstitution finantialInstitution,
            ZulwsfaturacaoClientesOut zulwsfaturacaoClientesOut) {
        boolean isSomeDocAssociatedWithReimbursementFailed = false;
        {
            for (ZulwsdocumentStatusWs1 item : zulwsfaturacaoClientesOut.getDocumentStatus().getItem()) {
                final DocumentStatusWrapper itemWrapper = new DocumentStatusWrapper(finantialInstitution, item);

                if (!itemWrapper.isSettlementNote()) {
                    isSomeDocAssociatedWithReimbursementFailed |= itemWrapper.integrationStatus() != StatusType.SUCCESS;
                }
            }
        }
        return isSomeDocAssociatedWithReimbursementFailed;
    }

    private boolean hasSettlementFailed(final FinantialInstitution finantialInstitution,
            ZulwsfaturacaoClientesOut zulwsfaturacaoClientesOut) {
        boolean hasSettlementFailed = false;
        for (ZulwsdocumentStatusWs1 item : zulwsfaturacaoClientesOut.getDocumentStatus().getItem()) {
            final DocumentStatusWrapper itemWrapper = new DocumentStatusWrapper(finantialInstitution, item);

            if (itemWrapper.isSettlementNote()) {
                hasSettlementFailed |= itemWrapper.integrationStatus() != StatusType.SUCCESS;
            }
        }
        return hasSettlementFailed;
    }

    private StatusType convertToStatusType(final DocumentStatusWrapper itemWrapper, final boolean hasSettlementFailed,
            final boolean isSomeDocAssociatedWithReimbursementFailed) {
        final String status = itemWrapper.getIntegrationStatus();
        final String sapDocumentNumber = itemWrapper.getSapDocumentNumber();

        if (itemWrapper.isReimbursement()) {
            return !isSomeDocAssociatedWithReimbursementFailed && S_KEY.equals(status) ? StatusType.SUCCESS : StatusType.ERROR;
        }

        if (!Strings.isNullOrEmpty(sapDocumentNumber) && S_KEY.equals(status) && !hasSettlementFailed) {
            return StatusType.SUCCESS;
        }

        return StatusType.ERROR;
    }

    @Override
    public String sendInfoOffline(DocumentsInformationInput documentsInformation) {
        throw new RuntimeException("not.implemented");
    }

    @Override
    public List<DocumentStatusWS> getIntegrationStatusFor(String finantialInstiution, List<String> documentInformaton) {
        throw new RuntimeException("not.implemented");
    }

    @Override
    public byte[] downloadCertifiedDocumentPrint(final String finantialInstitution, final String finantialDocumentNumber,
            String erpIdProcess) {

        final ZULWSFATURACAOCLIENTESBLK client = getClient();

        //Set Timeout for the client
        Map<String, Object> requestContext = ((BindingProvider) client).getRequestContext();
        requestContext.put(BindingProviderProperties.REQUEST_TIMEOUT, 15000); // Timeout in millis
        requestContext.put(BindingProviderProperties.CONNECT_TIMEOUT, 2000); // Timeout in millis

        ZulwsDocumentosInput input = new ZulwsDocumentosInput();

        input.setTaxRegistrationNumber(finantialInstitution);
        input.setFinantialDocumentNumber(finantialDocumentNumber);
        input.setIdProcesso(erpIdProcess);

        try {
            final ZulwsDocumentosOutput zulwsDocumentos = client.zulwsDocumentos(input);
            final StatusType status = S_KEY.equals(zulwsDocumentos.getStatus()) ? StatusType.SUCCESS : StatusType.ERROR;

            if (status != StatusType.SUCCESS) {
                throw new TreasuryDomainException(
                        "error.IERPExternalService.getCertifiedDocumentPrinted.unable.to.retrieve.document",
                        zulwsDocumentos.getErrorDescription());
            }

            return zulwsDocumentos.getBinary();
        } catch (final ServerSOAPFaultException e) {
            e.printStackTrace();
            throw new TreasuryDomainException(e,
                    "error.IERPExternalService.getCertifiedDocumentPrinted.unable.to.retrieve.document", e.getMessage());
        }

    }

    @Override
    public ReimbursementStateBean checkReimbursementState(final SettlementNote reimbursementNote,
            final IntegrationOperationLogBean logBean) {
        final ERPConfiguration erpConfiguration =
                reimbursementNote.getDebtAccount().getFinantialInstitution().getErpIntegrationConfiguration();
        final ZULWSFATURACAOCLIENTESBLK client = getClient();

        final SOAPLoggingHandler loggingHandler = SOAPLoggingHandler.createLoggingHandler((BindingProvider) client);

        //Set Timeout for the client
        Map<String, Object> requestContext = ((BindingProvider) client).getRequestContext();
        requestContext.put(BindingProviderProperties.REQUEST_TIMEOUT, 15000); // Timeout in millis
        requestContext.put(BindingProviderProperties.CONNECT_TIMEOUT, 2000); // Timeout in millis

        final CreditNote creditNote =
                (CreditNote) reimbursementNote.getSettlemetEntries().findFirst().get().getInvoiceEntry().getFinantialDocument();

        ZulwsReembolsosInput input = new ZulwsReembolsosInput();
        input.setFinantialDocumentNumber(creditNote.getUiDocumentNumber());
        input.setIdProcesso(erpConfiguration.getErpIdProcess());
        input.setTaxRegistrationNumber(reimbursementNote.getDebtAccount().getFinantialInstitution().getFiscalNumber());

        final ZulwsReembolsosOutput zulwsReembolsos = client.zulwsReembolsos(input);
        logBean.defineSoapInboundMessage(loggingHandler.getInboundMessage());
        logBean.defineSoapOutboundMessage(loggingHandler.getOutboundMessage());

        final Optional<ReimbursementProcessStatusType> reimbursementStatus =
                ReimbursementProcessStatusType.findUniqueByCode(zulwsReembolsos.getReimbursementStatusCode());

        boolean success = true;
        if (!reimbursementStatus.isPresent()) {
            success = false;
            logBean.appendErrorLog(
                    String.format("Erro na leitura do estado do reembolso: '%s'", zulwsReembolsos.getReimbursementStatusCode()));
        }

        DateTime reimbursementStatusDate = null;
        try {
            reimbursementStatusDate =
                    new DateTime(zulwsReembolsos.getReimbursementStatusDate()).toLocalDate().toDateTimeAtStartOfDay();
        } catch (final IllegalArgumentException e) {
            success = false;
            logBean.appendErrorLog("Erro na leitura da data: " + zulwsReembolsos.getReimbursementStatusDate());
        }

        if (Strings.isNullOrEmpty(zulwsReembolsos.getExerciseYear())) {
            success = false;
            logBean.appendErrorLog("Erro na leitura do ano de exercicio: " + zulwsReembolsos.getExerciseYear());
        }

        final ReimbursementStateBean stateBean = new ReimbursementStateBean(reimbursementNote, reimbursementStatus.orElse(null),
                zulwsReembolsos.getExerciseYear(), reimbursementStatusDate, success);

        return stateBean;
    }

    @Override
    public IERPExporter getERPExporter() {
        return new SAPExporter();
    }

    @Override
    public IERPImporter getERPImporter(InputStream inputStream) {
        return new SAPImporter(inputStream);
    }

    @Override
    protected BindingProvider getService() {
        BindingProvider prov = (BindingProvider) new ZULWSFATURACAOCLIENTESBLK_Service().getZULWSFATURACAOCLIENTESBLK();
        return prov;
    }

    private class DocumentStatusWrapper {
        private FinantialInstitution finantialInstitution;
        private ZulwsdocumentStatusWs1 itemStatus;

        private DocumentStatusWrapper(final FinantialInstitution finantialInstitution, ZulwsdocumentStatusWs1 itemStatus) {
            this.finantialInstitution = finantialInstitution;
            this.itemStatus = itemStatus;
        }

        private String getIntegrationStatus() {
            return this.itemStatus.getIntegrationStatus();
        }

        private String getErrorDescription() {
            return this.itemStatus.getErrorDescription();
        }

        private String getDocumentNumber() {
            return this.itemStatus.getDocumentNumber();
        }

        private String getSapDocumentNumber() {
            return this.itemStatus.getSapDocumentNumber();
        }

        private boolean isSettlementNote() {
            final FinantialDocument fd =
                    FinantialDocument.findByUiDocumentNumber(finantialInstitution, this.itemStatus.getDocumentNumber());

            if (fd == null) {
                return false;
            }

            return fd.isSettlementNote();
        }

        private boolean isReimbursement() {
            final FinantialDocument fd =
                    FinantialDocument.findByUiDocumentNumber(finantialInstitution, this.itemStatus.getDocumentNumber());

            if (fd == null) {
                return false;
            }

            return fd.isSettlementNote() && ((SettlementNote) fd).isReimbursement();
        }

        private StatusType integrationStatus() {
            return convertToStatusType(this, false, false);
        }

    }

}
