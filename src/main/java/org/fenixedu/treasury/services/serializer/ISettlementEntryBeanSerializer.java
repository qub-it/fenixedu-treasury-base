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
package org.fenixedu.treasury.services.serializer;

import org.fenixedu.treasury.dto.ISettlementInvoiceEntryBean;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.Period;

import com.google.gson.GsonBuilder;
import com.qubit.terra.framework.tools.gson.serializers.ByteArraySerializer;
import com.qubit.terra.framework.tools.gson.serializers.ClassSerializer;
import com.qubit.terra.framework.tools.gson.serializers.DateTimeSerializer;
import com.qubit.terra.framework.tools.gson.serializers.DurationSerializer;
import com.qubit.terra.framework.tools.gson.serializers.LocalDateSerializer;
import com.qubit.terra.framework.tools.gson.serializers.LocalTimeSerializer;
import com.qubit.terra.framework.tools.gson.serializers.LocalizedStringSerializer;
import com.qubit.terra.framework.tools.gson.serializers.PeriodSerializer;
import com.qubit.terra.framework.tools.primitives.LocalizedString;
import com.qubit.terra.framework.tools.serializer.ObjectSerializer;

public class ISettlementEntryBeanSerializer implements ObjectSerializer<ISettlementInvoiceEntryBean> {
    public static final String DUE_DATE = "dueDate";
    public static final String CREATION_DATE = "creationDate";
    public static final String DESCRITPION = "descritpion";
    public static final String AMOUNT = "amount";

    public static final String PRODUCT_ID = "productId";
    public static final String INCLUDED = "included";
    public static final String NOT_VALID = "notValid";

    public static final String DEBIT_ENTRY_ID = "debitEntryId";
    private final static GsonBuilder gsonBuilder = new GsonBuilder();

    static {
        gsonBuilder.registerTypeAdapter(DateTime.class, new DateTimeSerializer());
        gsonBuilder.registerTypeAdapter(LocalDate.class, new LocalDateSerializer());
        gsonBuilder.registerTypeAdapter(LocalTime.class, new LocalTimeSerializer());
        gsonBuilder.registerTypeAdapter(Duration.class, new DurationSerializer());
        gsonBuilder.registerTypeAdapter(Period.class, new PeriodSerializer());
        gsonBuilder.registerTypeAdapter(byte[].class, new ByteArraySerializer());
        gsonBuilder.registerTypeAdapter(LocalizedString.class, new LocalizedStringSerializer());
        gsonBuilder.registerTypeAdapter(Class.class, new ClassSerializer());
    }

    @Override
    public ISettlementInvoiceEntryBean deserialize(String serializedObject) {
        if (serializedObject == null) {
            return null;
        }
        return ISettlementInvoiceEntryBean.deserialize(serializedObject);
    }

    @Override
    public Class<ISettlementInvoiceEntryBean> getType() {
        return ISettlementInvoiceEntryBean.class;
    }

    @Override
    public String serialize(ISettlementInvoiceEntryBean entry) {
        if (entry == null) {
            return null;
        }
        return entry.serialize();

    }

}
