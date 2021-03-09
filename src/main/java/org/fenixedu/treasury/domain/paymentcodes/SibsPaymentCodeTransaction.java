package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class SibsPaymentCodeTransaction extends SibsPaymentCodeTransaction_Base {

    public static final String DATE_TIME_FORMAT = "yyyyMMddHHmm";

    public SibsPaymentCodeTransaction() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setResponsibleUsername(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());
        setCreationDate(new DateTime());
    }

    protected SibsPaymentCodeTransaction(SibsReportFile reportFile, SibsPaymentRequest sibsPaymentRequest, DateTime paymentDate,
            BigDecimal paidAmount, String sibsTransactionId, DateTime sibsProcessingDate,
            Set<SettlementNote> settlementNotes) {
        this();

        String entityReferenceCode =
                sibsPaymentRequest.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode();

        String referenceCode = sibsPaymentRequest.getReferenceCode();
        String transactionId =
                String.format("%s-%s-%s", entityReferenceCode, referenceCode, paymentDate.toString(DATE_TIME_FORMAT));
        this.init(sibsPaymentRequest, transactionId, paymentDate, paidAmount, settlementNotes);

        setSibsEntityReferenceCode(entityReferenceCode);
        setSibsPaymentReferenceCode(referenceCode);
        setSibsTransactionId(sibsTransactionId);

        setSibsReportFile(reportFile);

        checkRules();
    }

    protected SibsPaymentCodeTransaction(SibsPaymentRequest sibsPaymentRequest, DateTime paymentDate, BigDecimal paidAmount,
            String referenceCode, String sibsTransactionId, Set<SettlementNote> settlementNotes) {
        this();

        String entityReferenceCode =
                sibsPaymentRequest.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode();

        String transactionId =
                String.format("%s-%s-%s", entityReferenceCode, referenceCode, getPaymentDate().toString(DATE_TIME_FORMAT));
        this.init(sibsPaymentRequest, transactionId, paymentDate, paidAmount, settlementNotes);

        setSibsEntityReferenceCode(entityReferenceCode);
        setSibsPaymentReferenceCode(referenceCode);
        setSibsTransactionId(sibsTransactionId);

        checkRules();
    }

    public void checkRules() {
        super.checkRules();

        if (StringUtils.isEmpty(getSibsEntityReferenceCode())) {
            throw new TreasuryDomainException("error.SibsPaymentCodeTransaction.sibsEntityReferenceCode.required");
        }

        if (StringUtils.isEmpty(getSibsPaymentReferenceCode())) {
            throw new TreasuryDomainException("error.SibsPaymentCodeTransaction.sibsPaymentReferenceCode.required");
        }

        if (find(getSibsEntityReferenceCode(), getSibsPaymentReferenceCode(), getPaymentDate()).count() > 1) {
            throw new TreasuryDomainException("error.SibsPaymentCodeTransaction.transaction.duplicate",
                    getSibsEntityReferenceCode(), getSibsPaymentReferenceCode(),
                    getPaymentDate().toString(TreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD));
        }
    }

    // @formatter:off
    /*
     * 
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<SibsPaymentCodeTransaction> findAll() {
        return FenixFramework.getDomainRoot().getPaymentTransactionsSet().stream()
                .filter(t -> t instanceof SibsPaymentCodeTransaction).map(SibsPaymentCodeTransaction.class::cast);
    }

    public static boolean isReferenceProcessingDuplicate(String entityReferenceCode, String referenceCode, DateTime paymentDate) {
        return find(entityReferenceCode, referenceCode, paymentDate).findAny().isPresent();
    }

    private static Stream<SibsPaymentCodeTransaction> find(String entityReferenceCode, String referenceCode,
            DateTime paymentDate) {
        return findAll().filter(x -> entityReferenceCode.equals(x.getSibsEntityReferenceCode())
                && referenceCode.equals(x.getSibsPaymentReferenceCode()) 
                && paymentDate.equals(x.getPaymentDate()));
    }

    public static Stream<SibsPaymentCodeTransaction> findBySibsGatewayTransactionId(String sibsTransactionId) {
        return findAll().filter(i -> sibsTransactionId.equalsIgnoreCase(i.getSibsTransactionId()));
    }

    public static boolean isSibsGatewayReferenceProcessingDuplicate(String sibsTransactionId) {
        return findBySibsGatewayTransactionId(sibsTransactionId).count() > 0;
    }

    public static SibsPaymentCodeTransaction create(SibsReportFile reportFile, SibsPaymentRequest sibsPaymentRequest,
            DateTime paymentDate, BigDecimal paidAmount, String sibsTransactionId, DateTime sibsProcessingDate, 
            Set<SettlementNote> settlementNotes) {
        String entityReferenceCode =
                sibsPaymentRequest.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode();
        String referenceCode = sibsPaymentRequest.getReferenceCode();
        
        if (SibsPaymentCodeTransaction.isReferenceProcessingDuplicate(entityReferenceCode, referenceCode, paymentDate)) {
            throw new TreasuryDomainException("error.SibsPaymentCodeTransaction.transaction.duplicate", entityReferenceCode,
                    referenceCode, paymentDate.toString(TreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD));
        }

        return new SibsPaymentCodeTransaction(reportFile, sibsPaymentRequest, paymentDate, paidAmount,
                sibsTransactionId, sibsProcessingDate, settlementNotes);
    }

    public static SibsPaymentCodeTransaction create(SibsPaymentRequest sibsPaymentRequest, DateTime paymentDate,
            BigDecimal paidAmount, String referenceCode, String sibsTransactionId, Set<SettlementNote> settlementNotes) {
        String entityReferenceCode =
                sibsPaymentRequest.getDigitalPaymentPlatform().castToSibsPaymentCodePoolService().getEntityReferenceCode();

        if (SibsPaymentCodeTransaction.isReferenceProcessingDuplicate(entityReferenceCode, referenceCode, paymentDate)) {
            throw new TreasuryDomainException("error.SibsPaymentCodeTransaction.transaction.duplicate", entityReferenceCode,
                    referenceCode, paymentDate.toString(TreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD));
        }

        return new SibsPaymentCodeTransaction(sibsPaymentRequest, paymentDate, paidAmount, referenceCode, sibsTransactionId,
                settlementNotes);
    }

}
