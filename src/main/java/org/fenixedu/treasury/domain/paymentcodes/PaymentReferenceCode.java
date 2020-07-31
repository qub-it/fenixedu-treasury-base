/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
 *
 *
 *
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.onlinepaymentsgateway.api.PaymentStateBean;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.pool.PaymentCodePool;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGateway;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGatewayLog;
import org.fenixedu.treasury.dto.document.managepayments.PaymentReferenceCodeBean;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.LocalDate;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

@Deprecated()
public class PaymentReferenceCode extends PaymentReferenceCode_Base {
    private static final int LENGTH_REFERENCE_CODE = 9;
    private static final BigDecimal SIBS_IGNORE_MAX_AMOUNT = BigDecimal.ZERO;
    public static final String TREASURY_OPERATION_LOG_TYPE = "TREASURY_OPERATION_LOG_TYPE";

    public PaymentReferenceCode() {
        super();
    }

    protected void init(final String referenceCode, final LocalDate beginDate, final LocalDate endDate,
            final PaymentReferenceCodeStateType state, PaymentCodePool pool, BigDecimal minAmount, BigDecimal maxAmount,
            final String sibsMerchantTransactionId, final String sibsReferenceId) {
        setReferenceCode(Strings.padStart(referenceCode, LENGTH_REFERENCE_CODE, '0'));
        setBeginDate(beginDate);
        setEndDate(endDate);
        setState(state);
        setPaymentCodePool(pool);
        setMinAmount(minAmount);
        setMaxAmount(maxAmount);
        setSibsMerchantTransactionId(sibsMerchantTransactionId);
        setSibsReferenceId(sibsReferenceId);

        checkRules();
    }

    public Interval getValidInterval() {
        return new Interval(getBeginDate().toDateTimeAtStartOfDay(),
                getEndDate().plusDays(1).toDateTimeAtStartOfDay().minusSeconds(1));

    }

    private void checkRules() {
        if (this.getMinAmount() == null) {
            this.setMinAmount(BigDecimal.ZERO);
        }
        if (this.getMaxAmount() == null) {
            this.setMaxAmount(BigDecimal.ZERO);
        }

        if (findByReferenceCode(this.getPaymentCodePool().getEntityReferenceCode(), getReferenceCode(),
                this.getPaymentCodePool().getFinantialInstitution()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.referenceCode.duplicated");
        }

        if (getPaymentCodePool().getPaymentCodeGenerator().isSibsMerchantTransactionAndReferenceIdRequired()
                && StringUtils.isEmpty(getSibsMerchantTransactionId())) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.sibsMerchantTransactionId.required");
        }

        if (getPaymentCodePool().getPaymentCodeGenerator().isSibsMerchantTransactionAndReferenceIdRequired()
                && StringUtils.isEmpty(getSibsReferenceId())) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.sibsReferenceId.required");
        }
    }

    public boolean isDeletable() {
        return getReportedInFilesSet().isEmpty();
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.cannot.delete");
        }

        setPaymentCodePool(null);
        setTargetPayment(null);
        deleteDomainObject();
    }

    @Atomic
    public static PaymentReferenceCode create(final String referenceCode, final LocalDate beginDate, final LocalDate endDate,
            final PaymentReferenceCodeStateType state, PaymentCodePool pool, BigDecimal minAmount, BigDecimal maxAmount) {

        PaymentReferenceCode paymentReferenceCode = new PaymentReferenceCode();
        paymentReferenceCode.init(referenceCode, beginDate, endDate, state, pool, minAmount, maxAmount, null, null);
        return paymentReferenceCode;
    }

    public static PaymentReferenceCode createForSibsOnlinePaymentGateway(final String referenceCode, final LocalDate beginDate,
            final LocalDate endDate, final PaymentReferenceCodeStateType state, PaymentCodePool pool, BigDecimal amount,
            final String sibsMerchantTransactionId, final String sibsReferenceId) {
        PaymentReferenceCode paymentReferenceCode = new PaymentReferenceCode();
        paymentReferenceCode.init(referenceCode, beginDate, endDate, state, pool, amount, amount, sibsMerchantTransactionId,
                sibsReferenceId);

        return paymentReferenceCode;
    }

    public static Stream<PaymentReferenceCode> findAll() {
        Set<PaymentReferenceCode> result = new HashSet<PaymentReferenceCode>();

        for (PaymentCodePool pool : PaymentCodePool.findAll().collect(Collectors.toList())) {
            result.addAll(pool.getPaymentReferenceCodesSet());
        }

        return result.stream();
    }

    public static Stream<PaymentReferenceCode> findByReferenceCode(String entityReferenceCode, String referenceCode,
            FinantialInstitution finantialInstitution) {
        return findByReferenceCode(referenceCode, finantialInstitution)
                .filter(x -> x.getPaymentCodePool().getEntityReferenceCode().equals(entityReferenceCode));
    }

    private static Stream<PaymentReferenceCode> find(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getPaymentCodePoolsSet().stream().flatMap(i -> i.getPaymentReferenceCodesSet().stream());
    }

    public static Stream<PaymentReferenceCode> findByReferenceCode(final String referenceCode,
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(i -> referenceCode.equalsIgnoreCase(i.getReferenceCode()));
    }

    public static Stream<PaymentReferenceCode> findByBeginDate(final LocalDate beginDate,
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(i -> beginDate.equals(i.getBeginDate()));
    }

    public static Stream<PaymentReferenceCode> findByEndDate(final LocalDate endDate, FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(i -> endDate.equals(i.getEndDate()));
    }

    public static Stream<PaymentReferenceCode> findByState(
            final org.fenixedu.treasury.domain.paymentcodes.PaymentReferenceCodeStateType state,
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(i -> state.equals(i.getState()));
    }

    public static Stream<PaymentReferenceCode> findBySibsReferenceId(final String sibsReferenceId) {
        return findAll().filter(p -> !StringUtils.isEmpty(p.getSibsReferenceId()))
                .filter(p -> sibsReferenceId.equals(p.getSibsReferenceId()));
    }

    public static Optional<PaymentReferenceCode> findUniqueBySibsReferenceId(final String sibsReferenceId) {
        return findBySibsReferenceId(sibsReferenceId).findFirst();
    }

    public static Stream<PaymentReferenceCode> findBySibsMerchantTransactionId(final String sibsMerchantTransactionId) {
        return findAll().filter(p -> !StringUtils.isEmpty(p.getSibsMerchantTransactionId()))
                .filter(p -> sibsMerchantTransactionId.equals(p.getSibsMerchantTransactionId()));
    }

    public static Optional<PaymentReferenceCode> findUniqueBySibsMerchantTransactionId(final String sibsMerchantTransactionId) {
        return findBySibsMerchantTransactionId(sibsMerchantTransactionId).findFirst();
    }

    public String getFormattedCode() {
        final StringBuilder result = new StringBuilder();
        int i = 1;
        for (char character : getReferenceCode().toCharArray()) {
            result.append(character);
            if (i % 3 == 0) {
                result.append(" ");
            }
            i++;
        }

        return result.charAt(result.length() - 1) == ' ' ? result.deleteCharAt(result.length() - 1).toString() : result
                .toString();
    }

    @Override
    public void setReferenceCode(String code) {
//        if (getReferenceCode() == null) {
        super.setReferenceCode(code);
//        } else if (code != getReferenceCode()) {
//            throw new TreasuryDomainException("error.accounting.PaymentCode.cannot.modify.code");
//        }
    }

    @Atomic
    public void bruteForceSetReferenceCode(String code) {
        super.setReferenceCode(code);
    }

    @Override
    @Atomic
    public void setState(PaymentReferenceCodeStateType state) {
        super.setState(state);
    }

    public boolean isNew() {
        return getState() == PaymentReferenceCodeStateType.UNUSED;
    }

    protected void reuseCode() {
        setState(PaymentReferenceCodeStateType.UNUSED);
    }

    public boolean isUsed() {
        return getState() == PaymentReferenceCodeStateType.USED;
    }

    public boolean isAnnulled() {
        return getState() == PaymentReferenceCodeStateType.ANNULLED;
    }

    public boolean isProcessed() {
        return getState() == PaymentReferenceCodeStateType.PROCESSED;
    }

    public void anull() {
        setState(PaymentReferenceCodeStateType.ANNULLED);
    }

    public boolean isFixedAmount() {
        return this.getPaymentCodePool().getIsFixedAmount();
    }

    public boolean isAvailableForReuse() {
        return !isNew();
    }

    public void update(final LocalDate startDate, final LocalDate endDate, final BigDecimal minAmount,
            final BigDecimal maxAmount) {
        super.setBeginDate(startDate);
        super.setEndDate(endDate);
        super.setMinAmount(minAmount);
        super.setMaxAmount(maxAmount != null ? maxAmount : SIBS_IGNORE_MAX_AMOUNT);
        checkRules();
    }

    @Atomic
    public Set<SettlementNote> processPayment(final String responsibleUsername, BigDecimal amountToPay, DateTime paymentDate,
            String sibsTransactionId, String comments, final DateTime whenProcessedBySibs, final SibsReportFile sibsReportFile,
            final boolean checkSibsTransactionIdDuplication) {

        if (!isNew() && SibsTransactionDetail.isReferenceProcessingDuplicate(this.getReferenceCode(),
                this.getPaymentCodePool().getEntityReferenceCode(), paymentDate)) {
            return null;
        }

        if (checkSibsTransactionIdDuplication) {
            if (SibsTransactionDetail.isSibsOppwaReferenceProcessingDuplicate(sibsTransactionId)) {
                throw new RuntimeException("Duplicate transaction id: " + sibsTransactionId);
            }
        }

        final Set<SettlementNote> noteSet = this.getTargetPayment().processPayment(responsibleUsername, amountToPay, paymentDate,
                sibsTransactionId, comments);

        final DebtAccount referenceDebtAccount = this.getTargetPayment().getDebtAccount();

        final String debtAccountId = referenceDebtAccount.getExternalId();
        final String customerId = referenceDebtAccount.getCustomer().getExternalId();
        final String businessIdentification = referenceDebtAccount.getCustomer().getBusinessIdentification();
        final String fiscalNumber = valueOrEmpty(referenceDebtAccount.getCustomer().getFiscalCountry()) + ":"
                + valueOrEmpty(referenceDebtAccount.getCustomer().getFiscalNumber());
        final String customerName = referenceDebtAccount.getCustomer().getName();

        for (SettlementNote settlementNote : noteSet) {
            final String settlementDocumentNumber = settlementNote.getUiDocumentNumber();

            SibsTransactionDetail transactionDetail =
                    SibsTransactionDetail.create(sibsReportFile, comments, whenProcessedBySibs, paymentDate, amountToPay,
                            getPaymentCodePool().getEntityReferenceCode(), getReferenceCode(), sibsTransactionId, debtAccountId,
                            customerId, businessIdentification, fiscalNumber, customerName, settlementDocumentNumber);

        }

        return noteSet;

    }

    @Atomic(mode = TxMode.READ)
    public void processPaymentReferenceCodeTransaction(final SibsOnlinePaymentsGatewayLog log, PaymentStateBean bean) {
        if (!bean.getMerchantTransactionId().equals(getSibsMerchantTransactionId())) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.merchantTransactionId.not.equal");
        }

        FenixFramework.atomic(() -> {
            final SibsOnlinePaymentsGateway sibsOnlinePaymentsGateway = getPaymentCodePool().getSibsOnlinePaymentsGateway();
            final DebtAccount debtAccount = getTargetPayment().getDebtAccount();

            log.associateSibsOnlinePaymentGatewayAndDebtAccount(sibsOnlinePaymentsGateway, debtAccount);
            log.setPaymentCode(getReferenceCode());
        });

        final BigDecimal amount = bean.getAmount();
        final DateTime paymentDate = bean.getPaymentDate();

        FenixFramework.atomic(() -> {
            log.savePaymentInfo(amount, paymentDate);
        });

        if (amount == null || !TreasuryConstants.isPositive(amount)) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.amount");
        }

        if (paymentDate == null) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.processPaymentReferenceCodeTransaction.invalid.payment.date");
        }

        if (SibsTransactionDetail.isReferenceProcessingDuplicate(getReferenceCode(),
                getPaymentCodePool().getEntityReferenceCode(), paymentDate)) {
            FenixFramework.atomic(() -> {
                log.markAsDuplicatedTransaction();
            });

        } else if (SibsTransactionDetail.isSibsOppwaReferenceProcessingDuplicate(bean.getTransactionId())) {
            FenixFramework.atomic(() -> {
                log.markAsDuplicatedTransaction();
            });
        } else {
            final String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();

            final Set<SettlementNote> settlementNotes =
                    processPayment(StringUtils.isNotEmpty(loggedUsername) ? loggedUsername : "unknown", amount, paymentDate,
                            bean.getTransactionId(), bean.getMerchantTransactionId(), new DateTime(), null, true);

            FenixFramework.atomic(() -> {
                log.markSettlementNotesCreated(settlementNotes);
            });
        }
    }

    public String getDescription() {
        return this.getPaymentCodePool().getEntityReferenceCode() + " " + this.getReferenceCode();
    }

    static public PaymentReferenceCode readByCode(final String code, FinantialInstitution finantialInstitution) {
        if (StringUtils.isEmpty(code)) {
            return null;
        }
        PaymentReferenceCode paymentReferenceCode = null;
        for (PaymentCodePool pool : finantialInstitution.getPaymentCodePoolsSet()) {
            paymentReferenceCode = pool.getPaymentReferenceCodesSet().stream().filter(y -> y.getReferenceCode().equals(code))
                    .findFirst().orElse(null);
            if (paymentReferenceCode != null) {
                break;
            }
        }
        return paymentReferenceCode;
    }

    public String getReferenceCodeWithoutCheckDigits() {
        if (Boolean.TRUE.equals(this.getPaymentCodePool().getUseCheckDigit())) {
            if (this.getReferenceCode().length() >= 2) {
                return this.getReferenceCode().substring(0, this.getReferenceCode().length() - 2);
            } else {
                return this.getReferenceCode();
            }
        }
        throw new TreasuryDomainException("error.PaymentReferenceCode.not.from.pool.with.checkdigit");
    }

    public SibsReportFile getReportOnDate(DateTime transactionWhenRegistered) {
        return this.getReportedInFilesSet().stream().filter(x -> x.getWhenProcessedBySibs().equals(transactionWhenRegistered))
                .findFirst().orElse(null);
    }

    @Atomic
    public void createPaymentTargetTo(FinantialDocument finantialDocument) {
        if (this.getTargetPayment() != null && Boolean.TRUE.equals(this.getTargetPayment().getValid())) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.payment.target.already.exists");
        }
        FinantialDocumentPaymentCode targetToFinantialDocument =
                FinantialDocumentPaymentCode.create(finantialDocument, this, true);
        this.setTargetPayment(targetToFinantialDocument);
        this.setState(PaymentReferenceCodeStateType.USED);
        this.setPayableAmount(finantialDocument.getOpenAmount());
        checkRules();
    }

    @Atomic
    public void createPaymentTargetTo(final Set<DebitEntry> debitNoteEntries, final Set<Installment> installments,
            final BigDecimal payableAmount) {
        if (this.getTargetPayment() != null && Boolean.TRUE.equals(this.getTargetPayment().getValid())) {
            throw new TreasuryDomainException("error.PaymentReferenceCode.payment.target.already.exists");
        }

        MultipleEntriesPaymentCode target = MultipleEntriesPaymentCode.create(debitNoteEntries, installments, this, true);
        this.setTargetPayment(target);
        this.setState(PaymentReferenceCodeStateType.USED);
        this.setPayableAmount(payableAmount);
        checkRules();
    }

    @Atomic
    public void anullPaymentReferenceCode() {
        if (PaymentReferenceCodeStateType.PROCESSED == this.getState()) {
            throw new TreasuryDomainException(
                    "error.PaymentReferenceCode.anullPaymentReferenceCode.cannot.anull.processed.references");
        }

        if (!this.getState().equals(PaymentReferenceCodeStateType.ANNULLED)) {
            this.setState(PaymentReferenceCodeStateType.ANNULLED);
        }

        checkRules();
    }

    private String valueOrEmpty(final String value) {
        if (Strings.isNullOrEmpty(value)) {
            return "";
        }

        return value;
    }

    @Atomic(mode = TxMode.READ)
    @Deprecated
    public static PaymentReferenceCode createPaymentReferenceCodeForMultipleDebitEntries(final DebtAccount debtAccount,
            final PaymentReferenceCodeBean bean) {
        throw new RuntimeException("deprecated");
    }

}
