/**
 * This file was created by Quorum Born IT <http://www.qub-it.com/> and its 
 * copyright terms are bind to the legal agreement regulating the FenixEdu@ULisboa 
 * software development project between Quorum Born IT and Serviços Partilhados da
 * Universidade de Lisboa:
 *  - Copyright © 2015 Quorum Born IT (until any Go-Live phase)
 *  - Copyright © 2015 Universidade de Lisboa (after any Go-Live phase)
 *
 * Contributors: anil.mamede@qub-it.com
 *
 * 
 * This file is part of FenixEdu QubDocs.
 *
 * FenixEdu QubDocs is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FenixEdu QubDocs is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with FenixEdu QubDocs.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.fenixedu.treasury.services.reports.helpers;

import java.util.Locale;


import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
import org.fenixedu.treasury.util.Constants;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.Partial;
import org.joda.time.YearMonthDay;

import com.qubit.terra.docs.util.helpers.IDocumentHelper;

public class DateHelper implements IDocumentHelper {

    Locale pt = new Locale("pt");

    public String numericDate(final LocalDate localDate) {
        return localDate.toString("dd/MM/yyyy");
    }

    public String numericDate(final YearMonthDay yearMonthDay) {
        return numericDate(yearMonthDay.toLocalDate());
    }

    public String numericDateTime(final DateTime dateTime) {
        return dateTime.toString("dd/MM/yyyy HH:mm");
    }

    public LocalizedString extendedDate(final LocalDate localDate) {
    	
        LocalizedString i18NString = new LocalizedString();
        for (Locale locale : TreasuryPlataformDependentServicesFactory.implementation().availableLocales()) {
            String month = localDate.toString("MMMM", locale);
            if (locale.getLanguage().equals("pt")) {
                month = month.toLowerCase(); // Java does not follow the Portuguese Language Orthographic Agreement of 1990
            }
            String message =
                    Constants.bundleI18N("message.DateHelper.extendedDate",
                            localDate.toString("dd", locale), month, localDate.toString("yyyy", locale)).getContent(locale);
            
            i18NString = i18NString.with(locale, message);
        }
        return i18NString;
    }

    public LocalizedString extendedDate(final YearMonthDay yearMonthDay) {
        return extendedDate(yearMonthDay.toLocalDate());
    }

    public String monthYear(final Partial partial) {
        return partial.toString("MM/yyyy");
    }

    public String monthYear(final LocalDate localDate) {
        return localDate.toString("MM/yyyy");
    }

    public String date(final DateTime dateTime) {
        return dateTime.toString("dd/MM/yyyy");
    }

    public String date(final LocalDate localDate) {
        return localDate.toString("dd/MM/yyyy");
    }

    public String date(final YearMonthDay yearMonthDay) {
        return yearMonthDay.toString("dd/MM/yyyy");
    }

}