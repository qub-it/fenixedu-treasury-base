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

import static java.lang.String.format;
import static org.fenixedu.treasury.util.TreasuryConstants.DATE_FORMAT_YYYY_MM_DD;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.DebitNote;
import org.fenixedu.treasury.domain.document.DocumentNumberSeries;
import org.fenixedu.treasury.domain.document.FinantialDocumentType;
import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.fenixedu.treasury.dto.InterestRateBean;
import org.fenixedu.treasury.dto.InterestRateBean.CreatedInterestEntry;
import org.fenixedu.treasury.dto.InterestRateBean.InterestInformationDetail;
import org.fenixedu.treasury.dto.SettlementDebitEntryBean;
import org.fenixedu.treasury.dto.SettlementInterestEntryBean;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

public class VirtualInterestHandler implements IVirtualPaymentEntryHandler {
    @Override
    public List<ISettlementInvoiceEntryBean> createISettlementInvoiceEntryBean(SettlementNoteBean settlementNoteBean) {
        List<ISettlementInvoiceEntryBean> result = new ArrayList<>();

        for (SettlementDebitEntryBean debitEntryBean : settlementNoteBean.getDebitEntriesByType(SettlementDebitEntryBean.class)) {
            if (debitEntryBean.isIncluded() && TreasuryConstants.isEqual(debitEntryBean.getDebitEntry().getOpenAmount(),
                    debitEntryBean.getSettledAmount())) {

                //Calculate interest only if we are making a FullPayment
                InterestRateBean debitInterest =
                        debitEntryBean.getDebitEntry().calculateUndebitedInterestValue(settlementNoteBean.getDate());
                if (TreasuryConstants.isPositive(debitInterest.getInterestAmount())) {
                    SettlementInterestEntryBean interestEntryBean =
                            new SettlementInterestEntryBean(debitEntryBean.getDebitEntry(), debitInterest);
                    interestEntryBean.setVirtualPaymentEntryHandler(this);
                    interestEntryBean.setIncluded(true);
                    interestEntryBean.setCalculationDescription(getCalculationDescription(settlementNoteBean, interestEntryBean));

                    result.add(interestEntryBean);
                }
            }
        }
        return result;
    }

    private Map<String, List<String>> getCalculationDescription(SettlementNoteBean settlementNoteBean,
            SettlementInterestEntryBean interestEntryBean) {
        Map<String, List<String>> map = new LinkedHashMap<>();
        final Currency currency = settlementNoteBean.getDebtAccount().getFinantialInstitution().getCurrency();

        String title = TreasuryConstants.treasuryBundle("label.VirtualInterestHandler.Calculated_interests");
        List<String> lines = new ArrayList<>();
        for (final InterestInformationDetail detail : interestEntryBean.getInterest().getInterestInformationList()) {
            lines.add(format("[%s - %s]: %s", detail.getBegin().toString(DATE_FORMAT_YYYY_MM_DD),
                    detail.getEnd().toString(DATE_FORMAT_YYYY_MM_DD), currency.getValueFor(detail.getAmount())));

            lines.add(TreasuryConstants.treasuryBundle("label.InterestEntry.affectedAmount.description",
                    currency.getValueFor(detail.getAffectedAmount()), "" + detail.getNumberOfDays(),
                    detail.getInterestRate().toString()));
        }
        map.put(title, lines);
        if (!interestEntryBean.getInterest().getCreatedInterestEntriesList().isEmpty()) {
            title = TreasuryConstants.treasuryBundle("label.VirtualInterestHandler.Created_interests");

            lines = new ArrayList<>();
            for (final CreatedInterestEntry entry : interestEntryBean.getInterest().getCreatedInterestEntriesList()) {
                {
                    lines.add(format("[%s]: %s", entry.getEntryDate().toString(DATE_FORMAT_YYYY_MM_DD),
                            currency.getValueFor(entry.getAmount())));
                }
            }
            map.put(title, lines);
        }
        return map;
    }

    @Override
    public void execute(SettlementNoteBean settlementNoteBean, ISettlementInvoiceEntryBean invoiceEntryBean) {
        DocumentNumberSeries debitNoteSeries = DocumentNumberSeries
                .find(FinantialDocumentType.findForDebitNote(), settlementNoteBean.getDebtAccount().getFinantialInstitution())
                .filter(x -> Boolean.TRUE.equals(x.getSeries().getDefaultSeries())).findFirst().orElse(null);
        if (!(invoiceEntryBean instanceof SettlementInterestEntryBean)) {
            return;
        }

        SettlementInterestEntryBean interestEntryBean = (SettlementInterestEntryBean) invoiceEntryBean;

        DebitNote interestDebitNote = DebitNote.create(settlementNoteBean.getDebtAccount(), debitNoteSeries, new DateTime());

        if (settlementNoteBean.getReferencedCustomers().size() == 1 && settlementNoteBean.getReferencedCustomers().iterator()
                .next() != settlementNoteBean.getDebtAccount().getCustomer()) {
            Customer payorCustomer = settlementNoteBean.getReferencedCustomers().iterator().next();
            DebtAccount payorDebtAccount = payorCustomer.getDebtAccountFor(settlementNoteBean.getDebtAccount().getFinantialInstitution());
            interestDebitNote.setPayorDebtAccount(payorDebtAccount);
        }

        DebitEntry interestDebitEntry = interestEntryBean.getDebitEntry().createInterestRateDebitEntry(
                interestEntryBean.getInterest(), new DateTime(), Optional.<DebitNote> ofNullable(interestDebitNote));
        SettlementDebitEntryBean settlementDebitEntryBean = new SettlementDebitEntryBean(interestDebitEntry);
        settlementDebitEntryBean.setIncluded(true);
        settlementNoteBean.getDebitEntries().add(settlementDebitEntryBean);
        
        // As we are adding a debitEntryBean to settle, mark the virtual entry bean as not included, 
        // to not influence the calculation of advanced payment amount
        // TODO: Check with Diogo
        interestEntryBean.setIncluded(false);

    }

}
