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
 * Author: Paulo Dias
 * 2/10/2010
 */
package pt.lsts.neptus.console.plugins.containers;

import java.awt.Component;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.GroupLayout.SequentialGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.LayoutStyle;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.console.MainPanel;
import pt.lsts.neptus.console.plugins.containers.propeditor.HorizontalGroupPropertyEditor;
import pt.lsts.neptus.console.plugins.containers.propeditor.LinkSizeHorizontalPropertyEditor;
import pt.lsts.neptus.console.plugins.containers.propeditor.LinkSizeVerticalPropertyEditor;
import pt.lsts.neptus.console.plugins.containers.propeditor.ProfilesPropertyEditor;
import pt.lsts.neptus.console.plugins.containers.propeditor.VerticalGroupPropertyEditor;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsLoader;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.XMLUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Paulo Dias", version = "1.3.1", name = "Console Layout: Group", description = "Container using GroupLayout (see 'http://download.oracle.com/javase/"
        + "tutorial/uiswing/layout/group.html' and 'http://download.oracle.com/javase/"
        + "tutorial/uiswing/layout/groupExample.html')",
// documentation = "",
icon = "pt/lsts/neptus/plugins/containers/layout.png")
public class GroupLayoutContainer extends ContainerSubPanel implements ConfigurationListener, LayoutProfileProvider {

    public static final String GROUP_LAYOUT_SCHEMA = "console-group-layout.xsd";

    protected static final String NONE_PROFILE_STRING = "-- None --";

    @NeptusProperty(name = "Auto Create Container Gaps", description = "To automatic create gaps arround the container", 
            distribution = DistributionEnum.DEVELOPER)
    public boolean autoCreateContainerGaps = false;

    @NeptusProperty(name = "Auto Create Gaps", description = "To automatic create gaps arround the components",
            distribution = DistributionEnum.DEVELOPER)
    public boolean autoCreateGaps = false;

    @NeptusProperty(name = "Honors Visibility", distribution = DistributionEnum.DEVELOPER,
            description = "Set this so if  component is hidden is if it's not there at all. For most uses leave it at true.")
    public boolean honorsVisibility = true;

    @NeptusProperty(name = "Horizontal Group", description = "You can use the index number or the name as listed.", 
            editorClass = HorizontalGroupPropertyEditor.class, distribution = DistributionEnum.DEVELOPER)
    public String horizontalGroup = ""; // "<HorizontalGroup></HorizontalGroup>";

    @NeptusProperty(name = "Vertical Group", description = "You can use the index number or the name as listed.", 
            editorClass = VerticalGroupPropertyEditor.class, distribution = DistributionEnum.DEVELOPER)
    public String verticalGroup = ""; // "<VerticalGroup></VerticalGroup>";

    @NeptusProperty(name = "Link Size Horizontal", description = "You can use the index number or the name as listed.", 
            editorClass = LinkSizeHorizontalPropertyEditor.class, distribution = DistributionEnum.DEVELOPER)
    public String linkSizeHorizontal = ""; // "<LinkSizeHorizontal></LinkSizeHorizontal>";

    @NeptusProperty(name = "Link Size Vertical", description = "You can use the index number or the name as listed.", 
            editorClass = LinkSizeVerticalPropertyEditor.class, distribution = DistributionEnum.DEVELOPER)
    public String linkSizeVertical = ""; // "<LinkSizeVertical></LinkSizeVertical>";

    @NeptusProperty(name = "Profiles", description = "To set groups of visible components. "
            + "You can use the index number or the name as listed.", editorClass = ProfilesPropertyEditor.class,
            distribution = DistributionEnum.DEVELOPER)
    public String profiles = ""; // "<Profiles></Profiles>";

    @NeptusProperty(name = "Show Only Declared Profiles to User", description = "This if true will show only declared"
            + " profiles to users to choose (will hide the \"" + NONE_PROFILE_STRING + "\" option).",
            distribution = DistributionEnum.DEVELOPER)
    public boolean showOnlyProfilesToUser = false;

    private boolean inEditMode = false;
    /**
     * This will protect a re-layout illegal state exception while applying it (repaint called in the middle of the
     * apply
     */
    private boolean relayouting = false;

    private GroupLayout layout = null;

    protected LinkedHashMap<String, String> profilesList = new LinkedHashMap<String, String>();
    protected LinkedHashMap<String, LayoutHolder> profilesLayouts = new LinkedHashMap<String, LayoutHolder>();
    protected String defaultProfile = "";
    protected String activeProfile = "";

    private MouseListener mouseListener = null;

    private JMenu menuForProfileChangeInConsole = null;

    private DocumentBuilderFactory docBuilderFactory;
    public static Schema schema = null;

    {
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setNamespaceAware(false);
    }

    {
        if (schema == null) {
            SchemaFactory sm = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                File sFx = StreamUtil.copyStreamToTempFile(GroupLayoutContainer.class
                        .getResourceAsStream(GROUP_LAYOUT_SCHEMA));
                schema = sm.newSchema(sFx);
            }
            catch (Exception e) {
                NeptusLog.pub().warn(ReflectionUtil.getCallerStamp() + e.getMessage());
            }
        }
    }

    
    public GroupLayoutContainer(ConsoleLayout console) {
        super(console);
        initialize();
    }

    
    private void initialize() {
        this.setLayout(null);
        layout = new GroupLayout(this);
        this.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        mouseListener = getLayoutMouseListener();
        this.addMouseListener(getLayoutMouseListener());

        loadProfiles();
    }

    /**
     * This needs to be here in case the Horizontal and Vertical are not balanced, that is
     * if a component is missing in one of the layouts.
     * 
     * @see java.awt.Container#doLayout()
     */
    @Override
    public void doLayout() {
        try {
            if (!relayouting)
                super.doLayout();
        }
        catch (final IllegalStateException e) {
            new Thread() {
                public void run() {
                    GuiUtils.errorMessage(SwingUtilities.windowForComponent(GroupLayoutContainer.this), e);
                    e.printStackTrace();
                };
            }.start();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.SubPanel#postLoadInit()
     */
    @Override
    public void init() {
        super.init();

        addProfilesJMenuToConsole();
    }

    @Override
    public void clean() {
        super.clean();
        removeProfilesJMenuFromConsole();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        loadProfiles();
        if (subPanelList().length > 0)
            applyLayout();
        // maximizePanel(maximizePanel);
    }

    @Override
    protected void readChildFromXml(org.dom4j.Element el) {
        super.readChildFromXml(el);
        applyLayout();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ContainerSubPanel#addSubPanelExtra(pt.lsts.neptus.console.ConsolePanel)
     */
    @Override
    public boolean addSubPanelExtra(ConsolePanel panel) {
        panel.addMouseListener(getLayoutMouseListener());
        return true;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ContainerSubPanel#removeSubPanelExtra(pt.lsts.neptus.console.ConsolePanel)
     */
    @Override
    public void removeSubPanelExtra(ConsolePanel sp) {
        sp.removeMouseListener(getLayoutMouseListener());
    }

    private void addProfilesJMenuToConsole() {
        if (menuForProfileChangeInConsole != null && this.getParent() instanceof MainPanel
                && profilesList.keySet().size() == 0) {
            removeProfilesJMenuFromConsole();
        }
        else if (menuForProfileChangeInConsole == null && this.getParent() instanceof MainPanel
                && profilesList.keySet().size() > 0) {
            menuForProfileChangeInConsole = getConsole().getOrCreateJMenu(new String[] { I18n.text("Profiles") });
            menuForProfileChangeInConsole.addMenuListener(new MenuListener() {
                public void menuSelected(MenuEvent e) {
                    menuForProfileChangeInConsole.removeAll();
                    for (final String name : profilesList.keySet()) {
                        String defaultStr = "";
                        if (name.equalsIgnoreCase(defaultProfile))
                            defaultStr = " (default)";
                        JCheckBoxMenuItem vMItem = new JCheckBoxMenuItem(new AbstractAction(name + defaultStr) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                GroupLayoutContainer.this.setActiveProfile(name);
                            }
                        });
                        if (name.equalsIgnoreCase(activeProfile))
                            vMItem.setSelected(true);
                        menuForProfileChangeInConsole.add(vMItem);
                    }
                    if (!showOnlyProfilesToUser) {
                        JCheckBoxMenuItem vMNone = new JCheckBoxMenuItem(new AbstractAction(NONE_PROFILE_STRING) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                GroupLayoutContainer.this.setActiveProfile(getName());
                            }
                        });
                        menuForProfileChangeInConsole.add(vMNone);
                        if ("".equalsIgnoreCase(activeProfile))
                            vMNone.setSelected(true);
                    }
                }

                public void menuDeselected(MenuEvent e) {
                }

                public void menuCanceled(MenuEvent e) {
                }
            });
        }
    }

    /**
     * 
     */
    private void removeProfilesJMenuFromConsole() {
        if (menuForProfileChangeInConsole != null) {
            try {
                menuForProfileChangeInConsole.getParent().remove(menuForProfileChangeInConsole);
                menuForProfileChangeInConsole = null;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private String[] validateLayoutXML(InputStream is) {
        final Vector<String> validationMsgs = new Vector<String>();
        Validator validator = schema.newValidator();
        validator.setErrorHandler(new ErrorHandler() {
            @Override
            public void warning(SAXParseException exception) throws SAXException {
                validationMsgs.add("WARNING: " + exception.getMessage());
            }

            @Override
            public void error(SAXParseException exception) throws SAXException {
                validationMsgs.add("ERROR: " + exception.getMessage());
            }

            @Override
            public void fatalError(SAXParseException exception) throws SAXException {
                validationMsgs.add("FATAL: " + exception.getMessage());
            }
        });
        try {
            validator.validate(new StreamSource(is));
        }
        catch (Exception e) {
            validationMsgs.add("SOURCE: " + e.getMessage());
        }
        return validationMsgs.toArray(new String[validationMsgs.size()]);
    }

    /**
     * 
     */
    private void changeDefaultProfileInXml() {
        String profToChange = profiles.trim();
        boolean toChangeOrRemove = true;
        if ("".equalsIgnoreCase(defaultProfile))
            toChangeOrRemove = false;

        if (profToChange.startsWith("<Profile")) {
            if (toChangeOrRemove)
                profiles = "<Default profile=\"" + defaultProfile + "\" />\n" + profToChange;
        }
        else {
            Pattern pat = Pattern.compile("^<Default\\s+profile=\\s*(\"((.|\\s)+?)\"|'((.|\\s)+?)')\\s*/>");
            Matcher m = pat.matcher(profToChange);
            m.find();
            if (m.groupCount() > 0) {
                // NeptusLog.pub().info("<###>Change " + (m.group(2) != null ? m.group(2) : m.group(4))
                // + " with " + defaultProfile);
                String aspas = (m.group(2) != null ? "\"" : "'");
                if (toChangeOrRemove)
                    profiles = m.replaceFirst("<Default profile=" + aspas + defaultProfile + aspas + " />");
                else
                    profiles = m.replaceFirst("");
                // NeptusLog.pub().info("<###> "+profiles);
            }
            else {
                NeptusLog.pub().info("<###>Not able to change default profile from "
                        + (m.group(2) != null ? m.group(2) : m.group(4)) + " with " + defaultProfile);
            }
        }
    }

    /**
	 * 
	 */
    private void loadProfiles() {
        try {
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            ByteArrayInputStream bais = new ByteArrayInputStream(
                    ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><Profiles>" + profiles.trim() + "</Profiles>")
                            .getBytes());
            String[] vmsgs = validateLayoutXML(bais);
            if (vmsgs.length != 0) {
                String strMsg = "Invalid XML!\n";
                for (String str : vmsgs)
                    strMsg += "\n" + str;
                NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + "Profiles: " + strMsg);
                return;
            }
            bais.reset();
            Document docProfiles = builder.parse(bais);
            Element root = docProfiles.getDocumentElement();

            profilesList.clear();
            activeProfile = "";
            defaultProfile = "";

            Node bn = root.getFirstChild();
            while (bn != null) {
                if ("Default".equalsIgnoreCase(bn.getNodeName())) {
                    try {
                        String dName = bn.getAttributes().getNamedItem("profile").getTextContent();
                        if (!"".equals(dName))
                            defaultProfile = dName;
                    }
                    catch (Exception e) {
                        NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
                    }
                }
                else { // Profile
                    String pName = null;
                    try {
                        pName = bn.getAttributes().getNamedItem("name").getTextContent();
                    }
                    catch (Exception e) {
                        NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
                    }
                    if (pName != null) {
                        boolean exclude = false;
                        try {
                            exclude = Boolean.parseBoolean(bn.getAttributes().getNamedItem("exclude").getTextContent());
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        String pStringList = !exclude ? "+" : "-";
                        LayoutHolder layouHolder = null;
                        Node cn = bn.getFirstChild();
                        if (cn != null) {
                            while (cn != null) {
                                if ("Component".equalsIgnoreCase(cn.getNodeName())) {
                                    String cId = null;
                                    try {
                                        cId = cn.getAttributes().getNamedItem("id").getTextContent();
                                    }
                                    catch (Exception e) {
                                        NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
                                    }
                                    if (cId != null) {
                                        pStringList += "," + cId;
                                    }
                                }
                                else if ("Layout".equalsIgnoreCase(cn.getNodeName())) {
                                    layouHolder = new LayoutHolder();
                                    Node ln = cn.getFirstChild();
                                    if (ln != null) {
                                        while (ln != null) {
                                            String xmlStr = XMLUtil.nodeChildsToString(ln);
                                            if ("HorizontalGroup".equalsIgnoreCase(ln.getNodeName()))
                                                layouHolder.horizontalGroup = xmlStr;
                                            else if ("VerticalGroup".equalsIgnoreCase(ln.getNodeName()))
                                                layouHolder.verticalGroup = xmlStr;
                                            else if ("LinkSizeHorizontal".equalsIgnoreCase(ln.getNodeName()))
                                                layouHolder.linkSizeHorizontal = xmlStr;
                                            else if ("LinkSizeVertical".equalsIgnoreCase(ln.getNodeName()))
                                                layouHolder.linkSizeVertical = xmlStr;
                                            ln = ln.getNextSibling();
                                        }
                                    }
                                }
                                cn = cn.getNextSibling();
                            }
                        }

                        /*
                         * Now this is not needed because this profile might be just a relayout if
                         * (!"+".equalsIgnoreCase(pStringList) && !"-".equalsIgnoreCase(pStringList)) {
                         * profilesList.put(pName, pStringList); } Now just added every profile (line bellow)
                         */
                        profilesList.put(pName, pStringList);

                        if (layouHolder != null) {
                            profilesLayouts.put(pName, layouHolder);
                        }
                    }
                }
                bn = bn.getNextSibling();
            }
            if (defaultProfile != null && !"".equalsIgnoreCase(defaultProfile)) {
                if (profilesList.get(defaultProfile) != null)
                    activeProfile = defaultProfile;
                else
                    defaultProfile = "";
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
        }

        addProfilesJMenuToConsole();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.ContainerSubPanel#setEditMode(boolean)
     */
    @Override
    public void setEditMode(boolean edit) {
        inEditMode = edit;
        if (edit) {
            restoreComponentsLayoutVisibility();
            if (!"".equalsIgnoreCase(activeProfile))
                applyLayout(horizontalGroup, verticalGroup, linkSizeHorizontal, linkSizeVertical, false);
        }
        else {
            setActiveProfile(defaultProfile);
        }
        super.setEditMode(edit);
    }

    /**
	 * 
	 */
    private void applyLayout() {
        applyLayout(horizontalGroup, verticalGroup, linkSizeHorizontal, linkSizeVertical, true);
    }

    /**
     * @param horizontalGroupLocal
     * @param verticalGroupLocal
     * @param linkSizeHorizontalLocal
     * @param linkSizeVerticalLocal
     * @param applyProfile
     */
    private void applyLayout(String horizontalGroupLocal, String verticalGroupLocal, String linkSizeHorizontalLocal,
            String linkSizeVerticalLocal, boolean applyProfile) {
        relayouting = true;
        if (layout == null)
            return;
        
        try {
            layout.invalidateLayout(this);
            layout.setAutoCreateGaps(autoCreateGaps);
            layout.setAutoCreateContainerGaps(autoCreateContainerGaps);
            layout.setHonorsVisibility(honorsVisibility);

            try {
                DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
                ByteArrayInputStream bais = new ByteArrayInputStream(
                        ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><HorizontalGroup>" + horizontalGroupLocal.trim() + "</HorizontalGroup>")
                                .getBytes());
                String[] vmsgs = validateLayoutXML(bais);
                if (vmsgs.length != 0) {
                    String strMsg = "Invalid XML!\n";
                    for (String str : vmsgs)
                        strMsg += "\n" + str;
                    NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + "HorizontalGroup: " + strMsg);
                }
                bais.reset();
                Document docHG = builder.parse(bais);
                Element rootHG = docHG.getDocumentElement();

                bais = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><VerticalGroup>"
                        + verticalGroupLocal.trim() + "</VerticalGroup>").getBytes());
                vmsgs = validateLayoutXML(bais);
                if (vmsgs.length != 0) {
                    String strMsg = "Invalid XML!\n";
                    for (String str : vmsgs)
                        strMsg += "\n" + str;
                    NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + "VerticalGroup: " + strMsg);
                }
                bais.reset();
                Document docVG = builder.parse(bais);
                Element rootVG = docVG.getDocumentElement();

                bais = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><LinkSizeHorizontal>"
                        + linkSizeHorizontalLocal.trim() + "</LinkSizeHorizontal>").getBytes());
                vmsgs = validateLayoutXML(bais);
                if (vmsgs.length != 0) {
                    String strMsg = "Invalid XML!\n";
                    for (String str : vmsgs)
                        strMsg += "\n" + str;
                    NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + "LinkSizeHorizontal: " + strMsg);
                }
                bais.reset();
                Document docLH = builder.parse(bais);
                Element rootLH = docLH.getDocumentElement();

                bais = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><LinkSizeVertical>"
                        + linkSizeVerticalLocal.trim() + "</LinkSizeVertical>").getBytes());
                vmsgs = validateLayoutXML(bais);
                if (vmsgs.length != 0) {
                    String strMsg = "Invalid XML!\n";
                    for (String str : vmsgs)
                        strMsg += "\n" + str;
                    NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + "LinkSizeVertical: " + strMsg);
                }
                bais.reset();
                Document docLV = builder.parse(bais);
                Element rootLV = docLV.getDocumentElement();

                // Validate all elements together (later pass it upward)
                bais = new ByteArrayInputStream(("<?xml version=\"1.0\" encoding=\"UTF-8\"?><ConsoleGroupLayout>"
                        + "<HorizontalGroup>" + horizontalGroupLocal.trim() + "</HorizontalGroup>" + "<VerticalGroup>"
                        + verticalGroupLocal.trim() + "</VerticalGroup>" + "<LinkSizeHorizontal>"
                        + linkSizeHorizontalLocal.trim() + "</LinkSizeHorizontal>" + "<LinkSizeVertical>"
                        + linkSizeVerticalLocal.trim() + "</LinkSizeVertical>" +
                // "<Profiles>" + profiles.trim() + "</Profiles>" +
                        "</ConsoleGroupLayout>").getBytes());
                vmsgs = validateLayoutXML(bais);
                if (vmsgs.length != 0) {
                    String strMsg = "Invalid XML!\n";
                    for (String str : vmsgs)
                        strMsg += "\n" + str;
                    NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + "ConsoleGroupLayout: " + strMsg);
                    return; // If not valid exit
                }

                Group hGroup = createLayoutGroup(rootHG);
                Group vGroup = createLayoutGroup(rootVG);
                layout.setHorizontalGroup(hGroup);
                layout.setVerticalGroup(vGroup);

                createLayoutLinkSize(rootLH, true);
                createLayoutLinkSize(rootLV, false);
            }
            catch (Exception e) {
                NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
            }

            // maximizePanel(maximizePanel);
            if (!inEditMode && applyProfile)
                setActiveProfile(activeProfile);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            relayouting = false;
        }
    }

    /**
     * @param root
     * @return
     */
    private Group createLayoutGroup(Element root) {
        Group group = null;

        Node bn = root.getFirstChild();
        if (bn != null) {
            if (bn.getNodeName().equalsIgnoreCase("Sequence")) {
                group = layout.createSequentialGroup();
                createLayoutGroupRecursive(bn, group);
            }
            else if (bn.getNodeName().equalsIgnoreCase("Parallel")) {
                group = createLayoutParallelGroup(bn);
                createLayoutGroupRecursive(bn, group);
            }
            else
                return layout.createSequentialGroup();
        }
        return group;
    }

    /**
     * Internal use.
     * 
     * @param root
     * @param groupBase
     * @return
     */
    private Group createLayoutGroupRecursive(Node root, Group groupBase) {
        Node bn = root.getFirstChild();
        while (bn != null) {
            if (bn.getNodeName().equalsIgnoreCase("Sequence")) {
                SequentialGroup groupS = layout.createSequentialGroup();
                groupBase.addGroup(createLayoutGroupRecursive(bn, groupS));
            }
            else if (bn.getNodeName().equalsIgnoreCase("Parallel")) {
                ParallelGroup groupP = createLayoutParallelGroup(bn);
                groupBase.addGroup(createLayoutGroupRecursive(bn, groupP));
            }
            else if (bn.getNodeName().equalsIgnoreCase("Component")) {
                addLayoutComponent(bn, groupBase);
            }
            else if (bn.getNodeName().equalsIgnoreCase("Gap")) {
                addLayoutGap(bn, groupBase);
            }
            else if (bn.getNodeName().equalsIgnoreCase("GapComponents")
                    || bn.getNodeName().equalsIgnoreCase("PreferredGap")) {
                addLayoutSequentialGroupGapComponentOrPreferred(bn, groupBase);
            }
            bn = bn.getNextSibling();
        }
        return groupBase;
    }

    /**
     * @param bn
     * @return
     */
    private ParallelGroup createLayoutParallelGroup(Node bn) {
        boolean resizable = true;
        Alignment alignment = Alignment.LEADING;
        try {
            String al = bn.getAttributes().getNamedItem("alignment").getTextContent();
            alignment = Alignment.valueOf(al);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
        try {
            String rs = bn.getAttributes().getNamedItem("resizable").getTextContent();
            resizable = Boolean.getBoolean(rs);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
        return layout.createParallelGroup(alignment, resizable);
    }

    /**
     * @param bn
     * @param groupBase
     */
    private void addLayoutSequentialGroupGapComponentOrPreferred(Node bn, Group groupBase) {
        boolean isGapComponents = true;
        if (bn.getNodeName().equalsIgnoreCase("PreferredGap"))
            isGapComponents = false;
        ComponentPlacement type = LayoutStyle.ComponentPlacement.RELATED;
        int pref = GroupLayout.DEFAULT_SIZE, max = GroupLayout.PREFERRED_SIZE;
        try {
            String pl = bn.getAttributes().getNamedItem("type").getTextContent();
            type = ComponentPlacement.valueOf(pl);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
        try {
            String il = bn.getAttributes().getNamedItem("pref").getTextContent();
            pref = Integer.parseInt(il);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
            return;
        }
        try {
            String il = bn.getAttributes().getNamedItem("max").getTextContent();
            max = Integer.parseInt(il);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
        int firstComponent = -1, secondComponent = -1;
        String fcl1 = null, fcl2 = null;
        if (isGapComponents) {
            try {
                fcl1 = bn.getAttributes().getNamedItem("firstComponent").getTextContent();
                fcl2 = bn.getAttributes().getNamedItem("secondComponent").getTextContent();
            }
            catch (Exception e) {
                NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
                return;
            }
            try {
                firstComponent = Integer.parseInt(fcl1);
            }
            catch (Exception e) {
                 NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
            }
            try {
                secondComponent = Integer.parseInt(fcl2);
            }
            catch (Exception e) {
                 NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
            }
        }
        try {
            if (isGapComponents) {
                ConsolePanel comp1 = (firstComponent >= 0) ? panels.get(firstComponent) : getSubPanelByName(fcl1);
                ConsolePanel comp2 = (secondComponent >= 0) ? panels.get(secondComponent) : getSubPanelByName(fcl2);
                ((SequentialGroup) groupBase).addPreferredGap(comp1, comp2, type, pref, max);
            }
            else {
                ((SequentialGroup) groupBase).addPreferredGap(type, pref, max);
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
    }

    /**
     * @param bn
     * @param groupBase
     */
    private void addLayoutGap(Node bn, Group groupBase) {
        int min = Integer.MIN_VALUE, pref = -1, max = Integer.MIN_VALUE;
        try {
            String il = bn.getAttributes().getNamedItem("min").getTextContent();
            min = Integer.parseInt(il);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
        try {
            String il = bn.getAttributes().getNamedItem("pref").getTextContent();
            pref = Integer.parseInt(il);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
            return;
        }
        try {
            String il = bn.getAttributes().getNamedItem("max").getTextContent();
            max = Integer.parseInt(il);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
        try {
            if (min == Integer.MIN_VALUE || max == Integer.MIN_VALUE)
                groupBase.addGap(pref);
            else
                groupBase.addGap(min, pref, max);
        }
        catch (Exception e) {
            NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
    }

    /**
     * @param bn
     * @param groupBase
     */
    private void addLayoutComponent(Node bn, Group groupBase) {
        Alignment alignment = Alignment.LEADING;
        try {
            String al = bn.getAttributes().getNamedItem("alignment").getTextContent();
            alignment = Alignment.valueOf(al);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }

        int min = -1, pref = -1, max = -1;
        try {
            String il = bn.getAttributes().getNamedItem("min").getTextContent();
            min = Integer.parseInt(il);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
        try {
            String il = bn.getAttributes().getNamedItem("pref").getTextContent();
            pref = Integer.parseInt(il);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
        try {
            String il = bn.getAttributes().getNamedItem("max").getTextContent();
            max = Integer.parseInt(il);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
        }

        ConsolePanel comp = null;
        String il = null;
        int id = -1;
        try {
            il = bn.getAttributes().getNamedItem("id").getTextContent();
        }
        catch (Exception e) {
            NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
            return;
        }
        try {
            id = Integer.parseInt(il);
        }
        catch (Exception e) {
             NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
            // return;
        }

        try {
            comp = (id >= 0) ? panels.get(id) : getSubPanelByName(il);
            if (comp == null) {
                System.out.print("------------------ " + id + "  ----- " + il + " --- " + subPanelList().length);
                GuiUtils.printArray(subPanelList());
                System.out.flush();
            }
            try {
                if (groupBase.getClass().isAssignableFrom(ParallelGroup.class))
                    ((ParallelGroup) groupBase).addComponent(comp, alignment, min, pref, max);
                else
                    groupBase.addComponent(comp, min, pref, max);
            }
            catch (Exception e) {
                e.printStackTrace();
                groupBase.addComponent(comp, min, pref, max);
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(ReflectionUtil.getCallerStamp() + e.getMessage());
        }
    }

    /**
     * @param root
     * @param isForHorizontal
     */
    private void createLayoutLinkSize(Element root, boolean isForHorizontal) {
        Node bn = root.getFirstChild();
        while (bn != null) {
            if (bn.getNodeName().equalsIgnoreCase("LinkSizeGroup")) {
                Vector<Component> compVec = new Vector<Component>();
                Node cn = bn.getFirstChild();
                while (cn != null) {
                    if ("Component".equalsIgnoreCase(cn.getNodeName())) {
                        String cil = cn.getAttributes().getNamedItem("id").getTextContent();
                        int idx = -1;
                        try {
                            idx = Integer.parseInt(cil);
                        }
                        catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        ConsolePanel comp = (idx >= 0) ? panels.get(idx) : getSubPanelByName(cil);
                        if (comp != null)
                            compVec.add(comp);
                    }
                    cn = cn.getNextSibling();
                }
                if (compVec.size() >= 2) {
                    Component[] components = compVec.toArray(new Component[compVec.size()]);
                    layout.linkSize(isForHorizontal ? SwingConstants.HORIZONTAL : SwingConstants.VERTICAL, components);
                }
                bn = bn.getNextSibling();
            }
        }
    }

    /**
     * @return
     */
    private MouseListener getLayoutMouseListener() {
        if (mouseListener == null) {
            mouseListener = new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON3) {

                        Rectangle screenB = GuiUtils.getScreenBounds(e.getComponent().getX(), e.getComponent().getY());
                        int s = (int) (screenB.getHeight() / 22);

                        JPopupMenu popup = new JPopupMenu();
                        if (honorsVisibility) {
                            final Component comp = e.getComponent();
                            if (comp != GroupLayoutContainer.this) {
                                JMenuItem mtjmi = new JMenuItem(new AbstractAction("Maximize this "
                                        + PluginUtils.getPluginName(comp.getClass())) {
                                    @Override
                                    public void actionPerformed(ActionEvent ae) {
                                        // Component comp = e.getComponent();
                                        if (comp != null) {
                                            setComponentLayoutVisibility(false);
                                            setComponentLayoutVisibility(true, comp);
                                        }
                                    }
                                });
                                mtjmi.setIcon(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(comp.getClass()), 16,
                                        16));
                                popup.add(mtjmi);

                                JMenuItem mtjmi2 = new JMenuItem(new AbstractAction("Hide this "
                                        + PluginUtils.getPluginName(comp.getClass())) {
                                    @Override
                                    public void actionPerformed(ActionEvent ae) {
                                        // Component comp = e.getComponent();
                                        if (comp != null) {
                                            setComponentLayoutVisibility(false, comp);
                                        }
                                    }
                                });
                                mtjmi2.setIcon(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(comp.getClass()), 16,
                                        16));
                                popup.add(mtjmi2);

                                // popup.add(new JMenuItem(new AbstractAction("Restore") {
                                // @Override
                                // public void actionPerformed(ActionEvent ae) {
                                // //Component comp = e.getComponent();
                                // if (comp != null) {
                                // for (Component cp : mySubPanels) {
                                // cp.setVisible(true);
                                // }
                                // GroupLayoutContainer.this.repaint();
                                // }
                                // }
                                // }));
                                popup.addSeparator();
                            }
                        }
                        boolean isContainerSelected = false;
                        if (popup.getComponentCount() == 0)
                            isContainerSelected = true;
                        isContainerSelected = true;
                        if (isContainerSelected) {
                            JMenu addp = new JMenu("Maximize component");
                            int i = 0;
                            for (String spName : subPanelList()) {
                                final ConsolePanel comp = getSubPanelByName(spName);
                                boolean hidden = false;
                                try {
                                    ConsolePanel ssp = (ConsolePanel) comp;
                                    hidden = ssp.getVisibility() ? false : true;
                                }
                                catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                                JMenuItem cjmi = new JMenuItem(new AbstractAction("(" + (i++) + ") " + spName /*
                                                                                                               * PluginUtils
                                                                                                               * .
                                                                                                               * getPluginName
                                                                                                               * (comp.
                                                                                                               * getClass
                                                                                                               * ())
                                                                                                               */
                                        + (hidden ? " (hidden)" : "")) {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        setComponentLayoutVisibility(false);
                                        setComponentLayoutVisibility(true, comp);
                                    }
                                });
                                if (hidden)
                                    cjmi.setEnabled(false);
                                cjmi.setIcon(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(comp.getClass()), 16,
                                        16));
                                addp.add(cjmi);
                            }
                            MenuScroller.setScrollerFor(addp, s, 150, 0, 0);
                            popup.add(addp);
                            JMenu addp2 = new JMenu("Hide component");
                            i = 0;
                            for (String spName : subPanelList()) {
                                final ConsolePanel comp = getSubPanelByName(spName);
                                boolean hidden = false;
                                try {
                                    ConsolePanel ssp = (ConsolePanel) comp;
                                    hidden = ssp.getVisibility() ? false : true;
                                }
                                catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                                JMenuItem cjmi = new JMenuItem(new AbstractAction("(" + (i++) + ") " + spName /*
                                                                                                               * PluginUtils
                                                                                                               * .
                                                                                                               * getPluginName
                                                                                                               * (comp.
                                                                                                               * getClass
                                                                                                               * ())
                                                                                                               */
                                        + (hidden ? " (hidden)" : "")) {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        setComponentLayoutVisibility(false, comp);
                                    }
                                });
                                if (hidden)
                                    cjmi.setEnabled(false);
                                cjmi.setIcon(ImageUtils.getScaledIcon(PluginUtils.getPluginIcon(comp.getClass()), 16,
                                        16));
                                addp2.add(cjmi);
                            }
                            MenuScroller.setScrollerFor(addp2, s, 150, 0, 0);
                            popup.add(addp2);
                            popup.add(new JMenuItem(new AbstractAction("Restore") {
                                @Override
                                public void actionPerformed(ActionEvent ae) {
                                    if ("".equalsIgnoreCase(defaultProfile))
                                        restoreComponentsLayoutVisibility();
                                    else
                                        setActiveProfile(defaultProfile);
                                }
                            }));
                        }

                        if (popup.getComponentCount() != 0 && profilesList.size() > 0) {
                            popup.addSeparator();
                            JMenu addProfile = new JMenu("Activate Profile");
                            if (!showOnlyProfilesToUser) {
                                JCheckBoxMenuItem defProf = new JCheckBoxMenuItem(new AbstractAction(
                                        NONE_PROFILE_STRING) {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        setActiveProfile("");
                                    }
                                });
                                if ("".equalsIgnoreCase(activeProfile))
                                    defProf.setSelected(true);
                                addProfile.add(defProf);
                            }
                            for (String pName : profilesList.keySet()) {
                                final String name = pName;
                                JCheckBoxMenuItem cjmi = new JCheckBoxMenuItem(new AbstractAction(name
                                /* + (defaultProfile.equals(name) ? " *" : "") */) {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        setActiveProfile(name);
                                    }
                                });
                                if (name.equalsIgnoreCase(activeProfile))
                                    cjmi.setSelected(true);
                                addProfile.add(cjmi);
                            }
                            popup.add(addProfile);
                            JMenu setDefaultProfile = new JMenu("Set Default Profile");
                            if (!showOnlyProfilesToUser) {
                                JCheckBoxMenuItem defProf = new JCheckBoxMenuItem(new AbstractAction(
                                        NONE_PROFILE_STRING) {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        defaultProfile = "";
                                        changeDefaultProfileInXml();
                                    }
                                });
                                if ("".equalsIgnoreCase(defaultProfile))
                                    defProf.setSelected(true);
                                setDefaultProfile.add(defProf);
                            }
                            for (String pName : profilesList.keySet()) {
                                final String name = pName;
                                JCheckBoxMenuItem cjmi = new JCheckBoxMenuItem(new AbstractAction(name
                                /* + (defaultProfile.equals(name) ? " *" : "") */) {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        defaultProfile = name;
                                        changeDefaultProfileInXml();
                                    }
                                });
                                if (name.equalsIgnoreCase(defaultProfile))
                                    cjmi.setSelected(true);
                                setDefaultProfile.add(cjmi);
                            }
                            popup.add(setDefaultProfile);
                        }

                        if (popup.getComponentCount() != 0)
                            popup.addSeparator();

                        popup.add(new JMenuItem(new AbstractAction("Maximize container") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                setMaximizePanel(true);
                            }
                        }));
                        popup.add(new JMenuItem(new AbstractAction("Restore container") {
                            @Override
                            public void actionPerformed(ActionEvent ae) {
                                setMaximizePanel(false);
                            }
                        }));

                        popup.show(e.getComponent(), e.getX(), e.getY());
                        requestFocusInWindow();
                    }

                    if (GroupLayoutContainer.this.getParent() != null) {
                        for (MouseListener ml : GroupLayoutContainer.this.getParent().getMouseListeners()) {
                            ml.mouseClicked(e);
                        }
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (GroupLayoutContainer.this.getParent() != null) {
                        for (MouseListener ml : GroupLayoutContainer.this.getParent().getMouseListeners()) {
                            ml.mouseReleased(e);
                        }
                    }
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (GroupLayoutContainer.this.getParent() != null) {
                        for (MouseListener ml : GroupLayoutContainer.this.getParent().getMouseListeners()) {
                            ml.mousePressed(e);
                        }
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (GroupLayoutContainer.this.getParent() != null) {
                        for (MouseListener ml : GroupLayoutContainer.this.getParent().getMouseListeners()) {
                            ml.mouseExited(e);
                        }
                    }
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (GroupLayoutContainer.this.getParent() != null) {
                        for (MouseListener ml : GroupLayoutContainer.this.getParent().getMouseListeners()) {
                            ml.mouseEntered(e);
                        }
                    }
                }
            };
        }
        return mouseListener;
    }

    /**
     * @return
     *
     * @see pt.lsts.neptus.console.plugins.containers.LayoutProfileProvider#getActiveProfile()
     */
    @Override
    public String getActiveProfile() {
        return activeProfile;
    }

    /**
     * To activate a profile. Empty restores the full view.
     * 
     * @param name
     * @return
     *
     * @see pt.lsts.neptus.console.plugins.containers.LayoutProfileProvider#setActiveProfile(java.lang.String)
     */
    @Override
    public boolean setActiveProfile(String name) {
        if (name == null || "".equalsIgnoreCase(name)) {
            restoreComponentsLayoutVisibility();
            boolean resetLayout = false;
            if (!"".equalsIgnoreCase(activeProfile))
                resetLayout = true;
            activeProfile = "";
            propagateActiveProfileChange(name);
            if (resetLayout) {
                try {
                    applyLayout(horizontalGroup, verticalGroup, linkSizeHorizontal, linkSizeVertical, false);
                }
                catch (NullPointerException e) {
                    // This happens on the first call from the constructor
                    e.printStackTrace();
                }
            }

            this.invalidate();
            this.repaint();

            return true;
        }
        try {
            String profile = profilesList.get(name);
            if (profile == null)
                return false;
            boolean resetLayout = true;
            // if (!activeProfile.equalsIgnoreCase(name)) //It's not working properly for now
            // resetLayout = true;
            String[] plst = profile.split("[,]+");
            boolean exclude = false;
            if (plst.length <= 1 || "-".equalsIgnoreCase(plst[0].trim()))
                exclude = true; // For the case of empty # of Components the exclude here will make all visible (as
                                // wanted)
            Vector<ConsolePanel> components = new Vector<ConsolePanel>();
            for (int i = 1; i < plst.length; i++) {
                int id = -1;
                try {
                    id = Integer.parseInt(plst[i].trim());
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                ConsolePanel comp = (id >= 0) ? panels.get(id) : getSubPanelByName(plst[i].trim());
                components.add(comp);
            }
            boolean aVi;
            if (exclude)
                aVi = true;
            else
                aVi = false;
            setComponentLayoutVisibility(aVi);
            setComponentLayoutVisibility(!aVi, components.toArray(new Component[components.size()]));
            activeProfile = name;
            propagateActiveProfileChange(name);

            if (resetLayout) {
                LayoutHolder profileLayout = profilesLayouts.get(name);
                if (profileLayout == null)
                    applyLayout(horizontalGroup, verticalGroup, linkSizeHorizontal, linkSizeVertical, false);
                else
                    applyLayout(profileLayout.horizontalGroup, profileLayout.verticalGroup,
                            profileLayout.linkSizeHorizontal, profileLayout.linkSizeVertical, false);
            }

            this.invalidate();
            this.repaint();

            return true;
        }
        catch (Exception e) {
            this.invalidate();
            this.repaint();
            e.printStackTrace();
            
            return false;
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.containers.LayoutProfileProvider#listProfileNames()
     */
    @Override
    public String[] listProfileNames() {
        return profilesList.keySet().toArray(new String[0]);
    }
    
    /**
     * @param profileName
     */
    private void propagateActiveProfileChange(String profileName) {
        for (Component cp : panels) {
//            try {
//                GroupLayoutContainer glcc = (GroupLayoutContainer) cp;
//                glcc.setActiveProfile(profileName);
//            }
//            catch (ClassCastException e) {
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
            if (cp instanceof LayoutProfileProvider)
                ((LayoutProfileProvider) cp).setActiveProfile(profileName);
        }
    }

    /**
     * @return
     */
    public boolean resetActiveProfile() {
        return setActiveProfile("");
    }

    /**
	 * 
	 */
    private void restoreComponentsLayoutVisibility() {
        setComponentLayoutVisibility(true);
    }

    /**
     * @param visible
     */
    private void setComponentLayoutVisibility(boolean visible) {
        setComponentLayoutVisibility(visible, panels.toArray(new Component[panels.size()]));
    }

    /**
     * @param visible
     * @param components
     */
    private void setComponentLayoutVisibility(boolean visible, Component... components) {
        for (Component comp : components) {
            for (Component cp : panels) {
                if (cp.equals(comp)) {
                    try {
                        ConsolePanel ssp = (ConsolePanel) cp;
                        if (visible && ssp.getVisibility())
                            cp.setVisible(visible);
                        else
                            cp.setVisible(false);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        cp.setVisible(visible);
                    }
                    break;
                }
            }
        }
        GroupLayoutContainer.this.repaint();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.containers.LayoutProfileProvider#supportsMaximizePanelOnContainer()
     */
    @Override
    public boolean supportsMaximizePanelOnContainer() {
        return true;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.containers.LayoutProfileProvider#maximizePanelOnContainer(java.awt.Component)
     */
    @Override
    public boolean maximizePanelOnContainer(Component comp) {
        boolean ret = true;
        if (comp != null && panels.contains(comp)) {
            setComponentLayoutVisibility(false);
            setComponentLayoutVisibility(true, comp);
        }
        else {
            if (comp != null)
                ret = false;
            if ("".equalsIgnoreCase(defaultProfile))
                restoreComponentsLayoutVisibility();
            else
                ret = ret && setActiveProfile(defaultProfile);
        }
        return ret;
    }

    /**
     * To use for the Profile Layout.
     * 
     * @author pdias
     * 
     */
    private class LayoutHolder {
        String horizontalGroup = ""; // "<HorizontalGroup></HorizontalGroup>";
        String verticalGroup = ""; // "<VerticalGroup></VerticalGroup>";
        String linkSizeHorizontal = ""; // "<LinkSizeHorizontal></LinkSizeHorizontal>";
        String linkSizeVertical = ""; // "<LinkSizeVertical></LinkSizeVertical>";
    }

    /**
     * @param args
     */
    @SuppressWarnings("unused")
    public static void main(String[] args) {
        String profToChange = "<Default  profile= '\"teste\ndd' />\n<Profile />";

        profToChange = "<Default  profile=\"New Plan 'Control\" /><Profile name=\"New Plan Control\" exclude=\"true\"><Component id=\"Mission Elements\"/><Component id=\"ImcMissionStatePanel\"/></Profile><Profile name=\"Old Plan Control\" exclude=\"true\"><Component id=\"Plan Control\"/><Component id=\"Plan Control State\"/></Profile><Profile name=\"No GPS\" exclude=\"true\"><Component id=\"GPS Device Panel\"/><Component id=\"FindVehicle\"/></Profile>";

        Pattern pat = Pattern.compile("^<Default\\s+profile=\\s*(\"((.|\\s)+?)\"|'((.|\\s)+?)')\\s*/>");
        Matcher m = pat.matcher(profToChange);
        m.find();
        NeptusLog.pub().info("<###> "+m.groupCount());
        NeptusLog.pub().info("<###> "+m.group(2) + "    " + m.group(4));
        NeptusLog.pub().info("<###> "+m.replaceFirst("<Default profile=\"def\" />"));

        // if (true)
        // return;

        try {
            // ConfigFetch.initialize();

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            // dbf.setNamespaceAware(true);
            SchemaFactory sm = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            File sFx = StreamUtil.copyStreamToTempFile(GroupLayoutContainer.class
                    .getResourceAsStream(GROUP_LAYOUT_SCHEMA));
            Schema schema = sm.newSchema(sFx);
            dbf.setSchema(schema);
            dbf.setValidating(false);

            DocumentBuilder builder = dbf.newDocumentBuilder();
            ByteArrayInputStream bais = new ByteArrayInputStream(
                    ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<HorizontalGroup>" + "" + "</HorizontalGroup>")
                            .getBytes());
            Validator validator = schema.newValidator();
            // validator.setErrorHandler(errorHandler)
            validator.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException exception) throws SAXException {
                    System.err.println("WARNING: " + exception.getMessage());
                }

                @Override
                public void error(SAXParseException exception) throws SAXException {
                    System.err.println("ERROR: " + exception.getMessage());
                }

                @Override
                public void fatalError(SAXParseException exception) throws SAXException {
                    System.err.println("FATAL: " + exception.getMessage());
                }
            });

            NeptusLog.pub().info("<###> "+ReflectionUtil.class.getPackage().getName());
            validator.validate(new StreamSource(bais));

            bais.reset();
            Document docHG = builder.parse(bais);

            FileInputStream fis = new FileInputStream("./ConsoleGroupLayout-test.xml");
            validator.validate(new StreamSource(fis));

            FileInputStream fis1 = new FileInputStream("./ConsoleGroupLayout-test2.xml");
            validator.validate(new StreamSource(fis1));
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        if (true)
            return;

        ConfigFetch.initialize();
        PluginsLoader.load();
        ConsoleLayout cl = ConsoleLayout.forge(); // See better because it has a MiGLayout panel
        // ConsoleParse parse=new ConsoleParse(cl);
        // try
        // {
        // parse.parseFile(file.getAbsolutePath().toString());
        // }
        // catch (DocumentException e)
        // {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        //
        //
        // cl.setModeEdit(false);
        // cl.setModeEdit(true);
        cl.setVisible(true);

        // GuiUtils.testFrame(new GroupLayoutContainer());

        JPanel jp = new JPanel();
        GroupLayout layout = new GroupLayout(jp);
        jp.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        // JLabel label = new JLabel("Find what:");
        // JTextField textField = new JTextField("");
        // JCheckBox caseCheckBox = new JCheckBox("MatchCase");
        // JCheckBox wholeCheckBox = new JCheckBox("Whole Word");
        // JCheckBox wrapCheckBox = new JCheckBox("wrapCheckBox");
        // JCheckBox backCheckBox = new JCheckBox("backCheckBox");
        // JButton findButton = new JButton("Find");
        // JButton cancelButton = new JButton("Cancel");
        // layout.setHorizontalGroup(layout.createSequentialGroup()
        // .addComponent(label)
        // .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        // .addComponent(textField)
        // .addGroup(layout.createSequentialGroup()
        // .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        // .addComponent(caseCheckBox)
        // .addComponent(wholeCheckBox))
        // .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        // .addComponent(wrapCheckBox)
        // .addComponent(backCheckBox))))
        // .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        // .addComponent(findButton)
        // .addComponent(cancelButton))
        // );
        // layout.linkSize(SwingConstants.HORIZONTAL, findButton, cancelButton);
        //
        // layout.setVerticalGroup(layout.createSequentialGroup()
        // .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        // .addComponent(label)
        // .addComponent(textField)
        // .addComponent(findButton))
        // .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
        // .addGroup(layout.createSequentialGroup()
        // .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        // .addComponent(caseCheckBox)
        // .addComponent(wrapCheckBox))
        // .addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
        // .addComponent(wholeCheckBox)
        // .addComponent(backCheckBox)))
        // .addComponent(cancelButton))
        // );
        // GuiUtils.testFrame(jp);

    }
}
