package org.fenixedu.treasury.services.integration;

import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.services.integration.erp.ISaftExporterConfiguration;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Partial;

public class FenixEDUTreasuryPlatformDependentServices implements ITreasuryPlatformDependentServices {

    private static final int WAIT_TRANSACTION_TO_FINISH_MS = 500;

    /* Bundles */

    @Override
    public boolean isDynamicApplicationMessageDefined(String key) {
        throw new RuntimeException("not supported");
    }

    @Override
    public LocalizedString getDynamicApplicationMessage(String key) {
        throw new RuntimeException("not supported");
    }

    // Remove
    @Deprecated
    public static <T> String readVersioningCreatorUsername(T obj) {
        return getVersioningCreatorUsername(obj);
    }

    // MAINTAIN THIS METHOD
    public static <T> String getVersioningCreatorUsername(T obj) {
        try {
            String username = (String) PropertyUtils.getProperty(obj, "versioningCreator");

            return username;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Remove
    @Deprecated
    public static <T> DateTime readVersioningCreationDate(T obj) {
        return getVersioningCreationDate(obj);
    }

    // MAINTAIN THIS METHOD
    public static <T> DateTime getVersioningCreationDate(T obj) {
        try {
            DateTime creationDate = (DateTime) PropertyUtils.getProperty(obj, "versioningCreationDate");

            return creationDate;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Remove
    @Deprecated
    public static <T> String readVersioningUpdatorUsername(T obj) {
        return getVersioningUpdatorUsername(obj);
    }

    // MAINTAIN THIS METHOD
    public static <T> String getVersioningUpdatorUsername(T obj) {
        try {
            Object versioningUpdatedBy = PropertyUtils.getProperty(obj, "versioningUpdatedBy");

            if (versioningUpdatedBy == null) {
                return null;
            }

            return (String) PropertyUtils.getProperty(versioningUpdatedBy, "username");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Remove
    @Deprecated
    public static <T> DateTime readVersioningUpdateDate(T obj) {
        return getVersioningUpdateDate(obj);
    }

    // MAINTAIN THIS METHOD
    public static <T> DateTime getVersioningUpdateDate(T obj) {
        try {
            Object versioningUpdateDate = PropertyUtils.getProperty(obj, "versioningUpdateDate");

            if (versioningUpdateDate == null) {
                return null;
            }

            return (DateTime) PropertyUtils.getProperty(versioningUpdateDate, "date");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /* Domain entities events */

    @Override
    public ISaftExporterConfiguration getSaftExporterConfiguration(ERPConfiguration configuration) {
        return null;
    }

    @Override
    public Set<Partial> getHolidays() {
        throw new RuntimeException("not supported");
    }

    @Override
    public void certifyDocument(FinantialDocument finantialDocument) {
        // Do nothing
    }

    @Override
    public void annulCertifiedDocument(FinantialDocument finantialDocument) {
        // Do nothing
    }

    @Override
    public boolean hasCertifiedDocument(FinantialDocument finantialDocument) {
        return false;
    }

    @Override
    public boolean isProductCertified(Product product) {
        return false;
    }

    @Override
    public String getCertifiedDocumentNumber(FinantialDocument finantialDocument) {
        return null;
    }

    @Override
    public LocalDate getCertifiedDocumentDate(FinantialDocument finantialDocument) {
        return null;
    }

}
