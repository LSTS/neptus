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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Manuel Ribeiro
 * Feb 11, 2015
 */

package pt.lsts.neptus.mra.markermanagement;

import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.util.Dom4JUtil;

/**
 * @author Manuel R.
 *
 */
public class MarkerManagement {

    private JFrame frmMarkerManagement;
    private JTable table;
    private MarkerEdit markerEditFrame;
    protected MRAPanel mraPanel;
    private final ArrayList<LogMarker> logMarkers = new ArrayList<>();
    private String markerFilePath;
    private List<LogMarkerItem> markerList = new ArrayList<>();

    public MarkerManagement(NeptusMRA mra, MRAPanel mraPanel) {
        this.mraPanel = mraPanel;
        initialize();
    }

    private void initialize() {

        frmMarkerManagement = new JFrame();
        frmMarkerManagement.setIconImage(Toolkit.getDefaultToolkit().getImage(MarkerManagement.class.getResource("/images/menus/marker.png")));
        frmMarkerManagement.setTitle("Marker Management");
        frmMarkerManagement.setBounds(100, 100, 687, 426);
        frmMarkerManagement.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frmMarkerManagement.getContentPane().setLayout(new MigLayout("", "[grow]", "[grow]"));
        frmMarkerManagement.setVisible(true);
        frmMarkerManagement.setResizable(false);

        markerEditFrame = new MarkerEdit(this);
        markerEditFrame.setLocation(frmMarkerManagement.getLocation().x + frmMarkerManagement.getSize().width, frmMarkerManagement.getLocation().y);
        markerEditFrame.setSize(470, 540);
        
        logMarkers.addAll(mraPanel.getMarkers());

        JPanel panel = new JPanel();
        frmMarkerManagement.getContentPane().add(panel, "cell 0 0,grow");
        panel.setLayout(new MigLayout("", "[][][grow]", "[][][grow]"));

        //check if markers file exists and creates new file if not
        markersSetup();

        TableModel tableModel = new LogMarkerItemModel(markerList);
        table = new JTable(tableModel);

        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();


        int columnIndexToSort = 1;
        sortKeys.add(new RowSorter.SortKey(columnIndexToSort, SortOrder.DESCENDING));

        sorter.setSortKeys(sortKeys);
        sorter.sort();

       /* table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent event) {
                // do some actions here, for example
                // print first column value from selected row
                if (!event.getValueIsAdjusting()) {
                    openMarkerEditor();
                    System.out.println(table.getValueAt(table.getSelectedRow(), 0).toString());
                }
            }
        });
        */
        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table =(JTable) me.getSource();
                if (me.getClickCount() == 2) {
                    openMarkerEditor(table.getValueAt(table.getSelectedRow(), 1).toString());
                    //System.out.println(table.getValueAt(table.getSelectedRow(), 1).toString());
                }
            }
        });

        JButton button = new JButton("Print Markers");
        panel.add(button, "cell 0 0");

        JScrollPane scrollPane = new JScrollPane(table);

        panel.add(scrollPane, "cell 0 2 3 1,grow");

    }

    private void openMarkerEditor(String label) {

        LogMarkerItem selected = findMarker(label);
        if (selected == null) {
            System.out.println("DEBUG : cannot find selected marker on markers list!"); //FIXME DEBUG
            return;
        }
        markerEditFrame.loadMarker(selected);
        markerEditFrame.setVisible(true);

    }

    private LogMarkerItem findMarker(String label) {
        for (LogMarkerItem marker : markerList) {
            if (marker.getLabel().equals(label))
                return marker;
        }
        return null;
    }

    private void markersSetup() {

        markerFilePath = mraPanel.getSource().getFile("Data.lsf").getParent() + "/marks.xml";

        //XML markers file doesnt exist and there are Markers 
        if (!new File(markerFilePath).exists() && !logMarkers.isEmpty()) {
            NeptusLog.pub().info("Creating markers... ");
            createMarkers();
        }
        else {
            //XML markers file exists, load markers from it
            NeptusLog.pub().info("Loading markers... ");
            if(!loadMarkers()) {
                NeptusLog.pub().error("Corrupted markers file. Trying to create new markers file.");
                createMarkers();
            }
        }
    }

    private void createMarkers() {

        //XML document structure
        org.w3c.dom.Document xml = Dom4JUtil.createEmptyDOMDocument();
        Element rootElement = xml.createElement("markers");
        xml.appendChild(rootElement);

        int i=1;
        for(LogMarker l : mraPanel.getMarkers()) {
            LogMarkerItem marker = new LogMarkerItem(i, l.getLabel(), l.getTimestamp(), l.getLat(), l.getLon(), null, 0, "", 0, Classification.UNDEFINED);

            //add new LogMarkerItem to list
            markerList.add(marker);

            // XML related
            Element mark = xml.createElement("Mark");
            rootElement.appendChild(mark);

            //set Index
            Attr attr = xml.createAttribute("id");
            attr.setValue(Integer.toString(i));
            mark.setAttributeNode(attr);

            Element label = xml.createElement("Label");
            label.appendChild(xml.createTextNode(l.getLabel()));
            mark.appendChild(label);

            Element ts = xml.createElement("Timestamp");
            ts.appendChild(xml.createTextNode(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(l.getDate())));
            mark.appendChild(ts);

            Element lat = xml.createElement("Lat");
            lat.appendChild(xml.createTextNode(l.getLat() + ""));
            mark.appendChild(lat);

            Element lon = xml.createElement("Lon");
            lon.appendChild(xml.createTextNode(l.getLon() + ""));
            mark.appendChild(lon);

            Element image = xml.createElement("Image");
            image.appendChild(xml.createTextNode(""));
            mark.appendChild(image);

            Element draw = xml.createElement("Draw");
            draw.appendChild(xml.createTextNode(""));
            mark.appendChild(draw);

            Element range = xml.createElement("Range");
            range.appendChild(xml.createTextNode(""));
            mark.appendChild(range);

            Element depth = xml.createElement("Depth");
            depth.appendChild(xml.createTextNode(""));
            mark.appendChild(depth);

            Element classif = xml.createElement("Classification");
            classif.appendChild(xml.createTextNode(""));
            mark.appendChild(classif);

            Element annot = xml.createElement("Annotation");
            annot.appendChild(xml.createTextNode(""));
            mark.appendChild(annot);
            // end XML related
            i++;
        }

        // write the content into xml file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(xml);
            StreamResult result = new StreamResult(new File(markerFilePath));
            transformer.transform(source, result);
        }
        catch (TransformerException e) {
            e.printStackTrace();
        }

        NeptusLog.pub().info("Markers XML file saved - " + markerFilePath);

    }

    private boolean loadMarkers() {

        return false;
    }

}