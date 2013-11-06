/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Christian Fuchs
 * 21.11.2012
 */
package pt.up.fe.dceg.neptus.plugins.uavs.painters.elements;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleSubPanel;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.imc.EstimatedState;

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
