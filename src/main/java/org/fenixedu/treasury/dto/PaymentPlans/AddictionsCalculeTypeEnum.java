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
package org.fenixedu.treasury.dto.PaymentPlans;

import org.fenixedu.bennu.core.i18n.BundleUtil;
import org.fenixedu.treasury.util.TreasuryConstants;

import java.util.List;

public enum AddictionsCalculeTypeEnum {
    BEFORE_DEBIT_ENTRY, BY_INSTALLMENT_ENTRY_AMOUNT, AFTER_DEBIT_ENTRY;

    public static List<AddictionsCalculeTypeEnum> getAddictionsCalculeTypesForInterest() {
        return List.of(BY_INSTALLMENT_ENTRY_AMOUNT, AFTER_DEBIT_ENTRY);
    }

    public static List<AddictionsCalculeTypeEnum> getAddictionsCalculeTypesForPenaltyTax() {
        return List.of(BEFORE_DEBIT_ENTRY, AFTER_DEBIT_ENTRY);
    }

    public String getName() {
        return BundleUtil.getString(TreasuryConstants.BUNDLE,
                "label.AddictionsCalculeTypeEnum." + name());
    }

    public boolean isBeforeDebitEntry() {
        return this.equals(BEFORE_DEBIT_ENTRY);
    }

    public boolean isByInstallmentEntryAmount() {
        return this.equals(BY_INSTALLMENT_ENTRY_AMOUNT);
    }

    public boolean isAfterDebitEntry() {
        return this.equals(AFTER_DEBIT_ENTRY);
    }
}
