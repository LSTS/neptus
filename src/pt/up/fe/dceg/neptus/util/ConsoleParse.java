/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Rui Gonçalves
 * 2006/08/04
 */
package pt.up.fe.dceg.neptus.util;

import java.awt.MouseInfo;
import java.awt.Rectangle;
import java.io.File;
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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.loader.FileHandler;
import pt.up.fe.dceg.neptus.plugins.configWindow.SettingsWindow;
import pt.up.fe.dceg.neptus.plugins.containers.MigLayoutContainer;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

public class ConsoleParse implements FileHandler {

    public static ConsoleLayout consoleLayoutLoader(String consoleURL) {
        ConsoleLayout console = new ConsoleLayout();
        parseFile(consoleURL, console);

        console.setConsoleChanged(false);

        Rectangle screen = MouseInfo.getPointerInfo().getDevice().getDefaultConfiguration().getBounds();
        console.setLocation(screen.x, screen.y);
        
        console.imcOn();
        console.setVisible(true);

        return console;
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
        for (Iterator<?> i = list.iterator(); i.hasNext();) {
            Element element = (Element) i.next();
            
            if ("mainpanel".equals(element.getName())) {
                Attribute attribute = (Attribute) element.selectSingleNode("@name");
                if ("console main panel".equals(attribute.getValue())) {
                    ConfigFetch.mark("main panel");
                    parseConsoleMainPanel(element, console);
                    ConfigFetch.benchmark("main panel");
                }
            }
        }
        ConfigFetch.mark("reinit");
        console.initSubPanels();
        ConfigFetch.benchmark("reinit");
        
        // TODO CORE Settings plugin
        List<SubPanel> pluginSubPanel = console.getSubPanels();
        SubPanel migLayout = pluginSubPanel.get(0);
        if (migLayout != null && migLayout instanceof MigLayoutContainer) {
            List<SubPanel> subPanels = ((MigLayoutContainer) migLayout).getSubPanels();
            pluginSubPanel.addAll(subPanels);
        }
        Vector<PropertiesProvider> pluginPropProvider = subPanelToPropertiesProvider(pluginSubPanel);
        SettingsWindow settingsPopUpWindow = new SettingsWindow(console, pluginPropProvider);
        settingsPopUpWindow.init();
        pluginSubPanel.add(settingsPopUpWindow);
    }

    private static Vector<PropertiesProvider> subPanelToPropertiesProvider(List<SubPanel> pluginSubPanel) {
        // java does not convert for interfaces so a new Vector is created
        Vector<PropertiesProvider> pluginPropProvider = new Vector<PropertiesProvider>();
        for (SubPanel subPanel : pluginSubPanel) {
            pluginPropProvider.add(subPanel);
        }
        return pluginPropProvider;
    }


    public static void parseDocument(Document doc, ConsoleLayout console, String consoleURL) {
        parseElement((Element) doc.selectSingleNode("//" + ConsoleLayout.DEFAULT_ROOT_ELEMENT), console, consoleURL);
    }

    public static void parseFile(String consoleURL, ConsoleLayout console) {
        Document doc = null;

        try {
            SAXReader reader =  new SAXReader();
            doc = reader.read(consoleURL);
            Element rootconsole = (Element) doc.selectSingleNode("//" + ConsoleLayout.DEFAULT_ROOT_ELEMENT);
            parseElement(rootconsole, console, consoleURL);

            console.setXmlDoc(doc);
            console.setFileName(new File(consoleURL));
            console.setConsoleChanged(false);

        }
        catch (DocumentException e) {
            GuiUtils.errorMessage(null, e);
            NeptusLog.pub().error(" Console Base open file error [" + e.getStackTrace() + "]", e);
        }
    }

    public static void parseConsoleMainPanel(Node node, ConsoleLayout console) {
        List<?> list = node.selectNodes("*");
        ConfigFetch.mark("construct container");
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            SubPanel subpanel = null;
            // process subpanel tag
            if ("subpanel".equals(element.getName())) {
                Attribute attribute = element.attribute("class");
                try {
                    Class<?> clazz = Class.forName(attribute.getValue());
                    try {
                        subpanel = (SubPanel) clazz.getConstructor(ConsoleLayout.class).newInstance(console);
                        console.getMainPanel().addSubPanel(subpanel);
                        ConfigFetch.benchmark("construct container");
                        ConfigFetch.mark("in element of container");
                        subpanel.inElement(element);
                        ConfigFetch.benchmark("in element of container");
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error("creating subpanel new instance ", e);
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Error parsing " + attribute.getValue(), e);
                }
            }

        }
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
     * @see pt.up.fe.dceg.neptus.loader.FileHandler#handleFile(java.io.File)
     */
    @Override
    public void handleFile(File f) {
        ConsoleLayout c = consoleLayoutLoader(f.getAbsolutePath());
        c.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static ConsoleLayout testSubPanel(Class<?> subPanelClass) {
        return testSubPanel(subPanelClass, null);
    }

    // TODO See if these make sense
    // From GuiUtils 7/12/2008
    public static ConsoleLayout testSubPanel(Class<?> subPanelClass, PlanType plan) {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();

        ConsoleLayout cl = new ConsoleLayout();
        cl.setVisible(true);
        cl.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (plan != null) {
            cl.setMission(plan.getMissionType());
            cl.setMainSystem(plan.getVehicle());
            cl.setPlan(plan);
        }

        try {
            SubPanel panel = (SubPanel) subPanelClass.getConstructor(ConsoleLayout.class).newInstance(cl);
            panel.setBounds(5, 5, cl.getMainPanel().getWidth() - 10, cl.getMainPanel().getHeight() - 10);
            panel.setBounds(10, 10, (int) panel.getPreferredSize().getWidth(), (int) panel.getPreferredSize()
                    .getHeight());

            cl.getMainPanel().addSubPanel(panel, 10, 10);
            return cl;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // From GuiUtils 7/12/2008
    public static ConsoleLayout dummyConsole(SubPanel... panelsToTest) {
        ConfigFetch.initialize();
        ConsoleLayout layout = new ConsoleLayout();

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
