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
package org.fenixedu.treasury.services.integration.erp.sap;

import static org.fenixedu.treasury.util.TreasuryConstants.isPositive;
import static org.fenixedu.treasury.util.TreasuryConstants.divide;

import java.math.BigDecimal;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.FinantialDocumentEntry;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.document.SettlementEntry;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;

public class SAPExporterUtils {
    
    public static BigDecimal openAmountAtDate(final InvoiceEntry invoiceEntry, final DateTime when) {
        final Currency currency = invoiceEntry.getDebtAccount().getFinantialInstitution().getCurrency();
        
        if (invoiceEntry.isAnnulled()) {
            return BigDecimal.ZERO;
        }

        final BigDecimal openAmount = invoiceEntry.getAmountWithVat().subtract(payedAmountAtDate(invoiceEntry, when));

        return currency.getValueWithScale(isPositive(openAmount) ? openAmount : BigDecimal.ZERO);
    }
    
    public static BigDecimal payedAmountAtDate(final InvoiceEntry invoiceEntry, final DateTime when) {
        BigDecimal amount = BigDecimal.ZERO;
        for (final SettlementEntry entry : invoiceEntry.getSettlementEntriesSet()) {
            if(!entry.getCloseDate().isBefore(when)) {
                continue;
            }
            
            if (entry.getFinantialDocument() != null && entry.getFinantialDocument().isClosed()) {
                amount = amount.add(entry.getTotalAmount());
            }
        }
        
        return amount;
    }
    
    public static BigDecimal amountAtDate(final Invoice invoice, final DateTime when) {
        BigDecimal amount = BigDecimal.ZERO;
        for (FinantialDocumentEntry entry : invoice.getFinantialDocumentEntriesSet()) {
            amount = amount.add(openAmountAtDate((InvoiceEntry) entry, when));
        }

        return invoice.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(amount);
    }

    public static BigDecimal netAmountAtDate(final Invoice invoice, final DateTime when) {
        BigDecimal amount = BigDecimal.ZERO;
        for (FinantialDocumentEntry entry : invoice.getFinantialDocumentEntriesSet()) {
            if(!TreasuryConstants.isPositive(entry.getTotalAmount())) {
                continue;
            }
            
            BigDecimal entryAmountAtDate = openAmountAtDate((InvoiceEntry) entry, when);
            entryAmountAtDate = divide(entry.getNetAmount().multiply(entryAmountAtDate), entry.getTotalAmount());
            
            amount = amount.add(entryAmountAtDate);
        }

        return invoice.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(amount);
    }
    
}
