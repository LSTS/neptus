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
 * Author: Sérgio Ferreira
 * 24 de Fev de 2012
 */
package pt.lsts.neptus.plugins.uavs.daemons;

import java.awt.Graphics;
import java.util.Hashtable;

import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcId16;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleSubPanel;
import pt.lsts.neptus.plugins.containers.MigLayoutContainer;
import pt.lsts.neptus.plugins.planning.MapPanel;
import pt.lsts.neptus.plugins.uavs.UavLib;
import pt.lsts.neptus.plugins.uavs.UavVehicleIcon;
import pt.lsts.neptus.plugins.uavs.painters.elements.UavVehiclePainter;

/**
 * @author sergioferreira
 * @version 0.2
 * @category UavDaemon
 */
@PluginDescription(name="Uav Painter Deamon", icon="pt/lsts/neptus/plugins/uavs/planning.png", author="sergiofereira")
public class UavPainterDaemon extends SimpleSubPanel implements NeptusMessageListener{

    private static final long serialVersionUID = 1L;
    private MapPanel activePlanningPanel;
    private Hashtable<String,UavVehicleIcon>  vehicleTable;
    private boolean updateNeeded;
    
    //current profile active in the host's MigLayoutPanel
    private String profile;
    
    private UavVehiclePainter uavVehiclePainter;
    
    public UavPainterDaemon(ConsoleLayout console){  
        super(console);        
        removeAll();        
    }
    
    //------Setters and Getters------//
    
    //UavVehiclePainter
    public UavVehiclePainter getUavVehiclePainter() {
        return uavVehiclePainter;
    }

    private void setUavVehiclePainter(UavVehiclePainter uavVehiclePainter) {
        this.uavVehiclePainter = uavVehiclePainter;
    }

    //UpdateNeeded
    public void setUpdateNeeded(boolean updateNeeded) {
        this.updateNeeded = updateNeeded;
    }

    public boolean isUpdateNeeded() {
        return updateNeeded;
    }

    //VehicleImages
    public void setVehicleTable(Hashtable<String, UavVehicleIcon> vehicleTable) {
        this.vehicleTable = vehicleTable;
    }

    public Hashtable<String, UavVehicleIcon> getVehicleTable() {
        return vehicleTable;
    }

    //ActivePlanningPanel
    public void setActivePlanningPanel(MapPanel activePlanningPanel) {
        this.activePlanningPanel = activePlanningPanel;
    }


    public MapPanel getActivePlanningPanel() {     
        return activePlanningPanel;
    }

    //------Implemented Interfaces------//
    
    //NeptusMessageListener_BEGIN
    @Override
    public String[] getObservedMessages() {
        return new String[]{"EstimatedState"};
    }

    @Override
    public void messageArrived(IMCMessage message) {
        
        //detects if a new vehicle has become active and includes him in the drawing Hashtable
        if(!vehicleTable.containsKey(ImcSystemsHolder.lookupSystem(ImcId16.valueOf(message.getHeaderValue("src").toString())).getName())){
            vehicleTable.put(
                    ImcSystemsHolder.lookupSystem(ImcId16.valueOf(message.getHeaderValue("src").toString())).getName(),
                    new UavVehicleIcon("Top Down View"));

            setUpdateNeeded(true);
        }

        //if any change to the vehicles's states was detected, then the panel's representation is updated
        if(updateNeeded){
            uavVehiclePainter.setVehicleTable(vehicleTable);
            setUpdateNeeded(false);
        }        
    }
    //NeptusMessageListener_END
    
    //------Specific Methods------//
    
    @Override
    protected void paintComponent(Graphics g) {

        profile = ((MigLayoutContainer) this.getConsole().getMainPanel().getComponent(0)).currentProfile;
        
        if(profile.equals("TACO")){            
            uavVehiclePainter.setVehicleTable(vehicleTable);
        }
        else{
            
            //single vehicle to draw
            Hashtable<String,UavVehicleIcon> singleUav = new Hashtable<String,UavVehicleIcon>();
            singleUav.put(this.getMainVehicleId(), vehicleTable.get(this.getMainVehicleId()));
            uavVehiclePainter.setVehicleTable(singleUav);
        }
    }


    @Override
    public void initSubPanel() {
        setVehicleTable(new Hashtable<String, UavVehicleIcon>());        
        setActivePlanningPanel((MapPanel) UavLib.findPanelInConsole("PlanningPanel", this.getConsole()));
        setUavVehiclePainter(new UavVehiclePainter(activePlanningPanel,this));
        setUpdateNeeded(false);

        //sets up all the layers used by the panel
        //activePlanningPanel.getRenderer().setUavConsole(true);
        //activePlanningPanel.getRenderer().addMouseListener(uavVehiclePainter);
        //activePlanningPanel.getRenderer().addPostRenderPainter(uavVehiclePainter,"Uav Vehicle Painter");
    }
    

    @Override
    public void cleanSubPanel() {
    }
}
