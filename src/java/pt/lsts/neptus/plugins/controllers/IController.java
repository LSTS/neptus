/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Modified European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the Modified EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://github.com/LSTS/neptus/blob/develop/LICENSE.md
 * and http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Oct 5, 2013
 */
package pt.lsts.neptus.plugins.controllers;


import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FollowRefState;
import pt.lsts.imc.Reference;
import pt.lsts.neptus.types.vehicle.VehicleType;

/**
 * This interface is implemented by agents that control vehicles
 * @author zp
 */
public interface IController {

    /**
     * This method is used to implement the control law
     * @param vehicle The vehicle being controlled (plant)
     * @param estate The last known vehicle state
     * @param frefState The last known maneuver execution state
     * @return A {@link Reference} message with desired control reference
     */
    public Reference guide(VehicleType vehicle, EstimatedState estate, FollowRefState frefState);
    
    /**
     * @return The desired name for this controller (also used for plan names)
     */
    public String getControllerName();    
    
    /**
     * This method is used to check if a controller supports a given vehicle
     * @param vehicle A vehicle that can be controlled
     * @param state The current state of the vehicle
     * @return <code>true</code> if this vehicle can be controlled or <code>false</code> otherwise
     */
    public boolean supportsVehicle(VehicleType vehicle, EstimatedState state);
    
    /**
     * Method called when the controller is initially activated for a given vehicle
     * @param vehicle The vehicle that will be associated with this controller
     * @param state The last known state of the vehicle that will start being controlled
     */
    public void startControlling(VehicleType vehicle, EstimatedState state);
    
    /**
     * Method called when the control loop is terminated due to a connection time out.
     * The method {@link #stopControlling(VehicleType)} will be called next
     * @param vehicle The vehicle whose connection dropped
     */
    public void vehicleTimedOut(VehicleType vehicle);
    
    /**
     * Method called when the controller looses control over the vehicle. Either because the user commanded it, 
     * an error occurred or the connection with the vehicle was lost 
     * @param vehicle The vehicle that will stop being controlled
     */
    public void stopControlling(VehicleType vehicle);
    
}
