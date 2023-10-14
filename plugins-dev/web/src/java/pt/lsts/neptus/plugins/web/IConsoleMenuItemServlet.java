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
 * Author: Paulo Dias
 * 28/06/2011
 */
package pt.lsts.neptus.plugins.web;

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

