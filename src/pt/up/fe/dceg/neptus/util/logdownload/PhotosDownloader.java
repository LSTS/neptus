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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
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
 * Author: lsts
 * Jul 8, 2013
 */
package pt.up.fe.dceg.neptus.util.logdownload;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JProgressBar;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.ftp.FtpDownloader;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;

/**
 * @author lsts
 *
 */
public class PhotosDownloader extends JDialog implements FtpFileChangeListener {

    String host, sysname, logName;
    int port;
    
    JButton btnActivate = new JButton(new AbstractAction("Activate") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            setDoamState(true);
        }
    });
    
    JButton btnDeactivate = new JButton(new AbstractAction("Deactivate") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            setDoamState(false);
        }
    });
    JButton btnDownload = new JButton(new AbstractAction("Download") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            Thread t = new Thread(new Runnable() {
                
                @Override
                public void run() {
                    FtpDownloader downloader;
                    try {
                        downloader = new FtpDownloader(host, 30021);
                        String src = "/" + logName + "/Photos";
                        String dest =  "log/downloaded/" + sysname + "/" +logName + "/Photos/";
                        
                        File dir = new File(dest); 
                        if(!dir.exists()) {
                            dir.mkdirs();
                        }
//                        downloader.listener = PhotosDownloader.this;
                        downloader.downloadDirectory(src, dest);
                        setDoamState(false);
                        
                    }
                    catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }
                    
                }
            });
            t.start();

        }
    });
    
    JLabel txtFilename = new JLabel();
    JProgressBar bar = new JProgressBar(0, 100);
    public PhotosDownloader(Frame owner, String host, String sysname, int port, String logName) {
        super(owner, "Photo Downloader");
        this.host = host;
        this.sysname = sysname;;
        this.port = port;
        this.logName = logName;
//        ImcMsgManager.getManager().addListener(new MessageListener<MessageInfo, IMCMessage>() {
//            
//            @Override
//            public void onMessage(MessageInfo info, IMCMessage msg) {
//                if(msg.getAbbrev().equals("EntityActivationState")) {
//                    System.out.println(msg);
//                }
//            }
//        });
        
        getContentPane().setLayout(new MigLayout());
        
        bar.setStringPainted(true);
        bar.setValue(0);
        add(bar, "wrap");
        add(txtFilename, "wrap");
        add(btnActivate, "split");
        add(btnDeactivate);
        add(btnDownload);
        setSize(300,200);
        setVisible(true);
    }
    
    public void setDoamState(boolean state) {
        
        IMCMessage msg = new IMCMessage("PowerChannelControl"); 
        msg.setValue("name", "Camera - CPU");
        msg.setValue("op", (state ? 1 : 0));

        ImcMsgManager.getManager().sendMessageToSystem(msg, "lauv-dolphin-1");
    }
    
//    public static void main(String[] args) {
//        ImcMsgManager.getManager().start();
//        
//        PhotosDownloader downloader = new PhotosDownloader("","",0,"/20130703/135247_rows1-alt5_doam_ss/Photos");
//    }

    @Override
    public void fileChanged(String fileName) {
        txtFilename.setText("Downloading " + fileName);
        revalidate();
        repaint();
    }

    @Override
    public void progressChanged(int percent) {
        System.out.println(percent);
        bar.setValue(percent);
        revalidate();
        repaint();
    }
}
