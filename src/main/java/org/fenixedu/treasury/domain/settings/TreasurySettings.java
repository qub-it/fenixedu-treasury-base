package org.fenixedu.treasury.domain.settings;

import java.util.Optional;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Product;

import pt.ist.fenixframework.Atomic;;

public class TreasurySettings extends TreasurySettings_Base {

    protected TreasurySettings() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    @Atomic
    public void edit(final Currency defaultCurrency, Product interestProduct, Product advancePaymentProduct) {
        setDefaultCurrency(defaultCurrency);
        setInterestProduct(interestProduct);
        setAdvancePaymentProduct(advancePaymentProduct);
    }

    protected static Optional<TreasurySettings> findUnique() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getTreasurySettingsSet().stream().findFirst();
    }

    @Atomic
    public synchronized static TreasurySettings getInstance() {
        if (!findUnique().isPresent()) {
            TreasurySettings settings = new TreasurySettings();
        }

        return findUnique().get();
    }
}
