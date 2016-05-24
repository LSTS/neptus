/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Paulo Dias
 * 9/10/2011
 */
package pt.lsts.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "S57 Client (HTTP)")
public class TileS57 extends TileHttpFetcher {
    
    private static final long serialVersionUID = 3034418505820721067L;

    protected static String tileClassId = TileS57.class.getSimpleName();

    private static Map<String, TileS57> tilesMap = Collections.synchronizedMap(new HashMap<String, TileS57>());
    
    public static final int MAX_LEVEL_OF_DETAIL = 22;

    @NeptusProperty(name="Map Server URL", description="The URL to the S57 Map Server. " +
    		"[default='http://localhost:8082/map/s57/png?']")
    public static String mapServerURL = "http://localhost:8082/map/s57/png?";

    private static boolean alreadyInitialize = false;
    
//    {
//        try {
//            String confFx = "conf/" + TileS57.class.getSimpleName().toLowerCase() + ".properties";
//            if (new File(confFx).exists())
//                PluginUtils.loadProperties(confFx, TileS57.class);
//        }
//        catch (Exception e) {
//            NeptusLog.pub().error("Not possible to open \"conf/" + TileS57.class.getSimpleName().toLowerCase() + ".properties\"");
//            NeptusLog.pub().debug(e.getMessage());
//        }
//        
//        try {
//            URL url = new URL(mapServerURL);
//            httpConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(url.getHost())), 1); // was setMaxForRoute
//        }
//        catch (MalformedURLException e) {
//            e.printStackTrace();
//        }
//    }
    
    public TileS57(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
        initialize();
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileS57(String id) throws Exception {
        super(id);
        initialize();
    }

    private synchronized void initialize() {
        if (alreadyInitialize )
            return;
        alreadyInitialize = true;

        try {
            String confFx = "conf/" + TileS57.class.getSimpleName().toLowerCase() + ".properties";
            if (new File(confFx).exists())
                PluginUtils.loadProperties(confFx, TileS57.class);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Not possible to open \"conf/" + TileS57.class.getSimpleName().toLowerCase() + ".properties\"");
            NeptusLog.pub().debug(e.getMessage());
        }

        try {
            URL url = new URL(mapServerURL);
            httpComm.getHttpConnectionManager().setMaxPerRoute(new HttpRoute(new HttpHost(url.getHost())), 1); // was setMaxForRoute
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * @return the mapServerURL
     */
    public static String getMapServerURL() {
        return mapServerURL;
    }
    
    /**
     * @param mapServerURL the mapServerURL to set
     */
    public static void setMapServerURL(String mapServerURL) {
        TileS57.mapServerURL = mapServerURL;
    }
    
    @Override
    protected float getTransparencyToApplyToImage() {
        return 1f;
    }

    /**
     * @return
     */
    @Override
    protected String createTileRequestURL() {
     // URI := /map/s57/png?
        // q=lat1,lon1,lat2,lon2,width,height
        // cs=DAY (DUSK or NIGHT)
        // dc=all (base or standard)
        // dsp=true (display simple point symbols)
        // dsa=true (display simple area boundaries)
        // sf=true (ScaleFilter)
        // ss=true (Show Soundings)
        // sc=true (Show Contours Labels)
        // ssw=10 (Set Safe Water)
        // svsw=5 (Set Very Shallow Water)
        // svdw=20 (Set Very Deep Water)

        double[] locTL = MapTileUtil.xyToDegrees(worldX, worldY, levelOfDetail);
        double[] locBR = MapTileUtil.xyToDegrees(worldX + 256, worldY + 256, levelOfDetail);
        String requestQuery = "q=" + locTL[0]
                + "," + locTL[1]
                + "," + locBR[0]
                + "," + locBR[1]
                + "," + 256
                + "," + 256
                + "&cs=" + "DAY"
                + "&dc=" + "all"
                + "&dsp=" + true
                + "&dsa=" + true
                + "&sf=" + true
                + "&ss=" + true
                + "&ssw=" + 10
                + "&svsw=" + 5
                + "&svdw=" + 20;
        String urlGet = mapServerURL
                + (mapServerURL.endsWith("?") ? "" : "?")
                + requestQuery;
        
        return urlGet;
    }
    
    public static void staticPropertiesChanged() {
        try {
            PluginUtils.saveProperties("conf/" + TileS57.class.getSimpleName().toLowerCase() + ".properties", TileS57.class);
        }
        catch (Exception e) {
            NeptusLog.pub().debug(e.getMessage());
            NeptusLog.pub().error("Not possible to open \"conf/" + TileS57.class.getSimpleName().toLowerCase() + ".properties\"");
        }

        try {
            URL url = new URL(mapServerURL);
            httpComm.getHttpConnectionManager().setMaxPerRoute(new HttpRoute(new HttpHost(url.getHost())), 1); // was setMaxForRoute
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

//    public static String getTileStyleID() {
//        return "S57";
//    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.TileHttpFetcher#getWaitTimeMillisToSeparateConnections()
     */
    @Override
    protected long getWaitTimeMillisToSeparateConnections() {
        return 10;
    }

    /**
     * @return the tilesMap
     */
    @SuppressWarnings("unchecked")
    public static <T extends Tile> Map<String, T> getTilesMap() {
        return (Map<String, T>) tilesMap;
    }

    /**
     * 
     */
    public static void clearDiskCache() {
        Tile.clearDiskCache(tileClassId);
    }

    /**
     * @return 
     * 
     */
    public static <T extends Tile> Vector<T> loadCache() {
        return Tile.loadCache(tileClassId);
    }
    

    public static void main(String[] args) {
        Class<TileS57> clazz = TileS57.class;
        Vector<Field> dFA = new Vector<Field>();
        PluginUtils.extractFieldsWorker(clazz, dFA);
        final LinkedHashMap<String, PluginProperty> props = new LinkedHashMap<String, PluginProperty>();
        for (Field field : dFA) {
            try {
                PluginProperty pp = PluginUtils.createPluginProperty(null, field);
                if (pp != null)
                    props.put(pp.getName(), pp);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        GuiUtils.printList(dFA);
        PropertiesProvider pprov = new PropertiesProvider() {
            @Override
            public void setProperties(Property[] properties) {
                PluginUtils.setPluginProperties(TileS57.class, properties);
            }
            
            public String[] getPropertiesErrors(Property[] properties) {
                // TODO Auto-generated method stub
                return null;
            }
            
            public String getPropertiesDialogTitle() {
                return "Tile";
            }
            
            @Override
            public DefaultProperty[] getProperties() {
                return props.values().toArray(new PluginProperty[0]);
            }
        };
        PropertiesEditor.editProperties(pprov, true);
        PropertiesEditor.editProperties(pprov, true);
    }
}
