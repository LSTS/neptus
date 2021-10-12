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
 * 23/04/2017
 */
package pt.lsts.ripples;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.TimeZone;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import pt.lsts.colormap.ColorMap;
import pt.lsts.colormap.ColorMapFactory;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.RemoteSensorInfo;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Ripples Positions", icon = "pt/lsts/ripples/pin.png")
public class RipplesPositions extends ConsoleLayer {

    @NeptusProperty
    String positionsApiUrl = GeneralPreferences.ripplesUrl + "/positions";

    private final String authKey = GeneralPreferences.ripplesApiKey;

    ColorMap cmap = ColorMapFactory.createRedYellowGreenColorMap();

    LinkedHashMap<String, PositionUpdate> lastPositions = new LinkedHashMap<>();
    LinkedHashMap<String, ArrayList<PositionUpdate> > positions = new LinkedHashMap<>();
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
    {
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    }
    Image pin = null;
    int pinWidth = 0, pinHeight = 0;

    String error = null;
    
    @Override
    public boolean userControlsOpacity() {
        return false;
    }

    @Override
    public void initLayer() {
        pin = ImageUtils.getImage("pt/lsts/ripples/pin.png");
        pinWidth = pin.getWidth(getConsole());
        pinHeight = pin.getHeight(getConsole());
    }

    @Override
    public void cleanLayer() {
        // TODO Auto-generated method stub
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        super.paint(g, renderer);

        if (error != null) {
            g.setColor(Color.RED);
            g.drawString(error, 50, 50);
        }
        
        PositionUpdate pivot = null;
        
        synchronized (lastPositions) {
            for (PositionUpdate update : lastPositions.values()) {
                if (System.currentTimeMillis() - update.timestamp.getTime() > 3600 * 1000)
                    continue;

                double age = (System.currentTimeMillis() - update.timestamp.getTime()) / 3600.0 * 1000;
                
                String date = sdf.format(update.timestamp);
                Point2D pt = renderer.getScreenPosition(update.location);
                pt.setLocation(pt.getX() - pinWidth / 2, pt.getY() - pinHeight);
                g.drawImage(pin, (int) pt.getX(), (int) pt.getY(), getConsole());
                g.setColor(cmap.getColor(age).darker().darker());
                
                g.drawString(update.id, (int) pt.getX() + pinWidth + 2, (int) pt.getY() + pinHeight / 2);
                //g.setColor(Color.black);
                g.drawString(date, (int) pt.getX() + pinWidth + 2,
                        (int) pt.getY() + pinHeight / 2 + g.getFontMetrics().getHeight());

            }
            pivot = lastPositions.get(getConsole().getMainSystem());
        }
        
        if (pivot != null) {
            String age = DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - pivot.timestamp.getTime());
            g.setColor(Color.blue.brighter());
            g.drawString(pivot.id+" updated "+age+" ago.", 11, renderer.getHeight()-29);
            
            g.setColor(Color.black);
            g.drawString(pivot.id+" updated "+age+" ago.", 10, renderer.getHeight()-30);
        }
    }

    @Periodic(millisBetweenUpdates = 5000)
    public void pollActiveSystems() {
        if (!isVisible())
            return;

        try {
            JsonParser parser = new JsonParser();
            URL url = new URL(positionsApiUrl);

            URLConnection con = url.openConnection();
            if (authKey != null && !authKey.isEmpty()) {
                con.setRequestProperty ("Authorization", authKey);
            }

            JsonElement root = parser
                    .parse(new JsonReader(new InputStreamReader(con.getInputStream())));
            JsonArray posArray = root.getAsJsonArray();
            
            
            for (JsonElement position : posArray) {
                JsonObject obj = position.getAsJsonObject();
                double latDegs = obj.get("lat").getAsDouble();
                double lonDegs = obj.get("lon").getAsDouble();
                Date time = sdf.parse(obj.get("timestamp").getAsString());
                String name = obj.get("name").getAsString();
                int id = obj.get("imcId").getAsInt();
                
                PositionUpdate update = new PositionUpdate();
                update.id = id == -1 ? name : IMCDefinition.getInstance().getResolver().resolve(id);
                if (update.id.startsWith("unknown")) {
                    update.id = name;
                    if (id != -1) {
                        IMCDefinition.getInstance().getResolver().addEntry(id, name);
                    }
                }
                update.timestamp = time;
                update.location = new LocationType(latDegs, lonDegs);
                synchronized (lastPositions) {
                    
                    PositionUpdate lastUpdate = lastPositions.get(update.id);
                    
                    if (!lastPositions.containsKey(update.id) || lastPositions.get(update.id).timestamp.before(update.timestamp))
                        lastPositions.put(update.id, update);
                    if (!positions.containsKey(update.id))
                        positions.put(update.id, new ArrayList<>());
                    positions.get(update.id).add(update);
                    
                    if (lastUpdate == null || lastUpdate.timestamp.before(update.timestamp)) {
                        NeptusLog.pub().info("Publishing RemoteSensorInfo synthesized from Ripples position of system " + update.id);
                        
                        RemoteSensorInfo rsi = new RemoteSensorInfo();
                        rsi.setSrc(id);
                        rsi.setTimestamp(time.getTime()/1000.0);
                        rsi.setLat(update.location.getLatitudeRads());
                        rsi.setLon(update.location.getLongitudeRads());
                        
                        System.out.println(rsi.getId()+" :: "+rsi.getDate());
                        
                        rsi.setSensorClass("UUV");
                        NeptusLog.pub().info("RemoteSensorInfo::" + rsi.asJSON());
                        ImcMsgManager.getManager().postInternalMessage(update.id, rsi);    
                    }                    
                }
                
            }
            error = null;
        }
        catch (Exception e) {
            error = e.getClass().getSimpleName()+" "+e.getMessage();
            e.printStackTrace();
            NeptusLog.pub().error(e);
            getConsole().post(Notification
                    .error(getName(), e.getClass().getSimpleName() + " while polling device updates from Ripples.")
                    .requireHumanAction(false));
        }
    }

    static class PositionUpdate implements Comparable<PositionUpdate> {
        public String id;
        public Date timestamp;
        public LocationType location;
        
        @Override
        public int compareTo(PositionUpdate o) {
            return timestamp.compareTo(o.timestamp);
        }
    }
    
    public static void main(String[] args) throws ParseException {
        String date = "2019-05-30T10:26:12.000+0000";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        sdf.parse(date);

        sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        String date1 = "2021-09-07T10:36:01.000+00:00";
        sdf.parse(date1);

        RipplesPositions positions = new RipplesPositions();
        positions.pollActiveSystems();
    }
}
