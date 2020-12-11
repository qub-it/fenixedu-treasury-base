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
package org.fenixedu.treasury.domain.document;

import java.util.stream.Stream;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import com.qubit.terra.docs.core.IDocumentTemplate;
import com.qubit.terra.docs.core.IDocumentTemplateVersion;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TreasuryDocumentTemplate extends TreasuryDocumentTemplate_Base implements IDocumentTemplate {

    protected TreasuryDocumentTemplate() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TreasuryDocumentTemplate(final FinantialDocumentType finantialDocumentTypes,
            final FinantialEntity finantialEntity) {
        this();
        setFinantialDocumentType(finantialDocumentTypes);
        setFinantialEntity(finantialEntity);

        checkRules();
    }

    private void checkRules() {
        if (getFinantialDocumentType() == null) {
            throw new TreasuryDomainException("error.TreasuryDocumentTemplate.finantialDocumentTypes.required");
        }

        if (getFinantialEntity() == null) {
            throw new TreasuryDomainException("error.TreasuryDocumentTemplate.finantialEntity.required");
        }
        if (findByFinantialDocumentTypeAndFinantialEntity(getFinantialDocumentType(), getFinantialEntity()).count() > 1) {
            throw new TreasuryDomainException("error.TreasuryDocumentTemplate.duplicated");
        }
    }

    @Atomic
    public void edit(final FinantialDocumentType finantialDocumentTypes, final FinantialEntity finantialEntity) {
        setFinantialDocumentType(finantialDocumentTypes);
        setFinantialEntity(finantialEntity);

        checkRules();
    }

    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.TreasuryDocumentTemplate.cannot.delete");
        }

        for (TreasuryDocumentTemplateFile file : this.getTreasuryDocumentTemplateFilesSet()) {
            this.removeTreasuryDocumentTemplateFiles(file);
            file.delete();
        }

        setDomainRoot(null);
        setFinantialDocumentType(null);
        setFinantialEntity(null);
        deleteDomainObject();
    }

    public TreasuryDocumentTemplateFile getAtiveDocumentTemplateFile() {
        for (TreasuryDocumentTemplateFile documentTemplateFile : getTreasuryDocumentTemplateFilesSet()) {
            if (documentTemplateFile.getActive()) {
                return documentTemplateFile;
            }
        }
        return null;
    }

    @Atomic
    public TreasuryDocumentTemplateFile addFile(final TreasuryDocumentTemplate documentTemplate, final String displayName,
            final String fileName, final byte[] content) {
        TreasuryDocumentTemplateFile treasuryDocumentTemplateFile =
                TreasuryDocumentTemplateFile.create(this, displayName, fileName, content);

        activateFile(treasuryDocumentTemplateFile);

        return treasuryDocumentTemplateFile;
    }

    void activateFile(TreasuryDocumentTemplateFile treasuryDocumentTemplateFile) {
        for (TreasuryDocumentTemplateFile file : getTreasuryDocumentTemplateFilesSet()) {
            file.setActive(false);
        }
        treasuryDocumentTemplateFile.setActive(true);
    }

    @Atomic
    public static TreasuryDocumentTemplate create(final FinantialDocumentType finantialDocumentTypes,
            final FinantialEntity finantialEntity) {
        return new TreasuryDocumentTemplate(finantialDocumentTypes, finantialEntity);
    }

    public static Stream<TreasuryDocumentTemplate> findAll() {
        return FenixFramework.getDomainRoot().getTreasuryDocumentTemplatesSet().stream();
    }

    public static Stream<TreasuryDocumentTemplate> findByFinantialDocumentType(
            final FinantialDocumentType finantialDocumentType) {
        return finantialDocumentType.getTreasuryDocumentTemplatesSet().stream();
    }

    public static Stream<TreasuryDocumentTemplate> findByFinantialEntity(final FinantialEntity finantialEntity) {
        return finantialEntity.getTreasuryDocumentTemplatesSet().stream();
    }

    public static Stream<TreasuryDocumentTemplate> findByFinantialDocumentTypeAndFinantialEntity(
            final FinantialDocumentType finantialDocumentType, final FinantialEntity finantialEntity) {
        return findByFinantialDocumentType(finantialDocumentType).filter(i -> finantialEntity.equals(i.getFinantialEntity()));
    }

    @Override
    public void activateDocument() {
        // TODO Auto-generated method stub

    }

    @Override
    public void deactivateDocument() {

    }

    @Override
    public DateTime getCreationDate() {
        return getAtiveDocumentTemplateFile().getCreationDate();
    }

    @Override
    public IDocumentTemplateVersion getCurrentVersion() {
        return new IDocumentTemplateVersion() {

            @Override
            public byte[] getContent() {
                return getAtiveDocumentTemplateFile().getContent();
            }
        };
    }

    @Override
    public DateTime getUpdateDate() {
        return TreasuryPlataformDependentServicesFactory.implementation().versioningUpdateDate(this);
    }

    @Override
    public boolean isActive() {
        return getAtiveDocumentTemplateFile() != null;
    }

}
