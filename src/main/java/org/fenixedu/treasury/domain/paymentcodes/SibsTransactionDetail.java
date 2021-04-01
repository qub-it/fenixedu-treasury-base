package org.fenixedu.treasury.domain.paymentcodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class SibsTransactionDetail extends SibsTransactionDetail_Base {

    protected SibsTransactionDetail() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(final SibsReportFile sibsReport, final String comments, final DateTime whenProcessed,
            final DateTime whenRegistered, final java.math.BigDecimal amountPayed, final String sibsEntityReferenceCode,
            final String sibsPaymentReferenceCode, final String sibsTransactionId, final String debtAccountId,
            final String customerId, final String businessIdentification, final String fiscalNumber, final String customerName,
            final String settlementDocumentNumber) {
        setSibsReport(sibsReport);
        setComments(comments);
        setWhenProcessed(whenProcessed);
        setWhenRegistered(whenRegistered);
        setAmountPayed(amountPayed);
        setSibsEntityReferenceCode(sibsEntityReferenceCode);
        setSibsPaymentReferenceCode(sibsPaymentReferenceCode);
        setSibsTransactionId(sibsTransactionId);
        setDebtAccountId(debtAccountId);
        setCustomerId(customerId);
        setBusinessIdentification(businessIdentification);
        setFiscalNumber(fiscalNumber);
        setCustomerName(customerName);
        setSettlementDocumentNumber(settlementDocumentNumber);

        checkRules();
    }

    private void checkRules() {
        if(getDomainRoot() == null) {
            throw new TreasuryDomainException("error.SibsTransactionDetail.domainRoot.required");
        }
        
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        super.setSibsReport(null);
        deleteDomainObject();
    }

    @Atomic
    public static SibsTransactionDetail create(final SibsReportFile sibsReport, final String comments,
            final DateTime whenProcessed, final DateTime paymentDate, final java.math.BigDecimal amountPayed,
            final String sibsEntityReferenceCode, final String sibsPaymentReferenceCode, final String sibsTransactionId,
            final String debtAccountId, final String customerId, final String businessIdentification, final String fiscalNumber,
            final String customerName, final String settlementDocumentNumber) {
        SibsTransactionDetail sibsTransactionDetail = new SibsTransactionDetail();

        sibsTransactionDetail.init(sibsReport, comments, whenProcessed, paymentDate, amountPayed, sibsEntityReferenceCode,
                sibsPaymentReferenceCode, sibsTransactionId, debtAccountId, customerId, businessIdentification, fiscalNumber,
                customerName, settlementDocumentNumber);
        return sibsTransactionDetail;
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<SibsTransactionDetail> findAll() {

        Set<SibsTransactionDetail> result = new HashSet<>();
        List<SibsReportFile> reports = SibsReportFile.findAll().collect(Collectors.toList());
        for (SibsReportFile report : reports) {
            result.addAll(report.getSibsTransactionsSet());
        }
        
        result.addAll(FenixFramework.getDomainRoot().getSibsTransactionDetailSet());
        
        return result.stream();
    }

    public static Stream<SibsTransactionDetail> findBySibsReport(final SibsReportFile sibsReport) {
        return sibsReport.getSibsTransactionsSet().stream();
    }

    public static Stream<SibsTransactionDetail> findByComments(final String comments) {
        return findAll().filter(i -> comments.equalsIgnoreCase(i.getComments()));
    }

    public static Stream<SibsTransactionDetail> findByWhenProcessed(final DateTime whenProcessed) {
        return findAll().filter(i -> whenProcessed.equals(i.getWhenProcessed()));
    }

    public static Stream<SibsTransactionDetail> findByWhenRegistered(final DateTime whenRegistered) {
        return findAll().filter(i -> whenRegistered.equals(i.getWhenRegistered()));
    }

    public static Stream<SibsTransactionDetail> findByAmountPayed(final java.math.BigDecimal amountPayed) {
        return findAll().filter(i -> amountPayed.equals(i.getAmountPayed()));
    }

    public static Stream<SibsTransactionDetail> findBySibsEntityReferenceCode(final String sibsEntityReferenceCode) {
        return findAll().filter(i -> sibsEntityReferenceCode.equalsIgnoreCase(i.getSibsEntityReferenceCode()));
    }

    public static Stream<SibsTransactionDetail> findBySibsPaymentReferenceCode(final String sibsPaymentReferenceCode) {
        return findAll().filter(i -> sibsPaymentReferenceCode.equalsIgnoreCase(i.getSibsPaymentReferenceCode()));
    }

    public static Stream<SibsTransactionDetail> findBySibsTransactionId(final String sibsTransactionId) {
        return findAll().filter(i -> sibsTransactionId.equalsIgnoreCase(i.getSibsTransactionId()));
    }
    
    public static Stream<SibsTransactionDetail> findBySibsEntityAndReferenceCode(final String sibsEntityReferenceCode,
            final String sibsPaymentReferenceCode) {
        return findBySibsEntityReferenceCode(sibsEntityReferenceCode)
                .filter(i -> sibsPaymentReferenceCode.equalsIgnoreCase(i.getSibsPaymentReferenceCode()));
    }

    public static boolean isReferenceProcessingDuplicate(String referenceCode, String entityReferenceCode,
            DateTime whenRegistered) {

        return findAll().anyMatch(x -> x.getSibsEntityReferenceCode().equals(entityReferenceCode)
                && x.getSibsPaymentReferenceCode().equals(referenceCode) && x.getWhenRegistered().equals(whenRegistered));
    }
    
    public static boolean isSibsOppwaReferenceProcessingDuplicate(final String sibsTransactionId) {
        return findBySibsTransactionId(sibsTransactionId).count() > 0;
    }
    
}
