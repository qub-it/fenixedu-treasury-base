package org.fenixedu.treasury.services.integration;

import java.io.InputStream;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.document.FinantialDocument;
import org.fenixedu.treasury.domain.integration.ERPConfiguration;
import org.fenixedu.treasury.services.integration.erp.IERPExternalService;
import org.joda.time.DateTime;

public interface ITreasuryPlatformDependentServices {

	public void scheduleSingleDocument(final FinantialDocument finantialDocument);
	
	public IERPExternalService getERPExternalServiceImplementation(final ERPConfiguration erpConfiguration);

	/* File */
	
	public byte[] getFileContent(IGenericFile genericFile);

	public long getFileSize(IGenericFile genericFile);

	public String getFilename(IGenericFile genericFile);

	public InputStream getFileStream(IGenericFile genericFile);

	public DateTime getFileCreationDate(IGenericFile genericFile);

	public String getFileContentType(IGenericFile iGenericFile);

	public void createFile(final IGenericFile genericFile, final String displayName, final String fileName, final byte[] content);

	public void deleteFile(final IGenericFile genericFile);

	public String getLoggedUsername();
	
	/* Bundles */
	
    public String bundle(final String bundleName, final String key, final String... args);

    public LocalizedString bundleI18N(final String bundleName, final String key, final String... args);


	
}
