package org.fenixedu.treasury.domain.settings;

import java.util.Optional;
import java.util.Set;

import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.payments.integration.DigitalPaymentPlatformPaymentMode;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TreasurySettings extends TreasurySettings_Base {

    protected TreasurySettings() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    @Atomic
    public void edit(final Currency defaultCurrency, Product interestProduct, Product advancePaymentProduct) {
        setDefaultCurrency(defaultCurrency);
        setInterestProduct(interestProduct);
        setAdvancePaymentProduct(advancePaymentProduct);
    }

    public boolean isRestrictPaymentMixingLegacyInvoices() {
        return getRestrictPaymentMixingLegacyInvoices();
    }

    @Atomic
    public void restrictPaymentMixingLegacyInvoices() {
        setRestrictPaymentMixingLegacyInvoices(true);
    }

    @Atomic
    public void allowPaymentMixingLegacyInvoices() {
        setRestrictPaymentMixingLegacyInvoices(false);
    }

    protected static Optional<TreasurySettings> findUnique() {
        return FenixFramework.getDomainRoot().getTreasurySettingsSet().stream().findFirst();
    }

    @Atomic
    public synchronized static TreasurySettings getInstance() {
        if (!findUnique().isPresent()) {
            TreasurySettings settings = new TreasurySettings();
        }

        return findUnique().get();
    }

    @Override
    public void setCreditCardPaymentMethod(PaymentMethod creditCardPaymentMethod) {
        if(getCreditCardPaymentMethod() != null) {
            Set<DigitalPaymentPlatformPaymentMode> platforms =
                    getCreditCardPaymentMethod().getDigitalPaymentPlatformPaymentModesSet();
            for (DigitalPaymentPlatformPaymentMode platform : platforms) {
                platform.setPaymentMethod(creditCardPaymentMethod);
            }
        }
        
        super.setCreditCardPaymentMethod(creditCardPaymentMethod);

    }

    @Override
    public void setMbPaymentMethod(PaymentMethod mbPaymentMethod) {
        if(getMbPaymentMethod() != null) {
            Set<DigitalPaymentPlatformPaymentMode> platforms = getMbPaymentMethod().getDigitalPaymentPlatformPaymentModesSet();
            for (DigitalPaymentPlatformPaymentMode platform : platforms) {
                platform.setPaymentMethod(mbPaymentMethod);
            }
        }
        
        super.setMbPaymentMethod(mbPaymentMethod);
    }

    @Override
    public void setMbWayPaymentMethod(PaymentMethod mbWayPaymentMethod) {
        if(getMbWayPaymentMethod() != null) {
            Set<DigitalPaymentPlatformPaymentMode> platforms = getMbWayPaymentMethod().getDigitalPaymentPlatformPaymentModesSet();
            for (DigitalPaymentPlatformPaymentMode platform : platforms) {
                platform.setPaymentMethod(mbWayPaymentMethod);
            }
        }
        
        super.setMbWayPaymentMethod(mbWayPaymentMethod);
    }
}
