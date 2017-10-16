/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and ServiÃ§os Partilhados da
 * Universidade de Lisboa:
 *  - Copyright Â© 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright Â© 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: xpto@qub-it.com
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

package org.fenixedu.treasury.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;

import pt.ist.fenixframework.Atomic;;

public class TreasuryOperationLog extends TreasuryOperationLog_Base {

    public static Comparator<TreasuryOperationLog> COMPARATOR_BY_CREATION_DATE = new Comparator<TreasuryOperationLog>() {

        @Override
        public int compare(TreasuryOperationLog o1, TreasuryOperationLog o2) {
            if (o1.getVersioningCreationDate().isBefore(o2.getVersioningCreationDate())) {
                return -1;
            }
            if (o1.getVersioningCreationDate().isEqual(o2.getVersioningCreationDate())) {
                return 0;
            }
            return 1;
        }
    };

    protected TreasuryOperationLog() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
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
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getTreasuryOperationLogsSet().stream();
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
