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
 * Jul 12, 2012
 * $Id:: AisOverlay.java 9615 2012-12-30 23:08:28Z pdias                        $:
 */
package pt.up.fe.dceg.neptus.plugins.ais;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Vector;

import javax.swing.JPopupMenu;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.plugins.update.PeriodicUpdatesService;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "ZP", name = "AIS Overlay")
public class AisOverlay extends SimpleRendererInteraction implements IPeriodicUpdates {
    private static final long serialVersionUID = 1L;

    @NeptusProperty
    public boolean showNames = true;

    @NeptusProperty
    public boolean showSpeeds = true;

    @NeptusProperty
    public long updateMillis = 60000;

    @NeptusProperty
    public boolean showOnlyWhenInteractionIsActive = true;

    @NeptusProperty
    public boolean showStoppedShips = false;

    protected boolean active = false;
    protected Vector<ShipInfo> shipsOnMap = new Vector<ShipInfo>();
    protected StateRenderer2D renderer = null;

    /**
     * @param console
     */
    public AisOverlay(ConsoleLayout console) {
        super(console);
    }

    @Override
    public boolean isExclusive() {
        return true;
    }

    @Override
    public void setActive(boolean mode, StateRenderer2D source) {
        super.setActive(mode, source);
        active = mode;
        if (active)
            update();
    }

    @Override
    public String getName() {
        return super.getName();
    }

    @Override
    public long millisBetweenUpdates() {
        return updateMillis;
    }

    protected Thread lastThread = null;

    @Override
    public boolean update() {

        System.out.println("AIS update...");

        if (showOnlyWhenInteractionIsActive && !active)
            return true;

        // don't let more than one thread be running at a time
        if (lastThread != null)
            return true;

        lastThread = new Thread() {
            public void run() {
                if (renderer == null)
                    return;

                LocationType topLeft = renderer.getTopLeftLocationType();
                LocationType bottomRight = renderer.getBottomRightLocationType();

                shipsOnMap = ShipInfo.getShips(bottomRight.getLatitudeAsDoubleValue(),
                        topLeft.getLongitudeAsDoubleValue(), topLeft.getLatitudeAsDoubleValue(),
                        bottomRight.getLongitudeAsDoubleValue(), showStoppedShips);
                lastThread = null;

                renderer.repaint();
                System.out.println("FINISHED! Found " + shipsOnMap.size() + " boats");
            };
        };
        lastThread.start();

        return true;
    }

    @Override
    public void cleanSubPanel() {
        PeriodicUpdatesService.unregister(this);
        shipsOnMap.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.SimpleRendererInteraction#mouseClicked(java.awt.event.MouseEvent,
     * pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void mouseClicked(MouseEvent event, StateRenderer2D source) {
        super.mouseClicked(event, source);

        JPopupMenu popup = new JPopupMenu();
        popup.add("Settings").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(AisOverlay.this, getConsole(), true);
            }
        });

        popup.add("Update now").addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                update();
            }
        });

        popup.show(source, event.getX(), event.getY());
    }

    protected GeneralPath path = new GeneralPath();
    {
        path.moveTo(-3, 4);
        path.lineTo(0, -5);
        path.lineTo(3, 4);
        path.lineTo(-3, 4);
        path.closePath();
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);
        this.renderer = renderer;

        if (showOnlyWhenInteractionIsActive && !active)
            return;

        if (lastThread != null) {
            g.drawString("Updating AIS layer...", 10, 15);
        }

        for (ShipInfo ship : shipsOnMap) {
            LocationType shipLoc = ship.getLocation();
            Point2D pt = renderer.getScreenPosition(shipLoc);

            g.translate(pt.getX(), pt.getY());

            if (showNames) {
                g.setColor(Color.red.darker().darker());
                g.drawString(ship.getName(), 5, 5);
            }

            if (showSpeeds) {
                g.setColor(Color.black);
                g.drawString(GuiUtils.getNeptusDecimalFormat(1).format(ship.getSpeedMps()) + " m/s", 5, 15);
            }

            g.setColor(Color.red);
            if (ship.getSpeedMps() == 0) {
                g.fill(new Ellipse2D.Double(-3, -3, 6, 6));
            }
            else {
                g.rotate(ship.getHeadingRads());
                g.fill(path);
                g.rotate(-ship.getHeadingRads());
            }

            g.translate(-pt.getX(), -pt.getY());
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

}
