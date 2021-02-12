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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: Paulo Dias
 * 18 de Jun de 2011
 */
package pt.lsts.neptus.plugins.mraplots;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.ColorMapUtils;
import pt.lsts.neptus.colormap.DataDiscretizer;
import pt.lsts.neptus.colormap.DataDiscretizer.DataPoint;
import pt.lsts.neptus.gui.BlockingGlassPane;
import pt.lsts.neptus.gui.ColorMapListRenderer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.MRAProperties;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.visualizations.SimpleMRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * FIXME - To be deleted?
 * 
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Paulo Dias", name = "Bathymetry 2D", version = "0.9", icon = "pt/lsts/neptus/plugins/mraplots/2D_v2.png", active=false)
public class BathymetryPlotter extends SimpleMRAVisualization {

    // GUI
    private JScrollPane imageScrollPane;
    private JComboBox<?> cmapCombo;
    private JFormattedTextField gridSizeTextField;
    private JFormattedTextField widthSizeTextField;
    private JFormattedTextField heightSizeTextField;
    private JFormattedTextField timeStepTextField;
    private JComboBox<String> bottomDistanceEntityCombo;
    private JToolBar toolbar;
    private BufferedImage image;
    private BufferedImage caption;
    private JPanel holderPanel;
    private BlockingGlassPane blockPanel;

    private int gridSize = 100;
    private int targetImageWidth = 512;
    private int targetImageHeight = 512;
    private boolean started = false;
    private boolean onError = false;
    private int imcVersion;

    private LinkedHashMap<Integer, String> entities;
    private Vector<Double> xVec, yVec, zVec;
    private XYZDataType xyzData;
    private DataDiscretizer dd;
    private DataPoint[] dps;
    private Rectangle2D.Double bounds;

    /**
     * @param panel
     */
    public BathymetryPlotter(MRAPanel panel) {
        super(panel);
    }

    @Override
    public Double getDefaultTimeStep() {
        return MRAProperties.defaultTimestep;
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        //        if (source.getLsfIndex().getDefinitions().getVersion().compareTo("5.0.0") >= 0) {
        //            imcVersion = 5;
        //            return source.getLog("EstimatedState") != null;
        //        }
        //        else {
        //            imcVersion = 4;
        //            boolean ret = true;
        //
        //            ret = ret && source.getLog("EstimatedState") != null;
        //            ret = ret && source.getLog("BottomDistance") != null;
        //
        //            return ret;
        //        }
        return false;
    }

    @Override
    public JComponent getVisualization(IMraLogGroup source, double timestep) {
        if (started)
            return this;
        blockPanel = new BlockingGlassPane();
        started = true;

        this.source = source;
        this.timestep = timestep;

        switch (imcVersion) {
            case 4:
                setupIMC4(source, timestep);
                break;
            case 5:
                setupIMC5(source, timestep);
                break;
            default:
                NeptusLog.pub().debug(I18n.text("Unsupported IMC version: ") + imcVersion);
        }
        commonSetup();
        runWork();

        return this;
    }

    private void setupIMC5(IMraLogGroup source, double timestep) {
        toolbar = new JToolBar(JToolBar.HORIZONTAL);
        toolbar.setFloatable(false);
    }

    private void commonSetup() {
        cmapCombo = new JComboBox<Object>(ColorMap.cmaps.toArray(new ColorMap[ColorMap.cmaps.size()]));
        cmapCombo.setSelectedItem(ColorMapFactory.createJetColorMap());
        cmapCombo.setRenderer(new ColorMapListRenderer());
        toolbar.add(cmapCombo);

        JButton bt = new JButton(new AbstractAction(I18n.text("Recreate")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                runWork();
            }
        });
        toolbar.add(bt);

        JButton bt1 = new JButton(new AbstractAction(I18n.text("Save")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                save();
            }
        });
        toolbar.add(bt1);

        setLayout(new BorderLayout());
        add(toolbar, BorderLayout.NORTH);
        // toolbar.setEnabled(false);
        // for (Component component : toolbar.getComponents()) {
        // component.setEnabled(false);
        // }
        imageScrollPane = new JScrollPane();
        imageScrollPane.setBorder(null);

        holderPanel = new JPanel();
        holderPanel.setLayout(new BorderLayout());
        holderPanel.add(imageScrollPane);

        add(holderPanel);
    }

    private void setupIMC4(IMraLogGroup source, double timestep) {
        toolbar = new JToolBar(JToolBar.HORIZONTAL);
        toolbar.setFloatable(false);

        gridSizeTextField = new JFormattedTextField(GuiUtils.getNeptusIntegerFormat());
        gridSizeTextField.setText("" + gridSize);
        gridSizeTextField.setColumns(4);

        // Get the entites in the BottomDistance msgs
        entities = LogUtils.getEntities(source);
        Vector<String> bottomDistanceEntitiesVector = new Vector<String>();
        bottomDistanceEntitiesVector.add("ALL");

        IMraLog bdSouce = source.getLog("BottomDistance");
        IMCMessage le = bdSouce.nextLogEntry();
        while (le != null) {
            int eid = le.getInteger("src_ent");
            String eName = entities.get(eid);
            if (!bottomDistanceEntitiesVector.contains(eName))
                bottomDistanceEntitiesVector.add(eName);
            le = bdSouce.nextLogEntry();
        }
        String[] bdEntitiesArray = bottomDistanceEntitiesVector
                .toArray(new String[bottomDistanceEntitiesVector.size()]);
        Arrays.sort(bdEntitiesArray, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int res = o1.compareTo(o2);
                if (res == 0)
                    return res;
                if ("ALL".equals(o1) || "ALL".equals(o2))
                    return "ALL".equals(o1) ? -1 : 1;
                if ("Depth & Heading Control".equals(o1) || "Depth & Heading Control".equals(o2))
                    return "Depth & Heading Control".equals(o1) ? -1 : 1;
                if ("DVL".equals(o1) || "DVL".equals(o2))
                    return "DVL".equals(o1) ? -1 : 1;
                if ("Echo Sounder".equals(o1) || "Echo Sounder".equals(o2))
                    return "Echo Sounder".equals(o1) ? -1 : 1;
                return res;
            }
        });
        bottomDistanceEntityCombo = new JComboBox<String>(bdEntitiesArray);
        bottomDistanceEntityCombo.setSelectedItem(bdEntitiesArray.length > 1 ? bdEntitiesArray[1] : bdEntitiesArray[0]);
        toolbar.add(new JLabel(" " + I18n.text("Entity") + ": "));
        toolbar.add(bottomDistanceEntityCombo);

        widthSizeTextField = new JFormattedTextField(GuiUtils.getNeptusIntegerFormat());
        widthSizeTextField.setText("" + targetImageWidth);
        widthSizeTextField.setColumns(4);
        heightSizeTextField = new JFormattedTextField(GuiUtils.getNeptusIntegerFormat());
        heightSizeTextField.setText("" + targetImageWidth);
        heightSizeTextField.setColumns(4);
        timeStepTextField = new JFormattedTextField(GuiUtils.getNeptusDecimalFormat(4));
        timeStepTextField.setText("" + timestep);
        timeStepTextField.setColumns(7);
        timeStepTextField.addPropertyChangeListener("timestep", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Object source = evt.getSource();
                if (source == timeStepTextField) {
                    setTimestep(((Number) timeStepTextField.getValue()).doubleValue());
                }
            }
        });
        toolbar.add(new JLabel(" " + I18n.text("Time step (s)") + ": "));
        toolbar.add(timeStepTextField);

        toolbar.add(Box.createHorizontalStrut(10));
        toolbar.add(new JLabel(" " + I18n.text("Image width") + ": "));
        toolbar.add(widthSizeTextField);
        toolbar.add(new JLabel(" " + I18n.text("height") + ": "));
        toolbar.add(heightSizeTextField);
        toolbar.add(Box.createHorizontalStrut(10));
    }

    private void setTimestep(double timestep) {
        this.timestep = timestep;
    }

    protected void save() {
        int opt = JOptionPane.showOptionDialog(
                this,
                I18n.text("Save image or as map"),
                I18n.text("Save options"),
                JOptionPane.CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                new String[] { I18n.text("Save Image With Caption"), I18n.text("Save Plain Image"),
                    I18n.text("Save as Height Map"), I18n.text("Save as Map"), I18n.text("Cancel") },
                    I18n.text("Save Image"));
        Graphics imageGraphics;
        if (opt < 4 && opt > -1) {
            String name = JOptionPane.showInputDialog(this, I18n.text("Input image name"));
            BufferedImage toSave = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            switch (opt) {
                case 1: // save bathymetry with caption
                    imageGraphics = toSave.getGraphics();
                    imageGraphics.drawImage(image, 0, 0, null);
                    break;
                case 0: // save bathymetry without caption
                    imageGraphics = toSave.getGraphics();
                    imageGraphics.drawImage(image, 0, 0, null);
                    imageGraphics.drawImage(caption, 0, 0, null);
                    break;
                case 2: // height map
                    ColorMapUtils.generateInterpolatedColorMap(bounds, dps, 0, toSave.createGraphics(),
                            toSave.getWidth(), toSave.getHeight(), 255, ColorMapFactory.createGrayScaleColorMap(),
                            dd.minVal[0] * 0.995, dd.maxVal[0] * 1.005);
                    break;
                case 3: // save bathymetry, height map and map
                    if (!"".equalsIgnoreCase(name)) {
                        ColorMapUtils.generateInterpolatedColorMap(bounds, dps, 0, toSave.createGraphics(),
                                toSave.getWidth(), toSave.getHeight(), 255, ColorMapFactory.createGrayScaleColorMap(),
                                dd.minVal[0] * 0.995, dd.maxVal[0] * 1.005);
                        XYZUtils.getAsMapType(image, toSave, name, "./", xyzData.centerLoc, xyzData.scale,
                                -xyzData.maxZ, -xyzData.minZ);
                    }
                    break;
            }
            if (!"".equalsIgnoreCase(name)) {
                File destFile = new File("./" + name + ".png");
                XYZUtils.saveImageToPNG(toSave, destFile);
            }
        }
    }

    private Thread t = null;

    private void runWork() {
        toolbar.setEnabled(false);

        holderPanel.removeAll();
        holderPanel.add(blockPanel);
        blockPanel.block(true);
        blockPanel.setText(I18n.text("Generating bathymetry plot"));

        for (Component component : toolbar.getComponents()) {
            component.setEnabled(false);
        }

        t = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    switch (imcVersion) {
                        case 4:
                            generateIMC4();
                        case 5:
                            generateIMC5();
                        default:
                            break;
                    }

                    double scaleY = image.getWidth() / bounds.getWidth();
                    double scaleX = image.getHeight() / bounds.getHeight();
                    double minY = bounds.getMinX();
                    double minX = bounds.getMinY();
                    XYZUtils.drawPath((Graphics2D) image.getGraphics(), scaleX, scaleY, minX, minY, timestep, source);
                    caption = new BufferedImage(200, 250, BufferedImage.TYPE_INT_ARGB);
                    XYZUtils.drawLegend((Graphics2D) caption.getGraphics(), (ColorMap) cmapCombo.getSelectedItem(),
                            I18n.text("Bathymetry"), 1 / xyzData.scale, "m", -xyzData.minZ, -xyzData.maxZ);

                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            remove(imageScrollPane);
                            imageScrollPane.setBorder(null);
                            JPanel panel = new JPanel(new MigLayout("wrap 2"));
                            panel.add(new JLabel(new ImageIcon(caption)));
                            panel.add(new JLabel(new ImageIcon(image)));
                            imageScrollPane.getViewport().setView(panel);
                            add(imageScrollPane, BorderLayout.CENTER);

                            toolbar.setEnabled(true);
                            for (Component component : toolbar.getComponents()) {
                                component.setEnabled(true);
                            }

                            revalidate();

                            blockPanel.setText(I18n.text("Done"));
                            holderPanel.removeAll();
                            holderPanel.add(imageScrollPane);

                            blockPanel.block(false);
                        }
                    });
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
                t = null;
            }
        });
        t.setName("BathymetryPlotter thread");
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void onCleanup() {
        if (blockPanel != null)
            blockPanel.block(false);
        if (t != null)
            t.interrupt();
        super.onCleanup();
    }

    private boolean generateIMC4() {
        long timestamp = System.currentTimeMillis();
        NeptusLog.pub().debug(timestamp + " generateIMC4");
        try {
            IMraLogGroup src = source;
            try {
                timestep = Double.parseDouble(timeStepTextField.getText());
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                timeStepTextField.setText("" + timestep);
            }

            String selEntityStr = (String) bottomDistanceEntityCombo.getSelectedItem();
            int selBdEntInt = 255;
            for (Integer eid : entities.keySet()) {
                if (entities.get(eid).equals(selEntityStr)) {
                    selBdEntInt = eid;
                    break;
                }
            }

            IMraLog bParser = src.getLog("BottomDistance");
            IMraLog stateParser = src.getLog("EstimatedState");

            xVec = new Vector<Double>();
            yVec = new Vector<Double>();
            zVec = new Vector<Double>();

            try {
                gridSize = Integer.parseInt(gridSizeTextField.getText());
            }
            catch (NumberFormatException e1) {
                e1.printStackTrace();
                gridSizeTextField.setText("" + 100);
            }

            dd = new DataDiscretizer(1);

            IMCMessage stateEntry, bEntry = null;

            IMCMessage entry = stateParser.nextLogEntry();
            timestamp = System.currentTimeMillis();
            NeptusLog.pub().debug(timestamp + " finding reference location");
            // try to generate homeref from logs
            LocationType imageCornerRef = LogUtils.getHomeRef(src);
            // in the case homeRef is null, the first position is copied to homeref
            if (imageCornerRef == null) {
                imageCornerRef = new LocationType();
                imageCornerRef.setLatitudeDegs(Math.toDegrees(entry.getDouble("lat")));
                imageCornerRef.setLatitudeDegs(Math.toDegrees(entry.getDouble("lon")));
            }
            // LocationType compare = Bathymetry3DGenerator.getLocationIMC5((EstimatedState) entry);
            LocationType tmp = new LocationType();

            bEntry = bParser.nextLogEntry();
            double bValue = 0;
            timestamp = System.currentTimeMillis();
            NeptusLog.pub().debug(timestamp + " Analysing data");
            while (bEntry != null && !onError) {
                if (bEntry != null) {
                    stateEntry = stateParser.getEntryAtOrAfter(bParser.currentTimeMillis());
                    if (stateEntry == null) {
                        bParser.advance((long) (timestep * 1000));
                        bEntry = bParser.getCurrentEntry();
                        continue;
                    }
                    if ((selBdEntInt != 255 && selBdEntInt != bEntry.getInteger("src_ent"))
                            || (bEntry != null && !"VALID".equalsIgnoreCase(bEntry.getString("validity")))) {
                        while ((bEntry != null && bEntry.getDouble("src_ent") != selBdEntInt)
                                || (bEntry != null && !"VALID".equalsIgnoreCase(bEntry.getString("validity")))) {
                            bEntry = bParser.nextLogEntry();
                        }
                    }

                    stateEntry = stateParser.getEntryAtOrAfter(bParser.currentTimeMillis());

                    tmp = LogUtils.getLocation(imageCornerRef, stateEntry);

                    if (tmp.getAllZ() < 0.8) {
                        bParser.advance((long) (timestep * 1000));
                        bEntry = bParser.getCurrentEntry();
                        continue;
                    }

                    tmp = tmp.getNewAbsoluteLatLonDepth();
                    double distance = bEntry.getDouble("value") + tmp.getDepth();
                    if (distance != 0) {
                        if (bValue == 0)
                            bValue = distance;
                        distance = bValue * 0.9 + distance * 0.1;
                        bValue = distance;
                    }
                    tmp.setDepth(0);
                    double[] offs = tmp.getOffsetFrom(imageCornerRef);
                    // NED
                    xVec.add(offs[0]); // North
                    yVec.add(offs[1]); // East
                    zVec.add(-distance); // Down

                    dd.addPoint(offs[1], -offs[0], -distance);
                }
                bParser.advance((long) (1000 * timestep));
                bEntry = bParser.getCurrentEntry();
                if (bEntry != null && (selBdEntInt != 255 && selBdEntInt != bEntry.getInteger("src_ent"))
                        || (bEntry != null && !"VALID".equalsIgnoreCase(bEntry.getString("validity")))) {
                    while ((bEntry != null && bEntry.getDouble("src_ent") != selBdEntInt)
                            || (bEntry != null && !"VALID".equalsIgnoreCase(bEntry.getString("validity")))) {
                        bEntry = bParser.nextLogEntry();
                    }
                }
            }

            timestamp = System.currentTimeMillis();
            NeptusLog.pub().debug(timestamp + " generating image");
            if (onError)
                return false;

            try {
                targetImageWidth = Integer.parseInt(widthSizeTextField.getText());
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                widthSizeTextField.setText("" + targetImageWidth);
            }

            try {
                targetImageHeight = Integer.parseInt(heightSizeTextField.getText());
            }
            catch (NumberFormatException e) {
                e.printStackTrace();
                heightSizeTextField.setText("" + targetImageHeight);
            }

            generateClippedImage(gridSize, imageCornerRef, targetImageWidth, targetImageHeight);

            if (onError)
                return false;

            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            onError = true;
            return false;
        }
    }

    private void generateClippedImage(int gridSize, LocationType imageCornerRef, int targetImageWidth,
            int targetImageHeight) {
        dps = dd.getDataPoints();

        long timestamp = System.currentTimeMillis();
        NeptusLog.pub().debug(timestamp + " getInterpolatedData");
        xyzData = XYZUtils.getInterpolatedData(imageCornerRef, xVec, yVec, zVec, targetImageWidth, targetImageHeight,
                gridSize);

        image = new BufferedImage(xyzData.width, xyzData.height, BufferedImage.TYPE_INT_ARGB);

        double maxX = dd.maxX + 25;
        double maxY = dd.maxY + 25;
        double minX = dd.minX - 25;
        double minY = dd.minY - 25;

        // width/height
        double dimensionX = maxX - minX;
        double dimensionY = maxY - minY;

        double ratioWanted = (double) targetImageWidth / (double) targetImageHeight;
        double ratioReal = dimensionX / dimensionY;

        if (ratioReal < ratioWanted)
            dimensionX = dimensionY * ratioWanted;
        else
            dimensionY = dimensionX / ratioWanted;

        // center
        double centerX = (maxX + minX) / 2;
        double centerY = (maxY + minY) / 2;

        // In meters
        bounds = new Rectangle2D.Double(centerX - dimensionX / 2, centerY - dimensionY / 2, dimensionX, dimensionY);

        try {
            timestamp = System.currentTimeMillis();
            NeptusLog.pub().debug(timestamp + " generateInterpolatedColorMap");
            ColorMapUtils.generateInterpolatedColorMap(bounds, dps, 0, image.createGraphics(), image.getWidth(),
                    image.getHeight(), 255, (ColorMap) cmapCombo.getSelectedItem(), dd.minVal[0] * 0.995,
                    dd.maxVal[0] * 1.005);
        }
        catch (NullPointerException e) {
            NeptusLog.pub().error(e, e);
        }

        // Clip with CHull

        timestamp = System.currentTimeMillis();
        NeptusLog.pub().debug(timestamp + " Clip with CHull");
        try {

            double scaleX = image.getWidth() / bounds.getWidth(); // Px/m
            double scaleY = image.getHeight() / bounds.getHeight();

            double minX1 = bounds.getMinX();
            double minY1 = bounds.getMinY();
            double maxX1 = bounds.getMaxX();
            double maxY1 = bounds.getMaxY();

            Graphics2D g2 = (Graphics2D) image.getGraphics().create();
            g2.setColor(new Color(0, 0, 0, 255));

            Graphics2D g2S = (Graphics2D) image.getGraphics().create();
            g2S.translate(-minX1 * scaleX, -minY1 * scaleY);
            g2S.scale(scaleX, scaleY);
            AffineTransform transfS = g2S.getTransform();
            g2S.dispose();

            ArrayList<Point2D> points = dd.computeConvexHull();
            ArrayList<Point2D> pointsGrow = MathMiscUtils.dilatePolygon(points, 10.0);
            GeneralPath cHullShapeS = new GeneralPath();
            cHullShapeS.setWindingRule(Path2D.WIND_EVEN_ODD);
            cHullShapeS.moveTo(minX1, minY1);
            cHullShapeS.lineTo(maxX1, minY1);
            cHullShapeS.lineTo(maxX1, maxY1);
            cHullShapeS.lineTo(minX1, maxY1);
            cHullShapeS.lineTo(minX1, minY1);
            for (int i = 0; i < pointsGrow.size(); i++) {
                if (i == 0)
                    cHullShapeS.moveTo(pointsGrow.get(i).getX(), pointsGrow.get(i).getY());
                else
                    cHullShapeS.lineTo(pointsGrow.get(i).getX(), pointsGrow.get(i).getY());

                if (i == pointsGrow.size() - 1)
                    cHullShapeS.lineTo(pointsGrow.get(0).getX(), pointsGrow.get(0).getY());
            }

            // clear
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
            g2.setColor(new Color(0, 255, 0, 255));
            g2.fill(cHullShapeS.createTransformedShape(transfS));
            g2.dispose();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        timestamp = System.currentTimeMillis();
        NeptusLog.pub().debug(timestamp + " done");
    }

    private boolean generateIMC5() {
        long timestamp = System.currentTimeMillis();
        NeptusLog.pub().debug(timestamp + " starting generateIMC5");
        xVec = new Vector<Double>();
        yVec = new Vector<Double>();
        zVec = new Vector<Double>();
        dd = new DataDiscretizer(1);

        timestamp = System.currentTimeMillis();
        NeptusLog.pub().debug(timestamp + " setting up imageCornerRef");
        EstimatedState firstEstStateMsg = source.getLsfIndex().getFirst(EstimatedState.class);
        LocationType imageCornerRef = LogUtils.getLocation(firstEstStateMsg);

        double waterColumn;
        double bValue = 0;
        LocationType estStateMsgLocation;
        LsfIndex lsfIndex = source.getLsfIndex();
        timestamp = System.currentTimeMillis();
        NeptusLog.pub().debug(timestamp + " processing points");
        for (EstimatedState currEstStateMsg : lsfIndex.getIterator(EstimatedState.class)) {
            if (currEstStateMsg.getAlt() < 0 || currEstStateMsg.getDepth() < MRAProperties.minDepthForBathymetry
                    || Math.abs(currEstStateMsg.getTheta()) > Math.toDegrees(10)) {
                continue;
            }
            waterColumn = currEstStateMsg.getDepth() + currEstStateMsg.getAlt();
            estStateMsgLocation = LogUtils.getLocation(currEstStateMsg);
            double[] offs = estStateMsgLocation.getOffsetFrom(imageCornerRef);
            // NED
            xVec.add(offs[0]); // North
            yVec.add(offs[1]); // East
            zVec.add(-waterColumn); // Down

            if (waterColumn != 0) {
                if (bValue == 0)
                    bValue = waterColumn;
                waterColumn = bValue * 0.9 + waterColumn * 0.1;
                bValue = waterColumn;
            }
            dd.addPoint(offs[1], -offs[0], -waterColumn);
        }

        timestamp = System.currentTimeMillis();
        NeptusLog.pub().debug(timestamp + " generating image");

        int targetImageWidth = 1024;
        int targetImageHeight = 768;
        generateClippedImage(gridSize, imageCornerRef, targetImageWidth, targetImageHeight);
        return true;
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }
}
