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
 * Author: Hugo Dias
 * Oct 29, 2012
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
