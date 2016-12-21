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
 * 8/10/2011
 */
package pt.lsts.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.http.HttpHost;
import org.apache.http.conn.routing.HttpRoute;

import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.util.coord.MapTileUtil;


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

    private static boolean alreadyInitialize = false;
//    {
//        httpConnectionManager.setMaxPerRoute(new HttpRoute(new HttpHost(HOST)), 1); // was setMaxForRoute
//    }
    
    public TileGoogleMaps(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
        initialize();
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileGoogleMaps(String id) throws Exception {
        super(id);
        initialize();
    }

    private synchronized void initialize() {
        if (alreadyInitialize)
            return;
        alreadyInitialize = true;
        httpComm.getHttpConnectionManager().setMaxPerRoute(new HttpRoute(new HttpHost(HOST)), 1); // was setMaxForRoute
    }

    /**
     * @return
     */
    @Override
    protected String createTileRequestURL() {
        double[] ret = MapTileUtil.xyToDegrees(worldX+256/2, worldY+256/2, levelOfDetail);
        
        String urlGet = "http://" + HOST + "/maps/api/staticmap?center=" + ret[0] + ","
                + ret[1] + "&zoom=" + levelOfDetail + "&size=256x256&sensor=false";
        return urlGet;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.TileHttpFetcher#getWaitTimeMillisToSeparateConnections()
     */
    @Override
    protected long getWaitTimeMillisToSeparateConnections() {
        return (long) ((!isInStateForbidden()?800:5000) + ((!isInStateForbidden()?500:5000) * rnd.nextDouble()));
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.TileHttpFetcher#getWaitTimeLock()
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
