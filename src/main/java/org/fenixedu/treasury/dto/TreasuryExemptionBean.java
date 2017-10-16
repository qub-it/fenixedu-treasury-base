package org.fenixedu.treasury.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

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
    private BigDecimal valuetoexempt;
    private String reason;

    public TreasuryExemptionBean() {
        this.setTreasuryExemptionTypes(TreasuryExemptionType.findAll().sorted(TreasuryExemptionType.COMPARE_BY_NAME)
                .collect(Collectors.toList()));
    }

    public TreasuryExemptionBean(TreasuryEvent treasuryEvent) {
        this();
        this.treasuryEvent = treasuryEvent;
        this.setDebitEntries(DebitEntry.findActive(treasuryEvent)
                .map(l -> new TreasuryTupleDataSourceBean(l.getExternalId(), l.getDescription()))
                .sorted(TreasuryTupleDataSourceBean.COMPARE_BY_TEXT).collect(Collectors.toList()));
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

    public BigDecimal getValuetoexempt() {
        return valuetoexempt;
    }

    public void setValuetoexempt(BigDecimal valuetoexempt) {
        this.valuetoexempt = valuetoexempt;
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
    
}
