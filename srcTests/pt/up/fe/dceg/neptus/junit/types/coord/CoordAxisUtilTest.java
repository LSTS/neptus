/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 12/Fev/2005
 */
package pt.up.fe.dceg.neptus.junit.types.coord;

import java.util.Arrays;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.types.coord.CoordAxisUtil;
import pt.up.fe.dceg.neptus.types.coord.CoordinateSystem;

/**
 * @author Paulo
 *
 */
public class CoordAxisUtilTest extends TestCase
{

    public void testGetAxisAnglesshortshortshort()
    {
        double[] res = CoordAxisUtil.getAxisAngles(CoordinateSystem.NORTH_DIRECTION,
                CoordinateSystem.EAST_DIRECTION, CoordinateSystem.DOWN_DIRECTION);
        double[] tres = {0d, 0d, 0d};
        boolean test = Arrays.equals(res, tres);
        assertEquals(true, test);
        
        res = CoordAxisUtil.getAxisAngles(CoordinateSystem.NORTH_DIRECTION,
                CoordinateSystem.DOWN_DIRECTION, CoordinateSystem.WEST_DIRECTION);
        tres[0] = 90d;
        tres[1] = 0d;
        tres[2] = 0d;
        test = Arrays.equals(res, tres);
        assertEquals(true, test);

        res = CoordAxisUtil.getAxisAngles(CoordinateSystem.NORTH_DIRECTION,
                CoordinateSystem.WEST_DIRECTION, CoordinateSystem.UP_DIRECTION);
        tres[0] = 180d;
        tres[1] = 0d;
        tres[2] = 0d;
        test = Arrays.equals(res, tres);
        assertEquals(true, test);

        res = CoordAxisUtil.getAxisAngles(CoordinateSystem.NORTH_DIRECTION,
                CoordinateSystem.UP_DIRECTION, CoordinateSystem.EAST_DIRECTION);
        tres[0] = -90d;
        tres[1] = 0d;
        tres[2] = 0d;
        test = Arrays.equals(res, tres);
        assertEquals(true, test);
    }
    
    
    public void testGetAxisDirectionsdoubledoubledouble ()
    {
        short[] res = CoordAxisUtil.getAxisDirections(0d,0d,0d);
        short[] tres = {CoordinateSystem.NORTH_DIRECTION,
                CoordinateSystem.EAST_DIRECTION, CoordinateSystem.DOWN_DIRECTION};
        boolean test = Arrays.equals(res, tres);
        assertEquals(true, test);

        res = CoordAxisUtil.getAxisDirections (90d, 0d, 0d);
        tres[0] = CoordinateSystem.NORTH_DIRECTION;
        tres[1] = CoordinateSystem.DOWN_DIRECTION; 
        tres[2] = CoordinateSystem.WEST_DIRECTION;
        test = Arrays.equals(res, tres);
        assertEquals(true, test);

        res = CoordAxisUtil.getAxisDirections (180d, 0d, 0d);
        tres[0] = CoordinateSystem.NORTH_DIRECTION;
        tres[1] = CoordinateSystem.WEST_DIRECTION; 
        tres[2] = CoordinateSystem.UP_DIRECTION;
        test = Arrays.equals(res, tres);
        assertEquals(true, test);

        res = CoordAxisUtil.getAxisDirections (-90d, 0d, 0d);
        tres[0] = CoordinateSystem.NORTH_DIRECTION;
        tres[1] = CoordinateSystem.UP_DIRECTION; 
        tres[2] = CoordinateSystem.EAST_DIRECTION;
        test = Arrays.equals(res, tres);
        assertEquals(true, test);

        res = CoordAxisUtil.getAxisDirections (-90d, 23d, 45d);
        tres[0] = CoordinateSystem.UNKNOWN_DIRECTION;
        tres[1] = CoordinateSystem.UNKNOWN_DIRECTION; 
        tres[2] = CoordinateSystem.UNKNOWN_DIRECTION;
        test = Arrays.equals(res, tres);
        assertEquals(true, test);

    }
    
   
    
    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(CoordAxisUtilTest.class);
    }

}
