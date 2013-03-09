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
 * 21/Fev/2005
 */
package pt.up.fe.dceg.neptus.junit.types.coord;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import junit.framework.TestCase;
import pt.up.fe.dceg.neptus.types.coord.CoordinateUtil;

/**
 * @author Paulo Dias
 * @author Sérgio Fraga
 *
 */
public class CoordinateUtilTest extends TestCase
{

    public static void main(String[] args)
    {
        junit.swingui.TestRunner.run(CoordinateUtilTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception
    {
        super.tearDown();
    }
    
    public void testSphericalToCartesianCoordinates()
    {
    	double error = 0.01;
	    double[] testVector = new double[3];
	    testVector[0] = 10;
	    testVector[1] = 0;
	    testVector[2] = 90;
	    double[] st = 
	        CoordinateUtil.sphericalToCartesianCoordinates(testVector[0],testVector[1],testVector[2]);
	    assertEquals(10, st[0],error);
	    assertEquals(0, st[1],error);
	    assertEquals(0, st[2],error);	    
    }
       
    public void testCartesianToSphericalCoordinates()
    {
    	double error = 0.01;
	    double[] testVector = new double[3];
	    testVector[0] = 0;
	    testVector[1] = 0;
	    testVector[2] = 1;
	    double[] st = 
	        CoordinateUtil.cartesianToSphericalCoordinates(testVector[0],testVector[1],testVector[2]);
	    assertEquals(1, st[0],error);
	    assertEquals(0, st[1],error);
	    assertEquals(0, st[2],error);	    
    }

    
    public void testCylindricalToCartesianCoordinates()
    {
    	double error = 0.01;
	    double[] testVector = new double[3];
	    testVector[0] = 10;
	    testVector[1] = 90;
	    testVector[2] = 3;
	    double[] st = 
	        CoordinateUtil.cylindricalToCartesianCoordinates(testVector[0],testVector[1],testVector[2]);
	    printVector("Cyl", testVector);
	    printVector("2 Rec", st);
	    assertEquals(0, st[0],error);
	    assertEquals(10, st[1],error);
	    assertEquals(3, st[2],error);
    }

    
    public void testCartesianToCylindricalCoordinates()
    {
    	double error = 0.01;
	    double[] testVector = new double[3];
	    testVector[0] = 0;
	    testVector[1] = 10;
	    testVector[2] = 3;
	    double[] st = 
	        CoordinateUtil.cartesianToCylindricalCoordinates(testVector[0],testVector[1],testVector[2]);
	    printVector("Rec", testVector);
	    printVector("2 Cyl", st);
	    assertEquals(10, st[0],error);
	    assertEquals(90*Math.PI/180, st[1],error);
	    assertEquals(3, st[2],error);
    }

    
    public void testCylindricalToSphericalCoordinates()
    {
    	double error = 0.01;
	    double[] testVector = new double[3];
	    testVector[0] = 2;
	    testVector[1] = 45;
	    testVector[2] = 3;
	    double[] st = 
	        CoordinateUtil.cylindricalToShericalCoordinates(testVector[0],testVector[1],testVector[2]);
	    printVector("Cyl", testVector);
	    printVector("2 Sph", st);
	    assertEquals(3.60555127546, st[0],error);
	    assertEquals(45*Math.PI/180, st[1],error);
	    assertEquals(33.69*Math.PI/180, st[2],error);
    }

    
    public void testSphericalToCylindricalCoordinates()
    {
    	double error = 0.01;
	    double[] testVector = new double[3];
	    testVector[0] = 1;
	    testVector[1] = 60;
	    testVector[2] = 45;
	    double[] st = 
	        CoordinateUtil.sphericalToCylindricalCoordinates(testVector[0],testVector[1],testVector[2]);
	    printVector("Sph", testVector);
	    printVector("2 Cyl", st);
	    assertEquals(0.7071067811865476, st[0],error);
	    assertEquals(60*Math.PI/180, st[1],error);
	    assertEquals(0.7071067811865476, st[2],error);
    }

    public void testAddSphericalToCartesianOffsetsAndGetAsCylindrical()
    {
    	double error = 0.01;
	    double[] testVector = new double[3];
	    testVector[0] = 1;
	    testVector[1] = 60;
	    testVector[2] = 45;
	    double[] testVector2 = new double[3];
	    testVector2[0] = 2;
	    testVector2[1] = 6;
	    testVector2[2] = 5;

	    double[] st = 
	        CoordinateUtil
                .addSphericalToCartesianOffsetsAndGetAsCylindrical(
                        testVector[0], testVector[1], testVector[2],
                        testVector2[0], testVector2[1], testVector2[2]);
	    printVector("Add Sph", testVector);
	    printVector("to Rec", testVector2);
	    printVector("equals Cyl", st);
	    assertEquals(7.018737977067005, st[0],error);
	    assertEquals(70.4077*Math.PI/180, st[1],error);
	    assertEquals(5.707106781186548, st[2],error);
    }
    
    private void printVector (String header, double[] vec)
    {
        String res = header.concat(": ");
        for (int i= 0; i < vec.length; i++)
        {
            res += " # " + vec[i];
        }
        System.out.println(res);
    }
    public void testParseLatitudeCoordToStringArray()
    {
	    String te = "41N3.6117";
	    String[] st = 
	        CoordinateUtil.parseLatitudeCoordToStringArray(te);
	    //System.out.println(st[1]+st[0]+st[2]+" "+st[3]);
	    assertEquals("N", st[0]);
	    assertEquals("41", st[1]);
	    assertEquals("3.6117", st[2]);
	    assertEquals("0", st[3]);
    }
    
    public void parseLatitudeCoordToDoubleValuedouble()
    {
	    String te = "41N3.6117";
	    assertEquals(41.060195d, 
	            CoordinateUtil.parseLatitudeCoordToDoubleValue(te), 0d);
    }
    
    public void testParseLongitudeCoordToStringArray()
    {
	    String te = "8W27.4009";
	    String[] st = 
	        CoordinateUtil.parseLongitudeCoordToStringArray(te);
	    //System.out.println(st[1]+st[0]+st[2]+" "+st[3]);
	    assertEquals("W", st[0]);
	    assertEquals("8", st[1]);
	    assertEquals("27.4009", st[2]);
	    assertEquals("0", st[3]);
    }
    
    public void parseLongitudeCoordToDoubleValuedouble()
    {
	    String te = "8W27.4009";
	    assertEquals(-8.456681666666666d, 
	            CoordinateUtil.parseLongitudeCoordToDoubleValue(te), 0d);
    }
    
	public void testDmstoDecimalDegreesConversion()
    {
        Calendar c = new GregorianCalendar();
        Random r = new Random(c.getTimeInMillis());

        for (int i = 0; i < 10; i++)
        {
            double teste = r.nextFloat() * 90;
            double[] dms;
            dms = CoordinateUtil.decimalDegreesToDMS(teste);
            assertEquals(teste, CoordinateUtil.dmsToDecimalDegrees(dms[0],
                    dms[1], dms[2]), 0d);
            //System.out.println(teste + " <=> "
            //        + CoordinateUtil.dmsToString(dms));
        }

    }
	
	public void testDmsToLatStringdoubledoubledouble()
	{
	    String lat = CoordinateUtil.dmsToLatString(41d, 16d, 1.2d);
	    assertEquals("41N16'1.200''", lat);
	}

	public void testDmsToLonStringdoubledoubledouble()
	{
	    String lon = CoordinateUtil.dmsToLonString(-8d, 36d, 0.2d);
	    assertEquals("8W36'0.200''", lon);
	}

	public void testDmToLatStringdoubledouble()
	{
	    String lat = CoordinateUtil.dmToLatString(41d, 16.2d);
	    assertEquals("41N16.200", lat);
	}

	public void testDmToLonStringdoubledouble()
	{
	    String lon = CoordinateUtil.dmToLonString(-8d, 36.2d);
	    assertEquals("8W36.200", lon);
	}
	
	public void testLatLonDiffDoubleDoubleDoubleDouble()
	{
		double[] dol = CoordinateUtil.latLonDiff(41.234, -8.456, 39.455, -8.456);
		System.out.println("val " + dol[0]  + " " + dol[1]);
		dol = CoordinateUtil.latLonDiff(41.234, -8.456, 41.234, -8.456);
		System.out.println("val " + dol[0]  + " " + dol[1]);
		dol = CoordinateUtil.latLonDiff(41.234, -8.456, 41.0, -8.45600001);
		System.out.println("val " + dol[0]  + " " + dol[1]);
		dol = CoordinateUtil.latLonDiff(0.0, 0.0, 0, 1);
		System.out.println("val " + dol[0]  + " " + dol[1]);
		dol = CoordinateUtil.latLonDiff(41.234, -8.456, 41.234, -8.45);
		System.out.println("val " + dol[0]  + " " + dol[1]);
		dol = CoordinateUtil.latLonDiff(39, -8.456, 33, -8.456);
		System.out.println("val " + dol[0]  + " " + dol[1]);
		assertFalse(true);
	}
}
