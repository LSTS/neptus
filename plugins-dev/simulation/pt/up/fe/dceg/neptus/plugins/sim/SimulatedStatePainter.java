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
 * 13/12/2011
 * $Id:: SimulatedStatePainter.java 9880 2013-02-07 15:23:52Z jqcorreia         $:
 */
package pt.up.fe.dceg.neptus.plugins.sim;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.SimulatedState;
import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.google.common.eventbus.Subscribe;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "zp", name = "Simulated State Layer", icon = "pt/up/fe/dceg/neptus/plugins/position/painter/simulator.png")
@LayerPriority(priority = 60)
public class SimulatedStatePainter extends SimpleSubPanel implements Renderer2DPainter {

    /**
     * @param console
     */
    public SimulatedStatePainter(ConsoleLayout console) {
        super(console);
    }

    protected SystemPositionAndAttitude simulatedState = null;
    protected long lastStateMillis = 0;
    protected GeneralPath path = new GeneralPath();
    {
        path.moveTo(0, -10);
        path.lineTo(0, -2);
        path.lineTo(-7, -4);
        path.lineTo(0, 10);
        path.lineTo(7, -4);
        path.lineTo(0, -2);
        path.closePath();
    }

    @Override
    public void initSubPanel() {
        setVisibility(false);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if ((System.currentTimeMillis() - lastStateMillis) > 2000)
            return;

        Point2D pt = renderer.getScreenPosition(simulatedState.getPosition());
        g.translate(pt.getX(), pt.getY());
        g.rotate(-renderer.getRotation() + simulatedState.getYaw() + Math.PI);

        g.setStroke(new BasicStroke(2f));
        g.setColor(Color.cyan.darker().darker());
        g.draw(path);
        g.setColor(Color.cyan.brighter().brighter());
        g.fill(path);
        g.drawString("S", -4, -4);

    }

    @Subscribe
    public void consume(SimulatedState simState) {
        LocationType loc = new LocationType(Math.toDegrees(simState.getLat()), Math.toDegrees(simState.getLon()));
        loc.setHeight(simState.getHeight());
        loc.translatePosition(simState.getX(), simState.getY(), simState.getZ());
        simulatedState = new SystemPositionAndAttitude(loc, simState.getPhi(), simState.getTheta(),
                simState.getPsi());
        lastStateMillis = System.currentTimeMillis();
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
