/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Nov 27, 2012
 */
package pt.up.fe.dceg.neptus.plugins.position;

import java.awt.Graphics2D;

import pt.up.fe.dceg.neptus.colormap.ColorMap;
import pt.up.fe.dceg.neptus.colormap.ColorMapFactory;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.GpsFix;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;

import com.google.common.eventbus.Subscribe;

/**
 * @author Margarida Faria
 */
@PluginDescription(name="Stream Speed Overlay")
public class StreamSpeedOverlay extends SimpleSubPanel implements Renderer2DPainter {

    private static final long serialVersionUID = 1L;

    // course over ground in radians
    protected double cogRads = 0;
    
    // speed over ground in m/s
    protected double speedMps = 0;     
    
    protected final double MAX_SPEED = 2; 
    
    public StreamSpeedOverlay(ConsoleLayout console) {
        super(console);
    }
    
    ColorMap cmap = ColorMapFactory.createRedYellowGreenColorMap();
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        // TODO Draw this in a meaningful way...
        g.setColor(cmap.getColor(Math.max(0, MAX_SPEED-speedMps)/MAX_SPEED));
        g.translate(renderer.getWidth()-100, 200);
        g.rotate(cogRads);
        g.drawString(""+(int)speedMps+" m/s", 0, 0);
    }
    
    @Subscribe
    public void consume(GpsFix gpsfix) {
        String sysId = gpsfix.getSourceName();
        // show only for main vehicle
        if (sysId != getConsole().getMainSystem())
            return;
        
        cogRads = gpsfix.getCog();
        speedMps = gpsfix.getSog();        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
