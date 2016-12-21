/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 18/02/2013
 */
package pt.lsts.neptus.params.editor;

import java.util.ArrayList;

public class PropertyEditorChangeValuesIfDependancyAdapter<T extends Number, E extends Object> {
    public enum TestOperation { EQUALS };

    protected final ArrayList<PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<T, E>> valuesIfTests = new ArrayList<>();
    protected final ArrayList<PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<T, E>> valuesI18nIfTests = new ArrayList<>();

    public PropertyEditorChangeValuesIfDependancyAdapter() {
    }

    public void addValuesIf(String dependantParamId, T testValue,
            PropertyEditorChangeValuesIfDependancyAdapter.TestOperation op, ArrayList<E> values) {
        addValuesIf(dependantParamId, testValue, op, values, null);
    }

    public void addValuesIf(String dependantParamId, T testValue,
            PropertyEditorChangeValuesIfDependancyAdapter.TestOperation op, ArrayList<E> values, ArrayList<E> valuesI18n) {
        PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<T, E> tt = new PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<>(
                dependantParamId, testValue, op, values);
        valuesIfTests.add(tt);

        if (valuesI18n != null && values.size() == valuesI18n.size()) {
            valuesI18n = null;
        }
        PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<T, E> ttI18n = new PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<>(
                dependantParamId, testValue, op, valuesI18n);
        valuesI18nIfTests.add(ttI18n);
    }
    
    /**
     * @return the valuesIfTests
     */
    public ArrayList<PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<T, E>> getValuesIfTests() {
        return valuesIfTests;
    }
    
    /**
     * @return the valuesI18nIfTests
     */
    public ArrayList<PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<T, E>> getValuesI18nIfTests() {
        return valuesI18nIfTests;
    }
    
    public static class ValuesIf<T extends Number, E extends Object> {
        public String dependantParamId;
        public T testValue;
        public PropertyEditorChangeValuesIfDependancyAdapter.TestOperation op;
        public ArrayList<E> values;
        
        public ValuesIf(String dependantParamId, T testValue, PropertyEditorChangeValuesIfDependancyAdapter.TestOperation op, ArrayList<E> values) {
            this.dependantParamId = dependantParamId;
            this.testValue = testValue;
            this.op = op;
            this.values = values;
        }
    }
}