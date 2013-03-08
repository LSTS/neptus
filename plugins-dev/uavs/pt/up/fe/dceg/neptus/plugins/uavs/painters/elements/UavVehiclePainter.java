/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Canasta
 * 5 de Dez de 2010
 * $Id:: UavVehiclePainter.java 9880 2013-02-07 15:23:52Z jqcorreia             $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.elements;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.Enumeration;
import java.util.Hashtable;

import pt.up.fe.dceg.neptus.mp.SystemPositionAndAttitude;
import pt.up.fe.dceg.neptus.plugins.planning.MapPanel;
import pt.up.fe.dceg.neptus.plugins.uavs.UavVehicleIcon;
import pt.up.fe.dceg.neptus.plugins.uavs.daemons.UavPainterDaemon;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;

/**
 * @author Canasta
 *
 */
public class UavVehiclePainter implements Renderer2DPainter, MouseListener{

    public static int WIDTH_RACIO = 10;
    public static int HEIGHT_RACIO = 15;
    
    //predetermined sizes for the associated renderer's icons
    private int vehicleWidth;
    private int vehicleHeight;
    
    private int legendOffset;
	
	private AffineTransform identity = new AffineTransform();
	
	//interface to the Panel that uses this painter
	private MapPanel connectedPanel;
	
	//entity responsible for updating the painter's vehicle details
    private UavPainterDaemon painterDeamon;
	
	//associates the vehicles to their correspondent icons
	private Hashtable<String,UavVehicleIcon> vehicleTable;
	
	public UavVehiclePainter(MapPanel connectedPanel, UavPainterDaemon painterDeamon){
	    setPainterDeamon(painterDeamon);
	    setConnectedPanel(connectedPanel);
	    setVehicleTable(new Hashtable<String,UavVehicleIcon>());
	}
	
	//------Setters and Getters------/
	
	//PainterDeamon
	private void setPainterDeamon(UavPainterDaemon painterDeamon) {
        this.painterDeamon = painterDeamon;
    }

    public UavPainterDaemon getPainterDeamon() {
        return painterDeamon;
    }

	//ConnectedPanel
    private void setConnectedPanel(MapPanel connectedPanel) {
        this.connectedPanel = connectedPanel;
        setVehicleWidth(connectedPanel.getWidth());
        setVehicleHeight(connectedPanel.getHeight());
    }

    public MapPanel getConnectedPanel() {
        return connectedPanel;
    }
    
    //VehicleWidth
    private void setVehicleWidth(int vehicleWidth) {
        this.vehicleWidth = vehicleWidth/WIDTH_RACIO;
        setLegendOffset(this.vehicleWidth);
    }
    
    public int getVehicleWidth() {
        return vehicleWidth;
    }

    //VehicleHeight
    private void setVehicleHeight(int vehicleHeight) {
        this.vehicleHeight = vehicleHeight/HEIGHT_RACIO;
    }
    
    public int getVehicleHeight() {
        return vehicleHeight;
    }

    //LegendOffset
    private void setLegendOffset(int vehicleWidth) {
        this.legendOffset = (vehicleWidth/2)+4;
    }
    
    public int getLegendOffset() {
        return legendOffset;
    }
    
    //VehicleTable
    public void setVehicleTable(Hashtable<String,UavVehicleIcon> vehicleTable) {
        
        String id;
        
        for(Enumeration<String> ids = vehicleTable.keys(); ids.hasMoreElements();){
            id = ids.nextElement();
            vehicleTable.get(id).setWidth(vehicleWidth);
            vehicleTable.get(id).setHeight(vehicleHeight);
            vehicleTable.get(id).buildIcon();
            vehicleTable.get(id).generateIconContactSurface();
        }
        
        this.vehicleTable = vehicleTable;
    }

    public Hashtable<String,UavVehicleIcon> getVehicleTable() {
        return vehicleTable;
    }

    //------Implemented Interfaces------/
	
    //Renderer2DPainter_BEGIN
    @Override
    public void paint(Graphics2D g2, StateRenderer2D renderer) {
        g2.setColor(Color.BLACK);
        
        // Normalizes the graphics transformation and sets the origin at the center of the panel
        determineDrawingOriginPoint(g2, renderer);     
        
        double[] vehiclePos;
        
        // And paints all the vehicles on screen
        for (Enumeration<String> vehicleIDs = vehicleTable.keys(); vehicleIDs.hasMoreElements();) {
            
            String vehicleID = (String) vehicleIDs.nextElement();
            SystemPositionAndAttitude vehicleState = (SystemPositionAndAttitude) renderer.getVehicleState(vehicleID);
            
            vehiclePos = vehicleState.getPosition().getOffsetFrom(renderer.getCenter());

            g2.rotate(renderer.getRotation());

            g2.rotate(-renderer.getRotation());
            // Translates to the centre of the vehicle..
            g2.translate(vehiclePos[1] * renderer.getZoom(), vehiclePos[0] * renderer.getZoom());

            vehicleTable.get(vehicleID).updateIconContactSurface(
                    (connectedPanel.getRendererWidth() / 2) + (vehiclePos[1] * renderer.getZoom()),
                    (connectedPanel.getRendererHeight() / 2) - (vehiclePos[0] * renderer.getZoom()));
            
            // vehicle label
            g2.rotate(-renderer.getRotation());
            g2.scale(1, -1);
            g2.drawString(vehicleID, legendOffset, 0);
            g2.scale(1, -1);
            g2.rotate(renderer.getRotation());

            g2.rotate(-vehicleState.getYaw());
            drawVehicleIcon(g2, vehicleID);
            g2.rotate(vehicleState.getYaw());

            // Sets the current position back at the centre
            g2.translate(-vehiclePos[1] * renderer.getZoom(), -vehiclePos[0] * renderer.getZoom());            
            }
            }
    //Renderer2DPainter_END
    
    //MouseListener_BEGIN
    @Override
    public void mouseClicked(MouseEvent arg0) {
        
        for(Enumeration<String> vehicles = vehicleTable.keys(); vehicles.hasMoreElements();){
            
            String vehicle = vehicles.nextElement();

            if(vehicleTable.get(vehicle).getIconContactSurface().contains((Point2D)arg0.getPoint())){
                if(!connectedPanel.getMainVehicleId().equalsIgnoreCase(vehicle)){
                    connectedPanel.mainVehicleChange(vehicle);
                }
            }
        }     
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }
    //MouseListener_END
    
	//------Specific Methods------/
  

    private void determineDrawingOriginPoint(Graphics2D g, StateRenderer2D renderer) {
        g.setTransform(identity);
        g.translate(renderer.getWidth() / 2, renderer.getHeight() / 2);
        g.scale(1, -1);
    }

    private void drawVehicleIcon(Graphics2D g, String vehicle) {

            g.setComposite(determineTransparency(vehicle));

        g.setColor(vehicleTable.get(vehicle).getAlertLevel());
            g.fill(vehicleTable.get(vehicle).getIcon());

            g.setColor(determineBorderColor(vehicle));
            g.setStroke(determineDrawStroke(vehicle));
            g.draw(vehicleTable.get(vehicle).getIcon());
            g.setColor(Color.black);

            g.setComposite(makeComposite(1.0f));
        }

    private AlphaComposite makeComposite(float alpha) {
        int type = AlphaComposite.SRC_OVER;
        return (AlphaComposite.getInstance(type, alpha));
    }

    private Color determineBorderColor(String vehicle) {
        if (vehicle.equalsIgnoreCase(connectedPanel.getMainVehicleId())) {
            return Color.black;
        }
        else {
            return Color.gray;
        }
    }

    private Composite determineTransparency(String vehicle) {
        if (vehicle.equalsIgnoreCase(connectedPanel.getMainVehicleId())) {
            return makeComposite(1.0f);
        }
        else {
            return makeComposite(0.7f);
        }
    }

    private Stroke determineDrawStroke(String vehicle) {
        if (vehicle.equalsIgnoreCase(connectedPanel.getMainVehicleId())) {
            return new BasicStroke(2.0f);
        }
        else {
            return new BasicStroke(0.5f);
        }
    }
}
