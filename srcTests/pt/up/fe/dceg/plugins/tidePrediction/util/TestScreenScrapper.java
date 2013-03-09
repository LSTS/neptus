/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Margarida Faria
 * Jan 28, 2013
 */
package pt.up.fe.dceg.plugins.tidePrediction.util;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import pt.up.fe.dceg.plugins.tidePrediction.Harbors;
import pt.up.fe.dceg.plugins.tidePrediction.TidePrediction;
import pt.up.fe.dceg.plugins.tidePrediction.TidePrediction.TIDE_TYPE;
import pt.up.fe.dceg.plugins.tidePrediction.TidePredictionFinder;

/**
 * @author Margarida Faria
 *
 */
public class TestScreenScrapper {
    static TidePredictionFinder finder;
    static double delta;

    @BeforeClass
    public static void testSetup() {
        finder = new TidePredictionFinder();
        delta = 0.0001;

    }

    @AfterClass
    public static void testCleanup() {
        // Teardown for data used by the unit tests
    }

    @Test
    public void testDateLowTide() throws Exception {
        float prediction;
        TidePrediction[] predictionsMarks;

        prediction = testDate(Harbors.LEIXOES, finder, new GregorianCalendar(2010, 4, 23, 18, 15));
        predictionsMarks = finder.getPredictionsMarks();
        assertEquals(1.03, predictionsMarks[0].getHeight(), delta);
        assertEquals(3.12, predictionsMarks[1].getHeight(), delta);
        assertEquals(1.0300362, prediction, delta);

        prediction = testDate(Harbors.LEIXOES, finder, new GregorianCalendar(2010, 4, 23, 19, 15));
        predictionsMarks = finder.getPredictionsMarks();
        assertEquals(TIDE_TYPE.LOW_TIDE, predictionsMarks[0].getTideType());
        assertEquals(1.03, predictionsMarks[0].getHeight(), 0.03);
        assertEquals(3.13, predictionsMarks[1].getHeight(), 0.03);
        assertEquals(1.1851, prediction, 0.03);
    }

    @Test
    public void testDateHighTide() throws Exception {
        float prediction;
        TidePrediction[] predictionsMarks;

        prediction = testDate(Harbors.LEIXOES, finder, new GregorianCalendar(2010, 4, 24, 18, 03));
        predictionsMarks = finder.getPredictionsMarks();
        assertEquals(TIDE_TYPE.HIGH_TIDE, predictionsMarks[0].getTideType());
        assertEquals(3.08, predictionsMarks[0].getHeight(), 0.03);
        assertEquals(0.91, predictionsMarks[1].getHeight(), 0.03);
        assertEquals(1.08, prediction, 0.03);

    }

    @Test
    public void testMoonClash() throws Exception {
        float prediction;
        TidePrediction[] predictionsMarks;

        prediction = testDate(Harbors.LEIXOES, finder, new GregorianCalendar(2010, 4, 27, 11, 00));
        predictionsMarks = finder.getPredictionsMarks();
        assertEquals(TIDE_TYPE.LOW_TIDE, predictionsMarks[0].getTideType());
        assertEquals(0.71, predictionsMarks[0].getHeight(), 0.03);
        assertEquals(3.4, predictionsMarks[1].getHeight(), 0.03);
        assertEquals(1.24, prediction, 0.03);

    }

    public float testDate(Harbors harbor, TidePredictionFinder finder, GregorianCalendar gregorianCalendar)
            throws Exception {
        Date wantedDate;
        Float tidePredictions;
        System.out.println("---------------------------------------------");
        wantedDate = gregorianCalendar.getTime();
        tidePredictions = finder.getTidePrediction(wantedDate, harbor, true);
        System.out.println("For " + harbor.name() + " at " + wantedDate.toString() + " height:" + tidePredictions);
        return tidePredictions;
    }
}
