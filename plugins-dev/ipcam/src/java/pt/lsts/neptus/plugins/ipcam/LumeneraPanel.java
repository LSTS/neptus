/*
 * Copyright (c) 2004-2021 Universidade do Porto - Faculdade de Engenharia
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
 * Author: jqcorreia
 * Oct 15, 2013
 */
package pt.lsts.neptus.plugins.ipcam;

import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;

/**
 * 
 * @author jqcorreia
 *
 */
public class LumeneraPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    BufferedImage image;
    String urltext = "http://10.0.10.74/cgi-usr/nph-image";

    int fps = 25;
    Thread refreshThread;
    
    public JButton btnRefresh = new JButton(new AbstractAction("Auto-refresh") {
        private static final long serialVersionUID = -1591503925037654541L;

        @Override
        public void actionPerformed(ActionEvent e) {
            if(refreshThread != null) {
                refreshThread.interrupt();
                
            }
            refreshThread = getRefreshThread();
            refreshThread.start();
        }
    });
    
    
    public LumeneraPanel() {
        
    }
    
    public Thread getRefreshThread() {
        return new Thread(new Runnable() {
            public boolean done = false;
            
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long interval = 1000 / fps;
                
                while(!done) {
                    long delta = System.currentTimeMillis() - currentTime;
                    if(delta >= interval) {
                        fetchNewImage();
                        currentTime = System.currentTimeMillis();
                    }
                }
            }
        });
    }
    
    public void fetchNewImage() {
        URL url;
        try {
            url = new URL(urltext);
            HttpURLConnection con = ((HttpURLConnection) url.openConnection());
            image = ImageIO.read(con.getInputStream());
            revalidate();
            repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Override
    protected void paintComponent(Graphics g) {

        if(image != null) {
            g.drawImage(image, 0,0, null);
        }
    }

    public static void main(String[] args) {
        JFrame f = new JFrame();
        LumeneraPanel panel = new LumeneraPanel();
        
        f.setLayout(new MigLayout());
        f.setSize(800,600);
        f.setVisible(true);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        f.add(panel, "w 100%, h 100%, wrap");
        f.add(panel.btnRefresh);
    }
}

