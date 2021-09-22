/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Sep 5, 2019
 */
package pt.lsts.neptus.plugins.alliance;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.alliance.NmeaPlotter.MTShip;

/**
 * @author zp
 *
 */
public class RipplesAisParser {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
    private static final SimpleDateFormat sdfIso = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    public static ArrayList<MTShip> getShips() throws Exception {
        URL url = new URL("https://ripples.lsts.pt/ais");
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();    
        JsonArray val = Json.parse(new InputStreamReader(httpConnection.getInputStream())).asArray();
       
        ArrayList<MTShip> ships = new ArrayList<>();
        
        val.forEach(s -> ships.add(parse(s.asObject())));
        
        return ships;
    }
    
    public static MTShip parse(JsonObject object) {
        MTShip ship = new MTShip();
        ship.LAT = object.getDouble("latitude", 0);
        ship.LON = object.getDouble("longitude", 0);
        ship.COURSE = object.getDouble("cog", 0);
        ship.SPEED = object.getDouble("sog", 0);
        ship.HEADING = object.getDouble("heading", 0);
        ship.SHIP_ID = object.getLong("mmsi", 0);
        ship.SHIPNAME = object.getString("name", "mmsi_"+ship.SHIP_ID);
        ship.TYPE = object.getInt("type", 0);
        ship.DESTINATION = object.getString("destination", "N/A");
        double bow = object.getDouble("bow", 0);
        double stern = object.getDouble("stern", 0);
        double port = object.getDouble("port", 0);
        double starboard = object.getDouble("starboard", 0);
        
        ship.WIDTH = (int) (port+starboard);
        ship.LENGTH = (int) (bow+stern);
        
        ship.L_FORE = (int) bow;
        ship.W_LEFT = (int) port;
        
        
        try {
            Date timestamp = sdf.parse(object.getString("timestamp", sdf.format(new Date())));
            ship.TIME = timestamp.getTime()/1000.0;
            ship.ELAPSED = (System.currentTimeMillis() - timestamp.getTime());
        }
        catch (ParseException e) {
            try { //2021-03-15T18:58:05.000+00:00
                Date timestamp = sdfIso.parse(object.getString("timestamp", sdf.format(new Date())));
                ship.TIME = timestamp.getTime()/1000.0;
                ship.ELAPSED = (System.currentTimeMillis() - timestamp.getTime());
            }
            catch (ParseException e1) {
                NeptusLog.pub().error("Error parsing date: "+object.getString("timestamp", "N/A"));
            }
        }
        
        return ship;
    }
    
    public static void main(String[] args) throws Exception {
        getShips().forEach(c -> {
            
        });
    }
}
