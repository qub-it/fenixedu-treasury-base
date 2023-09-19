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

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.Vat;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.InvoiceEntry;
import org.fenixedu.treasury.services.payments.virtualpaymententries.IVirtualPaymentEntryHandler;
import org.joda.time.LocalDate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

public interface ISettlementInvoiceEntryBean {
    public static final String DUE_DATE = "dueDate";
    public static final String CREATION_DATE = "creationDate";
    public static final String DESCRITPION = "descritpion";
    public static final String AMOUNT = "amount";
    
    public static final String PRODUCT_ID = "productId";
    public static final String INCLUDED = "included";
    public static final String NOT_VALID = "notValid";

    public static final String DEBIT_ENTRY_ID = "debitEntryId";
    public static final String TYPE = "type";

    public InvoiceEntry getInvoiceEntry();

    public String getDescription();

    public LocalDate getDueDate();

    /**
     * Amount divida
     * Open amount divida
     *
     * paymentAmount
     *
     * @return
     */

    public BigDecimal getEntryAmount();

    public BigDecimal getEntryOpenAmount();

    public BigDecimal getSettledAmount();

    public void setSettledAmount(BigDecimal debtAmount);

    public Vat getVat();

    public BigDecimal getVatRate();

    public boolean isIncluded();

    public void setIncluded(boolean isIncluded);

    public boolean isNotValid();

    public void setNotValid(boolean notValid);

    public FinantialDocument getFinantialDocument();

    // TODO: Rename method to getPaymentCustomers or getPaymentCustomerSet
    public Set<Customer> getPaymentCustomer();

    /*
     * Methods to support jsp, overriden in subclasses
     */

    default boolean isForDebitEntry() {
        return false;
    }

    default boolean isForInstallment() {
        return false;
    }

    default boolean isForCreditEntry() {
        return false;
    }

    default boolean isForPendingInterest() {
        return false;
    }

    default public boolean isForPaymentPenalty() {
        return false;
    }

    default public boolean isForPendingDebitEntry() {
        return false;
    }

    default IVirtualPaymentEntryHandler getVirtualPaymentEntryHandler() {
        return null;
    }

    default Map<String, List<String>> getCalculationDescription() {
        return Collections.emptyMap();
    }

    public String serialize();

    public void fillSerializable(JsonObject jsonObject);

    public static ISettlementInvoiceEntryBean deserialize(String serializedObject) {
        try {
            JsonObject jsonObject = new Gson().fromJson(serializedObject, JsonObject.class);

            Class<?> objectClass = Class.forName(jsonObject.get(TYPE).getAsString());
            ISettlementInvoiceEntryBean newInstance = (ISettlementInvoiceEntryBean) objectClass.getConstructor().newInstance();
            newInstance.fillSerializable(jsonObject);
            return newInstance;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
                | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

}
