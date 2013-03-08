/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jqcorreia
 * Oct 26, 2012
 * $Id:: SidescanPoint.java 9615 2012-12-30 23:08:28Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.plugins.sidescan;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author jqcorreia
 *
 */
public class SidescanPoint {
    public LocationType location;
    public int x,y;
    
    /**
     * @param location
     * @param x
     * @param y
     */
    public SidescanPoint(int x, int y, LocationType location) {
        super();
        this.location = location;
        this.x = x;
        this.y = y;
    }
}
