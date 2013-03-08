/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Oct 29, 2012
 * $Id:: SystemType.java 9615 2012-12-30 23:08:28Z pdias                        $:
 */
package pt.up.fe.dceg.neptus.systems;

import java.util.HashMap;
import java.util.Map;

import pt.up.fe.dceg.neptus.systems.SystemsManager.SystemClass;
import pt.up.fe.dceg.neptus.systems.SystemsManager.SystemLinkType;
import pt.up.fe.dceg.neptus.systems.links.SystemLink;
import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author Hugo
 * 
 */
public class SystemType {
    private int id;
    private String name;
    private String humanName;
    private SystemClass type;
    private LocationType location;
    private long locationAge;
    private Map<SystemLinkType, SystemLink> links = new HashMap<>();

    public SystemType() {

    }

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the humanName
     */
    public String getHumanName() {
        return humanName;
    }

    /**
     * @param humanName the humanName to set
     */
    public void setHumanName(String humanName) {
        this.humanName = humanName;
    }

    /**
     * @return the type
     */
    public SystemClass getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(SystemClass type) {
        this.type = type;
    }

    /**
     * Get the system location
     * 
     * @return the location this returns a clone to change the systems location use @call setLocation
     */
    public LocationType getLocation() {
        return location.getNewAbsoluteLatLonDepth();
    }

    /**
     * @param location the location to set
     */
    public void setLocation(LocationType location) {
        this.location = location;
    }

    public SystemLink getLink(SystemLinkType linkType){
        return links.get(linkType);
    }
    
    public void setLink(SystemLinkType linkType, SystemLink link){
        //TODO
    }
    /**
     * @return the locationAge
     */
    public long getLocationAge() {
        return locationAge;
    }

    /**
     * @param locationAge the locationAge to set
     */
    public void setLocationAge(long locationAge) {
        this.locationAge = locationAge;
    }

    /**
     * @return the links
     */
    public Map<SystemLinkType, SystemLink> getLinks() {
        return links;
    }

    /**
     * @param links the links to set
     */
    public void setLinks(Map<SystemLinkType, SystemLink> links) {
        this.links = links;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "SystemType [id=" + id + ", name=" + name + ", humanName=" + humanName + ", type=" + type
                + ", location=" + location + ", locationAge=" + locationAge + ", links=" + links + "]";
    }

    

}
