/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by condor
 * 4 de Jan de 2013
 */
package pt.up.fe.dceg.neptus.plugins.uavs.panels;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.MultiSystemIMCMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;

/**
 * @author sergioferreira
 * @version 1.0
 * @category UavPanel
 */
@PluginDescription(name="Uav PTU Control Panel", icon="pt/up/fe/dceg/neptus/plugins/uavs/wbutt.png", author="sergioferreira")
public class UavPTUControlPanel extends SimpleSubPanel{

    private static final long serialVersionUID = 1L;
    
    private String ptuName = "alfa-05";
    private String targetName = "alfa-07";
    private double ptuAlt = 85.0;
//    private boolean working = false;
    
    float i = 0;

    
    //listener object which allows the panel to tap into the various IMC messages
    private MultiSystemIMCMessageListener listener = new MultiSystemIMCMessageListener(this.getClass().getSimpleName()
            + " [" + Integer.toHexString(hashCode()) + "]") {

        @Override
        public void messageArrived(ImcId16 id, IMCMessage msg) {
               
            if(ImcSystemsHolder.lookupSystem(id).getName().equals(targetName)){

                if(true){

                    double pan = Math.atan(Math.abs(msg.getDouble("y"))/Math.abs(msg.getDouble("x")));
                    
                    double hipo = Math.sqrt( ( Math.pow(msg.getDouble("y"),2) + Math.pow(msg.getDouble("x"),2) ) );
                    double tilt = - ((Math.PI/2.0) + Math.atan( (Math.abs(msg.getDouble("depth") + msg.getDouble("z")) - ptuAlt) / hipo));
                    
                    if(msg.getDouble("y") > 0.0 && msg.getDouble("x") > 0.0){
                        send(ptuName, IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+pan+";Tilt="+tilt));
                    }
                    else if(msg.getDouble("y") > 0.0 && msg.getDouble("x") < 0.0){
                        send(ptuName, IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+(Math.PI-pan)+";Tilt="+tilt));
                    }
                    else if(msg.getDouble("y") < 0.0 && msg.getDouble("x") < 0.0){
                        send(ptuName, IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+(-(Math.PI-pan))+";Tilt="+tilt));
                    }
                    else{
                        send(ptuName, IMCDefinition.getInstance().create("RemoteActions", "actions", "Pan="+(-pan)+";Tilt="+tilt));
                    }
                }
            }
        }
    };
    
    public UavPTUControlPanel(ConsoleLayout console){  
        super(console);    
        
        //clears all the unused initializations of the standard SimpleSubPanel
        removeAll();        
    }
        
    @Override
    public void cleanSubPanel() {
        listener.clean();
    }
    
    @Override
    public void initSubPanel() {
        
        //sets up the listener to listen to all vehicles 
        listener.setSystemToListen();
        
        //which messages are listened to
        listener.setMessagesToListen("EstimatedState");       
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }
}
