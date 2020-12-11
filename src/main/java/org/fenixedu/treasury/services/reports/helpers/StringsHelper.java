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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.fenixedu.commons.StringNormalizer;
import org.fenixedu.commons.i18n.I18N;
import org.fenixedu.commons.i18n.LocalizedString;

import com.qubit.terra.docs.util.helpers.IDocumentHelper;

public class StringsHelper extends StringUtils implements IDocumentHelper {

    static public String SINGLE_SPACE = " ";

    public static String trimToEmpty(final Object input) {
        return input == null ? EMPTY : trimToEmpty(input.toString());
    }

    public static String trimToNull(final Object input) {
        return input == null ? null : trimToNull(input.toString());
    }

    public static boolean equalsTrimmed(final String o1, final String o2) {
        return equals(trimToEmpty(o1), trimToEmpty(o2));
    }

    public static boolean equalsTrimmedIgnoreCase(final String o1, final String o2) {
        return equalsIgnoreCase(trimToEmpty(o1), trimToEmpty(o2));
    }

    public static boolean equals(final LocalizedString o1, final LocalizedString o2, final Locale locale) {
        boolean result = o1 != null && o2 != null;

        if (result) {
            result = equals(normalize(o1.getContent(locale)), normalize(o2.getContent(locale)));
        }

        return result;
    }

    public static String normalize(String string) {
        String result = null;

        if (StringUtils.isNotBlank(string)) {
            String spacesReplacedString = removeDuplicateSpaces(string.trim());
            result = StringNormalizer.normalize(spacesReplacedString).toLowerCase();
        }

        return result;
    }

    protected static String removeDuplicateSpaces(String string) {
        Pattern pattern = Pattern.compile("\\s+");
        Matcher matcher = pattern.matcher(string);
        return matcher.replaceAll(" ");
    }

    public static LocalizedString getI18N(final String defaultContent, final String english) {
        LocalizedString result = new LocalizedString();

        if (StringUtils.isNotBlank(defaultContent)) {
            result = result.with(I18N.getLocale(), defaultContent.trim());
        }
        if (StringUtils.isNotBlank(english)) {
            result = result.with(new Locale("en", "GB"), english.trim());
        }

        return result;
    }

    public static LocalizedString capitalize(final LocalizedString i18nString) {
        LocalizedString result = new LocalizedString();
        for (Locale locale : i18nString.getLocales()) {
            result = result.with(locale, StringUtils.capitalize(i18nString.getContent(locale)));
        }

        return result;
    }

    public static LocalizedString joinLS(final Collection<LocalizedString> collection, final String separator) {
        Set<Locale> locales = new HashSet<Locale>();
        for (LocalizedString iter : collection) {
            locales.addAll(iter.getLocales());
        }

        LocalizedString result = new LocalizedString();
        for (Locale locale : locales) {
            Collection<String> messages = new ArrayList<String>();
            for (LocalizedString i18nString : collection) {
                messages.add(i18nString.getContent(locale));
            }

            result = result.with(locale, join(messages, separator));
        }

        return result;
    }

    public static boolean isEmpty(LocalizedString i18nString) {

        if (i18nString == null || i18nString.isEmpty()) {
            return true;
        }

        if (i18nString.getLocales().isEmpty()) {
            return true;
        }

        for (final Locale locale : i18nString.getLocales()) {
            if (StringUtils.isNotEmpty(i18nString.getContent(locale))) {
                return false;
            }
        }

        return true;

    }

/**
     * Allows variables replacement with configurable prefix and suffix
     * 
     * Usage example using '<' for prefix and '>' for suffix:
     *  - Template: Hello <name>
     *  - Variables: {name=User}
     *  - Result: Hello User
     * @return
     */
    public static String replaceVariables(final String template, final String prefix, final String suffix,
            final Map<String, String> variables) {
        return new StrSubstitutor(variables, prefix, suffix).replace(template);
    }

}