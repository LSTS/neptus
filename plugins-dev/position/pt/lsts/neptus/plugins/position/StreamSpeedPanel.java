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

import pt.lsts.imc.EstimatedStreamVelocity;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent;
import pt.lsts.neptus.console.plugins.SubPanelChangeListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.ILayerPainter;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.ReflectionUtil;

import com.google.common.eventbus.Subscribe;

/**
 * @author rjpg
 *
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Rui Gonçalves", name = "StreamSpeedPanel", 
		icon = "pt/lsts/neptus/plugins/position/wind.png", 
		description = "Stream Speed Display2")
@LayerPriority(priority=100)
public class StreamSpeedPanel 
extends ConsolePanel  
implements MainVehicleChangeListener, Renderer2DPainter, SubPanelChangeListener {

    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();
	
	
	private boolean initCalled=false;
	
    private double strX, strY;// stream vector

	/**
	 * 
	 */
	public StreamSpeedPanel(ConsoleLayout console) {
	    super(console);
		setVisibility(false);
	}
	
	@Override
	public void initSubPanel() {
		// TODO Auto-generated method stub
		
		renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
		for (ILayerPainter str2d : renderers) {
			str2d.addPostRenderPainter(this, this.getClass().getSimpleName());
		}
		
		if (initCalled)
			return;
		initCalled  = true;
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
		if(true){
			int rWidth = renderer.getWidth();
			double rangle = renderer.getRotation();
			
            double windAngle = Math.atan2(strY, strX);
            rangle -= windAngle;			
			
			// box coordinates
			int width = 70, height = 80;
			int cornerX = (int) (rWidth - 100 - width);
	        int cornerY = 10;
			
	        // Box
	        g2.setColor(new Color(255,255,255,100));
            g2.fillRect(cornerX, cornerY, width, height);
            g2.setColor(Color.BLACK);
            g2.drawRect(cornerX, cornerY, width, height);
            
            // value
            DecimalFormat myFormatter = new DecimalFormat("###.##");
            String output = myFormatter.format(Math.sqrt(strX * strX + strY * strY));

            String text =  output+" m/s";
            Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(text, g2);
            int advance = (int) (width - 20 - stringBounds.getWidth()) / 2;
            g2.drawString(text, advance + cornerX + 10, cornerY + 25);
            
            g2.drawString("wind", 12 + cornerX + 12, cornerY + 11);
            
            //arrow
	        g2.translate(cornerX + width / 2, cornerY + (height)/ 2 + 13);
	        //AffineTransform identity = g2.getTransform();    
	        g2.rotate(-rangle);
	        GeneralPath red = new GeneralPath();
	        red.moveTo(4, -15);
	        red.lineTo(-4, -15);
	        red.lineTo(-5, -5);
	        red.lineTo(5, -5);
	        red.closePath();

	        red.moveTo(6, 5);
	        red.lineTo(-6, 5);
	        red.lineTo(-7, 15);
	        red.lineTo(7, 15);
	        red.closePath();
	        g2.setColor(new Color(255,0,0,150));
	        g2.fill(red);
	        
            GeneralPath white = new GeneralPath();
            white.moveTo(6, 5);
            white.lineTo(-6, 5);
            white.lineTo(-5, -5);
            white.lineTo(5, -5);
            white.closePath();
            g2.setColor(new Color(255,255,255,150));
            g2.fill(white);
            
            GeneralPath outline = new GeneralPath();
            outline.moveTo(-7, 15);
            outline.lineTo(7, 15);
            outline.lineTo(4, -15);
            outline.lineTo(-4, -15);
            outline.closePath();
	       
	        g2.setColor(Color.BLACK);
            g2.draw(outline);
            g2.translate(-(cornerX + width / 2), -(cornerY + (height)/ 2 + 13));
		}
	}
	
	@Override
	public void cleanSubPanel() {

		renderers = getConsole().getSubPanelsOfInterface(ILayerPainter.class);
		for (ILayerPainter str2d : renderers) {
			str2d.removePostRenderPainter(this);
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
