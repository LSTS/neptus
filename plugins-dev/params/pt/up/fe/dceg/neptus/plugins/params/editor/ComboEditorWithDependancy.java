/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 18/02/2013
 */
package pt.up.fe.dceg.neptus.plugins.params.editor;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import pt.up.fe.dceg.neptus.gui.editor.ComboEditor;
import pt.up.fe.dceg.neptus.plugins.params.SystemProperty;
import pt.up.fe.dceg.neptus.plugins.params.editor.PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf;

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
//            System.out.println("-------------- 2");
            if(evt.getSource() instanceof SystemProperty) {
                SystemProperty sp = (SystemProperty) evt.getSource();
//                System.out.println("-------------- 3");
//                System.out.println(sp);
                if (sp.getValue() instanceof Number) {
                    for (int i = 0; i < pec.getValuesIfTests().size(); i++) {
                        PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<?, ?> vl = (ValuesIf<?, ?>) pec.getValuesIfTests().get(i);
                        PropertyEditorChangeValuesIfDependancyAdapter.ValuesIf<?, ?> vlI18n = (ValuesIf<?, ?>) pec.getValuesI18nIfTests().get(i);
//                        System.out.println("-------------- 4 " + i + "  " + vl.dependantParamId + " " + sp.getName());
                        if (!vl.dependantParamId.equals(sp.getName()))
                            continue;
//                        System.out.println("-------------- 5 " + i);
                        
                        if (vl.testValue.doubleValue() == ((Number)sp.getValue()).doubleValue()) {
//                            System.out.println("-------------- 6 " + i);
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