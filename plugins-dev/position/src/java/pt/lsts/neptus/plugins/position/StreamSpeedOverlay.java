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
 * Oct 26, 2010
 */
package pt.lsts.neptus.plugins.position;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Vector;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EstimatedStreamVelocity;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent;
import pt.lsts.neptus.console.plugins.SubPanelChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.ReflectionUtil;

/**
 * @author rjpg
 *
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Rui Gonçalves", name = "Stream Speed Overlay", icon = "pt/lsts/neptus/plugins/position/wind.png", 
description = "Stream speed overlay. Wind or water dependent on the type of vehicle.")
@LayerPriority(priority = 100)
public class StreamSpeedOverlay extends ConsolePanel
        implements MainVehicleChangeListener, Renderer2DPainter, SubPanelChangeListener {

    private static final Color COLOR_STRIPES_OUTLINE = new Color(255, 255, 255, 150);
    private static final Color COLOR_STRIPES_WIND = new Color(255, 0, 0, 150);
    private static final Color COLOR_STRIPES_WATER = new Color(0, 0, 255, 150).darker();
    
    private static final String MPS_STRING = "m/s";
    private static final String WIND_STRING = I18n.textc("wind", "Keep the text relatively small");
    private static final String WATTER_STRING = I18n.textc("water", "Keep the text relatively small");

    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();

    private DecimalFormat myFormatter = new DecimalFormat("###.##");

	private boolean initCalled = false;
    private double strX, strY;// stream vector

    private String labelText = WIND_STRING;
    
    private GeneralPath windsockRedStripes;
    private GeneralPath windsockWhiteStripes;
    private GeneralPath windsockOutline;
    
	public StreamSpeedOverlay(ConsoleLayout console) {
	    super(console);
		setVisibility(false);
		
        windsockRedStripes = new GeneralPath();
        windsockRedStripes.moveTo(4, -15);
        windsockRedStripes.lineTo(-4, -15);
        windsockRedStripes.lineTo(-5, -5);
        windsockRedStripes.lineTo(5, -5);
        windsockRedStripes.closePath();

        windsockRedStripes.moveTo(6, 5);
        windsockRedStripes.lineTo(-6, 5);
        windsockRedStripes.lineTo(-7, 15);
        windsockRedStripes.lineTo(7, 15);
        windsockRedStripes.closePath();

        windsockWhiteStripes = new GeneralPath();
        windsockWhiteStripes.moveTo(6, 5);
        windsockWhiteStripes.lineTo(-6, 5);
        windsockWhiteStripes.lineTo(-5, -5);
        windsockWhiteStripes.lineTo(5, -5);
        windsockWhiteStripes.closePath();

        windsockOutline = new GeneralPath();
        windsockOutline.moveTo(-7, 15);
        windsockOutline.lineTo(7, 15);
        windsockOutline.lineTo(4, -15);
        windsockOutline.lineTo(-4, -15);
        windsockOutline.closePath();
	}
	
	@Override
	public void initSubPanel() {
		renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
		for (ILayerPainter str2d : renderers) {
			str2d.addPostRenderPainter(this, this.getClass().getSimpleName());
		}
		
		if (initCalled)
			return;
		initCalled  = true;

		setupLabel();
		strX = strY = Double.NaN;
	}
		
	@Override
	public void subPanelChanged(SubPanelChangeEvent panelChange) {
		if (panelChange == null)
			return;

		if (ReflectionUtil.hasInterface(panelChange.getPanel().getClass(),
				ILayerPainter.class)) {

			ILayerPainter sub = (ILayerPainter) panelChange.getPanel();

			if (panelChange.added()) {
				renderers.add(sub);
				ILayerPainter str2d = sub;
				if (str2d != null) {
					str2d.addPostRenderPainter(this, "Stream Speed");
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
		int rWidth = renderer.getWidth();
		double rangle = renderer.getRotation();
		
        double windAngle = 0;
        String speedStr = "?";
        if (Double.isFinite(strX) && Double.isFinite(strY)) {
            windAngle = Math.atan2(strY, strX);
            speedStr = myFormatter.format(Math.sqrt(strX * strX + strY * strY));
        }

        rangle -= windAngle;			
		
		// Box coordinates
		int width = 70, height = 80;
		int cornerX = (int) (rWidth - 100 - width);
        int cornerY = 10;
		
        // Box
        g2.setColor(new Color(255,255,255,100));
        g2.fillRect(cornerX, cornerY, width, height);
        g2.setColor(Color.BLACK);
        g2.drawRect(cornerX, cornerY, width, height);
        
        String text =  speedStr + " " + MPS_STRING;
        Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(text, g2);
        int advance = (int) (width - 20 - stringBounds.getWidth()) / 2;
        g2.drawString(text, advance + cornerX + 10, cornerY + 25);
        
        g2.drawString(labelText, 12 + cornerX + 12, cornerY + 11);
        
        // Draw icon
        g2.translate(cornerX + width / 2, cornerY + (height)/ 2 + 13);
        
        g2.rotate(-rangle);
        
        if (WATTER_STRING.equalsIgnoreCase(labelText))
            g2.setColor(COLOR_STRIPES_WATER);
        else
            g2.setColor(COLOR_STRIPES_WIND);
        g2.fill(windsockRedStripes);

        g2.setColor(COLOR_STRIPES_OUTLINE);
        g2.fill(windsockWhiteStripes);
        
        g2.setColor(Color.BLACK);
        g2.draw(windsockOutline);
        g2.translate(-(cornerX + width / 2), -(cornerY + (height) / 2 + 13));
	}
	
	@Override
	public void cleanSubPanel() {
		renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
		for (ILayerPainter str2d : renderers) {
			str2d.removePostRenderPainter(this);
		}
	}

	@Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
	    setupLabel();
	    strX = strY = Double.NaN;
    }

    private void setupLabel() {
        String mvid = getMainVehicleId();
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(mvid);
	    if (sys != null) {
	        switch (sys.getTypeVehicle()) {
                case USV:
                case UUV:
                    labelText = WATTER_STRING;
                    break;
                default:
                    labelText = WIND_STRING;
                    break;
            }
	    }
    }
	
    @Subscribe
    public void on(EstimatedStreamVelocity msg) {
        if(!msg.getSourceName().equals(getConsole().getMainSystem()))
            return;
        strX = msg.getX();
        strY = msg.getY();
    }
}
