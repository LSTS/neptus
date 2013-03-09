/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 31/10/2012
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import java.util.List;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author pdias
 *
 */
public interface PathProvider {

    public abstract List<double[]> getPathPoints();

    public abstract List<LocationType> getPathLocations();

}