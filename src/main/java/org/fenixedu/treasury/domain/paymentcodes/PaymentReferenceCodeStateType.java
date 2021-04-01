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
package org.fenixedu.treasury.domain.paymentcodes;

import static org.fenixedu.treasury.util.TreasuryConstants.treasuryBundleI18N;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.domain.payments.integration.IPaymentRequestState;

public enum PaymentReferenceCodeStateType implements IPaymentRequestState {
    UNUSED, USED, ANNULLED, PROCESSED;

    public boolean isUnused() {
        return this == UNUSED;
    }

    public boolean isUsed() {
        return this == USED;
    }

    public boolean isAnnuled() {
        return this == ANNULLED;
    }

    public boolean isProcessed() {
        return this == PROCESSED;
    }

    public LocalizedString getDescriptionI18N() {
        return treasuryBundleI18N(getClass().getSimpleName() + "." + name());
    }

    @Override
    public String getCode() {
        return name();
    }

    @Override
    public LocalizedString getLocalizedName() {
        return getDescriptionI18N();
    }
}
