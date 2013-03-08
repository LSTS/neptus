/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Jan 10, 2013
 * $Id:: SettingsWindow.java 10030 2013-02-25 16:51:25Z mfaria                  $:
 */
package pt.up.fe.dceg.neptus.plugins.configWindow;

import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuItem;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;


/**
 * Window to encapsulate settings panel.<br>
 * Contains all the buttons.
 * 
 * @author Margarida Faria
 * 
 */
@Popup(icon = "images/menus/settings.png", name = "Console Settings", accelerator = KeyEvent.VK_F3, height = 400, width = 600)
public class SettingsWindow extends SimpleSubPanel {

    private static final long serialVersionUID = 1L;
    private FunctionalitiesSettings settingsPanel;
    private final Vector<PropertiesProvider> subPanels;
    private JCheckBox checkLvl;

    /**
     * Saves available plugins for later use in settings panel.
     * 
     * @param console
     * @param subPanels
     */
    public SettingsWindow(ConsoleLayout console, Vector<PropertiesProvider> subPanels) {
        super(console);
        this.subPanels = subPanels;
    }

    @Override
    protected JMenuItem createMenuItem(final POSITION popupPosition, String name2, ImageIcon icon) {
        JMenuItem menuItem = new JMenuItem(new AbstractAction(PluginUtils.i18nTranslate(name2),
                ImageUtils.getScaledIcon(icon, 16, 16)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                checkLvl.setSelected(false);
                settingsPanel.reset();
                setPopupPosition(popupPosition);
            }

        });

        return menuItem;
    }

    private void addButtons() {
        checkLvl = new JCheckBox(I18n.text("Advanced"));
        checkLvl.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                settingsPanel.updateForNewPermission();
            }
        });
        // direct keyboard inputs to tree after this is pressed
        checkLvl.setFocusable(false);

        JButton save = new JButton(I18n.text("Save"));
        save.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                settingsPanel.saveChanges();
                // checkLvl.setSelected(false);
                dialog.setVisible(false);
            }
        });

        JButton cancel = new JButton(I18n.text("Cancel"));
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // checkLvl.setSelected(false);
                dialog.setVisible(false);
            }
        });

        GuiUtils.reactEnterKeyPress(save);
        GuiUtils.reactEscapeKeyPress(cancel);

        JPanel buttonContainer = new JPanel();
        buttonContainer.setLayout(new MigLayout("rtl"));
        buttonContainer.add(cancel, "sg buttonOC");
        buttonContainer.add(save, "sg buttonOC");
        buttonContainer.add(checkLvl);
        this.add(buttonContainer, "grow");
    }

    @Override
    public void initSubPanel() {
        this.setLayout(new MigLayout("insets 0"));
        this.removeAll();
        this.settingsPanel = new FunctionalitiesSettings(ConfigFetch.getDistributionType().equals(
                DistributionEnum.CLIENT), subPanels);
        settingsPanel.reset();
        this.add(settingsPanel, "w 100%!, h 100%, wrap");
        addButtons();
        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
    }

    @Override
    public void cleanSubPanel() {
        settingsPanel = null;
    }
}
