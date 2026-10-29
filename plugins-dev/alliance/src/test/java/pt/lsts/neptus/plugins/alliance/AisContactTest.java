/*
 * Copyright (c) 2004-2026 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 */
package pt.lsts.neptus.plugins.alliance;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;

import de.baderjene.aistoolkit.aisparser.message.Message01;
import de.baderjene.aistoolkit.aisparser.message.Message02;
import de.baderjene.aistoolkit.aisparser.message.Message05;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.plugins.alliance.NmeaPlotter.MTShip;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;

/**
 * JUnit 4 test suite for {@link AisContact} and {@link AisContactDb}.
 */
public class AisContactTest {

    private AisContactDb db;

    @Before
    public void setUp() {
        db = new AisContactDb();
    }

    private AisContact findContact(int mmsi) {
        for (AisContact contact : db.getContacts()) {
            if (contact.getMmsi() == mmsi) {
                return contact;
            }
        }
        return null;
    }

    @Test
    public void testAisContactBasics() {
        int mmsi = 263123456;
        AisContact contact = new AisContact(mmsi);
        assertEquals(mmsi, contact.getMmsi());
        assertEquals("underfined", contact.getNavStatus());
        assertNotNull(contact.getLocation());
        assertTrue(contact.ageMillis() >= 0);

        contact.setLabel("SAGRES");
        assertEquals("SAGRES", contact.getLabel());

        LocationType loc = new LocationType(38.7, -9.1);
        contact.setLocation(loc);
        assertEquals(38.7, contact.getLocation().getLatitudeDegs(), 0.001);
        assertEquals(-9.1, contact.getLocation().getLongitudeDegs(), 0.001);

        contact.setHdg(180.5);
        contact.setCog(181.0);
        contact.setSog(12.3);
        contact.setRateOfTurn(1.5);
        contact.setNavStatus("Under way using engine");

        assertEquals(180.5, contact.getHdg(), 0.001);
        assertEquals(181.0, contact.getCog(), 0.001);
        assertEquals(12.3, contact.getSog(), 0.001);
        assertEquals(1.5, contact.getRateOfTurn(), 0.001);
        assertEquals("Under way using engine", contact.getNavStatus());
    }

    @Test
    public void testAisContactMessageUpdates() {
        AisContact contact = new AisContact(263000001);

        Message01 m1 = new Message01();
        m1.setLatitude(41.15);
        m1.setLongitude(-8.61);
        m1.setTrueHeading(90);
        m1.setCourseOverGround(92.5);
        m1.setSpeedOverGround(10.5);
        m1.setRateOfTurn(0.0);
        m1.setNavigationStatus(0);
        contact.update(m1);

        assertEquals(41.15, contact.getLocation().getLatitudeDegs(), 0.001);
        assertEquals(-8.61, contact.getLocation().getLongitudeDegs(), 0.001);
        assertEquals(90.0, contact.getHdg(), 0.001);
        assertEquals(92.5, contact.getCog(), 0.001);
        assertEquals(10.5, contact.getSog(), 0.001);

        Message05 m5 = new Message05();
        m5.setVesselName("CREOULA");
        m5.setCallSign("CSBE");
        m5.setShipType(60);
        m5.setDimensionToBow(40);
        m5.setDimensionToStern(20);
        m5.setDimensionToPort(5);
        m5.setDimensionToStarboard(5);
        m5.setDraught(4.5);
        contact.update(m5);

        assertEquals("CREOULA", contact.getLabel());
        assertNotNull(contact.getAdditionalProperties());
        assertEquals(4.5, contact.getAdditionalProperties().getDraught(), 0.001);
    }

    @Test
    public void testAisContactSentinelsDirect() {
        assertFalse("Rejects 0,0", AisContact.isValidPosition(0.0, 0.0));
        assertFalse("Rejects lat > 90", AisContact.isValidPosition(91.0, 0.0));
        assertFalse("Rejects lon > 180", AisContact.isValidPosition(0.0, 181.0));
        assertTrue("Accepts valid coords", AisContact.isValidPosition(38.5, -9.0));

        assertFalse("Rejects 360 course", AisContact.isValidAngle(360.0));
        assertFalse("Rejects 511 heading", AisContact.isValidAngle(511.0));
        assertTrue("Accepts 359.9 angle", AisContact.isValidAngle(359.9));

        assertFalse("Rejects 102.3 kn speed", AisContact.isValidSpeed(102.3));
        assertTrue("Accepts 15.0 kn speed", AisContact.isValidSpeed(15.0));

        // Verify sentinels do not overwrite valid state
        AisContact c = new AisContact(263000002);
        c.setLocation(new LocationType(41.5, -8.5));
        c.setHdg(45.0);
        c.setSog(8.0);

        Message01 badMsg = new Message01();
        badMsg.setLatitude(91.0);
        badMsg.setLongitude(181.0);
        badMsg.setTrueHeading(511);
        badMsg.setCourseOverGround(360.0);
        badMsg.setSpeedOverGround(102.3);
        c.update(badMsg);

        assertEquals(41.5, c.getLocation().getLatitudeDegs(), 0.001);
        assertEquals(-8.5, c.getLocation().getLongitudeDegs(), 0.001);
        assertEquals(45.0, c.getHdg(), 0.001);
        assertEquals(8.0, c.getSog(), 0.001);
    }

    @Test
    public void testAisContactDbMTShipParsing() {
        String json = "{"
                + "\"SHIP_ID\": 263999999, "
                + "\"SHIPNAME\": \"POLAR PIONEER\", "
                + "\"LAT\": 38.70, "
                + "\"LON\": -9.15, "
                + "\"SPEED\": 14.5, "
                + "\"HEADING\": 270.0, "
                + "\"COURSE\": 268.0, "
                + "\"DRAUGHT\": 6.2, "
                + "\"WIDTH\": 16, "
                + "\"LENGTH\": 72, "
                + "\"TIME\": 1600000000.0, "
                + "\"ELAPSED\": 0"
                + "}";
        db.processJson(json);

        AisContact c = findContact(263999999);
        assertNotNull("Contact created", c);
        assertEquals("POLAR PIONEER", c.getLabel());
        assertEquals(38.70, c.getLocation().getLatitudeDegs(), 0.001);
        assertEquals(270.0, c.getHdg(), 0.001);
        assertEquals(14.5, c.getSog(), 0.001);
    }

    @Test
    public void testAisContactDbPositionlessContact() {
        // New contact at 0,0 buffered from map
        String jsonGhost = "{\"SHIP_ID\": 263888001, \"SHIPNAME\": \"GHOST_SHIP\", \"LAT\": 0.0, \"LON\": 0.0}";
        db.processJson(jsonGhost);
        assertNull("0,0 ghost buffered from active contacts", findContact(263888001));
        assertEquals("Name learned in cache", "GHOST_SHIP", db.getNameForMMSI(263888001));

        // Valid position publishes it seamlessly
        String jsonFixed = "{\"SHIP_ID\": 263888001, \"SHIPNAME\": \"GHOST_SHIP\", \"LAT\": 38.7, \"LON\": -9.1}";
        db.processJson(jsonFixed);
        assertNotNull("Contact published once coordinates valid", findContact(263888001));
    }

    @Test
    public void testAisContactDbNamePreservation() {
        // 1. Establish initial good name
        String json1 = "{\"SHIP_ID\": 263777001, \"SHIPNAME\": \"ORIGINAL_NAME\", \"LAT\": 38.0, \"LON\": -9.0}";
        db.processJson(json1);
        assertEquals("ORIGINAL_NAME", db.getNameForMMSI(263777001));

        // 2. Incoming update with [SAT-AIS] does not overwrite learned name
        String jsonSat = "{\"SHIP_ID\": 263777001, \"SHIPNAME\": \"[SAT-AIS]\", \"LAT\": 38.1, \"LON\": -9.0}";
        db.processJson(jsonSat);
        assertEquals("Good name preserved across [SAT-AIS]", "ORIGINAL_NAME", db.getNameForMMSI(263777001));

        // 3. Incoming update with null name does not overwrite learned name
        String jsonNull = "{\"SHIP_ID\": 263777001, \"LAT\": 38.2, \"LON\": -9.0}";
        db.processJson(jsonNull);
        assertEquals("Good name preserved across null name", "ORIGINAL_NAME", db.getNameForMMSI(263777001));
    }

    @Test
    public void testAisContactDbNpeEdgeCases() {
        // Missing SHIPNAME falls back to MMSI_<id> safely
        String jsonNoName = "{\"SHIP_ID\": 263666001, \"LAT\": 38.0, \"LON\": -9.0}";
        db.processJson(jsonNoName);
        AisContact c = findContact(263666001);
        assertNotNull(c);
        assertTrue("Fallback to MMSI_id", "MMSI_263666001".equals(c.getLabel()) || "263666001".equals(c.getLabel()));

        // Out-of-bounds MMSI rejected safely
        db.processJson("{\"SHIP_ID\": -500, \"LAT\": 38.0, \"LON\": -9.0}");
        assertNull(findContact(-500));
        db.processJson("{\"SHIP_ID\": 1000000000, \"LAT\": 38.0, \"LON\": -9.0}");
        assertNull(findContact(1000000000));

        // Malformed JSON caught cleanly
        db.processJson("this is not json");
        db.processJson("{ this: also: not: valid }");
    }

    @Test
    public void testAisContactDbSentinelsAndTimestamps() {
        long t1 = System.currentTimeMillis() / 1000;
        MTShip ship = new MTShip();
        ship.SHIP_ID = 263555001;
        ship.SHIPNAME = "ORDER_TEST";
        ship.LAT = 38.0;
        ship.LON = -9.0;
        ship.HEADING = 511.0; // Sentinel heading
        ship.COURSE = 360.0;  // Sentinel course
        ship.SPEED = 102.3;   // Sentinel speed
        ship.TIME = (double) t1;
        db.setMTShip(ship);

        AisContact c = findContact(263555001);
        assertNotNull(c);
        assertNotEquals("511 heading filtered", 511.0, c.getHdg(), 0.001);
        assertNotEquals("360 course filtered", 360.0, c.getCog(), 0.001);
        assertNotEquals("102.3 speed filtered", 102.3, c.getSog(), 0.001);

        // Out-of-order packet (3 minutes in the past) rejected
        MTShip oldShip = new MTShip();
        oldShip.SHIP_ID = 263555001;
        oldShip.SHIPNAME = "ORDER_TEST";
        oldShip.LAT = 45.0; // false position
        oldShip.LON = -5.0;
        oldShip.TIME = (double) (t1 - 180);
        db.setMTShip(oldShip);

        assertEquals("Old packet did not rewind latitude", 38.0, c.getLocation().getLatitudeDegs(), 0.001);
    }

    @Test
    public void testAisContactDbCourseSentinelCheck() {
        int mmsi = 263444001;
        MTShip ship = new MTShip();
        ship.SHIP_ID = mmsi;
        ship.SHIPNAME = "COURSE_TEST";
        ship.LAT = 40.0;
        ship.LON = -8.5;
        ship.COURSE = 0.0;
        ship.HEADING = 275.0;
        ship.TIME = System.currentTimeMillis() / 1000.0;
        db.setMTShip(ship);

        ExternalSystem sys = ExternalSystemsHolder.lookupSystem("COURSE_TEST");
        assertNotNull(sys);
        Object storedCourse = sys.retrieveData(SystemUtils.COURSE_DEGS_KEY);
        assertNotNull("Course stored in ExternalSystem", storedCourse);
        assertEquals("Course 0 falls back to heading 275", 275.0, (Double) storedCourse, 0.001);
    }

    @Test
    public void testAisContactDbPurge() {
        long nowSec = System.currentTimeMillis() / 1000;
        db.processJson("{\"SHIP_ID\": 263333001, \"SHIPNAME\": \"FRESH\", \"LAT\": 41.0, \"LON\": -8.5, \"TIME\": " + nowSec + "}");
        db.processJson("{\"SHIP_ID\": 263333002, \"SHIPNAME\": \"STALE\", \"LAT\": 41.0, \"LON\": -8.5, \"TIME\": " + (nowSec - 60) + ", \"ELAPSED\": 60000}");

        assertNotNull(findContact(263333001));
        assertNotNull(findContact(263333002));

        db.purge(30000); // 30s threshold

        assertNotNull("Fresh contact retained", findContact(263333001));
        assertNull("Stale contact purged", findContact(263333002));
    }

    /**
     * Replicates the exact race condition:
     * In legacy code, updateSystem mutated dimV outside the lock while saveCache iterated
     * dimV.keySet() under the lock, throwing ConcurrentModificationException on new vessels.
     */
    @Test
    public void testAisContactDbConcurrencyRace() throws Exception {
        final AtomicBoolean running = new AtomicBoolean(true);
        final AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

        // Mutator thread rapidly ingesting NEW vessels with dimensions
        Thread mutator = new Thread(() -> {
            int idCounter = 390_000_000;
            while (running.get()) {
                MTShip ship = new MTShip();
                ship.SHIP_ID = idCounter++;
                ship.SHIPNAME = "RACE_VESSEL_" + ship.SHIP_ID;
                ship.LAT = 38.5;
                ship.LON = -9.0;
                ship.DRAUGHT = 5.0 + (idCounter % 15);
                ship.WIDTH = 12 + (idCounter % 6);
                ship.LENGTH = 60 + (idCounter % 30);
                ship.TIME = System.currentTimeMillis() / 1000.0;
                db.setMTShip(ship);
            }
        }, "JUnit-Mutator-Thread");

        // Flusher thread invoking saveCache() concurrently
        Thread flusher = new Thread(() -> {
            for (int i = 0; i < 200 && running.get(); i++) {
                try {
                    db.saveCache();
                }
                catch (Throwable t) {
                    errorRef.compareAndSet(null, t);
                    break;
                }
            }
            running.set(false);
        }, "JUnit-Flusher-Thread");

        mutator.start();
        flusher.start();

        flusher.join(2500);
        running.set(false);
        mutator.join(1000);

        assertNull("Concurrent saveCache and updateSystem threw exception: " + errorRef.get(), errorRef.get());
    }
}
