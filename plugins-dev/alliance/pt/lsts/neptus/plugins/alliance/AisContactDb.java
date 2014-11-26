/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.systems.external.ExternalSystem;
import pt.lsts.neptus.systems.external.ExternalSystemsHolder;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.util.NMEAUtils;
import de.baderjene.aistoolkit.aisparser.AISObserver;
import de.baderjene.aistoolkit.aisparser.message.Message;
import de.baderjene.aistoolkit.aisparser.message.Message01;
import de.baderjene.aistoolkit.aisparser.message.Message03;
import de.baderjene.aistoolkit.aisparser.message.Message05;

/**
 * @author zp
 *
 */
public class AisContactDb implements AISObserver {

    private LinkedHashMap<Integer, AisContact> contacts = new LinkedHashMap<>();
    private LinkedHashMap<Integer, String> labelCache = new LinkedHashMap<>();
    File cache = new File("conf/ais.cache");

    public AisContactDb() {
        if (!cache.canRead())
            return;
        int count = 0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(cache));
            String line = reader.readLine();
            while (line != null) {
                String[] parts = line.split(",");
                int mmsi = Integer.parseInt(parts[0]);
                String name = parts[1].trim();    
                labelCache.put(mmsi, name);
                line = reader.readLine();
                count++;
            }
            reader.close();
            System.out.println("Read "+count+" vessel names from "+cache.getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveCache() {
        int count = 0;
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(cache));

            for (Entry<Integer,String> entry : labelCache.entrySet()) {
                writer.write(entry.getKey()+","+entry.getValue()+"\n");     
                count++;
            }
            writer.close();
            System.out.println("Wrote "+count+" vessel names to "+cache.getAbsolutePath());
        }
        catch (Exception e) {
            e.printStackTrace();
        }        
    }

    
    private String lastGGA = null;
    public void processGGA(String sentence) {
        lastGGA = sentence;
        //System.err.println(lastGGA);
    }
    
    public void processRattm(String sentence) {
        if (lastGGA == null)
            return;
        //$GPGGA,132446.00,3811.805048,N,00856.490411,W,2,06,1.2,30.5,M,49.7,M,3.8,0000*6F
        LocationType myLoc = NMEAUtils.processGGASentence(lastGGA);
        //$RATTM,17,1.25,60.9,T,7.9,358.0,T,1.11,-4.3,N,wb,T,,133714,M*27
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
            id = "radar-"+parts[1];
        
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
        //$A-TLL,1,4330.60542,N,01623.45716,E,IVER-UPCT,,,315.4*02
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
            NeptusLog.pub().debug("Unable to parse coordinates in "+sentence);
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
            //e.printStackTrace();
        }
        
        LocationType loc = new LocationType(lat, lon);
        
        if (!contacts.containsKey(mmsi)) {
            AisContact contact = new AisContact(mmsi);
            contacts.put(mmsi, contact);
        }
        //System.out.println(mmsi);
        AisContact contact = contacts.get(mmsi);
        contact.setLocation(loc);
        contact.setCog(heading);
        contact.setLabel(id);
        if (ImcSystemsHolder.getSystemWithName(id) == null)
            updateSystem(mmsi, loc, heading);
    }

    public void updateSystem(int mmsi, LocationType loc, double heading) {
        String name = contacts.get(mmsi).getLabel();
        if (name.equals(""+mmsi))
            return;
        
        ExternalSystem sys = ExternalSystemsHolder.lookupSystem(name);
        if (sys == null) {
            sys = new ExternalSystem(name);
            ExternalSystemsHolder.registerSystem(sys);
        }
        sys.setLocation(contacts.get(mmsi).getLocation());
        sys.setAttitudeDegrees(contacts.get(mmsi).getCog());
        sys.setType(SystemTypeEnum.UNKNOWN);
    }
    
    @Override
    public synchronized void update(Message arg0) {
        int mmsi = arg0.getSourceMmsi();
        switch (arg0.getType()) {
            case 1:
                if (!contacts.containsKey(mmsi))
                    contacts.put(mmsi, new AisContact(mmsi));
                contacts.get(mmsi).update((Message01)arg0);
                if (labelCache.containsKey(mmsi))
                    contacts.get(mmsi).setLabel(labelCache.get(mmsi));
                updateSystem(mmsi, contacts.get(mmsi).getLocation(), contacts.get(mmsi).getCog());
                break;
            case 3:
                if (!contacts.containsKey(mmsi))
                    contacts.put(mmsi, new AisContact(mmsi));
                contacts.get(mmsi).update((Message03)arg0);
                if (labelCache.containsKey(mmsi))
                    contacts.get(mmsi).setLabel(labelCache.get(mmsi));
                updateSystem(mmsi, contacts.get(mmsi).getLocation(), contacts.get(mmsi).getCog());
                break;
            case 5:
                if (!contacts.containsKey(mmsi))
                    contacts.put(mmsi, new AisContact(mmsi));
                contacts.get(mmsi).update((Message05)arg0);
                String name = ((Message05)arg0).getVesselName();
                labelCache.put(mmsi, name);
                updateSystem(mmsi, contacts.get(mmsi).getLocation(), contacts.get(mmsi).getCog());
                break;
            default:
                System.err.println("Ignoring AIS message of type "+arg0.getType());
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
            System.out.println("Removing "+rem+" because is more than "+maximumAgeMillis+" milliseconds old.");
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
