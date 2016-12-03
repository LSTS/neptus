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
 * Author: tsm
 * Nov 14, 2013
 */

package pt.lsts.neptus.plugins.multibeam.viewers;


import com.google.common.eventbus.Subscribe;
import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorBar;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.comm.transports.udp.UDPTransport;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.MultibeamUtil;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ColorUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.LsfLogSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Cross section viewer for Multibeam data
 * */
@PluginDescription(author = "Tiago Marques", version = "0.1", name = "Multibeam: Cross-Section Viewer")
@Popup(pos = Popup.POSITION.TOP_LEFT, width = 1000, height = 800)
public class MultibeamCrossSection extends ConsolePanel implements MainVehicleChangeListener, ConfigurationListener {

    private ExecutorService threadExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        String nameBase = new StringBuilder().append(MultibeamCrossSection.class.getSimpleName())
                .append("::").append(Integer.toHexString(MultibeamCrossSection.this.hashCode()))
                .append("::").toString();
        ThreadGroup group = new ThreadGroup(nameBase);
        AtomicLong c = new AtomicLong(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread td = new Thread(group, r, nameBase + c.getAndIncrement());
            td.setDaemon(true);
            return td;
        }
    });

    // viewe's parameters
    @NeptusProperty(name="Sensor's range", category="Visualization parameters", userLevel = NeptusProperty.LEVEL.REGULAR)
    public double mbRange = 30;

    @NeptusProperty(name="Data's color map", category="Visualization parameters", userLevel = NeptusProperty.LEVEL.REGULAR)
    private ColorMap colorMap = ColorMapFactory.createJetColorMap();

    // grid's number of rows
    private final int N_ROWS = 5;

    // grid's number of columns
    private final int N_COLS = 10;

    private final Color GRID_COLOR = Color.GREEN.darker().darker();
    private final Color LABELS_COLOR = Color.GRAY;

    /* Viewer's GUI */

    // contains data's panel and info labels
    private JPanel viewer;

    // contains data and grid image
    private JPanel dataPanel;

    // contains information labels
    private JPanel infoPanel;

    // where data will be displayed
    private BufferedImage dataImage;
    private final AlphaComposite transparentComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    // layer with range and beam's scale
    private BufferedImage gridLayer;
    private int cellSize;
    private final double alpha = Math.toRadians(120);
    private final int startAngle = 180;
    private final int angleExtend = 180;
    private int arcX;
    private int arcY;
    private int arcWidth;
    private int arcHeight;

    // when the window is resized
    private boolean gridInvalidated = false;

    // information labels
    private final JLabel vehicleIdLabel = new JLabel("ID: ");
    private final JLabel vehicleIdValue = new JLabel("n/a");

    private final JLabel headingLabel = new JLabel("HDG: ");
    private final JLabel headingValue = new JLabel("n/a");

    private final JLabel latLabel = new JLabel("LAT: ");
    private final JLabel latValue = new JLabel("n/a");

    private final JLabel lonLabel = new JLabel("LON: ");
    private final JLabel lonValue = new JLabel("n/a");

    private final JLabel speedLabel = new JLabel("SPEED: ");
    private final JLabel speedValue = new JLabel("n/a");

    private final JLabel pitchLabel = new JLabel("PITCH: ");
    private final JLabel pitchValue = new JLabel("n/a");

    private final JLabel rollLabel = new JLabel("ROLL: ");
    private final JLabel rollvalue = new JLabel("n/a");

    private final JLabel altLabel  = new JLabel("ALT: ");
    private final JLabel altValue = new JLabel("n/a");

    private ColorBar colorBar = null;

    // Data
    private List<BathymetrySwath> dataList = Collections.synchronizedList(new ArrayList<BathymetrySwath>());
    private SystemPositionAndAttitude currState = null;


    public MultibeamCrossSection(ConsoleLayout console) {
        super(console);
        initGUI();
    }

    private void initGUI() {
        viewer = initViewerPanel();
        dataPanel = initDataPanel();
        infoPanel = initInfoPanel();

        viewer.add(infoPanel, "w 100%, h 20%, wrap");
        viewer.add(dataPanel, "w 100%, h 80%");
        setLayout(new MigLayout("ins 0, gap 0", "[][grow]", "[top][grow]"));
        add(viewer, "w 100%, h 100%,  grow");
    }

    private JPanel initViewerPanel() {
        JPanel vPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };

        // Deal with panel resize by recreating the image buffers
        vPanel.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                if (e.getID() == ComponentEvent.COMPONENT_RESIZED) {
                    createImages();
                    gridInvalidated = true;
                }
            }
        });
        vPanel.setBackground(Color.black);
        vPanel.setLayout(new MigLayout());
        vPanel.setPreferredSize(new Dimension(this.getWidth(), this.getHeight()));
        return vPanel;
    }

    private JPanel initDataPanel() {
        JPanel dataPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                if(gridInvalidated) {
                    gridLayer = createGridImage();
                    gridInvalidated = false;
                }
                g.drawImage(gridLayer, 0, 0, null);
                g.drawImage(dataImage, 0, 0, null);
            }
        };
        dataPanel.setBackground(Color.black);
        return dataPanel;
    }

    private JPanel initInfoPanel() {
        JPanel infoPanel = new JPanel() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
            }
        };

        infoPanel.setLayout(new MigLayout("wrap", "300[]10[]150[][]", "[][][][]"));
        infoPanel.setBackground(Color.black);
        infoPanel.setPreferredSize(new Dimension(viewer.getWidth(), viewer.getHeight()));

        final Font f = latLabel.getFont().deriveFont(18.0f);

        vehicleIdLabel.setFont(f);
        vehicleIdLabel.setForeground(LABELS_COLOR);
        vehicleIdValue.setForeground(GRID_COLOR);
        infoPanel.add(vehicleIdLabel);
        infoPanel.add(vehicleIdValue);

        headingLabel.setFont(f);
        headingLabel.setForeground(LABELS_COLOR);
        headingValue.setForeground(GRID_COLOR);
        infoPanel.add(headingLabel);
        infoPanel.add(headingValue);

        latLabel.setFont(f);
        latLabel.setForeground(LABELS_COLOR);
        latValue.setForeground(GRID_COLOR);
        infoPanel.add(latLabel);
        infoPanel.add(latValue);

        lonLabel.setFont(f);
        lonLabel.setForeground(LABELS_COLOR);
        lonValue.setForeground(GRID_COLOR);
        infoPanel.add(lonLabel);
        infoPanel.add(lonValue);

        speedLabel.setFont(f);
        speedLabel.setForeground(LABELS_COLOR);
        speedValue.setForeground(GRID_COLOR);
        infoPanel.add(speedLabel);
        infoPanel.add(speedValue);

        pitchLabel.setFont(f);
        pitchLabel.setForeground(LABELS_COLOR);
        pitchValue.setForeground(GRID_COLOR);
        infoPanel.add(pitchLabel);
        infoPanel.add(pitchValue);

        rollLabel.setFont(f);
        rollLabel.setForeground(LABELS_COLOR);
        rollvalue.setForeground(GRID_COLOR);
        infoPanel.add(rollLabel);
        infoPanel.add(rollvalue);

        altLabel.setFont(f);
        altLabel.setForeground(LABELS_COLOR);
        altValue.setForeground(GRID_COLOR);
        infoPanel.add(altLabel);
        infoPanel.add(altValue);

        colorBar = createColorBar();
        infoPanel.add(colorBar, "span, growx");

        return infoPanel;
    }

    private BufferedImage createGridImage() {
        int gridWidth;
        int gridHeight;

        // adjust grid's dimensions

        if(dataPanel.getWidth() < dataPanel.getHeight()) {
            cellSize = dataPanel.getWidth() / N_COLS;
            gridHeight = N_ROWS * cellSize;
            gridWidth = N_COLS * cellSize;

            if(gridHeight > dataPanel.getHeight()) {
                float ratio = 1 - dataPanel.getHeight() / (float) gridHeight;
                cellSize -= cellSize * ratio;
                gridWidth = N_COLS * cellSize;
                gridHeight = N_ROWS * cellSize;
            }
        }
        else {
            cellSize = dataPanel.getHeight() / N_ROWS;
            gridWidth = N_COLS * cellSize;
            gridHeight = N_ROWS * cellSize;

            if(gridWidth > dataPanel.getWidth()) {
                float ratio = 1 - dataPanel.getWidth() / (float) gridWidth;
                cellSize -= cellSize * ratio;
                gridWidth = N_COLS * cellSize;
                gridHeight = N_ROWS * cellSize;
            }
        }


        BufferedImage grid = new BufferedImage(gridWidth,
                gridHeight, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = (Graphics2D) grid.getGraphics().create();
        g.setColor(Color.GREEN.darker().darker());

        // dotted line
        g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {3,2}, 0));

        // vertical lines
        for(int i = 0; i <= N_COLS; i++) {
            int x = cellSize * i;
            int y = gridHeight;

            if(i == N_COLS)
                x--;

            g.drawLine(x, 0, x, y);
        }

        // horizontal lines
        for(int i = 0; i <= N_ROWS; i++) {
            int x = gridWidth;
            int y = cellSize * i;

            if(i == N_ROWS)
                y--;

            g.drawLine(0, y, x, y);
        }

        drawBeamScale(g, gridWidth, gridHeight);
        drawRangeScale(g, cellSize);

        return grid;
    }

    private void drawBeamScale(Graphics2D g, int gridWidth, int gridHeight) {
        int xi = gridWidth  / 2;
        int yi = 0;
        int xf = (int) Math.round(xi - Math.sin(this.alpha/2) * gridHeight);
        int yf = gridHeight / 2;

        // left side
        g.drawLine(xi, yi, xf, yf);

        // right side
        xf = (int) Math.round(xi + Math.sin(this.alpha/2) * gridHeight);
        g.drawLine(xi, yi, xf, yf);

        // setup and draw arc
        // read g.drawArc() docs
        this.arcX = (int) Math.round(xi - Math.sin(this.alpha/2) * gridHeight);
        this.arcY = 0;
        this.arcWidth = gridWidth - 2*arcX;
        this.arcHeight = gridHeight;

        g.drawArc(arcX, arcY, arcWidth, arcHeight, startAngle, angleExtend);
    }

    private ColorBar createColorBar() {
        ColorBar cBar = new ColorBar(ColorBar.HORIZONTAL_ORIENTATION, this.colorMap) {
            @Override
            public void paint(Graphics g) {
                super.paint(g);

                Graphics g2 = g.create();

                g2.setColor(ColorUtils.invertColor(this.getCmap().getColor(0)));
                g2.drawString("0m", 2, colorBar.getHeight() - 3);

                long maxVal = Math.round(mbRange);
                long medVal = Math.round(maxVal / 2d);
                if (maxVal != medVal && this.getWidth() > 150) {
                    String medString = String.valueOf(medVal) + "m";
                    Rectangle2D strBnds = g2.getFontMetrics().getStringBounds(medString, g2);
                    g2.setColor(ColorUtils.invertColor(this.getCmap().getColor(0.5)));
                    g2.drawString(medString, (int) (colorBar.getWidth() / 2d - strBnds.getWidth() / 2d), colorBar.getHeight() - 3);
                }

                String maxString = String.valueOf(maxVal) + "m";
                Rectangle2D strBnds = g2.getFontMetrics().getStringBounds(maxString, g2);
                g2.setColor(ColorUtils.invertColor(this.getCmap().getColor(1)));
                g2.drawString(maxString, (int) (colorBar.getWidth() - strBnds.getWidth() - 2), colorBar.getHeight() - 3);

                g2.dispose();
            }
        };

        return cBar;
    }

    // Create images and layer to display the data
    private void createImages() {
        synchronized (dataList) {
            dataImage = ImageUtils.createCompatibleImage(viewer.getWidth(), viewer.getHeight(), Transparency.TRANSLUCENT);
        }
    }

    private void drawRangeScale(Graphics2D g, int cellSize) {
        g.setColor(LABELS_COLOR);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 16.0f));
        double rangeScale = mbRange / N_ROWS;
        for(int i = 1; i <= N_ROWS; i++)
            g.drawString(Double.toString(i * rangeScale), 10, i * cellSize - 2);
    }

    @Override
    public void cleanSubPanel() {
        threadExecutor.shutdownNow();
    }

    @Override
    public void initSubPanel() {

    }

    private double getDataMaxDepth(BathymetrySwath swath) {
        double max = mbRange;
        BathymetryPoint[] data = swath.getData();
        for(int i = 0; i < data.length; i++)
            if(data[i] != null && data[i].depth > max)
                max = data[i].depth;

        return max;
    }

    private void drawMultibeamData(BathymetrySwath swath) {
        // flush previous data
        dataImage = ImageUtils.createCompatibleImage(viewer.getWidth(), viewer.getHeight(), Transparency.TRANSLUCENT);

        // draw new data
        BathymetryPoint[] data = swath.getData();
        for(int i = 0; i < data.length; i++) {
            if(data[i] == null)
                continue;

            // translate to the center of the display
            int x = gridLayer.getWidth() / 2;
            int y = 0;

            // scale coordinates
            double scale = gridLayer.getHeight() / mbRange;
            x += (int) (Math.round(data[i].east * scale));
            y += (int) (Math.round(data[i].depth * scale));

            dataImage.setRGB(x, y, colorMap.getColor(data[i].depth / mbRange).getRGB());
        }

        dataPanel.repaint();
    }

    @Subscribe
    public void onSonarData(SonarData msg){
        try {
            if (!msg.getSourceName().equals(getMainVehicleId()) || msg.getType() != SonarData.TYPE.MULTIBEAM ||
                    gridLayer == null || dataImage == null)
                return;

            if (currState == null)
                currState = new SystemPositionAndAttitude();

            BathymetrySwath swath = MultibeamUtil.getMultibeamSwath(msg, currState);

            if (swath == null) {
                NeptusLog.pub().warn("Null bathymetry swath from " + msg.getSourceName() + " at " + msg.getTimestampMillis());
                return;
            }

            // update grid's scale if necessary
            double maxDepth = getDataMaxDepth(swath);
            if(maxDepth != mbRange) {
                int factor = (int) Math.round(maxDepth / mbRange);
                mbRange +=  factor*10;
                gridInvalidated = true;
            }

            drawMultibeamData(swath);
        }
        catch (Exception e) {
            e.printStackTrace();
            System.out.println();
        }
    }

    @Subscribe
    public void onEstimatedState(EstimatedState msg) {
        if(!msg.getSourceName().equals(getMainVehicleId()))
            return;
        currState = new SystemPositionAndAttitude(msg);

        if(currState != null) {
            vehicleIdValue.setText(getMainVehicleId());
            double heading = currState.getYaw() * 180 / Math.PI;
            headingValue.setText(toRoundedString(heading));

            LocationType loc = currState.getPosition();
            latValue.setText(loc.getLatitudeAsPrettyString());
            lonValue.setText(loc.getLongitudeAsPrettyString());

            speedValue.setText(toRoundedString(currState.getV()));
            pitchValue.setText(toRoundedString(currState.getPitch()));
            rollvalue.setText(toRoundedString(currState.getRoll()));
            altValue.setText(toRoundedString(currState.getAltitude()));
        }
    }

    /**
     * From a given estimated state value returns
     * it rounded and in string format
     * */
    private String toRoundedString(double value) {
        return Double.toString(Math.round(value * 100000) / 100000.0);
    }

    @Override
    public void propertiesChanged() {
        colorBar.setCmap(ColorMapFactory
                .createInvertedColorMap((InterpolationColorMap) colorMap));

        gridInvalidated = true;
        dataPanel.repaint();
    }

    // for testing
    public static void main(String[] args) {
        String dataFile = (System.getProperty("user.dir") + "/" + "log/maridan-multibeam/Data.lsf.gz");
        System.out.println("** Reading: " + dataFile);

        UDPTransport udp = new UDPTransport(6002, 1);

        try {
            LsfLogSource source = new LsfLogSource(dataFile, null);
            DeltaTParser mbParser = new DeltaTParser(source);

            Collection<Integer> vehSrcs = source.getVehicleSources();
            if (vehSrcs.size() < 1)
                return;

            int mainVehSrc = vehSrcs.iterator().next();
            System.out.println("*** Setting message source as: " + mainVehSrc);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IMCOutputStream imcOs = new IMCOutputStream(baos);

            BathymetrySwath swath = mbParser.nextSwath();
            while (swath != null) {
                SystemPositionAndAttitude pose = swath.getPose();
                EstimatedState currentEstimatedState = pose.toEstimatedState();
                SonarData sd = MultibeamUtil.swathToSonarData(swath, pose);

                currentEstimatedState.setSrc(mainVehSrc);
                sd.setSrc(mainVehSrc);

                currentEstimatedState.setTimestamp(sd.getTimestamp());

                baos.reset();
                currentEstimatedState.serialize(imcOs);
                udp.sendMessage("localhost", 6001, baos.toByteArray());
                baos.reset();
                sd.serialize(imcOs);
                udp.sendMessage("localhost", 6001, baos.toByteArray());

                Thread.sleep(50);

                swath = mbParser.nextSwath();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
