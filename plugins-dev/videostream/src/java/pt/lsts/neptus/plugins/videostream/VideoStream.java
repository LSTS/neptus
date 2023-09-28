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
 * Author: Pedro Gonçalves
 * Apr 4, 2015
 */
package pt.lsts.neptus.plugins.videostream;

import com.google.common.eventbus.Subscribe;
import foxtrot.AsyncTask;
import foxtrot.AsyncWorker;
import net.miginfocom.swing.MigLayout;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import pt.lsts.imc.CcuEvent;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.MapFeature;
import pt.lsts.imc.MapPoint;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.preview.payloads.CameraFOV;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.params.SystemProperty;
import pt.lsts.neptus.params.SystemProperty.Scope;
import pt.lsts.neptus.params.SystemProperty.Visibility;
import pt.lsts.neptus.platform.OsInfo;
import pt.lsts.neptus.platform.OsInfo.Family;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.map.AbstractElement;
import pt.lsts.neptus.types.map.MapGroup;
import pt.lsts.neptus.types.map.MapType;
import pt.lsts.neptus.types.map.MarkElement;
import pt.lsts.neptus.types.mission.MapMission;
import pt.lsts.neptus.types.mission.MissionType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.SearchOpenCv;
import pt.lsts.neptus.util.UtilCv;
import pt.lsts.neptus.util.conf.ConfigFetch;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.Dialog.ModalityType;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

/**
 * Neptus Plugin for Video Stream and tag frame/object
 *
 * @author pedrog
 * @author Pedro Costa
 * @category Vision
 */
@SuppressWarnings("serial")
@Popup(pos = POSITION.CENTER, width = 640, height = 480, accelerator = 'R')
@LayerPriority(priority = 0)
@PluginDescription(name = "Video Stream", version = "1.4", author = "Pedro Gonçalves", description = "Plugin to view IP Camera streams", icon = "images/menus/camera.png")
public class VideoStream extends ConsolePanel implements ItemListener {

    private static final String BASE_FOLDER_FOR_IMAGES = ConfigFetch.getLogsFolder() + "/images";
    private static final String BASE_FOLDER_FOR_URLINI = "ipUrl.ini";
    // Default width and heihgt of Console
    private static final int DEFAULT_WIDTH_CONSOLE = 640;
    private static final int DEFAULT_HEIGHT_CONSOLE = 480;

    @NeptusProperty(name = "Axis Camera RTPS URL", editable = false)
    private String camRtpsUrl = "rtsp://10.0.20.207:554/live/ch01_0";

    @NeptusProperty(name = "Cam Tilt Deg Value", editable = true)
    private double camTiltDeg = 45.0f;// this value may be in configuration

    @NeptusProperty(name = "Broadcast positions to other CCUs", editable = true)
    private boolean broadcastPositions = false;

    // Opencv library name
    private Socket clientSocket = null;
    // Send data for sync
    private PrintWriter out = null;
    // Buffer for data image
    private InputStream is = null;
    // Buffer for info of data image
    private BufferedReader in = null;
    // Flag state of TCP connection
    private boolean tcpOK = false;
    // Strut Video Capture Opencv
    private VideoCapture capture;
    private VideoCapture captureSave;
    // Width size of image
    private int widthImgRec;
    // Height size of image
    private int heightImgRec;
    // Width size of Console
    private int widthConsole = DEFAULT_WIDTH_CONSOLE;
    // Height size of Console
    private int heightConsole = DEFAULT_HEIGHT_CONSOLE;
    // Black Image
    private Scalar black = new Scalar(0);
    // flag for state of neptus logo
    private boolean noVideoLogoState = false;
    // Scale factor of x pixel
    private float xScale;
    // Scale factor of y pixel
    private float yScale;
    // read size of pack compress
    private String line;
    // Buffer for data receive from DUNE over TCP
    private String duneGps;
    // Size of image received
    private int lengthImage;
    // buffer for save data receive
    private byte[] data;
    // Buffer image for showImage
    private BufferedImage offlineImage;
    private BufferedImage onScreenImage;
    // Flag - Lost connection to the vehicle
    private boolean state = false;
    // Flag - Show/hide Menu JFrame
    private boolean show_menu = false;
    // Flag state of IP CAM
    private boolean ipCam = false;
    // Close comTCP state
    private boolean closeComState = false;
    // Url of IPCam
    private String[][] dataUrlIni;
    private boolean closingPanel = false;
    private boolean refreshTemp;

    // JPanel for info and config values
    private JPanel config;
    // JText info of data receive
    private JLabel txtText;
    // JText of data receive over IMC message
    private JLabel txtData;
    // JText of data warning message
    private JLabel warningText;
    // JText of data receive over DUNE TCP message
    private JLabel txtDataTcp;
    // JFrame for menu options
    private JDialog menu;
    // CheckBox to save image to HD
    private JCheckBox saveToDiskCheckBox;
    // JPopup Menu
    private JPopupMenu popup;

    // Flag to enable/disable zoom
    private boolean zoomMask = false;
    // String for the info treatment
    private String info;
    // String for the info of Image Size Stream
    private String infoSizeStream;
    // Data system
    private Date date = new Date();
    // Location of log folder
    private String logDir;
    // Decompress data received
    private Inflater decompresser = new Inflater(false);
    // Create an expandable byte array to hold the decompressed data
    private ByteArrayOutputStream bos;
    // Image resize
    private Mat matResize;
    // Image receive
    private Mat mat;
    // Image receive to save
    private Mat matSaveImg;
    // Size of output frame
    private Size size = null;

    // counter for frame tag ID
    private short frameTagID = 1;

    // Flag for IPCam Ip Check
    private boolean statePingOk = false;
    // JPanel for color state of ping to host IPCam
    private JPanel colorStateIPCam;
    // JDialog for IPCam Select
    private JDialog ipCamPing;
    // JButton to confirm IPCam
    private JButton selectIPCam;
    // JComboBox for list of IPCam in ipUrl.ini
    private JComboBox<String> ipCamList;
    // row select from string matrix of IPCam List
    private int selectedItemIndex;
    // JLabel for text IPCam Ping
    private JLabel onOffIndicator;
    // JTextField for IPCam name
    private JTextField fieldName = new JTextField(I18n.text("Name"));
    // JTextField for IPCam ip
    private JTextField fieldIP = new JTextField(I18n.text("IP"));
    // JTextField for IPCam url
    private JTextField fieldUrl = new JTextField(I18n.text("URL"));
    // state of ping to host
    private boolean statePing;
    // JPanel for zoom point
    private JPanel zoomImg = new JPanel();
    // Buffer image for zoom Img Cut
    private BufferedImage zoomImgCut;
    // JLabel to show zoom image
    private JLabel zoomLabel = new JLabel();
    // Graphics2D for zoom image scaling
    private Graphics2D graphics2D;
    // BufferedImage for zoom image scaling
    private BufferedImage scaledCutImage;
    // Buffer image for zoom image temp
    private BufferedImage zoomTemp;
    // PopPup zoom Image
    private JPopupMenu popupzoom;
    // cord x for zoom
    private int zoomX = 100;
    // cord y for zoom
    private int zoomY = 100;

    // check ip for Host - TCP
    // JFormattedTextField for host ip
    private JFormattedTextField hostIP;
    // JDialog to check host connection
    private JDialog ipHostPing;
    // JPanel for host ip check
    private JPanel ipHostCheck;
    // Flag of ping state to host
    private boolean pingHostOk = false;
    // Flag for Histogram image
    private boolean histogramflag = false;
    // Flag to save snapshot
    private boolean saveSnapshot = false;

    // *** TEST FOR SAVE VIDEO **/
    private File outputfile;
    private boolean flagBuffImg = false;
    private int cnt = 0;
    private int FPS = 8;
    // *************************/

    // worker thread designed to acquire the data packet from DUNE
    private Thread updater = null;
    // worker thread designed to save image do HD
    private Thread saveImg = null;
    // worker thread create ipUrl.ini in conf folder
    private Thread createIPUrl = null;

    // WatchDog variables/objects
    private Thread watchDog;
    private long endTimeMillis;
    private boolean virtualEndThread;
    private boolean isAliveIPCam;
    private boolean isCleanTurnOffCam;
    private CameraFOV camFov = null;
    private Point2D mouseLoc = null;
    private StoredSnapshot snap = null;
    private boolean paused = false;

    public VideoStream(ConsoleLayout console) {
        super(console);

        // Initialize size variables
        updateSizeVariables(this);

        if (findOpenCV()) {
            NeptusLog.pub().info(I18n.text("OpenCv-4.4.0 found."));
            // clears all the unused initializations of the standard ConsolePanel
            removeAll();
            // Resize Console
            this.addComponentListener(new ComponentAdapter() {
                public void componentResized(ComponentEvent evt) {
                    Component c = evt.getComponent();
                    updateSizeVariables(c);
                    matResize = new Mat((int) size.height, (int) size.width, CvType.CV_8UC3);
                    if (!ipCam) {
                        initImage();
                    }
                }
            });

            // Mouse click
            mouseListenerInit();

            // Detect key-pressed
            this.addKeyListener(new KeyListener() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_Z && zoomMask) {
                        zoomMask = false;
                        popupzoom.setVisible(false);
                    }
                    if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                        paused = false;
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if ((e.getKeyCode() == KeyEvent.VK_Z) && ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0)
                            && !zoomMask) {
                        if (ipCam) {
                            zoomMask = true;
                            popupzoom.add(zoomImg);
                        }
                    }
                    else if ((e.getKeyCode() == KeyEvent.VK_I)
                            && ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0)) {
                        openIPCamManagementPanel();
                    }
                    else if ((e.getKeyCode() == KeyEvent.VK_X)
                            && ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0)) {
                        NeptusLog.pub().info("Closing all Video Streams...");
                        state = false;
                        ipCam = false;
                    }
                    else if ((e.getKeyCode() == KeyEvent.VK_C)
                            && ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0)) {
                        menu.setVisible(true);
                    }
                    else if (e.getKeyChar() == 'z' && zoomMask) {
                        int xLocMouse = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x - 11;
                        int yLocMouse = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y - 11;
                        if (xLocMouse < 0) {
                            xLocMouse = 0;
                        }
                        if (yLocMouse < 0) {
                            yLocMouse = 0;
                        }

                        if (xLocMouse + 52 < VideoStream.this.getSize().getWidth() && xLocMouse - 52 > 0
                                && yLocMouse + 60 < VideoStream.this.getSize().getHeight() && yLocMouse - 60 > 0) {
                            zoomX = xLocMouse;
                            zoomY = yLocMouse;
                            popupzoom.setLocation(MouseInfo.getPointerInfo().getLocation().x - 150,
                                    MouseInfo.getPointerInfo().getLocation().y - 150);
                        }
                        else {
                            popupzoom.setVisible(false);
                            zoomMask = false;
                        }
                    }
                    else if ((e.getKeyCode() == KeyEvent.VK_H)
                            && ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0)) {
                        histogramflag = !histogramflag;
                    }
                    else if ((e.getKeyCode() == KeyEvent.VK_S)
                            && ((e.getModifiersEx() & KeyEvent.ALT_DOWN_MASK) != 0)) {
                        saveSnapshot = true;
                    }
                    else if ((e.getKeyCode() == KeyEvent.VK_CONTROL)) {
                        paused = true;
                    }
                    else if ((e.getKeyCode() == KeyEvent.VK_F)
                            && e.getModifiersEx() == KeyEvent.ALT_DOWN_MASK) {
                        maximizeVideoStreamPanel();
                    }
                }

                @Override
                public void keyTyped(KeyEvent e) {
                }
            });
            this.setFocusable(true);
        }
        else {
            NeptusLog.pub().error("Opencv not found.");
            closingPanel = true;
            setBackground(Color.BLACK);
            // JLabel for image
            this.setLayout(new MigLayout("al center center"));
            // JLabel info
            String opencvInstallLink = "";
            if (OsInfo.getFamily() == Family.UNIX) {
                opencvInstallLink = "<br>" + I18n.text(
                        "Install OpenCv 4.4 and dependencies at <br>https://www.lsts.pt/bin/opencv/v4.4.0-x64_x86/deb/");
            }
            else if (OsInfo.getFamily() == Family.WINDOWS) {
                opencvInstallLink = "<br>" + I18n.text(
                        "Install OpenCv 4.4 and dependencies at <br>https://www.lsts.pt/bin/opencv/v4.4.0-x64_x86/win-x64_86/");
            }
            warningText = new JLabel(
                    "<html>" + I18n.text("Please install OpenCV 4.4.0 and its dependencies." + opencvInstallLink));
            warningText.setForeground(Color.BLACK);
            warningText.setFont(new Font("Courier New", Font.ITALIC, 18));
            this.add(warningText);
        }
    }

    private void updateSizeVariables(Component comp) {
        widthConsole = comp.getSize().width;
        heightConsole = comp.getSize().height;
        xScale = (float) widthConsole / widthImgRec;
        yScale = (float) heightConsole / heightImgRec;
        size = new Size(widthConsole, heightConsole);
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
                post(new EventMouseLookAt(null));
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
                        String id = placeLocationOnMap(loc);
                        snap = new StoredSnapshot(id, loc, e.getPoint(), onScreenImage, new Date());
                        snap.setCamFov(camFov);
                        try {
                            snap.store();
                        }
                        catch (Exception ex) {
                            NeptusLog.pub().error(ex);
                        }
                    }
                }

                if (e.getButton() == MouseEvent.BUTTON3) {
                    popup = new JPopupMenu();
                    JMenuItem item;

                    popup.add(item = new JMenuItem(I18n.text("Close all connections"),
                                    ImageUtils.createImageIcon(String.format("images/menus/exit.png"))))
                            .addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    NeptusLog.pub().info("Clossing all Video Stream...");
                                    noVideoLogoState = false;
                                    isCleanTurnOffCam = true;
                                    if (tcpOK) {
                                        try {
                                            clientSocket.close();
                                        }
                                        catch (IOException e1) {
                                            e1.printStackTrace();
                                        }
                                    }
                                    state = false;
                                    ipCam = false;
                                }
                            });
                    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));

                    popup.add(item = new JMenuItem(I18n.text("Start IPCam"),
                                    ImageUtils.createImageIcon("images/menus/camera.png")))
                            .addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    openIPCamManagementPanel();
                                }
                            });
                    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));

                    popup.addSeparator();

                    popup.add(item = new JMenuItem(I18n.text("Config"),
                                    ImageUtils.createImageIcon(String.format("images/menus/configure.png"))))
                            .addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    menu.setVisible(true);
                                }
                            });
                    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));

                    popup.add(item = new JMenuItem(I18n.text("Histogram filter on/off"),
                                    ImageUtils.createImageIcon("images/menus/histogram.png")))
                            .addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    histogramflag = !histogramflag;
                                }
                            });
                    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));

                    popup.add(item = new JMenuItem(I18n.text("Snapshot"),
                                    ImageUtils.createImageIcon("images/menus/snapshot.png")))
                            .addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    saveSnapshot = true;
                                }
                            });
                    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK));

                    popup.add(item = new JMenuItem(I18n.text("Maximize Window"),
                                    ImageUtils.createImageIcon("images/menus/maximize.png")))
                            .addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    maximizeVideoStreamPanel();
                                }
                            });
                    item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.ALT_MASK));

                    popup.addSeparator();

                    JLabel infoZoom = new JLabel(I18n.text("For zoom use Alt-Z"));
                    popup.add(infoZoom, JMenuItem.CENTER_ALIGNMENT);
                    popup.show((Component) e.getSource(), e.getX(), e.getY());
                }
            }
        });
    }

    private void maximizeVideoStreamPanel() {
        JDialog dialog = (JDialog) SwingUtilities.getWindowAncestor(VideoStream.this);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize().getSize();
        if (dialog.getSize().equals(screenSize)) {
            // Already maximized
            screenSize = new Dimension(DEFAULT_WIDTH_CONSOLE, DEFAULT_HEIGHT_CONSOLE);
        }
        dialog.setSize(screenSize);
        // We call the resize with its own size to call componentResized
        // method of the componentAdapter set in the constructor
        VideoStream.this.resize(VideoStream.this.getSize());
    }

    // Read ipUrl.ini to find IPCam ON
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void openIPCamManagementPanel() {
        // JPanel for IPCam Select (MigLayout)
        JPanel ipCamManagementPanel = new JPanel(new MigLayout());

        repaintParametersTextFields();
        dataUrlIni = readIPUrl();
        int sizeDataUrl = dataUrlIni.length;
        String nameIPCam[] = new String[sizeDataUrl];
        for (int i = 0; i < sizeDataUrl; i++) {
            nameIPCam[i] = dataUrlIni[i][0];
        }

        ipCamPing = new JDialog(SwingUtilities.getWindowAncestor(VideoStream.this), I18n.text("Select IPCam"));
        ipCamPing.setResizable(true);
        ipCamPing.setModalityType(ModalityType.DOCUMENT_MODAL);
        ipCamPing.setSize(440, 200);
        ipCamPing.setLocationRelativeTo(VideoStream.this);

        ImageIcon imgIPCam = ImageUtils.createImageIcon("images/menus/camera.png");
        ipCamPing.setIconImage(imgIPCam.getImage());
        ipCamPing.setResizable(false);
        ipCamPing.setBackground(Color.GRAY);
        ipCamList = new JComboBox(nameIPCam);
        ipCamList.setSelectedIndex(0);
        ipCamList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ipCamList.setEnabled(false);
                selectIPCam.setEnabled(false);
                selectedItemIndex = ipCamList.getSelectedIndex();
                statePing = false;
                if (selectedItemIndex > 0) {
                    colorStateIPCam.setBackground(Color.LIGHT_GRAY);
                    onOffIndicator.setText("---");
                    statePingOk = false;

                    fieldName.setText(I18n.text(dataUrlIni[selectedItemIndex][0]));
                    fieldName.validate();
                    fieldName.repaint();
                    fieldIP.setText(I18n.text(dataUrlIni[selectedItemIndex][1]));
                    fieldIP.validate();
                    fieldIP.repaint();
                    fieldUrl.setText(I18n.text(dataUrlIni[selectedItemIndex][2]));
                    fieldUrl.validate();
                    fieldUrl.repaint();

                    AsyncTask task = new AsyncTask() {
                        @Override
                        public Object run() throws Exception {
                            statePing = pingIPCam(dataUrlIni[selectedItemIndex][1]);
                            return null;
                        }

                        @Override
                        public void finish() {
                            if (statePing) {
                                selectIPCam.setEnabled(true);
                                camRtpsUrl = dataUrlIni[selectedItemIndex][2];
                                colorStateIPCam.setBackground(Color.GREEN);
                                onOffIndicator.setText("ON");
                                ipCamList.setEnabled(true);
                            }
                            else {
                                selectIPCam.setEnabled(false);
                                colorStateIPCam.setBackground(Color.RED);
                                onOffIndicator.setText("OFF");
                                ipCamList.setEnabled(true);
                            }
                            selectIPCam.validate();
                            selectIPCam.repaint();
                        }
                    };
                    AsyncWorker.getWorkerThread().postTask(task);
                }
                else {
                    statePingOk = false;
                    colorStateIPCam.setBackground(Color.RED);
                    onOffIndicator.setText("OFF");
                    ipCamList.setEnabled(true);
                    repaintParametersTextFields();
                }
            }
        });
        ipCamManagementPanel.add(ipCamList, "split 3, width 50:250:250, center");

        colorStateIPCam = new JPanel();
        onOffIndicator = new JLabel(I18n.text("OFF"));
        onOffIndicator.setFont(new Font("Verdana", 1, 14));
        colorStateIPCam.setBackground(Color.RED);
        colorStateIPCam.add(onOffIndicator);
        ipCamManagementPanel.add(colorStateIPCam, "h 30!, w 30!");

        selectIPCam = new JButton(I18n.text("Select IPCam"), imgIPCam);
        selectIPCam.setEnabled(false);
        selectIPCam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (statePingOk) {
                    NeptusLog.pub().info("IPCam Select: " + dataUrlIni[selectedItemIndex][0]);
                    ipCamPing.setVisible(false);
                    ipCam = true;
                    state = false;
                }
            }
        });
        ipCamManagementPanel.add(selectIPCam, "h 30!, wrap");

        JButton addNewIPCam = new JButton(I18n.text("Add New IPCam"));
        addNewIPCam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // Execute when button is pressed
                writeToFile(String.format("%s (%s)#%s#%s\n", fieldName.getText(), fieldIP.getText(), fieldIP.getText(),
                        fieldUrl.getText()));
                reloadIPCamList();
            }
        });

        JButton removeIpCam = new JButton(I18n.text("Remove IPCam"));
        removeIpCam.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent event) {

                int lineToRemove = ipCamList.getSelectedIndex();
                String ipUrlFilename = ConfigFetch.getConfFolder() + "/" + BASE_FOLDER_FOR_URLINI;

                // Execute when button is pressed
                UtilVideoStream.removeLineFromFile(lineToRemove, ipUrlFilename);
                reloadIPCamList();
            }

        });

        ipCamManagementPanel.add(fieldName, "w 410!, wrap");
        ipCamManagementPanel.add(fieldIP, "w 410!, wrap");
        ipCamManagementPanel.add(fieldUrl, "w 410!, wrap");
        ipCamManagementPanel.add(addNewIPCam, "w 120!, center, wrap");
        ipCamManagementPanel.add(removeIpCam, "w 120!, center");

        ipCamPing.add(ipCamManagementPanel);
        ipCamPing.pack();
        ipCamPing.setVisible(true);
    }

    private void repaintParametersTextFields() {
        fieldName.setText(I18n.text("NAME"));
        fieldName.validate();
        fieldName.repaint();
        fieldIP.setText(I18n.text("IP"));
        fieldIP.validate();
        fieldIP.repaint();
        fieldUrl.setText(I18n.text("URL"));
        fieldUrl.validate();
        fieldUrl.repaint();
    }

    // Write to file
    private void writeToFile(String textString) {
        String iniRsrcPath = FileUtil.getResourceAsFileKeepName(BASE_FOLDER_FOR_URLINI);
        File confIni = new File(ConfigFetch.getConfFolder() + "/" + BASE_FOLDER_FOR_URLINI);
        if (!confIni.exists()) {
            FileUtil.copyFileToDir(iniRsrcPath, ConfigFetch.getConfFolder());
        }
        FileUtil.saveToFile(confIni.getAbsolutePath(), textString, "UTF-8", true);
    }

    // Reloads the list of IP cams
    private void reloadIPCamList() {
        AsyncTask task = new AsyncTask() {
            @Override
            public Object run() throws Exception {
                dataUrlIni = readIPUrl();
                return null;
            }

            @Override
            public void finish() {
                int sizeDataUrl = dataUrlIni.length;
                String nameIPCam[] = new String[sizeDataUrl];
                for (int i = 0; i < sizeDataUrl; i++) {
                    nameIPCam[i] = dataUrlIni[i][0];
                }

                ipCamList.removeAllItems();
                for (int i = 0; i < nameIPCam.length; i++) {
                    String sample = nameIPCam[i];
                    ipCamList.addItem(sample);
                }
            }
        };
        AsyncWorker.getWorkerThread().postTask(task);
    }

    // Ping CamIp
    private boolean pingIPCam(String host) {
        statePingOk = UtilVideoStream.pingIp(host);
        return statePingOk;
    }

    // Read file
    private String[][] readIPUrl() {
        String iniRsrcPath = FileUtil.getResourceAsFileKeepName(BASE_FOLDER_FOR_URLINI);
        File confIni = new File(ConfigFetch.getConfFolder() + "/" + BASE_FOLDER_FOR_URLINI);
        if (!confIni.exists()) {
            FileUtil.copyFileToDir(iniRsrcPath, ConfigFetch.getConfFolder());
        }
        return UtilVideoStream.readIpUrl(confIni);
    }

    private String timestampToReadableHoursString(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        return format.format(date);
    }

    /**
     * Adapted from ContactMarker.placeLocationOnMap()
     */
    private String placeLocationOnMap(LocationType loc) {
        if (getConsole().getMission() == null) {
            return null;
        }

        loc.convertToAbsoluteLatLonDepth();
        double lat = loc.getLatitudeDegs();
        double lon = loc.getLongitudeDegs();
        long timestamp = System.currentTimeMillis();
        String id = I18n.text("Snap") + "-" + frameTagID + "-" + timestampToReadableHoursString(timestamp);

        AbstractElement elems[] = MapGroup.getMapGroupInstance(getConsole().getMission()).getMapObjectsByID(id);

        while (elems.length > 0) {
            frameTagID++;
            id = I18n.text("Snap") + "-" + frameTagID + "-" + timestampToReadableHoursString(timestamp);
            elems = MapGroup.getMapGroupInstance(getConsole().getMission()).getMapObjectsByID(id);
        }
        frameTagID++;// increment ID

        MissionType mission = getConsole().getMission();
        LinkedHashMap<String, MapMission> mapList = mission.getMapsList();
        if (mapList == null) {
            return id;
        }
        if (mapList.size() == 0) {
            return id;
        }
        // MapMission mapMission = mapList.values().iterator().next();
        MapGroup.resetMissionInstance(getConsole().getMission());
        MapType mapType = MapGroup.getMapGroupInstance(getConsole().getMission()).getMaps()[0];// mapMission.getMap();
        // NeptusLog.pub().info("<###>MARKER --------------- " + mapType.getId());
        MarkElement contact = new MarkElement(mapType.getMapGroup(), mapType);

        contact.setId(id);
        contact.setCenterLocation(new LocationType(lat, lon));
        mapType.addObject(contact);
        mission.save(false);
        MapPoint point = new MapPoint();
        point.setLat(lat);
        point.setLon(lon);
        point.setAlt(0);
        MapFeature feature = new MapFeature();
        feature.setFeatureType(MapFeature.FEATURE_TYPE.POI);
        feature.setFeature(Arrays.asList(point));
        if (broadcastPositions) {
            CcuEvent event = new CcuEvent();
            event.setType(CcuEvent.TYPE.MAP_FEATURE_ADDED);
            event.setId(id);
            event.setArg(feature);
            this.getConsole().getImcMsgManager().broadcastToCCUs(event);
        }

        NeptusLog.pub().info("placeLocationOnMap: " + id + " - " + loc);
        return id;
    }

    // Print Image to JPanel
    @Override
    protected void paintComponent(Graphics g) {
        if (refreshTemp && onScreenImage != null) {
            g.drawImage(onScreenImage, 0, 0, this);
            refreshTemp = false;
        }
        else {
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, (int) size.width, (int) size.height);
        }
    }

    private void showImage(BufferedImage image) {
        if (!paused) {
            onScreenImage = image;
        }
        refreshTemp = true;
        repaint();
    }

    // Config Layout
    private void configLayout() {
        // Create Buffer (type MAT) for Image resize
        matResize = new Mat(heightConsole, widthConsole, CvType.CV_8UC3);

        // Config JFrame zoom img
        zoomImg.setSize(300, 300);
        popupzoom = new JPopupMenu();
        popupzoom.setSize(300, 300);

        logDir = String.format(BASE_FOLDER_FOR_IMAGES + "/%s", date.toString().replace(":", "-"));

        // JPanel for info and config values
        config = new JPanel(new MigLayout());

        // JCheckBox save to HD
        saveToDiskCheckBox = new JCheckBox(I18n.text("Save Image to Disk"));
        saveToDiskCheckBox.setMnemonic(KeyEvent.VK_C);
        saveToDiskCheckBox.setSelected(false);
        saveToDiskCheckBox.addItemListener(this);
        config.add(saveToDiskCheckBox, "width 160:180:200, h 40!, wrap");

        // JLabel info Data received
        txtText = new JLabel();
        txtText.setToolTipText(I18n.text("Info of Frame Received"));
        info = String.format("Img info");
        txtText.setText(info);
        config.add(txtText, "cell 0 4 3 1, wrap");

        // JLabel info
        txtData = new JLabel();
        txtData.setToolTipText(I18n.text("Info of GPS Received over IMC"));
        info = String.format("GPS IMC");
        txtData.setText(info);
        config.add(txtData, "cell 0 6 3 1, wrap");

        menu = new JDialog(SwingUtilities.getWindowAncestor(VideoStream.this), I18n.text("Menu Config"));
        menu.setResizable(false);
        menu.setModalityType(ModalityType.DOCUMENT_MODAL);
        menu.setSize(450, 350);
        menu.setLocationRelativeTo(VideoStream.this);
        menu.setVisible(show_menu);
        ImageIcon imgMenu = ImageUtils.createImageIcon(String.format("images/menus/configure.png"));
        menu.setIconImage(imgMenu.getImage());
        menu.add(config);
    }

    @Override
    public void itemStateChanged(ItemEvent e) {
        // checkbox listener
        Object source = e.getItemSelectable();
        if (source == saveToDiskCheckBox) {
            if (ipCam && saveToDiskCheckBox.isSelected()) {
                flagBuffImg = true;
            }
            if (!ipCam || !saveToDiskCheckBox.isSelected()) {
                flagBuffImg = false;
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        closingPanel = true;
    }

    /*
     * (non-Javadoc)
     *
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        if (findOpenCV()) {
            getConsole().getImcMsgManager().addListener(this);
            configLayout();
            createIPUrl = createFile();
            createIPUrl.start();
            updater = updaterThread();
            updater.start();
            saveImg = updaterThreadSave();
            saveImg.start();
        }
        else {
            NeptusLog.pub().error("Opencv not found.");
            closingPanel = true;
            return;
        }
        setMainVehicle(getConsole().getMainSystem());
    }

    private Thread createFile() {
        Thread ipUrl = new Thread("Create file IPUrl Thread") {
            @Override
            public void run() {
                String iniRsrcPath = FileUtil.getResourceAsFileKeepName(BASE_FOLDER_FOR_URLINI);
                File confIni = new File(ConfigFetch.getConfFolder() + "/" + BASE_FOLDER_FOR_URLINI);
                if (!confIni.exists()) {
                    FileUtil.copyFileToDir(iniRsrcPath, ConfigFetch.getConfFolder());
                }
            }
        };
        ipUrl.setDaemon(true);
        return ipUrl;
    }

    // Find OPENCV JNI in host PC
    private boolean findOpenCV() {
        return SearchOpenCv.searchJni();
    }

    // Get size of image over TCP
    private void initSizeImage() {
        // Width size of image
        try {
            widthImgRec = Integer.parseInt(in.readLine());
        }
        catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
        // Height size of image
        try {
            heightImgRec = Integer.parseInt(in.readLine());
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        xScale = (float) widthConsole / widthImgRec;
        yScale = (float) heightConsole / heightImgRec;
        // Create Buffer (type MAT) for Image receive
        mat = new Mat(heightImgRec, widthImgRec, CvType.CV_8UC3);
    }

    // Thread to handle data receive
    private Thread updaterThread() {
        Thread ret = new Thread("Video Stream Thread") {
            @Override
            public void run() {
                initImage();
                setupWatchDog(4000);
                while (true) {
                    if (closingPanel) {
                        state = false;
                        ipCam = false;
                    }
                    else if (ipCam) {
                        if (state == false) {
                            // Create Buffer (type MAT) for Image receive
                            mat = new Mat(heightImgRec, widthImgRec, CvType.CV_8UC3);
                            capture = new VideoCapture();
                            capture.open(camRtpsUrl);
                            if (capture.isOpened()) {
                                state = true;
                                NeptusLog.pub().info("Video Strem from IPCam is captured");
                                startWatchDog();
                                isCleanTurnOffCam = false;
                            }
                            else {
                                ipCam = false;
                                NeptusLog.pub().info("Video Strem from IPCam is not captured");
                            }
                        }
                        // IPCam Capture
                        else if (ipCam && state) {
                            long startTime = System.currentTimeMillis();
                            isAliveIPCam = false;
                            resetWatchDog(4000);
                            while (watchDog.isAlive() && !isAliveIPCam) {
                                capture.read(mat);
                                isAliveIPCam = true;
                            }
                            if (isAliveIPCam) {
                                resetWatchDog(4000);
                            }

                            if (!ipCam) {
                                continue;
                            }

                            long stopTime = System.currentTimeMillis();
                            if ((stopTime - startTime) != 0) {
                                infoSizeStream = String.format("Size(%d x %d) | Scale(%.2f x %.2f) | FPS:%d |\t\t\t",
                                        mat.cols(), mat.rows(), xScale, yScale, (int) (1000 / (stopTime - startTime)));
                            }

                            txtText.setText(infoSizeStream);

                            if (mat.empty()) {
                                NeptusLog.pub().error(I18n.text("ERROR capturing img of IPCam"));
                                continue;
                            }

                            xScale = (float) widthConsole / mat.cols();
                            yScale = (float) heightConsole / mat.rows();
                            Imgproc.resize(mat, matResize, size);
                            // Convert Mat to BufferedImage
                            offlineImage = UtilCv.matToBufferedImage(matResize);
                            // Display image in JFrame
                            if (histogramflag) {
                                if (zoomMask) {
                                    zoomTemp = offlineImage;
                                    getCutImage(UtilCv.histogramCv(zoomTemp), zoomX, zoomY);
                                    popupzoom.setVisible(true);
                                }
                                else {
                                    popupzoom.setVisible(false);
                                }

                                if (saveSnapshot) {
                                    UtilCv.saveSnapshot(UtilCv.addText(UtilCv.histogramCv(offlineImage),
                                                    I18n.text("Histogram - On"), Color.GREEN, offlineImage.getWidth() - 5, 20),
                                            String.format(logDir + "/snapshotImage"));
                                    saveSnapshot = false;
                                }
                                showImage(UtilCv.addText(UtilCv.histogramCv(offlineImage), I18n.text("Histogram - On"),
                                        Color.GREEN, offlineImage.getWidth() - 5, 20));
                            }
                            else {
                                if (zoomMask) {
                                    getCutImage(offlineImage, zoomX, zoomY);
                                    popupzoom.setVisible(true);
                                }
                                else {
                                    popupzoom.setVisible(false);
                                }

                                if (saveSnapshot) {
                                    UtilCv.saveSnapshot(
                                            UtilCv.addText(offlineImage, I18n.text("Histogram - Off"), Color.RED,
                                                    offlineImage.getWidth() - 5, 20),
                                            String.format(logDir + "/snapshotImage"));
                                    saveSnapshot = false;
                                }
                                showImage(UtilCv.addText(offlineImage, I18n.text("Histogram - Off"), Color.RED,
                                        offlineImage.getWidth() - 5, 20));
                            }

                            // // save image tag to disk
                            // if (snap != null) {
                            // try {
                            // snap.capture = offlineImage;
                            // snap.store();
                            // snap = null;
                            // }
                            // catch (IOException e) {
                            // e.printStackTrace();
                            // }
                            // }
                        }
                    }
                    else {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        initImage();
                    }
                    if (closingPanel) {
                        break;
                    }
                }
            }
        };
        ret.setDaemon(true);
        return ret;
    }

    private static File checkExistenceOfFolderForFile(File fx) {
        File p = fx.getParentFile();
        if (!p.exists()) {
            p.mkdirs();
        }

        return fx;
    }

    // Thread to handle save image
    private Thread updaterThreadSave() {
        Thread si = new Thread("Save Image") {
            @Override
            public void run() {
                matSaveImg = new Mat(heightImgRec, widthImgRec, CvType.CV_8UC3);
                boolean stateSetUrl = false;

                while (true) {
                    if (ipCam && !stateSetUrl) {
                        captureSave = new VideoCapture();
                        captureSave.open(camRtpsUrl, Videoio.CAP_ANY);
                        if (captureSave.isOpened()) {
                            stateSetUrl = true;
                        }
                    }
                    if (ipCam && stateSetUrl) {
                        if (flagBuffImg == true) {
                            long startTime = System.currentTimeMillis();
                            captureSave.read(matSaveImg);
                            if (!matSaveImg.empty()) {
                                String imageJpeg = null;
                                try {
                                    if (histogramflag) {
                                        imageJpeg = String.format("%s/imageSave/%d_H.jpeg", logDir, cnt);
                                        outputfile = checkExistenceOfFolderForFile(new File(imageJpeg));
                                        ImageIO.write(UtilCv.histogramCv(UtilCv.matToBufferedImage(matSaveImg)), "jpeg",
                                                outputfile);
                                    }
                                    else {
                                        imageJpeg = String.format("%s/imageSave/%d.jpeg", logDir, cnt);
                                        outputfile = checkExistenceOfFolderForFile(new File(imageJpeg));
                                        ImageIO.write(UtilCv.matToBufferedImage(matSaveImg), "jpeg", outputfile);
                                    }
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }
                                cnt++;
                                long stopTime = System.currentTimeMillis();
                                while ((stopTime - startTime) < (1000 / FPS)) {
                                    stopTime = System.currentTimeMillis();
                                }
                            }
                        }
                        else {
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    else {
                        try {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (closingPanel) {
                        break;
                    }
                }
            }
        };
        si.setDaemon(true);
        return si;
    }

    @Subscribe
    public void consume(ConsoleEventMainSystemChange evt) {
        setMainVehicle(evt.getCurrent());
    }

    private void setMainVehicle(String vehicle) {
        camFov = null;

        ArrayList<SystemProperty> props = ConfigurationManager.getInstance().getPropertiesByEntity(vehicle, "UAVCamera",
                Visibility.DEVELOPER, Scope.GLOBAL);

        String camModel = "";
        double hAOV = 0, vAOV = 0, camTilt = 0;

        for (SystemProperty p : props) {
            if (p.getName().equals("Onboard Camera")) {
                camModel = "" + p.getValue();
            }
            else if (p.getName().equals("(" + camModel + ") Horizontal AOV")) {
                hAOV = Math.toRadians(Double.valueOf("" + p.getValue()));
            }
            else if (p.getName().equals("(" + camModel + ") Vertical AOV")) {
                vAOV = Math.toRadians(Double.valueOf("" + p.getValue()));
            }
            else if (p.getName().equals("(" + camModel + ") Tilt Angle")) {
                camTilt = Math.PI / 2 + Math.toRadians(Double.valueOf("" + p.getValue()));
            }
        }

        if (!camModel.isEmpty()) {
            camFov = new CameraFOV(hAOV, vAOV);
            camFov.setTilt(camTilt);
            NeptusLog.pub().info("Using " + camModel + " camera with " + Math.toDegrees(hAOV) + " x "
                    + Math.toDegrees(vAOV) + " AOV");
        }
        else {
            NeptusLog.pub().error("Could not load camera FOV");
            getConsole().post(Notification.warning(I18n.text("CameraFOV"), I18n.text("Could not load camera FOV")));
            camFov = CameraFOV.defaultFov();
        }
    }

    // IMC handle
    @Subscribe
    public void consume(EstimatedState msg) {
        // System.out.println("Source Name "+msg.getSourceName()+"ID
        // "+getMainVehicleId());
        if (msg.getSourceName().equals(getMainVehicleId()) && findOpenCV()) {
            if (camFov != null) {
                if (!paused) {
                    camFov.setState(msg);
                }
                if (mouseLoc != null) {
                    EventMouseLookAt lookAt = new EventMouseLookAt(camFov.getLookAt(mouseLoc.getX(), mouseLoc.getY()));
                    getConsole().post(lookAt);
                }
            }
        }
    }

    // Fill cv::Mat image with zeros
    private void initImage() {
        if (!noVideoLogoState) {
            if (ImageUtils.getImage("images/novideo.png") == null) {
                matResize.setTo(black);
                offlineImage = UtilCv.matToBufferedImage(matResize);
            }
            else {
                offlineImage = UtilVideoStream.resizeBufferedImage(
                        ImageUtils.toBufferedImage(ImageUtils.getImage("images/novideo.png")), size);
            }

            if (offlineImage != null) {
                showImage(offlineImage);
                noVideoLogoState = true;
            }
        }
        else {
            offlineImage = UtilVideoStream
                    .resizeBufferedImage(ImageUtils.toBufferedImage(ImageUtils.getImage("images/novideo.png")), size);
            showImage(offlineImage);
        }
    }

    // Received data Image
    private void receivedDataImage() {
        long startTime = System.currentTimeMillis();
        try {
            line = in.readLine();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        if (line == null) {
            GuiUtils.errorMessage(VideoStream.this, I18n.text("Connection error"),
                    I18n.text("Lost connection with vehicle"), ModalityType.DOCUMENT_MODAL);
            state = false;
            // closeTcpCom();
            try {
                clientSocket.close();
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        else {
            lengthImage = Integer.parseInt(line);
            // buffer for save data receive
            data = new byte[lengthImage];
            // Send 1 for server for sync data send
            out.println("1\0");
            // read data image (ZP)
            int read = 0;
            while (read < lengthImage) {
                int readBytes = 0;
                try {
                    readBytes = is.read(data, read, lengthImage - read);
                }
                catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
                if (readBytes < 0) {
                    System.err.println("stream ended");
                    closeTcpCom();
                    return;
                }
                read += readBytes;
            }
            // Receive data GPS over tcp DUNE
            try {
                duneGps = in.readLine();
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
            // Decompress data received
            decompresser = new Inflater(false);
            decompresser.setInput(data, 0, lengthImage);
            // Create an expandable byte array to hold the decompressed data
            bos = new ByteArrayOutputStream(data.length);
            // Decompress the data
            byte[] buf = new byte[(widthImgRec * heightImgRec * 3)];
            while (!decompresser.finished()) {
                try {
                    int count = decompresser.inflate(buf);
                    bos.write(buf, 0, count);
                }
                catch (DataFormatException e) {
                    break;
                }
            }
            try {
                bos.close();
            }
            catch (IOException e) {
            }
            // Get the decompressed data
            byte[] decompressedData = bos.toByteArray();

            // Transform byte data to cv::Mat (for display image)
            mat.put(0, 0, decompressedData);
            // Resize image to console size
            Imgproc.resize(mat, matResize, size);

            // Display image in JFrame
            if (histogramflag) {
                if (saveSnapshot) {
                    UtilCv.saveSnapshot(UtilCv.addText(UtilCv.histogramCv(offlineImage), I18n.text("Histogram - On"),
                            Color.GREEN, offlineImage.getWidth() - 5, 20), String.format(logDir + "/snapshotImage"));
                    saveSnapshot = false;
                }
                showImage(UtilCv.addText(UtilCv.histogramCv(offlineImage), I18n.text("Histogram - On"), Color.GREEN,
                        offlineImage.getWidth() - 5, 20));
            }
            else {
                if (saveSnapshot) {
                    UtilCv.saveSnapshot(UtilCv.addText(offlineImage, I18n.text("Histogram - Off"), Color.RED,
                            offlineImage.getWidth() - 5, 20), String.format(logDir + "/snapshotImage"));
                    saveSnapshot = false;
                }
                showImage(UtilCv.addText(offlineImage, I18n.text("Histogram - Off"), Color.RED,
                        offlineImage.getWidth() - 5, 20));
            }

            if (histogramflag) {
                showImage(UtilCv.addText(UtilCv.histogramCv(UtilCv.matToBufferedImage(matResize)),
                        I18n.text("Histogram - On"), Color.GREEN, matResize.cols() - 5, 20));
                if (saveSnapshot) {
                    UtilCv.saveSnapshot(
                            UtilCv.addText(UtilCv.histogramCv(UtilCv.matToBufferedImage(matResize)),
                                    I18n.text("Histogram - On"), Color.GREEN, matResize.cols() - 5, 20),
                            String.format(logDir + "/snapshotImage"));
                    saveSnapshot = false;
                }
            }
            else {
                showImage(UtilCv.addText(UtilCv.matToBufferedImage(matResize), I18n.text("Histogram - Off"), Color.RED,
                        matResize.cols() - 5, 20));
                if (saveSnapshot) {
                    UtilCv.saveSnapshot(UtilCv.addText(UtilCv.matToBufferedImage(matResize),
                                    I18n.text("Histogram - On"), Color.RED, matResize.cols() - 5, 20),
                            String.format(logDir + "/snapshotImage"));
                    saveSnapshot = false;
                }
            }

            xScale = (float) widthConsole / widthImgRec;
            yScale = (float) heightConsole / heightImgRec;
            long stopTime = System.currentTimeMillis();
            while ((stopTime - startTime) < (1000 / FPS)) {
                stopTime = System.currentTimeMillis();
            }

            info = String.format("Size(%d x %d) | Scale(%.2f x %.2f) | FPS:%d | Pak:%d (KiB:%d)", widthImgRec,
                    heightImgRec, xScale, yScale, (int) 1000 / (stopTime - startTime), lengthImage, lengthImage / 1024);
            txtText.setText(info);
            txtDataTcp.setText(duneGps);
        }
    }

    // Close TCP COM
    private void closeTcpCom() {
        try {
            is.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try {
            in.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        out.close();
        try {
            clientSocket.close();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    // Zoom in
    private void getCutImage(BufferedImage imageToCut, int w, int h) {
        if (w - 50 <= 0) {
            w = 55;
        }
        if (w + 50 >= imageToCut.getWidth()) {
            w = imageToCut.getWidth() - 55;
        }
        if (h - 50 <= 0) {
            h = 55;
        }
        if (h + 50 >= imageToCut.getHeight()) {
            h = imageToCut.getHeight() - 55;
        }

        if (popupzoom.isShowing()) {
            zoomImgCut = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
            for (int i = -50; i < 50; i++) {
                for (int j = -50; j < 50; j++) {
                    zoomImgCut.setRGB(i + 50, j + 50, imageToCut.getRGB(w + i, h + j));
                }
            }

            // Create new (blank) image of required (scaled) size
            scaledCutImage = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
            // Paint scaled version of image to new image
            graphics2D = scaledCutImage.createGraphics();
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.drawImage(zoomImgCut, 0, 0, 300, 300, null);
            // clean up
            graphics2D.dispose();
            // draw image
            zoomLabel.setIcon(new ImageIcon(scaledCutImage));
            zoomImg.revalidate();
            zoomImg.add(zoomLabel);
        }
    }

    private void setupWatchDog(double timeout) {
        watchDog = new Thread(new Runnable() {
            @Override
            public void run() {
                methodWatchDog(timeout);
            }
        });
    }

    private void startWatchDog() {
        if (watchDog.getState().toString() != "TIMED_WAITING") {
            watchDog.start();
        }
    }

    private void resetWatchDog(double timeout) {
        endTimeMillis = (long) (System.currentTimeMillis() + timeout);
        virtualEndThread = false;
    }

    private void methodWatchDog(double miliseconds) {
        endTimeMillis = (long) (System.currentTimeMillis() + miliseconds);
        virtualEndThread = false;
        while (true) {
            if (System.currentTimeMillis() > endTimeMillis && !virtualEndThread) {
                if (!isCleanTurnOffCam) {
                    NeptusLog.pub().error("TIME OUT IPCAM");
                    NeptusLog.pub().info("Clossing all Video Stream...");
                    noVideoLogoState = false;
                    state = false;
                    ipCam = false;
                    initImage();
                }
                virtualEndThread = true;
            }
            else if (virtualEndThread) {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                }
            }

            try {
                Thread.sleep(100);
            }
            catch (InterruptedException e) {
            }
        }
    }
}
