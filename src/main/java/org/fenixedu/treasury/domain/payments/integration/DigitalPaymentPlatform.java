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
package org.fenixedu.treasury.domain.payments.integration;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.onlinepaymentsgateway.api.DigitalPlatformResultBean;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentPlatformService;
import org.fenixedu.treasury.domain.paymentcodes.integration.ISibsPaymentCodePoolService;
import org.fenixedu.treasury.domain.payments.IMbwayPaymentPlatformService;
import org.fenixedu.treasury.domain.payments.PaymentRequest;
import org.fenixedu.treasury.domain.payments.PaymentRequestLog;
import org.fenixedu.treasury.domain.settings.TreasurySettings;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public abstract class DigitalPaymentPlatform extends DigitalPaymentPlatform_Base {

    public static final Comparator<DigitalPaymentPlatform> COMPARE_BY_NAME =
            (o1, o2) -> o1.getName().compareTo(o2.getName()) * 10 + o1.getExternalId().compareTo(o2.getExternalId());

    protected DigitalPaymentPlatform() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected void init(FinantialInstitution finantialInstitution, FinantialEntity finantialEntity, String name, boolean active) {
        setFinantialInstitution(finantialInstitution);
        setFinantialEntity(finantialEntity);
        setName(name);
        setActive(active);
    }

    protected void checkRules() {
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatform.domainRoot.required");
        }

        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatform.finantialInstitution.required");
        }

        if (getFinantialEntity() == null) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatform.finantialEntity.required");
        }

        if (StringUtils.isEmpty(getName())) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatform.name.required");
        }
    }

    public boolean isSibsPaymentCodeServiceSupported() {
        return getDigitalPaymentPlatformPaymentModesSet().stream()
                .anyMatch(m -> m.getPaymentMethod() == TreasurySettings.getInstance().getMbPaymentMethod());
    }

    public boolean isForwardPaymentServiceSupported() {
        return getDigitalPaymentPlatformPaymentModesSet().stream()
                .anyMatch(m -> m.getPaymentMethod() == TreasurySettings.getInstance().getCreditCardPaymentMethod());
    }

    public boolean isMbwayServiceSupported() {
        return getDigitalPaymentPlatformPaymentModesSet().stream()
                .anyMatch(m -> m.getPaymentMethod() == TreasurySettings.getInstance().getMbWayPaymentMethod());
    }

    public boolean isActive() {
        return getActive();
    }

    public boolean isActive(PaymentMethod paymentMethod) {
        Optional<DigitalPaymentPlatformPaymentMode> optional =
                getDigitalPaymentPlatformPaymentModesSet().stream().filter(p -> p.getPaymentMethod() == paymentMethod).findAny();

        return isActive() && optional.isPresent() && optional.get().isActive();
    }

    public ISibsPaymentCodePoolService castToSibsPaymentCodePoolService() {
        return (ISibsPaymentCodePoolService) this;
    }

    public IForwardPaymentPlatformService castToForwardPaymentPlatformService() {
        return (IForwardPaymentPlatformService) this;
    }

    public IMbwayPaymentPlatformService castToMbwayPaymentPlatformService() {
        return (IMbwayPaymentPlatformService) this;
    }

    public Set<? extends PaymentRequest> getAssociatedPaymentRequestsSet() {
        return super.getPaymentRequestsSet();
    }

    public Set<DigitalPaymentPlatformPaymentMode> getActiveDigitalPaymentModesSet() {
        return getDigitalPaymentPlatformPaymentModesSet().stream().filter(p -> p.isActive()).collect(Collectors.toSet());
    }

    public void delete() {
        if (!super.getPaymentRequestsSet().isEmpty()) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatform.cannot.delete.due.to.requests");
        }

        super.setDomainRoot(null);
        super.setFinantialInstitution(null);
        super.setFinantialEntity(null);

        while (!getDigitalPaymentPlatformPaymentModesSet().isEmpty()) {
            getDigitalPaymentPlatformPaymentModesSet().iterator().next().delete();
        }

        if (getSibsPaymentExpiryStrategy() != null) {
            getSibsPaymentExpiryStrategy().delete();
        }
    }

    @Atomic(mode = TxMode.WRITE)
    public PaymentRequestLog log(PaymentRequest paymentRequest, String operationCode, String statusCode, String statusMessage,
            String requestBody, String responseBody) {
        final PaymentRequestLog log = PaymentRequestLog.create(paymentRequest, operationCode,
                paymentRequest.getCurrentState().getCode(), paymentRequest.getCurrentState().getLocalizedName());

        log.setStatusCode(statusCode);
        log.setStatusMessage(statusMessage);

        if (!Strings.isNullOrEmpty(requestBody)) {
            log.saveRequest(requestBody);
        }

        if (!Strings.isNullOrEmpty(responseBody)) {
            log.saveResponse(responseBody);
        }

        return log;
    }

    public PaymentRequestLog logException(PaymentRequest paymentRequest, Exception e, String operationCode, String statusCode,
            String statusMessage, String requestBody, String responseBody) {
        PaymentRequestLog log = log(paymentRequest, operationCode, statusCode, statusMessage, requestBody, responseBody);

        log.logException(e);
        return log;
    }

    public void updateFinantialInstitutionInfoHeader(boolean overrideFinantialInstitutionInfoHeader,
            LocalizedString finantialInstitutionInfoHeader) {
        if (finantialInstitutionInfoHeader != null) {
            finantialInstitutionInfoHeader.forEach((locale, text) -> {
                // Avoid XSS vulnerability
                String startTag = "<script>";
                String endTag = "</script>";
                if (text.indexOf(startTag) > 0 && text.indexOf(endTag) > 0) {
                    String textToRemove = text.substring(text.indexOf(startTag) + startTag.length(), text.indexOf(endTag));
                    text = text.replace(textToRemove, "");
                }

                text = text.replace(startTag, "").replace(endTag, "");

                finantialInstitutionInfoHeader.getOrDefault(locale, text);
            });
        }

        super.setOverrideFinantialInstitutionInfoHeader(overrideFinantialInstitutionInfoHeader);
        super.setFinantialInstitutionInfoHeader(finantialInstitutionInfoHeader);
    }

    public abstract List<? extends DigitalPlatformResultBean> getPaymentTransactionsReportListByMerchantId(
            String merchantTransationId);


    // ANIL 2025-05-08 (#qubIT-Fenix-6886)
    public abstract int getMaximumLengthForAddressStreetFieldOne();

    // ANIL 2025-05-08 (#qubIT-Fenix-6886)
    public abstract int getMaximumLengthForAddressCity();

    // ANIL 2025-05-08 (#qubIT-Fenix-6886)
    public abstract int getMaximumLengthForPostalCode();

    // @formatter:off
    /*
     * --------
     * SERVICES
     * --------
     */
    // @formatter:on

    public static Stream<? extends DigitalPaymentPlatform> findAll() {
        return FenixFramework.getDomainRoot().getDigitalPaymentPlatformsSet().stream();
    }

    public static Stream<? extends DigitalPaymentPlatform> findForSibsPaymentCodeService() {
        return findAll().filter(d -> d.isSibsPaymentCodeServiceSupported());
    }

    public static Stream<? extends DigitalPaymentPlatform> find(FinantialInstitution finantialInstitution) {
        return finantialInstitution.getDigitalPaymentPlatformsSet().stream();
    }

    public static Stream<? extends DigitalPaymentPlatform> findForSibsPaymentCodeService(
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(d -> d.isSibsPaymentCodeServiceSupported());
    }

    public static Stream<? extends DigitalPaymentPlatform> findForSibsPaymentCodeServiceByActive(
            FinantialInstitution finantialInstitution, boolean active) {
        PaymentMethod mbPaymentMethod = TreasurySettings.getInstance().getMbPaymentMethod();
        return find(finantialInstitution).filter(d -> d.isSibsPaymentCodeServiceSupported())
                .filter(d -> active == d.isActive(mbPaymentMethod));
    }

    public static Stream<? extends DigitalPaymentPlatform> findForForwardPaymentService(
            FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(d -> d.isForwardPaymentServiceSupported());
    }

    public static Stream<? extends DigitalPaymentPlatform> findForForwardPaymentService(FinantialInstitution finantialInstitution,
            boolean active) {
        PaymentMethod creditCardPaymentMethod = TreasurySettings.getInstance().getCreditCardPaymentMethod();
        return find(finantialInstitution).filter(d -> d.isForwardPaymentServiceSupported())
                .filter(d -> active == d.isActive(creditCardPaymentMethod));
    }

    public static Stream<? extends DigitalPaymentPlatform> find(FinantialInstitution finantialInstitution,
            PaymentMethod paymentMethod, boolean active) {
        return find(finantialInstitution).filter(d -> active == d.isActive(paymentMethod));
    }
}
