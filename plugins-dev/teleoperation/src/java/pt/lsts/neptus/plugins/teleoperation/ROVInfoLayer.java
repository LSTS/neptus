/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jqcorreia
 * Nov 7, 2013
 */
package pt.lsts.neptus.plugins.teleoperation;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.DesiredHeading;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.Distance;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.DoubleMinMaxValidator;

/**
 * @author jqcorreia
 *
 */
@PluginDescription(name = "ROV Information Layer", icon = "pt/lsts/neptus/plugins/position/position.png", description = "ROV Information Layer", category = CATEGORY.INTERFACE)
@Popup(pos = POSITION.CENTER, width = 220, height = 140, accelerator = '9')
@LayerPriority(priority = 70)
public class ROVInfoLayer extends ConsolePanel implements Renderer2DPainter {
    
    private static final long serialVersionUID = 4624519156694623532L;
    
    private static final double MIN_DEPTH_THRESH = 0.01;
    private static final double MAX_DEPTH_THRESH = 100;
    private static final double MIN_DISTANCE_THRESH = 0.01;
    private static final double MAX_DISTANCE_THRESH = 20;
    private static final double MIN_HEADING_THRESH = 0.1;
    private static final double MAX_HEADING_THRESH = 360;

    private double desiredDepth = 0;
    private double desiredHeading = 0;
    private double desiredDistance = 0;
    private double depth = 0;
    private double heading = 0;
    private double altitude = 0;
    private double distance = 0;

    private boolean loopWallTracking = false;
    private boolean loopHeadingControl = false;
    private boolean lastLoopWallTracking = false;
    private boolean lastLoopHeadingControl = false;

    @NeptusProperty(name="Heading threshold", description="Threshold when to flag difference RED")
    public double headingThresh = 15;

    @NeptusProperty(name="Depth threshold", description="Threshold when to flag difference RED")
    public double depthThresh = 0.3;

    @NeptusProperty(name="Distance threshold", description="Threshold when to flag difference RED")
    public double distanceThresh = 0.2;

    @NeptusProperty(name = "Desired Distance Entity Name", description = "DesiredDistance entity name")
    public String desiredEntityName = "Desired Distance";

    @NeptusProperty(name = "Distance Entity Name", description = "Distance entity name")
    public String distanceEntityName = "Filtered Distance";

    private JLabel info;

    private boolean isShowingDialog = false;

    public ROVInfoLayer(ConsoleLayout console) {
        super(console);
        removeAll();
        info = new JLabel("<html></html>");

        this.add(info);
    }

    @Override
    public void initSubPanel() {
        if (dialog != null)
            dialog.setResizable(false);
    }

    @Override
    public void cleanSubPanel() {
        removeAll();
    }

    public String validateDepthThresh(double value) {
        return new DoubleMinMaxValidator(MIN_DEPTH_THRESH, MAX_DEPTH_THRESH).validate(value);
    }

    public String validateHeadingThresh(double value) {
        return new DoubleMinMaxValidator(MIN_HEADING_THRESH, MAX_HEADING_THRESH).validate(value);
    }

    public String validateDistanceThresh(double value) {
        return new DoubleMinMaxValidator(MIN_DISTANCE_THRESH, MAX_DISTANCE_THRESH).validate(value);
    }

    private String getColor(double reference, double value, double threshold) {
        if (Math.abs(reference - value) > threshold)
            return "#ff0000";
        else
            return "#000000";
    }

    private String getInfo(boolean strike, String text, double desired, double value, double threshold, String unitString) {
        String txt = "";

        if (strike)
            txt += "<strike>";
        txt += "<b>" + text + ": </b> [" + MathMiscUtils.round(desired, 2) + unitString + "] <b><font color="
                + getColor(value, desired, threshold) + ">" + MathMiscUtils.round(value, 2) + unitString + "</font></b><br/>";
        if (strike)
            txt += "</strike>";

        return txt;
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (dialog!=null && dialog.isShowing())
            return;

        g.setColor(Color.BLACK);
        g.translate(0, renderer.getHeight() - 100);
        info.paint(g);
    }

    @Subscribe
    public void onMessage(EstimatedState state) {
        if(state.getSourceName().equals(getMainVehicleId())) {
            depth = MathMiscUtils.round(state.getDepth(), 3);
            heading = MathMiscUtils.round(state.getPsi(), 3);
            altitude = MathMiscUtils.round(state.getAlt(), 3);
        }
    }

    @Subscribe
    public void onMessage(DesiredZ dz) {
        if(dz.getSourceName().equals(getMainVehicleId())) {
            desiredDepth = MathMiscUtils.round(dz.getValue(), 3);
        }
    }

    @Subscribe
    public void onMessage(DesiredHeading dh) {
        if(dh.getSourceName().equals(getMainVehicleId())) {
            desiredHeading = MathMiscUtils.round(dh.getValue(), 3);
            lastLoopHeadingControl = loopHeadingControl = true;
        }
    }

    @Subscribe
    public void onMessage(Distance d) {
        if(!d.getSourceName().equals(getMainVehicleId()))
            return;

        int idDes = EntitiesResolver.resolveId(getMainVehicleId(),
                desiredEntityName);

        int idDis = EntitiesResolver.resolveId(getMainVehicleId(),
                distanceEntityName);

        if (d.getSrcEnt() == idDes) {
            desiredDistance = MathMiscUtils.round(d.getValue(), 3);
            lastLoopWallTracking = loopWallTracking = true;
        }

        if (d.getSrcEnt() == idDis)
            distance = MathMiscUtils.round(d.getValue(), 3);
    }
    
    @Periodic(millisBetweenUpdates=500)
    public boolean updateLabel(){
        double headingDeg = Math.toDegrees(heading);
        double desiredHeadingDeg = Math.toDegrees(desiredHeading);

        String txt = "<html><font color=#000000>";
        txt += getInfo(!lastLoopHeadingControl, I18n.text("Heading"), desiredHeadingDeg, headingDeg, headingThresh, "" + CoordinateUtil.CHAR_DEGREE);
        txt += getInfo(!lastLoopWallTracking, I18n.text("WallTrack"), desiredDistance, distance, distanceThresh, "m");
        txt += getInfo(false, I18n.text("Depth"), desiredDepth, depth, depthThresh, "m");
        txt += "<b>Altitude: " + MathMiscUtils.round(altitude, 2) + "m</b>";
        txt += "</font></html>";

        info.setText(txt);
        info.setForeground(Color.white);
        info.setHorizontalTextPosition(JLabel.CENTER);
        info.setHorizontalAlignment(JLabel.LEFT);
        info.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
        info.setBounds(0, 0, 300, 70);
        
        if (isShowingDialog)
            this.repaint();
        
        return true;
    }
    
    @Periodic(millisBetweenUpdates=5000)
    public boolean update() {
        lastLoopWallTracking = loopWallTracking;
        loopWallTracking = false;
        lastLoopHeadingControl = loopHeadingControl;
        loopHeadingControl = false;
        return true;
    }
}
