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
 * 19.11.2012
 * $Id:: UavSettingsDaemon.java 9839 2013-02-01 17:39:05Z sergioferreira        $:
 */
package pt.up.fe.dceg.neptus.plugins.uavs.daemons;

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.SystemsList;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.containers.MigLayoutContainer;
import pt.up.fe.dceg.neptus.plugins.planning.UavPiccoloControl;
import pt.up.fe.dceg.neptus.plugins.uavs.painters.elements.UavCameraFootprint;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType.VehicleTypeEnum;

/**
 * @author Christian Fuchs
 * @version 0.1
 * Daemon that changes Neptus settings based on the current console profile
 */
@PluginDescription(name="Uav Settings Deamon", icon="pt/up/fe/dceg/neptus/plugins/uavs/planning.png", author="ChristianFuchs")
public class UavSettingsDaemon extends SimpleSubPanel{

    private static final long serialVersionUID = 1L;
    private String profile;
    private SystemsList systemsList;
    private ConsoleLayout console;
    private UavPiccoloControl piccoloControl;
    private UavCameraFootprint cameraFootprint;
    
    public UavSettingsDaemon(ConsoleLayout console){
        super(console);
        removeAll();
    }
    
    private void setSystemsList(SystemsList list){
        this.systemsList = list;
    }
    
    private void setConsole(ConsoleLayout console){
        this.console = console;
    }
    
    private void setProfile(String profile){
        this.profile = profile;
    }
    
    private void setPiccoloControl(UavPiccoloControl piccoloControl){
        this.piccoloControl = piccoloControl;
    }
    
    private void setCameraFootprint(UavCameraFootprint cameraFootprint){
        this.cameraFootprint = cameraFootprint;
    }
    
    // change the settings according to the current profile
    private void changeSettings(){
        
        switch(profile){
            case "TACO":
                systemsList.showSystemsIconsThatAreFilteredOut = true;          //show all vehicles on map
                systemsList.systemsFilter = SystemTypeEnum.VEHICLE;             //filter to only vehicles
                systemsList.vehicleTypeFilter = VehicleTypeEnum.ALL;            //filter to all vehicles
                piccoloControl.showWaypoints = true;                            //show Piccolo waypoints
                cameraFootprint.setDoPaint(false);                              //don't show the camera footprint
                break;
            case "Video-Operator":
                systemsList.showSystemsIconsThatAreFilteredOut = false;         //only show filtered vehicles on map
                systemsList.systemsFilter = SystemTypeEnum.VEHICLE;             //filter to only vehicles
                systemsList.vehicleTypeFilter = VehicleTypeEnum.UAV;            //filter to only UAVs
                piccoloControl.showWaypoints = false;                           //don't show Piccolo waypoints
                cameraFootprint.setDoPaint(true);                               //show the camera footprint
                break;
            default:
                systemsList.showSystemsIconsThatAreFilteredOut = false;         //only show filtered vehicles on map
                systemsList.systemsFilter = SystemTypeEnum.VEHICLE;             //filter to only vehicles
                systemsList.vehicleTypeFilter = VehicleTypeEnum.UAV;            //filter to only UAVs
                piccoloControl.showWaypoints = true;                            //show Piccolo waypoints
                cameraFootprint.setDoPaint(false);                              //don't show the camera footprint
        }
    }
    
    @Override
    public void initSubPanel() {
        // initialize required items
        setConsole(this.getConsole());
        setSystemsList((SystemsList)((MigLayoutContainer) console.getMainPanel().getComponent(0)).getSubPanelByName("Systems List"));
        setProfile(((MigLayoutContainer) console.getMainPanel().getComponent(0)).currentProfile);
        setPiccoloControl((UavPiccoloControl)((MigLayoutContainer) console.getMainPanel().getComponent(0)).getSubPanelByName("UAV Piccolo Control"));
        setCameraFootprint((UavCameraFootprint)((MigLayoutContainer) console.getMainPanel().getComponent(0)).getSubPanelByName("Uav Camera Footprint"));
        
        // create a container listener that will change the settings whenever a component is added or removed to the MigLayout
        // this way, a profile change will be detected
        ContainerListener listener = new ContainerListener() {
            
            @Override
            public void componentRemoved(ContainerEvent e) {

                if (!profile.equals(((MigLayoutContainer) console.getMainPanel().getComponent(0)).currentProfile)){
                    profile = ((MigLayoutContainer) console.getMainPanel().getComponent(0)).currentProfile;
                    
                    changeSettings();
                    
                }
                
            }
            
            @Override
            public void componentAdded(ContainerEvent e) {

                if (!profile.equals(((MigLayoutContainer) console.getMainPanel().getComponent(0)).currentProfile)){
                    profile = ((MigLayoutContainer) console.getMainPanel().getComponent(0)).currentProfile;
                    
                    changeSettings();
                }
                
            }
        };
        
        // add the container listener to the MigLayout
        ((MigLayoutContainer) console.getMainPanel().getComponent(0)).addContainerListener(listener);
        
        // change settings at least once when the console is opened
        changeSettings();
        
    }
    
    @Override
    public void cleanSubPanel() {
    }
}