/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 5/10/2011
 * $Id:: HideMenusListener.java 9615 2012-12-30 23:08:28Z pdias                 $:
 */
package pt.up.fe.dceg.neptus.gui;

import java.awt.Component;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import pt.up.fe.dceg.neptus.events.NeptusEventHiddenMenus;
import pt.up.fe.dceg.neptus.events.NeptusEvents;

import com.google.common.eventbus.Subscribe;

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
