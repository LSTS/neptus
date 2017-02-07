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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.JXBusyLabel;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.plugins.propertiesproviders.SidescanConfig;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.mra.SidescanLogMarker;
import pt.lsts.neptus.mra.api.CorrectedPosition;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.Dom4JUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.gui.Java2sAutoTextField;
import pt.lsts.neptus.util.llf.LsfReport;
import pt.lsts.neptus.util.llf.LsfReportProperties;
/**
 * @author Manuel R.
 * TODO: Add right-click "Show marker on map"
 */
@SuppressWarnings("serial")
public class MarkerManagement extends JDialog {

    private static final int DEFAULT_COLUMN_TO_SORT = 0;
    private static final int WIDTH = 690;
    private static final int HEIGHT = 430;
    private static final String SHOW_ICON = "images/buttons/show.png";
    private static final String ANY_TXT = I18n.text("<ANY>");
    private final ArrayList<SidescanLogMarker> logMarkers = new ArrayList<>();
    protected MRAPanel mraPanel;
    private static InfiniteProgressPanel loader = InfiniteProgressPanel.createInfinitePanelBeans("");
    private FinderDialog find = null;
    private JPanel panel;
    private JTable table;
    private LogMarkerItemModel tableModel;
    private MarkerEdit markerEditFrame;
    private List<LogMarkerItem> markerList = new ArrayList<>();
    private String markerFilePath;
    private Document dom;

    public MarkerManagement(NeptusMRA mra, MRAPanel mraPanel) {
        this.mraPanel = mraPanel;
        initialize();

        setLocationRelativeTo(null);
        setModalityType(ModalityType.MODELESS);
    }

    private double depth(long timestamp) {
        IMraLogGroup source = mraPanel.getSource();
        CorrectedPosition pos = new CorrectedPosition(source);
        return pos.getPosition(timestamp / 1000.0).toEstimatedState().getDepth();
    }
    private void initialize() {

        if (mraPanel.getMarkers().isEmpty()) {
            GuiUtils.infoMessage(mraPanel, "MarkerManagement", "No markers to show!");
            deleteMarkersFiles();
            return;
        }

        // frmMarkerManagement = new JDialog(SwingUtilities.windowForComponent(mraPanel), ModalityType.MODELESS);
        setIconImage(
                Toolkit.getDefaultToolkit().getImage(MarkerManagement.class.getResource("/images/menus/marker.png")));
        setTitle(I18n.text("Marker Management"));
        setBounds(100, 100, WIDTH, HEIGHT);
        addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                if (markerEditFrame != null)
                    markerEditFrame.dispose();

                e.getWindow().setVisible(false);
            }
        });
        getContentPane().setLayout(new MigLayout("", "[grow]", "[grow]"));
        setVisible(true);
        setResizable(false);

        markerEditFrame = new MarkerEdit(this, SwingUtilities.windowForComponent(this));

        //Add existing LogMarkers (only SidescanLogMarker ones)
        for (LogMarker m : mraPanel.getMarkers()) {
            if (m.getClass() == SidescanLogMarker.class) {
                logMarkers.add((SidescanLogMarker) m);
            }
        }

        panel = new JPanel();
        getContentPane().add(panel, "cell 0 0,grow");
        panel.setLayout(new MigLayout("", "[][][grow]", "[][][grow]"));

        new Thread(new LoadMarkers()).start();

    }

    private void setupExportButton() {
        JButton exportButton = new JButton();

        exportButton.setHorizontalTextPosition(SwingConstants.CENTER);
        exportButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        exportButton.setIcon(ImageUtils.getIcon("images/menus/print.png"));

        panel.add(exportButton, "cell 0 0");

        exportButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (table != null) {
                    if (table.getSelectedRows().length > 0) {
                        for (int i=0; i< table.getSelectedRows().length ; i++ ) {
                            //TODO : export selected markers to a file (.pdf ? )
                        }
                    }
                }
            }
        });
    }

    private class FinderDialog extends JDialog {
        private Java2sAutoTextField lblTxt;
        private JButton prevBtn, nextBtn, findBtn;
        private JLabel statusLbl;
        private JXBusyLabel busyLbl;
        private Window parent;
        private ArrayList<String> lblList = new ArrayList<>();
        
        public FinderDialog(Window parent) {
            super(parent, ModalityType.MODELESS);
            this.parent = parent;

            initComponents();
        }
        
        public void updateList() {
            //TODO:
        }
        
        private void initComponents() {

           // setIconImage(MarkerManagement.this.getIcon().getImage());
            setSize(290,270);
            setTitle(I18n.text("Find"));
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setResizable(false);

            getContentPane().setLayout(new MigLayout("", "[][grow]", "[][][][][][]"));

            new Thread(new LoadComponentsThread()).start();

        }

        private class LoadComponentsThread implements Runnable {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            public void run() {
                
                JPanel buttonPane = new JPanel();
                buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
                JPanel panel = new JPanel();
                getContentPane().add(buttonPane, BorderLayout.SOUTH);
                getContentPane().add(panel, BorderLayout.CENTER);

                prevBtn = new JButton("Prev.");
                nextBtn = new JButton("Next");
                findBtn = new JButton("Find");
                
                getRootPane().setDefaultButton(findBtn);
                panel.setLayout(new MigLayout("", "[][grow]", "[][][][][][][]"));
                JLabel labelLbl = new JLabel("Label:");
                JLabel altLbl = new JLabel("Altitude:");
                JLabel depthLbl = new JLabel("Depth:");
                JLabel locRadLbl = new JLabel("Radius:");
                JLabel classLbl = new JLabel("Class.:");
                JLabel annotLbl = new JLabel("Annot.:");
                JLabel tagsLbl = new JLabel("Tags:");
                
                JTextField altValueTxt = new JTextField();
                JTextField depthValueTxt = new JTextField();
                
                ArrayList<String> model1 = new ArrayList<>();
                
                JComboBox depthParamCBox = new JComboBox();
                JComboBox altParamCBox = new JComboBox();
                JComboBox classCBox = new JComboBox();
                JTextField tagsTxt = new JTextField();
                
                model1.add(ANY_TXT);
                Classification[] classifList = LogMarkerItem.Classification.values();
                for (int i=0; i < classifList.length; i++) {
                    model1.add(classifList[i].toString());
                }
                
                altParamCBox.setModel(new DefaultComboBoxModel(new String[] {ANY_TXT, "<=", ">="}));
                depthParamCBox.setModel(new DefaultComboBoxModel(new String[] {ANY_TXT, "<=", ">="}));
                classCBox.setModel(new DefaultComboBoxModel(model1.toArray()));
    
                altValueTxt.setColumns(5);
                depthValueTxt.setColumns(5);
                tagsTxt.setColumns(10);
                
                for (LogMarkerItem lbl : markerList) 
                    if (!lblList.contains(lbl.getLabel()))
                        lblList.add(lbl.getLabel());
                
                Collections.sort(lblList, String.CASE_INSENSITIVE_ORDER);
                lblList.add(0, ANY_TXT);
                
                initTypeField(lblList);
                panel.add(labelLbl, "cell 0 0,alignx trailing");
                panel.add(lblTxt, "cell 1 0,growx");
                panel.add(altLbl, "cell 0 1,alignx trailing");
                panel.add(altParamCBox, "cell 1 1,growx");
                panel.add(altValueTxt, "cell 1 1");
                panel.add(depthLbl, "cell 0 2,alignx trailing");
                panel.add(depthParamCBox, "cell 1 2,growx");
                panel.add(depthValueTxt, "cell 1 2");
                panel.add(locRadLbl, "cell 0 3,alignx trailing");
                panel.add(classLbl, "cell 0 4,alignx trailing");
                panel.add(classCBox, "cell 1 4,growx");
                panel.add(annotLbl, "cell 0 5,alignx trailing");
                panel.add(tagsLbl, "cell 0 6,alignx trailing");
                panel.add(tagsTxt, "cell 1 6,growx");
                
                buttonPane.add(prevBtn);
                buttonPane.add(nextBtn);
                buttonPane.add(findBtn);
                
                pack();
            }
        }
        
        private void initTypeField(ArrayList<String> items) {
            lblTxt = new Java2sAutoTextField(items);
            lblTxt.setFont(new Font("Dialog", Font.PLAIN, 11));
            lblTxt.setColumns(10);
            lblTxt.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        lblTxt.selectAll();
                    }
                }
            });
            lblTxt.addActionListener(new ActionListener(){
                public void actionPerformed(ActionEvent e){
                    findBtn.doClick();
                }});
        }
    }


    private class LoadMarkers implements Runnable {

        @Override
        public void run() {

            panel.add(loader, "cell 0 2 3 1,grow");
            loader.setText(I18n.text("Initializing Marker Management..."));
            loader.start();

            //check for XML file, load or create a new one 
            if (!logMarkers.isEmpty()) 
                setupMarkers();
            loader.stop();
            panel.remove(loader);

            // setupPrintButton();

            JButton findBtn = new JButton();
            findBtn.setHorizontalTextPosition(SwingConstants.CENTER);
            findBtn.setVerticalTextPosition(SwingConstants.BOTTOM);
            findBtn.setIcon(ImageUtils.createScaleImageIcon(SHOW_ICON, 13, 13));
            findBtn.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (find == null)
                        find = new FinderDialog(SwingUtilities.windowForComponent(mraPanel));

                    if (!find.isVisible()) {
                        find.setVisible(true);
                        find.pack();
                    }
                    else {
                        find.setVisible(false);
                        find.dispose();
                    }
                }
            });

            panel.add(findBtn, "cell 2 0,alignx right");

            JMenuBar menuBar;
            JMenu menu;
            JMenuItem storeMenuItm, exitMenuItm;

            menu = new JMenu("File");
            menuBar = new JMenuBar();
            menuBar.add(menu);

            AbstractAction storeAction = new AbstractAction(I18n.text("Store in DB")) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    //TODO
                }
            };
            
            AbstractAction exitAction = new AbstractAction(I18n.text("Exit")) {

                @Override
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            };
            
            //a group of JMenuItems
            storeMenuItm = new JMenuItem(storeAction);
            exitMenuItm = new JMenuItem(exitAction);

            menu.add(storeMenuItm);
            menu.add(exitMenuItm);

            menuBar.add(menu);

            setJMenuBar(menuBar);

            tableModel = new LogMarkerItemModel(markerList);
            table = new JTable(tableModel);

            //define max columns width
            tableModel.setColumnsWidth(table);

            tableModel.setCenteredColumns(table);

            //define default column to sort when creating table
            tableModel.setTableSorter(DEFAULT_COLUMN_TO_SORT, table);

            table.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent me) {
                    JTable table =(JTable) me.getSource();
                    int rowIndex = table.getSelectedRow();
                    if (me.getClickCount() == 2) {
                        if (table.getSelectedRow() != -1)
                            openMarkerEditor(table.getValueAt(table.getSelectedRow(), 1).toString(), rowIndex);
                    }
                }
            });


            JScrollPane scrollPane = new JScrollPane(table);

            JPopupMenu popupMenu = new JPopupMenu();

            AbstractAction del = new AbstractAction(I18n.text("Delete marker"), null) {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    int rowIndex = table.getSelectedRow();
                    if (rowIndex != -1) {
                        LogMarkerItem selectedMarker = findMarker(table.getValueAt(rowIndex, 1).toString());

                        if (markerEditFrame.getOpenMarker() == selectedMarker) {
                            markerEditFrame.dispose();
                        }

                        removeMarkerItem(selectedMarker, rowIndex);
                        removePanelMarkerItem(selectedMarker);

                    }
                }
            };

            del.putValue(Action.SHORT_DESCRIPTION, I18n.text("Delete this marker."));
            
            AbstractAction copy = new AbstractAction(I18n.text("Copy location"), null) {

                @Override
                public void actionPerformed(ActionEvent arg0) {
                    int rowIndex = table.getSelectedRow();
                    if (rowIndex != -1) {
                        LogMarkerItem selectedMarker = findMarker(table.getValueAt(rowIndex, 1).toString());
                        StringSelection selec = new StringSelection(selectedMarker.getLocation().toString());
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selec, selec);
                    }
                }
            };

            popupMenu.add(del);
            popupMenu.add(copy);
            table.setComponentPopupMenu(popupMenu);
            panel.add(scrollPane, "cell 0 2 3 1,grow");
            table.updateUI();
            panel.updateUI();

        }

    }
    private void deleteMarkersFiles() {
        markerFilePath = mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/marks.xml";
        FileUtils.deleteQuietly(new File(markerFilePath));
        
        File markerImgPath = new File(mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/markers/");
        FileUtils.deleteQuietly(markerImgPath);
    }
    
    private void setupMarkers() {
        markerFilePath = mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/marks.xml";

        //XML markers file doesnt exist and there are Markers to be added
        if (!new File(markerFilePath).exists() && !logMarkers.isEmpty()) {
            File markerImgPath = new File(mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/markers/");
            FileUtils.deleteQuietly(markerImgPath);
            NeptusLog.pub().info(I18n.text("Creating markers..."));
            loader.setText(I18n.text("Creating markers file"));
            createMarkers();
        }
        else {
            if (changedMarkers()) {
                deleteMarkersFiles();
                markerList = new ArrayList<>();

                NeptusLog.pub().info(I18n.text("Updating markers..."));
                loader.setText(I18n.text("Updating markers file"));
                createMarkers();
            }
            else {

                //XML markers file exists, load markers from it
                NeptusLog.pub().info(I18n.text("Loading markers..."));
                loader.setText(I18n.text("Loading markers"));

                if(!loadMarkers()) {
                    loader.setText(I18n.text("Creating markers"));
                    NeptusLog.pub().error(I18n.text("Corrupted markers file. Trying to create new markers file."));
                    createMarkers();
                }
            }
        }
    }


    /** checks for added or deleted markers
     * @return
     */
    private boolean changedMarkers() {
        //loads xml marker file
        loadMarkers();

        if (markerList.size() != logMarkers.size())
            return true;

        for (LogMarker log : logMarkers) {
            if (findMarker(log.getLabel()) == null) {
                return true;
            }
        }

        return false;
    }

    private void getImageForSingleMarker(SidescanParser ssParser, SidescanLogMarker marker) {
        int nSubsys = ssParser.getSubsystemList().size();
        int colorMapCode = LsfReportProperties.sidescanColorMap;
        SidescanConfig config = new SidescanConfig();
        boolean globalColorMap = true;
        if (colorMapCode == -1) {
            globalColorMap = false;
        }
        else {
            config.colorMap = LsfReport.getColorMapFromCode(colorMapCode);
        }

        SidescanParameters sidescanParams = setupSscanParam(config);

        createImage(marker, ssParser, nSubsys, config, sidescanParams, globalColorMap);
    }

    /** Creates an image for every existing marker
     * 
     * @param ssParser
     */
    private void getImagesForMarkers(SidescanParser ssParser) {
        int nSubsys = ssParser.getSubsystemList().size();
        int colorMapCode = LsfReportProperties.sidescanColorMap;
        SidescanConfig config = new SidescanConfig();
        boolean globalColorMap = true;
        if (colorMapCode == -1) {
            globalColorMap = false;
        }
        else {
            config.colorMap = LsfReport.getColorMapFromCode(colorMapCode);
        }

        SidescanParameters sidescanParams = setupSscanParam(config);

        for (SidescanLogMarker m : logMarkers) {
            createImage(m, ssParser, nSubsys, config, sidescanParams, globalColorMap);
        }
    }

    private SidescanParameters setupSscanParam(SidescanConfig config) {

        SidescanParameters sidescanParams = new SidescanParameters(0, 0);
        sidescanParams.setNormalization(config.normalization);
        sidescanParams.setTvgGain(config.tvgGain);

        return sidescanParams;
    }

    /** Creates an image for a specified Sidescan logmarker
     * 
     * @param ssLogMarker
     * @param parser
     * @param nSubsys
     * @param config
     * @param sidescanParams
     * @param globalColorMap
     */
    private void createImage(SidescanLogMarker ssLogMarker, SidescanParser parser, int nSubsys, SidescanConfig config, 
            SidescanParameters sidescanParams, boolean globalColorMap) {

        ssLogMarker.setDefaults(parser.getSubsystemList().get(0));//setDefaults if they are N/A

        for (int i = 0; i < nSubsys; i++) {

            BufferedImage image = null;
            try {
                image = LsfReport.getSidescanMarkImage(mraPanel.getSource(), parser, sidescanParams, config, globalColorMap, 
                        ssLogMarker, i);
            }catch(Exception e){
                NeptusLog.pub().error(e.getMessage());
            }

            if (image != null) {
                String path = mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/markers/";
                File dir = new File(path);

                //create dir if it doesnt exists
                if (!dir.exists())
                    dir.mkdirs();

                try {
                    ImageIO.write(image, "PNG", new File(path, ssLogMarker.getLabel() + ".png"));
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            else {// no image to display
                NeptusLog.pub().info(I18n.text("Oops, no image available for mark ")+ssLogMarker.getLabel()+ " ...");
            }
        }
    }

    /** Opens the MarkerEdit with a specificed marker
     * 
     * @param label, name of the marker to be loaded to MarkerEdit
     * @param rowIndex
     */
    private void openMarkerEditor(String label, int rowIndex) {

        LogMarkerItem selected = findMarker(label);
        if (selected == null) {
            return;
        }
        markerEditFrame.loadMarker(selected, rowIndex);
        markerEditFrame.setVisible(true);

    }
    /** Adds a new marker to the marker management
     * Creates an image for the marker and updates the XML and table model accordingly.
     * 
     * @param marker
     */

    private void addNewMarker(SidescanLogMarker marker) {
        if (dom == null)
            return;
        SidescanParser ssParser = SidescanParserFactory.build(mraPanel.getSource());
        getImageForSingleMarker(ssParser, marker);
        int lastIndex = 0;
        if (!markerList.isEmpty()) {
            lastIndex = markerList.get(markerList.size()-1).getIndex();
        }

        int insertIndex = lastIndex + 1;
        Element rootElement = dom.getDocumentElement();

        newLogMarkerItem(marker, ssParser, rootElement, insertIndex);

        int row;
        if (tableModel.getRowCount() == 0)
            row = 1;
        else 
            row = tableModel.getRowCount()-1;
        tableModel.insertRow(row);
        //update XML
        saveXML(dom);
    }

    /** Creates a new LogMarkerItem and stores it on a document
     * 
     * @param ssLogMarker
     * @param ssParser
     * @param rootElem
     * @param index
     */
    private void newLogMarkerItem(SidescanLogMarker ssLogMarker, SidescanParser ssParser, Element rootElem, int index) {
        LocationType loc = ssLogMarker.getLocation();
        double[] altAndHeight = getAltAndHeight(ssLogMarker, ssParser);
        double range = ssLogMarker.getwMeters();
  
        double alt = altAndHeight[0];
        double depth = altAndHeight[1];
        
        ArrayList<String> photoList = new ArrayList<>();
        HashSet<String> tagList = new HashSet<>();
        
        String description;
        if (ssLogMarker.getDescription() != null && !ssLogMarker.getDescription().isEmpty())
            description = ssLogMarker.getDescription();
        else
            description = "<Your annotation here.>";
       
        LogMarkerItem marker = new LogMarkerItem(index, ssLogMarker.getLabel(), ssLogMarker.getTimestamp(), loc.getLatitudeDegs(), 
                loc.getLongitudeDegs(), getImgPath(ssLogMarker.getLabel()), null, description, alt, depth, range, Classification.UNDEFINED, photoList, tagList);

        //format date timestamp
        String date = DateTimeUtil.dateFormatterXMLUTC.format(ssLogMarker.getTimestamp());

        //add new LogMarkerItem to list
        markerList.add(marker);

        // XML related
        Element mark = dom.createElement("Mark");
        rootElem.appendChild(mark);

        //set Index
        Attr attr = dom.createAttribute("id");
        attr.setValue(Integer.toString(index));
        mark.setAttributeNode(attr);

        Element label = dom.createElement("Label");
        label.appendChild(dom.createTextNode(ssLogMarker.getLabel()));
        mark.appendChild(label);

        Element ts = dom.createElement("Timestamp");
        ts.appendChild(dom.createTextNode(date));
        mark.appendChild(ts);

        Element lat = dom.createElement("Lat");
        lat.appendChild(dom.createTextNode(loc.getLatitudeDegs() + ""));
        mark.appendChild(lat);

        Element lon = dom.createElement("Lon");
        lon.appendChild(dom.createTextNode(loc.getLongitudeDegs() + ""));
        mark.appendChild(lon);

        Element image = dom.createElement("Image");
        if (marker.getSidescanImgPath() != null) {
            image.appendChild(dom.createTextNode(marker.getSidescanImgPath()));
        } 
        else {
            image.appendChild(dom.createTextNode(""));
        }
        mark.appendChild(image);

        Element draw = dom.createElement("Draw");
        draw.appendChild(dom.createTextNode("N/A"));
        mark.appendChild(draw);
        
        Element photo = dom.createElement("Photos");
        mark.appendChild(photo);

        Element altitude = dom.createElement("Altitude");
        altitude.appendChild(dom.createTextNode(marker.getAltitude() + ""));
        mark.appendChild(altitude);

        Element dep = dom.createElement("Depth");
        dep.appendChild(dom.createTextNode(marker.getDepth() + ""));
        mark.appendChild(dep);

        Element rang = dom.createElement("Range");
        rang.appendChild(dom.createTextNode(marker.getRange() + ""));
        mark.appendChild(rang);

        Element classif = dom.createElement("Classification");
        classif.appendChild(dom.createTextNode(Classification.UNDEFINED.toString()));
        mark.appendChild(classif);

        Element annot = dom.createElement("Annotation");
        annot.appendChild(dom.createTextNode(marker.getAnnotation()));
        mark.appendChild(annot);
    }

    /** Creates images for every existing marker and creates a 
     *  new LogMarkerITem for each existing marker, along with all marker information
     */
    private void createMarkers() {

        SidescanParser ssParser = SidescanParserFactory.build(mraPanel.getSource());
        getImagesForMarkers(ssParser);

        //XML document structure
        dom = Dom4JUtil.createEmptyDOMDocument();
        Element rootElement = dom.createElement("markers");
        dom.appendChild(rootElement);

        int i=1;
        for(SidescanLogMarker l : logMarkers) {
            newLogMarkerItem(l, ssParser, rootElement, i);
            i++;
        }
        // write the content into xml file
        saveXML(dom);
    }

    /** Calculates the Altitude and Height of a specific Sidescan marker
     * 
     * @param ssLogMarker
     * @param ssParser
     * @return
     */
    private double[] getAltAndHeight(SidescanLogMarker ssLogMarker, SidescanParser ssParser) {
        ssLogMarker.setDefaults(ssParser.getSubsystemList().get(0));
        SidescanConfig config = new SidescanConfig();
        SidescanParameters sidescanParams = setupSscanParam(config);
        double altitude = 0;
        double depth = 0;
        int subsys = ssLogMarker.getSubSys();

        ArrayList<SidescanLine> lines = LsfReport.getLines(ssParser, subsys, sidescanParams, ssLogMarker);

        if (lines != null && !lines.isEmpty()) {
            //get altitude from the line in the middle of the list
            altitude = lines.get(lines.size()/2).getState().getAltitude(); 
            depth = depth(lines.get(lines.size()/2).getTimestampMillis());
        }
        
        // result[] = {altitude, depth}
        double[] result = { ((double)Math.round(altitude * 100) / 100), depth };

        return result;
    }

    
    /** Creates a File for a marker with the specified string
     * @param marker
     * @return File of marker image
     */
    private String getImgPath(String marker) {
        File f = new File(mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/markers/" + marker + ".png");

        if(f.exists() && !f.isDirectory()) {
            String relPath = "/mra/markers/" + marker +".png";
            return relPath;
        }

        return null;
    }

    /** Loads markers that are stored on a XML File
     * 
     * @return true if markers were loaded successfully
     */
    private boolean loadMarkers() {
        parseXmlFile();
        parseDocument();

        return !markerList.isEmpty();
    }

    /** Deletes a LogMarkerItem from the table. XML and udpates the table model.
     * 
     * @param marker, the marker to be removed from the marker management
     * @param row, the row position in the table of the affected marker
     */
    public void removeMarkerItem(LogMarkerItem marker, int row) {

        //update & save XML
        deleteEntryXML(dom, marker);
        saveXML(dom);

        tableModel.removeRow(row);
        markerList.remove(marker);
    }

    /** Deletes the LogMarker from the mraPanel.
     *  
     * @param sMarker, marker to be removed
     */
    public void removePanelMarkerItem(LogMarkerItem sMarker) {
        LogMarker marker = findLogMarker(sMarker.getLabel());

        //delete marker from mraPanel
        if (marker != null) {
            mraPanel.removeMarkerAux(marker);
        }
    }

    /** Deletes a LogMarker from the table, XML and updates the table model.
     *  (Only used when calling removeMarker from MRAPanel)
     * @param marker, marker to be removed
     */
    public void removeMarker(LogMarker marker) {
        int row = findMarker(marker);
        LogMarkerItem markerToDel = findMarker(marker.getLabel());

        //update & save XML
        deleteEntryXML(dom, markerToDel);
        saveXML(dom);

        tableModel.removeRow(row);
        markerList.remove(markerToDel);

    }

    /** Deletes an entry in the XML file
     * 
     * @param doc, the document (XML) from where the marker will be removed
     * @param mrkerToDel the marker to delete from doc
     */
    private void deleteEntryXML(Document doc, LogMarkerItem mrkerToDel) {

        String markLabel = mrkerToDel.getLabel();
        NodeList nodes = doc.getElementsByTagName("Mark");

        for (int i = 0; i < nodes.getLength(); i++) {
            Element mark = (Element)nodes.item(i);
            Element label = (Element)mark.getElementsByTagName("Label").item(0);
            String pLabel = label.getTextContent();
            if (pLabel.equals(markLabel)) {
                mark.getParentNode().removeChild(mark);
            }
        }
        //delete draw image file
        if (mrkerToDel.getDrawImgPath() != null) {
            if (!mrkerToDel.getDrawImgPath().toString().equals("N/A")) {
                deleteImage(mrkerToDel.getDrawImgPath().toString());
            }
        }

        //delete sidescan image file
        if (!mrkerToDel.getSidescanImgPath().toString().equals("")) {
            deleteImage(mrkerToDel.getSidescanImgPath().toString());
        }


    }

    /** Updates the table with a modified LogMarkerItem at a specified row 
     * 
     * @param selectedMarker The updated marker
     * @param row The row that has been updated
     */
    public void updateLogMarker(LogMarkerItem selectedMarker, int row) {
        tableModel.updateRow(row);

        //update & save XML
        updateEntryXML(dom, selectedMarker);
        saveXML(dom);

    }

    /** Updates an entry in the XML file
     * @param dom2 the document (XML) that will be updated with modified LogMarkerItem
     * @param selectedMarker the marker entry to be updated
     */
    private void updateEntryXML(Document doc, LogMarkerItem mrkerToUpd) {

        String markLabel = mrkerToUpd.getLabel();
        NodeList nodes = dom.getElementsByTagName("Mark");

        for (int i = 0; i < nodes.getLength(); i++) {
            Element mark = (Element)nodes.item(i);
            Element label = (Element)mark.getElementsByTagName("Label").item(0);
            String pLabel = label.getTextContent();
            if (pLabel.equals(markLabel)) {
                mark.getElementsByTagName("Annotation").item(0).setTextContent(mrkerToUpd.getAnnotation());
                findLogMarker(markLabel).setDescription(mrkerToUpd.getAnnotation());
                
                mark.getElementsByTagName("Classification").item(0).setTextContent(mrkerToUpd.getClassification().name());
                mark.getElementsByTagName("Draw").item(0).setTextContent(mrkerToUpd.getDrawImgPath());
                
                Element photosEl = (Element) mark.getElementsByTagName("Photos").item(0);
                NodeList photosNode = mark.getElementsByTagName("Photos");
                
                Set<Element> targetElements = new HashSet<Element>();
                for (int s = 0; s < photosNode.getLength(); s++) {
                    Element e = (Element)photosNode.item(s);
                    NodeList pList = e.getElementsByTagName("Photo");
                    if (pList != null) {
                        for (int k=0; k< pList.getLength(); k++) {
                            Element photo = (Element)pList.item(k);
                            if (!mrkerToUpd.getPhotosPath().contains(photo.getFirstChild().getNodeValue())) {
                                //target to remove
                                targetElements.add(photo);
                            }
                        }
                    }
                }
            

                for (Element e: targetElements) {
                    e.getParentNode().removeChild(e);
                }

                for (String imgPath : mrkerToUpd.getPhotosPath()) {
                    if (!photoAlreadyExists(photosNode, imgPath)) {
                        Element photo = dom.createElement("Photo");
                        photo.appendChild(dom.createTextNode(imgPath));
                        photosEl.appendChild(photo);
                    }
                }
                
                if (mrkerToUpd.getPhotosPath().isEmpty()) {
                    System.out.println("Deleting them all!"); //TODO
                }
                
                System.out.println("List: "+ mrkerToUpd.getPhotosPath().toString());
            }
        }
        //delete draw image, if exists
        if (mrkerToUpd.getDrawImgPath().equals("N/A")) {
            deleteImage(mrkerToUpd.getDrawImgPath());
        }
    }
    
    private boolean photoAlreadyExists(NodeList node, String photoPath){
        
        for (int j = 0; j < node.getLength(); j++) {
            Element el = (Element)node.item(j);
            NodeList pList = el.getElementsByTagName("Photo");
            if (pList != null) {
                for (int k=0; k< pList.getLength(); k++) {
                    Element photo = (Element)pList.item(k);
                    System.out.println("photo :"+ photo.getFirstChild().getNodeValue());
                    if (photo.getFirstChild().getNodeValue().equals(photoPath)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    /** Add new LogMarker to MarkerManagement
     * @marker, the marker to be added
     * 
     */
    public void addMarker(LogMarker marker) {
        if (marker.getClass() == SidescanLogMarker.class) {
            logMarkers.add((SidescanLogMarker) marker);

            addNewMarker((SidescanLogMarker) marker);
        }
    }

    public void deleteImage(String path) {
        try {
            File fileTemp = new File(path);
            if (fileTemp.exists()) {
                fileTemp.delete();
            }
        }catch(Exception f){
            f.printStackTrace();
        }
    }

    /** Write the content into xml file
     * @param doc, document content to be written to XML
     */
    private void saveXML(Document doc) {

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(markerFilePath).getAbsolutePath());
            transformer.transform(source, result);

            if (!markerList.isEmpty()) {
                NeptusLog.pub().info(I18n.text("Markers XML file saved - ") + markerFilePath);
            }
        }
        catch (TransformerException e) {
            e.printStackTrace();
        }
    }

    private LogMarker findLogMarker(String label) {

        for (LogMarker log : mraPanel.getMarkers()) {
            if (log.getClass() == SidescanLogMarker.class) {
                if (log.getLabel().equals(label))
                    return log;
            }
        }
        return null;
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
                if (e != null)
                    markerList.add(e);
            }
        }
    }

    private LogMarkerItem getLogMarkerItem(Element markerEl) {
        int index = getAttIntValue(markerEl, "id");
        String name = getTextValue(markerEl,"Label");
        String tsString = getTextValue(markerEl, "Timestamp");
        SimpleDateFormat format = DateTimeUtil.dateFormatterXMLUTC;
        Date parsed = null;
        String path = null;
        String drawPath = null;
        ArrayList<String> photoList = null;
        HashSet<String> tagList = null;
        double ts = 0;
        double lon = 0;
        double lat = 0;
        double altitude = 0;
        double depth = 0;
        double range = 0;
        Classification cls = Classification.valueOf(getTextValue(markerEl,"Classification"));
        String annot = "";

        try  {
            String ssImgPath = getTextValue(markerEl, "Image");
            path = ssImgPath;
            String ssDrawImgPath = getTextValue(markerEl, "Draw");
            drawPath = ssDrawImgPath;
            parsed = format.parse(tsString);
            ts = parsed.getTime();
            lon = getDoubleValue(markerEl,"Lon");
            lat = getDoubleValue(markerEl,"Lat");
            altitude = getDoubleValue(markerEl, "Altitude");
            depth = getDoubleValue(markerEl, "Depth");
            range = getDoubleValue(markerEl, "Range");
            cls = Classification.valueOf(getTextValue(markerEl,"Classification"));
            annot = getTextValue(markerEl, "Annotation");
            photoList = getListValue(markerEl, "Photos");
            //tagList = getListValue(markerEl, "Tags"); FIXME
        }
        catch (ParseException e1) {
            e1.printStackTrace();
        }
        catch (NullPointerException e) {
            e.printStackTrace();
            NeptusLog.pub().error(I18n.text("Error parsing marker values from XML file"));
            return null;
        }

        //Create new LogMarkerItem with the value read from xml
        LogMarkerItem e = new LogMarkerItem(index, name, ts, lat, lon, path, drawPath, annot, altitude, depth, range, cls, photoList, tagList);

        return e;
    }

   
    private ArrayList<String> getListValue(Element ele, String tagName) {
        ArrayList<String> res = new ArrayList<>();
        
        NodeList nl = ele.getElementsByTagName(tagName);
        if (nl != null ){
            for (int i=0; i < nl.getLength(); i++) {
                Element el = (Element)nl.item(i);
                NodeList pList = el.getElementsByTagName("Photo");
                if (pList != null) {
                    for (int j=0; j < pList.getLength(); j++) {
                        Element photo = (Element)pList.item(j);
                        res.add(photo.getFirstChild().getNodeValue());
                    }
                }
            }
        }
        
        return res;
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
        return Integer.parseInt(ele.getAttribute(tagName));
    }

    private double getDoubleValue(Element ele, String tagName) throws NullPointerException{
        return Double.parseDouble(getTextValue(ele,tagName));
    }

    public Point getwindowLocation() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double width = screenSize.getWidth();
        int locationX = getLocation().x + getSize().width;
        int locationY = getLocation().y;
        if (getLocation().x + getWidth() + (markerEditFrame.getWidth()) > width) {
            locationX = new Double(width).intValue() - getWidth() - getLocation().x;
        }
        Point p = new Point(locationX, locationY);

        return p;
    }

    /**
     * 
     */
    public void cleanup() {
        markerList.clear();
        logMarkers.clear();

        if (markerEditFrame != null)
            markerEditFrame.dispose();

        dispose();
    }

    private LogMarkerItem findMarker(String label) {
        for (LogMarkerItem marker : markerList) {
            if (marker.getLabel().equals(label)) {
                return marker;
            }
        }
        return null;
    }

    private int findMarker(LogMarker marker) {
        int row = -1;
        for(int i=0; i < table.getRowCount() ; i++) {
            if (table.getValueAt(i, 1).equals(marker.getLabel())) {
                row = i;
                break;
            }
        }

        return row;
    }

    public void openMarker(LogMarker marker) {
        int row = findMarker(marker);
        if (row == -1) {
            NeptusLog.pub().error(I18n.text("Cannot open selected marker..."));
            return;
        }
        table.setRowSelectionInterval(row, row);
        openMarkerEditor(table.getValueAt(table.getSelectedRow(), 1).toString(), row);
    }

    /**
     * @param selectMarkerRowIndex
     */
    public void prevMark(int index) {
        if (index > 0) {
            int rowToOpen = index - 1;
            openMarkerEditor(table.getValueAt(rowToOpen, 1).toString(), rowToOpen);
            table.setRowSelectionInterval(rowToOpen, rowToOpen);
        }
    }

    /**
     * @param selectMarkerRowIndex
     */
    public void nextMark(int index) {
        if (index >= 0 && index < table.getRowCount()-1) {
            int rowToOpen = index + 1;
            openMarkerEditor(table.getValueAt(rowToOpen, 1).toString(), rowToOpen);
            table.setRowSelectionInterval(rowToOpen, rowToOpen);
        }
        else 
            if (index >= table.getRowCount()-1) {
                int rowToOpen = 0;
                openMarkerEditor(table.getValueAt(rowToOpen, 1).toString(), rowToOpen);
                table.setRowSelectionInterval(rowToOpen, rowToOpen);
            }
    }
}