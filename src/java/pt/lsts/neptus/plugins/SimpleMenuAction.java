/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * 2009/09/24
 */
package pt.lsts.neptus.plugins;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.ImageUtils;

/**
 * This class eases the creation of menu actions in the console
 * 
 * @author ZP
 * 
 */
public abstract class SimpleMenuAction extends ConsolePanel implements ActionListener {
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
     * see {@link ConsolePanel#init()}
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
