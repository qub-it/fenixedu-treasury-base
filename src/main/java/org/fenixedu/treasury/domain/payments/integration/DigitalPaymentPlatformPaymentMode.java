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
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import pt.ist.fenixframework.FenixFramework;

public class DigitalPaymentPlatformPaymentMode extends DigitalPaymentPlatformPaymentMode_Base {

    public final static Comparator<DigitalPaymentPlatformPaymentMode> COMPARE_BY_ORDER =
            Comparator.comparing(DigitalPaymentPlatformPaymentMode::getPaymentModeOrder).thenComparing(DigitalPaymentPlatformPaymentMode::getExternalId);

    public DigitalPaymentPlatformPaymentMode() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setActiveForFrontend(false);
    }

    public DigitalPaymentPlatformPaymentMode(DigitalPaymentPlatform platform, PaymentMethod paymentMethod) {
        this();

        List<DigitalPaymentPlatformPaymentMode> orderedPaymentModesList =
                platform.getOrderedPaymentModesList();

        setDigitalPaymentPlatform(platform);
        setPaymentMethod(paymentMethod);
        setActive(true);

        setPaymentModeOrder(!orderedPaymentModesList.isEmpty() ? orderedPaymentModesList.get(orderedPaymentModesList.size() - 1)
                .getPaymentModeOrder() + 1 : 1);

        getDigitalPaymentPlatform().orderPaymentModes();

        checkRules();
    }

    private void checkRules() {
        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatformPaymentMode.domainRoot.required");
        }

        if (getPaymentMethod() == null) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatformPaymentMode.paymentMethod.required");
        }

        //In the future this validation can be removed if is intended to use the same paymentMethod for the various paymentModes available by the platform. eg: creditCard
        if (find(getDigitalPaymentPlatform(), getPaymentMethod()).count() > 1) {
            throw new TreasuryDomainException("error.DigitalPaymentPlatformPaymentMode.duplicated");
        }
    }

    public boolean isDigitalPaymentPlatformAndPaymentModeActive() {
        return isDigitalPaymentPlatformAndPaymentModeActive(false);
    }

    private boolean isDigitalPaymentPlatformAndPaymentModeActive(boolean isForFrontend) {
        return getDigitalPaymentPlatform().isActive() && isActive() && (!isForFrontend || Boolean.TRUE.equals(
                getActiveForFrontend()));
    }

    public boolean isActive() {
        return getActive();
    }

    public void activate() {
        setActive(true);
    }

    public void deactivate() {
        setActive(false);
    }

    public LocalizedString getDigitalPaymentPlatformPaymentMethodDesignation() {
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        LocalizedString result = new LocalizedString();
        for (Locale locale : services.availableLocales()) {
            result.with(locale, String.format("[%s] %s", getDigitalPaymentPlatform().getName(),
                    getPaymentMethod().getName().getContent(locale)));
        }

        return result;
    }

    public void delete() {
        super.setDomainRoot(null);
        super.setDigitalPaymentPlatform(null);
        super.setPaymentMethod(null);

        super.deleteDomainObject();
    }

    public boolean isFirstInPaymentOrder() {
        int index = getDigitalPaymentPlatform().getOrderedPaymentModesList().indexOf(this);

        return index == 0;
    }

    public boolean isLastInPaymentOrder() {
        int index = getDigitalPaymentPlatform().getOrderedPaymentModesList().indexOf(this);

        return index == (getDigitalPaymentPlatform().getOrderedPaymentModesList().size() - 1);
    }

    public void orderUp() {
        if(isFirstInPaymentOrder()) {
            // Already the first
            return;
        }

        List<DigitalPaymentPlatformPaymentMode> orderedPaymentModesList =
                getDigitalPaymentPlatform().getOrderedPaymentModesList();

        int index = orderedPaymentModesList.indexOf(this);

        DigitalPaymentPlatformPaymentMode previous = orderedPaymentModesList.get(index - 1);

        int temp = previous.getPaymentModeOrder();
        previous.setPaymentModeOrder(getPaymentModeOrder());

        setPaymentModeOrder(temp);

        getDigitalPaymentPlatform().orderPaymentModes();
    }

    public void orderDown() {
        if(isLastInPaymentOrder()) {
            // already the last
            return;
        }

        List<DigitalPaymentPlatformPaymentMode> orderedPaymentModesList =
                getDigitalPaymentPlatform().getOrderedPaymentModesList();

        int index = orderedPaymentModesList.indexOf(this);

        DigitalPaymentPlatformPaymentMode next = orderedPaymentModesList.get(index + 1);

        int temp = next.getPaymentModeOrder();
        next.setPaymentModeOrder(getPaymentModeOrder());
        setPaymentModeOrder(temp);

        getDigitalPaymentPlatform().orderPaymentModes();
    }

    /*
     * SERVICES
     */

    public static Stream<DigitalPaymentPlatformPaymentMode> findAll() {
        return FenixFramework.getDomainRoot().getDigitalPaymentPlatformPaymentModesSet().stream();
    }

    public static Stream<DigitalPaymentPlatformPaymentMode> findDigitalPaymentPlatformAndPaymentModeActive(
            FinantialInstitution finantialInstitution) {
        return findAll().filter(pm -> pm.getDigitalPaymentPlatform().getFinantialInstitution() == finantialInstitution)
                .filter(pm -> pm.isDigitalPaymentPlatformAndPaymentModeActive());
    }

    public static Stream<DigitalPaymentPlatformPaymentMode> findDigitalPaymentPlatformAndPaymentModeActive(
            FinantialInstitution finantialInstitution, boolean isForFrontend) {
        return findAll().filter(pm -> pm.getDigitalPaymentPlatform().getFinantialInstitution() == finantialInstitution)
                .filter(pm -> pm.isDigitalPaymentPlatformAndPaymentModeActive(isForFrontend));
    }

    public static Stream<DigitalPaymentPlatformPaymentMode> findDigitalPaymentPlatformAndPaymentModeActive() {
        return findAll().filter(pm -> pm.isDigitalPaymentPlatformAndPaymentModeActive());
    }

    public static Stream<DigitalPaymentPlatformPaymentMode> findDigitalPaymentPlatformAndPaymentModeActive(
            boolean isForFrontend) {
        return findAll().filter(pm -> pm.isDigitalPaymentPlatformAndPaymentModeActive(isForFrontend));
    }

    public static Stream<DigitalPaymentPlatformPaymentMode> find(DigitalPaymentPlatform platform, PaymentMethod paymentMethod) {
        return platform.getDigitalPaymentPlatformPaymentModesSet().stream().filter(p -> p.getPaymentMethod() == paymentMethod);
    }

    public static Optional<DigitalPaymentPlatformPaymentMode> findUnique(DigitalPaymentPlatform platform,
            PaymentMethod paymentMethod) {
        return find(platform, paymentMethod).findAny();
    }

    public static DigitalPaymentPlatformPaymentMode create(DigitalPaymentPlatform platform, PaymentMethod paymentMethod) {
        return new DigitalPaymentPlatformPaymentMode(platform, paymentMethod);
    }

}
