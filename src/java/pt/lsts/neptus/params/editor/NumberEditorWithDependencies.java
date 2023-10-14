/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: pdias
 * 29/03/2017
 */
package pt.lsts.neptus.params.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashMap;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.editor.NumberEditor;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.editor.PropertyEditorChangeValuesIfDependencyAdapter.ValuesIf;

/**
 * Numerical editor to be used if property has dependencies.
 * 
 * @author pdias
 *
 * @param <T>
 */
public class NumberEditorWithDependencies<T extends Number> extends NumberEditor<T> 
implements PropertyChangeListener {

    private LinkedHashMap<String, Object> dependencyVariables = new LinkedHashMap<>();
    private PropertyEditorChangeValuesIfDependencyAdapter<?, T> pec = null;
    
    /**
     * @param type
     * @param minValue
     * @param maxValue
     */
    public NumberEditorWithDependencies(Class<T> type, T minValue, T maxValue) {
        super(type, minValue, maxValue);
    }

    public NumberEditorWithDependencies(Class<T> type, T minValue, T maxValue,
            PropertyEditorChangeValuesIfDependencyAdapter<?, T> pec) {
        super(type, minValue, maxValue);
        this.pec = pec;
    }

    /**
     * @param type
     */
    public NumberEditorWithDependencies(Class<T> type) {
        super(type);
    }

    public NumberEditorWithDependencies(Class<T> type,
            PropertyEditorChangeValuesIfDependencyAdapter<?, T> pec) {
        super(type);
        this.pec = pec;
    }

    private void updateDependenciesVariables() {
        if (pec == null || pec.valuesIfTests.isEmpty()) {
            dependencyVariables.clear();
        }
        else {
            for (ValuesIf<?, T> vif : pec.valuesIfTests) {
                if (!dependencyVariables.containsKey(vif.dependantParamId))
                    dependencyVariables.put(vif.dependantParamId, null);
            }
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.editor.NumberEditor#convertFromString(java.lang.String)
     */
    @Override
    protected T convertFromString(String txt) throws NumberFormatException {
        T ret = super.convertFromString(txt);
        return ret;
    }
    
    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (pec == null || pec.valuesIfTests.isEmpty()) {
            clearAdmisibleRangeValues();
            return;
        }

        updateDependenciesVariables();
        
        boolean testVariablePresent = false;
        boolean passedAtLeastOneTest = false;

        ValuesIf<?, ?> toChangeTest = null;
        if(evt.getSource() instanceof SystemProperty) {
            SystemProperty sp = (SystemProperty) evt.getSource();
            
            if (dependencyVariables.containsKey(sp.getName()))
                dependencyVariables.put(sp.getName(), sp.getValue());
            else
                return;

            for (String testVarKey : dependencyVariables.keySet()) {
                Object testVarValue = dependencyVariables.get(testVarKey);
                if (testVarValue == null)
                    continue;
                
                
                for (int i = 0; i < pec.getValuesIfTests().size(); i++) {
                    ValuesIf<?, ?> vl = (ValuesIf<?, ?>) pec.getValuesIfTests().get(i);
                    
                    if (!vl.dependantParamId.equals(testVarKey))
                        continue;
                    
                    testVariablePresent = true;
                    boolean isPassedTest = false;
                    
                    try {
                        switch (vl.op) {
                            case EQUALS:
                                if (vl.testValue instanceof Number)
                                    isPassedTest = ((Number) vl.testValue).doubleValue() == ((Number) testVarValue).doubleValue();
                                else if (vl.testValue instanceof Boolean)
                                    isPassedTest = (Boolean) vl.testValue == (Boolean) testVarValue;
                                else if (vl.testValue instanceof String)
                                    isPassedTest = ((String) vl.testValue).equals((String) testVarValue);
                                else
                                    isPassedTest = vl.testValue.equals(testVarValue);
                                break;
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub()
                                .warn("Problem while evaluating test variable " + testVarKey + ": " + e.getMessage());
                    }
                    
                    if (isPassedTest) {
                        if (vl.values.isEmpty())
                            continue;
                        
                        passedAtLeastOneTest = true;
                        toChangeTest = vl;
                        
                        break;
                    }
                }
                
                if(passedAtLeastOneTest)
                    break;
            }
        }
        
        if (testVariablePresent && passedAtLeastOneTest) {
            clearAdmisibleRangeValues();
            addToAdmisibleRangeValues(toChangeTest.values);
        }
        if (testVariablePresent && !passedAtLeastOneTest) {
            clearAdmisibleRangeValues();
        }
    }
}
