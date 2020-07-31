package org.fenixedu.treasury.domain.payments;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.SibsPaymentCodeTransaction;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class PaymentTransaction extends PaymentTransaction_Base {

    public PaymentTransaction() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
        setResponsibleUsername(TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername());
    }

    protected PaymentTransaction(PaymentRequest paymentRequest, String transactionId, DateTime paymentDate, BigDecimal paidAmount,
            Set<SettlementNote> settlementNotes) {
        this();

        if (isTransactionDuplicate(transactionId)) {
            throw new TreasuryDomainException("error.PaymentTransaction.transaction.duplicate", transactionId);
        }
        
        this.init(paymentRequest, transactionId, paymentDate, paidAmount, settlementNotes);
        
        checkRules();
    }
    
    protected void init(PaymentRequest paymentRequest, String transactionId, DateTime paymentDate, BigDecimal paidAmount,
            Set<SettlementNote> settlementNotes) {

        setPaymentRequest(paymentRequest);
        setPaymentDate(paymentDate);
        setTransactionId(transactionId);
        setPaidAmount(paidAmount);
        getSettlementNotesSet().addAll(settlementNotes);

    }
    
    public void checkRules() {
        
        if(getPaymentRequest() == null) {
            throw new TreasuryDomainException("error.PaymentTransaction.paymentRequest.required");
        }
        
        if(getPaymentDate() == null) {
            throw new TreasuryDomainException("error.PaymentTransaction.paymentDate.required");
        }
        
        if(getPaidAmount() == null) {
            throw new TreasuryDomainException("error.PaymentTransaction.paidAmount.required");
        }
        
        if(!TreasuryConstants.isPositive(getPaidAmount())) {
            throw new TreasuryDomainException("error.PaymentTransaction.paidAmount.invalid");
        }
        
        if(findByTransactionId(getTransactionId()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentTransaction.transaction.duplicate", getTransactionId());
        }
    }
    
    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<? extends PaymentTransaction> findAll() {
        return FenixFramework.getDomainRoot().getPaymentTransactionsSet().stream();
    }
    
    public static Stream<? extends PaymentTransaction> findByTransactionId(String transactionId) {
        return findAll().filter(t -> transactionId.equalsIgnoreCase(t.getTransactionId()));
    }
    
    public static boolean isTransactionDuplicate(String transactionId) {
        return findByTransactionId(transactionId).findAny().isPresent();
    }
    
    public static PaymentTransaction create(PaymentRequest paymentRequest, String transactionId, DateTime paymentDate, BigDecimal paidAmount,
            Set<SettlementNote> settlementNotes) {
        return new PaymentTransaction(paymentRequest, transactionId, paymentDate, paidAmount, settlementNotes);
    }
}
