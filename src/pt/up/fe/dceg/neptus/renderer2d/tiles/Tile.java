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
 * 8/10/2011
 */
package pt.up.fe.dceg.neptus.renderer2d.tiles;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.imageio.ImageIO;

import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.util.ColorUtils;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.coord.MapTileUtil;

/**
 * @author pdias
 * 
 */
public abstract class Tile implements /*Renderer2DPainter,*/ Serializable {
    // http://www.viawindowslive.com/Articles/VirtualEarth/CreatingaVEpluginforNASAsWorldWind.aspx


    private static final long serialVersionUID = 564094012577853170L;
    
    private static boolean useImageFromLowerLevelOfDetailWhileLoading = true;
    
    private static final ReentrantReadWriteLock tileCacheDiskClearOrTileSaveLock = new ReentrantReadWriteLock();
    
    protected static final String TILE_BASE_CACHE_DIR;
    static {
        if (new File("../" + ".cache/wmcache").exists())
            TILE_BASE_CACHE_DIR = "../" + ".cache/wmcache";
        else
            TILE_BASE_CACHE_DIR = ".cache/wmcache";
    }
    protected static final String TILE_FX_EXTENSION = "png";
    
    public static final long MILISECONDS_TO_TILE_MEM_REMOVAL = 180000;
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
    protected BufferedImage image = null;
    protected boolean temporaryTransparencyDetectedOnImageOnDisk = false;
    private boolean showTileId = false;

    private Image imageFromLowerLevelOfDetail = null;
    private int levelOfDetailFromImageFromLowerLevelOfDetail = 0;
    
    private long lastPaintTimeMillis = -1;
    
    private Timer timer = null; // new Timer(this.getClass().getSimpleName() + " [" + Integer.toHexString(this.hashCode()) + "]");
    private TimerTask timerTask = null;
    
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
//        System.out.println(id + "  " + levelOfDetail + "  " + tileX + "  " + tileY + "  " + worldX + "  " + worldY);
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
                g2.drawString(state.toString(), 128, 128);
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
        
        tileCacheDiskClearOrTileSaveLock.readLock().lock();
        try {
            File outFile = new File(getTileFilePath());
            outFile.mkdirs();
            return ImageIO.write(image, TILE_FX_EXTENSION.toUpperCase(), outFile);
        }
        catch (IOException e) {
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
            if (!inFile.exists()) {
                lasErrorMessage = "Error loading tile from file not existing!";
                if (image == null)
                    state = TileState.ERROR;
//                scheduleLoadImageFromLowerLevelOfDetail();
                return false;
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

    /**
     * @param img
     */
    protected void testForAlfaOnLoaddImage(BufferedImage img) {
        temporaryTransparencyDetectedOnImageOnDisk = GuiUtils.hasAlpha(img);
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
//                    System.out.println("Run " + tileX + ":" + tileY + "   " + levelOfDetail + (imageFromLowerLevelOfDetail != null ? "|" +levelOfDetailFromImageFromLowerLevelOfDetail : "")) ;
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
//        System.out.println(quadKey);
        
        int currentLevelOfDetailFromImageFromLowerLevelOfDetail = imageFromLowerLevelOfDetail == null ? 0
                : levelOfDetailFromImageFromLowerLevelOfDetail;
        
        for (int nCuts = 1; nCuts < 6; nCuts++) {
            String tmpQK = quadKey.substring(0, quadKey.length() - nCuts);
            String tmpMatrix = quadKey.substring(quadKey.length() - nCuts);
//            System.out.println("tmpQK: " + tmpQK);
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
