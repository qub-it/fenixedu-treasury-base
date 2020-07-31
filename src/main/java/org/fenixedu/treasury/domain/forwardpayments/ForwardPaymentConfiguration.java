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
