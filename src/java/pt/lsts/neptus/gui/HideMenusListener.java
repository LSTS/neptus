/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 5/10/2011
 */
package pt.lsts.neptus.gui;

import java.awt.Component;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import com.google.common.eventbus.Subscribe;

import pt.lsts.neptus.events.NeptusEventHiddenMenus;
import pt.lsts.neptus.events.NeptusEvents;

/**
 * @author pdias
 * 
 */
public class HideMenusListener implements MenuListener {
    private Component[] components = new Component[0];
    private JMenuItem[] menuItems = new JMenuItem[0];
    private boolean show = false;
    private JMenu menu = null;

    public static HideMenusListener forge(Component[] components, JMenuItem[] menuItems){
        HideMenusListener instance = new HideMenusListener(components, menuItems);
        NeptusEvents.register(instance);
        return instance;
    }
    public static HideMenusListener forge(JMenuItem[] menuItems){
        HideMenusListener instance = new HideMenusListener(new Component[0], menuItems);
        NeptusEvents.register(instance);
        return instance;
    }

    public HideMenusListener(Component[] components, JMenuItem[] menuItems) {
        this.components = components;
        this.menuItems = menuItems;
        for (Component comp : components) {
            comp.setVisible(false);
        }
        for (JMenuItem mitem : menuItems) {
            mitem.setVisible(false);
        }
    }
    
    @Subscribe
    public void handleNeptusEventHiddenMenus(NeptusEventHiddenMenus e){
        this.show = !show;
        for (Component comp : components) {
            comp.setVisible(show);
        }
        for (JMenuItem mitem : menuItems) {
            mitem.setVisible(show);
        }
        
        menu.revalidate();
        menu.repaint();
    }
    
    public void menuSelected(MenuEvent e) {
        menu = (JMenu) e.getSource();
    }

    public void menuDeselected(MenuEvent e) {
        menu = (JMenu) e.getSource();
    }

    public void menuCanceled(MenuEvent e) {
        menu = (JMenu) e.getSource();
    }
}
