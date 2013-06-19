/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
package pt.up.fe.dceg.neptus.plugins.ipcam;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.commons.httpclient.HttpClient;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.planeditor.IEditorMenuExtension;
import pt.up.fe.dceg.neptus.planeditor.IMapPopup;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.uavs.UavLib;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.LayerPriority;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.VideoCreator;

import com.l2fprod.common.propertysheet.DefaultProperty;

/**
 * @author jfortuna
 *
 */
@Popup( pos = POSITION.RIGHT, width=640/2, height=368/2)
@LayerPriority(priority=0)
@PluginDescription(name="AirCam Display", author="JFortuna", description="Video display for Ubiquiti Cameras", icon="pt/up/fe/dceg/neptus/plugins/ipcam/camera.png")
public class AirCamDisplay extends SimpleSubPanel implements ConfigurationListener {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name="Camera IP", description="The IP address of the camera you want to display")
    public String ip = "10.0.20.209";

    @NeptusProperty(name="Milliseconds between refresh")
    public long millisBetweenRefresh = 500;

    protected BufferedImage imageToDisplay = null;
    protected boolean connected = true;
    protected Thread updater = null;
    protected String status = "initializing";

    public AirCamDisplay(ConsoleLayout console) {
        super(console);
        removeAll();

        status = "initializing...";
        updater = updaterThread();
        updater.setPriority(Thread.MIN_PRIORITY);
        updater.start();

        addMouseListener(new MouseAdapter() {
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

                    popup.add("Camera settings").addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            PropertiesEditor.editProperties(AirCamDisplay.this, getConsole(), true);
                        }
                    });
                    popup.show((Component)e.getSource(), e.getX(), e.getY());
                }

            }
        });
    }
    
    @Override
    public void paint(Graphics g) {
        if (imageToDisplay != null) {
            double factorw = (double) getWidth() / imageToDisplay.getWidth();
            double factorh = (double) getHeight() / imageToDisplay.getHeight();
            
            double factor = (factorw < factorh ? factorw : factorh);
            
            double w = getWidth();
            double h = getHeight();
            
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());
            ((Graphics2D)g).scale(factor,factor);
            g.drawImage(imageToDisplay,
                    (int) ((w - factor *  imageToDisplay.getWidth())  / (factor * 2)),
                    (int) ((h - factor * imageToDisplay.getHeight())  / (factor * 2)),
                    null);
        }
        else {
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(640, 368);
    }

    public void reconnect() {
        NeptusLog.pub().info("AirCamDisplay: reconnecting to "+ip+"...");
        connected = false;        
    }
    
    private Thread updaterThread() {
    
        return new Thread() {

            @Override
            public void run() {

                while(true) {   
                    
                    if (updater != this)
                        return;

                    if (ip == null)
                        break;
                    connected = true;
                    try {
                        URL snap = new URL("http://"+ip+"/snapshot.cgi");
                        imageToDisplay = ImageIO.read(snap);     
                        repaint();
                    }
                    catch (Exception e) {
                        status = "Error: "+e.getMessage();
                        e.printStackTrace();
                        NeptusLog.pub().warn(e);          
                        repaint();
                        connected = false;
                        status = "reconnecting";
                    }

                    try {
                        Thread.sleep(millisBetweenRefresh);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn(e);
                    }
                }
                NeptusLog.pub().info("<###>Thread exiting...");
            }
        };
    }

    @Override
    public void cleanSubPanel() {
        status = "stopping";
        ip = null;
        connected = false;
        updater.interrupt();
    }

    protected String previousURL = null;

    @Override
    public DefaultProperty[] getProperties() {
        previousURL = ip;
        return super.getProperties();
    }

    @Override
    public void propertiesChanged() {
        if (!ip.equals(previousURL))
            reconnect();
    }

    @Override
    public void initSubPanel() {
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        final AirCamDisplay display = new AirCamDisplay(null);
        GuiUtils.testFrame(display, "Camera Display");
    }
}