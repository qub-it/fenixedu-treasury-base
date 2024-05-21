package org.fenixedu.treasury.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.accesscontrol.spi.TreasuryUIAccessControlExtension;

@WebListener
public class FenixeduTreasuryUiInitializer implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        TreasuryAccessControlAPI.registerExtension(new TreasuryUIAccessControlExtension());
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
    }
}