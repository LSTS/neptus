/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 21//01/2012
 */
package pt.up.fe.dceg.neptus.app.tiles;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkEvent.EventType;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jdesktop.swingx.JXBusyLabel;
import org.jdesktop.swingx.JXStatusBar;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.InfiniteProgressPanel;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.renderer2d.WorldRenderPainter;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.ColorUtils;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.MathMiscUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;
import pt.up.fe.dceg.neptus.util.output.OutputMonitor;

/**
 * @author Paulo Dias
 * 
 */
@SuppressWarnings({ "unused", "serial" })
public class WorldMapPanel extends JPanel {

    public static Icon NEPTUS_ICON = new ImageIcon(ImageUtils.getScaledImage("images/neptus-icon1.png", 32, 32));

    private Timer timer = null;
    private TimerTask timerTask = null;

    // ------- UI Components -------
    private JPanel headerPanel = null;
    private JPanel toolButtonsPanel;
    private JButton showOptionsDialog = null;
    private JButton addKMLFileButton = null;
    private JButton unloadAllKMLFilesButton = null;
    private JCheckBox showKMLItems = null;
    private StateRenderer2D renderer = null;
    private JXStatusBar statusBar = null;
    private JLabel levelOfDetailLabel;
    private JButton saveImageButton = null;
    private JButton copyImageButton = null;
    private JXBusyLabel busyPanel;
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JLabel memInfoLabel;
    private JLabel loadingTilesLabel;
    private JButton stopLoadingButton;

    private Renderer2DPainter kmlPainter = null;
    private MouseMotionListener rendererMouseMotionListener = null;
    private MouseListener rendererMouseListener = null;
    private KMLPlacemark infoBalloonFeature = null;

    private static File lastSuccessSavedDir = null;
    private static File lastSuccessKMLDir = null;

    private WorldRenderPainter worldRenderPainter = null;

    private JFrame jFrame;
    private JInternalFrame jInternalFrame;

    @NeptusProperty
    public LocationType center = new LocationType();

    @NeptusProperty
    public int levelOfDetail = 7;

    @NeptusProperty
    public String kmlFiles = "";

    private Map<String, Vector<KMLPlacemark>> placemarksHolder = Collections
            .synchronizedMap(new LinkedHashMap<String, Vector<KMLPlacemark>>());

    private DocumentBuilderFactory docBuilderFactory;
    {
        docBuilderFactory = DocumentBuilderFactory.newInstance();
        docBuilderFactory.setIgnoringComments(true);
        docBuilderFactory.setIgnoringElementContentWhitespace(true);
        docBuilderFactory.setNamespaceAware(false);
    }

    private static final String ROOT_PREFIX;
    static {
        if (new File("../" + "conf").exists())
            ROOT_PREFIX = "../";
        else {
            ROOT_PREFIX = "";
            new File("conf").mkdir();
        }
    }

    {
        try {
            String confFx = ROOT_PREFIX + "conf/" + WorldMapPanel.class.getSimpleName().toLowerCase() + ".properties";
            if (new File(confFx).exists())
                PluginUtils.loadProperties(confFx, WorldMapPanel.this);
        }
        catch (Exception e) {
            NeptusLog.pub().error(
                    "Not possible to open \"conf/" + WorldMapPanel.class.getSimpleName().toLowerCase()
                            + ".properties\"");
        }

        Vector<String> validFiles = new Vector<String>();
        String[] kfl = kmlFiles.split(",");
        for (String path : kfl) {
            File file = new File(path.trim());
            if (!file.exists())
                continue;

            validFiles.add(path.trim());
            lastSuccessKMLDir = file;
            Vector<KMLPlacemark> placemarks = getPlacemarksFromKML(file);
            if (placemarks.size() > 0) {
                String kmlName = file.getName().replaceFirst("((\\.[kK][mM][lL]))$", "");
                placemarksHolder.put(kmlName, placemarks);
            }
        }
        kmlFiles = "";
        for (String path : validFiles.toArray(new String[validFiles.size()])) {
            kmlFiles += kmlFiles.length() > 0 ? "," + path : path;
        }
    }

    /**
     * 
     */
    public WorldMapPanel() {
        initialize();
    }

    /**
     * 
     */
    private void initialize() {

        showOptionsDialog = new JButton(new AbstractAction("Preferences") {
            @Override
            public void actionPerformed(ActionEvent e) {
                worldRenderPainter.showChooseMapStyleDialog(WorldMapPanel.this);
            }
        });

        addKMLFileButton = new JButton(new AbstractAction("Add KML File") {
            @Override
            public void actionPerformed(ActionEvent e) {
                addKMLFileButton.setEnabled(false);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        JFileChooser fc = createFileKMLChooser("Choose a KML File");
                        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                        int option = fc.showOpenDialog(WorldMapPanel.this);
                        if (JFileChooser.APPROVE_OPTION == option) {
                            File file = fc.getSelectedFile();
                            // String kmlString = FileUtil.getFileAsString(file);
                            lastSuccessKMLDir = file;
                            Vector<KMLPlacemark> placemarks = getPlacemarksFromKML(file);
                            if (placemarks.size() > 0) {
                                String kmlName = file.getName().replaceFirst("((\\.[kK][mM][lL]))$", "");
                                placemarksHolder.put(kmlName, placemarks);

                                String[] fxs = kmlFiles.split(",");
                                Vector<String> fxsv = new Vector<String>();
                                for (String path : fxs) {
                                    if (!fxsv.contains(path))
                                        fxsv.add(path);
                                }
                                if (!fxsv.contains(file.getPath()))
                                    fxsv.add(file.getPath());
                                kmlFiles = "";
                                for (String path : fxsv.toArray(new String[fxsv.size()])) {
                                    kmlFiles += kmlFiles.length() > 0 ? "," + path : path;
                                }
                            }
                        }

                        return null;
                    }

                    @Override
                    protected void done() {
                        addKMLFileButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        });

        unloadAllKMLFilesButton = new JButton(new AbstractAction("Unload all KML Files") {
            @Override
            public void actionPerformed(ActionEvent e) {
                unloadAllKMLFilesButton.setEnabled(false);
                SwingWorker<Void, Void> sw = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        infoBalloonFeature = null;
                        placemarksHolder.clear();
                        kmlFiles = "";
                        return null;
                    }

                    @Override
                    protected void done() {
                        unloadAllKMLFilesButton.setEnabled(true);
                    }
                };
                sw.execute();
            }
        });

        showKMLItems = new JCheckBox("Show KML Objects");
        showKMLItems.setSelected(true);
        // showKMLItems.addChangeListener(new ChangeListener() {
        // @Override
        // public void stateChanged(ChangeEvent e) {
        // }
        // });

        FlowLayout flowLayout = new FlowLayout();
        flowLayout.setAlignment(FlowLayout.LEADING);
        toolButtonsPanel = new JPanel();
        toolButtonsPanel.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        toolButtonsPanel.setLayout(flowLayout);
        toolButtonsPanel.setSize(8, 8);
        toolButtonsPanel.add(new JLabel(NEPTUS_ICON));
        toolButtonsPanel.add(Box.createHorizontalStrut(10));
        toolButtonsPanel.add(showOptionsDialog);
        toolButtonsPanel.add(Box.createHorizontalStrut(10));
        toolButtonsPanel.add(addKMLFileButton);
        toolButtonsPanel.add(unloadAllKMLFilesButton);
        toolButtonsPanel.add(showKMLItems);

        headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.add(toolButtonsPanel);

        renderer = new StateRenderer2D(center) {
            @Override
            protected void init() {
                super.init();

                // This while is needed because the WorldRenderPainter on the StateRenderer2D.init() 
                //  is loaded on a thread so it may not be immediately in the renderer painters
                while (worldRenderPainter == null) {
                    try { Thread.sleep(100); } catch (InterruptedException e) { }
                    for (Renderer2DPainter painter : painters.getPreRenderPainters()) {
                        if (painter instanceof WorldRenderPainter) {
                            worldRenderPainter = (WorldRenderPainter) painter;
                            break;
                        }
                    }
                }
            }
        };
        renderer.setLevelOfDetail(levelOfDetail);
        renderer.setMapCenterShow(false);
        renderer.addPostRenderPainter(getKMLPainter(), "KML Painter");
        renderer.addMouseListener(getRendererMouseListener());
        renderer.addMouseMotionListener(getRendererMouseMotionListener());

        worldRenderPainter.setShowOnScreenControls(false);
        worldRenderPainter.setUseTransparency(false);

        levelOfDetailLabel = new JLabel("Zoom Level: " + levelOfDetail);
        zoomInButton = new JButton(new AbstractAction("+") {
            @Override
            public void actionPerformed(ActionEvent e) {
                renderer.zoomIn();
            }
        });
        zoomOutButton = new JButton(new AbstractAction("-") {
            @Override
            public void actionPerformed(ActionEvent e) {
                renderer.zoomOut();
            }
        });

        memInfoLabel = new JLabel();

        loadingTilesLabel = new JLabel();
        stopLoadingButton = new JButton(new AbstractAction("Stop") {
            @Override
            public void actionPerformed(ActionEvent e) {
                WorldRenderPainter.clearMemCache();
            }
        });

        saveImageButton = new JButton(new AbstractAction("Save Image") {
            @Override
            public void actionPerformed(ActionEvent e) {
                BufferedImage image = new BufferedImage(renderer.getWidth(), renderer.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = (Graphics2D) image.getGraphics();
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillRect(0, 0, image.getWidth(), image.getHeight());
                worldRenderPainter.paint((Graphics2D) image.getGraphics(), renderer, false);

                JFileChooser fc = createFileChooser("Save image as");
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                int option = fc.showSaveDialog(WorldMapPanel.this);
                File file = null;
                if (JFileChooser.APPROVE_OPTION == option) {
                    file = fc.getSelectedFile();
                }
                else
                    return;

                try {
                    String ext = FileUtil.getFileExtension(file);
                    if (!ext.toLowerCase().equalsIgnoreCase("png")) {
                        file = new File(file.getParent(), ext.equalsIgnoreCase("") ? file.getName() + ".png" : file
                                .getName().replaceFirst("\\." + ext + "$", ".png"));
                    }

                    ImageIO.write(image, "png", file);
                }
                catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        copyImageButton = new JButton(new AbstractAction("Copy Image") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copyImageButton.setEnabled(false);
                final BufferedImage image = new BufferedImage(renderer.getWidth(), renderer.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = (Graphics2D) image.getGraphics();
                g2.setColor(Color.LIGHT_GRAY);
                g2.fillRect(0, 0, image.getWidth(), image.getHeight());
                worldRenderPainter.paint((Graphics2D) image.getGraphics(), renderer, false);

                SwingWorker<Void, Void> sworker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        ClipboardOwner owner = new ClipboardOwner() {
                            public void lostOwnership(Clipboard clipboard, Transferable contents) {
                            }
                        };
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new Transferable() {
                            DataFlavor df = DataFlavor.imageFlavor;

                            @Override
                            public boolean isDataFlavorSupported(DataFlavor flavor) {
                                return df.equals(flavor);
                            }

                            @Override
                            public DataFlavor[] getTransferDataFlavors() {
                                return new DataFlavor[] { df };
                            }

                            @Override
                            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException,
                                    IOException {
                                return image;
                            }
                        }, owner);
                        return null;
                    }

                    @Override
                    protected void done() {
                        copyImageButton.setEnabled(true);
                    }
                };
                sworker.run();
            }
        });

        busyPanel = InfiniteProgressPanel.createBusyAnimationInfiniteBeans(20);
        // busyPanel.setVisible(false);

        statusBar = new JXStatusBar();
        statusBar.add(levelOfDetailLabel);
        statusBar.add(zoomInButton);
        statusBar.add(zoomOutButton);
        statusBar.add(memInfoLabel, JXStatusBar.Constraint.ResizeBehavior.FILL);
        statusBar.add(saveImageButton);
        statusBar.add(copyImageButton);
        statusBar.add(loadingTilesLabel);
        statusBar.add(stopLoadingButton);
        statusBar.add(busyPanel);

        // BorderLayout borderLayout = new BorderLayout();
        // borderLayout.setHgap(5);
        // borderLayout.setVgap(5);
        this.setLayout(new BorderLayout());

        this.add(headerPanel, BorderLayout.NORTH);
        this.add(renderer, BorderLayout.CENTER);
        this.add(statusBar, BorderLayout.SOUTH);
        // this.setSize(new Dimension(1024, 600));

        getTimer().scheduleAtFixedRate(getTimerTask(), 500, 200);
    }

    /**
     * @return
     */
    private Renderer2DPainter getKMLPainter() {
        if (kmlPainter == null) {
            kmlPainter = new Renderer2DPainter() {
                @Override
                public void paint(Graphics2D g, StateRenderer2D renderer) {
                    if (!showKMLItems.isSelected())
                        return;

                    for (String kmlFileName : placemarksHolder.keySet().toArray(
                            new String[placemarksHolder.keySet().size()])) {
                        Vector<KMLPlacemark> vecPlacemarks = placemarksHolder.get(kmlFileName);
                        for (KMLPlacemark kmlPlacemark : vecPlacemarks) {
                            if (!kmlPlacemark.visibility)
                                continue;

                            Graphics2D g2 = (Graphics2D) g.create();
                            LocationType loc = new LocationType();
                            loc.setLatitude(kmlPlacemark.latDegrees);
                            loc.setLongitude(kmlPlacemark.lonDegrees);
                            Point2D pointXY = renderer.getScreenPosition(loc);
                            g2.translate(pointXY.getX(), pointXY.getY());

                            GeneralPath pointer = new GeneralPath();
                            int upOff = -10, leftOff = -5, leafOff = 10;
                            pointer.moveTo(leftOff, upOff);
                            pointer.lineTo(0, 0);
                            pointer.lineTo(-leftOff, upOff);
                            pointer.curveTo(-leftOff, upOff - leafOff, leftOff, upOff - leafOff, leftOff, upOff);
                            pointer.closePath();
                            g2.setColor(ColorUtils.setTransparencyToColor(Color.GREEN, 160));
                            g2.fill(pointer);
                            g2.setColor(ColorUtils.setTransparencyToColor(Color.BLACK, 160));
                            Stroke oldStroke = g2.getStroke();
                            g2.setStroke(new BasicStroke(2));
                            g2.draw(pointer);
                            g2.setStroke(oldStroke);
                            // g2.fillOval(-5, -5 - 10, 10, 10);

                            g2.setColor(ColorUtils.setTransparencyToColor(Color.GREEN, 200));
                            g2.drawString(kmlPlacemark.name, -1, 15);
                            g2.drawString(kmlPlacemark.name, -3, 15);
                            g2.setColor(ColorUtils.setTransparencyToColor(Color.BLACK, 200));
                            g2.drawString(kmlPlacemark.name, -2, 14);

                            g2.dispose();
                        }
                    }

                    if (infoBalloonFeature != null && infoBalloonFeature.visibility) {
                        String txt = infoBalloonFeature.description != null ? infoBalloonFeature.description
                                : ("<html><b>"
                                        + infoBalloonFeature.name
                                        + "</b>"
                                        + (infoBalloonFeature.address != null ? "<br>" + infoBalloonFeature.address
                                                : "") + (infoBalloonFeature.phoneNumber != null ? "<br>"
                                        + infoBalloonFeature.phoneNumber : ""));
                        // JLabel label = new JLabel(txt);
                        JEditorPane label = new JEditorPane();
                        label.setEditable(false);
                        label.setBackground(Color.white);
                        label.addHyperlinkListener(new HyperlinkListener() {
                            @Override
                            public void hyperlinkUpdate(HyperlinkEvent e) {
                                if (e.getEventType() != EventType.ACTIVATED)
                                    return;

                                try {
                                    // label.setPage(e.getURL());
                                    // label.repaint();
                                }
                                catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
                        label.setContentType("text/html");
                        label.setText(txt);
                        label.setOpaque(true);
                        label.setBackground(ColorUtils.setTransparencyToColor(Color.WHITE, 200));
                        label.setForeground(ColorUtils.setTransparencyToColor(Color.BLACK, 200));

                        Graphics2D g2 = (Graphics2D) g.create();

                        // FontMetrics metrics = g2.getFontMetrics(label.getFont());
                        Dimension dim = label.getPreferredSize();
                        Rectangle2D bounds = new Rectangle2D.Double(0, 0, dim.getWidth(), dim.getHeight());
                        label.setBorder(new EmptyBorder(5, 5, 5, 5));
                        label.setBounds((int) (-bounds.getWidth() - 5), (int) (-bounds.getHeight() - 5),
                                (int) (bounds.getWidth() + 10), (int) (bounds.getHeight() + 10));

                        LocationType loc = new LocationType();
                        loc.setLatitude(infoBalloonFeature.latDegrees);
                        loc.setLongitude(infoBalloonFeature.lonDegrees);
                        Point2D pointXY = renderer.getScreenPosition(loc);
                        g2.translate(pointXY.getX(), pointXY.getY());

                        int offsetX = 20, offsetY = -20, boxBorderSize = 10;
                        Polygon pointerShape = new Polygon(new int[] { 0, offsetX,
                                offsetX + (int) Math.signum(offsetX) * 20 }, new int[] { 0,
                                offsetY + -(int) Math.signum(offsetY) * boxBorderSize,
                                offsetY + -(int) Math.signum(offsetY) * boxBorderSize }, 3);
                        g2.setColor(ColorUtils.setTransparencyToColor(Color.WHITE, 200));
                        g2.fill(pointerShape);

                        g2.translate(offsetX, offsetY - bounds.getHeight());
                        // g2.fillOval(-5, -5 - 10, 10, 10);
                        label.paint(g2);
                        g2.translate(-offsetX, -(offsetY - bounds.getHeight()));

                        g2.dispose();
                    }
                }
            };
        }
        return kmlPainter;
    }

    private boolean mouseActive = false;

    /**
     * @return the rendererMouseListener
     */
    private MouseListener getRendererMouseListener() {
        if (rendererMouseListener == null) {
            rendererMouseListener = new MouseListener() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!mouseActive)
                        return;
                    if (e.getButton() != MouseEvent.BUTTON1)
                        return;
                    infoBalloonFeature = null;
                    int offSet = 20, offSetDiv2 = offSet / 2;
                    for (String key : placemarksHolder.keySet().toArray(new String[placemarksHolder.keySet().size()])) {
                        for (KMLPlacemark pmk : placemarksHolder.get(key)) {
                            if (!pmk.visibility)
                                continue;
                            LocationType loc = new LocationType();
                            loc.setLatitude(pmk.latDegrees);
                            loc.setLongitude(pmk.lonDegrees);
                            Point2D pointXY = renderer.getScreenPosition(loc);
                            Rectangle2D bbox = new Rectangle2D.Double(pointXY.getX() - offSetDiv2, pointXY.getY()
                                    - offSetDiv2, offSet, offSet);
                            // System.out.println("mouseClicked > " + e.getX() + " :: " + e.getY() + "   " +
                            // pointXY.getX() + " :: " + pointXY.getY() + "    " +
                            // bbox.contains((Point2D)e.getPoint()));
                            if (bbox.contains((Point2D) e.getPoint())) {
                                infoBalloonFeature = pmk;
                                break;
                            }
                        }

                    }
                    // System.out.println("mouseClicked > " + e.getX() + " :: " + e.getY());
                }

                @Override
                public void mousePressed(MouseEvent e) {
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    mouseActive = true;
                    // System.out.println("mouseEntered > " + e.getX() + " :: " + e.getY());
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mouseActive = false;
                    // System.out.println("mouseExited > " + e.getX() + " :: " + e.getY());
                }
            };
        }
        return rendererMouseListener;
    }

    /**
     * @return the rendererMouseMotionListener
     */
    private MouseMotionListener getRendererMouseMotionListener() {
        if (rendererMouseMotionListener == null) {
            rendererMouseMotionListener = new MouseMotionListener() {
                @Override
                public void mouseMoved(MouseEvent e) {
                    if (!mouseActive)
                        return;
                    // for (HoveringButton hb : controlRenderButtons) {
                    // Rectangle2D ret = hb.createRectangle2DBounds();
                    // if(ret.contains((Point2D)e.getPoint()))
                    // hb.setHovering(true);
                    // else
                    // hb.setHovering(false);
                    // }
                    // System.out.println("mouseMoved > " + e.getX() + " :: " + e.getY());
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                }
            };
        }
        return rendererMouseMotionListener;
    }

    /**
     * 
     */
    public void dispose() {
        if (timerTask != null) {
            timerTask.cancel();
            timerTask = null;
        }

        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#finalize()
     */
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    /**
     * @return the timer
     */
    public Timer getTimer() {
        if (timer == null) {
            timer = new Timer(WorldMapPanel.class.getSimpleName() + " [" + Integer.toHexString(this.hashCode()) + "]", true);
        }
        return timer;
    }

    /**
     * @return the timerTask
     */
    public TimerTask getTimerTask() {
        if (timerTask == null) {
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    center.setLocation(renderer.getCenter());
                    levelOfDetail = renderer.getLevelOfDetail();
                    long nlt = WorldRenderPainter.getNumberOfLoadingMapTiles();
                    long nlt1 = WorldRenderPainter.getNumberOfLoadedMapTiles();

                    levelOfDetailLabel.setText("Zoom Level: " + levelOfDetail);
                    memInfoLabel.setText("Free Memory: "
                            + MathMiscUtils.parseToEngineeringRadix2Notation(Runtime.getRuntime().freeMemory(), 1)
                            + "B of "
                            + MathMiscUtils.parseToEngineeringRadix2Notation(Runtime.getRuntime().totalMemory(), 1)
                            + "B");
                    loadingTilesLabel.setText("Tiles Loading: " + nlt + " of " + nlt1);
                    busyPanel.setVisible(nlt != 0);
                    busyPanel.setBusy(nlt != 0);

                    WorldMapPanel.this.invalidate();
                    WorldMapPanel.this.validate();
                    WorldMapPanel.this.repaint();
                    renderer.repaint();
                }
            };
        }
        return timerTask;
    }

    public synchronized void savePropertiesToDisk() {
        try {
            PluginUtils.saveProperties(ROOT_PREFIX + "conf/" + WorldMapPanel.class.getSimpleName().toLowerCase()
                    + ".properties", WorldMapPanel.this);
        }
        catch (Exception e) {
            NeptusLog.pub().error(
                    "Not possible to open \"conf/" + WorldMapPanel.class.getSimpleName().toLowerCase()
                            + ".properties\"");
        }
    }

    private JFrame getJFrame(String title) {
        String defaultTitle = WorldMapPanel.class.getSimpleName();
        if (title == null)
            jFrame = new JFrame(defaultTitle);
        else if (title.equalsIgnoreCase(""))
            jFrame = new JFrame(defaultTitle);
        else
            jFrame = new JFrame(title);
        jFrame.setPreferredSize(new Dimension(880, 600));
        jFrame.getContentPane().setLayout(new BorderLayout());
        jFrame.getContentPane().add(this);
        jFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                savePropertiesToDisk();
                hideFrame();
            }
        });
        GuiUtils.centerOnScreen(jFrame);
        jFrame.setIconImages(ConfigFetch.getIconImagesForFrames());
        jFrame.setVisible(true);

        return jFrame;
    }

    private void hideFrame() {
        if (jFrame != null) {
            jFrame.setVisible(false);
            jFrame.dispose();
        }
        if (jInternalFrame != null) {
            jInternalFrame.setVisible(false);
            jInternalFrame.doDefaultCloseAction();
            jInternalFrame.dispose();
        }

        dispose();
    }

    /**
     * @param internalFrame The jInternalFrame to set.
     */
    public void setJInternalFrame(JInternalFrame internalFrame) {
        jInternalFrame = internalFrame;
        jInternalFrame.addInternalFrameListener(new InternalFrameAdapter() {
            public void internalFrameClosed(InternalFrameEvent e) {
            }
        });
    }

    private JFileChooser createFileChooser(String name) {
        JFileChooser fc = new JFileChooser();
        fc.setName(name);
        fc.setFileFilter(new FileNameExtensionFilter("PNG images", "png", "PNG"));
        fc.setMultiSelectionEnabled(false);
        File last = new File(".");
        if (lastSuccessSavedDir != null && lastSuccessSavedDir.exists()) {
            last = lastSuccessSavedDir.isDirectory() ? lastSuccessSavedDir : lastSuccessSavedDir.getParentFile();
        }
        fc.setCurrentDirectory(last);
        return fc;
    }

    private JFileChooser createFileKMLChooser(String name) {
        JFileChooser fc = new JFileChooser();
        fc.setName(name);
        fc.setFileFilter(new FileNameExtensionFilter("KML File", "kml", "KML"));
        fc.setMultiSelectionEnabled(false);
        File last = new File(".");
        if (lastSuccessKMLDir != null && lastSuccessKMLDir.exists()) {
            last = lastSuccessKMLDir.isDirectory() ? lastSuccessKMLDir : lastSuccessKMLDir.getParentFile();
        }
        fc.setCurrentDirectory(last);
        return fc;
    }

    private Vector<KMLPlacemark> getPlacemarksFromKML(File kmlFile) {
        Vector<KMLPlacemark> ret = new Vector<WorldMapPanel.KMLPlacemark>();
        try {
            DocumentBuilder builder = docBuilderFactory.newDocumentBuilder();
            // ByteArrayInputStream bais = new ByteArrayInputStream(kmlString.getBytes());
            FileInputStream fis = new FileInputStream(kmlFile);
            Document docProfiles = builder.parse(fis);
            Element root = docProfiles.getDocumentElement();

            // Node bn = root.getFirstChild();
            // while (bn != null) {
            // if ("Default".equalsIgnoreCase(bn.getNodeName())) {
            // try {
            // String dName = bn.getAttributes().getNamedItem("profile").getTextContent();
            // if (!"".equals(dName))
            // defaultProfile = dName;
            // } catch (Exception e) {
            // NeptusLog.pub().debug(ReflectionUtil.getCallerStamp() + e.getMessage());
            // }
            // }
            // bn = bn.getNextSibling();
            // }

            NodeList placemarkList = root.getElementsByTagName("Placemark");
            for (int i = 0; i < placemarkList.getLength(); i++) {
                Element placeMarkNd = (Element) placemarkList.item(i);
                String idAttStr = placeMarkNd.getAttribute("id");
                String id = null;
                if (idAttStr != null && !"".equalsIgnoreCase(idAttStr))
                    id = idAttStr;

                NodeList nameNdLst = placeMarkNd.getElementsByTagName("name");
                if (nameNdLst.getLength() < 1) {
                    continue;
                }
                String name = ((Element) nameNdLst.item(0)).getTextContent();
                if (name == null || "".equalsIgnoreCase(name)) {
                    continue;
                }
                if (id == null)
                    id = name;

                KMLPlacemark pmk = new KMLPlacemark(id, name);

                Node parentNd = placeMarkNd.getParentNode();
                if (parentNd != null) {
                    if ("Folder".equalsIgnoreCase(parentNd.getNodeName())) {
                        NodeList nameNd = ((Element) parentNd).getElementsByTagName("name");
                        if (nameNd.getLength() > 0) {
                            String nameP = ((Element) nameNd.item(0)).getNodeValue();
                            pmk.folder = nameP;
                        }
                    }
                }

                boolean errorState = false;
                Node bn = placeMarkNd.getFirstChild();
                while (bn != null) {
                    try {
                        if ("styleUrl".equalsIgnoreCase(bn.getNodeName())) {
                            try {
                                String dName = bn.getTextContent();
                                pmk.styleUrl = dName;
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if ("visibility".equalsIgnoreCase(bn.getNodeName())) {
                            try {
                                String dName = bn.getTextContent();
                                pmk.visibility = extractAsBoolean(dName);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if ("address".equalsIgnoreCase(bn.getNodeName())) {
                            try {
                                String dName = bn.getTextContent();
                                pmk.address = dName;
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if ("phoneNumber".equalsIgnoreCase(bn.getNodeName())) {
                            try {
                                String dName = bn.getTextContent();
                                pmk.phoneNumber = dName;
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if ("description".equalsIgnoreCase(bn.getNodeName())) {
                            try {
                                String dName = bn.getTextContent();
                                pmk.description = dName;
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        else if ("Point".equalsIgnoreCase(bn.getNodeName())) {
                            Node cn = bn.getFirstChild();
                            while (cn != null) {
                                if ("coordinates".equalsIgnoreCase(cn.getNodeName())) {
                                    try {
                                        String dName = cn.getTextContent();
                                        double[] pointCoord = extractPointCoordinates(dName);
                                        pmk.latDegrees = pointCoord[0];
                                        pmk.lonDegrees = pointCoord[1];
                                        pmk.altitude = pointCoord[2];
                                    }
                                    catch (Exception e) {
                                        e.printStackTrace();
                                        errorState = true;
                                    }
                                }
                                cn = cn.getNextSibling();
                            }
                        }
                    }
                    catch (Exception e) {
                        errorState = true;
                    }
                    bn = bn.getNextSibling();
                }

                if (!errorState) {
                    ret.add(pmk);
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * @param dName
     * @return
     */
    private double[] extractPointCoordinates(String value) throws ArrayIndexOutOfBoundsException, NumberFormatException {
        // coordinates lon,lat[,alt]
        String[] sValues = value.split(",");
        if (sValues.length < 2)
            throw new ArrayIndexOutOfBoundsException("At least 2 values are mandatory, found " + sValues.length);
        double[] ret = new double[3];
        ret[0] = Double.parseDouble(sValues[1]);
        ret[1] = Double.parseDouble(sValues[0]);
        if (sValues.length < 3)
            ret[2] = 0;
        else
            ret[2] = Double.parseDouble(sValues[2]);

        return ret;
    }

    /**
     * @param value
     * @return
     */
    private boolean extractAsBoolean(String value) {
        if ("1".equalsIgnoreCase(value))
            value = "true";
        else if ("0".equalsIgnoreCase(value))
            value = "false";
        boolean boolValue = Boolean.parseBoolean(value);
        return boolValue;
    }

    private static class KMLFeatureAbstractType {
        public final String id;
        public String name;
        public boolean visibility = true;
        public String address = null;
        public String phoneNumber = null;
        public String description = null;

        public String styleUrl = null;

        public String folder = null;

        public KMLFeatureAbstractType(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public KMLFeatureAbstractType(String name) {
            this.id = name;
            this.name = name;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof KMLFeatureAbstractType)
                return id.equals(((KMLFeatureAbstractType) obj).id);
            return false;
        }
    }

    private static class KMLPlacemark extends KMLFeatureAbstractType {
        public double latDegrees = 0;
        public double lonDegrees = 0;
        public double altitude = 0;

        public KMLPlacemark(String id, String name) {
            super(id, name);
        }

        public KMLPlacemark(String name) {
            super(name);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof KMLPlacemark)
                return id.equals(((KMLPlacemark) obj).id);
            return false;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        OutputMonitor.setDisable(true);
        // BasicConfigurator.resetConfiguration();
        Logger.getRootLogger().setLevel(Level.FATAL);
        NeptusLog.pubRoot().setLevel(Level.FATAL);

        ConfigFetch.initialize();

        Logger.getRootLogger().setLevel(Level.FATAL);
        NeptusLog.wasteRoot().setLevel(Level.OFF);
        NeptusLog.pubRoot().setLevel(Level.FATAL);

        if (ConfigFetch.isOSEqual(ConfigFetch.OS_LINUX))
            GuiUtils.setLookAndFeel();
        else
            GuiUtils.setSystemLookAndFeel();

        PluginUtils.loadPlugins();

        WorldMapPanel panel = new WorldMapPanel();
        panel.getJFrame(null);
        panel.jFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        panel.jFrame.setSize(790, 580);
        GuiUtils.centerOnScreen(panel.jFrame);
    }
}
