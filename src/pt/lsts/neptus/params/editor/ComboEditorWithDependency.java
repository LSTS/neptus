/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Paulo Dias
 * 18/02/2013
 */
package pt.lsts.neptus.params.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;

import pt.lsts.neptus.gui.editor.ComboEditor;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.editor.PropertyEditorChangeValuesIfDependencyAdapter.ValuesIf;

public class ComboEditorWithDependency<T extends Object> extends ComboEditor<T> implements PropertyChangeListener {

    private LinkedHashMap<Object, Object> dependencyVariables = new LinkedHashMap<>();
    private PropertyEditorChangeValuesIfDependencyAdapter<?, ?> pec;
        
        /**
         * @param options
         * @param pec
         */
        public ComboEditorWithDependency(T[] options, PropertyEditorChangeValuesIfDependencyAdapter<?, ?> pec) {
            this(options, null, pec);
        }

        /**
         * @param options
         * @param stringValues The String to show in the combo.
         * @param pec
         */
        public ComboEditorWithDependency(T[] options, String[] stringValues, PropertyEditorChangeValuesIfDependencyAdapter<?, ?> pec) {
            super(options, stringValues);
            this.pec = pec;
        }
        
        private void updateDependenciesVariables() {
            if (pec == null || pec.valuesIfTests.isEmpty()) {
                dependencyVariables.clear();
            }
            else {
                for (ValuesIf<?, ?> vif : pec.valuesIfTests) {
                    if (!dependencyVariables.containsKey(vif.dependantParamId))
                        dependencyVariables.put(vif.dependantParamId, null);
                }
            }
        }

        /* (non-Javadoc)
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        @SuppressWarnings("unchecked")
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (pec == null || pec.valuesIfTests.isEmpty())
                return;

            updateDependenciesVariables();
            
            if(evt.getSource() instanceof SystemProperty) {
                SystemProperty sp = (SystemProperty) evt.getSource();
                
                if (dependencyVariables.containsKey(sp.getName()))
                    dependencyVariables.put(sp.getName(), sp.getValue());
                else
                    return;

                boolean found = false;
                
                for (Object testVarKey : dependencyVariables.keySet()) {
                    Object testVarValue = dependencyVariables.get(testVarKey);
                    if (testVarValue == null)
                        continue;
                    
                    for (int i = 0; i < pec.getValuesIfTests().size(); i++) {
                        PropertyEditorChangeValuesIfDependencyAdapter.ValuesIf<?, ?> vl = (ValuesIf<?, ?>) pec.getValuesIfTests().get(i);
                        PropertyEditorChangeValuesIfDependencyAdapter.ValuesIf<?, ?> vlI18n = (ValuesIf<?, ?>) pec.getValuesI18nIfTests().get(i);
                        if (!vl.dependantParamId.equals(testVarKey))
                            continue;
                        
                        boolean isEquals = false;
                        if (vl.testValue instanceof Number)
                            isEquals = ((Number) vl.testValue).doubleValue() == ((Number) testVarValue).doubleValue();
                        else if (vl.testValue instanceof Boolean)
                            isEquals = (Boolean) vl.testValue == (Boolean) testVarValue;
                        else if (vl.testValue instanceof String)
                            isEquals = ((String) vl.testValue).equals((String) testVarValue);
                        else
                            isEquals = vl.testValue.equals(testVarValue);
                        
                        if (isEquals) {
                            combo.removeAllItems();
                            for (Object item : vl.values)
                                combo.addItem((T) item);
                            stringValues.clear();
                            if (vlI18n != null && vlI18n.values != null) {
                                for (Object item : vlI18n.values)
                                    stringValues.add(item.toString());
                            }
                            found = true;
                            break;
                        }
                        
                        if (found)
                            break;
                    }
                }
            }
        }
    }