/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Margarida Faria
 * Jan 9, 2013
 */
package pt.lsts.neptus.console.plugins;

import java.awt.Component;
import java.beans.PropertyEditor;
import java.lang.reflect.Field;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.gui.editor.BitmaskPropertyEditor;
import pt.lsts.neptus.gui.editor.ColorMapPropertyEditor;
import pt.lsts.neptus.gui.editor.EnumEditor;
import pt.lsts.neptus.gui.editor.EnumeratedPropertyEditor;
import pt.lsts.neptus.gui.editor.ImcId16Editor;
import pt.lsts.neptus.gui.editor.LocationTypePropertyEditor;
import pt.lsts.neptus.gui.editor.NeptusDoubleEditor;
import pt.lsts.neptus.gui.editor.PlanActionsEditor;
import pt.lsts.neptus.gui.editor.RenderSelectionEditor;
import pt.lsts.neptus.gui.editor.RenderType;
import pt.lsts.neptus.gui.editor.Script;
import pt.lsts.neptus.gui.editor.ScriptSelectionEditor;
import pt.lsts.neptus.gui.editor.VehicleSelectionEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.Bitmask;
import pt.lsts.neptus.messages.Enumerated;
import pt.lsts.neptus.mp.actions.PlanActions;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PropertyType;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.conf.GeneralPreferences;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertyRendererRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.swing.renderer.DefaultCellRenderer;

/**
 * Manages all the settings for the plugins in use and the general preferences.<br>
 * <br>
 * Here a functionality is either a class with the PluginDescription annotation or the class containing the general
 * preferences. A property is an attribute of the class with the NeptusProperty annotation.<br>
 * <br>
 * A property to be shown must match the current permission level (REGULAR/ADVANCED) and console type
 * (CLIENT/DEVELOPER).
 * 
 * @author Margarida Faria
 * 
 */
public class FunctionalitiesSettings extends JPanel {
    private static final long serialVersionUID = 6448256757823976181L;
    private PropertyEditorRegistry pEditorRegistry;
    private PropertyRendererRegistry pRenderRegistry;

    private final boolean clientConsole;
    private LEVEL permissionLvl;

    private final HashMap<LEVEL, DefaultTreeModel> levelTrees;
    private JSplitPane splitPane;
    private JTree tree;

    /**
     * Initializes the components for the functionalities settings:<br>
     * - find all the available functionalities and builds a tree model for each permission level<br>
     * - creates a tree and puts it in the scroll pane to the left, the holder for the properties panel on the right
     * 
     * @param clientConsole true if this is a console for a client, false if it's for a developer
     * @param subPanels
     */
    public FunctionalitiesSettings(boolean clientConsole, Vector<PropertiesProvider> subPanels) {
        super(new MigLayout());
        this.clientConsole = clientConsole;
        levelTrees = new HashMap<LEVEL, DefaultTreeModel>();
        setupFrame();
        setupTreeModels(subPanels);
    }

    /**
     * Sets up one tree model for each level of permissions and stores it for later use.<br>
     * 
     * The places searched for settings are all the objects in subPanels and the GeneralPreferences.
     * 
     * @param subPanels
     */
    private void setupTreeModels(Vector<PropertiesProvider> subPanels) {
        permissionLvl = LEVEL.REGULAR;
        levelTrees.put(LEVEL.REGULAR, createTreeModel(subPanels));
        permissionLvl = LEVEL.ADVANCED;
        levelTrees.put(LEVEL.ADVANCED, createTreeModel(subPanels));
    }

    /**
     * Create all the components of the panel: JSplitPane with a scroll pane with the tree on the left side and a holder
     * JPanel for properties on the right.
     * 
     */
    private void setupFrame() {
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0);
        // Setup right panel holder
        JPanel holderPropertiesPanel = new JPanel(new MigLayout("insets 0"));
        splitPane.setRightComponent(holderPropertiesPanel);
        // Setup left panel with tree and it's selection behavior
        tree = buildAndSetupTree();
        JScrollPane treePane = new JScrollPane(tree);
        splitPane.setLeftComponent(treePane);
        splitPane.setDividerLocation(200);
        add(splitPane, "w 100%, h 100%");
        // revalidate();
    }

    /**
     * Searches in the subPanels and the GeneralPreferences for settings and builds a tree with a node for each plugin
     * with settings for current permission level and type of console.
     * 
     * @return a JTree with one node for each functionality with properties.
     */
    private JTree buildAndSetupTree() {
        final JTree tree = new JTree();
        // Setup tree options
        tree.setCellRenderer(new IconRenderer());
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        tree.expandRow(0);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        // setup interaction with right pane
        final JPanel holderPropertiesPanel = (JPanel) splitPane.getRightComponent();
        if (tree.getModel().getChildCount(tree.getModel().getRoot()) == 0) {
            holderPropertiesPanel.add(new JLabel(I18n.text("There are no settings to display")));
        }
        else {
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent e) {
                    TreePath path = e.getNewLeadSelectionPath();
                    holderPropertiesPanel.removeAll();
                    if (path != null) {
                        PropertySheetPanel propertiesPanel;
                        // called due to selecting a different node
                        DefaultMutableTreeNode defaultMutableTreeNode = (DefaultMutableTreeNode) path
                                .getLastPathComponent();
                        ClassPropertiesInfo userObject = (ClassPropertiesInfo) defaultMutableTreeNode.getUserObject();
                        propertiesPanel = userObject.getPropertiesPanel();
                        // if there is no built panel fetch values
                        if (propertiesPanel == null) {
                            propertiesPanel = createPropertiesPanelForClass(userObject.getClassInstance());
                            userObject.setPropertiesPanel(propertiesPanel);
                        }
                        holderPropertiesPanel.add(propertiesPanel, "w 100%, h 100%");
                    }
                    revalidate();
                }
            });
        }
        return tree;
    }

    /**
     * Sets properties panels to null. Values might have changed in the mean time.<br>
     * The starting permission level is always REGULAR so the tree for REGULAR permissions is set to the scroll pane of
     * the left panel.<br>
     */
    public void reset() {
        // reset properties panels
        resetPropertiesPanels(LEVEL.REGULAR);
        resetPropertiesPanels(LEVEL.ADVANCED);
        // set default permissions
        permissionLvl = LEVEL.REGULAR;
        // set the tree for REGULAR permissions to the scroll pane of the right panel
        DefaultTreeModel model = levelTrees.get(permissionLvl);
        tree.setModel(model);
        // remove property panels from left
        JPanel holderPanel = (JPanel) splitPane.getRightComponent();
        holderPanel.removeAll();
        repaint();
    }

    public void saveChanges() {
        TreeModel model = tree.getModel();
        Object root = model.getRoot();
        ClassPropertiesInfo classInfo;
        int classCount = model.getChildCount(root);
        for (int c = 0; c < classCount; c++) {
            classInfo = (ClassPropertiesInfo) ((DefaultMutableTreeNode) model.getChild(root, c)).getUserObject();
            PropertySheetPanel propertiesPanel = classInfo.getPropertiesPanel();
            if (propertiesPanel != null) {
                // to exit edit mode and retrieve the value
                propertiesPanel.getTable().commitEditing();
                setProperties(classInfo);
            }
        }
        GeneralPreferences.saveProperties();
    }

    private void setProperties(ClassPropertiesInfo classInfo) {
        DefaultProperty[] beforeProps;
        PropertiesProvider provider;
        String[] errors;
        beforeProps = convertPropertyType(classInfo.getPropertiesPanel().getProperties());
        provider = classInfo.getClassInstance();
        errors = provider.getPropertiesErrors(beforeProps);
        if (errors != null && errors.length > 0) {
            printErrors(errors);
        }
        else {
            provider.setProperties(beforeProps);
        }
    }

    private void printErrors(String[] errors) {
        String errorsString = "<html>" + I18n.text("The following errors were found") + ":<br>";
        int i = 1;
        for (String error : errors) {
            errorsString = errorsString + "<br> &nbsp;" + (i++) + ") " + error;
        }
        errorsString = errorsString + "</html>";
        GuiUtils.errorMessage(new JFrame(I18n.text("Error")), I18n.text("Invalid properties"), errorsString);
    }

    private DefaultProperty[] convertPropertyType(Property[] properties) {
        DefaultProperty defaultProperties[] = new DefaultProperty[properties.length];
        DefaultProperty newProp;
        for (int i = 0; i < properties.length; i++) {
            Property property = properties[i];
            newProp = new DefaultProperty();
            newProp.setName(property.getName());
            newProp.setDisplayName(property.getName());
            newProp.setCategory(property.getCategory());
            newProp.setShortDescription(property.getShortDescription());
            newProp.setEditable(property.isEditable());
            newProp.setType(property.getType());
            newProp.setValue(property.getValue());
            defaultProperties[i] = newProp;
        }
        return defaultProperties;
    }

    /**
     * Save properties entered.<br>
     * Change permission. <br>
     * Switch tree model.<br>
     * Create properties panel for every properties panel existing in the old model.<br>
     * Update new properties panel.
     */
    public void updateForNewPermission() {

        // save opened window
        TreePath selPath = ((DefaultTreeSelectionModel) tree.getSelectionModel()).getSelectionPath();
        // Save properties entered
        HashMap<String, Property[]> oldProps = extractProperties(tree.getModel());

        switchPermissionLevel();

        restoreInsertedProperties(oldProps, selPath);

    }

    /**
     * Restore opened settings node if still there.
     * 
     * @param oldProps properties inserted in the old model
     * @param selPath path to the node selected in the old model
     */
    private void restoreInsertedProperties(HashMap<String, Property[]> oldProps, TreePath selPath) {
        DefaultTreeModel newModel = levelTrees.get(permissionLvl);
        // Create properties panel for every properties panel existing in the old model
        updateUsedPanels(oldProps, newModel);
        final JPanel holderPropertiesPanel = (JPanel) splitPane.getRightComponent();
        holderPropertiesPanel.removeAll();
        if (selPath != null) {
            // returns node from previous model
            DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selPath.getLastPathComponent();
            if (selectedNode.getUserObject() instanceof ClassPropertiesInfo) {
                // find selected class by name
                ClassPropertiesInfo selectedClassInfo = (ClassPropertiesInfo) selectedNode.getUserObject();
                String selectedClassInfoName = selectedClassInfo.getName();
                // see if it's still visible
                DefaultMutableTreeNode classNode;
                ClassPropertiesInfo classInfo;
                DefaultMutableTreeNode root = (DefaultMutableTreeNode) newModel.getRoot();
                int classCount = newModel.getChildCount(root);
                for (int c = 0; c < classCount; c++) {
                    classNode = (DefaultMutableTreeNode) newModel.getChild(root, c);
                    classInfo = (ClassPropertiesInfo) classNode.getUserObject();
                    if (classInfo.getName().equalsIgnoreCase(selectedClassInfoName)) {
                        tree.setSelectionPath(new TreePath(classNode.getPath()));
                        c = classCount;
                    }
                }
            }
        }
        holderPropertiesPanel.repaint();
    }

    private void switchPermissionLevel() {
        if (permissionLvl == LEVEL.REGULAR) {
            permissionLvl = LEVEL.ADVANCED;
        }
        else {
            permissionLvl = LEVEL.REGULAR;
        }
        // Switch tree model
        tree.setModel(levelTrees.get(permissionLvl));
        // Model must be set before the selection path so that when TreeSelectionEvent is triggered the path already
        // refers to the intended model.
    }

    /**
     * Cycle through the nodes in the new model.<br>
     * For each node in the old model that also had a panel, create (for the new permission level) and transfer all the
     * properties in the old one that are still present. <br>
     * As panel is only created if is visualized, this guarantees that all previously modified properties keep
     * modifications.
     * 
     * @param oldProps properties inserted in the old model
     * @param newModel set the old properties in the new model
     */
    private void updateUsedPanels(HashMap<String, Property[]> oldProps, DefaultTreeModel newModel) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) newModel.getRoot();
        int childCount = newModel.getChildCount(root);
        DefaultMutableTreeNode node;
        ClassPropertiesInfo funcInfo;
        Property[] storedProps;
        for (int i = 0; i < childCount; i++) {
            node = (DefaultMutableTreeNode) newModel.getChild(root, i);
            funcInfo = (ClassPropertiesInfo) node.getUserObject();
            storedProps = oldProps.get(funcInfo.getName());
            // If this functionality was viewed it might contain changes
            if (storedProps != null) {
                // Create panel for it
                PropertySheetPanel propertyPanel = createPropertiesPanelForClass(funcInfo.getClassInstance());
                Property newProps[] = propertyPanel.getProperties();
                // go through properties
                int newPropsLength = newProps.length;
                int storedPropsLength = storedProps.length;
                // find who has more properties
                if (newPropsLength < storedPropsLength) {
                    // lowering the permission
                    int pStored = 0;
                    int pNew = 0;
                    while (pNew < newPropsLength && pStored < storedPropsLength) {
                        if (storedProps[pStored].getName().equals(newProps[pNew].getName())) {
                            newProps[pNew].setValue(storedProps[pStored].getValue());
                            pNew++;
                        }
                        pStored++;
                    }
                }
                else {
                    // going to higher permission
                    int pStored = 0;
                    int pNew = 0;
                    while (pNew < newPropsLength && pStored < storedPropsLength) {
                        if (storedProps[pStored].getName().equals(newProps[pNew].getName())) {
                            newProps[pNew].setValue(storedProps[pStored].getValue());
                            pStored++;
                        }
                        pNew++;
                    }
                }
                // set the updated properties panel
                funcInfo.setPropertiesPanel(propertyPanel);
            }
        }
    }

    private HashMap<String, Property[]> extractProperties(TreeModel model) {
        HashMap<String, Property[]> props = new HashMap<>();
        DefaultMutableTreeNode classNode;
        Object root = model.getRoot();
        ClassPropertiesInfo classInfo;
        int classCount = model.getChildCount(root);
        Property[] classProperties;
        for (int c = 0; c < classCount; c++) {
            classNode = (DefaultMutableTreeNode) model.getChild(root, c);
            classInfo = (ClassPropertiesInfo) classNode.getUserObject();
            PropertySheetPanel propertiesPanel = classInfo.getPropertiesPanel();
            if (propertiesPanel != null) {
                // force out of edit mode
                propertiesPanel.getTable().commitEditing();
                classProperties = propertiesPanel.getProperties();
                props.put(classInfo.getName(), classProperties.clone());
            }
        }
        return props;
    }

    /**
     * Set all properties panels to null to mark for refresh.
     * 
     * @param level for which the model will be reset
     */
    private void resetPropertiesPanels(LEVEL level) {
        TreeModel model = levelTrees.get(level);
        int functionalityCount = model.getChildCount(model.getRoot());
        ClassPropertiesInfo funcInfo;
        DefaultMutableTreeNode node;
        for (int i = 0; i < functionalityCount; i++) {
            node = (DefaultMutableTreeNode) model.getChild(model.getRoot(), i);
            funcInfo = (ClassPropertiesInfo) node.getUserObject();
            funcInfo.setPropertiesPanel(null);
        }
    }

    private ArrayList<DefaultMutableTreeNode> buildTreeNodes(Vector<PropertiesProvider> subPanels) {
        // Available functionalities
        PropertiesProvider subPanel;
        ArrayList<DefaultMutableTreeNode> unsortedNodes = new ArrayList<DefaultMutableTreeNode>();
        for (Iterator<PropertiesProvider> iterator = subPanels.iterator(); iterator.hasNext();) {
            subPanel = iterator.next();
            findPropertiesCreateNodeUpdateNodeList(subPanel, unsortedNodes);
        }
        // General preferences
        GeneralPreferences genPref = new GeneralPreferences();
        findPropertiesCreateNodeUpdateNodeList(genPref, unsortedNodes);
        return unsortedNodes;
    }

    private void findPropertiesCreateNodeUpdateNodeList(PropertiesProvider subPanel,
            ArrayList<DefaultMutableTreeNode> unsortedNodes) {
        // find properties
        PropertySheetPanel propertiesPanel = createPropertiesPanelForClass(subPanel);
        // create node and update tree
        if (propertiesPanel.getPropertyCount() > 0) {
            unsortedNodes.add(createNode(subPanel, propertiesPanel));
        }
    }

    private DefaultMutableTreeNode createNode(PropertiesProvider funcClass, PropertySheetPanel propertiesPanel) {
        PluginDescription pluginAnnotation = funcClass.getClass().getAnnotation(PluginDescription.class);
        String name, icon;
        if (pluginAnnotation == null) {
            name = funcClass.getPropertiesDialogTitle();
            if (name == null || name.isEmpty())
                name = funcClass.getClass().getSimpleName();
            icon = "";
        }
        else {
            name = pluginAnnotation.name();
            if (name == null || name.length() == 0) {
                name = funcClass.getPropertiesDialogTitle();
                if (name == null || name.isEmpty())
                    name = funcClass.getClass().getSimpleName();
                char firstLetter = Character.toUpperCase(name.charAt(0));
                name = firstLetter + name.substring(1);
            }
            icon = pluginAnnotation.icon();
        }
        ClassPropertiesInfo classInfo = new ClassPropertiesInfo(funcClass, null, name, propertiesPanel, icon);
        return new DefaultMutableTreeNode(classInfo);
    }

    private ArrayList<DefaultMutableTreeNode> sortNodes(ArrayList<DefaultMutableTreeNode> unsortedNodes) {
        Collections.sort(unsortedNodes, new Comparator<DefaultMutableTreeNode>() {
            @Override
            public int compare(DefaultMutableTreeNode n1, DefaultMutableTreeNode n2) {
                String s1 = I18n.text(((ClassPropertiesInfo) n1.getUserObject()).getName());
                String s2 = I18n.text(((ClassPropertiesInfo) n2.getUserObject()).getName());
                Collator collator = Collator.getInstance(Locale.US);
                return collator.compare(s1, s2);
            }
        });
        return unsortedNodes;
    }

    private DefaultTreeModel createTreeModel(Vector<PropertiesProvider> subPanels) {
        // Find nodes
        ArrayList<DefaultMutableTreeNode> nodes = buildTreeNodes(subPanels);
        // Sort them
        nodes = sortNodes(nodes);
        // Create JTree
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");
        // Add nodes because
        // if the nodes are added after setting root node invisible all will be invisible!
        DefaultMutableTreeNode defaultMutableTreeNode;
        for (Iterator<DefaultMutableTreeNode> nodeIt = nodes.iterator(); nodeIt.hasNext();) {
            defaultMutableTreeNode = nodeIt.next();
            root.add(defaultMutableTreeNode);
        }
        return new DefaultTreeModel(root);
    }

    private <T> PropertySheetPanel createPropertiesPanelForClass(PropertiesProvider funcClass) {
        initEditorRegistry();
        initRenderRegistry();
        final PropertySheetPanel propertiesPanel = new PropertySheetPanel();
        propertiesPanel.setEditorFactory(pEditorRegistry);
        propertiesPanel.setRendererFactory(pRenderRegistry);
        propertiesPanel.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        propertiesPanel.setSortingCategories(true);
        propertiesPanel.setDescriptionVisible(true);
        propertiesPanel.setSorting(true);
        propertiesPanel.setToolBarVisible(false);

        NeptusProperty neptusProperty = null;
        LEVEL userLevel;
//        for (Field f : funcClass.getClass().getFields()) {
        for (Field f : funcClass.getClass().getDeclaredFields()) {
            neptusProperty = f.getAnnotation(NeptusProperty.class);
            if (neptusProperty == null)
                continue;
            
            f.setAccessible(true); // To be able to access private and protected NeptusProperties
            
            // CLIENT / DEVELOPER
            if (clientConsole && neptusProperty.distribution() == DistributionEnum.DEVELOPER)
                continue;
            // ADVANCED / REGULAR
            userLevel = neptusProperty.userLevel();
            if (permissionLvl.getLevel() < userLevel.getLevel())
                continue;
            PluginProperty pp;
            try {
                pp = extractPluginProperty(f, funcClass);
            }
            catch (Exception e) {
                NeptusLog.pub().error(funcClass.getClass().getSimpleName() + "." + f.getName(), e);
                throw e;
            }
            if (pp != null)
                propertiesPanel.addProperty(pp);
        }
        return propertiesPanel;
    }

    @SuppressWarnings("unchecked")
    private <T> PluginProperty extractPluginProperty(Field f, T class1) {
        NeptusProperty neptusProperty = f.getAnnotation(NeptusProperty.class);
        Object fieldValue = null;
        try {
            fieldValue = f.get(class1);
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        
        // Name
        String nameRaw = neptusProperty.name();
        String displayName;
        if (nameRaw == null || nameRaw.length() == 0) {
            nameRaw = f.getName();
            char firstLetter = Character.toUpperCase(nameRaw.charAt(0));
            displayName = firstLetter + nameRaw.substring(1);
        }
        else {
            displayName = nameRaw;
        }
        // Type
        Class<?> type = f.getType();

        PluginProperty pp = new PluginProperty(nameRaw, type, fieldValue);
        pp.setValue(fieldValue);

        // Editable
        if (neptusProperty.editable() == false) {
            pp.setEditable(false);
        }
        else {
            pp.setEditable(true);
        }
        // Display name
        // signal the scope is the whole Neptus
        if (class1.getClass().equals(GeneralPreferences.class))
            displayName = "* " + I18n.text(displayName);
        else
            displayName = I18n.text(displayName);
        pp.setDisplayName(displayName);
        // Category
        if (neptusProperty.category() != null) {
            pp.setCategory(I18n.text(neptusProperty.category()));
        }
        // Short description
        Map<String, PluginProperty> hashMap = PluginUtils.getDefaultsValues(class1);
        StringBuilder description = new StringBuilder();
        description.append(I18n.text(neptusProperty.description()));
        String defaultValue;
        if (hashMap == null) {
            // no value!
            defaultValue = I18n.textf("No default found for class %className", class1.getClass().getSimpleName());
        }
        else {
            PluginProperty pluginProperty = hashMap.get(f.getName());
            if (pluginProperty == null) {
                // no value!
                defaultValue = I18n.textf("No default found for field %fieldName", f.getName());
            }
            else {
                Object defaultPropValue = pluginProperty.getValue();
                defaultValue = (defaultPropValue == null ? I18n.text("Absence of value") : ((f.getType()
                        .getEnumConstants() != null ? I18n.text(defaultPropValue.toString()) : defaultPropValue
                        .toString())));
            }
        }
        description.append(" (");
        description.append(I18n.text("Default value"));
        description.append(": ");
        description.append(defaultValue);
        description.append(")");
        pp.setShortDescription(description.toString());

        // Editor class - ATTENTION must be the last or wont work!
        Class<? extends PropertyEditor> editClass = null;
        if (neptusProperty.editorClass() != PropertyEditor.class) {
            editClass = neptusProperty.editorClass();
        }
        if (editClass != null) {
            pEditorRegistry.registerEditor(pp, editClass);
        }
        else {
            if (ReflectionUtil.hasInterface(f.getType(), PropertyType.class)) {
                PropertyType pt = (PropertyType) fieldValue;
                pEditorRegistry.registerEditor(pp, pt.getPropertyEditor());
            }
            if (f.getType().getEnumConstants() != null) {
                if (fieldValue != null) {
                    pEditorRegistry
                            .registerEditor(pp, new EnumEditor((Class<? extends Enum<?>>) fieldValue.getClass()));
                }
            }
        }
        return pp;
    }

    private void initEditorRegistry() {
        pEditorRegistry = new PropertyEditorRegistry();
        pEditorRegistry.registerDefaults();
        pEditorRegistry.registerEditor(LocationType.class, LocationTypePropertyEditor.class);
        pEditorRegistry.registerEditor(VehicleType.class, VehicleSelectionEditor.class);
        pEditorRegistry.registerEditor(Script.class, ScriptSelectionEditor.class);
        pEditorRegistry.registerEditor(RenderType.class, RenderSelectionEditor.class);
        pEditorRegistry.registerEditor(Enumerated.class, EnumeratedPropertyEditor.class);
        pEditorRegistry.registerEditor(Bitmask.class, BitmaskPropertyEditor.class);
        pEditorRegistry.registerEditor(ColorMap.class, ColorMapPropertyEditor.class);
        pEditorRegistry.registerEditor(ImcId16.class, ImcId16Editor.class);
        pEditorRegistry.registerEditor(PlanActions.class, PlanActionsEditor.class);
        pEditorRegistry.registerEditor(Double.class, NeptusDoubleEditor.class);
        pEditorRegistry.registerEditor(Float.class, NeptusDoubleEditor.class);
    }

    @SuppressWarnings("serial")
    void initRenderRegistry() {
        pRenderRegistry = new PropertyRendererRegistry();
        pRenderRegistry.registerDefaults();
        pRenderRegistry.registerRenderer(ImcId16.class, new DefaultCellRenderer() {
            private static final long serialVersionUID = 4138765256774163544L;
            {
                setShowOddAndEvenRows(false);
            }

            @Override
            protected String convertToString(Object value) {
                try {
                    ImcId16 id = (ImcId16) value;
                    return id.toPrettyString();
                }
                catch (Exception e) {
                    return super.convertToString(value);
                }
            }
        });
        pRenderRegistry.registerRenderer(LocationType.class, new DefaultCellRenderer() {
            private static final long serialVersionUID = -1063560117208559935L;
            {
                setShowOddAndEvenRows(false);
            }
            private String toolTip = "";

            @Override
            protected String convertToString(Object value) {
                try {
                    LocationType loc = (LocationType) value;
                    toolTip = loc.toString();
                    setToolTipText(toolTip);
                    return loc.toString();
                }
                catch (Exception e) {
                    return super.convertToString(value);
                }
            }
        });
        pRenderRegistry.registerRenderer(Double.class, new DefaultCellRenderer() {
            private static final long serialVersionUID = 4694991901775088021L;
            {
                setOpaque(false);
            }
            protected DecimalFormat format = new DecimalFormat("0.0######");

            @Override
            protected String convertToString(Object value) {
                return format.format(value);
            }
        });
        pRenderRegistry.registerRenderer(Enum.class, new DefaultCellRenderer() {
            {
                setOpaque(false);
            }

            @Override
            protected String convertToString(Object value) {
                return I18n.text(value.toString());
            }
        });

    }

    class IconRenderer extends DefaultTreeCellRenderer {
        private static final long serialVersionUID = 7206236693078391402L;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
            Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
            if (userObject instanceof ClassPropertiesInfo) {
                setIcon(((ClassPropertiesInfo) userObject).getIcon());
                setText(I18n.text(((ClassPropertiesInfo) userObject).getName()));
            }

            return this;
        }
    }
}

