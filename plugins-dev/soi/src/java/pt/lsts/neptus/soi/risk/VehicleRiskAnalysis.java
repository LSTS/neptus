/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * May 8, 2018
 */
package pt.lsts.neptus.soi.risk;

import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import pt.lsts.neptus.types.coord.LocationType;

public class VehicleRiskAnalysis {
    LocationType location = null;
    Date lastCommunication = null;
    Date nextCommunication = null;
    Double fuelLevel = null;
    ArrayList<String> errors = new ArrayList<>();
    TreeMap<Date, String> collisions = new TreeMap<>();
    
    /**
     * @return the location
     */
    public LocationType getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(LocationType location) {
        this.location = location;
    }

    /**
     * @return the lastCommunication
     */
    public Date getLastCommunication() {
        return lastCommunication;
    }

    /**
     * @param lastCommunication the lastCommunication to set
     */
    public void setLastCommunication(Date lastCommunication) {
        this.lastCommunication = lastCommunication;
    }

    /**
     * @return the nextCommunication
     */
    public Date getNextCommunication() {
        return nextCommunication;
    }

    /**
     * @param nextCommunication the nextCommunication to set
     */
    public void setNextCommunication(Date nextCommunication) {
        this.nextCommunication = nextCommunication;
    }

    /**
     * @return the fuelLevel
     */
    public Double getFuelLevel() {
        return fuelLevel;
    }

    /**
     * @param fuelLevel the fuelLevel to set
     */
    public void setFuelLevel(Double fuelLevel) {
        this.fuelLevel = fuelLevel;
    }

    /**
     * @return the errors
     */
    public ArrayList<String> getErrors() {
        return errors;
    }

    /**
     * @param errors the errors to set
     */
    public void setErrors(ArrayList<String> errors) {
        this.errors = errors;
    }

    /**
     * @return the collisions
     */
    public TreeMap<Date, String> getCollisions() {
        return collisions;
    }

    /**
     * @param collisions the collisions to set
     */
    public void setCollisions(TreeMap<Date, String> collisions) {
        this.collisions = collisions;
    }

    ArrayList<String> problems() {
        ArrayList<String> ret = new ArrayList<>();
        
        if (collisions.size() > 0)
            ret.add(collisions.size()+" collisions detected");
        
        if (errors.size() > 0)
            ret.add(errors.size()+" errors reported");
        
        if (lastCommunication != null && lastCommunication.getTime() < System.currentTimeMillis() - 30 * 60_000)
            ret.add("no communications for more than 30 minutes");
        
        
        if (fuelLevel != null && fuelLevel > 1 && fuelLevel < 25)
            ret.add("fuel is running low ("+fuelLevel+")");
        return ret;
    }
}