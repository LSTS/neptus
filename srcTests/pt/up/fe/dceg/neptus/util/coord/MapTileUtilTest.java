/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo Dias
 * Oct 14, 2011
 */
package pt.up.fe.dceg.neptus.util.coord;

import java.awt.geom.Point2D;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

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
        
        double[] actual = MapTileUtil.XYToDegrees(xy.getX(), xy.getY(), levelOfDetail);
        System.out.println(latitude+","+longitude);
        System.out.println(actual[0]+","+actual[1]);

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
        
        double[] actual = MapTileUtil.XYToDegrees(xy.getX()+deltaX, xy.getY()+deltaY, levelOfDetail);
        System.out.println(latitude+","+longitude);
        System.out.println(actual[0]+","+actual[1]);

    }

}
