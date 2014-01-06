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
 * 19.11.2012
 */
package pt.lsts.neptus.plugins.uavs.daemons;

import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.plugins.SystemsList;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleSubPanel;
import pt.lsts.neptus.plugins.containers.MigLayoutContainer;
import pt.lsts.neptus.plugins.planning.UavPiccoloControl;
import pt.lsts.neptus.plugins.uavs.painters.elements.UavCameraFootprint;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehicleType.VehicleTypeEnum;

/**
 * @author Christian Fuchs
 * @version 0.1
 * Daemon that changes Neptus settings based on the current console profile
 */
@PluginDescription(name="Uav Settings Deamon", icon="pt/lsts/neptus/plugins/uavs/planning.png", author="ChristianFuchs")
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