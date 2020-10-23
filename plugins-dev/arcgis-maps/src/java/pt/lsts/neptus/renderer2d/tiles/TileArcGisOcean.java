/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * 25/03/2018
 */
package pt.lsts.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.MapTileProvider;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "ArcGis Ocean", isBaseMapOrLayer = true)
public class TileArcGisOcean extends TileHttpFetcher implements ConfigurationListener {

    private static final long serialVersionUID = 5217292966729763087L;

    protected static String tileClassId = TileArcGisOcean.class.getSimpleName();

    private static Map<String, TileArcGisOcean> tilesMap = Collections.synchronizedMap(new HashMap<String, TileArcGisOcean>());

    private static final int MAX_LEVEL_OF_DETAIL = 13;
    
    private static String BASE_URL = "http://services.arcgisonline.com/ArcGIS/rest/services/";
    private static String LAYER_MAP = "Ocean_Basemap";
    private static String BASE_URL_END = "/MapServer/tile/";

    public TileArcGisOcean(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image)
            throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileArcGisOcean(String id) throws Exception {
        super(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
    }

    public static int getMaxLevelOfDetail() {
        return MAX_LEVEL_OF_DETAIL;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.renderer2d.tiles.Tile#getTransparencyToApplyToImage()
     */
    @Override
    protected float getTransparencyToApplyToImage() {
        return 1;
    }

    /**
     * @return
     */
    @Override
    protected String createTileRequestURL() {
        if (levelOfDetail > MAX_LEVEL_OF_DETAIL)
            return "http://non-existing-url.nope/";
        
        // http://services.arcgisonline.com/ArcGIS/rest/services/Ocean_Basemap/MapServer/tile/10/399/169
        String urlGet = BASE_URL + LAYER_MAP + BASE_URL_END + levelOfDetail + "/" + tileY + "/" + (tileX);
        NeptusLog.pub().debug("<###> "+urlGet);
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
