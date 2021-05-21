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
package org.fenixedu.treasury.domain.forwardpayments;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.exceptions.ForwardPaymentAlreadyPayedException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InstallmentPaymenPlanBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class ForwardPaymentRequest extends ForwardPaymentRequest_Base {

    private static final Comparator<? super ForwardPaymentRequest> ORDER_COMPARATOR = (o1,
            o2) -> Long.compare(o1.getOrderNumber(), o2.getOrderNumber()) * 10 + o1.getExternalId().compareTo(o2.getExternalId());

    public ForwardPaymentRequest() {
        super();
    }

    protected ForwardPaymentRequest(DigitalPaymentPlatform platform, DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, BigDecimal payableAmount) {
        this();
        this.init(platform, debtAccount, debitEntries, installments, payableAmount,
                TreasurySettings.getInstance().getCreditCardPaymentMethod());

        setState(ForwardPaymentStateType.CREATED);
        setOrderNumber(lastForwardPayment().isPresent() ? lastForwardPayment().get().getOrderNumber() + 1 : 1);

        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();

        if (getOrderNumber() <= 0) {
            throw new TreasuryDomainException("error.ForwardPaymentRequest.orderNumber.invalid");
        }
    }

    private static Optional<ForwardPaymentRequest> lastForwardPayment() {
        return findAll().max(ORDER_COMPARATOR);
    }

    public boolean isActive() {
        return getState() != ForwardPaymentStateType.REJECTED;
    }

    @Override
    public ForwardPaymentStateType getCurrentState() {
        return getState();
    }

    @Override
    public boolean isInCreatedState() {
        return getState() == ForwardPaymentStateType.CREATED;
    }

    @Override
    public boolean isInRequestedState() {
        return getState() == ForwardPaymentStateType.REQUESTED;
    }

    public boolean isInAuthorizedState() {
        return getState() == ForwardPaymentStateType.AUTHORIZED;
    }

    @Override
    public boolean isInPaidState() {
        return getState() == ForwardPaymentStateType.PAYED;
    }

    @Override
    public boolean isInAnnuledState() {
        return isInRejectedState();
    }

    public boolean isInRejectedState() {
        return getState() == ForwardPaymentStateType.REJECTED;
    }

    public boolean isInStateToPostProcessPayment() {
        return !isInAnnuledState() && !isInRejectedState() && !isInPaidState();
    }

    @Override
    public String fillPaymentEntryMethodId() {
        return null;
    }

    @Override
    protected boolean payAllDebitEntriesInterests() {
        return true;
    }

    @Deprecated
    public PaymentRequestLog reject(String operationCode, String statusCode, String errorMessage, String requestBody,
            String responseBody) {
        setState(ForwardPaymentStateType.REJECTED);

        PaymentRequestLog log = getDigitalPaymentPlatform().log(this, statusCode, errorMessage, requestBody, responseBody);
        log.setOperationCode(operationCode);
        log.setOperationSuccess(false);
        log.setTransactionWithPayment(false);

        checkRules();

        return log;
    }

    public void reject() {
        setState(ForwardPaymentStateType.REJECTED);

        checkRules();
    }

    public PaymentRequestLog advanceToRequestState(String operationCode, String statusCode, String statusMessage,
            String requestBody, String responseBody) {
        setState(ForwardPaymentStateType.REQUESTED);
        PaymentRequestLog log = getDigitalPaymentPlatform().log(this, statusCode, statusMessage, requestBody, responseBody);
        log.setOperationCode(operationCode);

        checkRules();

        return log;
    }

    public PaymentRequestLog advanceToAuthenticatedState(String statusCode, String statusMessage, String requestBody,
            String responseBody) {
        setState(ForwardPaymentStateType.AUTHENTICATED);
        PaymentRequestLog log = getDigitalPaymentPlatform().log(this, statusCode, statusMessage, requestBody, responseBody);
        log.setOperationCode("advanceToAuthenticatedState");

        checkRules();

        return log;
    }

    public PaymentRequestLog advanceToAuthorizedState(String statusCode, String errorMessage, String requestBody,
            String responseBody) {
        if (!isActive()) {
            throw new TreasuryDomainException("error.ForwardPayment.not.in.active.state");
        }

        if (isInAuthorizedState()) {
            throw new TreasuryDomainException("error.ForwardPayment.already.authorized");
        }

        if (isInPaidState()) {
            throw new ForwardPaymentAlreadyPayedException("error.ForwardPayment.already.payed");
        }

        setState(ForwardPaymentStateType.AUTHORIZED);
        PaymentRequestLog log = getDigitalPaymentPlatform().log(this, statusCode, errorMessage, requestBody, responseBody);
        log.setOperationCode("advanceToAuthorizedState");

        checkRules();

        return log;
    }

    @Deprecated
    public PaymentRequestLog advanceToPaidState(String statusCode, String statusMessage, BigDecimal paidAmount,
            DateTime transactionDate, String transactionId, String authorizationNumber, String requestBody, String responseBody,
            String justification) {

        if (!isActive()) {
            throw new TreasuryDomainException("error.ForwardPayment.not.in.active.state");
        }

        if (isInPaidState()) {
            throw new ForwardPaymentAlreadyPayedException("error.ForwardPayment.already.payed");
        }

        if (!getPaymentTransactionsSet().isEmpty()) {
            throw new TreasuryDomainException("error.ForwardPayment.with.settlement.note.already.associated");
        }

        setState(ForwardPaymentStateType.PAYED);

        PaymentRequestLog log = getDigitalPaymentPlatform().log(this, statusCode, statusMessage, requestBody, responseBody);

        log.setOperationCode("advanceToPaidState");
        log.setOperationSuccess(true);
        log.setTransactionWithPayment(true);

        Function<PaymentRequest, Map<String, String>> additionalPropertiesMapFunction =
                (o) -> fillPaymentEntryPropertiesMap(transactionId, transactionDate, statusCode);

        Set<SettlementNote> resultSettlementNotes = null;
        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            resultSettlementNotes = internalProcessPaymentInNormalPaymentMixingLegacyInvoices(paidAmount, transactionDate,
                    String.valueOf(getOrderNumber()), transactionId, additionalPropertiesMapFunction);
        } else {
            resultSettlementNotes = internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(paidAmount, transactionDate,
                    String.valueOf(getOrderNumber()), transactionId, additionalPropertiesMapFunction);
        }

        PaymentTransaction paymentTransaction =
                PaymentTransaction.create(this, transactionId, transactionDate, paidAmount, resultSettlementNotes);
        paymentTransaction.setJustification(justification);

        log.setPaymentTransaction(paymentTransaction);

        checkRules();

        return log;
    }

    public PaymentTransaction advanceToPaidState(String statusCode, BigDecimal paidAmount, DateTime transactionDate,
            String transactionId, String justification) {

        if (!isActive()) {
            throw new TreasuryDomainException("error.ForwardPayment.not.in.active.state");
        }

        if (isInPaidState()) {
            throw new ForwardPaymentAlreadyPayedException("error.ForwardPayment.already.payed");
        }

        if (!getPaymentTransactionsSet().isEmpty()) {
            throw new TreasuryDomainException("error.ForwardPayment.with.settlement.note.already.associated");
        }

        setState(ForwardPaymentStateType.PAYED);

        Function<PaymentRequest, Map<String, String>> additionalPropertiesMapFunction =
                (o) -> fillPaymentEntryPropertiesMap(transactionId, transactionDate, statusCode);

        Set<SettlementNote> resultSettlementNotes = null;
        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            resultSettlementNotes = internalProcessPaymentInNormalPaymentMixingLegacyInvoices(paidAmount, transactionDate,
                    String.valueOf(getOrderNumber()), transactionId, additionalPropertiesMapFunction);
        } else {
            resultSettlementNotes = internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(paidAmount, transactionDate,
                    String.valueOf(getOrderNumber()), transactionId, additionalPropertiesMapFunction);
        }

        PaymentTransaction paymentTransaction =
                PaymentTransaction.create(this, transactionId, transactionDate, paidAmount, resultSettlementNotes);
        paymentTransaction.setJustification(justification);

        checkRules();

        return paymentTransaction;
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(String transactionId, DateTime transactionDate, String statusCode) {
        final Map<String, String> paymentEntryPropertiesMap = Maps.newHashMap();

        paymentEntryPropertiesMap.put("OrderNumber", String.valueOf(getOrderNumber()));

        if (!Strings.isNullOrEmpty(transactionId)) {
            paymentEntryPropertiesMap.put("TransactionId", transactionId);
        }

        if (transactionDate != null) {
            paymentEntryPropertiesMap.put("TransactionDate",
                    transactionDate.toString(TreasuryConstants.DATE_TIME_FORMAT_YYYY_MM_DD));
        }

        if (!Strings.isNullOrEmpty(statusCode)) {
            paymentEntryPropertiesMap.put("StatusCode", statusCode);
        }

        return paymentEntryPropertiesMap;
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     *
     */
    // @formatter:on

    public static Stream<ForwardPaymentRequest> findAll() {
        return PaymentRequest.findAll().filter(p -> p instanceof ForwardPaymentRequest).map(ForwardPaymentRequest.class::cast);
    }

    public static Stream<ForwardPaymentRequest> find(DebitEntry debitEntry) {
        return debitEntry.getPaymentRequestsSet().stream().filter(p -> p instanceof ForwardPaymentRequest)
                .map(ForwardPaymentRequest.class::cast);
    }

    public static Stream<ForwardPaymentRequest> findAllByStateType(final ForwardPaymentStateType... stateTypes) {
        List<ForwardPaymentStateType> t = Lists.newArrayList(stateTypes);
        return findAll().filter(f -> t.contains(f.getState()));
    }

    public static ForwardPaymentRequest create(SettlementNoteBean bean,
            Function<ForwardPaymentRequest, String> successUrlFunction,
            Function<ForwardPaymentRequest, String> insuccessUrlFunction) {
        Set<DebitEntry> debitEntries =
                bean.getIncludedInvoiceEntryBeans().stream().map(ISettlementInvoiceEntryBean::getInvoiceEntry)
                        .filter(i -> i != null).map(DebitEntry.class::cast).collect(Collectors.toSet());

        Set<Installment> installments =
                bean.getIncludedInvoiceEntryBeans().stream().filter(i -> i instanceof InstallmentPaymenPlanBean && i.isIncluded())
                        .map(InstallmentPaymenPlanBean.class::cast).map(ib -> ib.getInstallment()).collect(Collectors.toSet());

        ForwardPaymentRequest request = new ForwardPaymentRequest(bean.getDigitalPaymentPlatform(), bean.getDebtAccount(),
                debitEntries, installments, bean.getTotalAmountToPay());

        request.setForwardPaymentSuccessUrl(successUrlFunction.apply(request));
        request.setForwardPaymentInsuccessUrl(insuccessUrlFunction.apply(request));

        return request;
    }

    public static ForwardPaymentRequest create(DigitalPaymentPlatform plataform, DebtAccount debtAccount,
            Set<DebitEntry> debitEntries, Set<Installment> installments, BigDecimal amount,
            Function<ForwardPaymentRequest, String> successUrlFunction,
            Function<ForwardPaymentRequest, String> insuccessUrlFunction) {

        ForwardPaymentRequest request = new ForwardPaymentRequest(plataform, debtAccount, debitEntries, installments, amount);

        request.setForwardPaymentSuccessUrl(successUrlFunction.apply(request));
        request.setForwardPaymentInsuccessUrl(insuccessUrlFunction.apply(request));

        return request;
    }

    public static Optional<ForwardPaymentRequest> findUniqueByMerchantTransactionId(String merchantTransactionId) {
        return findAll().filter(pr -> pr.getMerchantTransactionId().equals(merchantTransactionId)).findFirst();
    }
}
