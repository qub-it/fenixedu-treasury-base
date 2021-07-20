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
package org.fenixedu.treasury.util;

import java.util.List;

import org.fenixedu.treasury.domain.Customer;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import eu.europa.ec.taxud.tin.algorithm.TINValid;

public class FiscalCodeValidation {

    private static final List<String> VALIDATED_COUNTRIES = Lists.newArrayList(
            "PT", "AT", "BE", "BG", "CY", "DE", "DK", "EE", "ES", "FI", 
            "FR", "HR", "HU", "IE", "IT", "LT", "LU", "NL", "PL", "SE",
            "SI", "GB");
    
    public static boolean isValidationAppliedToFiscalCountry(final String countryCode) {
        return VALIDATED_COUNTRIES.contains(countryCode.toUpperCase());
    }
    
    public static boolean isValidFiscalNumber(final String countryCode, final String fiscalNumber) {
        if (Strings.isNullOrEmpty(countryCode)) {
            return false;
        }

        if (Strings.isNullOrEmpty(fiscalNumber)) {
            return false;
        }

        if (!TreasuryConstants.isDefaultCountry(countryCode) && Customer.DEFAULT_FISCAL_NUMBER.equals(fiscalNumber)) {
            return false;
        }
        
        if(VALIDATED_COUNTRIES.contains(countryCode.toUpperCase())) {
            int checkTIN = TINValid.checkTIN(translateCountry(countryCode.toUpperCase()), fiscalNumber);
            
            return checkTIN == 0 || checkTIN == 2;
        }

        return true;
    }

    private static String translateCountry(String countryCode) {
        if("GB".equals(countryCode)) {
            return "UK";
        }
        
        return countryCode;
    }
    
}
