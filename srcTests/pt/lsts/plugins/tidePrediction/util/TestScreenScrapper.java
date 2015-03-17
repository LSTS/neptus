/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Margarida Faria
 * Jan 28, 2013
 */
package pt.lsts.plugins.tidePrediction.util;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.tidePrediction.Harbors;
import pt.lsts.neptus.plugins.tidePrediction.PtHydrographicWeb;
import pt.lsts.neptus.util.bathymetry.TidePrediction;
import pt.lsts.neptus.util.bathymetry.TidePrediction.TIDE_TYPE;
/**
 * @author Margarida Faria
 *
 */
public class TestScreenScrapper {
    static PtHydrographicWeb finder;
    static double delta;

    @BeforeClass
    public static void testSetup() {
        finder = new PtHydrographicWeb(Harbors.LEIXOES);
        delta = 0.0001;

    }

    @AfterClass
    public static void testCleanup() {
        // Teardown for data used by the unit tests
    }

    @Test
    public void testDateLowTide() throws Exception {
        float prediction;
        ArrayList<TidePrediction> predictionsMarks;

        prediction = testDate(Harbors.LEIXOES, finder, new GregorianCalendar(2010, GregorianCalendar.MAY, 23, 18, 15));
        assertEquals(1.0300362, prediction, delta);
        predictionsMarks = finder.getPredictionsMarks();
        assertEquals(1.03, predictionsMarks.get(0).getHeight(), delta);
        assertEquals(3.12, predictionsMarks.get(1).getHeight(), delta);

        prediction = testDate(Harbors.LEIXOES, finder, new GregorianCalendar(2010, GregorianCalendar.MAY, 23, 19, 15));
        assertEquals(1.1621268, prediction, delta);
        predictionsMarks = finder.getPredictionsMarks();
        assertEquals(TIDE_TYPE.LOW_TIDE, predictionsMarks.get(0).getTideType());
        assertEquals(1.03, predictionsMarks.get(0).getHeight(), 0.03);
        assertEquals(3.13, predictionsMarks.get(1).getHeight(), 0.03);

        prediction = testDate(Harbors.LEIXOES, finder, new GregorianCalendar(2010, GregorianCalendar.MAY, 23, 22, 15));
        assertEquals(2.5177317, prediction, delta);
    }

    @Test
    public void testDateHighTide() throws Exception {
        float prediction;
        ArrayList<TidePrediction> predictionsMarks;

        prediction = testDate(Harbors.LEIXOES, finder, new GregorianCalendar(2010, GregorianCalendar.MAY, 24, 18, 03));
        predictionsMarks = finder.getPredictionsMarks();
        assertEquals(TIDE_TYPE.HIGH_TIDE, predictionsMarks.get(0).getTideType());
        assertEquals(3.08, predictionsMarks.get(0).getHeight(), 0.03);
        assertEquals(0.91, predictionsMarks.get(1).getHeight(), 0.03);
        assertEquals(1.08, prediction, 0.03);

    }

    @Test
    public void testMoonClash() throws Exception {
        float prediction;
        ArrayList<TidePrediction> predictionsMarks;

        prediction = testDate(Harbors.LEIXOES, finder, new GregorianCalendar(2010, GregorianCalendar.MAY, 27, 11, 00));
        predictionsMarks = finder.getPredictionsMarks();
        assertEquals(TIDE_TYPE.LOW_TIDE, predictionsMarks.get(0).getTideType());
        assertEquals(0.71, predictionsMarks.get(0).getHeight(), 0.03);
        assertEquals(3.4, predictionsMarks.get(1).getHeight(), 0.03);
        assertEquals(1.24, prediction, 0.03);

    }

    public float testDate(Harbors harbor, PtHydrographicWeb finder, GregorianCalendar gregorianCalendar)
            throws Exception {
        Date wantedDate;
        Float tidePredictions;
        NeptusLog.pub().info("<###>---------------------------------------------\n");
        wantedDate = gregorianCalendar.getTime();
        tidePredictions = finder.getTidePrediction(wantedDate, true);
        NeptusLog.pub().info("<###>For " + harbor.name() + " at " + wantedDate.toString() + " height:" + tidePredictions);
        return tidePredictions;
    }
}
