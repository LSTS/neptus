/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by rjpg
 * Oct 26, 2010
 * $Id:: StreamSpeedPanel.java 9834 2013-02-01 17:20:52Z pdias                  $:
 */

package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Vector;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelChangeEvent;
import pt.up.fe.dceg.neptus.console.plugins.SubPanelChangeListener;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.ILayerPainter;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.util.ReflectionUtil;

/**
 * @author rjpg
 *
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Rui Gonçalves", name = "StreamSpeedPanel", 
		icon = "pt/up/fe/dceg/neptus/plugins/position/wind.png", 
		description = "Stream Speed Display2")
@LayerPriority(priority=100)
public class StreamSpeedPanel 
extends SimpleSubPanel  
implements MainVehicleChangeListener, Renderer2DPainter, SubPanelChangeListener, NeptusMessageListener {

    private Vector<ILayerPainter> renderers = new Vector<ILayerPainter>();
	
	
	private boolean initCalled=false;
    private double strX, strY, strZ; // stream vector

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

		addMenuItem("Tools>"+PluginUtils.getPluginName(this.getClass())+">Settings", null, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				PropertiesEditor.editProperties(StreamSpeedPanel.this, getConsole(), true);
			}
		});
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
			
            double windAngle = Math.atan2(strY, strX) + Math.PI;
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
            String output = myFormatter.format(Math.sqrt(strX * strX + strY * strY + strZ * strZ));

            String text =  output+" m/s";
            Rectangle2D stringBounds = g2.getFontMetrics().getStringBounds(text, g2);
            int advance = (int) (width - 20 - stringBounds.getWidth()) / 2;
            g2.drawString(text, advance + cornerX + 10, cornerY + 25);
            
            g2.drawString("wind", 12 + cornerX + 12, cornerY + 11);
            
            //arrow
	        g2.translate(cornerX + width / 2, cornerY + (height)/ 2 + 13);
	        //AffineTransform identity = g2.getTransform();    
	        g2.rotate(-rangle);
	        GeneralPath gp = new GeneralPath();
	        gp.moveTo(0, -15);
	        gp.lineTo(-8, 10);
	        gp.lineTo(0, 7);
	        gp.lineTo(8, 10);
	        gp.closePath();
	            
	        g2.setColor(new Color(0,255,0,200));
	        g2.fill(gp);
	       
	        g2.setColor(Color.BLACK);
            g2.draw(gp);
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

    @Override
    public String[] getObservedMessages() {
        return new String[] { "EstimatedStreamVelocity" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        strX = message.getDouble("x");
        strY = message.getDouble("y");
        strZ = message.getDouble("z");

    }
}