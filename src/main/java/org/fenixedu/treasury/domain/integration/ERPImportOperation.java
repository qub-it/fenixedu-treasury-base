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

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ERPImportOperation extends ERPImportOperation_Base {

    public static final Comparator<ERPImportOperation> COMPARE_BY_VERSIONING_CREATION_DATE =
            new Comparator<ERPImportOperation>() {

                @Override
                public int compare(final ERPImportOperation o1, final ERPImportOperation o2) {
                    int c = TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(o1)
                            .compareTo(TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(o2));
                    return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
                }
            };

    protected ERPImportOperation() {
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
            throw new TreasuryDomainException("error.ERPImportOperation.file.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.ERPImportOperation.finantialInstitution.required");
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
            throw new TreasuryDomainException("error.ERPImportOperation.cannot.delete");
        }

        this.setFinantialInstitution(null);
        for (FinantialDocument document : this.getFinantialDocumentsSet()) {
            this.removeFinantialDocuments(document);
        }
        super.delete();
    }

    @Atomic
    public static ERPImportOperation create(String filename, final byte[] bytes, final FinantialInstitution finantialInstitution,
            final String erpOperationId, final DateTime executionDate, final boolean processed, final boolean success,
            final boolean corrected) {
        ERPImportOperation eRPImportOperation = new ERPImportOperation();
        OperationFile file = OperationFile.create(filename, bytes, eRPImportOperation);
        eRPImportOperation.init(file, finantialInstitution, erpOperationId, executionDate, processed, success, corrected);
        return eRPImportOperation;
    }

    public static Stream<ERPImportOperation> findAll() {
        Set<ERPImportOperation> results = new HashSet<ERPImportOperation>();
        for (FinantialInstitution fi : FenixFramework.getDomainRoot().getFinantialInstitutionsSet()) {
            results.addAll(fi.getIntegrationOperationsSet().stream().filter(x -> x instanceof ERPImportOperation)
                    .map(ERPImportOperation.class::cast).collect(Collectors.toList()));
        }
        return results.stream();
    }

    public static Stream<ERPImportOperation> findByFile(final OperationFile file) {
        final Set<ERPImportOperation> result = Sets.newHashSet();

        final IntegrationOperation operation = file.getIntegrationOperation();
        if (operation != null && operation instanceof ERPImportOperation) {
            result.add((ERPImportOperation) operation);
        }

        return result.stream();
    }

    public static Stream<ERPImportOperation> findByFinantialInstitution(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getIntegrationOperationsSet().stream().filter(x -> x instanceof ERPImportOperation)
                .map(ERPImportOperation.class::cast);
    }

    public static Stream<ERPImportOperation> find(final FinantialDocument finantialDocument) {
        return finantialDocument.getErpImportOperationsSet().stream();
    }

    public static Stream<ERPImportOperation> findByExecutionDate(final DateTime executionDate) {
        return findAll().filter(i -> executionDate.equals(i.getExecutionDate()));
    }

    public static Stream<ERPImportOperation> findByProcessed(final boolean processed) {
        return findAll().filter(i -> processed == i.getProcessed());
    }

    public static Stream<ERPImportOperation> findBySuccess(final boolean success) {
        return findAll().filter(i -> success == i.getSuccess());
    }

    public static Stream<ERPImportOperation> findByCorrected(final boolean corrected) {
        return findAll().filter(i -> corrected == i.getCorrected());
    }

    public static Stream<ERPImportOperation> findByErrorLog(final String errorLog) {
        return findAll().filter(i -> errorLog.equalsIgnoreCase(i.getErrorLog()));
    }

}
