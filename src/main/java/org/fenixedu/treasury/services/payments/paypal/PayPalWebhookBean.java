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
package org.fenixedu.treasury.services.payments.paypal;

import java.io.IOException;
import java.math.BigDecimal;

import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.domain.paypal.PayPal;
import org.joda.time.DateTime;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.paypal.http.serializer.Json;
import com.paypal.orders.Order;

public class PayPalWebhookBean implements DigitalPlatformResultBean {
    private String resource_type;
    private String event_type;
    private Order order;
    
    public PayPalWebhookBean(String body) {
        JsonParser parser = new JsonParser();
        JsonObject json = (JsonObject) parser.parse(body);
        JsonElement jsonElement = json.get("resource");

        try {
            resource_type = json.get("resource_type").getAsString();
            event_type = json.get("event_type").getAsString();
            order = new Json().decode(jsonElement.toString(), Order.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getResource_type() {
        return resource_type;
    }

    public String getEvent_type() {
        return event_type;
    }

    public Order getOrder() {
        return order;
    }

    public void setResource_type(String resource_type) {
        this.resource_type = resource_type;
    }

    public void setEvent_type(String event_type) {
        this.event_type = event_type;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    @Override
    public BigDecimal getAmount() {
        if ("COMPLETED".equals(order.status())) {
            return order.purchaseUnits().stream().flatMap(unit -> unit.payments().captures().stream())
                    .map(capture -> new BigDecimal(capture.amount().value())).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        if ("APPROVED".equals(order.status())) {
            return order.purchaseUnits().stream().map(unit -> new BigDecimal(unit.amountWithBreakdown().value()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        return BigDecimal.ZERO;
    }

    @Override
    public String getMerchantTransactionId() {
        return null;
    }

    @Override
    public String getPaymentBrand() {
        return null;
    }

    @Override
    public DateTime getPaymentDate() {
        return DateTime.parse(order.updateTime() == null ? order.createTime() : order.updateTime());
    }

    @Override
    public String getPaymentResultCode() {
        return order.status();
    }

    @Override
    public String getPaymentResultDescription() {
        return event_type;
    }

    @Override
    public String getPaymentType() {
        return resource_type;
    }

    @Override
    public String getTimestamp() {
        return null;
    }

    @Override
    public String getTransactionId() {
        return order.status();
    }

    @Override
    public boolean isOperationSuccess() {
        return !PayPal.STATUS_FAIL.equals(order.status());
    }

    @Override
    public boolean isPaid() {
        return PayPal.STATUS_PAID.equals(order.status());
    }
}
