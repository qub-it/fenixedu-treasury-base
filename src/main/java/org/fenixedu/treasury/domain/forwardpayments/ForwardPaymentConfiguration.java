package org.fenixedu.treasury.domain.forwardpayments;

import java.util.Optional;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.implementations.IForwardPaymentImplementation;
import org.fenixedu.treasury.dto.forwardpayments.ForwardPaymentConfigurationBean;

import pt.ist.fenixframework.Atomic;;

public class ForwardPaymentConfiguration extends ForwardPaymentConfiguration_Base {

    private ForwardPaymentConfiguration() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
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

        checkRules();
    }

    private void checkRules() {
        if (getFinantialInstitution() == null) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.finantialInstitution.required");
        }

        if (getFinantialInstitution().getForwardPaymentConfigurationsSet().size() > 1) {
            throw new TreasuryDomainException("error.ForwardPaymentConfiguration.finantialInstitution.only.one.allowed");
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

        checkRules();
    }
    
    @Atomic
    public void saveVirtualTPACertificate(final String filename, final byte[] contents) {
        if(getVirtualTPACertificate() != null) {
            ForwardPaymentConfigurationFile virtualTPACertificate = getVirtualTPACertificate();
            setVirtualTPACertificate(null);
            virtualTPACertificate.delete();
        }
        
        setVirtualTPACertificate(ForwardPaymentConfigurationFile.create(filename, contents));
    }

    public boolean isActive() {
        return getActive();
    }

    public String formattedAmount(final ForwardPayment forwardPayment) {
        return implementation().getFormattedAmount(forwardPayment);
    }

    public IForwardPaymentImplementation implementation() {
        try {
            return (IForwardPaymentImplementation) Class.forName(getImplementation()).newInstance();
        } catch (final InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Atomic
    public static ForwardPaymentConfiguration create(final FinantialInstitution finantialInstitution,
            final ForwardPaymentConfigurationBean bean) {
        return new ForwardPaymentConfiguration(finantialInstitution, bean);
    }
    
    public static Optional<ForwardPaymentConfiguration> find(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getForwardPaymentConfigurationsSet().stream().findFirst();
    }

    public static boolean isActive(final FinantialInstitution finantialInstitution) {
        if(!find(finantialInstitution).isPresent()) {
            return false;
        }
        
        return find(finantialInstitution).get().isActive();
    }

}
