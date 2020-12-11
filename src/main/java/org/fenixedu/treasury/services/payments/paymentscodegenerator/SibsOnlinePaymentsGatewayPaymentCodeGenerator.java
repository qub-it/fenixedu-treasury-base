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
package org.fenixedu.treasury.services.payments.paymentscodegenerator;

import java.math.BigDecimal;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.MbCheckoutResultBean;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.MultipleEntriesPaymentCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCode;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGateway;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGatewayLog;
import org.fenixedu.treasury.dto.document.managepayments.PaymentReferenceCodeBean;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class SibsOnlinePaymentsGatewayPaymentCodeGenerator implements IPaymentCodeGenerator {

    private PaymentCodePool paymentCodePool;

    public SibsOnlinePaymentsGatewayPaymentCodeGenerator(final PaymentCodePool paymentCodePool) {
        this.paymentCodePool = paymentCodePool;
    }

    @Override
    @Atomic(mode = TxMode.READ)
    public PaymentReferenceCode createPaymentReferenceCode(final DebtAccount debtAccount, final PaymentReferenceCodeBean bean) {

        final BigDecimal paymentAmount = bean.getPaymentAmount();

        final PaymentReferenceCode paymentReferenceCode = generateNewCodeFor(debtAccount, paymentAmount, new LocalDate(),
                new LocalDate().plusMonths(paymentCodePool.getSibsOnlinePaymentsGateway().getNumberOfMonthsToExpirePaymentReferenceCode()), 
                Sets.newHashSet(bean.getSelectedDebitEntries()));

        return paymentReferenceCode;
    }

    private PaymentReferenceCode generateNewCodeFor(final DebtAccount debtAccount, final BigDecimal amount, LocalDate validFrom,
            LocalDate validTo, Set<DebitEntry> selectedDebitEntries) {
        if (!Boolean.TRUE.equals(this.paymentCodePool.getActive())) {
            throw new TreasuryDomainException("error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.paymentCodePool.not.active");
        }

        for (DebitEntry debitEntry : selectedDebitEntries) {
            final long activePaymentCodesOnDebitEntryCount = 
                    MultipleEntriesPaymentCode.findNewByDebitEntry(debitEntry).count()
                    + MultipleEntriesPaymentCode.findUsedByDebitEntry(debitEntry).count();
            
            if (activePaymentCodesOnDebitEntryCount >= MultipleEntriesPaymentCode.MAX_PAYMENT_CODES_FOR_DEBIT_ENTRY) {
                throw new TreasuryDomainException("error.MultipleEntriesPaymentCode.debit.entry.with.active.payment.code",
                        debitEntry.getDescription());
            }
        }
        
        final SibsOnlinePaymentsGateway sibsGateway = this.paymentCodePool.getSibsOnlinePaymentsGateway();
        final String merchantTransactionId = sibsGateway.generateNewMerchantTransactionId();

        final SibsOnlinePaymentsGatewayLog log = createLog(sibsGateway, debtAccount);

        try {

            FenixFramework.atomic(() -> {
                log.saveMerchantTransactionId(merchantTransactionId);
                log.logRequestSendDate();
            });

            final MbCheckoutResultBean checkoutResultBean =
                    sibsGateway.generateMBPaymentReference(amount, validFrom.toDateTimeAtStartOfDay(),
                            validTo.toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1), merchantTransactionId);

            final String sibsReferenceId = checkoutResultBean.getTransactionId();
            FenixFramework.atomic(() -> {
                log.logRequestReceiveDateAndData(checkoutResultBean.getTransactionId(), checkoutResultBean.isOperationSuccess(),
                        false, checkoutResultBean.getPaymentGatewayResultCode(),
                        checkoutResultBean.getOperationResultDescription());
                log.saveRequestAndResponsePayload(checkoutResultBean.getRequestLog(), checkoutResultBean.getResponseLog());
                log.savePaymentTypeAndBrand(
                        checkoutResultBean.getPaymentType() != null ? checkoutResultBean.getPaymentType().name() : null, 
                        checkoutResultBean.getPaymentBrand() != null ? checkoutResultBean.getPaymentBrand().name() : null);
            });

            if (!checkoutResultBean.isOperationSuccess()) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.request.not.successful");
            }

            final String paymentCode = checkoutResultBean.getPaymentReference();

            if (Strings.isNullOrEmpty(paymentCode)) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.reference.not.empty");
            }
            
            if (PaymentReferenceCode.findByReferenceCode(this.paymentCodePool.getEntityReferenceCode(), paymentCode,
                    this.paymentCodePool.getFinantialInstitution()).count() >= 1) {
                throw new TreasuryDomainException("error.PaymentReferenceCode.referenceCode.duplicated");
            }

            if (!StringUtils.isEmpty(merchantTransactionId)) {
                if (PaymentReferenceCode.findBySibsMerchantTransactionId(merchantTransactionId).count() >= 1) {
                    throw new TreasuryDomainException("error.PaymentReferenceCode.sibsMerchantTransaction.found.duplicated");
                }
            }

            if (!StringUtils.isEmpty(sibsReferenceId)) {
                if (PaymentReferenceCode.findBySibsReferenceId(sibsReferenceId).count() >= 1) {
                    throw new TreasuryDomainException("error.PaymentReferenceCode.sibsReferenceId.found.duplicated");
                }
            }
            
            return createPaymentReferenceCodeInstance(amount, validFrom, validTo, log, paymentCode, merchantTransactionId,
                    sibsReferenceId, selectedDebitEntries);
        } catch (final Exception e) {
            final boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

            saveExceptionLog(log, e, isOnlinePaymentsGatewayException);

            if (e instanceof TreasuryDomainException) {
                throw (TreasuryDomainException) e;
            } else {
                String message =
                        isOnlinePaymentsGatewayException ? "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.gateway.communication" : "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.unknown";

                throw new TreasuryDomainException(e, message);
            }
        }
    }

    @Atomic(mode = TxMode.WRITE)
    private void saveExceptionLog(final SibsOnlinePaymentsGatewayLog log, final Exception e,
            final boolean isOnlinePaymentsGatewayException) {
        log.logRequestReceiveDateAndData(null, false, false, null, null);
        log.markExceptionOccuredAndSaveLog(e);

        if (isOnlinePaymentsGatewayException) {
            log.saveRequestAndResponsePayload(((OnlinePaymentsGatewayCommunicationException) e).getRequestLog(),
                    ((OnlinePaymentsGatewayCommunicationException) e).getResponseLog());
        }
    }

    @Atomic(mode = TxMode.WRITE)
    private PaymentReferenceCode createPaymentReferenceCodeInstance(final BigDecimal amount, final LocalDate validFrom,
            final LocalDate validTo, final SibsOnlinePaymentsGatewayLog log, final String paymentCode,
            final String sibsMerchantTransactionId, final String sibsReferenceId, final Set<DebitEntry> selectedDebitEntries) throws Exception {
        log.savePaymentCode(paymentCode);

        PaymentReferenceCode paymentReferenceCode = PaymentReferenceCode.createForSibsOnlinePaymentGateway(paymentCode, validFrom, validTo,
                PaymentReferenceCodeStateType.USED, this.paymentCodePool, amount, sibsMerchantTransactionId, sibsReferenceId);

        paymentReferenceCode.createPaymentTargetTo(selectedDebitEntries, amount);
        return paymentReferenceCode;
    }

    @Atomic(mode = TxMode.WRITE)
    private SibsOnlinePaymentsGatewayLog createLog(final SibsOnlinePaymentsGateway sibsGateway, final DebtAccount debtAccount) {
        return SibsOnlinePaymentsGatewayLog.createLogForRequestPaymentCode(sibsGateway, debtAccount);
    }

    @Override
    public PaymentCodePool getReferenceCodePool() {
        return this.paymentCodePool;
    }

    @Override
    public boolean isSibsMerchantTransactionAndReferenceIdRequired() {
        return true;
    }

}
