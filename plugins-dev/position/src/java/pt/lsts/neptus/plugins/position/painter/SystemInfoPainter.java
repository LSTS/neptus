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
 * Author: José Correia
 * 2/12/2011
 */
package pt.lsts.neptus.plugins.position.painter;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.CpuUsage;
import pt.lsts.imc.Current;
import pt.lsts.imc.FuelLevel;
import pt.lsts.imc.GpsFix;
import pt.lsts.imc.Heartbeat;
import pt.lsts.imc.StorageUsage;
import pt.lsts.imc.Voltage;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.MathMiscUtils;

/**
 * @author jqcorreia
 * @author pdias
 */
@PluginDescription(name = "System Information On Map", icon = "pt/lsts/neptus/plugins/position/painter/sysinfo.png", description = "System Information display on map", documentation = "system-info/system-info.html", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 70)
public class SystemInfoPainter extends ConsoleLayer {

    private static final String GPS_DIFF = I18n.textc("DIFF", "Differencial GPS. Use a single small word");
    private static final String GPS_3D = I18n.textc("3D", "Use a single small word");
    private static final String GPS_2D = I18n.textc("2D", "Use a single small word");
    private static final String GPS_NO_FIX = I18n.textc("NoFix", "Use a single small word");
    private static final String GPS_MAN = I18n.textc("MAN", "Manual input.Use a single small word");
    private static final String GPS_SIM = I18n.textc("SIM", "Simulated. Use a single small word");

    private static final int RECT_WIDTH = 248;
    private static final int RECT_HEIGHT = 70;
    private static final int MARGIN = 5;

    private String strCpu, strFuel, strComms, strDisk, strGPS, strGPSFix, strFixedSat;

    @NeptusProperty(name = "Enable")
    public boolean enablePainter = true;

    @NeptusProperty(name = "Enable Info", description = "Paint Vehicle Information on panel")
    public boolean paintInfo = true;

    @NeptusProperty(name = "Display Confidence Level", description = "Display Fuel Level estimation Confidence on panel")
    public boolean showConfidence = true;

    @NeptusProperty(name = "Display Current", description = "Display drawn Current on panel")
    public boolean showCurrent = false;

    @NeptusProperty(name = "Display GPS", description = "Display GPS fix status on panel")
    public boolean showGPS = false;

    @NeptusProperty(name = "Entity Name", description = "Vehicle Battery entity name")
    public String batteryEntityName = "Batteries";

    private JLabel toDraw;
    private String mainSysName;

    private long lastMessageMillis = 0;

    private int cpuUsage = 0, fixQuality = 0, fixedSats = 0;;
    private double batteryVoltage, current, fixHdop;
    private float fuelLevel, confidenceLevel;
    private int storageUsage;
    private GpsFix.TYPE fixType;
    private int fixValidity;

    private int hbCount = 0;
    private int lastHbCount = 0;

    @Override
    public void initLayer() {
        mainSysName = getConsole().getMainSystem();
        toDraw = new JLabel("<html></html>");

        strCpu = I18n.textc("CPU", "Use a single small word");
        strFuel = I18n.textc("Fuel", "Use a single small word");
        strDisk = I18n.textc("Disk", "Use a single small word");
        strComms = I18n.textc("Comms", "Use a single small word");
        strGPS = I18n.textc("GPS", "Use a single small word");
        strFixedSat = I18n.textc("Sats", "Short for satellite. Use a single small word");

        strGPSFix = GPS_NO_FIX;
    }

    private InterpolationColorMap rygColorMap = new InterpolationColorMap(new double[] { 0.0, 0.01, 0.75, 1.0 },
            new Color[] { Color.black, new Color(0xff7367), Color.yellow, Color.green.brighter() });

    private InterpolationColorMap greenToBlack = new InterpolationColorMap(new double[] { 0.0, 0.75, 1.0 },
            new Color[] { Color.black, Color.green.darker(), Color.green.brighter().brighter() });

    private ColorMap rygInverted = ColorMapFactory.createInvertedColorMap(rygColorMap);

    private String getColor(double percent, boolean inverted, boolean commsDead) {
        Color c;
        if (commsDead)
            return "#000000";
        if (!inverted)
            c = rygColorMap.getColor(percent / 100.0);
        else
            c = rygInverted.getColor(percent / 100.0);

        return String.format("#%02X%02X%02X", c.getRed(), c.getGreen(), c.getBlue());
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (!enablePainter || mainSysName == null)
            return;

        boolean commsDead = false;
        if (System.currentTimeMillis() - lastMessageMillis > 10000)
            commsDead = true;

        // System Info
        if (paintInfo) {

            if (lastHbCount > 5)
                lastHbCount = 5;
            String txt = "<html>";
            txt += "<b>" + strCpu + ":</b> <font color=" + getColor(cpuUsage, true, commsDead) + ">" + cpuUsage
                    + "%</font><br/>";
            txt += "<b>" + strFuel + ":</b> <font color=" + getColor(fuelLevel, false, commsDead) + ">"
                    + (int) Math.round(fuelLevel) + "%</font> <font color=#cccccc>(" + (int) (batteryVoltage * 10) / 10f + "V";
            if (showCurrent)
                txt += "@" + (int) (current * 10) / 10f + "A";
            if (showConfidence)
                txt += ", ~" + (long) MathMiscUtils.round(confidenceLevel, 0) + "%";
            txt += "</font>)<br/>";
            txt += "<b>" + strDisk + ":</b> <font color=" + getColor(storageUsage, false, commsDead) + ">"
                    + storageUsage + "%</font><br/>";
            txt += "<b>" + strComms + ":</b> <font color=" + getColor(lastHbCount * 20, false, commsDead) + ">"
                    + (lastHbCount * 20) + "%</font>";
            if (showGPS) {
                txt += " <b>" + strGPS + ":</b> <font color=" + getColor(fixQuality, false, commsDead) + ">"
                        + strGPSFix + "</font>";
                txt += " (<font color=" + getColor(fixQuality, false, commsDead) + ">"
                        + fixedSats + "</font> " + strFixedSat + ")";
            }
            txt += "<br/>";
            txt += "</html>";

            toDraw.setText(txt);
            toDraw.setForeground(Color.white);
            toDraw.setHorizontalTextPosition(JLabel.CENTER);
            toDraw.setHorizontalAlignment(JLabel.LEFT);
            toDraw.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

            g.setColor(new Color(0, 0, 0, 200));
            g.drawRoundRect(renderer.getWidth() - RECT_WIDTH - MARGIN, renderer.getHeight() - RECT_HEIGHT - MARGIN,
                    RECT_WIDTH, RECT_HEIGHT, 20, 20);
            g.setColor(new Color(0, 0, 0, 100));
            g.fillRoundRect(renderer.getWidth() - RECT_WIDTH - MARGIN, renderer.getHeight() - RECT_HEIGHT - MARGIN,
                    RECT_WIDTH, RECT_HEIGHT, 20, 20);
            g.translate(renderer.getWidth() - RECT_WIDTH - MARGIN, renderer.getHeight() - RECT_HEIGHT - MARGIN);

            toDraw.setBounds(0, 0, RECT_WIDTH, RECT_HEIGHT);
            toDraw.paint(g);
        }

        double ellapsed = (System.currentTimeMillis() - lastMessageMillis);
        double val = Math.max(0, (3000 - ellapsed) / 3000);
        g.setColor(greenToBlack.getColor(val));
        g.fill(new Ellipse2D.Double(RECT_WIDTH - 14, 9, 8, 8));
    }

    @Subscribe
    public void consume(CpuUsage msg) {
        if (!msg.getSourceName().equals(mainSysName))
            return;
        cpuUsage = msg.getValue();
    }

    @Subscribe
    public void consume(StorageUsage msg) {
        if (!msg.getSourceName().equals(mainSysName))
            return;
        storageUsage = 100 - msg.getValue();
    }

    @Subscribe
    public void consume(Voltage msg) {
        if (!msg.getSourceName().equals(mainSysName))
            return;
        int id = EntitiesResolver.resolveId(mainSysName, batteryEntityName);
        if (msg.getSrcEnt() != id)
            return;
        batteryVoltage = msg.getValue();
    }

    @Subscribe
    public void consume(Current msg) {
        if (!msg.getSourceName().equals(mainSysName))
            return;
        int id = EntitiesResolver.resolveId(mainSysName, batteryEntityName);
        if (msg.getSrcEnt() != id)
            return;
        current = msg.getValue();
    }

    @Subscribe
    public void consume(FuelLevel msg) {
        if (!msg.getSourceName().equals(mainSysName))
            return;
        fuelLevel = (float) msg.getValue();
        confidenceLevel = (float) msg.getConfidence();
    }

    @Subscribe
    public void consume(GpsFix msg) {
        if (!msg.getSourceName().equals(mainSysName))
            return;
        fixType = msg.getType();
        fixValidity = msg.getValidity();
        fixHdop = msg.getHdop();
        fixHdop = Math.round(fixHdop*100.0)/100.0;
        fixedSats = msg.getSatellites();
        
        switch (fixType) {
            case DEAD_RECKONING:
                fixQuality = 0;
                strGPSFix = GPS_NO_FIX; //.concat(" ("+fixHdop+")");
                break;
            case STANDALONE:
                fixQuality = 70;
                strGPSFix = GPS_2D;
                if ((fixValidity & GpsFix.GFV_VALID_VDOP) != 0) {
                    fixQuality = 90;
                    strGPSFix = GPS_3D.concat(" ("+fixHdop+")");
                }
                break;
            case DIFFERENTIAL:
                fixQuality = 100;
                strGPSFix = GPS_DIFF.concat(" ("+fixHdop+")");
                break;
            case MANUAL_INPUT:
                fixQuality = 100;
                strGPSFix = GPS_MAN;
                break;
            case SIMULATION:
                fixQuality = 100;
                strGPSFix = GPS_SIM.concat(" ("+fixHdop+")");
                break;
            default:
                fixQuality = 0;
                strGPSFix = "-";
                break;
        }
    }

    @Subscribe
    public void consume(Heartbeat msg) {
        if (!msg.getSourceName().equals(mainSysName))
            return;

        hbCount++;
        lastMessageMillis = System.currentTimeMillis();
    }

    @Subscribe
    public void consume(ConsoleEventMainSystemChange ev) {
        // Resolve Batteries entity ID to check battery values
        batteryVoltage = 0.0;
        current = 0.0;
        fuelLevel = 0.0f;
        cpuUsage = 0;
        storageUsage = 0;
        hbCount = 0;
        strGPSFix = GPS_NO_FIX;
        fixQuality = 0;
        fixHdop = 0.0;
        mainSysName = ev.getCurrent();

        ImcSystemState state = getState();
        if (state != null) {
            if (state.last(Heartbeat.class) != null)
                lastMessageMillis = state.last(Heartbeat.class).getTimestampMillis();
            if (state.last(StorageUsage.class) != null)
                storageUsage = 100 - state.last(StorageUsage.class).getValue();
            if (state.last(CpuUsage.class) != null)
                cpuUsage = state.last(CpuUsage.class).getValue();
            if (state.last(FuelLevel.class) != null)
                fuelLevel = (float) state.last(FuelLevel.class).getValue();
            try {
                if (state.last(Voltage.class, batteryEntityName) != null)
                    batteryVoltage = state.last(Voltage.class, batteryEntityName).getValue();
            }
            catch (Exception e) {
                batteryVoltage = 0.0;
            }
        }
    }

    @Periodic(millisBetweenUpdates = 5000)
    public boolean update() {
        lastHbCount = hbCount;
        hbCount = 0;

        return true;
    }

    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void cleanLayer() {
    }
}
