package org.fenixedu.treasury.services.integration;

import com.google.common.base.Strings;
import com.qubit.solution.fenixedu.bennu.webservices.domain.webservice.WebServiceAuthenticationLevel;
import com.qubit.solution.fenixedu.bennu.webservices.domain.webservice.WebServiceClientConfiguration;
import com.qubit.solution.fenixedu.bennu.webservices.domain.webservice.WebServiceConfiguration;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.bennu.core.security.Authenticate;
import org.fenixedu.bennu.core.signals.DomainObjectEvent;
import org.fenixedu.bennu.core.signals.Signal;
import org.fenixedu.bennu.core.util.CoreConfiguration;
import org.fenixedu.bennu.io.domain.GenericFile;
import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.bennu.scheduler.TaskRunner;
import org.fenixedu.bennu.scheduler.domain.SchedulerSystem;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.*;
import org.fenixedu.treasury.domain.Currency;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.forwardpayments.ForwardPaymentRequest;
import org.fenixedu.treasury.domain.forwardpayments.implementations.PaylineWebServiceClient;
import org.fenixedu.treasury.domain.forwardpayments.payline.PaylineConfiguration;
import org.fenixedu.treasury.domain.forwardpayments.payline.PaylineWebServiceResponse;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.services.integration.erp.IERPExporter;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.fenixedu.treasury.services.integration.erp.ISaftExporterConfiguration;
import org.fenixedu.treasury.services.integration.erp.tasks.ERPExportSingleDocumentsTask;
import org.fenixedu.treasury.services.integration.forwardpayments.payline.*;
import org.fenixedu.treasury.util.TreasuryConstants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import pt.ist.fenixWebFramework.servlets.filters.contentRewrite.GenericChecksumRewriter;
import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.DomainObject;

import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class FenixEDUTreasuryPlatformDependentServices implements ITreasuryPlatformDependentServices {

    private static final int WAIT_TRANSACTION_TO_FINISH_MS = 500;

    /* File */

    @Override
    public byte[] getFileContent(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getContent();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getFileSize(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getSize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DateTime getFileCreationDate(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getCreationDate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFilename(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getFilename();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getFileStream(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getStream();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFileContentType(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            return file.getContentType();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createFile(final IGenericFile genericFile, final String fileName, final String contentType,
            final byte[] content) {
        try {
            GenericFile file = TreasuryFile.create(fileName, contentType, content);

            PropertyUtils.setProperty(genericFile, "treasuryFile", file);
            genericFile.setFileId(file.getExternalId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public void deleteFile(final IGenericFile genericFile) {
        try {
            GenericFile file = (GenericFile) PropertyUtils.getProperty(genericFile, "treasuryFile");

            file.delete();
            PropertyUtils.setProperty(genericFile, "treasuryFile", null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    /* Bundles */

    @Override
    public boolean isDynamicApplicationMessageDefined(String key) {
        throw new RuntimeException("not supported");
    }

    @Override
    public LocalizedString getDynamicApplicationMessage(String key){
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

    @Override
    public byte[] getFileContent(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileContent(String): not supported");
    }

    @Override
    public long getFileSize(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileSize(String): not supported");
    }

    @Override
    public String getFilename(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFilename(String): not supported");
    }

    @Override
    public InputStream getFileStream(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileStream(String): not supported");
    }

    @Override
    public DateTime getFileCreationDate(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileCreationDate(String): not supported");
    }

    @Override
    public String getFileContentType(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.getFileContentType(String): not supported");
    }

    @Override
    public String createFile(String fileName, String contentType, byte[] content) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.createFile(): not supported");
    }

    @Override
    public void deleteFile(String fileId) {
        throw new RuntimeException("FenixEDUTreasuryPlatformDependentServices.deleteFile(String): not supported");
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
