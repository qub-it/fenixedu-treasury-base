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
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.sibspaymentsgateway.integration.SibsPaymentsGateway;
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
        
        if(getPaymentRequest().getDigitalPaymentPlatform() instanceof SibsPaymentsGateway) {
            if (!StringUtils.isEmpty(getSibsTransactionId()) && findBySibsGatewayTransactionId(getSibsTransactionId()).count() > 1) {
                throw new TreasuryDomainException("error.SibsPaymentCodeTransaction.sibsTransactionId.duplicate",
                        getSibsTransactionId());
            }
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
