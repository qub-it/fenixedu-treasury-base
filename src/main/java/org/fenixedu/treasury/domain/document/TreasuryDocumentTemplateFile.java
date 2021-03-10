/**
 * Copyright (c) 2015, Quorum Born IT <http://www.qub-it.com/>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, without
 * modification, are permitted provided that the following
 * conditions are met:
 *
 *  (o) Redistributions of source code must retain the above
 *  copyright notice, this list of conditions and the following
 *  disclaimer.
 *
 *  (o) Redistributions in binary form must reproduce the
 *  above copyright notice, this list of conditions and the
 *  following disclaimer in the documentation and/or other
 *  materials provided with the distribution.
 *
 *  (o) Neither the name of Quorum Born IT nor the names of
 *  its contributors may be used to endorse or promote products
 *  derived from this software without specific prior written
 *  permission.
 *
 *  (o) Universidade de Lisboa and its respective subsidiary
 *  Serviços Centrais da Universidade de Lisboa (Departamento
 *  de Informática), hereby referred to as the Beneficiary,
 *  is the sole demonstrated end-user and ultimately the only
 *  beneficiary of the redistributed binary form and/or source
 *  code.
 *
 *  (o) The Beneficiary is entrusted with either the binary form,
 *  the source code, or both, and by accepting it, accepts the
 *  terms of this License.
 *
 *  (o) Redistribution of any binary form and/or source code is
 *  only allowed in the scope of the Universidade de Lisboa
 *  FenixEdu(™)’s implementation projects.
 *
 *  (o) This license and conditions of redistribution of source
 *  code/binary can oly be reviewed by the Steering Comittee of
 *  FenixEdu(™) <http://www.fenixedu.org/>.
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
package org.fenixedu.treasury.domain.document;


import java.util.stream.Stream;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TreasuryDocumentTemplateFile extends TreasuryDocumentTemplateFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "application/vnd.oasis.opendocument.text";
    public static final String FILE_EXTENSION = ".odt";

    public TreasuryDocumentTemplateFile() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    protected TreasuryDocumentTemplateFile(final TreasuryDocumentTemplate documentTemplate, final boolean active,
            final String displayName, final String fileName, final byte[] content) {
        this();

        TreasuryPlataformDependentServicesFactory.implementation().createFile(this, fileName, CONTENT_TYPE, content);
        setTreasuryDocumentTemplate(documentTemplate);
        setActive(active);

        documentTemplate.activateFile(this);

        checkRules();
    }

    private void checkRules() {
        if (getTreasuryDocumentTemplate() == null) {
            throw new TreasuryDomainException("error.TreasuryDocumentTemplateFile.documentTemplate.required");
        }
    }

    @Atomic
    public void edit(final TreasuryDocumentTemplate documentTemplate, final boolean active) {
        setTreasuryDocumentTemplate(documentTemplate);
        setActive(active);

        checkRules();
    }

    public boolean isDeletable() {
        return true;
    }

    @Override
    @Atomic
    public void delete() {
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.TreasuryDocumentTemplateFileDomainObject.cannot.delete");
        }

        setDomainRoot(null);
        setTreasuryDocumentTemplate(null);
        services.deleteFile(this);

        super.deleteDomainObject();
    }

    @Atomic
    static TreasuryDocumentTemplateFile create(final TreasuryDocumentTemplate documentTemplate, final String displayName,
            final String fileName, final byte[] content) {
        TreasuryDocumentTemplateFile documentTemplateFile =
                new TreasuryDocumentTemplateFile(documentTemplate, false, displayName, fileName, content);
        return documentTemplateFile;
    }

    public static Stream<TreasuryDocumentTemplateFile> findAll() {
        return FenixFramework.getDomainRoot().getTreasuryDocumentTemplateFilesSet().stream();
    }

    public static Stream<TreasuryDocumentTemplateFile> findByDocumentTemplate(final TreasuryDocumentTemplate documentTemplate) {
        return documentTemplate.getTreasuryDocumentTemplateFilesSet().stream();
    }

    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControlAPI.isBackOfficeMember(username, getTreasuryDocumentTemplate().getFinantialEntity());
    }

}
