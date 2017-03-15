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


import static pt.lsts.neptus.mra.markermanagement.MarkerManagement.MARKERS_REL_PATH;
import static pt.lsts.neptus.mra.markermanagement.MarkerManagement.PHOTOS_PATH_NAME;
import static pt.lsts.neptus.mra.markermanagement.MarkerManagement.SEPARATOR;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicButtonUI;

import org.apache.commons.io.FileUtils;
import org.imgscalr.Scalr;
import org.imgscalr.Scalr.Method;
import org.jdesktop.swingx.JXStatusBar;

import com.google.common.base.Splitter;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author Manuel R.
 * TODO: @Zoom : Crop from image file (better resolution)
 */
@SuppressWarnings("serial")
public class MarkerEdit extends JFrame {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 420;
    private static final int MAX_IMG_WIDTH = 600;
    private static final int MAX_IMG_HEIGHT = 420;
    private static final int RULER_SIZE = 15;
    private static final String DEFAULT_IMG_PATH = "/images/unknown.png";
    private String path;
    private int selectMarkerRowIndex = -1;
    private int mouseX, mouseY, initialX, initialY, lastMouseX, lastMouseY, zoomScale = 2;
    private boolean enableFreeDraw = false;
    private boolean enableRectDraw = false;
    private boolean enableCircleDraw = false;
    private boolean enableGrid = false;
    private boolean enableRuler = true;
    private boolean enableZoom = false;
    private boolean mouseDown = false;
    private boolean toDeleteDraw = false;
    private JPanel imgPanel, tagPanel, infoPanel, photosPanel; 
    private MarkerManagement parent;
    private AbstractAction save, del;
    private LogMarkerItem selectedMarker;
    private HashSet<String> tagList = new HashSet<>();
    private ArrayList<Point> pointsList = new ArrayList<>();
    private JLabel markerImage, nameLabelValue, timeStampValue, locationValue;
    private JLabel altitudeValue, depthValue, statusLabel;
    private JComboBox<String> classifValue;
    private JTextArea annotationValue;
    private JButton rectDrawBtn, circleDrawBtn, freeDrawBtn, exportImgBtn; 
    private JButton saveBtn, clearDrawBtn, showGridBtn, showRulerBtn;
    private JToggleButton zoomBtn;
    private JXStatusBar statusBar = new JXStatusBar();
    private JList<String> photoList;
    private DefaultListModel<String> photoListModel = new DefaultListModel<String>();
    private JScrollPane photoListScroll;
    private BufferedImage layer, rulerLayer, image, drawImageOverlay, zoomLayer;

    public MarkerEdit(MarkerManagement parent, Window window) {
        this.parent = parent;

        setupMenu();
        initialize();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initialize() {
        //Log location path
        path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + MARKERS_REL_PATH;

        setLocationRelativeTo(null);
        setBounds(getLocation().x, getLocation().y, WIDTH, HEIGHT);
        setIconImage(Toolkit.getDefaultToolkit().getImage(this.getClass().getResource("/images/menus/edit.png")));

        infoPanel = new JPanel();
        infoPanel.requestFocus();
        JPanel paintPanel = new JPanel();
        JPanel statusPanel = new JPanel(new BorderLayout());
        photosPanel = new JPanel(new BorderLayout());
        tagPanel = new JPanel();

        tagPanel.setLayout(new BoxLayout(tagPanel, BoxLayout.Y_AXIS));
        infoPanel.setBorder(new EmptyBorder(0, 1, 0, 0));
        infoPanel.setLayout(new MigLayout("", "[5.00][3px,grow][5.00]", "[3px][][][][][][][][][][][][][][][grow][grow]"));
        statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, Color.GRAY));
        photosPanel.setBorder(new TitledBorder(new EtchedBorder(), I18n.text("Photos")+":") );
        paintPanel.setLayout(new BorderLayout());

        JScrollPane annotationScrollPane = new JScrollPane();
        JScrollPane mainScrollPane = new JScrollPane(infoPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, 
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        photoListScroll = new JScrollPane();

        JLabel nameLabel = new JLabel(I18n.text("Label:"));
        Font f = nameLabel.getFont(); // bold 
        nameLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); 
        nameLabelValue = new JLabel("LABEL_NAME");
        JLabel timeStampLabel = new JLabel("Date:");
        timeStampLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); 
        JLabel locationLabel = new JLabel("Location:");
        locationLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); 
        JLabel altitudeLabel = new JLabel("Altitude:");
        altitudeLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); 
        timeStampValue = new JLabel("LABEL_DATE");
        altitudeValue = new JLabel("LABEL_ALT");
        JLabel depthLabel = new JLabel("Depth:");
        depthLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); 
        locationValue  = new JLabel("LABEL_LOCATION");
        depthValue = new JLabel("LABEL_DEPTH");
        JLabel classifLabel = new JLabel("Classification:");
        classifLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); 
        JLabel annotationLabel = new JLabel("Annotation:");
        annotationLabel.setFont(f.deriveFont(f.getStyle() ^ Font.BOLD)); 

        statusLabel = new JLabel();
        statusLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));

        annotationValue = new JTextArea();
        classifValue = new JComboBox<String>();
        photoList = new JList<String>();

        annotationValue.setColumns(10);
        annotationValue.setTabSize(5);
        annotationValue.setText(I18n.text("<Annotations here>"));
        annotationValue.setLineWrap(true); //Auto down line if the line is too long
        annotationValue.setWrapStyleWord(true); //Auto set up the style of words
        annotationValue.setRows(3);

        annotationScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        annotationScrollPane.setViewportView(annotationValue);

        classifValue.setBackground(Color.WHITE);
        classifValue.setModel(new DefaultComboBoxModel(Classification.values()));

        photoList.setLayoutOrientation(JList.VERTICAL);
        photoList.setVisibleRowCount(3);
        photoList.setModel(photoListModel);

        photoListScroll.setViewportView(photoList);
        photoListScroll.setVisible(false);

        tagPanel.setBorder(new TitledBorder(new EtchedBorder(), I18n.text("Tags")+":"));

        infoPanel.add(nameLabel, "cell 1 1");
        infoPanel.add(nameLabelValue, "cell 1 2,growx");
        infoPanel.add(timeStampLabel, "cell 1 3");
        infoPanel.add(timeStampValue, "cell 1 4,growx");
        infoPanel.add(locationLabel, "cell 1 5");
        infoPanel.add(locationValue, "cell 1 6,growx");
        infoPanel.add(altitudeLabel, "cell 1 7");
        infoPanel.add(altitudeValue, "cell 1 8,growx");
        infoPanel.add(depthLabel, "cell 1 9");
        infoPanel.add(depthValue, "cell 1 10,growx");
        infoPanel.add(classifLabel, "cell 1 11");
        infoPanel.add(classifValue, "cell 1 12,growx");
        infoPanel.add(annotationLabel, "cell 1 13");
        infoPanel.add(annotationScrollPane, "cell 1 14,growx");
        photosPanel.add(photoListScroll, BorderLayout.CENTER);
        infoPanel.add(photosPanel, "cell 1 15,grow");
        infoPanel.add(tagPanel, "cell 1 16,grow");
        statusPanel.add(statusBar);

        getContentPane().add(mainScrollPane, BorderLayout.EAST);
        getContentPane().add(paintPanel, BorderLayout.CENTER);
        getContentPane().add(statusPanel, BorderLayout.SOUTH);

        markerImage = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if (image != null) {
                    Graphics2D rg2d = null;
                    if (layer!=null) {
                        Graphics2D lg2d = (Graphics2D) layer.getGraphics();
                        lg2d.setBackground(new Color(100, 100, 255, 0));
                        lg2d.clearRect(0, 0, layer.getWidth(), layer.getHeight());

                        rg2d = (Graphics2D) rulerLayer.getGraphics();
                        rg2d.setBackground(new Color(100, 100, 255, 0));
                        rg2d.clearRect(0, 0, rulerLayer.getWidth(), rulerLayer.getHeight());

                        Graphics2D zg2d = (Graphics2D) zoomLayer.getGraphics();
                        zg2d.setBackground(new Color(100, 100, 255, 0));
                        zg2d.clearRect(0, 0, zoomLayer.getWidth(), zoomLayer.getHeight());
                    }

                    g.drawImage(image, RULER_SIZE+1, RULER_SIZE+1, null);

                    //horizontal line
                    g.drawLine(RULER_SIZE, image.getHeight()+RULER_SIZE, image.getWidth()+RULER_SIZE, image.getHeight()+RULER_SIZE);

                    //vertical line
                    g.drawLine(RULER_SIZE, RULER_SIZE+1, RULER_SIZE, image.getHeight()+RULER_SIZE);

                    if (enableRectDraw && layer != null) 
                        drawRect(layer.getGraphics(), 0, 0);

                    if (enableFreeDraw && layer != null)
                        drawFree(layer.getGraphics());

                    if (enableCircleDraw && layer != null)
                        drawCircle(layer.getGraphics(), 0, 0);

                    if (enableZoom && mouseDown && layer != null)
                        zoom(layer.getGraphics(), zoomLayer.getGraphics());

                    if (layer != null)
                        g.drawImage(layer, RULER_SIZE+1, RULER_SIZE+1, null);

                    if (drawImageOverlay != null)
                        g.drawImage(drawImageOverlay, RULER_SIZE+1, RULER_SIZE+1, null);

                    //Draw ruler
                    if (enableRuler && rulerLayer != null && rg2d != null) {
                        drawRuler(rg2d);
                        g.drawImage(rulerLayer, 0, 0, null);
                    }

                    if (zoomLayer != null)
                        g.drawImage(zoomLayer, RULER_SIZE+1, RULER_SIZE+1, null);
                }
            }
        };

        markerImage.setHorizontalAlignment(SwingConstants.CENTER);
        markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource(DEFAULT_IMG_PATH)));
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
                if (image == null)
                    return;

                mouseX = e.getX();
                mouseY = e.getY();
                if ((mouseX > RULER_SIZE && mouseX < image.getWidth()+RULER_SIZE && mouseY > 0) &&
                        (mouseY < image.getHeight())) {

                    pointsList.add(new Point(mouseX-RULER_SIZE-1, mouseY-RULER_SIZE-1));
                }
                markerImage.repaint();
            }
        });

        imgPanel = new JPanel();
        imgPanel.setBackground(Color.GRAY);
        imgPanel.add(markerImage);

        paintPanel.add(new JScrollPane(imgPanel), BorderLayout.CENTER);

        setupPhotoMenu();

        Timer SimpleTimer = new Timer(3500, new ActionListener(){
            @Override
            public void actionPerformed(ActionEvent e) {
                statusBar.removeAll();
                statusBar.updateUI();
                statusBar.repaint();
            }
        });
        SimpleTimer.start();
    }

    private void setupPhotoMenu() {
        JPopupMenu photoPopupMenu = new JPopupMenu(); 

        AbstractAction delAction = new AbstractAction(I18n.text("Remove photo"), null) {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                String toRemove = (String) photoList.getSelectedValue();
                if (toRemove == null)
                    return;

                removePhoto(toRemove);
            }
        };

        AbstractAction selectAction = new AbstractAction(I18n.text("Use as main photo"), null) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String mainPhoto = (String) photoList.getSelectedValue();
                if (mainPhoto == null)
                    return;

                selectMainPhoto(mainPhoto);
            }
        };

        AbstractAction openAction = new AbstractAction(I18n.text("Open photo"), null) {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                String toOpen = (String) photoList.getSelectedValue();
                if (toOpen == null)
                    return;

                openPhoto(toOpen);
            }

            private void openPhoto(String toOpen) {
                String path = MARKERS_REL_PATH.concat(PHOTOS_PATH_NAME)
                        .concat(selectedMarker.getLabel())
                        .concat(SEPARATOR)
                        .concat(toOpen);
                String absPath = parent.mraPanel.getSource().getFile("Data.lsf").getParent();
                File fileToOpen = new File(absPath.concat(path));

                if (fileToOpen != null) {

                    try {
                        Desktop.getDesktop().open(fileToOpen);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        if (selectedMarker != null) {
            //only add this option if no SS image available
            if (selectedMarker.getSidescanImgPath() == null)
                photoPopupMenu.add(selectAction);
        }
        photoPopupMenu.add(openAction);
        photoPopupMenu.add(delAction);

        photoList.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)  {check(e);}
            public void mouseReleased(MouseEvent e) {check(e);}

            public void check(MouseEvent e) {
                if (e.isPopupTrigger()) { //if the event shows the menu
                    photoList.setSelectedIndex(photoList.locationToIndex(e.getPoint())); //select the item
                    photoPopupMenu.show(photoList, e.getX(), e.getY()); //and show the menu
                }
            }
        });

    }

    private void addPhoto() {
        String[] imgExtensions =  { "bmp", "jpg", "jpeg", "wbmp", "png", "gif" } ;

        JFileChooser fileChooser = GuiUtils.getFileChooser(path, 
                I18n.text("Image Files"), imgExtensions);

        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(true);
        int status = fileChooser.showOpenDialog(this);

        if (status == JFileChooser.APPROVE_OPTION) {
            File[] selectedFile = fileChooser.getSelectedFiles();
            if (selectedFile != null) {
                String folder = createFolder(path);
                for (File s : selectedFile) {
                    if (FileUtil.copyFile(s.getPath(), folder + SEPARATOR + s.getName())) {
                        addToList(s.getName(), MARKERS_REL_PATH.concat(PHOTOS_PATH_NAME).concat(selectedMarker.getLabel()).concat(SEPARATOR));
                    }
                }
                showPhotoList();
            }
        }
    }

    private void addTag() {
        String tagString = JOptionPane.showInputDialog(this,
                I18n.text("Please enter tag(s):") + "\n" + I18n.text("(max 20 characters each and separated by comma)"), I18n.text("Add Tag(s)"), JOptionPane.OK_CANCEL_OPTION);

        if (tagString == null)
            return;

        if (tagString.isEmpty())
            return;

        Iterable<String> tagIt = Splitter.on(',')
                .trimResults()
                .omitEmptyStrings()
                .split(tagString); //split by comma's

        for (String tag : tagIt) {
            if (tag.length() <= 20) { // Limit to 20 chars each tag
                tagList.add(tag);
                //update panel
                ButtonCloseComponent e = new ButtonCloseComponent(tag);
                e.getTagButton().addActionListener(new ActionListener() {

                    @Override
                    public void actionPerformed(ActionEvent x) {
                        selectedMarker.removeTag(tag);
                        tagPanel.remove(e);
                        tagPanel.revalidate();
                        tagPanel.repaint();
                        infoPanel.repaint();
                    }
                });
                tagPanel.add(e);
            }
        }

        tagPanel.revalidate();
        tagPanel.repaint();
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    private boolean addToList(String name, String path) {
        for (String photo : selectedMarker.getPhotosPath()) {
            String picName = photo.substring(photo.lastIndexOf(SEPARATOR)+1, photo.length());
            if (picName.equals(name))
                return false;
        }

        photoListModel.addElement(name);
        selectedMarker.getPhotosPath().add(path.concat(name));
        return true;
    }

    private String createFolder(String path) {
        File folder = new File(path.concat(SEPARATOR)
                .concat(PHOTOS_PATH_NAME));
        String finalPath = folder.getPath().concat(SEPARATOR)
                .concat(selectedMarker.getLabel());
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
        String remPath = MARKERS_REL_PATH.concat(PHOTOS_PATH_NAME)
                .concat(selectedMarker.getLabel())
                .concat(SEPARATOR)
                .concat(toRemove);
        String absPath = parent.mraPanel.getSource().getFile("Data.lsf").getParent();
        File toRemoveFile = new File(absPath.concat(remPath));
        File photosPath = new File(absPath.concat(MARKERS_REL_PATH).concat(PHOTOS_PATH_NAME));
        FileUtils.deleteQuietly(toRemoveFile);

        selectedMarker.getPhotosPath().remove(remPath);
        photoListModel.removeElement(toRemove);

        File markerFile = new File(absPath.concat(MARKERS_REL_PATH)
                .concat(PHOTOS_PATH_NAME)
                .concat(selectedMarker.getLabel())
                .concat(SEPARATOR));
        try {
            //Delete folder if it's empty
            if (isDirEmpty(markerFile.toPath()))
                FileUtils.deleteDirectory(markerFile);

            //Delete PHOTOS_REL_PATH folder if it's empty
            if (isDirEmpty(photosPath.toPath()))
                FileUtils.deleteDirectory(photosPath);
        }
        catch (IOException e) {
            //do nothing
        }

        if (selectedMarker.getPhotosPath().isEmpty())
            hidePhotoList();
    }

    private void selectMainPhoto(String mainPhoto) {
        selectedMarker.setMainPhoto(mainPhoto);
        parent.updateLogMarker(selectedMarker, selectMarkerRowIndex);
    }

    private static boolean isDirEmpty(final Path directory) throws IOException {
        try(DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    public void updateStatusBar(String msg){
        statusLabel.setText(msg);
        statusBar.removeAll();
        statusBar.add(statusLabel);
        statusBar.updateUI();
        statusBar.repaint();
        repaint();

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

        g2d.setColor(Color.BLACK);

        //Horizontal ruler (range)

        double step = zoomRangeStep * (image.getWidth()+margin) / range;
        double r = zoomRangeStep;
        int c = margin + (int) step;
        g2d.setColor(Color.WHITE);

        for (; c<=image.getWidth()+margin; c += step , r += zoomRangeStep) {
            int length = (int)(Math.log10(r)+1);
            g2d.setColor(Color.WHITE);
            g2d.drawLine(c, y, c, y-lineWith);
            if (enableGrid)
                g2d.drawLine(c, RULER_SIZE+1, c, y);

            margin = length == 1 ? 10 : 13 + ((length-1) * 5);

            g2d.drawString("" + (int) r, c - margin, y-1);
            g2d.setColor(Color.BLACK);
            g2d.drawLine(c, y, c, y+lineWith);

        }

        /*
        //Vertical ruler (height)

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
         */
    }

    public void loadMarker(LogMarkerItem log, int rowIndex) {
        statusBar.removeAll();
        tagPanel.removeAll();
        toDeleteDraw = false;
        selectedMarker = log;
        selectMarkerRowIndex = rowIndex;
        int prefWidth = 265;
        int prefHeight = 110;

        if (selectedMarker.getSidescanImgPath() != null ) {
            try {
                String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent();
                File f = new File(path + selectedMarker.getSidescanImgPath());
                image = ImageIO.read(f);

                int width = image.getWidth();
                int height = image.getHeight();

                if (width > MAX_IMG_WIDTH)
                    width = width / 2;

                if (height > MAX_IMG_HEIGHT)
                    height = height / 2;

                image = Scalr.resize(image, Method.ULTRA_QUALITY, width, height);

                markerImage.setIcon(null);
                markerImage.repaint();
                markerImage.setPreferredSize(new Dimension(width + RULER_SIZE + 10, height + RULER_SIZE + 10));
                setLocation(parent.getwindowLocation());

                if (selectedMarker.getDrawImgPath() != null && !selectedMarker.getDrawImgPath().toString().equals("N/A")) {
                    File fDraw = new File(path + selectedMarker.getDrawImgPath());
                    drawImageOverlay = ImageIO.read(fDraw);
                }
                else
                    drawImageOverlay = null;

                layer = ImageUtils.createCompatibleImage(image.getWidth(), 
                        image.getHeight(), Transparency.TRANSLUCENT);

                rulerLayer = ImageUtils.createCompatibleImage(markerImage.getPreferredSize().width, 
                        markerImage.getPreferredSize().height, Transparency.TRANSLUCENT);
                zoomLayer = ImageUtils.createCompatibleImage(image.getWidth(), 
                        image.getHeight(), Transparency.TRANSLUCENT);

                clearLayer();
                imgPanel.revalidate();
                imgPanel.repaint();
                setBounds(getLocation().x, getLocation().y, WIDTH, HEIGHT);

            } catch (IOException e) {
                NeptusLog.pub().error(I18n.text("Error reading image file for marker: ")+ selectedMarker.getLabel());
                chooseDisplayImage(prefWidth, prefHeight);
            }
        }
        else
            chooseDisplayImage(prefWidth, prefHeight);

        //disable all buttons except save, del, add photos, add tags
        if (image == null || layer == null) {
            rectDrawBtn.setEnabled(false);
            circleDrawBtn.setEnabled(false);
            freeDrawBtn.setEnabled(false);
            clearDrawBtn.setEnabled(false);
            exportImgBtn.setEnabled(false);
            zoomBtn.setEnabled(false);
            showGridBtn.setEnabled(false);
            showRulerBtn.setEnabled(false);
        } else {
            rectDrawBtn.setEnabled(true);
            circleDrawBtn.setEnabled(true);
            freeDrawBtn.setEnabled(true);
            exportImgBtn.setEnabled(true);
            clearDrawBtn.setEnabled(true);
            zoomBtn.setEnabled(true);
            showGridBtn.setEnabled(true);
            showRulerBtn.setEnabled(true);
        }

        enableCircleDraw = enableFreeDraw = enableRectDraw = false;
        setInfo();

        if (selectedMarker.getPhotosPath().isEmpty())
            hidePhotoList();
        else
            showPhotoList();

        //update photoList popup menu
        for (MouseListener e : photoList.getMouseListeners())
            photoList.removeMouseListener(e);

        setupPhotoMenu();
    }

    /**
     * @param prefWidth
     * @param prefHeight
     */
    private void chooseDisplayImage(int prefWidth, int prefHeight) {
        image = null;
        layer = null;

        //if there's a main photo, use it
        if (selectedMarker.getMainPhoto() != null) {
            try {
                String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent();
                File f = new File(path.concat(MARKERS_REL_PATH)
                        .concat(PHOTOS_PATH_NAME)
                        .concat(selectedMarker.getLabel())
                        .concat(SEPARATOR)
                        .concat(selectedMarker.getMainPhoto()));

                image = ImageIO.read(f);

                int width = image.getWidth();
                int height = image.getHeight();

                if (width > MAX_IMG_WIDTH)
                    width = width / 2;

                if (height > MAX_IMG_HEIGHT)
                    height = height / 2;

                image = Scalr.resize(image, Method.ULTRA_QUALITY, width, height);

                markerImage.setIcon(null);
                markerImage.repaint();
                markerImage.setPreferredSize(new Dimension(width + RULER_SIZE + 10, height + RULER_SIZE + 10));
                setLocation(parent.getwindowLocation());
                drawImageOverlay = null;
                imgPanel.revalidate();
                imgPanel.repaint();
                setBounds(getLocation().x, getLocation().y, WIDTH, HEIGHT);

            }
            catch (IOException e) {
                markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource(DEFAULT_IMG_PATH)));
                markerImage.setPreferredSize(new Dimension(markerImage.getIcon().getIconWidth(), markerImage.getIcon().getIconHeight()));
                markerImage.repaint();
                imgPanel.revalidate();
                imgPanel.repaint();
                setBounds(getLocation().x, getLocation().y, WIDTH, HEIGHT);
                setLocation(parent.getwindowLocation());
            }
        }
        else {
            markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource(DEFAULT_IMG_PATH)));
            markerImage.setPreferredSize(new Dimension(markerImage.getIcon().getIconWidth(), markerImage.getIcon().getIconHeight()));
            markerImage.repaint();
            imgPanel.revalidate();
            imgPanel.repaint();
            setBounds(getLocation().x, getLocation().y, WIDTH, HEIGHT);
            setLocation(parent.getwindowLocation());
        }

    }

    private void setInfo() {
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

        for (String tag : selectedMarker.getTags()) {
            ButtonCloseComponent bt = new ButtonCloseComponent(tag);
            tagPanel.add(bt);

            bt.getTagButton().addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    selectedMarker.removeTag(tag);
                    tagPanel.remove(bt);
                    tagPanel.revalidate();
                    tagPanel.repaint();
                    infoPanel.repaint();
                }
            });

        }

        tagPanel.setPreferredSize(tagPanel.getPreferredSize());

        infoPanel.repaint();

        VehicleType veh = LogUtils.getVehicle(parent.mraPanel.getSource());
        String vehicle = (veh != null ? " | "+veh.getName() : "");
        setTitle(I18n.text("Marker: ") + nameLabelValue.getText() + " | " + timeStampValue.getText() + " | " + parent.mraPanel.getSource().name() + vehicle);

    }

    private void showSuccessDlg(String path) {
        if (!path.endsWith(".png"))
            path = path + ".png";

        GuiUtils.showInfoPopup(I18n.text("Success"), I18n.text("Image exported to: ")+path);
        updateStatusBar(I18n.text("Image") + " '" + path + "' "+ I18n.text("exported successfully..."));
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

        saveBtn = createBtn("images/menus/save.png", I18n.text("Save"));
        JButton delBtn = createBtn("images/menus/editdelete.png", I18n.text("Delete"));
        exportImgBtn = createBtn("images/menus/export.png", I18n.text("Export"));
        rectDrawBtn = createBtn("images/menus/rectdraw.png", I18n.text("Draw rectangle"));
        circleDrawBtn = createBtn("images/menus/circledraw.png", I18n.text("Draw circle"));
        freeDrawBtn = createBtn("images/menus/freedraw.png", I18n.text("Draw"));
        clearDrawBtn = createBtn("images/menus/clear.png", I18n.text("Clear all"));
        showGridBtn = createBtn("images/menus/grid.png", I18n.text("Show grid"));
        showRulerBtn = createBtn("images/menus/ruler.png", I18n.text("Show ruler"));
        JButton addPhotoBtn = createBtn("images/menus/attach.png", I18n.text("Add Photo(s)"));
        JButton addTagBtn = createBtn("images/menus/comment.png", I18n.text("Add Tag(s)"));
        JButton previousMarkBtn = createBtn("images/menus/previous.png", I18n.text("Previous Mark"));
        JButton nextMarkBtn = createBtn("images/menus/next.png", I18n.text("Next Mark"));

        zoomBtn = createToggleBtn("images/menus/zoom_btn.png", I18n.text("Zoom"));

        save = new AbstractAction(I18n.text("Save"), ImageUtils.getIcon("images/menus/save.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                Classification classif = (Classification) classifValue.getSelectedItem();
                String annotation = annotationValue.getText();

                //begin saving draw image
                BufferedImage img = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = img.createGraphics();

                if (enableRectDraw) 
                    drawRect(g2d, lastMouseX, lastMouseY);

                if (enableCircleDraw) 
                    drawCircle(g2d, lastMouseX, lastMouseY);

                if (enableFreeDraw) 
                    drawFree(g2d);

                if (drawImageOverlay != null)
                    g2d.drawImage(drawImageOverlay, 0, 0, null);

                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        File drawFile = new File(path, selectedMarker.getLabel() + "_draw.png");
                        boolean updateDrawPath = false;
                        if (!isTransparent(img)) {
                            try {
                                ImageIO.write(img, "PNG", drawFile);
                            } catch (IOException ie) {
                                NeptusLog.pub().error(I18n.text("Error writing image to file..."));
                                updateStatusBar("Error writing image to file...");
                            }

                            updateDrawPath = true;
                        }
                        g2d.dispose();

                        String relPath = MARKERS_REL_PATH + selectedMarker.getLabel() +"_draw.png";

                        if (updateDrawPath)
                            selectedMarker.setDrawImgPath(relPath);

                        selectedMarker.setClassification(classif);
                        selectedMarker.setAnnotation(annotation);
                        selectedMarker.setTags(tagList);

                        if (toDeleteDraw) {
                            parent.deleteImage(drawFile.toString());
                            selectedMarker.setDrawImgPath("N/A");
                        }

                        parent.updateLogMarker(selectedMarker, selectMarkerRowIndex);
                        markerImage.repaint();
                        updateStatusBar("Saving completed...");

                        return null;
                    }
                };
                worker.execute();

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
                enableGrid = !enableGrid;

                if (enableGrid)
                    updateStatusBar("Showing Grid...");
                else
                    updateStatusBar("Hiding Grid...");

                markerImage.repaint();
            }
        };
        AbstractAction showRuler = new AbstractAction(I18n.text("Show ruler"), ImageUtils.getIcon("images/menus/ruler.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                enableRuler = !enableRuler;

                if (enableRuler) 
                    updateStatusBar("Showing ruler...");
                else
                    updateStatusBar("Hiding Ruler...");

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
                updateStatusBar("Clearing all drawings...");
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

        AbstractAction addPhotoAction = new AbstractAction(I18n.text("Add Photo")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                addPhoto();
            }
        };

        AbstractAction addTagAction = new AbstractAction(I18n.text("Add Tag")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                addTag();
            }
        };

        JSeparator separator1 = new JSeparator(){
            @Override
            public Dimension getMaximumSize(){
                return new Dimension(5, 25);
            }
        };
        separator1.setOrientation(JSeparator.VERTICAL);

        JSeparator separator2 = new JSeparator(){
            @Override
            public Dimension getMaximumSize(){
                return new Dimension(5, 25);
            }
        };
        separator2.setOrientation(JSeparator.VERTICAL);

        JSeparator separator3 = new JSeparator(){
            @Override
            public Dimension getMaximumSize(){
                return new Dimension(5, 25);
            }
        };
        separator3.setOrientation(JSeparator.VERTICAL);

        //add buttons to toolbar
        toolBar.add(saveBtn);
        toolBar.add(delBtn);
        toolBar.add(exportImgBtn);
        toolBar.add(separator1);
        toolBar.add(rectDrawBtn);
        toolBar.add(circleDrawBtn);
        toolBar.add(freeDrawBtn);
        toolBar.add(clearDrawBtn);
        toolBar.add(separator2); 
        toolBar.add(showGridBtn);
        toolBar.add(showRulerBtn);
        toolBar.add(zoomBtn);
        toolBar.add(separator3);
        toolBar.add(addPhotoBtn);
        toolBar.add(addTagBtn);
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
        addPhotoBtn.addActionListener(addPhotoAction);
        addTagBtn.addActionListener(addTagAction);
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
        toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK), "save");
        toolBar.getActionMap().put("save", save);

        toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, InputEvent.CTRL_MASK), "delete");
        toolBar.getActionMap().put("delete", del);

        toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK), "showGrid");
        toolBar.getActionMap().put("showGrid", showGrid);

        toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.CTRL_MASK), "showRuler");
        toolBar.getActionMap().put("showRuler", showRuler);

        toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextMark");
        toolBar.getActionMap().put("nextMark", nextMark);

        toolBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "prevMark");
        toolBar.getActionMap().put("prevMark", previousMark);

        toolBar.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "nextMark");
        toolBar.getActionMap().put("nextMark", nextMark);

        toolBar.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "prevMark");
        toolBar.getActionMap().put("prevMark", previousMark);

        getContentPane().add(toolBar, BorderLayout.PAGE_START);
    }

    private boolean isTransparent(BufferedImage img) {
        final int xmin = img.getMinX();
        final int ymin = img.getMinY();

        final int ymax = ymin + img.getHeight();
        final int xmax = xmin + img.getWidth();

        for (int i = xmin;i<xmax;i++)
        {
            for (int j = ymin;j<ymax;j++)
            {
                int pixel = img.getRGB(i, j);

                Color mycolor = new Color(pixel);
                if (mycolor.equals(Color.WHITE))
                    return false;
            }
        }
        return true;
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
        infoPanel.remove(photosPanel);
        infoPanel.revalidate();
        infoPanel.repaint();
    }

    private void showPhotoList() {
        photoListModel.clear();
        photoListScroll.setVisible(true);
        for (String photo : selectedMarker.getPhotosPath()) {
            String p = photo.substring(photo.lastIndexOf(SEPARATOR)+1, photo.length());
            photoListModel.addElement(p);
        }
        infoPanel.add(photosPanel, "cell 1 15,grow");
        infoPanel.revalidate();
        infoPanel.repaint();;
    }

    /**
     * Contains a JLabel to show the text and 
     * a JButton with user defined action 
     */
    public class ButtonCloseComponent extends JPanel {
        private static final long serialVersionUID = -6269739636016471305L;
        private JButton button = null;

        public ButtonCloseComponent(String text) {
            //unset default FlowLayout' gaps
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setOpaque(false);

            //make JLabel read titles from JTabbedPane
            JLabel label = new JLabel(text);
            add(label);
            //add more space between the label and the button
            label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
            //tab button
            button = new TagButton();
            add(button);
            //add more space to the top of the component
            setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        }

        public JButton getTagButton() {
            return button;
        }

        private class TagButton extends JButton {
            private static final long serialVersionUID = 1699308100643428110L;

            public TagButton() {
                int size = 17;
                setPreferredSize(new Dimension(size, size));
                setToolTipText("Remove tag");
                setUI(new BasicButtonUI());
                setContentAreaFilled(false);
                setFocusable(false);
                setBorder(BorderFactory.createEtchedBorder());
                setBorderPainted(false);
                addMouseListener(buttonMouseListener);
                setRolloverEnabled(true);
            }

            //we don't want to update UI for this button
            public void updateUI() {
            }

            //paint the cross
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                //shift the image for pressed buttons
                if (getModel().isPressed()) {
                    g2.translate(1, 1);
                }
                g2.setStroke(new BasicStroke(2));
                g2.setColor(Color.BLACK);
                if (getModel().isRollover()) {
                    g2.setColor(Color.RED);
                }
                int delta = 6;
                g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
                g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
                g2.dispose();
            }
        }

        private final MouseListener buttonMouseListener = new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                Component component = e.getComponent();
                if (component instanceof AbstractButton) {
                    AbstractButton button = (AbstractButton) component;
                    button.setBorderPainted(true);
                }
            }

            public void mouseExited(MouseEvent e) {
                Component component = e.getComponent();
                if (component instanceof AbstractButton) {
                    AbstractButton button = (AbstractButton) component;
                    button.setBorderPainted(false);
                }
            }
        };
    }
}