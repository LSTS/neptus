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
 * 28/06/2011
 * $Id:: IConsoleMenuItemServlet.java 9615 2012-12-30 23:08:28Z pdias           $:
 */
package pt.up.fe.dceg.neptus.plugins.web;

import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;

/**
 * @author pdias
 *
 */
public interface IConsoleMenuItemServlet {
    public ConsoleMenuItem[] getConsoleMenuItems();
    public void informCreatedConsoleMenuItem(Hashtable<String, JMenuItem> consoleMenuItems);

    public static class ConsoleMenuItem {
        private String itemSubPath;
        private ImageIcon icon;
        private ActionListener actionListener;
        /**
         * @param itemSubPath This is the subpath. This will be prefixed 
         *          by "Tools>Web Server>" + servlet name + ">".
         * @param icon
         * @param actionListener
         */
        public ConsoleMenuItem(String itemSubPath, ImageIcon icon, ActionListener actionListener) {
            this.itemSubPath = itemSubPath;
            this.icon = icon;
            this.actionListener = actionListener;
        }
        /**
         * @return the itemSubPath
         */
        public String getItemSubPath() {
            return itemSubPath;
        }
        /**
         * @return the icon
         */
        public ImageIcon getIcon() {
            return icon;
        }
        /**
         * @return the actionListener
         */
        public ActionListener getActionListener() {
            return actionListener;
        }
    }
}

