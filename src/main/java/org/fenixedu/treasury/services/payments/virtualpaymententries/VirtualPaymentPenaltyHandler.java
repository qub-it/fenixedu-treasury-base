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
package org.fenixedu.treasury.services.payments.virtualpaymententries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxSettings;
import org.fenixedu.treasury.domain.paymentpenalty.PaymentPenaltyTaxTreasuryEvent;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.PaymentPenaltyEntryBean;
import org.fenixedu.treasury.dto.SettlementDebitEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.LocalDate;

public class VirtualPaymentPenaltyHandler implements IVirtualPaymentEntryHandler {
    @Override
    public List<ISettlementInvoiceEntryBean> createISettlementInvoiceEntryBean(SettlementNoteBean settlementNoteBean) {
        if (PaymentPenaltyTaxSettings.findActive().findFirst().isEmpty()) {
            return new ArrayList<>();
        }
        List<ISettlementInvoiceEntryBean> result = new ArrayList<>();

        for (SettlementDebitEntryBean debitEntryBean : settlementNoteBean.getDebitEntriesByType(SettlementDebitEntryBean.class)) {
            if (debitEntryBean.isIncluded() && TreasuryConstants.isEqual(debitEntryBean.getDebitEntry().getOpenAmount(),
                    debitEntryBean.getSettledAmount())) {

                PaymentPenaltyEntryBean calculatePaymentPenaltyTax = PaymentPenaltyTaxTreasuryEvent
                        .calculatePaymentPenaltyTax(debitEntryBean.getDebitEntry(), settlementNoteBean.getDate().toLocalDate());

                if (calculatePaymentPenaltyTax != null) {

                    // It will be included by default if TreasurySettings.getInstance().isRestrictPaymentMixingLegacyInvoices() == false
                    // and debit not is not exported in legacy ERP
                    calculatePaymentPenaltyTax.setIncluded(
                            !debitEntryBean.getDebitEntry().isExportedInERPAndInRestrictedPaymentMixingLegacyInvoices());

                    calculatePaymentPenaltyTax.setVirtualPaymentEntryHandler(this);
                    calculatePaymentPenaltyTax
                            .setCalculationDescription(getCalculationDescription(settlementNoteBean, calculatePaymentPenaltyTax));
                    result.add(calculatePaymentPenaltyTax);
                }
            }
        }
        return result;
    }

    private Map<String, List<String>> getCalculationDescription(SettlementNoteBean settlementNoteBean,
            PaymentPenaltyEntryBean calculatePaymentPenaltyTax) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        String title = TreasuryConstants.treasuryBundle("label.VirtualPaymentPenaltyHandler.Payment_Penalty");
        List<String> lines = new ArrayList<>();
        lines.add(calculatePaymentPenaltyTax.getDebitEntry().getDescription());
        map.put(title, lines);
        return map;
    }

    @Override
    public void execute(SettlementNoteBean settlementNoteBean, ISettlementInvoiceEntryBean invoiceEntryBean) {
        if (!(invoiceEntryBean instanceof PaymentPenaltyEntryBean)) {
            return;
        }

        PaymentPenaltyEntryBean paymentPenaltyEntryBean = (PaymentPenaltyEntryBean) invoiceEntryBean;
        FinantialEntity finantialEntity = paymentPenaltyEntryBean.getDebitEntry().getFinantialEntity();

        DebtAccount debtAccount = settlementNoteBean.getDebtAccount();
        DocumentNumberSeries documentNumberSeries =
                DocumentNumberSeries.findUniqueDefaultSeries(FinantialDocumentType.findForDebitNote(), finantialEntity);
        LocalDate whenDebtCreationDate = settlementNoteBean.getDate().toLocalDate();
        DebitNote debitNote = DebitNote.create(settlementNoteBean.getFinantialEntity(), debtAccount, null, documentNumberSeries,
                whenDebtCreationDate.toDateTimeAtStartOfDay(), whenDebtCreationDate, null, Collections.emptyMap(), null, null);

        if (settlementNoteBean.getReferencedCustomers().size() == 1
                && settlementNoteBean.getReferencedCustomers().iterator().next() != debtAccount.getCustomer()) {
            Customer payorCustomer = settlementNoteBean.getReferencedCustomers().iterator().next();
            DebtAccount payorDebtAccount = payorCustomer.getDebtAccountFor(debtAccount.getFinantialInstitution());
            debitNote.setPayorDebtAccount(payorDebtAccount);
        }

        DebitEntry paymentPenaltyEntry = PaymentPenaltyTaxTreasuryEvent
                .checkAndCreatePaymentPenaltyTax(paymentPenaltyEntryBean.getDebitEntry(), whenDebtCreationDate, debitNote, false);

        SettlementDebitEntryBean settlementDebitEntryBean = new SettlementDebitEntryBean(paymentPenaltyEntry);
        settlementDebitEntryBean.setIncluded(true);
        settlementNoteBean.getDebitEntries().add(settlementDebitEntryBean);

        // As we are adding a debitEntryBean to settle, mark the virtual entry bean as not included, 
        // to not influence the calculation of advanced payment amount
        paymentPenaltyEntryBean.setIncluded(false);
    }

}
