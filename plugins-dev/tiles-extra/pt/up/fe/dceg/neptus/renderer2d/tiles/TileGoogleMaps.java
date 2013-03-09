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
 * 8/10/2011
 */
package pt.up.fe.dceg.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;

import pt.up.fe.dceg.neptus.plugins.MapTileProvider;
import pt.up.fe.dceg.neptus.util.coord.MapTileUtil;


/**
 * @author pdias
 *
 */
@MapTileProvider(name = "Google Maps")
public class TileGoogleMaps extends TileHttpFetcher {

    // http://mt1.google.com/vt/lyrs=m@162000000&hl=pt-PT&src=api&x=0&y=0&z=0
    
    // http://maps.google.com/maps/api/staticmap?center=0,0&zoom=0&size=256x256&sensor=false 
    //     maptype=roadmap, satellite, hybrid, and terrain
    //     format=PNG, GIF, and JPEG 
    // http://code.google.com/intl/pt-PT/apis/maps/documentation/staticmaps/

    private static final long serialVersionUID = 536559879996297467L;

    private static final String HOST = "maps.google.com";

    private static final ReentrantLock lock = new ReentrantLock();

    protected static String tileClassId = TileGoogleMaps.class.getSimpleName();
    
    private static Map<String, TileGoogleMaps> tilesMap = Collections.synchronizedMap(new HashMap<String, TileGoogleMaps>());

    static {
        httpConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(HOST)), 1); // was setMaxForRoute
    }
    
    public TileGoogleMaps(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileGoogleMaps(String id) throws Exception {
        super(id);
    }

//    public static String getTileStyleID() {
//        return "Google Maps";
//    }

    /**
     * @return
     */
    @Override
    protected String createTileRequestURL() {
        double[] ret = MapTileUtil.XYToDegrees(worldX+256/2, worldY+256/2, levelOfDetail);
        
        String urlGet = "http://" + HOST + "/maps/api/staticmap?center=" + ret[0] + ","
                + ret[1] + "&zoom=" + levelOfDetail + "&size=256x256&sensor=false";
        return urlGet;
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.tiles.TileHttpFetcher#getWaitTimeMillisToSeparateConnections()
     */
    @Override
    protected long getWaitTimeMillisToSeparateConnections() {
        return (long) ((!isInStateForbidden()?800:5000) + ((!isInStateForbidden()?500:5000) * rnd.nextDouble()));
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.tiles.TileHttpFetcher#getWaitTimeLock()
     */
    @Override
    protected ReentrantLock getWaitTimeLock() {
        return lock;
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
    
    public static boolean isFetchableOrGenerated() {
        return false;
    }
}
