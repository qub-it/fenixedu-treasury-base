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
package org.fenixedu.treasury.domain.forwardpayments;

import java.io.InputStream;
import java.util.stream.Stream;

import com.qubit.terra.framework.services.ServiceProvider;
import com.qubit.terra.framework.services.fileSupport.FileDescriptor;
import com.qubit.terra.framework.services.fileSupport.FileManager;
import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.FenixFramework;

@Deprecated
public class ForwardPaymentConfigurationFile extends ForwardPaymentConfigurationFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "application/octet-stream";

    public ForwardPaymentConfigurationFile() {
        super();
        
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControlAPI.isManager(username);
    }

    @Override
    public void delete() {
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        setDomainRoot(null);

        FileManager fileManager = ServiceProvider.getService(FileManager.class);

        if (StringUtils.isNotEmpty(getFileDescriptorId())) {
            fileManager.delete(getFileDescriptorId());
        }

        if (getTreasuryFile() != null) {
            services.deleteFile(this);
        }

        super.deleteDomainObject();
    }
    
    public static ForwardPaymentConfigurationFile create(final String filename, final byte[] contents) {
        throw new RuntimeException("not used anymore");
    }

    public static Stream<ForwardPaymentConfigurationFile> findAll() {
        return FenixFramework.getDomainRoot().getVirtualTPACertificateSet().stream();
    }

    @Override
    public byte[] getContent() {
        FileDescriptor fd = getFileDescriptor();
        if (fd != null) {
            return fd.getContent();
        }

        return IGenericFile.super.getContent();
    }

    @Override
    public long getSize() {
        FileDescriptor fd = getFileDescriptor();
        if (fd != null) {
            return fd.getSize();
        }

        return IGenericFile.super.getSize();
    }

    @Override
    public String getFilename() {
        FileDescriptor fd = getFileDescriptor();
        if (fd != null) {
            return fd.getName();
        }

        return IGenericFile.super.getFilename();
    }

    @Override
    public String getContentType() {
        FileDescriptor fd = getFileDescriptor();
        if (fd != null) {
            return fd.getContentType();
        }

        return IGenericFile.super.getContentType();
    }

    @Override
    public InputStream getStream() {
        FileDescriptor fd = getFileDescriptor();

        if (fd != null) {
            return fd.getReadStream();
        }

        return IGenericFile.super.getStream();
    }

    private FileDescriptor getFileDescriptor() {
        if (StringUtils.isNotBlank(getFileDescriptorId())) {
            return ServiceProvider.getService(FileManager.class).getFileDescriptor(getFileDescriptorId());
        }

        return null;
    }

}
