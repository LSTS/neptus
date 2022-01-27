/*
 * Copyright (c) 2004-2021 
 * Author: Nikolai Lauvås
 */
package pt.lsts.neptus.plugins.planprobspecinterface;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import java.util.Vector;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;


import pt.lsts.imc.PlanProbSpec;
import pt.lsts.imc.PolygonVertex;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleRendererInteraction;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.renderer2d.StateRendererInteraction;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Nikolai Lauvås
 * 
 */
@PluginDescription(name = "Online Planner - Shortest Path", icon = "pt/lsts/neptus/plugins/planprobspecinterface/shortest.png")
public class ShortestPathPlanner extends SimpleRendererInteraction implements Renderer2DPainter,
        StateRendererInteraction {


    public LocationType destination = null;
    public LocationType initial = null;
    public LocationType bottomLeft = null;
    public LocationType topRight = null;

    protected boolean isActive;

    @NeptusProperty(name = "Default Speed (m/s)")
    public double defaultSpeed = 1.0;

    @NeptusProperty(name = "Vehicle(-1=current)")
    public int vehicle = 0x2810;

    @NeptusProperty(name = "Custom Parameters")
    public String customparameters = "t=60.0;p=1;a=1;";
    /**
     * @param console
     */
    public ShortestPathPlanner(ConsoleLayout console) {
        super(console);
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        isActive = mode;
    }

    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {

        final Point2D mousePosition = event.getPoint();
        final StateRenderer2D renderer = source;

        if (event.getButton() == MouseEvent.BUTTON3) {
            JPopupMenu menu = new JPopupMenu();
            menu.add("Define boundary's bottom left").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    bottomLeft = renderer.getRealWorldLocation(mousePosition);
                }
            });

            menu.add("Define boundary's top right").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    topRight = renderer.getRealWorldLocation(mousePosition);
                }
            });
            menu.add("Define initial location").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    initial = renderer.getRealWorldLocation(mousePosition);
                }
            });

            menu.add("Define destination location").addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    destination = renderer.getRealWorldLocation(mousePosition);
                }
            });

            menu.addSeparator();

            menu.add("Shortest Path settings").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    PropertiesEditor.editProperties(ShortestPathPlanner.this, true);

                }
            });

            menu.addSeparator();

            menu.add("Generate plan").addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    generatePlan();
                }
            });

            menu.show(source, (int) mousePosition.getX(), (int) mousePosition.getY());
        }
    }

    protected void generatePlan() {
        PlanProbSpec spec = new PlanProbSpec();
        spec.setVehicle(vehicle);
        short test = 1;
        spec.setProblemTypeVal(test);
        spec.setSpeed(defaultSpeed);

        
        spec.setStartLat(initial.getLatitudeRads());
        spec.setStartLon(initial.getLongitudeRads());
        spec.setEndLat(destination.getLatitudeRads());
        spec.setEndLon(destination.getLongitudeRads());
        Vector<PolygonVertex> area = new Vector<>();
        area.add(new PolygonVertex(bottomLeft.getLatitudeRads(), bottomLeft.getLongitudeRads()));
        area.add(new PolygonVertex(topRight.getLatitudeRads(), topRight.getLongitudeRads()));
        spec.setArea(area);
        spec.setCustom(customparameters);
        

        send(spec);
        NeptusLog.pub().info("Sent feasible path request to vehicle");
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {

        if (destination != null) {
            Point2D point = renderer.getScreenPosition(destination);
            if (isActive)
                g.setColor(Color.red);
            else
                g.setColor(Color.red.darker().darker());

            g.fill(new Ellipse2D.Double(point.getX() - 5, point.getY() - 5, 10, 10));
            g.draw(new Ellipse2D.Double(point.getX() - 5, point.getY() - 5, 10, 10));
        }

        g.setColor(Color.green.darker());
        g.setStroke(new BasicStroke(4.0f));

        if (initial != null) {
            Point2D point = renderer.getScreenPosition(initial);
            if (isActive)
                g.setColor(Color.green);
            else
                g.setColor(Color.green.darker().darker());

            g.fill(new Ellipse2D.Double(point.getX() - 5, point.getY() - 5, 10, 10));
            g.draw(new Ellipse2D.Double(point.getX() - 5, point.getY() - 5, 10, 10));
        }

        if (isActive) {
            if (bottomLeft != null) {
                Point2D point = renderer.getScreenPosition(bottomLeft);
                g.draw(new Line2D.Double(point.getX(), point.getY(), point.getX() + 20, point.getY()));
                g.draw(new Line2D.Double(point.getX(), point.getY(), point.getX(), point.getY() - 20));
            }

            if (topRight != null) {
                Point2D point = renderer.getScreenPosition(topRight);
                g.draw(new Line2D.Double(point.getX() - 20, point.getY(), point.getX(), point.getY()));
                g.draw(new Line2D.Double(point.getX(), point.getY(), point.getX(), point.getY() + 20));
            }
        }

        g.setColor(Color.black);
        g.setStroke(new BasicStroke(1.0f));
        if (topRight != null && bottomLeft != null) {
            Point2D topo = renderer.getScreenPosition(topRight);
            Point2D fundo = renderer.getScreenPosition(bottomLeft);

            double x = Math.min(topo.getX(), fundo.getX());
            double y = Math.min(topo.getY(), fundo.getY());

            double w = Math.abs(topo.getX() - fundo.getX());
            double h = Math.abs(topo.getY() - fundo.getY());

            g.draw(new Rectangle2D.Double(x, y, w, h));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
