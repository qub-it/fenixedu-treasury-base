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

import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibs.outgoing.SibsOutgoingPaymentFile;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class SibsOutputFile extends SibsOutputFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "text/plain";

    public SibsOutputFile() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    private String createPaymentFile(Set<SibsPaymentCodePool> paymentCodePoolsToInclude, DateTime lastSuccessfulSentDateTime,
            StringBuilder errorsBuilder) {

        // check selected pools are not empty
        if (paymentCodePoolsToInclude.isEmpty()) {
            throw new TreasuryDomainException("error.SibsOutputFile.selected.paymentCodePools.empty");
        }

        // check sourceInstitutionId and destinationInstitutionId are the same
        String sourceInstitutionId = paymentCodePoolsToInclude.iterator().next().getSourceInstitutionId();
        String destinationInstitutionId = paymentCodePoolsToInclude.iterator().next().getDestinationInstitutionId();
        
        if(StringUtils.isEmpty(sourceInstitutionId)) {
            throw new TreasuryDomainException("error.SibsOutputFile.sourceInstitutionId.required");
        }
        
        if(StringUtils.isEmpty(destinationInstitutionId)) {
            throw new TreasuryDomainException("error.SibsOutputFile.destinationInstitutionId.required");
        }

        if (paymentCodePoolsToInclude.stream().anyMatch(p -> !sourceInstitutionId.equals(p.getSourceInstitutionId()))) {
            throw new TreasuryDomainException("error.SibsOutputFile.selected.paymentCodePools.sourceInstitutionId.differ");
        }

        if (paymentCodePoolsToInclude.stream().anyMatch(p -> !destinationInstitutionId.equals(p.getDestinationInstitutionId()))) {
            throw new TreasuryDomainException("error.SibsOutputFile.selected.paymentCodePools.destinationInstitutionId.differ");
        }

        // check entity reference code is the same
        String entityReferenceCode = paymentCodePoolsToInclude.iterator().next().getEntityReferenceCode();
        if (paymentCodePoolsToInclude.stream().anyMatch(p -> !entityReferenceCode.equals(p.getEntityReferenceCode()))) {
            throw new TreasuryDomainException("error.SibsOutputFile.selected.paymentCodePools.entityReferenceCode.differ");
        }

        SibsOutgoingPaymentFile sibsOutgoingPaymentFile = new SibsOutgoingPaymentFile(sourceInstitutionId,
                destinationInstitutionId, entityReferenceCode, lastSuccessfulSentDateTime);

        for (SibsReferenceCode referenceCode : getNotPaidReferenceCodes(paymentCodePoolsToInclude, errorsBuilder)) {
            addCalculatedPaymentCodesFromEvent(sibsOutgoingPaymentFile, referenceCode, errorsBuilder);
        }

        return sibsOutgoingPaymentFile.render();

    }

    @Deprecated
    protected String createPaymentFile(FinantialInstitution finantialInstitution, String sibsEntityReferenceCode,
            final DateTime lastSuccessfulSentDateTime, final StringBuilder errorsBuilder) {
        SibsPaymentCodePool pool = SibsPaymentCodePool.find(finantialInstitution)
                .filter(p -> sibsEntityReferenceCode.equals(p.getEntityReferenceCode())).findFirst().get();

        SibsOutgoingPaymentFile sibsOutgoingPaymentFile = new SibsOutgoingPaymentFile(pool.getSourceInstitutionId(),
                pool.getDestinationInstitutionId(), sibsEntityReferenceCode, lastSuccessfulSentDateTime);

        for (SibsReferenceCode referenceCode : getNotPaidReferenceCodes(finantialInstitution, sibsEntityReferenceCode,
                errorsBuilder)) {
            addCalculatedPaymentCodesFromEvent(sibsOutgoingPaymentFile, referenceCode, errorsBuilder);
        }

        return sibsOutgoingPaymentFile.render();
    }

    @Deprecated
    private Set<SibsReferenceCode> getNotPaidReferenceCodes(FinantialInstitution finantialInstitution,
            String sibsEntityReferenceCode, StringBuilder errorsBuilder) {
        return SibsPaymentCodePool.find(finantialInstitution)
                .filter(p -> sibsEntityReferenceCode.equals(p.getEntityReferenceCode()))
                .flatMap(SibsPaymentCodePool::getPaymentCodesToExport).collect(Collectors.toSet());
    }

    private Set<SibsReferenceCode> getNotPaidReferenceCodes(Set<SibsPaymentCodePool> paymentCodePoolsToInclude,
            StringBuilder errorsBuilder) {
        return paymentCodePoolsToInclude.stream().flatMap(SibsPaymentCodePool::getPaymentCodesToExport)
                .collect(Collectors.toSet());
    }

    private void appendToErrors(StringBuilder errorsBuilder, String externalId, Throwable e) {
        errorsBuilder.append("Error in : " + externalId + "-" + e.getLocalizedMessage()).append("\n");

        this.setErrorLog(errorsBuilder.toString());
    }

    protected void addCalculatedPaymentCodesFromEvent(final SibsOutgoingPaymentFile file, final SibsReferenceCode referenceCode,
            StringBuilder errorsBuilder) {
        try {
            file.addAssociatedPaymentCode(referenceCode);
            file.addLine(referenceCode.getReferenceCode(), referenceCode.getMinAmount(), referenceCode.getMaxAmount(),
                    referenceCode.getValidFrom(), referenceCode.getValidTo());
        } catch (Throwable e) {
            appendToErrors(errorsBuilder, referenceCode.getExternalId(), e);
        }
    }

    private String outgoingFilename() {
        return String.format("SIBS-%s.txt", new DateTime().toString("dd-MM-yyyy_H_m_s"));
    }

    @Override
    public boolean isAccessible(final String username) {
        return true;
    }

    protected void init(final FinantialInstitution finantialInstitution, final java.lang.String errorLog,
            final java.lang.String infoLog, final java.lang.String printedPaymentCodes) {
        setFinantialInstitution(finantialInstitution);
        setErrorLog(errorLog);
        setInfoLog(infoLog);
        setPrintedPaymentCodes(printedPaymentCodes);

        checkRules();
    }

    private void checkRules() {
    }

    @Atomic
    public void edit(final FinantialInstitution finantialInstitution, final String errorLog, final String infoLog,
            final String printedPaymentCodes) {
        setFinantialInstitution(finantialInstitution);
        setErrorLog(errorLog);
        setInfoLog(infoLog);
        setPrintedPaymentCodes(printedPaymentCodes);

        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Override
    @Atomic
    public void delete() {
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        setFinantialInstitution(null);

        setDomainRoot(null);
        services.deleteFile(this);

        super.deleteDomainObject();
    }

    /* ********
     * SERVICES
     * ********
     */
    public static Stream<SibsOutputFile> findAll() {
        return FenixFramework.getDomainRoot().getSibsOutputFilesSet().stream();
    }

    public static Stream<SibsOutputFile> findByFinantialInstitution(final FinantialInstitution finantialInstitution) {
        return finantialInstitution.getSibsOutputFilesSet().stream();
    }

    public static Stream<SibsOutputFile> findByErrorLog(final java.lang.String errorLog) {
        return findAll().filter(i -> errorLog.equalsIgnoreCase(i.getErrorLog()));
    }

    public static Stream<SibsOutputFile> findByInfoLog(final java.lang.String infoLog) {
        return findAll().filter(i -> infoLog.equalsIgnoreCase(i.getInfoLog()));
    }

    @Deprecated
    public static SibsOutputFile create(FinantialInstitution finantialInstitution, String sibsEntityReferenceCode,
            DateTime lastSuccessfulSentDateTime) {
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        SibsOutputFile file = new SibsOutputFile();

        try {
            StringBuilder errorsBuilder = new StringBuilder();
            byte[] paymentFileContents = file
                    .createPaymentFile(finantialInstitution, sibsEntityReferenceCode, lastSuccessfulSentDateTime, errorsBuilder)
                    .getBytes("ASCII");

            services.createFile(file, file.outgoingFilename(), CONTENT_TYPE, paymentFileContents);

            file.setLastSuccessfulExportation(lastSuccessfulSentDateTime);
            file.setErrorLog(errorsBuilder.toString());
        } catch (Exception e) {
            StringBuilder builder = new StringBuilder();
            builder.append(e.getLocalizedMessage()).append("\n");
            for (StackTraceElement el : e.getStackTrace()) {
                builder.append(el.toString()).append("\n");
            }

            services.createFile(file, file.outgoingFilename(), CONTENT_TYPE, new byte[0]);

            file.setLastSuccessfulExportation(lastSuccessfulSentDateTime);
            file.setErrorLog(builder.toString());
        }

        return file;
    }

    public static SibsOutputFile create(Set<SibsPaymentCodePool> paymentCodePoolsToInclude, DateTime lastSuccessfulSentDateTime) {
        try {
            final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

            SibsOutputFile file = new SibsOutputFile();

            StringBuilder errorsBuilder = new StringBuilder();
            byte[] paymentFileContents = file
                    .createPaymentFile(paymentCodePoolsToInclude, lastSuccessfulSentDateTime, errorsBuilder).getBytes("ASCII");

            services.createFile(file, file.outgoingFilename(), CONTENT_TYPE, paymentFileContents);

            file.setLastSuccessfulExportation(lastSuccessfulSentDateTime);
            file.setErrorLog(errorsBuilder.toString());

            return file;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
