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
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.markermanagement.LogMarkerItem.Classification;
import pt.lsts.neptus.util.ImageUtils;


/**
 * @author Manuel R.
 *
 */
@SuppressWarnings("serial")
public class MarkerEdit extends JFrame {

    private static final long serialVersionUID = 1613149353413851878L;
    private final int RULER_SIZE = 15;
    private JMenuBar menuBar;

    private MarkerManagement parent;
    private AbstractAction save, del, exit, freeDraw, rectDraw, circleDraw, clearDraw, exportImage;
    private JPopupMenu drawPopupMenu;
    private LogMarkerItem selectedMarker;
    private int selectMarkerRowIndex = -1;

    private JLabel markerImage;
    private JLabel nameLabelValue;
    private JLabel timeStampValue;
    private JLabel locationValue;
    private JLabel altitudeValue;
    private JComboBox<String> classifValue;
    private JTextArea annotationValue;
    private int mouseX, mouseY, initialX, initialY;
    private boolean enableFreeDraw = false;
    private boolean enableRectDraw = false;
    private boolean enableCircleDraw = false;
    private BufferedImage layer;
    private BufferedImage image;

    private ArrayList<Point> pointsList = new ArrayList<>();

    public MarkerEdit(MarkerManagement parent) {
        setIconImage(Toolkit.getDefaultToolkit().getImage(MarkerEdit.class.getResource("/images/menus/edit.png")));
        this.parent = parent;

        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setBounds(100, 100, 590, 395);

        setupFileMenu();

        initialize();

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void initialize() {

        JPanel panel = new JPanel();
        getContentPane().add(panel, BorderLayout.CENTER);
        panel.setLayout(new MigLayout("", "[][][][][grow][][][][grow]", "[][][][][][][grow][][grow]"));

        markerImage = new JLabel() { 
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D lg2d = (Graphics2D) layer.getGraphics();
                lg2d.setBackground(new Color(100, 100, 255, 0));
                lg2d.clearRect(0, 0, layer.getWidth(), layer.getHeight());

                if (image!=null) {
                    g.drawImage(image, RULER_SIZE+1, RULER_SIZE+1, null);

                    drawZoomRuler(g);

                    if (enableRectDraw) 
                        drawRect(g);

                    if (enableFreeDraw)
                        drawFree(g);

                    if (enableCircleDraw)
                        drawCircle(g);

                    g.drawImage(layer, 16, 0, null);
                }
            }
        };

        markerImage.setHorizontalAlignment(SwingConstants.CENTER);
        markerImage.setIcon(new ImageIcon(MarkerEdit.class.getResource("/images/unknown.png")));

        setupDrawPopup();

        markerImage.setComponentPopupMenu(drawPopupMenu);

        markerImage.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
                pointsList.clear();
                //((JLabel) e.getSource()).repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();         

                if (e.getButton() == MouseEvent.BUTTON1) {
                    initialX = mouseX;
                    initialY = mouseY;
                    if (enableFreeDraw) 
                        pointsList.add(new Point(mouseX, mouseY));
                    // ((JLabel) e.getSource()).repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                mouseX = mouseY = -1;                
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
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
                if ((mouseX > RULER_SIZE && mouseX < markerImage.getPreferredSize().width && mouseY > 0) &&
                        (mouseY < markerImage.getPreferredSize().height-RULER_SIZE )) {

                    pointsList.add(new Point(mouseX, mouseY));
                    ((JLabel) e.getSource()).repaint();

                }
            }
        });
        panel.add(markerImage, "cell 0 0 7 7,alignx left,aligny top");

        layer = ImageUtils.createCompatibleImage(markerImage.getPreferredSize().width-RULER_SIZE, 
                markerImage.getPreferredSize().height-RULER_SIZE, Transparency.TRANSLUCENT);

        JLabel nameLabel = new JLabel("Label:");
        panel.add(nameLabel, "cell 7 0,alignx left");

        nameLabelValue = new JLabel();
        nameLabelValue.setBackground(Color.WHITE);
        nameLabelValue.setText("MARKER_LABEL");

        panel.add(nameLabelValue, "cell 8 0,alignx left");


        JLabel timeStampLabel = new JLabel("Timestamp:");
        panel.add(timeStampLabel, "cell 7 1,alignx left");

        timeStampValue = new JLabel("TS");
        panel.add(timeStampValue, "cell 8 1,alignx left");

        JLabel locationLabel = new JLabel("Location:");
        panel.add(locationLabel, "cell 7 2,alignx left");

        locationValue = new JLabel("LOCATION");
        panel.add(locationValue, "cell 8 2,alignx left");

        JLabel altitudeLabel = new JLabel("Altitude:");
        panel.add(altitudeLabel, "cell 7 3,alignx left");

        altitudeValue = new JLabel("ALTITUDE");
        panel.add(altitudeValue, "cell 8 3,alignx left");

        JLabel classifLabel = new JLabel("Classification:");
        panel.add(classifLabel, "cell 7 4,alignx trailing");

        classifValue = new JComboBox<>();
        classifValue.setBackground(Color.WHITE);
        classifValue.setModel(new DefaultComboBoxModel(Classification.values()));
        panel.add(classifValue, "cell 8 4,alignx left");

        JLabel annotationLabel = new JLabel("Annotation:");
        panel.add(annotationLabel, "cell 7 5");

        JScrollPane scrollPane = new JScrollPane();
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        panel.add(scrollPane, "cell 7 6 2 1,grow");

        annotationValue = new JTextArea();
        annotationValue.setText("<Your annotations here>");
        annotationValue.setLineWrap(true); //Auto down line if the line is too long
        annotationValue.setWrapStyleWord(true); //Auto set up the style of words
        annotationValue.setRows(8);
        scrollPane.setViewportView(annotationValue);
    }

    private void drawRect(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        int x = Math.min(initialX, mouseX);
        int y = Math.min(initialY, mouseY);
        int w = Math.max(initialX, mouseX) - Math.min(initialX, mouseX);
        int h = Math.max(initialY, mouseY) - Math.min(initialY, mouseY);

        System.out.println("x: "+x + " y: "+ y + " w: " + w + " h: "+ h);
        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, w, h);

    }


    private void drawFree(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;

        for (Point p : pointsList) {
            g2.setColor(Color.WHITE);
            g2.drawLine(p.x, p.y, p.x, p.y);
        }
    }

    private void drawCircle(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(Color.WHITE);
        int x = Math.min(initialX, mouseX);
        int y = Math.min(initialY, mouseY);

        int w = Math.max(initialX, mouseX) - Math.min(initialX, mouseX);
        int h = Math.max(initialY, mouseY) - Math.min(initialY, mouseY);

        g2.drawOval(x,y,w,h);
    }

    private void drawZoomRuler(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;

        int fontSize = 11;
        int margin = 8;
        int lineWith = 9;
        g2d.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        g2d.setColor(Color.BLACK);

        //draw zero
        g2d.drawString("0", margin, image.getHeight()+RULER_SIZE+10);

        double range = selectedMarker.getRange();
        float zoomRangeStep = 1;
        if (range > 10.0 && range < 30.0)
            zoomRangeStep = 2;
        else {
            if (range > 30.0)
                zoomRangeStep = 5;
        }
        //System.out.println("Range "+ range);

        //horizontal black rectangle
        //g2d.setColor(new Color(.3f, .4f, .5f, .6f));
        g2d.setColor(new Color(.5f, .6f, .7f,.8f));
        Rectangle horizRect = new Rectangle(RULER_SIZE, image.getHeight()+3, image.getWidth()+1, 12);
        g2d.fill(horizRect);

        //vertical black rectangle
        Rectangle vertRect = new Rectangle(RULER_SIZE, RULER_SIZE+1, RULER_SIZE, image.getHeight()-12);
        g2d.fill(vertRect);

        g2d.setColor(Color.BLACK);
        //horizontal line
        g2d.drawLine(RULER_SIZE, image.getHeight()+RULER_SIZE, image.getWidth()+RULER_SIZE, image.getHeight()+RULER_SIZE);

        //vertical line
        g2d.drawLine(RULER_SIZE, RULER_SIZE+1, RULER_SIZE, image.getHeight()+RULER_SIZE);

        // horizontal ruler (range)
        int y = image.getHeight()+RULER_SIZE;
        double step = zoomRangeStep * (image.getWidth()+margin) / range;
        double r = zoomRangeStep;
        int c = margin + (int) step;
        g2d.setColor(Color.WHITE);

        for (; c<=image.getWidth()+margin; c += step , r += zoomRangeStep) {
            int length = (int)(Math.log10(r)+1);
            g2d.setColor(Color.WHITE);
            g2d.drawLine(c, y, c, y-lineWith);
            if (length >= 2) {
                margin = 13;
            }
            g2d.drawString("" + (int) r, c - margin, y-1);
            g2d.setColor(Color.BLACK);
            g2d.drawLine(c, y, c, y+lineWith);

        }

        // vertical ruler (height)
        double height = selectedMarker.getHeight() / 2.0;

        double zoomRangeStepV = 2.0;
        double stepV = zoomRangeStepV * (image.getHeight()) / height;
        double rV = 0;
        int cV = y;


        for (; cV >= 0 ; cV -= stepV , rV += zoomRangeStepV) {
            if (cV < y) {
                g2d.setColor(Color.WHITE);
                g2d.drawLine(RULER_SIZE+1, cV, (RULER_SIZE+3)+lineWith, cV);
                g2d.drawString("" + (int) rV,  RULER_SIZE + 4 , cV+11);

                g2d.setColor(Color.BLACK);
                g2d.drawLine(RULER_SIZE+1, cV, (RULER_SIZE)-lineWith, cV);
            }
        }

    }

    public void loadMarker(LogMarkerItem log, int rowIndex) {
        selectedMarker = log;
        selectMarkerRowIndex = rowIndex;

        if (selectedMarker.getSidescanImgPath() != null ) {
            try {
                image = ImageIO.read(new File(selectedMarker.getSidescanImgPath().getPath()));
                int width = image.getWidth();
                int height = image.getHeight();
                markerImage.repaint();
                markerImage.setPreferredSize(new Dimension(width+25, height+25));

                setBounds(100, 100, width + 265 + 25, height + 80 + 25);
                setLocation(parent.getwindowLocation());

            } catch (IOException e) {
                NeptusLog.pub().error("Error reading image file for "+ selectedMarker.getLabel());
            }
        }

        nameLabelValue.setText(selectedMarker.getLabel());
        nameLabelValue.setToolTipText(selectedMarker.getLabel());
        timeStampValue.setText(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(selectedMarker.getTimestamp()));
        locationValue.setText(selectedMarker.getLocation().toString());
        altitudeValue.setText(Double.toString(selectedMarker.getAltitude()));
        classifValue.setSelectedItem(selectedMarker.getClassification());
        annotationValue.setText(selectedMarker.getAnnotation());
        nameLabelValue.setSize(nameLabelValue.getPreferredSize() );

    }

    private void setupDrawPopup() {
        drawPopupMenu = new JPopupMenu();
        freeDraw = new AbstractAction(I18n.text("Free draw"), null) {

            @Override
            public void actionPerformed(ActionEvent arg0) {
                enableRectDraw = false;
                enableCircleDraw = false;
                enableFreeDraw = true;
                System.out.println("free draw enabled");
            }
        };
        freeDraw.putValue(Action.SHORT_DESCRIPTION, I18n.text("Draw with mouse mov."));

        rectDraw = new AbstractAction(I18n.text("Rectangle draw"), null) {


            @Override
            public void actionPerformed(ActionEvent arg0) {
                enableFreeDraw = false;
                enableCircleDraw = false;
                enableRectDraw = true;
                System.out.println("rectangle draw enabled");
            }
        };
        rectDraw.putValue(Action.SHORT_DESCRIPTION, I18n.text("Draw a rectangle."));

        clearDraw = new AbstractAction(I18n.text("Clear Drawings"), null) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                enableFreeDraw = false;
                enableRectDraw = false;
                enableCircleDraw = false;

                System.out.println("cancel all draws");

            }
        };
        clearDraw.putValue(Action.SHORT_DESCRIPTION, I18n.text("Clear draw."));


        circleDraw = new AbstractAction(I18n.text("Circle Draw"), null) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                enableFreeDraw = false;
                enableRectDraw = false;
                enableCircleDraw = true;
            }
        };
        circleDraw.putValue(Action.SHORT_DESCRIPTION, I18n.text("Draw a circle."));


        exportImage = new AbstractAction(I18n.text("Export Image"), null) {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                //TODO SAVE IMAGE
                if (markerImage != null) {
                    BufferedImage img = new BufferedImage(markerImage.getWidth(), markerImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2d = img.createGraphics();
                    markerImage.printAll(g2d);
                    g2d.dispose();

                    String path = parent.mraPanel.getSource().getFile("Data.lsf").getParent() + "/markers/";

                    JFileChooser fileChooser = new JFileChooser(new File(path));
                    fileChooser.setFileFilter(new FileFilter() {
                        @Override
                        public boolean accept(File f) {
                            if (f.isDirectory()) {
                                return true;
                            }
                            final String name = f.getName();
                            return name.endsWith(".png") || name.endsWith(".jpg");
                        }

                        @Override
                        public String getDescription() {
                            return "*.png,*.jpg";
                        }
                    });
                    
                    int retVal = fileChooser.showSaveDialog(null);
                    if(retVal == JFileChooser.APPROVE_OPTION){

                        try {
                            ImageIO.write(img, "png", fileChooser.getSelectedFile());
                        }
                        catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }

            }
        };
        exportImage.putValue(Action.SHORT_DESCRIPTION, I18n.text("Export this image to a file."));

        drawPopupMenu.add(rectDraw);
        drawPopupMenu.add(circleDraw);
        drawPopupMenu.add(freeDraw);
        drawPopupMenu.add(clearDraw);
        drawPopupMenu.addSeparator();
        drawPopupMenu.add(exportImage);

    }

    private void setupFileMenu() {
        menuBar = new JMenuBar();
        setJMenuBar(menuBar);

        JMenu mnFile = new JMenu(I18n.text("File"));
        menuBar.add(mnFile);

        save = new AbstractAction(I18n.text("Save"), ImageUtils.getIcon("images/menus/save.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                //get values from fields
                Classification classif = (Classification) classifValue.getSelectedItem();
                String annotation = annotationValue.getText();

                selectedMarker.setClassification(classif);
                selectedMarker.setAnnotation(annotation);
                parent.updateTableRow(selectedMarker, selectMarkerRowIndex);
            }
        };
        save.putValue(Action.SHORT_DESCRIPTION, I18n.text("Save Marker") + ".");

        del = new AbstractAction(I18n.text("Delete"), ImageUtils.getIcon("images/menus/editdelete.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                int res = showDelDialog();
                if (res==0)  { 
                    parent.deleteLog(selectedMarker, selectMarkerRowIndex);
                    dispose();
                }
            }
        };
        del.putValue(Action.SHORT_DESCRIPTION, I18n.text("Delete Marker") + ".");


        exit = new AbstractAction(I18n.text("Exit"), ImageUtils.getIcon("images/menus/exit.png")) {

            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
                dispose();
            }
        };
        exit.putValue(Action.SHORT_DESCRIPTION, I18n.text("Exit Marker Editor") + ".");

        mnFile.add(save);
        mnFile.add(del);
        mnFile.add(exit);

        JMenu mnDraw = new JMenu("Draw");
        menuBar.add(mnDraw);

        JMenuItem mntmClearDrawings = new JMenuItem("Clear drawings");
        mnDraw.add(mntmClearDrawings);

    }

    private int showDelDialog() {
        Object[] options = {"Yes, please", "No, thanks"};
        int n = JOptionPane.showOptionDialog(this,
                "Are you sure you want to delete this marker?",
                "Confirm delete",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[0]);

        return n;
    }

}
