/*
 * Copyright (c) 2004-2025 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * 18/11/2023
 */
package pt.lsts.neptus.plugins.videoreader;

import com.google.common.eventbus.Subscribe;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.FuelLevel;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.RenderStringUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.conf.PreferencesListener;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static pt.lsts.neptus.util.AngleUtils.nomalizeAngleDegrees180;
import static pt.lsts.neptus.util.AngleUtils.nomalizeAngleDegrees360;
import static pt.lsts.neptus.util.ImageUtils.getImage;
import static pt.lsts.neptus.util.ImageUtils.getScaledImage;

@PluginDescription(name = "Video Reader", version = "0.1", experimental = true, author = "Paulo Dias",
        description = "Plugin to view IP Camera streams using FFMPEG", icon = "images/menus/camera.png",
        category = PluginDescription.CATEGORY.INTERFACE)
@Popup(name = "Video Reader", width = 640, height = 480, icon = "images/menus/camera.png")
public class VideoReader extends ConsolePanel implements PreferencesListener {
    static final String BASE_FOLDER_FOR_URL_INI = "ipUrl.ini";

    private static final int DEFAULT_WIDTH_CONSOLE = 640;
    private static final int DEFAULT_HEIGHT_CONSOLE = 480;

    private static final int MAX_NULL_FRAMES_FOR_RECONNECT = 10;
    public static final String IMAGE_NO_VIDEO = "images/novideo.png";

    final static Color LABEL_WHITE_COLOR = new Color(255, 255, 255, 200);

    final ExecutorService service = Executors.newCachedThreadPool(new ThreadFactory() {
        private final String namePrefix = VideoReader.class.getSimpleName() + "::"
                + Integer.toHexString(VideoReader.this.hashCode());
        private final AtomicInteger counter = new AtomicInteger(0);
        private final ThreadGroup group = new ThreadGroup(namePrefix);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r);
            t.setName(VideoReader.class.getCanonicalName() + " " + (counter.getAndIncrement()));
            t.setDaemon(true);
            return t;
        }
    });

    @NeptusProperty(name = "Camera URL", editable = false)
    private String camUrl = ""; //rtsp://10.0.20.207:554/live/ch01_0
    private String camName = "";

    private AtomicInteger threadsIdCounter = new AtomicInteger(0);


    private PlayerOpenCv player;

    private BufferedImage offlineImage;
    private BufferedImage onScreenImage;
    private BufferedImage onScreenImageLastGood;

    // Flag for Histogram image
    private boolean histogramFlag = false;

    private int widthImgRec;
    // Height size of image
    private int heightImgRec;
    // Width size of Console
    private int widthConsole = DEFAULT_WIDTH_CONSOLE;
    // Height size of Console
    private int heightConsole = DEFAULT_HEIGHT_CONSOLE;
    // Scale factor of x pixel
    private float xScale;
    // Scale factor of y pixel
    private float yScale;
    //private CameraFOV camFov = null;
    private Point2D mouseLoc = null;

    private ArrayList<Camera> cameraList;
    private boolean closingPanel = false;

    private boolean refreshTemp;
    private boolean paused = false;

    // JPopup Menu
    private JPopupMenu popup;
    private final IpCamManagementPanel ipCamManagementPanel;
    // JTextField for IPCam name
    private final JLabel streamNameJLabel;
    private final JLabel streamWarnJLabel;

    private double lastAspectRatio = (double) widthConsole /heightConsole;

    // JLabel for additional information
    private String positionLabel = "";
    private String rpyLabel = "";
    private String velLabel = "";
    private String depthLabel = "";
    private String altitudeLabel = "";
    private String fuelLabel = "";

    private final Image fuelFullImage = getImage("images/full_battery_video.png");
    private final Image fuelAboveHalfImage = getImage("images/above_half_battery_video.png");
    private final Image fuelBelowHalfImage = getImage("images/below_half_battery_video.png");
    private final Image fuelLowImage = getImage("images/low_battery_video.png");
    private final Image fuelEmptyImage = getImage("images/no_battery_video.png");
    private Image fuelImage = getImage("images/no_battery_video.png");
    private Image fuelIcon = getScaledImage(fuelImage, 18, 18, false);
    private final Image positionImage = getImage("images/position_video.png");
    private Image positionIcon = getScaledImage(positionImage, 18, 18, false);
    private final Image rpyImage = getImage("images/rpy_video.png");
    private Image rpyIcon = getScaledImage(rpyImage, 18, 18, false);
    private final Image velImage = getImage("images/speed_video.png");
    private Image velIcon = getScaledImage(velImage, 18, 18, false);
    private final Image depthImage = getImage("images/depth_video.png");
    private Image depthIcon = getScaledImage(depthImage, 18, 18, false);
    private final Image altitudeImage = getImage("images/altitude_video.png");
    private Image altitudeIcon = getScaledImage(altitudeImage, 18, 18, false);

    private Image lastFuelImage = fuelImage;

    private int infoFontSize = 14;

    private JMenuItem showInfoItem;

    private boolean showVehicleInfo = false;

    private double positionLatDeg = Double.NaN;
    private double positionLonDeg = Double.NaN;

    public VideoReader(ConsoleLayout console) {
        this(console, false);
    }

    public VideoReader(ConsoleLayout console, boolean usedInsideAnotherConsolePanel) {
        super(console, usedInsideAnotherConsolePanel);

        removeAll();

        initPopupMenu();

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                updateSize(evt);
            }

            @Override
            public void componentShown(ComponentEvent evt) {
                updateSize(evt);
            }

            private void updateSize(ComponentEvent evt) {
                Component c = evt.getComponent();
                updateSizeVariables(c);
                if (isDisconnect()) {
                    setupNoVideoImage();
                }

                if (player != null) {
                    player.sizeChange(c.getSize());
                }
            }
        });

        this.setToolTipText(I18n.text("not connected"));

        ipCamManagementPanel = new IpCamManagementPanel(this, (name, url) -> {
                    camName = name;
                    camUrl = url;
                    return null;
                },
                this::connectStream);

        // Mouse click
        mouseListenerInit();

        streamNameJLabel = new JLabel();
        streamNameJLabel.setForeground(LABEL_WHITE_COLOR);
        streamNameJLabel.setBackground(new Color(0, 0, 0, 80));
        streamNameJLabel.setOpaque(true);
        streamNameJLabel.setHorizontalAlignment(SwingConstants.CENTER);
        streamNameJLabel.setVerticalAlignment(SwingConstants.TOP);
        streamNameJLabel.setVerticalTextPosition(SwingConstants.TOP);

        streamWarnJLabel = new JLabel();
        streamWarnJLabel.setForeground(LABEL_WHITE_COLOR);
        streamWarnJLabel.setOpaque(false);
        streamWarnJLabel.setHorizontalAlignment(SwingConstants.CENTER);
        streamWarnJLabel.setVerticalAlignment(SwingConstants.BOTTOM);
        streamWarnJLabel.setVerticalTextPosition(SwingConstants.BOTTOM);
        streamWarnJLabel.setText("⚠");
    }

    @Override
    public void initSubPanel() {
        GeneralPreferences.addPreferencesListener(this);
        service.execute(Util::createIpUrlFile);
        //setMainVehicle(getConsole().getMainSystem());
    }

    @Override
    public void cleanSubPanel() {
        GeneralPreferences.removePreferencesListener(this);
        closingPanel = true;
        service.shutdown();
        disconnectStream();
    }

    @Override
    protected void paintComponent(Graphics g) {
        boolean warn = false;
        if (refreshTemp && onScreenImage != null) {
            g.drawImage(onScreenImage, 0, 0, this);
            refreshTemp = false;
        }
        else if (onScreenImageLastGood != null && (onScreenImageLastGood.getWidth() == widthConsole
                && onScreenImageLastGood.getHeight() == heightConsole)) {
            g.drawImage(onScreenImageLastGood, 0, 0, this);
            warn = true;
        }
        else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, (int) widthConsole, (int) heightConsole);
        }

        if (isConnect() || isConnecting() || isDisconnecting()) {
            String text = camName; //ipCamManagementPanel.getStreamName();
            Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
            streamNameJLabel.setText(text);
            streamNameJLabel.setSize((int) widthConsole, (int) bounds.getHeight() + 5);
            streamNameJLabel.paint(g);

            int x = 10;
            int y = heightConsole;
            int iconSpacing = 5;
            double scaleFactor = (double) (Math.min(widthConsole, heightConsole)) / ((double) (DEFAULT_WIDTH_CONSOLE + DEFAULT_HEIGHT_CONSOLE) / 2);
            int fontSize = validateFontSize(g, (int) (14 * scaleFactor), positionLabel);
            int iconSize = fontSize + 8;
            Font font = new Font("Arial", Font.PLAIN, fontSize);
            int lineHeight = (int) (fontSize * 2);

            if (showVehicleInfo) {
                double aspectRatio = (double) widthConsole / heightConsole;
                if (lastAspectRatio != aspectRatio) {
                    lastAspectRatio = aspectRatio;

                    fuelIcon = getScaledImage(fuelImage, iconSize, iconSize, false);
                    rpyIcon = getScaledImage(rpyImage, iconSize, iconSize, false);
                    velIcon = getScaledImage(velImage, iconSize, iconSize, false);
                    depthIcon = getScaledImage(depthImage, iconSize, iconSize, false);
                    altitudeIcon = getScaledImage(altitudeImage, iconSize, iconSize, false);
                    positionIcon = getScaledImage(positionImage, iconSize, iconSize, false);
                }

                if (lastFuelImage != fuelImage) {
                    lastFuelImage = fuelImage;
                    fuelIcon = getScaledImage(fuelImage, iconSize, iconSize, false);
                }

                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                AlphaComposite composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); // 50% opacity
                g2d.setComposite(composite);
                g2d.setColor(Color.BLACK);

                int positionLabelWidth = getLabelWidth(g, fontSize, positionLabel);
                int leftPanelRectWidth = (3 * x) + positionIcon.getWidth(this) + iconSpacing + positionLabelWidth; //(int) (200 * scaleFactor);
                int leftPanelRectHeight = (int) ((lineHeight * 4) + (fontSize));
                g2d.fillRoundRect(-x, y - leftPanelRectHeight, leftPanelRectWidth, leftPanelRectHeight + x, 30, 30);

                int rpyLabelWidth = getLabelWidth(g, fontSize, rpyLabel);
                int rightPanelRectWidth = (3 * x) + rpyIcon.getWidth(this) + iconSpacing + rpyLabelWidth; //(int) (200 * scaleFactor);
                int rightPanelRectHeight = (int) ((lineHeight * 3) + (fontSize));
                g2d.fillRoundRect(x + widthConsole - rightPanelRectWidth, y - rightPanelRectHeight, rightPanelRectWidth + x, rightPanelRectHeight + x, 30, 30);

                Graphics2D g2dInfo = (Graphics2D) g;
                RenderStringUtils.drawStringVideo(g2dInfo, font, Color.WHITE, fuelLabel, widthConsole, widthConsole, lineHeight * 2, fuelIcon, iconSpacing, lineHeight, true, true, this);
                RenderStringUtils.drawStringVideo(g2dInfo, font, Color.WHITE, velLabel, widthConsole, widthConsole, y - (lineHeight * 2), velIcon, iconSpacing, lineHeight, false, true, this);
                RenderStringUtils.drawStringVideo(g2dInfo, font, Color.WHITE, rpyLabel, widthConsole, widthConsole, y - (lineHeight), rpyIcon, iconSpacing, lineHeight, false, true, this);
                RenderStringUtils.drawStringVideo(g2dInfo, font, Color.WHITE, depthLabel, widthConsole, x, y - (lineHeight * 3), depthIcon, iconSpacing, lineHeight, false, false, this);
                RenderStringUtils.drawStringVideo(g2dInfo, font, Color.WHITE, altitudeLabel, widthConsole, x, y - (lineHeight * 2), altitudeIcon, iconSpacing, lineHeight, false, false, this);
                RenderStringUtils.drawStringVideo(g2dInfo, font, Color.WHITE, positionLabel, widthConsole, x, y - (lineHeight), positionIcon, iconSpacing, lineHeight, false, false, this);

                g2dInfo.dispose();
                g2d.dispose();
            }

            if (warn) {
                streamWarnJLabel.setSize((int) widthConsole, (int) heightConsole);
                streamWarnJLabel.paint(g);
            }
        }
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
        fuelLabel = "";
        velLabel = "";
        rpyLabel = "";
        depthLabel = "";
        altitudeLabel = "";
        positionLabel = "";
        fuelImage = fuelEmptyImage;
    }

    private int validateFontSize(Graphics g, int fontSize, String text){
        Graphics2D gTemp = (Graphics2D) g.create();
        Font font = new Font("Arial", Font.PLAIN, fontSize);
        gTemp.setFont(font);
        gTemp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gTemp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fm = gTemp.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int res = (textWidth * 100) / widthConsole;
        gTemp.dispose();
        if (res > 40) {
            return infoFontSize;
        }
        infoFontSize = fontSize;
        return fontSize;
    }

    private int getLabelWidth(Graphics g, int fontSize, String text){
        Graphics2D gTemp = (Graphics2D) g.create();
        Font font = new Font("Arial", Font.PLAIN, fontSize);
        gTemp.setFont(font);
        gTemp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gTemp.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        FontMetrics fm = gTemp.getFontMetrics();
        gTemp.dispose();
        return fm.stringWidth(text);
    }

    private boolean isConnect() {
        return player != null && player.isStreamingActive() && !player.isStopRequest();
    }

    private boolean isConnecting() {
        return player != null && !player.isStreamingActive() && !player.isStopRequest()
                && !player.isStreamingFinished();
    }

    private boolean isDisconnecting() {
        return player != null && player.isStreamingActive() && player.isStopRequest();
    }

    private boolean isDisconnect() {
        return player == null || player.isStreamingFinished() || player.isStopRequest();
    }

    private void setupNoVideoImage() {
        Image noVideoImage = getImage(IMAGE_NO_VIDEO);
        if (noVideoImage == null) {
            BufferedImage blackImage = ImageUtils.createCompatibleImage(1, 1, 255);
            blackImage.setRGB(0, 0, 0);
        }
        noVideoImage = noVideoImage != null && noVideoImage.getWidth(null) > 0
                    && noVideoImage.getHeight(null) > 0
                    && widthConsole >= 0 && heightConsole >= 0
                //? ImageUtils.getScaledImage(noVideoImage, widthConsole, heightConsole, true)
                ? Util.resizeBufferedImage(ImageUtils.toBufferedImage(getImage("images/novideo.png")), new Dimension(widthConsole, heightConsole))
                : noVideoImage;

        BufferedImage onScreenImage = noVideoImage == null
                ? null
                : ImageUtils.toBufferedImage(noVideoImage);
        showImage(onScreenImage);
    }

    private void connectStream() {
        if (isConnect() || isConnecting()) {
            disconnectStream();
        }

        String cid = String.format("%05X-%d", VideoReader.this.hashCode(), threadsIdCounter.getAndIncrement());
        try {
            //player = new Player(String.format("%05X-%d", VideoReader.this.hashCode(), threadsIdCounter.getAndIncrement()), service);
            player = new PlayerOpenCv(cid, service);
            player.sizeChange(new Dimension(widthConsole, heightConsole));
            player.setHistogramFlag(histogramFlag);
            player.start(camUrl /*fieldUrl.getText()*/, image -> {
                //BufferedImage scaledImage = ImageUtils.toBufferedImage(ImageUtils.getFastScaledImage(image, widthConsole, heightConsole, true));
                BufferedImage scaledImage = image;
                showImage(scaledImage);
                return null;
            });
        }
        catch (Exception | Error e) {
            String error = cid + " :: ERROR :: " + e.getMessage();
            NeptusLog.pub().error(error);
            getConsole().post(Notification.warning(PluginUtils.getPluginName(this.getClass()), error));
            if (player != null) {
                player.setStopRequest();
                player = null;
            }
        }

        repaint(100);
    }

    private void disconnectStream() {
        if (player == null) {
            return;
        }

        PlayerOpenCv playerToDisconnect = player;
        player = null;
        playerToDisconnect.setStopRequest();

        onScreenImageLastGood = null;
        setupNoVideoImage();
        repaint(100);
    }

    private void showImage(BufferedImage image) {
        if (!paused) {
            if (onScreenImage != null) {
                onScreenImageLastGood = onScreenImage;
            }

            onScreenImage = image;
        }
        refreshTemp = true;
        repaint();
    }

    @Periodic(millisBetweenUpdates = 1_000)
    public void updateToolTip() {
        String tooltipText = I18n.text("not connected");
        if (isConnecting()) {
            tooltipText = I18n.text("connecting to") + " " + ipCamManagementPanel.getStreamName();
        }
        else if (isConnect()) {
            tooltipText = I18n.text("streaming from") + " " + ipCamManagementPanel.getStreamName();
        }
        else if (isDisconnecting()) {
            tooltipText = I18n.text("disconnecting from") + " " + ipCamManagementPanel.getStreamName();
        }
        this.setToolTipText(I18n.text(tooltipText));

        if (isDisconnect()) {
            onScreenImageLastGood = null;
            setupNoVideoImage();
        }
        repaint(500);
    }

    private void updateSizeVariables(Component comp) {
        widthConsole = comp.getSize().width;
        heightConsole = comp.getSize().height;
        xScale = (float) widthConsole / widthImgRec;
        yScale = (float) heightConsole / heightImgRec;
        //size = new Size(widthConsole, heightConsole);
    }

    // Mouse click Listener
    private void mouseListenerInit() {
        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
//                if (camFov != null) {
//                    double width = ((Component) e.getSource()).getWidth();
//                    double height = ((Component) e.getSource()).getHeight();
//                    double x = e.getX();
//                    double y = height - e.getY();
//                    mouseLoc = new Point2D.Double((x / width - 0.5) * 2, (y / height - 0.5) * 2);
//                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                mouseLoc = null;
                //post(new EventMouseLookAt(null));
            }

            public void mouseClicked(MouseEvent e) {
//                if (SwingUtilities.isLeftMouseButton(e) && e.isControlDown()) {
//                    if (camFov != null) {
//                        double width = ((Component) e.getSource()).getWidth();
//                        double height = ((Component) e.getSource()).getHeight();
//                        double x = e.getX();
//                        double y = height - e.getY();
//                        mouseLoc = new Point2D.Double((x / width - 0.5) * 2, (y / height - 0.5) * 2);
//                        LocationType loc = camFov.getLookAt(mouseLoc.getX(), mouseLoc.getY());
//                        loc.convertToAbsoluteLatLonDepth();
//                        String id = placeLocationOnMap(loc);
//                        snap = new StoredSnapshot(id, loc, e.getPoint(), onScreenImage, new Date());
//                        snap.setCamFov(camFov);
//                        try {
//                            snap.store();
//                        }
//                        catch (Exception ex) {
//                            NeptusLog.pub().error(ex);
//                        }
//                    }
//                }

                if (e.getButton() == MouseEvent.BUTTON3) {
                    popup.show((Component) e.getSource(), e.getX(), e.getY());
                }
            }
        });
    }

    private void initPopupMenu() {
        popup = new JPopupMenu();
        JMenuItem item;

        popup.add(item = new JMenuItem(I18n.text("Connect to stream"),
                        ImageUtils.createImageIcon("images/menus/camera.png")))
                .addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        openIPCamManagementPanel();
                        //service.execute(VideoReader.this::connectStream);
                    }
                });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_MASK));

        popup.add(item = new JMenuItem(I18n.text("Close stream connection"),
                        ImageUtils.createImageIcon("images/menus/exit.png")))
                .addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        NeptusLog.pub().info("Closing video stream");
                        service.execute(VideoReader.this::disconnectStream);
//                                    noVideoLogoState = false;
//                                    isCleanTurnOffCam = true;
//                                    state = false;
//                                    ipCam = false;
//                                    closeCapture(capture);
                        repaint(500);
                    }
                });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_MASK));

        popup.addSeparator();

        popup.add(item = new JMenuItem(I18n.text("Toggle Histogram filter"),
                        ImageUtils.createImageIcon("images/menus/histogram.png")))
                .addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        histogramFlag = !histogramFlag;
                        if (player != null) {
                            player.setHistogramFlag(histogramFlag);
                        }
                    }
                });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_MASK));

        popup.add(item = new JMenuItem(I18n.text("Maximize window"),
                        ImageUtils.createImageIcon("images/menus/maximize.png")))
                .addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        maximizeVideoStreamPanel();
                    }
                });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_MASK));

        popup.addSeparator();

        showInfoItem = new JCheckBoxMenuItem(I18n.text("Show vehicle information"));

        showInfoItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                showVehicleInformation();
            }
        });

        popup.add(showInfoItem);


//                    popup.addSeparator();

//                    JLabel infoZoom = new JLabel(I18n.text("For zoom use Alt-Z"));
//                    infoZoom.setEnabled(false);
//                    popup.add(infoZoom, JMenuItem.CENTER_ALIGNMENT);

//                    JLabel markSnap = new JLabel(I18n.text("Ctr+Click to mark frame in the map"));
//                    markSnap.setEnabled(false);
//                    popup.add(markSnap, JMenuItem.CENTER_ALIGNMENT);

    }

    private void showVehicleInformation() {
        showVehicleInfo = !showVehicleInfo;
    }

    private void maximizeVideoStreamPanel() {
        JDialog dialog = (JDialog) SwingUtilities.getWindowAncestor(VideoReader.this);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize().getSize();
        if (dialog.getSize().equals(screenSize)) {
            // Already maximized
            screenSize = new Dimension(DEFAULT_WIDTH_CONSOLE, DEFAULT_HEIGHT_CONSOLE);
        }
        dialog.setSize(screenSize);
        // We call the resize with its own size to call componentResized
        // method of the componentAdapter set in the constructor
        VideoReader.this.setSize(VideoReader.this.getSize());
    }

    // Read ipUrl.ini to find IPCam ON
    private void openIPCamManagementPanel() {
        // JPanel for IPCam Select (MigLayout)
        ipCamManagementPanel.show(camUrl);
    }

    @Subscribe
    public void on(EstimatedState msg) {
        String mainVehicleId = getMainVehicleId();
        if (!msg.getSourceName().equals(mainVehicleId))
            return;

        double latDeg = Math.toDegrees(msg.getLat());
        double lonDeg = Math.toDegrees(msg.getLon());
        LocationType position = new LocationType(latDeg, lonDeg);
        position.setOffsetNorth(msg.getX());
        position.setOffsetEast(msg.getY());
        position.setOffsetDown(msg.getZ());
        position.convertToAbsoluteLatLonDepth();
        positionLatDeg = position.getLatitudeDegs();
        positionLonDeg = position.getLongitudeDegs();
        String latStr = position.getLatitudeAsPrettyString();
        String lonStr = position.getLongitudeAsPrettyString();
        double roll = nomalizeAngleDegrees180(Math.toDegrees(msg.getPhi()));
        String rollStr = String.format("%+04d", (int) roll).replace("+"," ");
        double pitch = nomalizeAngleDegrees180(Math.toDegrees(msg.getTheta()));
        String pitchStr = String.format("%+04d", (int) pitch).replace("+"," ");
        double yaw = nomalizeAngleDegrees360(Math.toDegrees(msg.getPsi()));
        String yawStr = String.format("%+04d", (int) yaw).replace("+"," ");
        double vx = msg.getVx();
        double vy = msg.getVy();
        double vel = Math.sqrt(Math.pow(vx,2) + Math.pow(vy,2));
        String velStr = String.format("%+06.2f", vel).replace("+"," ");
        if (velStr.equals("-00.00")) {
            velStr = " 00.00";
        }
        double depth = msg.getDepth();
        double altitude = msg.getAlt();

        positionLabel = latStr + " / " + lonStr;
        rpyLabel = rollStr + "°(R), " + pitchStr + "°(P), " + yawStr + "°(H)";
        velLabel =  velStr + " m/s";
        depthLabel = String.format("%.2f m", depth);
        altitudeLabel = String.format("%.2f m", altitude);
    }

    @Subscribe
    public void on(FuelLevel msg) {
        String mainVehicleId = getMainVehicleId();
        if (!msg.getSourceName().equals(mainVehicleId))
            return;

        double fuelLevel = msg.getValue();

        if (fuelLevel >= 90.0) {
            fuelImage = fuelFullImage;
        }
        else if (fuelLevel >= 60.0) {
            fuelImage = fuelAboveHalfImage;
        }
        else if (fuelLevel >= 40.0) {
            fuelImage = fuelBelowHalfImage;
        }

        else if (fuelLevel >= 10.0) {
            fuelImage = fuelLowImage;
        }
        else {
            fuelImage = fuelEmptyImage;
        }

        fuelLabel = String.format("%02d%%", (int) fuelLevel);
    }

    @Override
    public void preferencesUpdated() {
        if (showVehicleInfo) {
            if (!Double.isNaN(positionLatDeg) && !Double.isNaN(positionLonDeg)) {
                String latStr = CoordinateUtil.latitudeAsPrettyString(positionLatDeg);
                String lonStr = CoordinateUtil.latitudeAsPrettyString(positionLonDeg);
                positionLabel = latStr + " / " + lonStr;
            }
        }
    }
}
