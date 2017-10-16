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
package org.fenixedu.treasury.dto;

import java.util.Comparator;

import org.fenixedu.commons.StringNormalizer;

public class TreasuryTupleDataSourceBean implements ITreasuryBean {

    public static final Comparator<TreasuryTupleDataSourceBean> COMPARE_BY_TEXT = new Comparator<TreasuryTupleDataSourceBean>() {

        @Override
        public int compare(final TreasuryTupleDataSourceBean o1, final TreasuryTupleDataSourceBean o2) {
            int c = o1.getText().compareTo(o2.getText());
            return c != 0 ? c : o1.getId().compareTo(o2.getId());
        }
    };

    private String id;
    private String text;
    private String normalizedText;

    public TreasuryTupleDataSourceBean() {

    }

    public TreasuryTupleDataSourceBean(final String id, final String text) {
        this.id = id;
        this.text = text;
        this.normalizedText = StringNormalizer.normalize(text);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.normalizedText = StringNormalizer.normalize(text);
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public void setNormalizedText(String normalizedText) {
        this.normalizedText = normalizedText;
    }
}
