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
 * 07/03/2013
 */
package pt.lsts.neptus.renderer2d.tiles;

import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.plugins.NeptusProperty;

/**
 * From http://webapp.navionics.com/
 * @author pdias
 *
 */
@MapTileProvider(name = "NV Map Overlay", isBaseMapOrLayer = true)
public class TileNVMaps extends TileHttpFetcher implements ConfigurationListener {
    
    //TODO put copyright text to show in renderer "© NVMap contributors"
    
    private static final long serialVersionUID = -6223894220961990645L;

    protected static String tileClassId = TileNVMaps.class.getSimpleName();

    private static Map<String, TileNVMaps> tilesMap = Collections.synchronizedMap(new HashMap<String, TileNVMaps>());

    private static final int MAX_LEVEL_OF_DETAIL = 22;
    private static String BASE_URL = "http://d2hcl9zx8watk4.cloudfront.net/tile/";
    private static String LAYERS_URL = "?LAYERS=config_1_20.00_1&TRANSPARENT=FALSE&UGC=TRUE&navtoken=TmF2aW9uaWNzX2ludGVybmFscHVycG9zZV8wMDAwMSt3ZWJhcHAubmF2aW9uaWNzLmNvbSt4bjFhN2R4dGo0aQ==";
    
    @NeptusProperty(name = "Base URL", description = "The first URL part without tiles info and ending in '/'.")
    private static String baseUrl = BASE_URL;

    @NeptusProperty(name = "Layers URL Part", 
            description = "The layers, 'LAYERS=config_1_20.00_1', '1' should be one, '20.00' is the safety depth you want and the last '1' means finer bathymetry, (for lower detail use '0'). Also 'navtoken=xxx' the token to use, may change.")
    private static String layersUrl = LAYERS_URL;

    public TileNVMaps(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileNVMaps(String id) throws Exception {
        super(id);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (baseUrl.length() == 0)
            baseUrl = BASE_URL;
        if (layersUrl.length() == 0)
            layersUrl = LAYERS_URL;
    }
    
    public static int getMaxLevelOfDetail() {
        return MAX_LEVEL_OF_DETAIL;
    }

    /* (non-Javadoc)
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
        if (baseUrl.length() == 0)
            baseUrl = BASE_URL;
        if (layersUrl.length() == 0)
            layersUrl = LAYERS_URL;

        // zoom/x/y
        // http://d2hcl9zx8watk4.cloudfront.net/tile/10/488/383?LAYERS=config_1_1_0&TRANSPARENT=FALSE&navtoken=TmF2aW9uaWNzX2ludGVybmFscHVycG9zZV8wMDAwMSt3ZWJhcHAubmF2aW9uaWNzLmNvbQ==
        // http://d2hcl9zx8watk4.cloudfront.net/tile/11/975/767?LAYERS=config_1_1_0&TRANSPARENT=FALSE&navtoken=TmF2aW9uaWNzX2ludGVybmFscHVycG9zZV8wMDAwMSt3ZWJhcHAubmF2aW9uaWNzLmNvbQ==
//        String urlGet = "http://d2hcl9zx8watk4.cloudfront.net/tile/" + levelOfDetail + "/" + tileX + "/" + tileY 
//                + "?LAYERS=config_1_1_0&TRANSPARENT=FALSE&navtoken=TmF2aW9uaWNzX2ludGVybmFscHVycG9zZV8wMDAwMSt3ZWJhcHAubmF2aW9uaWNzLmNvbQ==";
        String urlGet = baseUrl + levelOfDetail + "/" + tileX + "/" + tileY + layersUrl;
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
