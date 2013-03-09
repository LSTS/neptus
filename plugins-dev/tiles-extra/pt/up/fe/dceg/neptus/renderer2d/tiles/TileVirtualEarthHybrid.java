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

import pt.up.fe.dceg.neptus.plugins.MapTileProvider;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "Virtual Earth Hybrid")
public class TileVirtualEarthHybrid extends TileVirtualEarth {
    
    private static final long serialVersionUID = -1827702213399356919L;

    protected static String tileClassId = TileVirtualEarthHybrid.class.getSimpleName();

    private static Map<String, TileVirtualEarthHybrid> tilesMap = Collections.synchronizedMap(new HashMap<String, TileVirtualEarthHybrid>());

    public TileVirtualEarthHybrid(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileVirtualEarthHybrid(String id) throws Exception {
        super(id);
    }
    
    /**
     * @return
     */
    @Override
    protected String createTileRequestURL() {
        int server = (int) (4 * rnd.nextFloat()); // server r0, r1, r2, and r3
        String urlGet = "http://r" + server + ".ortho.tiles.virtualearth.net/tiles/"
                + "h" + id + ".png?g=1";
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
