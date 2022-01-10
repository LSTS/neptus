/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * 21/Fev/2005
 */
package pt.lsts.neptus.junit.types.coord;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Random;

import junit.framework.TestCase;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.types.coord.CoordinateUtil;

/**
 * @author Paulo Dias
 * @author Sérgio Fraga
 *
 */
public class CoordinateUtilTest extends TestCase {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(CoordinateUtilTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testSphericalToCartesianCoordinates() {
        double error = 0.01;
        double[] testVector = new double[3];
        testVector[0] = 10;
        testVector[1] = 0;
        testVector[2] = 90;
        double[] st = CoordinateUtil.sphericalToCartesianCoordinates(testVector[0], testVector[1], testVector[2]);
        assertEquals(10, st[0], error);
        assertEquals(0, st[1], error);
        assertEquals(0, st[2], error);
    }

    public void testCartesianToSphericalCoordinates() {
        double error = 0.01;
        double[] testVector = new double[3];
        testVector[0] = 0;
        testVector[1] = 0;
        testVector[2] = 1;
        double[] st = CoordinateUtil.cartesianToSphericalCoordinates(testVector[0], testVector[1], testVector[2]);
        assertEquals(1, st[0], error);
        assertEquals(0, st[1], error);
        assertEquals(0, st[2], error);
    }

    public void testCylindricalToCartesianCoordinates() {
        double error = 0.01;
        double[] testVector = new double[3];
        testVector[0] = 10;
        testVector[1] = 90;
        testVector[2] = 3;
        double[] st = CoordinateUtil.cylindricalToCartesianCoordinates(testVector[0], testVector[1], testVector[2]);
        printVector("Cyl", testVector);
        printVector("2 Rec", st);
        assertEquals(0, st[0], error);
        assertEquals(10, st[1], error);
        assertEquals(3, st[2], error);
    }

    public void testCartesianToCylindricalCoordinates() {
        double error = 0.01;
        double[] testVector = new double[3];
        testVector[0] = 0;
        testVector[1] = 10;
        testVector[2] = 3;
        double[] st = CoordinateUtil.cartesianToCylindricalCoordinates(testVector[0], testVector[1], testVector[2]);
        printVector("Rec", testVector);
        printVector("2 Cyl", st);
        assertEquals(10, st[0], error);
        assertEquals(90 * Math.PI / 180, st[1], error);
        assertEquals(3, st[2], error);
    }

    public void testCylindricalToSphericalCoordinates() {
        double error = 0.01;
        double[] testVector = new double[3];
        testVector[0] = 2;
        testVector[1] = 45;
        testVector[2] = 3;
        double[] st = CoordinateUtil.cylindricalToShericalCoordinates(testVector[0], testVector[1], testVector[2]);
        printVector("Cyl", testVector);
        printVector("2 Sph", st);
        assertEquals(3.60555127546, st[0], error);
        assertEquals(45 * Math.PI / 180, st[1], error);
        assertEquals(33.69 * Math.PI / 180, st[2], error);
    }

    public void testSphericalToCylindricalCoordinates() {
        double error = 0.01;
        double[] testVector = new double[3];
        testVector[0] = 1;
        testVector[1] = 60;
        testVector[2] = 45;
        double[] st = CoordinateUtil.sphericalToCylindricalCoordinates(testVector[0], testVector[1], testVector[2]);
        printVector("Sph", testVector);
        printVector("2 Cyl", st);
        assertEquals(0.7071067811865476, st[0], error);
        assertEquals(60 * Math.PI / 180, st[1], error);
        assertEquals(0.7071067811865476, st[2], error);
    }

    public void testAddSphericalToCartesianOffsetsAndGetAsCylindrical() {
        double error = 0.01;
        double[] testVector = new double[3];
        testVector[0] = 1;
        testVector[1] = 60;
        testVector[2] = 45;
        double[] testVector2 = new double[3];
        testVector2[0] = 2;
        testVector2[1] = 6;
        testVector2[2] = 5;

        double[] st = CoordinateUtil.addSphericalToCartesianOffsetsAndGetAsCylindrical(testVector[0], testVector[1],
                testVector[2], testVector2[0], testVector2[1], testVector2[2]);
        printVector("Add Sph", testVector);
        printVector("to Rec", testVector2);
        printVector("equals Cyl", st);
        assertEquals(7.018737977067005, st[0], error);
        assertEquals(70.4077 * Math.PI / 180, st[1], error);
        assertEquals(5.707106781186548, st[2], error);
    }

    private void printVector(String header, double[] vec) {
        String res = header.concat(": ");
        for (int i = 0; i < vec.length; i++) {
            res += " # " + vec[i];
        }
        NeptusLog.pub().info("<###> " + res);
    }

    public void parseLatitudeCoordToDoubleValuedouble() {
        String te = "41N3.6117";
        assertEquals(41.060195d, CoordinateUtil.parseCoordString(te), 0d);
    }

    public void parseLongitudeCoordToDoubleValuedouble() {
        String te = "8W27.4009";
        assertEquals(-8.456681666666666d, CoordinateUtil.parseCoordString(te), 0d);
    }

    public void testDmstoDecimalDegreesConversion() {
        Calendar c = new GregorianCalendar();
        Random r = new Random(c.getTimeInMillis());

        for (int i = 0; i < 10; i++) {
            double teste = r.nextFloat() * 90;
            double[] dms;
            dms = CoordinateUtil.decimalDegreesToDMS(teste);
            assertEquals(teste, CoordinateUtil.dmsToDecimalDegrees(dms[0], dms[1], dms[2]), 0d);
        }
    }

    public void testDmsToLatStringdoubledoubledouble() {
        String lat = CoordinateUtil.dmsToLatString(41d, 16d, 1.2d);
        assertEquals("41N16'1.20''", lat);
    }

    public void testDmsToLonStringdoubledoubledouble() {
        String lon = CoordinateUtil.dmsToLonString(-8d, 36d, 0.2d);
        assertEquals("8W36'0.20''", lon);
    }

    public void testDmToLatStringdoubledouble() {
        String lat = CoordinateUtil.dmToLatString(41d, 16.2d);
        assertEquals("41N16.200", lat);
    }

    public void testDmToLonStringdoubledouble() {
        String lon = CoordinateUtil.dmToLonString(-8d, 36.2d);
        assertEquals("8W36.200", lon);
    }

    public void testLatLonDiffDoubleDoubleDoubleDouble() {
        double[] dol = CoordinateUtil.latLonDiff(41.234, -8.456, 39.455, -8.456);
        NeptusLog.pub().info("<###>val " + dol[0] + " " + dol[1]);
        dol = CoordinateUtil.latLonDiff(41.234, -8.456, 41.234, -8.456);
        NeptusLog.pub().info("<###>val " + dol[0] + " " + dol[1]);
        dol = CoordinateUtil.latLonDiff(41.234, -8.456, 41.0, -8.45600001);
        NeptusLog.pub().info("<###>val " + dol[0] + " " + dol[1]);
        dol = CoordinateUtil.latLonDiff(0.0, 0.0, 0, 1);
        NeptusLog.pub().info("<###>val " + dol[0] + " " + dol[1]);
        dol = CoordinateUtil.latLonDiff(41.234, -8.456, 41.234, -8.45);
        NeptusLog.pub().info("<###>val " + dol[0] + " " + dol[1]);
        dol = CoordinateUtil.latLonDiff(39, -8.456, 33, -8.456);
        NeptusLog.pub().info("<###>val " + dol[0] + " " + dol[1]);
        assertFalse(true);
    }
}
