package org.fenixedu.treasury.domain.integration;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;;

public class ERPImportOperation extends ERPImportOperation_Base {

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
        for (FinantialInstitution fi : pt.ist.fenixframework.FenixFramework.getDomainRoot().getFinantialInstitutionsSet()) {
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
