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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Correia
 * Jan 28, 2013
 */
package pt.lsts.neptus.params;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.editor.ArrayListEditor;
import pt.lsts.neptus.gui.editor.ComboEditor;
import pt.lsts.neptus.gui.editor.NumberEditor;
import pt.lsts.neptus.gui.editor.StringPatternEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.ValueTypeEnum;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.params.editor.ComboEditorWithDependancy;
import pt.lsts.neptus.params.editor.PropertyEditorChangeValuesIfDependancyAdapter;
import pt.lsts.neptus.params.editor.custom.CustomSystemPropertyEditor;
import pt.lsts.neptus.params.renderer.BooleanSystemPropertyRenderer;
import pt.lsts.neptus.params.renderer.I18nSystemPropertyRenderer;
import pt.lsts.neptus.params.renderer.SystemPropertyRenderer;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import com.l2fprod.common.beans.editor.AbstractPropertyEditor;
import com.l2fprod.common.beans.editor.BooleanAsCheckBoxPropertyEditor;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

/**
 * @author pdias
 * @author jqcorreia
 */
public class ConfigurationManager {

    public static final String CONF_DIR = "conf/params/";

    private HashMap<String, HashMap<String, SystemProperty>> map = new LinkedHashMap<String, HashMap<String, SystemProperty>>();
    private List<String> sections = new ArrayList<String>();
    private Document doc;

    private static ConfigurationManager instance = null;
    private static boolean loading = false;

    private ConfigurationManager() {
        loadConfigurations();
    }

    public static ConfigurationManager getInstance() {
        if (instance == null) {
            if (!loading) {
                loading = true;
                instance = new ConfigurationManager();
                loading = false;
            }
        }
        long timeS = System.currentTimeMillis();
        while (loading) {
            try {
                Thread.sleep(100);
                if (System.currentTimeMillis() - timeS > 3000) {
                    timeS = System.currentTimeMillis();
                    NeptusLog.pub().warn("Waiting for parameters to be loaded...");
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return instance;
    }

    private void loadConfigurations() {
        String lang = GeneralPreferences.language;

        File fx = new File(CONF_DIR);
        if (fx.exists()) {
            for(File f : fx.listFiles()) {
                if (!f.isFile())
                    continue;
                String fname = f.getName();
                String fext = FileUtil.getFileExtension(fname);
                if (!fname.replaceAll("." + fext + "$", "").endsWith(lang))
                    continue;

                NeptusLog.pub().debug("Loading vehicle configuration from " + f.getName());
                String systemName = f.getName().split("\\.")[0];
                try {
                    map.put(systemName, readConfiguration(f));
                } 
                catch (InvalidConfigurationException e) {
                    NeptusLog.pub().error(e.getMessage());
                    e.printStackTrace();
               }
            }
        }
    }

    private String getTagContents(Element root, String name) {
        Element node = (Element)root.selectSingleNode(name);
        if (node == null)
            return null;

        return node.getStringValue();
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, SystemProperty> readConfiguration(File file) throws InvalidConfigurationException {
        LinkedHashMap<String, SystemProperty> params = new LinkedHashMap<>();
        SAXReader reader = new SAXReader();

        try {
            doc = reader.read(file);
        }
        catch (DocumentException e1) {
            e1.printStackTrace();
        }
        params.clear();
        List<?> sectionList = doc.selectNodes("//config/*");

        // Check format version.
        // String format = doc.getRootElement().attributeValue("format");
        // if (format == null || !format.equals("1")) {
        //     throw new InvalidConfigurationException(file.getName(), "unsupported format");            
        // }

        for(Object osection : sectionList) {
            Element section = (Element) osection;
            String sectionName = section.attributeValue("name");
            if (sectionName == null) {
                NeptusLog.pub().error("Error loading unnamed section for " + file.getName());
                continue;
            }

            LinkedHashMap<String, SystemProperty> sectionParams = new LinkedHashMap<>();
            
            String sectionI18nName = section.attributeValue("name-i18n");
            if (sectionI18nName == null)
                sectionI18nName = sectionName;

            sections.add(sectionName);

            Node editorNode = section.selectSingleNode("@editor");
            CustomSystemPropertyEditor sectionCustomEditor = null;
            if (editorNode != null) {
                String editorStr = editorNode.getText();
                try {
                    String str = CustomSystemPropertyEditor.class.getPackage().getName() + "." + editorStr + "CustomEditor";
//                    System.out.println("###########     " + str);
                    Class<?> clazz = Class.forName(str);
                    try {
                        sectionCustomEditor = (CustomSystemPropertyEditor) clazz.getConstructor(Map.class).newInstance(sectionParams);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
            
            for(Object oparam : section.selectNodes("*")) {
                SystemProperty property;
                Element param = (Element) oparam;

                String paramName = param.attributeValue("name");
                if (paramName == null) {
                    NeptusLog.pub().error("Error loading unnamed param for section " + sectionName + " for " + file.getName());
                    continue;
                }
                
                String paramI18nName = getTagContents(param, "name-i18n");
                String scope = getTagContents(param, "scope");
                String visibility = getTagContents(param, "visibility");
                String type = getTagContents(param, "type");
                String desc = getTagContents(param, "desc");
                String units = getTagContents(param, "units");
                String defaultValue = getTagContents(param, "default");

                // Optional (may not exist)
                // If exists is shown as a combobox (may have values-i18n sibling for string type).
                Element pValues = (Element) param.selectSingleNode("values");
                // If exists is shown as a combobox but depends on some other parameter,
                //  there are more than one, inside param equals|less|greater and values (may have values-i18n sibling for string type).
                @SuppressWarnings("rawtypes")
                List pValuesIfList = param.selectNodes("values-if");

                // Optional (may not exist)
                String minStr = getTagContents(param, "min");
                String maxStr = getTagContents(param, "max");

                // Optional (may not exist)
                // This only exists for list:xxx types
                String sizeList = getTagContents(param, "size");
                String sizeMinList = getTagContents(param, "min-size");
                String sizeMaxList = getTagContents(param, "max-size");

                // Let us get the type and if is a list
                ValueTypeEnum valueType = ValueTypeEnum.STRING;
                boolean isList = false;
                Class<?> clazz;
                if (type.equals(SystemProperty.ValueTypeEnum.BOOLEAN.getText())) {
                    clazz = Boolean.class;
                    valueType = ValueTypeEnum.BOOLEAN;
                }
                else if (type.endsWith(SystemProperty.ValueTypeEnum.INTEGER.getText())) {
                    clazz = Long.class;
                    valueType = ValueTypeEnum.INTEGER;
                    if (type.startsWith("list:")) {
                        clazz = ArrayList.class;
                        isList = true;
                    }
                }
                else if (type.endsWith(SystemProperty.ValueTypeEnum.REAL.getText())) {
                    clazz = Double.class;
                    valueType = ValueTypeEnum.REAL;
                    if (type.startsWith("list:")) {
                        clazz = ArrayList.class;
                        isList = true;
                    }
                }
                else if (type.equals("list:" + SystemProperty.ValueTypeEnum.STRING.getText())) {
                    clazz = ArrayList.class;
                    valueType = ValueTypeEnum.STRING;
                    isList = true;
                }
                else {
                    if (type.startsWith("list:"))
                        clazz = ArrayList.class;
                    else
                        clazz = String.class;
                    valueType = ValueTypeEnum.STRING;
                }

                // Lets get the default value
                Object value = !isList ? getValueTypedFromString(defaultValue, valueType) : getListValueTypedFromString(defaultValue, valueType);

                AbstractPropertyEditor propEditor = null;
                DefaultCellRenderer propRenderer = null;

                // If in need of a bounded property
                Double minV = null;
                Double maxV = null;
                String minMaxStr = "";
                if (minStr != null) {
                    try {
                        minV = Double.parseDouble(minStr);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().debug(e.getMessage());
                        minV = null;
                    }
                }
                if (maxStr != null) {
                    try {
                        maxV = Double.parseDouble(maxStr);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().debug(e.getMessage());
                        maxV = null;
                    }
                }

                if (isList) {
                    int size = ArrayListEditor.UNLIMITED_SIZE;
                    int minSize = 0;
                    int maxSize = ArrayListEditor.UNLIMITED_SIZE;
                    if (sizeList != null) {
                        try {
                            size = Integer.parseInt(sizeList);
                        }
                        catch (NumberFormatException e) {
                        }
                        minSize = maxSize = size;
                    }
                    else if (sizeMinList != null || sizeMaxList != null) {
                        if (sizeMinList != null) {
                            try {
                                minSize = Integer.parseInt(sizeMinList);
                            }
                            catch (NumberFormatException e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }
                        }
                        if (sizeMaxList != null) {
                            try {
                                maxSize = Integer.parseInt(sizeMaxList);
                            }
                            catch (NumberFormatException e) {
                                NeptusLog.pub().debug(e.getMessage());
                            }
                        }
                    }
                    else {
                        minSize = maxSize = size;
                    }
                    property = new SystemProperty();
                    switch (valueType) {
                        case INTEGER:
                            // propEditor = ArrayListEditor.forgeLong(minSize, maxSize);
                            propEditor = minV == null && maxV == null ? ArrayListEditor.forgeLong(minSize, maxSize)
                                    : ArrayListEditor.forgeLong(minSize, maxSize,
                                            minV == null ? null : minV.longValue(),
                                            maxV == null ? null : maxV.longValue());
                            minMaxStr = minV == null ? "" : I18n.text("min") + "=" + minV.longValue() + units;
                            String commaSepStr = minMaxStr.length() != 0 ? ", " : "";
                            minMaxStr += maxV == null ? "" : commaSepStr + I18n.text("max") + "=" + maxV.longValue()
                                    + units;
                            break;
                        case REAL:
                            // propEditor = ArrayListEditor.forgeDouble(minSize, maxSize);
                            propEditor = minV == null && maxV == null ? ArrayListEditor.forgeDouble(minSize, maxSize)
                                    : ArrayListEditor.forgeDouble(minSize, maxSize,
                                            minV == null ? null : minV.doubleValue(),
                                            maxV == null ? null : maxV.doubleValue());
                            minMaxStr = minV == null ? "" : I18n.text("min") + "=" + minV.doubleValue() + units;
                            commaSepStr = minMaxStr.length() != 0 ? ", " : "";
                            minMaxStr += maxV == null ? "" : commaSepStr + I18n.text("max") + "=" + maxV.doubleValue()
                                    + units;
                            break;
                        default:
                            String stringTypeStringNotString = type.replaceAll("^list:", "");
                            if (stringTypeStringNotString.equals(I18n.textmark("ipv4-address")))
                                propEditor = ArrayListEditor.forgeString(minSize, maxSize, ArrayListEditor.IP_ADDRESS_PATTERN);
                            else
                                propEditor = ArrayListEditor.forgeString(minSize, maxSize);
                            break;
                    }
                }
                else if (pValues != null) {
                    property = new SystemProperty();
//                    NeptusLog.pub().info("<###> "+pValues.getStringValue());
                    String vlStr = pValues.getStringValue();
                    ArrayList<?> values = extractStringListToArrayList(type, vlStr);
                    ComboEditor<?> comboEditor = null;

                    if (values != null) {
                        if (type.equals(SystemProperty.ValueTypeEnum.INTEGER.getText())) {
                            comboEditor = new ComboEditor<>(((ArrayList<Long>) values).toArray(new Long[0]));
                        }
                        else if (type.equals(SystemProperty.ValueTypeEnum.REAL.getText())) {
                            comboEditor = new ComboEditor<>(((ArrayList<Double>) values).toArray(new Double[0]));
                        }
                        else { // if (type.equals(SystemProperty.ValueTypeEnum.STRING.getText())) {
                            ArrayList<?> valuesI18n = extractI18nValues(type, pValues, values);
                            comboEditor = new ComboEditor<>(((ArrayList<String>) values).toArray(new String[0]),
                                    valuesI18n == null ? null : ((ArrayList<String>) valuesI18n).toArray(new String[0]));

                            // Prep. I18n renderer
                            HashMap<String, String> i18nMapper = new HashMap<>();
                            for (int i = 0; i < Math.min(values.size(), valuesI18n.size()); i++) {
                                Object valObj = values.get(i);
                                Object vaI18nlObj = valuesI18n.get(i);
                                i18nMapper.put(valObj.toString(), vaI18nlObj.toString());
                            }
                            if (i18nMapper.size() > 0)
                                propRenderer = new I18nSystemPropertyRenderer(i18nMapper);
                        }
                        propEditor = comboEditor;
                    }
                }
                else if (pValuesIfList != null) {
                    property = new SystemProperty();
                    ComboEditor<?> comboEditor = null;
                    PropertyEditorChangeValuesIfDependancyAdapter<?, ?> pt;
                    if (type.equals(SystemProperty.ValueTypeEnum.INTEGER.getText())) {
                        pt = new PropertyEditorChangeValuesIfDependancyAdapter<Number, Long>();
                    }
                    else if (type.equals(SystemProperty.ValueTypeEnum.REAL.getText())) {
                        pt = new PropertyEditorChangeValuesIfDependancyAdapter<Number, Double>();
                    }
                    else {
                        pt = new PropertyEditorChangeValuesIfDependancyAdapter<Number, String>();
                    }

                    if (pt != null) {
                        // Prep. I18n renderer
                        HashMap<String, String> i18nMapper = new HashMap<>();

                        for (Object obj : pValuesIfList) {
                            Element elem = (Element) obj;
                            Element paramComp = (Element) elem.selectSingleNode("param");
                            Element eqParam = (Element) elem.selectSingleNode("equals");
                            Element valuesParam = (Element) elem.selectSingleNode("values");
                            if (paramComp == null || eqParam == null || valuesParam == null)
                                continue;

                            ArrayList<?> values = extractStringListToArrayList(type, valuesParam.getTextTrim());

                            if (values != null) {
                                double tv;
                                try {
                                    tv = Double.parseDouble(eqParam.getTextTrim());
                                }
                                catch (NumberFormatException e) {
                                    e.printStackTrace();
                                    break;
                                }
                                if (type.equals(SystemProperty.ValueTypeEnum.INTEGER.getText())) {
                                    ((PropertyEditorChangeValuesIfDependancyAdapter<Number, Long>) pt).addValuesIf(
                                            paramComp.getText(), tv,
                                            PropertyEditorChangeValuesIfDependancyAdapter.TestOperation.EQUALS,
                                            (ArrayList<Long>) values);
                                }
                                else if (type.equals(SystemProperty.ValueTypeEnum.REAL.getText())) {
                                    ((PropertyEditorChangeValuesIfDependancyAdapter<Number, Double>) pt).addValuesIf(
                                            paramComp.getText(), tv,
                                            PropertyEditorChangeValuesIfDependancyAdapter.TestOperation.EQUALS,
                                            (ArrayList<Double>) values);
                                }
                                else if (type.equals(SystemProperty.ValueTypeEnum.STRING.getText())) {
                                    ArrayList<?> valuesI18n = extractI18nValues(type, valuesParam, values);
                                    ((PropertyEditorChangeValuesIfDependancyAdapter<Number, String>) pt).addValuesIf(
                                            paramComp.getText(), tv,
                                            PropertyEditorChangeValuesIfDependancyAdapter.TestOperation.EQUALS,
                                            (ArrayList<String>) values, valuesI18n != null ? (ArrayList<String>) valuesI18n : null);

                                    // Prep. I18n renderer
                                    for (int i = 0; i < Math.min(values.size(), valuesI18n.size()); i++) {
                                        Object valObj = values.get(i);
                                        Object vaI18nlObj = valuesI18n.get(i);
                                        i18nMapper.put(valObj.toString(), vaI18nlObj.toString());
                                    }
                                }
                                else {
                                    break;
                                }
                            }
                        }
                        // Prep. I18n renderer
                        if (i18nMapper.size() > 0)
                            propRenderer = new I18nSystemPropertyRenderer(i18nMapper);
                    }

                    ArrayList<?> values = pt.getValuesIfTests().size() > 0 ? pt.getValuesIfTests().get(0).values : null;
                    ArrayList<?> valuesI18n = pt.getValuesI18nIfTests().size() > 0 ? pt.getValuesI18nIfTests().get(0).values : null;
                    if (values != null) {
                        if (type.equals(SystemProperty.ValueTypeEnum.INTEGER.getText())) {
                            comboEditor = new ComboEditorWithDependancy<>(((ArrayList<Long>) values).toArray(new Long[0]), pt);
                        }
                        else if (type.equals(SystemProperty.ValueTypeEnum.REAL.getText())) {
                            comboEditor = new ComboEditorWithDependancy<>(((ArrayList<Double>) values).toArray(new Double[0]), pt);
                        }
                        else {
                            comboEditor = new ComboEditorWithDependancy<>(((ArrayList<String>) values).toArray(new String[0]),
                                    valuesI18n == null ? null : ((ArrayList<String>) valuesI18n).toArray(new String[0]), pt);
                        }
                        propEditor = comboEditor;
                    }
                }
                else {
                    property = new SystemProperty();
                }

                if (propEditor == null) {
                    switch (valueType) {
                        case BOOLEAN:
                            propEditor = new BooleanAsCheckBoxPropertyEditor();
                            break;
                        case INTEGER:
                            propEditor = minV == null && maxV == null ? new NumberEditor<>(Long.class) : new NumberEditor<>(
                                    Long.class, minV == null ? null : minV.longValue(), maxV == null ? null : maxV.longValue());
                            minMaxStr = minV == null ? "" : I18n.text("min") + "=" + minV.longValue() + units;
                            String commaSepStr = minMaxStr.length() != 0 ? ", " : "";
                            minMaxStr += maxV == null ? "" : commaSepStr + I18n.text("max") + "=" + maxV.longValue() + units;
                            break;
                        case REAL:
                            propEditor = minV == null && maxV == null ? new NumberEditor<>(Double.class) : new NumberEditor<>(
                                    Double.class, minV == null ? null : minV.doubleValue(), maxV == null ? null : maxV.doubleValue());
                            minMaxStr = minV == null ? "" : I18n.text("min") + "=" + minV.doubleValue() + units;
                            commaSepStr = minMaxStr.length() != 0 ? ", " : "";
                            minMaxStr += maxV == null ? "" : commaSepStr + I18n.text("max") + "=" + maxV.doubleValue() + units;
                            break;
                        default:
                            String stringTypeStringNotString = type;
                            if (stringTypeStringNotString.equals("ipv4-address")) {
                                propEditor = new StringPatternEditor(ArrayListEditor.IP_ADDRESS_PATTERN);
                            }
                            else
                                propEditor = new StringPatternEditor(".*");
                            break;
                    }
                }

                property.setValueType(valueType);
                property.setName(paramName);
                property.setDisplayName(paramI18nName);

                if (value != null) {
                    property.setValue(value);
                    property.setDefaultValue(value);
                }

                property.setType(clazz);

                String lstSizeTxt = "";
                if (isList) {
                    lstSizeTxt = "[";
                    if (sizeList != null) {
                        try {
                            int sl = Integer.parseInt(sizeList);
                            lstSizeTxt += sl;
                        }
                        catch (NumberFormatException e) {
                            e.printStackTrace();
                            lstSizeTxt += "*";
                        }
                    }
                    else if (sizeMinList != null && sizeMaxList != null) {
                        double minS;
                        double maxS;
                        try {
                            minS = Double.parseDouble(sizeMinList);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            minS = 0;
                        }
                        try {
                            maxS = Double.parseDouble(sizeMaxList);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                            maxS = ArrayListEditor.UNLIMITED_SIZE;
                        }
                        if (minS < 0)
                            minS = 0;
                        if (maxS <= 0)
                            maxS = ArrayListEditor.UNLIMITED_SIZE;
                        if (minS > maxS)
                            minS = 0;

                        lstSizeTxt += (int) minS == (int) minS ? (int) minS : (int) minS + "," + (int) minS;
                    }
                    else
                        lstSizeTxt += "*";
                    lstSizeTxt += "]";
                }

                String unitsTxt = units.length() > 0 ? "\n(" + units + ")" : "";
                String typeTxt = type != null ? "\n["
                        + (type.startsWith("list:") ? I18n.text(type.substring(0, 4)) + ":"
                                + I18n.text(type.substring(5)) : I18n.text(type)) + lstSizeTxt + "]" : "";
                String defaultTxt = defaultValue != null ? "\n[" + I18n.text("default") + "=" + defaultValue + units + "]" : "";
                String minMaxValuesTxt = minMaxStr.length() > 0 ? "[" + minMaxStr + "]" : "";
                property.setShortDescription(desc + unitsTxt + typeTxt + defaultTxt + minMaxValuesTxt);
                property.setCategory(sectionI18nName);
                property.setCategoryId(sectionName);
                property.setScope(SystemProperty.Scope.fromString(scope));
                property.setVisibility(SystemProperty.Visibility.fromString(visibility));

                // Setting editor
                if (propEditor != null)
                    property.setEditor(propEditor);

                // Setting renderer
                if (propRenderer != null) {
                    property.setRenderer(propRenderer);
                }
                else if (valueType == ValueTypeEnum.BOOLEAN) {
                    property.setRenderer(new BooleanSystemPropertyRenderer());
                }
                else if (units.length() > 0) {
                    property.setRenderer(new SystemPropertyRenderer(units));
                }
                else {
                    property.setRenderer(new SystemPropertyRenderer());
                }

                if (sectionCustomEditor != null) {
                    property.setSectionCustomEditor(sectionCustomEditor);
//                    sectionCustomEditor = null;
                }
                
                params.put(sectionName + "." + paramName, property);
                sectionParams.put(paramName, property);
            }
        }
        return params;
    }

    /**
     * @param type
     * @param pValues
     * @param values
     * @return
     */
    private ArrayList<?> extractI18nValues(String type, Element pValues, ArrayList<?> values) {
        Node nd = pValues.selectSingleNode("following-sibling::*");
        ArrayList<?> valuesI18n = null;
        if (nd != null && "values-i18n".equals(nd.getName())) {
            Element elem = (Element) nd;
            String vlI18nStr = elem.getStringValue();
            valuesI18n = extractStringListToArrayList(type, vlI18nStr);
            if (values.size() != valuesI18n.size()) {
                valuesI18n = null;
            }
        }
        return valuesI18n;
    }

    /**
     * @param type
     * @param vlStr
     * @return
     */
    @SuppressWarnings("unchecked")
    private ArrayList<?> extractStringListToArrayList(String type, String vlStr) {
        ArrayList<?> values = null;
        for (String st : vlStr.split("( *, *)+")) {
            st = st.trim();
            if (type.equals(SystemProperty.ValueTypeEnum.INTEGER.getText())) {
                if (values == null) {
                    values = new ArrayList<Long>();
                }
                // Long vl = 0L;
                try {
                    String dv = st == null ? null : st.replaceAll("\\.\\d+$", "");
                    long vl = dv != null ? Long.parseLong(
                            dv.contains("x") ? dv.replaceFirst("^0x", "") : dv,
                            dv.contains("x") ? 16 : 10) : 0L;
                    ((ArrayList<Long>) values).add(vl);
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            else if (type.equals(SystemProperty.ValueTypeEnum.REAL.getText())) {
                if (values == null) {
                    values = new ArrayList<Double>();
                }
                try {
                    double vl = st != null ? Double.parseDouble(st) : 0.0;
                    ((ArrayList<Double>) values).add(vl);
                }
                catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            else if (type.equals(SystemProperty.ValueTypeEnum.STRING.getText())) {
                if (values == null) {
                    values = new ArrayList<String>();
                }
                ((ArrayList<String>) values).add(st);
            }
        }
        return values;
    }

    /**
     * @return
     *
     */
    public static Object getValueTypedFromString(String valueStr, SystemProperty.ValueTypeEnum type) {
        if (type == SystemProperty.ValueTypeEnum.BOOLEAN) {
            if (valueStr == null)
                return false;
            switch (valueStr.toLowerCase().trim()) {
                case "true":
                case "yes":
                case "1":
                    return true;
                default:
                    return false;
            }
        }
        else if (type == SystemProperty.ValueTypeEnum.INTEGER) {
            try {
                String dv = valueStr == null ? null : valueStr.replaceAll("\\.\\d+$", "");
                return  dv != null ? Long.parseLong(
                        dv.contains("x") ? dv.replaceFirst("^0x", "") : dv,
                        dv.contains("x") ? 16 : 10) : 0L;
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                return 0L;
            }
        }
        else if (type == SystemProperty.ValueTypeEnum.REAL) {
            try {
                return valueStr != null ? Double.parseDouble(valueStr) : 0.0;
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                return 0.0;
            }
        }
        else {
            return valueStr;
        }
    }

    public static Object getListValueTypedFromString(String valueStr, SystemProperty.ValueTypeEnum type) {
        ArrayList<? extends Object> retLst;
        String[] tokens = valueStr == null ? new String[0] : valueStr.split(",");
        switch (type) {
            case INTEGER:
                ArrayList<Long> tmpLLst = new ArrayList<Long>();
                for (String tk : tokens) {
                    tk = tk.trim();
                    Long tokenObjVal = (Long) getValueTypedFromString(tk, type);
                    if (tokenObjVal != null) {
                        tmpLLst.add(tokenObjVal);
                    }
                }
                retLst = tmpLLst;
                break;
            case REAL:
                ArrayList<Double> tmpRLst = new ArrayList<Double>();
                for (String tk : tokens) {
                    tk = tk.trim();
                    Double tokenObjVal = (Double) getValueTypedFromString(tk, type);
                    if (tokenObjVal != null) {
                        tmpRLst.add(tokenObjVal);
                    }
                }
                retLst = tmpRLst;
                break;
            case STRING:
                ArrayList<String> tmpSLst = new ArrayList<String>();
                for (String tk : tokens) {
                    tk = tk.trim();
                    String tokenObjVal = (String) getValueTypedFromString(tk, type);
                    if (tokenObjVal != null) {
                        tmpSLst.add(tokenObjVal);
                    }
                }
                retLst = tmpSLst;
                break;
            default:
                return null;
        }

        return retLst;
    }

    public ArrayList<SystemProperty> getPropertiesByEntity(String system, String entity, Visibility vis, Scope scope) {
        return getPropertiesByEntityWorker(system, entity, vis, scope, false);
    }

    private ArrayList<SystemProperty> getPropertiesByEntityWorker(String system, String entity, Visibility vis, Scope scope, boolean giveUpSearchOnFirstFound) {
        ArrayList<SystemProperty> list = new ArrayList<>();
        HashMap<String, SystemProperty> sy = map.get(system);
        if (sy != null) {
            for(SystemProperty p : sy.values()) {
                if ((entity == null || p.getCategory().equals(entity)) && p.getVisibility().ordinal() >= vis.ordinal()
                        && (p.getScope() == scope || scope == Scope.GLOBAL)) {
                    list.add(p);
                    if (giveUpSearchOnFirstFound)
                        break;
                }
            }
        }
        return list;
    }
    
    public ArrayList<SystemProperty> getClonedProperties(String system, Visibility vis, Scope scope) {
        ArrayList<SystemProperty> props = getPropertiesByEntity(system, null, vis, scope);
        Map<String, CustomSystemPropertyEditor> customEditors = new HashMap<>();

        ArrayList<SystemProperty> clones = new ArrayList<>();

        for (SystemProperty p : props) {
            SystemProperty sp = new SystemProperty();
            sp.setCategory(p.getCategory());
            sp.setCategoryId(p.getCategoryId());
            sp.setDefaultValue(p.getDefaultValue());
            sp.setDisplayName(p.getDisplayName());
            sp.setEditable(p.isEditable());
            sp.setEditor(p.getEditor());
            sp.setName(p.getName());
            sp.setRenderer(p.getRenderer());
            if (sp.getRenderer() != null && sp.getRenderer() instanceof SystemPropertyRenderer) {
                try {
                    DefaultCellRenderer clone = (DefaultCellRenderer) ((SystemPropertyRenderer) sp.getRenderer()).clone();
                    sp.setRenderer(clone);
                }
                catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            sp.setScope(p.getScope());
            sp.setShortDescription(p.getShortDescription());
            sp.setValue(p.getValue());
            sp.setType(p.getType());
            sp.setValueType(p.getValueType());
            sp.setVisibility(p.getVisibility());

            try {
                CustomSystemPropertyEditor ce = customEditors.get(p.getCategoryId());
                if (ce == null) {
                    ce = p.getSectionCustomEditor() != null ? p.getSectionCustomEditor().clone() : p
                            .getSectionCustomEditor();
                    customEditors.put(p.getCategoryId(), ce);
                }
                sp.setSectionCustomEditor(ce);
            }
            catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
            if (sp.getSectionCustomEditor() != null) {
                CustomSystemPropertyEditor ce = sp.getSectionCustomEditor();
                SystemProperty kv = ce.getSystemPropertiesList().get(p.getName());
                if (kv != null)
                    ce.getSystemPropertiesList().put(sp.getName(), sp);
                
                // System.out.println("System Property (" + p.getName() + "): " + Integer.toHexString(p.hashCode()) + " --- " + Integer.toHexString(sp.hashCode()));
                // System.out.println("Custom Section Editor: " + Integer.toHexString(p.getSectionCustomEditor().hashCode()) + " --- " + Integer.toHexString(sp.getSectionCustomEditor().hashCode()));
                // GuiUtils.printList(p.getSectionCustomEditor().getSystemPropertiesList().values());
                // GuiUtils.printList(sp.getSectionCustomEditor().getSystemPropertiesList().values());
            }

            clones.add(sp);
        }
        return clones;
    }

    public ArrayList<SystemProperty> getProperties(String system, Visibility vis, Scope scope) {
        return getPropertiesByEntity(system, null, vis, scope);
    }

    public boolean hasProperties(String system, Visibility vis, Scope scope) {
        return getPropertiesByEntityWorker(system, null, vis, scope, true).size() > 0;
    }

    /**
     * @param str
     * @return
     */
    public static String convertArrayListToStringToPropValueString(String str) {
        return str.replaceAll("^\\[", "").replaceAll("\\]$", "");
    }

    public static void main(String[] args) {
        ConfigurationManager confMan = new ConfigurationManager();
        confMan.loadConfigurations();
        NeptusLog.pub().info("<###> "+confMan.getPropertiesByEntity("lauv-dolphin-1", "Sidescan", Visibility.USER, Scope.MANEUVER));
        NeptusLog.pub().info("<###> "+confMan.getProperties("lauv-dolphin-1", Visibility.USER, Scope.MANEUVER));
        NeptusLog.pub().info("<###> "+confMan.getProperties("lauv-dolphin-1", Visibility.USER, Scope.PLAN));
        NeptusLog.pub().info("<###> "+confMan.getProperties("lauv-dolphin-1", Visibility.DEVELOPER, Scope.GLOBAL));
    }
}
