/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Author: João Cordeiro
 * Dez 3, 2025
 */
package pt.lsts.neptus.params.util;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import pt.lsts.imc.TypedEntityParameter;
import pt.lsts.imc.QueryTypedEntityParameters;
import pt.lsts.imc.TypedEntityParameterEditor;
import pt.lsts.imc.TypedEntityParametersOptions;
import pt.lsts.imc.ValuesIf;
import pt.lsts.neptus.gui.editor.ArrayListEditor;

import static java.lang.Math.abs;

/**
 * Converts a QTEP reply into a DOM4J <section> element.
 * Does NOT create a full document.
 * Used by ConfigurationManager to assemble the full XML.
 */
public class QtepToSectionConverter {

    /**
     * Converts a QTEP message into a single <section> element.
     * @param qtep A QueryTypedEntityParameters REPLY message.
     * @return DOM4J Element corresponding to a <section>, or null if empty.
     */
    public static Element convertToSection(QueryTypedEntityParameters qtep) {

        if (qtep == null || qtep.getParameters() == null || qtep.getParameters().isEmpty()) {
            return null;
        }

        String sectionName = qtep.getEntityName();
        if (sectionName == null || sectionName.isEmpty()) {
            sectionName = "default";
        }

        Element section = DocumentHelper.createElement("section")
                .addAttribute("name", sectionName)
                .addAttribute("name-i18n", sectionName);

        for (TypedEntityParametersOptions param : qtep.getParameters()) {
            if (param.getMgid() == TypedEntityParameterEditor.ID_STATIC) {
                TypedEntityParameterEditor editor = (TypedEntityParameterEditor) param;
                section.addAttribute("editor", editor.getValue());
            }
            else {
                TypedEntityParameter tep = (TypedEntityParameter) param;
                addParameter(section, tep);
            }
        }

        return section;
    }

    private static void addParameter(Element section, TypedEntityParameter param) {
        boolean isEditable = true;

        Element p = section.addElement("param")
                .addAttribute("name", param.getName());

        switch (param.getVisibility()) {
            case USER_NOT_EDITABLE:
                isEditable = false;
            case USER:
                p.addElement("visibility").setText("user");
                break;
            case DEVELOPER_NOT_EDITABLE:
                isEditable = false;
            case DEVELOPER:
            default:
                p.addElement("visibility").setText("developer");
        }

        if (!isEditable) {
            p.addAttribute("editable", "false");
        }

        p.addElement("name-i18n").setText(param.getName());

        String type = isIPv4AddressParam(param) ? "ipv4-address" : covertType(param.getType());
        p.addElement("type").setText(type);

        if (param.getScopeStr() != null)
            p.addElement("scope").setText(param.getScopeStr().toLowerCase());

        if (param.getDefaultValue() != null)
            p.addElement("default").setText(param.getDefaultValue());

        if (param.getUnits() != null)
            p.addElement("units").setText(param.getUnits());

        if (isNumericType(param.getTypeVal())) {

            double min = param.getMinValue();
            double max = param.getMaxValue();

            if (!Double.isNaN(min) && abs(min) < 1e30)
                p.addElement("min").setText(String.valueOf(min));

            if (!Double.isNaN(max) && abs(max) < 1e30)
                p.addElement("max").setText(String.valueOf(max));
        }

        if (param.getDescription() != null && !param.getDescription().isEmpty())
            p.addElement("desc").setText(param.getDescription());

        if (isListType(param.getTypeVal())) {

            if (param.getListMinSize() > 0)
                p.addElement("min-size").setText(String.valueOf(param.getListMinSize()));

            if (param.getListMaxSize() < 32000)
                p.addElement("max-size").setText(String.valueOf(param.getListMaxSize()));
        }

        if (param.getValuesList() != null && !param.getValuesList().isEmpty()) {
            String vals = String.join(",", param.getValuesList());
            p.addElement("values").setText(vals);
            p.addElement("values-i18n").setText(vals);
        }

        // values-if
        if (param.getValuesIfList() != null) {
            for (ValuesIf vf : param.getValuesIfList()) {
                Element vif = p.addElement("values-if");
                vif.addElement("param").setText(vf.getParam());
                vif.addElement("equals").setText(vf.getValue());

                if (vf.getValuesList() != null && !vf.getValuesList().isEmpty())
                    vif.addElement("values")
                            .setText(String.join(",", vf.getValuesList()));
            }
        }
    }

    private static String covertType(TypedEntityParameter.TYPE type) {
        switch (type) {
            case BOOL:
                return "boolean";
            case INT:
                return "integer";
            case FLOAT:
                return "real";
            case STRING:
                return "string";
            case LIST_BOOL:
                return "list:boolean";
            case LIST_INT:
                return "list:integer";
            case LIST_FLOAT:
                return "list:real";
            case LIST_STRING:
                return "list:string";
            default:
                return "string";
        }
    }
    
    private static boolean isIPv4AddressParam(TypedEntityParameter param) {
        if (param == null || param.getDefaultValue() == null) return false;
        
        String defaultValue = param.getDefaultValue().toString().trim();
        
        String ipv4Pattern = ArrayListEditor.IP_ADDRESS_PATTERN;

        return defaultValue.matches(ipv4Pattern);
    }

    private static boolean isNumericType(int type) {
        return type == 2 || type == 3 || type == 6 || type == 7;
    }

    private static boolean isListType(int type) {
        return type >= 5 && type <= 8;
    }
}
