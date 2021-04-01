package org.fenixedu.treasury.services.integration;

import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.joda.time.DateTime;

public interface ITreasuryPlatformDependentServices {

	void scheduleSingleDocument(final FinantialDocument finantialDocument);

	IERPExternalService getERPExternalServiceImplementation(final ERPConfiguration erpConfiguration);

	/* File */

	byte[] getFileContent(IGenericFile genericFile);

	long getFileSize(IGenericFile genericFile);

	String getFilename(IGenericFile genericFile);

	InputStream getFileStream(IGenericFile genericFile);

	DateTime getFileCreationDate(IGenericFile genericFile);

	String getFileContentType(IGenericFile iGenericFile);

	void createFile(final IGenericFile genericFile, final String fileName, final String contentType,
			final byte[] content);

	void deleteFile(final IGenericFile genericFile);
	
    /* File */

	byte[] getFileContent(String fileId);

    long getFileSize(String fileId);

    String getFilename(String fileId);

    InputStream getFileStream(String fileId);

    DateTime getFileCreationDate(String fileId);

    String getFileContentType(String fileId);

    String createFile(final String fileName, final String contentType,
            final byte[] content);

    void deleteFile(String fileId);
	
	/* User */

	String getLoggedUsername();

	/* Locales */

	// TODO: provide the default locale of the platform
	Locale defaultLocale();
	
	Set<Locale> availableLocales();

	/* Bundles */

	String bundle(final String bundleName, final String key, final String... args);

	String bundle(final Locale locale, final String bundleName, final String key, final String... args);

	LocalizedString bundleI18N(final String bundleName, final String key, final String... args);

	
	/* Versioning Information */
	
	<T> String versioningCreatorUsername(final T obj);
	
	<T> DateTime versioningCreationDate(final T obj);
	
    <T> String versioningUpdatorUsername(final T obj);
    
    <T> DateTime versioningUpdateDate(final T obj);

    /* ERP Integration */
    void scheduleDocumentForExportation(final FinantialDocument finantialDocument);

}
