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
 * Author: Paulo Dias
 * Mar 2, 2013
 */
package pt.lsts.neptus.params;

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;

import com.l2fprod.common.propertysheet.Property;
import com.l2fprod.common.propertysheet.PropertyEditorRegistry;
import com.l2fprod.common.propertysheet.PropertyRendererRegistry;
import com.l2fprod.common.propertysheet.PropertySheet;
import com.l2fprod.common.propertysheet.PropertySheetPanel;
import com.l2fprod.common.propertysheet.PropertySheetTableModel.Item;
import net.miginfocom.swing.MigLayout;

import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.EntityParameters;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.QueryEntityParameters;
import pt.lsts.imc.QueryTypedEntityParameters;
import pt.lsts.imc.SaveEntityParameters;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.admin.CommsAdmin;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.comm.manager.imc.MessageDeliveryListener;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.params.util.HandleQtepReply;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class SystemConfigurationEditorPanel extends JPanel implements PropertyChangeListener {

    public static final String CARD_PROPERTIES = "properties";
    public static final String CARD_CATEGORIES = "categories";
    public static final String CARD_PROGRESS = "progress";

    protected final LinkedHashMap<String, SystemProperty> params = new LinkedHashMap<>();

    private static boolean isAskForCategories = true;
    List<String> expectedCategories = null;

    // this variable can be null
    public ConsoleLayout console;

    private JPanel swapPropertiesAndCategoriesPanel;
    private JPanel mainPanel;
    private JPanel categoriesPanel;
    private JPanel vehiclePanel;
    private JPanel uiPanel;
    private JPanel settingsPanel;

    private JPanel propertiesPanel;
    private InfiniteProgressPanel progressPanel;
    protected PropertySheetPanel psp;
    private JButton sendButton;
    private JButton saveButton;
    private JButton refreshButton;
    private JButton resetButton;
    private JButton collapseButton;
    private JButton expandButton;
    private JButton syncDefinitionsButton;
    private JButton generateXmlButton;

    private JLabel titleLabel;
    private JCheckBox checkAdvance;
    private JCheckBox checkSelection;
    private JCheckBox checkGenerateXml;
    private JComboBox<Scope> scopeComboBox;
    private JToggleButton fakeSyncButton;

    protected boolean refreshing = false;
    private PropertyEditorRegistry per;
    private PropertyRendererRegistry prr;

    private Scope scopeToUse = Scope.GLOBAL;
    private Visibility visibility = Visibility.USER;

    protected String systemId;
    protected ImcSystem sid = null;
    protected int reqId;

    private final Map<String, SystemProperty> receivedQtepProps = new LinkedHashMap<>();
    private boolean isSyncing = false;
    private final Set<String> receivedEntities = Collections.synchronizedSet(new HashSet<>());
    private HandleQtepReply qtepHandler;
    private volatile long syncStartTime = 0;
    private boolean isToSave = false;
    private static final ImageIcon WARNING_ICON = ImageUtils.createImageIcon("images/icons/noty-warning.png");

    protected ImcMsgManager imcMsgManager;

    // Category name, category label on gui
    private Map<String, String> touchedCategoriesList;
    private Map<String, String> lastSelectedCategoriesList;

    public SystemConfigurationEditorPanel(String systemId, Scope scopeToUse, Visibility visibility,
            boolean showSendButton, boolean showScopeCombo, boolean showResetButton, ImcMsgManager imcMsgManager, ConsoleLayout console ) {
        this(systemId, scopeToUse, visibility, showSendButton, showScopeCombo, showResetButton, false, imcMsgManager, console);
    }

    public SystemConfigurationEditorPanel(String systemId, Scope scopeToUse, Visibility visibility,
                                          boolean showSendButton, boolean showScopeCombo, boolean showResetButton,
                                          boolean showFakeSyncButton, ImcMsgManager imcMsgManager, ConsoleLayout console) {
        this.systemId = systemId;
        this.imcMsgManager = imcMsgManager;
        
        this.scopeToUse = scopeToUse;
        this.visibility = visibility;
        this.console = console;

        this.qtepHandler = new HandleQtepReply(console, systemId);
        this.qtepHandler.setOwner(this);
        console.getImcMsgManager().addListener(qtepHandler);

        initialize(showSendButton, showScopeCombo, showResetButton, showFakeSyncButton);
    }

    private void initialize(boolean showSendButton, boolean showScopeCombo, boolean showResetButton, boolean showFakeSyncButton) {
        setLayout(new MigLayout());

        mainPanel = new JPanel(new MigLayout("fill, insets 0"));

        scopeComboBox = new JComboBox<Scope>(Scope.values()) {
            public void setSelectedItem(Object anObject) {
                super.setSelectedItem(anObject);
            }
        };
        scopeComboBox.setRenderer(new ListCellRenderer<Scope>() {
            @Override
            public Component getListCellRendererComponent(JList<? extends Scope> list, Scope value, int index,
                    boolean isSelected, boolean cellHasFocus) {
                JLabel label = new JLabel(I18n.text(value.getText()));
                label.setOpaque(true);
                if (isSelected) {
                    label.setBackground(list.getSelectionBackground());
                    label.setForeground(list.getSelectionForeground());
                }
                else {
                    label.setBackground(list.getBackground());
                    label.setForeground(list.getForeground());
                }
    
                return label;
            }
        });
        scopeComboBox.setSelectedItem(scopeToUse);
        scopeComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                scopeToUse = (Scope) e.getItem();
                new Thread(() -> {
                    setSystemId(getSystemId());
                }).start();
            }
        });
        
        titleLabel = new JLabel("<html><b>" + createTitle() + "</b></html>");
        mainPanel.add(titleLabel, "w 100%, wrap");

        // Configure Property sheet
        psp = new PropertySheetPanel();
        psp.setSortingCategories(true);
        psp.setSortingProperties(false);
        psp.setDescriptionVisible(true);
        psp.setMode(PropertySheet.VIEW_AS_CATEGORIES);
        psp.setToolBarVisible(false);
        
        resetPropertiesEditorAndRendererFactories();

        propertiesPanel = new JPanel(new CardLayout());
        propertiesPanel.add(psp, CARD_PROPERTIES);
        progressPanel = new InfiniteProgressPanel(I18n.text("Please wait..."));
        propertiesPanel.add(progressPanel, CARD_PROGRESS);

        mainPanel.add(propertiesPanel, "w 100%, h 100%, wrap");

        categoriesPanel = new JPanel(new MigLayout("fill, insets 0"));

        swapPropertiesAndCategoriesPanel = new JPanel(new CardLayout());
        swapPropertiesAndCategoriesPanel.add(mainPanel, CARD_PROPERTIES);
        swapPropertiesAndCategoriesPanel.add(categoriesPanel, CARD_CATEGORIES);

        add(swapPropertiesAndCategoriesPanel, "w 100%, h 100%");

        vehiclePanel = new JPanel(new MigLayout("insets 0", "[grow, left]"));

        sendButton = new JButton(new AbstractAction(I18n.text("Send")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        sendPropertiesToSystem();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        sendButton.setToolTipText(I18n.text("Send the modified properties."));
        if (showSendButton) {
            vehiclePanel.add(sendButton, "sg buttons1, split, gapbefore 5px");
        }

        refreshButton = new JButton(new AbstractAction(I18n.text("Refresh")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        refreshPropertiesOnPanel(true, true,
                                new String[] {CommsAdmin.CommChannelType.WIFI.name, CommsAdmin.CommChannelType.IRIDIUM.name});
                        return null;
                    }
                };
                worker.execute();
            }
        });
        refreshButton.setToolTipText(I18n.text("Requests the entities sections parameters from the vehicle."));
        vehiclePanel.add(refreshButton, "sg buttons1, split");

        saveButton = new JButton(new AbstractAction(I18n.text("Save")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        savePropertiesToSystem();
                        return null;
                    }
                };
                worker.execute();
            }
        });
        saveButton.setToolTipText(I18n.text("Saves the visible entities sections in the vehicle."));
        if (showSendButton) {
            vehiclePanel.add(saveButton, "sg buttons1, split");
        }

        resetButton = new JButton(new AbstractAction(I18n.text("Reset")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetPropertiesOnPanel();
            }
        });
        resetButton.setToolTipText(I18n.text("Local reset. Needs to be sent to system."));
        if (showResetButton)
            vehiclePanel.add(resetButton, "sg buttons1, split");
        resetButton.setToolTipText(I18n.text("Local reset. Needs to be sent to system."));

        fakeSyncButton = new JToggleButton(new AbstractAction(I18n.text("Consider Sync")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        // Inside the refreshPropertiesOnPanel we look at this button state and act accordingly
                        refreshPropertiesOnPanel(false, false, false, new String[0]);
                        return null;
                    }
                };
                worker.execute();
            }
        });
        fakeSyncButton.setToolTipText(I18n.text("Consider sync with the system, useful for reducing parameters to send."));
        if (showFakeSyncButton) {
            vehiclePanel.add(fakeSyncButton, "sg buttons1, split, gapbefore push, gapafter 5px");
        }

        mainPanel.add(vehiclePanel, "growx, wrap");

        uiPanel = new JPanel(new MigLayout("insets 0", "[grow, left]"));

        collapseButton = new JButton(new AbstractAction(I18n.text("Collapse All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < psp.getTable().getSheetModel().getRowCount(); i++) {
                    Item o = (Item) psp.getTable().getSheetModel().getObject(i);
                    if (o.isVisible() && !o.hasToggle()) {
                        o.getParent().toggle();
                    }
                }
            }
        });
        collapseButton.setToolTipText(I18n.text("Collapse all sections."));
        uiPanel.add(collapseButton, "sg buttons3, split, gapbefore 5px");

        expandButton = new JButton(new AbstractAction(I18n.text("Expand All")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < psp.getTable().getSheetModel().getRowCount(); i++) {
                    Item o = (Item) psp.getTable().getSheetModel().getObject(i);
                    if (!o.isVisible()) {
                        if (o.hasToggle() && !o.isVisible()) {
                            o.toggle();
                        } else if (!o.hasToggle() && o.getParent() != null && !o.getParent().isVisible()) {
                            o.getParent().toggle();
                        }
                    }
                }
            }
        });
        uiPanel.add(expandButton, "sg buttons3, split");
        expandButton.setToolTipText(I18n.text("Expand all sections."));

        syncDefinitionsButton = new JButton(new AbstractAction(I18n.text("Sync Definitions")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (sid.isSimulated()) {
                    showSimulatedVehicleConfirmation();
                } else {
                    doSyncParameters();
                }
            }
        });
        syncDefinitionsButton.setToolTipText(I18n.text("Synchronize local parameters configuration with vehicle configuration."));

        uiPanel.add(syncDefinitionsButton, "sg buttons4, split, gapbefore push");
        /*generateXmlButton = new JButton(new AbstractAction(I18n.text("Generate Definitions File")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    generateXMLFile();
                }
                catch (ParserConfigurationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });*/
        //uiPanel.add(generateXmlButton, "sg buttons4, split, gapafter 5px");
        checkGenerateXml = new JCheckBox(I18n.text("Save Current Definitions"));
        uiPanel.add(checkGenerateXml, "sg buttons6, split, gapafter 5px");
        checkGenerateXml.addItemListener(e -> {
            isToSave = true;
        });

        mainPanel.add(uiPanel, "growx, wrap");

        settingsPanel = new JPanel(new MigLayout("insets 0", "[grow, left]"));

        if (showScopeCombo)
            settingsPanel.add(scopeComboBox, "split, w :160:");

        checkAdvance = new JCheckBox(I18n.text("Access Developer Parameters"));
        checkAdvance.setToolTipText("<html>" + I18n.textc("Be careful changing these values.<br>They may make the vehicle inoperable.",
                "This will be a tooltip, and use <br> to change line."));
        settingsPanel.add(checkAdvance, "split, sg checkboxes, gapbefore 5px");
        checkAdvance.setSelected(visibility == Visibility.DEVELOPER);
        checkAdvance.addItemListener(e -> {
            if (checkAdvance.isSelected())
                visibility = Visibility.DEVELOPER;
            else
                visibility = Visibility.USER;

            // FIXME This might not make sense to not always ask for categories
            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    refreshPropertiesOnPanel(false, false, new String[] {CommsAdmin.CommChannelType.WIFI.name});
                    return null;
                }
            };
            worker.execute();
        });
        checkAdvance.setFocusable(false);

        checkSelection = new JCheckBox(I18n.text("Ask for categories"));
        checkSelection.setToolTipText("<html>" + I18n.textc("Ask for categories before send.",
                "This will be a tooltip, and use <br> to change line."));
        checkSelection.addItemListener(e -> {
            isAskForCategories = checkSelection.isSelected();
            updateSendButtons();
        });
        checkSelection.setSelected(isAskForCategories);
        checkSelection.setFocusable(false);
        settingsPanel.add(checkSelection, "sg checkboxes");

        mainPanel.add(settingsPanel, "growx, wrap");

        // FIXME This might not make sense to not always ask for categories if no wifi
        refreshPropertiesOnPanel(false, false, new String[] {CommsAdmin.CommChannelType.WIFI.name});

        revalidate();
        repaint();
    }

    private void doSyncParameters() {

        if (isSyncing) {
            return;
        }

        isSyncing = true;
        syncStartTime = System.currentTimeMillis();

        receivedQtepProps.clear();
        receivedEntities.clear();

        syncDefinitionsButton.setEnabled(false);
        syncDefinitionsButton.setText(I18n.text("Syncing..."));

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                try {
                    if (isAskForCategories) {
                        expectedCategories = askForCategories("sync", lastSelectedCategoriesList).get();
                        sendQtepRequests(expectedCategories, true);
                    }
                    else {
                        expectedCategories =
                                new ArrayList<>(EntitiesResolver.getEntities(systemId).values());
                        sendQtepRequests(null, false);
                    }
                    qtepHandler.startSync(SystemConfigurationEditorPanel.this, systemId, expectedCategories, reqId, true);
                }
                catch (Exception e) {
                    isSyncing = false;
                    expectedCategories = null;
                    SwingUtilities.invokeLater(() -> {
                        syncDefinitionsButton.setEnabled(true);
                        if (isAskForCategories) {
                            syncDefinitionsButton.setText(I18n.text("Sync Definitions >"));
                        } else {
                            syncDefinitionsButton.setText(I18n.text("Sync Definitions"));
                        }
                    });
                }
                return null;
            }
        };

        worker.execute();
    }

    public void onQtepSyncFinished(Map<String, SystemProperty> props) {
        if (!props.isEmpty()) {
            ConfigurationManager manager = ConfigurationManager.getInstance();
            Map<String, SystemProperty> local =
                    manager.listToMap(manager.getProperties(systemId, visibility, scopeToUse));

            Map<String, SystemProperty> merged =
                    manager.mergeSystemProperties(local, props);

            manager.setProperties(systemId, merged, isToSave);
            reloadPropertiesOnPanel(merged, isToSave);
        }

        isSyncing = false;

        syncDefinitionsButton.setEnabled(true);
        syncDefinitionsButton.setText(
                isAskForCategories
                        ? I18n.text("Sync Definitions >")
                        : I18n.text("Sync Definitions")
        );
    }

    public void applyExternalQtepDefinitions(String systemId,
                                             Map<String, SystemProperty> props, boolean save) {

        ConfigurationManager mgr = ConfigurationManager.getInstance();

        Map<String, SystemProperty> local =
                mgr.listToMap(mgr.getProperties(
                        systemId, visibility, scopeToUse));

        Map<String, SystemProperty> merged =
                mgr.mergeSystemProperties(local, props);

        mgr.setProperties(systemId, merged, save);
    }

    private void reloadPropertiesOnPanel(Map<String, SystemProperty> merged, boolean save) {

        showWaiterUpdateProperties(true);

        try {
            List<String> openCategories = removeAllPropertiesFromPanel();
            resetPropertiesEditorAndRendererFactories();
            params.clear();

            long now = System.currentTimeMillis();
            Visibility currentVisibility = this.visibility;

            for (SystemProperty sp : merged.values()) {
                if (sp.getVisibility() == Visibility.DEVELOPER && currentVisibility == Visibility.USER) {
                    continue;
                }

                params.put(sp.getCategoryId() + "." + sp.getName(), sp);
                sp.addPropertyChangeListener(this);
                psp.addProperty(sp);

                if (sp.getEditor() != null) {
                    per.registerEditor(sp, sp.getEditor());
                }

                if (sp.getRenderer() != null) {
                    prr.registerRenderer(sp, sp.getRenderer());
                }

                // fake sync update with null check
                if (fakeSyncButton != null && fakeSyncButton.isSelected()) {
                    sp.setTimeFakeSync(now);
                } else {
                    sp.resetTimeFakeSync();
                }
            }

            // Let us make sure all dependencies between properties are ok
            for (SystemProperty spCh : params.values()) {
                for (SystemProperty sp : params.values()) {
                    PropertyChangeEvent evt = new PropertyChangeEvent(spCh, spCh.getName(), null, spCh.getValue());
                    sp.propertyChange(evt);
                }
            }

            // Close all categories
            for (int i = 0; i < psp.getTable().getSheetModel().getRowCount(); i++) {
                Item o = (Item) psp.getTable().getSheetModel().getObject(i);
                if (o.isVisible() && !o.hasToggle()) {
                    if (!openCategories.contains(o.getParent().getName())) {
                        o.getParent().toggle();
                    }
                }
            }

            revalidate();
            repaint();

        } finally {
            showWaiterUpdateProperties(false);
            if (save) {
                try {
                    generateXMLFile(systemId);
                }
                catch (ParserConfigurationException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    private void sendQtepRequests(List<String> entities, boolean askForCategories) {
        QueryTypedEntityParameters req = new QueryTypedEntityParameters();
        reqId = IMCSendMessageUtils.getNextRequestId();
        req.setOp(QueryTypedEntityParameters.OP.REQUEST);
        req.setRequestId(reqId);
        if (askForCategories && entities != null) {
            for (String entity: entities) {
                req.setEntityName(entity);
                send(req, true, CommsAdmin.CommChannelType.WIFI.name);
            }
        } else {
            req.setEntityName("");
            send(req, true, CommsAdmin.CommChannelType.WIFI.name);
        }
    }

    private void generateXMLFile(String systemId) throws ParserConfigurationException {
        SystemProperty.Scope scopeToUse = SystemProperty.Scope.GLOBAL;
        SystemProperty.Visibility visibility = SystemProperty.Visibility.DEVELOPER;
        ConfigurationManager.getInstance().generateXML(systemId, visibility, scopeToUse);
    }

    private void updateSendButtons() {
        List<Action> actions = new ArrayList<>();
        actions.add(sendButton.getAction());
        actions.add(refreshButton.getAction());
        actions.add(saveButton.getAction());
        actions.add(syncDefinitionsButton.getAction());

        String suffix = " >";
        for (Action action : actions) {
            String name = (String) action.getValue("Name");
            if (!isAskForCategories && name.endsWith(suffix)) {
                name = name.substring(0, name.length() - 2);
            } else if (isAskForCategories && !name.endsWith(suffix)) {
                name += suffix;
            }
            action.putValue("Name", name);
        }
    }

    private void resetPropertiesEditorAndRendererFactories() {
        per = new PropertyEditorRegistry();
        // per.registerDefaults();
        psp.setEditorFactory(per);
        prr = new PropertyRendererRegistry();
        // prr.registerDefaults();
        psp.setRendererFactory(prr);
    }
    
    /**
     * @return the params
     */
    public LinkedHashMap<String, SystemProperty> getParams() {
        return params;
    }
    
    /**
     * @return the systemId
     */
    public String getSystemId() {
        return systemId;
    }
    
    /**
     * @param systemId the systemId to set
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
        sid = ImcSystemsHolder.getSystemWithName(this.systemId);
        fakeSyncButton.setSelected(false);
        // FIXME This might not make sense to not always ask for categories
        refreshPropertiesOnPanel(false, false, new String[]{CommsAdmin.CommChannelType.WIFI.name});
    }

    /**
     * @return the refreshing
     */
    public boolean isRefreshing() {
        return refreshing;
    }
    
    /**
     * @param refreshing the refreshing to set
     */
    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }
    
    private synchronized void refreshPropertiesOnPanel(boolean askForCategories, boolean popGuiOnError, String[] channelsToUse) {
        refreshPropertiesOnPanel(askForCategories, popGuiOnError, true, channelsToUse);
    }

    private synchronized void refreshPropertiesOnPanel(boolean askForCategories, boolean popGuiOnError, boolean askForRefresh, String[] channelsToUse) {
        try {
            showWaiterUpdateProperties(true);

            // FIXME
            //Map<String, String> oldCategoriesOnPanel = getCategoriesOnPanel(true);

            titleLabel.setText("<html><b>" + createTitle() + "</b></html>");
            List<String> openCategories = removeAllPropertiesFromPanel();

            resetPropertiesEditorAndRendererFactories();

            ArrayList<SystemProperty> pr = ConfigurationManager.getInstance().getProperties(systemId, visibility, scopeToUse);
            ArrayList<String> secNames = new ArrayList<>();
            long now = System.currentTimeMillis();
            for (SystemProperty sp : pr) {
                String sectionName = sp.getCategoryId();
                String name = sp.getName();
                if (!secNames.contains(sectionName))
                    secNames.add(sectionName);
                params.put(sectionName + "." + name, sp);
                sp.addPropertyChangeListener(this);
                psp.addProperty(sp);
                if (sp.getEditor() != null) {
                    per.registerEditor(sp, sp.getEditor());
                }
                if (sp.getRenderer() != null) {
                    prr.registerRenderer(sp, sp.getRenderer());
                }

                // Check if fake sync is enabled
                if (fakeSyncButton.isSelected()) {
                    sp.setTimeFakeSync(now);
                } else {
                    sp.resetTimeFakeSync();
                }
            }
            // Let us make sure all dependencies between properties are ok
            for (SystemProperty spCh : params.values()) {
                for (SystemProperty sp : params.values()) {
                    PropertyChangeEvent evt = new PropertyChangeEvent(spCh, spCh.getName(), null, spCh.getValue());
                    sp.propertyChange(evt);
                }
            }

            // Close all categories
            for (int i = 0; i < psp.getTable().getSheetModel().getRowCount(); i++) {
                Item o = (Item) psp.getTable().getSheetModel().getObject(i);
                if (o.isVisible() && !o.hasToggle()) {
                    if (!openCategories.contains(o.getParent().getName())) {
                        o.getParent().toggle();
                    }
                }
            }

            List<String> queryCategoriesList;
            if (askForCategories && isAskForCategories) {
                List<String> validCategories;
                try {
                    validCategories = askForCategories("refresh", lastSelectedCategoriesList).get();
                }
                catch (Exception e) {
                    // Do nothing
                    validCategories = Collections.emptyList();
                }
                queryCategoriesList = new ArrayList<>();
                for (String category : validCategories) {
                    if (category != null && validCategories.contains(category))
                        queryCategoriesList.add(category);
                }
            } else {
                queryCategoriesList = secNames;
            }

            showWaiterUpdateProperties(false);
            revalidate();
            repaint();

            if (askForRefresh) {
                for (String sectionName : queryCategoriesList) {
                    boolean ret = queryValues(sectionName, scopeToUse.getText(), visibility.getText(), popGuiOnError, channelsToUse);
                    if (!ret) {
                        break;
                    }
                }
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        } finally {
            showWaiterUpdateProperties(false);
            revalidate();
            repaint();
        }
    }

    private synchronized void resetPropertiesOnPanel() {
        for (SystemProperty sp : params.values()) {
            sp.resetToDefault();
        }
        psp.repaint();
    }

    private List<String> removeAllPropertiesFromPanel() {
        List<String> toggledCategories = new ArrayList<>();
        for (int i = 0; i < psp.getTable().getSheetModel().getRowCount(); i++) {
            Item o = (Item) psp.getTable().getSheetModel().getObject(i);
            if (o.isVisible() && !o.hasToggle()) {
                String name = o.getParent().getName();
                if (!toggledCategories.contains(name)) {
                    toggledCategories.add(name);
                }
            }
        }

        params.clear();
        for (Property p : psp.getProperties()) {
            psp.removeProperty(p);
        }

        return toggledCategories;
    }

    private void showWaiterUpdateProperties(boolean wait) {
        ((CardLayout) propertiesPanel.getLayout()).show(propertiesPanel, wait ? CARD_PROGRESS : CARD_PROPERTIES);
        progressPanel.setBusy(wait);
    }

    private String createTitle() {
        return I18n.textf("%systemName Parameters", getSystemId() == null ? "" : getSystemId());
    }

    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
//        NeptusLog.pub().info("<###>--------------- " + evt);
        if(!refreshing && evt.getSource() instanceof SystemProperty) {
            SystemProperty sp = (SystemProperty) evt.getSource();
            sp.setValue(evt.getNewValue());
            
            for (SystemProperty sprop : params.values()) {
                sprop.propertyChange(evt);
            }
            sp.propertyChange(evt);

            if (touchedCategoriesList == null)
                touchedCategoriesList = new LinkedHashMap<>();
            touchedCategoriesList.putIfAbsent(sp.getCategoryId(), sp.getCategory());
        }
    }
    
    private boolean queryValues(String entityName, String scope, String visibility, boolean popGuiOnError, String... channelsToUse) {
        QueryEntityParameters qep = new QueryEntityParameters();
        qep.setScope(scope);
        qep.setVisibility(visibility);
        qep.setName(entityName);
        return send(qep, popGuiOnError, channelsToUse);
    }

    private boolean saveRequest(String entityName) {
        SaveEntityParameters qep = new SaveEntityParameters();
        qep.setName(entityName);
        return send(qep, true);
    }

    private boolean sendProperty(SystemProperty... propsList) {
        Map<String, ArrayList<EntityParameter>> mapCategoryParameterList = new LinkedHashMap<>();
        for (SystemProperty prop : propsList) {
            if (prop.getValue() == null)
                continue;

            String category = prop.getCategoryId();
            if (category == null)
                continue;
            
            EntityParameter ep = new EntityParameter();
            ep.setName(prop.getName());
            boolean isList = false;
            if (ArrayList.class.equals(prop.getType()))
                isList = true;
            String str = (String) prop.getValue().toString();
            if (isList)
                str = ConfigurationManager.convertArrayListToStringToPropValueString(str);
            ep.setValue(str);

            ArrayList<EntityParameter> entParamList = mapCategoryParameterList.get(category);
            if (entParamList == null) {
                entParamList = new ArrayList<>();
                mapCategoryParameterList.put(category, entParamList);
            }
            entParamList.add(ep);
        }

        ArrayList<SetEntityParameters> msgs = new ArrayList<>(mapCategoryParameterList.size());
        for (String cat : mapCategoryParameterList.keySet()) {
            ArrayList<EntityParameter> propList = mapCategoryParameterList.get(cat);
            SetEntityParameters setParams = new SetEntityParameters();
            setParams.setName(cat);
            setParams.setParams(propList);
            msgs.add(setParams);
        }

        for (SetEntityParameters setEntityParameters : msgs) {
            if (!send(setEntityParameters, true))
                return false; // If one fails, rest is skipped, this helps to avoid iridium to be flooded all errors
        }
        return true;
    }

    private Map<String, String> getCategoriesOnPanel(boolean onlyVisible) {
        Map<String, String> categories = new LinkedHashMap<>();
        for (SystemProperty sp : params.values()) {
            String category = sp.getCategoryId();
            if (!categories.containsKey(category))
                categories.put(category, sp.getCategory());
        }

        if (onlyVisible) {
            List<String> visibleCategoriesI18n = new ArrayList<>();
            for (int i = 0; i < psp.getTable().getSheetModel().getRowCount(); i++) {
                Item o = (Item) psp.getTable().getSheetModel().getObject(i);
                if (o.hasToggle()) { // Is a category
                    if (!visibleCategoriesI18n.contains(o.getName())) {
                        visibleCategoriesI18n.add(o.getName()); //I18n name
                    }
                } else if (!o.hasToggle() && o.getParent() != null) {
                    if (!visibleCategoriesI18n.contains(o.getParent().getName())) {
                        visibleCategoriesI18n.add(o.getParent().getName()); //I18n name
                    }
                }
            }

            categories = categories.entrySet().stream().filter(e -> visibleCategoriesI18n.contains(e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        return categories;
    }

    private Future<List<String>>  askForCategories(String forWhat) {
        return askForCategories(forWhat, null);
    }

    private Future<List<String>> askForCategories(String forWhat, Map<String, String> previousCheckCategoriesOnPanel) {
        CompletableFuture<List<String>> future = new CompletableFuture<>();
        Map<String, String> categories = getCategoriesOnPanel(true);
        List<String> chosenCategories = new ArrayList<>(categories.keySet());
        List<JCheckBox> checkBoxes = new ArrayList<>();

        if ((previousCheckCategoriesOnPanel == null || previousCheckCategoriesOnPanel.isEmpty())
                && lastSelectedCategoriesList != null) {
            previousCheckCategoriesOnPanel = new LinkedHashMap<>();
            previousCheckCategoriesOnPanel.putAll(lastSelectedCategoriesList);
        }

        for (String category : chosenCategories.stream().sorted().collect(Collectors.toList())) {
            JCheckBox cb = new JCheckBox(categories.get(category));
            cb.addActionListener(e -> {
                if (((JCheckBox) e.getSource()).isSelected()) {
                    if (!chosenCategories.contains(category))
                        chosenCategories.add(category);
                } else {
                    chosenCategories.remove(category);
                }
            });
            cb.setSelected(true); // To set ticked;
            if (previousCheckCategoriesOnPanel != null && !previousCheckCategoriesOnPanel.containsKey(category)) {
                cb.setSelected(false);
            }
            // if selected add to checkCategories, else remove it
            if (cb.isSelected() && !chosenCategories.contains(category))
                chosenCategories.add(category);
            if (!cb.isSelected())
                chosenCategories.remove(category);
            checkBoxes.add(cb);
        }
        categoriesPanel.removeAll();
        // categoriesPanel.setLayout(new MigLayout("align center, fill, insets 0"));

        JLabel systemLabel = new JLabel("<html><b>" + createTitle() + "</b></html>");

        systemLabel.setFont(systemLabel.getFont().deriveFont(systemLabel.getFont().getStyle() | java.awt.Font.BOLD));
        JLabel label = new JLabel(I18n.textf("Select the categories to be used for >> %action", forWhat));
        categoriesPanel.add(systemLabel, "wrap");
        categoriesPanel.add(label, "wrap");

        JPanel checklistPanels = new JPanel(new MigLayout("fillx, wrap 2", "[left]rel[grow,fill]", "[]10[]"));
        JScrollPane scrollPane = new JScrollPane(checklistPanels);
        categoriesPanel.add(scrollPane, "w 100%, h 100%, wrap");
        for (JCheckBox cb : checkBoxes) {
            checklistPanels.add(cb, "");
        }

        JButton selectAllButton = new JButton(I18n.text("Sel All"));
        JButton selectNoneButton = new JButton(I18n.text("Sel None"));
        categoriesPanel.add(selectAllButton, "sg buttons, split");
        categoriesPanel.add(selectNoneButton, "sg buttons, split, gapafter 30");
        selectAllButton.addActionListener(e -> {
            checkBoxes.forEach(cb -> {
                if (!cb.isSelected()) {
                    String categoryLabel = cb.getText();
                    String category = categories.entrySet().stream()
                            .filter(ee -> ee.getValue().equals(categoryLabel))
                            .map(Map.Entry::getKey).findFirst().orElse(null);
                    if (!chosenCategories.contains(cb.getText()))
                        chosenCategories.add(category);
                    cb.setSelected(true);
                }
            });
        });
        selectNoneButton.addActionListener(e -> {
            chosenCategories.clear();
            checkBoxes.forEach(cb -> cb.setSelected(false));
        });

        JButton okButton = new JButton(I18n.text("Ok"));
        JButton cancelButton = new JButton(I18n.text("Cancel"));
        categoriesPanel.add(okButton, "gapbefore push, sg buttons, split");
        categoriesPanel.add(cancelButton, "sg buttons, split");
        okButton.addActionListener(e -> {
            future.complete(new ArrayList<>(chosenCategories));
            // SwingUtilities.getWindowAncestor(categoriesPanel).dispose();
            CardLayout card = (CardLayout) swapPropertiesAndCategoriesPanel.getLayout();
            card.show(swapPropertiesAndCategoriesPanel, CARD_PROPERTIES);
            revalidate();
            repaint();

            if (lastSelectedCategoriesList == null) {
                lastSelectedCategoriesList = new LinkedHashMap<>();
            } else {
                lastSelectedCategoriesList.clear();
            }
            chosenCategories.forEach(cat -> lastSelectedCategoriesList.putIfAbsent(cat, categories.get(cat)));
        });
        cancelButton.addActionListener(e -> {
            future.completeExceptionally(new RuntimeException("User cancelled"));
            // SwingUtilities.getWindowAncestor(categoriesPanel).dispose();
            CardLayout card = (CardLayout) swapPropertiesAndCategoriesPanel.getLayout();
            card.show(swapPropertiesAndCategoriesPanel, CARD_PROPERTIES);
            revalidate();
            repaint();
        });
        GuiUtils.reactEnterKeyPress(okButton);
        GuiUtils.reactEscapeKeyPress(cancelButton);

        CardLayout card = (CardLayout) swapPropertiesAndCategoriesPanel.getLayout();
        card.show(swapPropertiesAndCategoriesPanel, CARD_CATEGORIES);
        revalidate();
        repaint();
        return future;
    }

    private void showSimulatedVehicleConfirmation() {
        categoriesPanel.removeAll();
        categoriesPanel.setLayout(new MigLayout("fill, insets 10", "[grow,fill]", "[][grow][]"));

        JLabel titleLabel = new JLabel("<html><b>Simulated Vehicle Warning</b></html>");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        categoriesPanel.add(titleLabel, "wrap, gapbottom 10");

        JPanel centerPanel = new JPanel(new MigLayout("fill, insets 0", "[center]", "[center]"));
        JPanel messagePanel = new JPanel(new MigLayout("insets 10 30 10 30", "[center]", "[]")); // Fixed width column
        JLabel warningIcon = new JLabel(WARNING_ICON);
        JLabel message = new JLabel(
                I18n.text("<html><div style='width:400px; line-height:1.5;'>The vehicle is simulated.<br>Are you sure you want to proceed with synchronization?</div></html>")
        );

        messagePanel.add(warningIcon, "gapbefore 50, gapright 10");
        messagePanel.add(message, "w 400!, top");

        centerPanel.add(messagePanel, "center");
        categoriesPanel.add(centerPanel, "grow, pushy, wrap, gapbottom 10");

        JPanel buttonsPanel = new JPanel(new MigLayout("insets 0", "[grow][center][grow]", "[]"));
        JButton yesButton = new JButton(I18n.text("Yes"));
        JButton noButton = new JButton(I18n.text("No"));

        yesButton.addActionListener(ev -> {
            CardLayout card = (CardLayout) swapPropertiesAndCategoriesPanel.getLayout();
            card.show(swapPropertiesAndCategoriesPanel, CARD_PROPERTIES);
            doSyncParameters();
        });

        noButton.addActionListener(ev -> {
            CardLayout card = (CardLayout) swapPropertiesAndCategoriesPanel.getLayout();
            card.show(swapPropertiesAndCategoriesPanel, CARD_PROPERTIES);
        });

        buttonsPanel.add(new JLabel(), "growx, pushx");
        buttonsPanel.add(yesButton, "sg buttons, gapbefore 100");
        buttonsPanel.add(noButton, "sg buttons");
        buttonsPanel.add(new JLabel(), "growx, pushx");
        categoriesPanel.add(buttonsPanel, "south, dock south, hmin 50, hmax 50, gapbottom 0");

        CardLayout card = (CardLayout) swapPropertiesAndCategoriesPanel.getLayout();
        card.show(swapPropertiesAndCategoriesPanel, CARD_CATEGORIES);
        revalidate();
        repaint();
    }

    /**
     * This will send to the system the necessary SetEntityParameters messages with SystemProperty message(s)
     * that are needed. It will only send the SystemProperty messages that are locally dirty.
     */
    private void sendPropertiesToSystem() {
        List<String> validCategories = null;
        try {
            validCategories = isAskForCategories ? askForCategories("send", touchedCategoriesList).get() : null;
        }
        catch (Exception e) {
            return;
        }

        Set<SystemProperty> sentProps = new LinkedHashSet<>();
        ArrayList<SystemProperty> sysPropToSend = new ArrayList<>();
        for (SystemProperty sp : params.values()) {
            if (validCategories != null && !validCategories.contains(sp.getCategoryId()))
                continue; // Skip if not in the list of valid categories
            if (sp.getTimeDirty() > (fakeSyncButton.isSelected() ? sp.getTimeFakeSync() : sp.getTimeSync())) {
                // sendProperty(sp);
                sysPropToSend.add(sp);
                sentProps.add(sp);
            }
        }
        if (!sysPropToSend.isEmpty()) {
            boolean ret = sendProperty(sysPropToSend.toArray(new SystemProperty[0]));
            if (ret) {
                touchedCategoriesList.clear();
            }

            if (!ret) {
                return;
            }

            ArrayList<String> secNames = new ArrayList<>();
            for (SystemProperty sp : sentProps) {
                String sectionName = sp.getCategoryId();
                if (!secNames.contains(sectionName))
                    secNames.add(sectionName);
            }        
            for (String sec : secNames) {
                // TODO See if we want to ask back from Iridium, sending through Wifi
                if (!queryValues(sec, scopeToUse.getText(), visibility.getText(), true,
                        CommsAdmin.CommChannelType.WIFI.name)) {
                    break;
                }
            }
        }
    }

    private void savePropertiesToSystem() {
        List<String> validCategories;
        try {
            validCategories = isAskForCategories ? askForCategories("save").get() : null;
        }
        catch (Exception e) {
            return;
        }
        Collection<SystemProperty> propsInPanel = params.values();
        if (!propsInPanel.isEmpty()) {
            ArrayList<String> secNames = new ArrayList<>();
            for (SystemProperty sp : propsInPanel) {
                String sectionName = sp.getCategoryId();
                if (validCategories != null && !validCategories.contains(sectionName))
                    continue; // Skip if not in the list of valid categories
                if (!secNames.contains(sectionName))
                    secNames.add(sectionName);
            }
            boolean ret = true;
            for (String sec : secNames) {
                // TODO See if we want to ask back from Iridium
                ret = queryValues(sec, scopeToUse.getText(), visibility.getText(), true);
                if (!ret)
                    break;
            }

            if (ret) {
                for (String sec : secNames) {
                    if (!saveRequest(sec))
                        break;
                }
            }
        }
    }

    private boolean send(IMCMessage msg, boolean popGuiOnError, String... channelsToUse) { //}, boolean askApprovalOtherThanWifi) {
        MessageDeliveryListener mdl = new MessageDeliveryListener() {
            @Override
            public void deliveryUnreacheable(IMCMessage message) {
            }
            
            @Override
            public void deliveryUncertain(IMCMessage message, Object msg) {
            }
            
            @Override
            public void deliveryTimeOut(IMCMessage message) {
            }
            
            @Override
            public void deliverySuccess(IMCMessage message) {
            }
            
            @Override
            public void deliveryError(IMCMessage message, Object error) {
            }
        };
        if (sid == null)
            sid = ImcSystemsHolder.getSystemWithName(getSystemId());
        boolean sendReliably = sid != null;
        String system = sid != null ? sid.getName() : systemId;
//        if (sid != null) {
//            imcMsgManager.sendReliablyNonBlocking(msg, sid.getId(), mdl);
//        }
//        else {
//            imcMsgManager.sendMessageToSystem(msg, getSystemId());
//        }
        return IMCSendMessageUtils.sendMessage(msg, (sendReliably ? ImcMsgManager.TRANSPORT_TCP
                        : null), mdl, this, I18n.text("Error sending msg params"),
                false, "", true, true,
                true, popGuiOnError, channelsToUse, system);
    }
    
    public static void updatePropertyWithMessageArrived(SystemConfigurationEditorPanel systemConfEditor, IMCMessage message) {
        if (systemConfEditor == null || message == null || !(message instanceof EntityParameters))
            return;

        try {
            systemConfEditor.setRefreshing(true);
            EntityParameters eps = EntityParameters.clone(message);
            String section = eps.getName();
            for(EntityParameter ep : eps.getParams()) {
                SystemProperty p = systemConfEditor.getParams().get(section + "." + ep.getName());
                if(p == null) {
                    NeptusLog.pub().warn("Property not in config: " + section + " - " + ep.getName() + " from system with ID " + message.getSrc());
                }
                else {
                    boolean isList = false;
//                    NeptusLog.pub().info("<###>Prop type and if is list:: " + p.getType() + " " + (ArrayList.class.equals(p.getType())));
                    if (ArrayList.class.equals(p.getType()))
                        isList = true;
                    //Object value = ConfigurationManager.getValueTypedFromString(ep.getValue(), p.getValueType());
                    Object value = !isList ? ConfigurationManager.getValueTypedFromString(ep.getValue(),
                            p.getValueType()) : ConfigurationManager.getListValueTypedFromString(ep.getValue(),
                            p.getValueType());
                    p.setValue(value);
                    p.setTimeSync(System.currentTimeMillis());
                }
            }
            systemConfEditor.revalidate();
            systemConfEditor.repaint();
            systemConfEditor.setRefreshing(false);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getReqId() {
        return reqId;
    }

    private List<String> getExpectedCategories() {
        return expectedCategories;
    }

    public static void main(String[] args) {
//        ImcMsgManager.getManager().start();
//        MonitorIMCComms icmm = new MonitorIMCComms(ImcMsgManager.getManager());
//        GuiUtils.testFrame(icmm);
        GuiUtils.setLookAndFeel();
        String vehicle = "lauv-noptilus-1";
        
        GeneralPreferences.language = "en_US";
        
        //final SystemConfigurationEditorPanel sc1 = new SystemConfigurationEditorPanel(vehicle, Scope.MANEUVER,
              //  Visibility.USER, true, true, true, true, ImcMsgManager.getManager());
        //final SystemConfigurationEditorPanel sc2 = new SystemConfigurationEditorPanel(vehicle, Scope.MANEUVER,
               // Visibility.USER, true, false, true, true, ImcMsgManager.getManager());
        
//        ImcMsgManager.getManager().addListener(new MessageListener<MessageInfo, IMCMessage>() {
//            @Override
//            public void onMessage(MessageInfo info, IMCMessage msg) {
//                NeptusLog.pub().info("<###>---------");
//                SystemConfigurationEditorPanel.updatePropertyWithMessageArrived(sc1, msg);
//                SystemConfigurationEditorPanel.updatePropertyWithMessageArrived(sc2, msg);
//            }
//        }, vehicle, new MessageFilter<MessageInfo, IMCMessage>() {
//            @Override
//            public boolean isMessageToListen(MessageInfo info, IMCMessage msg) {
//                boolean ret = EntityParameter.ID_STATIC == msg.getMgid();
//                return ret;
//            }
//        });
        
     //   GuiUtils.testFrame(sc1);
      //  GuiUtils.testFrame(sc2);
    }
}
