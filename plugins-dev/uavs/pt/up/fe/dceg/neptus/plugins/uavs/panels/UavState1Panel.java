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
 * 22.10.2012
 */
package pt.up.fe.dceg.neptus.plugins.uavs.panels;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Hashtable;

import javax.swing.Timer;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventMainSystemChange;
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

import com.google.common.eventbus.Subscribe;

/**
 * @author Christian Fuchs
 * @version 0.2
 * @category UavPanel
 * Neptus panel which indicates the UAV's state through a "green light" for each subsystem
 */

@PluginDescription(name="Uav State 1 Panel", icon="pt/up/fe/dceg/neptus/plugins/uavs/planning.png", author="Christian Fuchs")
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
