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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 9/10/2011
 */
package pt.lsts.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.util.coord.MapTileUtil;


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

    private static boolean alreadyInitialize = false;
    
//    {
//        httpConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost("www.gebco.net")), 4); // was setMaxForRoute
//    }

    public TileGEBCO(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
        initialize();
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileGEBCO(String id) throws Exception {
        super(id);
        initialize();
    }

    public static int getMaxLevelOfDetail() {
        return MAX_LEVEL_OF_DETAIL;
    }
    
    private synchronized void initialize() {
        if (alreadyInitialize)
            return;
        alreadyInitialize = true;
        httpComm.getHttpConnectionManager().setMaxPerRoute(new HttpRoute(new HttpHost("www.gebco.net")), 4); // was setMaxForRoute
    }
    
    /**
     * @return
     */
    @Override
    protected String createTileRequestURL() {
        double lat1 = 35, lat2 = 44, lon1 = -12.7, lon2 = 5;
        double[] ret1 = MapTileUtil.xyToDegrees(worldX, worldY, levelOfDetail);
        double[] ret2 = MapTileUtil.xyToDegrees(worldX + 256, worldY + 256, levelOfDetail);

        lat1 = Math.min(ret1[0], ret2[0]);
        lat2 = Math.max(ret1[0], ret2[0]);
        lon1 = Math.min(ret1[1], ret2[1]);
        lon2 = Math.max(ret1[1], ret2[1]);
        
        String urlGet = "http://www.gebco.net/data_and_products/gebco_web_services/" +
        		"web_map_service/mapserv?request=getmap&service=wms&" +
        		"BBOX=" + lon1 + "," + lat1 + "," + lon2 + "," + lat2 +
        		"&srs=EPSG:4326&format=image/jpeg&layers=gebco_08_grid&" +
        		"width=256&height=256&version=1.1.1";
        NeptusLog.pub().info("<###> "+urlGet);
        return urlGet;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.TileHttpFetcher#getWaitTimeMillisToSeparateConnections()
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
