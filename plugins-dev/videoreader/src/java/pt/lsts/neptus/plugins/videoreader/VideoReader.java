/*
 * Copyright (c) 2004-2023 Universidade do Porto - Faculdade de Engenharia
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

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.preview.payloads.CameraFOV;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.ImageUtils;

import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@PluginDescription(name = "Video Reader", version = "0.1", experimental = true, author = "Paulo Dias",
        description = "Plugin to view IP Camera streams using FFMPEG", icon = "images/menus/camera.png",
        category = PluginDescription.CATEGORY.INTERFACE)
@Popup(name = "Video Reader", width = 640, height = 480, icon = "images/menus/camera.png")
public class VideoReader extends ConsolePanel {
    static final String BASE_FOLDER_FOR_URL_INI = "ipUrl.ini";

    private static final int DEFAULT_WIDTH_CONSOLE = 640;
    private static final int DEFAULT_HEIGHT_CONSOLE = 480;

    private static final int MAX_NULL_FRAMES_FOR_RECONNECT = 10;
    public static final String IMAGE_NO_VIDEO = "images/novideo.png";

    private final Color LABEL_WHITE_COLOR = new Color(255, 255, 255, 200);

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
    private String camUrl = "rtsp://127.0.0.1:8554/"; //rtsp://10.0.20.207:554/live/ch01_0

    private AtomicInteger emptyFramesCounter = new AtomicInteger(0);
    private AtomicInteger threadsIdCounter = new AtomicInteger(0);


    private Player player;

    private BufferedImage offlineImage;
    private BufferedImage onScreenImage;
    private BufferedImage onScreenImageLastGood;

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
    private CameraFOV camFov = null;
    private Point2D mouseLoc = null;

    private ArrayList<Camera> cameraList;
    private boolean closingPanel = false;

    private boolean refreshTemp;
    private boolean paused = false;

    // JPopup Menu
    private JPopupMenu popup;
    private IpCamManagementPanel ipCamManagementPanel;
    // JTextField for IPCam name
    private JLabel streamNameJLabel;
    private JLabel streamWarnJLabel;

    public VideoReader(ConsoleLayout console) {
        this(console, false);
    }

    public VideoReader(ConsoleLayout console, boolean usedInsideAnotherConsolePanel) {
        super(console, usedInsideAnotherConsolePanel);

        removeAll();
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
            }
        });

        this.setToolTipText(I18n.text("not connected"));

        ipCamManagementPanel = new IpCamManagementPanel(this, s -> {
                    camUrl = s;
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
    }

    @Override
    public void initSubPanel() {
        service.execute(Util::createIpUrlFile);
        //setMainVehicle(getConsole().getMainSystem());
    }

    @Override
    public void cleanSubPanel() {
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
            String text = ipCamManagementPanel.getStreamName();
            Rectangle2D bounds = g.getFontMetrics().getStringBounds(text, g);
            streamNameJLabel.setText(text);
            streamNameJLabel.setSize((int) widthConsole, (int) bounds.getHeight() + 5);
            streamNameJLabel.paint(g);

            if (warn) {
                String textWarn = "⚠";
                streamWarnJLabel.setText(textWarn);
                streamWarnJLabel.setSize((int) widthConsole, (int) heightConsole);
                streamWarnJLabel.paint(g);
            }
        }
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
        Image noVideoImage = ImageUtils.getImage(IMAGE_NO_VIDEO);
        if (noVideoImage == null) {
            BufferedImage blackImage = ImageUtils.createCompatibleImage(1, 1, 255);
            blackImage.setRGB(0, 0, 0);
        }
        noVideoImage = noVideoImage != null && noVideoImage.getWidth(null) > 0
                    && noVideoImage.getHeight(null) > 0
                    && widthConsole >= 0 && heightConsole >= 0
                //? ImageUtils.getScaledImage(noVideoImage, widthConsole, heightConsole, true)
                ? Util.resizeBufferedImage(ImageUtils.toBufferedImage(ImageUtils.getImage("images/novideo.png")), new Dimension(widthConsole, heightConsole))
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

        player = new Player(String.format("%05X-%d", VideoReader.this.hashCode(), threadsIdCounter.getAndIncrement()), service);
        try {
            player.start(camUrl /*fieldUrl.getText()*/, image -> {
                BufferedImage scaledImage = ImageUtils.toBufferedImage(ImageUtils.getFastScaledImage(image, widthConsole, heightConsole, true));
                showImage(scaledImage);
                return null;
            });
        }
        catch (IOException | InterruptedException | RuntimeException e) {
            String error = player.getId() + " :: ERROR :: " + e.getMessage();
            NeptusLog.pub().error(error);
            getConsole().post(Notification.warning(PluginUtils.getPluginName(this.getClass()), error));
            player.setStopRequest();
            player = null;
        }

        repaint(100);
    }

    private void disconnectStream() {
        if (player == null) {
            return;
        }

        System.out.println("disconnectStream");

        Player playerToDisconnect = player;
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
                if (camFov != null) {
                    double width = ((Component) e.getSource()).getWidth();
                    double height = ((Component) e.getSource()).getHeight();
                    double x = e.getX();
                    double y = height - e.getY();
                    mouseLoc = new Point2D.Double((x / width - 0.5) * 2, (y / height - 0.5) * 2);
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                mouseLoc = null;
                //post(new EventMouseLookAt(null));
            }

            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.isControlDown()) {
                    if (camFov != null) {
                        double width = ((Component) e.getSource()).getWidth();
                        double height = ((Component) e.getSource()).getHeight();
                        double x = e.getX();
                        double y = height - e.getY();
                        mouseLoc = new Point2D.Double((x / width - 0.5) * 2, (y / height - 0.5) * 2);
                        LocationType loc = camFov.getLookAt(mouseLoc.getX(), mouseLoc.getY());
                        loc.convertToAbsoluteLatLonDepth();
                        //String id = placeLocationOnMap(loc);
//                        snap = new StoredSnapshot(id, loc, e.getPoint(), onScreenImage, new Date());
//                        snap.setCamFov(camFov);
//                        try {
//                            snap.store();
//                        }
//                        catch (Exception ex) {
//                            NeptusLog.pub().error(ex);
//                        }
                    }
                }

                if (e.getButton() == MouseEvent.BUTTON3) {
                    popup = new JPopupMenu();
                    JMenuItem item;

                    popup.add(item = new JMenuItem(I18n.text("Connect to a IPCam"),
                                    ImageUtils.createImageIcon("images/menus/camera.png")))
                            .addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    openIPCamManagementPanel();
                                    //service.execute(VideoReader.this::connectStream);
                                }
                            });
                    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_MASK));

                    popup.add(item = new JMenuItem(I18n.text("Close connection"),
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

                    popup.add(item = new JMenuItem(I18n.text("Maximize window"),
                                    ImageUtils.createImageIcon("images/menus/maximize.png")))
                            .addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    maximizeVideoStreamPanel();
                                }
                            });
                    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.ALT_MASK));

//                    popup.addSeparator();

//                    JLabel infoZoom = new JLabel(I18n.text("For zoom use Alt-Z"));
//                    infoZoom.setEnabled(false);
//                    popup.add(infoZoom, JMenuItem.CENTER_ALIGNMENT);

//                    JLabel markSnap = new JLabel(I18n.text("Ctr+Click to mark frame in the map"));
//                    markSnap.setEnabled(false);
//                    popup.add(markSnap, JMenuItem.CENTER_ALIGNMENT);

                    popup.show((Component) e.getSource(), e.getX(), e.getY());
                }
            }
        });
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
}
