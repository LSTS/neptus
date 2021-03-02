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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: tsmarques
 * 27 Apr 2015
 */
package pt.lsts.neptus.console.plugins.kml;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.imageio.ImageIO;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import de.micromata.opengis.kml.v_2_2_0.AbstractObject;
import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.GroundOverlay;
import de.micromata.opengis.kml.v_2_2_0.LatLonBox;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import de.micromata.opengis.kml.v_2_2_0.gx.LatLonQuad;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.SpeedType;
import pt.lsts.neptus.mp.SpeedType.Units;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.ImageElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.AngleUtils;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.NameNormalizer;
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author tsmarques
 * @author pdias
 */
@PluginDescription(name = "Kml Import", description = "Import map features from KML, from a file or URL",
    author = "tsmarques, pdias", version = "0.7", icon = "pt/lsts/neptus/console/plugins/kml/kml-icon.png")
@Popup(name = "Kml Import", pos = POSITION.CENTER, width = 230, height = 500)
@LayerPriority(priority = 50)
public class KmlImport extends ConsolePanel {
    private static final int MAX_NUMBER_OF_MANEUVERS_TO_IMPORT = 50;

    private static final Color COLOR_SELECTED = new Color(200, 255, 200);

    private JMenuBar menuBar;
    private JMenu openMenu;
    private JMenuItem kmlFile; /* load kml features from a file */
    private JMenuItem kmlUrl; /* load kml features from a URL */
    
    private String kmlFeatUrl; /* tmp store Url given by the user */

    private JPopupMenu rightClickPopup;
    private JMenuItem rightClickAddItem;
    private JMenuItem rightClickAddAsPlan; /* add kml LineStrings as vehicles plans */

    private JList<JLabel> listingPanel; /* actual listing of kml features */
    private final DefaultListModel<JLabel> listModel = new DefaultListModel<>();
    private JFileChooser fileChooser;

    private TreeMap<String, Feature> kmlFeatures; /* init in listKmlFeatures()*/
    private TreeMap<String, String> featuresGeom; /* init in listKmlFeatures()*/
    private ArrayList<String> addedFeatures; /* features already added to the map */    

    public KmlImport(ConsoleLayout console) {
        super(console);
        addedFeatures = new ArrayList<>();
        initPluginPanel();        
        initListingPanel();  
    }

    private void initPluginPanel() {
        setLayout(new BorderLayout());
        
        menuBar  = new JMenuBar();
        openMenu = new JMenu(I18n.text("Open"));
        kmlFile = new JMenuItem(I18n.text("Open from file"));
        kmlUrl = new JMenuItem(I18n.text("Open from URL"));
        kmlFeatUrl = "";

        openMenu.add(kmlFile);
        openMenu.add(kmlUrl);
        menuBar.add(openMenu);

        add(menuBar, BorderLayout.NORTH);
        addMenuListeners();

        fileChooser = GuiUtils.getFileChooser(ConfigFetch.getUserHomeFolder(),
                I18n.text("KML files"), "kml", "kmz");

        rightClickPopup = new JPopupMenu();
        rightClickAddItem = new JMenuItem(I18n.text("Add to map"));
        rightClickPopup.add(rightClickAddItem);

        rightClickAddItem.addActionListener(e -> {
            int selectedFeatureIndex = listingPanel.getSelectedIndex();
            String featName = listModel.getElementAt(selectedFeatureIndex).getText();
            String validID = NameNormalizer.asIdentifier(featName);
            String idByUser = JOptionPane.showInputDialog(I18n.text("Element ID"), validID);

            if(idByUser != null && !idByUser.isEmpty()) {
                SwingWorker<String, Void> sw = new SwingWorker<String, Void>() {
                    String ret = null;

                    @Override
                    protected String doInBackground() {
                        try {
                            ret = addFeatureToMap(featName, idByUser, false);
                        }
                        catch (Exception e1) {
                            ret = e1.getMessage();
                        }
                        return ret;
                    }

                    @Override
                    protected void done() {
                        if (ret != null && !ret.isEmpty()) {
                            GuiUtils.errorMessage(SwingUtilities.windowForComponent(KmlImport.this),
                                    KmlImport.this.getName(), ret);
                        }
                    }
                };
                sw.execute();
            }
        });
        
        rightClickAddAsPlan = new JMenuItem(I18n.text("Add as plan"));
        rightClickAddAsPlan.addActionListener(e -> {
            int selectedFeatureIndex = listingPanel.getSelectedIndex();
            String featName = listModel.getElementAt(selectedFeatureIndex).getText();
            String validID = NameNormalizer.asIdentifier(featName);
            String idByUser = JOptionPane.showInputDialog(I18n.text("Plan ID"), validID);

            if(idByUser != null && !idByUser.isEmpty()) {
                SwingWorker<String, Void> sw = new SwingWorker<String, Void>() {
                    String ret = null;

                    @Override
                    protected String doInBackground() {
                        try {
                            ret = addFeatureToMap(featName, idByUser, true);
                        }
                        catch (Exception e1) {
                            ret = e1.getMessage();
                        }
                        return ret;
                    }

                    @Override
                    protected void done() {
                        if (ret != null && !ret.isEmpty()) {
                            GuiUtils.errorMessage(SwingUtilities.windowForComponent(KmlImport.this),
                                    KmlImport.this.getName(), ret);
                        }
                    }
                };
                sw.execute();
            }
        });
    }

    private void initListingPanel() {
        listingPanel = new JList<>(listModel);
        
        JScrollPane scrollPane = new JScrollPane(listingPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(scrollPane);

        listingPanel.setCellRenderer(new CustomListCellRenderer());

        listingPanel.addMouseListener(new MouseAdapter() {
            private void addPopUpItems() {
                rightClickPopup.removeAll();
                rightClickPopup.add(rightClickAddItem);
                
                int selectedFeatureIndex = listingPanel.getSelectedIndex();
                String featName = listModel.getElementAt(selectedFeatureIndex).getText();
                String featGeom = featuresGeom.get(featName);
                
                if(featGeom.equals("LineString"))
                    rightClickPopup.add(rightClickAddAsPlan);
            }
            
            public void mousePressed(MouseEvent e) {
                showPopup(e);
            }

            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent me) {
                if(SwingUtilities.isRightMouseButton(me) 
                        && !listingPanel.isSelectionEmpty()
                        && listingPanel.locationToIndex(me.getPoint())
                        == listingPanel.getSelectedIndex()) {
                    
                    listingPanel.getSelectedValue().setBackground(COLOR_SELECTED);
                    addPopUpItems();
                    rightClickPopup.show(listingPanel, me.getX(), me.getY());
                }
            }
        });
    }

    private void listKmlFeatures(URL url, boolean fromFile) {
        cleanListing();

        KmlReader kml = new KmlReader(url, fromFile);
        if(!kml.streamIsOpen) {
            showErrorMessage("Stream could not be opened.");
            return;
        }
        kmlFeatures = kml.extractFeatures();
        featuresGeom = new TreeMap<>();

        for(String fname : kmlFeatures.keySet()) {
            String fgeom = extractFeatureType(fname);
            featuresGeom.put(fname, fgeom);
            listModel.addElement(getFeatureLabel(fname, fgeom));
        }
    }

    private String extractFeatureType(String fname) {
        Feature f = kmlFeatures.get(fname);
        if (f instanceof Placemark && ((Placemark) f).getGeometry() != null)
            return ((Placemark) f).getGeometry().getClass().getSimpleName();

        return f.getClass().getSimpleName();
    }
    
    private JLabel getFeatureLabel(String fname, String fgeom) {
        JLabel feature = new JLabel(fname);
        String iconUrl = "";

        switch (fgeom) {
            case "Point":
                iconUrl = "images/mark.png";
                break;
            case "LineString":
                iconUrl = "pt/lsts/neptus/plugins/map/interactions/draw-line.png";
                break;
            case "Polygon":
                iconUrl = "pt/lsts/neptus/plugins/map/interactions/poly.png";
                break;
            case "GroundOverlay":
                iconUrl = "images/buttons/new_image.png";
                break;
        }
        
        feature.setName(fname);
        feature.setIcon(ImageUtils.getScaledIcon(iconUrl, 15, 15));        
        return feature;
    }

    private void addMenuListeners() {
        kmlFile.addActionListener(e -> {
            int result = fileChooser.showOpenDialog(getParent());
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                try {
                    URL fileUrl = selectedFile.toURI().toURL();
                    kmlFeatUrl = fileUrl.toString();
                    listKmlFeatures(fileUrl, true);
                }
                catch(MalformedURLException e1) {
                    e1.printStackTrace();
                }
            }
        });

        kmlUrl.addActionListener(e -> {
            String urlStr = JOptionPane.showInputDialog(I18n.text("Enter a URL"), kmlFeatUrl);
            if(urlStr != null && !urlStr.equals("")) {
                try {
                    kmlFeatUrl = urlStr;
                    listKmlFeatures(new URL(urlStr), false);
                }
                catch(MalformedURLException e1) {
                    showErrorMessage(I18n.text("URL not valid!"));
                }
            }
        });
    }

    private String addFeatureToMap(String featName, String idByUser, boolean addAsPlan) {
        String errorMsg = null;
        
        Feature f = kmlFeatures.get(featName);
        String featGeom = featuresGeom.get(featName);
        
        if (f instanceof Placemark) {
            Placemark feature = (Placemark) f;

            switch (featGeom) {
                case "Point":
                    addAsPoint((Point) feature.getGeometry(), idByUser);
                    break;
                case "LineString":
                    if (!addAsPlan)
                        addAsPathElement(feature, idByUser, false);
                    else
                        errorMsg = addLineStringAsPlan(feature, idByUser);
                    break;
                case "Polygon":
                    addAsPathElement(feature, idByUser, true);
                    break;
                default:
                    errorMsg = "No valid geometry found in Placemark!";
                    break;
            }
        }
        else if (f instanceof GroundOverlay) {
            errorMsg = addAsImage((GroundOverlay) f, idByUser);
        }
        else {
            errorMsg = "Feature not supported!";
        }
        
        if (errorMsg == null)
            addedFeatures.add(featName);
        
        return errorMsg;
    }
    
    private void addAsPoint(Point point, String idByUser) {
        Coordinate coords = point.getCoordinates().get(0);
        
        MapType mapType = getMapToAddElements();
        MarkElement kmlPoint = new MarkElement(mapType.getMapGroup(), mapType);
        LocationType kmlPointLoc = new LocationType(coords.getLatitude(), coords.getLongitude());

        kmlPoint.setId(idByUser);
        kmlPoint.setCenterLocation(kmlPointLoc);
        mapType.addObject(kmlPoint);
        
        MissionType mission = getConsole().getMission();
        mission.save(false);
    }
    
    /* Add a LineString or Polygon as a Neptus PathElement */
    private void addAsPathElement(Placemark feature, String idByUser, boolean isFilled) {
        MapType mapType = getMapToAddElements();
        List<Coordinate> coords = getPathCoordinates(feature, isFilled);       
        
        LocationType firstLoc = new LocationType(coords.get(0).getLatitude(), coords.get(0).getLongitude());
        PathElement pathElem = new PathElement(mapType.getMapGroup(), mapType, firstLoc);
        pathElem.addPoint(0, 0, 0, false);

        /* add points to the path */
        for(int i = 1; i < coords.size(); i++) {
            Coordinate coord = coords.get(i);
            LocationType elemLoc = new LocationType(coord.getLatitude(), coord.getLongitude());
            
            double[] offsets = elemLoc.getOffsetFrom(firstLoc);
            pathElem.addPoint(offsets[1], offsets[0], offsets[2], false);
        }    
        
        pathElem.setId(idByUser);
        pathElem.setFilled(isFilled);
        pathElem.setShape(isFilled);
        
        mapType.addObject(pathElem);
        
        MissionType mission = getConsole().getMission();
        mission.save(false);
    }
    
    private String addLineStringAsPlan(Placemark lineString, String idByUser) {
        MissionType mission = getConsole().getMission();
        
        String mainVehicle = getConsole().getMainSystem();
        PlanType plan = new PlanType(mission);
        
        plan.setId(idByUser);
        plan.setVehicle(mainVehicle);
        
        List<Coordinate> coords = getPathCoordinates(lineString, false);
        int nManeuver = 0;
        for (Coordinate coord : coords) {
            nManeuver++;
            
            if (nManeuver > MAX_NUMBER_OF_MANEUVERS_TO_IMPORT)
                return I18n.textf("Excessive number of maneuvers to add to plan (%n)", MAX_NUMBER_OF_MANEUVERS_TO_IMPORT);
            
            Goto maneuver = new Goto();
            maneuver.setId("point " + nManeuver);
            LocationType loc = new LocationType(coord.getLatitude(), coord.getLongitude());
            ManeuverLocation mloc = new ManeuverLocation(loc);
            mloc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
            mloc.setZ(0);
            
            maneuver.setSpeed(new SpeedType(1.3, Units.MPS));
            maneuver.setManeuverLocation(mloc);
            plan.getGraph().addManeuverAtEnd(maneuver);
        }
        mission.addPlan(plan);
        mission.save(false);
        getConsole().warnMissionListeners();
        
        return null;
    }
    
    private List<Coordinate> getPathCoordinates(Placemark feature, boolean featureIsPolygon) {
        List<Coordinate> coords;
        
        /* get coordinates of the LineStrings forming the polygon boundary */
        if(featureIsPolygon) {
            Polygon polyg = (Polygon) feature.getGeometry();
            coords = polyg.getOuterBoundaryIs().getLinearRing().getCoordinates();
        }
        else
            coords = ((LineString) feature.getGeometry()).getCoordinates();
        
        return coords;
    }
    
    /**
     * @param feature
     * @param idByUser
     */
    private String addAsImage(GroundOverlay feature, String idByUser) {
        MapType mapType = getMapToAddElements();
        
        String fHref = feature.getIcon().getHref();
        
        ImageElement imgElement = new ImageElement(mapType.getMapGroup(), mapType);
        
//        boolean fVisible = feature.isVisibility() == null ? true : feature.isVisibility();
        int transparency = 0; // 0 to 100
        try {
            String fColor = feature.getColor(); // aabbggrr
            if (fColor != null && fColor.length() == 8) {
                transparency = Integer.parseUnsignedInt(fColor.substring(0, 2), 16);
                transparency = transparency * 100 / 255;
                transparency = 100 - transparency; // It is the reverse!
            }
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        
        LocationType topLeftLoc = null;
        LocationType topRightLoc = null;
        LocationType bottomLeftLoc = null;
        LocationType bottomRightLoc = null;
        LatLonBox latLonBox = feature.getLatLonBox();
        if (latLonBox != null) {
            double nLat = latLonBox.getNorth();
            double sLat = latLonBox.getSouth();
            double eLon = latLonBox.getEast();
            double wLon = latLonBox.getWest();
            topLeftLoc = new LocationType(nLat, wLon);
            topRightLoc = new LocationType(nLat, eLon);
            bottomLeftLoc = new LocationType(sLat, wLon);
            bottomRightLoc = new LocationType(sLat, eLon);
        }
        else {
            List<AbstractObject> oExt = feature.getGroundOverlayObjectExtension();
            for (AbstractObject abstractObject : oExt) {
                if (abstractObject instanceof LatLonQuad) {
                    // The coordinates must be specified in counter-clockwise order with the first
                    // coordinate corresponding to the lower-left corner of the overlayed image.
                    LatLonQuad latLonQuad = (LatLonQuad) abstractObject;
                    List<Coordinate> coords = latLonQuad.getCoordinates();
                    if (coords.size() == 4) {
                        Coordinate cll = coords.get(0);
                        bottomLeftLoc = new LocationType(cll.getLatitude(), cll.getLongitude());
                        Coordinate clr = coords.get(1);
                        bottomRightLoc = new LocationType(clr.getLatitude(), clr.getLongitude());
                        Coordinate cur = coords.get(2);
                        topRightLoc = new LocationType(cur.getLatitude(), cur.getLongitude());
                        Coordinate cul = coords.get(3);
                        topLeftLoc = new LocationType(cul.getLatitude(), cul.getLongitude());
                    }
                    break;
                }
            }
            if (topLeftLoc == null)
                return I18n.text("Error parsing LatLonQuad element.");
        }
        
        double rotationDeg = latLonBox == null ? 0 : latLonBox.getRotation(); // CounterClockWise
        rotationDeg = AngleUtils.nomalizeAngleDegrees360(360 - rotationDeg); // Make it ClockWise
        
        // addPathElement(rotationDeg, topLeftLoc, topRightLoc, bottomRightLoc, bottomLeftLoc);

        // Getting the image to local storage
        URL urlKml;
        try {
            urlKml = new URL(kmlFeatUrl);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return I18n.textf("Some problem with KML file location (%URL).", kmlFeatUrl);
        }
        File imgFile = getImageFileFromKml(urlKml, fHref);
        if (imgFile == null || !imgFile.exists())
            return I18n.textf("Not possible to find image '%file'.", fHref);
        
        Image img = null;
        try {
            img = ImageIO.read(imgFile.toURI().toURL());
        }
        catch (Exception e1) {
            e1.printStackTrace();
        } // ImageUtils.getImageWaitLoad(imgFile.getAbsolutePath());
        if (img == null)
            img = ImageUtils.getImageWaitLoad(imgFile.getAbsolutePath());
        
        if (img == null)
            return I18n.textf("Not possible to find image '%file'.", fHref);
        
        if (latLonBox == null) {
            double maxLat = Math.max(topLeftLoc.getLatitudeDegs(), topRightLoc.getLatitudeDegs());
            maxLat = Math.max(maxLat, bottomRightLoc.getLatitudeDegs());
            maxLat = Math.max(maxLat, bottomLeftLoc.getLatitudeDegs());
            double minLat = Math.min(topLeftLoc.getLatitudeDegs(), topRightLoc.getLatitudeDegs());
            minLat = Math.min(minLat, bottomRightLoc.getLatitudeDegs());
            minLat = Math.min(minLat, bottomLeftLoc.getLatitudeDegs());
            double maxLon = Math.max(topLeftLoc.getLongitudeDegs(), topRightLoc.getLongitudeDegs());
            maxLon = Math.max(maxLon, bottomRightLoc.getLongitudeDegs());
            maxLon = Math.max(maxLon, bottomLeftLoc.getLongitudeDegs());
            double minLon = Math.min(topLeftLoc.getLongitudeDegs(), topRightLoc.getLongitudeDegs());
            minLon = Math.min(minLon, bottomRightLoc.getLongitudeDegs());
            minLon = Math.min(minLon, bottomLeftLoc.getLongitudeDegs());

            Image imageAlt = getAdjustedLatLonQuadedImage(img, topLeftLoc, topRightLoc, bottomRightLoc, bottomLeftLoc,
                    minLat, maxLat, minLon, maxLon);

            topLeftLoc = new LocationType(maxLat, minLon); 
            topRightLoc = new LocationType(maxLat, maxLon);
            bottomRightLoc = new LocationType(minLat, maxLon);
            bottomLeftLoc = new LocationType(minLat, minLon);
            
            img = imageAlt;

            imgFile = new File(ConfigFetch.getNeptusTmpDir(), FileUtil.getFileNameWithoutExtension(imgFile)
                    + "_alt" + ".png");
            try {
                ImageIO.write((RenderedImage) img, "png", imgFile);
            }
            catch (IOException e) {
                e.printStackTrace();
                return I18n.text("Error transforming image!");
            }
        }
        
        double meterDistanceH = topLeftLoc.getHorizontalDistanceInMeters(topRightLoc);
        double meterDistanceV = topLeftLoc.getHorizontalDistanceInMeters(bottomLeftLoc);
        
        LocationType centerLoc = topLeftLoc.getNewAbsoluteLatLonDepth();
        centerLoc.setOffsetSouth(meterDistanceV / 2);
        centerLoc.setOffsetEast(meterDistanceH / 2);
        centerLoc.convertToAbsoluteLatLonDepth();
        
        int imgWidth = img.getWidth(null);
        int imgHeight = img.getHeight(null);
        double scaleH = meterDistanceH / imgWidth;
        double scaleV = meterDistanceV / imgHeight;
        
        imgElement.setId(idByUser);
        
        imgElement.setCenterLocation(centerLoc);
        imgElement.setTransparency(transparency);
        imgElement.setYawDeg(rotationDeg);
        imgElement.setImage(img);
        imgElement.setImageFileName(imgFile.getAbsolutePath());
        imgElement.setImageScale(scaleH);
        if (Double.compare(scaleH, scaleV) != 0)
            imgElement.setImageScaleV(scaleV);
        
        mapType.addObject(imgElement);
        
        MissionType mission = getConsole().getMission();
        mission.save(false);
        
        return null;
    }

    /**
     * @param img
     * @param topLeftLoc
     * @param topRightLoc
     * @param bottomRightLoc
     * @param bottomLeftLoc
     * @param minLat
     * @param maxLat
     * @param minLon
     * @param maxLon
     * @return
     */
    private Image getAdjustedLatLonQuadedImage(Image img, LocationType topLeftLoc, LocationType topRightLoc,
            LocationType bottomRightLoc, LocationType bottomLeftLoc,
            double minLat, double maxLat, double minLon, double maxLon) {
        
        int w = img.getWidth(null);
        int h = img.getHeight(null);
        
        BufferedImage srcImg = new BufferedImage(img.getWidth(null), img.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics g = srcImg.createGraphics();
        g.drawImage(img, 0, 0, null);
        g.dispose(); 
        
        float uly = (float) ((maxLat - topLeftLoc.getLatitudeDegs()) / (maxLat - minLat));
        float ulx = (float) ((topLeftLoc.getLongitudeDegs() - minLon) / (maxLon - minLon));

        float ury = (float) ((maxLat - topRightLoc.getLatitudeDegs()) / (maxLat - minLat));
        float urx = (float) ((topRightLoc.getLongitudeDegs() - minLon) / (maxLon - minLon));

        float lry = (float) ((maxLat - bottomRightLoc.getLatitudeDegs()) / (maxLat - minLat));
        float lrx = (float) ((bottomRightLoc.getLongitudeDegs() - minLon) / (maxLon - minLon));

        float lly = (float) ((maxLat - bottomLeftLoc.getLatitudeDegs()) / (maxLat - minLat));
        float llx = (float) ((bottomLeftLoc.getLongitudeDegs() - minLon) / (maxLon - minLon));

        uly = Math.abs(h * uly);
        ulx = Math.abs(w * ulx);

        ury = Math.abs(h * ury);
        urx = Math.abs(w * urx);

        lry = Math.abs(h * lry);
        lrx = Math.abs(w * lrx);

        lly = Math.abs(h * lly);
        llx = Math.abs(w * llx);

        SkewImage skew = new SkewImage(srcImg);
        BufferedImage outImg = skew.setCorners(ulx, uly, // UL
                urx, ury,  // UR
                lrx, lry,  // LR
                llx, lly); // LL
        
        return outImg;
    }

    /**
     * @param urlKml
     * @param fHref
     * @return
     */
    private File getImageFileFromKml(URL urlKml, String fHref) {
        try {
            // Try if path is absolute
            File refFx = new File(fHref);
            if (refFx.exists()) {
                return refFx;
            }
            else {
                try {
                    // Try if path is URL
                    URL refUrl = new URL(fHref);
                    String fxName = new File(refUrl.getPath()).getName();
                    URLConnection conn = refUrl.openConnection();
                    InputStream reader = conn.getInputStream();
                    File outFx = new File(new File(ConfigFetch.getNeptusTmpDir()), fxName);
                    outFx.getParentFile().mkdirs();
                    outFx.createNewFile();
                    StreamUtil.copyStreamToFile(reader, outFx);
                    if (outFx.exists())
                        return outFx;
                }
                catch (MalformedURLException  e) {
                    NeptusLog.pub().warn(e);
                }
            }
            
            switch (urlKml.getProtocol()) {
                case "file":
                    File kmlFx = new File(urlKml.toURI());
                    String extFx = FileUtil.getFileExtension(kmlFx);
                    if ("kml".equalsIgnoreCase(extFx)) {
                        // At this stage the path must be relative
                        File outFx = new File(kmlFx, fHref);
                        if (outFx.exists())
                            return outFx;
                    }
                    else if ("kmz".equalsIgnoreCase(extFx)) {
                        ZipInputStream zip = new ZipInputStream(urlKml.openStream());
                        ZipEntry entry;
                        while ((entry = zip.getNextEntry()) != null) {
                            String nm = entry.getName();
                            if (fHref.equals(nm)) {
                                File destinationDir = new File(ConfigFetch.getNeptusTmpDir());
                                File outFx = new File(destinationDir, nm);
                                if (!outFx.toPath().normalize().startsWith(destinationDir.toPath()))
                                    throw new Exception("Bad zip entry");
                                outFx.getParentFile().mkdirs();
                                outFx.createNewFile();
                                outFx.deleteOnExit();
                                StreamUtil.copyStreamToFile(zip, outFx);
                                if (outFx.exists())
                                    return outFx;
                            }
                        }
                    }
                    break;
                case "http":
                case "https":
                    break;
                default:
                    return null;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressWarnings("unused")
    private void addPathElement(double rotationDegs, LocationType... locs) {
        if (locs.length < 3)
            return;
        
        MapType mapType = getMapToAddElements();

        LocationType firstLoc = locs[0];
        PathElement pathElem = new PathElement(mapType.getMapGroup(), mapType, firstLoc);
        pathElem.addPoint(0, 0, 0, false);

        /* add points to the path */
        for(int i = 1; i < locs.length; i++) {
            LocationType elemLoc = locs[i];
            double[] offsets = elemLoc.getOffsetFrom(firstLoc);
            pathElem.addPoint(offsets[1], offsets[0], offsets[2], false);
        }    
        
        pathElem.setFilled(true);
        pathElem.setShape(true);
        
        pathElem.setYawDeg(rotationDegs);
        
        mapType.addObject(pathElem);
    }
//    private void calculate(double nLat, double sLat, double eLon, double wLon, ImageElement img) {
//        double pixelDistance = mark1.getCenterLocation().getHorizontalDistanceInMeters(mark2.getCenterLocation());
//        double meterDistance = lt1.getHorizontalDistanceInMeters(lt2);      
//        double scale = meterDistance / pixelDistance;
//        
//        double[] pixelOffsets = (new LocationType()).getOffsetFrom(mark1.getCenterLocation());
//        
//        LocationType finalLoc = new LocationType(lt1);
//        finalLoc.translatePosition(pixelOffsets[0]*scale, pixelOffsets[1]*scale, pixelOffsets[2]*scale);        
//        
//        imgObject.setCenterLocation(finalLoc);
//        imgObject.setImageScale(scale);
//    }

    private MapType getMapToAddElements() {
        return MapGroup.getMapGroupInstance(getConsole().getMission()).getMaps()[0];
    }

    private void cleanListing() {
        int nElements = listModel.getSize();
        if(nElements != 0)           
            listModel.removeAllElements();
    }
    
    private void showErrorMessage(String msg) {
        GuiUtils.errorMessage(this, this.getName(), msg);
    }

    private class CustomListCellRenderer implements ListCellRenderer<JLabel> {
        @Override
        public Component getListCellRendererComponent(JList<? extends JLabel> list, JLabel value, int index,
                boolean isSelected, boolean cellHasFocus) {
            value.setOpaque(true);
            if (isSelected) {
                value.setBackground(list.getSelectionBackground());
                value.setForeground(list.getSelectionForeground());
            }
            else {
                if(!addedFeatures.contains(value.getText()))
                    value.setBackground(list.getBackground());
                else
                    value.setBackground(COLOR_SELECTED);
                
                value.setForeground(list.getForeground());
            }        
            return value;
        }
    }
    
    @Override
    public void cleanSubPanel() {
    }

    @Override
    public void initSubPanel() {
    }
}
