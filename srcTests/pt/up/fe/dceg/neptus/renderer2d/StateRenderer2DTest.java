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
 * Oct 20, 2011
 * $Id::                                                                        $:
 */
package pt.up.fe.dceg.neptus.renderer2d;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

/**
 * @author Hugo Dias
 *
 */
public class StateRenderer2DTest {
    StateRenderer2D srend;
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
        srend = new StateRenderer2D();
        srend.setSize(400, 400);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void test() {
        LocationType center = srend.getCenter();
        System.out.println("panel size: "+ srend.getWidth()+","+srend.getHeight()+" lod: "+srend.getLevelOfDetail());
        System.out.println(center);
        System.out.println(srend.getScreenPosition(center));
        //srend.setCenter(new LocationType(41, 41));
        //System.out.println(srend.getCenter());
        System.out.println(srend.getScreenPosition(new LocationType(41.711233, -9.18457)));
    }

}
