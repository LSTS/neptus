/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 */
package pt.up.fe.dceg.neptus.mp;

import java.awt.Color;
import java.util.LinkedHashMap;

import javax.swing.ImageIcon;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.mp.maneuvers.DefaultManeuver;
import pt.up.fe.dceg.neptus.mp.maneuvers.Goto;
import pt.up.fe.dceg.neptus.mp.maneuvers.LocatedManeuver;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.util.GuiUtils;

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

        ClassLoader loader = new Goto().getClass().getClassLoader();
        Maneuver man = null;
        try {
            Class<?> clazz = loader.loadClass(classFileName);
            Object tmp = null;

            tmp = clazz.newInstance();

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
