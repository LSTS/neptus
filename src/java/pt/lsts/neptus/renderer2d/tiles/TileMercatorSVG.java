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
 * 8/10/2011
 */
package pt.lsts.neptus.renderer2d.tiles;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.SVGAbstractTranscoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.Document;

import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.SvgUtil;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * @author pdias
 *
 */
@MapTileProvider(name = "Mercator SVG (Local)")
public class TileMercatorSVG extends Tile {

    private static final long serialVersionUID = -6947456498157990203L;

    protected static String tileClassId = TileMercatorSVG.class.getSimpleName();
    
    private static Map<String, TileMercatorSVG> tilesMap = Collections.synchronizedMap(new HashMap<String, TileMercatorSVG>());
    
    private static final Object lock = new Object();
    private static boolean docLoaded = false;
    private static final Object docLoadedLock = new Object();

    protected static final String fxWM = "/images/World_Blank_Map_Mercator_projection.svg";

    protected static final double w = 634.26801, h = 476.7276;
    
    protected static final LocationType centerOfImage = new LocationType();
    {
        centerOfImage.setLatitudeDegs(-1.5);
        centerOfImage.translatePosition(-227E3, -12E3, 0);
        centerOfImage.setLongitudeDegs(10);
        centerOfImage.convertToAbsoluteLatLonDepth();
    }
    
    private static final int offsetX = 0, offsetY = 0;

    private static final int MAX_LEVEL_OF_DETAIL = 15;
    
    private static PrintTranscoder prm;
    private static PageFormat page;

    {
        if (!docLoaded) {
            synchronized (docLoadedLock) {
                if (!docLoaded) {
                    prm = loadWorld(fxWM, w, h);
                    Paper paper = new Paper();
                    paper.setSize(w, h);
                    paper.setImageableArea(0, 0, w, h);
                    page = new PageFormat();
                    page.setPaper(paper);
                    docLoaded = true;
                }
            }
        }
    }

    public TileMercatorSVG(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        super(levelOfDetail, tileX, tileY, image);
    }
    
    /**
     * @param id
     * @throws Exception
     */
    public TileMercatorSVG(String id) throws Exception {
        super(id);
    }

    protected static PrintTranscoder loadWorld(String fx, double w, double h) {
        try {
            String parser = XMLResourceDescriptor.getXMLParserClassName();
            SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
            String data = FileUtil.getFileAsString(FileUtil.getResourceAsFile(fx));
            Document wDoc = f.createDocument(null, new StringReader((String) data));
            wDoc = SvgUtil.cleanInkscapeSVG(wDoc);
            PrintTranscoder prm = new PrintTranscoder();
            prm.addTranscodingHint(SVGAbstractTranscoder.KEY_WIDTH, Float.valueOf(Double.valueOf(w).floatValue()));
            prm.addTranscodingHint(SVGAbstractTranscoder.KEY_HEIGHT, Float.valueOf(Double.valueOf(h).floatValue()));
            TranscoderInput ti = new TranscoderInput(wDoc);
            prm.transcode(ti, null);
            return prm;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        catch (NoClassDefFoundError e) {
            System.out.print("Batik missing in the classpath. Proceding without worldmap.");
            return null;
        }
        catch (Error e) {
            e.printStackTrace();
            return null;
        }
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.Tile#retryLoadingTile()
     */
    @Override
    public void retryLoadingTile() {
        // Does not make sense to retry
    }
    
    public static int getMaxLevelOfDetail() {
        return MAX_LEVEL_OF_DETAIL;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.Tile#createTileImage()
     */
    @Override
    protected void createTileImage() {
        if (getState() == TileState.DISPOSING)
            return;
        if (prm == null) {
            setState(TileState.FATAL_ERROR);
            lasErrorMessage = "Not able to load SVG Map painter!";
            return;
        }
        setState(TileState.LOADING);
        new Thread(this.getClass().getSimpleName() + " [" + Integer.toHexString(this.hashCode()) + "]") {
            @Override
            public void run() {
                int msize = MapTileUtil.mapSize(levelOfDetail);
                double[] xyWC = { msize / 2, msize / 2 };
                Point2D xyWG = centerOfImage.getPointInPixel(levelOfDetail);
                
                BufferedImage cache = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = cache.createGraphics();

                g2.translate((xyWG.getX() - xyWC[0]), (xyWG.getY() - xyWC[1]));
                double zt = msize / w;
                g2.translate(0, (msize - h * zt) / 2);
                Graphics2D gg = null;
                if (tileX == 0) {
                    gg = (Graphics2D) g2.create();
                    gg.translate(-msize, -tileY * 256);
                    gg.scale(zt, zt);
                    gg.translate(offsetX, offsetY);
                }
                g2.translate(-tileX * 256, -tileY * 256);
                g2.scale(zt, zt);
                g2.translate(offsetX, offsetY);
                synchronized (lock) {
                    if (TileMercatorSVG.this.getState() != TileState.DISPOSING) {
                        if (gg != null)
                            prm.print(gg, page, 0);
                    }
                    else
                        return;
                    if (TileMercatorSVG.this.getState() != TileState.DISPOSING)
                        prm.print(g2, page, 0);
                    else
                        return;
                }
                if (gg != null)
                    gg.dispose();
                g2.dispose();
//                cache = (BufferedImage) GuiUtils.applyTransparency(cache, 0.2f);
                temporaryTransparencyDetectedOnImageOnDisk = false;
                image = cache;
                
//                loadTransformedImage();
                
                TileMercatorSVG.this.setState(TileState.LOADED);
                saveTile();
                //NeptusLog.pub().info("<###> "+image);
            }
        }.start();
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.Tile#testForAlfaOnLoaddImage(java.awt.image.BufferedImage)
     */
    @Override
    protected void testForAlfaOnLoaddImage(BufferedImage img) {
        temporaryTransparencyDetectedOnImageOnDisk = false; // this has to be overwritten because the SVG has transparent parts
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.tiles.Tile#getTransparencyToApplyToImage()
     */
    @Override
    protected float getTransparencyToApplyToImage() {
        return 0.2f;
    }
    
    public static boolean isFetchableOrGenerated() {
        return false;
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
        clearDiskCache(tileClassId);
    }

    /**
     * @return 
     * 
     */
    public static Vector<TileMercatorSVG> loadCache() {
        return loadCache(tileClassId);
    }

    public static void main(String[] args) throws Exception {
//        Tile tile = new TileMercadorSVG(MapTileUtil.tileXYToQuadKey(1, 1, 1));
//        TileMercadorSVG.loadCache();
    }
}
