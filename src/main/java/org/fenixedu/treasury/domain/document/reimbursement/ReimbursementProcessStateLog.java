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
package org.fenixedu.treasury.domain.document.reimbursement;

import java.util.Comparator;
import java.util.stream.Stream;

import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.joda.time.DateTime;

import com.google.common.base.Strings;

import pt.ist.fenixframework.Atomic;
import pt.ist.fenixframework.FenixFramework;

public class ReimbursementProcessStateLog extends ReimbursementProcessStateLog_Base {

    public static final Comparator<ReimbursementProcessStateLog> COMPARE_BY_VERSIONING_DATE =
            new Comparator<ReimbursementProcessStateLog>() {

                @Override
                public int compare(final ReimbursementProcessStateLog o1, final ReimbursementProcessStateLog o2) {
                    int c = TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(o1)
                            .compareTo(TreasuryPlataformDependentServicesFactory.implementation().versioningCreationDate(o2));
                    return c != 0 ? c : o1.getExternalId().compareTo(o2.getExternalId());
                }
            };

    public ReimbursementProcessStateLog() {
        super();
        setDomainRoot(FenixFramework.getDomainRoot());
    }

    protected ReimbursementProcessStateLog(final SettlementNote settlementNote,
            final ReimbursementProcessStatusType reimbursementProcessStatusType, final String statusId, final DateTime statusDate,
            final String remarks) {
        this();

        setSettlementNote(settlementNote);
        setReimbursementProcessStatusType(reimbursementProcessStatusType);
        setStatusId(statusId);
        setStatusDate(statusDate);
        setRemarks(remarks);

        checkRules();
    }

    private void checkRules() {

        if (getDomainRoot() == null) {
            throw new TreasuryDomainException("error.ReimbursementProcessStateLog.bennu.required");
        }

        if (getReimbursementProcessStatusType() == null) {
            throw new TreasuryDomainException("error.ReimbursementProcessStateLog.reimbursementProcessStatusType.required");
        }

        if (Strings.isNullOrEmpty(getStatusId())) {
            throw new TreasuryDomainException("error.ReimbursementProcessStateLog.statusId.required");
        }

        if (getStatusDate() == null) {
            throw new TreasuryDomainException("error.ReimbursementProcessStateLog.statusDate.required");
        }
    }

    // @formatter:off
    /* ********
     * SERVICES
     * ********
     */
    // @formatter:off

    public static Stream<ReimbursementProcessStateLog> findAll() {
        return FenixFramework.getDomainRoot().getReimbursementProcessStateLogsSet().stream();
    }

    public static Stream<ReimbursementProcessStateLog> find(final SettlementNote settlementNote) {
        return settlementNote.getReimbursementProcessStateLogsSet().stream();
    }

    @Atomic
    public static ReimbursementProcessStateLog create(final SettlementNote settlementNote,
            final ReimbursementProcessStatusType reimbursementProcessStatusType, final String statusId, final DateTime statusDate,
            final String remarks) {

        return new ReimbursementProcessStateLog(settlementNote, reimbursementProcessStatusType, statusId, statusDate, remarks);
    }

}
