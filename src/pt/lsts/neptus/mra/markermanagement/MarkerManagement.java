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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import net.miginfocom.swing.MigLayout;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.mra.SidescanLogMarker;
import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.Dom4JUtil;
/**
 * @author Manuel R.
 *
 */
public class MarkerManagement {

    private static final boolean DEBUG = true;
    
    private final int DEFAULT_COLUMN_TO_SORT = 0;
    private JFrame frmMarkerManagement;
    private JTable table;
    private LogMarkerItemModel tableModel;
    private MarkerEdit markerEditFrame;
    protected MRAPanel mraPanel;
    private final ArrayList<LogMarker> logMarkers = new ArrayList<>();
    private String markerFilePath;
    private List<LogMarkerItem> markerList = new ArrayList<>();

    private Document dom;


    /**
     * @wbp.parser.entryPoint
     */
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
        
        //set markerEdit frame location next to this window
        markerEditFrame.setLocation(frmMarkerManagement.getLocation().x + frmMarkerManagement.getSize().width, frmMarkerManagement.getLocation().y);

        //Add existing LogMarkers (only SidescanLogMarker ones)
        for (LogMarker m : mraPanel.getMarkers()) {
            if (m.getClass() == SidescanLogMarker.class) {
               logMarkers.add(m);
            }
        }
        //logMarkers.addAll(mraPanel.getMarkers());

        JPanel panel = new JPanel();
        frmMarkerManagement.getContentPane().add(panel, "cell 0 0,grow");
        panel.setLayout(new MigLayout("", "[][][grow]", "[][][grow]"));

        //check if markers file exists and creates new file if not
        markersSetup();

        tableModel = new LogMarkerItemModel(markerList);
        table = new JTable(tableModel);

        //define max columns width
        setColumnsWidth();
        
        setCenteredColumns();

        //define default column to sort when creating table
        setTableSorter(DEFAULT_COLUMN_TO_SORT);

        table.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table =(JTable) me.getSource();
                int rowIndex = table.getSelectedRow();
                if (me.getClickCount() == 2) {

                    openMarkerEditor(table.getValueAt(table.getSelectedRow(), 1).toString(), rowIndex);
                    //System.out.println(table.getValueAt(table.getSelectedRow(), 1).toString());
                }
            }
        });

        JButton button = new JButton("Print Markers");
        panel.add(button, "cell 0 0");

        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //TODO PRINT MARKERS
            }
        });
        JScrollPane scrollPane = new JScrollPane(table);

        panel.add(scrollPane, "cell 0 2 3 1,grow");

    }
    
    private void setTableSorter(int columnIndexToSort) {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(table.getModel());
        table.setRowSorter(sorter);
        List<RowSorter.SortKey> sortKeys = new ArrayList<>();

        sortKeys.add(new RowSorter.SortKey(columnIndexToSort, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);
        sorter.sort();
    }
    
    private void setCenteredColumns() {
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment( JLabel.CENTER );
        
        DefaultTableCellRenderer centerRenderer2 = new DefaultTableCellRenderer();
        centerRenderer2.setHorizontalAlignment( JLabel.LEFT );
        
        table.setDefaultRenderer(String.class, centerRenderer2);
        table.setDefaultRenderer(Classification.class, centerRenderer);
        table.setDefaultRenderer(Integer.class, centerRenderer);
        table.setDefaultRenderer(Double.class, centerRenderer);
        table.setDefaultRenderer(LocationType.class, centerRenderer);
    }
    
    private void setColumnsWidth() {
        // column 0 - width
        table.getColumnModel().getColumn(0).setMinWidth(25);
        table.getColumnModel().getColumn(0).setMaxWidth(25);
        table.getColumnModel().getColumn(0).setPreferredWidth(25);

        // column 2 - width
        table.getColumnModel().getColumn(2).setMaxWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(115);
        
        // column 3 - width
        table.getColumnModel().getColumn(3).setMaxWidth(170);
        table.getColumnModel().getColumn(3).setPreferredWidth(145);
        
        // column 4 - width
        table.getColumnModel().getColumn(4).setMaxWidth(75);
        table.getColumnModel().getColumn(4).setPreferredWidth(70);
        
        // column 5 - width
        table.getColumnModel().getColumn(5).setMaxWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(105);
    }

    private void openMarkerEditor(String label, int rowIndex) {

        LogMarkerItem selected = findMarker(label);
        if (selected == null) {
            if (DEBUG)
                System.out.println("Cannot find selected marker on markers list!");
            return;
        }
        markerEditFrame.loadMarker(selected, rowIndex);
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
        Document xml = Dom4JUtil.createEmptyDOMDocument();
        Element rootElement = xml.createElement("markers");
        xml.appendChild(rootElement);

        int i=1;
        for(LogMarker l : mraPanel.getMarkers()) {

           // BufferedImage bufImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
            //TODO build sidescanimage 
            LocationType loc = l.getLocation();

            LogMarkerItem marker = new LogMarkerItem(i, l.getLabel(), l.getTimestamp(), loc.getLatitudeDegs(), loc.getLongitudeDegs(), null, "<Your annotation here.>", 0, Classification.UNDEFINED);

            
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
            ts.appendChild(xml.createTextNode(l.getTimestamp() + ""));
            mark.appendChild(ts);

            Element lat = xml.createElement("Lat");
            lat.appendChild(xml.createTextNode(loc.getLatitudeDegs() + ""));
            mark.appendChild(lat);

            Element lon = xml.createElement("Lon");
            lon.appendChild(xml.createTextNode(loc.getLongitudeDegs() + ""));
            mark.appendChild(lon);

            Element image = xml.createElement("Image");
            image.appendChild(xml.createTextNode(""));
            //image.setTextContent(encodedImage); // store it inside node
            mark.appendChild(image);

            Element draw = xml.createElement("Draw");
            draw.appendChild(xml.createTextNode(""));
            mark.appendChild(draw);

            Element altitude = xml.createElement("Altitude");
            altitude.appendChild(xml.createTextNode(marker.getAltitude() + ""));
            mark.appendChild(altitude);

            Element classif = xml.createElement("Classification");
            classif.appendChild(xml.createTextNode(Classification.UNDEFINED.toString()));
            mark.appendChild(classif);

            Element annot = xml.createElement("Annotation");
            annot.appendChild(xml.createTextNode(marker.getAnnotation()));
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
        parseXmlFile();
        parseDocument();
        if (DEBUG)
            printData();


        return !markerList.isEmpty();
    }

    public void deleteLog(LogMarkerItem selectedMarker, int row) {
        markerList.remove(selectedMarker);
        tableModel.removeRow(row);

        if (DEBUG)
            System.out.println("after deleted: " + markerList.size());
        
        //TODO: save XML

    }

    public void updateTableRow(LogMarkerItem selectedMarker, int row) {
        LogMarkerItem marker = findMarker(selectedMarker.getLabel());
        marker.copy(selectedMarker);
        
        if (DEBUG)
            System.out.println("udpating row "+ row);
        
        tableModel.updateRow(row);
    }

    private void parseXmlFile(){
        //get the factory
        DocumentBuilderFactory docBuildfactory = DocumentBuilderFactory.newInstance();

        try {
            //Using factory get an instance of document builder
            DocumentBuilder docBuilder = docBuildfactory.newDocumentBuilder();

            //parse using builder to get DOM representation of the XML file
            dom = docBuilder.parse(markerFilePath);

        }catch(ParserConfigurationException pce) {
            pce.printStackTrace();
        }catch(SAXException se) {
            se.printStackTrace();
        }catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void parseDocument(){
        Element docEle = dom.getDocumentElement();
        markerList.clear();

        //get nodelist of <LogMarkerItem> elements
        NodeList nl = docEle.getElementsByTagName("Mark");

        if(nl != null && nl.getLength() > 0) {
            for(int i = 0 ; i < nl.getLength();i++) {

                //get the marker element
                Element el = (Element)nl.item(i);

                //get the marker object
                LogMarkerItem e = getLogMarkerItem(el);

                //add it to list
                markerList.add(e);
            }
        }
    }

    private LogMarkerItem getLogMarkerItem(Element markerEl) {
        int index = getAttIntValue(markerEl, "id");
        String name = getTextValue(markerEl,"Label");

        double ts = getDoubleValue(markerEl,"Timestamp");
        double lon = getDoubleValue(markerEl,"Lon");
        double lat = getDoubleValue(markerEl,"Lat");
        // BufferedImage image = 
        // BufferedImage draw = 
        double altitude = -1.0;
        Classification cls = Classification.UNDEFINED;
        String annot = "";
        try  {
            altitude = getDoubleValue(markerEl,"Altitude");
            cls = Classification.valueOf(getTextValue(markerEl,"Classification"));
            annot = getTextValue(markerEl, "Annotation");
        }
        catch (NullPointerException e) {
            if (DEBUG)
                System.out.println("null pointer");
        }

        //Create new LogMarkerItem with the value read from xml
        LogMarkerItem e = new LogMarkerItem(index, name, ts, lat, lon, null, annot, altitude, cls);

        return e;
    }

    private String getTextValue(Element ele, String tagName) {
        String textVal = null;
        NodeList nl = ele.getElementsByTagName(tagName);
        if(nl != null && nl.getLength() > 0) {
            Element el = (Element)nl.item(0);
            textVal = el.getFirstChild().getNodeValue();
        }

        return textVal;
    }

    @SuppressWarnings("unused")
    private int getIntValue(Element ele, String tagName) throws NullPointerException {
        return Integer.parseInt(getTextValue(ele,tagName));
    }

    private int getAttIntValue(Element ele, String tagName) throws NullPointerException {
        return Integer.parseInt(ele.getAttribute( tagName));
    }

    private double getDoubleValue(Element ele, String tagName) throws NullPointerException{
        return Double.parseDouble(getTextValue(ele,tagName));
    }


    private void printData(){

        System.out.println("No of LogMarkerItems '" + markerList.size() + "'.");

        Iterator<LogMarkerItem> it = markerList.iterator();
        while(it.hasNext()) {
            System.out.println(it.next().toString());
        }
    }

    public static void main(String[] args) {


    }
}