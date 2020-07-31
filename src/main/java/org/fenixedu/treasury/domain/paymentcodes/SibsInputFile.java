/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: ricardo.pedro@qub-it.com, anil.mamede@qub-it.com
 * 
 *
 * 
 * This file is part of FenixEdu Treasury.
 *
 * FenixEdu Treasury is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu Treasury is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu Treasury.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.fenixedu.treasury.domain.paymentcodes;

import java.util.Comparator;
import java.util.stream.Stream;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.domain.paymentcodes.integration.SibsPaymentCodePool;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.services.payments.sibs.incomming.SibsIncommingPaymentFile;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class SibsInputFile extends SibsInputFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "text/plain";

    public static final Comparator<SibsInputFile> COMPARATOR_BY_DATE = (o1, o2) -> {
        int c = o1.getCreationDate().compareTo(o2.getCreationDate());

        return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
    };

    protected SibsInputFile() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
        setCreationDate(new DateTime());
    }

    protected SibsInputFile(DateTime whenProcessedBySIBS, String filename, byte[] content,
            final String uploader) {
        this();
        init(whenProcessedBySIBS, filename, content, uploader);
    }

    protected void init(DateTime whenProcessedBySIBS, String filename, byte[] content,
            final String uploader) {
        TreasuryPlataformDependentServicesFactory.implementation().createFile(this, filename, CONTENT_TYPE, content);

        setWhenProcessedBySibs(whenProcessedBySIBS);
        setUploaderUsername(uploader);

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
        final ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();

        if (!isDeletable()) {
            throw new TreasuryDomainException("error.SibsInputFile.cannot.delete");
        }

        setDomainRoot(null);
        setFinantialInstitution(null);

        services.deleteFile(this);

        super.deleteDomainObject();
    }

    @Atomic
    public static SibsInputFile create(DateTime whenProcessedBySIBS,
            String filename, byte[] content, final String uploader) {
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
            
            if (!SibsPaymentCodePool.find(entityReferenceCode).findAny().isPresent()) {
                throw new TreasuryDomainException(
                        "label.error.administration.payments.sibs.managesibsinputfile.error.in.sibs.inputfile.poolNull",
                        entityReferenceCode);
            }

            String loggedUsername = TreasuryPlataformDependentServicesFactory.implementation().getLoggedUsername();
            SibsInputFile sibsInputFile = new SibsInputFile(whenProcessedBySibs, filename, 
                    sibsContent, loggedUsername);
            
            return sibsInputFile;
        } catch (RuntimeException ex) {
            throw new TreasuryDomainException(
                    "label.error.administration.payments.sibs.managesibsinputfile.error.in.sibs.inputfile",
                    ex.getLocalizedMessage());
        }

    }

    public static Stream<SibsInputFile> findAll() {
        return FenixFramework.getDomainRoot().getSibsInputFilesSet().stream();
    }

    @Override
    public boolean isAccessible(final String username) {
        return TreasuryAccessControlAPI.isBackOfficeMember(username, getFinantialInstitution());
    }

}
