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
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.Invoice;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.services.payments.virtualpaymententries.IVirtualPaymentEntryHandler;
import org.fenixedu.treasury.services.payments.virtualpaymententries.VirtualPaymentEntryFactory;
import org.joda.time.LocalDate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import pt.ist.fenixframework.FenixFramework;

public class PaymentPenaltyEntryBean implements ISettlementInvoiceEntryBean, ITreasuryBean, Serializable {

    private static final String VIRTUAL_PAYMENT_ENTRY_HANDLER = "virtualPaymentEntryHandler";

    private static final String DESCRIPTION = "description";

    private static final String CALCULATED_DESCRIPTION = "calculatedDescription";

    private static final String DUE_DATE = "dueDate";

    private DebitEntry originDebitEntry;

    private boolean isIncluded;

    private boolean isNotValid;

    private String description;

    private LocalDate dueDate;

    private BigDecimal amount;

    private IVirtualPaymentEntryHandler virtualPaymentEntryHandler;
    private Map<String, List<String>> calculationDescription;

    public PaymentPenaltyEntryBean() {
        this.isIncluded = false;
    }

    public PaymentPenaltyEntryBean(DebitEntry originDebitEntry, String description, LocalDate dueDate, BigDecimal amount) {
        super();
        this.originDebitEntry = originDebitEntry;
        this.description = description;
        this.dueDate = dueDate;
        this.amount = amount;
    }

    public DebitEntry getDebitEntry() {
        return originDebitEntry;
    }

    @Override
    public InvoiceEntry getInvoiceEntry() {
        return null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public LocalDate getDueDate() {
        return dueDate;
    }

    @Override
    public BigDecimal getEntryAmount() {
        return amount;
    }

    @Override
    public BigDecimal getEntryOpenAmount() {
        return amount;
    }

    @Override
    public BigDecimal getSettledAmount() {
        return amount;
    }

    @Override
    public void setSettledAmount(BigDecimal debtAmount) {
        amount = debtAmount;
    }

    @Override
    public Vat getVat() {
        return null;
    }

    @Override
    public BigDecimal getVatRate() {
        return null;
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
        return null;
    }

    @Override
    public Set<Customer> getPaymentCustomer() {
        return originDebitEntry.getFinantialDocument() != null
                && ((Invoice) originDebitEntry.getFinantialDocument()).getPayorDebtAccount() != null ? Collections.singleton(
                        ((Invoice) originDebitEntry.getFinantialDocument()).getPayorDebtAccount().getCustomer()) : Collections
                                .singleton(originDebitEntry.getDebtAccount().getCustomer());
    }

    @Override
    public boolean isForPaymentPenalty() {
        return true;
    }

    @Override
    public IVirtualPaymentEntryHandler getVirtualPaymentEntryHandler() {
        return virtualPaymentEntryHandler;
    }

    public void setVirtualPaymentEntryHandler(IVirtualPaymentEntryHandler virtualPaymentEntryHandler) {
        this.virtualPaymentEntryHandler = virtualPaymentEntryHandler;
    }

    @Override
    public Map<String, List<String>> getCalculationDescription() {
        return calculationDescription;
    }

    public void setCalculationDescription(Map<String, List<String>> calculationDescription) {
        this.calculationDescription = calculationDescription;
    }

    @Override
    public String serialize() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.add(TYPE, new JsonPrimitive(getClass().getName()));
        jsonObject.add(AMOUNT, new JsonPrimitive(getSettledAmount().toPlainString()));
        jsonObject.add(CALCULATED_DESCRIPTION, new JsonPrimitive(serializeCalculationDescription(calculationDescription)));
        jsonObject.add(DESCRIPTION, new JsonPrimitive(description));
        jsonObject.add(DUE_DATE, new JsonPrimitive(dueDate.toString()));
        jsonObject.add(INCLUDED, new JsonPrimitive(isIncluded));
        jsonObject.add(NOT_VALID, new JsonPrimitive(isNotValid));
        jsonObject.add(DEBIT_ENTRY_ID, new JsonPrimitive(originDebitEntry.getExternalId()));
        jsonObject.add(VIRTUAL_PAYMENT_ENTRY_HANDLER,
                new JsonPrimitive(virtualPaymentEntryHandler != null ? virtualPaymentEntryHandler.getClass().getName() : ""));

        return jsonObject.toString();
    }

    @Override
    public void fillSerializable(JsonObject jsonObject) {
        this.amount = jsonObject.get(AMOUNT).getAsBigDecimal();
        this.calculationDescription = deserializeCalculationDescription(jsonObject.get(CALCULATED_DESCRIPTION).getAsString());
        this.description = jsonObject.get(DESCRIPTION).getAsString();
        this.dueDate = LocalDate.parse(jsonObject.get(DUE_DATE).getAsString());
        this.isIncluded = jsonObject.get(INCLUDED).getAsBoolean();
        this.isNotValid = jsonObject.get(NOT_VALID).getAsBoolean();
        this.originDebitEntry = FenixFramework.getDomainObject(jsonObject.get(DEBIT_ENTRY_ID).getAsString());

        for (IVirtualPaymentEntryHandler handler : VirtualPaymentEntryFactory.implementation().getHandlers()) {
            String className = jsonObject.get(VIRTUAL_PAYMENT_ENTRY_HANDLER).getAsString();
            if (className.equals(handler.getClass().getName())) {
                this.virtualPaymentEntryHandler = handler;
            }
        }
    }

    private String serializeCalculationDescription(Map<String, List<String>> calculationDescription2) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Map<String, List<String>>>() {
        }.getType();
        return gson.toJson(calculationDescription2, listType);
    }

    private Map<String, List<String>> deserializeCalculationDescription(String asString) {
        Gson gson = new Gson();
        Type listType = new TypeToken<Map<String, List<String>>>() {
        }.getType();
        return gson.fromJson(asString, listType);
    }

}
