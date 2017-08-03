/*
 * Copyright (c) 2004-2017 Universidade do Porto - Faculdade de Engenharia
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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import pt.lsts.colormap.ColorMap;
import pt.lsts.colormap.ColorMapFactory;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayer;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name = "Ripples Positions", icon = "pt/lsts/ripples/pin.png")
public class RipplesPositions extends ConsoleLayer {

    @NeptusProperty
    String firebasePath = "https://neptus.firebaseio.com/";

    ColorMap cmap = ColorMapFactory.createRedYellowGreenColorMap();

    LinkedHashMap<String, PositionUpdate> positions = new LinkedHashMap<>();
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.S Z");
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
        
        synchronized (positions) {
            for (PositionUpdate update : positions.values()) {
                if (System.currentTimeMillis() - update.timestamp.getTime() > 3600 * 1000)
                    continue;

                double age = (System.currentTimeMillis() - update.timestamp.getTime()) / 3600.0 * 1000;

                String date = sdf.format(update.timestamp);
                Point2D pt = renderer.getScreenPosition(update.location);
                pt.setLocation(pt.getX() - pinWidth / 2, pt.getY() - pinHeight);
                g.drawImage(pin, (int) pt.getX(), (int) pt.getY(), getConsole());
                g.setColor(cmap.getColor(age).darker().darker());
                
                g.drawString(update.id, (int) pt.getX() + pinWidth + 2, (int) pt.getY() + pinHeight / 2);
                g.setColor(Color.black);
                g.drawString(date, (int) pt.getX() + pinWidth + 2,
                        (int) pt.getY() + pinHeight / 2 + g.getFontMetrics().getHeight());

            }
            pivot = positions.get(getConsole().getMainSystem());
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
            URL url = new URL(firebasePath.trim() + (firebasePath.trim().endsWith("/") ? "" : "/") + ".json");

            JsonElement root = parser
                    .parse(new JsonReader(new InputStreamReader(url.openConnection().getInputStream())));
            Set<Entry<String, JsonElement>> assets = root.getAsJsonObject().get("assets").getAsJsonObject().entrySet();

            for (Entry<String, JsonElement> asset : assets) {
                long updatedAt = asset.getValue().getAsJsonObject().get("updated_at").getAsLong();
                JsonElement position = asset.getValue().getAsJsonObject().get("position");
                if (position == null)
                    continue;

                double latDegs = position.getAsJsonObject().get("latitude").getAsDouble();
                double lonDegs = position.getAsJsonObject().get("longitude").getAsDouble();

                PositionUpdate update = new PositionUpdate();
                update.id = asset.getKey();
                update.timestamp = new Date(updatedAt);
                update.location = new LocationType(latDegs, lonDegs);

                synchronized (positions) {
                    positions.put(update.id, update);
                }
            }
            error = null;
        }
        catch (Exception e) {
            error = e.getClass().getSimpleName()+" "+e.getMessage();
            NeptusLog.pub().error(e);
            getConsole().post(Notification
                    .error(getName(), e.getClass().getSimpleName() + " while polling device updates from Ripples.")
                    .requireHumanAction(false));
        }
    }

    static class PositionUpdate {
        public String id;
        public Date timestamp;
        public LocationType location;
    }

}
