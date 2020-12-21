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
 * Author: Manuel R.
 * Nov 22, 2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * @author Manuel R
 * Parameter metadata parser extracted from parameters
 * 
 */
public class ParameterMetadataMapReader {

    private static final boolean DEBUG = false;
    private static final String METADATA_NAME = "name";
    private static final String METADATA_DISPLAYNAME = "humanName";
    private static final String METADATA_DESCRIPTION = "documentation";
    private static final String METADATA_UNITS = "Units";
    private static final String METADATA_RANGE = "Range";
    private static final String METADATA_INCREMENT = "Increment";
    private static final String METADATA_BITMASK = "Bitmask";
    private static final String METADATA_VALUES = "values";
    @SuppressWarnings("unused")
    private static final String METADATA_VALUE = "value";
    private static final String METADATA_VALUE_CODE = "code";
    private static final String METADATA_PARAMETERS = "parameters";
    private static final String METADATA_LIBRARY = "libraries";
    private static final String METADATA_VEHICLES = "vehicles";

    public static HashMap<String, ParameterMetadata> parseMetadata(File input, String vehType) throws IOException {
        if (!input.isFile())
            return null;

        HashMap<String, ParameterMetadata> metadataMap = new HashMap<String, ParameterMetadata>();
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = null;
        Document doc = null;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            doc = dBuilder.parse(input);
        }
        catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
        catch (SAXException | IOException e) {
            e.printStackTrace();
            return null;
        }

        doc.getDocumentElement().normalize();
        Element root = doc.getDocumentElement();
        printDebug("Root element :" + root.getNodeName());

        NodeList vehTypes = doc.getElementsByTagName(METADATA_PARAMETERS);

        for (int temp = 0; temp < vehTypes.getLength(); temp++) {
            Node nNode = vehTypes.item(temp);

            if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                Element eElement = (Element) nNode;

                parseNode(eElement, metadataMap, METADATA_VEHICLES, vehType);
                parseNode(eElement, metadataMap, METADATA_LIBRARY, null);

            }
        }
        return metadataMap;
    }

    private static void parseNode(Element eElement, HashMap<String, ParameterMetadata> metadataMap, String metaType, String vehType) {
        boolean validate = true;
        if (vehType != null)
            validate = eElement.getAttribute(METADATA_NAME).equals(vehType);

        if (eElement.getParentNode().getNodeName().equals(metaType) && validate) {
            printDebug("name : " + eElement.getAttribute(METADATA_NAME));
            NodeList childs = eElement.getChildNodes();
            for (int tempChild = 0; tempChild < childs.getLength(); tempChild++) {
                Node nNodeChild = childs.item(tempChild);
                if (nNodeChild.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElementChild = (Element) nNodeChild;
                    String displayName = eElementChild.getAttribute(METADATA_DISPLAYNAME);
                    String description = StringUtils.capitalize(eElementChild.getAttribute(METADATA_DESCRIPTION));
                    String paramNameStr = eElementChild.getAttribute(METADATA_NAME);
                    String[] paramNameArr = paramNameStr.split(":");
                    String paramName = (paramNameArr.length > 1 ? paramNameArr[1] : paramNameStr);

                    printDebug("   humanName : " + displayName + " | " + paramName + " | " + description);

                    if (paramName != null && displayName != null) {
                        ParameterMetadata metadata = parseElement(eElementChild);
                        metadata.setDisplayName(displayName);
                        metadata.setName(paramName);
                        metadata.setDescription(description);
                        metadataMap.put(paramName, metadata);
                    }
                }
            }
        }
    }

    private static ParameterMetadata parseElement(Element parent) {
        ParameterMetadata metadata = new ParameterMetadata();

        NodeList childList = parent.getChildNodes();
        for (int temp = 0; temp < childList.getLength(); temp++) {
            Node child = childList.item(temp);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childElem = (Element) child;
                printDebug("                > " + childElem.getNodeName() + " " + childElem.getAttribute(METADATA_NAME));

                switch (childElem.getAttribute(METADATA_NAME)) {
                    case METADATA_RANGE:
                        String range = childElem.getTextContent();
                        metadata.setRange(range);

                        break;
                    case METADATA_INCREMENT:
                        String increment = childElem.getTextContent();
                        metadata.setIncrement(increment);

                        break;
                    case METADATA_UNITS:
                        String units = childElem.getTextContent();
                        metadata.setUnits(units);

                        break;
                    case METADATA_BITMASK:
                        String bitmask = childElem.getTextContent();
                        metadata.setBitmask(bitmask);

                        break;
                    default:
                        break;
                }

                if (childElem.getNodeName().equals(METADATA_VALUES)) {
                    NodeList valueList = childElem.getChildNodes();
                    for (int temp2 = 0; temp2 < valueList.getLength(); temp2++) {
                        Node valueChild = valueList.item(temp2);

                        if (valueChild.getNodeType() == Node.ELEMENT_NODE) {
                            Element valueChildElem = (Element) valueChild;
                            printDebug("                     > " + valueChildElem.getAttribute(METADATA_VALUE_CODE) + " " + valueChildElem.getTextContent());
                            metadata.parseValues(valueChildElem.getAttribute(METADATA_VALUE_CODE), valueChildElem.getTextContent());
                        }
                    }
                    printDebug("                     > " + metadata.getValues().toString());
                }
            }
        }

        return metadata;
    }

    private static void printDebug(String toPrint) {
        if (DEBUG)
            System.out.println(toPrint);
    }
}
