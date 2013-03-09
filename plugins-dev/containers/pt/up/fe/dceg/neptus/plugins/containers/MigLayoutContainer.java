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
 * Author: jqcorreia
 * 29/11/2011
 */
package pt.up.fe.dceg.neptus.plugins.containers;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import net.miginfocom.swing.MigLayout;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.ContainerSubPanel;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginDescription.CATEGORY;
import pt.up.fe.dceg.neptus.plugins.containers.propeditor.MiGLayoutXmlPropertyEditor;

/**
 * @author jqcorreia
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "José Quadrado", version = "1.0.0", name = "Console Layout: MiG", description = "Container using MigLayout",
// documentation = "",
icon = "pt/up/fe/dceg/neptus/plugins/containers/layout.png", category = CATEGORY.INTERFACE)
public class MigLayoutContainer extends ContainerSubPanel implements ConfigurationListener, LayoutProfileProvider {

    @NeptusProperty(name = "XML Definitions", description = "XML layout definition", editorClass = MiGLayoutXmlPropertyEditor.class,
 distribution = DistributionEnum.DEVELOPER)
    public String xmlDef = "";

    @NeptusProperty(name = "Current Profile", description = "Name of the current active profile",
 distribution = DistributionEnum.DEVELOPER)
    public String currentProfile = "";

    private final ArrayList<String> profileList = new ArrayList<String>();

    private JMenu profilesMenu;

    public MigLayoutContainer(ConsoleLayout console) {
        super(console);
        setLayout(new MigLayout("ins 0"));
    }

    @Override
    public void init() {
        loadProfiles();
        // Enforce at least one profile
        if (profileList.size() >= 1 && currentProfile.equals("")) {
            changeProfile(profileList.get(0));
            applyLayout();
        }
        else {
            changeProfile(currentProfile); // This call maybe redundant but is needed for profile menu update
            applyLayout();
        }
        super.init();
    }

    private void loadProfiles() {
        this.removeAll();
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

        profilesMenu = console.getOrCreateJMenu(new String[] { I18n.text("Profiles") });
        for (String name : profileList) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(new AbstractAction(name) {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    changeProfile(e.getActionCommand());
                    applyLayout();
                }
            });
            profilesMenu.add(item);
        }
    }

    public void changeProfile(String profileName) {
        currentProfile = profileName;

        for (int i = 0; i < profilesMenu.getItemCount(); i++) {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) profilesMenu.getItem(i);
            item.setSelected(item.getText().equals(profileName));
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
        if (profilesMenu != null) {
            profilesMenu.getParent().remove(profilesMenu);
            profilesMenu = null;
        }
    }

    @Override
    public void propertiesChanged() {
        if (panels.size() > 0) { // Meaning it is not the first run where the properties are all empty
            loadProfiles();
            changeProfile(currentProfile);
            applyLayout();
        }
    }

    public void applyLayout() {
        this.removeAll();
        if (xmlDef.isEmpty())
            return;
        SAXReader reader = new SAXReader();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(xmlDef));

        // Parse new XML layout definition
        try {
            Document doc = reader.read(is);
            @SuppressWarnings("unchecked")
            List<Node> nodes = doc.selectNodes("//profiles/profile");
            for (Node node : nodes) {
                if (node.valueOf("@name").equals(currentProfile)) {
                    System.out.println(node.valueOf("@name") + "  true");
                    parse(node, this);
                }
            }

        }
        catch (DocumentException e) {
            NeptusLog.pub().error("reading inner xml", e);
        }
        getConsole().revalidate();
        getConsole().repaint();
    }

    public SubPanel parse(Node node, JComponent parent) {
        Element e = (Element) node;
        if (!node.hasContent()) {
            return null;
        }
        else {
            @SuppressWarnings("unchecked")
            List<Element> elements = e.elements();
            for (Element element : elements) {
                if ("container".equals(element.getName())) {
                    String layoutparam = element.attributeValue("layoutparam");
                    String colparam = element.attributeValue("colparam");
                    String rowparam = element.attributeValue("rowparam");
                    String addParam = element.attributeValue("param");
                    String type = element.attributeValue("type") == null ? "" : element.attributeValue("type");
                    if ("tabcontainer".equals(type)) {
                        JTabbedPane tabbedPane = new JTabbedPane();
                        parent.add(tabbedPane, addParam);
                        parse(element, tabbedPane);
                    }
                    else {
                        SubPanel container = new SubPanel(console);
                        container.setLayout(new MigLayout(layoutparam, colparam, rowparam));
                        parent.add(container, addParam);
                        parse(element, container);
                    }
                }
                if ("child".equals(element.getName())) {
                    String name = element.attributeValue("name");
                    String param = element.attributeValue("param");
                    SubPanel child = this.getSubPanelByName(name);
                    if (child != null) {
                        parent.add(child, param);
                    }
                }
                if ("tab".equals(element.getName())) {
                    JTabbedPane tabsPane = (JTabbedPane) parent;
                    String name = element.attributeValue("tabname");
                    String layoutparam = element.attributeValue("layoutparam");
                    String colparam = element.attributeValue("colparam");
                    String rowparam = element.attributeValue("rowparam");
                    JPanel tab = new JPanel();
                    tab.setLayout(new MigLayout(layoutparam, colparam, rowparam));
                    tabsPane.addTab(name, null, tab, name);
                    parse(element, tab);
                }
            }
            return null;
        }

    }

    @Override
    public void addSubPanel(SubPanel panel) {
        panels.add(panel);
    }

    @Override
    public void removeSubPanel(SubPanel sp) {
        panels.remove(sp);
        this.remove(sp);
        applyLayout();
        sp.clean();
    }

    @Override
    public void clean() {
        super.clean();
        removeProfilesMenu();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.containers.LayoutProfileProvider#getActiveProfile()
     */
    @Override
    public String getActiveProfile() {
        return currentProfile;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.containers.LayoutProfileProvider#setActiveProfile(java.lang.String)
     */
    @Override
    public boolean setActiveProfile(String name) {
        if (profileList.contains(name)) {
            changeProfile(name);
            applyLayout();
        }
        else
            return false;

        return true;
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.containers.LayoutProfileProvider#listProfileNames()
     */
    @Override
    public String[] listProfileNames() {
        return profileList.toArray(new String[0]);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.containers.LayoutProfileProvider#supportsMaximizePanelOnContainer()
     */
    @Override
    public boolean supportsMaximizePanelOnContainer() {
        return false;
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.containers.LayoutProfileProvider#maximizePanelOnContainer(java.awt.Component)
     */
    @Override
    public boolean maximizePanelOnContainer(Component comp) {
        return false;
    }
}
