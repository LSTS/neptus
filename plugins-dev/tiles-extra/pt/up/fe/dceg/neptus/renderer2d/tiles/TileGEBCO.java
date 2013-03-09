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
 */
package pt.up.fe.dceg.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;

import pt.up.fe.dceg.neptus.plugins.MapTileProvider;
import pt.up.fe.dceg.neptus.util.coord.MapTileUtil;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "www.GEBCO.net")
public class TileGEBCO extends TileHttpFetcher {
    
    /*
     * http://www.gebco.net/data_and_products/gebco_web_services/web_map_service/#getmap
     
        Data set acknowledgement: "Imagery reproduced from the GEBCO_08 Grid, version 20100927, www.gebco.net"
        
        http://www.gebco.net/data_and_products/gebco_web_services/web_map_service/mapserv?request=getmap&service=wms&BBOX=-12.7,35,5,44&srs=EPSG:4326&format=image/jpeg&layers=gebco_08_grid&width=256&height=256&version=1.1.1
        
        Only works through proxy (this one works 50.22.206.179:8080).
    */

    private static final long serialVersionUID = -7203527367652271493L;

    protected static String tileClassId = TileGEBCO.class.getSimpleName();

    private static Map<String, TileGEBCO> tilesMap = Collections.synchronizedMap(new HashMap<String, TileGEBCO>());

    private static final int MAX_LEVEL_OF_DETAIL = 20;
    
    static {
        httpConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost("www.gebco.net")), 4); // was setMaxForRoute
    }

    public TileGEBCO(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileGEBCO(String id) throws Exception {
        super(id);
    }

    public static int getMaxLevelOfDetail() {
        return MAX_LEVEL_OF_DETAIL;
    }
    
    /**
     * @return
     */
    @Override
    protected String createTileRequestURL() {
        double lat1 = 35, lat2 = 44, lon1 = -12.7, lon2 = 5;
        double[] ret1 = MapTileUtil.XYToDegrees(worldX, worldY, levelOfDetail);
        double[] ret2 = MapTileUtil.XYToDegrees(worldX + 256, worldY + 256, levelOfDetail);

        lat1 = Math.min(ret1[0], ret2[0]);
        lat2 = Math.max(ret1[0], ret2[0]);
        lon1 = Math.min(ret1[1], ret2[1]);
        lon2 = Math.max(ret1[1], ret2[1]);
        
        String urlGet = "http://www.gebco.net/data_and_products/gebco_web_services/" +
        		"web_map_service/mapserv?request=getmap&service=wms&" +
        		"BBOX=" + lon1 + "," + lat1 + "," + lon2 + "," + lat2 +
        		"&srs=EPSG:4326&format=image/jpeg&layers=gebco_08_grid&" +
        		"width=256&height=256&version=1.1.1";
        System.out.println(urlGet);
        return urlGet;
    }
    
    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.renderer2d.tiles.TileHttpFetcher#getWaitTimeMillisToSeparateConnections()
     */
    @Override
    protected long getWaitTimeMillisToSeparateConnections() {
        return (long) (10 * rnd.nextDouble());
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
}
