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
package org.fenixedu.treasury.dto;

import java.util.Set;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.FinantialInstitution;
import org.fenixedu.treasury.domain.ProductGroup;
import org.fenixedu.treasury.domain.VatExemptionReason;
import org.fenixedu.treasury.domain.VatType;

import com.google.common.collect.Sets;

public class ProductBean {
    
    private ProductGroup productGroup;
    private String code;
    private LocalizedString description;
    private LocalizedString unitOfMeasure;
    private boolean active;
    private boolean legacy;
    private int tuitionInstallmentOrder;
    private VatType vatType;
    private VatExemptionReason vatExemptionReason;
    private Set<FinantialInstitution> finantialInstitutionsSet = Sets.newHashSet();
    
    public ProductBean() {
    }
    
    // @formatter:off
    /* *****************
     * GETTERS & SETTERS
     * *****************
     */
    // @formatter:on
    
    public ProductGroup getProductGroup() {
        return productGroup;
    }
    
    public void setProductGroup(ProductGroup productGroup) {
        this.productGroup = productGroup;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public LocalizedString getDescription() {
        return description;
    }
    
    public void setDescription(LocalizedString description) {
        this.description = description;
    }
    
    public LocalizedString getUnitOfMeasure() {
        return unitOfMeasure;
    }
    
    public void setUnitOfMeasure(LocalizedString unitOfMeasure) {
        this.unitOfMeasure = unitOfMeasure;
    }
    
    public boolean isActive() {
        return active;
    }
    
    public void setActive(boolean active) {
        this.active = active;
    }
    
    public boolean isLegacy() {
        return legacy;
    }
    
    public void setLegacy(boolean legacy) {
        this.legacy = legacy;
    }
    
    public int getTuitionInstallmentOrder() {
        return tuitionInstallmentOrder;
    }
    
    public void setTuitionInstallmentOrder(int tuitionInstallmentOrder) {
        this.tuitionInstallmentOrder = tuitionInstallmentOrder;
    }
    
    public VatType getVatType() {
        return vatType;
    }
    
    public void setVatType(VatType vatType) {
        this.vatType = vatType;
    }
    
    public VatExemptionReason getVatExemptionReason() {
        return vatExemptionReason;
    }
    
    public void setVatExemptionReason(VatExemptionReason vatExemptionReason) {
        this.vatExemptionReason = vatExemptionReason;
    }
    
    public Set<FinantialInstitution> getFinantialInstitutionsSet() {
        return finantialInstitutionsSet;
    }
    
    public void setFinantialInstitutionsSet(Set<FinantialInstitution> finantialInstitutionsSet) {
        this.finantialInstitutionsSet = finantialInstitutionsSet;
    }
}
