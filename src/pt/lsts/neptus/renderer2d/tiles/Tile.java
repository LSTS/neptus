/*
 * Copyright (c) 2004-2019 Universidade do Porto - Faculdade de Engenharia
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

import com.sun.imageio.plugins.png.PNGMetadata;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.io.FileUtils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * @author pdias
 * 
 */
public abstract class Tile implements /*Renderer2DPainter,*/ Serializable {
    // http://www.viawindowslive.com/Articles/VirtualEarth/CreatingaVEpluginforNASAsWorldWind.aspx


    private static final long serialVersionUID = 564094012577853170L;
    
    private static boolean useImageFromLowerLevelOfDetailWhileLoading = true;
    
    private static final ReentrantReadWriteLock tileCacheDiskClearOrTileSaveLock = new ReentrantReadWriteLock();
    
    protected static String TILE_BASE_CACHE_DIR;
    
    static {
        if (new File("../" + ".cache/wmcache").exists())
            TILE_BASE_CACHE_DIR = "../" + ".cache/wmcache";
        else
            TILE_BASE_CACHE_DIR = ".cache/wmcache";
    }
    
    protected static final String TILE_FX_EXTENSION = "png";

    static HashMap<String, HashMap<String, Long>> cacheExpiration = new HashMap<>();
    
    public static final long MILISECONDS_TO_TILE_MEM_REMOVAL = 20000;
    private static final int MILLIS_TO_NOT_TRY_LOAD_LOW_LEVEL_IMAGE = 30000;
    
    private static final Color COLOR_WHITE_TRANS_100 = ColorUtils.setTransparencyToColor(Color.WHITE, 100);
    private static final Color COLOR_GREEN = Color.GREEN;
    
    public enum TileState { LOADING, RETRYING, LOADED, ERROR, FATAL_ERROR, DISPOSING };
    private TileState state = TileState.LOADING;
    protected String lasErrorMessage = "";
    
    protected static String tileClassId = "unknown";
    
    public final String id;
    public final int levelOfDetail;
    public final int tileX, tileY;
    public final int worldX, worldY;
    public long expiration;
    protected BufferedImage image = null;
    protected boolean temporaryTransparencyDetectedOnImageOnDisk = false; //only for base layers
    private boolean showTileId = false;

    private Image imageFromLowerLevelOfDetail = null;
    private int levelOfDetailFromImageFromLowerLevelOfDetail = 0;
    
    private long lastPaintTimeMillis = -1;
    
    private Timer timer = null; // new Timer(this.getClass().getSimpleName() + " [" + Integer.toHexString(this.hashCode()) + "]");
    private TimerTask timerTask = null;

    private static Timer saveTimer = new Timer("TileExpirationMapSaveTimer");
    private static AtomicBoolean hasSaveTimer = new AtomicBoolean(false);
    private static final long SAVE_INTERVAL = 120000; //2 minutes

    /**
     * @param levelOfDetail
     * @param tileX
     * @param tileY
     * @param image
     * @throws Exception
     */
    public Tile(Integer levelOfDetail, Integer tileX, Integer tileY, BufferedImage image) throws Exception {
        this.id = MapTileUtil.tileXYToQuadKey(tileX, tileY, levelOfDetail);
        this.levelOfDetail = levelOfDetail;
        this.tileX = tileX;
        this.tileY = tileY;
        int[] pxy = MapTileUtil.tileXYToPixelXY(tileX, tileY);
        worldX = pxy[0];
        worldY = pxy[1];

        testForAlfaOnLoaddImage(image);
        this.image = image;

        state = TileState.LOADED;
        
        lastPaintTimeMillis = System.currentTimeMillis();
    }
    
    /**
     * This method will try to load from file ({@link #loadTile()}). If not,
     * will call {@link #createTileImage()}. This calls will run on a {@link Thread} 
     * and because of that this constructor will return straightway. 
     * @param id
     *            This should be the
     *            {@link MapTileUtil#tileXYToQuadKey(int, int, int)} and
     *            {@link MapTileUtil#quadKeyToTileXY(String)}
     * @throws Exception
     */
    public Tile(String id) throws Exception {
        this.id = id;
        levelOfDetail = id.length();
        int[] tlxy = MapTileUtil.quadKeyToTileXY(id);
        tileX = tlxy[0];
        tileY = tlxy[1];
        int[] pxy = MapTileUtil.tileXYToPixelXY(tileX, tileY);
        worldX = pxy[0];
        worldY = pxy[1];
//        NeptusLog.pub().info("<###> "+id + "  " + levelOfDetail + "  " + tileX + "  " + tileY + "  " + worldX + "  " + worldY);
        loadOrCreateTileImage();
    }

    /**
     * @return the lastPaintTimeMillis
     */
    public long getLastPaintTimeMillis() {
        return lastPaintTimeMillis;
    }
    
    /**
     * @return the state
     */
    public TileState getState() {
        return state;
    }

    /**
     * The state will not change if the current state = {@link TileState#DISPOSING} || 
     * {@link TileState#FATAL_ERROR} except to put the state in {@link TileState#DISPOSING}..
     * @param state the state to set
     */
    protected void setState(TileState state) {
        if (state != TileState.DISPOSING
                && (this.state == TileState.DISPOSING || this.state == TileState.FATAL_ERROR))
            return;
        this.state = state;
    }
    
    /**
     * @return the lasErrorMessage
     */
    public String getLasErrorMessage() {
        return lasErrorMessage;
    }
    
    /**
     * Will retry to reload the tile if the {@link #state}==ERROR
     * Please call this even if you need to override it.
     */
    public void retryLoadingTile() {
        if (state == TileState.ERROR) {
            setState(TileState.LOADING);
            loadOrCreateTileImage();
        }
    }
    
    /**
     * @return the showTileId
     */
    public boolean isShowTileId() {
        return showTileId;
    }
    
    /**
     * @param showTileId the showTileId to set
     */
    public void setShowTileId(boolean showTileId) {
        this.showTileId = showTileId;
    }
    
    /**
     * Put a static method like this in an overwrite class to indicate
     * the maximum level of detail to display the tiles.
     * @return
     */
    public static int getMaxLevelOfDetail() {
        return MapTileUtil.LEVEL_MAX;
    }
    
    /**
     * This method will try to load from file ({@link #loadTile()}). If not,
     * will call {@link #createTileImage()}.
     * This creates a {@link Thread} to execute and return straightway.
     */
    protected final void loadOrCreateTileImage() {
        lastPaintTimeMillis = System.currentTimeMillis();

        new Thread("World Tile loader") {
            @Override
            public void run() {
                if (!loadTile()) {
                    createTileImage();
                }
            }
        }.start();
    }
    
    /**
     *  [0.0, 1.0]
     * @return
     */
    protected float getTransparencyToApplyToImage() {
        return 0.4f;
    }

    /**
     * Implement this to generate the tile image. Please test the {@link #state},
     * if equal to {@link TileState#DISPOSING} or
     * {@link TileState#FATAL_ERROR} return without processing.
     */
    protected abstract void createTileImage();
    
    public static boolean isFetchableOrGenerated() {
        return false;
    }
    
    /**
     * Override this to return the tile map style id
     * @return
     */
    public static String getTileStyleID() {
        return null;
    }

    /**
     * Override to be warned if any of the {@link NeptusProperty} getChanged by {@link PropertiesEditor}
     */
    public static void staticPropertiesChanged() {
    }

    /**
     * This method should be overridden to return a tile map.
     * @return In this base implementation return ALWAYS null.
     */
    public static <T extends Tile> Map<String, T> getTilesMap() {
        return null;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    public void paint(Graphics2D g, StateRenderer2D renderer, boolean useTransparency) {
        lastPaintTimeMillis = System.currentTimeMillis();
        
        Point2D xyWC = renderer.getCenter().getPointInPixel(renderer.getLevelOfDetail());
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(renderer.getWidth() / 2, renderer.getHeight() / 2);
        g2.translate((worldX - xyWC.getX()), (worldY - xyWC.getY()));
        if (image == null) {
            if (imageFromLowerLevelOfDetail != null) {
                Graphics2D gt = (Graphics2D) g2.create();
                double sz = 256 / imageFromLowerLevelOfDetail.getWidth(null);
                gt.scale(sz, sz);
                if (useTransparency && !temporaryTransparencyDetectedOnImageOnDisk 
                        && getTransparencyToApplyToImage() >= 0 && getTransparencyToApplyToImage() < 1)
                    gt.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getTransparencyToApplyToImage()));
                gt.drawImage(imageFromLowerLevelOfDetail, 0, 0, imageFromLowerLevelOfDetail.getWidth(null),
                        imageFromLowerLevelOfDetail.getHeight(null), 0, 0, imageFromLowerLevelOfDetail.getWidth(null),
                        imageFromLowerLevelOfDetail.getHeight(null), null);
                gt.dispose();
            }
            else {
                // added 6/10/2012 (pdias)
                scheduleLoadImageFromLowerLevelOfDetail();
            }
            g2.setColor(COLOR_WHITE_TRANS_100);
//            g2.drawLine(0, 0, 255, 255);
//            g2.drawLine(255, 0, 0, 255);
            if (state == TileState.LOADING || state == TileState.RETRYING) {
                g2.setFont(new Font("Arial", 0, 10));
                g2.drawString(I18n.text(state.toString()), 128, 128);
            }
            g2.dispose();
        }
        else {
            Graphics2D gt = (Graphics2D) g2.create();
            if (useTransparency && !temporaryTransparencyDetectedOnImageOnDisk
                    && getTransparencyToApplyToImage() >= 0 && getTransparencyToApplyToImage() < 1)
                gt.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, getTransparencyToApplyToImage()));
            gt.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), 0, 0, image.getWidth(),
                    image.getHeight(), null);
            gt.dispose();

            if (showTileId) {
                g2.setColor(COLOR_GREEN);
                g2.setFont(new Font("Arial", 0, 10));
                g2.drawString(getId(), 128, 128);
            }
        }
        g2.dispose();
    }
    
    /**
     * This will invalidate this tile for future use;
     * If overridden call the super.dispose. 
     */
    public void dispose() {
        state = TileState.DISPOSING;
        
        if (timerTask != null)
            timerTask.cancel();
        
        if (timer != null)
            timer.cancel();
        
        timerTask = null;
        timer = null;
    }

    protected final String getTileFilePath() {
        return getTileFilePathFor(levelOfDetail, tileX, tileY);
    }

    protected final String getTileFilePathFor(int levelOfDetailToUse, int tileXToUse, int tileYToUse) {
        return TILE_BASE_CACHE_DIR + "/" + this.getClass().getSimpleName() + "/z"
                + levelOfDetailToUse + "/x" + tileXToUse + "/y" + tileYToUse + "." + TILE_FX_EXTENSION;
    }

    /**
     * Saves the tile to {@link #TILE_BASE_CACHE_DIR}/[Class SimpleName]/z
     * {@link #levelOfDetail}/x{@link #tileX}/y{@link #tileY}.
     * {@link #TILE_FX_EXTENSION}
     * 
     * @return
     */
    public boolean saveTile() {
        if (state != TileState.LOADED)
            return false;
        
        if (image == null) {
            NeptusLog.pub().warn(String.format("Tile image %s is null!", getTileFilePath()));
            return false;
        }

        tileCacheDiskClearOrTileSaveLock.readLock().lock();
        try {
            File outFile = new File(getTileFilePath());
            outFile.getParentFile().mkdirs();
            outFile.createNewFile();
            System.out.println("Saving expiration date for tile: " + getId() + " from map: " + getClass().getSimpleName());
            System.out.println("expiration = " + expiration);


            // https://docs.oracle.com/javase/8/docs/api/javax/imageio/metadata/doc-files/png_metadata.html
            ImageWriter writer = ImageIO.getImageWritersByFormatName("png").next();

            ImageWriteParam writeParam = writer.getDefaultWriteParam();
            ImageTypeSpecifier typeSpecifier = ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB);

            //adding metadata
            IIOMetadata metadata = writer.getDefaultImageMetadata(typeSpecifier, writeParam);

            IIOMetadataNode textEntry = new IIOMetadataNode("tEXtEntry");
            textEntry.setAttribute("keyword", "expiration");
            textEntry.setAttribute("value", Long.toString(expiration));

            IIOMetadataNode text = new IIOMetadataNode("tEXt");
            text.appendChild(textEntry);

            IIOMetadataNode root = new IIOMetadataNode("javax_imageio_png_1.0");
            root.appendChild(text);

            metadata.mergeTree("javax_imageio_png_1.0", root);

            //writing the data
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream stream = ImageIO.createImageOutputStream(baos);
            writer.setOutput(stream);
            writer.write(metadata, new IIOImage(image, null, metadata), writeParam);
            stream.close();

            FileUtils.writeByteArrayToFile(outFile, baos.toByteArray());

            HashMap<String, Long> currMapStyleCache = cacheExpiration.get(getClass().getAnnotation(MapTileProvider.class).name());
            if(currMapStyleCache != null){
                currMapStyleCache.put(id, expiration);
            } else {
                HashMap<String, Long> newMap = new HashMap<>();
                newMap.put(id, expiration);
                cacheExpiration.put(getClass().getAnnotation(MapTileProvider.class).name(), newMap);
            }
            if(!hasSaveTimer.get()) {
                saveTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        saveCacheExpiration();
                        hasSaveTimer.set(false);
                    }
                }, SAVE_INTERVAL);
                hasSaveTimer.set(true);
            }

            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        finally {
            tileCacheDiskClearOrTileSaveLock.readLock().unlock();
        }
    }

    /**
     * Loads the tile from {@link #TILE_BASE_CACHE_DIR}/[Class SimpleName]/z
     * {@link #levelOfDetail}/x{@link #tileX}/y{@link #tileY}.
     * {@link #TILE_FX_EXTENSION}
     * Sets the {@link #state}
     */
    public boolean loadTile() {
        tileCacheDiskClearOrTileSaveLock.readLock().lock();
        try {
            if (image == null)
                state = TileState.LOADING;
            File inFile = new File(getTileFilePath());

            if(hasExpired()){
                if (!inFile.exists()) {
                    lasErrorMessage = "Error loading tile from file not existing!";
                    if (image == null)
                        state = TileState.ERROR;
                    // scheduleLoadImageFromLowerLevelOfDetail();
                    return false;
                } else {
                    System.out.println("Checking file expiration");
                    if(hasExpired(inFile)){
                        state = TileState.ERROR;
                        return false;
                    }
                }
            }
            
            BufferedImage img;
            try {
                img = ImageIO.read(inFile);
            }
            catch (IndexOutOfBoundsException e) {
                inFile.delete();
                throw new Exception("Image not complete to load! Was deleted.");
            }
            testForAlfaOnLoaddImage(img);
            image = img;
            imageFromLowerLevelOfDetail = null;
            state = TileState.LOADED;
            return true;
        }
        catch (Exception e) {
            lasErrorMessage = "Error loading tile from file: " + e;
            if (image == null)
                state = TileState.ERROR;
            loadImageFromLowerLevelOfDetail();
            return false;
        }
        finally {
            tileCacheDiskClearOrTileSaveLock.readLock().unlock();
        }
    }

    private boolean hasExpired() {
        HashMap<String, Long> currMapStyleCache = cacheExpiration.get(getClass().getAnnotation(MapTileProvider.class).name());
        Long expiration = currMapStyleCache.get(id);
        if(expiration != null) {
            return expiration <= System.currentTimeMillis();
        } else {
            return true;
        }
    }

    private boolean hasExpired(File inFile) throws IOException {
        // https://docs.oracle.com/javase/8/docs/api/javax/imageio/metadata/doc-files/png_metadata.html
        ImageReader imageReader = ImageIO.getImageReadersByFormatName("png").next();

        imageReader.setInput(ImageIO.createImageInputStream(inFile), true);

        // read metadata of first image
        IIOMetadata metadata = imageReader.getImageMetadata(0);

        // the PNG image reader already create a PNGMetadata Object
        PNGMetadata pngmeta = (PNGMetadata) metadata;
        NodeList childNodes = pngmeta.getStandardTextNode().getChildNodes();

        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            String keyword = node.getAttributes().getNamedItem("keyword").getNodeValue();
            String value = node.getAttributes().getNamedItem("value").getNodeValue();
            if("expiration".equals(keyword)){
                try {
                    expiration = Long.valueOf(value);

                    // add new entry to cache map
                    HashMap<String, Long> currMapStyleCache = cacheExpiration.get(getClass().getAnnotation(MapTileProvider.class).name());
                    if(currMapStyleCache != null){
                        currMapStyleCache.put(id, expiration);
                    } else {
                        HashMap<String, Long> newMap = new HashMap<>();
                        newMap.put(id, expiration);
                        cacheExpiration.put(getClass().getAnnotation(MapTileProvider.class).name(), newMap);
                    }

                    return expiration <= System.currentTimeMillis();
                } catch (NumberFormatException e) {
                    NeptusLog.pub().info(String.format("Could not load expiration metadata for map tile %s of style '%s'",
                            id,
                            getClass().getSimpleName()));
                    return true;
                }
            }
        }
        return true;
    }

    public static void setCache(String mapKey, boolean state) {
        if(state) {
            cacheExpiration.put(mapKey,loadCacheExpiration(mapKey));
        } else {
            cacheExpiration.remove(mapKey);
        }
    }

    private static HashMap<String, Long> loadCacheExpiration(String mapKey) {
        System.out.println("Loading cache file for: " + mapKey);
        File serFile = new File(TILE_BASE_CACHE_DIR + "/serializedCaches/" + mapKey);
        if(!serFile.exists()) {
            System.out.println(String.format("No cache expiration found at '%s'", serFile.getPath()));
            return new HashMap<>();
        }

        try (FileInputStream fis = new FileInputStream(serFile);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object savedObject = ois.readObject();
            if (savedObject instanceof HashMap){
                return ((HashMap) savedObject);
            } else {
                throw new Exception("Saved Object is not instance of HashMap");
            }
        } catch(Exception e) {
            System.out.println("An error occurred while saving cache expiration data");
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    private static void saveCacheExpiration() {
        System.out.println("Saving cache");
        for (Map.Entry<String, HashMap<String, Long>> mapEntry : cacheExpiration.entrySet()) {
            System.out.println("Key = " + mapEntry.getKey());
            try {
                File serFile = new File(TILE_BASE_CACHE_DIR + "/serializedCaches/" + mapEntry.getKey());
                serFile.getParentFile().mkdirs();
                serFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(serFile);
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(mapEntry.getValue());
                oos.close();
                fos.close();
            } catch(IOException ioe) {
                System.out.println("An error occurred while saving cache expiration data");
                ioe.printStackTrace();
            }
        }
    }

    public static void cleanup() {
        saveTimer.cancel();
        saveCacheExpiration();
    }

    /**
     * @param img
     */
    protected void testForAlfaOnLoaddImage(BufferedImage img) {
        boolean isBaseOrLayer = isBaseOrLayerMap();
        temporaryTransparencyDetectedOnImageOnDisk = isBaseOrLayer ? GuiUtils.hasAlpha(img) : false;
    }

    /**
     * @return
     */
    protected boolean isBaseOrLayerMap() {
        try {
            MapTileProvider anotat = this.getClass().getAnnotation(MapTileProvider.class);
            return anotat == null ? true : anotat.isBaseMapOrLayer();
        }
        catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
    
    /**
     * 
     */
    private void scheduleLoadImageFromLowerLevelOfDetail() {
        if (state == TileState.DISPOSING || !useImageFromLowerLevelOfDetailWhileLoading)
            return;

        if (imageFromLowerLevelOfDetail != null && levelOfDetail - levelOfDetailFromImageFromLowerLevelOfDetail == 1) {
            return;
        }

        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
//                    NeptusLog.pub().info("<###>Run " + tileX + ":" + tileY + "   " + levelOfDetail + (imageFromLowerLevelOfDetail != null ? "|" +levelOfDetailFromImageFromLowerLevelOfDetail : "")) ;
                    loadImageFromLowerLevelOfDetail();
                    if (state == TileState.DISPOSING || image != null || 
                            (System.currentTimeMillis() - lastPaintTimeMillis > MILLIS_TO_NOT_TRY_LOAD_LOW_LEVEL_IMAGE)) {
                        this.cancel();
                        timerTask = null;
                        if (timer != null) {
                            timer.cancel();
                            timer = null;
                        }
                    }
                    if (imageFromLowerLevelOfDetail != null && levelOfDetail - levelOfDetailFromImageFromLowerLevelOfDetail == 1) {
                        this.cancel();
                        timerTask = null;
                        if (timer != null) {
                            timer.cancel();
                            timer = null;
                        }
                    }
                }
            };
            if (timer == null)
                timer = new Timer(this.getClass().getSimpleName() + " [" + Integer.toHexString(this.hashCode())
                        + "] ::LOD" + levelOfDetail + ":: " + "createLowerLevelTileImage", true);
            timer.scheduleAtFixedRate(timerTask, 1000, 30000);
        }
    }

    private boolean isFetchingAlternativeImage = false;
    private void loadImageFromLowerLevelOfDetail() {
        if (isFetchingAlternativeImage)
            return;
        
        isFetchingAlternativeImage = true;
        
        if (imageFromLowerLevelOfDetail != null && levelOfDetail - levelOfDetailFromImageFromLowerLevelOfDetail == 1)
            return;
        
        String quadKey = MapTileUtil.tileXYToQuadKey(tileX, tileY, levelOfDetail);
//        NeptusLog.pub().info("<###> "+quadKey);
        
        int currentLevelOfDetailFromImageFromLowerLevelOfDetail = imageFromLowerLevelOfDetail == null ? 0
                : levelOfDetailFromImageFromLowerLevelOfDetail;
        
        for (int nCuts = 1; nCuts < 6; nCuts++) {
            String tmpQK = quadKey.substring(0, quadKey.length() - nCuts);
            String tmpMatrix = quadKey.substring(quadKey.length() - nCuts);
//            NeptusLog.pub().info("<###>tmpQK: " + tmpQK);
            try {
                int[] tmpTs = MapTileUtil.quadKeyToTileXY(tmpQK);
                int tmpTileX = tmpTs[0], tmpTileY = tmpTs[1]; 
                
                if (currentLevelOfDetailFromImageFromLowerLevelOfDetail == tmpQK.length())
                    return;
                
                File inFile = new File(getTileFilePathFor(tmpQK.length(), tmpTileX, tmpTileY));
                
                if (!inFile.exists()) {
                    continue;
                }
                
                BufferedImage img;
                try {
                    img = ImageIO.read(inFile);
                }
                catch (IndexOutOfBoundsException e) {
//                    inFile.delete();
//                    throw new Exception("Image not finished loaded yet.");
                    continue;
                }
                
                int px = 0, py = 0;
                for (int i = 0; i < tmpMatrix.length(); i++) {
                    int slice = Integer.parseInt("" + tmpMatrix.charAt(i));
                    switch (slice) {
                        case 0:
                            px += 0;
                            py += 0;
                            break;
                        case 1:
                            px += 256 / Math.pow(2, i) / 2 ;
                            py += 0;
                            break;
                        case 2:
                            px += 0;
                            py += 256 / Math.pow(2, i) / 2 ;
                            break;
                        case 3:
                            px += 256 / Math.pow(2, i) / 2 ;
                            py += 256 / Math.pow(2, i) / 2 ;
                            break;
                    }
                }
                double tileDivision = Math.pow(2, tmpMatrix.length());
                int sts = (int) (256 / tileDivision);
                img = img.getSubimage(px, py, sts, sts);
                
                imageFromLowerLevelOfDetail = ImageUtils.getScaledImage(img, 256, 256, true);
//                imageFromLowerLevelOfDetail = applyTransparency(imageFromLowerLevelOfDetail, getTransparencyToApplyToImage());
                levelOfDetailFromImageFromLowerLevelOfDetail = tmpQK.length();
                break;
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        isFetchingAlternativeImage = false;
    }
    
    /**
     * This should be override, this implementation does not do anything.
     */
    public static void clearDiskCache() {
        
    }
    
    /**
     * This will delete the disk cache. This will run on a Thread.
     * A public static method clearDiskCache() should be created for every class extending Tile. 
     * @param tileClassId This should be the class {@link Class#getSimpleName()}.
     */
    protected static void clearDiskCache(String tileClassId) {
        final String path = TILE_BASE_CACHE_DIR + "/" + tileClassId;
        Thread t = new Thread() {
            public void run() {
                tileCacheDiskClearOrTileSaveLock.writeLock().lock();
                try {
                    File base = new File(path);
                    File[] zList = base.listFiles(new FileFilter() {
                        @Override
                        public boolean accept(File pathname) {
                            return pathname.isDirectory() && (pathname.getName().length() > 0 && pathname.getName().charAt(0) == 'z');
                        }
                    });
                    if (zList == null)
                        return;
                    for (File fileZ : zList) {
                        String zStr = fileZ.getName().replace("z", "");
                        try {
                            Integer.parseInt(zStr);
                            File[] xList = fileZ.listFiles(new FileFilter() {
                                @Override
                                public boolean accept(File pathname) {
                                    return pathname.isDirectory() && (pathname.getName().length() > 0 && pathname.getName().charAt(0) == 'x');
                                }
                            });
                            if (xList == null)
                                continue;
                            for (File fileX : xList) {
                                String xStr = fileX.getName().replace("x", "");
                                try {
                                    Integer.parseInt(xStr);
                                    File[] yList = fileX.listFiles(new FileFilter() {
                                        @Override
                                        public boolean accept(File pathname) {
                                            return pathname.isFile()
                                                    && TILE_FX_EXTENSION.equalsIgnoreCase(FileUtil
                                                            .getFileExtension(pathname))
                                                             && (pathname.getName().length() > 0 && pathname.getName().charAt(0) == 'y');
                                        }
                                    });
                                    if (yList == null)
                                        continue;
                                    for (File fileY : yList) {
                                        String yStr = fileY.getName().replace("y", "").replace("." + TILE_FX_EXTENSION, "");
                                        try {
                                            Integer.parseInt(yStr);
                                            fileY.delete();
                                        }
                                        catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    fileX.delete();
                                }
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            fileZ.delete();
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    base.delete();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                finally {
                    tileCacheDiskClearOrTileSaveLock.writeLock().unlock();
                }
            };
        };
        t.setDaemon(true);
        t.start();
    }

    /**
     * This will delete the disk cache.
     * A public static method loadCache() should be created for every class extending Tile. 
     * @param tileClassId This should be the class {@link Class#getSimpleName()}.
     * @return A vector of Tiles (one per image cache).
     */
    protected static <T extends Tile> Vector<T> loadCache(String tileClassId) {
        String path = TILE_BASE_CACHE_DIR + "/" + tileClassId;
        Vector<T> ret = new Vector<T>();
        File base = new File(path);
        File[] zList = base.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory() && (pathname.getName().length() > 0 && pathname.getName().charAt(0) == 'z');
            }
        });
        if (zList == null)
            return ret;
        for (File fileZ : zList) {
            String zStr = fileZ.getName().replace("z", "");
            try {
                int lod = Integer.parseInt(zStr);
                File[] xList = fileZ.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.isDirectory() && (pathname.getName().length() > 0 && pathname.getName().charAt(0) == 'x');
                    }
                });
                if (xList == null)
                    continue;
                for (File fileX : xList) {
                    String xStr = fileX.getName().replace("x", "");
                    try {
                        int xTile = Integer.parseInt(xStr);
                        File[] yList = fileX.listFiles(new FileFilter() {
                            @Override
                            public boolean accept(File pathname) {
                                return pathname.isFile()
                                        && TILE_FX_EXTENSION.equalsIgnoreCase(FileUtil
                                                .getFileExtension(pathname))
                                                 && (pathname.getName().length() > 0 && pathname.getName().charAt(0) == 'y');
                            }
                        });
                        if (yList == null)
                            continue;
                        for (File fileY : yList) {
                            String yStr = fileY.getName().replace("y", "").replace("." + TILE_FX_EXTENSION, "");
                            try {
                                int yTile = Integer.parseInt(yStr);
                                String pack = Tile.class.getPackage().getName() + ".";
                                BufferedImage image;
                                try {
                                    image = ImageIO.read(fileY);
                                }
                                catch (IndexOutOfBoundsException e) {
                                    fileY.delete();
                                    throw new Exception("Image not complete to load! Was deleted.");
                                }
                                @SuppressWarnings("unchecked")
                                T tile = (T) Tile.class
                                        .getClassLoader()
                                        .loadClass(pack + tileClassId)
                                        .getConstructor(Integer.class, Integer.class,
                                                Integer.class, BufferedImage.class)
                                        .newInstance(lod, xTile, yTile, image);
                                ret.add(tile);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        
        return ret;
    }
}
