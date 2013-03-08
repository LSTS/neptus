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
 * $Id:: PropertyEditorChangeValuesIfDependancyAdapter.java 10068 2013-03-05 03#$:
 */
package pt.up.fe.dceg.neptus.plugins.params.editor;

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