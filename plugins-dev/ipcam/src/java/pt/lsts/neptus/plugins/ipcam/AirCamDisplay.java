/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: João Fortuna
 * Dec 5, 2012
 */
package pt.lsts.neptus.plugins.ipcam;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;

import com.xuggle.mediatool.IMediaListener;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IError;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.util.conf.StringPatternValidator;

enum Status{
    OFF,INIT,CONN,STOP,RCON;
}

/**
 * Neptus panel designed to allow operator with viewer access to on-board IP camera's video stream, through RTSP protocol.
 * 
 * @author canastaman
 * @version 2.2
 * @category CameraPanel 
 *
 */
@Popup( pos = POSITION.RIGHT, width=640, height=400)
@LayerPriority(priority=0)
@PluginDescription(name="AirCam Display", version="2.2", author="Sergio Ferreira", description="Video displayer for IP Cameras", icon="pt/lsts/neptus/plugins/ipcam/camera.png")
public class AirCamDisplay extends ConsolePanel implements ConfigurationListener, ItemListener{

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name="Camera IP", description="The IP address of the camera you want to display",  userLevel = LEVEL.REGULAR)
    public String ip = "10.0.20.209";
    
    @NeptusProperty(name="Camera Brand", description="Brand for the installed camera (not case sensitive)",  userLevel = LEVEL.REGULAR)
    public String brand = "ubiquiti";
    
    @NeptusProperty(name="Camera Alias", description="Camera's network alias camera (not case sensitive)",  userLevel = LEVEL.REGULAR)
    public String alias = "camera-03";
    
    //small test with private camera element to emulate vehicle-def file loading data
    private Camera activeCamera;
    
    //small listener which allows the user to quickly tap into the panel settings
    private MouseAdapter mouseListener;

    //represents the current state of connection between the display panel and the camera
    private Status status = Status.OFF;
        
    //worker thread designed to acquire the data packet from the online camera
    protected Thread updater = null;

    //collection of 'detected' cameras on the network 
    protected LinkedHashMap<String,Camera> cameraList;
    
    private JComboBox<String> choicelist = null;
    
    private JTextField ipField = null;
    private JTextField positionField = null;
    private JTextField zoomField = null;
    
    private JPanel propertiesPanel = null;
    private JPanel controlPanel = null; 
    
    private ImagePanel imagePanel = null;
    
    private JToggleButton recordButton = null;
    private JToggleButton connectButton = null;
    
    public AirCamDisplay(ConsoleLayout console) {
        super(console);

        // clears all the unused initializations of the standard ConsolePanel
        removeAll();
    }
    
    /**
     * In order to validate the IP address in the {@link #ip} NeptusProperty.
     * @param value
     * @return
     */
    public static String validateIP(String value) {
        return new StringPatternValidator("\\d{2,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}").validate(value);
    }

    //Listener
    private void setMouseListener() {
        
        mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                
                if (e.getButton() == MouseEvent.BUTTON3) {
                    
                    JPopupMenu popup = new JPopupMenu();

                    popup.add(I18n.text("Settings")).addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PropertiesEditor.editProperties(AirCamDisplay.this, getConsole(), true);
                        }
                    });
                    
                    popup.show((Component)e.getSource(), e.getX(), e.getY());
                }
            }
        };        
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 400);
    }

    public void establishConnection() {
        NeptusLog.pub().info(this.getClass().getSimpleName()+" attemptig to establish connection with "+ip);
         
        if(status == Status.STOP || status == Status.OFF) {
            status = Status.INIT;
            updater = updaterThread();
            updater.start();
        }      
        else
            status = Status.RCON;
    }
    
    private Thread updaterThread() {
        
        Thread ret = new Thread("RTSP Worker Thread") {

            boolean isRunning = true;
            boolean isRecording = false;
            String path = null;
            String recordPath = "./log/";
            Calendar date = Calendar.getInstance();
            IMediaReader mediaReader;
            IMediaWriter mediaWriter;
            
            private IMediaListener mediaListener = new MediaListenerAdapter() {
                @Override
                public void onVideoPicture(IVideoPictureEvent event) {
                    try {
                        
                        imagePanel.setImage(event.getImage());
                        
                        if(recordButton.isSelected()){
                            
                            if(!isRecording){
                                //initializes the writer responsible for recording
                                date.setTimeInMillis(System.currentTimeMillis());                               
                                mediaWriter = ToolFactory.makeWriter(recordPath+date.getTime()+"_"+activeCamera.alias+".mp4");
                                mediaWriter.addVideoStream(0, 0, ICodec.ID.CODEC_ID_H264,event.getImage().getWidth(),event.getImage().getHeight());
                                isRecording = true;
                            }
                            
                            mediaWriter.encodeVideo(0,event.getImage(),System.currentTimeMillis(),TimeUnit.MILLISECONDS);
                        }
                        else{
                            
                            if(isRecording)
                                mediaWriter.close();
                            
                            isRecording = recordButton.isSelected();
                        }
                        
                        repaint();
                    }
                    catch (Exception ex) {                        
                        status = Status.STOP;
                        NeptusLog.pub().error(ex);
                        NeptusLog.pub().warn("Verify camera settings before attempting to connect"); 
                    }
                }
            };
            
            @Override
            public void run() {
                while (isRunning) {
                    //ensures only one thread is launched
                    if (updater == this) {
                        if (status != Status.STOP) {
                            if (status == Status.INIT) {
                                if (activeCamera.brand.equalsIgnoreCase("axis")) {
                                    path = "rtsp://"+activeCamera.ip+"/axis-media/media.amp";
                                }
                                else if (activeCamera.brand.equalsIgnoreCase("ubiquiti")) {
                                    path = "rtsp://"+activeCamera.ip+":554/live/ch00_0";
                                }
                                                                
                                //initializes the reader responsible for the rtsp data input
                                mediaReader = ToolFactory.makeReader(path);
                                mediaReader.setBufferedImageTypeToGenerate(BufferedImage.TYPE_3BYTE_BGR);
                                mediaReader.setQueryMetaData(false);
                                mediaReader.addListener(mediaListener);
                                
                                status = Status.CONN;
                                
                                try {
                                    mediaReader.open();
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e);
                                    NeptusLog.pub().warn("Verify camera settings before attempting to connect"); 
                                    status = Status.STOP;
                                }                                
                            }
                            else if (status == Status.RCON) {
                                mediaReader.removeListener(mediaListener);
                                mediaReader.close();
                                status = Status.INIT;
                            }
                            
                            if (status == Status.CONN) {
                                IError err = null;
                                if (mediaReader != null)
                                    err = mediaReader.readPacket();
                        
                                if (err != null) {                                    
                                    NeptusLog.pub().error(err);
                                    NeptusLog.pub().warn("Verify camera settings before attempting to connect");                                    
                                    status = Status.STOP;
                                }
                            }
                        }
                        else
                            isRunning = false;
                    }
                    else
                        isRunning = false;
                }
                status = Status.STOP;
                
                //in case of internal thread termination
                connectButton.setSelected(false);
                recordButton.setSelected(false);
                recordButton.setEnabled(false);
                              
                if(isRecording)
                    mediaWriter.close();
                
                NeptusLog.pub().info(this.getName() + " exiting");
            }
        };

        return ret;
    }

    @Override
    public void cleanSubPanel() {
        status = Status.STOP;
    }

    @Override
    public void propertiesChanged() {
        
        if(status != Status.OFF){
            
            Camera newCam = new Camera(ip,alias,"Nose",brand,52.0,1.0);
            
            if(cameraList.get(alias)!= null) {
                choicelist.addItem(alias);
            }
                      
            cameraList.put(alias, newCam);
        }
    }

    @Override
    public void initSubPanel() {
        setMouseListener();
        addMouseListener(mouseListener);
        cameraList = new LinkedHashMap<String,Camera>();
        
        imagePanel = new ImagePanel();
        choicelist = new JComboBox<String>();
        ipField = new JTextField();
        positionField = new JTextField();
        zoomField = new JTextField();
        recordButton = new JToggleButton();
        connectButton = new JToggleButton();
        
        //gather up camera information
        loadCameraList();        
        
        propertiesPanelSetup();        
        
        controlPanelSetup();
        
        recordButton.addItemListener(this);
        connectButton.addItemListener(this);
        choicelist.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    
                    //gathers new camera to connect
                    activeCamera = cameraList.get(choicelist.getSelectedItem());
                    
                    ipField.setText(activeCamera.ip);
                    positionField.setText(activeCamera.position);
                    zoomField.setText(Double.toString(activeCamera.zoom));
                    
                    if(status != Status.STOP && status != Status.OFF) {                        
                        establishConnection();
                    }
                }
            }
        );
        
        //panel general layout setup
        this.setLayout(new MigLayout());
        this.add(imagePanel,"w 100%, h 100%, span 1 2, growy");
        this.add(propertiesPanel,"w 100px!, h 100%, wrap");
        this.add(controlPanel,"w 100px!, h 100px!");
    }

    /**
     * 
     */
    private void controlPanelSetup() {
        controlPanel = new JPanel(new MigLayout());
        controlPanel.add(new JLabel(I18n.text("Connection"),SwingConstants.CENTER), "w 100%, wrap");
            connectButton.setText(I18n.text("Off"));
            connectButton.setBackground(Color.red.darker());
            connectButton.setHorizontalAlignment(JButton.CENTER);
        controlPanel.add(connectButton, "w 100%, wrap");
        controlPanel.add(new JLabel(I18n.text("Record Stream"),SwingConstants.CENTER), "w 100%, wrap");
            recordButton.setText(I18n.text("Off"));
            recordButton.setBackground(Color.red.darker());
            recordButton.setHorizontalAlignment(JButton.CENTER);
            recordButton.setEnabled(false);
        controlPanel.add(recordButton, "w 100%, wrap");
    }

    /**
     * 
     */
    private void propertiesPanelSetup() {
        propertiesPanel = new JPanel(new MigLayout());
        propertiesPanel.add(new JLabel(I18n.text("Camera Alias"),SwingConstants.CENTER), "w 100%, wrap");
        propertiesPanel.add(choicelist, "w 100%, wrap");
        propertiesPanel.add(new JLabel(I18n.text("IP Address"),SwingConstants.CENTER), "w 100%, wrap");
            ipField.setText(activeCamera.ip);
            ipField.setHorizontalAlignment(JTextField.CENTER);
            ipField.setEditable(false);
        propertiesPanel.add(ipField, "w 100%, wrap");
        propertiesPanel.add(new JLabel(I18n.text("Position"),SwingConstants.CENTER), "w 100%, wrap");
            positionField.setText(activeCamera.position);
            positionField.setHorizontalAlignment(JTextField.CENTER);
            positionField.setEditable(false);
        propertiesPanel.add(positionField, "w 100%, wrap");
        propertiesPanel.add(new JLabel(I18n.text("Zoom"),SwingConstants.CENTER), "w 100%, wrap");
            zoomField.setText(Double.toString(activeCamera.zoom));
            zoomField.setHorizontalAlignment(JTextField.CENTER);
            zoomField.setEditable(false);
        propertiesPanel.add(zoomField, "w 100%, wrap");
    }

    /**
     * Method responsible for the collection of detected cameras
     */
    private void loadCameraList() {
        activeCamera = new Camera("10.0.20.209","camera-03","Nose","Ubiquiti",52.0,1.0);
        cameraList.put("camera-04", new Camera("10.0.20.97","camera-04","Nose","Ubiquiti",52.0,1.0));
        cameraList.put("camera-03", activeCamera);
        cameraList.put("camera-08", new Camera("10.0.20.199","camera-08","Nose","Axis",52.0,1.0)); 
        
        for (Camera cam : cameraList.values()) {
            choicelist.addItem(cam.alias);
        }
        choicelist.setSelectedIndex(0);
    }

    /* (non-Javadoc)
     * @see java.awt.event.ItemListener#itemStateChanged(java.awt.event.ItemEvent)
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
    
        JToggleButton input = (JToggleButton) e.getSource();
        
        if(input.equals(connectButton)) {
            if(e.getStateChange() == ItemEvent.SELECTED){
                
                //gathers new camera to connect
                activeCamera = cameraList.get(choicelist.getSelectedItem());
                           
                //changes appearance
                connectButton.setText(I18n.text("On"));
                ipField.setText(activeCamera.ip);
                positionField.setText(activeCamera.position);
                zoomField.setText(Double.toString(activeCamera.zoom));
                
                establishConnection();
                
                //enables recording
                recordButton.setEnabled(true);
            }
            else{
                status = Status.STOP;
                
                //changes appearance
                connectButton.setText(I18n.text("Off"));
                
                //disable recording
                recordButton.setSelected(false);
                recordButton.setEnabled(false);
            }
        }
        else if(input.equals(recordButton)) {
            if(e.getStateChange() == ItemEvent.SELECTED){
                NeptusLog.pub().info("Starting recording");
                
                //changes appearance
                recordButton.setText(I18n.text("On"));
            }
            else{
                NeptusLog.pub().info("Stopping recording");
                
                //changes appearance
                recordButton.setText(I18n.text("Off")); 
            }
        }       
    }
}

/**
 * Auxiliary class that emulates the existence of a camera Database, if camera data was loaded from external files this class would become obsolete
 */
class Camera
{
    /*
     * Some of the data will be placeholder to evaluate operator receptivity
     */
    public String ip; 
    public String alias;  
    public String position; 
    public String brand;
    public double fov;
    public double zoom;
    
    public Camera(String ip, String alias, String position, String brand, double d, double e) {
        this.ip = ip;
        this.alias = alias;
        this.position = position;
        this.brand = brand;
        this.fov = d;
        this.zoom = e;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return alias;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) 
            return false;
        if (obj == this) 
            return true;
        if (!(obj instanceof Camera))
            return false;
        
        Camera newCamera = (Camera)obj;
        
        if (this.ip.equals(newCamera.ip))
            return true;
        else
            return false;
    }
 };