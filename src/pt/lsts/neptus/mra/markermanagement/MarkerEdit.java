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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.FileUtils;
import org.jdesktop.swingx.JXStatusBar;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.llf.LogUtils;


/**
 * @author Manuel R.
 *
 */
@SuppressWarnings("serial")
public class MarkerEdit extends JDialog {

    private final int RULER_SIZE = 15;
    private int selectMarkerRowIndex = -1;
    private JPanel panel = new JPanel(); 
    private MarkerManagement parent;
    private AbstractAction save, del;
    private LogMarkerItem selectedMarker;
    private JLabel markerImage, nameLabelValue, timeStampValue, locationValue, altitudeValue, depthValue;
    private JComboBox<String> classifValue;
    private JTextArea annotationValue;
    private JButton rectDrawBtn, circleDrawBtn, freeDrawBtn, exportImgBtn;
    private int mouseX, mouseY, initialX, initialY, lastMouseX, lastMouseY, zoomScale = 2;
    private boolean enableFreeDraw = false;
    private boolean enableRectDraw = false;
    private boolean enableCircleDraw = false;
    private boolean enableGrid = false;
    private boolean enableRuler = true;
    private boolean enableZoom = false;
    private boolean mouseDown = false;
    private boolean toDeleteDraw = false;
    private BufferedImage layer,  rulerLayer, image, drawImageOverlay, zoomLayer;
    private ArrayList<Point> pointsList = new ArrayList<>();
    private JXStatusBar statusBar = new JXStatusBar();
    private JScrollPane listScroll;
    private DefaultListModel<String> photoListModel = new DefaultListModel<String>();

    public MarkerEdit(MarkerManagement parent, Window window) {
        super(window, ModalityType.MODELESS);
        this.parent = parent;

        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 590, 395);
        setIconImage(Toolkit.getDefaultToolkit().getImage(MarkerEdit.class.getResource("/images/menus/edit.png")));

        setupMenu();
        initialize();
    }


    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initialize() {
        panel.requestFocus();
        FocusListener l = new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
            }

            @Override
            public void focusGained(FocusEvent e) {
            }
        };
        panel.addFocusListener(l);
        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new MigLayout("", "[][][][][grow][][][][grow]", "[][][][][][][grow][][grow]"));
        //FIXME
        markerImage = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (image!=null && layer!=null) {
                    Graphics2D lg2d = (Graphics2D) layer.getGraphics();
                    lg2d.setBackground(new Color(100, 100, 255, 0));
                    lg2d.clearRect(0, 0, layer.getWidth(), layer.getHeight());

                    Graphics2D rg2d = (Graphics2D) rulerLayer.getGraphics();
                    rg2d.setBackground(new Color(100, 100, 255, 0));
                    rg2d.clearRect(0, 0, rulerLayer.getWidth(), rulerLayer.getHeight());

                    Graphics2D zg2d = (Graphics2D) zoomLayer.getGraphics();
                    zg2d.setBackground(new Color(100, 100, 255, 0));
                    zg2d.clearRect(0, 0, zoomLayer.getWidth(), zoomLayer.getHeight());

                    g.drawImage(image, RULER_SIZE+1, RULER_SIZE+1, null);

                    //horizontal line
                    g.drawLine(RULER_SIZE, image.getHeight()+RULER_SIZE, image.getWidth()+RULER_SIZE, image.getHeight()+RULER_SIZE);

                    //vertical line
                    g.drawLine(RULER_SIZE, RULER_SIZE+1, RULER_SIZE, image.getHeight()+RULER_SIZE);

                    if (enableRectDraw) 
                        drawRect(layer.getGraphics(), 0, 0);

                    if (enableFreeDraw)
                        drawFree(layer.getGraphics());

                    if (enableCircleDraw)
                        drawCircle(layer.getGraphics(), 0, 0);

                    if (enableZoom && mouseDown)
                        zoom(layer.getGraphics(), zoomLayer.getGraphics());

                    g.drawImage(layer, RULER_SIZE+1, RULER_SIZE+1, null);

                    if (drawImageOverlay != null)
                        g.drawImage(drawImageOverlay, RULER_SIZE+1, RULER_SIZE+1, null);

                    //Draw ruler
                    if (enableRuler) {
                        drawRuler(rg2d);
                        g.drawImage(rulerLayer, 0, 0, null);
                    }

                    g.drawImage(zoomLayer, RULER_SIZE+1, RULER_SIZE+1, null);
                }
            }
        };

        markerImage.setHorizontalAlignment(SwingConstants.CENTER);
        markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource("/images/unknown.png")));
        markerImage.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                //before ((JPanel) e.getSource()).repaint();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseDown = false;
                    lastMouseX = mouseX;
                    lastMouseY = mouseY;
                }
                markerImage.repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {

                if (SwingUtilities.isLeftMouseButton(e)) {
                    mouseDown = true;
                    if (enableFreeDraw && e.getClickCount() == 2) {
                        pointsList.clear();
                    }
                    mouseX = e.getX();
                    mouseY = e.getY();
                    initialX = mouseX;
                    initialY = mouseY;
                }
                markerImage.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseX = mouseY = -1;    
            }
        });

        markerImage.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }   

            @Override
            public void mouseDragged(MouseEvent e) {                    
                mouseX = e.getX();
                mouseY = e.getY();
                if ((mouseX > RULER_SIZE && mouseX < image.getWidth()+RULER_SIZE && mouseY > 0) &&
                        (mouseY < image.getHeight())) {

                    pointsList.add(new Point(mouseX-RULER_SIZE-1, mouseY-RULER_SIZE-1));
                }
                markerImage.repaint();
            }
        });

        panel.add(markerImage, "cell 0 0 7 7,alignx left,aligny top");

        JLabel nameLabel = new JLabel(I18n.text("Label:"));
        nameLabelValue = new JLabel();
        JLabel timeStampLabel = new JLabel(I18n.text("Timestamp:"));
        timeStampValue = new JLabel("TS");
        JLabel locationLabel = new JLabel(I18n.text("Location:"));
        locationValue = new JLabel("LOCATION");
        JLabel altitudeLabel = new JLabel(I18n.text("Altitude:"));
        altitudeValue = new JLabel("ALTITUDE");
        JLabel classifLabel = new JLabel(I18n.text("Classification:"));
        classifValue = new JComboBox<>();
        JLabel annotationLabel = new JLabel(I18n.text("Annotation:"));
        JLabel depthLabel = new JLabel(I18n.text(" / Depth:"));
        depthValue = new JLabel("DEPTH");

        nameLabelValue.setBackground(Color.WHITE);
        nameLabelValue.setText("MARKER_LABEL");
        panel.add(nameLabel, "cell 7 0,alignx left");
        panel.add(nameLabelValue, "cell 8 0,alignx left");
        panel.add(timeStampLabel, "cell 7 1,alignx left");
        panel.add(timeStampValue, "cell 8 1,alignx left");
        panel.add(locationLabel, "cell 7 2,alignx left");
        panel.add(locationValue, "cell 8 2,alignx left");
        panel.add(altitudeLabel, "cell 7 3,alignx left");
        panel.add(altitudeValue, "flowx,cell 8 3,alignx left");
        panel.add(classifLabel, "cell 7 4,alignx trailing");
        classifValue.setBackground(Color.WHITE);
        classifValue.setModel(new DefaultComboBoxModel(Classification.values()));
        panel.add(classifValue, "cell 8 4,alignx left");
        panel.add(annotationLabel, "cell 7 5");

        JPanel panelTwo = new JPanel();
        panel.add(panelTwo, "cell 7 6 2 1, growx, aligny top");
        panelTwo.setLayout(new BorderLayout(5, 5));
        JScrollPane scrollPane = new JScrollPane();
        panelTwo.add(scrollPane, BorderLayout.NORTH);
        annotationValue = new JTextArea();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        annotationValue.setText(I18n.text("<Your annotations here>"));
        annotationValue.setLineWrap(true); //Auto down line if the line is too long
        annotationValue.setWrapStyleWord(true); //Auto set up the style of words
        annotationValue.setRows(3);
        scrollPane.setViewportView(annotationValue);

        JPanel southPanel = new JPanel();
        panelTwo.add(southPanel, BorderLayout.SOUTH);
        southPanel.setLayout(new BorderLayout(0, 0));

        JPanel eastPanel = new JPanel();
        southPanel.add(eastPanel, BorderLayout.EAST);
        eastPanel.setLayout(new BorderLayout(0, 0));

        listScroll = new JScrollPane();
        eastPanel.add(listScroll, BorderLayout.EAST);

        JList photoList = new JList();
        photoList.setModel(photoListModel);
        listScroll.setViewportView(photoList);
        listScroll.setVisible(false);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BorderLayout(20, 20));
        eastPanel.add(buttonPanel, BorderLayout.CENTER);

        JPanel eastBtnPanel = new JPanel();
        buttonPanel.add(eastBtnPanel, BorderLayout.EAST);
        eastBtnPanel.setLayout(new MigLayout("", "[89px][89px]", "[23px][][]"));

        JButton addBtn = new JButton(I18n.text("Add Photo"));
        addBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                addPhoto();
            }
        });
        eastBtnPanel.add(addBtn, "cell 1 1,alignx center,aligny top");

        JButton remBtn = new JButton(I18n.text("Remove Photo"));
        remBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String toRemove = (String) photoList.getSelectedValue();
                if (toRemove == null)
                    return;

                removePhoto(toRemove);
            }

        });
        eastBtnPanel.add(remBtn, "cell 1 2,alignx center,aligny top");

        panel.add(depthLabel, "cell 8 3");
        panel.add(depthValue, "cell 8 3");

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY));

        statusPanel.add(statusBar);
        getContentPane().add(statusPanel, BorderLayout.SOUTH);
    }

    private boolean addPhoto() {
        String[] imgExtensions =  { "bmp", "jpg", "jpeg", "wbmp", "png", "gif" } ;

        String path = parent.logPath().concat("/mra/markers/");
        JFileChooser fileChooser = GuiUtils.getFileChooser(path, 
                I18n.text("Image Files"), imgExtensions);

        fileChooser.setAcceptAllFileFilterUsed(false);
        int status = fileChooser.showOpenDialog(ConfigFetch.getSuperParentFrame());

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile != null) {
                String folder = createFolder(path);
                
                if (FileUtil.copyFile(selectedFile.getPath(), folder + "/" + selectedFile.getName())) {
                    addToList(selectedFile.getName(), "/mra/markers/photos/"+selectedMarker.getLabel()+"/");
                    showPhotoList();
                    addStatusBarMsg("Added new photo");
                }
            }
        } 
        return false;
    }

    private boolean addToList(String name, String path) {
        for (String photo : selectedMarker.getPhotosPath()) {
            String picName = photo.substring(photo.lastIndexOf("/"), photo.length());
            if (picName.equals(name))
                return false;
        }

        photoListModel.addElement(name);
        selectedMarker.getPhotosPath().add(path.concat(name));
        return true;
    }


    private String createFolder(String path) {
        File folder = new File(path.concat("/photos/"));
        String finalPath = folder.getPath() + "/" + selectedMarker.getLabel();
        if (!folder.exists()) {
            folder.mkdir();
            folder = new File(finalPath);
            if (!folder.exists()) {
                folder.mkdir();
            }
            return folder.getAbsolutePath();
        }
        else {
            folder = new File(finalPath);
            if (!folder.exists()) {
                folder.mkdir();
            }
            return folder.getAbsolutePath();
        }
    }

    private void removePhoto(String toRemove) {
        String source = parent.logPath();
        String path = "/mra/markers/photos/"+selectedMarker.getLabel()+"/" + toRemove;
        File toRemoveFile = new File(source + path);

        FileUtils.deleteQuietly(toRemoveFile);
        selectedMarker.getPhotosPath().remove(path);

        photoListModel.removeElement(toRemove);

    }
    public void addStatusBarMsg(String msg){
        JLabel jlabel = new JLabel(msg);
        jlabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        statusBar.removeAll();
        statusBar.updateUI();
        statusBar.add(jlabel);
        statusBar.updateUI();
    }

    private void drawRect(Graphics g, int endX, int endY) {
        if (mouseX == -1 && lastMouseX == -1) 
            return;
        Graphics2D g2 = (Graphics2D) g;

        if (endX != 0 && endY != 0) {
            mouseX = endX;
            mouseY = endY;
        }
        int x = 0, y = 0, w = 0, h = 0;
        if (mouseX == -1) { //mouse got out of markerimage window (use last X and Y coordinates)
            x = Math.min(initialX - RULER_SIZE - 1, lastMouseX - RULER_SIZE -1);
            y = Math.min(initialY - RULER_SIZE - 1, lastMouseY - RULER_SIZE -1);
            w = Math.max(initialX - RULER_SIZE -1, lastMouseX - RULER_SIZE -1) - Math.min(initialX - RULER_SIZE -1, lastMouseX - RULER_SIZE -1);
            h = Math.max(initialY - RULER_SIZE -1, lastMouseY - RULER_SIZE -1) - Math.min(initialY - RULER_SIZE -1, lastMouseY - RULER_SIZE -1);

        } else {
            x = Math.min(initialX - RULER_SIZE - 1, mouseX - RULER_SIZE -1);
            y = Math.min(initialY - RULER_SIZE - 1, mouseY - RULER_SIZE -1);
            w = Math.max(initialX - RULER_SIZE -1, mouseX - RULER_SIZE -1) - Math.min(initialX - RULER_SIZE -1, mouseX - RULER_SIZE -1);
            h = Math.max(initialY - RULER_SIZE -1, mouseY - RULER_SIZE -1) - Math.min(initialY - RULER_SIZE -1, mouseY - RULER_SIZE -1);

        }
        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, w, h);
    }

    private void zoom(Graphics img, Graphics zoom) {
        if (mouseX == -1 && lastMouseX == -1) 
            return;
        Graphics2D g2 = (Graphics2D) img;
        Graphics2D gZoom = (Graphics2D) zoom;

        g2.setColor(Color.WHITE);
        g2.drawRect(mouseX - RULER_SIZE -1 -25, mouseY - RULER_SIZE -1 -25, 50, 50);

        int ZOOM_BOX_SIZE = 50;

        int X = (int) MathMiscUtils.clamp(mouseX - RULER_SIZE -1, ZOOM_BOX_SIZE / 2, image.getWidth() - ZOOM_BOX_SIZE / 2);
        int Y = (int) MathMiscUtils.clamp(mouseY - RULER_SIZE -1, ZOOM_BOX_SIZE / 2, image.getHeight() - ZOOM_BOX_SIZE / 2);

        //TODO : ir buscar crop da imagem ao proprio ficheiro, pq tem melhor resolução

        BufferedImage zoomImage = image.getSubimage(X - ZOOM_BOX_SIZE / 2, Y - ZOOM_BOX_SIZE / 2, 50, ZOOM_BOX_SIZE);

        int w = zoomImage.getWidth();
        int h = zoomImage.getHeight();

        gZoom.drawRect(image.getWidth()-RULER_SIZE-(w*zoomScale)-1, image.getHeight()-RULER_SIZE-(h*zoomScale)-1, w*zoomScale+1, h*zoomScale+1);
        gZoom.drawImage(ImageUtils.getFasterScaledInstance(zoomImage, w*zoomScale, h*zoomScale), image.getWidth()-RULER_SIZE-(w*zoomScale), image.getHeight()-RULER_SIZE-(h*zoomScale), null);
    }

    private void drawFree(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        for (Point p : pointsList) {
            g2.setColor(Color.WHITE);
            g2.drawLine(p.x+2, p.y, p.x, p.y);
        }
    }

    private void drawCircle(Graphics g, int endX, int endY) {
        if (mouseX == -1 && lastMouseX == -1) 
            return;

        if (endX != 0 && endY != 0) {
            mouseX = endX;
            mouseY = endY;
        }
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.WHITE);
        int x = 0, y = 0, w = 0, h = 0;
        if (mouseX == -1) { //mouse got out of markerimage window (use last X and Y coordinates)
            x = Math.min(initialX - RULER_SIZE - 1, lastMouseX - RULER_SIZE -1);
            y = Math.min(initialY - RULER_SIZE - 1, lastMouseY - RULER_SIZE -1);
            w = Math.max(initialX - RULER_SIZE -1, lastMouseX - RULER_SIZE -1) - Math.min(initialX - RULER_SIZE -1, lastMouseX - RULER_SIZE -1);
            h = Math.max(initialY - RULER_SIZE -1, lastMouseY - RULER_SIZE -1) - Math.min(initialY - RULER_SIZE -1, lastMouseY - RULER_SIZE -1);
        } else {
            x = Math.min(initialX - RULER_SIZE - 1, mouseX - RULER_SIZE -1);
            y = Math.min(initialY - RULER_SIZE - 1, mouseY - RULER_SIZE -1);
            w = Math.max(initialX - RULER_SIZE -1, mouseX - RULER_SIZE -1) - Math.min(initialX - RULER_SIZE -1, mouseX - RULER_SIZE -1);
            h = Math.max(initialY - RULER_SIZE -1, mouseY - RULER_SIZE -1) - Math.min(initialY - RULER_SIZE -1, mouseY - RULER_SIZE -1);
        }

        g2.drawOval(x,y,w,h);
    }

    private void drawRuler(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;

        int fontSize = 11;
        int margin = 8;
        int lineWith = 9;
        int y = image.getHeight()+RULER_SIZE;

        g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        g2d.setColor(Color.BLACK);

        //draw zero
        g2d.drawString("0", RULER_SIZE-5, y+lineWith);

        //set range (width) and height
        double range = selectedMarker.getRange();
        double height = selectedMarker.getHeight();
        
        float zoomRangeStep = 2;
        if (range >= 10.0 && range < 30.0)
            zoomRangeStep = 3;
        else {
            if (range >= 30.0)
                zoomRangeStep = 5;
        }
        //System.out.println("Range "+ range);

        //horizontal rectangle
        //g2d.setColor(new Color(.3f, .4f, .5f, .6f));
        g2d.setColor(new Color(.5f, .6f, .7f,.8f));
        if (range != 0) {
            Rectangle horizRect = new Rectangle(RULER_SIZE+1, image.getHeight()+3, image.getWidth(), 12);
            g2d.fill(horizRect);
        }

        //vertical rectangle
        if (height != 0) {
            Rectangle vertRect = new Rectangle(RULER_SIZE+1, RULER_SIZE+1, RULER_SIZE, image.getHeight()-1);
            g2d.fill(vertRect);
        }

        g2d.setColor(Color.BLACK);

        // horizontal ruler (range)

        double step = zoomRangeStep * (image.getWidth()+margin) / range;
        double r = zoomRangeStep;
        int c = margin + (int) step;
        g2d.setColor(Color.WHITE);

        for (; c<=image.getWidth()+margin; c += step , r += zoomRangeStep) {
            int length = (int)(Math.log10(r)+1);
            g2d.setColor(Color.WHITE);
            g2d.drawLine(c, y, c, y-lineWith);
            if (enableGrid)
                g2d.drawLine(c, RULER_SIZE, c, y);

            if (length >= 2) {
                margin = 13;
            }
            g2d.drawString("" + (int) r, c - margin, y-1);
            g2d.setColor(Color.BLACK);
            g2d.drawLine(c, y, c, y+lineWith);

        }

        // vertical ruler (height)

        double zoomRangeStepV = 2.0;
        double stepV = zoomRangeStepV * (image.getHeight()) / height;
        double rV = 0;
        int cV = y;

        //System.out.println("height "+ height);

        for (; cV >= 0 ; cV -= stepV , rV += zoomRangeStepV) {
            if (cV < y && rV < height) {
                g2d.setColor(Color.WHITE);
                g2d.drawLine(RULER_SIZE+1, cV, (RULER_SIZE+3)+lineWith, cV);
                g2d.drawString("" + (int) rV,  RULER_SIZE + 4 , cV+11);
                if (enableGrid)
                    g2d.drawLine(RULER_SIZE+1, cV, image.getWidth()+RULER_SIZE, cV);

                g2d.setColor(Color.BLACK);
                g2d.drawLine(RULER_SIZE, cV, (RULER_SIZE)-lineWith, cV);
            }
        }
    }

    public void loadMarker(LogMarkerItem log, int rowIndex) {
        statusBar.removeAll();
        toDeleteDraw = false;
        selectedMarker = log;
        selectMarkerRowIndex = rowIndex;
        int prefWidth = 265;
        int prefHeight = 80+30;

        if (selectedMarker.getSidescanImgPath() != null ) {
            try {
                String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent();
                File f = new File(path + selectedMarker.getSidescanImgPath());

                image = ImageIO.read(f);

                int width = image.getWidth();
                int height = image.getHeight();
                if (width > 300 || height > 300) {
                    width = width / 3;
                    height = height / 3;
                    image = (BufferedImage) ImageUtils.getFasterScaledInstance(image, width, height);

                }

                markerImage.setIcon(null);
                markerImage.repaint();
                markerImage.setPreferredSize(new Dimension(width + RULER_SIZE + 10, height + RULER_SIZE + 10));

                setBounds(100, 100, width + prefWidth + RULER_SIZE + 10, height + prefHeight + RULER_SIZE + 10);
                setLocation(parent.getwindowLocation());

                if (selectedMarker.getDrawImgPath() != null && !selectedMarker.getDrawImgPath().toString().equals("N/A")) {
                    File fDraw = new File(path + selectedMarker.getDrawImgPath());
                    drawImageOverlay = ImageIO.read(fDraw);
                } else 
                    drawImageOverlay = null;

                layer = ImageUtils.createCompatibleImage(image.getWidth(), 
                        image.getHeight(), Transparency.TRANSLUCENT);

                rulerLayer = ImageUtils.createCompatibleImage(markerImage.getPreferredSize().width, 
                        markerImage.getPreferredSize().height, Transparency.TRANSLUCENT);
                zoomLayer = ImageUtils.createCompatibleImage(image.getWidth(), 
                        image.getHeight(), Transparency.TRANSLUCENT);
                clearLayer();
            } catch (IOException e) {
                NeptusLog.pub().error(I18n.text("Error reading image file for marker: ")+ selectedMarker.getLabel() + " ...");
                image = null;
                markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource("/images/unknown.png")));
                markerImage.setPreferredSize(new Dimension(markerImage.getIcon().getIconWidth(), markerImage.getIcon().getIconHeight()));
                setBounds(100, 100, markerImage.getIcon().getIconWidth() + prefWidth + 10, markerImage.getIcon().getIconHeight() + prefHeight);
                setLocation(parent.getwindowLocation());
            }
        } else {
            image = null;
            markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource("/images/unknown.png")));
            markerImage.setPreferredSize(new Dimension(markerImage.getIcon().getIconWidth(), markerImage.getIcon().getIconHeight()));
            setBounds(100, 100, markerImage.getIcon().getIconWidth() + prefWidth + RULER_SIZE + 10, markerImage.getIcon().getIconHeight() + prefHeight + RULER_SIZE + 10);
            setLocation(parent.getwindowLocation());
        }
        if (image == null || layer == null) {
            rectDrawBtn.setEnabled(false);
            circleDrawBtn.setEnabled(false);
            freeDrawBtn.setEnabled(false);
            exportImgBtn.setEnabled(false);
        } else {
            rectDrawBtn.setEnabled(true);
            circleDrawBtn.setEnabled(true);
            freeDrawBtn.setEnabled(true);
            exportImgBtn.setEnabled(true);
        }
        enableCircleDraw = enableFreeDraw = enableRectDraw = false;
        nameLabelValue.setText(selectedMarker.getLabel());
        nameLabelValue.setToolTipText(selectedMarker.getLabel());
        timeStampValue.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(selectedMarker.getTimestamp()));
        locationValue.setText(selectedMarker.getLocation().toString());
        String altitudeVal = selectedMarker.getAltitude() < 0 ? "-" : Double.toString(selectedMarker.getAltitude()) + " m";
        altitudeValue.setText(altitudeVal);

        NumberFormat nf = GuiUtils.getNeptusDecimalFormat();
        DecimalFormat df2 = (DecimalFormat)nf;
        df2.applyPattern("###.##");
        double formatedDepth = Double.valueOf(df2.format(selectedMarker.getDepth()));

        depthValue.setText(Double.toString(formatedDepth) + " m");
        classifValue.setSelectedItem(selectedMarker.getClassification());
        annotationValue.setText(selectedMarker.getAnnotation());
        nameLabelValue.setSize(nameLabelValue.getPreferredSize() );

        VehicleType veh = LogUtils.getVehicle(parent.mraPanel.getSource());
        String vehicle = (veh != null ? " | "+veh.getName() : "");
        setTitle(I18n.text("Marker: ") + nameLabelValue.getText() + " | " + timeStampValue.getText() + " | " + parent.mraPanel.getSource().name() + vehicle);
        
        if (selectedMarker.getPhotosPath().isEmpty())
            hidePhotoList();
        else
            showPhotoList();
    }

    private void showSuccessDlg(String path) {
        if (!path.endsWith(".png"))
            path = path + ".png";

        GuiUtils.showInfoPopup(I18n.text("Success"), I18n.text("Image exported to: ")+path);
        addStatusBarMsg("Image '"+path+"' exported successfully...");
    }

    private String chooseSaveFile(BufferedImage image, String path) {

        JFileChooser fileChooser = new JFileChooser(new File(path));
        fileChooser.setFileFilter(new FileFilter() {

            @Override
            public String getDescription() {
                return "*.png";
            }

            @Override
            public boolean accept(File file) {
                if (file.isDirectory()) {
                    return true;
                } else {
                    String path = file.getAbsolutePath().toLowerCase();
                    if (path.endsWith(".png")) {
                        return true;
                    }
                }
                return false;
            }
        });

        int status = fileChooser.showSaveDialog(null);

        String fileName = null;

        if (status == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            try {
                fileName = selectedFile.getCanonicalPath();
                if (!fileName.endsWith(".png")) {
                    selectedFile = new File(fileName + ".png");
                }
                ImageIO.write(image, "png", selectedFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return fileName;
    }

    private JToggleButton createToggleBtn(String iconPath, String toolTipTxt) {
        JToggleButton btn = new JToggleButton();

        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setIcon(ImageUtils.getIcon(iconPath));
        btn.setToolTipText(toolTipTxt);

        return btn;
    }

    private JButton createBtn(String iconPath, String toolTipTxt) {
        JButton btn = new JButton();

        btn.setHorizontalTextPosition(SwingConstants.CENTER);
        btn.setVerticalTextPosition(SwingConstants.BOTTOM);
        btn.setIcon(ImageUtils.getIcon(iconPath));
        btn.setToolTipText(toolTipTxt);

        return btn;
    }

    private void setupMenu() {

        final JPopupMenu popup = new JPopupMenu();
        final JPopupMenu popupSlider = new JPopupMenu();

        JSlider slider = new JSlider(JSlider.HORIZONTAL, 1, 5, 2);
        slider.setMinorTickSpacing(1);
        slider.setMajorTickSpacing(1);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        slider.setFont(new Font("SansSerif", Font.PLAIN, 10));
        slider.setPreferredSize(new Dimension(70, 50));
        ChangeListener l = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                JSlider source = (JSlider)ce.getSource();
                if (!source.getValueIsAdjusting()) {
                    zoomScale = (int)source.getValue();
                }
            }
        };
        slider.addChangeListener(l);
        popupSlider.add(slider);

        JToolBar toolBar = new JToolBar();
        toolBar.setRequestFocusEnabled(false);
        toolBar.setFloatable(false);
        toolBar.setRollover(true);

        JButton saveBtn = createBtn("images/menus/save.png", I18n.text("Save"));
        JButton delBtn = createBtn("images/menus/editdelete.png", I18n.text("Delete"));
        rectDrawBtn = createBtn("images/menus/rectdraw.png", I18n.text("Draw rectangle"));
        circleDrawBtn = createBtn("images/menus/circledraw.png", I18n.text("Draw circle"));
        freeDrawBtn = createBtn("images/menus/freedraw.png", I18n.text("Draw"));
        JButton clearDrawBtn = createBtn("images/menus/clear.png", I18n.text("Clear all"));
        JButton showGridBtn = createBtn("images/menus/grid.png", I18n.text("Show grid"));
        JButton showRulerBtn = createBtn("images/menus/ruler.png", I18n.text("Show ruler"));
        exportImgBtn = createBtn("images/menus/export.png", I18n.text("Export"));

        JButton previousMarkBtn = createBtn("images/menus/previous.png", I18n.text("Previous Mark"));
        JButton nextMarkBtn = createBtn("images/menus/next.png", I18n.text("Next Mark"));

        JToggleButton zoomBtn = createToggleBtn("images/menus/zoom_btn.png", I18n.text("Zoom"));

        save = new AbstractAction(I18n.text("Save"), ImageUtils.getIcon("images/menus/save.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                Classification classif = (Classification) classifValue.getSelectedItem();
                String annotation = annotationValue.getText();

                //save drawing image
                BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();
                String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/markers/";

                // drawRect(layer.getGraphics(), lastMouseX, lastMouseY);

                //g2d.drawImage(image, 0, 0, null);
                if (enableRectDraw)
                    drawRect(g2d, lastMouseX, lastMouseY);
                if (enableCircleDraw)
                    drawCircle(g2d, lastMouseX, lastMouseY);
                if (enableFreeDraw)
                    drawFree(g2d);

                if (drawImageOverlay != null)
                    g2d.drawImage(drawImageOverlay, 0, 0, null);

                File drawFile = new File(path, selectedMarker.getLabel() + "_draw.png");
                // save image to file
                try {
                    ImageIO.write(img, "PNG", drawFile);
                } catch (IOException ie) {
                    NeptusLog.pub().error(I18n.text("Error writing image to file..."));
                    addStatusBarMsg("Error writing image to file...");
                }

                g2d.dispose();

                String relPath = "/mra/markers/" + selectedMarker.getLabel() +"_draw.png";

                //end save drawing image
                selectedMarker.setDrawImgPath(relPath);
                selectedMarker.setClassification(classif);
                selectedMarker.setAnnotation(annotation);
                if (toDeleteDraw) {
                    parent.deleteImage(drawFile.toString());
                    selectedMarker.setDrawImgPath("null");
                }
                parent.updateLogMarker(selectedMarker, selectMarkerRowIndex);
                markerImage.repaint();
                addStatusBarMsg("Saving completed...");
            }
        };

        del = new AbstractAction(I18n.text("Delete"), ImageUtils.getIcon("images/menus/editdelete.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                int res = GuiUtils.confirmDialog(null, I18n.text("Confirm delete"), I18n.text("Are you sure you want to delete this marker?"));
                if (res == 0)  { 
                    parent.removeMarkerItem(selectedMarker, selectMarkerRowIndex);
                    parent.removePanelMarkerItem(selectedMarker);
                    dispose();
                }
            }
        };

        AbstractAction showGrid = new AbstractAction(I18n.text("Show grid"), ImageUtils.getIcon("images/menus/grid.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (enableGrid)
                    enableGrid = false;
                else
                    enableGrid = true;

                markerImage.repaint();
            }
        };
        AbstractAction showRuler = new AbstractAction(I18n.text("Show ruler"), ImageUtils.getIcon("images/menus/ruler.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (enableRuler) {
                    enableRuler = false;
                    addStatusBarMsg("Showing ruler...");
                }
                else {
                    enableRuler = true;
                    addStatusBarMsg("Hiding Ruler...");
                }
                markerImage.repaint();
            }
        };

        AbstractAction clearDrawings = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                enableFreeDraw = false;
                enableRectDraw = false;
                enableCircleDraw = false;
                drawImageOverlay = null;
                clearLayer();

                //delete draw image if exists
                toDeleteDraw = true;
                addStatusBarMsg("Clearing all drawings...");
            }
        };
        AbstractAction drawRect = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                clearLayer();
                enableFreeDraw = false;
                enableRectDraw = true;
                enableCircleDraw = false;
                enableZoom = false;
            }
        };

        AbstractAction drawCircle = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                clearLayer();
                enableRectDraw = false;
                enableCircleDraw = true;
                enableFreeDraw = false;
                enableZoom = false;
            }
        };

        AbstractAction drawFree = new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                pointsList.clear();
                clearLayer();
                enableRectDraw = false;
                enableCircleDraw = false;
                enableFreeDraw = true;
                enableZoom = false;
            }
        };

        AbstractAction exportImgOnly = new AbstractAction(I18n.text("Image only")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (markerImage != null) {
                    BufferedImage img = new BufferedImage(markerImage.getWidth(), markerImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();

                    g2d.drawImage(image, 0, 0, null);
                    g2d.dispose();
                    String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/markers/";

                    // save image to file
                    String fileName = chooseSaveFile(img, path);
                    // show saved dialog
                    if (fileName != null)
                        showSuccessDlg(fileName); 
                }
            }
        };

        AbstractAction exportImageWruler = new AbstractAction(I18n.text("Image w/ ruler")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (markerImage != null && rulerLayer != null) {

                    BufferedImage img = new BufferedImage(markerImage.getWidth()+RULER_SIZE, markerImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();

                    g2d.drawImage(image, RULER_SIZE, 0, null);
                    g2d.drawImage(rulerLayer, 0, -RULER_SIZE, null);
                    g2d.dispose();
                    String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/markers/";

                    // save image to file
                    String fileName = chooseSaveFile(img, path);
                    // show saved dialog
                    if (fileName != null)
                        showSuccessDlg(fileName);
                }
            }
        };

        AbstractAction exportImgWdrawing = new AbstractAction(I18n.text("Image w/ drawing")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (markerImage != null && rulerLayer != null) {
                    BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();
                    String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/markers/";

                    // drawRect(layer.getGraphics(), lastMouseX, lastMouseY);

                    g2d.drawImage(image, 0, 0, null);
                    if (enableRectDraw)
                        drawRect(g2d, lastMouseX, lastMouseY);
                    if (enableCircleDraw)
                        drawCircle(g2d, lastMouseX, lastMouseY);
                    if (enableFreeDraw)
                        drawFree(g2d);

                    if (drawImageOverlay != null)
                        g2d.drawImage(drawImageOverlay, 0, 0, null);

                    g2d.dispose();

                    // save image to file
                    String fileName = chooseSaveFile(img, path);
                    // show saved dialog
                    if (fileName != null)
                        showSuccessDlg(fileName);
                }
            }
        };

        AbstractAction exportAll = new AbstractAction(I18n.text("All")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (markerImage != null && rulerLayer != null) {

                    BufferedImage img = new BufferedImage(markerImage.getWidth()+RULER_SIZE, markerImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();

                    g2d.drawImage(image, RULER_SIZE, 0, null);
                    g2d.drawImage(layer, 0, -RULER_SIZE, null);
                    g2d.drawImage(rulerLayer, 0, -RULER_SIZE, null);

                    g2d.dispose();
                    String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/mra/markers/";

                    // save image to file
                    String fileName = chooseSaveFile(img, path);
                    // show saved dialog
                    if (fileName != null)
                        showSuccessDlg(fileName);
                }
            }
        };

        AbstractAction previousMark = new AbstractAction(I18n.text("prevMark")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                parent.prevMark(selectMarkerRowIndex);
                statusBar.removeAll();
            }
        };

        AbstractAction nextMark = new AbstractAction(I18n.text("nextMark")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                parent.nextMark(selectMarkerRowIndex);
                statusBar.removeAll();
            }
        };
        AbstractAction zoomAction = new AbstractAction(I18n.text("Zoom")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (enableZoom) {
                    enableZoom = false;
                    zoomBtn.setSelected(false);
                }
                else {
                    enableCircleDraw = enableFreeDraw = enableRectDraw = false;
                    enableZoom = true;
                    zoomBtn.setSelected(true);
                }
            }
        };

        //add buttons to toolbar
        toolBar.add(saveBtn);
        toolBar.add(delBtn);
        toolBar.addSeparator(); 
        toolBar.add(rectDrawBtn);
        toolBar.add(circleDrawBtn);
        toolBar.add(freeDrawBtn);
        toolBar.add(clearDrawBtn);
        toolBar.addSeparator(); 
        toolBar.add(showGridBtn);
        toolBar.add(showRulerBtn);
        toolBar.add(zoomBtn);
        toolBar.addSeparator();
        toolBar.add(exportImgBtn);

        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(previousMarkBtn); 
        toolBar.add(nextMarkBtn);

        popup.add(new JMenuItem(exportImgOnly));
        popup.add(new JMenuItem(exportImageWruler));
        popup.add(new JMenuItem(exportImgWdrawing));
        popup.add(new JMenuItem(exportAll));

        //setup actions
        saveBtn.addActionListener(save);
        delBtn.addActionListener(del);
        rectDrawBtn.addActionListener(drawRect);
        circleDrawBtn.addActionListener(drawCircle);
        freeDrawBtn.addActionListener(drawFree);
        clearDrawBtn.addActionListener(clearDrawings);
        showGridBtn.addActionListener(showGrid);
        showRulerBtn.addActionListener(showRuler);
        zoomBtn.addActionListener(zoomAction);
        nextMarkBtn.addActionListener(nextMark);
        previousMarkBtn.addActionListener(previousMark);

        exportImgBtn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        zoomBtn.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupSlider.show(e.getComponent(), e.getX()+10, e.getY()+15);
                }
            }
        });
        //setup shortcuts - key bindings
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK), "save");
        panel.getActionMap().put("save", save);

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK), "delete");
        panel.getActionMap().put("delete", del);

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK), "showGrid");
        panel.getActionMap().put("showGrid", showGrid);

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK), "showRuler");
        panel.getActionMap().put("showRuler", showRuler);

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextMark");
        panel.getActionMap().put("nextMark", nextMark);

        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "prevMark");
        panel.getActionMap().put("prevMark", previousMark);

        toolBar.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextMark");
        toolBar.getActionMap().put("nextMark", nextMark);

        toolBar.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "prevMark");
        toolBar.getActionMap().put("prevMark", previousMark);

        getContentPane().add(toolBar, BorderLayout.PAGE_START);
    }

    private void clearLayer(){
        initialX = initialY = mouseX = mouseY = lastMouseX = lastMouseY = -1;
        Graphics2D g2d = (Graphics2D) layer.getGraphics();
        g2d.setBackground(new Color(100, 100, 255, 0));
        g2d.clearRect(0, 0, layer.getWidth(), layer.getHeight());
        g2d.dispose();
        markerImage.repaint();
    }

    public LogMarkerItem getOpenMarker(){
        return selectedMarker;
    }

    private void hidePhotoList() {
        listScroll.setVisible(false);
    }

    private void showPhotoList() {
        photoListModel.clear();
        for (String photo : selectedMarker.getPhotosPath()) {
            String p = photo.substring(photo.lastIndexOf("/")+1, photo.length());
            photoListModel.addElement(p);
        }
        listScroll.setVisible(true);
    }
}