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
package org.fenixedu.treasury.domain.paymentcodes;

import java.util.Comparator;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.TreasuryFile;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.payments.sibs.SIBSPaymentsImporter;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFile;
import org.joda.time.DateTime;

import com.qubit.terra.framework.services.ServiceProvider;
import com.qubit.terra.framework.services.fileSupport.FileDescriptor;
import com.qubit.terra.framework.services.fileSupport.FileManager;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;
import pt.ist.fenixframework.core.AbstractDomainObject;

public class SibsInputFile extends SibsInputFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "text/plain";

    public static final Comparator<SibsInputFile> COMPARATOR_BY_DATE =
            Comparator.comparing((SibsInputFile o) -> o.getCreationDate()).thenComparing(AbstractDomainObject::getExternalId);

    protected SibsInputFile() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    protected SibsInputFile(DateTime whenProcessedBySIBS, String filename, byte[] content, final String uploader) {
        this();
        init(whenProcessedBySIBS, filename, content, uploader);
    }

    protected void init(DateTime whenProcessedBySIBS, String filename, byte[] content, final String uploader) {
        FileManager fileManager = ServiceProvider.getService(FileManager.class);

        FileDescriptor fileDescriptor = fileManager.createFile(filename, content.length, CONTENT_TYPE, content);
        setFileDescriptorId(fileDescriptor.getId());

        setWhenProcessedBySibs(whenProcessedBySIBS);
        setUploaderUsername(uploader);

        String sibsEntityCode = new SIBSPaymentsImporter().readSibsEntityCode(filename, content);
        setSibsEntityCode(sibsEntityCode);

        checkRules();
    }

    private void checkRules() {
    }

    public boolean isDeletable() {
        return true;
    }

    @Override
    @Deprecated
    public FinantialInstitution getFinantialInstitution() {
        // TODO Auto-generated method stub
        return super.getFinantialInstitution();
    }

    @Override
    @Deprecated
    public void setFinantialInstitution(FinantialInstitution finantialInstitution) {
        // TODO Auto-generated method stub
        super.setFinantialInstitution(finantialInstitution);
    }

    @Override
    @Atomic
    public void delete() {
        FileManager fileManager = ServiceProvider.getService(FileManager.class);

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.SibsInputFile.cannot.delete");
        }

        setDomainRoot(null);
        setFinantialInstitution(null);

        if (StringUtils.isNotEmpty(getFileDescriptorId())) {
            fileManager.delete(getFileDescriptorId());
        }

        if (getTreasuryFile() != null) {
            getTreasuryFile().delete();
        }

        super.deleteDomainObject();
    }

    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControlAPI.isBackOfficeMember(username, getFinantialInstitution());
    }

    @Atomic
    public void updateLastProcessExecutionDate() {
        setLastProcessExecutionDate(new DateTime());
    }

    @Atomic
    public static SibsInputFile create(DateTime whenProcessedBySIBS, String filename, byte[] content, final String uploader) {
        return new SibsInputFile(whenProcessedBySIBS, filename, content, uploader);
    }

    @Atomic
    public static SibsInputFile createSibsInputFile(DateTime whenProcessedBySibs, final String filename,
            final byte[] sibsContent) {
        try {

            final SibsIncommingPaymentFile file = SibsIncommingPaymentFile.parse(filename, sibsContent);
            if (file.getHeader().getWhenProcessedBySibs().toDateTimeAtStartOfDay().compareTo(whenProcessedBySibs) != 0) {
                whenProcessedBySibs = file.getHeader().getWhenProcessedBySibs().toDateTimeAtStartOfDay();
            }

            String entityReferenceCode = file.getHeader().getEntityCode();

            if (SibsPaymentCodePool.find(entityReferenceCode).findAny().isEmpty()) {
                throw new TreasuryDomainException(
                        "label.error.administration.payments.sibs.managesibsinputfile.error.in.sibs.inputfile.poolNull",
                        entityReferenceCode);
            }

            String loggedUsername = org.fenixedu.treasury.util.TreasuryConstants.getAuthenticatedUsername();

            return new SibsInputFile(whenProcessedBySibs, filename, sibsContent, loggedUsername);
        } catch (RuntimeException ex) {
            throw new TreasuryDomainException(
                    "label.error.administration.payments.sibs.managesibsinputfile.error.in.sibs.inputfile",
                    ex.getLocalizedMessage());
        }

    }

    public static Stream<SibsInputFile> findAll() {
        return FenixFramework.getDomainRoot().getSibsInputFilesSet().stream();
    }

    // 2026-08-12 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public String getFileId() {
        return super.getFileId();
    }

    // 2026-08-12 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public void setFileId(String fileId) {
        super.setFileId(fileId);
    }

    // 2026-08-12 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public TreasuryFile getTreasuryFile() {
        return super.getTreasuryFile();
    }

    // 2026-08-12 (#qubIT-Fenix-8024)
    //
    // The property fileDescriptorId is used to get the file id
    @Override
    @Deprecated
    public void setTreasuryFile(TreasuryFile treasuryFile) {
        super.setTreasuryFile(treasuryFile);
    }

}
