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

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.paymentPlan.Installment;
import org.fenixedu.treasury.domain.sibsonlinepaymentsgateway.SibsOnlinePaymentsGateway;
import org.fenixedu.treasury.services.integration.FenixEDUTreasuryPlatformDependentServices;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import pt.ist.fenixframework.Atomic;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

@Deprecated
public abstract class PaymentCodeTarget extends PaymentCodeTarget_Base {

    public PaymentCodeTarget() {
        super();
    }

    public abstract Set<SettlementNote> processPayment(final String username, final BigDecimal amountToPay,
            DateTime whenRegistered, String sibsTransactionId, String comments);

    public abstract String getDescription();

    public String getTargetPayorDescription() {
        if (getDebtAccount() != null) {
            return getDebtAccount().getCustomer().getBusinessIdentification() + "-" + getDebtAccount().getCustomer().getName();
        }
        return "----";
    }

    public abstract boolean isPaymentCodeFor(final TreasuryEvent event);

    public BigDecimal getPayableAmount() {
        return getPaymentReferenceCode().getPayableAmount();
    }

    public boolean isPaymentCodeTarget() {
        return true;
    }

    public boolean isMultipleEntriesPaymentCode() {
        return false;
    }

    public boolean isFinantialDocumentPaymentCode() {
        return false;
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
    protected Set<SettlementNote> internalProcessPayment(final String username, final BigDecimal amount,
            final DateTime whenRegistered, final String sibsTransactionId, final String comments,
            Set<InvoiceEntry> invoiceEntriesToPay, Set<Installment> installmentsToPay) {
        throw new RuntimeException("not supported");
    }

    public String fillPaymentEntryMethodId() {
        // ANIL (2017-09-13) Required by used ERP at this date
        return String.format("COB PAG SERV %s", getPaymentReferenceCode().getPaymentCodePool().getEntityReferenceCode());
    }

    private Map<String, String> fillPaymentEntryPropertiesMap(final String sibsTransactionId) {
        final Map<String, String> paymentEntryPropertiesMap = Maps.newHashMap();
        paymentEntryPropertiesMap.put("ReferenceCode", getPaymentReferenceCode().getReferenceCode());
        paymentEntryPropertiesMap.put("EntityReferenceCode",
                getPaymentReferenceCode().getPaymentCodePool().getEntityReferenceCode());

        if (!Strings.isNullOrEmpty(sibsTransactionId)) {
            paymentEntryPropertiesMap.put("SibsTransactionId", sibsTransactionId);
        }

        return paymentEntryPropertiesMap;
    }

    public static String PAYMENT_TYPE_DESCRIPTION() {
        return treasuryBundle("label.IPaymentProcessorForInvoiceEntries.paymentProcessorDescription.paymentReferenceCode");
    }

    public abstract DocumentNumberSeries getDocumentSeriesInterestDebits();

    public abstract DocumentNumberSeries getDocumentSeriesForPayments();

    public abstract LocalDate getDueDate();

    public abstract Set<Product> getReferencedProducts();

    public abstract Set<Customer> getReferencedCustomers();

    public PaymentMethod getPaymentMethod() {
        return getPaymentReferenceCode().getPaymentCodePool().getPaymentMethod();
    }

    public DateTime getPaymentRequestDate() {
        return FenixEDUTreasuryPlatformDependentServices.getVersioningCreationDate(this);
    }

    public String getPaymentRequestStateDescription() {
        return getPaymentReferenceCode().getState().getDescriptionI18N().getContent();
    }

    public String getPaymentTypeDescription() {
        return PAYMENT_TYPE_DESCRIPTION();
    }

    public SibsOnlinePaymentsGateway getSibsOnlinePaymentsGateway() {
        return getPaymentReferenceCode().getPaymentCodePool().getSibsOnlinePaymentsGateway();
    }

    public String getSibsOppwaMerchantTransactionId() {
        return getPaymentReferenceCode().getSibsMerchantTransactionId();
    }

    public String getSibsOppwaTransactionId() {
        return getPaymentReferenceCode().getSibsReferenceId();
    }
}
