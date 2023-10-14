/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: 
 * 20??/??/??
 */
package pt.lsts.neptus.mp;

import java.awt.Color;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mp.maneuvers.DefaultManeuver;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.mp.maneuvers.LocatedManeuver;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * This class returns maneuver instances of various types. It is used to load the maneuver implementations relative to
 * any vehicle.
 * 
 * @author Zé Carlos
 */
public class ManeuverFactory {

    private LinkedHashMap<String, Maneuver> availableManeuvers = new LinkedHashMap<String, Maneuver>();

    double manAltitude = 0.0;
    boolean forceManeuverAltitude = false;

    public ManeuverFactory(VehicleType vehicle) {
        if (vehicle == null)
            return;
        
        for (String manName : vehicle.getFeasibleManeuvers().keySet()) {
            Maneuver man = ManeuverFactory.createManeuver(manName, vehicle.getFeasibleManeuvers().get(manName));
            if (man == null)
                man = new DefaultManeuver();
            
            availableManeuvers.put(man.getType(), man);
        }
    }

    public ImageIcon getManeuverIcon(String manName) {
        if (availableManeuvers.containsKey(manName))
            return availableManeuvers.get(manName).getIcon();
        return GuiUtils.getLetterIcon('?', Color.white, Color.red.darker(), 16);
    }

    public void putManeuver(Maneuver man) {
        availableManeuvers.put(man.getType(), man);
    }

    public String[] getAvailableManeuversIDs() {
        return availableManeuvers.keySet().toArray(new String[] {});
    }

    public Maneuver getManeuver(String maneuverName) {
        if (availableManeuvers.containsKey(maneuverName)) {
            Maneuver tmp = (Maneuver) availableManeuvers.get(maneuverName).clone();
            if (tmp instanceof LocatedManeuver && forceManeuverAltitude) {
                LocationType lt = new LocationType(((LocatedManeuver) tmp).getManeuverLocation());
                lt.setHeight(manAltitude);
                lt.setOffsetDown(0);
                ((LocatedManeuver) tmp).getManeuverLocation().setLocation(lt);
            }
            return tmp;
        }
        else {
            NeptusLog.pub().error(this + ": The maneuver " + maneuverName + " can't be created");
            return null;
        }
    }

    /**
     * Verifies if a given class can be loaded
     * 
     * @param classFileName The class file name
     * @return
     */
    public boolean existsManeuver(String classFileName) {
        return (createManeuver("", classFileName) != null);
    }

    /**
     * Tries to load the given maneuver class
     * 
     * @param classFileName A maneuver class file name
     * @return The selected maneuver or null if it doesn't exist
     */
    public static Maneuver createManeuver(String manName, String classFileName) {

        ClassLoader loader = Goto.class.getClassLoader();
        Maneuver man = null;
        try {
            Class<?> clazz = loader.loadClass(classFileName);
            Object tmp = null;

            tmp = clazz.getDeclaredConstructor().newInstance();

            if (tmp == null)
                tmp = new DefaultManeuver();

            if (tmp instanceof Maneuver)
                man = (Maneuver) tmp;
        }
        catch (Exception e) {
            e.printStackTrace();
            NeptusLog.pub().warn("class not found: " + classFileName);
        }
        return man;

    }

    public boolean isForceManeuverAltitude() {
        return forceManeuverAltitude;
    }

    public void setForceManeuverAltitude(boolean forceManeuverAltitude) {
        this.forceManeuverAltitude = forceManeuverAltitude;
    }

    public double getManAltitude() {
        return manAltitude;
    }

    public void setManAltitude(double manAltitude) {
        this.manAltitude = manAltitude;
    }

}
