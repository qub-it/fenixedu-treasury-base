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

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.fenixedu.treasury.dto.ITreasuryBean;
import org.fenixedu.treasury.dto.TreasuryTupleDataSourceBean;
import org.fenixedu.treasury.domain.Product;
import org.fenixedu.treasury.domain.document.DebitEntry;
import org.fenixedu.treasury.domain.event.TreasuryEvent;
import org.fenixedu.treasury.domain.exemption.TreasuryExemptionType;

public class TreasuryExemptionBean implements ITreasuryBean, Serializable {

    private static final long serialVersionUID = 1L;
    private TreasuryEvent treasuryEvent;

    private List<TreasuryTupleDataSourceBean> treasuryExemptionTypes;
    private List<TreasuryTupleDataSourceBean> products;
    private List<TreasuryTupleDataSourceBean> debitEntries;
    private TreasuryExemptionType treasuryExemptionType;
    private DebitEntry debitEntry;
    private BigDecimal netAmountToExempt;
    private String reason;
    private String currencySymbol;

    public TreasuryExemptionBean() {
        this.setTreasuryExemptionTypes(TreasuryExemptionType.findAll().sorted(TreasuryExemptionType.COMPARE_BY_NAME)
                .collect(Collectors.toList()));
    }

    public TreasuryExemptionBean(TreasuryEvent treasuryEvent) {
        this();
        this.treasuryEvent = treasuryEvent;
        this.setDebitEntries(DebitEntry.findActive(treasuryEvent)
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), debitEntryDescription(l)))
                .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList()));
    }

    private String debitEntryDescription(final DebitEntry debitEntry) {
        if(debitEntry.getFinantialDocument() == null) {
            return debitEntry.getDescription();
        }
        
        return String.format("%s [%s]", debitEntry.getDescription(), debitEntry.getFinantialDocument().getUiDocumentNumber());
    }

    public List<TreasuryTupleDataSourceBean> getTreasuryExemptionTypes() {
        return treasuryExemptionTypes;
    }

    public TreasuryEvent getTreasuryEvent() {
        return treasuryEvent;
    }

    public void setTreasuryEvent(TreasuryEvent treasuryEvent) {
        this.treasuryEvent = treasuryEvent;
    }

    public void setTreasuryExemptionTypes(List<TreasuryExemptionType> treasuryExemptionTypes) {
        this.treasuryExemptionTypes = treasuryExemptionTypes.stream().map(treasuryExemptionType -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setText(treasuryExemptionType.getName().getContent());
            tuple.setId(treasuryExemptionType.getExternalId());
            return tuple;
        }).collect(Collectors.toList());
    }

    public List<TreasuryTupleDataSourceBean> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products.stream().map(product -> {
            TreasuryTupleDataSourceBean tuple = new TreasuryTupleDataSourceBean();
            tuple.setText(product.getName().getContent());
            tuple.setId(product.getExternalId());
            return tuple;
        }).collect(Collectors.toList());
    }

    public TreasuryExemptionType getTreasuryExemptionType() {
        return treasuryExemptionType;
    }

    public void setTreasuryExemptionType(TreasuryExemptionType treasuryExemptionType) {
        this.treasuryExemptionType = treasuryExemptionType;
    }

    public List<TreasuryTupleDataSourceBean> getDebitEntries() {
        return debitEntries;
    }

    public void setDebitEntries(List<TreasuryTupleDataSourceBean> debitEntries) {
        this.debitEntries = debitEntries;
    }

    public BigDecimal getNetAmountToExempt() {
        return this.netAmountToExempt;
    }

    public void setNetAmountToExempt(BigDecimal netAmountToExempt) {
        this.netAmountToExempt = netAmountToExempt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public DebitEntry getDebitEntry() {
        return debitEntry;
    }
    
    public void setDebitEntry(DebitEntry debitEntry) {
        this.debitEntry = debitEntry;
    }
    
    public String getCurrencySymbol() {
        return currencySymbol;
    }
    
    public void setCurrencySymbol(String currencySymbol) {
        this.currencySymbol = currencySymbol;
    }
    
}
