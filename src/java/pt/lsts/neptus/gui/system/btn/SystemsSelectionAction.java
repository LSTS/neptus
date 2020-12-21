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
 * 24/06/2011
 */
package pt.lsts.neptus.gui.system.btn;

import java.awt.event.ActionEvent;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;

import pt.lsts.neptus.gui.system.img.MainIcon;
import pt.lsts.neptus.gui.system.img.SelectionIcon;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class SystemsSelectionAction extends AbstractAction {

    public static enum SelectionType {
        MAIN(I18n.text("Main")), SELECTION(I18n.text("Selection"));
        private String type = "Main"; 
        private SelectionType(String type) {
            this.type = type;
        }
        public String getType() {
            return type;
        }
        public static SelectionType getSelectionTypeFromString(String type) {
            for (SelectionType elem : SelectionType.values()) {
                if (elem.getType().equalsIgnoreCase(type))
                    return elem;
            }
            return SelectionType.MAIN;
        }
        public static SelectionType getNextSelectionTypeFromString(String type) {
            boolean returnNext = false;
            for (SelectionType elem : SelectionType.values()) {
                if (returnNext)
                    return elem;
                if (elem.getType().equalsIgnoreCase(type))
                    returnNext = true;
            }
            return SelectionType.MAIN;
        }
    };
    
    private String tooltipPrefix = "";
    private Hashtable<SelectionType, Icon> icons = new Hashtable<SystemsSelectionAction.SelectionType, Icon>();
    private SelectionType current = SelectionType.MAIN;

    /**
     * @param tooltipPrefix
     * @param iconSize
     */
    public SystemsSelectionAction(String tooltipPrefix, int iconSize) {
        this.tooltipPrefix = tooltipPrefix
                + (tooltipPrefix.length() == 0 || tooltipPrefix.endsWith(" ") ? "" : " ");
        if (iconSize < 10)
            iconSize = 10;
        icons.put(SelectionType.MAIN, new MainIcon(iconSize));
        icons.put(SelectionType.SELECTION, new SelectionIcon(iconSize));
        
        updateActionAndIcon();
    }

    /**
     * @param tooltipPrefix
     */
    public SystemsSelectionAction(String tooltipPrefix) {
        this(tooltipPrefix, 32);
    }

    /**
     * @param iconSize
     */
    public SystemsSelectionAction(int iconSize) {
        this("", iconSize);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        current = SelectionType.getNextSelectionTypeFromString(current.getType());
        updateActionAndIcon();
    }

    /**
     * 
     */
    private void updateActionAndIcon() {
        putValue(AbstractAction.SMALL_ICON, icons.get(current));
        putValue(AbstractAction.SHORT_DESCRIPTION, tooltipPrefix + current.getType());
    }

    /**
     * @return 
     * 
     */
    public SelectionType getSelectionType() {
        return current;
    }
    
    /**
     * @param ev
     * @return
     */
    public static boolean getClearSelectionOption(final ActionEvent ev) {
        return ((ev.getModifiers() & ActionEvent.CTRL_MASK) == ActionEvent.CTRL_MASK);
    }

    public static void main(String[] args) {
        SystemsSelectionAction action = new SystemsSelectionAction(100);
        JButton but = new JButton(action);
        
        GuiUtils.testFrame(but);
    }
}
