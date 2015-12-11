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
 * Author: Pedro Gonçalves
 * Apr 4, 2015
 */
package pt.lsts.neptus.plugins.vision;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.RenderingHints;
import java.awt.Toolkit;
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
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.KeyStroke;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.Announce;
import pt.lsts.imc.CcuEvent;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.MapFeature;
import pt.lsts.imc.MapPoint;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
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
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * Neptus Plugin for Video Stream and tag frame/object
 * 
 * @author pedrog
 * @version 1.2
 * @category Vision
 *
 */
@SuppressWarnings("serial")
@Popup( pos = POSITION.RIGHT, width=640, height=480)
@LayerPriority(priority=0)
@PluginDescription(name="Video Stream", version="1.2", author="Pedro Gonçalves", description="Plugin for View video Stream TCP-Ip/IpCam", icon="pt/lsts/neptus/plugins/ipcam/camera.png")
public class Vision extends ConsolePanel implements ConfigurationListener, ItemListener{

    private static final String BASE_FOLDER_FOR_IMAGES = "log/images";
    private static final String BASE_FOLDER_FOR_ICON_IMAGES = "iconImages";
    private static final String BASE_FOLDER_FOR_URLINI = "ipUrl.ini";

    @NeptusProperty(name = "Axis Camera RTPS URL")
    private String camRtpsUrl = "rtsp://10.0.20.207:554/live/ch01_0";
    
    @NeptusProperty(name = "HOST IP for TCP-RasPiCam")
    private String ipHost = "10.0.20.130";

    @NeptusProperty(name = "Port Number for TCP-RasPiCam")
    private int portNumber = 2424;
    
    //Opencv library name
    private String libOpencvName = "libopencv2.4-jni";
    //private ServerSocket serverSocket = null;
    private Socket clientSocket = null;
    //Send data for sync 
    private PrintWriter out = null; 
    //Buffer for data image
    private InputStream is = null;
    //Buffer for info of data image
    private BufferedReader in = null;
    //Flag state of TCP connection
    private boolean tcpOK = false;
    //Strut Video Capture Opencv
    private VideoCapture capture;
    //Width size of image
    private int widthImgRec;
    //Height size of image
    private int heightImgRec;
    //Width size of Console
    private int widhtConsole = 640;
    //Height size of Console
    private int heightConsole = 480;
    //Scale factor of x pixel
    private float xScale;
    //Scale factor of y pixel
    private float yScale;
    //x pixel cord
    private int xPixel;
    //y pixel cord
    private int yPixel;
    //read size of pack compress
    private String line;
    //Buffer for data receive from DUNE over TCP
    private String duneGps;
    //Size of image received
    private int lengthImage;
    //buffer for save data receive
    private byte[] data;
    //Buffer image for JFrame/showImage
    private BufferedImage temp;
    //Flag - start acquired image
    private boolean raspiCam = false;
    //Flag - Lost connection to the vehicle
    private boolean state = false;
    //Flag - Show/hide Menu JFrame
    private boolean show_menu = false;
    //Flag state of IP CAM
    private boolean ipCam = false;
    //Save image tag flag
    private boolean captureFrame = false;
    //Close comTCP state
    private boolean closeComState = false;
    //Url of IpCam
    private String dataUrlIni[][];
    
    private boolean closingPanel = false;
    
    //JLabel for image
    private JLabel picLabel;
    //JPanel for display image
    private JPanel panelImage;
    //JPanel for info and config values
    private JPanel config;
    //JText info of data receive
    private JLabel txtText;
    //JText of data receive over IMC message
    private JLabel txtData;
    //JText of data warning message
    private JLabel warningText;
    //JText of data receive over DUNE TCP message
    private JLabel txtDataTcp;
    //JFrame for menu options
    private JFrame menu;
    //CheckBox to save image to HD
    private JCheckBox saveToDiskCheckBox;
    //JPopup Menu
    private JPopupMenu popup;
    //Flag to enable/disable zoom 
    private boolean zoomMask = false;
    
    //String for the info treatment 
    private String info;
    //String for the info of Image Size Stream
    private String infoSizeStream;
    //Data system
    private Date date = new Date();
    //Location of log folder
    private String logDir;
    //Image resize
    private Mat matResize;
    //Image receive
    private Mat mat;
    //Size of output frame
    private Size size = null;
    //Counter for image tag
    private int cntTag = 1;

    //counter for frame tag ID
    private short frameTagID =1;
    //lat, lon: frame Tag pos to be marked as POI
    private double lat,lon;

    //Flag for IpCam Ip Check
    boolean statePingOk = false;
    //JPanel for color state of ping to host ipcam
    private JPanel colorStateIpCam;
    //JFrame for IpCam Select
    private JFrame ipCamPing = new JFrame(I18n.text("Select IpCam"));
    //JPanel for IpCam Select (MigLayout)
    private JPanel ipCamCheck = new JPanel(new MigLayout());
    //JButton to confirm ipcam
    private JButton selectIpCam;
    //JComboBox for list of ipcam in ipUrl.ini
    @SuppressWarnings("rawtypes")
    private JComboBox ipCamList;
    //row select from string matrix of IpCam List
    private int rowSelect;
    //JLabel for text ipCam Ping
    private JLabel jlabel;
    //Dimension of Desktop Screen
    private Dimension dim;
    //JPanel for zoom point
    private JPanel zoomImg = new JPanel();
    //Buffer image for zoom Img Cut
    private BufferedImage zoomImgCut;
    //JLabel to show zoom image
    private JLabel zoomLabel = new JLabel();
    //Graphics2D for zoom image scaling
    private Graphics2D graphics2D;
    //BufferedImage for zoom image scaling
    private BufferedImage scaledCutImage;
    //PopPup zoom Image
    private JPopupMenu popupzoom;
    //cord x for zoom
    private int zoomX = 100;
    //cord y for zoom
    private int zoomY = 100;
    
    //!check ip for Host - TCP
    //JFormattedTextField for host ip
    private JFormattedTextField hostIp;
    //JFrame to check host connection
    private JFrame ipHostPing;
    //JPanel for host ip check
    private JPanel ipHostCheck;
    //Flag of ping state to host
    private boolean pingHostOk = false;
    
    //*** TEST FOR SAVE VIDEO **/
    private File outputfile;
    private boolean flagBuffImg = false;
    private int cnt = 0;
    private int FPS = 10;
    //*************************/
    
    //worker thread designed to acquire the data packet from DUNE
    private Thread updater = null; 
    //worker thread designed to save image do HD
    private Thread saveImg = null;
    
    public Vision(ConsoleLayout console) {
        super(console);
        if(findOpenCV()) {
            //clears all the unused initializations of the standard ConsolePanel
            removeAll();
            //!Resize Console
            this.addComponentListener(new ComponentAdapter() {  
                public void componentResized(ComponentEvent evt) {
                    Component c = evt.getComponent();
                    widhtConsole = c.getSize().width;
                    heightConsole = c.getSize().height;
                    widhtConsole = widhtConsole - 22;
                    heightConsole = heightConsole - 22;          
                    xScale = (float)widhtConsole/widthImgRec;
                    yScale = (float)heightConsole/heightImgRec;
                    size = new Size(widhtConsole, heightConsole);
                    matResize = new Mat(heightConsole, widhtConsole, CvType.CV_8UC3);
                    if(!raspiCam && !ipCam)
                        inicImage();
                }
            });
            //!Mouse click
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getButton() == MouseEvent.BUTTON1){
                        if(raspiCam || ipCam) {
                            xPixel = (int) ((e.getX() - 11) / xScale);  //shift window bar
                            yPixel = (int) ((e.getY() - 11) / yScale) ; //shift window bar
                            if(raspiCam && !ipCam && tcpOK) {
                                if (xPixel >= 0 && yPixel >= 0 && xPixel <= widthImgRec && yPixel <= heightImgRec)
                                    out.printf("%d#%d;\0", xPixel,yPixel);
                            }
                            captureFrame = true;
                            //place mark on map as POI
                            placeLocationOnMap();
                        }
                    }
                    if (e.getButton() == MouseEvent.BUTTON3) {
                        popup = new JPopupMenu();
                        JMenuItem item1;
                        popup.add(item1 = new JMenuItem(I18n.text("Start")+" RasPiCam", ImageUtils.createImageIcon(String.format(BASE_FOLDER_FOR_ICON_IMAGES + "/raspicam.jpg")))).addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                checkHostIp();
                            }
                        });
                        item1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
                        JMenuItem item2;
                        popup.add(item2 = new JMenuItem(I18n.text("Close all connections"), ImageUtils.createImageIcon(String.format(BASE_FOLDER_FOR_ICON_IMAGES + "/close.gif")))).addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                NeptusLog.pub().info("Clossing all Video Stream...");
                                if(raspiCam && tcpOK) {
                                    try {
                                        clientSocket.close();
                                    }
                                    catch (IOException e1) {
                                        e1.printStackTrace();
                                    }
                                }
                                raspiCam = false;
                                state = false;
                                ipCam = false;
                            }
                        });
                        item2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
                        JMenuItem item3;  
                        popup.add(item3 = new JMenuItem(I18n.text("Start IpCam"), ImageUtils.createImageIcon(String.format(BASE_FOLDER_FOR_ICON_IMAGES + "/ipcam.png")))).addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                checkIpCam();        
                            }
                        });
                        popup.addSeparator();
                        item3.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
                        JMenuItem item4;
                        popup.add(item4 = new JMenuItem(I18n.text("Config"), ImageUtils.createImageIcon(String.format(BASE_FOLDER_FOR_ICON_IMAGES + "/config.jpeg")))).addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                menu.setVisible(true);
                            }
                        });
                        item4.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
                        popup.show((Component) e.getSource(), e.getX(), e.getY());
                    }
                }
            });
            //!Detect key-pressed
            this.addKeyListener(new KeyListener() {            
                @Override
                public void keyReleased(KeyEvent e) {
                    if(e.getKeyCode() == KeyEvent.VK_Z && zoomMask) {
                        zoomMask = false;
                        popupzoom.setVisible(false);
                    }
                }
                @Override
                public void keyPressed(KeyEvent e) {
                    if((e.getKeyCode() == KeyEvent.VK_Z) && ((e.getModifiers() & KeyEvent.ALT_MASK) != 0) && !zoomMask) {
                        if(raspiCam || ipCam) {
                            zoomMask = true;
                            popupzoom.add(zoomImg);
                        }
                    }
                    else if((e.getKeyCode() == KeyEvent.VK_I) && ((e.getModifiers() & KeyEvent.ALT_MASK) != 0))
                        checkIpCam();
                    else if((e.getKeyCode() == KeyEvent.VK_R) && ((e.getModifiers() & KeyEvent.ALT_MASK) != 0))
                        checkHostIp();
                    else if((e.getKeyCode() == KeyEvent.VK_X) && ((e.getModifiers() & KeyEvent.ALT_MASK) != 0)) {
                        NeptusLog.pub().info("Clossing all Video Stream...");
                        raspiCam = false;
                        state = false;
                        ipCam = false;
                    }
                    else if((e.getKeyCode() == KeyEvent.VK_C) && ((e.getModifiers() & KeyEvent.ALT_MASK) != 0))
                        menu.setVisible(true);
                    else if(e.getKeyChar() == 'z' && zoomMask) {
                        int xLocMouse = MouseInfo.getPointerInfo().getLocation().x - getLocationOnScreen().x - 11;
                        int yLocMouse = MouseInfo.getPointerInfo().getLocation().y - getLocationOnScreen().y - 11;
                        if(xLocMouse < 0)
                            xLocMouse = 0;
                        if(yLocMouse < 0)
                            yLocMouse = 0;
                        
                        if(xLocMouse + 52 < panelImage.getSize().getWidth() && xLocMouse - 52 > 0 && yLocMouse + 60 < panelImage.getSize().getHeight() && yLocMouse - 60 > 0){
                            zoomX = xLocMouse;
                            zoomY = yLocMouse;
                            popupzoom.setLocation(MouseInfo.getPointerInfo().getLocation().x - 150, MouseInfo.getPointerInfo().getLocation().y - 150);
                            getCutImage(temp, zoomX, zoomY);
                            popupzoom.setVisible(true);
                        }
                        else
                            popupzoom.setVisible(false);
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
            
            //JLabel for image
            picLabel = new JLabel();
            //JPanel for Image
            panelImage = new JPanel();
            panelImage.setBackground(Color.LIGHT_GRAY);
            panelImage.setSize(this.getWidth(), this.getHeight());
            this.setLayout(new MigLayout());
            this.add(panelImage, BorderLayout.NORTH);
            BufferedImage errorImg = null;
            try {
                errorImg = ImageIO.read(new File(FileUtil.getResourceAsFileKeepName("images/errorOpencv.png")));
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
            showImage(resize(errorImg, 320, 240));
            //JLabel info
            warningText = new JLabel("  " +I18n.textf("Please install %libopencv and its dependencies.", libOpencvName)+ "  ");
            warningText.setForeground(Color.RED);
            warningText.setFont(new Font("Courier New", Font.ITALIC, 18));
            warningText.setBackground(Color.yellow);
            warningText.setOpaque(true);
            this.add(warningText, BorderLayout.SOUTH); 
            return;
        }
        return;
    }
    
    //!Check ip given by user
    private void checkHostIp() {
        ipHostPing = new JFrame(I18n.text("Host IP")+" - RasPiCam");
        ipHostPing.setSize(340, 80);
        ipHostPing.setLocation(dim.width/2-ipCamPing.getSize().width/2, dim.height/2-ipCamPing.getSize().height/2);
        ipHostCheck = new JPanel(new MigLayout());
        ImageIcon imgIpCam = ImageUtils.createImageIcon(String.format(BASE_FOLDER_FOR_ICON_IMAGES + "/raspicam.jpg"));
        ipHostPing.setIconImage(imgIpCam.getImage());
        ipHostPing.setResizable(false);
        ipHostPing.setBackground(Color.GRAY);
        JLabel infoHost = new JLabel(I18n.text("Host Ip: "));
        ipHostCheck.add(infoHost, "cell 0 4 3 1");
        hostIp = new JFormattedTextField();
        hostIp.setValue(new String());
        hostIp.setColumns(8);
        hostIp.setValue(ipHost);
        hostIp.addPropertyChangeListener("value", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ipHost = new String((String) evt.getNewValue());
            }
        });
        ipHostCheck.add(hostIp);
        colorStateIpCam = new JPanel();
        jlabel = new JLabel(I18n.text("OFF"));
        jlabel.setFont(new Font("Verdana",1,14));
        colorStateIpCam.setBackground(Color.RED);
        colorStateIpCam.add(jlabel);
        ipHostCheck.add(colorStateIpCam,"h 30!, w 30!");
        selectIpCam = new JButton(I18n.text("Check"));
        selectIpCam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(pingIpCam(ipHost)) {
                    colorStateIpCam.setBackground(Color.GREEN);
                    jlabel.setText("ON");
                    pingHostOk = true;
                }
                else {
                    colorStateIpCam.setBackground(Color.RED);
                    jlabel.setText("OFF");
                    pingHostOk = false;
                }
            }
        });
        ipHostCheck.add(selectIpCam,"h 30!");
        selectIpCam = new JButton(I18n.text("OK"));
        selectIpCam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(pingHostOk) {
                    ipHostPing.setVisible(false);
                    if(!ipCam) {
                        raspiCam = true;
                        ipCam = false;
                        closeComState = false;
                    }
                    else {
                        NeptusLog.pub().info("Clossing IpCam Stream...");
                        closeComState = false;
                        raspiCam = true;
                        state = false;
                        ipCam = false;
                    }
                }
            }
        });
        ipHostCheck.add(selectIpCam,"h 30!");
        ipHostPing.add(ipHostCheck);
        ipHostPing.setVisible(true);
    }
    
    //!Read ipUrl.ini to find IpCam ON
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void checkIpCam() {
        dataUrlIni = readIpUrl();
        int sizeDataUrl = dataUrlIni.length;
        String nameIpCam[] = new String[sizeDataUrl];
        for (int i=0; i < sizeDataUrl; i++)
            nameIpCam[i] = dataUrlIni[i][0];
        
        ipCamPing = new JFrame(I18n.text("Select IpCam"));
        ipCamPing.setSize(340, 200);
        ipCamPing.setLocation(dim.width / 2 - ipCamPing.getSize().width / 2,
                dim.height / 2 - ipCamPing.getSize().height / 2);
        ipCamCheck = new JPanel(new MigLayout());
        ImageIcon imgIpCam = ImageUtils.createImageIcon(String.format(BASE_FOLDER_FOR_ICON_IMAGES + "/ipcam.png"));
        ipCamPing.setIconImage(imgIpCam.getImage());
        ipCamPing.setResizable(false);
        ipCamPing.setBackground(Color.GRAY);                  
        ipCamList = new JComboBox(nameIpCam);
        ipCamList.setSelectedIndex(0);
        ipCamList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JComboBox cb = (JComboBox)e.getSource();
                rowSelect = cb.getSelectedIndex();
                if(rowSelect != 0) {
                    if(pingIpCam(dataUrlIni[rowSelect][1])) {
                        camRtpsUrl = dataUrlIni[rowSelect][2];
                        colorStateIpCam.setBackground(Color.GREEN);
                        jlabel.setText("ON");
                    }
                    else {
                        colorStateIpCam.setBackground(Color.RED);
                        jlabel.setText("OFF");
                    }   
                }
                else {
                    statePingOk = false;
                    colorStateIpCam.setBackground(Color.RED);
                    jlabel.setText("OFF");
                }
            }
        });
        ipCamCheck.add(ipCamList,"span, split 3, center");
        
        colorStateIpCam = new JPanel();
        jlabel = new JLabel(I18n.text("OFF"));
        jlabel.setFont(new Font("Verdana",1,14));
        colorStateIpCam.setBackground(Color.RED);
        colorStateIpCam.add(jlabel);
        ipCamCheck.add(colorStateIpCam,"h 30!, w 30!");
        
        selectIpCam = new JButton(I18n.text("Select IpCam"), imgIpCam);
        selectIpCam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if(statePingOk) {
                    NeptusLog.pub().info("IpCam Select: "+dataUrlIni[rowSelect][0]);
                    ipCamPing.setVisible(false);
                    if(!raspiCam) {
                        ipCam = true;
                        raspiCam = false;
                        state = false;
                    }
                    else {
                        NeptusLog.pub().info("Clossing RasPiCam Stream...");
                        ipCam = true;
                        raspiCam = false;
                        state = false;
                        closeComState = true;
                    }
                }
            }
        });
        ipCamCheck.add(selectIpCam,"h 30!");
        
        JTextField fieldName = new JTextField(I18n.text("Name"));
        JTextField fieldIp = new JTextField(I18n.text("Ip"));
        JTextField fieldUrl = new JTextField(I18n.text("Url"));
        JButton addNewIpCam = new JButton(I18n.text("Add New IpCam"));
        addNewIpCam.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //Execute when button is pressed
                writeToFile(String.format("%s (%s)#%s#%s\n", fieldName.getText(), fieldIp.getText(), fieldIp.getText(), fieldUrl.getText()));
                dataUrlIni = readIpUrl();
                int sizeDataUrl = dataUrlIni.length;
                String nameIpCam[] = new String[sizeDataUrl];
                for (int i=0; i < sizeDataUrl; i++)
                    nameIpCam[i] = dataUrlIni[i][0];
                
                ipCamList.removeAllItems();
                for (int i = 0; i < nameIpCam.length; i++) {
                    String sample = nameIpCam[i];
                    ipCamList.addItem(sample);
                }
            }
        });
        
        ipCamCheck.add(fieldName, "w 320!, wrap");
        ipCamCheck.add(fieldIp, "w 320!, wrap");
        ipCamCheck.add(fieldUrl, "w 320!, wrap");
        ipCamCheck.add(addNewIpCam, "w 120!, center");
        
        
        ipCamPing.add(ipCamCheck);
        ipCamPing.setVisible(true);
    }
    
  //Write to file
    private void writeToFile(String textString){
        BufferedWriter brf = null;
        String iniRsrcPath = FileUtil.getResourceAsFileKeepName(BASE_FOLDER_FOR_URLINI);
        File confIni = new File(ConfigFetch.getConfFolder() + "/" + BASE_FOLDER_FOR_URLINI);
        if (!confIni.exists()) {
            FileUtil.copyFileToDir(iniRsrcPath, ConfigFetch.getConfFolder());
        }
        try {
            brf = new BufferedWriter(new FileWriter(confIni, true));
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try {
            brf.write(textString);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        try {
            brf.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    //!Ping CamIp
    private boolean pingIpCam (String host) {
        boolean ping = false;
        try {
            String cmd = "";
            if(System.getProperty("os.name").startsWith("Windows")) {   
                    // For Windows
                    cmd = "ping -n 1 " + host;
            }
            else {
                    // For Linux and OSX
                    cmd = "ping -c 1 " + host;
            }
            Process myProcess = Runtime.getRuntime().exec(cmd);
            try {
                myProcess.waitFor();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(myProcess.exitValue() == 0)
                ping = true;
            else
                ping = false;
        }
        catch (UnknownHostException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        } //Ping doesnt work 
        
        statePingOk = ping;
        return ping;
    }
    
    //!Read file
    private String[][] readIpUrl() {
        //Open the file for reading and split (#)
        BufferedReader br = null;
        String lineFile;
        String[] splits;
        String[][] dataSplit = null;
        int cntReader = 0;
        try {
            String iniRsrcPath = FileUtil.getResourceAsFileKeepName(BASE_FOLDER_FOR_URLINI);
            File confIni = new File(ConfigFetch.getConfFolder() + "/" + BASE_FOLDER_FOR_URLINI);
            if (!confIni.exists()) {
                FileUtil.copyFileToDir(iniRsrcPath, ConfigFetch.getConfFolder());
            }
            br = new BufferedReader(new FileReader(confIni));
            while ((lineFile = br.readLine()) != null)
                cntReader++;
            
            br.close();
            br = new BufferedReader(new FileReader(confIni));
            
            System.out.println("Valor: "+cntReader);
            
            dataSplit = new String[cntReader+1][3];
            cntReader = 1;
            dataSplit[0][0] = "IpCam Select";
            while ((lineFile = br.readLine()) != null) {
                splits = lineFile.split("#");
                dataSplit[cntReader][0] = splits[0];
                dataSplit[cntReader][1] = splits[1];
                dataSplit[cntReader][2] = splits[2];
                cntReader++;
            }
        }
        catch (IOException e) {
           System.err.println("Error: " + e);
        }
        try {
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return dataSplit;
    }
    
    public String timestampToReadableHoursString(long timestamp){
        Date date = new Date(timestamp);
        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
        return format.format(date);
    }

    /**
     * Adapted from ContactMarker.placeLocationOnMap()
     */
    private void placeLocationOnMap() {

        if (getConsole().getMission() == null)
            return;

        double lat = this.lat;
        double lon = this.lon;
        long timestamp = System.currentTimeMillis();
        String id = I18n.text("FrameTag") + "-" + frameTagID + "-" + timestampToReadableHoursString(timestamp);

        boolean validId = false;
        while (!validId) {
            id = JOptionPane.showInputDialog(getConsole(), I18n.text("Please enter new mark name"), id);
            if (id == null)
                return;
            AbstractElement elems[] = MapGroup.getMapGroupInstance(getConsole().getMission()).getMapObjectsByID(id);
            if (elems.length > 0)
                GuiUtils.errorMessage(getConsole(), I18n.text("Add mark"),
                        I18n.text("The given ID already exists in the map. Please choose a different one"));
            else
                validId = true;
        }
        frameTagID++;//increment ID

        MissionType mission = getConsole().getMission();
        LinkedHashMap<String, MapMission> mapList = mission.getMapsList();
        if (mapList == null)
            return;
        if (mapList.size() == 0)
            return;
        // MapMission mapMission = mapList.values().iterator().next();
        MapGroup.resetMissionInstance(getConsole().getMission());
        MapType mapType = MapGroup.getMapGroupInstance(getConsole().getMission()).getMaps()[0];// mapMission.getMap();
        // NeptusLog.pub().info("<###>MARKER --------------- " + mapType.getId());
        MarkElement contact = new MarkElement(mapType.getMapGroup(), mapType);

        contact.setId(id);
        contact.setCenterLocation(new LocationType(lat,lon));
        mapType.addObject(contact);
        mission.save(false);
        MapPoint point = new MapPoint();
        point.setLat(lat);
        point.setLon(lon);
        point.setAlt(0);
        MapFeature feature = new MapFeature();
        feature.setFeatureType(MapFeature.FEATURE_TYPE.POI);
        feature.setFeature(Arrays.asList(point));
        CcuEvent event = new CcuEvent();
        event.setType(CcuEvent.TYPE.MAP_FEATURE_ADDED);
        event.setId(id);
        event.setArg(feature);
        this.getConsole().getImcMsgManager().broadcastToCCUs(event);
        NeptusLog.pub().info("placeLocationOnMap: " + id + " - Pos: lat: " + this.lat + " ; lon: " + this.lon);
    }

    //!Print Image to JPanel
    private void showImage(BufferedImage image) {
        picLabel.setIcon(new ImageIcon(image));
        panelImage.revalidate();
        panelImage.add(picLabel, BorderLayout.CENTER);
        repaint();
    }
        
    //!Config Layout
    private void configLayout() {
        dim = Toolkit.getDefaultToolkit().getScreenSize();
        //Create Buffer (type MAT) for Image resize
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        matResize = new Mat(heightConsole, widhtConsole, CvType.CV_8UC3);
        
        //Config JFrame zoom img
        zoomImg.setSize(300, 300);
        popupzoom = new JPopupMenu();
        popupzoom.setSize(300, 300);
        //!Create folder to save image data
        //Create folder image in log if don't exist
        File dir = new File(String.format(BASE_FOLDER_FOR_IMAGES));
        dir.mkdir();
        //Create folder Image to save data received
        dir = new File(String.format(BASE_FOLDER_FOR_IMAGES + "/%s", date));
        dir.mkdir();
        //Create folder Image Tag
        dir = new File(String.format(BASE_FOLDER_FOR_IMAGES + "/%s/imageTag", date));
        dir.mkdir();
        //Create folder Image Save
        dir = new File(String.format(BASE_FOLDER_FOR_IMAGES + "/%s/imageSave", date));
        dir.mkdir();
        logDir = String.format(BASE_FOLDER_FOR_IMAGES + "/%s", date);
        
        //JLabel for image
        picLabel = new JLabel();
        //JPanel for Image
        panelImage = new JPanel();
        panelImage.setBackground(Color.LIGHT_GRAY);
        panelImage.setSize(this.getWidth(), this.getHeight());
        this.setLayout(new MigLayout());
        this.add(panelImage, BorderLayout.CENTER);
        
        //JPanel for info and config values      
        config = new JPanel(new MigLayout());

        //JCheckBox save to HD
        saveToDiskCheckBox = new JCheckBox(I18n.text("Save Image to Disk"));
        saveToDiskCheckBox.setMnemonic(KeyEvent.VK_C);
        saveToDiskCheckBox.setSelected(false);
        saveToDiskCheckBox.addItemListener(this);
        config.add(saveToDiskCheckBox,"width 160:180:200, h 40!, wrap");
        
        //JLabel info Data received
        txtText = new JLabel();
        txtText.setToolTipText(I18n.text("Info of Frame Received"));
        info = String.format("Img info");
        txtText.setText(info);
        config.add(txtText, "cell 0 4 3 1, wrap");
        
        //JLabel info Data GPS received TCP
        txtDataTcp = new JLabel();
        txtDataTcp.setToolTipText(I18n.text("Info of GPS Received over TCP (Raspi)"));
        info = String.format("GPS TCP");
        txtDataTcp.setText(info);
        config.add(txtDataTcp, "cell 0 5 3 1, wrap");
        
        //JLabel info
        txtData = new JLabel();
        txtData.setToolTipText(I18n.text("Info of GPS Received over IMC"));
        info = String.format("GPS IMC");
        txtData.setText(info);
        config.add(txtData, "cell 0 6 3 1, wrap");
                
        menu = new JFrame(I18n.text("Menu Config"));
        menu.setResizable(false);
        menu.setSize(450, 350);
        menu.setLocation(dim.width/2-menu.getSize().width/2, dim.height/2-menu.getSize().height/2);
        menu.setVisible(show_menu);
        ImageIcon imgMenu = ImageUtils.createImageIcon(String.format(BASE_FOLDER_FOR_ICON_IMAGES + "/config.jpeg"));
        menu.setIconImage(imgMenu.getImage());
        menu.add(config);
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        //checkbox listener
        Object source = e.getItemSelectable();
        //System.out.println("source: "+source);
        if (source == saveToDiskCheckBox) {
            //System.out.println("raspiCam="+raspiCam+"ipCam"+ipCam+"saveToDiskCheckBox"+saveToDiskCheckBox.isSelected());
            if ((raspiCam == true || ipCam == true) && saveToDiskCheckBox.isSelected() == true) {
                flagBuffImg = true;
                //System.out.println("Valor: "+flagBuffImg);
            }
            if ((raspiCam == false && ipCam == false) || saveToDiskCheckBox.isSelected() == false) {
                flagBuffImg=false;
            }
        }
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        if(raspiCam){
            NeptusLog.pub().info("Closing TCP connection to RaspiCam ");
            if(raspiCam && tcpOK)
                closeTcpCom();
        }
        closingPanel = true;
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        if(findOpenCV()){
            getConsole().getImcMsgManager().addListener(this);
            configLayout();
            updater = updaterThread();
            updater.start();
            saveImg = updaterThreadSave();
            saveImg.start();
        }
        else
        {
            NeptusLog.pub().error("Opencv not found.");
            closingPanel = true;
            return;
        }
    }
    
    //!Find OPENCV JNI in host PC
    private boolean findOpenCV() {
        return SearchOpenCv.searchJni();
    }
    
    //Get size of image
    private void initSizeImage() {
        //Width size of image
        try {
            widthImgRec = Integer.parseInt(in.readLine());
        }
        catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
        //Height size of image
        try {
            heightImgRec = Integer.parseInt(in.readLine());
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        xScale = (float) widhtConsole / widthImgRec;
        yScale = (float) heightConsole / heightImgRec;
        //Create Buffer (type MAT) for Image receive
        mat = new Mat(heightImgRec, widthImgRec, CvType.CV_8UC3);
    }
    
    //!Thread to handle data receive
    private Thread updaterThread() {
        Thread ret = new Thread("Video Stream Thread") {
            @Override
            public void run() {
                inicImage();
                while(true) {
                    if (closingPanel) {
                        raspiCam = false;
                        state = false;
                        ipCam = false;
                    }
                    else if (raspiCam && !ipCam ) {
                        if(state == false) {
                            //connection
                            if(tcpConnection()) {
                                //receive info of image size
                                initSizeImage();
                                state = true;
                            }
                        }
                        else {
                            //receive data image
                            if(!closeComState)
                                receivedDataImage();
                            else
                                closeTcpCom();
                            if(!raspiCam && !state)
                                closeTcpCom();
                        }
                    }
                    else if (!raspiCam && ipCam) {
                        if (state == false){
                            //Create Buffer (type MAT) for Image receive
                            mat = new Mat(heightImgRec, widthImgRec, CvType.CV_8UC3);
                            capture = new VideoCapture();
                            capture.open(camRtpsUrl);
                            if (capture.isOpened()) {
                                state = true;
                                cntTag = 1;
                                NeptusLog.pub().info("Video Strem from IpCam is captured");
                            }
                            else {
                                ipCam = false;
                                NeptusLog.pub().info("Video Strem from IpCam is not captured");
                            }
                        }
                        //IpCam Capture
                        else if(!raspiCam && ipCam && state) {
                            long startTime = System.currentTimeMillis();
                            capture.grab();
                            capture.read(mat);
                            while(mat.empty()) {
                                System.out.println("ERRO");
                                capture.read(mat);
                            }
                            xScale = (float) widhtConsole / mat.cols();
                            yScale = (float) heightConsole / mat.rows();
                            Imgproc.resize(mat, matResize, size);       
                            //Convert Mat to BufferedImage
                            temp=matToBufferedImage(matResize);
                            //Display image in JFrame
                            long stopTime = System.currentTimeMillis();
                            long fpsResult = stopTime - startTime;
                            if(fpsResult != 0)
                                infoSizeStream = String.format("Size(%d x %d) | Scale(%.2f x %.2f) | FPS:%d |\t\t\t", mat.cols(), mat.rows(),xScale,yScale,(int)(1000/fpsResult));

                            txtText.setText(infoSizeStream);
                            showImage(temp);
                            
                            if( captureFrame ) {
                                xPixel = xPixel - widhtConsole/2;
                                yPixel = -(yPixel - heightConsole/2);
                                String imageTag = String.format("%s/imageTag/(%d)_%s_X=%d_Y=%d.jpeg",logDir,cntTag,info,xPixel,yPixel);
                                outputfile = new File(imageTag);
                                try {
                                    ImageIO.write(temp, "jpeg", outputfile);
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }
                                captureFrame = false;
                                cntTag++;
                            }
                        }
                    }
                    else{
                        try {
                            TimeUnit.MILLISECONDS.sleep(1000);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        inicImage();
                    }
                    if (closingPanel)
                        break;
                }
            }
        };
        ret.setDaemon(true);
        return ret;
    }

    //!Thread to handle save image
    private Thread updaterThreadSave() {
        Thread si = new Thread("Save Image") {
            @Override
            public void run() {
                while(true) {
                    if (raspiCam || ipCam) {
                        if(flagBuffImg == true) {
                            //flagBuffImg = false;
                            long startTime = System.currentTimeMillis();
                            String imageJpeg = String.format("%s/imageSave/%d.jpeg",logDir,cnt);
                            outputfile = new File(imageJpeg);
                            try {
                                ImageIO.write(temp, "jpeg", outputfile);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                            cnt++;
                            long stopTime = System.currentTimeMillis();
                            while((stopTime - startTime) < (1000/FPS)) {
                                stopTime = System.currentTimeMillis();
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
                    if (closingPanel)
                        break;
               }
           }
        };
        si.setDaemon(true);
        return si;
    }
    
    //!IMC handle
    @Subscribe
    public void consume(EstimatedState msg) {   
        //System.out.println("Source Name "+msg.getSourceName()+"ID "+getMainVehicleId());
        if(msg.getSourceName().equals(getMainVehicleId())) {
            try {
                //! update the position of target
                //LAT and LON rad
                double latRad = msg.getLat();
                double lonRad = msg.getLon();
                //LAT and LON deg
                double latDeg = Math.toDegrees(latRad);
                double lonDeg = Math.toDegrees(lonRad);

                LocationType locationType = new LocationType(latDeg,lonDeg);

                //Offset (m)
                double offsetN = msg.getX();
                double offsetE = msg.getY();
                
                //Height of Vehicle
                double heightRelative = msg.getHeight()-msg.getZ();//absolute altitude - zero of that location
                locationType.setOffsetNorth(offsetN);
                locationType.setOffsetEast(offsetE);
                locationType.setHeight(heightRelative);

                double camTiltDeg = 45.0f;//this value may be in configuration
                info = String.format("(IMC) LAT: %f # LON: %f # ALT: %.2f m", lat, lon, heightRelative);
                LocationType tagLocationType = calcTagPosition(locationType.convertToAbsoluteLatLonDepth(), Math.toDegrees(msg.getPsi()), camTiltDeg);
                this.lat = tagLocationType.convertToAbsoluteLatLonDepth().getLatitudeDegs();
                this.lon = tagLocationType.convertToAbsoluteLatLonDepth().getLongitudeDegs();
                txtData.setText(info);
            }
            catch (Exception e) {
            }
        }
    }

    /**
     *
     * @param locationType
     * @param orientationDegrees
     * @param camTiltDeg
     * @return
     */
    public LocationType calcTagPosition(LocationType locationType, double orientationDegrees, double camTiltDeg) {
        double altitude = locationType.getHeight();
        double dist = Math.tan(Math.toRadians(camTiltDeg))*(Math.abs(altitude));// hypotenuse
        double offsetN = Math.cos(Math.toRadians(orientationDegrees))*dist;//oposite side
        double offsetE = Math.sin(Math.toRadians(orientationDegrees))*dist;// adjacent side

        LocationType tagLocationType = locationType.convertToAbsoluteLatLonDepth();
        tagLocationType.setOffsetNorth(offsetN);
        tagLocationType.setOffsetEast(offsetE);
        return tagLocationType.convertToAbsoluteLatLonDepth();
    }

    @Subscribe
    public void consume(Announce announce) {
    }
    
    //!Fill cv::Mat image with zeros
    public void inicImage() {
        Scalar black = new Scalar(0);
        matResize.setTo(black);
        temp=matToBufferedImage(matResize);
        showImage(temp);
    }
    
    //!Received data Image
    public void receivedDataImage() {
        long startTime = System.currentTimeMillis();
        try {
            line = in.readLine();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        if (line == null){
            JOptionPane.showMessageDialog(panelImage, I18n.text("Lost connection with vehicle"), I18n.text("Connection error"), JOptionPane.ERROR_MESSAGE);
            raspiCam = false;
            state = false;
            //closeTcpCom();
            try {
                clientSocket.close();
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        else {        
            lengthImage = Integer.parseInt(line);
            //buffer for save data receive
            data = new byte[lengthImage];
            //Send 1 for server for sync data send
            out.println("1\0");
            //read data image (ZP)
            int read = 0;
            while (read < lengthImage) {
                int readBytes = 0;
                try {
                    readBytes = is.read(data, read, lengthImage-read);
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
            //Receive data GPS over tcp DUNE
            try {
                duneGps = in.readLine();
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
            //Decompress data received 
            Inflater decompresser = new Inflater(false);
            decompresser.setInput(data,0,lengthImage);
            //Create an expandable byte array to hold the decompressed data
            ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
            //Decompress the data
            byte[] buf = new byte[(widthImgRec * heightImgRec * 3)];
            while (!decompresser.finished()) 
            {
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
            
            //Transform byte data to cv::Mat (for display image)
            mat.put(0, 0, decompressedData);
            //Resize image to console size
            Imgproc.resize(mat, matResize, size);
                       
            //Convert Mat to BufferedImage
            temp=matToBufferedImage(matResize);    
            
            xScale = (float) widhtConsole / widthImgRec;
            yScale = (float) heightConsole / heightImgRec;
            long stopTime = System.currentTimeMillis();
            while((stopTime - startTime) < (1000/FPS))
                stopTime = System.currentTimeMillis();
            
            info = String.format("Size(%d x %d) | Scale(%.2f x %.2f) | FPS:%d | Pak:%d (KiB:%d)", widthImgRec, heightImgRec,xScale,yScale,(int) 1000/(stopTime - startTime),lengthImage,lengthImage/1024);
            txtText.setText(info);
            txtDataTcp.setText(duneGps);
            //Display image in JFrame
            showImage(temp);
        }     
    }
    
    //!Resize Buffered Image
    public static BufferedImage resize(BufferedImage img, int newW, int newH) { 
        Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        return dimg;
    }
    
    //!Close TCP COM
    public void closeTcpCom() {
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
    
    //!Create Socket service
    public boolean tcpConnection() {
        //Socket Config    
        NeptusLog.pub().info("Waiting for connection from RasPiCam...");
        try { 
            clientSocket = new Socket(ipHost, portNumber);
            if(clientSocket.isConnected());
                tcpOK=true;
        } 
        catch (IOException e) 
        { 
            //NeptusLog.pub().error("Accept failed...");
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            }
            catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            tcpOK = false; 
        }
        if(tcpOK){
            NeptusLog.pub().info("Connection successful from Server: "+clientSocket.getInetAddress()+":"+clientSocket.getLocalPort());
            NeptusLog.pub().info("Receiving data image from RasPiCam...");
                
            //Send data for sync 
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }

            //Buffer for data image
            try{
                is = clientSocket.getInputStream();
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
            //Buffer for info of data image
            in = new BufferedReader( new InputStreamReader( is ));

            return true;
        }
        else
            return false;
    }
    
    /**  
     * Converts/writes a Mat into a BufferedImage.  
     * @param matrix Mat of type CV_8UC3 or CV_8UC1  
     * @return BufferedImage of type TYPE_3BYTE_BGR or TYPE_BYTE_GRAY  
     */  
    public static BufferedImage matToBufferedImage(Mat matrix) {
        int cols = matrix.cols();  
        int rows = matrix.rows();  
        int elemSize = (int)matrix.elemSize();  
        byte[] data = new byte[cols * rows * elemSize];  
        int type;  
        matrix.get(0, 0, data);  
        switch (matrix.channels()) {  
            case 1:  
                type = BufferedImage.TYPE_BYTE_GRAY;  
                break;  
            case 3:  
                type = BufferedImage.TYPE_3BYTE_BGR;  
                // bgr to rgb  
                byte b;  
                for(int i=0; i<data.length; i=i+3) {  
                    b = data[i];  
                    data[i] = data[i+2];  
                    data[i+2] = b;  
                }  
                break;  
        default:  
            return null;  
        }
        BufferedImage image2 = new BufferedImage(cols, rows, type);  
        image2.getRaster().setDataElements(0, 0, cols, rows, data);  
        return image2;
    }
    
    //!Zoom in
    public void getCutImage(BufferedImage imageToCut, int w, int h) {
        zoomImgCut = new BufferedImage (100, 100, BufferedImage.TYPE_3BYTE_BGR);
        for( int i = -50; i < 50; i++ )
            for( int j = -50; j < 50; j++ )
                zoomImgCut.setRGB(i + 50, j + 50, imageToCut.getRGB( w+i, h+j));

        // Create new (blank) image of required (scaled) size
        scaledCutImage = new BufferedImage(300, 300, BufferedImage.TYPE_INT_ARGB);
        // Paint scaled version of image to new image
        graphics2D = scaledCutImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        graphics2D.drawImage(zoomImgCut, 0, 0, 300, 300, null);
        // clean up
        graphics2D.dispose();
        //draw image
        zoomLabel.setIcon(new ImageIcon(scaledCutImage));
        zoomImg.revalidate();
        zoomImg.add(zoomLabel);
    }
}