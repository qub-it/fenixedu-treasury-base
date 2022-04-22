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
package org.fenixedu.treasury.domain.sibsonlinepaymentsgateway;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.PaymentStateBean;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class MbwayPaymentRequest extends MbwayPaymentRequest_Base {

    public MbwayPaymentRequest() {
        super();

        setCreationDate(new DateTime());
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected MbwayPaymentRequest(SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway, DebtAccount debtAccount,
            Set<InvoiceEntry> invoiceEntries, Set<Installment> installments, BigDecimal payableAmount, String phoneNumber,
            String sibsMerchantTransactionId, String sibsReferenceId) {
        this();

        setSibsOnlinePaymentsGateway(sibsOnlinePaymentsGateway);
        setDebtAccount(debtAccount);
        getInvoiceEntriesSet().addAll(invoiceEntries);
        getInstallmentsSet().addAll(installments);
        setPayableAmount(payableAmount);
        setPhoneNumber(phoneNumber);
        setSibsMerchantTransactionId(sibsMerchantTransactionId);
        setSibsReferenceId(sibsReferenceId);
        setState(PaymentReferenceCodeStateType.USED);

        checkRules();
    }

    private void checkRules() {

        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.domainRoot.required");
        }

        if (getCreationDate() == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.creationDate.required");
        }

        if (getSibsOnlinePaymentsGateway() == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsOnlinePaymentsGateway.required");
        }

        if (getDebtAccount() == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.debtAccount.required");
        }

        if (getInvoiceEntriesSet().isEmpty() && getInstallmentsSet().isEmpty()) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.invoiceEntriesSet.required");
        }

        if (getPayableAmount() == null || !TreasuryConstants.isPositive(getPayableAmount())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.payableAmount.required");
        }

        if (StringUtils.isEmpty(getPhoneNumber())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.phoneNumber.required");
        }

        if (StringUtils.isEmpty(getSibsMerchantTransactionId())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsMerchantTransaction.required");
        }

        if (findBySibsMerchantTransactionId(getSibsMerchantTransactionId()).count() > 1) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsMerchantTransactionId.not.unique");
        }

        checkParametersAreValid(getDebtAccount(), getInvoiceEntriesSet());
    }

    public Set<SettlementNote> internalProcessPaymentInNormalPaymentMixingLegacyInvoices(final String username,
            final BigDecimal amount, final DateTime paymentDate, final String sibsTransactionId, final String comments,
            Set<InvoiceEntry> invoiceEntriesToPay, Set<Installment> installmentsToPay,
            Function<Object, Map<String, String>> fillPaymentEntryPropertiesMapFunction) {
        throw new RuntimeException("not supported");
    }

    public Set<SettlementNote> internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(final String username,
            final BigDecimal amount, final DateTime paymentDate, final String sibsTransactionId, final String comments,
            final Set<InvoiceEntry> invoiceEntriesToPay, Set<Installment> installmentsToPay,
            Function<Object, Map<String, String>> fillPaymentEntryPropertiesMapFunction) {
        throw new RuntimeException("not supported");

    }

    @Atomic
    private Set<SettlementNote> processPayment(final String username, final BigDecimal amount, final DateTime paymentDate,
            final String sibsTransactionId, final String comments) {
        throw new RuntimeException("not supported");
    }

    @Atomic(mode = TxMode.READ)
    public void processMbwayTransaction(final SibsOnlinePaymentsGatewayLog log, PaymentStateBean bean) {
        if (!bean.getMerchantTransactionId().equals(getSibsMerchantTransactionId())) {
            throw new TreasuryDomainException(
                    "error.MbwayPaymentRequest.processMbwayTransaction.merchantTransactionId.not.equal");
        }

        FenixFramework.atomic(() -> {
            final SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway = getSibsOnlinePaymentsGateway();
            final DebtAccount debtAccount = getDebtAccount();

            log.associateSibsOnlinePaymentGatewayAndDebtAccount(sibsOnlinePaymentsGateway, debtAccount);
        });

        final BigDecimal amount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate();

        FenixFramework.atomic(() -> {
            log.savePaymentInfo(amount, paymentDate);
        });

        if (amount == null || !TreasuryConstants.isPositive(amount)) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.processMbwayTransaction.invalid.payment.date");
        }

        if (MbwayTransaction.isTransactionProcessingDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> {
                log.markAsDuplicatedTransaction();
            });
        } else {

            FenixFramework.atomic(() -> {
                final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

                final Set<SettlementNote> settlementNotes =
                        processPayment(StringUtils.isNotEmpty(loggedUsername) ? loggedUsername : "unknown", amount, paymentDate,
                                bean.getTransactionId(), bean.getMerchantTransactionId());
                MbwayTransaction.create(this, bean.getTransactionId(), amount, paymentDate, settlementNotes);

                log.markSettlementNotesCreated(settlementNotes);
            });
        }
    }

    public DocumentNumberSeries getDocumentSeriesForPayments() {
        return getSibsOnlinePaymentsGateway().getMbwayDocumentSeries();
    }

    public DocumentNumberSeries getDocumentSeriesInterestDebits() {
        return DocumentNumberSeries.find(FinantialDocumentType.findForDebitNote(), getDocumentSeriesForPayments().getSeries());
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(String sibsTransactionId) {
        final Map<String, String> result = new HashMap<>();

        result.put("SibsTransactionId", sibsTransactionId);

        return result;
    }

    public Set<Customer> getReferencedCustomers() {
        final Set<InvoiceEntry> invoiceEntrySet = getInvoiceEntriesSet();
        final Set<Installment> installments = getInstallmentsSet();
        
        final Set<Customer> result = Sets.newHashSet();
        for (final InvoiceEntry entry : invoiceEntrySet) {
            if (entry.getFinantialDocument() != null && ((Invoice) entry.getFinantialDocument()).isForPayorDebtAccount()) {
                result.add(((Invoice) entry.getFinantialDocument()).getPayorDebtAccount().getCustomer());
                continue;
            }

            result.add(entry.getDebtAccount().getCustomer());
        }

        for (final Installment entry : installments) {
            result.addAll(entry.getInstallmentEntriesSet().stream().map(e -> e.getDebitEntry())
                    .map(deb -> (deb.getFinantialDocument() != null && ((Invoice) deb.getFinantialDocument())
                            .isForPayorDebtAccount()) ? ((Invoice) deb.getFinantialDocument()).getPayorDebtAccount()
                                    .getCustomer() : deb.getDebtAccount().getCustomer())
                    .collect(Collectors.toSet()));
        }

        return result;
    }

    public DateTime getPaymentRequestDate() {
        return getCreationDate();
    }

    public PaymentMethod getPaymentMethod() {
        return getSibsOnlinePaymentsGateway().getMbwayPaymentMethod();
    }

    public String getPaymentRequestStateDescription() {
        return getState().getDescriptionI18N().getContent();
    }

    public String getPaymentTypeDescription() {
        return PAYMENT_TYPE_DESCRIPTION();
    }

    public String fillPaymentEntryMethodId() {
        return "";
    }

    public boolean isMbwayRequest() {
        return true;
    }

    public static String PAYMENT_TYPE_DESCRIPTION() {
        return treasuryBundle("label.IPaymentProcessorForInvoiceEntries.paymentProcessorDescription.mbwayPaymentRequest");
    }

    /* ************ */
    /* * SERVICES * */
    /* ************ */

    @Atomic(mode = TxMode.READ)
    public static MbwayPaymentRequest create(SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway, DebtAccount debtAccount,
            Set<InvoiceEntry> invoiceEntries, Set<Installment> installments, String countryPrefix, String localPhoneNumber) {
        throw new RuntimeException("not supported");
    }

    private static void checkParametersAreValid(final DebtAccount debtAccount, final Set<InvoiceEntry> invoiceEntries) {
        for (final InvoiceEntry invoiceEntry : invoiceEntries) {
            final DebitEntry debitEntry = (DebitEntry) invoiceEntry;

            // Ensure all debit entries are the same debt account
            if (debitEntry.getDebtAccount() != debtAccount) {
                throw new TreasuryDomainException("error.MbwayPaymentRequest.debit.entry.not.same.debt.account");
            }

            // Ensure debit entries have payable amount
            if (!TreasuryConstants.isGreaterThan(debitEntry.getOpenAmount(), BigDecimal.ZERO)) {
                throw new TreasuryDomainException("error.MbwayPaymentRequest.debit.entry.open.amount.must.be.greater.than.zero");
            }
        }

//        if (getReferencedCustomers(invoiceEntries, Collections.emptySet()).size() > 1) {
//            throw new TreasuryDomainException("error.MbwayPaymentRequest.referencedCustomers.only.one.allowed");
//        }

        SettlementNote.checkMixingOfInvoiceEntriesExportedInLegacyERP(invoiceEntries);
    }

    @Atomic(mode = TxMode.WRITE)
    private static MbwayPaymentRequest createMbwayPaymentRequest(SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway,
            DebtAccount debtAccount, Set<InvoiceEntry> invoiceEntries, Set<Installment> installments, String phoneNumber,
            BigDecimal payableAmount, String merchantTransactionId, String sibsReferenceId) {
        return new MbwayPaymentRequest(sibsOnlinePaymentsGateway, debtAccount, invoiceEntries, installments, payableAmount,
                phoneNumber, merchantTransactionId, sibsReferenceId);
    }

    @Atomic(mode = TxMode.WRITE)
    private static SibsOnlinePaymentsGatewayLog createLog(final SibsOnlinePaymentsGateway sibsGateway,
            final DebtAccount debtAccount) {
        return SibsOnlinePaymentsGatewayLog.createLogForRequestPaymentCode(sibsGateway, debtAccount);
    }

    // ############
    // # SERVICES #
    // ############

    public static Stream<MbwayPaymentRequest> findAll() {
        return FenixFramework.getDomainRoot().getMbwayPaymentRequestsSet().stream();
    }

    public static Stream<MbwayPaymentRequest> findBySibsMerchantTransactionId(final String sibsMerchantTransactionId) {
        return findAll().filter(r -> r.getSibsMerchantTransactionId().equals(sibsMerchantTransactionId));
    }

    public static Stream<MbwayPaymentRequest> findBySibsReferenceId(final String sibsReferenceId) {
        return findAll().filter(r -> r.getSibsReferenceId().equals(sibsReferenceId));
    }

    public static Optional<MbwayPaymentRequest> findUniqueBySibsReferenceId(final String sibsReferenceId) {
        return findBySibsReferenceId(sibsReferenceId).findFirst();
    }

    public static Optional<MbwayPaymentRequest> findUniqueBySibsMerchantTransactionId(final String sibsMerchantTransactionId) {
        return findBySibsMerchantTransactionId(sibsMerchantTransactionId).findFirst();
    }

    public String getSibsOppwaMerchantTransactionId() {
        return getSibsMerchantTransactionId();
    }

    public String getSibsOppwaTransactionId() {
        return getSibsReferenceId();
    }

}
