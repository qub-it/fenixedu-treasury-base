package org.fenixedu.treasury.domain.sibsonlinepaymentsgateway;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class MbwayTransaction extends MbwayTransaction_Base {

    public MbwayTransaction() {
        super();
        setCreationDate(new DateTime());
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public MbwayTransaction(MbwayPaymentRequest mbwayPaymentRequest, String sibsTransactionId, BigDecimal amount,
            DateTime paymentDate, final Set<SettlementNote> settlementNotes) {
        this();

        setMbwayPaymentRequest(mbwayPaymentRequest);
        setAmount(amount);
        setSibsTransactionId(sibsTransactionId);
        setPaymentDate(paymentDate);
        getSettlementNotesSet().addAll(settlementNotes);
        
        checkRules();
    }

    private void checkRules() {
        
        if(getDomainRoot() == null) {
            throw new TreasuryDomainException("error.MbwayTransaction.domainRoot.required");
        }
        
        if(getMbwayPaymentRequest() == null) {
            throw new TreasuryDomainException("error.MbwayTransaction.mbwayPaymentRequest.required");
        }
        
        if(StringUtils.isEmpty(getSibsTransactionId())) {
            throw new TreasuryDomainException("error.MbwayTransaction.sibsTransactionId.required");
        }
        
        if(getPaymentDate() == null) {
            throw new TreasuryDomainException("error.MbwayTransaction.paymentDate.required");
        }
        
        if(getSettlementNotesSet().isEmpty()) {
            throw new TreasuryDomainException("error.MbwayTransaction.paymentDate.required");
        }
        
        if(getAmount() == null) {
            throw new TreasuryDomainException("error.MbwayTransaction.amount.required");
        }
        
        if(findBySibsTransactionId(getSibsTransactionId()).count() >= 2) {
            throw new TreasuryDomainException("error.MbwayTransaction.sibsTransactionId.already.registered");
        }
        
    }
    
    /* ************ */
    /* * SERVICES * */
    /* ************ */
    
    public static MbwayTransaction create(final MbwayPaymentRequest mbwayPaymentRequest, final String sibsTransactionId,
            final BigDecimal amount, final DateTime paymentDate, final Set<SettlementNote> settlementNotes) {
        final MbwayTransaction mbwayTransaction =
                new MbwayTransaction(mbwayPaymentRequest, sibsTransactionId, amount, paymentDate, settlementNotes);

        return mbwayTransaction;
    }
    
    public static Stream<MbwayTransaction> findAll() {
        return FenixFramework.getDomainRoot().getMbwayPaymentTransactionsSet().stream();
    }
    
    public static Stream<MbwayTransaction> find(final MbwayPaymentRequest mbwayPaymentRequest) {
        return mbwayPaymentRequest.getMbwayTransactionsSet().stream();
    }
    
    public static Stream<MbwayTransaction> findBySibsTransactionId(final String sibsTransactionId) {
        return findAll().filter(t -> t.getSibsTransactionId().equals(sibsTransactionId));
    }
    
    public static boolean isTransactionProcessingDuplicate(final String sibsTransactionId) {
        return findBySibsTransactionId(sibsTransactionId).count() > 0;
    }

}
