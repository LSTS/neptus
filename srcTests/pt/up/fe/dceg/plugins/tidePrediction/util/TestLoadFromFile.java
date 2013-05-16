/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: meg
 * May 15, 2013
 */
package pt.up.fe.dceg.plugins.tidePrediction.util;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URL;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.BeforeClass;
import org.junit.Test;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.util.bathymetry.LocalData;

/**
 * @author meg
 *
 */
public class TestLoadFromFile {
    static double delta;
    static LocalData finder;

    @BeforeClass
    public static void testSetup() {
        URL resource = TestLoadFromFile.class.getResource("tides.txt");
        finder = new LocalData(new File(resource.getPath()));
        delta = 0.00001;

    }

    @Test
    public void testDateLowTide() throws Exception {
        float prediction;

        prediction = testDate(finder, new GregorianCalendar(2010, GregorianCalendar.APRIL, 23, 18, 15));
        assertEquals(1.0300362, prediction, delta);

        prediction = testDate(finder, new GregorianCalendar(2010, GregorianCalendar.APRIL, 23, 19, 15));
        assertEquals(1.1621268, prediction, delta);

        prediction = testDate(finder, new GregorianCalendar(2010, GregorianCalendar.APRIL, 23, 22, 15));
        assertEquals(2.5177317, prediction, delta);
    }

    @Test
    public void testDateBeforeTides() throws Exception {
        float prediction;
        prediction = testDate(finder, new GregorianCalendar(2008, GregorianCalendar.APRIL, 23, 18, 15));
        assertEquals(0, prediction, delta);
    }

    @Test
    public void testDateAfterTides() throws Exception {
        float prediction;
        prediction = testDate(finder, new GregorianCalendar(2011, GregorianCalendar.APRIL, 23, 18, 15));
        assertEquals(0, prediction, delta);
    }

    @Test
    public void testDateHighTide() throws Exception {
        float prediction = testDate(finder, new GregorianCalendar(2010, GregorianCalendar.APRIL, 24, 18, 03));
        assertEquals(1.08, prediction, delta);
    }

    private float testDate(LocalData finder, GregorianCalendar gregorianCalendar) throws Exception {
        Date wantedDate;
        Float tidePredictions;
        NeptusLog.pub().info("<###>---------------------------------------------");
        wantedDate = gregorianCalendar.getTime();
        tidePredictions = finder.getTidePrediction(wantedDate, true);
        NeptusLog.pub().info("<###> " + wantedDate.toString() + " height:" + tidePredictions);
        return tidePredictions;
    }
}
