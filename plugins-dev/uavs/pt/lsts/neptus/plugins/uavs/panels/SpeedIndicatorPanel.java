/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Sérgio Ferreira
 * Apr 24, 2014
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.text.DecimalFormat;

import javax.swing.ImageIcon;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Maneuver;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.MultiSystemIMCMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.util.ImageUtils;


/**
 * Neptus panel designed to show indicated speed and ground speed on the same frame. It allows for the setup of limits for maximum and
 * minimum acceptable velocity.
 * 
 * @author canastaman
 * @version 3.0
 * @category UavPanel  
 * 
 */
@PluginDescription(name = "Speed Indicator Panel", icon = "pt/lsts/neptus/plugins/uavs/speed.png", author = "canasta",  version = "3.0", category = CATEGORY.INTERFACE)
public class SpeedIndicatorPanel extends ConsolePanel implements MainVehicleChangeListener{
    
    private static final long serialVersionUID = 1L;

    @NeptusProperty(name="Minimum Speed", description="Speed below which the vehicle enters VStall (carefull with units)")
    public double minSpeed = 14.0;
    
    @NeptusProperty(name="Maximum Speed", description="Speed above which it's undesirable to fly (carefull with units)")
    public double maxSpeed = 22.0;
    
    @NeptusProperty(name="Units", description="Units of measurement (check for m/s and uncheck for knot)")
    public boolean isSIUnit = true;
    
    static public final double MS_TO_KNOTS_CONV = 1.94384449244;
    
    private final ImageIcon ICON_UP = ImageUtils.getIcon("images/planning/up.png");
        
    //indicates if the UAV as changed plan and needs to update it's command speed value
    private boolean samePlan = false;
    
    private String currentPlan = null;
    private String currentManeuver = null;
    
    private PlanSpecification planSpec = null;
    
    private double aSpeed;
    private double gSpeed;
    private double cSpeed;
    
    //display output formatter
    private DecimalFormat formatter = new DecimalFormat("0.00");
    
    //listener object which allows the panel to tap into the various IMC messages
    private MultiSystemIMCMessageListener listener;

    public SpeedIndicatorPanel(ConsoleLayout console) {
        super(console);
        
        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }
    
    //Listener
    private void setListener(){
        
        listener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName() + " [" + Integer.toHexString(hashCode()) + "]") {

            @Override
            public void messageArrived(ImcId16 id, IMCMessage msg) {

               if(msg.getAbbrev().equals("EstimatedState")){
                   gSpeed = Math.sqrt(
                           msg.getDouble("vx") * msg.getDouble("vx") +
                           msg.getDouble("vy") * msg.getDouble("vy") +
                           msg.getDouble("vz") * msg.getDouble("vz"));
                   
                   aSpeed = Math.sqrt(
                           msg.getDouble("u") * msg.getDouble("u") +
                           msg.getDouble("v") * msg.getDouble("v") +
                           msg.getDouble("w") * msg.getDouble("w"));
               }
               else if(msg.getAbbrev().equals("PlanControlState")){
                   if(msg.getAsNumber("state").longValue() == STATE.EXECUTING.value()){    
                       
                       if(!msg.getAsString("plan_id").equals(currentPlan))
                           samePlan = false;
                       
                       currentPlan = msg.getAsString("plan_id");
                       currentManeuver = msg.getAsString("man_id");
                       
                       if(planSpec != null && samePlan){
                           for(PlanManeuver planMan: planSpec.getManeuvers()){
                               if(planMan.getManeuverId().equals(currentManeuver)){
                                   Maneuver man = planMan.getData();
                                   cSpeed = man.getAsNumber("speed").doubleValue();
                               }
                           }
                       }
                                              
                       if(!samePlan){
                           IMCMessage planControlMessage = IMCDefinition.getInstance().create("PlanControl");
                           planControlMessage.setValue("type", 0);        
                           planControlMessage.setValue("op", "GET");
                           planControlMessage.setValue("request_id",IMCSendMessageUtils.getNextRequestId());
                           
                           IMCSendMessageUtils.sendMessage(planControlMessage, I18n.text("Error requesting plan specificaion"),true, 
                                   getConsole().getMainSystem());
                       }
                   }
                   else{
                       samePlan = false;
                   }
               }
               else if(msg.getAbbrev().equals("PlanControl") && !samePlan){

                   if(msg.getMessage("arg").getAbbrev().equals("PlanSpecification")){
                       planSpec = (PlanSpecification) msg.getMessage("arg");
                       samePlan = true;                      
                   }                   
               }

//               System.out.println("gSpeed: "+formatter.format(gSpeed)+" aSpeed: "+formatter.format(aSpeed)+" cSpeed: "+formatter.format(cSpeed));
               repaint();
            }
        };        
    }

    @Override
    public void initSubPanel() {
        setListener();
//        setLayers(new LinkedHashMap<String, IUavPainter>());
//        setVehicleAltitudes(new LinkedHashMap<String, Integer>());
//        setArgs(new LinkedHashMap<String, Object>());

        // sets up the listener to listen to all vehicles
        listener.setSystemToListen(ImcSystemsHolder.lookupSystemByName(getConsole().getMainSystem()).getId());

        // which messages are listened to
        listener.setMessagesToListen("EstimatedState","PlanControlState","PlanControl");
        
 
        
//        // sets up all the layers used by the panel
//        layers.put("Skybox", new UavCoverLayerPainter("Skybox"));
//        layers.put("SidePanel", new UavCoverLayerPainter("SidePanel"));
//        layers.put("AltitudeLabel", new UavLabelPainter("AltitudeLabel"));
//        layers.put("UavRulerPainter",  new UavRulerPainter("Ruler"));
//        layers.put("UavMissionElement", new UavMissionElementPainter("Uavs"));     
                                
//        // sets up initial colors for the cover panels
//        args.put("Skybox.Color", new Color[] {Color.blue,Color.gray.brighter()});
//        args.put("SidePanel.Color",  new Color[] {Color.gray,Color.gray});
//        args.put("AltitudeLabel.Color",  new Color[] {Color.gray.brighter(),Color.gray.brighter()});
//       
//        this.addComponentListener(this);
//        
//        updateLabelText();
//        updatePainterSizes();  
        
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        listener.setSystemToListen(ImcSystemsHolder.lookupSystemByName(getConsole().getMainSystem()).getId());
    }
    
    @Override
    public void cleanSubPanel() {
        listener.clean();
    }
}
