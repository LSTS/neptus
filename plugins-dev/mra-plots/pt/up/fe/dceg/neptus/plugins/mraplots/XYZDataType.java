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
 * 21 de Jun de 2012
 */
package pt.up.fe.dceg.neptus.plugins.mraplots;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author pdias
 *
 */
public class XYZDataType {
    public LocationType topCornerLoc;
    public LocationType centerLoc;
    public int width, height;
    public double scale;
    public double[][] dataSet;
    public double minX, minY, minZ, maxX, maxY, maxZ;
}
