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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Sérgio Ferreira
 * 14 de Dez de 2010
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.Collections;
import java.util.LinkedHashMap;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.containers.MigLayoutContainer;
import pt.lsts.neptus.plugins.MultiSystemIMCMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.lsts.neptus.plugins.uavs.painters.background.UavCoverLayerPainter;
import pt.lsts.neptus.plugins.uavs.painters.elements.UavLabelPainter;
import pt.lsts.neptus.plugins.uavs.painters.elements.UavMissionElementPainter;
import pt.lsts.neptus.plugins.uavs.painters.foreground.UavRulerPainter;

/**
 * Neptus panel which allows the console operator to see the current altitudes of all active vehicles, 
 * detected by the console, and filtered by the selected type. Current use is limited to UAVs
 * 
 * @author canastaman
 * @version 3.0
 * @category UavPanel  
 * 
 */
@PluginDescription(name = "Uav Altitude Panel", icon = "pt/lsts/neptus/plugins/uavs/planning.png", author = "canasta",  version = "3.0", category = CATEGORY.INTERFACE)
public class UavAltitudePanel extends ConsolePanel implements ComponentListener {

    private static final long serialVersionUID = 1L;

    //values are determined empirically to allow operator readability
    private final static int SIDE_PANEL_WIDTH = 25;
    private final static int BOTTOM_LABEL_HEIGHT = 25;
        
    @NeptusProperty(name = "Vehicles detected", category = "UAV", userLevel = LEVEL.REGULAR)
    public String vehicleType = "UAV";
        
    @NeptusProperty(name = "Minimum security vertical distance between UAVs", category = "UAV", userLevel = LEVEL.REGULAR)
    public Integer secAlt = 50;
    
    @NeptusProperty(name = "Panel measuring units", category = "UAV", userLevel = LEVEL.REGULAR)
    public String measure = "SI";
    
    @NeptusProperty(name = "Individual ruler mark width in pixels", category = "UAV", userLevel = LEVEL.REGULAR)
    public Integer markWidth = 5;
    
    //layers to be painted as background for panel's draw area
    private LinkedHashMap<String, IUavPainter> layers;
    
    //arguments passed to each layer in the painting phase, in order to provide them with necessary data to allow rendering
    private LinkedHashMap<String, Object> args;

    //table containing the rendered vehicles and their altitudes
    private LinkedHashMap<String, Integer> vehicleAltitudes;

    //current profile active in the host's MigLayoutPanel
    private String profile;
   
    //listener object which allows the panel to tap into the various IMC messages
    private MultiSystemIMCMessageListener listener;

    public UavAltitudePanel(ConsoleLayout console) {
        super(console);
        
        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    // ------Setters and Getters------//

    // Layers
    private void setLayers(LinkedHashMap<String, IUavPainter> backgroundLayers) {
        this.layers = backgroundLayers;
    }

    // Args
    private void setArgs(LinkedHashMap<String, Object> args) {
        this.args = args;
    }

    // VehicleAltitudes
    private void setVehicleAltitudes(LinkedHashMap<String, Integer> layerArgs) {
        this.vehicleAltitudes = layerArgs;
    }
    
    //Listener
    private void setListener(){
        
        listener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName() + " [" + Integer.toHexString(hashCode()) + "]") {

            @Override
            public void messageArrived(ImcId16 id, IMCMessage msg) {

                //Check if the message is coming from a UAV. Only if it is, do something
                if (ImcSystemsHolder.lookupSystem(id).getTypeVehicle().name().equalsIgnoreCase(vehicleType)){
                    
                    //updates the vehicle's registered altitude
                    vehicleAltitudes.put(ImcSystemsHolder.lookupSystem(id).getName(), msg.getInteger("height") + (-msg.getInteger("z")));
                    repaint();
                }            
            }
        };        
    }

    @Override
    public void initSubPanel() {
        setListener();
        setLayers(new LinkedHashMap<String, IUavPainter>());
        setVehicleAltitudes(new LinkedHashMap<String, Integer>());
        setArgs(new LinkedHashMap<String, Object>());

        // sets up the listener to listen to all vehicles
        listener.setSystemToListen();

        // which messages are listened to
        listener.setMessagesToListen("EstimatedState");

        // sets up all the layers used by the panel
        layers.put("Skybox", new UavCoverLayerPainter("Skybox"));
        layers.put("SidePanel", new UavCoverLayerPainter("SidePanel"));
        layers.put("AltitudeLabel", new UavLabelPainter("AltitudeLabel"));
        layers.put("UavRulerPainter",  new UavRulerPainter("Ruler"));
        layers.put("UavMissionElement", new UavMissionElementPainter("Uavs"));     
                                
        // sets up initial colors for the cover panels
        args.put("Skybox.Color", new Color[] {Color.blue,Color.gray.brighter()});
        args.put("SidePanel.Color",  new Color[] {Color.gray,Color.gray});
        args.put("AltitudeLabel.Color",  new Color[] {Color.gray.brighter(),Color.gray.brighter()});
       
        this.addComponentListener(this);
        
        updateLabelText();
        updatePainterSizes();  
    }

    @Override
    public void cleanSubPanel() {
        listener.clean();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        //determine the active profile before making any calculation
        if(this.getConsole().getMainPanel().getComponent(0).getClass().getSimpleName().equals("MigLayoutContainer"))
            profile = ((MigLayoutContainer) this.getConsole().getMainPanel().getComponent(0)).currentProfile;
        else
            profile = "None";       
          
        //arranges the arguments for the different painter needs
        prepareArgs();

        synchronized (layers) {
            for (IUavPainter layer : layers.values()) {
                Graphics2D gNew = (Graphics2D)g.create();
                layer.paint(gNew, this.getWidth(), this.getHeight(), args);
                gNew.dispose();
            }
            args.clear();
        }        
    }
    
    // ------Specific Methods------//

    /**
     * Method which makes the necessary preparations to the data that is to be sent to the UavPainters in <b>layers</b>. 
     * These preparations depend on the active <b>profile</b>
     * 
     * @return void
     */
    private void prepareArgs() {
                      
        //preparation for UavMissionElementPainter (Uav)
        if (profile.equals("TACO")) {

            // vehicles to draw
            args.put("Uavs.VehicleList", vehicleAltitudes);
        }
        else {

            // single vehicle to draw
            LinkedHashMap<String, Integer> singleUav = new LinkedHashMap<String, Integer>();
            if (vehicleAltitudes.get(this.getMainVehicleId()) != null) {
                singleUav.put(this.getMainVehicleId(), vehicleAltitudes.get(this.getMainVehicleId()));
            }
            args.put("Uavs.VehicleList", singleUav);
        }

        //preparation for UavRulerPainter (Ruler)
        if(!vehicleAltitudes.isEmpty()){
            args.put("Ruler.MaxAlt", Collections.max(vehicleAltitudes.values()));
            args.put("Uavs.MaxAlt", Collections.max(vehicleAltitudes.values()));
        }
    }

    /**
     * Method called when then panel size is altered.
     * 
     * @return void
     */
    private void updatePainterSizes() {
        
        //updates each of the panels draw points
        args.put("Skybox.DrawPoint", new int[] {0, 0});
        args.put("SidePanel.DrawPoint", new int[] {this.getWidth() - SIDE_PANEL_WIDTH, 0});  
        args.put("AltitudeLabel.DrawPoint", new int[] {0, this.getHeight() - BOTTOM_LABEL_HEIGHT});  
        
        //updates each of the panels sizes
        args.put("Skybox.Size", new int[] {this.getWidth() - SIDE_PANEL_WIDTH, this.getHeight() - BOTTOM_LABEL_HEIGHT});
        args.put("SidePanel.Size", new int[] {SIDE_PANEL_WIDTH, this.getHeight()});
        args.put("AltitudeLabel.Size", new int[] {this.getWidth() - SIDE_PANEL_WIDTH, BOTTOM_LABEL_HEIGHT});  
        
        repaint();
    }
    
    /**
     * Method called when the bar measure units are changed.
     * 
     * @return void
     */
    private void updateLabelText() {

        //updates label's text
        if(measure.equals("SI"))
            args.put("AltitudeLabel.Text", "Alt. "+"[m]");
        else if(measure.equals("Imperial"))
            args.put("AltitudeLabel.Text", "Alt. "+"[f]"); 
    }
    
    // ------Listeners------//
    
    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentResized(ComponentEvent e) {
        updatePainterSizes();    
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentMoved(ComponentEvent e) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentShown(ComponentEvent e) {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentHidden(ComponentEvent e) {
        // TODO Auto-generated method stub
        
    }
}
