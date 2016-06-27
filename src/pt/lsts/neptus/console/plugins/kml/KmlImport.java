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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

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

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.maneuvers.Goto;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
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

    private TreeMap<String, Placemark> kmlFeatures; /* init in listKmlFeatures()*/
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
            String fgeom = kmlFeatures.get(fname).getGeometry().getClass().getSimpleName();
            featuresGeom.put(fname, fgeom);
            listModel.addElement(getFeatureLabel(fname, fgeom));
        }
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
        Placemark feature = kmlFeatures.get(featName);
        String featGeom = featuresGeom.get(featName);
        
        if(featGeom.equals("Point"))
            addAsPoint((Point)((Placemark) feature).getGeometry(), idByUser);
        
        else if(featGeom.equals("LineString"))
            if(!addAsPlan)
                addAsPathElement(feature, idByUser, false);
            else
                addLineStringAsPlan(feature, idByUser);
        
        else if(featGeom.equals("Polygon"))
            addAsPathElement(feature, idByUser, true);

        
        addedFeatures.add(featName);
    }
    
    private void addAsPoint(Point point, String idByUser) {
        Coordinate coords = point.getCoordinates().get(0);
        
        MapType mapType = MapGroup.getMapGroupInstance(getConsole().getMission()).getMaps()[0];
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
        MapType mapType = MapGroup.getMapGroupInstance(getConsole().getMission()).getMaps()[0];
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
            maneuver.setSpeedUnits("m/s");
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
