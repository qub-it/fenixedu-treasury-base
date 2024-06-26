package org.fenixedu.treasury.services.accesscontrol.spi;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.groups.DynamicGroup;
import org.fenixedu.treasury.domain.FinantialEntity;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.FiscalYear;
import org.fenixedu.treasury.domain.accesscontrol.TreasuryAccessControlConfiguration;
import org.fenixedu.treasury.domain.document.SettlementNote;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

public class TreasuryUIAccessControlExtension implements ITreasuryAccessControlExtension<Object> {

    private static final String TREASURY_MANAGERS = "treasuryManagers";

    private static final String TREASURY_BACK_OFFICE = "treasuryBackOffice";

    private static final String TREASURY_FRONT_OFFICE = "treasuryFrontOffice";

    private static final String TREASURY_ALLOW_TO_ANNUL_SETTLEMENT_NOTES_WITHOUT_RESTRICTIONS =
            "treasuryAllowAnnulSettlementsWithoutAnyRestriction";

    @Override
    public boolean isFrontOfficeMember(final String username) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return getOrCreateDynamicGroup(TREASURY_FRONT_OFFICE).isMember(user);
    }

    @Override
    public boolean isFrontOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return getOrCreateDynamicGroup(TREASURY_FRONT_OFFICE).isMember(user);
    }

    @Override
    public boolean isBackOfficeMember(final String username) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return getOrCreateDynamicGroup(TREASURY_BACK_OFFICE).isMember(user);
    }

    @Override
    public boolean isBackOfficeMember(final String username, final FinantialInstitution finantialInstitution) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return getOrCreateDynamicGroup(TREASURY_BACK_OFFICE).isMember(user);
    }

    @Override
    public boolean isBackOfficeMember(final String username, final FinantialEntity finantialEntity) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return getOrCreateDynamicGroup(TREASURY_BACK_OFFICE).isMember(user);
    }

    @Override
    public boolean isManager(final String username) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return getOrCreateDynamicGroup(TREASURY_MANAGERS).isMember(user);
    }

    @Override
    public boolean isAllowToModifySettlements(final String username, final FinantialInstitution finantialInstitution) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return getOrCreateDynamicGroup(TREASURY_FRONT_OFFICE).isMember(user);
    }

    @Override
    @Deprecated
    public boolean isAllowToModifyInvoices(final String username, final FinantialInstitution finantialInstitution) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return getOrCreateDynamicGroup(TREASURY_FRONT_OFFICE).isMember(user);
    }

    @Override
    public boolean isAllowToConditionallyAnnulSettlementNote(final String username, final SettlementNote settlementNote) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final FinantialInstitution finantialInstitution = settlementNote.getDebtAccount().getFinantialInstitution();

        if (!TreasuryAccessControlAPI.isAllowToModifySettlements(username, finantialInstitution)) {
            return false;
        }

        if (settlementNote.getPaymentTransaction() != null) {
            return false;
        }

        LocalDate annulmentControlDate = settlementNote.getErpCertificationDate();

        if (annulmentControlDate == null) {
            if (settlementNote.isExportedInLegacyERP()) {
                return false;
            } else if (settlementNote.isDocumentToExport()) {
                return true;
            } else if (settlementNote.getClearDocumentToExportDate() != null) {
                annulmentControlDate = settlementNote.getClearDocumentToExportDate().toLocalDate();
            } else {
                return false;
            }
        }

        final int year = annulmentControlDate.getYear();

        if (!FiscalYear.findUnique(finantialInstitution, year).isPresent()) {
            return false;
        }

        FiscalYear fiscalYear = FiscalYear.findUnique(finantialInstitution, year).get();

        if (fiscalYear.getSettlementAnnulmentLimitDate() == null) {
            return false;
        }

        final DateTime limitDateTime =
                fiscalYear.getSettlementAnnulmentLimitDate().toDateTimeAtStartOfDay().plusDays(1).minusSeconds(1);
        if (!new DateTime().isAfter(limitDateTime)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean isAllowToAnnulSettlementNoteWithoutAnyRestriction(final String username, final SettlementNote settlementNote) {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return false;
        }

        final User user = User.findByUsername(username);

        return getOrCreateDynamicGroup(TREASURY_ALLOW_TO_ANNUL_SETTLEMENT_NOTES_WITHOUT_RESTRICTIONS).isMember(user);
    }

    private DynamicGroup getOrCreateDynamicGroup(final String dynamicGroupName) {
        final DynamicGroup dynamicGroup = DynamicGroup.get(dynamicGroupName);

        if (!dynamicGroup.isDefined()) {
            User manager = User.findByUsername("manager");
            if (manager != null) {
                dynamicGroup.mutator().grant(manager);
            } else {
                dynamicGroup.toPersistentGroup();
            }
        }

        return dynamicGroup;
    }

    @Override
    public Set<String> getFrontOfficeMemberUsernames() {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return Collections.emptySet();
        }

        return getOrCreateDynamicGroup(TREASURY_FRONT_OFFICE).getMembers().filter(m -> !isNullOrEmpty(m.getUsername()))
                .map(m -> m.getUsername()).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getBackOfficeMemberUsernames() {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return Collections.emptySet();
        }

        return getOrCreateDynamicGroup(TREASURY_BACK_OFFICE).getMembers().filter(m -> !isNullOrEmpty(m.getUsername()))
                .map(m -> m.getUsername()).collect(Collectors.toSet());
    }

    @Override
    public Set<String> getTreasuryManagerMemberUsernames() {
        if (!TreasuryAccessControlConfiguration.isAccessControlByBennuDynamicGroups()) {
            return Collections.emptySet();
        }

        return getOrCreateDynamicGroup(TREASURY_MANAGERS).getMembers().filter(m -> !isNullOrEmpty(m.getUsername()))
                .map(m -> m.getUsername()).collect(Collectors.toSet());
    }

}