/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Paulo Dias
 * 14/Jan/2005
 * $Id:: MapMission.java 9616 2012-12-30 23:23:22Z pdias                  $:
 */
package pt.up.fe.dceg.neptus.types.mission;

import pt.up.fe.dceg.neptus.types.map.MapType;

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
