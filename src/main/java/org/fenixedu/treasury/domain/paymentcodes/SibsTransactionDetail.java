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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class SibsTransactionDetail extends SibsTransactionDetail_Base {

    protected SibsTransactionDetail() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(final SibsReportFile sibsReport, final String comments, final DateTime whenProcessed,
            final DateTime whenRegistered, final java.math.BigDecimal amountPayed, final String sibsEntityReferenceCode,
            final String sibsPaymentReferenceCode, final String sibsTransactionId, final String debtAccountId,
            final String customerId, final String businessIdentification, final String fiscalNumber, final String customerName,
            final String settlementDocumentNumber) {
        setSibsReport(sibsReport);
        setComments(comments);
        setWhenProcessed(whenProcessed);
        setWhenRegistered(whenRegistered);
        setAmountPayed(amountPayed);
        setSibsEntityReferenceCode(sibsEntityReferenceCode);
        setSibsPaymentReferenceCode(sibsPaymentReferenceCode);
        setSibsTransactionId(sibsTransactionId);
        setDebtAccountId(debtAccountId);
        setCustomerId(customerId);
        setBusinessIdentification(businessIdentification);
        setFiscalNumber(fiscalNumber);
        setCustomerName(customerName);
        setSettlementDocumentNumber(settlementDocumentNumber);

        checkRules();
    }

    private void checkRules() {
        if(getDomainRoot() == null) {
            throw new TreasuryDomainException("error.SibsTransactionDetail.domainRoot.required");
        }
        
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());
        super.setSibsReport(null);
        deleteDomainObject();
    }

    @Atomic
    public static SibsTransactionDetail create(final SibsReportFile sibsReport, final String comments,
            final DateTime whenProcessed, final DateTime paymentDate, final java.math.BigDecimal amountPayed,
            final String sibsEntityReferenceCode, final String sibsPaymentReferenceCode, final String sibsTransactionId,
            final String debtAccountId, final String customerId, final String businessIdentification, final String fiscalNumber,
            final String customerName, final String settlementDocumentNumber) {
        SibsTransactionDetail sibsTransactionDetail = new SibsTransactionDetail();

        sibsTransactionDetail.init(sibsReport, comments, whenProcessed, paymentDate, amountPayed, sibsEntityReferenceCode,
                sibsPaymentReferenceCode, sibsTransactionId, debtAccountId, customerId, businessIdentification, fiscalNumber,
                customerName, settlementDocumentNumber);
        return sibsTransactionDetail;
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<SibsTransactionDetail> findAll() {

        Set<SibsTransactionDetail> result = new HashSet<>();
        List<SibsReportFile> reports = SibsReportFile.findAll().collect(Collectors.toList());
        for (SibsReportFile report : reports) {
            result.addAll(report.getSibsTransactionsSet());
        }
        
        result.addAll(FenixFramework.getDomainRoot().getSibsTransactionDetailSet());
        
        return result.stream();
    }

    public static Stream<SibsTransactionDetail> findBySibsReport(final SibsReportFile sibsReport) {
        return sibsReport.getSibsTransactionsSet().stream();
    }

    public static Stream<SibsTransactionDetail> findByComments(final String comments) {
        return findAll().filter(i -> comments.equalsIgnoreCase(i.getComments()));
    }

    public static Stream<SibsTransactionDetail> findByWhenProcessed(final DateTime whenProcessed) {
        return findAll().filter(i -> whenProcessed.equals(i.getWhenProcessed()));
    }

    public static Stream<SibsTransactionDetail> findByWhenRegistered(final DateTime whenRegistered) {
        return findAll().filter(i -> whenRegistered.equals(i.getWhenRegistered()));
    }

    public static Stream<SibsTransactionDetail> findByAmountPayed(final java.math.BigDecimal amountPayed) {
        return findAll().filter(i -> amountPayed.equals(i.getAmountPayed()));
    }

    public static Stream<SibsTransactionDetail> findBySibsEntityReferenceCode(final String sibsEntityReferenceCode) {
        return findAll().filter(i -> sibsEntityReferenceCode.equalsIgnoreCase(i.getSibsEntityReferenceCode()));
    }

    public static Stream<SibsTransactionDetail> findBySibsPaymentReferenceCode(final String sibsPaymentReferenceCode) {
        return findAll().filter(i -> sibsPaymentReferenceCode.equalsIgnoreCase(i.getSibsPaymentReferenceCode()));
    }

    public static Stream<SibsTransactionDetail> findBySibsTransactionId(final String sibsTransactionId) {
        return findAll().filter(i -> sibsTransactionId.equalsIgnoreCase(i.getSibsTransactionId()));
    }
    
    public static Stream<SibsTransactionDetail> findBySibsEntityAndReferenceCode(final String sibsEntityReferenceCode,
            final String sibsPaymentReferenceCode) {
        return findBySibsEntityReferenceCode(sibsEntityReferenceCode)
                .filter(i -> sibsPaymentReferenceCode.equalsIgnoreCase(i.getSibsPaymentReferenceCode()));
    }

    public static boolean isReferenceProcessingDuplicate(String referenceCode, String entityReferenceCode,
            DateTime whenRegistered) {

        return findAll().anyMatch(x -> x.getSibsEntityReferenceCode().equals(entityReferenceCode)
                && x.getSibsPaymentReferenceCode().equals(referenceCode) && x.getWhenRegistered().equals(whenRegistered));
    }
    
    public static boolean isSibsOppwaReferenceProcessingDuplicate(final String sibsTransactionId) {
        return findBySibsTransactionId(sibsTransactionId).count() > 0;
    }
    
}
