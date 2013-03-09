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
 * Author: José Pinto
 * Jul 11, 2012
 */
package pt.up.fe.dceg.neptus.plugins.ais;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import pt.up.fe.dceg.neptus.types.coord.LocationType;

import com.google.gson.Gson;


/**
 * @author zp
 *
 */
public class ShipInfo {

    protected String speed, country, name, type, course, lat, lon, name_resolved = null;
    protected Double lat_resolved = null, lon_resolved = null, course_resolved = null, speed_resolved = null;    
    static Gson gson = new Gson();
    
    public double getLatitudeDegs() {
        if (lat_resolved == null) {
            String[] parts = lat.split(" ");
            lat_resolved = Double.parseDouble(parts[0]);
            if (parts[1].trim().equalsIgnoreCase("s"))
                lat_resolved = -lat_resolved;
        }
        return lat_resolved;
    }
    
    public double getLongitudeDegs() {
        if (lon_resolved == null) {
            String[] parts = lon.split(" ");
            lon_resolved = Double.parseDouble(parts[0]);
            if (parts[1].trim().equalsIgnoreCase("w"))
                lon_resolved = -lon_resolved;
        }
        return lon_resolved;        
    }
    
    public String getName() {
        if (name_resolved == null) {
            name_resolved = name.split("\\(")[0].trim();
        }
        return name_resolved;
    }
    
    public double getHeadingRads() {
        if (course_resolved == null) {
            course_resolved = Math.toRadians(Double.parseDouble(course.split(" ")[0]));
        }
        return course_resolved;
    }
    
    public double getSpeedMps() {
        if (speed_resolved == null) {
            speed_resolved = Double.parseDouble(speed.split(" ")[0]);
            speed_resolved *= 0.514444;
        }
        return speed_resolved;
    }
    
    public String getCountry() {
        return country.trim();
    }
    
    public String getType() {
        return type.trim();
    }
    
    LocationType getLocation() {
        return new LocationType(getLatitudeDegs(), getLongitudeDegs());
    }
    

    public static ShipInfo getShipInfo(String mmsi) {
        try  {
            URL url = new URL("http://www.vesselfinder.com/vessels/shipinfo?mmsi="+mmsi+"&full=false&extra=false");
            InputStream is = url.openConnection().getInputStream();
            ShipInfo sinfo = gson.fromJson(new InputStreamReader(is), ShipInfo.class);
            is.close();
            return sinfo;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public static Vector<ShipInfo> getShips(double min_lat, double min_lon, double max_lat, double max_lon, boolean includeStoppedShips) {
        Vector<ShipInfo> ships = new Vector<ShipInfo>();
        try {
            URL url = new URL("http://www.vesselfinder.com/vessels/vesselsonmap?bbox="+min_lon+","+min_lat+","+max_lon+","+max_lat);            
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream()));            
            String line;
            while ((line = br.readLine()) != null) {
                
                String parts[] = line.split("\t");
                
                if (parts.length <3)
                    continue;
                
                // ship is stopped
                if (parts[3].equals("0"))
                    continue;
                
                String mmsi = parts[5];
                ShipInfo ship = getShipInfo(mmsi);
                if (ship != null)
                    ships.add(ship);                
            }
            br.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ships;
    }
    
    
    public static void main(String[] args) {
        
        
        double min_lat = 38.456394754157, min_lon = -8.9209507498921, max_lat = 38.522501475549, max_lon = -8.7436245475065;
        
        Vector<ShipInfo> ships;
        
        ships = getShips(min_lat, min_lon, max_lat, max_lon, true);
        
        for (ShipInfo s : ships) {
            System.out.println(s.getName()+"->"+s.getLocation().toString());
        }
        
    }
}
