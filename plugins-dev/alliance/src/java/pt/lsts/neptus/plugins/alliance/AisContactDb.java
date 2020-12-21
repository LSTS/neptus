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
 *
 * Author: zp
 * Jul 24, 2014
 */
package pt.lsts.neptus.plugins.alliance;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import com.google.gson.Gson;

import de.baderjene.aistoolkit.aisparser.AISObserver;
import de.baderjene.aistoolkit.aisparser.message.Message;
import de.baderjene.aistoolkit.aisparser.message.Message01;
import de.baderjene.aistoolkit.aisparser.message.Message03;
import de.baderjene.aistoolkit.aisparser.message.Message05;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.SystemUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.plugins.alliance.NmeaPlotter.MTShip;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.AISUtil;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.NMEAUtils;
import pt.lsts.neptus.util.UnitsUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author zp
 * @author pdias
 */
public class AisContactDb implements AISObserver {

    private LinkedHashMap<Integer, AisContact> contacts = new LinkedHashMap<>();
    private LinkedHashMap<Integer, String> labelCache = new LinkedHashMap<>();
    private LinkedHashMap<Integer, HashMap<String, Object>> dimensionsCache = new LinkedHashMap<>();

    private File cache = new File(ConfigFetch.getConfFolder() + "/ais.cache");

    private String lastGGA = null;
    private String lastGPHDT = null;

    Gson gson = new Gson();
    
    public AisContactDb() {
        if (!cache.canRead())
            return;
        int count = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(cache));
            String line = reader.readLine();
            while (line != null) {
                String[] parts = line.split(",");
                int mmsi;
                try {
                    mmsi = Integer.parseInt(parts[0]);
                }
                catch (Exception e1) {
                    mmsi = Integer.parseInt(parts[0].replaceAll("^0x", ""), 16);
                }
                String name = parts[1].trim();
                labelCache.put(mmsi, name);

                HashMap<String, Object> dimV = new HashMap<>();
                for (int i = 2; i < parts.length; i++) {
                    String tk = parts[i].trim();
                    String[] prs = tk.split("=");
                    if (prs.length > 1) {
                        String n = prs[0].trim();
                        try {
                            double v = Double.parseDouble(prs[1].trim());
                            dimV.put(n, v);
                        }
                        catch (Exception e) {
                            NeptusLog.pub().info(String.format("Not found a number, adding as string for %s", n));
                            dimV.put(n, prs[1].trim());
                        }
                    }
                }
                if (dimV.size() > 0)
                    dimensionsCache.put(mmsi, dimV);

                line = reader.readLine();
                count++;
            }
            reader.close();
            NeptusLog.pub().info("Read " + count + " vessel names from " + cache.getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveCache() {
        int count = 0;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(cache));

            for (Entry<Integer, String> entry : labelCache.entrySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(entry.getKey()).append(",").append(entry.getValue());

                HashMap<String, Object> dimV = dimensionsCache.get(entry.getKey());
                if (dimV != null) {
                    for (String n : dimV.keySet()) {
                        sb.append(",");
                        sb.append(n).append("=").append("" + dimV.get(n));
                    }
                }

                sb.append("\n");
                writer.write(sb.toString());
                count++;
            }
            writer.close();
            NeptusLog.pub().info("Wrote " + count + " vessel names to " + cache.getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getNameForMMSI(int mmsi) {
        return labelCache.get(mmsi);
    }
    
    public void processGGA(String sentence) {
        lastGGA = sentence;
        LocationType myLoc = NMEAUtils.processGGASentence(lastGGA);
        Date dateTime = NMEAUtils.processGGATimeFromSentence(lastGGA);
        ExternalSystem extSys = ExternalSystemsHolder.lookupSystem("Ship");
        if (extSys == null) {
            ExternalSystem es = new ExternalSystem("Ship");
            extSys = ExternalSystemsHolder.registerSystem(es);
        }
        LocationType oldLoc = extSys.getLocation();
        extSys.setLocation(myLoc, dateTime == null ? System.currentTimeMillis() : dateTime.getTime());
        if (oldLoc.compareTo(myLoc) != 0) {
            NeptusLog.pub().debug((String.format(">>>>>>>>> Ship >>>>>>> %s  :: %s :: %s :: %s", sentence, 
                    CoordinateUtil.latitudeAsPrettyString(myLoc.getLatitudeDegs()), CoordinateUtil.longitudeAsPrettyString(myLoc.getLongitudeDegs()),
                    DateTimeUtil.dateTimeFormatterISO8601.format(new Date(extSys.getLocationTimeMillis())))));
        }
    }

    public void processGLL(String sentence) {
        LocationType myLoc = NMEAUtils.processGLLSentence(sentence);
        Date dateTime = NMEAUtils.processGLLTimeFromSentence(sentence);
        ExternalSystem extSys = ExternalSystemsHolder.lookupSystem("Ship");
        if (extSys == null) {
            ExternalSystem es = new ExternalSystem("Ship");
            extSys = ExternalSystemsHolder.registerSystem(es);
        }
        LocationType oldLoc = extSys.getLocation();
        extSys.setLocation(myLoc, dateTime == null ? System.currentTimeMillis() : dateTime.getTime());
        if (oldLoc.compareTo(myLoc) != 0) {
            NeptusLog.pub().debug((String.format(">>>>>>>>> Ship GLL >>>>>>> %s  :: %s :: %s :: %s", sentence, 
                    CoordinateUtil.latitudeAsPrettyString(myLoc.getLatitudeDegs()), CoordinateUtil.longitudeAsPrettyString(myLoc.getLongitudeDegs()),
                    DateTimeUtil.dateTimeFormatterISO8601.format(new Date(extSys.getLocationTimeMillis())))));
        }
    }

    public void processGPHDT(String sentence) {
        lastGPHDT = sentence;
        double myHeadingDegs = NMEAUtils.processGPHDTSentence(lastGPHDT);
        ExternalSystem extSys = ExternalSystemsHolder.lookupSystem("Ship");
        if (extSys == null) {
            ExternalSystem es = new ExternalSystem("Ship");
            extSys = ExternalSystemsHolder.registerSystem(es);
        }
        double oldHdg = extSys.getYawDegrees();
        extSys.setAttitudeDegrees(myHeadingDegs, System.currentTimeMillis());
        if (Double.compare(oldHdg, extSys.getYawDegrees()) != 0) {
            NeptusLog.pub().debug((String.format(">>>>>>>>> Ship HDG >>>>>>> %s  :: %s", sentence, "" + extSys.getYawDegrees())));
        }
    }

    public void processRattm(String sentence) {
        if (lastGGA == null)
            return;
        // $GPGGA,132446.00,3811.805048,N,00856.490411,W,2,06,1.2,30.5,M,49.7,M,3.8,0000*6F
        LocationType myLoc = NMEAUtils.processGGASentence(lastGGA);
        // $RATTM,17,1.25,60.9,T,7.9,358.0,T,1.11,-4.3,N,wb,T,,133714,M*27
        String[] parts = sentence.trim().split(",");
        double heading = Double.parseDouble(parts[3]);
        double dist = Double.parseDouble(parts[2]) * 1852;

        LocationType newLoc = new LocationType(myLoc);
        newLoc.setAzimuth(heading);
        newLoc.setOffsetDistance(dist);
        newLoc.convertToAbsoluteLatLonDepth();
        String id = parts[11];
        Integer rid = Integer.parseInt(parts[1]);

        if (id.isEmpty())
            id = "radar-" + parts[1];

        if (ImcSystemsHolder.getSystemWithName(id) != null && ImcSystemsHolder.getSystemWithName(id).isActive()) {
            return;
        }

        if (!contacts.containsKey(rid)) {
            AisContact contact = new AisContact(rid);
            contacts.put(rid, contact);
        }

        AisContact contact = contacts.get(rid);
        contact.setLocation(newLoc);
        contact.setLabel(id);
    }

    public void processBtll(String sentence) {
        // $A-TLL,1,4330.60542,N,01623.45716,E,IVER-UPCT,,,315.4*02
        String[] parts = sentence.replaceFirst("\\*\\w\\w$", "").trim().split(",");
        int mmsi = Integer.parseInt(parts[1]);
        double lat = 0, lon = 0;
        try {
            lat = NMEAUtils.nmeaLatOrLongToWGS84(parts[2]);
            lon = NMEAUtils.nmeaLatOrLongToWGS84(parts[4]);

            if (parts[3].equals("S"))
                lat = -lat;

            if (parts[5].equals("W"))
                lon = -lon;
        }
        catch (Exception e) {
            NeptusLog.pub().debug("Unable to parse coordinates in " + sentence);
            return;
        }
        String id = parts[6];

        // no need to use AIS for systems using IMC
        if (ImcSystemsHolder.getSystemWithName(id) != null && ImcSystemsHolder.getSystemWithName(id).isActive()) {
            return;
        }

        double heading = 0;
        try {
            heading = Double.parseDouble(parts[9]);
        }
        catch (Exception e) {
            // e.printStackTrace();
        }

        LocationType loc = new LocationType(lat, lon);

        if (!contacts.containsKey(mmsi)) {
            AisContact contact = new AisContact(mmsi);
            contacts.put(mmsi, contact);
        }
        // System.out.println(mmsi);
        AisContact contact = contacts.get(mmsi);
        contact.setLocation(loc);
        contact.setCog(heading);
        contact.setLabel(id);
        if (ImcSystemsHolder.getSystemWithName(id) == null)
            updateSystem(mmsi, loc, heading,System.currentTimeMillis());
    }
    
    public void setMTShip(MTShip ship) {
        int mmsi = (int) ship.SHIP_ID;
        String name = ship.SHIPNAME;
        if (ship.SHIPNAME.equals("[SAT-AIS]")) {
            name = "SAT_"+ship.SHIP_ID;
        }
        AisContact contact;
        if (!contacts.containsKey(mmsi)) {
            contact = new AisContact(mmsi);
            contacts.put(mmsi, contact);
        }
        else {
            contact = contacts.get(mmsi);
            contact.setLastUpdate((long)(ship.TIME * 1000.0));
            contacts.replace(mmsi, contact);
        }
        contact.setLocation(new LocationType(ship.LAT, ship.LON));
        contact.setHdg(ship.HEADING);
        contact.setCog(ship.COURSE);
        contact.setLabel(name);
        contact.setSog(ship.SPEED);
        contact.setRateOfTurn(ship.ROT);
        contact.setNavStatus(ship.STATUS_NAME);
        Message05 additionalProps = new Message05();
        additionalProps.setVesselName(name);
        additionalProps.setDimensionToPort(ship.W_LEFT);
        additionalProps.setDimensionToStarboard(ship.WIDTH-ship.W_LEFT);
        additionalProps.setDimensionToBow(ship.L_FORE);
        additionalProps.setDimensionToStern(ship.LENGTH-ship.L_FORE);
        additionalProps.setAisVersion(-1);
        additionalProps.setCallSign("Unknown");
        additionalProps.setDraught(0);
        additionalProps.setShipType(ship.TYPE);
        additionalProps.setDestination(ship.DESTINATION);
        contact.update(additionalProps);
        long time = System.currentTimeMillis()-ship.ELAPSED;
        contact.setLastUpdate(time);
        updateSystem(mmsi,new LocationType(ship.LAT,ship.LON),ship.HEADING,time);
    }
    
    public void processJson(String sentence) {
        MTShip ship = gson.fromJson(sentence, MTShip.class);
        setMTShip(ship);
    }

    public void updateSystem(int mmsi, LocationType loc, double heading,long millis) {
        AisContact contact = contacts.get(mmsi);
        String name = contact.getLabel();
        ExternalSystem sys = NMEAUtils.getAndRegisterExternalSystem(mmsi, name);
        
        sys.setLocation(loc, millis);
        sys.setAttitudeDegrees(heading > 360 ? contact.getCog() : heading,millis);

        if (!dimensionsCache.containsKey(mmsi))
            dimensionsCache.put(mmsi, new HashMap<String, Object>());
        HashMap<String, Object> dimV = dimensionsCache.get(mmsi);

        sys.storeData(SystemUtils.MMSI_KEY, mmsi);

        sys.storeData(SystemUtils.GROUND_SPEED_KEY, contact.getSog() / UnitsUtil.MS_TO_KNOT);
        sys.storeData(SystemUtils.COURSE_DEGS_KEY, contact.getCog() == 0 ? heading : contact.getCog());
        sys.storeData(SystemUtils.RATE_OF_TURN_DEGS_PER_MIN_KEY, contact.getRateOfTurn());

        sys.storeData(SystemUtils.NAV_STATUS_KEY, contact.getNavStatus());

        if (contact.getAdditionalProperties() != null) {
            String shipType = AISUtil.translateShipType(contact.getAdditionalProperties().getShipType());
            sys.storeData(SystemUtils.SHIP_TYPE_KEY, shipType);
            sys.setType(SystemUtils.getSystemTypeFrom(shipType));
            sys.setTypeExternal(SystemUtils.getExternalTypeFrom(shipType));
            sys.setTypeVehicle(SystemUtils.getVehicleTypeFrom(shipType));

            sys.storeData(SystemUtils.CALL_SIGN_KEY, contact.getAdditionalProperties().getCallSign());

            sys.storeData(SystemUtils.DRAUGHT_KEY, contact.getAdditionalProperties().getDraught());
            sys.storeData(SystemUtils.WIDTH_KEY, contact.getAdditionalProperties().getDimensionToPort()
                    + contact.getAdditionalProperties().getDimensionToStarboard());
            sys.storeData(SystemUtils.WIDTH_CENTER_OFFSET_KEY, contact.getAdditionalProperties().getDimensionToPort()
                    - contact.getAdditionalProperties().getDimensionToStarboard());
            sys.storeData(SystemUtils.LENGHT_KEY, contact.getAdditionalProperties().getDimensionToStern()
                    + contact.getAdditionalProperties().getDimensionToBow());
            sys.storeData(SystemUtils.LENGHT_CENTER_OFFSET_KEY, contact.getAdditionalProperties().getDimensionToStern()
                    - contact.getAdditionalProperties().getDimensionToBow());

//            System.out.println(sys.getName() + " " + sys.retrieveData(SystemUtils.LENGHT_CENTER_OFFSET_KEY) + " "
//                    + sys.retrieveData(SystemUtils.WIDTH_CENTER_OFFSET_KEY) + "   @"
//                    + sys.retrieveData(SystemUtils.GROUND_SPEED_KEY) + "   HDG:" + sys.getYawDegrees());

            dimV.put(SystemUtils.DRAUGHT_KEY, sys.retrieveData(SystemUtils.DRAUGHT_KEY));
            dimV.put(SystemUtils.WIDTH_KEY, sys.retrieveData(SystemUtils.WIDTH_KEY));
            dimV.put(SystemUtils.LENGHT_KEY, sys.retrieveData(SystemUtils.LENGHT_KEY));
            dimV.put(SystemUtils.WIDTH_CENTER_OFFSET_KEY, sys.retrieveData(SystemUtils.WIDTH_CENTER_OFFSET_KEY));
            dimV.put(SystemUtils.LENGHT_CENTER_OFFSET_KEY, sys.retrieveData(SystemUtils.LENGHT_CENTER_OFFSET_KEY));
        }
        else {
            if (dimV != null) {
                if (!sys.containsData(SystemUtils.DRAUGHT_KEY))
                    sys.storeData(SystemUtils.DRAUGHT_KEY, dimV.get(SystemUtils.DRAUGHT_KEY));
                if (!sys.containsData(SystemUtils.WIDTH_KEY))
                    sys.storeData(SystemUtils.WIDTH_KEY, dimV.get(SystemUtils.WIDTH_KEY));
                if (!sys.containsData(SystemUtils.WIDTH_CENTER_OFFSET_KEY))
                    sys.storeData(SystemUtils.WIDTH_CENTER_OFFSET_KEY, dimV.get(SystemUtils.WIDTH_CENTER_OFFSET_KEY));
                if (!sys.containsData(SystemUtils.LENGHT_KEY))
                    sys.storeData(SystemUtils.LENGHT_KEY, dimV.get(SystemUtils.LENGHT_KEY));
                if (!sys.containsData(SystemUtils.LENGHT_CENTER_OFFSET_KEY))
                    sys.storeData(SystemUtils.LENGHT_CENTER_OFFSET_KEY, dimV.get(SystemUtils.LENGHT_CENTER_OFFSET_KEY));
            }
        }
    }

    @Override
    public synchronized void update(Message arg0) {
        int mmsi = arg0.getSourceMmsi();
        switch (arg0.getType()) {
            case 1:
                if (!contacts.containsKey(mmsi))
                    contacts.put(mmsi, new AisContact(mmsi));
                contacts.get(mmsi).update((Message01) arg0);
                if (labelCache.containsKey(mmsi))
                    contacts.get(mmsi).setLabel(labelCache.get(mmsi));
                updateSystem(mmsi, contacts.get(mmsi).getLocation(), contacts.get(mmsi).getCog(),System.currentTimeMillis());
                break;
            case 3:
                if (!contacts.containsKey(mmsi))
                    contacts.put(mmsi, new AisContact(mmsi));
                contacts.get(mmsi).update((Message03) arg0);
                if (labelCache.containsKey(mmsi))
                    contacts.get(mmsi).setLabel(labelCache.get(mmsi));
                updateSystem(mmsi, contacts.get(mmsi).getLocation(), contacts.get(mmsi).getCog(),System.currentTimeMillis());
                break;
            case 5:
                if (!contacts.containsKey(mmsi))
                    contacts.put(mmsi, new AisContact(mmsi));
                contacts.get(mmsi).update((Message05) arg0);
                String name = ((Message05) arg0).getVesselName().trim();
                labelCache.put(mmsi, name);
                updateSystem(mmsi, contacts.get(mmsi).getLocation(), contacts.get(mmsi).getCog(),System.currentTimeMillis());
                break;
            default:
                NeptusLog.pub().warn("Ignoring AIS message of type " + arg0.getType());
                break;
        }
    }

    public synchronized void purge(long maximumAgeMillis) {
        Vector<Integer> toRemove = new Vector<>();

        for (Entry<Integer, AisContact> entry : contacts.entrySet()) {
            if (entry.getValue().ageMillis() > maximumAgeMillis)
                toRemove.add(entry.getKey());
        }

        for (int rem : toRemove) {
            NeptusLog.pub().debug("Removing " + rem + " because is more than " + maximumAgeMillis + " milliseconds old.");
            contacts.remove(rem);
        }
    }

    /**
     * @return the contacts
     */
    public Collection<AisContact> getContacts() {
        Vector<AisContact> c = new Vector<>();
        c.addAll(this.contacts.values());
        return c;
    }
}
