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
 * 26.10.2012
 */
package pt.up.fe.dceg.neptus.plugins.uavs.panels;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.Timer;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.MultiSystemIMCMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.uavs.IndicatorButton;
import pt.up.fe.dceg.neptus.plugins.uavs.UavPaintersBag;
import pt.up.fe.dceg.neptus.plugins.uavs.interfaces.IUavPainter;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.elements.UavStateIndicatorPainter;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.EntitiesResolver;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author Christian Fuchs
 * @version 0.3
 * Neptus panel which indicates the UAV's state through a "green light" for each UAV
 */

@PluginDescription(name="Uav State X Panel", icon="pt/up/fe/dceg/neptus/plugins/uavs/planning.png", author="Christian Fuchs")
public class UavStateXPanel extends SimpleSubPanel{

  //--------------declarations-----------------------------------//    
    
    private static final long serialVersionUID = 1L;
    
    // hashtable containing the arguments needed for painting
    private Hashtable<String,IndicatorButton> args;
    
    // hashtable containing all entities of all UAVs
    // a bit tricky to store both the UAV's and the entity's name, so the format is as follows:
    // <UAVNamme>--<EntityName>, <EntityStatus>
    private Hashtable<String, Integer> uavStatus;
    
    // hashtable containing all entities of all UAVs and a string description
    // the format is the same as for uavStatus:
    // UAVNamme>--<EntityName>, <EntityStatusDescription>
    private Hashtable<String, String> uavStatusDescription;
    
    // different layers to be painted on top of the panel's draw area
    private UavPaintersBag layers;
    
    // timer that periodically updates the panel
    private Timer timer;
    
    // hashtable that contains the last time a message was recieved for each UAV
    private Hashtable<String,Long> lastContact;
    
//--------------end of declarations----------------------------//
    
    public UavStateXPanel(ConsoleLayout console){  
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
    
    //uavStatus
    private void setUavStatus(Hashtable<String, Integer> uavStatus){
        this.uavStatus = uavStatus;
    }
    
    //uavStatusDescription
    private void setUavStatusDescription(Hashtable<String, String> uavStatusDescription){
        this.uavStatusDescription = uavStatusDescription;
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
    
  //listener object which allows the panel to tap into the various IMC messages
    private MultiSystemIMCMessageListener listener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName()
            + " [" + Integer.toHexString(hashCode()) + "]") {

        @Override
        public void messageArrived(ImcId16 id, IMCMessage message) {
            
            // Check if the message is coming from a UAV. Only if it is, do something
            if (ImcSystemsHolder.lookupSystem(id).getTypeVehicle().name().equalsIgnoreCase("UAV")){
                
                if(message.getAbbrev().equals("Heartbeat")){

                    lastContact.put(ImcSystemsHolder.lookupSystem(id).getName(), System.currentTimeMillis());
                    uavStatus.put(ImcSystemsHolder.lookupSystem(id).getName() + "--Heartbeat", 1);
                    uavStatusDescription.put(ImcSystemsHolder.lookupSystem(id).getName() + "--Heartbeat", "Recieving Dune Telemetry");
                    
                }
                
                // check if the entities resolver gives null - this may happen when the whole message stuff is not yet fully initialized
                // after a few seconds it should not matter anymore
                else if (EntitiesResolver.resolveName(ImcSystemsHolder.lookupSystem(id).getName(), message.getHeader().getInteger("src_ent")) != null){
                    
                    //updates the vehicles' entity states
                    uavStatus.put(ImcSystemsHolder.lookupSystem(id).getName() + "--" + EntitiesResolver.resolveName(ImcSystemsHolder.lookupSystem(id).getName(), message.getHeader().getInteger("src_ent")), message.getInteger("state"));     
                    uavStatusDescription.put(ImcSystemsHolder.lookupSystem(id).getName() + "--" + EntitiesResolver.resolveName(ImcSystemsHolder.lookupSystem(id).getName(), message.getHeader().getInteger("src_ent")), message.getString("description"));
                    
//                    repaint();
                    
                }
            } 
        }
    };
    
  //--------------end of IMC message stuff-----------------------//

    @Override
    public void initSubPanel(){
        
        //sets up the listener to listen to all vehicles 
        listener.setSystemToListen();
        
        //which messages are listened to
        listener.setMessagesToListen("EntityState", "Heartbeat");
        
        // Initialize required items
        setArgs(new Hashtable<String,IndicatorButton>());
        setUavStatus(new Hashtable<String,Integer>());
        setUavStatusDescription(new Hashtable<String,String>());
        setLayers(new UavPaintersBag());
        setLastContact(new Hashtable<String,Long>());
        
        // sets up all the layers used by the panel
        addLayer("State Indicator Painter", 1, new UavStateIndicatorPainter(), 0);
        
        // create the timer and its actionlistener
        // every 1000 milliseconds, it will repaint the panel
        setTimer(new Timer(1000, new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent arg0) {
                
                repaint();
                
            }
            
        }));
        
        timer.start();
        
    }
    
    @Override
    public void cleanSubPanel(){
        listener.clean();
    }
    
    // Method adds a new painter layer to the UavPainterBag
    public void addLayer(String name, Integer priority, IUavPainter layer, int cacheMillis) {
        this.layers.addPainter(name, layer, priority, cacheMillis);
    }
    
    // Method that reads the UAV's name from uavStatus and finds out whether any entity has a not normal status
    private void prepareArgs(){
        
        // two Strings to store the current UAV's and entity's name in the loop
        String uavName;
        String entityName;
        
        // set the status for all UAVs to NORMAL
        // if any entity is non-normal, this will be fixed in the next loop
        for(String uav: args.keySet()){
            args.get(uav).setState("NORMAL");
        }
        
        // loop over all entries in uavStatus, i.e. all combinations of UAV and entity name
        for(String entity: uavStatus.keySet()){
            
            // get the UAV's and entity's name
            uavName = findUavName(entity);
            entityName = findEntityName(entity);
            
            // if the last communication is older than 3 seconds, set "Hearbeat" to error
            if ((System.currentTimeMillis() - lastContact.get(uavName)) > 3000 ){
                uavStatus.put(uavName + "--Heartbeat", 4);
                uavStatusDescription.put(uavName + "--Heartbeat", "No Dune Telemetry for more than 3 seconds");
            }
            
            // if there is no UAV with this name, create that entry in args with BOOT status
            if((args.get(uavName) == null)){
                IndicatorButton button = new IndicatorButton(uavName, true);
                args.put(uavName, button);
                add(button);
                
                if (getConsole().getMainSystem().equals(uavName)){
                    button.setEnabled(true);
                }
            }
            
            // find out which state is worse - the current UAV state or the state of the entity that is being looked at in this loop
            // and set the UAV state to this worst state
            // stringToState of the UAV's state is necessary because it is stored as a String, while the entity's state in the arguments
            // is integer
            args.get(uavName).setState(findUavState(stringToState(args.get(uavName).getState()), uavStatus.get(entity)));
            
            // add the current entity and its state to the button's list of all entities
            args.get(uavName).addToState(entityName, stateToString(uavStatus.get(entity)));
            args.get(uavName).addToStateDescription(entityName, uavStatusDescription.get(entity));
            
        }
        
    }
    
    // method that returns the worse of 2 states
    private String findUavState(int uavState, int entityState){
        
        int worseState = uavState;
        
        // if the state of the entity is NORMAL, don't do anything
        if (entityState == 1){
        }
        // if the entity's state is BOOT and the UAV's state is normal, the worse state is BOOT
        else if ((uavState == 1) && (entityState == 0)){
            worseState = entityState;
        }
        // if the entity's state is higher than the UAV's state (and it's not comparing BOOT and NORMAL),
        // the entity's state is the worse state
        else if (entityState > uavState){
            worseState = entityState;
        }
        
        // return a string representation of the worse state
        return stateToString(worseState);
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
    
    // returns an integer representation of the state
    private int stringToState(String stringState){
        switch (stringState){
            case "BOOT":
                return 0;
            case "NORMAL":
                return 1;
            case "FAULT":
                return 2;
            case "ERROR":
                return 3;
            case "FAILURE":
                return 4;
            default:
                return 4;
        }
    }
    
    @Override
    protected void paintComponent(Graphics g){
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
    
    // Method that takes the concatenated string of UAV and entity name and returns just the UAV's name
    private String findUavName(String string){
              
        return string.split("--")[0];
    }
    
    //Method that takes the concatenated string of UAV and entity name and returns just the entity's name
    private String findEntityName(String string){
        
        return string.split("--")[1];
    }
    
    public void iAmClicked(IndicatorButton button){
        
        for (Object o : this.getComponents()){
            ((IndicatorButton)o).setEnabled(false);
        }
        
        button.setEnabled(true);
        
        getConsole().setMainSystem(button.getEntityName());
        
    }
}
