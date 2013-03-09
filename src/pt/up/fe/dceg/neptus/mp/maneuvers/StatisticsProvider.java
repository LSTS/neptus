/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Mar 15, 2011
 */
package pt.up.fe.dceg.neptus.mp.maneuvers;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author zp
 *
 */
public interface StatisticsProvider {

    public double getCompletionTime(LocationType initialPosition);
    public double getDistanceTravelled(LocationType initialPosition);
    public double getMaxDepth();
    public double getMinDepth();    
    
}
