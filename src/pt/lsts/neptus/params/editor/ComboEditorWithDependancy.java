/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import pt.lsts.neptus.gui.editor.ComboEditor;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.editor.PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf;

public class ComboEditorWithDependancy<T extends Object> extends ComboEditor<T> implements PropertyChangeListener {

        private PropertyEditorChangeValuesIfDependancyAdapter<?, ?> pec;
        
        /**
         * @param options
         * @param pec
         */
        public ComboEditorWithDependancy(T[] options, PropertyEditorChangeValuesIfDependancyAdapter<?, ?> pec) {
            this(options, null, pec);
        }

        /**
         * @param options
         * @param stringValues The String to show in the combo.
         * @param pec
         */
        public ComboEditorWithDependancy(T[] options, String[] stringValues, PropertyEditorChangeValuesIfDependancyAdapter<?, ?> pec) {
            super(options, stringValues);
            this.pec = pec;
        }

        /* (non-Javadoc)
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        @SuppressWarnings("unchecked")
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
//            NeptusLog.pub().info("<###>-------------- 2");
            if(evt.getSource() instanceof SystemProperty) {
                SystemProperty sp = (SystemProperty) evt.getSource();
//                NeptusLog.pub().info("<###>-------------- 3");
//                NeptusLog.pub().info("<###> "+sp);
                if (sp.getValue() instanceof Number) {
                    for (int i = 0; i < pec.getValuesIfTests().size(); i++) {
                        PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<?, ?> vl = (ValuesIf<?, ?>) pec.getValuesIfTests().get(i);
                        PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<?, ?> vlI18n = (ValuesIf<?, ?>) pec.getValuesI18nIfTests().get(i);
//                        NeptusLog.pub().info("<###>-------------- 4 " + i + "  " + vl.dependantParamId + " " + sp.getName());
                        if (!vl.dependantParamId.equals(sp.getName()))
                            continue;
//                        NeptusLog.pub().info("<###>-------------- 5 " + i);
                        
                        if (vl.testValue.doubleValue() == ((Number)sp.getValue()).doubleValue()) {
//                            NeptusLog.pub().info("<###>-------------- 6 " + i);
                            combo.removeAllItems();
                            for (Object item : vl.values)
                                combo.addItem((T) item);
                            stringValues.clear();
                            if (vlI18n != null && vlI18n.values != null) {
                                for (Object item : vlI18n.values)
                                    stringValues.add(item.toString());
                            }
                            break;
                        }
                    }
                }
            }
        }
    }