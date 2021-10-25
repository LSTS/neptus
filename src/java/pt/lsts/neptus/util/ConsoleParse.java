/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Rui Gonçalves
 * 2006/08/04
 */
package pt.lsts.neptus.util;

import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.JFrame;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.IConsoleInteraction;
import pt.lsts.neptus.console.IConsoleLayer;
import pt.lsts.neptus.console.plugins.planning.MapPanel;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.loader.FileHandler;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.conf.ConfigFetch;

public class ConsoleParse implements FileHandler {

    public static ConsoleLayout consoleLayoutLoader(String consoleURL) {
        ConsoleLayout console = ConsoleLayout.forge(consoleURL);

        Rectangle screen = MouseInfo.getPointerInfo().getDevice().getDefaultConfiguration().getBounds();
        console.setLocation(screen.x, screen.y);

        console.setVisible(true);
        return console;
    }
    
    @Override
    public String getName() {
        return "Console";
    }

    public static Document initparse(String consoleURL) throws DocumentException {
        SAXReader reader =  new SAXReader();
        Document doc = reader.read(consoleURL);
        return doc;
    }

    public static void parseString(String str, ConsoleLayout console, String consoleURL) {
        try {
            boolean valid = validate(str);
            if (!valid)
                NeptusLog.pub().error(" Console Base parse XML not valid!!");
            Document document = DocumentHelper.parseText(str);
            parseDocument(document, console, consoleURL);
        }
        catch (DocumentException e) {
            GuiUtils.errorMessage(null, e);
            NeptusLog.pub().error(" Console Base parse XML string", e);
            return;
        }
    }

    public static void parseElement(Element rootconsole, ConsoleLayout console, String consoleUrl) {
        InfiniteProgressPanel progressPanel = InfiniteProgressPanel.createInfinitePanelBeans(I18n.text("Loading"));
        Component gPane = console.getGlassPane();
        console.setGlassPane(progressPanel);
        try {
            List<?> list = rootconsole.selectNodes("@*");
            for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
                Attribute attribute = (Attribute) iter.next();

                if ("name".equals(attribute.getName())) {
                    console.setName(attribute.getValue());
                }

                if ("mission-file".equals(attribute.getName())) {
                    String missionFile = null;
                    if ((consoleUrl != null) || (!"".equalsIgnoreCase(consoleUrl)))
                        missionFile = ConfigFetch.resolvePathWithParent(consoleUrl, attribute.getText());
                    else
                        missionFile = attribute.getText();

                    if (!new File(missionFile).canRead())
                        GuiUtils.errorMessage(ConfigFetch.getSuperParentFrame(), "Console loading",
                                "Error setting main mission: File not found");
                    else
                        console.setMission(new MissionType(missionFile));
                }
                if ("main-vehicle".equals(attribute.getName())) {
                    console.addSystem(attribute.getValue());
                    console.setMainSystem(attribute.getValue());
                }

                if ("width".equals(attribute.getName())) {
                    console.setSize(Integer.parseInt(attribute.getValue()), console.getHeight());
                    console.repaint();
                }
                if ("height".equals(attribute.getName())) {
                    console.setSize(console.getWidth(), Integer.parseInt(attribute.getValue()));
                    console.repaint();
                }
                if ("resizable".equals(attribute.getName())) {
                    boolean resizable = attribute.getValue().equals("true");

                    if (resizable) {
                        console.setResizableConsole(true);
                        console.setResizable(true);
                        console.repaint();
                    }
                    else {
                        console.setResizableConsole(false);
                        console.setResizable(false);
                    }
                }
            }
            list = rootconsole.selectNodes("/" + ConsoleLayout.DEFAULT_ROOT_ELEMENT + "/*");
            List<IConsoleLayer> layers = new ArrayList<>();
            List<IConsoleInteraction> interactions = new ArrayList<>();
            
            for (Iterator<?> i = list.iterator(); i.hasNext();) {
                Element element = (Element) i.next();
                
                if ("mainpanel".equals(element.getName())) {
                    Attribute attribute = (Attribute) element.selectSingleNode("@name");
                    if ("console main panel".equals(attribute.getValue())) {
                        parseConsoleMainPanel(element, console);
                    }
                }
                else if ("layers".equals(element.getName())) {
                    layers = parseConsoleLayers(element, console);
                }
                else if ("interactions".equals(element.getName())) {
                    interactions = parseConsoleInteractions(element, console);
                }
                    
            }

            console.initSubPanels();
            
            // Add map layers and interactions
            Vector<MapPanel> maps = console.getSubPanelsOfClass(MapPanel.class);
            if (maps.isEmpty() && !layers.isEmpty()) {
                NeptusLog.pub().error("Cannot add "+layers.size()+" layers because there is no MapPanel");            
            }
            if (maps.isEmpty() && !interactions.isEmpty()) {
                NeptusLog.pub().error("Cannot add "+interactions.size()+" interactions because there is no MapPanel");            
            }
            else {
                for (IConsoleLayer layer : layers) {
                    console.addMapLayer(layer);
                }
                
                for (IConsoleInteraction inter : interactions) {
                    console.addInteraction(inter);
                }
            }
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e.getMessage());
        }
        finally {
            console.setGlassPane(gPane);
        }
    }

    public static void parseDocument(Document doc, ConsoleLayout console, String consoleURL) {
        parseElement((Element) doc.selectSingleNode("//" + ConsoleLayout.DEFAULT_ROOT_ELEMENT), console, consoleURL);
    }

    public static void parseFile(String consoleURL, ConsoleLayout console) {
        Document doc = null;
        
        try {
            File fx = new File(consoleURL);
            URL url = fx.toURI().toURL();

            SAXReader reader =  new SAXReader();
            doc = reader.read(url);
            Element rootconsole = (Element) doc.selectSingleNode("//" + ConsoleLayout.DEFAULT_ROOT_ELEMENT);
            parseElement(rootconsole, console, consoleURL);

            console.setXmlDoc(doc);
            console.setFileName(new File(consoleURL));
            console.setConsoleChanged(false);

        }
        catch (Exception e) {
            GuiUtils.errorMessage(null, e);
            NeptusLog.pub().error(" Console Base open file " + consoleURL + " error [" + e.getStackTrace() + "]", e);
        }
    }
    
    private static Vector<IConsoleLayer> parseConsoleLayers(Node node, ConsoleLayout console) {
        List<?> list = node.selectNodes("*");
        Vector<IConsoleLayer> ret = new Vector<>();
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            try {
                String className = element.attribute("class").getValue();
                IConsoleLayer cp = (IConsoleLayer) Class.forName(className).getDeclaredConstructor().newInstance();
                cp.parseXmlElement(element);
                ret.add(cp);
            }
            catch (Exception e) {
                e.printStackTrace();
                NeptusLog.pub().error("Error parsing " + element.asXML());
            }
        }        
        
        Collections.sort(ret, new Comparator<IConsoleLayer>() {
            @Override
            public int compare(IConsoleLayer o1, IConsoleLayer o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        
        return ret;
    }
    
    private static Vector<IConsoleInteraction> parseConsoleInteractions(Node node, ConsoleLayout console) {
        List<?> list = node.selectNodes("*");
        Vector<IConsoleInteraction> ret = new Vector<>();
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            try {
                String className = element.attribute("class").getValue();
                //FIXME
                IConsoleInteraction cp = (IConsoleInteraction) Class.forName(className).getDeclaredConstructor().newInstance();
                cp.parseXmlElement(element);
                ret.add(cp);
            }
            catch (Exception e) {
                NeptusLog.pub().error("Error parsing " + element.asXML());
            }
        }      
        
        Collections.sort(ret, new Comparator<IConsoleInteraction>() {
            @Override
            public int compare(IConsoleInteraction o1, IConsoleInteraction o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        return ret;
    }
    
    

    private static Vector<ConsolePanel> parseConsoleMainPanel(Node node, ConsoleLayout console) {
        List<?> list = node.selectNodes("*");
        Vector<ConsolePanel> panels = new Vector<>();
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            ConsolePanel subpanel = null;
            // process subpanel tag
            
            switch (element.getName()) {
                case "subpanel":
                case "panel":
                case "widget":
                    Attribute attribute = element.attribute("class");
                    try {
                        Class<?> clazz = Class.forName(attribute.getValue());
                        try {
                            subpanel = (ConsolePanel) clazz.getConstructor(ConsoleLayout.class).newInstance(console);
                            console.getMainPanel().addSubPanel(subpanel);
                            panels.add(subpanel);
                            subpanel.inElement(element);
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error("creating subpanel new instance ", e);
                        }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error("Error parsing " + attribute.getValue(), e);
                    }
                    break;                
                default:
                    NeptusLog.pub().error("Unrecognized console component: "+element.getName());
                    break;
            }
        }
        
        return panels;
    }

    public static void parseCommunications(Node node, ConsoleLayout console) {
        List<?> list = node.selectNodes("*");

        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();

            if ("neptus-communitation".equals(element.getName())) {
                Attribute active = element.attribute("active");
                Attribute system = element.attribute("system");
                if (active == null || system == null) {
                    NeptusLog.pub().error(" Console Base open file error XML seaware config");
                    return;
                }
                //console.setVehicleNeptusCommunications(system.getValue(), Boolean.parseBoolean(active.getValue()));
            }
        }
    }

    /**
     * @param xml
     * @return
     */
    public static boolean validate(String xml) {
        try {
            String sLoc = new File(ConfigFetch.getConsoleSchemaLocation()).getAbsoluteFile().toURI().toString();
            XMLValidator xmlVal = new XMLValidator(xml, sLoc);
            boolean ret = xmlVal.validate();
            return ret;
        }
        catch (Exception e) {
            GuiUtils.errorMessage(null, e);
            NeptusLog.pub().error("Console:validate", e);
            return false;
        }
    }

    /**
     * @param file
     * @return
     */
    public static boolean validate(File file) {
        try {
            String xml = FileUtil.getFileAsString(file);
            return validate(xml);
        }
        catch (Exception e) {
            GuiUtils.errorMessage(null, e);
            NeptusLog.pub().error("Console:validate", e);
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.loader.FileHandler#handleFile(java.io.File)
     */
    @Override
    public Window handleFile(File f) {
        ConsoleLayout console = ConsoleLayout.forge(f.getAbsolutePath());
        //ConsoleLayout c = consoleLayoutLoader(f.getAbsolutePath());
        //c.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return console;
    }

    public static ConsoleLayout testSubPanel(Class<?> subPanelClass) {
        return testSubPanel(subPanelClass, null);
    }

    // TODO See if these make sense
    // From GuiUtils 7/12/2008
    public static ConsoleLayout testSubPanel(Class<?> subPanelClass, PlanType plan) {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();

        ConsoleLayout cl = ConsoleLayout.forge();
        cl.setVisible(true);
        cl.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (plan != null) {
            cl.setMission(plan.getMissionType());
            cl.setMainSystem(plan.getVehicle());
            cl.setPlan(plan);
        }

        try {
            ConsolePanel panel = (ConsolePanel) subPanelClass.getConstructor(ConsoleLayout.class).newInstance(cl);
            panel.setBounds(5, 5, cl.getMainPanel().getWidth() - 10, cl.getMainPanel().getHeight() - 10);
            panel.setBounds(10, 10, (int) panel.getPreferredSize().getWidth(), (int) panel.getPreferredSize()
                    .getHeight());

            cl.getMainPanel().removeAll();
            cl.getMainPanel().addSubPanel(panel, 10, 10);
            return cl;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // From GuiUtils 7/12/2008
    public static ConsoleLayout dummyConsole(ConsolePanel... panelsToTest) {
        ConfigFetch.initialize();
        ConsoleLayout layout = ConsoleLayout.forge();

        layout.setSize(800, 600);
        layout.setMainSystem("lauv-seacon-1");
        int curX = 0;
        for (int i = 0; i < panelsToTest.length; i++) {
            layout.getMainPanel().addSubPanel(panelsToTest[i], curX, 0);
            curX += panelsToTest[i].getWidth();
        }
        layout.setVisible(true);
        GuiUtils.centerOnScreen(layout);
        layout.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        return layout;
    }

}
