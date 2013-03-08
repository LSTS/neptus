/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by sergioferreira
 * 24 de Fev de 2012
 * $Id:: UavPainterDaemon.java 9839 2013-02-01 17:39:05Z sergioferreira         $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs.daemons;

import java.awt.Graphics;
import java.util.Hashtable;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.containers.MigLayoutContainer;
import pt.up.fe.dceg.neptus.plugins.planning.MapPanel;
import pt.up.fe.dceg.neptus.plugins.uavs.UavLib;
import pt.up.fe.dceg.neptus.plugins.uavs.UavVehicleIcon;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.elements.UavVehiclePainter;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcId16;

/**
 * @author sergiofereira
 * @version 0.2
 * @category UavDaemon
 */
@PluginDescription(name="Uav Painter Deamon", icon="pt/up/fe/dceg/neptus/plugins/uavs/planning.png", author="sergiofereira")
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
