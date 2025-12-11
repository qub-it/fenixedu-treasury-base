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
package org.fenixedu.treasury.domain.accesscontrol;

import java.util.Collections;
import java.util.List;

import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.services.accesscontrol.spi.ITreasuryAccessControlExtension;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.fenixedu.treasury.util.TreasuryConstants;

public class TreasuryAccessControl {

    private static TreasuryAccessControl _instance = new TreasuryAccessControl();

    private List<ITreasuryAccessControlExtension<?>> extensions = Collections.synchronizedList(Lists.newArrayList());

    private TreasuryAccessControl() {
    }

    public boolean isFrontOfficeMember() {
        return isFrontOfficeMember(TreasuryConstants.getAuthenticatedUsername());
    }

    public boolean isBackOfficeMember() {
        return isBackOfficeMember(TreasuryConstants.getAuthenticatedUsername());
    }

    public boolean isFrontOfficeMember(FinantialInstitution finantialInstitution) {
        return isFrontOfficeMember(TreasuryConstants.getAuthenticatedUsername(), finantialInstitution);
    }

    public boolean isBackOfficeMember(FinantialInstitution finantialInstitution) {
        return isBackOfficeMember(TreasuryConstants.getAuthenticatedUsername(), finantialInstitution);
    }

    public boolean isManager() {
        return isManager(TreasuryConstants.getAuthenticatedUsername());
    }

    public boolean isFrontOfficeMember(final String username) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isFrontOfficeMember(username)) {
                return true;
            }
        }

        return false;
    }

    public boolean isFrontOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isFrontOfficeMember(username, finantialInstitution)) {
                return true;
            }
        }

        return false;
    }

    public boolean isFrontOfficeMember(String username, FinantialEntity finantialEntity) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isFrontOfficeMember(username, finantialEntity)) {
                return true;
            }
        }

        return false;
    }

    public <T> boolean isFrontOfficeMemberWithinContext(final String username, final T context) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (!ext.isContextObjectApplied(context)) {
                continue;
            }

            if (((ITreasuryAccessControlExtension<T>) ext).isFrontOfficeMemberWithinContext(username, context)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBackOfficeMember(final String username) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isBackOfficeMember(username)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBackOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isBackOfficeMember(username, finantialInstitution)) {
                return true;
            }
        }

        return false;
    }

    public boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isBackOfficeMember(username, finantialEntity)) {
                return true;
            }
        }

        return false;
    }

    public <T> boolean isBackOfficeMemberWithinContext(final String username, final T context) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (!ext.isContextObjectApplied(context)) {
                continue;
            }

            if (((ITreasuryAccessControlExtension<T>) ext).isBackOfficeMemberWithinContext(username, context)) {
                return true;
            }
        }

        return false;
    }

    public boolean isManager(final String username) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isManager(username)) {
                return true;
            }
        }

        return false;
    }

    public boolean isRegistered(Class<? extends ITreasuryAccessControlExtension<?>> clazz) {
        return extensions.stream().anyMatch(e -> clazz.equals(e.getClass()));
    }

    public void registerExtension(ITreasuryAccessControlExtension<?> extension) {
        extensions.add(extension);
    }

    public void unregisterExtension(Class<? extends ITreasuryAccessControlExtension<?>> extensionClazz) {
        extensions.removeIf(e -> extensionClazz.equals(e.getClass()));
    }

    public List<ITreasuryAccessControlExtension<?>> getRegisteredExtensions() {
        return this.extensions;
    }

    @Deprecated
    public boolean isAllowToModifyInvoices(final String username, final FinantialInstitution finantialInstitution) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isAllowToModifyInvoices(username, finantialInstitution)) {
                return true;
            }
        }

        return false;
    }

    public boolean isAllowToModifySettlements(final String username, final FinantialInstitution finantialInstitution) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isAllowToModifySettlements(username, finantialInstitution)) {
                return true;
            }
        }

        return false;
    }

    public boolean isAllowToConditionallyAnnulSettlementNote(final String username, final SettlementNote settlementNote) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isAllowToConditionallyAnnulSettlementNote(username, settlementNote)) {
                return true;
            }
        }

        return false;
    }

    public boolean isAllowToAnnulSettlementNoteWithoutAnyRestriction(final String username, final SettlementNote settlementNote) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.isAllowToAnnulSettlementNoteWithoutAnyRestriction(username, settlementNote)) {
                return true;
            }
        }

        return false;
    }

    public boolean hasPermission(String username, String permissionCode, FinantialEntity finantialEntity) {
        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            if (ext.hasPermission(username, permissionCode, finantialEntity)) {
                return true;
            }
        }

        return false;
    }

    public java.util.Set<String> getFrontOfficeMemberUsernames() {
        final java.util.Set<String> result = Sets.newHashSet();

        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            result.addAll(ext.getFrontOfficeMemberUsernames());
        }

        return result;
    }

    public java.util.Set<String> getBackOfficeMemberUsernames() {
        final java.util.Set<String> result = Sets.newHashSet();

        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            result.addAll(ext.getBackOfficeMemberUsernames());
        }

        return result;
    }

    public java.util.Set<String> getTreasuryManagerMemberUsernames() {
        final java.util.Set<String> result = Sets.newHashSet();

        for (ITreasuryAccessControlExtension<?> ext : extensions) {
            result.addAll(ext.getTreasuryManagerMemberUsernames());
        }

        return result;
    }

    public static TreasuryAccessControl getInstance() {
        return _instance;
    }

}
