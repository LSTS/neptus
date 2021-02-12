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
 * Created in 4/Out/2005
 */
package pt.lsts.neptus.gui.checklist;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedList;

import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

/**
 * @author Paulo Dias
 *
 */
class MyJTaskPaneGroup
extends JXTaskPane
implements PropertyChangeListener
{
    private static final long serialVersionUID = -770514691508885176L;

    public static final String CHILD_ITEM_CHECKED_PROPERTY = "child item checked change";

    public static final String INFO_SEPARATOR = "  \u00A0 ("; // \u00A0 No-Break Space or \u00B7 Middle Point

    //Com isto consigo chegar aos CheckItemPanels:
    //this.getContentPane().getComponents()[0].getClass());
    //assim posso passar sem a linked list
    public LinkedList<Component> listCheckItemPanel = new LinkedList<Component>();

    /**
     * 
     */
    public MyJTaskPaneGroup()
    {
        super();
        this.addPropertyChangeListener(this);
    }

    public String getGroupName() {
        String ret = getTitle();
        int index = ret.lastIndexOf(INFO_SEPARATOR);
        if (index != -1) {
            ret = ret.substring(0, index);
        }	
        return ret;
    }

    public void setGroupName(String groupName) {
        this.setTitle(groupName);
        fixTitle();
    }

    private void fixTitle() {
        int[] ret = computeItemsTotalAndChecked();
        setTitle(getGroupName() + INFO_SEPARATOR +
                ret[1] + "/" + ret[0] + ")");
    }

    /* (non-Javadoc)
     * @see java.awt.Container#add(java.awt.Component)
     */
    @Override
    public Component add(Component arg0)
    {
        listCheckItemPanel.add(arg0);
        Component addComp = super.add(arg0);
        fixIcon();
        return addComp;
    }


    /* (non-Javadoc)
     * @see java.awt.Container#add(java.awt.Component, int)
     */
    @Override
    public Component add(Component arg0, int arg1)
    {
        //System.err.println("Size Remove before: " + listCheckItemPanel.size() + " " + arg1);
        listCheckItemPanel.add(arg1, arg0);
        Component addComp = super.add(arg0, arg1);
        fixIcon();
        return addComp;
    }


    /* (non-Javadoc)
     * @see com.l2fprod.common.swing.JTaskPaneGroup#remove(java.awt.Component)
     */
    @Override
    public void remove(Component arg0)
    {
        //System.err.println("Size Remove before: " + listCheckItemPanel.size());
        listCheckItemPanel.remove(arg0);
        //System.err.println("Size Remove after: " + listCheckItemPanel.size());
        super.remove(arg0);
        fixIcon();
    }


    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object arg0) {
        if (arg0 instanceof MyJTaskPaneGroup) {
            MyJTaskPaneGroup comp = (MyJTaskPaneGroup) arg0;
            //if (this.getTitle().equals(comp.getTitle()))
            if (this.getGroupName().equals(comp.getGroupName()))
                return true;
            else
                return false;
        }
        else
            return false;
    }


    /* (non-Javadoc)
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent e)
    {
        String prop = e.getPropertyName();
        //System.err.println("........." + prop);
        if (prop.equals(ChecklistPanel.DIRTY_PROPERTY))
        {
            boolean newValue = ((Boolean) e.getNewValue())
                    .booleanValue();
            //NeptusLog.pub().info("> " + ChecklistPanel.CHANGED_PROPERTY
            //        + ": " + newValue + " > " + this);
            ((JXTaskPaneContainer) this.getParent()).firePropertyChange(
                    ChecklistPanel.DIRTY_PROPERTY, !newValue, newValue);
        }
        else if (prop.equals(CHILD_ITEM_CHECKED_PROPERTY))
        {
            boolean newValue = ((Boolean) e.getNewValue()).booleanValue();
            //NeptusLog.pub().info("> " + CHILD_ITEM_CHECKED_PROPERTY
            //        + ": " + newValue + " > " + this);
            if (newValue)
            {
                fixIcon();
            }
            else
            {
                this.setIcon(ChecklistPanel.NOT_OK_IMAGE_ICON);
                fixTitle();
                this.revalidate();
                this.repaint();
            }
        }
    }


    /**
     * Calculates {@link #computeItemsLogicalAND()} and fixs the icon accordently.
     */
    public void fixIcon()
    {
        boolean result = computeItemsLogicalAND();
        if (result)
            this.setIcon(ChecklistPanel.OK_IMAGE_ICON);
        else
            this.setIcon(ChecklistPanel.NOT_OK_IMAGE_ICON);
        fixTitle();
        this.revalidate();
        this.repaint();
    }

    /**
     * @return The logical AND with the checked of all child CheckItems.
     */
    public boolean computeItemsLogicalAND()
    {
        Component[] comps = this.getContentPane().getComponents();
        boolean logAnd = true;
        for (Component cp : comps)
        {
            CheckItemPanel cip = (CheckItemPanel) cp; 
            logAnd &= cip.isChecked();
            if (!logAnd)
                break;
        }
        return logAnd;
    }

    public int[] computeItemsTotalAndChecked()
    {
        Component[] comps = this.getContentPane().getComponents();
        int count = 0, checked = 0;
        for (Component cp : comps)
        {
            CheckItemPanel cip = (CheckItemPanel) cp; 
            count++;
            if (cip.isChecked())
                checked++;
        }
        return new int[] {count, checked};
    }

    /* (non-Javadoc)
     * @see java.awt.Container#getComponentCount()
     */
    /*
    public int getComponentCount()
    {
        return this.getContentPane().getComponentCount();
    }
     */
}
