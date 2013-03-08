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
 * Mar 23, 2005
 * $Id:: ManeuverType.java 9616 2012-12-30 23:23:22Z pdias                $:
 */
package pt.up.fe.dceg.neptus.types.mission;

/**
 * @deprecated
 * This class will store data relative to the maneuvers that appear in a plan graph
 * @author ZP
 *
 */
public class ManeuverType {

    private String id = null;
    private String type = null;
    
    
    
    /**
     * Creates a new maneuver given its id and type
     * @param maneuverId The id of the maneuver
     * @param type The type of the maneuver
     */
    public ManeuverType(String maneuverId, String type) {
        setId(maneuverId);
        setType(type);
    }
    
    /**
     * Gets the maneuver ID
     * @return the maneuver ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sets the maneuver ID
     * @param id The id of the maneuver
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Gets the maneuver type
     * @return The maneuver type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Sets the maneuver type
     * @param type The new type of this maneuver
     */
    public void setType(String type) {
        this.type = type;
    }
}
