/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2009/09/24
 */
package pt.up.fe.dceg.neptus.plugins.actions;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.CheckMenuChangeListener;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * This class eases the creation of menu actions in the console
 * 
 * @author ZP
 * 
 */
public abstract class SimpleMenuAction extends SimpleSubPanel implements ActionListener {
    private static final long serialVersionUID = 1L;
    protected AbstractAction action = null;
    protected boolean selectionState = false;

    private JCheckBoxMenuItem checkItem = null;
    private JMenuItem item = null;

    public SimpleMenuAction(ConsoleLayout console) {
        super(console);
        setVisibility(false);
        setSize(100, 100);
        setBackground(Color.yellow);
    }

    /**
     * This method may be overriden if a custom name and path is to be used.<br>
     * Path components are divided by ">". <br>
     * Example: <b>"Tools>Internet>Enabled"</b>
     * 
     * @return The path and name to be used in the menu
     */
    public String getMenuName() {
        return "Plugins>" + PluginUtils.getPluginName(getClass());
    }

    /**
     * By default, this method returns <b>false</b> (not selectable). <br>
     * Must return <b>true</b> to have a selectable menu.
     * 
     * @return boolean flag stating whether this menu has a check box.
     */
    public boolean isMenuSelectable() {
        return false;
    }

    /**
     * Checks the state of the menu selection.
     * 
     * @return <b>false</b> if the menu is not selectable or is in an not selected state.
     */
    protected boolean isMenuSelected() {
        return selectionState;
    }

    /**
     * In a selectable menu, changes the current state to the given value.<br>
     * <i>This method prints an error in case {@link #isMenuSelectable()} returns <b>false</b>
     * 
     * @param selected Whether the menu is to be selected.
     */
    protected void setMenuSelected(boolean selected) {
        if (checkItem != null)
            checkItem.setSelected(selected);
        else {
            NeptusLog.pub().warn("The menu '" + getMenuName() + "' does not support selection states");
        }
    }

    /**
     * Enables or disables the associated menu item
     * 
     * @param enabled The enabled state of the associated menu item
     */
    protected void setMenuEnabled(boolean enabled) {
        item.setEnabled(enabled);
    }

    /**
     * @return Current enabled state of the associated menu item
     */
    protected boolean isMenuEnabled() {
        return item.isEnabled();
    }

    /**
     * see {@link SimpleSubPanel#init()}
     */
    @Override
    public void initSubPanel() {
        if (isMenuSelectable()) {
            item = checkItem = addCheckMenuItem(getMenuName(),
                    ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new CheckMenuChangeListener() {
                        @Override
                        public void menuChecked(ActionEvent e) {
                            selectionState = true;
                            actionPerformed(e);
                        }

                        @Override
                        public void menuUnchecked(ActionEvent e) {
                            selectionState = false;
                            actionPerformed(e);
                        }
                    });
        }
        else {
            item = addMenuItem(getMenuName(), ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), this);
        }
    }

    public static void main(String[] args) {
        ConsoleParse.testSubPanel(SimpleMenuAction.class);
    }
}
