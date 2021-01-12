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
package pt.lsts.neptus.junit.types.coord;

import java.util.Arrays;

import junit.framework.TestCase;
import pt.lsts.neptus.types.coord.CoordAxisUtil;
import pt.lsts.neptus.types.coord.CoordinateSystem;

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
