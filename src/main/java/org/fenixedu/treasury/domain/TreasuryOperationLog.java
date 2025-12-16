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
package org.fenixedu.treasury.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.FenixEDUTreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class TreasuryOperationLog extends TreasuryOperationLog_Base {

    public static Comparator<TreasuryOperationLog> COMPARATOR_BY_CREATION_DATE = new Comparator<TreasuryOperationLog>() {

        @Override
        public int compare(TreasuryOperationLog o1, TreasuryOperationLog o2) {
            DateTime o1CreationDate = FenixEDUTreasuryPlatformDependentServices.getVersioningCreationDate(o1);
            DateTime o2CreationDate = FenixEDUTreasuryPlatformDependentServices.getVersioningCreationDate(o2);
            if (o1CreationDate.isBefore(o2CreationDate)) {
                return -1;
            }
            if (o1CreationDate.isEqual(o2CreationDate)) {
                return 0;
            }
            return 1;
        }
    };

    protected TreasuryOperationLog() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected TreasuryOperationLog(final String log, final String oid, final String type) {
        this();
        init(log, oid, type);
    }

    protected void init(final String log, final String oid, final String type) {
        setLog(log);
        setDomainOid(oid);
        setType(type);
        checkRules();
    }

    private void checkRules() {
        //if (findByLog(getLog().count()>1)
        //{
        //  throw new TreasuryDomainException("error.TreasuryOperationLog.log.duplicated");
        //} 
        //if (findByOid(getOid().count()>1)
        //{
        //  throw new TreasuryDomainException("error.TreasuryOperationLog.oid.duplicated");
        //} 
        //if (findByType(getType().count()>1)
        //{
        //  throw new TreasuryDomainException("error.TreasuryOperationLog.type.duplicated");
        //} 
    }

    @Atomic
    public void edit(final String log, final String oid, final String type) {
        setLog(log);
        setDomainOid(oid);
        setType(type);
        checkRules();
    }

    @Override
    protected void checkForDeletionBlockers(Collection<String> blockers) {
        super.checkForDeletionBlockers(blockers);
    }

    @Atomic
    public void delete() {
        TreasuryDomainException.throwWhenDeleteBlocked(getDeletionBlockers());

        setDomainRoot(null);

        deleteDomainObject();
    }

    @Atomic
    public static TreasuryOperationLog create(final String log, final String oid, final String type) {
        return new TreasuryOperationLog(log, oid, type);
    }

    public static Stream<TreasuryOperationLog> findAll() {
        return FenixFramework.getDomainRoot().getTreasuryOperationLogsSet().stream();
    }

    public static Stream<TreasuryOperationLog> findByLog(final String log) {
        return findAll().filter(i -> log.equalsIgnoreCase(i.getLog()));
    }

    public static Stream<TreasuryOperationLog> findByOid(final String oid) {
        return findAll().filter(i -> oid.equalsIgnoreCase(i.getDomainOid()));
    }

    public static Stream<TreasuryOperationLog> findByType(final String type) {
        return findAll().filter(i -> type.equalsIgnoreCase(i.getType()));
    }

}
