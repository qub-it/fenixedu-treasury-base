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
package org.fenixedu.treasury.domain.sibspaymentsgateway;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.MbWayCheckoutResultBean;
import org.fenixedu.onlinepaymentsgateway.api.PaymentStateBean;
import org.fenixedu.onlinepaymentsgateway.exceptions.OnlinePaymentsGatewayCommunicationException;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.domain.sibspaymentsgateway.integration.SibsPaymentsGateway;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class MbwayRequest extends MbwayRequest_Base {

    public MbwayRequest() {
        super();
    }

    protected MbwayRequest(DigitalPaymentPlatform platform, DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, BigDecimal payableAmount, String phoneNumber, String merchantTransactionId,
            String transactionId) {
        this();

        this.init(platform, debtAccount, debitEntries, installments, payableAmount,
                TreasurySettings.getInstance().getMbWayPaymentMethod());

        setPhoneNumber(phoneNumber);
        setMerchantTransactionId(merchantTransactionId);
        setTransactionId(transactionId);

        setState(PaymentReferenceCodeStateType.USED);

        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();

        if (StringUtils.isEmpty(getPhoneNumber())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phoneNumber.required");
        }

        if (StringUtils.isEmpty(getMerchantTransactionId())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsMerchantTransaction.required");
        }
    }

    @Atomic
    private Set<SettlementNote> processPayment(final BigDecimal paidAmount, DateTime paymentDate, String sibsTransactionId,
            String comments) {
        Function<PaymentRequest, Map<String, String>> additionalPropertiesMapFunction =
                (o) -> fillPaymentEntryPropertiesMap(sibsTransactionId);

        this.setState(PaymentReferenceCodeStateType.PROCESSED);

        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            return internalProcessPaymentInNormalPaymentMixingLegacyInvoices(paidAmount, paymentDate, sibsTransactionId, comments,
                    additionalPropertiesMapFunction);
        } else {
            return internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(paidAmount, paymentDate, sibsTransactionId,
                    comments, additionalPropertiesMapFunction);
        }
    }

    @Atomic(mode = TxMode.READ)
    public PaymentTransaction processMbwayTransaction(SibsPaymentsGatewayLog log, PaymentStateBean bean) {
        if (!bean.getMerchantTransactionId().equals(getMerchantTransactionId())) {
            throw new TreasuryDomainException(
                    "error.MbwayPaymentRequest.processMbwayTransaction.merchantTransactionId.not.equal");
        }

        final BigDecimal paidAmount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate();

        FenixFramework.atomic(() -> {
            log.savePaymentInfo(paidAmount, paymentDate);
        });

        if (paidAmount == null || !TreasuryConstants.isPositive(paidAmount)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.payment.date");
        }

        if (PaymentTransaction.isTransactionDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> {
                log.markAsDuplicatedTransaction();
            });
            return null;
        }

        try {
            return FenixFramework.atomic(() -> {
                final Set<SettlementNote> settlementNotes =
                        processPayment(paidAmount, paymentDate, bean.getTransactionId(), bean.getMerchantTransactionId());
                PaymentTransaction transaction =
                        PaymentTransaction.create(this, bean.getTransactionId(), paymentDate, paidAmount, settlementNotes);

                if(transaction != null) {
                    log.setPaymentTransaction(transaction);
                }
                
                return transaction;
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(String sibsTransactionId) {
        final Map<String, String> result = new HashMap<>();

        result.put("SibsTransactionId", sibsTransactionId);

        return result;
    }

    @Override
    public String fillPaymentEntryMethodId() {
        return null;
    }

    @Override
    public PaymentReferenceCodeStateType getCurrentState() {
        return super.getState();
    }

    @Override
    public boolean isInCreatedState() {
        return false;
    }

    @Override
    public boolean isInRequestedState() {
        return getState() == PaymentReferenceCodeStateType.USED;
    }

    @Override
    public boolean isInPaidState() {
        return getState() == PaymentReferenceCodeStateType.PROCESSED;
    }

    @Override
    public boolean isInAnnuledState() {
        return getState() == PaymentReferenceCodeStateType.ANNULLED;
    }

    /* ************ */
    /* * SERVICES * */
    /* ************ */

    public static Stream<MbwayRequest> findAll() {
        return PaymentRequest.findAll().filter(p -> p instanceof MbwayRequest).map(MbwayRequest.class::cast);
    }

    public static Stream<MbwayRequest> findBySibsGatewayMerchantTransactionId(String sibsGatewayMerchantTransactionId) {
        return PaymentRequest.findBySibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId)
                .filter(p -> p instanceof MbwayRequest).map(MbwayRequest.class::cast);
    }

    public static Optional<MbwayRequest> findUniqueBySibsGatewayMerchantTransactionId(String sibsGatewayMerchantTransactionId) {
        return findBySibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId).findAny();
    }

    public static Stream<? extends PaymentRequest> findBySibsGatewayTransactionId(String sibsGatewayTransactionId) {
        return PaymentRequest.findBySibsGatewayTransactionId(sibsGatewayTransactionId).filter(p -> p instanceof MbwayRequest)
                .map(MbwayRequest.class::cast);
    }

    public static Optional<? extends PaymentRequest> findUniqueBySibsGatewayTransactionId(String sibsGatewayTransactionId) {
        return findBySibsGatewayTransactionId(sibsGatewayTransactionId).findAny();
    }

    @Atomic(mode = TxMode.READ)
    public static MbwayRequest create(SibsPaymentsGateway sibsOnlinePaymentsGateway, DebtAccount debtAccount,
            Set<DebitEntry> debitEntries, Set<Installment> installments, String countryPrefix, String localPhoneNumber) {

        if (getReferencedCustomers(debitEntries, installments).size() > 1) {
            throw new TreasuryDomainException("error.PaymentRequest.referencedCustomers.only.one.allowed");
        }

        if (StringUtils.isEmpty(countryPrefix)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.countryPrefix.required");
        }

        if (StringUtils.isEmpty(localPhoneNumber)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.required");
        }

        if (!countryPrefix.matches("\\d+")) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.countryPrefix.number.format.required");
        }

        if (!localPhoneNumber.matches("\\d+")) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phone.number.format.required");
        }

        String phoneNumber = String.format("%s#%s", countryPrefix, localPhoneNumber);

        BigDecimal payableAmountDebitEntries =
                debitEntries.stream().map(e -> e.getOpenAmountWithInterests()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmountInstallments =
                installments.stream().map(i -> i.getOpenAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal payableAmount = payableAmountDebitEntries.add(payableAmountInstallments);

        String merchantTransactionId = sibsOnlinePaymentsGateway.generateNewMerchantTransactionId();

        SibsPaymentsGatewayLog log = createLog(merchantTransactionId);

        try {
            FenixFramework.atomic(() -> {
                log.logRequestSendDate();
            });

            final MbWayCheckoutResultBean checkoutResultBean =
                    sibsOnlinePaymentsGateway.generateMbwayReference(payableAmount, merchantTransactionId, phoneNumber);

            FenixFramework.atomic(() -> {
                log.logRequestReceiveDateAndData(checkoutResultBean.getTransactionId(), checkoutResultBean.isOperationSuccess(),
                        false, checkoutResultBean.getPaymentGatewayResultCode(),
                        checkoutResultBean.getOperationResultDescription());
                log.saveRequest(checkoutResultBean.getRequestLog());
                log.saveResponse(checkoutResultBean.getResponseLog());
            });

            if (!checkoutResultBean.isOperationSuccess()) {
                throw new TreasuryDomainException(
                        "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor.request.not.successful");
            }

            MbwayRequest mbwayPaymentRequest = createMbwayPaymentRequest(sibsOnlinePaymentsGateway, debtAccount, debitEntries,
                    installments, phoneNumber, payableAmount, merchantTransactionId, checkoutResultBean.getTransactionId());

            FenixFramework.atomic(() -> {
                log.setPaymentRequest(mbwayPaymentRequest);
                log.setStateCode(mbwayPaymentRequest.getState().name());
                log.setStateDescription(mbwayPaymentRequest.getState().getDescriptionI18N());
            });

            return mbwayPaymentRequest;

        } catch (Exception e) {
            boolean isOnlinePaymentsGatewayException = e instanceof OnlinePaymentsGatewayCommunicationException;

            FenixFramework.atomic(() -> {

                log.logRequestReceiveDateAndData(null, false, false, null, null);
                log.logException(e);

                if (isOnlinePaymentsGatewayException) {
                    OnlinePaymentsGatewayCommunicationException onlineException = (OnlinePaymentsGatewayCommunicationException) e;
                    log.saveRequest(onlineException.getRequestLog());
                    log.saveResponse(onlineException.getResponseLog());
                }
            });

            if (e instanceof TreasuryDomainException) {
                throw (TreasuryDomainException) e;
            } else {
                final String message = "error.SibsOnlinePaymentsGatewayPaymentCodeGenerator.generateNewCodeFor."
                        + (isOnlinePaymentsGatewayException ? "gateway.communication" : "unknown");

                throw new TreasuryDomainException(e, message);
            }
        }
    }

    @Atomic(mode = TxMode.WRITE)
    private static SibsPaymentsGatewayLog createLog(String sibsGatewayMerchantTransactionId) {
        return SibsPaymentsGatewayLog.createForMbwayPaymentRequest(sibsGatewayMerchantTransactionId);
    }

    @Atomic(mode = TxMode.WRITE)
    private static MbwayRequest createMbwayPaymentRequest(SibsPaymentsGateway sibsOnlinePaymentsGateway, DebtAccount debtAccount,
            Set<DebitEntry> debitEntries, Set<Installment> installments, String phoneNumber, BigDecimal payableAmount,
            String sibsGatewayMerchantTransactionId, String sibsGatewayTransactionId) {
        return new MbwayRequest(sibsOnlinePaymentsGateway, debtAccount, debitEntries, installments, payableAmount, phoneNumber,
                sibsGatewayMerchantTransactionId, sibsGatewayTransactionId);
    }

}
