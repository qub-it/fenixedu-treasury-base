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

import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.TreasuryConstants;

import com.google.gson.JsonObject;

public class TreasuryDomainException extends RuntimeException {

    // Bennu DomainException
    private final String key;

    private final String[] args;

    private final String bundle;

    private final Status status;

    protected TreasuryDomainException(String bundle, String key, String... args) {
        this(Status.PRECONDITION_FAILED, bundle, key, args);
    }

    protected TreasuryDomainException(Status status, String bundle, String key, String... args) {
        super(key);
        this.status = status;
        this.bundle = bundle;
        this.key = key;
        this.args = args;
    }

    protected TreasuryDomainException(Throwable cause, String bundle, String key, String... args) {
        this(cause, Status.INTERNAL_SERVER_ERROR, bundle, key, args);
    }

    protected TreasuryDomainException(Throwable cause, Status status, String bundle, String key, String... args) {
        super(key, cause);
        this.status = status;
        this.bundle = bundle;
        this.key = key;
        this.args = args;
    }

    public String getLocalizedMessage() {
        return TreasuryPlataformDependentServicesFactory.implementation().bundle(this.bundle, this.key, this.args);
    }

    public Status getResponseStatus() {
        return this.status;
    }

    public JsonObject asJson() {
        JsonObject json = new JsonObject();
        json.addProperty("message", getLocalizedMessage());
        return json;
    }

    public String getKey() {
        return this.key;
    }

    public String[] getArgs() {
        return this.args;
    }
    
    // TreasuryDomainException
    
    private static final long serialVersionUID = 1L;

    public TreasuryDomainException(String key, String... args) {
        this(TreasuryConstants.BUNDLE, key, args);
    }

    public TreasuryDomainException(Status status, String key, String... args) {
        this(status, TreasuryConstants.BUNDLE, key, args);
    }

    public TreasuryDomainException(Throwable cause, String key, String... args) {
        this(cause, TreasuryConstants.BUNDLE, key, args);
    }

    public TreasuryDomainException(Throwable cause, Status status, String key, String... args) {
        this(cause, status, TreasuryConstants.BUNDLE, key, args);
    }

    public static void throwWhenDeleteBlocked(Collection<String> blockers) {
        if (!blockers.isEmpty()) {
            throw new TreasuryDomainException("key.return.argument", new String[] { blockers.stream().collect(Collectors.joining(", ")) });
        }
    }
}
