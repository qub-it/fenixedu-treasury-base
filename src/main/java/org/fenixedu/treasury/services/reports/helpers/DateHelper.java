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
package org.fenixedu.treasury.services.reports.helpers;

import java.util.Locale;

import org.fenixedu.commons.i18n.LocalizedString;
import org.fenixedu.treasury.services.integration.ITreasuryPlatformDependentServices;
import org.fenixedu.treasury.services.integration.TreasuryPlataformDependentServicesFactory;
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
        ITreasuryPlatformDependentServices services = TreasuryPlataformDependentServicesFactory.implementation();
        
        LocalizedString i18NString = new LocalizedString();
        for (Locale locale : services.availableLocales()) {
            String month = localDate.toString("MMMM", locale);
            if (locale.getLanguage().equals("pt")) {
                month = month.toLowerCase(); // Java does not follow the Portuguese Language Orthographic Agreement of 1990
            }
            String message =
                    services.bundle(locale, "resources.FenixeduQubdocsReportsResources", "message.DateHelper.extendedDate",
                            localDate.toString("dd", locale), month, localDate.toString("yyyy", locale));
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
