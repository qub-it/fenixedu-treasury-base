package org.fenixedu.treasury.domain.paymentcodes.pool;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.payments.paymentscodegenerator.IPaymentCodeGenerator;

import com.google.common.base.Strings;

import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class PaymentCodeGeneratorInstance extends PaymentCodeGeneratorInstance_Base {

    public PaymentCodeGeneratorInstance() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    public PaymentCodeGeneratorInstance(final String name, final String implementationClassName) {
        this();
        
        setName(name);
        setImplementationClassName(implementationClassName);
        
        checkRules();
    }

    private void checkRules() {
        
        if(getDomainRoot() == null) {
            throw new TreasuryDomainException("error.PaymentCodeGeneratorInstance.domainRoot.required");
        }
        
        if(Strings.isNullOrEmpty(getName())) {
            throw new TreasuryDomainException("error.PaymentCodeGeneratorInstance.name.required");
        }
        
        if(Strings.isNullOrEmpty(getImplementationClassName())) {
            throw new TreasuryDomainException("error.PaymentCodeGeneratorInstance.implementationClassName.required");
        }
        
        if(findByImplementationClassName(getImplementationClassName()).count() > 1) {
            throw new TreasuryDomainException("error.PaymentCodeGeneratorInstance.implementationClassName.already.created");
        }
        
    }

    public IPaymentCodeGenerator getPaymentCodeGenerator(final PaymentCodePool paymentCodePool) {
        try {
            return (IPaymentCodeGenerator) Class.forName(getImplementationClassName()).getConstructor(PaymentCodePool.class)
                    .newInstance(paymentCodePool);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | NoSuchMethodException | SecurityException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:on
    
    public static PaymentCodeGeneratorInstance create(final String name, final String implementationClassName) {
        return new PaymentCodeGeneratorInstance(name, implementationClassName);
    }
    
    public static Stream<PaymentCodeGeneratorInstance> findAll() {
        return FenixFramework.getDomainRoot().getPaymentCodeGeneratorInstancesSet().stream();
    }
    
    public static Stream<PaymentCodeGeneratorInstance> findByImplementationClassName(final String implementationClassName) {
        return findAll().filter(e -> e.getImplementationClassName().toLowerCase().equals(implementationClassName.toLowerCase()));
    }
    
    public static Optional<PaymentCodeGeneratorInstance> findUniqueByImplementationClassName(final String implementationClassName) {
        return findByImplementationClassName(implementationClassName).findFirst();
    }

}
