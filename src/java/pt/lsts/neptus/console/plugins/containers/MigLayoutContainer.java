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
 * Author: José Correia
 * 29/11/2011
 */
package pt.lsts.neptus.console.plugins.containers;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.console.plugins.containers.propeditor.MiGLayoutXmlPropertyEditor;
import pt.lsts.neptus.events.NeptusEventLayoutChanged;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;

/**
 * @author jqcorreia
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(author = "José Quadrado, Paulo Dias", version = "2.0.0", name = "Console Layout: MigLayout", description = "This container uses MigLayout manager", icon = "pt/lsts/neptus/plugins/containers/layout.png", category = CATEGORY.INTERFACE)
public class MigLayoutContainer extends ContainerSubPanel implements ConfigurationListener, LayoutProfileProvider {

    public static final String LAYOUT_SCHEMA = "miglayout-container.xsd";

    @NeptusProperty(name = "XML Definitions", description = "XML layout definition", editorClass = MiGLayoutXmlPropertyEditor.class, distribution = DistributionEnum.DEVELOPER)
    public String xmlDef = "<profiles>\n  <profile name=\"Normal\">\n    <container layoutparam=\"ins 0\" param=\"w 100%, h 100%\">\n      <child name=\"Map Panel\" param=\"w 100%, h 100%\"/>\n    </container>\n  </profile>\n</profiles>";

    @NeptusProperty(name = "Current Profile", description = "Name of the current active profile", distribution = DistributionEnum.DEVELOPER)
    public String currentProfile = "Normal";

    private final ArrayList<String> profileList = new ArrayList<String>();

    private JMenu profilesMenu;

    private Map<String, JDialog> windowMap = new HashMap<>();
    private ArrayList<String> usedWindows = new ArrayList<>();
    
    public MigLayoutContainer(ConsoleLayout console) {
        super(console);
        NeptusEvents.register(this);
        setLayout(new MigLayout("ins 0"));
    }

    @Override
    public void init() {
        loadProfiles();
        // Enforce at least one profile
        if (profileList.size() >= 1 && currentProfile.equals("")) {
            changeProfile(profileList.get(0));
            applyLayout(this.xmlDef);
        }
        else {
            if(currentProfile != "")
                changeProfile(currentProfile); // This call maybe redundant but is needed for profile menu update
            applyLayout(this.xmlDef);
        }
        super.init();
        
        // To allow the windows not to appear before the console is visible
        addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && e.getChanged() == getConsole())
                    setVisibilityToWindows();
            }
        });
    }

    private void loadProfiles() {
        removeAllComponentsFromPanelAndWindows();
        if (xmlDef.isEmpty())
            return;

        SAXReader reader = new SAXReader();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xmlDef));

        // Parse new XML layout definition
        profileList.clear();
        try {
            Document doc = reader.read(is);
            Element root = doc.getRootElement();
            Iterator<?> i = root.elementIterator();

            for (; i.hasNext();) {
                Element e = (Element) i.next();
                if (e.getQualifiedName().equals("profile")) {
                    String profileName = e.attributeValue("name");
                    profileList.add(profileName);
                }
            }
        }
        catch (DocumentException e) {
            NeptusLog.pub().error("mig container loading profiles", e);
        }

        removeProfilesMenu(); // Cleanup the menu before (re)loading the profiles

        profilesMenu = getConsole().getOrCreateJMenu(new String[] { I18n.text("Profiles") });
        for (final String name : profileList) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(new AbstractAction(I18n.text(name)) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (name != "") {
                        changeProfile(name);
                    }
                    applyLayout(xmlDef);
                }
            });

            profilesMenu.add(item);
        }
    }

    private void removeAllComponentsFromPanelAndWindows() {
        this.removeAll();
        for (JDialog w : windowMap.values())
            w.getContentPane().removeAll();
    }

    public void changeProfile(String profileName) {
        currentProfile = profileName;
        NeptusLog.pub().info("currentProfile: "+currentProfile);
        if(profileName!=""){
            for (int i = 0; i < profilesMenu.getItemCount(); i++) {
                JCheckBoxMenuItem item = (JCheckBoxMenuItem) profilesMenu.getItem(i);
                item.setSelected(item.getText().equals(profileName));
            }
        }
        else{
            profileName="";
        }
        propagateActiveProfileChange(profileName);
    }

    private void propagateActiveProfileChange(String profileName) {
        for (Component cp : panels) {
            if (cp instanceof LayoutProfileProvider)
                ((LayoutProfileProvider) cp).setActiveProfile(profileName);
        }
    }

    private void removeProfilesMenu() {
        if (profilesMenu != null && profilesMenu.getParent() != null) {
            profilesMenu.getParent().remove(profilesMenu);
            profilesMenu = null;
        }
    }

    @Override
    public void propertiesChanged() {
        if (panels.size() > 0) { // Meaning it is not the first run where the properties are all empty
            loadProfiles();
            changeProfile(currentProfile);
            applyLayout(this.xmlDef);
        }
    }

    @Subscribe
    public void consume(NeptusEventLayoutChanged e) {
        this.applyLayout(e.getLayout());
    }

    public void applyLayout(String xml) {
        removeAllComponentsFromPanelAndWindows();
        if (xml.isEmpty())
            return;
        SAXReader reader = new SAXReader();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xml));

        usedWindows.clear();
        
        // Parse new XML layout definition
        try {
            Document doc = reader.read(is);
            @SuppressWarnings("unchecked")
            List<Node> nodes = doc.selectNodes("//profiles/profile");
            for (Node node : nodes) {
                if (node.valueOf("@name").equals(currentProfile)) {
                    NeptusLog.pub().debug("Loaded profile " + node.valueOf("@name"));
                    parse(node, this);
                }
            }
        }
        catch (DocumentException e) {
            NeptusLog.pub().error("reading inner xml", e);
        }
        
        setVisibilityToWindows();

        getConsole().revalidate();
        getConsole().repaint();
    }

    private void setVisibilityToWindows() {
        for (String name : windowMap.keySet()) {
            JDialog w = windowMap.get(name);
            if (!usedWindows.contains(name)) {
                w.setVisible(false);
            }
            else {
                // To allow the windows not to appear before the console is visible
                w.setVisible(getConsole().isVisible());
            }
            w.revalidate();
            w.repaint();
        }
    }

    public void parse(Node node, JComponent parent) {
        Element e = (Element) node;
        if (!node.hasContent()) {
            return;
        }
        else {
            @SuppressWarnings("unchecked")
            List<Element> elements = e.elements();
            for (Element element : elements) {
                if ("container".equals(element.getName())) {
                    String layoutparam = element.attributeValue("layoutparam"); // optional
                    String colparam = element.attributeValue("colparam"); // optional
                    String rowparam = element.attributeValue("rowparam"); // optional
                    String addParam = element.attributeValue("param"); // optional
                    String type = element.attributeValue("type") == null ? "" : element.attributeValue("type");
                    // Let us try the tab type, his is deprecated, the tab element should be used
                    if (element.selectSingleNode("tab") != null || "tabcontainer".equals(type)) {
                        JTabbedPane tabbedPane = new JTabbedPane();
                        parent.add(tabbedPane, addParam);
                        parse(element, tabbedPane);
                    }
                    else {
                        ConsolePanel container = new ConsolePanel(getConsole()) {
                            private static final long serialVersionUID = 8543725153078587308L;

                            @Override
                            public void cleanSubPanel() {
                            }

                            @Override
                            public void initSubPanel() {
                            }
                        };
                        container.setLayout(new MigLayout(layoutparam, colparam, rowparam));
                        parent.add(container, addParam);
                        parse(element, container);
                    }
                }
                else if ("child".equals(element.getName())) {
                    String name = element.attributeValue("name");
                    String param = element.attributeValue("param"); // optional
                    ConsolePanel child = this.getSubPanelByName(name);
                    if (child != null) {
                        parent.add(child, param);
                    }
                }
                else if ("tab".equals(element.getName())) {
                    JTabbedPane tabsPane = (JTabbedPane) parent;
                    String name = element.attributeValue("tabname");
                    String layoutparam = element.attributeValue("layoutparam"); // optional
                    String colparam = element.attributeValue("colparam"); // optional
                    String rowparam = element.attributeValue("rowparam"); // optional
                    JPanel tab = new JPanel();
                    tab.setLayout(new MigLayout(layoutparam, colparam, rowparam));
                    tabsPane.addTab(name, null, tab, name);
                    parse(element, tab);
                }
                else if ("window".equals(element.getName())) {
                    String nameparam = element.attributeValue("name");
                    if (nameparam == null || nameparam.length() <= 0) {
                        NeptusLog.pub().warn("Invalid window name, ignoring.");
                        continue;
                    }
                    nameparam = nameparam.trim();
                    String layoutparam = element.attributeValue("layoutparam"); // optional
                    String colparam = element.attributeValue("colparam"); // optional
                    String rowparam = element.attributeValue("rowparam"); // optional

                    ConsolePanel container = new ConsolePanel(getConsole()) {
                        private static final long serialVersionUID = 8543725153078587308L;

                        @Override
                        public void cleanSubPanel() {
                        }

                        @Override
                        public void initSubPanel() {
                        }
                    };
                    
                    container.setLayout(new MigLayout(layoutparam, colparam, rowparam));

                    JDialog window;
                    if (windowMap.containsKey(nameparam)) {
                        window = windowMap.get(nameparam);
                        window.getContentPane().removeAll();
                        window.revalidate();
                        window.repaint();
                    }
                    else {
                        window = new JDialog(getConsole());
                        window.setVisible(false);
                        window.setSize(getConsole().getWidth(), getConsole().getHeight());
                        window.setTitle(nameparam);
                        window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                        windowMap.put(nameparam, window);
                    }
                    window.setLayout(new BorderLayout());
                    window.getContentPane().add(container, BorderLayout.CENTER);
                    usedWindows.add(nameparam);

                    parse(element, container);
                }
            }
            return;
        }
    }

    @Override
    protected boolean addSubPanelExtra(ConsolePanel panel) {
        return true;
    }
    
    @Override
    protected void addSubPanelFinishUp() {
        if (!isChildsBulkLoad())
            applyLayout(this.xmlDef);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ContainerSubPanel#addSubPanelBulkFinishUp()
     */
    @Override
    protected void addSubPanelBulkFinishUp() {
        applyLayout(this.xmlDef);
    }
    
    @Override
    public void removeSubPanelExtra(ConsolePanel sp) {
        applyLayout(this.xmlDef);
    }

    @Override
    public void clean() {
        removeAllComponentsFromPanelAndWindows();
        super.clean();
        removeProfilesMenu();
        for (JDialog window : windowMap.values()) {
            window.removeAll();
            window.dispose();
        }
        windowMap.clear();
        usedWindows.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.containers.LayoutProfileProvider#getActiveProfile()
     */
    @Override
    public String getActiveProfile() {
        return currentProfile;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.containers.LayoutProfileProvider#setActiveProfile(java.lang.String)
     */
    @Override
    public boolean setActiveProfile(String name) {
        if (profileList.contains(name)) {
            changeProfile(name);
            applyLayout(this.xmlDef);
        }
        else
            return false;

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.containers.LayoutProfileProvider#listProfileNames()
     */
    @Override
    public String[] listProfileNames() {
        return profileList.toArray(new String[0]);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.containers.LayoutProfileProvider#supportsMaximizePanelOnContainer()
     */
    @Override
    public boolean supportsMaximizePanelOnContainer() {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.containers.LayoutProfileProvider#maximizePanelOnContainer(java.awt.Component)
     */
    @Override
    public boolean maximizePanelOnContainer(Component comp) {
        return false;
    }
}
