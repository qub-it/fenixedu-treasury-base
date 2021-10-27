package org.fenixedu.treasury.base;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.AdhocCustomer;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.Customer;
import org.fenixedu.treasury.domain.CustomerType;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.FiscalCountryRegion;
import org.fenixedu.treasury.domain.debt.DebtAccount;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
//import org.fenixedu.treasury.util.TreasuryBootstrapUtil;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.DomainRoot;
import pt.ist.standards.geographic.Country;
import pt.ist.standards.geographic.District;
import pt.ist.standards.geographic.Municipality;
import pt.ist.standards.geographic.Planet;

public class BasicTreasuryUtils {

    public static void startup(Callable<?> startup) {
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                TreasuryPlataformDependentServicesFactory.registerImplementation(new TreasuryPlatformDependentServicesForTests());
//                TreasuryBootstrapUtil.InitializeDomain();
                FinantialInstitution finantialInstitution = createFinantialInstitution();
                createFinantialEntity(finantialInstitution);
                startup.call();
                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }

    }

    public static FinantialInstitution createFinantialInstitution() {
        Country country = new Country(Planet.getEarth(), "portugal", "pt", "ptr", "1");
        District district = new District(country, "lisboa", "lisboa");
        return FinantialInstitution.create(FiscalCountryRegion.findByRegionCode("PT"), Currency.findByCode("EUR"),
                "FinantialInstitution", "123456789", "companyId", "Finantial Institution", "company name", "address", country,
                district, new Municipality(district, "lisboa", "lisboa"), "", "");
    }

    public static FinantialEntity createFinantialEntity(FinantialInstitution finantialInstitution) {
        return FinantialEntity.create(finantialInstitution, "FINANTIAL_ENTITY", ls("Entidade Financeira"));
    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }

}
