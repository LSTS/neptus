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
 * Author: condor
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
