/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Author: João Fortuna
 * Dec 5, 2012
 */
package pt.lsts.neptus.plugins.ipcam;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;

import javax.swing.JPopupMenu;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.renderer2d.LayerPriority;
import pt.lsts.neptus.util.GuiUtils;

import com.xuggle.mediatool.IMediaListener;
import com.xuggle.mediatool.IMediaReader;
import com.xuggle.mediatool.MediaListenerAdapter;
import com.xuggle.mediatool.ToolFactory;
import com.xuggle.mediatool.event.IVideoPictureEvent;
import com.xuggle.xuggler.IError;

enum Status{
    INIT,CONN,STOP,RCON;
}

/**
 * Neptus panel designed to allow operator with viewer access to on-board IP camera's video stream, through RTSP protocol.
 * 
 * @author jfortuna
 * @author canastaman
 * @version 2.1
 * @category CameraPanel 
 *
 */
@Popup( pos = POSITION.RIGHT, width=640, height=400)
@LayerPriority(priority=0)
@PluginDescription(name="AirCam Display", version="2.1", author="Sergio Ferreira", description="Video displayer for IP Cameras", icon="pt/lsts/neptus/plugins/ipcam/camera.png")
public class AirCamDisplay extends ConsolePanel implements ConfigurationListener, ComponentListener{

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name="Camera IP", description="The IP address of the camera you want to display")
    public String ip = "10.0.20.209";
    
    @NeptusProperty(name="Camera Brand", description="Brand for the installed camera (not case sensitive)")
    public String brand = "ubiquiti";
    
    //small listener which allows the user to quickly tap into the panel settings
    private MouseAdapter mouseListener;

    //represents the current state of connection between the display panel and the camera
    private Status status = Status.INIT;
    
    //image size factor for resizing purposes
    private double factor;
    
    protected BufferedImage imageToDisplay = null;
    
    //worker thread designed to acquire the data packet from the online camera
    protected Thread updater = null;
    
    //Listener
    private void setMouseListener(){
        
        mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                
                if (e.getButton() == MouseEvent.BUTTON3) {
                    
                    JPopupMenu popup = new JPopupMenu();
                    
                    popup.add("Reconnect").addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            reconnect();
                        }
                    });

                    popup.add("Settings").addActionListener(new ActionListener() {

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

    public AirCamDisplay(ConsoleLayout console) {
        super(console);

        // clears all the unused initializations of the standard ConsolePanel
        removeAll();
    }
    
    @Override
    public void paint(Graphics g) {
        
        //backdrop reset
        g.setColor(Color.black);
        g.fillRect(0, 0, getWidth(), getHeight());
        
        if (imageToDisplay != null) {
            ((Graphics2D)g).scale(factor,factor);
            g.drawImage(imageToDisplay,
                    (int) ((getWidth() - factor *  imageToDisplay.getWidth())  / (factor * 2)),
                    (int) ((getHeight() - factor * imageToDisplay.getHeight())  / (factor * 2)),
                    null);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 400);
    }

    public void reconnect() {
        NeptusLog.pub().info(this.getClass().getSimpleName()+" attemptig to reconnect to "+ip);
        
        if(status == Status.STOP) {
            status = Status.INIT;
            updater = updaterThread();
            updater.setPriority(Thread.MIN_PRIORITY);
            updater.start();
        }      
        else
            status = Status.RCON;
    }
    
    private Thread updaterThread() {
        // Could't we also initialize and start it here only if needed?
        Thread ret = new Thread("RTSP Worker Thread") {

            boolean isRunning = true;
            String path = null;
            IMediaReader mediaReader;
            
            private IMediaListener mediaListener = new MediaListenerAdapter() {
                @Override
                public void onVideoPicture(IVideoPictureEvent event) {
                    try {
                        imageToDisplay = event.getImage();
                        repaint();
                    }
                    catch (Exception ex) {
                        status = Status.STOP;
                        NeptusLog.pub().error(ex);
                        NeptusLog.pub().warn("Verify camera settings before attempting to reconnect"); 
                    }
                }
            };
            
            @Override
            public void run() {
                while (isRunning) {
                    //ensures only one thread is launched
                    if (updater == this) { // pdias: Why even start a thread if is not going to be stop?
                        if (status != Status.STOP) {
                            if (status == Status.INIT) {
                                if (brand.equalsIgnoreCase("axis")) {
                                    path = "rtsp://"+ip+"/axis-media/media.amp";
                                }
                                else if (brand.equalsIgnoreCase("ubiquiti")) {
                                    path = "rtsp://"+ip+":554/live/ch00_0";
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
                                    NeptusLog.pub().warn("Verify camera settings before attempting to reconnect"); 
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
                                    NeptusLog.pub().warn("Verify camera settings before attempting to reconnect");                                    
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
                imageToDisplay = null;
                status = Status.STOP;
                NeptusLog.pub().info(this.getName() + " exiting");
            }
        };
        // ret.setPriority(Thread.MIN_PRIORITY); // pdias: Why not set this here?
        // ret.start();

        return ret;
    }

    @Override
    public void cleanSubPanel() {
        status = Status.STOP;
    }

    @Override
    public void propertiesChanged() {
        if (status != Status.INIT)
            reconnect();
    }

    @Override
    public void initSubPanel() {
        setMouseListener();
        addMouseListener(mouseListener);
        addComponentListener(this);
                       
        //initialize video thread
        updater = updaterThread();
        updater.setPriority(Thread.MIN_PRIORITY);
        updater.start();
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentResized(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentResized(ComponentEvent e) {
        System.out.println("resized");
        if (imageToDisplay!=null) {
            double factorw = (double) getWidth() / imageToDisplay.getWidth();
            double factorh = (double) getHeight() / imageToDisplay.getHeight();
            factor = (factorw < factorh ? factorw : factorh);
        }        
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentMoved(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentMoved(ComponentEvent e) {
        // TODO Auto-generated method stub
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentShown(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentShown(ComponentEvent e) {
        System.out.println("shown");
    }

    /* (non-Javadoc)
     * @see java.awt.event.ComponentListener#componentHidden(java.awt.event.ComponentEvent)
     */
    @Override
    public void componentHidden(ComponentEvent e) {
        System.out.println("hidden");
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        final AirCamDisplay display = new AirCamDisplay(null);
        GuiUtils.testFrame(display, "Camera Display");
    }
}