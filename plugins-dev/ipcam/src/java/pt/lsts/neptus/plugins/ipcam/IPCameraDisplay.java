/*
 * Copyright (c) 2004-2024 Universidade do Porto - Faculdade de Engenharia
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
 * Author: José Pinto
 * Jun 22, 2010
 */
package pt.lsts.neptus.plugins.ipcam;

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
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;

import com.l2fprod.common.propertysheet.DefaultProperty;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.http.client.HttpClientConnectionHelper;

/**
 * @author zp
 *
 */
@Popup( pos = POSITION.RIGHT, width=400, height=400)
@PluginDescription(name="IP Camera Display", author="ZP", description="Video display for Conceptronic IP Camera", icon="pt/lsts/neptus/plugins/ipcam/camera.png")
public class IPCameraDisplay extends ConsolePanel implements ConfigurationListener {

    private static final long serialVersionUID = 1L;

    public enum InterpolationStyle { Nearest_Neighbour, Bilinear, Bicubic }; 

    @NeptusProperty(name="MJPEG URL", description="The URL where to download the MJPEG stream")
    public String url = "http://192.168.106.59/cgi/mjpg/mjpeg.cgi";

    @NeptusProperty(name="Fit video to panel size")
    public boolean autoSize = true;

    @NeptusProperty(name="Record to disk")
    public boolean record = true;
    
    @NeptusProperty(name="Recorded Video FPS")
    public int fps = 10;
    
    
    @NeptusProperty(name="Milliseconds between connection retries")
    public long millisBetweenTries = 1000;

    
    protected MJPEGCreator videoCreator = null;	
    protected BufferedImage imageToDisplay = null;
    protected byte[] cache = new byte[100*1024];
    protected HttpClientConnectionHelper httpComm;
    protected boolean connected = true;
    private final byte buff[] = new byte[256];
    protected Thread updater = null;
    protected String status = "initializing";

    public IPCameraDisplay(ConsoleLayout console) {
        super(console);
        removeAll();
        
        httpComm = new HttpClientConnectionHelper(HttpClientConnectionHelper.MAX_TOTAL_CONNECTIONS, 
                HttpClientConnectionHelper.DEFAULT_MAX_CONNECTIONS_PER_ROUTE, 1000, true);
        httpComm.initializeComm();

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
                            PropertiesEditor.editProperties(IPCameraDisplay.this, getConsole(), true);
                        }
                    });
                    JCheckBoxMenuItem recordOption = new JCheckBoxMenuItem("Record to disk");
                    recordOption.addActionListener(new ActionListener() {
                        
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            if (((JCheckBoxMenuItem)e.getSource()).isSelected()) {
                                record = true;
                            }
                            else {
                                record = false;                                
                            }
                        }
                    });
                    recordOption.setSelected(record);
                    popup.add(recordOption);
                    popup.show((Component)e.getSource(), e.getX(), e.getY());
                }
                
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        if (imageToDisplay != null) {
            if (!autoSize)
                g.drawImage(imageToDisplay, 1, 1, null);
            else {
                double factor1 = (getWidth()-2.0)/imageToDisplay.getWidth();
                double factor2 = (getHeight()-2.0)/imageToDisplay.getHeight();
                double factor = Math.min(factor1,factor2);
                ((Graphics2D)g).scale(factor, factor);
                g.drawImage(imageToDisplay, 0, 0, null);
                ((Graphics2D)g).scale(1/factor,1/factor);
                //g.drawImage(imageToDisplay, 1, 1, getWidth()-2, (int)(imageToDisplay.getHeight()*factor),0,0,(int)(imageToDisplay.getWidth()*factor), imageToDisplay.getHeight(), null);
            }
        }
        else {
            g.setColor(Color.black);
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(Color.gray);
            g.drawRect(0, 0, 320, 240);
            g.drawRect(0, 0, 640, 480);
        }
        g.setColor(Color.red);
        g.drawString(status, 10, 20);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, 240);
    }

    public void reconnect() {
        NeptusLog.pub().info("IPCameraDisplay: reconnecting to "+url+"...");
        connected = false;        
    }

    private Thread updaterThread() {
        return new Thread() {
            
            @Override
            public void run() {

                while(true) {	
                    if (updater != this)
                        return;
                    if (videoCreator != null)
                        try {
                            videoCreator.finishAVI();
                            videoCreator = null;
                        }
                    catch (Exception e) {
                        e.printStackTrace();
                    }

                    if (url == null)
                        break;
                    HttpGet get = new HttpGet(url);				
                    connected = true;
                    try {
                        HttpClientContext context = HttpClientContext.create();
                        HttpResponse resp = httpComm.getClient().execute(get, context);
                        httpComm.autenticateProxyIfNeeded(resp, context);
                        
                        InputStream is = resp.getEntity().getContent();
                        
                       
                        while (connected) {

                            if (record)
                                status = "[REC]";
                            else
                                status = "";

                            int prev = 0, cur = 0;
                            while (prev != 45 && cur != 45) {
                                prev = cur;
                                cur = is.read();
                            }
                            
                            //boundary
                            String line = "--"+readLine(is, false);
                            
                            String cl = line = readLine(is, false);
                            while (!line.startsWith("Content-Length:")) {
                                line = readLine(is, false);
                            }
                            cl = line;                            
                            readLine(is, false);
                            
                            int numBytes = 0;
                            if (cl != null)
                                numBytes = Integer.parseInt(cl.split(":")[1].trim());
                            
                            int offset = 2, r = 0;
                            int count = 0, lastByte = 0, thisByte = 0;
                            
                            while (true) {
                                thisByte = is.read();
                                count++;                                
                                if (lastByte == 0xFF && thisByte == 0xD8)
                                    break;
                                else
                                    lastByte = thisByte;							        						    
                            }
                            
                            numBytes -= (count+offset);	
                            
                            cache[0] = (byte)0xFF;
                            cache[1] = (byte)0xD8;

                            while (numBytes > offset) {
                                r = is.read(cache, offset, numBytes-offset);
                                if (r == -1) {
                                    is.close();
                                    imageToDisplay = null;
                                    connected = false;
                                    NeptusLog.pub().error("IPCamera: disconnected");
                                    System.err.println("IPCamera: disconnected");
                                    break;
                                }
                                offset += r;
                            }

                            if (videoCreator != null && !record) {
                                videoCreator.finishAVI();
                                videoCreator = null;
                            }
                            if (record && imageToDisplay != null) {
                                new File("log/ipcam").mkdirs();
                                if (videoCreator == null)
                                    videoCreator = new MJPEGCreator(imageToDisplay.getWidth(), imageToDisplay.getHeight(), 10);								
                                if (cache.length >= numBytes && numBytes > 0) {
                                    videoCreator.addImage(cache, numBytes);
                                }
                            }
                            

                            ByteArrayInputStream bais = new ByteArrayInputStream(cache, 0, numBytes);
                            ImageInputStream iis = ImageIO.createImageInputStream(bais);

                            BufferedImage image = ImageIO.read(iis);

                            imageToDisplay = image;		
                            repaint();
                        }
                        status = "reconnecting";
                        is.close();
                        get.abort();
                    }
                    catch (Exception e) {
                        status = "Error: "+e.getMessage();
                        e.printStackTrace();
                        NeptusLog.pub().warn(e);          
                        repaint();
                        connected = false;
                    }
                    finally {

                    }

                    try {
                        Thread.sleep(millisBetweenTries);
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn(e);
                    }
                }
                NeptusLog.pub().info("<###>Thread exiting...");
            }
        };
    }

    /**
     * Reads an HTTP Header line (terminated by '\n')
     */
    String readLine(InputStream is, boolean debug) throws IOException {
        int offset = 0;
        char prev = '\0';
        int r = 0;
        
        while (prev != '\n') {
            try {
                r = is.read(buff, offset, 1);
            }
            catch (Exception e) {
                e.printStackTrace();
                return "ERROR";
            }
            if (r == -1)
                break;
            if (debug)
                System.out.printf("%02x ", buff[offset]);

            prev = (char) buff[offset++];			
        }
        if (debug)
            System.out.println();
        return new String(buff, 0, offset).trim();

    }

    @Override
    public void cleanSubPanel() {
        if (httpComm != null)
            httpComm.cleanUp();
        
        status = "stopping";
        url = null;
        connected = false;
        updater.interrupt();
    }

    protected String previousURL = null;
    protected boolean wasRecording = false;
    
    @Override
    public DefaultProperty[] getProperties() {
        previousURL = url;
        wasRecording = record;
        return super.getProperties();
    }
    
    @Override
    public void propertiesChanged() {
        if (!url.equals(previousURL))
            reconnect();
        try {
            if (wasRecording && !record && videoCreator != null) {                
                videoCreator.finishAVI();
                videoCreator = null;
            }
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
        }
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        final IPCameraDisplay display = new IPCameraDisplay(null);
        GuiUtils.testFrame(display, "Camera Display");
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

}