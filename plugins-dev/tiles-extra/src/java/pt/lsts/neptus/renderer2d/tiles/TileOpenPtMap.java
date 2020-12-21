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

import pt.lsts.neptus.plugins.MapTileProvider;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "Open Pt Map Overlay", isBaseMapOrLayer = false)
public class TileOpenPtMap extends TileHttpFetcher {
    
    //TODO put copyright text to show in renderer "© OpenStreetMap contributors"
    
    private static final long serialVersionUID = -6223894220961990674L;

    protected static String tileClassId = TileOpenPtMap.class.getSimpleName();

    private static Map<String, TileOpenPtMap> tilesMap = Collections.synchronizedMap(new HashMap<String, TileOpenPtMap>());

    private static final int MAX_LEVEL_OF_DETAIL = 18;

    public TileOpenPtMap(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileOpenPtMap(String id) throws Exception {
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
        String urlGet = "http://www.openptmap.org/tiles/" + levelOfDetail + "/" + tileX + "/" + tileY + ".png";
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
