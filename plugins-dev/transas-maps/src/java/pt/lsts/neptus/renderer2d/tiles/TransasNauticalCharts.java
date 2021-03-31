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

import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.plugins.NeptusProperty;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "Transas Nautical Charts", isBaseMapOrLayer = false)
public class TransasNauticalCharts extends TileHttpFetcher implements ConfigurationListener {

    private static final long serialVersionUID = -6223894220961990645L;

    protected static String tileClassId = TransasNauticalCharts.class.getSimpleName();

    private static Map<String, TransasNauticalCharts> tilesMap = Collections.synchronizedMap(new HashMap<String, TransasNauticalCharts>());

    private static final int MAX_LEVEL_OF_DETAIL = 18;
    private static String BASE_URL = "http://wms.transas.com/TMS/1.0.0/TX97-transp/";
    private static String TOKEN = "9e53bcb2-01d0-46cb-8aff-512e681185a4";

    @NeptusProperty(name = "Base URL", description = "The first URL part")
    private static String baseUrl = BASE_URL;

    @NeptusProperty(name = "Token", description = "Transas token key")
    private static String token = TOKEN;

    public TransasNauticalCharts(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image)
            throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TransasNauticalCharts(String id) throws Exception {
        super(id);
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        if (baseUrl.length() == 0)
            baseUrl = BASE_URL;
        if (token.length() == 0)
            token = TOKEN;
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
        if (baseUrl.length() == 0)
            baseUrl = BASE_URL;
        if (token.length() == 0)
            token = TOKEN;

        if (levelOfDetail > 17)
            return "http://non-existing-url.nope/";
        int max = (int)  Math.pow(2, levelOfDetail)-1;
        
        String urlGet = baseUrl + levelOfDetail + "/" + tileX + "/" + (max-tileY) + ".png?token="+token;
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
