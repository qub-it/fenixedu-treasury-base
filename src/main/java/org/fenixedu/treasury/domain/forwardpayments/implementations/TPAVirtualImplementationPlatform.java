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
package org.fenixedu.treasury.domain.forwardpayments.implementations;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.settings.TreasurySettings;
import org.fenixedu.treasury.dto.SettlementNoteBean;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentStatusBean;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.google.common.base.Strings;
import com.google.gson.GsonBuilder;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;

@Deprecated
public class TPAVirtualImplementationPlatform extends TPAVirtualImplementationPlatform_Base
        implements IForwardPaymentPlatformService {

    static final String A030 = "A030";
    static final String TPA_VIRTUAL_ID_FIELD = "A001";
    static final String REFERENCE_CODE_FIELD = "C007";
    static final String CURRENCY_FIELD = "A105";
    static final String AMOUNT_FIELD = "A061";
    static final String PAN_FIELD = "C003";
    static final String EXPIRATION_FIELD = "C004";
    static final String OPERATION_STATUS_FIELD = "C016";
    static final String SECURE_HASH_CODE_FIELD = "C013";
    static final String A037 = "A037";

    public static final String AUTHENTICATION_REQUEST_MESSAGE = "H3D0";
    static final String AUTHENTICATION_RESPONSE_MESSAGE = "MH05";
    static final String C016_AUTHENTICATION_REGISTERED_CODE = "01";
    static final String C016_AUTHORIZATION_ACCEPTED_CODE = "02";
    static final String C016_PAYMENT_ACCEPTED_CODE = "03";
    static final String C016_AUTHORIZATION_CANCELLED = "04";
    static final String C016_UNABLE_TO_CONTACT_HOST_RESPONSE_CODE = "99";

    static final String A038 = "A038";
    static final String A038_SUCCESS = "000";

    static final String M020 = "M020";
    static final String M120 = "M120";

    static final String C016 = "C016";
    static final String C016_POS_RESP = "0X";
    static final String C016_NEG_RESP = "99";
    static final int C016_AUTHENTICATED_STATE = 1;
    static final int C016_AUTHORIZED = 2;
    static final int C016_PAYED = 3;
    static final int C016_AUTHORIZED_CANCELLED = 4;

    static final String C026 = "C026";

    static final String M001 = "M001";
    static final String M101 = "M101";

    static final String M002 = "M002";
    static final String M102 = "M102";

    static final String A077 = "A077";
    static final String A078 = "A078";
    static final String A085 = "A085";
    static final String A086 = "A086";

    public static final String EURO_CODE = "9782";

    public TPAVirtualImplementationPlatform() {
        super();
    }

    protected TPAVirtualImplementationPlatform(FinantialInstitution finantialInstitution, String name) {
        throw new RuntimeException("deprecated");
    }

    @Override
    public IForwardPaymentController getForwardPaymentController(ForwardPaymentRequest forwardPayment) {
        return IForwardPaymentController.getForwardPaymentController(forwardPayment);
    }

    @Atomic(mode = TxMode.WRITE)
    public boolean processPayment(ForwardPaymentRequest forwardPayment, Map<String, String> responseMap) {
        throw new RuntimeException("deprecated");
    }

    @Override
    public ForwardPaymentStatusBean paymentStatus(final ForwardPaymentRequest forwardPayment) {
        throw new RuntimeException("deprecated");
    }

    private String errorMessage(final Map<String, String> responseData) {
        throw new RuntimeException("deprecated");
    }

    public Map<String, String> mapAuthenticationRequest(final ForwardPaymentRequest forwardPayment) {
        return new HashMap<>();
    }

    private BigDecimal payedAmount(Map<String, String> responseMap) {
        if (!Strings.isNullOrEmpty(responseMap.get(AMOUNT_FIELD))) {
            return new BigDecimal(responseMap.get(AMOUNT_FIELD));
        }

        return null;
    }

    private String transactionId(Map<String, String> responseMap) {
        if (!Strings.isNullOrEmpty(responseMap.get(C026))) {
            return responseMap.get(C026);
        }

        return null;
    }

    private DateTime authorizationSibsDate(final Map<String, String> responseMap) {
        if (!responseMap.containsKey(A037)) {
            return null;
        }

        return DateTimeFormat.forPattern("YYYYMMddHHmmss").parseDateTime(responseMap.get(A037));
    }

    private boolean isResponseSuccess(final Map<String, String> responseMap) {
        if (responseMap.get(A038) == null) {
            return false;
        }

        try {
            return Integer.parseInt(responseMap.get(A038)) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isPaymentStatusSuccess(final Map<String, String> responseMap) {
        return isResponseSuccess(responseMap) && responseMap.get(A030) != null && M120.equals(responseMap.get(A030));
    }

    private boolean isAuthorizationSuccess(Map<String, String> responseMap) {
        return isResponseSuccess(responseMap) && responseMap.containsKey(A030) && M101.equals(responseMap.get(A030));
    }

    private boolean isPaymentSuccess(final Map<String, String> responseMap) {
        return isResponseSuccess(responseMap) && responseMap.containsKey(A030) && M102.equals(responseMap.get(A030));
    }

    private boolean isAuthenticationResponseMessage(Map<String, String> responseMap) {
        return responseMap.containsKey(A030) && AUTHENTICATION_RESPONSE_MESSAGE.equals(responseMap.get(A030));
    }

    private String responseCode(final Map<String, String> responseMap) {
        if (!responseMap.containsKey(A038)) {
            return null;
        }

        return responseMap.get(A038);
    }

    @Override
    public String getPaymentURL(ForwardPaymentRequest forwardPayment) {
        throw new RuntimeException("deprecated");
    }

    public String getReturnURL(ForwardPaymentRequest forwardPayment) {
        return String.format("%s/%s", TreasurySettings.getInstance().getForwardPaymentReturnDefaultURL(),
                forwardPayment.getExternalId());
    }

    private String json(final Object obj) {
        GsonBuilder builder = new GsonBuilder();
        return builder.create().toJson(obj);
    }

    @Override
    public String getLogosJspPage() {
        return "implementations/tpavirtual/logos.jsp";
    }

    @Override
    public String getWarningBeforeRedirectionJspPage() {
        return null;
    }

    @Override
    @Atomic
    public PostProcessPaymentStatusBean postProcessPayment(ForwardPaymentRequest forwardPayment, String justification,
            Optional<String> specificTransactionId) {
        throw new TreasuryDomainException("label.ManageForwardPayments.postProcessPayment.not.supported.yet");
    }

    @Override
    public int getMaximumLengthForAddressStreetFieldOne() {
        return 50;
    }

    @Override
    public int getMaximumLengthForAddressCity() {
        return 50;
    }

    @Override
    public int getMaximumLengthForPostalCode() {
        return 16;
    }

    /* SERVICES */

    public static final TPAVirtualImplementationPlatform create(FinantialInstitution finantialInstitution, String name) {
        throw new RuntimeException("deprecated");
    }

    @Override
    public ForwardPaymentRequest createForwardPaymentRequest(SettlementNoteBean bean,
            Function<ForwardPaymentRequest, String> successUrlFunction,
            Function<ForwardPaymentRequest, String> insuccessUrlFunction) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public List<? extends DigitalPlatformResultBean> getPaymentTransactionsReportListByMerchantId(String merchantTransationId) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public PostProcessPaymentStatusBean processForwardPayment(ForwardPaymentRequest forwardPayment) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public PostProcessPaymentStatusBean processForwardPaymentFromWebhook(PaymentRequestLog log, DigitalPlatformResultBean bean) {
        throw new RuntimeException("not implemented");
    }
}
