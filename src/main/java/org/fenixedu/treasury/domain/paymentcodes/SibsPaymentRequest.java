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
package org.fenixedu.treasury.domain.paymentcodes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentTransaction;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatform;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.joda.time.DateTime;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import pt.ist.fenixframework.Atomic;

public class SibsPaymentRequest extends SibsPaymentRequest_Base {

    public SibsPaymentRequest() {
        super();
    }

    protected SibsPaymentRequest(SibsReferenceCode sibsReferenceCode, DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, BigDecimal payableAmount) {
        this();
        
        // Double check if sibsReferenceCode is not associated to other sibsPaymentRequest
        if(sibsReferenceCode.getSibsPaymentRequest() != null) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.sibsReferenceCode.not.free");
        }
        
        this.init(sibsReferenceCode.getDigitalPaymentPlatform(), debtAccount, debitEntries, installments, payableAmount,
                TreasurySettings.getInstance().getMbPaymentMethod());

        setEntityReferenceCode(sibsReferenceCode.getEntityReferenceCode());
        setReferenceCode(sibsReferenceCode.getReferenceCode());
        setSibsReferenceCode(sibsReferenceCode);

        setState(PaymentReferenceCodeStateType.USED);

        checkRules();
    }

    public SibsPaymentRequest(DigitalPaymentPlatform platform, DebtAccount debtAccount, Set<DebitEntry> debitEntries,
            Set<Installment> installments, String entityCode, String referenceCode, BigDecimal payableAmount,
            String merchantTransactionId, String sibsReferenceId) {
        this();
        this.init(platform, debtAccount, debitEntries, installments, payableAmount,
                TreasurySettings.getInstance().getMbPaymentMethod());

        setEntityReferenceCode(entityCode);
        setReferenceCode(referenceCode);
        setMerchantTransactionId(merchantTransactionId);
        setTransactionId(sibsReferenceId);

        setState(PaymentReferenceCodeStateType.USED);

        if (StringUtils.isEmpty(getMerchantTransactionId())) {
            throw new TreasuryDomainException("error.MbwayPaymentRequest.sibsMerchantTransaction.required");
        }


        checkRules();
    }

    @Override
    public void checkRules() {
        super.checkRules();

        if (!getDigitalPaymentPlatform().isSibsPaymentCodeServiceSupported()) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.digitalPaymentPlatform.not.supports.sibs.service");
        }
        
        if(getDigitalPaymentPlatform() instanceof SibsPaymentCodePool && getSibsReferenceCode() == null) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.sibsReferenceCode.required");
        }
        
        if(StringUtils.isEmpty(getEntityReferenceCode())) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.entityReferenceCode.required");
        }
        
        if(StringUtils.isEmpty(getReferenceCode())) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.referenceCode.required");
        }
        
        // For now ensure only one sibsPaymentRequest for the same entityCode and referenceCode exists
        // Later we may remove this restriction
        if(find(getEntityReferenceCode(), getReferenceCode()).count() > 1) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.request.already.exists.for.entityCode.and.referenceCode");
        }
    }

    @Override
    public void setSibsReferenceCode(SibsReferenceCode sibsReferenceCode) {
        // Double check if sibsReferenceCode is not associated to other sibsPaymentRequest
        if(sibsReferenceCode.getSibsPaymentRequest() != null && sibsReferenceCode.getSibsPaymentRequest() != this) {
            throw new TreasuryDomainException("error.SibsPaymentRequest.sibsReferenceCode.associated.to.other.sibsPaymentRequest");
        }
        
        super.setSibsReferenceCode(sibsReferenceCode);
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

    public void anull() {
        setState(PaymentReferenceCodeStateType.ANNULLED);
    }

    public String getDescription() {
        List<String> descriptions =
                getOrderedDebitEntries().stream().map(DebitEntry::getDescription).collect(Collectors.toList());
        descriptions
                .addAll(getOrderedInstallments().stream().map(i -> i.getDescription().getContent()).collect(Collectors.toList()));
        return String.join("\n", descriptions);
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

    @Atomic
    public PaymentTransaction processPayment(BigDecimal paidAmount, DateTime paymentDate, String sibsTransactionId,
            String sibsImportationFilename, String sibsMerchantTransactionId, DateTime whenProcessedBySibs,
            SibsReportFile sibsReportFile, boolean checkSibsTransactionIdDuplication) {

        if (SibsPaymentCodeTransaction.isReferenceProcessingDuplicate(getEntityReferenceCode(), getReferenceCode(),
                paymentDate)) {
            return null;
        }

        if (checkSibsTransactionIdDuplication
                && SibsPaymentCodeTransaction.isSibsGatewayReferenceProcessingDuplicate(sibsTransactionId)) {
            throw new RuntimeException("Duplicate transaction id: " + sibsTransactionId);
        }

        if (getState() == PaymentReferenceCodeStateType.UNUSED || getState() == PaymentReferenceCodeStateType.USED) {
            setState(PaymentReferenceCodeStateType.PROCESSED);
        }

        Set<SettlementNote> noteSet = new HashSet<>();

        SibsPaymentCodeTransaction transaction = SibsPaymentCodeTransaction.create(sibsReportFile, this, paymentDate, paidAmount,
                sibsTransactionId, whenProcessedBySibs, sibsImportationFilename, noteSet);

        Function<PaymentRequest, Map<String, String>> additionalPropertiesMapFunction =
                (o) -> fillPaymentEntryPropertiesMap(sibsTransactionId);

        if (!TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices()) {
            noteSet.addAll(internalProcessPaymentInNormalPaymentMixingLegacyInvoices(paidAmount, paymentDate, sibsTransactionId,
                    sibsImportationFilename, additionalPropertiesMapFunction));
        } else {
            noteSet.addAll(internalProcessPaymentInRestrictedPaymentMixingLegacyInvoices(paidAmount, paymentDate,
                    sibsTransactionId, sibsImportationFilename, additionalPropertiesMapFunction));
        }

        for (SettlementNote settlementNote : noteSet) {
            String observations = "";
            if (!StringUtils.isEmpty(sibsMerchantTransactionId)) {
                observations = sibsMerchantTransactionId;
            } else if (!StringUtils.isEmpty(sibsImportationFilename)) {
                observations = String.format("%s [%s]", sibsImportationFilename, getReferenceCode());
            }

            settlementNote.setDocumentObservations(observations);
            if (settlementNote.getAdvancedPaymentCreditNote() != null) {
                settlementNote.getAdvancedPaymentCreditNote().setDocumentObservations(observations);
            }
        }

        transaction.getSettlementNotesSet().addAll(noteSet);

        return transaction;
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(final String sibsTransactionId) {
        final Map<String, String> paymentEntryPropertiesMap = new HashMap<>();

        paymentEntryPropertiesMap.put("ReferenceCode", getReferenceCode());
        paymentEntryPropertiesMap.put("EntityReferenceCode", getEntityReferenceCode());

        if (!Strings.isNullOrEmpty(sibsTransactionId)) {
            paymentEntryPropertiesMap.put("SibsTransactionId", sibsTransactionId);
        }

        return paymentEntryPropertiesMap;
    }

    // @formatter:off
    /*
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static Stream<SibsPaymentRequest> findAll() {
        return PaymentRequest.findAll().filter(p -> p instanceof SibsPaymentRequest).map(SibsPaymentRequest.class::cast);
    }

    public static Stream<SibsPaymentRequest> find(String entityReferenceCode, String referenceCode) {
        return findAll().filter(p -> entityReferenceCode.equals(p.getEntityReferenceCode()))
                .filter(p -> referenceCode.equals(p.getReferenceCode()));
    }

    public static Stream<SibsPaymentRequest> findBySibsGatewayMerchantTransactionId(String sibsGatewayMerchantTransactionId) {
        return PaymentRequest.findBySibsGatewayMerchantTransactionId(sibsGatewayMerchantTransactionId)
                .filter(p -> p instanceof SibsPaymentRequest).map(SibsPaymentRequest.class::cast);
    }

    public static Stream<SibsPaymentRequest> findBySibsGatewayTransactionId(String sibsGatewayTransactionId) {
        return PaymentRequest.findBySibsGatewayTransactionId(sibsGatewayTransactionId)
                .filter(p -> p instanceof SibsPaymentRequest).map(SibsPaymentRequest.class::cast);
    }

    public static Optional<SibsPaymentRequest> findUniqueBySibsGatewayTransactionId(String sibsGatewayTransactionId) {
        return findBySibsGatewayTransactionId(sibsGatewayTransactionId).findAny();
    }

    public static Stream<SibsPaymentRequest> find(final DebitEntry debitEntry) {
        return debitEntry.getPaymentRequestsSet().stream().filter(r -> r instanceof SibsPaymentRequest)
                .map(SibsPaymentRequest.class::cast);
    }
    
    public static Stream<SibsPaymentRequest> find(Installment installment) {
        return installment.getPaymentRequestsSet().stream().filter(r -> r instanceof SibsPaymentRequest)
                .map(SibsPaymentRequest.class::cast);
    }

    public static Stream<SibsPaymentRequest> findWithDebitEntries(final Set<DebitEntry> debitEntries) {
        final Set<SibsPaymentRequest> paymentCodes = debitEntries.stream().flatMap(d -> find(d)).collect(Collectors.toSet());

        final Set<SibsPaymentRequest> result = Sets.newHashSet();
        for (SibsPaymentRequest code : paymentCodes) {
            if (!Sets.symmetricDifference(code.getDebitEntriesSet(), debitEntries).isEmpty()) {
                continue;
            }

            result.add(code);
        }

        return result.stream();
    }

    public static Stream<SibsPaymentRequest> findCreatedByDebitEntry(final DebitEntry debitEntry) {
        return find(debitEntry).filter(p -> p.isInCreatedState());
    }

    public static Stream<SibsPaymentRequest> findRequestedByDebitEntry(final DebitEntry debitEntry) {
        return find(debitEntry).filter(p -> p.isInRequestedState());
    }

    public static Stream<SibsPaymentRequest> findCreatedByInstallment(Installment installment) {
        return find(installment).filter(p -> p.isInCreatedState());
    }

    public static Stream<SibsPaymentRequest> findRequestedByInstallment(Installment installment) {
        return find(installment).filter(p -> p.isInRequestedState());
    }

    public static Stream<SibsPaymentRequest> findCreatedByDebitEntriesSet(final Set<DebitEntry> debitEntries) {
        return findWithDebitEntries(debitEntries).filter(p -> p.isInCreatedState());
    }

    public static Stream<SibsPaymentRequest> findRequestedByDebitEntriesSet(final Set<DebitEntry> debitEntries) {
        return findWithDebitEntries(debitEntries).filter(p -> p.isInRequestedState());
    }

    public static SibsPaymentRequest create(SibsReferenceCode sibsReferenceCode, DebtAccount debtAccount,
            Set<DebitEntry> debitEntries, Set<Installment> installments, BigDecimal payableAmount) {
        return new SibsPaymentRequest(sibsReferenceCode, debtAccount, debitEntries, installments, payableAmount);
    }

    public static SibsPaymentRequest create(DigitalPaymentPlatform platform, DebtAccount debtAccount,
            Set<DebitEntry> debitEntries, Set<Installment> installments, BigDecimal payableAmount, String entityCode,
            String referenceCode, String merchantTransactionId, String sibsReferenceId) {
        return new SibsPaymentRequest(platform, debtAccount, debitEntries, installments, entityCode, referenceCode, payableAmount,
                merchantTransactionId, sibsReferenceId);
    }

    public String getUiDescription() {
        return String.format("%s ( %s - %s )", getPaymentMethod().getName().getContent(), getEntityReferenceCode(),
                getReferenceCode());
    }
}
