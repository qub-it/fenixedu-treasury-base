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
package org.fenixedu.treasury.domain.document;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Stream;

import org.fenixedu.bennu.core.domain.Bennu;
import org.fenixedu.treasury.domain.PaymentMethod;
import org.fenixedu.treasury.domain.exceptions.TreasuryDomainException;
import org.fenixedu.treasury.util.Constants;

import pt.ist.fenixframework.Atomic;;

public class PaymentEntry extends PaymentEntry_Base {

    protected PaymentEntry() {
        super();
        setDomainRoot(pt.ist.fenixframework.FenixFramework.getDomainRoot());
    }

    protected PaymentEntry(final PaymentMethod paymentMethod, final SettlementNote settlementNote, 
            final BigDecimal payedAmount, final String paymentMethodId, final Map<String, String> propertiesMap) {
        this();
        init(paymentMethod, settlementNote, payedAmount, paymentMethodId, propertiesMap);
    }

    protected void init(final PaymentMethod paymentMethod, final SettlementNote settlementNote, 
            final BigDecimal payedAmount, final String paymentMethodId, final Map<String, String> propertiesMap) {
        setPaymentMethod(paymentMethod);
        setSettlementNote(settlementNote);
        setPayedAmount(payedAmount);
        setPaymentMethodId(paymentMethodId);
        setPropertiesJsonMap(Constants.propertiesMapToJson(propertiesMap));
        
        
        checkRules();
    }

    private void checkRules() {
        //
        //CHANGE_ME add more busines validations
        //
        if (getPaymentMethod() == null) {
            throw new TreasuryDomainException("error.PaymentEntry.paymentMethod.required");
        }

        if (getSettlementNote() == null) {
            throw new TreasuryDomainException("error.PaymentEntry.settlementNote.required");
        }

        //CHANGE_ME In order to validate UNIQUE restrictions
        //if (findByPaymentMethod(getPaymentMethod().count()>1)
        //{
        //  throw new TreasuryDomainException("error.PaymentEntry.paymentMethod.duplicated");
        //} 
        //if (findBySettlementNote(getSettlementNote().count()>1)
        //{
        //  throw new TreasuryDomainException("error.PaymentEntry.settlementNote.duplicated");
        //} 
        //if (findByPayedAmount(getPayedAmount().count()>1)
        //{
        //  throw new TreasuryDomainException("error.PaymentEntry.payedAmount.duplicated");
        //} 
    }

    @Atomic
    public void edit(final PaymentMethod paymentMethod, final SettlementNote settlementNote,
            final java.math.BigDecimal payedAmount, final String paymentMethodId) {
        setPaymentMethod(paymentMethod);
        setSettlementNote(settlementNote);
        setPayedAmount(payedAmount);
        setPaymentMethodId(paymentMethodId);
        checkRules();
    }

    public Map<String, String> getPropertiesMap() {
        return Constants.propertiesJsonToMap(getPropertiesJsonMap());
    }

    public boolean isDeletable() {
        return true;
    }

    @Atomic
    public void delete() {
        if (!isDeletable()) {
            throw new TreasuryDomainException("error.PaymentEntry.cannot.delete");
        }

        setDomainRoot(null);
        this.setPaymentMethod(null);

        deleteDomainObject();
    }

    @Atomic
    public static PaymentEntry create(final PaymentMethod paymentMethod, final SettlementNote settlementNote,
            final BigDecimal payedAmount, final String paymentMethodId, final Map<String, String> propertiesMap) {
        return new PaymentEntry(paymentMethod, settlementNote, payedAmount, paymentMethodId, propertiesMap);
    }

    // @formatter: off
    /************
     * SERVICES *
     ************/
    // @formatter: on

    public static Stream<PaymentEntry> findAll() {
        return pt.ist.fenixframework.FenixFramework.getDomainRoot().getPaymentEntriesSet().stream();
    }

    public static Stream<PaymentEntry> findByPaymentMethod(final PaymentMethod paymentMethod) {
        return paymentMethod.getPaymentEntriesSet().stream();
    }

    public static Stream<PaymentEntry> findBySettlementNote(final SettlementNote settlementNote) {
        return settlementNote.getPaymentEntriesSet().stream();
    }

    public static Stream<PaymentEntry> findByPayedAmount(final java.math.BigDecimal payedAmount) {
        return findAll().filter(i -> payedAmount.equals(i.getPayedAmount()));
    }

}
