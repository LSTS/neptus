/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Hugo Dias
 * Oct 14, 2011
 */
package pt.lsts.neptus.util.coord;

import java.awt.geom.Point2D;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import pt.lsts.neptus.NeptusLog;

/**
 * @author Hugo Dias
 *
 */
public class MapTileUtilTest {

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void full_conversion() {
        //39.909736,-8.02002
        double latitude = -85.01987330316388;
        double longitude = -8.676892600193582;
//        double[] expected = {latitude, longitude};
        int levelOfDetail = 11;
        Point2D xy = MapTileUtil.degreesToXY(latitude, longitude, levelOfDetail);
        
        double[] actual = MapTileUtil.xyToDegrees(xy.getX(), xy.getY(), levelOfDetail);
        NeptusLog.pub().info("<###> "+latitude+","+longitude);
        NeptusLog.pub().info("<###> "+actual[0]+","+actual[1]);

    }
    
    @Test
    public void translate() {
        //-85.03709998362422,-8.75173696054514539
        double latitude = -85.01987330316388;
        double longitude = -8.676892600193582;
        double deltaX = 0.0;
        double deltaY = -24.8125;
        int levelOfDetail = 11;
        Point2D xy = MapTileUtil.degreesToXY(latitude, longitude, levelOfDetail);
        
        double[] actual = MapTileUtil.xyToDegrees(xy.getX()+deltaX, xy.getY()+deltaY, levelOfDetail);
        NeptusLog.pub().info("<###> "+latitude+","+longitude);
        NeptusLog.pub().info("<###> "+actual[0]+","+actual[1]);

    }

}
