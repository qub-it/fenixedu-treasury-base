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
package org.fenixedu.treasury.domain.forwardpayments;

import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentController;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentImplementation;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentConfigurationBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class ForwardPaymentConfiguration extends ForwardPaymentConfiguration_Base {

    private static final Logger logger = LoggerFactory.getLogger(ForwardPaymentConfiguration.class);

    public static Comparator<ForwardPaymentConfiguration> COMPARATOR_BY_FINANTIAL_INSTITUTION_AND_NAME = (o1, o2) -> {
        int c = FinantialInstitution.COMPARATOR_BY_NAME.compare(o1.getFinantialInstitution(), o2.getFinantialInstitution());
        if (c != 0) {
            return c;
        }

        c = o1.getName().compareTo(o2.getName());
        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    public static Comparator<ForwardPaymentConfiguration> COMPARATOR_BY_NAME = (o1, o2) -> {
        int c = o1.getName().compareTo(o2.getName());
        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    private ForwardPaymentConfiguration() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    private ForwardPaymentConfiguration(final FinantialInstitution finantialInstitution,
            final ForwardPaymentConfigurationBean bean) {

        this();

        setFinantialInstitution(finantialInstitution);

        setActive(bean.isActive());
        setName(bean.getName());
        setPaymentURL(bean.getPaymentURL());
        setReturnURL(bean.getReturnURL());
        setVirtualTPAMOXXURL(bean.getVirtualTPAMOXXURL());
        setVirtualTPAMerchantId(bean.getVirtualTPAMerchantId());
        setVirtualTPAId(bean.getVirtualTPAId());
        setVirtualTPAKeyStoreName(bean.getVirtualTPAKeyStoreName());
        setVirtualTPACertificateAlias(bean.getVirtualTPACertificateAlias());
        setVirtualTPACertificatePassword(bean.getVirtualTPACertificatePassword());
        setImplementation(bean.getImplementation());
        setPaylineMerchantId(bean.getPaylineMerchantId());
        setPaylineMerchantAccessKey(bean.getPaylineMerchantAccessKey());
        setPaylineContractNumber(bean.getPaylineContractNumber());

        setSeries(bean.getSeries());
        setPaymentMethod(bean.getPaymentMethod());

        setReimbursementPolicyJspFile(bean.getReimbursementPolicyJspFile());
        setPrivacyPolicyJspFile(bean.getPrivacyPolicyJspFile());
        setLogosJspPageFile(bean.getLogosJspPageFile());

        checkRules();
    }

    private void checkRules() {
        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.finantialInstitution.required");
        }

        if (findActive(getFinantialInstitution()).count() > 1) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.finantialInstitution.only.one.active.allowed");
        }

        if (!StringUtils.isEmpty(getImplementation())) {
            try {
                implementation();
            } catch (Exception e) {
                throw new TreasuryDomainException("error.ForwardPaymentConfiguration.unknown.implementation");
            }
        }
    }

    @Atomic
    public void edit(final ForwardPaymentConfigurationBean bean) {

        setActive(bean.isActive());
        setName(bean.getName());
        setPaymentURL(bean.getPaymentURL());
        setReturnURL(bean.getReturnURL());
        setVirtualTPAMOXXURL(bean.getVirtualTPAMOXXURL());
        setVirtualTPAMerchantId(bean.getVirtualTPAMerchantId());
        setVirtualTPAId(bean.getVirtualTPAId());
        setVirtualTPAKeyStoreName(bean.getVirtualTPAKeyStoreName());
        setVirtualTPACertificateAlias(bean.getVirtualTPACertificateAlias());
        setVirtualTPACertificatePassword(bean.getVirtualTPACertificatePassword());
        setImplementation(bean.getImplementation());
        setPaylineMerchantId(bean.getPaylineMerchantId());
        setPaylineMerchantAccessKey(bean.getPaylineMerchantAccessKey());
        setPaylineContractNumber(bean.getPaylineContractNumber());

        setSeries(bean.getSeries());
        setPaymentMethod(bean.getPaymentMethod());

        setReimbursementPolicyJspFile(bean.getReimbursementPolicyJspFile());
        setPrivacyPolicyJspFile(bean.getPrivacyPolicyJspFile());
        setLogosJspPageFile(bean.getLogosJspPageFile());

        checkRules();
    }

    @Atomic
    public void saveVirtualTPACertificate(final String filename, final byte[] contents) {
        if (getVirtualTPACertificate() != null) {
            ForwardPaymentConfigurationFile virtualTPACertificate = getVirtualTPACertificate();
            setVirtualTPACertificate(null);
            virtualTPACertificate.delete();
        }

        ForwardPaymentConfigurationFile file = ForwardPaymentConfigurationFile.create(filename, contents);
        setVirtualTPACertificate(file);
    }

    public void delete() {
        if (!getForwardPaymentsSet().isEmpty()) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.cannot.delete");
        }

        if (getSibsOnlinePaymentsGateway() != null) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.remove.sibs.oppwa.configuration.first");
        }

        this.setDomainRoot(null);
        this.setFinantialInstitution(null);
        this.setPaymentMethod(null);
        this.setSeries(null);
        if (this.getVirtualTPACertificate() != null) {
            this.getVirtualTPACertificate().delete();
        }

        super.deleteDomainObject();
    }

    public boolean isActive() {
        return getActive();
    }

    public boolean isLogosPageDefined() {
        return !Strings.isNullOrEmpty(implementation().getLogosJspPage(this));
    }

    public boolean isReimbursementPolicyTextDefined() {
        return !Strings.isNullOrEmpty(getReimbursementPolicyJspFile());
    }

    public boolean isPrivacyPolicyTextDefined() {
        return !Strings.isNullOrEmpty(getPrivacyPolicyJspFile());
    }

    public String formattedAmount(final ForwardPayment forwardPayment) {
        return implementation().getFormattedAmount(forwardPayment);
    }

    public IForwardPaymentController getForwardPaymentController(final ForwardPayment forwardPayment) {
        return implementation().getForwardPaymentController(forwardPayment);
    }

    public IForwardPaymentImplementation implementation() {
        try {
            return (IForwardPaymentImplementation) Class.forName(getImplementation()).newInstance();
        } catch (final InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getImplementationCode() {
        try {
            return implementation().getImplementationCode();
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage(), e);
        }

        return null;
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on

    @Atomic
    public static ForwardPaymentConfiguration create(final FinantialInstitution finantialInstitution,
            final ForwardPaymentConfigurationBean bean) {
        return new ForwardPaymentConfiguration(finantialInstitution, bean);
    }

    public static Stream<ForwardPaymentConfiguration> findAll() {
        return FenixFramework.getDomainRoot().getForwardPaymentConfigurationsSet().stream();
    }

    public static Stream<ForwardPaymentConfiguration> find(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getForwardPaymentConfigurationsSet().stream();
    }

    public static Stream<ForwardPaymentConfiguration> findActive(final FinantialInstitution finantialInstitution) {
        return find(finantialInstitution).filter(e -> e.isActive());
    }

    public static Optional<ForwardPaymentConfiguration> findUniqueActive(final FinantialInstitution finantialInstitution) {
        return findActive(finantialInstitution).findFirst();
    }

    public static boolean isActive(final FinantialInstitution finantialInstitution) {
        if (!findUniqueActive(finantialInstitution).isPresent()) {
            return false;
        }

        return findUniqueActive(finantialInstitution).get().isActive();
    }
}
