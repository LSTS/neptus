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
 * 2011/07/03
 */
package pt.lsts.neptus.plugins.position;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Vector;

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
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.ReflectionUtil;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Paulo Dias", name = "RemoteState Simple Display", description = "Remote State simple display")
@LayerPriority(priority = 60)
public class RemoteStateSimplePanel extends ConsolePanel implements MainVehicleChangeListener, Renderer2DPainter,
        SubPanelChangeListener, NeptusMessageListener {

    @NeptusProperty(name = "Show Icon")
    public boolean iconVisible = true;

    @NeptusProperty(name = "Seconds to display ranges")
    public int secondsToDisplayRanges = 5;

    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();

    private long lastCalcPosTimeMillis = 0;
    private double lat = Double.NaN, lon = Double.NaN, depth = Double.NaN, headingDegrees = Double.NaN;

    private static GeneralPath arrowShape = null;

    /**
	 * 
	 */
    public RemoteStateSimplePanel(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }

    @Override
    public void initSubPanel() {
        renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
        for (ILayerPainter str2d : renderers) {
            str2d.addPostRenderPainter(this, this.getClass().getSimpleName());
        }

        addMenuItem("Tools>" + PluginUtils.getPluginName(this.getClass()) + ">Settings", null, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(RemoteStateSimplePanel.this, getConsole(), true);
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.NeptusMessageListener#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return new String[] { "RemoteState" };
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * pt.lsts.neptus.plugins.NeptusMessageListener#messageArrived(pt.lsts.neptus.util.comm.vehicle.IMCMessage
     * )
     */
    @Override
    public void messageArrived(IMCMessage message) {
        try {
            double latTmp = message.getDouble("lat");
            double lonTmp = message.getDouble("lon");
            if (Double.isNaN(latTmp) || Double.isNaN(lonTmp))
                return;

            lat = Math.toDegrees(latTmp);
            lon = Math.toDegrees(lonTmp);
            lastCalcPosTimeMillis = System.currentTimeMillis();

            depth = message.getDouble("depth");
            double headingTmp = message.getDouble("psi");
            if (!Double.isNaN(headingTmp))
                headingDegrees = Math.toDegrees(headingTmp);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
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
        if (iconVisible) {
            if (Double.isNaN(lat) || Double.isNaN(lon))
                return;

            double alfaPercentage = 1.0;
            long deltaTimeMillis = System.currentTimeMillis() - lastCalcPosTimeMillis;
            if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 2.0) {
                alfaPercentage = 0.5;
            }
            else if (deltaTimeMillis > secondsToDisplayRanges * 1000.0 / 4.0) {
                alfaPercentage = 0.7;
            }

            double rotationAngle = renderer.getRotation();

            LocationType lt = new LocationType();
            lt.setLatitudeDegs(lat);
            lt.setLongitudeDegs(lon);
            Point2D centerPos = renderer.getScreenPosition(new LocationType(lt));

            Graphics2D g = (Graphics2D) g2.create();
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
                g.drawString(
                        "RemoteState " + getConsole().getMainSystem() + " :: depth=" + MathMiscUtils.round(depth, 1),
                        10, 15);

            if (!Double.isNaN(headingDegrees)) {
                double newYaw = Math.toRadians(headingDegrees);
                Shape shape = getArrow();
                g.rotate(-rotationAngle);
                g.rotate(newYaw + Math.PI);
                color = Color.BLACK; // orangeNINFO.brighter();//new Color(255, 255, 0).brighter();
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (150 * alfaPercentage)));
                g.setStroke(new BasicStroke(2));
                g.fill(shape);
                color = Color.BLACK.darker(); // orangeNINFO.brighter();//new Color(255, 255, 0).brighter();
                g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (150 * alfaPercentage)));
                g.draw(shape);
                g.setColor(Color.BLACK);
                g.rotate(-(newYaw + Math.PI));
                g.rotate(rotationAngle);
            }
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

    private static GeneralPath getArrow() {
        if (arrowShape == null) {
            arrowShape = new GeneralPath();
            arrowShape.moveTo(-2, 0);
            arrowShape.lineTo(2, 0);
            arrowShape.lineTo(2, 0);
            arrowShape.lineTo(8, 0);
            arrowShape.lineTo(0, 8);
            arrowShape.lineTo(-8, 0);
            arrowShape.lineTo(-2, 0);
            arrowShape.closePath();
        }
        return arrowShape;
    }

}