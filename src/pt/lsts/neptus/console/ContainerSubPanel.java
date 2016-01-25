/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 2009/09/16
 */
package pt.lsts.neptus.console;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.LockableSubPanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author ZP
 * @author Paulo Dias
 */
public class ContainerSubPanel extends ConsolePanel implements LockableSubPanel {

    private static final long serialVersionUID = 1L;
    @NeptusProperty(name = "Maximize Panel", description = "Use this to indicate that this panel "
            + "should be maximized on load. (Only works for top level panels.)", distribution = DistributionEnum.DEVELOPER)
    public boolean maximizePanel = true;
    
    protected List<ConsolePanel> panels = new ArrayList<>();

    public ContainerSubPanel(ConsoleLayout console) {
        super(console);
        setEditMode(getEditMode());
    }

    /**
     * @param maximize
     */
    protected void setMaximizePanel(boolean maximize) {
        if (maximize)
            getConsole().maximizePanel(this);
        else
            getConsole().minimizePanel(this);
    }

    @Override
    public void init() {
        for (ConsolePanel sp : panels) {
            sp.init();
        }
        if (maximizePanel)
            setMaximizePanel(true);
    }
    

    @Override
    public void setEditMode(boolean b) {
        super.setEditMode(b);
        for (ConsolePanel sp : panels) {
            try {
                sp.setEditMode(b);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addSubPanel(ConsolePanel panel) {
        panels.add(panel);
        this.add(panel);
    }

    public void removeSubPanel(ConsolePanel sp) {
        panels.remove(sp);
        this.remove(sp);
        doLayout();
        invalidate();
        revalidate();
    }

    private List<String> subPanelNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < panels.size(); i++) {
            int count = 0;
            for (int j = 0; j < i; j++) {
                if (panels.get(j).getName().equalsIgnoreCase(panels.get(i).getName()))
                    count++;
            }
            String name = count > 0 ? panels.get(i).getName() + "_" + count : panels.get(i).getName();
            names.add(name);
        }
        return names;
    }

    public String[] subPanelList() {
        return subPanelNames().toArray(new String[0]);
    }

    public void removeSubPanel(String subPanelName) {
        ConsolePanel sp = getSubPanelByName(subPanelName);
        sp.clean();
        if (sp != null)
            removeSubPanel(sp);

    }

    public ConsolePanel getSubPanelByName(String name) {
        List<String> names = subPanelNames();
        int index = names.indexOf(name);
        if (index != -1)
            return panels.get(index);
        return null;
    }

    @Override
    public void XML_ChildsWrite(Element e) {
        for (ConsolePanel sp : panels) {

            try {
                e.add(sp.asElement("child"));
            }
            catch (Exception ex) {
                Component parent = SwingUtilities.getWindowAncestor(this);
                if (parent == null)
                    parent = this;
                GuiUtils.errorMessage(parent, "Error saving XML of " + sp.getName(),
                        "[" + sp.getName() + "]: " + ex.getMessage());
                ex.printStackTrace();
            }

        }
    }

    @Override
    public void XML_ChildsRead(Element el) {
        List<?> list = el.selectNodes("*");
        for (Iterator<?> iter = list.iterator(); iter.hasNext();) {
            Element element = (Element) iter.next();
            ConsolePanel subpanel = null;
            // process childs 
            if ("child".equals(element.getName())) {
                Attribute attribute = element.attribute("class");
                try {
                    Class<?> clazz = Class.forName(attribute.getValue());
                    try {
                        subpanel = (ConsolePanel) clazz.getConstructor(ConsoleLayout.class).newInstance(getConsole());
                        addSubPanel(subpanel);
                        subpanel.inElement(element);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().error("creating subpanel new instance " + clazz.getName(), e);
                    }
                }
                catch (Exception e) {
                    NeptusLog.pub().error("Error parsing " + attribute.getValue(), e);
                }
            }
        }
    }
   
    @Override
    public void XML_PropertiesWrite(Element e) {
        String xml = PluginUtils.getConfigXml(this);
        try {
            Element el = DocumentHelper.parseText(xml).getRootElement();

            for (Object child : el.elements()) {
                Element aux = (Element) child;
                aux.detach();
                e.add(aux);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void clean() {
        super.clean();
        this.removeAll();
        for (ConsolePanel panel : this.panels) {
            try {
                panel.clean();
                NeptusLog.pub().info("Cleaned " + panel.getName() + " in " + ContainerSubPanel.this.getName());
            }
            catch (Exception e) {
                NeptusLog.pub().error("Error cleaning " + panel.getName() + " in " + ContainerSubPanel.this.getName() + " :: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean isLocked() {
        if (panels.size() == 0)
            return false;
        boolean ret = true;
        for (ConsolePanel subPanel : panels) {
            try {
                LockableSubPanel lsp = (LockableSubPanel) subPanel;
                ret = ret && lsp.isLocked();
                if (!ret)
                    break;
            }
            catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return ret;
    }

    @Override
    public void lock() {
        for (ConsolePanel subPanel : panels) {
            try {
                LockableSubPanel lsp = (LockableSubPanel) subPanel;
                lsp.lock();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void unLock() {
        for (ConsolePanel subPanel : panels) {
            try {
                LockableSubPanel lsp = (LockableSubPanel) subPanel;
                lsp.unLock();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @return the mySubPanels
     */
    public List<ConsolePanel> getSubPanels() {
        return panels;
    }
    
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

}