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

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.VatType;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.services.payments.virtualpaymententries.IVirtualPaymentEntryHandler;
import org.fenixedu.treasury.services.payments.virtualpaymententries.VirtualPaymentEntryFactory;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.ISODateTimeFormat;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import pt.ist.fenixframework.FenixFramework;

public class SettlementInterestEntryBean implements ISettlementInvoiceEntryBean, ITreasuryBean, Serializable {

    private static final String INTEREST_DESCRIPTION = "interestDescription";

    private static final String VIRTUAL_PAYMENT_ENTRY_HANDLER = "virtualPaymentEntryHandler";

    private static final String INTEREST_STATIC = "interest";

    private static final String DESCRIPTION = "description";

    private static final String INTEREST_DEBIT_ENTRY_DATE_TIME = "interestDebitEntryDateTime";

    private static final long serialVersionUID = 1L;

    private DebitEntry debitEntry;

    private boolean isIncluded;

    private InterestRateBean interest;

    private IVirtualPaymentEntryHandler virtualPaymentEntryHandler;
    private Map<String, List<String>> calculationDescription;

    public SettlementInterestEntryBean() {
        this.isIncluded = false;
    }

    public SettlementInterestEntryBean(DebitEntry debitEntry, InterestRateBean interest) {
        this();
        this.debitEntry = debitEntry;
        this.interest = interest;
    }

    public InterestRateBean getInterest() {
        return interest;
    }

    public void setInterest(InterestRateBean interest) {
        this.interest = interest;
    }

    public DebitEntry getDebitEntry() {
        return debitEntry;
    }

    public void setDebitEntry(DebitEntry debitEntry) {
        this.debitEntry = debitEntry;
    }

    public LocalDate getDocumentDueDate() {
        return debitEntry.getFinantialDocument() != null ? debitEntry.getFinantialDocument().getDocumentDueDate() : debitEntry
                .getDueDate();
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
    public InvoiceEntry getInvoiceEntry() {
        return null;
    }

    @Override
    public String getDescription() {
        return getInterest().getDescription();
    }

    @Override
    public boolean isNotValid() {
        return false;
    }

    @Override
    public void setNotValid(boolean notValid) {
    }

    @Override
    public BigDecimal getVatRate() {
        return debitEntry.getVatRate();
    }

    @Override
    public FinantialDocument getFinantialDocument() {
        return null;
    }

    @Override
    public Vat getVat() {
        final VatType vatType = TreasurySettings.getInstance().getInterestProduct().getVatType();
        return Vat.findActiveUnique(vatType, debitEntry.getDebtAccount().getFinantialInstitution(), DateTime.now()).get();

    }

    @Override
    public Set<Customer> getPaymentCustomer() {
        return Collections.emptySet();
    }

    @Override
    public LocalDate getDueDate() {
        if (this.interest != null && this.interest.getInterestDebitEntryDateTime() != null) {
            return this.interest.getInterestDebitEntryDateTime().toLocalDate();
        }

        return new LocalDate();
    }

    @Override
    public BigDecimal getEntryAmount() {
        return null;
    }

    @Override
    public BigDecimal getEntryOpenAmount() {
        return null;
    }

    @Override
    public BigDecimal getSettledAmount() {
        return getInterest().getInterestAmount();
    }

    @Override
    public void setSettledAmount(BigDecimal debtAmount) {

    }

    /*
     * Methods to support jsp, overriden in subclasses
     */

    @Override
    public boolean isForPendingInterest() {
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
        jsonObject.add(DEBIT_ENTRY_ID, new JsonPrimitive(getDebitEntry().getExternalId()));
        jsonObject.add(INCLUDED, new JsonPrimitive(isIncluded));
        jsonObject.add(INTEREST_STATIC, new JsonPrimitive(serializeInterest(interest)));
        jsonObject.add(DESCRIPTION, new JsonPrimitive(serializeCalculationDescription(calculationDescription)));
        jsonObject.add(VIRTUAL_PAYMENT_ENTRY_HANDLER,
                new JsonPrimitive(virtualPaymentEntryHandler != null ? virtualPaymentEntryHandler.getClass().getName() : ""));

        return jsonObject.toString();
    }

    @Override
    public void fillSerializable(JsonObject jsonObject) {
        this.debitEntry = FenixFramework.getDomainObject(jsonObject.get(DEBIT_ENTRY_ID).getAsString());
        this.isIncluded = jsonObject.get(INCLUDED).getAsBoolean();
        this.calculationDescription = deserializeCalculationDescription(jsonObject.get(DESCRIPTION).getAsString());
        this.interest = deserializeInterest(new Gson().fromJson(jsonObject.get(INTEREST_STATIC).getAsString(), JsonObject.class));

        for (IVirtualPaymentEntryHandler handler : VirtualPaymentEntryFactory.implementation().getHandlers()) {
            String className = jsonObject.get(VIRTUAL_PAYMENT_ENTRY_HANDLER).getAsString();
            if (className.equals(handler.getClass().getName())) {
                this.virtualPaymentEntryHandler = handler;
            }
        }
    }

    private String serializeInterest(InterestRateBean interest2) {

        JsonObject jsonObject = new JsonObject();
        jsonObject.add(INTEREST_DESCRIPTION, new JsonPrimitive(interest2.getDescription()));
        jsonObject.add(AMOUNT, new JsonPrimitive(interest2.getInterestAmount()));

        String interestDebitEntryDateTime = interest2.getInterestDebitEntryDateTime() != null ? interest2
                .getInterestDebitEntryDateTime().toString(ISODateTimeFormat.dateTime()) : "";
        jsonObject.add(INTEREST_DEBIT_ENTRY_DATE_TIME, new JsonPrimitive(interestDebitEntryDateTime));

        return jsonObject.toString();
    }

    private InterestRateBean deserializeInterest(JsonObject jsonObject) {
        InterestRateBean interestRateBean = new InterestRateBean();
        interestRateBean.setDescription(jsonObject.get(INTEREST_DESCRIPTION).getAsString());
        interestRateBean.setInterestAmount(jsonObject.get(AMOUNT).getAsBigDecimal());

        if (StringUtils.isNotEmpty(jsonObject.get(INTEREST_DEBIT_ENTRY_DATE_TIME).getAsString())) {
            String dateTimeSerializedValue = jsonObject.get(INTEREST_DEBIT_ENTRY_DATE_TIME).getAsString();
            DateTime entryDateTime = ISODateTimeFormat.dateTime().parseDateTime(dateTimeSerializedValue);
            interestRateBean.setInterestDebitEntryDateTime(entryDateTime);
        }

        return interestRateBean;
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
