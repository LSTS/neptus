/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: 
 * 12/Fev/2005
 */
package pt.lsts.neptus.types.coord;

import java.util.Arrays;

/**
 * The base for the definition of this coordinate system
 * is the N-E-D. That is x pointing to north, y to east 
 * and z down. The origin of the default coordinate system
 * is N0 E0 with hieght 0 meters.
 * 
 * @version 1.0  2005-02-12
 * @author Paulo Dias
 */
public class CoordAxisUtil
{

    public static final short[] NED = {CoordinateSystem.NORTH_DIRECTION,
            CoordinateSystem.EAST_DIRECTION, CoordinateSystem.DOWN_DIRECTION};
        
    /**
     * Comment for <code>VALID_AXIS_DIRECTIONS</code>
     */
    public static final short[][] VALID_AXIS_DIRECTIONS = 
        {
            {CoordinateSystem.NORTH_DIRECTION, CoordinateSystem.EAST_DIRECTION,
                CoordinateSystem.DOWN_DIRECTION},
            {CoordinateSystem.NORTH_DIRECTION, CoordinateSystem.DOWN_DIRECTION,
                CoordinateSystem.WEST_DIRECTION},
            {CoordinateSystem.NORTH_DIRECTION, CoordinateSystem.WEST_DIRECTION,
                CoordinateSystem.UP_DIRECTION},
            {CoordinateSystem.NORTH_DIRECTION, CoordinateSystem.UP_DIRECTION,
                CoordinateSystem.EAST_DIRECTION},
        };
    

    /**
     * Comment for <code>VALID_AXIS_ANGLES</code>
     */
    public static final double[][] VALID_AXIS_ANGLES = 
    	{
            {   0d,    0d,    0d},
            {  90d,    0d,    0d},
            { 180d,    0d,    0d},
            { -90d,    0d,    0d},
    	};

    
    /**
     * @param xAxisDirection
     * @param yAxisDirection
     * @param zAxisDirection
     * @return null if non valid entries. 
     */
    public static double[] getAxisAngles (short xAxisDirection, 
            short yAxisDirection, short zAxisDirection)
    {
        boolean test = false;
        short[] dir = {xAxisDirection, yAxisDirection, zAxisDirection};
        
        for (int i = 0; i < VALID_AXIS_DIRECTIONS.length; i++)
        {
            short[] dirTest = VALID_AXIS_DIRECTIONS[i];
            test = Arrays.equals(dir, dirTest);
            if (test == true)
            {
                if (i >= VALID_AXIS_ANGLES.length)
                    return null;
                double[] result = VALID_AXIS_ANGLES[i];
                return result;
            }
        }       
        return null;
    }
    
    
    /**
     * <b>Note:</b> Despit the combination can give a known direction(
     *  {@link #VALID_AXIS_DIRECTIONS}), the return will only base itself 
     *  on the {@link #VALID_AXIS_ANGLES} and {@link #VALID_AXIS_DIRECTIONS}
     *  combination.    
     * @param roll
     * @param pitch
     * @param yaw
     * @return An 3 length array of {@link CoordinateSystem.UNKNOWN_DIRECTION} 
     *  if unknown directions. 
     */
    public static short[] getAxisDirections (double roll, 
            double pitch, double yaw)
    {
        short[] unknown = {CoordinateSystem.UNKNOWN_DIRECTION, 
                CoordinateSystem.UNKNOWN_DIRECTION,
                CoordinateSystem.UNKNOWN_DIRECTION};
        boolean test = false;
        double[] ang = {roll, pitch, yaw};

        for (int i = 0; i < VALID_AXIS_ANGLES.length; i++)
        {
            double[] angTest = VALID_AXIS_ANGLES[i];
            test = Arrays.equals(ang, angTest);
            if (test == true)
            {
                if (i >= VALID_AXIS_DIRECTIONS.length)
                    return unknown;
                short[] result = VALID_AXIS_DIRECTIONS[i];
                return result;
            }
        }
        return unknown;        
    }
    
    
    /**
     * @param dir
     * @return
     */
    public static short parseDirection (String dir)
    {
        if (dir.equalsIgnoreCase("north"))
            return CoordinateSystem.NORTH_DIRECTION;
        if (dir.equalsIgnoreCase("south"))
            return CoordinateSystem.SOUTH_DIRECTION;
        if (dir.equalsIgnoreCase("east"))
            return CoordinateSystem.EAST_DIRECTION;
        if (dir.equalsIgnoreCase("west"))
            return CoordinateSystem.WEST_DIRECTION;
        if (dir.equalsIgnoreCase("up"))
            return CoordinateSystem.UP_DIRECTION;
        if (dir.equalsIgnoreCase("down"))
            return CoordinateSystem.DOWN_DIRECTION;

        if (dir.equalsIgnoreCase("n"))
            return CoordinateSystem.NORTH_DIRECTION;
        if (dir.equalsIgnoreCase("s"))
            return CoordinateSystem.SOUTH_DIRECTION;
        if (dir.equalsIgnoreCase("e"))
            return CoordinateSystem.EAST_DIRECTION;
        if (dir.equalsIgnoreCase("w"))
            return CoordinateSystem.WEST_DIRECTION;
        if (dir.equalsIgnoreCase("u"))
            return CoordinateSystem.UP_DIRECTION;
        if (dir.equalsIgnoreCase("d"))
            return CoordinateSystem.DOWN_DIRECTION;

        return CoordinateSystem.UNKNOWN_DIRECTION;
    }

    
    /**
     * @see CoordinateSystem
     * @param dir
     * @return
     */
    public static String getDirectionAsString (short dir)
    {
        if (dir == CoordinateSystem.NORTH_DIRECTION)
            return CoordinateSystem.NORTH_DIRECTION_STRING;
        if (dir == CoordinateSystem.SOUTH_DIRECTION)
            return CoordinateSystem.SOUTH_DIRECTION_STRING;
        if (dir == CoordinateSystem.EAST_DIRECTION)
            return CoordinateSystem.EAST_DIRECTION_STRING;
        if (dir == CoordinateSystem.WEST_DIRECTION)
            return CoordinateSystem.WEST_DIRECTION_STRING;
        if (dir == CoordinateSystem.UP_DIRECTION)
            return CoordinateSystem.UP_DIRECTION_STRING;
        if (dir == CoordinateSystem.DOWN_DIRECTION)
            return CoordinateSystem.DOWN_DIRECTION_STRING;
        
        return CoordinateSystem.UNKNOWN_DIRECTION_STRING;
    }

}
