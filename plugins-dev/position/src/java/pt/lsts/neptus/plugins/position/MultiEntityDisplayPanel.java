/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 28/05/2011
 */
package pt.lsts.neptus.plugins.position;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.LinkedHashMap;

import javax.swing.Box;

import org.jdesktop.swingx.JXPanel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.SystemsList;
import pt.lsts.neptus.gui.painters.SubPanelTitlePainter;
import pt.lsts.neptus.messages.Enumerated;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
@PluginDescription(name = "Multi Entity Display Panel", author="Paulo Dias", version="1.0",
        icon="pt/lsts/neptus/plugins/position/position.png")
@SuppressWarnings("serial")
public class MultiEntityDisplayPanel extends ConsolePanel implements ConfigurationListener,
        NeptusMessageListener, MainVehicleChangeListener, IPeriodicUpdates {

    @NeptusProperty(name="Title")
    public String titleTxt = "";
    
    @NeptusProperty(name="Show Title")
    public boolean showTitle = false;

    @NeptusProperty(name="Show Title Inline for Every Entity")
    public boolean showTitleInline = false;

    @NeptusProperty(name="Message Name")
    public String messageName = "";
    
    @NeptusProperty(name="Message Field Name")
    public String messageFieldName = "";
    
    @NeptusProperty(name="Decimal Houses (if numeric value)")
    public short decimalHouses = 1;

    @NeptusProperty(name="Unit", description="The unit of the value.")
    public String unitName = "";

    @NeptusProperty(name="Show Unit in the Title")
    public boolean showUnitInTitle = false;

    @NeptusProperty(name="Number of columns", description="The number of columns to be displayed")
    public int numberOfCols = 1;

    @NeptusProperty(name="Update period (ms)", description="Interval between display updates, in milliseconds")
    public long millisBetweenUpdates = 200;
    
    @NeptusProperty(name="Font Size", description="The font size. Use '0' for automatic.")
    public int fontSize = DisplayPanel.DEFAULT_FONT_SIZE;

    private boolean noMessagesYet = true;
    private LinkedHashMap<String, DisplayPanel> displays = new LinkedHashMap<String, DisplayPanel>();
    private LinkedHashMap<String, String> values = new LinkedHashMap<String, String>();
    private LinkedHashMap<String, Long> lastUpdates = new LinkedHashMap<String, Long>();

    private JXPanel holder;
    private SubPanelTitlePainter backPainter;

    
    /**
     * 
     */
    public MultiEntityDisplayPanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    
    private void initialize() {
        removeAll();
        displays.clear();
        values.clear();
        lastUpdates.clear();
        
        backPainter = new SubPanelTitlePainter(
                (showTitle || noMessagesYet) ? (titleTxt.toLowerCase() + (showUnitInTitle ? " ("
                        + unitName + ")" : "")) : "");
        
        holder = new JXPanel();
        holder.setOpaque(false);
        int numCols = Math.max(numberOfCols, 1);
        holder.setLayout(new GridLayout(0, numCols));

        JXPanel holderB = new JXPanel();
        holderB.setBackgroundPainter(backPainter);
        holderB.setLayout(new BorderLayout());
        holderB.add(holder);
        if (!"".equalsIgnoreCase(titleTxt) && showTitle && !showTitleInline)
            holderB.add(Box.createVerticalStrut(fontSize), BorderLayout.NORTH);

        setLayout(new BorderLayout());
        add(holderB);
    }

    @Override
    public void propertiesChanged() {
        mainVehicleChange(getMainVehicleId());
    }

    private void resetView() {
        //clean();
        noMessagesYet = true;
        initialize();
        invalidate();
        validate();
    }
    
    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange e) {
        resetView();
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        for (String key : displays.keySet()) {
            try {
                long lastUpTime = lastUpdates.get(key);
                String value = values.get(key);
                DisplayPanel d = displays.get(key);
                if ((System.currentTimeMillis() - lastUpTime) > 5000)
                    d.setFontColor(Color.red.darker());
                else
                    d.setFontColor(Color.black);
                d.setText(value);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return millisBetweenUpdates;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.NeptusMessageListener#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        if ("".equalsIgnoreCase(messageName) || "".equalsIgnoreCase(messageFieldName))
            return new String[0];
        
        return new String[] {messageName};
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.NeptusMessageListener#messageArrived(pt.lsts.neptus.util.comm.vehicle.IMCMessage)
     */
    @Override
    public void messageArrived(IMCMessage message) {
        try {
            int srcEnt = message.getHeader().getInteger("src_ent");
            String entityName = EntitiesResolver.resolveName(
                    ImcSystemsHolder.lookupSystemByName(getMainVehicleId()).getId().toString(),
                    srcEnt);
            if (entityName == null)
                return;
            DisplayPanel dp;
            if (!displays.containsKey(entityName)) {
                String prefixAdded = "", postfixAdded = "";
                if (showTitle && !"".equalsIgnoreCase(titleTxt) && showTitleInline) {
                    prefixAdded = titleTxt + " ";
                    if (showUnitInTitle)
                        postfixAdded = " (" + unitName + ")";
                }

                dp = new DisplayPanel(prefixAdded + entityName.toLowerCase() + postfixAdded);
                dp.setFontSize(fontSize);
                displays.put(entityName, dp);
                holder.add(dp);
                invalidate();
                validate();
            }
            else {
                dp = displays.get(entityName);
            }
            lastUpdates.put(entityName, System.currentTimeMillis());
            Object value = message.getValue(messageFieldName);
            String valueStr = value.toString();
            try {
                double dvalue = MathMiscUtils.round(((Number)value).doubleValue(), decimalHouses);
                if (decimalHouses < 1)
                    valueStr = "" + (int) dvalue;
                else
                    valueStr = "" + dvalue;
                if (value instanceof Enumerated)
                    valueStr = value.toString();
            }
            catch (Exception e) {
                // So is not a Number and so toString works.
                e.getMessage();
                valueStr = value.toString();
            }
            String unitPostfix = "";
            if (!showUnitInTitle)
                unitPostfix =  " " + unitName;
            values.put(entityName, valueStr + unitPostfix);
            if (noMessagesYet) {
                noMessagesYet = false;
                if (!showTitle || (showTitle && ("".equalsIgnoreCase(titleTxt) || showTitleInline))) {
                    backPainter.setTitle("");
                    //removeAll(); // to remove the spacer
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        ConfigFetch.initialize();
        VehiclesHolder.loadVehicles();
        MultiEntityDisplayPanel mdp = new MultiEntityDisplayPanel(null);
        mdp.messageName = "EntityState";
        mdp.messageFieldName = "state";
        ConsoleParse.dummyConsole(mdp, new SystemsList(null));
        mdp.propertiesChanged();
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }


    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
