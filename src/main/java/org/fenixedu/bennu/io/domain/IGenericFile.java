package org.fenixedu.bennu.io.domain;

import java.io.InputStream;

import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.DomainObject;

public interface IGenericFile extends DomainObject {

	public boolean isAccessible(final String username);

	public void delete();
	
	default public byte[] getContent() {
		return TreasuryPlataformDependentServicesFactory.implementation().getFileContent(this);
	}

	default public long getSize() {
		return TreasuryPlataformDependentServicesFactory.implementation().getFileSize(this);
	}

	default public DateTime getCreationDate() {
		return TreasuryPlataformDependentServicesFactory.implementation().getFileCreationDate(this);
	}

	default public String getFilename() {
		return TreasuryPlataformDependentServicesFactory.implementation().getFilename(this);
	}

	default public InputStream getStream() {
		return TreasuryPlataformDependentServicesFactory.implementation().getFileStream(this);
	}

	default public String getContentType() {
		return TreasuryPlataformDependentServicesFactory.implementation().getFileContentType(this);
	}
	
}