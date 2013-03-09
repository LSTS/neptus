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
 * 07/03/2013
 */
package pt.up.fe.dceg.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import pt.up.fe.dceg.neptus.plugins.MapTileProvider;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "Open Sea Map Overlay")
public class TileOpenSeaMap extends TileHttpFetcher {
    
    //TODO put copyright text to show in renderer "© OpenStreetMap contributors"
    
    private static final long serialVersionUID = -6223894220961990674L;

    protected static String tileClassId = TileOpenSeaMap.class.getSimpleName();

    private static Map<String, TileOpenSeaMap> tilesMap = Collections.synchronizedMap(new HashMap<String, TileOpenSeaMap>());

    private static final int MAX_LEVEL_OF_DETAIL = 18;

    public TileOpenSeaMap(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileOpenSeaMap(String id) throws Exception {
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
        // zoom/x/y
        // http://c.tile.openstreetmap.org/14/7801/6129.png
        // http://tiles.openseamap.org/seamark/15/17485/10492.png
        String urlGet = "http://www.openptmap.org/tiles/" + levelOfDetail + "/" + tileX + "/" + tileY + ".png";
        urlGet = "http://tiles.openseamap.org/seamark/" + levelOfDetail + "/" + tileX + "/" + tileY + ".png";
        return urlGet;
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
