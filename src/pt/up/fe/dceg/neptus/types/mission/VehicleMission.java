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
 */
package pt.up.fe.dceg.neptus.types.mission;

import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;

/**
 * @author Paulo Dias
 *
 */
public class VehicleMission
{
    private String id = "";
    private String name = "";
    
    private boolean homeRefUsed = false;
    private CoordinateSystem coordinateSystem = null;
    private VehicleType vehicle = null;

    /**
     * 
     */
    public VehicleMission()
    {
        super();
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
     * @return Returns the homeRefUsed.
     */
    public boolean isHomeRefUsed()
    {
        return homeRefUsed;
    }
    /**
     * @param homeRefUsed The homeRefUsed to set.
     */
    public void setHomeRefUsed(boolean homeRefUsed)
    {
        this.homeRefUsed = homeRefUsed;
    }
    /**
     * @return Returns the coordSystem.
     */
    public CoordinateSystem getCoordinateSystem()
    {
        return coordinateSystem;
    }
    /**
     * @param coordSystem The coordSystem to set.
     */
    public void setCoordinateSystem(CoordinateSystem coordSystem)
    {
        this.coordinateSystem = coordSystem;
    }
    
    
    /**
     * @return Returns the vehicle.
     */
    public VehicleType getVehicle()
    {
        return vehicle;
    }
    /**
     * @param vehicle The vehicle to set.
     */
    public void setVehicle(VehicleType vehicle)
    {
        this.vehicle = vehicle;
    }
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return getName() + " (" + getId() +")";
    }
}
