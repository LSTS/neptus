/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: zp
 * 17/05/2016
 */
package pt.lsts.neptus.plugins.videostream;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.mp.preview.payloads.CameraFOV;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.PathElement;

/**
 * @author zp
 *
 */
@PluginDescription(name="Camera Footprint Layer")
public class CamFootprintLayer extends ConsoleLayer {

    @NeptusProperty
    private Color footprintColor = Color.green;
        
    private CameraFOV camFov = null;
    private PathElement groundFootprint = new PathElement();
    private EventMouseLookAt lookAt = null;
    
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {
        groundFootprint.setMyColor(footprintColor);
        setMainVehicle(getConsole().getMainSystem());
    }

    @Override
    public void cleanLayer() {
        
    }
    
    @Subscribe
    public void on(EventMouseLookAt mouseLookAt) {
        if (mouseLookAt.isNull())
            this.lookAt = null;
        else
            this.lookAt = mouseLookAt;
    }
    
    @Subscribe
    public void on(EstimatedState msg) {
        try {
            if (!msg.getSourceName().equals(getConsole().getMainSystem()))
                return;
            
            if (camFov != null) {
                camFov.setState(msg);
                
                ArrayList<LocationType> locs = camFov.getFootprintQuad();
                if (groundFootprint == null) {
                    groundFootprint = new PathElement();
                    groundFootprint.setMyColor(footprintColor);
                }
                groundFootprint.clear();
                groundFootprint.setCenterLocation(locs.get(0));
                for (LocationType l : locs)
                    groundFootprint.addPoint(l);
            }    
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void setMainVehicle(String vehicle) {
        camFov = null;
        groundFootprint = null;
        
        ArrayList<SystemProperty> props = ConfigurationManager.getInstance().getPropertiesByEntity(vehicle, "UAVCamera",
                Visibility.DEVELOPER, Scope.GLOBAL);

        String camModel = "";
        double hAOV = 0, vAOV = 0, camTilt = 0;
        
        for (SystemProperty p : props) {
            if (p.getName().equals("Onboard Camera"))
                camModel = ""+p.getValue();
            else if (p.getName().equals("("+camModel+") Horizontal AOV"))
                hAOV = Math.toRadians(Double.valueOf(""+p.getValue()));
            else if (p.getName().equals("("+camModel+") Vertical AOV"))
                vAOV = Math.toRadians(Double.valueOf(""+p.getValue()));
            else if (p.getName().equals("("+camModel+") Tilt Angle"))
                camTilt = Math.PI/2+Math.toRadians(Double.valueOf(""+p.getValue()));
            
        }
        
        if (!camModel.isEmpty()) {
            camFov = new CameraFOV(hAOV, vAOV);
            camFov.setTilt(camTilt);
            
            NeptusLog.pub().info("Using " + camModel + " camera with " + Math.toDegrees(hAOV) + " x "
                    + Math.toDegrees(vAOV) + " AOV");
        }
        else {
            camFov = CameraFOV.defaultFov();
            NeptusLog.pub().error("Could not calculate camera model for "+vehicle);            
        }
                  
    }
    
    @Subscribe
    public void on(ConsoleEventMainSystemChange evt) {
        setMainVehicle(evt.getCurrent());
    }
    
    public String getInfoHtml() {
        
        String html = "<html>";
        if (camFov == null)
            html += "<font color='red'>FOV not available</font>";
        else {
            html += "<b>Alt: </b>"+(int)camFov.getAltitude()+"<br>";
            html += "<b>Roll: </b>"+(int)Math.toDegrees(camFov.getRoll())+"<br>";
            html += "<b>Pitch: </b>"+(int)Math.toDegrees(camFov.getPitch())+"<br>";
            html += "<b>Yaw: </b>"+(int)Math.toDegrees(camFov.getYaw())+"<br>";    
            html += "<b>Tilt: </b>"+(int)Math.toDegrees(camFov.getTilt())+"<br>";
        }        
        html +="</html>";
        return html;
    }
    
    
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        JLabel lbl = new JLabel(getInfoHtml());
        lbl.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        lbl.setOpaque(true);
        lbl.setBackground(new Color(255,255,255,128));
        lbl.setSize(lbl.getPreferredSize());
        
        g.setTransform(renderer.getIdentity());
        g.translate(10, 10);
        lbl.paint(g);
        g.setTransform(renderer.getIdentity());
        
        if (groundFootprint != null)
            groundFootprint.paint(g, renderer, renderer.getRotation());   
        
        if (lookAt != null) {
            g.setTransform(renderer.getIdentity());
            Point2D pt = renderer.getScreenPosition(lookAt);
            g.setColor(Color.white);
            g.fill(new Ellipse2D.Double(pt.getX()-5, pt.getY()-5, 10, 10));
            g.setColor(Color.red);
            g.fill(new Ellipse2D.Double(pt.getX()-2.5, pt.getY()-2.5, 5, 5));
        }
    }
}
