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
package org.fenixedu.treasury.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.document.CreditEntry;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.services.serializer.ISettlementEntryBeanSerializer;
import org.joda.time.LocalDate;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.qubit.terra.framework.tools.serializer.IntrospectorTool;

import pt.ist.fenixframework.FenixFramework;

public class SettlementCreditEntryBean implements ISettlementInvoiceEntryBean, ITreasuryBean, Serializable {

    private static final String CREDIT_AMOUNT = "creditAmount";

    private static final long serialVersionUID = 1L;

    private CreditEntry creditEntry;

    private boolean isIncluded;

    private boolean isNotValid;

    private BigDecimal creditAmount;

    public SettlementCreditEntryBean() {
    }

    public SettlementCreditEntryBean(CreditEntry creditEntry) {
        this.creditEntry = creditEntry;
        this.isIncluded = false;
        this.isNotValid = false;
        this.creditAmount = creditEntry.getOpenAmount();
    }

    public CreditEntry getCreditEntry() {
        return creditEntry;
    }

    public void setCreditEntry(CreditEntry creditEntry) {
        this.creditEntry = creditEntry;
    }

    public String getDocumentNumber() {
        return creditEntry.getFinantialDocument() != null ? creditEntry.getFinantialDocument().getDocumentNumber() : null;
    }

    @Deprecated
    public LocalDate getDocumentDueDate() {
        return creditEntry.getFinantialDocument() != null ? creditEntry.getFinantialDocument().getDocumentDueDate() : creditEntry
                .getEntryDateTime().toLocalDate();
    }

    @Deprecated
    public BigDecimal getCreditAmount() {
        if (creditAmount == null) {
            return null;
        }

        return creditEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(creditAmount);
    }

    @Deprecated
    public BigDecimal getCreditAmountWithVat() {
        if (creditAmount == null) {
            return null;
        }

        return creditEntry.getDebtAccount().getFinantialInstitution().getCurrency().getValueWithScale(creditAmount);
    }

    @Deprecated
    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    @Override
    public InvoiceEntry getInvoiceEntry() {
        return creditEntry;
    }

    @Override
    public String getDescription() {
        return creditEntry.getDescription();
    }

    @Override
    public LocalDate getDueDate() {
        return creditEntry.getDueDate();
    }

    @Override
    public BigDecimal getEntryAmount() {
        return creditEntry.getAmount();
    }

    @Override
    public BigDecimal getEntryOpenAmount() {
        return creditEntry.getOpenAmount();
    }

    @Override
    public BigDecimal getSettledAmount() {
        return creditAmount;
    }

    @Override
    public void setSettledAmount(BigDecimal debtAmount) {
        this.creditAmount = debtAmount;
    }

    @Override
    public Vat getVat() {
        return creditEntry.getVat();
    }

    @Override
    public BigDecimal getVatRate() {
        return creditEntry.getVatRate();
    }

    @Override
    public boolean isIncluded() {
        return isIncluded;
    }

    @Override
    public void setIncluded(boolean isIncluded) {
        this.isIncluded = isIncluded;
    }

    @Override
    public boolean isNotValid() {
        return isNotValid;
    }

    @Override
    public void setNotValid(boolean notValid) {
        this.isNotValid = notValid;
    }

    @Override
    public FinantialDocument getFinantialDocument() {
        return creditEntry.getFinantialDocument();
    }

    @Override
    public Set<Customer> getPaymentCustomer() {
        return creditEntry.getFinantialDocument() != null
                && ((Invoice) creditEntry.getFinantialDocument()).getPayorDebtAccount() != null ? Collections.singleton(
                        ((Invoice) creditEntry.getFinantialDocument()).getPayorDebtAccount().getCustomer()) : Collections
                                .singleton(creditEntry.getDebtAccount().getCustomer());
    }

    /*
     * Methods to support jsp, overriden in subclasses
     */

    @Override
    public boolean isForCreditEntry() {
        return true;
    }

    @Override
    public String serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(IntrospectorTool.TYPE, new JsonPrimitive(getClass().getName()));
        jsonObject.add(CREDIT_AMOUNT, new JsonPrimitive(getInvoiceEntry().getExternalId()));
        jsonObject.add(ISettlementEntryBeanSerializer.INCLUDED, new JsonPrimitive(isIncluded));
        jsonObject.add(ISettlementEntryBeanSerializer.NOT_VALID, new JsonPrimitive(isNotValid));
        jsonObject.add(ISettlementEntryBeanSerializer.AMOUNT, new JsonPrimitive(getSettledAmount().toPlainString()));
        return jsonObject.toString();
    }

    @Override
    public void fillSerializable(JsonObject jsonObject) {
        this.creditEntry = FenixFramework.getDomainObject(jsonObject.get(CREDIT_AMOUNT).getAsString());
        this.isIncluded = jsonObject.get(ISettlementEntryBeanSerializer.INCLUDED).getAsBoolean();
        this.isNotValid = jsonObject.get(ISettlementEntryBeanSerializer.NOT_VALID).getAsBoolean();
        this.creditAmount = jsonObject.get(ISettlementEntryBeanSerializer.AMOUNT).getAsBigDecimal();
    }

}
