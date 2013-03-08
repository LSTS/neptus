/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Christian Fuchs
 * 21.11.2012
 * $Id:: UavCameraFootprint.java 9846 2013-02-02 03:32:12Z robot                $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.elements;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.google.common.eventbus.Subscribe;

/**
 * @author Christian Fuchs
 *
 */

@PluginDescription(name = "Uav Camera Footprint", author = "Christian Fuchs")
public class UavCameraFootprint extends SimpleSubPanel implements Renderer2DPainter {

//--------------declarations-----------------------------------//   
    private static final long serialVersionUID = 1L;
    
    // used to save the estimated state as recieved from the vehicle
    private EstimatedState state = null;
    
    // the field of view of the camere
    //TODO somehow make this configurable (config file or something)
    private double fieldOfView = Math.toRadians(60);
    
    // used to (de)activate painting
    private boolean doPaint = false;
    
//--------------end of declarations----------------------------//

    public UavCameraFootprint(ConsoleLayout console) {
        super(console);
        
        //clears all the unused initializations of the standard SimpleSubPanel
        removeAll();    
    }
    
//--------------Setters and Getters----------------------------//
    
    public void setDoPaint(boolean doPaint){
        this.doPaint = doPaint;
    }
    
//--------------End of Setters and Getters---------------------//
    
//--------------start of IMC message stuff---------------------//
    
    @Subscribe
    public void estimatedState(EstimatedState state) {
       
        // if the message is not from the main vehicle, skip the rest of the method
        if (!state.getSourceName().equals(getConsole().getMainSystem())){
            return;
        }
         
        this.state = state;
        
    }
    
//--------------end of IMC message stuff-----------------------//

    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        
        // check if state is empty, if it is just don't do anything
        if ((state == null) || (!doPaint)){
            return;
        }
        
        // compute the length of the footprint's square's sides
        int length = (int) (2 * (state.getHeight() - state.getZ()) * Math.tan(fieldOfView / 2));
        
        // translate the length of the square's sides to the correct size for the map
        // getZoom() gives pixels/meter in the map
        double zoom = renderer.getZoom();
        int R = (int) (length * zoom);
        
        // compute the offset due to rotation of the UAV and the camera in meters first
        // mind the directions!!!
        // u and v are body centered to the front and right respectively
        // theta is positive for upwards pitch and phi is positive for a roll to the right
        int offU = (int) (state.getHeight() * Math.tan(state.getTheta()));
        int offV = - (int) (state.getHeight() * Math.tan(state.getPhi()));
        
        // translate the offsets to the correct size for the map
        offU = (int) (offU * zoom);
        offV = (int) (offV * zoom);
        
        // Move to the location of the UAV on the map and rotate the same way as the map does
        LocationType loc = new LocationType();
        loc.setLatitudeRads(state.getLat());
        loc.setLongitudeRads(state.getLon());
        loc.setHeight(state.getHeight());
        loc.translatePosition(state.getX(), state.getY(), state.getZ());
        Point2D pt = renderer.getScreenPosition(loc);
        
        // move to the location of the UAV
        g.translate(pt.getX(), pt.getY());
        
        // rotate once again to correct for the UAVs heading
        g.rotate(state.getPsi());
        
        // move according to the offsets offU and offV
        // again, mind the directions... The last rotation aligned the graphics with the body centered
        // reference frame to x = v and y = -u
        g.translate(offV, - offU);
        
        // rotate the same as the map
        g.rotate(renderer.getRotation());
        
        g.setColor(Color.gray.brighter());
        
        AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.75f);
        g.setComposite(composite);
        
        // draw the indicator
        g.fillRoundRect(- R/2, - R/2, R, R, R/4, R/4);
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
