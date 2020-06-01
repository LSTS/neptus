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
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent.SubPanelChangeAction;
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
    private boolean childsBulkLoad = false;

    public ContainerSubPanel(ConsoleLayout console) {
        super(console);
        setEditMode(getEditMode());
    }

    /**
     * @return the childsBulkLoad
     */
    protected boolean isChildsBulkLoad() {
        return childsBulkLoad;
    }
    
    /**
     * @param childsBulkLoad the childsBulkLoad to set
     */
    private void setChildsBulkLoad(boolean childsBulkLoad) {
        this.childsBulkLoad = childsBulkLoad;
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

    /**
     * If you don't want to add directly to container JPanel override this with false.
     * 
     * @return
     */
    protected boolean isAddSubPanelToPanelOrLetExtensionDoIt() {
        return true;
    }
    
    public final void addSubPanel(ConsolePanel panel) {
        if (panel == null || !addSubPanelExtra(panel))
            return;
        
        panels.add(panel);
        if (isAddSubPanelToPanelOrLetExtensionDoIt())
            this.add(panel);
        
        addSubPanelFinishUp();

        if (!childsBulkLoad)
            reloadLayoutOfComponent();

        // Let us inform the addition
        getConsole().informSubPanelListener(panel, SubPanelChangeAction.ADDED);
    }

    private void reloadLayoutOfComponent() {
        doLayout();
        invalidate();
        revalidate();
    }

    /**
     * Called from {@link #addSubPanel(ConsolePanel)} for extra work.
     * Empty implementation, override if needed it.
     * 
     * Return false to abort addition.
     * 
     * @param panel
     */
    protected boolean addSubPanelExtra(ConsolePanel panel) {
        return true;
    }

    /**
     * This is called at the end of the bulk load of child elements in {@link #readChildFromXml(Element)}
     * and can be used to layout only at the end of all childs.
     * 
     * This is an empty implementation.
     */
    protected void addSubPanelBulkFinishUp() {
    }
    
    /**
     * Override this to finish up layout tasks after the new component is added to container.
     * 
     * Check the {@link #isChildsBulkLoad()} to decide to layout the component here or wait for
     * the bulk load of all child. In this case use the {@link #addSubPanelBulkFinishUp()} 
     * for this layout.
     */
    protected void addSubPanelFinishUp() {
    }

    public final void removeSubPanel(ConsolePanel sp) {
        panels.remove(sp);
        if (isAddSubPanelToPanelOrLetExtensionDoIt())
            this.remove(sp);
        
        removeSubPanelExtra(sp);
        
        sp.clean();
        
        reloadLayoutOfComponent();
        
        // Let us inform the removal
        getConsole().informSubPanelListener(sp, SubPanelChangeAction.REMOVED);
    }

    /**
     * Called from {@link #removeSubPanel(ConsolePanel)} for extra work.
     * Empty implementation, override if needed it.
     * 
     * @param panel
     */
    protected void removeSubPanelExtra(ConsolePanel panel) {
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
        if (sp != null) {
            sp.clean();
            removeSubPanel(sp);
        }
    }

    public ConsolePanel getSubPanelByName(String name) {
        List<String> names = subPanelNames();
        int index = names.indexOf(name);
        if (index != -1)
            return panels.get(index);
        return null;
    }

    @Override
    protected void writeChildToXml(Element e) {
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
    protected void readChildFromXml(Element el) {
        List<?> list = el.selectNodes("*");
        setChildsBulkLoad(true);
        
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
        
        addSubPanelBulkFinishUp();
        setChildsBulkLoad(false);
        reloadLayoutOfComponent();
    }
   
    @Override
    protected void writePropertiesToXml(Element e) {
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
    
    public int getSubPanelsCount() {
        return panels.size();
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