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
package org.fenixedu.treasury.domain.exceptions;


import java.util.Collection;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response.Status;

import org.fenixedu.bennu.core.domain.exceptions.DomainException;
import org.fenixedu.treasury.util.TreasuryConstants;

public class TreasuryDomainException extends DomainException {

    private static final long serialVersionUID = 1L;

    public TreasuryDomainException(String key, String... args) {
        super(TreasuryConstants.BUNDLE, key, args);
    }

    public TreasuryDomainException(Status status, String key, String... args) {
        super(status, TreasuryConstants.BUNDLE, key, args);
    }

    public TreasuryDomainException(Throwable cause, String key, String... args) {
        super(cause, TreasuryConstants.BUNDLE, key, args);
    }

    public TreasuryDomainException(Throwable cause, Status status, String key, String... args) {
        super(cause, status, TreasuryConstants.BUNDLE, key, args);
    }

    public static void throwWhenDeleteBlocked(Collection<String> blockers) {
        if (!blockers.isEmpty()) {
            throw new TreasuryDomainException("key.return.argument", blockers.stream().collect(Collectors.joining(", ")));
        }
    }
}
