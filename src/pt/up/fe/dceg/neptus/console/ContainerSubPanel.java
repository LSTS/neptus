/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by ZP
 * 2009/09/16
 */
package pt.up.fe.dceg.neptus.console;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.SwingUtilities;

import org.dom4j.Attribute;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.plugins.LockableSubPanel;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelProvider;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

/**
 * @author ZP
 * @author Paulo Dias
 */
public class ContainerSubPanel extends SubPanel implements SubPanelProvider, LockableSubPanel {

    private static final long serialVersionUID = 1L;
    @NeptusProperty(name = "Maximize Panel", description = "Use this to indicate that this panel "
            + "should be maximized on load. (Only works for top level panels.)", distribution = DistributionEnum.DEVELOPER)
    public boolean maximizePanel = false;
    protected List<SubPanel> panels = new ArrayList<>();

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
        for (SubPanel sp : panels) {
            sp.init();
        }
        if (maximizePanel)
            setMaximizePanel(true);
    }

    @Override
    public void setEditMode(boolean b) {
        super.setEditMode(b);
        for (SubPanel sp : panels)
            sp.setEditMode(b);
    }

    @Override
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }

    public void addSubPanel(SubPanel panel) {
        panels.add(panel);
        this.add(panel);
    }

    public void removeSubPanel(SubPanel sp) {
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
        SubPanel sp = getSubPanelByName(subPanelName);
        sp.clean();
        if (sp != null)
            removeSubPanel(sp);

    }

    public SubPanel getSubPanelByName(String name) {
        List<String> names = subPanelNames();
        int index = names.indexOf(name);
        if (index != -1)
            return panels.get(index);
        return null;
    }

    @Override
    public void XML_ChildsWrite(Element e) {
        for (SubPanel sp : panels) {

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
            SubPanel subpanel = null;
            // process childs 
            if ("child".equals(element.getName())) {
                Attribute attribute = element.attribute("class");
                ConfigFetch.mark(attribute.getValue());
                try {
                    Class<?> clazz = Class.forName(attribute.getValue());
                    try {
                        subpanel = (SubPanel) clazz.getConstructor(ConsoleLayout.class).newInstance(console);
                        addSubPanel(subpanel);
                        subpanel.inElement(element);
                        ConfigFetch.benchmark(attribute.getValue());
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
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
    }

    @Override
    public String getPropertiesDialogTitle() {
        return PluginUtils.getPluginName(this.getClass()) + " parameters";
    }

    @Override
    public String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }

    @Override
    public void XML_PropertiesRead(Element e) {
        PluginUtils.setConfigXml(this, e.asXML());
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

        //MainPanel.cleanPanels(panels);
    }

    @Override
    public boolean isLocked() {
        if (panels.size() == 0)
            return false;
        boolean ret = true;
        for (SubPanel subPanel : panels) {
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
        for (SubPanel subPanel : panels) {
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
        for (SubPanel subPanel : panels) {
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
    public List<SubPanel> getSubPanels() {
        return panels;
    }

}