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
 * 20??/??/??
 * $Id:: MathMiscUtilsTest.java 9616 2012-12-30 23:23:22Z pdias           $:
 */
package pt.up.fe.dceg.neptus.junit.util;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;

public class MathMiscUtilsTest extends TestCase
{

    static double errorD = 0.00001d;
    static float errorF = 0.00001f;
    
    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(MathMiscUtilsTest.class);
    }

//    /*
//     * Test method for 'pt.up.fe.dceg.neptus.util.MathMiscUtils.round(double, int)'
//     */
//    public void testRoundDoubleInt()
//    {
//
//    }
//
//    /*
//     * Test method for 'pt.up.fe.dceg.neptus.util.MathMiscUtils.round(float, int)'
//     */
//    public void testRoundFloatInt()
//    {
//
//    }

    /*
     * Test method for 'pt.up.fe.dceg.neptus.util.MathMiscUtils.parseEngineeringModeToDouble(String)'
     */
    public void testParseEngineeringModeToDouble()
    {
        double test = MathMiscUtils.parseEngineeringModeToDouble("10");
        assertEquals(test, 10, errorD);
        test = MathMiscUtils.parseEngineeringModeToDouble("100M");
        assertEquals(test, 100E6, errorD);
        test = MathMiscUtils.parseEngineeringModeToDouble("100k");
        assertEquals(test, 100E3, errorD);
        test = MathMiscUtils.parseEngineeringModeToDouble("100K");
        //assertEquals(test, Double.NaN, errorD);
        assertEquals(test, Double.NaN);
        test = MathMiscUtils.parseEngineeringModeToDouble("100u");
        assertEquals(test, 100E-6, errorD);
        test = MathMiscUtils.parseEngineeringModeToDouble("100m");
        assertEquals(test, 100E-3, errorD);
    }

    /*
     * Test method for 'pt.up.fe.dceg.neptus.util.MathMiscUtils.filterDeadZone(double, double, double)'
     */
    public void testFilterDeadZoneDoubleDoubleDouble()
    {
        double yy =MathMiscUtils.filterDeadZone(0d, 0.1d, 1d);
        assertEquals(yy, 0d, errorD);
        yy =MathMiscUtils.filterDeadZone(0.1d, 0.1d, 1d);
        assertEquals(yy, 0d, errorD);
        yy =MathMiscUtils.filterDeadZone(0.009d, 0.01d, 1d);
        assertEquals(yy, 0d, errorD);
        yy =MathMiscUtils.filterDeadZone(-0.009d, 0.01d, 1d);
        assertEquals(yy, 0d, errorD);
        yy =MathMiscUtils.filterDeadZone(1d, 0.1d, 1d);
        assertEquals(yy, 1d, errorD);
        yy =MathMiscUtils.filterDeadZone(-1d, 0.1d, 1d);
        assertEquals(yy, -1d, errorD);
    }

    /*
     * Test method for 'pt.up.fe.dceg.neptus.util.MathMiscUtils.filterDeadZone(double, double)'
     */
    public void testFilterDeadZoneDoubleDouble()
    {
        double yy =MathMiscUtils.filterDeadZone(0d, 0.1d);
        assertEquals(yy, 0d, errorD);
        yy =MathMiscUtils.filterDeadZone(0.1d, 0.1d);
        assertEquals(yy, 0d, errorD);
        yy =MathMiscUtils.filterDeadZone(1d, 0.1d);
        assertEquals(yy, 1d, errorD);
    }

    /*
     * Test method for 'pt.up.fe.dceg.neptus.util.MathMiscUtils.filterDeadZone(float, float, float)'
     */
    public void testFilterDeadZoneFloatFloatFloat()
    {
        float yy =MathMiscUtils.filterDeadZone(0f, 0.1f, 1f);
        assertEquals(yy, 0f, errorF);
        yy =MathMiscUtils.filterDeadZone(0.1f, 0.1f, 1f);
        assertEquals(yy, 0d, errorF);
        yy =MathMiscUtils.filterDeadZone(1f, 0.1f, 1f);
        assertEquals(yy, 1d, errorF);
    }

    /*
     * Test method for 'pt.up.fe.dceg.neptus.util.MathMiscUtils.filterDeadZone(float, float)'
     */
    public void testFilterDeadZoneFloatFloat()
    {
        float yy =MathMiscUtils.filterDeadZone(0f, 0.1f);
        assertEquals(yy, 0f, errorF);
        yy =MathMiscUtils.filterDeadZone(0.1f, 0.1f);
        assertEquals(yy, 0d, errorF);
        yy =MathMiscUtils.filterDeadZone(1f, 0.1f);
        assertEquals(yy, 1d, errorF);
    }

    public void parseToEngineeringRadix2Notation() {
    	String val = MathMiscUtils.parseToEngineeringRadix2Notation(2048.0, 1);
    	assertEquals(val, "2ki");
    }
    
}
