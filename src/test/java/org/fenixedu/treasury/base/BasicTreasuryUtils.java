package org.fenixedu.treasury.base;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryBootstrapper;
import org.fenixedu.treasury.util.TreasuryConstants;

import pt.ist.esw.advice.pt.ist.fenixframework.AtomicInstance;
import pt.ist.fenixframework.Atomic.TxMode;
import pt.ist.fenixframework.FenixFramework;

public class BasicTreasuryUtils {

    public static void startup(Callable<?> startup) {
        try {
            FenixFramework.getTransactionManager().withTransaction(() -> {
                TreasuryPlataformDependentServicesFactory.registerImplementation(new TreasuryPlatformDependentServicesForTests());
                TreasuryBootstrapper.bootstrap("teste", "teste", "PT");
                startup.call();
                return null;
            }, new AtomicInstance(TxMode.WRITE, true));
        } catch (Exception e) {
            throw new UndeclaredThrowableException(e);
        }

    }

    public static LocalizedString ls(String string) {
        return new LocalizedString(TreasuryConstants.DEFAULT_LANGUAGE, string);
    }

}
