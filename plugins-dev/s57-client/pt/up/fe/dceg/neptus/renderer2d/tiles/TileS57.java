/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 9/10/2011
 * $Id:: TileS57.java 9615 2012-12-30 23:08:28Z pdias                           $:
 */
package pt.up.fe.dceg.neptus.renderer2d.tiles;

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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.plugins.MapTileProvider;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginProperty;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.coord.MapTileUtil;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

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
    
    static {
        try {
            String confFx = "conf/" + TileS57.class.getSimpleName().toLowerCase() + ".properties";
            if (new File(confFx).exists())
                PluginUtils.loadProperties(confFx, TileS57.class);
        }
        catch (Exception e) {
           NeptusLog.pub().error("Not possible to open \"conf/" + TileS57.class.getSimpleName().toLowerCase() + ".properties\"");
        }
        
        try {
            URL url = new URL(mapServerURL);
            httpConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(url.getHost())), 1); // was setMaxForRoute
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
    
    public TileS57(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileS57(String id) throws Exception {
        super(id);
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

        double[] locTL = MapTileUtil.XYToDegrees(worldX, worldY, levelOfDetail);
        double[] locBR = MapTileUtil.XYToDegrees(worldX + 256, worldY + 256, levelOfDetail);
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
           NeptusLog.pub().error("Not possible to open \"conf/" + TileS57.class.getSimpleName().toLowerCase() + ".properties\"");
        }

        try {
            URL url = new URL(mapServerURL);
            httpConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(url.getHost())), 1); // was setMaxForRoute
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

//    public static String getTileStyleID() {
//        return "S57";
//    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.tiles.TileHttpFetcher#getWaitTimeMillisToSeparateConnections()
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
