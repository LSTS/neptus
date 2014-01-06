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
 * 22.10.2012
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.Timer;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.plugins.MultiSystemIMCMessageListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleSubPanel;
import pt.lsts.neptus.plugins.uavs.IndicatorButton;
import pt.lsts.neptus.plugins.uavs.UavPaintersBag;
import pt.lsts.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.lsts.neptus.plugins.uavs.painters.elements.UavStateIndicatorPainter;

import com.google.common.eventbus.Subscribe;

/**
 * @author Christian Fuchs
 * @version 0.2
 * @category UavPanel
 * Neptus panel which indicates the UAV's state through a "green light" for each subsystem
 */

@PluginDescription(name="Uav State 1 Panel", icon="pt/lsts/neptus/plugins/uavs/planning.png", author="Christian Fuchs")
public class UavState1Panel extends SimpleSubPanel{
    
//--------------declarations-----------------------------------//
    
    private static final long serialVersionUID = 1L;
    
    // hashtable containing the arguments needed for painting
    private Hashtable<String,IndicatorButton> args;
    
    // hashtable containing the names and states of all subsystems/entities
    private Hashtable<String,Integer> entityStates;
    
    // hashtable containing the names of all subsystems/entities and a longer description of the state
    private Hashtable<String,String> entityStatesDescriptions;
    
    // different layers to be painted on top of the panel's draw area
    private UavPaintersBag layers;
    
    // timer that periodically updates the panel
    private Timer timer;
    
    // hashtable that contains the last time a message was recieved for each UAV
    private Hashtable<String,Long> lastContact;
    
    // needed to stop painting until messages arrive
    private boolean doPaint;
    
    private MultiSystemIMCMessageListener listener;
    
//--------------end of declarations----------------------------//
    
    public UavState1Panel(ConsoleLayout console){  
        super(console);    
        
        //clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }
    
//--------------Setters and Getters----------------------------//
    
    //Layers
    private void setLayers(UavPaintersBag layers) {
        this.layers = layers;
    }
    
    //Args
    private void setArgs(Hashtable<String, IndicatorButton> args) {
        this.args = args;
    }
    
    //EntityStates
    private void setEntityStates(Hashtable<String, Integer> args) {
        this.entityStates = args;
    }
    
    //EntityStatesDescriptions
    private void setEntityStatesDescriptions(Hashtable<String,String> entityStatesDescriptions){
        this.entityStatesDescriptions = entityStatesDescriptions;
    }
    
    private void setTimer(Timer timer){
        this.timer = timer;
    }
    
    //LastContact
    private void setLastContact(Hashtable<String, Long> args) {
        this.lastContact = args;
    }
    
//--------------End of Setters and Getters---------------------//    
    
//--------------start of IMC message stuff---------------------//
    
    /**
     * 
     */
    private void initializeMultiSystemMessageListener() {
        //listener object which allows the panel to tap into the various IMC messages
        listener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName()
                + " [" + Integer.toHexString(hashCode()) + "]") {
            
            @Override
            public void messageArrived(ImcId16 id, IMCMessage message) {
                // Check if the message is coming from a UAV. Only if it is, do something
                if (ImcSystemsHolder.lookupSystem(id).getTypeVehicle().name().equalsIgnoreCase("UAV")){
                    if(message.getAbbrev().equals("Heartbeat")){
                        lastContact.put(getMainVehicleId(), System.currentTimeMillis());
                        entityStates.put("Heartbeat", 1);
                        entityStatesDescriptions.put("Heartbeat", "Recieving Dune Telemetry");
                        
                        // once messages arrive, start painting
                        doPaint = true;
                    }
                    else if(EntitiesResolver.resolveName(getMainVehicleId(), message.getHeader().getInteger("src_ent")) != null){
                        // check if the entities resolver gives null - this may happen when the whole message stuff is not yet fully initialized
                        // after a few seconds it should not matter anymore
                        entityStates.put(EntitiesResolver.resolveName(getMainVehicleId(), message.getHeader().getInteger("src_ent")), message.getInteger("state"));
                        entityStatesDescriptions.put(EntitiesResolver.resolveName(getMainVehicleId(), message.getHeader().getInteger("src_ent")), message.getString("description"));
                        
                    }
                }
                
            }
        };
    }

//--------------end of IMC message stuff-----------------------//
    
    @Override
    public void initSubPanel(){
        initializeMultiSystemMessageListener();
        
        //sets up the listener to listen to the main vehicle
        listener.setSystemToListen(ImcSystemsHolder.getSystemWithName(getMainVehicleId()).getId());
        
        //which messages are listened to
        listener.setMessagesToListen("EntityState", "Heartbeat");
        
        // Initialize required items
        setArgs(new Hashtable<String,IndicatorButton>());
        setEntityStates(new Hashtable<String,Integer>());
        setEntityStatesDescriptions(new Hashtable<String,String>());
        setLayers(new UavPaintersBag());
        setLastContact(new Hashtable<String,Long>());
        doPaint = false;
        
        // create the timer and its actionlistener
        // every 1000 milliseconds, it will repaint the panel
        setTimer(new Timer(1000, new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                
                repaint();
                
            }
            
        }));
        
        // sets up all the layers used by the panel
        addLayer("State Indicator Painter", 1, new UavStateIndicatorPainter(), 0);
        
        timer.start();
        
    }
    
    @Override
    public void cleanSubPanel() {
        listener.clean();
        listener = null;
    }
    
    // Method adds a new painter layer to the UavPainterBag
    public void addLayer(String name, Integer priority, IUavPainter layer, int cacheMillis) {
        this.layers.addPainter(name, layer, priority, cacheMillis);
    }
    
    // for each entity this creates a new indicatorButton if none is present
    // also sets the correct state for all indicatorButtons
    private void prepareArgs(){
        
        // if the last communication is older than 3 seconds, set "Hearbeat" to error
        if ((System.currentTimeMillis() - lastContact.get(getMainVehicleId())) > 3000 ){
            entityStates.put("Heartbeat", 4);
            entityStatesDescriptions.put("Heartbeat", "No Dune Telemetry for more than 3 seconds");
        }
        
        for(String entity: entityStates.keySet()){
            
            if (args.get(entity) == null){
                
                IndicatorButton button = new IndicatorButton((entity), false);
                args.put(entity, button);
                add(button);
            }
            
            args.get(entity).setState(stateToString(entityStates.get(entity)));
            args.get(entity).setDescription(entityStatesDescriptions.get(entity));
        }
        
    }
    
    // returns a string representation of the state
    private String stateToString(Integer intState){
        switch (intState){
            case 0:
                return "BOOT";
            case 1:
                return "NORMAL";
            case 2:
                return "FAULT";
            case 3:
                return "ERROR";
            case 4:
                return "FAILURE";
            default:
                return "FAILURE";
        }
    }
    
    @Override
    protected void paintComponent(Graphics g){
        
        // if doPaint is false, don't paint
        if(!doPaint){
            return;
        }
        
        super.paintComponent(g);
        
        prepareArgs();
        
        synchronized (layers) {
            for (IUavPainter layer : layers.getPostRenderPainters()) {
                Graphics2D gNew = (Graphics2D)g.create();
                layer.paint(gNew, this.getWidth(), this.getHeight(), args);
                gNew.dispose();
            }
        }
    }
    
    // listen for main vehicle change events
    @Subscribe
    public void onMainVehicleChange(ConsoleEventMainSystemChange e){
        
        //point the listener to the main vehicle
        listener.setSystemToListen(ImcSystemsHolder.getSystemWithName(getMainVehicleId()).getId());
    }
    
}
