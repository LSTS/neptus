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
 * Author: Hugo Dias
 * 14 de Mai de 2013
 */
package pt.lsts.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.plaf.basic.BasicButtonUI;

import org.apache.commons.lang3.text.WordUtils;
import org.jdesktop.swingx.JXBusyLabel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.AbstractConsolePlugin;
import pt.lsts.neptus.console.ConsoleInteraction;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.ContainerSubPanel;
import pt.lsts.neptus.console.IConsoleInteraction;
import pt.lsts.neptus.console.IConsoleLayer;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent.SubPanelChangeAction;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Hugo Dias
 * @author pdias
 * 
 */
@Popup(name = "Plugin Manager", icon = "images/buttons/events.png", pos = POSITION.CENTER, width = 500, height = 500, accelerator = 'P')
// @PluginDescription(name = "Plugin Manager", icon = "images/buttons/events.png")
public class PluginManager extends ConsolePanel {

    private static final long serialVersionUID = 1L;
    private JPanel content;
    @SuppressWarnings("unused")
    private JPanel statusPanel;
    private JPanel info;
    private JLabel name = new JLabel();
    private JLabel type = new JLabel();
    private JTextArea description = new JTextArea();
    private JList<String> activePluginsList;
    private JList<String> availablePluginsList;
    private Map<String, Object> pluginsMap = new LinkedHashMap<String, Object>();
    private Map<String, Class<?>> plugins = new LinkedHashMap<String, Class<?>>();
    private String activeSelected = null;
    private String availableSelected = null;
    private ContainerSubPanel container;
    private JButton btnAdd;
    private JButton btnRemove;
    private JButton btnSettings;
    private JButton btnExit;
    private JXBusyLabel progress;

    private KeyListener keyboardListener;
    
    private SettingsWindow settingsWindow = null;

    /**
     * Constructor
     * 
     * @param console
     */
    public PluginManager(ConsoleLayout console) {
        super(console);
        this.setLayout(new BorderLayout());
        // Content Panel
        this.add(content = new JPanel(new MigLayout("gap 5px 0, ins 5px")), BorderLayout.CENTER);

        // Add components to the content panel
        this.createComponents();
        // /this.refreshActivePlugins();
        this.createActions();
        this.createListeners();
    }

    /**
     * Create components
     */
    private void createComponents() {
        JPanel activePluginsPanel = new JPanel(new MigLayout("ins 0"));
        JPanel availablePluginsPanel = new JPanel(new MigLayout("ins 0"));
        info = new JPanel(new MigLayout("gap 5px 0, ins 1px"));
        info.setBorder(BorderFactory.createTitledBorder(I18n.text("Info")));
        info.add(new JLabel("<html><b>" + I18n.text("Name") + ":"));
        info.add(name, "span, wrap");
        info.add(new JLabel("<html><b>" + I18n.text("Type") + ":"));
        info.add(type, "span, wrap");
        JScrollPane descriptionScrollPane = new JScrollPane();
        description.setEditable(false);
        descriptionScrollPane.setViewportView(description);
        descriptionScrollPane.setBorder(null);
        info.add(descriptionScrollPane, "h 100%, span");
        JPanel okPanel = new JPanel(new MigLayout("ins 0", "push[]", ""));

        content.add(availablePluginsPanel, "h 100%, w 50%");
        content.add(activePluginsPanel, "h 100%, w 50%, wrap");
        content.add(info, "w 100%, h 250px, span, wrap");
        content.add(okPanel, "gaptop 5, gapbottom 5, gapright 5, w 100%, h 60px, span");

        // availablePluginsPanel components
        // label
        JLabel availablePluginsLabel = new JLabel("<html><b>" + I18n.text("Available"));
        availablePluginsPanel.add(availablePluginsLabel, "h 7%");
        // buttons
        btnAdd = new JButton();
        btnAdd.setUI(new BasicButtonUI());
        availablePluginsPanel.add(btnAdd, "w 80px::, h 7%, wrap");

        // list
        JScrollPane listScroll = new JScrollPane();
        availablePluginsList = new JList<String>();
        availablePluginsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        availablePluginsList.setListData(this.getAvailablePlugins());

        listScroll.setViewportView(availablePluginsList);
        availablePluginsPanel.add(listScroll, "h 93%, w 100%, span");

        // activePluginsPanel components
        // label
        JLabel activePluginsLabel = new JLabel("<html><b>" + I18n.text("Active"));
        activePluginsPanel.add(activePluginsLabel, "h 7%");
        // buttons
        btnRemove = new JButton();
        btnRemove.setUI(new BasicButtonUI());
        activePluginsPanel.add(btnRemove, "w 80px::, h 7%");

        btnSettings = new JButton();
        btnSettings.setUI(new BasicButtonUI());
        activePluginsPanel.add(btnSettings, "w 80px::, h 7%, wrap");

        // list
        JScrollPane activePluginsScrollPane = new JScrollPane();
        activePluginsList = new JList<String>();
        activePluginsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        activePluginsScrollPane.setViewportView(activePluginsList);
        activePluginsPanel.add(activePluginsScrollPane, "h 93%, w 100%, span");
        
        btnExit = new JButton();
        progress =  InfiniteProgressPanel.createBusyAnimationInfiniteBeans(20);
        progress.setVisible(false);
        okPanel.add(progress, "");
        okPanel.add(btnExit, "w 80px::");

        // Key bindings
        GuiUtils.reactKeyPress(btnAdd, KeyEvent.VK_ADD);
        GuiUtils.reactKeyPress(btnAdd, KeyEvent.VK_PLUS);

        GuiUtils.reactKeyPress(btnRemove, KeyEvent.VK_SUBTRACT);
        GuiUtils.reactKeyPress(btnRemove, KeyEvent.VK_MINUS);
        
        GuiUtils.reactKeyPress(btnSettings, KeyEvent.VK_PERIOD);
        GuiUtils.reactKeyPress(btnSettings, KeyEvent.VK_MULTIPLY);
        GuiUtils.reactKeyPress(btnSettings, KeyEvent.VK_ASTERISK);
        
        GuiUtils.reactEscapeKeyPress(btnExit);
    }

    private void createActions() {
        // Add button action
        btnAdd.setAction(new AbstractAction(I18n.text("Add")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (availableSelected == null)
                    return;
                
                progress.setBusy(true);
                progress.setVisible(true);
                enableButtons(false);
                
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        Class<?> clazz = plugins.get(availableSelected);
                        if (clazz == null) {
                            NeptusLog.pub().error("Plugin \"" + activeSelected
                                    + "\" is not properly loaded as plugin!");
                            return null;
                        }

                        if (container == null && ContainerSubPanel.class.isAssignableFrom(clazz)) {
                            ConsolePanel sp = PluginsRepository.getPanelPlugin(availableSelected, getConsole());
                            if (sp != null) {
                                getConsole().getMainPanel().addSubPanel(sp);
                                container = (ContainerSubPanel) sp;
                                sp.init();
                                getConsole().informSubPanelListener(sp, SubPanelChangeAction.ADDED);
                                refreshActivePlugins();
                                warnSettingsWindowAdd(sp);
                                getConsole().setConsoleChanged(true);
                            }
                            
                            return null;
                        }

                        if (container != null && ContainerSubPanel.class.isAssignableFrom(clazz)) {
                            return null;
                        }
                        
                        if (ConsolePanel.class.isAssignableFrom(clazz)) {
                            if (container != null) {
                                ConsolePanel sp = PluginsRepository.getPanelPlugin(availableSelected, getConsole());
                                container.addSubPanel(sp);
                                sp.init();
                                getConsole().informSubPanelListener(sp, SubPanelChangeAction.ADDED);
                                refreshActivePlugins();
                                warnSettingsWindowAdd(sp);
                                NeptusLog.pub().warn(
                                        "Added new console panel: " + sp.getName() + " Class name : "
                                                + sp.getClass().getCanonicalName());
                                getConsole().setConsoleChanged(true);
                            }
                        }

                        if (ConsoleLayer.class.isAssignableFrom(clazz)) {
                            ConsoleLayer sp = PluginsRepository.getConsoleLayer(availableSelected);
                            getConsole().addMapLayer(sp);
                            refreshActivePlugins();
                            warnSettingsWindowAdd(sp);
                            NeptusLog.pub().warn(
                                    "Added new console layer: " + sp.getName() + " Class name : "
                                            + sp.getClass().getCanonicalName());
                            getConsole().setConsoleChanged(true);
                        }

                        if (ConsoleInteraction.class.isAssignableFrom(clazz)) {
                            ConsoleInteraction sp = PluginsRepository.getConsoleInteraction(availableSelected);
                            getConsole().addInteraction(sp);
                            refreshActivePlugins();
                            warnSettingsWindowAdd(sp);
                            NeptusLog.pub().warn(
                                    "Added new console interaction: " + sp.getName() + " Class name : "
                                            + sp.getClass().getCanonicalName());
                            getConsole().setConsoleChanged(true);
                        }
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().warn(e.getMessage());;
                        }
                        progress.setBusy(false);
                        progress.setVisible(false);
                        enableButtons(true);
                    }
                };
                sw.execute();;
            }
        });

        // Remove button action
        btnRemove.setAction(new AbstractAction(I18n.text("Remove")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (activeSelected == null)
                    return;

                progress.setBusy(true);
                progress.setVisible(true);
                enableButtons(false);
                
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        String activeSelectedFixed = activeSelected.replaceAll("_\\d*$", "");
                        Class<?> clazz = plugins.get(activeSelectedFixed);
                        if (clazz == null) {
                            NeptusLog.pub().error("Plugin \"" + activeSelectedFixed
                                    + "\" is not properly loaded as plugin!");
                            return null;
                        }
                        
                        if (ConsolePanel.class.isAssignableFrom(clazz)) {
                            ConsolePanel sp = (ConsolePanel) pluginsMap.get(activeSelected);
                            if (container != null && container.getSubPanelsCount() == 0 
                                    &&  sp == container) {
                                getConsole().getMainPanel().removeSubPanel(container);
                                container = null;
                            }
                            else {
                                if (container != null)
                                    container.removeSubPanel(activeSelected);
                                else // Try to remove from console
                                    getConsole().removeSubPanel(sp);
                            }
                            getConsole().informSubPanelListener(sp, SubPanelChangeAction.REMOVED);
                            refreshActivePlugins();
                            warnSettingsWindowRemove(sp);
                            NeptusLog.pub().warn(
                                    "Removed console panel: " + sp.getName() + " Class name : "
                                            + sp.getClass().getCanonicalName());
                            getConsole().setConsoleChanged(true);
                        }

                        if (ConsoleLayer.class.isAssignableFrom(clazz)) {
                            ConsoleLayer sp = (ConsoleLayer) pluginsMap.get(activeSelected);
                            getConsole().removeMapLayer(sp);
                            refreshActivePlugins();
                            warnSettingsWindowRemove(sp);
                            NeptusLog.pub().warn(
                                    "Removed layer: " + sp.getName() + " Class name : " + sp.getClass().getCanonicalName());
                            getConsole().setConsoleChanged(true);
                        }

                        if (ConsoleInteraction.class.isAssignableFrom(clazz)) {
                            ConsoleInteraction sp = (ConsoleInteraction) pluginsMap.get(activeSelected);
                            getConsole().removeInteraction(sp);
                            refreshActivePlugins();
                            warnSettingsWindowRemove(sp);
                            NeptusLog.pub().warn(
                                    "Removed console interaction: " + sp.getName() + " Class name : "
                                            + sp.getClass().getCanonicalName());
                            getConsole().setConsoleChanged(true);
                        }

                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().warn(e.getMessage());;
                        }
                        progress.setBusy(false);
                        progress.setVisible(false);
                        enableButtons(true);
                    }
                };
                sw.execute();;
            }
        });
        btnRemove.getAction().putValue(Action.SHORT_DESCRIPTION, I18n.text("Remove selected plugin"));

        btnSettings.setAction(new AbstractAction(I18n.text("Settings")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (activeSelected == null)
                    return;
                if (pluginsMap.get(activeSelected) instanceof PropertiesProvider) {
                    PropertiesEditor.editProperties((PropertiesProvider) pluginsMap.get(activeSelected),
                            SwingUtilities.getWindowAncestor(PluginManager.this), true);
                    getConsole().setConsoleChanged(true);
                }
            }
        });
        btnSettings.getAction().putValue(Action.SHORT_DESCRIPTION,
                I18n.text("Open settings dialog for the selected plugin"));

        btnExit.setAction(new AbstractAction(I18n.text("Exit")) {
            private static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                Window sa = SwingUtilities.getWindowAncestor(PluginManager.this);
                if (sa != null)
                    sa.setVisible(false);
            }
        });

    }

    private void enableButtons(boolean b) {
        btnAdd.setEnabled(b);
        btnRemove.setEnabled(b);
        btnSettings.setEnabled(b);
    }

    private void createListeners() {
        activePluginsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = activePluginsList.locationToIndex(e.getPoint());
                    if (index > -1) {
                        activeSelected = (String) activePluginsList.getSelectedValue();
                        if (pluginsMap.get(activeSelected) instanceof PropertiesProvider) {
                            PropertiesEditor.editProperties((PropertiesProvider) pluginsMap.get(activeSelected),
                                    SwingUtilities.getWindowAncestor(PluginManager.this), true);
                        }
                    }
                }
                if (e.getClickCount() == 1) {
                    int index = activePluginsList.locationToIndex(e.getPoint());
                    if (index > -1) {
                        activeSelected = (String) activePluginsList.getSelectedValue();
                        String activeSelectedFixed = activeSelected.replaceAll("_\\d*$", "");
                        Class<?> clazz = plugins.get(activeSelectedFixed);
                        if (clazz == null) {
                            NeptusLog.pub().error("Plugin \"" + activeSelectedFixed
                                    + "\" is not properly loaded as plugin!");
                        }

                        updateDescriptionTextInGui(clazz);
                    }
                }
            }
        });
        availablePluginsList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    int index = availablePluginsList.locationToIndex(e.getPoint());
                    if (index > -1) {
                        String pluginName = (String) availablePluginsList.getSelectedValue();
                        availableSelected = pluginName;
                        String pluginNameFixed = pluginName.replaceAll("_\\d*$", "");
                        Class<?> clazz = plugins.get(pluginNameFixed);
                        if (clazz == null) {
                            NeptusLog.pub().error("Plugin \"" + pluginNameFixed
                                    + "\" is not properly loaded as plugin!");
                        }

                        updateDescriptionTextInGui(clazz);
                    }
                }
            }
        });

        keyboardListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }

            @Override
            public void keyReleased(KeyEvent e) {
                String pluginName;
                if ((JList<?>) e.getSource() == availablePluginsList) {
                    pluginName = (String) availablePluginsList.getSelectedValue();
                    availableSelected = pluginName;
                }
                else {
                    pluginName = (String) activePluginsList.getSelectedValue();
                    activeSelected = pluginName;
                }

                String pluginNameFixed = pluginName.replaceAll("_\\d*$", "");
                Class<?> clazz = plugins.get(pluginNameFixed);
                if (clazz == null) {
                    NeptusLog.pub().error("Plugin \"" + pluginNameFixed
                            + "\" is not properly loaded as plugin!");
                }

                updateDescriptionTextInGui(clazz);
            }
        };
        availablePluginsList.addKeyListener(keyboardListener);
        activePluginsList.addKeyListener(keyboardListener);
    }

    private void updateDescriptionTextInGui(Class<?> clazz) {
        if (clazz == null) {
            name.setText("");
            type.setText("");
            description.setText("");
        }
        else {
            name.setText(PluginUtils.getPluginName(clazz));
            boolean experimental = PluginUtils.isPluginExperimental(clazz);
            type.setText(getType(clazz) + (experimental ? " (" + I18n.text("experimental") + ")" : ""));
            description.setText(WordUtils.wrap(PluginUtils.getPluginDescription(clazz), 72));
            description.setCaretPosition(0);
        }
    }

    private String getType(Class<?> clazz) {
        String type = I18n.text("N/A");
        if (clazz != null) {
            if (ConsolePanel.class.isAssignableFrom(clazz))
                type = I18n.text("Panel");
            else if (ConsoleLayer.class.isAssignableFrom(clazz))
                type = I18n.text("Layer");
            else if (ConsoleInteraction.class.isAssignableFrom(clazz))
                type = I18n.text("Interaction");
        }
        return type;
    }

    private String[] getAvailablePlugins() {
        plugins.putAll(PluginsRepository.getPanelPlugins());
        plugins.putAll(PluginsRepository.getConsoleInteractions());
        plugins.putAll(PluginsRepository.getConsoleLayerPlugins());
        String[] pluginNames = plugins.keySet().toArray(new String[0]);
        // Natural sort
        Arrays.sort(pluginNames, new Comparator<String>() {
            private Collator collator = Collator.getInstance(Locale.US);
            @Override
            public int compare(String o1, String o2) {
                // return collator.compare(PluginUtils.i18nTranslate(o1), PluginUtils.i18nTranslate(o2));
                return collator.compare(o1, o2);
            }
        });

        return pluginNames;
    }

    public void reset() {
        refreshActivePlugins();
    }
    
    private void refreshActivePlugins() {
        pluginsMap.clear();
        List<String> names = new ArrayList<>();
        for (ConsolePanel panel : getConsole().getSubPanels()) {
            names.add(panel.getName());
            pluginsMap.put(panel.getName(), panel);
            if (panel instanceof ContainerSubPanel) {
                container = (ContainerSubPanel) panel;
                
                String[] panelsNames = ((ContainerSubPanel) panel).subPanelList();
                Arrays.sort(panelsNames, new Comparator<String>() {
                    private Collator collator = Collator.getInstance(Locale.US);
                    @Override
                    public int compare(String o1, String o2) {
                        return collator.compare(o1, o2);
                    }
                });
                
                for (String name : panelsNames) {
                    ConsolePanel sp = container.getSubPanelByName(name);
                    names.add(name);
                    pluginsMap.put(name, sp);
                }
            }
        }

        for (IConsoleLayer layer : getConsole().getLayers()) {
            names.add(layer.getName());
            pluginsMap.put(layer.getName(), layer);
        }

        for (IConsoleInteraction interaction : getConsole().getInteractions()) {
            names.add(interaction.getName());
            pluginsMap.put(interaction.getName(), interaction);
        }

        String firstPanel = names.isEmpty() ? null : names.get(0);
        // Natural sort
        Collections.sort(names, new Comparator<String>() {
            private Collator collator = Collator.getInstance(Locale.US);
            @Override
            public int compare(String o1, String o2) {
                if (firstPanel != null) {
                    if (firstPanel.equals(o1))
                        return -1;
                    if (firstPanel.equals(o2))
                        return 1;
                }
                
                return collator.compare(o1, o2);
            }
        });

        activePluginsList.setListData(names.toArray(new String[0]));
    }

    @Override
    public void initSubPanel() {
        this.refreshActivePlugins();
    }

    @Override
    public void cleanSubPanel() {
    }
    
    /**
     * @param settingsWindow the settingsWindow to set
     */
    public void setSettingsWindow(SettingsWindow settingsWindow) {
        this.settingsWindow = settingsWindow;
    }
    
    private void warnSettingsWindowAdd(ConsolePanel sp) {
        if(this.settingsWindow != null && sp instanceof PropertiesProvider)
            this.settingsWindow.addPropertiesProvider((PropertiesProvider) sp);
    }

    private void warnSettingsWindowAdd(AbstractConsolePlugin sp) {
        if(this.settingsWindow != null && sp instanceof PropertiesProvider)
            this.settingsWindow.addPropertiesProvider((PropertiesProvider) sp);
    }

    private void warnSettingsWindowRemove(ConsolePanel sp) {
        if(this.settingsWindow != null && sp instanceof PropertiesProvider)
            this.settingsWindow.removePropertiesProvider((PropertiesProvider) sp);
    }

    private void warnSettingsWindowRemove(AbstractConsolePlugin sp) {
        if(this.settingsWindow != null && sp instanceof PropertiesProvider)
            this.settingsWindow.removePropertiesProvider((PropertiesProvider) sp);
    }
}
