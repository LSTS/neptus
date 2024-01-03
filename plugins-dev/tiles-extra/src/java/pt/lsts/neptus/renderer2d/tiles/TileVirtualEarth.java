/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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

import pt.lsts.neptus.plugins.MapTileProvider;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "Virtual Earth")
public class TileVirtualEarth extends TileHttpFetcher {
    
    /*
    * http://r0.ortho.tiles.virtualearth.net/tiles/r0.png?g=1
    * http://r1.ortho.tiles.virtualearth.net/tiles/r1.png?g=1
    * http://r2.ortho.tiles.virtualearth.net/tiles/r2.png?g=1
    * http://r3.ortho.tiles.virtualearth.net/tiles/r3.png?g=1 
    * As you zoom in, the
    * tiles are further subdivided, so when you move to zoom level 2, the upper
    * right quarter divides into four tiles 10, 11, 12 and 13. This indexing system
    * continues recursively, allowing the earth's surface to be broken up into a
    * grid, with each square represented by a number which we'll refer to as its
    * quad key. As you can see, server r0 always delivers top left squares, server
    * r1 top right and so on. The significance of the "r" is road - other
    * designators include "a" for aerial and "h" for hybrid. Aerial and hybrid
    * images are delivered from a different set of tile servers.
    * http://r2.ortho.tiles.virtualearth.net/tiles/r120202001322.png?g=22
    * 
    * String base = "http://r0.ortho.tiles.virtualearth.net/tiles/r";
    //count = (++count) % 4;
    //NeptusLog.pub().info("<###>count="+count);
    return base + getSatURLString(x, y, zoom)+".png?g=1";
    
    *  private String getSatURLString (int x, int y, int z) {
           String ret = "";
           for (int i = z; i >= 0; i--) {
               int mask = 1 << (i - 1);
               int cell = 0;
               if ((x & mask) != 0)
                   cell += 1;
               if ((y & mask) != 0)
                   cell += 2;
               ret = ret + cell;
           }
           NeptusLog.pub().info("<###> "+ret);
           return ret;
       }
*/

    private static final long serialVersionUID = -7203527367652271490L;

    protected static String tileClassId = TileVirtualEarth.class.getSimpleName();

    private static Map<String, TileVirtualEarth> tilesMap = Collections.synchronizedMap(new HashMap<String, TileVirtualEarth>());

    private static final int MAX_LEVEL_OF_DETAIL = 20;
    
    public TileVirtualEarth(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }

    /**
     * @param id
     * @throws Exception
     */
    public TileVirtualEarth(String id) throws Exception {
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
        int server = (int) (4 * rnd.nextFloat()); // server r0, r1, r2, and r3
        String urlGet = "http://r" + server + ".ortho.tiles.virtualearth.net/tiles/"
                + "r" + id + ".png?g=1";
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
