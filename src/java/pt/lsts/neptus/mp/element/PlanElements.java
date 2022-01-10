/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 16/04/2017
 */
package pt.lsts.neptus.mp.element;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.util.XMLUtil;

/**
 * @author pdias
 *
 */
public class PlanElements {

    protected List<IPlanElement<?>> planElements = Collections.synchronizedList(new LinkedList<IPlanElement<?>>());
    
    public PlanElements() {
    }

    /**
     * @return the planElements
     */
    public List<IPlanElement<?>> getPlanElements() {
        return planElements;
    }
    
    /**
     * @param nd
     */
    public boolean load(String xml) {
        Document doc = XMLUtil.createDocumentFromXmlString(xml);
        return load(doc.getDocumentElement());
    }

    /**
     * @param asElement
     */
    public boolean load(Element element) {
        @SuppressWarnings("rawtypes")
        LinkedHashMap<String, Class<? extends IPlanElement>> pElementsPlugins = PluginsRepository.getPlanElements();
        
        planElements.clear();
        
        Element root = element;
        NodeList cElems = root.getChildNodes();
        for (int i = 0; i < cElems.getLength(); i++) {
            Node el = cElems.item(i);
            if (!(el instanceof Element))
                continue;
            String nodeName = el.getNodeName();
            IPlanElement<?> planElementPg = searchPlugin(pElementsPlugins, nodeName);
            if (planElementPg == null)
                continue;
            String elXml = XMLUtil.nodeToString(el);
            if (planElementPg.loadElementXml(elXml) == null)
                continue;
            
            planElements.add(planElementPg);
        }
        
        return true;
    }

    /**
     * @param pElementsPlugins
     * @param nodeName
     * @return
     */
    @SuppressWarnings("rawtypes")
    private IPlanElement<?> searchPlugin(LinkedHashMap<String, Class<? extends IPlanElement>> pElementsPlugins,
            String nodeName) {
        String searchString = nodeName + "PlanElement";
        String searchString2 = nodeName;
        for (String nm : pElementsPlugins.keySet()) {
            if (nm.endsWith(searchString) || nm.endsWith(searchString2)) {
                Class<? extends IPlanElement> clazz = pElementsPlugins.get(nm);
                try {
                    IPlanElement<?> pe = clazz.getDeclaredConstructor().newInstance();
                    return pe;
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    /**
     * @param string
     * @return
     */
    public Element asElement(String string) {
        Document doc = XMLUtil.createEmptyDocument();
        Element root = doc.createElement(string);
        doc.appendChild(root);
        for (IPlanElement<?> iPlanElement : planElements) {
            String ipXml = iPlanElement.getElementAsXml();
            Document ipDoc = XMLUtil.createDocumentFromXmlString(ipXml);
            // System.out.println("1- " + XMLUtil.nodeToString(ipDoc.getDocumentElement()));
            try {
                root.appendChild(doc.adoptNode(ipDoc.getDocumentElement()));
            }
            catch (DOMException e) {
                e.printStackTrace();
            }
            // System.out.println("2- " + XMLUtil.nodeToString(doc.getDocumentElement()));
            // System.out.println("3- " + XMLUtil.nodeToString(root));
        }
        return root;
    }
}
