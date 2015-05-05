/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * https://www.lsts.pt/neptus/licence.
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

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.map.PathElement;
import pt.lsts.neptus.util.ImageUtils;

import de.micromata.opengis.kml.v_2_2_0.Coordinate;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.LineString;
import de.micromata.opengis.kml.v_2_2_0.Point;
import de.micromata.opengis.kml.v_2_2_0.Polygon;

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
    private static final int WIDTH = 230;
    private static final int HEIGHT = 500;

    private JMenuBar menuBar;
    private JMenu openMenu;
    private JMenuItem kmlFile; /* load kml features from a file */
    private JMenuItem kmlUrl; /* load kml features from a URL */
    
    private String kmlFeatUrl; /* tmp store Url given by the user */

    private JPopupMenu rightClickPopup;
    private JMenuItem rightClickAddItem;


    private JList<JLabel> listingPanel; /* actual listing of kml features */
    private final DefaultListModel<JLabel> listModel = new DefaultListModel<>();
    private JFileChooser fileChooser;

    private TreeMap<String, Placemark> kmlFeatures;
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
        openMenu = new JMenu("Open");
        kmlFile = new JMenuItem("Open from file");
        kmlUrl = new JMenuItem("Open from Url");
        kmlFeatUrl = "";

        openMenu.add(kmlFile);
        openMenu.add(kmlUrl);
        menuBar.add(openMenu);

        add(menuBar, BorderLayout.NORTH);
        addMenuListeners();

        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));

        rightClickPopup = new JPopupMenu();
        rightClickAddItem = new JMenuItem("Add to map");
        rightClickPopup.add(rightClickAddItem);

        rightClickAddItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedFeatureIndex = listingPanel.getSelectedIndex();
                String featName = ((JLabel) listModel.getElementAt(selectedFeatureIndex)).getText();
                String idByUser = JOptionPane.showInputDialog("Element Id");
                
                if(idByUser != null && !idByUser.equals(""))
                  addFeatureToMap(featName, idByUser);
            }
        });
    }

    private void initListingPanel() {
        listingPanel = new JList<>(listModel);
        listingPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));
        add(listingPanel);

        listingPanel.setCellRenderer(new CustomListCellRenderer());

        listingPanel.addMouseListener(new MouseAdapter() {
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
                    rightClickPopup.show(listingPanel, me.getX(), me.getY());
                }
            }
        });
    }

    private void listKmlFeatures(URL url) {
        cleanListing();

        KmlReader kml = new KmlReader(url, true);
        kmlFeatures = kml.extractFeatures();

        for(String fname : kmlFeatures.keySet()) {
            String fgeom = kmlFeatures.get(fname).getGeometry().getClass().getSimpleName();
            listModel.addElement(getFeatureLabel(fname, fgeom));
        }
    }
    
    private JLabel getFeatureLabel(String fname, String fgeom) {
        JLabel feature = new JLabel(fname);
        String iconUrl = "";
        
        if(fgeom.equals("Point"))
            iconUrl = "pt/lsts/neptus/console/plugins/kml/icons/point.png";
        else if(fgeom.equals("LineString"))
            iconUrl = "pt/lsts/neptus/console/plugins/kml/icons/lnstr.png";
        else if(fgeom.equals("Point"))
            iconUrl = "pt/lsts/neptus/console/plugins/kml/icons/polyg.png";
        
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
                    System.out.println("Selected file: " + selectedFile.getAbsolutePath());                 
                    try {
                        URL fileUrl = new URL(selectedFile.getAbsolutePath().toString());
                        listKmlFeatures(fileUrl);
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
                String urlStr = JOptionPane.showInputDialog("Enter a URL", kmlFeatUrl);
                if(urlStr != null && !urlStr.equals("")) {
                    System.out.println("URL: " + urlStr);

                    try {
                        kmlFeatUrl = urlStr;
                        listKmlFeatures(new URL(urlStr));
                    }
                    catch(MalformedURLException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
    }

    private void addFeatureToMap(String featName, String idByUser) {
        Placemark feature = kmlFeatures.get(featName);
        String featGeom = feature.getGeometry().getClass().getSimpleName();
        
        if(featGeom.equals("Point"))
            addPoint((Point)((Placemark) feature).getGeometry(), idByUser);
        
        else if(featGeom.equals("LineString"))
            addPathElement(feature, idByUser, false);
        
        else if(featGeom.equals("Polygon"))
            addPathElement(feature, idByUser, true);

        
        addedFeatures.add(featName);
    }
    
    private void addPoint(Point point, String idByUser) {
        Coordinate coords = point.getCoordinates().get(0);
        
        MapType mapType = MapGroup.getMapGroupInstance(getConsole().getMission()).getMaps()[0];
        MarkElement kmlPoint = new MarkElement(mapType.getMapGroup(), mapType);
        LocationType kmlPointLoc = new LocationType(coords.getLatitude(), coords.getLongitude());

        kmlPoint.setId(idByUser);
        kmlPoint.setCenterLocation(kmlPointLoc);
        mapType.addObject(kmlPoint);
    }
    
    private void addPathElement(Placemark feature,String idByUser, boolean isFilled) {
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
        pathElem.setFill(isFilled);
        pathElem.setShape(isFilled);
        
        mapType.addObject(pathElem);
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
    public void cleanSubPanel() {}

    @Override
    public void initSubPanel() {}
}
