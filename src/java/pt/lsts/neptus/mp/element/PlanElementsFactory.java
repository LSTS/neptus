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
 * 17/04/2017
 */
package pt.lsts.neptus.mp.element;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.util.XMLUtil;

/**
 * This loads the plan elements tag. E.g.:
 * 
 * <code><br/><br/>
 * &lt;planElements><br/>
 *   &nbsp;&nbsp;&lt;item class="pt.lsts.neptus.mp.element.RendezvousPointsPlanElement" /><br/>
 *   &nbsp;&nbsp;&lt;item class="pt.lsts.neptus.mp.element.OperationLimitsPlanElement" args="limits=true" /><br/>
 *   &nbsp;&nbsp;&lt;item class="pt.lsts.neptus.mp.element.LandPlanElement" args="limits true"><br/>
 *     &nbsp;&nbsp;&nbsp;&nbsp;&lt;Land><br/>
 *       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;point lat="41" lon="-8" /><br/>
 *       &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&lt;point lat="41.1" lon="-8.4" /><br/>
 *     &nbsp;&nbsp;&nbsp;&nbsp;&lt;/Land><br/>
 *   &nbsp;&nbsp;&lt;/item><br/>
 * &lt;/planElements>"
 * </code>
 * 
 * <br/>
 * <br/>
 * The args its optional and it's format is dependent, and it's feed as in the main args array. <br/>
 * The class has to exist or will be discarded.
 * 
 * <br/>
 * <br/>
 * The inner tag (only one will be read) should optionally be given and should be compatible with
 * {@link IPlanElement#loadElementXml(String) call.}
 * 
 * @author pdias
 *
 */
public class PlanElementsFactory {

    private static final String TAG_ROOT = "elements";
    private static final String TAG_ITEM = "item";
    private static final String TAG_ARG_CLASS = "class";
    private static final String TAG_ARG_ARGS = "args";

    private final String systemId;

    private List<Class<IPlanElement<?>>> planElementsClasses = new LinkedList<>();
    private Map<String, String[]> planElementsStartArgs = new LinkedHashMap<>();
    private Map<String, Element> planElementsTypeXml = new LinkedHashMap<>();

    private static Map<String, String> planElementsNames = new LinkedHashMap<>();

    public PlanElementsFactory(String systemId) {
        this.systemId = systemId;
    }

    public PlanElementsFactory(String systemId, String xml) {
        this(systemId);
        loadXml(xml);
    }

    public PlanElementsFactory(String systemId, Element element) {
        this(systemId);
        loadXml(element);
    }

    /**
     * @return the systemId
     */
    public String getSystemId() {
        return systemId;
    }

    /**
     * @return the planElementsClasses as unmodifiable list.
     */
    public List<Class<IPlanElement<?>>> getPlanElementsClasses() {
        return Collections.unmodifiableList(planElementsClasses);
    }

    /**
     * @return
     */
    public int getPlanElementsSize() {
        return planElementsClasses.size();
    }

    /**
     * Instantiates new IPlanElements. Elements in {@link IPlanElement#getElement()} may not be instantiated.
     * 
     * @return
     */
    public ArrayList<IPlanElement<?>> getPlanElementsInstances() {
        ArrayList<IPlanElement<?>> ret = new ArrayList<>();

        for (Class<IPlanElement<?>> iPEClazz : planElementsClasses) {
            IPlanElement<?> newInstance = getPlanInstance(iPEClazz);
            if (newInstance == null)
                continue;

            ret.add(newInstance);
        }

        return ret;
    }

    /**
     * @param iPEClazz
     * @return
     */
    public IPlanElement<?> getPlanInstance(Class<IPlanElement<?>> iPEClazz) {
        if (!planElementsClasses.contains(iPEClazz))
            return null;

        IPlanElement<?> newInstance = null;
        
        String[] args = planElementsStartArgs.get(iPEClazz.getName());
        try {
            if (args != null && args.length > 0) {
                for (Constructor<?> string : iPEClazz.getConstructors()) {
                    System.out.println(string);
                }
                Constructor<IPlanElement<?>> contructor = iPEClazz.getConstructor(String[].class);
                newInstance = contructor.newInstance(new Object[] { args });
            }
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
        }
        try {
            if (newInstance == null)
                newInstance = iPEClazz.getDeclaredConstructor().newInstance();
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
        }

        if (newInstance != null) {
            Element emlXml = planElementsTypeXml.get(iPEClazz.getName());
            if (emlXml != null) {
                newInstance.loadElementXml(XMLUtil.nodeToString(emlXml));
            }
            
            addNameFor(newInstance);
        }

        return newInstance;
    }

    public String getNameFor(Class<IPlanElement<?>> iPEClazz) {
        synchronized (planElementsNames) {
            String name = planElementsNames.get(iPEClazz.getName());
            if (name != null)
                return name;
            
            IPlanElement<?> pe = getPlanInstance(iPEClazz);
            name = addNameFor(pe);
            return name;
        }
    }
    
    private String addNameFor(IPlanElement<?> pe) {
        synchronized (planElementsNames) {
            String name = planElementsNames.get(pe.getClass().getName());
            if (name == null) {
                name = pe.getName();
                planElementsNames.put(pe.getClass().getName(), name);
            }
            
            return name;
        }
    }
    
    /**
     * Calls the 
     * 
     * @param pe
     * @return
     */
    public IPlanElement<?> configureInstance(IPlanElement<?> pe) {
        if (pe != null)
            addNameFor(pe);
        else
            return pe;
        
        String[] args = planElementsStartArgs.get(pe.getClass().getName());
        try {
            Method configureMethod = pe.getClass().getMethod("configure", String[].class);
            configureMethod.invoke(pe, args == null ? new String[0] : args);
        }
        catch (Exception e) {
            if (args == null)
                NeptusLog.pub().error(e);
            else
                NeptusLog.pub().warn(e);
        }
        return pe;
    }
    
    /**
     * Loads its content from XML.
     * 
     * @param xml
     */
    public void loadXml(String xml) {
        Document doc = XMLUtil.createDocumentFromXmlString(xml);
        loadXml(doc.getDocumentElement());
    }

    /**
     * Loads its content from XML element.
     * 
     * @param element
     */
    public void loadXml(Element element) {
        NodeList itemsNodes = element.getElementsByTagName(TAG_ITEM);
        for (int i = 0; i < itemsNodes.getLength(); i++) {
            Node iNd = itemsNodes.item(i);
            if (!iNd.hasAttributes())
                continue;

            NamedNodeMap attrs = iNd.getAttributes();
            Node classNd = attrs.getNamedItem(TAG_ARG_CLASS);
            if (classNd == null)
                continue;

            String classStr = classNd.getTextContent();
            try {
                @SuppressWarnings("unchecked")
                Class<IPlanElement<?>> clazz = (Class<IPlanElement<?>>) ClassLoader.getSystemClassLoader()
                        .loadClass(classStr);
                planElementsClasses.add(clazz);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
                continue;
            }

            Node argsNd = attrs.getNamedItem(TAG_ARG_ARGS);
            if (argsNd == null)
                continue;

            try {
                String argsStr = argsNd.getTextContent().trim();
                String[] args = argsStr.split(" {1,}");
                if (!argsStr.isEmpty() && args.length > 0)
                    planElementsStartArgs.put(classStr, args);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(e);
            }

            // Getting optional element initialization XML element
            NodeList childNodes = iNd.getChildNodes();
            for (int j = 0; j < childNodes.getLength(); j++) {
                if (childNodes.item(j) instanceof Element) {
                    Element elm = (Element) childNodes.item(j).cloneNode(true);
                    planElementsTypeXml.put(classStr, elm);
                    break;
                }
            }
        }
    }

    /**
     * Return the contents as XML text.
     * 
     * @return
     */
    public String asXml() {
        Element element = asXmlElement();
        if (element == null)
            return "";

        return XMLUtil.nodeToString(element);
    }

    /**
     * Return the contents as XML element.
     * 
     * @return
     */
    public Element asXmlElement() {
        if (planElementsClasses.isEmpty())
            return null;

        Document doc = XMLUtil.createEmptyDocument();
        Element root = doc.createElement(TAG_ROOT);
        doc.appendChild(root);

        for (Class<IPlanElement<?>> clazz : planElementsClasses) {
            Element itemNd = doc.createElement(TAG_ITEM);
            String className = clazz.getName();
            itemNd.setAttribute(TAG_ARG_CLASS, className);
            root.appendChild(itemNd);

            String[] argsStr = planElementsStartArgs.get(className);
            if (argsStr != null && argsStr.length > 0) {
                StringBuilder sb = new StringBuilder(argsStr.length * 3);
                Arrays.asList(argsStr).stream().forEach(str -> sb.append(str).append(" "));
                itemNd.setAttribute(TAG_ARG_ARGS, sb.toString().trim());
            }

            Element emlXml = planElementsTypeXml.get(className);
            if (emlXml != null) {
                try {
                    emlXml = (Element) doc.adoptNode(emlXml);
                    root.appendChild(emlXml);
                }
                catch (DOMException e) {
                    NeptusLog.pub().warn(e);
                }
            }
        }

        return root;
    }

    public static void main(String[] args) {
        String xmlStr = "<planElements>\r\n"
                + "        <item class=\"pt.lsts.neptus.mp.element.RendezvousPointsPlanElement\" args=\"gsgs gstsg\" >\r\n"
                + "            <RendezvousPoints>\r\n" + "                <point>\r\n"
                + "                    <latDeg>41.0</latDeg>\r\n" + "                    <lonDeg>-8.0</lonDeg>\r\n"
                + "                </point>\r\n" + "                <point>\r\n"
                + "                    <latDeg>41.17785</latDeg>\r\n"
                + "                    <lonDeg>-8.59796</lonDeg>\r\n" + "                </point>\r\n"
                + "            </RendezvousPoints>" + "        </item>\r\n"
                + "        <item class=\"pt.lsts.neptus.mp.element.OperationLimitsPlanElement\" args=\"limits=true\" />\r\n"
                + "    </planElements>";
        PlanElementsFactory pef = new PlanElementsFactory("lauv-noptilus-1", xmlStr);
        System.out.println(pef.asXml());

        ArrayList<IPlanElement<?>> pElms = pef.getPlanElementsInstances();

        pElms.forEach(p -> System.out.println("PElem: " + p.getName() + " :: " + p.getHoldingTypeName() + " :: "
                + p.getHoldingType() + " :: " + p.getElementAsXml()));
    }
}
