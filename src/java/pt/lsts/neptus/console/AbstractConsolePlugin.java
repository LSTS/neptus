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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * Jan 21, 2014
 */
package pt.lsts.neptus.console;

import javax.swing.ImageIcon;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.MissionChangeListener;
import pt.lsts.neptus.console.plugins.PlanChangeListener;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.PluginMenuUtils;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.plugins.update.PeriodicUpdatesService;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
public abstract class AbstractConsolePlugin implements PropertiesProvider {

    private ConsoleLayout console;
    private ImageIcon icon;
    
    @Override
    public DefaultProperty[] getProperties() {
        return PluginUtils.getPluginProperties(this);
    }

    @Override
    public void setProperties(Property[] properties) {
        PluginUtils.setPluginProperties(this, properties);
        propertiesChanged();
    }

    @Override
    public final String getPropertiesDialogTitle() {
        return PluginUtils.getPluginName(getClass())+" parameters";
    }

    @Override
    public final String[] getPropertiesErrors(Property[] properties) {
        return PluginUtils.validatePluginProperties(this, properties);
    }
    
    public Element asElement(String rootElement) {
        Document doc = null;
        doc = DocumentHelper.createDocument();
        Element root = doc.addElement(rootElement);
        root.addAttribute("class", this.getClass().getName());
        Element properties = root.addElement("properties");
        
        String xml = PluginUtils.getConfigXml(this);
        try {
            Element el = DocumentHelper.parseText(xml).getRootElement();

            for (Object child : el.elements()) {
                Element aux = (Element) child;
                aux.detach();
                properties.add(aux);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        
        return root;
    }
    
    public void parseXmlElement(Element elem) {
        PluginUtils.setConfigXml(this, elem.element("properties").asXML());      
    }
    
    public String getName() {
        return PluginUtils.getPluginName(getClass());
    }
    
    public final void init(ConsoleLayout console) {
        this.console = console;
        
        initPlugin(console);
        
        // After all setup let us register the IPeriodicUpdates and Message callbacks
        
        if (this instanceof MissionChangeListener)
            getConsole().addMissionListener((MissionChangeListener) this);

        if (this instanceof PlanChangeListener)
            getConsole().addPlanListener((PlanChangeListener) this);
        
        if (this instanceof IPeriodicUpdates) {
            PeriodicUpdatesService.register((IPeriodicUpdates) this);
        }
        
        PeriodicUpdatesService.registerPojo(this);
        PluginMenuUtils.addPluginMenus(console, this);
        
        NeptusEvents.register(this, console);
        getConsole().getImcMsgManager().registerBusListener(this);
    }
    
    /**
     * Use this to setup your plugin before the register of 
     * the IPeriodicUpdates and Message callbacks.
     */
    protected abstract void initPlugin(ConsoleLayout console);

    public void clean() {
        NeptusEvents.unregister(this, getConsole());
        if (this instanceof MissionChangeListener) {
            getConsole().removeMissionListener((MissionChangeListener) this);
        }

        if (this instanceof MainVehicleChangeListener) {
            getConsole().removeMainVehicleListener((MainVehicleChangeListener)this);
        }

        if (this instanceof PlanChangeListener) {
            getConsole().removePlanListener((PlanChangeListener) this);
        }

        if (this instanceof IPeriodicUpdates)
            PeriodicUpdatesService.unregister((IPeriodicUpdates) this);

        PeriodicUpdatesService.unregisterPojo(this);
        PluginMenuUtils.removePluginMenus(console, this);
        
        getConsole().getImcMsgManager().unregisterBusListener(this);        
    }
    
    protected final ImcSystemState getState() {
        return getConsole().getImcMsgManager().getState(getConsole().getMainSystem());
    }
    
    public final ImageIcon getIcon() {
        if (icon == null)
            icon = ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass()));
        
        return icon;
    }
    
    public void propertiesChanged() {
        
    }

    /**
     * @return the console
     */
    public ConsoleLayout getConsole() {
        return console;
    }    
}
