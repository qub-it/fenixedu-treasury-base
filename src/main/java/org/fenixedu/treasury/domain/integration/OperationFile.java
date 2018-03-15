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
package org.fenixedu.treasury.domain.integration;

import org.fenixedu.bennu.io.domain.IGenericFile;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;

import pt.ist.fenixframework.Atomic;

public class OperationFile extends OperationFile_Base implements IGenericFile {

    public static final String CONTENT_TYPE = "application/octet-stream";
    
    public OperationFile() {
        super();
        // this.setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    public OperationFile(String fileName, byte[] content) {
        this();
        
        TreasuryPlataformDependentServicesFactory.implementation().createFile(this, fileName, CONTENT_TYPE, content);
    }

    @Override
    // TODO: Implement
    public boolean isAccessible(final String username) {
        throw new RuntimeException("not implemented");
    }

    private void checkRules() {
        //
        // CHANGE_ME add more busines validations
        //

        // CHANGE_ME In order to validate UNIQUE restrictions
    }

    @Atomic
    public void edit() {
        checkRules();
    }

    public boolean isDeletable() {
        return true;
    }

    @Override
    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.OperationFile.cannot.delete");
        }

        this.setLogIntegrationOperation(null);
        this.setIntegrationOperation(null);

        TreasuryPlataformDependentServicesFactory.implementation().deleteFile(this);
        deleteDomainObject();
    }

    @Atomic
    public static OperationFile create(String fileName, byte[] bytes, IntegrationOperation operation) {
    	
        OperationFile operationFile = new OperationFile();
        
        TreasuryPlataformDependentServicesFactory.implementation().createFile(operationFile, fileName, CONTENT_TYPE, bytes);

        return operationFile;
    }

    @Atomic
    public static OperationFile createLog(final String fileName, final byte[] bytes, final IntegrationOperation operation) {
        OperationFile operationFile = new OperationFile();
        
        TreasuryPlataformDependentServicesFactory.implementation().createFile(operationFile, fileName, CONTENT_TYPE, bytes);
        
        operationFile.setLogIntegrationOperation(operation);
        return operationFile;
    }

}
