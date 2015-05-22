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
    //Width size of image
    int width;
    //Height size of image
    int height;
    //Scale factor of x
    float x_scale;
    //Scale factor of y
    float y_scale;
    //read size of pack compress
    String line;
    //Size of image received
    int length_image;
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
    //Label for image
    JLabel picLabel;
    //Panel for Image
    JPanel frame;
    //Panel for display image
    JPanel panel_image;
    //Panel for info and config values
    JPanel config;
    //Text info of data receive
    JTextField txtText;
    //Text of data receive over IMC message
    JTextField txtData;
    //JFrame for menu options
    JFrame menu;
    //Buffer for the info treatment 
    String info;
    //Popup Menu
    JPopupMenu popup;

    //*** TEST FOR SAVE VIDEO **/
    File outputfile;
    boolean flag_buff_img = false;
    int cnt=0;
    int FPS = 12;
    //*************************/
    
    //IMC message
    EstimatedState msg;
    protected LinkedHashMap<String, EstimatedState> msgsSetLeds = new LinkedHashMap<>(); 
    
    //worker thread designed to acquire the data packet from DUNE
    protected Thread updater = null; 
  //worker thread designed to save image do HD
    protected Thread save_img = null;
    
    public Vision(ConsoleLayout console) {
        super(console);
        //clears all the unused initializations of the standard ConsolePanel
        removeAll();
        //Mouse click
        addMouseListener(new MouseListener() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1){
                    int mouse_x = (int) ((e.getX() - 13)/x_scale);  //shift window bar
                    int mouse_y = (int) ((e.getY() - 10)/y_scale) ; //shift window bar
                    if (mouse_x >= 0 && mouse_y >= 0 && mouse_x <= width && mouse_y <= height ){
                        out.printf("%d#%d;\0", mouse_x,mouse_y);
                    }
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
                        }
                    });
                    popup.add("Close Connection").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            isRunning = false;
                            state = false;
                        }
                    });
                    popup.add("Menu/Config").addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            //show_menu = !show_menu;
                            menu.setVisible(true);
                        }
                    });
                    popup.show((Component) e.getSource(), e.getX(), e.getY());
                }
            }
        });
        return;
    }
    
    //!Print Image to JPanel
    public void show_image(BufferedImage image){
        picLabel.setIcon(new ImageIcon(image));
        panel_image.add(picLabel);
        repaint();
    }
    
    //!Config Layout
    public void layout_user(){
        //Label for image
        picLabel = new JLabel();
        //Panel for Image
        panel_image = new JPanel();
        panel_image.setBackground(Color.black);
        frame = new JPanel();
        frame.setLayout(new BorderLayout());
        frame.add(panel_image, BorderLayout.WEST);
        
        this.setLayout(new MigLayout());
        this.add(frame);
        
        //Panel for info and config values      
        config = new JPanel(new MigLayout());

        //Tpl ComboBox
        String[] sizeStrings = { "TPL Size", "25", "50", "75", "100", "150"};
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final JComboBox tplList = new JComboBox(sizeStrings);
        tplList.setSelectedIndex(0);
        tplList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
              {
                String string_value = (String) tplList.getSelectedItem();
                String check = "TPL Size";
                if (string_value != check ){
                int value = Integer.parseInt(string_value);
                out.printf("-1#%d;\0", value);
                }
              }
        });
        config.add(tplList,"width 160:180:200, h 30!");
                
        //Window ComboBox
        String[] sizeStrings2 = { "Window Size", "30", "55", "80", "105", "155"};
        @SuppressWarnings({ "rawtypes", "unchecked" })
        final JComboBox windowList = new JComboBox(sizeStrings2);
        windowList.setSelectedIndex(0);
        windowList.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
              {
                String string_value = (String) windowList.getSelectedItem();
                String check = "Window Size";
                if (string_value != check ){
                int value = Integer.parseInt(string_value);
                out.printf("-2#%d;\0", value);
                }
              }
        });
        config.add(windowList,"width 160:180:200, h 30!, wrap");
            
        //Button Save snapshot
        JButton button_I = new JButton();
        button_I.setText("Snapshot");
        button_I.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.printf("-3#123;\0");
              }
        });
        config.add(button_I, "width 160:180:200, h 40!");
                
        //Button Save Video
        JButton button_V = new JButton();
        button_V.setText("Save/Stop video");
        button_V.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.printf("-4#123;\0");
              }
        });
        config.add(button_V,"width 160:180:200, h 40!, wrap");
        
        //Text info Data received
        txtText = new JTextField();
        txtText.setEditable(false);
        txtText.setToolTipText("Info of Frame received from DUNE.");
        info = String.format("X = 0 - Y = 0   x 1   0 bytes (KiB = 0)\t\t    ");
        txtText.setText(info);
        config.add(txtText, "cell 0 4 3 1, wrap");
        
        //Text info
        txtData = new JTextField();
        txtData.setEditable(false);
        txtData.setToolTipText("Info of Frame received from DUNE.");
        info = String.format("\t\t\t\t");
        txtData.setText(info);
        config.add(txtData, "cell 0 5 3 1, wrap");
        
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
        save_img = updaterThread2();
        save_img.start();
    }
    
    //Get size of image
    public void init_size_image(){
        //Width size of image
        try {
            width = Integer.parseInt(in.readLine());
        }
        catch (NumberFormatException | IOException e) {
            e.printStackTrace();
        }
        //Height size of image
        try {
            height = Integer.parseInt(in.readLine());
        }
        catch (NumberFormatException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        x_scale = (float)960/width;
        y_scale = (float)720/height;
        //System.out.printf("\n\nImage Size: %d x %d\n\n", width, height);
    }
    
    //!Thread to handle data receive
    private Thread updaterThread() {
        Thread ret = new Thread("Vision Thread") {
            @Override
            public void run() {
                while(true){
                    if (isRunning ) {
                        if (state == false){
                            //connection
                            tcp_connection();
                            //receive info of image size
                            init_size_image();
                            state = true;
                        }
                        //receive data image
                        received_data_image();
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
                    else
                        inic_image();
                }
            }
        };
        return ret;
    }

  //!Thread to handle save image
    private Thread updaterThread2() {
        Thread si = new Thread("Save Image") {
            @Override
            public void run() {
                while(true){
                    if (isRunning ) {
                        if(flag_buff_img == true){
                            flag_buff_img = false;
                            long startTime = System.currentTimeMillis();
                            String teste = String.format("/home/pedro/foto_java/%d.jpeg",cnt);
                            outputfile = new File(teste);
                            try {
                                ImageIO.write(temp, "jpeg", outputfile);
                            }
                            catch (IOException e) {
                                e.printStackTrace();
                            }
                            cnt++;
                            long stopTime = System.currentTimeMillis();
                            //long elapsedTime = stopTime - startTime;
                            while((stopTime - startTime) < (1000/FPS))
                            {
                                stopTime = System.currentTimeMillis();
                            }
                            long elapsedTime = stopTime - startTime;
                            System.out.println("\n"+elapsedTime);
                        }
                        else
                        {
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            }
                            catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }     
                    }
                    else
                    {
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
        if(msg.getSrc() == 3078){
            try {
                //! update the position of target
                //LAT and LON rad
                double lat_rad = msg.getLat();
                double lon_rad = msg.getLon();
                //LAT and LON deg
                double lat_deg = lat_rad*(180/Math.PI);
                double lon_deg = lon_rad*(180/Math.PI);
                //Offset (m)
                double offset_n = msg.getX();
                double offset_e = msg.getY();
                //Lat and Lon final
                double lat = lat_deg + (180/Math.PI)*(offset_e/6378137);
                double lon = lon_deg + (180/Math.PI)*(offset_n/6378137)/Math.cos(lat_deg);
                //height of Vehicle 
                double height_v = msg.getHeight();
                //System.out.println("SourceEntity: "+msg.getSrc());
                //System.out.printf("LAT: %f # LON: %f # rad\n",lat_rad, lon_rad);
                info = String.format("LAT: %f # LON: %f # Alt: %.2f m", lat, lon, height_v);
                txtData.setText(info);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @Subscribe
    public void consume(Announce announce) {
        //System.out.println("Announce: "+announce.getSysName()+"  ID: "+announce.getSrc());
        //System.out.println("RECEIVED ANNOUNCE"+announce);
    }
    
    //!Fill cv::Mat image with zeros
    public void inic_image(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat mat_resize = new Mat(720, 960, CvType.CV_8UC3);
        Scalar black = new Scalar(0);
        mat_resize.setTo(black);
        temp=matToBufferedImage(mat_resize);
        show_image(temp);
    }
    
    //!Received data Image
    public void received_data_image(){
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        try {
            line = in.readLine();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }
        
        if (line == null){
            //custom title, error icon
            JOptionPane.showMessageDialog(frame,
                "Lost connection with Vehicle...",
                "Connection error",
                JOptionPane.ERROR_MESSAGE);
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
            catch (IOException e) 
            { 
            }    
        }
        else{        
            length_image = Integer.parseInt(line);
            //buffer for save data receive
            data = new byte[length_image];
            //Send 1 for server for sync data send
            out.println("1\0");
            //read data image for buffer (ZP)
            int read = 0;
            while (read < length_image) {
                int read_bytes = 0;
                try {
                    read_bytes = is.read(data, read, length_image-read);
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                if (read_bytes < 0) {
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
                read += read_bytes;
            }
            
            //Decompress data received 
            Inflater decompresser = new Inflater(false);
            decompresser.setInput(data,0,length_image);
            //Create an expandable byte array to hold the decompressed data
            ByteArrayOutputStream bos = new ByteArrayOutputStream(data.length);
            // Decompress the data
            byte[] buf = new byte[(width*height*3)];
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
            Mat mat = new Mat(height, width, CvType.CV_8UC3);
            mat.put(0, 0, decompressedData);
            //Resize image to 960x720 resolution
            Mat mat_resize = new Mat(960, 720, CvType.CV_8UC3);
            Size size = new Size(960, 720);
            Imgproc.resize(mat, mat_resize, size);
                       
            //Convert Mat to BufferedImage
            temp=matToBufferedImage(mat_resize);
            
            //TODO: CHANGE TO TRUE FOR END DEBUG (SAVE IMAGE TO DISK)
            flag_buff_img = false;      
            
            //Display image in JFrame
            info = String.format("X = %d - Y = %d   x %.2f   %d bytes (KiB = %d)", width, height,x_scale,length_image,length_image/1024);
            txtText.setText(info);
            show_image(temp);
        }
    }
    
    //!Create Socket service
    public void tcp_connection(){
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
