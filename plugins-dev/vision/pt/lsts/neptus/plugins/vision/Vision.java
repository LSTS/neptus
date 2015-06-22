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
 * Author: Pedro Gonçalves
 * Apr 4, 2015
 */
package pt.lsts.neptus.plugins.vision;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import pt.lsts.imc.Announce;
import pt.lsts.imc.EstimatedState;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.LayerPriority;

import com.google.common.eventbus.Subscribe;
import pt.lsts.util.WGS84Utilities;


/**
 * Neptus Plugin for Video Stream 
 * 
 * @author pedrog
 * @version 1.0
 * @category Vision
 *
 */
@Popup( pos = POSITION.RIGHT, width=640, height=400)
@LayerPriority(priority=0)
@PluginDescription(name="Video Stream", version="1.0", author="Pedro Gonçalves", description="Neptus Plugin for View video Strem TCP-IP", icon="pt/lsts/neptus/plugins/ipcam/camera.png")
public class Vision extends ConsolePanel implements ConfigurationListener, ItemListener{

    private static final long serialVersionUID = 1L;
    
    ServerSocket serverSocket = null;
    Socket clientSocket = null;
    //Send data for sync 
    PrintWriter out = null; 
    //Buffer for data image
    InputStream is = null;
    //Buffer for info of data image
    BufferedReader in = null;
    //Struct Video Capture Opencv
    VideoCapture capture;
    //Width size of image
    int widthImgRec;
    //Height size of image
    int heightImgRec;
    //Scale factor of x
    float xScale;
    //Scale factor of y
    float yScale;
    //read size of pack compress
    String line;
    //Buffer for data receive from DUNE over tcp
    String duneGps;
    //Size of image received
    int lengthImage;
    //buffer for save data receive
    byte[] data;
    //Buffer image for JFrame/showImage
    BufferedImage temp;
    //Flag - start acquired image
    boolean isRunning = false;
    //Flag - Lost connection to the vehicle
    boolean state = false;
    //Flag - Show/hide Menu JFrame
    boolean show_menu = false;
    //Flag state of IP CAM
    boolean ipCam = false;
    //Save image tag flag
    boolean captureFrame = false;
    //JLabel for image
    JLabel picLabel;
    //JPanel for Image
    JPanel frame;
    //JPanel for display image
    JPanel panelImage;
    //JPanel for info and config values
    JPanel config;
    //JText info of data receive
    JTextField txtText;
    //JText of data receive over IMC message
    JTextField txtData;
    //JText of data receive over DUNE TCP message
    JTextField txtDataTcp;
    //JFrame for menu options
    JFrame menu;
    //String for the info treatment 
    String info;
    //JPopup Menu
    JPopupMenu popup;
    //Data system
    Date date = new Date();
    //Location of log folder
    String logDir;
    //Image resize
    Mat matResize;
    //Image receive
    Mat mat;
    //Size of output frame
    Size size = new Size(960, 720);
    //ID vehicle
    int idVehicle = 0;
    //Counter for image tag
    int cntTag = 1;

    //*** TEST FOR SAVE VIDEO **/
    File outputfile;
    boolean flagBuffImg = false;
    int cnt=0;
    int FPS = 10;
    //*************************/
    
    //IMC message
    EstimatedState msg;
    protected LinkedHashMap<String, EstimatedState> msgsSetLeds = new LinkedHashMap<>(); 
    
    //worker thread designed to acquire the data packet from DUNE
    protected Thread updater = null; 
    //worker thread designed to save image do HD
    protected Thread saveImg = null;
    
    public Vision(ConsoleLayout console) {
        super(console);
        //clears all the unused initializations of the standard ConsolePanel
        removeAll();
        //Mouse click
        addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1){
                    int mouseX = (int) ((e.getX() - 13)/xScale);  //shift window bar
                    int mouseY = (int) ((e.getY() - 10)/yScale) ; //shift window bar
                    if(isRunning && !ipCam)
                        if (mouseX >= 0 && mouseY >= 0 && mouseX <= widthImgRec && mouseY <= heightImgRec )
                            out.printf("%d#%d;\0", mouseX,mouseY);
                    
                    System.out.println(getMainVehicleId()+"X = " +mouseX+ " Y = " +mouseY);
                    captureFrame = true;
                }
            }
            @Override
            public void mouseReleased(MouseEvent e) {
            }
            @Override
            public void mouseEntered(MouseEvent e) {
            }
            @Override
            public void mouseExited(MouseEvent e) {
            }
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    popup = new JPopupMenu();
                    popup.add("Start Connection").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            isRunning = true;
                            ipCam = false;
                        }
                    });
                    popup.add("Close Connection").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            isRunning = false;
                            state = false;
                            ipCam = false;
                        }
                    });
                    popup.add("Menu/Config").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            //show_menu = !show_menu;
                            menu.setVisible(true);
                        }
                    });
                    popup.add("Start IP-CAM").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            ipCam = true;
                            isRunning = false;
                            state = false;
                        }
                    });
                    popup.show((Component) e.getSource(), e.getX(), e.getY());
                }
            }
        });
        return;
    }
    
    //!Print Image to JPanel
    public void showImage(BufferedImage image) {
        picLabel.setIcon(new ImageIcon(image));
        panelImage.add(picLabel);
        repaint();
    }
    
    //!Config Layout
    public void layout_user(){
        //Create Buffer (type MAT) for Image resize
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        matResize = new Mat(720, 960, CvType.CV_8UC3);
        
        //!Create folder to save image data
        //Create folder image in log if don't exist
        File dir = new File(String.format("log/image"));
        dir.mkdir();
        //Create folder Image to save data received
        dir = new File(String.format("log/image/%s",date));
        dir.mkdir();
        //Create folder Image Tag
        dir = new File(String.format("log/image/%s/image_tag",date));
        dir.mkdir();
        //Create folder Image Save
        dir = new File(String.format("log/image/%s/image_save",date));
        dir.mkdir();
        logDir = String.format("log/image/%s",date);
        
        //JLabel for image
        picLabel = new JLabel();
        //JPanel for Image
        panelImage = new JPanel();
        panelImage.setBackground(Color.black);
        frame = new JPanel();
        frame.setLayout(new BorderLayout());
        frame.add(panelImage, BorderLayout.WEST);
        
        this.setLayout(new MigLayout());
        this.add(frame);
        
        //JPanel for info and config values      
        config = new JPanel(new MigLayout());

        //Tpl JComboBox
        String[] sizeStrings = { "TPL Size", "25", "50", "75", "100", "150"};
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final JComboBox tplList = new JComboBox(sizeStrings);
        tplList.setSelectedIndex(0);
        tplList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String stringValue = (String) tplList.getSelectedItem();
                String check = "TPL Size";
                if (stringValue != check ){
                int value = Integer.parseInt(stringValue);
                out.printf("-1#%d;\0", value);
                }
              }
        });
        config.add(tplList,"width 160:180:200, h 30!");
                
        //Window JComboBox
        String[] sizeStrings2 = { "Window Size", "30", "55", "80", "105", "155"};
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final JComboBox windowList = new JComboBox(sizeStrings2);
        windowList.setSelectedIndex(0);
        windowList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String stringValue = (String) windowList.getSelectedItem();
                String check = "Window Size";
                if (stringValue != check ){
                int value = Integer.parseInt(stringValue);
                out.printf("-2#%d;\0", value);
                }
              }
        });
        config.add(windowList,"width 160:180:200, h 30!, wrap");
            
        //JButton Save snapshot
        JButton buttonS = new JButton();
        buttonS.setText("Snapshot");
        buttonS.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.printf("-3#123;\0");
              }
        });
        config.add(buttonS, "width 160:180:200, h 40!");
                
        //JButton Save Video
        JButton buttonV = new JButton();
        buttonV.setText("Save/Stop video");
        buttonV.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.printf("-4#123;\0");
            }
        });
        config.add(buttonV,"width 160:180:200, h 40!, wrap");
        
        //JText info Data received
        txtText = new JTextField();
        txtText.setEditable(false);
        txtText.setToolTipText("Info of Frame received from DUNE.");
        info = String.format("X = 0 - Y = 0   x 1   0 bytes (KiB = 0)\t\t  ");
        txtText.setText(info);
        config.add(txtText, "cell 0 4 3 1, wrap");
        
        //JText info Data GPS received TCP
        txtDataTcp = new JTextField();
        txtDataTcp.setEditable(false);
        txtDataTcp.setToolTipText("Info of GPS received from DUNE (TCP).");
        info = String.format("\t\t\t\t  ");
        txtDataTcp.setText(info);
        config.add(txtDataTcp, "cell 0 5 3 1, wrap");
        
        //JText info
        txtData = new JTextField();
        txtData.setEditable(false);
        txtData.setToolTipText("Info of Frame received from DUNE.");
        info = String.format("\t\t\t\t  ");
        txtData.setText(info);
        config.add(txtData, "cell 0 6 3 1, wrap");
        
        //Vehicle JComboBox
  /*      String[] vehicleStrings = { "Vehicle", "x8-03", "mariner-01","aero-01"};
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final JComboBox vehicleList = new JComboBox(vehicleStrings);
        vehicleList.setSelectedIndex(0);
        vehicleList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String stringValue = (String) vehicleList.getSelectedItem();
                String check = "TPL Size";
                if (stringValue != check ){
                    if (stringValue == "x8-03")
                        idVehicle = 3078;
                    if (stringValue == "mariner-01")
                        idVehicle = 3081;
                    if (stringValue == "aero-01")
                        idVehicle = 3080;
                }
              }
        });
        config.add(vehicleList,"width 160:180:200, h 30!");*/
        
        menu = new JFrame("Menu_Config");
        menu.setVisible(show_menu);
        menu.setResizable(false);
        menu.setSize(400, 350);
        menu.add(config);
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
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
    }
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        ImcMsgManager.getManager().addListener(this);
        layout_user();
        updater = updaterThread();
        updater.start();
        saveImg = updaterThreadSave();
        saveImg.start();
    }
    
    //Get size of image
    public void initSizeImage(){
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
        xScale = (float)960/widthImgRec;
        yScale = (float)720/heightImgRec;
        //Create Buffer (type MAT) for Image receive
        mat = new Mat(heightImgRec, widthImgRec, CvType.CV_8UC3);
    }
    
    //!Thread to handle data receive
    private Thread updaterThread() {
        Thread ret = new Thread("Vision Thread") {
            @Override
            public void run() {
                while(true){
                    if (isRunning && !ipCam ) {
                        if (state == false){
                            //connection
                            tcpConnection();
                            //receive info of image size
                            initSizeImage();
                            state = true;
                        }
                        //receive data image
                        receivedDataImage();
                        if(!isRunning && !state){
                            try {
                                is.close();
                            }
                            catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            try {
                                in.close();
                            }
                            catch (IOException e1) {
                                e1.printStackTrace();
                            }
                            out.close();
                        }
                    }
                    else if (!isRunning && ipCam) {
                        if (state == false){
                            xScale = (float)960/240;
                            yScale = (float)720/180;
                            //Create Buffer (type MAT) for Image receive
                            mat = new Mat(heightImgRec, widthImgRec, CvType.CV_8UC3);
                            state = true;
                            capture = new VideoCapture("rtsp://10.0.20.102:554/axis-media/media.amp?streamprofile=Mobile");
                            cntTag = 1;
                            if (capture.isOpened())
                                System.out.println("Video is captured");
                            else
                                System.out.println("Video is not captured");
                        }
                        //TODO: Cap ip cam
                        else if(!isRunning && ipCam && state) {
                            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
                            capture.grab();
                            capture.read(mat);
                            Imgproc.resize(mat, matResize, size);       
                            //Convert Mat to BufferedImage
                            temp=matToBufferedImage(matResize);
                            showImage(temp);
                            
                            if( captureFrame ) {
                                String imageTag = String.format("%s/image_tag/(%d)_%s.jpeg",logDir,cntTag,info);
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
                    else
                        inicImage();
                }
            }
        };
        return ret;
    }

    //!Thread to handle save image
    private Thread updaterThreadSave() {
        Thread si = new Thread("Save Image") {
            @Override
            public void run() {
                while(true){
                    if (isRunning ) {
                        if(flagBuffImg == true){
                            flagBuffImg = false;
                            long startTime = System.currentTimeMillis();
                            String imageJpeg = String.format("%s/%d.jpeg",logDir,cnt);
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
                        else{
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }     
                    }
                    else{
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    }

               }
           }
        };
        return si;
    }
    
    //!IMC handle
    @Subscribe
    public void consume(EstimatedState msg) {   
        System.out.println("Source Name "+msg.getSourceName()+"ID "+getMainVehicleId());
        if(msg.getSourceName().equals(getMainVehicleId())){
            try {
                //! update the position of target
                //LAT and LON rad
                double latRad = msg.getLat();
                double lonRad = msg.getLon();
                //LAT and LON deg
                double latDeg = latRad*(180/Math.PI);
                double lonDeg = lonRad*(180/Math.PI);
                //Offset (m)
                double offsetN = msg.getX();
                double offsetE = msg.getY();
                //Lat and Lon final
                double lat = latDeg + (180/Math.PI)*(offsetE/6378137);
                double lon = lonDeg + (180/Math.PI)*(offsetN/6378137)/Math.cos(latDeg);
                //height of Vehicle 
                double heightV = msg.getHeight();
                //orienation
                double orientationRad = msg.getPsi();

                double camTiltDeg = 45.0f;//this value may be in configuration
                calcTagPosition(lat, lon, heightV, Math.toDegrees(orientationRad), camTiltDeg);
                info = String.format("(IMC) LAT: %f # LON: %f # ALT: %.2f m", lat, lon, heightV);

                txtData.setText(info);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void calcTagPosition(double lat, double lon, double heightV, double orientationDegrees, double camTiltDeg){
        double dist = Math.tan(camTiltDeg)*heightV;// hypotenuse
        double x = Math.sin(orientationDegrees)*dist;//oposite side
        double y = Math.cos(orientationDegrees)*dist;// adjacent side
        double[] finalPointArray = WGS84Utilities.WGS84displace(lat,lon,0,x,y,0);// final central camera view point in lat and lon
        System.out.println("lat: "+finalPointArray[0]+", lon: "+finalPointArray[1]+", depth: "+finalPointArray[2]);
        //create a message with finalPointArray[0,1,2]
    }
    
    @Subscribe
    public void consume(Announce announce) {
        //System.out.println("Announce: "+announce.getSysName()+"  ID: "+announce.getSrc());
        //System.out.println("RECEIVED ANNOUNCE"+announce);
    }
    
    //!Fill cv::Mat image with zeros
    public void inicImage(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Scalar black = new Scalar(0);
        matResize.setTo(black);
        temp=matToBufferedImage(matResize);
        showImage(temp);
    }
    
    //!Received data Image
    public void receivedDataImage(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        try {
            line = in.readLine();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        
        if (line == null){
            //custom title, error icon
            JOptionPane.showMessageDialog(frame, "Lost connection with Vehicle...", "Connection error", JOptionPane.ERROR_MESSAGE);
            isRunning = false;
            state = false;
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
            @SuppressWarnings("unused")
            ServerSocket serverSocket = null;    
            try { 
                serverSocket = new ServerSocket(2424); 
            } 
            catch (IOException e){ 
            }    
        }
        else{        
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
                }
                if (readBytes < 0) {
                    System.err.println("stream ended");
                    try {
                        is.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    } 
                    try {
                        clientSocket.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    } 
                    try {
                        serverSocket.close();
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    } 
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
            // Decompress the data
            byte[] buf = new byte[(widthImgRec*heightImgRec*3)];
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
            //System.out.println("Original Size = " + decompressedData.length +" bytes");
            
            //Transform byte data to cv::Mat (for display image)
            //mat = new Mat(heightImgRec, widthImgRec, CvType.CV_8UC3);
            mat.put(0, 0, decompressedData);
            //Resize image to 960x720 resolution
            //matResize = new Mat(960, 720, CvType.CV_8UC3);
            Imgproc.resize(mat, matResize, size);
                       
            //Convert Mat to BufferedImage
            temp=matToBufferedImage(matResize);
            
            //TODO: CHANGE TO TRUE FOR END DEBUG (SAVE IMAGE TO DISK)
            flagBuffImg = false;      
            
            //Display image in JFrame
            info = String.format("X = %d - Y = %d   x %.2f   %d bytes (KiB = %d)", widthImgRec, heightImgRec,xScale,lengthImage,lengthImage/1024);
            txtText.setText(info);
            txtDataTcp.setText(duneGps);
            showImage(temp);
        }
    }
    
    //!Create Socket service
    public void tcpConnection(){
        //Socket Config
        ServerSocket serverSocket = null;    
        try { 
            serverSocket = new ServerSocket(2424); 
        } 
        catch (IOException e) 
        { 
            System.err.println("Could not listen on port: "+ serverSocket); 
            System.exit(1); 
        } 
        Socket clientSocket = null; 
        System.out.println ("Waiting for connection.....");
        try { 
            clientSocket = serverSocket.accept(); 
        } 
        catch (IOException e) 
        { 
            System.err.println("Accept failed."); 
            System.exit(1); 
        } 
        System.out.println ("Connection successful from Server: "+clientSocket.getInetAddress()+":"+serverSocket.getLocalPort());
        System.out.println ("Receiving data image.....");
        
        //Send data for sync 
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }

        //Buffer for data image
        try {
            is = clientSocket.getInputStream();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        //Buffer for info of data image
        in = new BufferedReader( new InputStreamReader( is ));
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
}
