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
 * 3/Set/2005
 * $Id:: ChecklistMission.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.types.mission;

import pt.up.fe.dceg.neptus.types.checklist.ChecklistType;

/**
 * @author Paulo Dias
 *
 */
public class ChecklistMission
{
    String id = "";
    String name = "";
    String href = "";
    
    ChecklistType checklist = null;

    /**
     * 
     */
    public ChecklistMission()
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
    public void setHrefAndLoadChecklist(String href)
    {
        this.href = href;
        this.loadChecklist();
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
    public ChecklistType getChecklist()
    {
        return checklist;
    }
    /**
     * @param checklist The map to set.
     */
    public void setChecklist(ChecklistType checklist)
    {
        this.checklist = checklist;
        setHref(checklist.getOriginalFilePath());
        setId(checklist.getName());
        setName(checklist.getName());        
    }

    /**
     * @param map The map to load.
     */
    public void loadChecklist()
    {
        this.checklist = new ChecklistType(getHref());
    }
    /**
     * @param map The map to load.
     */
    public void loadChecklist(String url)
    {
        this.checklist = new ChecklistType(url);
    }


    
    public String toString()
    {
        return getId();
    }
}
