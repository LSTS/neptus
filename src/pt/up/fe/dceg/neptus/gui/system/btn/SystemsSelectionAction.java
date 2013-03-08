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
 * 24/06/2011
 * $Id:: SystemsSelectionAction.java 9615 2012-12-30 23:08:28Z pdias            $:
 */
package pt.up.fe.dceg.neptus.gui.system.btn;

import java.awt.event.ActionEvent;
import java.util.Hashtable;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JButton;

import pt.up.fe.dceg.neptus.gui.system.img.MainIcon;
import pt.up.fe.dceg.neptus.gui.system.img.SelectionIcon;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.GuiUtils;

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
