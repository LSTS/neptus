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
 * Author: Paulo Dias
 * 14/Jan/2005
 */
package pt.lsts.neptus.types.mission;

import pt.lsts.neptus.types.map.MapType;

/**
 * @author Paulo Dias
 *
 */
public class MapMission
{
    String id = "";
    String name = "";
    String href = "";
    
    MapType map = null;

    /**
     * 
     */
    public MapMission()
    {
        super();
    }

    /**
     * @return Returns the href.
     */
    public String getHref()
    {
        return href;
    }
    /**
     * @param href The href to set.
     */
    public void setHref(String href)
    {
        this.href = href;
    }
    /**
     * @param href The href to set.
     */
    public void setHrefAndLoadMap(String href)
    {
        this.href = href;
        this.loadMap();
    }
    /**
     * @return Returns the id.
     */
    public String getId()
    {
        return id;
    }
    /**
     * @param id The id to set.
     */
    public void setId(String id)
    {
        this.id = id;
    }
    /**
     * @return Returns the name.
     */
    public String getName()
    {
        return name;
    }
    /**
     * @param name The name to set.
     */
    public void setName(String name)
    {
        this.name = name;
    }
    /**
     * @return Returns the map.
     */
    public MapType getMap()
    {
        return map;
    }
    /**
     * @param map The map to set.
     */
    public void setMap(MapType map)
    {
        this.map = map;
    }

    /**
     * @param map The map to load.
     */
    public void loadMap()
    {
        this.map = new MapType(getHref());
    }
    /**
     * @param map The map to load.
     */
    public void loadMap(String url)
    {
        this.map = new MapType(url);
    }
    
    public String toString()
    {
        return getId();
    }
}
