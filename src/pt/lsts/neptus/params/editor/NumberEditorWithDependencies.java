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

import org.apache.commons.lang3.tuple.Pair;

import pt.lsts.neptus.gui.editor.NumberEditor;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.editor.PropertyEditorChangeValuesIfDependencyAdapter.ValuesIf;

/**
 * @author pdias
 *
 * @param <T>
 */
public class NumberEditorWithDependencies<T extends Number> extends NumberEditor<T> 
implements PropertyChangeListener {

    private PropertyEditorChangeValuesIfDependencyAdapter<?, T> pec = null;
    private ValuesIf<?, ?> activeTest = null;
    
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

    /* (non-Javadoc)
     * @see pt.lsts.neptus.gui.editor.NumberEditor#convertFromString(java.lang.String)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected T convertFromString(String txt) throws NumberFormatException {
        T ret = super.convertFromString(txt);

        if (pec == null || activeTest == null)
            return ret;

        T validValueIfFail = null;
        try {
            for (Object elm : activeTest.values) {
                if (elm instanceof Pair<?, ?>) {
                    Pair<T, T> validRange = (Pair<T, T>) elm;
                    if (ret.doubleValue() >= validRange.getLeft().doubleValue() &&
                            ret.doubleValue() <= validRange.getRight().doubleValue()) {
                        return ret;
                    }
                    else {
                        if (validValueIfFail == null)
                            validValueIfFail = validRange.getLeft();
                    }
                }
                else {
                    if (ret == (T) elm) {
                        return ret;
                    }
                    else {
                        if (validValueIfFail == null)
                            validValueIfFail = (T) elm;
                    }
                }
            }
        }
        catch (Exception e) {
            throw new NumberFormatException(e.getMessage());
        }
        
        return validValueIfFail != null ? validValueIfFail : ret;
    }
    
    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (pec == null || pec.valuesIfTests.isEmpty()) {
            activeTest = null;
            return;
        }

        boolean testVariablePresent = false;

        ValuesIf<?, ?> toChangeTest = null;
        if(evt.getSource() instanceof SystemProperty) {
            SystemProperty sp = (SystemProperty) evt.getSource();
            
            
            for (int i = 0; i < pec.getValuesIfTests().size(); i++) {
                ValuesIf<?, ?> vl = (ValuesIf<?, ?>) pec
                        .getValuesIfTests().get(i);
                
                if (!vl.dependantParamId.equals(sp.getName()))
                    continue;

                testVariablePresent = true;
                
                boolean isPassedTest = false;
                switch (vl.op) {
                    case EQUALS:
                        if (vl.testValue instanceof Number)
                            isPassedTest = ((Number) vl.testValue).doubleValue() == ((Number) sp.getValue()).doubleValue();
                        else if (vl.testValue instanceof Boolean)
                            isPassedTest = (Boolean) vl.testValue == (Boolean) sp.getValue();
                        else if (vl.testValue instanceof String)
                            isPassedTest = ((String) vl.testValue).equals((String) sp.getValue());
                        else
                            isPassedTest = vl.testValue.equals(sp.getValue());
                        break;
                }
                
                if (isPassedTest) {
                    if (vl.values.isEmpty())
                        continue;

                    toChangeTest = vl;
                    
                    break;
                }
            }
        }
        if (testVariablePresent)
            activeTest = toChangeTest;
    }
}
