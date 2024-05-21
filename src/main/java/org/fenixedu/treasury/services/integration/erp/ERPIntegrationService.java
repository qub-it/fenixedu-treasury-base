/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
 *
 * 
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.treasury.services.integration.erp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.document.reimbursement.ReimbursementProcessStatusType;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.domain.integration.ERPImportOperation;
import org.fenixedu.treasury.domain.integration.IntegrationOperationLogBean;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentStatusWS.StatusType;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationInput;
import org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationOutput;
import org.fenixedu.treasury.services.integration.erp.dto.IntegrationStatusOutput;
import org.fenixedu.treasury.services.integration.erp.dto.InterestRequestValueInput;
import org.fenixedu.treasury.services.integration.erp.dto.InterestRequestValueOuptut;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.qubit.solution.fenixedu.bennu.webservices.services.server.BennuWebService;

@WebService
public class ERPIntegrationService extends BennuWebService {

    public static boolean validate(String username, String password) {
        Optional<FinantialInstitution> findUniqueByFiscalCode = FinantialInstitution.findUniqueByFiscalCode(username);
        if (findUniqueByFiscalCode.isPresent()) {

            ERPConfiguration configuration = findUniqueByFiscalCode.get().getErpIntegrationConfiguration();
            if (configuration != null) {
                return password.compareToIgnoreCase(configuration.getPassword()) == 0;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.fenixedu.treasury.services.integration.erp.IERPIntegrationService#sendInfoOnline(org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationInput)
     */
    @WebMethod
    public DocumentsInformationOutput sendInfoOnline(DocumentsInformationInput documentsInformation) {
        validateRequestHeader(documentsInformation.getFinantialInstitution());

        FinantialInstitution finantialInstitution = validateFinantialInstitution(documentsInformation);
        //Integrate the information from XML SAFT
        DateTime now = new DateTime();
        String filename = finantialInstitution.getFiscalNumber() + "_" + now.toString() + ".xml";
        ERPImportOperation operation = ERPImportOperation.create(filename, documentsInformation.getData(), finantialInstitution,
                null, now, false, false, false);

        final IERPImporter erpImporter = finantialInstitution.getErpIntegrationConfiguration()
                .getERPExternalServiceImplementation().getERPImporter(operation.getFile().getStream());
        final DocumentsInformationOutput result = erpImporter.processAuditFile(operation);
        return result;
    }

    private FinantialInstitution validateFinantialInstitution(DocumentsInformationInput documentsInformation) {
        FinantialInstitution finantialInstitution =
                FinantialInstitution.findUniqueByFiscalCode(documentsInformation.getFinantialInstitution()).orElse(null);

        if (finantialInstitution == null) {
            throw new TreasuryDomainException("error.integration.erp.invalid.fiscalinstitution");
        }
        return finantialInstitution;
    }

    /* (non-Javadoc)
     * @see org.fenixedu.treasury.services.integration.erp.IERPIntegrationService#sendInfoOffline(org.fenixedu.treasury.services.integration.erp.dto.DocumentsInformationInput)
     */
    @WebMethod
    public String sendInfoOffline(DocumentsInformationInput documentsInformation) {
        validateRequestHeader(documentsInformation.getFinantialInstitution());

        FinantialInstitution finantialInstitution = validateFinantialInstitution(documentsInformation);
        //Integrate the information from XML SAFT
        DateTime now = new DateTime();
        String filename = finantialInstitution.getFiscalNumber() + "_" + now.toString() + ".xml";

        final IntegrationOperationLogBean logBean = new IntegrationOperationLogBean();
        ERPImportOperation operation = null;
        try {
            File externalFile = new File(documentsInformation.getDataURI());
            byte[] bytes = Files.toByteArray(externalFile);
            operation = ERPImportOperation.create(filename, bytes, finantialInstitution, null, now, false, false, false);

            IERPImporter erpImporter = finantialInstitution.getErpIntegrationConfiguration().getERPExternalServiceImplementation()
                    .getERPImporter(operation.getFile().getStream());
            erpImporter.processAuditFile(operation);

            return operation.getExternalId();
        } catch (Exception e) {
            if (operation != null) {
                logBean.appendErrorLog(e.getLocalizedMessage());
                return operation.getExternalId();
            }

            throw new RuntimeException(e);
        } finally {
            operation.appendLog(logBean.getErrorLog(), logBean.getIntegrationLog(), logBean.getSoapInboundMessage(),
                    logBean.getSoapOutboundMessage());
        }
    }

    /* (non-Javadoc)
     * @see org.fenixedu.treasury.services.integration.erp.IERPIntegrationService#getIntegrationStatusFor(java.lang.String)
     */
    @WebMethod
    public IntegrationStatusOutput getIntegrationStatusFor(String finantialInstitution, List<String> documentNumbers) {

        validateRequestHeader(finantialInstitution);
        IntegrationStatusOutput result = new IntegrationStatusOutput();
        List<DocumentStatusWS> statusList = new ArrayList<DocumentStatusWS>();
        for (String documentNumber : documentNumbers) {
//            IntegrationStatusOutput status = new IntegrationStatusOutput();
            DocumentStatusWS docStatus = new DocumentStatusWS();
            FinantialDocument document = FinantialDocument.findByUiDocumentNumber(
                    FinantialInstitution.findUniqueByFiscalCode(finantialInstitution).orElse(null), documentNumber);

            if (document == null) {
                docStatus.setIntegrationStatus(StatusType.ERROR);
            } else {
                docStatus.setIntegrationStatus(StatusType.SUCCESS);
            }
//            status.setDocumentStatus(docStatus);
            statusList.add(docStatus);
        }
        result.setDocumentStatus(statusList);
        result.setRequestId(finantialInstitution);
        return result;
    }

    @WebMethod
    @Deprecated
    /*
     * ANIL 2023-05-11: Not used anymore
     */
    public InterestRequestValueOuptut getInterestValueFor(InterestRequestValueInput interestRequest) {
        throw new RuntimeException("deprecated");
    }

    @WebMethod(operationName = "reimbursementStateChange")
    public IntegrationStatusOutput processReimbursementStateChange(
            @WebParam(name = "finantialInstitution") final String finantialInstitutionFiscalNumber,
            @WebParam(name = "finantialDocument") final String finantialDocumentNumber,
            @WebParam(name = "exerciseYear") final String exerciseYear,
            @WebParam(name = "reimbursementStatus") final String reimbursementStatusCode,
            @WebParam(name = "reimbursementStatusDate") final java.util.Calendar reimbursementStatusDate) {

        if (Strings.isNullOrEmpty(finantialInstitutionFiscalNumber)) {
            throw new TreasuryDomainException("error.integration.erp.invalid.fiscalInstitution");
        }

        if (Strings.isNullOrEmpty(finantialDocumentNumber)) {
            throw new TreasuryDomainException("error.integration.erp.invalid.fiscalInstitution");
        }

        if (Strings.isNullOrEmpty(exerciseYear)) {
            throw new TreasuryDomainException("error.integration.erp.invalid.yearExercise");
        }

        try {
            Integer.valueOf(exerciseYear);
        } catch (final NumberFormatException e) {
            throw new TreasuryDomainException("error.integration.erp.invalid.yearExercise");
        }

        if (Strings.isNullOrEmpty(reimbursementStatusCode)
                || !ReimbursementProcessStatusType.findUniqueByCode(reimbursementStatusCode).isPresent()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementStatus");
        }

        if (reimbursementStatusDate == null) {
            throw new TreasuryDomainException("error.integration.erp.invalid.stateDate");
        }

        if (!FinantialInstitution.findUniqueByFiscalCode(finantialInstitutionFiscalNumber).isPresent()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.fiscalInstitution");
        }

        final FinantialInstitution finantialInstitution =
                FinantialInstitution.findUniqueByFiscalCode(finantialInstitutionFiscalNumber).get();

        if (!FinantialDocument.findUniqueByDocumentNumber(finantialDocumentNumber).isPresent()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.settlementNote");
        }

        final FinantialDocument finantialDocument = FinantialDocument.findUniqueByDocumentNumber(finantialDocumentNumber).get();

        if (!finantialDocument.isSettlementNote()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.settlementNote");
        }

        final SettlementNote settlementNote = (SettlementNote) finantialDocument;

        if (!settlementNote.isReimbursement()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.settlementNote");
        }

        if (settlementNote.getDebtAccount().getFinantialInstitution() != finantialInstitution) {
            throw new TreasuryDomainException("error.integration.erp.invalid.settlementNote.not.of.finantialInstitution");
        }

        if (!settlementNote.isClosed()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementNote.state");
        }

        if (settlementNote.getCurrentReimbursementProcessStatus() == null) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementNote.current.status.invalid");
        }

        if (settlementNote.getCurrentReimbursementProcessStatus().isFinalStatus()) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementNote.current.status.is.final");
        }

        final ReimbursementProcessStatusType reimbursementStatus =
                ReimbursementProcessStatusType.findUniqueByCode(reimbursementStatusCode).get();

        if (!reimbursementStatus.isAfter(settlementNote.getCurrentReimbursementProcessStatus())) {
            throw new TreasuryDomainException("error.integration.erp.invalid.reimbursementNote.next.status.invalid");
        }

        //settlementNote.processReimbursementStateChange(reimbursementStatus, erpProcessId, exerciseYear, new DateTime(reimbursementStatusDate));

        final DocumentStatusWS documentStatusWs = new DocumentStatusWS();
        documentStatusWs.setDocumentNumber(finantialDocumentNumber);
        documentStatusWs.setErrorDescription("");
        documentStatusWs.setIntegrationStatus(StatusType.SUCCESS);

        final IntegrationStatusOutput output = new IntegrationStatusOutput();

        output.setRequestId("");
        output.setDocumentStatus(Lists.newArrayList(documentStatusWs));

        return output;
    }

    private void validateRequestHeader(String finantialInstitution) {
//        if (finantialInstitution == null || getSecurityHeader() == null
//                || !finantialInstitution.equalsIgnoreCase(getSecurityHeader().getUsername())) {
//            throw new SecurityException("invalid request permission");
//        }
    }

}
