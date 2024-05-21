package org.fenixedu.treasury.services.groups;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundle;

import java.util.stream.Stream;

import org.fenixedu.bennu.core.annotation.GroupOperator;
import org.fenixedu.bennu.core.domain.User;
import org.fenixedu.bennu.core.domain.groups.PersistentGroup;
import org.fenixedu.bennu.core.groups.CustomGroup;
import org.fenixedu.treasury.domain.accesscontrol.PersistentTreasuryFrontOfficeGroup;
import org.fenixedu.treasury.services.accesscontrol.TreasuryAccessControlAPI;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.collect.Sets;

@GroupOperator("treasuryFrontOffice")
public class TreasuryFrontOfficeGroup extends CustomGroup {

    private static final long serialVersionUID = 1L;

    private static final TreasuryFrontOfficeGroup INSTANCE = new TreasuryFrontOfficeGroup();

    private TreasuryFrontOfficeGroup() {
        super();
    }

    public static TreasuryFrontOfficeGroup get() {
        return INSTANCE;
    }

    @Override
    public Stream<User> getMembers() {
        final java.util.Set<User> result = Sets.newHashSet();
        for (final String username : TreasuryAccessControlAPI.getFrontOfficeMemberUsernames()) {
            final User user = User.findByUsername(username);
            
            if(user != null) {
                result.add(user);
            }
        }
        
        return result.stream();
    }

    @Override
    public Stream<User> getMembers(final DateTime when) {
        return getMembers();
    }

    @Override
    public String getPresentationName() {
        return treasuryBundle("label.TreasuryFrontOfficeGroup.description");
    }

    @Override
    public boolean isMember(final User user) {
        return getMembers().anyMatch(u -> u == user);
    }

    @Override
    public boolean isMember(final User user, final DateTime when) {
        return getMembers().anyMatch(u -> u == user);
    }

    @Override
    public PersistentGroup toPersistentGroup() {
        return PersistentTreasuryFrontOfficeGroup.getInstance();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(TreasuryFrontOfficeGroup.class);
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof TreasuryFrontOfficeGroup;
    }

}
