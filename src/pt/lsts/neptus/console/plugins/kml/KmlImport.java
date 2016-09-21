/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
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
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;

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
import pt.lsts.neptus.mp.Maneuver;
import pt.lsts.neptus.mp.ManeuverLocation;
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
import pt.lsts.neptus.util.StreamUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author tsmarques
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Kml Import", description = "Import map features from KML, from a file or URL", author = "tsmarques", version = "0.1")
@Popup(name = "Kml Import", pos = POSITION.CENTER, width = 230, height = 500)
@LayerPriority(priority = 50)
public class KmlImport extends ConsolePanel {
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

        rightClickAddItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedFeatureIndex = listingPanel.getSelectedIndex();
                String featName = ((JLabel) listModel.getElementAt(selectedFeatureIndex)).getText();
                String idByUser = JOptionPane.showInputDialog(I18n.text("Element ID"), featName);
                
                if(idByUser != null)
                  addFeatureToMap(featName, idByUser, false);
            }
        });
        
        rightClickAddAsPlan = new JMenuItem(I18n.text("Add as plan"));
        rightClickAddAsPlan.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedFeatureIndex = listingPanel.getSelectedIndex();
                String featName = ((JLabel) listModel.getElementAt(selectedFeatureIndex)).getText();
                String idByUser = JOptionPane.showInputDialog(I18n.text("Plan ID"), featName);
                
                if(idByUser != null)
                  addFeatureToMap(featName, idByUser, true);
            }
        });
    }

    private void initListingPanel() {
        listingPanel = new JList<>(listModel);
        listingPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(listingPanel);

        listingPanel.setCellRenderer(new CustomListCellRenderer());

        listingPanel.addMouseListener(new MouseAdapter() {
            private void addPopUpItems() {
                rightClickPopup.removeAll();
                rightClickPopup.add(rightClickAddItem);
                
                int selectedFeatureIndex = listingPanel.getSelectedIndex();
                String featName = ((JLabel) listModel.getElementAt(selectedFeatureIndex)).getText();
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
        if (f instanceof Placemark)
            return ((Placemark) f).getGeometry().getClass().getSimpleName();

        return f.getClass().getSimpleName();
    }
    
    private JLabel getFeatureLabel(String fname, String fgeom) {
        JLabel feature = new JLabel(fname);
        String iconUrl = "";
        
        if(fgeom.equals("Point"))
            iconUrl = "images/mark.png";
        else if(fgeom.equals("LineString"))
            iconUrl = "pt/lsts/neptus/plugins/map/interactions/draw-line.png";
        else if(fgeom.equals("Polygon"))
            iconUrl = "pt/lsts/neptus/plugins/map/interactions/poly.png";
        else if (fgeom.equals("GroundOverlay"))
            iconUrl = "images/buttons/new_image.png";
        
        feature.setName(fname);
        feature.setIcon(ImageUtils.getScaledIcon(iconUrl, 15, 15));        
        return feature;
    }

    private void addMenuListeners() {
        kmlFile.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
            }
        });

        kmlUrl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {               
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
            }
        });
    }

    private void addFeatureToMap(String featName, String idByUser, boolean addAsPlan) {
        Feature f = kmlFeatures.get(featName);
        String featGeom = featuresGeom.get(featName);
        
        if (f instanceof Placemark) {
            Placemark feature = (Placemark) f;
            
            if(featGeom.equals("Point")) {
                addAsPoint((Point) feature.getGeometry(), idByUser);
            }
            else if(featGeom.equals("LineString")) {
                if(!addAsPlan)
                    addAsPathElement(feature, idByUser, false);
                else
                    addLineStringAsPlan(feature, idByUser);
            }
            else if(featGeom.equals("Polygon")) {
                addAsPathElement(feature, idByUser, true);
            }
        }
        else if (f instanceof GroundOverlay) {
            addAsImage((GroundOverlay) f, idByUser);
        }
        
        addedFeatures.add(featName);
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
            
            double offsets[] = elemLoc.getOffsetFrom(firstLoc);
            pathElem.addPoint(offsets[1], offsets[0], offsets[2], false);
        }    
        
        pathElem.setId(idByUser);
        pathElem.setFilled(isFilled);
        pathElem.setShape(isFilled);
        
        mapType.addObject(pathElem);
        
        MissionType mission = getConsole().getMission();
        mission.save(false);
    }
    
    private void addLineStringAsPlan(Placemark lineString, String idByUser) {
        MissionType mission = getConsole().getMission();
        
        String mainVehicle = getConsole().getMainSystem();
        PlanType plan = new PlanType(mission);
        
        plan.setId(idByUser);
        plan.setVehicle(mainVehicle);
        
        List<Coordinate> coords = getPathCoordinates(lineString, false);
        int nManeuver = 0;
        for (Coordinate coord : coords) {
            nManeuver++;
            
            Goto maneuver = new Goto();
            maneuver.setId("point " + nManeuver);
            LocationType loc = new LocationType(coord.getLatitude(), coord.getLongitude());
            ManeuverLocation mloc = new ManeuverLocation(loc);
            mloc.setZUnits(ManeuverLocation.Z_UNITS.DEPTH);
            mloc.setZ(0);
            
            maneuver.setSpeed(1.3);
            maneuver.setSpeedUnits(Maneuver.SPEED_UNITS.METERS_PS);
            maneuver.setManeuverLocation(mloc);
            plan.getGraph().addManeuverAtEnd(maneuver);
        }
        mission.addPlan(plan);
        mission.save(false);
        getConsole().warnMissionListeners();
    }
    
    private List<Coordinate> getPathCoordinates(Placemark feature, boolean featureIsPolygon) {
        List<Coordinate> coords;
        
        /* get coordinates of the LineStrings forming the polygon boundary */
        if(featureIsPolygon) {
            Polygon polyg = (Polygon)((Placemark) feature).getGeometry();
            coords = polyg.getOuterBoundaryIs().getLinearRing().getCoordinates();
        }
        else
            coords = ((LineString)((Placemark) feature).getGeometry()).getCoordinates();
        
        return coords;
    }
    
    /**
     * @param feature
     * @param idByUser
     */
    private void addAsImage(GroundOverlay feature, String idByUser) {
        MapType mapType = getMapToAddElements();
//        mapType.asDocument();
        
        String fHref = feature.getIcon().getHref();
        
        ImageElement imgElement = new ImageElement(mapType.getMapGroup(), mapType);
        
        boolean fVisible = feature.isVisibility() == null ? true : feature.isVisibility();
        int transparency = 100; // 0 to 100
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
        if (!fVisible)
            transparency = 0;
        
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
                return;
        }
        
        // For now only LatLonBox is supported!!!
        if (latLonBox == null)
            return;
        
        // Not supported by ImageElement
        double rotationDeg = latLonBox == null ? 0 : latLonBox.getRotation(); // CounterClockWise
        rotationDeg = AngleUtils.nomalizeAngleDegrees360(360 - rotationDeg); // Make it ClockWise
        
        addPathElement(rotationDeg, topLeftLoc, topRightLoc, bottomRightLoc, bottomLeftLoc);
        
        double meterDistanceH = topLeftLoc.getHorizontalDistanceInMeters(topRightLoc);
        double meterDistanceV = topLeftLoc.getHorizontalDistanceInMeters(bottomLeftLoc);
        
        LocationType centerLoc = topLeftLoc.getNewAbsoluteLatLonDepth();
        centerLoc.setOffsetSouth(meterDistanceV / 2);
        centerLoc.setOffsetEast(meterDistanceH / 2);
        centerLoc.convertToAbsoluteLatLonDepth();
        
        // Getting the image to local storage
        
        URL urlKml;
        try {
            urlKml = new URL(kmlFeatUrl);
        }
        catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
        File imgFile = getImageFileFromKml(urlKml, fHref);
        if (imgFile == null || !imgFile.exists())
            return;
        
        Image img = ImageUtils.getImage(imgFile.getAbsolutePath());
        
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
        imgElement.setImageScaleV(scaleV);
        
        mapType.addObject(imgElement);
        
        MissionType mission = getConsole().getMission();
        mission.save(false);
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
                            if (fHref.equals(entry.getName())) {
                                File outFx = new File(new File(ConfigFetch.getNeptusTmpDir()), nm);
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
            double offsets[] = elemLoc.getOffsetFrom(firstLoc);
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
