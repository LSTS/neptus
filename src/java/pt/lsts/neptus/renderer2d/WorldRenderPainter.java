/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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
 * 2/10/2011
 */
package pt.lsts.neptus.renderer2d;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXStatusBar;

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.proxy.ProxyInfoProvider;
import pt.lsts.neptus.doc.NeptusDoc;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.PropertiesProvider;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.MapTileProvider;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginProperty;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.renderer2d.tiles.MapPainterProvider;
import pt.lsts.neptus.renderer2d.tiles.Tile;
import pt.lsts.neptus.renderer2d.tiles.Tile.TileState;
import pt.lsts.neptus.renderer2d.tiles.TileMercatorSVG;
import pt.lsts.neptus.renderer2d.tiles.TileOpenStreetMap;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.ReflectionUtil;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.coord.MapTileUtil;

/**
 * @author pdias
 *
 */
@NeptusDoc(ArticleFilename = "world-overlay/world-overlay.html", Section = "Renderer2D")
@LayerPriority(priority = -500)
public class WorldRenderPainter implements Renderer2DPainter, MouseListener, MouseMotionListener {

    private final int ICON_SIZE = 20;
    private final Image ICON_WORLD_ON = ImageUtils.getScaledIcon("images/world/worldmap-show.png", ICON_SIZE, ICON_SIZE).getImage();
    public final Image ICON_WORLD_SETTINGS = ImageUtils.getScaledIcon("images/world/worldmap-settings.png", ICON_SIZE, ICON_SIZE).getImage();
    private final Image ICON_WORLD_DIALOG = ImageUtils.getScaledIcon("images/world/worldmap-conf-dialog.png", ICON_SIZE, ICON_SIZE).getImage();

    private enum PropertiesOrCustomOptionsDialogEnum { None, PropertiesDialog, CustomOptionsDialog };

    static {
        long start = System.currentTimeMillis();
        Class<?>[] vis = ReflectionUtil.listTileProviders();
        for (Class<?> sp : vis) {
            if (ReflectionUtil.hasAnnotation(sp, MapTileProvider.class))
                PluginsRepository.addPlugin(sp.getCanonicalName());
        }
        NeptusLog.pub().debug("Loading MapProviders in "
                + DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - start));
    }

    @NeptusProperty
    public static String defaultActiveLayers = TileMercatorSVG.class.getAnnotation(MapTileProvider.class).name();

    private static final String ROOT_PREFIX;
    static {
        if (new File("../" + "conf").exists())
            ROOT_PREFIX = "../";
        else {
            ROOT_PREFIX = "";
            new File("conf").mkdir();
        }
    }

    static {
        try {
            String confFx = ROOT_PREFIX + "conf/" + WorldRenderPainter.class.getSimpleName().toLowerCase() + ".properties";
            if (new File(confFx).exists())
                PluginUtils.loadProperties(confFx, WorldRenderPainter.class);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Not possible to open \"conf/"
                    + WorldRenderPainter.class.getSimpleName().toLowerCase() + ".properties\"");
        }
    }

    private static final int MAX_LEVEL_OF_DETAIL = 15;

    private static final Color COLOR_CYAN_TRANSP_200 = ColorUtils.setTransparencyToColor(Color.CYAN, 200);
    private static final Color COLOR_WHITE_TRANS_150 = ColorUtils.setTransparencyToColor(Color.WHITE, 150);
    private static final Color COLOR_BLACK_TRANS_150 = ColorUtils.setTransparencyToColor(Color.BLACK, 150);
    private static final Color COLOR_BLACK_TRANS_40 = ColorUtils.setTransparencyToColor(Color.BLACK, 40);

    private Vector<HoveringButton> controlRenderButtons = new Vector<HoveringButton>();
    private HoveringButton mapControlButton = null;
    private HoveringButton mapSettingsButton = null;
    private HoveringButton mapShowActiveLayerDialogButton = null;

    private JDialog dialogProperties = null;
    private LinkedHashMap<String, JDialog> openPaintersDialog = new LinkedHashMap<String, JDialog>();

    private boolean showOnScreenControls = true;
    private boolean drawWorldBoundaries = false;
    private boolean drawWorldMap = false;
    private boolean useTransparency = true;

    private StateRenderer2D renderer2D = null;
    private Renderer2DPainter postRenderPainter = null;

    private static Map<String, Boolean> mapActiveHolderList = Collections.synchronizedMap(new LinkedHashMap<String, Boolean>());
    private static Map<String, Boolean> mapBaseOrLayerHolderList = Collections.synchronizedMap(new LinkedHashMap<String, Boolean>());
    private static Map<String, Short> mapLayerPrioriryHolderList = Collections.synchronizedMap(new LinkedHashMap<String, Short>());
    private static Map<String, MapPainterProvider> mapPainterHolderList = Collections.synchronizedMap(new LinkedHashMap<String, MapPainterProvider>());
    private static Map<String, Map<String, Tile>> tileHolderList = Collections.synchronizedMap(new LinkedHashMap<String, Map<String, Tile>>());
    private static Map<String, Class<? extends Tile>> tileClassList = Collections.synchronizedMap(new LinkedHashMap<String, Class<? extends Tile>>());
    
    private static List<String> mapsOrderedForPainting = Collections.synchronizedList(new ArrayList<String>());
    
    static {
        long start = System.currentTimeMillis();

        String mapId = TileMercatorSVG.class.getAnnotation(MapTileProvider.class).name();
        mapActiveHolderList.put(mapId, true); //TileMercadorSVG.getTileStyleID()
        mapBaseOrLayerHolderList.put(mapId, TileMercatorSVG.class.getAnnotation(MapTileProvider.class).isBaseMapOrLayer());
        mapLayerPrioriryHolderList.put(mapId, TileMercatorSVG.class.getAnnotation(MapTileProvider.class).layerPriority());
        tileHolderList.put(mapId, TileMercatorSVG.getTilesMap());
        tileClassList.put(mapId, TileMercatorSVG.class);

        mapId = TileOpenStreetMap.class.getAnnotation(MapTileProvider.class).name();
        mapActiveHolderList.put(mapId, false); //TileOpenStreetMap.getTileStyleID()
        mapBaseOrLayerHolderList.put(mapId, TileMercatorSVG.class.getAnnotation(MapTileProvider.class).isBaseMapOrLayer());
        mapLayerPrioriryHolderList.put(mapId, TileMercatorSVG.class.getAnnotation(MapTileProvider.class).layerPriority());
        tileHolderList.put(mapId, TileOpenStreetMap.getTilesMap());
        tileClassList.put(mapId, TileOpenStreetMap.class);

        Vector<Class<? extends MapTileProvider>> lst = new Vector<Class<? extends MapTileProvider>>();
        for (Class<? extends MapTileProvider> clazz : PluginsRepository.getTileProviders().values()) {
            try {
                if (lst.isEmpty()) {
                    lst.add(clazz);
                    continue;
                }
                String id = clazz.getAnnotation(MapTileProvider.class).name();
                for (int i = 0; i < lst.size(); i++) {
                    Class<? extends MapTileProvider> clazzC = lst.get(i);
                    String idComp = clazzC.getAnnotation(MapTileProvider.class).name();
                    //                    NeptusLog.pub().info("<###> "+idComp);
                    if (id.compareTo(idComp) < 0) {
                        int indx = lst.indexOf(clazzC);
                        lst.add(indx, clazz);
                        break;
                    }
                    else if (i == lst.size() - 1) {
                        lst.add(clazz);
                        break;
                    }
                } 
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Class<? extends MapTileProvider> clazz : lst) {
            try {
                String id = clazz.getAnnotation(MapTileProvider.class).name();
                if (mapActiveHolderList.containsKey(id))
                    continue;
                try {
                    clazz.asSubclass(Tile.class);
                    @SuppressWarnings("unchecked")
                    Class<? extends Tile> cz = (Class<? extends Tile>) clazz;
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Tile> map = (Map<String, Tile>) clazz.getMethod("getTilesMap").invoke(null);
                        mapActiveHolderList.put(id, false);
                        mapBaseOrLayerHolderList.put(id, clazz.getAnnotation(MapTileProvider.class).isBaseMapOrLayer());
                        mapLayerPrioriryHolderList.put(id, clazz.getAnnotation(MapTileProvider.class).layerPriority());
                        tileHolderList.put(id, map);
                        tileClassList.put(id, cz);
                    }
                    catch (ClassCastException e) {
                        e.printStackTrace();
                    }
                }
                catch (ClassCastException e) {
                    try {
                        @SuppressWarnings({ "unchecked", "unused" })
                        Class<? extends MapPainterProvider> cz = (Class<? extends MapPainterProvider>) clazz;
                        MapPainterProvider instance = (MapPainterProvider) clazz.getConstructor().newInstance();
                        mapActiveHolderList.put(id, false);
                        mapBaseOrLayerHolderList.put(id, clazz.getAnnotation(MapTileProvider.class).isBaseMapOrLayer());
                        mapLayerPrioriryHolderList.put(id, clazz.getAnnotation(MapTileProvider.class).layerPriority());
                        mapPainterHolderList.put(id, instance);
                    }
                    catch (ClassCastException e1) {
                        e1.printStackTrace();
                    }
                    catch (NoSuchMethodException  e2) {
                        e2.printStackTrace();
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (defaultActiveLayers.length() != 0) {
            List<String> list = Arrays.asList(defaultActiveLayers.split(";"));
            for (String mapKey : mapActiveHolderList.keySet()) {
                mapActiveHolderList.put(mapKey, false);
                // mapLayerPrioriryHolderList.put(mapKey, (short) 0);
            }
            for (String mapDefTag : list) {
                String[] tags = mapDefTag.split(":");
                String mapDef = tags[0];
                if (mapActiveHolderList.containsKey(mapDef))
                    mapActiveHolderList.put(mapDef, true);
                if (mapLayerPrioriryHolderList.containsKey(mapDef) && tags.length > 1) {
                    try {
                        short prio = Short.parseShort(tags[1]);
                        mapLayerPrioriryHolderList.put(mapDef, prio);
                    }
                    catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
            boolean isAtLeastOneSet = false;
            for (Boolean mapBool : mapActiveHolderList.values()) {
                if (mapBool) {
                    isAtLeastOneSet = true;
                    break;
                }
            }
            if (!isAtLeastOneSet)
                mapActiveHolderList.entrySet().iterator().next().setValue(true);
        }

        refreshMapsListOrderedForPainting();
        
        NeptusLog.pub().info("Initialized Map Providers in "
                + DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - start));
    }

    /**
     * Is going to be filled with the call to {@link #hasPropertiesOrCustomDialog(String)}. Don't use it directly.
     */
    private static Map<String, PropertiesOrCustomOptionsDialogEnum> mapHasPropertiesOrCustomOptions = Collections.synchronizedMap(new LinkedHashMap<String, PropertiesOrCustomOptionsDialogEnum>());

    // To keep info to the exterior updated periodically
    private static long numberOfLoadingMapTiles = 0;
    private static long numberOfLoadedMapTiles = 0;

    private static Timer timer = new Timer(WorldRenderPainter.class.getSimpleName() + " Timer", true);
    private static TimerTask ttask = new TimerTask() {
        @Override
        public void run() {
            Collection<Map<String, Tile>> list = tileHolderList.values();

            //            long ts = System.nanoTime();
            //            String txt = "Tiles cleanup:";
            //            for (Map<String, ?> map : list) {
            //                txt += "\n\t " + map.size() + "\t    " + map.getClass().getSimpleName();
            //            }
            //            NeptusLog.pub().info("<###> "+txt);

            for (Map<String, ?> map : list) {
                String[] tlist = map.keySet().toArray(new String[0]);
                for (String key : tlist) {
                    Tile tile = (Tile) map.get(key);
                    if (System.currentTimeMillis() - tile.getLastPaintTimeMillis() > Tile.MILISECONDS_TO_TILE_MEM_REMOVAL) {
                        map.remove(key);
                        tile.dispose();
                    }
                }
            }

            //            txt = "finished tiles cleanup (" + (System.nanoTime() - ts) + "ns):";
            //            for (Map<String, ?> map : list) {
            //                txt += "\n\t " + map.size() + "\t    " + map.getClass().getSimpleName();
            //            }
            //            NeptusLog.pub().info("<###> "+txt);
        }
    };

    private static TimerTask ttask1 = new TimerTask() {
        @Override
        public void run() {
            Collection<Map<String, Tile>> list = tileHolderList.values();

            long tmpNumberOfLoadingMapTiles = 0, tmpNumberOfLoadedMapTiles = 0;

            for (Map<String, ?> map : list) {
                String[] tlist = map.keySet().toArray(new String[0]);
                for (String key : tlist) {
                    Tile tile = (Tile) map.get(key);
                    tmpNumberOfLoadedMapTiles++;
                    switch (tile.getState()) {
                        case LOADING:
                        case RETRYING:
                            tmpNumberOfLoadingMapTiles++;
                            break;
                        default:
                            break;
                    }
                }
            }

            numberOfLoadingMapTiles = tmpNumberOfLoadingMapTiles;
            numberOfLoadedMapTiles = tmpNumberOfLoadedMapTiles;
        }
    };

    static {
        timer.scheduleAtFixedRate(ttask, 30000, Tile.MILISECONDS_TO_TILE_MEM_REMOVAL / 2);
        timer.scheduleAtFixedRate(ttask1, 5000, 1000);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                clearMemCache();

                ttask.cancel();
                ttask1.cancel();
                timer.cancel();
            }
        });
    }

    /**
     * 
     */
    public WorldRenderPainter(StateRenderer2D renderer2D, boolean drawWorldBoundaries, boolean drawWorldMap, String... mapStyle) {
        if (renderer2D != null)
            this.renderer2D = renderer2D;

        this.drawWorldBoundaries = drawWorldBoundaries;
        this.drawWorldMap = drawWorldMap;

        int xPos = -35, yPos = 100;
        mapControlButton = new HoveringButton(ICON_WORLD_ON) {
            public void onSelectedChange(boolean selected) {
                WorldRenderPainter.this.drawWorldMap = selected;
            };
        };
        mapControlButton.setXYConfiguredPos(xPos, yPos);
        mapControlButton.setSelected(this.drawWorldMap);
        controlRenderButtons.add(mapControlButton);

        yPos += 5 + ICON_SIZE;
        mapSettingsButton = new HoveringButton(ICON_WORLD_SETTINGS) {
            public void onSelectedChange(boolean selected) {
                WorldRenderPainter.this.showChooseMapStyleDialog(WorldRenderPainter.this.renderer2D);
            };
        };
        mapSettingsButton.setXYConfiguredPos(xPos, yPos);
        mapSettingsButton.setToggle(false);
        controlRenderButtons.add(mapSettingsButton);

        yPos += 5 + ICON_SIZE;
        mapShowActiveLayerDialogButton = new HoveringButton(ICON_WORLD_DIALOG) {
            public void onSelectedChange(boolean selected) {
                Window wp = SwingUtilities.windowForComponent(WorldRenderPainter.this.renderer2D);
                for (String mp : mapActiveHolderList.keySet()) {
                    if (mapActiveHolderList.get(mp)) {
                        switch (hasPropertiesOrCustomDialog(mp)) {
                            case CustomOptionsDialog:
                                JDialog dialog = getOrCreateCustomOptionsDialog(mp);
                                if (dialog == null)
                                    continue;
                                boolean makeCustomOptionsDialogIndependent = getClassForStyle(mp).getAnnotation(
                                        MapTileProvider.class).makeCustomOptionsDialogIndependent();
                                if (!makeCustomOptionsDialogIndependent)
                                    dialog.setModalityType(ModalityType.DOCUMENT_MODAL);

                                dialog.requestFocus();
                                GuiUtils.centerParent(dialog, wp);
                                dialog.setVisible(true);
                                break;

                            case PropertiesDialog:
                                Vector<Field> dFA = new Vector<Field>();
                                PluginUtils.extractFieldsWorker(getClassForStyle(mp), dFA);
                                if (dFA.isEmpty())
                                    continue;
                                PropertiesProvider pprov = createPropertiesProvider(mp, dFA);
                                PropertiesEditor.editProperties(pprov, wp, true);
                                break;
                            case None:
                                break;
                        }
                    }
                }
            };
        };
        mapShowActiveLayerDialogButton.setXYConfiguredPos(xPos, yPos);
        mapShowActiveLayerDialogButton.setToggle(false);
        controlRenderButtons.add(mapShowActiveLayerDialogButton);

        for (String key : mapStyle) {
            if (mapActiveHolderList.containsKey(key)) {
                mapActiveHolderList.put(key, true);
            }
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
    }

    /**
     * This call will cleanup this component and then the component should not be used anymore.
     */
    public void cleanup() {
//        if (timer != null) {
//            timer.cancel();
//            timer = null;
//        }

        if (dialogProperties != null) {
            dialogProperties.setVisible(false);
            dialogProperties.dispose();
        }
        for (JDialog dialog : openPaintersDialog.values()) {
            if (dialog != null) {
                dialog.setVisible(false);
                dialog.dispose();
            }   
        }
        renderer2D = null;
    }

    public synchronized static void savePropertiesToDisk() {
        try {
            PluginUtils.saveProperties(
                    ROOT_PREFIX + "conf/" + WorldRenderPainter.class.getSimpleName().toLowerCase() + ".properties",
                    WorldRenderPainter.class);
        }
        catch (Exception e) {
            NeptusLog.pub().error("Not possible to open \"conf/"
                    + WorldRenderPainter.class.getSimpleName().toLowerCase() + ".properties\"");
        }
    }

    /**
     * @return the useTransparency
     */
    public boolean isUseTransparency() {
        return useTransparency;
    }

    /**
     * @param useTransparency the useTransparency to set
     */
    public void setUseTransparency(boolean useTransparency) {
        this.useTransparency = useTransparency;
    }

    /**
     * @return the maxLevelOfDetail
     */
    public static int getMaxLevelOfDetail(String mapStyle) {
        Class<?> clazz;
        Class<? extends Tile> clazz1 = tileClassList.get(mapStyle);
        if (clazz1 == null) {
            MapPainterProvider mp = mapPainterHolderList.get(mapStyle);
            if (mp == null)
                return MAX_LEVEL_OF_DETAIL;
            else
                clazz = mp.getClass();
        }
        else {
            clazz = clazz1; 
        }

        try {
            return (Integer) clazz.getMethod("getMaxLevelOfDetail").invoke(null);
        }
        catch (Exception e) {
            return MAX_LEVEL_OF_DETAIL;
        }
    }

    /**
     * @return the numberOfLoadingMapTiles
     */
    public static long getNumberOfLoadingMapTiles() {
        return numberOfLoadingMapTiles;
    }

    /**
     * @return the numberOfLoadedMapTiles
     */
    public static long getNumberOfLoadedMapTiles() {
        return numberOfLoadedMapTiles;
    }

    /**
     * 
     */
    public static void clearMemCache() {
        for (Map<String, Tile> map : tileHolderList.values()) {
            Tile[] lst = map.values().toArray(new Tile[0]);
            map.clear();
            for (Tile tile : lst) {
                tile.dispose();
            }
        }
    }

    /**
     * @param mapStyle
     */
    public static void clearMemCache(String mapStyle) {
        Map<String, Tile> map = tileHolderList.get(mapStyle);
        if (map != null) {
            Tile[] lst = map.values().toArray(new Tile[0]);
            map.clear();
            for (Tile tile : lst) {
                tile.dispose();
            }
        }
    }

    /**
     * 
     */
    public static void clearDiskCache() {
        for (Class<? extends Tile> clazz : tileClassList.values()) {
            try {
                clazz.getMethod("clearDiskCache").invoke(null);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param mapStyle
     */
    public static void clearDiskCache(String mapStyle) {
        Class<? extends Tile> clazz = tileClassList.get(mapStyle);
        if (clazz != null)
            try {
                clazz.getMethod("clearDiskCache").invoke(null);
            }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    //    /**
    //     * @return the mapStyle
    //     */
    //    public String getMapStyle() {
    //        return mapStyle;
    //    }
    //
    /**
     * @param mapStyle the mapStyle to set
     */
    public void setMapStyle(String mapStyleName) {
        setMapStyle(true, true, mapStyleName);
    }

    public void setMapStyle(boolean exclusive, boolean activate, String... mapStyleName) {
        List<String> mapStyleList = Arrays.asList(mapStyleName);
        for (String mapStyle : mapStyleList) {
            for (String mapKey : mapActiveHolderList.keySet()) {
                if (mapKey.equalsIgnoreCase(mapStyle)) {
                    mapActiveHolderList.put(mapKey, activate);
                }
                else {
                    if (exclusive && !mapStyleList.contains(mapKey)) {
                        mapActiveHolderList.put(mapKey, !activate);
                    }
                }
            }
        }

        updateDefaultActiveLayers();
        savePropertiesToDisk();
    }

    private void updateDefaultActiveLayers() {
        String tmp = "";
        for (String mapKey : mapActiveHolderList.keySet()) {
            if (mapActiveHolderList.get(mapKey)) {
                tmp += (tmp.length() != 0 ? ";" : "") + mapKey;
                if (!mapBaseOrLayerHolderList.get(mapKey) && mapLayerPrioriryHolderList.get(mapKey) != 0)
                    tmp += ":" + mapLayerPrioriryHolderList.get(mapKey);
            }
        }
        defaultActiveLayers = tmp;
    }

    /**
     * @return the drawWorldBoundaries
     */
    public boolean isDrawWorldBoundaries() {
        return drawWorldBoundaries;
    }

    /**
     * @param drawWorldBoundaries the drawWorldBoundaries to set
     */
    public void setDrawWorldBoundaries(boolean drawWorldBoundaries) {
        this.drawWorldBoundaries = drawWorldBoundaries;
    }

    /**
     * @return the drawWorldMap
     */
    public boolean isDrawWorldMap() {
        return drawWorldMap;
    }

    /**
     * @param drawWorldMap the drawWorldMap to set
     */
    public void setDrawWorldMap(boolean drawWorldMap) {
        this.drawWorldMap = drawWorldMap;
        mapControlButton.setSelected(drawWorldMap);
    }

    /**
     * @return the showOnScreenControls
     */
    public boolean isShowOnScreenControls() {
        return showOnScreenControls;
    }

    /**
     * @param showOnScreenControls the showOnScreenControls to set
     */
    public void setShowOnScreenControls(boolean showOnScreenControls) {
        this.showOnScreenControls = showOnScreenControls;
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.renderer2d.Renderer2DPainter#paint(java.awt.Graphics2D, pt.lsts.neptus.renderer2d.StateRenderer2D)
     */
    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        paint(g, renderer, useTransparency);
    }

    public void paint(Graphics2D g, StateRenderer2D renderer, boolean useTransparency) {
        renderer2D = renderer;
        if (drawWorldBoundaries)
            drawWorldBoundaries(g, renderer);
        if (drawWorldMap)
            drawWorldMap(g, renderer, useTransparency);        
    }

    /**
     * 
     */
    public Renderer2DPainter getPostRenderPainter() {
        if (postRenderPainter == null) {
            postRenderPainter = new ControlsPostRender();
        }
        return postRenderPainter;
    }

    @LayerPriority (priority = 100)
    public class ControlsPostRender implements Renderer2DPainter {
        @Override
        public void paint(Graphics2D g, StateRenderer2D renderer) {
            if (!isShowOnScreenControls()) {
                for (HoveringButton hb : controlRenderButtons) {
                    hb.setVisible(false);
                }
                return;
            }

            boolean visible = true;
            if (!mapControlButton.isSelected()) {
                visible = false;
            }
            for (HoveringButton hb : controlRenderButtons) {
                if (hb != mapControlButton)
                    hb.setVisible(visible);
            }
            if (visible) {
                boolean show = false;
                for (String ms : mapActiveHolderList.keySet()) {
                    if (!mapActiveHolderList.get(ms))
                        continue;
                    if (hasPropertiesOrCustomDialog(ms) != PropertiesOrCustomOptionsDialogEnum.None) {
                        show = true;
                        break;
                    }
                }
                mapShowActiveLayerDialogButton.setVisible(show);
            }
            for (HoveringButton hb : controlRenderButtons) {
                hb.paint(g, renderer);
            }
        }
    }

    /**
     * @param g
     * @param renderer
     */
    private void drawWorldBoundaries(Graphics2D g, StateRenderer2D renderer) {
        // Draw the world boundaries
        Graphics2D g2t = (Graphics2D) g.create();
        g2t.translate(renderer.getWidth() / 2, renderer.getHeight() / 2);
        int msize = MapTileUtil.mapSize(renderer.getLevelOfDetail());

        Point2D xyWC = renderer.getCenter().getPointInPixel(renderer.getLevelOfDetail());

        double[] rMinMax = {0.0, 0.0, msize, msize}; // [wXMin, wYMin, wXMax, wYMax]
        if (renderer.getRotation() != 0) {
            rMinMax = getRendererWorldXYMinMax(renderer);
        }
        int minX = ((int)rMinMax[0] - (int)xyWC.getX());
        int maxX = ((int)rMinMax[2] - (int)xyWC.getX());
        int minY = ((int)rMinMax[1] - (int)xyWC.getY());
        int maxY = ((int)rMinMax[3] - (int)xyWC.getY());
        GeneralPath wShape = new GeneralPath();
        int debugOff = -1;
        if (renderer.getRotation() == 0) {
            // The use of the clipping makes it for the boundary never to be drawn bigger than the render size
            wShape.moveTo(Math.max(minX,-renderer.getWidth()/2)+debugOff, Math.max(minY,-renderer.getHeight()/2)+debugOff);
            wShape.lineTo(Math.max(minX,-renderer.getWidth()/2)+debugOff, Math.min(maxY,renderer.getHeight()/2)-debugOff);
            wShape.lineTo(Math.min(maxX,renderer.getWidth()/2)-debugOff, Math.min(maxY,renderer.getHeight()/2)-debugOff);
            wShape.lineTo(Math.min(maxX,renderer.getWidth()/2)-debugOff, Math.max(minY,-renderer.getHeight()/2)+debugOff);
        }
        else {
            // The use of the clipping makes it for the boundary never to be drawn bigger than the render size
            wShape.moveTo(minX+debugOff, minY+debugOff);
            wShape.lineTo(minX+debugOff, maxY-debugOff);
            wShape.lineTo(maxX-debugOff, maxY-debugOff);
            wShape.lineTo(maxX-debugOff, minY+debugOff);
        }
        wShape.closePath();
        g2t.rotate(-renderer.getRotation());
        g2t.setColor(COLOR_BLACK_TRANS_40);
        g2t.draw(wShape);
        g2t.dispose();
        g2t = null;
    }

    /**
     * @param g
     * @param renderer
     */
    private void drawWorldMap(Graphics2D g, StateRenderer2D renderer, boolean useTransparency) {
        List<String> mapKeys = mapsOrderedForPainting; // getOrderedMapList(true);
        for (String mapKey : mapKeys) {
            String mapStyle = mapKey;

            if (!mapActiveHolderList.get(mapKey))
                continue;

            if (renderer.getLevelOfDetail() <= getMaxLevelOfDetail(mapStyle)) {
                if (tileHolderList.containsKey(mapKey)) {
                    int[] tmmr = getTileMinMaxForRenderer(renderer);
                    int tileXMin = tmmr[0];
                    int tileXMax = tmmr[2];
                    int tileYMin = tmmr[1];
                    int tileYMax = tmmr[3];

                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.rotate(-renderer.getRotation(), renderer.getWidth() / 2, renderer.getHeight() / 2);
                    for (int x = tileXMin; x <= tileXMax; x++) {
                        for (int y = tileYMin; y <= tileYMax; y++) {
                            String quadKey = MapTileUtil.tileXYToQuadKey(x, y, renderer.getLevelOfDetail());
                            Tile tile = null;
                            try {
                                Map<String, Tile> map = tileHolderList.get(mapStyle);
                                if (map != null) {
                                    tile = map.get(quadKey);
                                    if (tile == null) {
                                        try {
                                            tile = createTile(quadKey, mapStyle);
                                            if (tile != null)
                                                map.put(quadKey, tile);
                                        }
                                        catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                            if (tile != null) {
                                if (tile.getState() == TileState.ERROR)
                                    tile.retryLoadingTile();
                                tile.paint(g2, renderer, useTransparency);
                            }
                        }
                    }
                    g2.dispose();
//                    break;
                }
                else if (mapPainterHolderList.containsKey(mapStyle)) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    try {
                        mapPainterHolderList.get(mapStyle).paint(g2, renderer);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    g2.dispose();
//                    break;
                }
            }
        }
        drawLatLonZeroMarker(g, renderer);
        drawOverlayName(g, renderer);
    }

    private Tile createTile(String quadKey, String mapStyle) throws Exception {
        Class<? extends Tile> clazz = tileClassList.get(mapStyle);
        if (clazz != null) {
            Tile tile = clazz.getConstructor(String.class).newInstance(quadKey);
            if (tile != null)
                return tile;
        }
        return null;
    }

    /**
     * @param g
     * @param renderer
     */
    private void drawLatLonZeroMarker(Graphics2D g, StateRenderer2D renderer) {
        Graphics2D g2 = (Graphics2D) g.create();
        Point2D gXY = renderer.getScreenPosition(LocationType.ABSOLUTE_ZERO);
        g2.translate(gXY.getX(), gXY.getY());
        g2.setColor(COLOR_CYAN_TRANSP_200);
        g2.drawOval(-4, -4, 8, 8);
        g2.dispose();
    }

    /**
     * @param g
     * @param renderer
     */
    private void drawOverlayName(Graphics2D g, StateRenderer2D renderer) {
        String mapStyle = "";
        for (String mapKey : mapActiveHolderList.keySet()) {
            if (mapActiveHolderList.get(mapKey)) {
                String lodTxt = "";
                if (renderer.getLevelOfDetail() > getMaxLevelOfDetail(mapKey))
                    lodTxt = " (" + I18n.textf("no tiles at this zoom, max is %zoomLevel", getMaxLevelOfDetail(mapKey)) + ")";
                mapStyle += (mapStyle.length() != 0 ? ", " : "") + mapKey + lodTxt;
            }
        }

        Graphics2D gg = (Graphics2D) g.create();
        gg.setColor(COLOR_WHITE_TRANS_150);
        gg.setFont(new Font("Arial", 0, 10));
        String text = I18n.textf("\u00A9 %layerName world overlay", mapStyle.toString().toLowerCase());
        text = StringUtils.wrapEveryNChars(text, (short) 100);
        Rectangle2D stringBounds = gg.getFontMetrics().getStringBounds(text, g);
        int advance = (int) (renderer.getWidth() - 20 - stringBounds.getWidth()) / 2;
        gg.drawString(text, advance + 10, (float) (renderer.getHeight() - stringBounds.getHeight() - 10));
        gg.setColor(COLOR_BLACK_TRANS_150);
        gg.drawString(text, advance + 10 + 1, (float) (renderer.getHeight() - stringBounds.getHeight() - 10 + 1));
        gg.dispose();
    }

    /**
     * @param renderer
     * @return wXMin, wYMin, wXMax, wYMax array
     */
    public static double[] getRendererWorldXYMinMax(StateRenderer2D renderer) {
        Point2D xyWC = renderer.getCenter().getPointInPixel(renderer.getLevelOfDetail());
        double wXMin = xyWC.getX() - renderer.getWidth() / 2.0;
        double wXMax = xyWC.getX() + renderer.getWidth() / 2.0;
        double wYMin = xyWC.getY() - renderer.getHeight() / 2.0;
        double wYMax = xyWC.getY() + renderer.getHeight() / 2.0;

        if (renderer.getRotation() != 0) {
            double[] llbox = getRendererWorldLatLonDegsMinMax(renderer);
            double latMax = llbox[2];
            double latMin = llbox[0];
            double lonMax = llbox[3];
            double lonMin = llbox[1];

            Point2D lmin = MapTileUtil.degreesToXY(latMin, lonMin, renderer.getLevelOfDetail());
            Point2D lmax = MapTileUtil.degreesToXY(latMax, lonMax, renderer.getLevelOfDetail());
            wXMin = Math.min(lmin.getX(), lmax.getX());
            wXMax = Math.max(lmin.getX(), lmax.getX());
            wYMin = Math.min(lmin.getY(), lmax.getY());
            wYMax = Math.max(lmin.getY(), lmax.getY());
        }

        return new double[] { wXMin, wYMin, wXMax, wYMax };
    }

    /**
     * @param renderer
     * @return latMin, lonMin, latMax, lonMax array
     */
    public static double[] getRendererWorldLatLonDegsMinMax(StateRenderer2D renderer) {
        LocationType topLeft = renderer.getRealWorldLocation(new Point2D.Double(0,0));
        LocationType bottomRight = renderer.getRealWorldLocation(new Point2D.Double(renderer.getWidth(),renderer.getHeight()));
        LocationType topRight = renderer.getRealWorldLocation(new Point2D.Double(renderer.getWidth(),0));
        LocationType bottomLeft = renderer.getRealWorldLocation(new Point2D.Double(0, renderer.getHeight()));

        double lat1, lat2, lat3, lat4, lon1, lon2, lon3, lon4;
        lat1 = topLeft.getLatitudeDegs();
        lat2 = bottomRight.getLatitudeDegs();
        lat3 = topRight.getLatitudeDegs();
        lat4 = bottomLeft.getLatitudeDegs();

        lon1 = topLeft.getLongitudeDegs();
        lon2 = bottomRight.getLongitudeDegs();
        lon3 = topRight.getLongitudeDegs();
        lon4 = bottomLeft.getLongitudeDegs();

        double latMax = Math.max(Math.max(Math.max(lat1,lat2),lat3),lat4);
        double latMin = Math.min(Math.min(Math.min(lat1,lat2),lat3),lat4);
        double lonMax = Math.max(Math.max(Math.max(lon1,lon2),lon3),lon4);
        double lonMin = Math.min(Math.min(Math.min(lon1,lon2),lon3),lon4);

        if (renderer.getRotation() != 0) {
            latMax = Math.max(Math.max(Math.max(lat1,lat2),lat3),lat4);
            latMin = Math.min(Math.min(Math.min(lat1,lat2),lat3),lat4);
            lonMax = Math.max(Math.max(Math.max(lon1,lon2),lon3),lon4);
            lonMin = Math.min(Math.min(Math.min(lon1,lon2),lon3),lon4);
        }

        return new double[] { latMin, lonMin, latMax, lonMax };
    }

    /**
     * Return the tileXMin, tileYMin, tileXMax, tileYMax array for the current 
     * renderer level of detail.
     * @param renderer
     * @return tileXMin, tileYMin, tileXMax, tileYMax array
     */
    public static int[] getTileMinMaxForRenderer(StateRenderer2D renderer) {
        double[] tmpRWMaxMin = getRendererWorldXYMinMax(renderer);
        int wXMin = (int) tmpRWMaxMin[0];
        int wXMax = (int) tmpRWMaxMin[2];
        int wYMin = (int) tmpRWMaxMin[1];
        int wYMax = (int) tmpRWMaxMin[3];

        int[] tlMin = MapTileUtil.pixelXYToTileXY(wXMin, wYMin);
        int[] tlMax = MapTileUtil.pixelXYToTileXY(wXMax, wYMax);
        int tileXMin = tlMin[0];
        int tileXMax = tlMax[0];
        int tileYMin = tlMin[1];
        int tileYMax = tlMax[1];
        return new int[] { tileXMin, tileYMin, tileXMax, tileYMax };
    }

    private void fetchAllTilesForRendererVisibleArea(StateRenderer2D renderer, String mapStyle) {
        int[] tmmr = getTileMinMaxForRenderer(renderer);
        int tileXMin = tmmr[0];
        int tileXMax = tmmr[2];
        int tileYMin = tmmr[1];
        int tileYMax = tmmr[3];
        int levelOfDetail = renderer.getLevelOfDetail();
        int maxLevelOfDetail = Math.min(getMaxLevelOfDetail(mapStyle), levelOfDetail + 2);
        NeptusLog.pub().info("<###>tileXMin=" + tileXMin + ", tileYMin=" + tileYMin + ", tileXMax=" + tileXMax + ", tileYMax=" + tileYMax);
        Vector<String> bagList = new Vector<String>();
        for (int x = tileXMin; x <= tileXMax; x++) {
            for (int y = tileYMin; y <= tileYMax; y++) {
                String quadKey = MapTileUtil.tileXYToQuadKey(x, y, levelOfDetail);
                bagList.add(quadKey);
                //                NeptusLog.pub().info("<###> "+maxLevelOfDetail + " >= \t" + levelOfDetail + " :: \t" + quadKey);
                if (levelOfDetail >= maxLevelOfDetail)
                    continue;
                for (int sLoD = levelOfDetail + 1; sLoD <= maxLevelOfDetail; sLoD++) {
                    produceQuadKeysWorker(quadKey, maxLevelOfDetail, bagList);
                }
            }
        }
        NeptusLog.pub().info("<###> "+bagList.size() + " tiles");
        Collections.sort(bagList, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.length() - o2.length();
            }
        });
        //        GuiUtils.printList(bagList);

        for (String quadKey : bagList) {
            Map<String, Tile> map = tileHolderList.get(mapStyle);
            if (map != null) {
                Tile tile = map.get(quadKey);
                if (tile == null) {
                    try {
                        tile = createTile(quadKey, mapStyle);
                        if (tile != null)
                            map.put(quadKey, tile);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                else {
                    if (tile.getState() == TileState.ERROR)
                        tile.retryLoadingTile();
                }
            }
        }
    }

    private List<String> produceQuadKeysWorker(String quadKey, int maxLevelOfDetail, List<String> bagList) {
        if (quadKey.length() >= maxLevelOfDetail)
            return bagList;
        String qk0 = quadKey + "0";
        String qk1 = quadKey + "1";
        String qk2 = quadKey + "2";
        String qk3 = quadKey + "3";

        bagList.add(qk0);
        bagList.add(qk1);
        bagList.add(qk2);
        bagList.add(qk3);

        //        NeptusLog.pub().info("<###> "+maxLevelOfDetail + " >= \t" + (quadKey.length() + 1) + " :: \t" + qk0);
        produceQuadKeysWorker(qk0, maxLevelOfDetail, bagList);
        //        NeptusLog.pub().info("<###> "+maxLevelOfDetail + " >= \t" + (quadKey.length() + 1) + " :: \t" + qk1);
        produceQuadKeysWorker(qk1, maxLevelOfDetail, bagList);
        //        NeptusLog.pub().info("<###> "+maxLevelOfDetail + " >= \t" + (quadKey.length() + 1) + " :: \t" + qk2);
        produceQuadKeysWorker(qk2, maxLevelOfDetail, bagList);
        //        NeptusLog.pub().info("<###> "+maxLevelOfDetail + " >= \t" + (quadKey.length() + 1) + " :: \t" + qk3);
        produceQuadKeysWorker(qk3, maxLevelOfDetail, bagList);

        return bagList;
    }

    public void showChooseMapStyleDialog(Component parent) {
        if (dialogProperties != null) {
            GuiUtils.centerParent(dialogProperties, dialogProperties.getOwner());
            dialogProperties.setVisible(true);
            //dialogProperties.dispose();
            return;
        }

        createChooseMapStyleDialog();
        dialogProperties.setVisible(true);
    }

    /**
     * @param stateRenderer2D
     */
    @SuppressWarnings("serial")
    public void createChooseMapStyleDialog() {
        if (dialogProperties != null) {
            return;
        }
        Window winParent = SwingUtilities.windowForComponent(renderer2D); //parent);
        dialogProperties = new JDialog(winParent);
        dialogProperties.setLayout(new BorderLayout(10, 0));
        dialogProperties.setSize(700, 350);
        dialogProperties.setIconImages(ConfigFetch.getIconImagesForFrames());
        dialogProperties.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialogProperties.setTitle(I18n.text("World Map Layer"));
        
        ButtonGroup baseMapsButtonGroup = new ButtonGroup();
        JPanel confPanel = new JPanel(new MigLayout("ins 0, wrap 5"));
        confPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        List<String> mapKeys = getOrderedMapList();
        boolean alreadyinsertedBaseOrLayerMapSeparator = false;
        confPanel.add(new JLabel("<html><b>" + I18n.text("Base Maps") + "</b></html>"), "wrap");
        for (final String ms : mapKeys) {
            if (!alreadyinsertedBaseOrLayerMapSeparator) {
                if (mapBaseOrLayerHolderList.containsKey(ms) && !mapBaseOrLayerHolderList.get(ms)) {
                    alreadyinsertedBaseOrLayerMapSeparator = true;
                    confPanel.add(new JLabel("<html><b>" + I18n.text("Layer Maps") + "</b></html>"), "wrap");
                }
            }
            
            final JToggleButton rButton;
            if (mapBaseOrLayerHolderList.containsKey(ms) && mapBaseOrLayerHolderList.get(ms))
                rButton = new JRadioButton(ms.toString());
            else
                rButton = new JCheckBox(ms.toString());
            rButton.setActionCommand(ms);
            if (mapActiveHolderList.containsKey(ms) && mapActiveHolderList.get(ms))
                rButton.setSelected(true);
            rButton.addItemListener(new ItemListener() {
                @Override
                public void itemStateChanged(ItemEvent e) {
                    WorldRenderPainter.this.setMapStyle(false, rButton.isSelected(), ms);
                }
            });
            if (mapBaseOrLayerHolderList.containsKey(ms) && mapBaseOrLayerHolderList.get(ms))
                baseMapsButtonGroup.add(rButton);
            confPanel.add(rButton, "sg sel, grow, push");

            if (mapBaseOrLayerHolderList.containsKey(ms) && mapBaseOrLayerHolderList.get(ms)) {
                confPanel.add(new JLabel(), "sg prio");
            }
            else {
                short lp = mapLayerPrioriryHolderList.get(ms);
                final JSpinner spinner = new JSpinner(new SpinnerNumberModel(lp, 0, 10, 1));
                spinner.setSize(new Dimension(20, 20));
                spinner.setToolTipText(I18n.text("This sets the layer priority. The higher the value more on top will appear."));
                ((JSpinner.NumberEditor) spinner.getEditor()).getTextField().setEditable(false);
                ((JSpinner.NumberEditor) spinner.getEditor()).getTextField().setBackground(Color.WHITE);
                spinner.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        short val = ((Integer) spinner.getValue()).shortValue();
                        mapLayerPrioriryHolderList.put(ms, val);
                        updateDefaultActiveLayers();
                        refreshMapsListOrderedForPainting();
                        savePropertiesToDisk();
                    }
                });
                confPanel.add(spinner, "sg prio, width 50:50:");
            }

            boolean tileOrMapProvider = isTileOrMapProvider(ms);
            if (tileOrMapProvider) {
                final JButton clearButton = new JButton();
                AbstractAction clearAction = new AbstractAction(I18n.text("Clear cache").toLowerCase()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        clearButton.setEnabled(false);
                        new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                clearMemCache(ms);
                                clearDiskCache(ms);
                                return null;
                            }

                            @Override
                            protected void done() {
                                try {
                                    get();
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e);
                                }
                                clearButton.setEnabled(true);
                            }
                        }.execute();
                    }
                };
                clearButton.setAction(clearAction);
                confPanel.add(clearButton, "sg buttons");
            }
            else {
                confPanel.add(new JLabel(), "sg buttons");
            }

            final Class<?> clazz = getClassForStyle(ms);
            if (clazz.getAnnotation(MapTileProvider.class).usePropertiesOrCustomOptionsDialog()) {
                Vector<Field> dFA = new Vector<Field>();
                PluginUtils.extractFieldsWorker(clazz, dFA);
                if (dFA.isEmpty()) {
                    JLabel label = new JLabel(I18n.text("No properties").toLowerCase());
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setEnabled(false);
                    confPanel.add(label, "sg buttons");
                }
                else {
                    final PropertiesProvider pprov = createPropertiesProvider(ms, dFA);
                    confPanel.add(new JButton(new AbstractAction(I18n.text("Edit properties").toLowerCase()) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PropertiesEditor.editProperties(pprov, dialogProperties, true);
                        }
                    }), "sg buttons");
                }
            }
            else {
                boolean makeCustomOptionsDialogIndependent = clazz.getAnnotation(MapTileProvider.class)
                        .makeCustomOptionsDialogIndependent();
                try {
                    JDialog dialog = getOrCreateCustomOptionsDialog(ms);
                    if (dialog == null)
                        throw new Exception("No custom options dialog found!!");

                    if (!makeCustomOptionsDialogIndependent)
                        dialog.setModalityType(ModalityType.DOCUMENT_MODAL);

                    final JDialog dialog1 = dialog;
                    confPanel.add(new JButton(new AbstractAction(I18n.text("Edit properties").toLowerCase()) {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            dialog1.requestFocus();
                            GuiUtils.centerParent(dialog1, dialogProperties);
                            dialog1.setVisible(true);
                        }
                    }), "sg buttons");
                }
                catch (Exception e1) {
                    JLabel label = new JLabel(I18n.text("No properties").toLowerCase());
                    label.setHorizontalAlignment(SwingConstants.CENTER);
                    label.setEnabled(false);
                    confPanel.add(label, "sg buttons");
                }
            }

            boolean isFetch = false;
            if (tileOrMapProvider) {
                try {
                    Class<? extends Tile> clazz1 = tileClassList.get(ms);
                    isFetch = (Boolean) clazz1.getMethod("isFetchableOrGenerated").invoke(null);
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
            if (!isFetch) {
                JLabel label = new JLabel("");
                label.setHorizontalAlignment(SwingConstants.CENTER);
                label.setEnabled(false);
                confPanel.add(label, "sg buttons");
            }
            else {
                final JButton fetchButton = new JButton();
                /// To fetch the map tiles from the visible area
                AbstractAction fetchAction = new AbstractAction(I18n.text("Fetch visible area").toLowerCase()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        fetchButton.setEnabled(false);
                        new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                if (renderer2D != null)
                                    fetchAllTilesForRendererVisibleArea(renderer2D, ms);
                                return null;
                            }

                            @Override
                            protected void done() {
                                try {
                                    get();
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e);
                                }
                                fetchButton.setEnabled(true);
                            }
                        }.execute();
                    }
                };
                fetchButton.setAction(fetchAction);
                fetchButton.setToolTipText(I18n.text("Fetch visible area tiles to up to 2 more zoom levels."));
                confPanel.add(fetchButton, "sg buttons");
            }
        }

        final JLabel levelOfDetailLabel = new JLabel();
        final JButton zoomInButton = new JButton(new AbstractAction("+") {
            @Override
            public void actionPerformed(ActionEvent e) {
                renderer2D.zoomIn();
            }
        });
        zoomInButton.setToolTipText(I18n.text("Zoom in"));
        final JButton zoomOutButton = new JButton(new AbstractAction("-") {
            @Override
            public void actionPerformed(ActionEvent e) {
                renderer2D.zoomOut();
            }
        });
        zoomOutButton.setToolTipText(I18n.text("Zoom out"));
        final JLabel memInfoLabel = new JLabel();
        final JLabel loadingTilesLabel = new JLabel();
        final JButton stopLoadingButton = new JButton(new AbstractAction(I18n.text("Stop Loading")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearMemCache();
            }
        });
        final JXBusyLabel busyPanel = InfiniteProgressPanel.createBusyAnimationInfiniteBeans(20);
        busyPanel.setVisible(false);
        JXStatusBar statusBar = new JXStatusBar();
        statusBar.add(levelOfDetailLabel);
        statusBar.add(zoomInButton);
        statusBar.add(zoomOutButton);
        statusBar.add(memInfoLabel, JXStatusBar.Constraint.ResizeBehavior.FILL);
        statusBar.add(loadingTilesLabel);
        statusBar.add(stopLoadingButton);
        statusBar.add(busyPanel);
        dialogProperties.add(statusBar, BorderLayout.NORTH);

        final JCheckBox useProxyCheck = new JCheckBox(new AbstractAction(I18n.text("Use HTTP Proxy")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    ProxyInfoProvider.setEnableProxy(((JCheckBox)e.getSource()).isSelected());
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });
        useProxyCheck.setSelected(ProxyInfoProvider.isEnableProxy());
        useProxyCheck.setOpaque(false);

        final JButton proxySettingsButton = new JButton(new AbstractAction(I18n.text("Configure HTTP Proxy")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                ProxyInfoProvider.showConfigurations(dialogProperties);
            }
        });

        JXStatusBar bottomStatusBar = new JXStatusBar();
        bottomStatusBar.add(useProxyCheck);
        bottomStatusBar.add(proxySettingsButton);

        dialogProperties.add(bottomStatusBar, BorderLayout.SOUTH);

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                int levelOfDetail = renderer2D.getLevelOfDetail();
                long nlt = WorldRenderPainter.getNumberOfLoadingMapTiles();
                long nlt1 = WorldRenderPainter.getNumberOfLoadedMapTiles();

                levelOfDetailLabel.setText(I18n.text("Zoom Level:") + " " + levelOfDetail);
                memInfoLabel.setText(I18n.textf("Free Memory: %freeMem of %totalMem",
                        MathMiscUtils.parseToEngineeringRadix2Notation(Runtime.getRuntime().freeMemory(), 1)
                        + "B",
                        MathMiscUtils.parseToEngineeringRadix2Notation(Runtime.getRuntime().totalMemory(), 1)
                        +  "B"));
                loadingTilesLabel.setText(I18n.textf("Tiles Loading: %tiles of %totalTiles", nlt, nlt1));
                busyPanel.setVisible(nlt != 0);
                busyPanel.setBusy(nlt != 0);

                useProxyCheck.setSelected(ProxyInfoProvider.isEnableProxy());

                dialogProperties.repaint();
            }
        }, 500, 200);

        JScrollPane scroll = new JScrollPane(confPanel);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        dialogProperties.add(scroll);
        GuiUtils.centerParent(dialogProperties, winParent);
        //        dialogProperties.setVisible(true);
    }

    private static void refreshMapsListOrderedForPainting() {
        mapsOrderedForPainting.clear();
        mapsOrderedForPainting.addAll(getOrderedMapList(false));
    }

    private static List<String> getOrderedMapList() {
        return getOrderedMapList(false);
    }

    private static List<String> getOrderedMapList(final boolean orderWithDisplayPriority) {
        // Order according with being base map or layer
        Comparator<String> comparatorMapBaseOrLayer = new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                boolean o1Base = mapBaseOrLayerHolderList.containsKey(o1) ? mapBaseOrLayerHolderList.get(o1) : false;
                boolean o2Base = mapBaseOrLayerHolderList.containsKey(o2) ? mapBaseOrLayerHolderList.get(o2) : false;
                short o1Prio = mapLayerPrioriryHolderList.containsKey(o1) ? mapLayerPrioriryHolderList.get(o1) : 0;
                short o2Prio = mapLayerPrioriryHolderList.containsKey(o2) ? mapLayerPrioriryHolderList.get(o2) : 0;
                if (o1Base ^ o2Base) // One base map other layer
                    return o1Base ? -1 : 1;
                else if (o1Base & o2Base) // Both base maps
                    return 0;                    
                else  // Both layer maps
                    return orderWithDisplayPriority ? o1Prio - o2Prio : 0;
            }
        };
        String[] tmpArrayMapKeysToSorted = mapActiveHolderList.keySet().toArray(new String[0]);
        Arrays.sort(tmpArrayMapKeysToSorted, comparatorMapBaseOrLayer);
        List<String> mapKeys = Arrays.asList(tmpArrayMapKeysToSorted);
        return mapKeys;
    }

    /**
     * @param mapStyle
     * @param dFA
     * @return
     */
    private PropertiesProvider createPropertiesProvider(final String mapStyle, Vector<Field> dFA) {
        final Class<?> clazz = getClassForStyle(mapStyle);
        final LinkedHashMap<String, PluginProperty> props = new LinkedHashMap<String, PluginProperty>();
        Map<String, PluginProperty> defaults = PluginUtils.getDefaultsValues(clazz);
        for (Field field : dFA) {
            try {
                String defaultStr = null;
                
                if (defaults.containsKey(field.getName()))
                    defaultStr = defaults.get(field.getName()).serialize();
                
                PluginProperty pp = PluginUtils.createPluginProperty(null, field, defaultStr, true);
                if (pp != null)
                    props.put(pp.getName(), pp);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        return new PropertiesProvider() {
            @Override
            public void setProperties(Property[] properties) {
                PluginUtils.setPluginProperties(clazz, properties);
                if (properties != null && properties.length > 0) {
                    try {
                        Method met = clazz.getMethod("staticPropertiesChanged");
                        if (met != null)
                            met.invoke(null);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public String[] getPropertiesErrors(Property[] properties) {
                //return PluginUtils.validatePluginProperties(clazz, properties);
                return null;
            }

            @Override
            public String getPropertiesDialogTitle() {
                return mapStyle.toString() +  " - " + I18n.text("Map Provider Properties");
            }

            @Override
            public DefaultProperty[] getProperties() {
                return props.values().toArray(new PluginProperty[0]);
            }
        };
    }

    /**
     * @param ms
     * @param tileOrMapProvider
     * @return
     */
    private Class<?> getClassForStyle(String mapStyle) {
        boolean tileOrMapProvider = isTileOrMapProvider(mapStyle);

        Class<?> clazz;
        if (tileOrMapProvider)
            clazz = tileClassList.get(mapStyle);
        else
            clazz = mapPainterHolderList.get(mapStyle).getClass();
        return clazz;
    }

    /**
     * @param mapStyle
     * @return
     */
    private boolean isTileOrMapProvider(String mapStyle) {
        boolean tileOrMapProvider = true;
        if (mapPainterHolderList.containsKey(mapStyle))
            tileOrMapProvider = false;
        return tileOrMapProvider;
    }

    /**
     * 
     */
    private PropertiesOrCustomOptionsDialogEnum hasPropertiesOrCustomDialog(String mapStyle) {
        PropertiesOrCustomOptionsDialogEnum ret = mapHasPropertiesOrCustomOptions.get(mapStyle);
        if (ret != null)
            return ret;

        Class<?> clazz = getClassForStyle(mapStyle);
        if (clazz == null) {
            mapHasPropertiesOrCustomOptions.put(mapStyle, PropertiesOrCustomOptionsDialogEnum.None);
            return PropertiesOrCustomOptionsDialogEnum.None;
        }
        if (clazz.getAnnotation(MapTileProvider.class).usePropertiesOrCustomOptionsDialog()) {
            Vector<Field> dFA = new Vector<Field>();
            PluginUtils.extractFieldsWorker(clazz, dFA);
            if (!dFA.isEmpty()) {
                mapHasPropertiesOrCustomOptions.put(mapStyle, PropertiesOrCustomOptionsDialogEnum.PropertiesDialog);
                return PropertiesOrCustomOptionsDialogEnum.PropertiesDialog;
            }
            else {
                mapHasPropertiesOrCustomOptions.put(mapStyle, PropertiesOrCustomOptionsDialogEnum.None);
                return PropertiesOrCustomOptionsDialogEnum.None;
            }
        }
        else {
            if (getOrCreateCustomOptionsDialog(mapStyle) != null) {
                mapHasPropertiesOrCustomOptions.put(mapStyle, PropertiesOrCustomOptionsDialogEnum.CustomOptionsDialog);
                return PropertiesOrCustomOptionsDialogEnum.CustomOptionsDialog;
            }
            else {
                mapHasPropertiesOrCustomOptions.put(mapStyle, PropertiesOrCustomOptionsDialogEnum.None);
                return PropertiesOrCustomOptionsDialogEnum.None;
            }
        }
    }

    /**
     * @param mapStyle
     * @param clazz
     * @return
     */
    private JDialog getOrCreateCustomOptionsDialog(String mapStyle) {
        createChooseMapStyleDialog(); // just to create if not already created

        Class<?> clazz = getClassForStyle(mapStyle);
        if (clazz == null)
            return null;
        if (clazz.getAnnotation(MapTileProvider.class).usePropertiesOrCustomOptionsDialog())
            return null;

        JDialog parentDialogProperties = this.dialogProperties;

        JDialog dialog = openPaintersDialog.get(mapStyle);

        if (dialog == null) {
            try {
                dialog = (JDialog) clazz.getMethod("getOptionsDialog", JDialog.class, StateRenderer2D.class).invoke(null,
                        parentDialogProperties, renderer2D);
            }
            catch (Exception e1) {
                try {
                    dialog = (JDialog) clazz.getMethod("getOptionsDialog", JDialog.class,
                            StateRenderer2D.class).invoke(mapPainterHolderList.get(mapStyle), parentDialogProperties,
                                    renderer2D);
                }
                catch (Exception e) {
                    try {
                        dialog = (JDialog) clazz.getMethod("getOptionsDialog", JDialog.class).invoke(null,
                                parentDialogProperties);
                    }
                    catch (Exception e2) {
                        try {
                            dialog = (JDialog) clazz.getMethod("getOptionsDialog", JDialog.class).invoke(
                                    mapPainterHolderList.get(mapStyle), parentDialogProperties);
                        }
                        catch (Exception e3) {
                            dialog = null;
                        }
                    }
                }
            }

            if (dialog != null)
                openPaintersDialog.put(mapStyle, dialog);
        }
        //        if (dialog == null)
        //            throw new Exception();
        return dialog;
    }

    //  Mouse related methods

    private boolean mouseActive = false;

    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseDragged(MouseEvent e) {
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseMoved(MouseEvent e) {
        if (!mouseActive)
            return;
        for (HoveringButton hb : controlRenderButtons) {
            Rectangle2D ret = hb.createRectangle2DBounds();
            if(ret.contains((Point2D)e.getPoint()))
                hb.setHovering(true);
            else
                hb.setHovering(false);
        }
        //        NeptusLog.pub().info("<###>mouseMoved > " +  e.getX() + " :: " + e.getY());
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseClicked(MouseEvent e) {
        if (!mouseActive)
            return;
        for (HoveringButton hb : controlRenderButtons) {
            Rectangle2D ret = hb.createRectangle2DBounds();
            if(ret.contains((Point2D)e.getPoint()))
                hb.toggleSelected();
        }
        //        NeptusLog.pub().info("<###>mouseClicked > " + e.getX() + " :: " + e.getY());
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    @Override
    public void mousePressed(MouseEvent e) {
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseReleased(MouseEvent e) {
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseEntered(MouseEvent e) {
        mouseActive = true;
        //        NeptusLog.pub().info("<###>mouseEntered > " + e.getX() + " :: " + e.getY());
    }

    /* (non-Javadoc)
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    @Override
    public void mouseExited(MouseEvent e) {
        mouseActive = false;
        for (HoveringButton hb : controlRenderButtons) {
            hb.setHovering(false);
        }
        //        NeptusLog.pub().info("<###>mouseExited > " + e.getX() + " :: " + e.getY());
    }
    
    public Map<String, MapPainterProvider> getMapPainters() {
        return Collections.unmodifiableMap(mapPainterHolderList);
    }
}
