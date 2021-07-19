/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 * 	(o) Redistributions of source code must retain the above
 * 	copyright notice, this list of conditions and the following
 * 	disclaimer.
 *
 * 	(o) Redistributions in binary form must reproduce the
 * 	above copyright notice, this list of conditions and the
 * 	following disclaimer in the documentation and/or other
 * 	materials provided with the distribution.
 *
 * 	(o) Neither the name of Quorum Born IT nor the names of
 * 	its contributors may be used to endorse or promote products
 * 	derived from this software without specific prior written
 * 	permission.
 *
 * 	(o) Universidade de Lisboa and its respective subsidiary
 * 	Serviços Centrais da Universidade de Lisboa (Departamento
 * 	de Informática), hereby referred to as the Beneficiary,
 * 	is the sole demonstrated end-user and ultimately the only
 * 	beneficiary of the redistributed binary form and/or source
 * 	code.
 *
 * 	(o) The Beneficiary is entrusted with either the binary form,
 * 	the source code, or both, and by accepting it, accepts the
 * 	terms of this License.
 *
 * 	(o) Redistribution of any binary form and/or source code is
 * 	only allowed in the scope of the Universidade de Lisboa
 * 	FenixEdu(™)’s implementation projects.
 *
 * 	(o) This license and conditions of redistribution of source
 * 	code/binary can oly be reviewed by the Steering Comittee of
 * 	FenixEdu(™) <http://www.fenixedu.org/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL “Quorum Born IT�? BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.fenixedu.treasury.domain.payments;

import java.io.InputStream;

import org.apache.commons.beanutils.PropertyUtils;
import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

public class PaymentRequestLogFile extends PaymentRequestLogFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "application/octet-stream";

    public PaymentRequestLogFile() {
        super();

        this.setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    protected PaymentRequestLogFile(String filename, byte[] content) {
        this();
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        services.createFile(this, filename, CONTENT_TYPE, content);
    }

    @Override
    public boolean isAccessible(String username) {
        return false;
    }

    public String getContentAsString() {
        if (getContent() != null) {
            return new String(getContent());
        }

        return null;
    }

    @Override
    public void delete() {
        setDomainRoot(null);
    }

    @Override
    public byte[] getContent() {
        try {
            if (PropertyUtils.getPropertyDescriptor(this, "treasuryFile") != null) {
                Object file = PropertyUtils.getProperty(this, "treasuryFile");

                if (file != null) {
                    return TreasuryPlataformDependentServicesFactory.implementation().getFileContent(this);
                }
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileContent(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public long getSize() {
        try {
            if (PropertyUtils.getPropertyDescriptor(this, "treasuryFile") != null) {
                Object file = PropertyUtils.getProperty(this, "treasuryFile");

                if (file != null) {
                    return TreasuryPlataformDependentServicesFactory.implementation().getFileSize(this);
                }
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileSize(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DateTime getCreationDate() {
        try {
            if (PropertyUtils.getPropertyDescriptor(this, "treasuryFile") != null) {
                Object file = PropertyUtils.getProperty(this, "treasuryFile");

                if (file != null) {
                    return TreasuryPlataformDependentServicesFactory.implementation().getFileCreationDate(this);
                }
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileCreationDate(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getFilename() {
        try {
            if (PropertyUtils.getPropertyDescriptor(this, "treasuryFile") != null) {
                Object file = PropertyUtils.getProperty(this, "treasuryFile");

                if (file != null) {
                    return TreasuryPlataformDependentServicesFactory.implementation().getFilename(this);
                }
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFilename(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream getStream() {
        try {
            if (PropertyUtils.getPropertyDescriptor(this, "treasuryFile") != null) {
                Object file = PropertyUtils.getProperty(this, "treasuryFile");

                if (file != null) {
                    return TreasuryPlataformDependentServicesFactory.implementation().getFileStream(this);
                }
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileStream(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getContentType() {
        try {
            if (PropertyUtils.getPropertyDescriptor(this, "treasuryFile") != null) {
                Object file = PropertyUtils.getProperty(this, "treasuryFile");

                if (file != null) {
                    return TreasuryPlataformDependentServicesFactory.implementation().getFileContentType(this);
                }
            }
            
            return TreasuryPlataformDependentServicesFactory.implementation().getFileContentType(getFileId());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // @formatter:off
    /*
     * 
     * ********
     * SERVICES
     * ********
     */
    // @formatter:on

    public static PaymentRequestLogFile create(String filename, byte[] content) {
        return new PaymentRequestLogFile(filename, content);
    }
}
