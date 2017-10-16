package org.fenixedu.treasury.domain.integration;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;;

public class ERPExportOperation extends ERPExportOperation_Base {

    public static final Comparator<ERPExportOperation> COMPARE_BY_VERSIONING_CREATION_DATE =
            new Comparator<ERPExportOperation>() {

                @Override
                public int compare(final ERPExportOperation o1, final ERPExportOperation o2) {
                    int c = Comparator.comparing(IntegrationOperation::getVersioningCreationDate).compare(o1, o2);
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

    public static Stream<ERPExportOperation> findAll() {
        Set<ERPExportOperation> results = new HashSet<ERPExportOperation>();
        for (FinantialInstitution fi : pt.ist.fenixframework.FenixFramework.getDomainRoot().getFinantialInstitutionsSet()) {
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
