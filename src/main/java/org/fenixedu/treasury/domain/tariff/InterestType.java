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
package org.fenixedu.treasury.domain.tariff;

import java.util.ArrayList;
import java.util.List;


import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.util.Constants;

public enum InterestType {

    DAILY, MONTHLY, GLOBAL_RATE, FIXED_AMOUNT;

    public boolean isDaily() {
        return this == DAILY;
    }

    public boolean isGlobalRate() {
        return this == GLOBAL_RATE;
    }

    public boolean isMonthly() {
        return this == MONTHLY;
    }

    public boolean isFixedAmount() {
        return this == FIXED_AMOUNT;
    }

    public LocalizedString getDescriptionI18N() {
        return Constants.bundleI18N(getClass().getSimpleName() + "." + name());
    }

    public static List<InterestType> findAll() {
        List<InterestType> asList = new ArrayList<InterestType>();
        asList.add(GLOBAL_RATE);
        asList.add(FIXED_AMOUNT);
        return asList;
    }
}