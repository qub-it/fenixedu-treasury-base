package org.fenixedu.treasury.services.payments.paymentscodegenerator;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.dto.document.managepayments.PaymentReferenceCodeBean;

@Deprecated
public interface IPaymentCodeGenerator {

    default public boolean canGenerateNewCode(boolean forceGeneration) {
        if(!getReferenceCodePool().isGenerateReferenceCodeOnDemand() && !forceGeneration) {
            return false;
        }
        
        return getReferenceCodePool().getNextReferenceCode() < getReferenceCodePool().getMaxReferenceCode();
    }
    
    public PaymentCodePool getReferenceCodePool();

    public PaymentReferenceCode createPaymentReferenceCode(final DebtAccount debtAccount, PaymentReferenceCodeBean bean);

    public boolean isSibsMerchantTransactionAndReferenceIdRequired();
    
}
