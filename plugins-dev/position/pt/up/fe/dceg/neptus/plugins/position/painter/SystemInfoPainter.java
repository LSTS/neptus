/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Correia
 * 2/12/2011
 */
package pt.up.fe.dceg.neptus.plugins.position.painter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.alarms.AlarmChangeListener;
import pt.lsts.neptus.alarms.AlarmManager.AlarmLevel;
import pt.lsts.neptus.alarms.AlarmProvider;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.SimpleSubPanel;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.comm.manager.imc.EntitiesResolver;
import pt.lsts.imc.IMCMessage;

/**
 * @author jqcorreia
 * 
 */
@SuppressWarnings("serial")
// "Information On Map"
@PluginDescription(name = "System Information On Map", icon = "pt/up/fe/dceg/neptus/plugins/position/position.png", description = "System Information display on map", documentation = "system-info/system-info.html", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 70)
public class SystemInfoPainter extends SimpleSubPanel implements Renderer2DPainter, NeptusMessageListener,
        IPeriodicUpdates, ConfigurationListener, AlarmChangeListener {

    private static final int ICON_SIZE = 24;
    private final ImageIcon CPU_ICON = ImageUtils.getScaledIcon(
            ImageUtils.getImage(getClass().getResource("images/cpu-icon.png")), ICON_SIZE, ICON_SIZE);
    private final ImageIcon BATT_ICON = ImageUtils.getScaledIcon(
            ImageUtils.getImage(getClass().getResource("images/battery-icon.png")), ICON_SIZE, ICON_SIZE);
    private final ImageIcon DISK_ICON = ImageUtils.getScaledIcon(
            ImageUtils.getImage(getClass().getResource("images/disk-icon.png")), ICON_SIZE, ICON_SIZE);
    private final ImageIcon NET_ICON = ImageUtils.getScaledIcon(
            ImageUtils.getImage(getClass().getResource("images/wifi-icon.png")), ICON_SIZE, ICON_SIZE);

    private static final int RECT_WIDTH = 250;
    private static final int RECT_HEIGHT = 100;
    private static final int MARGIN = 5;

    @NeptusProperty(name = "Enable")
    public boolean enablePainter = true;

    @NeptusProperty(name = "Enable Info", description = "Paint Vehicle Information on panel")
    public boolean paintInfo = true;

    @NeptusProperty(name = "Enable Alarm", description = "Paint border on alarm state")
    public boolean paintBorder = true;

    @NeptusProperty(name = "Entity Name", description = "Vehicle Battery entity name")
    public String batteryEntityName = "Batteries";

    private String mainSysName;

    private int cpuUsage = 0;
    private double batteryVoltage;
    private float fuelLevel, confidenceLevel;
    private int storageUsage;

    private int hbCount = 0;
    private int lastHbCount = 0;

    private Font textFont;

    private AlarmLevel alarmLevel = AlarmLevel.NORMAL;

    public SystemInfoPainter(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
        mainSysName = getConsole().getMainSystem();

        // Register as AlarmProvider for Cpu/Batt/Net/Disk

        addMenuItem(
                I18n.text("Advanced") + ">" + PluginUtils.getPluginI18nName(this.getClass()) + " "
                        + I18n.text("Enable/Disable"), null, new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        enablePainter = !enablePainter;
                    }

                });

        // Initialize the fonts
        try {
            textFont = new Font("Arial", Font.BOLD, 12);
        }
        catch (Exception e1) {
            e1.printStackTrace();
            NeptusLog.pub().info("<###>Font Loading Error");
        }
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!enablePainter || mainSysName == null)
            return;

        // Red alarm border
        if (paintBorder) {
            if (alarmLevel.getValue() > AlarmLevel.NORMAL.getValue()) {
                g.setStroke(new BasicStroke(3));
                g.setColor(getAlarmColor());
                g.drawRect(0, 0, renderer.getWidth() - 2, renderer.getHeight() - 2);
            }
        }

        // System Info
        if (paintInfo) {
            g.setColor(new Color(255, 255, 255, 75));
            // g.setFont(font);
            g.drawRoundRect(renderer.getWidth() - RECT_WIDTH - MARGIN, renderer.getHeight() - RECT_HEIGHT - MARGIN,
                    RECT_WIDTH, RECT_HEIGHT, 20, 20);
            g.fillRoundRect(renderer.getWidth() - RECT_WIDTH - MARGIN, renderer.getHeight() - RECT_HEIGHT - MARGIN,
                    RECT_WIDTH, RECT_HEIGHT, 20, 20);
            g.translate(renderer.getWidth() - RECT_WIDTH - MARGIN, renderer.getHeight() - RECT_HEIGHT - MARGIN);

            g.setColor(Color.BLACK);
            g.setFont(textFont);
            g.drawString(I18n.text("Vehicle")+": " + mainSysName, 5, 15);

//            g.drawImage(CPU_ICON.getImage(), 5, 25, null);
            g.drawString("CPU usage: " + cpuUsage + "%", 5, 40);

//            g.drawImage(BATT_ICON.getImage(), 65, 25, null);
            g.drawString("Fuel Level: " + (int) fuelLevel + "% " + (int) (batteryVoltage * 100) / 100f + "V " + MathMiscUtils.round(confidenceLevel, 2) + "%", 5, 55);
//            g.drawString((int) (batteryVoltage * 100) / 100f + " V", 90, 47);

//            g.drawImage(DISK_ICON.getImage(), 130, 25, null);
            g.drawString("Storage Usage: " + storageUsage + "%", 5, 70);

//            g.drawImage(NET_ICON.getImage(), 195, 25, null);
//
//            // Preventing an Heartbeat rate of 120%
//            if (lastHbCount > 5)
//                lastHbCount = 5;

            g.drawString("Heartbeat: " + (lastHbCount * 20) + "%", 5, 85);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.NeptusMessageListener#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return new String[] { "CpuUsage", "StorageUsage", "Voltage", "Heartbeat", "FuelLevel" };
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.NeptusMessageListener#messageArrived(pt.up.fe.dceg.neptus.imc.IMCMessage)
     */
    @Override
    public void messageArrived(IMCMessage message) {
        if (message.getAbbrev().equals("CpuUsage")) {
            cpuUsage = message.getInteger("value");
        }
        else if (message.getAbbrev().equals("StorageUsage")) {
            storageUsage = message.getInteger("value");
        }
        else if (message.getAbbrev().equals("Voltage")) {
            if (message.getHeader().getInteger("src_ent") == EntitiesResolver.resolveId(getConsole().getMainSystem(),
                    batteryEntityName))
                batteryVoltage = message.getDouble("value");
        }
        else if (message.getAbbrev().equals("Heartbeat")) {
            hbCount++;
        }
        else if (message.getAbbrev().equals("FuelLevel")) {
            fuelLevel = message.getFloat("value");
            confidenceLevel = message.getFloat("confidence");
        }
    }

    @Override
    public void mainVehicleChangeNotification(String id) {
        // Resolve Batteries entity ID to check battery values
        batteryVoltage = 0.0;
        fuelLevel = 0.0f;
        cpuUsage = 0;
        storageUsage = 0;
        hbCount = 0;
        mainSysName = getConsole().getMainSystem();
    }

    // Periodical Update to assess the hearbeat reception rate
    @Override
    public long millisBetweenUpdates() {
        return 5000;
    }

    @Override
    public boolean update() {
        lastHbCount = hbCount;
        hbCount = 0;

        return true;
    }

    @Override
    public void propertiesChanged() {

    }

    /**
     * Return the color of the border to paint in StateRenderer2D
     * 
     * @return the color based on general alarm level
     */
    Color getAlarmColor() {
        switch (alarmLevel) {
            case INFO:
                return Color.BLUE.brighter();
            case FAULT:
                return Color.yellow;
            case ERROR:
                return Color.orange;
            case FAILURE:
                return Color.red;
            default:
                return null;
        }
    }

    @Override
    public void alarmStateChanged(AlarmProvider provider) {

    }

    @Override
    public void maxAlarmStateChanged(AlarmLevel maxlevel) {
        alarmLevel = maxlevel;
    }

    @Override
    public void alarmAdded(AlarmProvider provider) {
    }

    @Override
    public void alarmRemoved(AlarmProvider provider) {
    }

    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
