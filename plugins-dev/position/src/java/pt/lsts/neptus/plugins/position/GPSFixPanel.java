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
 * Author: Rui Gonçalves
 * 2010/07/14
 */
package pt.lsts.neptus.plugins.position;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.Vector;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent;
import pt.lsts.neptus.console.plugins.SubPanelChangeListener;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ScatterPointsElement;
import pt.lsts.neptus.util.ReflectionUtil;

/**
 * @author rjpg
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Rui Gonçalves", name = "GPSFixPanel", icon = "pt/lsts/neptus/plugins/position/gpsbutton.png", description = "GPS Fix display")
@LayerPriority(priority = 40)
public class GPSFixPanel extends ConsolePanel implements MainVehicleChangeListener, Renderer2DPainter,
        SubPanelChangeListener, NeptusMessageListener {

    // @NeptusProperty (name="Entity", description="The entity of GPS to display (use 'GPS' for default GPS)")
    // public String entity = "GPS";

    @NeptusProperty(name = "Tail number of Points", description = "Number of points for GPS fix history.")
    public int tailPoints = 30;

    @NeptusProperty(name = "Show tail of fix GPS Points")
    public boolean showTailPoints = true;

    @NeptusProperty(name = "Show GPS Fix Icon")
    public boolean gpsFixIconVisible = true;

    @NeptusProperty(name = "Seconds to display ranges")
    public int secondsToDisplayRanges = 10;

    private boolean isExtGpsFix = true;

    {
        IMCMessage dummy = IMCDefinition.getInstance().create("GpsFix");
        if (dummy != null) {
            if (dummy.getMessageType().getFieldType("validity") != null)
                isExtGpsFix = true;
            else
                isExtGpsFix = false;
        }
    }

    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();

    private long lastCalcPosTimeMillis = 0;
    private boolean initCalled = false;
    private double lat, lon, radius;
    private ScatterPointsElement scatter = null;

    /**
	 * 
	 */
    public GPSFixPanel(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }

    @Override
    public void initSubPanel() {

        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.addPostRenderPainter(this, this.getClass().getSimpleName());
        }

        if (initCalled)
            return;
        initCalled = true;

        addMenuItem("Settings>Vehicle Trail Settings", null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(GPSFixPanel.this, getConsole(), true);
            }
        });
    }

    @Override
    public void subPanelChanged(SubPanelChangeEvent panelChange) {
        if (panelChange == null)
            return;

        if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(), ILayerPainter.class)) {
            ILayerPainter sub = (ILayerPainter) panelChange.getPanel();

            if (panelChange.added()) {
                renderers.add(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.addPostRenderPainter(this, "GPS FIX");
                }
            }

            if (panelChange.removed()) {
                renderers.remove(sub);
                ILayerPainter str2d = sub;
                if (str2d != null) {
                    str2d.removePostRenderPainter(this);

                }
            }
        }
    }

    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {
        if (showTailPoints) {
            if (scatter != null) {
                Graphics2D gS = (Graphics2D) g2.create();
                scatter.paint(gS, renderer, -renderer.getRotation());
            }
        }
        else
            scatter.clearPoints();

        if (gpsFixIconVisible) {
            double alfaPercentage = 1.0;
            long deltaTimeMillis = System.currentTimeMillis() - lastCalcPosTimeMillis;
            if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 2.0) {
                alfaPercentage = 0.5;
            }
            else if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 4.0) {
                alfaPercentage = 0.7;
            }

            LocationType lt = new LocationType();
            lt.setLatitudeDegs(lat);
            lt.setLongitudeDegs(lon);
            Point2D centerPos = renderer.getScreenPosition(new LocationType(lt));

            double radius = this.radius * renderer.getZoom();

            Graphics2D g = (Graphics2D) g2.create();

            Ellipse2D ellis = new Ellipse2D.Double(centerPos.getX() - radius, centerPos.getY() - radius, radius * 2,
                    radius * 2);
            g.setColor(new Color(255, 128, 128, (int) (64 * alfaPercentage)));
            g.fill(ellis);
            g.setColor(new Color(255, 128, 25, (int) (255 * alfaPercentage)));
            g.draw(ellis);

            g.setColor(new Color(0, 0, 0, (int) (255 * alfaPercentage)));
            g.draw(new Ellipse2D.Double(centerPos.getX() - 10, centerPos.getY() - 10, 20, 20));

            g.setColor(new Color(139, 69, 19, (int) (255 * alfaPercentage)));
            g.draw(new Ellipse2D.Double(centerPos.getX() - 12, centerPos.getY() - 12, 24, 24));
            g.setColor(new Color(0, 0, 0, (int) (255 * alfaPercentage)));
            g.draw(new Ellipse2D.Double(centerPos.getX() - 14, centerPos.getY() - 14, 28, 28));
            g.translate(centerPos.getX(), centerPos.getY());
            // g.setColor(new Color(255, 255, 0, (int) (200 * alfaPercentage)).darker());
            Color color = new Color(139, 69, 19).darker();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (200 * alfaPercentage)));
            g.fill(new Ellipse2D.Double(-7, -7, 14, 14));
            // g.setColor(new Color(255, 255, 0, (int) (150 * alfaPercentage)).brighter());
            color = new Color(139, 69, 19, 0).brighter();
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (150 * alfaPercentage)));
            g.setStroke(new BasicStroke(2));
            g.draw(new Ellipse2D.Double(-7, -7, 14, 14));
            g.setColor(new Color(0, 0, 0, (int) (150 * alfaPercentage)));
            g.fill(new Ellipse2D.Double(-2, -2, 4, 4));
            g.setColor(Color.BLACK);
            if (getConsole() != null && getConsole().getMainSystem() != null)
                g.drawString("GPS " + getConsole().getMainSystem(), 10, 10);
            g.translate(-centerPos.getX(), -centerPos.getY());
        }
    }

    @Override
    public void cleanSubPanel() {
        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.removePostRenderPainter(this);
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "GpsFix" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        if (isExtGpsFix)
            if (!message.getBitmask("validity").get("VALID_POS")) // Is it a valid fix?
                return;

        lat = Math.toDegrees(message.getDouble("lat"));
        lon = Math.toDegrees(message.getDouble("lon"));
        radius = message.getDouble("hacc");
        radius = Double.isNaN(radius) ? 0 : radius;

        lastCalcPosTimeMillis = System.currentTimeMillis();

        LocationType lt = new LocationType();
        lt.setLatitudeDegs(lat);
        lt.setLongitudeDegs(lon);

        if (showTailPoints) {
            if (scatter == null) {
                scatter = new ScatterPointsElement();
                scatter.setColor(Color.green.darker(), Color.black);
                scatter.setCenterLocation(lt);
                scatter.setNumberOfPoints(tailPoints);
            }
            if (scatter.getPoints().size() >= tailPoints) {
                if (scatter.getPoints().size() > 0)
                    scatter.getPoints().remove(0);
            }
            //double[] distFromRef = lt.getOffsetFrom(scatter.getCenterLocation());
            scatter.addPoint(lt);
        }
        else {
            if (scatter != null) {
                scatter.clearPoints();
            }
        }

    }
}