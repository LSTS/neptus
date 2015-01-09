/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: jqcorreia
 * Nov 7, 2013
 */
package pt.lsts.neptus.plugins.teleoperation;

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import pt.lsts.imc.DesiredHeading;
import pt.lsts.imc.DesiredZ;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.DoubleMinMaxValidator;

import com.google.common.eventbus.Subscribe;

/**
 * @author jqcorreia
 *
 */
@PluginDescription(name = "ROV Information Layer", icon = "pt/lsts/neptus/plugins/position/position.png", description = "ROV Information Layer", category = CATEGORY.INTERFACE)
@LayerPriority(priority = 70)
public class ROVInfoLayer extends ConsolePanel implements Renderer2DPainter
{
    private static final long serialVersionUID = 4624519156694623532L;
    /**
     * @param console
     */
    
    private static final double MIN_DEPTH_THRESH = 0.01;
    private static final double MAX_DEPTH_THRESH = 100;
    private static final double MIN_HEADING_THRESH = 0.1;
    private static final double MAX_HEADING_THRESH = 360;

    JLabel info;
    private double desiredDepth = 0;
    private double desiredHeading = 0;
    private double depth = 0;
    private double heading = 0;
    private double altitude = 0;

    @NeptusProperty(name="Heading threshold", description="Threshold when to flag difference RED")
    public double headingThresh = 15;

    @NeptusProperty(name="Depth threshold", description="Threshold when to flag difference RED")
    public double depthThresh = 0.3;

    public ROVInfoLayer(ConsoleLayout console) {
        super(console);

	info = new JLabel("<html></html>");
    }

    private String getColor(double reference, double value, double threshold) {
	if (Math.abs(reference - value) > threshold)
	    return "#ff0000";
	else
	    return "#000000";
    }

    public String validateDepthThresh(double value) {
        return new DoubleMinMaxValidator(MIN_DEPTH_THRESH, MAX_DEPTH_THRESH).validate(value);
    }

    public String validateHeadingThresh(double value) {
        return new DoubleMinMaxValidator(MIN_HEADING_THRESH, MAX_HEADING_THRESH).validate(value);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        g.setColor(Color.BLACK);
        g.translate(0, renderer.getHeight() - 100);

	double headingDeg = Math.toDegrees(heading);
	double desiredHeadingDeg = Math.toDegrees(desiredHeading);

	String txt = "<html><font color=#000000>";
	txt += "<b>Heading: </b> [" + MathMiscUtils.round(desiredHeadingDeg, 2) + "] <b><font color="
	    + getColor(headingDeg, desiredHeadingDeg, headingThresh) + ">" + MathMiscUtils.round(headingDeg, 2) + "</font></b><br/>";
	txt += "<b>Depth: </b> [" + MathMiscUtils.round(desiredDepth, 2) + "] <b><font color=" + getColor(depth, desiredDepth, depthThresh) + ">" + MathMiscUtils.round(depth, 2) + "</font></b><br/>";
	txt += "<b>Altitude: " + MathMiscUtils.round(altitude, 2) + "</b>";
	txt += "</font></html>";

	info.setText(txt);
	info.setForeground(Color.white);
	info.setHorizontalTextPosition(JLabel.CENTER);
	info.setHorizontalAlignment(JLabel.LEFT);
	info.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));
	info.setBounds(0, 0, 300, 70);
	info.paint(g);
    }

    @Override
    public void initSubPanel() {
        
    }

    @Override
    public void cleanSubPanel() {
        
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
        }
    }
}
