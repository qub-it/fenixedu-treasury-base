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
package org.fenixedu.treasury.domain.integration;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ERPExportOperation extends ERPExportOperation_Base {

    public static final Comparator<ERPExportOperation> COMPARE_BY_VERSIONING_CREATION_DATE =
            new Comparator<ERPExportOperation>() {

                @Override
                public int compare(final ERPExportOperation o1, final ERPExportOperation o2) {
                    int c = TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(o1)
                            .compareTo(TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(o2));
                    return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
                }
            };

    protected ERPExportOperation() {
        super();
    }

    protected void init(final OperationFile file, final FinantialInstitution finantialInstitution, final String erpOperationId,
            final DateTime executionDate, final boolean processed, final boolean success, final boolean corrected) {
        setFile(file);

        setFinantialInstitution(finantialInstitution);
        setErpOperationId(erpOperationId);
        setExecutionDate(executionDate);
        setProcessed(processed);
        setSuccess(success);
        setCorrected(corrected);

        checkRules();
    }

    private void checkRules() {
        if (getFile() == null) {
            throw new TreasuryDomainException("error.ERPExportOperation.file.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.ERPExportOperation.finantialInstitution.required");
        }
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Override
    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.ERPExportOperation.cannot.delete");
        }
        this.setFinantialInstitution(null);

        for (FinantialDocument document : this.getFinantialDocumentsSet()) {
            this.removeFinantialDocuments(document);
        }

        super.delete();
    }

    @Atomic
    public static ERPExportOperation create(final byte[] data, final String filename,
            final FinantialInstitution finantialInstitution, final String erpOperationId, final DateTime executionDate,
            final boolean processed, final boolean success, final boolean corrected) {
        ERPExportOperation eRPExportOperation = new ERPExportOperation();
        OperationFile file;
        if (data == null) {
            file = OperationFile.create(filename, new byte[0], eRPExportOperation);
        } else {
            file = OperationFile.create(filename, data, eRPExportOperation);
        }
        eRPExportOperation.init(file, finantialInstitution, erpOperationId, executionDate, processed, success, corrected);

        return eRPExportOperation;
    }

    @Atomic
    public static ERPExportOperation copy(final ERPExportOperation log) {
        final byte[] data = log.getFile().getContent();
        final String filename = log.getFile().getFilename();
        final FinantialInstitution finantialInstitution = log.getFinantialInstitution();
        final String erpOperationId = log.getErpOperationId();
        final DateTime executionDate = log.getExecutionDate();
        final boolean processed = log.getProcessed();
        final boolean success = log.getSuccess();
        final boolean corrected = log.getCorrected();

        final ERPExportOperation copy =
                create(data, filename, finantialInstitution, erpOperationId, executionDate, processed, success, corrected);

        copy.appendLog(log.getErrorLog(), log.getIntegrationLog(), log.getSoapInboundMessage(), log.getSoapOutboundMessage());
        copy.getFinantialDocumentsSet().addAll(log.getFinantialDocumentsSet());

        return copy;
    }

    public static Stream<ERPExportOperation> findAll() {
        Set<ERPExportOperation> results = new HashSet<ERPExportOperation>();
        for (FinantialInstitution fi : FenixFramework.getDomainRoot().getFinantialInstitutionsSet()) {
            results.addAll(fi.getIntegrationOperationsSet().stream().filter(x -> x instanceof ERPExportOperation)
                    .map(ERPExportOperation.class::cast).collect(Collectors.toList()));
        }
        return results.stream();
    }

    // TODO legidio, shouldn't be just file.getIntegrationOperation(), if instanceof ERPExportOperation?
    public static Stream<ERPExportOperation> findByFile(final FinantialInstitution finantialInstitution,
            final OperationFile file) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> file.equals(i.getFile()));
    }

    public static Stream<ERPExportOperation> findByFinantialInstitution(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getIntegrationOperationsSet().stream().filter(x -> x instanceof ERPExportOperation)
                .map(ERPExportOperation.class::cast);
    }

    public static Stream<ERPExportOperation> find(final FinantialDocument finantialDocument) {
        return finantialDocument.getErpExportOperationsSet().stream();
    }

    public static Stream<ERPExportOperation> findByExecutionDate(final FinantialInstitution finantialInstitution,
            final DateTime executionDate) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> executionDate.equals(i.getExecutionDate()));
    }

    public static Stream<ERPExportOperation> findByProcessed(final FinantialInstitution finantialInstitution,
            final boolean processed) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> processed == i.getProcessed());
    }

    public static Stream<ERPExportOperation> findBySuccess(final FinantialInstitution finantialInstitution,
            final boolean success) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> success == i.getSuccess());
    }

    public static Stream<ERPExportOperation> findByCorrected(final FinantialInstitution finantialInstitution,
            final boolean corrected) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> corrected == i.getCorrected());
    }

    public static Stream<ERPExportOperation> findByErrorLog(final FinantialInstitution finantialInstitution,
            final java.lang.String errorLog) {
        return findByFinantialInstitution(finantialInstitution).filter(i -> errorLog.equalsIgnoreCase(i.getErrorLog()));
    }

}
