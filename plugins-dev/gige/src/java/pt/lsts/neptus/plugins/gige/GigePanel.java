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
 * Oct 8, 2013
 */
package pt.lsts.neptus.plugins.gige;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.gige.GigeManager.GigeDatagramListener;
import pt.lsts.neptus.util.VideoCreator;

/**
 * @author jqcorreia
 *
 */
@PluginDescription(name="GiGe Panel", description="GiGe Panel")
@Popup(name = "GiGe Panel", pos=POSITION.CENTER, width = 800, height = 600, accelerator=KeyEvent.VK_F5)
public class GigePanel extends ConsolePanel implements GigeDatagramListener{
    private static final long serialVersionUID = 1L;
    
    static int recordNum = 0;
    
    BufferedImage image = new BufferedImage(720, 480,
            BufferedImage.TYPE_BYTE_GRAY);
    JPanel panel = new JPanel() {
        private static final long serialVersionUID = 1L;

        protected void paintComponent(java.awt.Graphics g) {
            g.drawImage(image, 0, 0, null);
            if(recording)
                creator.addFrame(image, System.currentTimeMillis());
        };
    };

    DataBufferByte dbb = (DataBufferByte) image.getRaster().getDataBuffer();
    byte[] ib = dbb.getData();
    GigeManager manager;
    ByteBuffer bb;

    ArrayList<Integer> receivedList = new ArrayList<Integer>();
    boolean debug = true;
    boolean recording = false;
    
    VideoCreator creator;
    
    private JButton start = new JButton(new AbstractAction("Start") {

        /**
         * 
         */
        private static final long serialVersionUID = -6416906682002548227L;

        @Override
        public void actionPerformed(ActionEvent e) {
            manager.startStream();
        }
    });

    private JButton stop = new JButton(new AbstractAction("Stop") {

        /**
         * 
         */
        private static final long serialVersionUID = 6761584339646110219L;

        @Override
        public void actionPerformed(ActionEvent e) {
            manager.stopStream();
        }
    });
    
    private JButton connect = new JButton(new AbstractAction("Connect") {

        /**
         * 
         */
        private static final long serialVersionUID = -4026628689363444940L;

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                manager.connect(ipField.getText());
            }
            catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    });
    JTextField ipField = new JTextField();
    
    private JButton record = new JButton(new AbstractAction("Record") {

        /**
         * 
         */
        private static final long serialVersionUID = 791901689647495239L;

        @Override
        public void actionPerformed(ActionEvent e) {
            if(!recording) {
                recording = true;
                try {
                    creator = new VideoCreator(new File("ROV_" + recordNum++ + ".mp4"), 720, 480);
                }
                catch (Exception e1) {
                    e1.printStackTrace();
                }
                record.setText("Recording");
            }
            else {
                
                creator.closeStreams();
                recording = false;
                record.setText("Record");
            }
        }
        
    });
    /**
     * @param console
     */
    public GigePanel(ConsoleLayout console) {
        super(console);
    }


    @Override
    public void initSubPanel() {
        manager = new GigeManager("10.0.10.72");
        manager.addDatagramListener(this);

        setLayout(new MigLayout());


        setLayout(new MigLayout());

        add(panel, "w 720!, h 480!, wrap");
        add(connect, "split");
        add(start, "");
        add(stop, "");
        add(record, "wrap");
        add(ipField, "w 100");
    }

    @Override
    public void cleanSubPanel() {
        if(manager != null) {
            manager.stopStream();
            manager = null;
        }
    }   
    int currentBlockId = -1;
    boolean trailerReceived = false;
    int numPackets = 0;
    
    @Override
    public void receivedDatagram(DatagramPacket packet) {
        try {
            byte buf[] = packet.getData();
            bb = ByteBuffer.wrap(buf);

            int type = bb.get(4) & 0x0F; // Payload type (leader, data or trailer)

            if (type == 1) {
              if(!trailerReceived && (currentBlockId != -1)) {
                  manager.requestResend(numPackets + 1, numPackets + 1, currentBlockId);
                  return;
              }
                trailerReceived = false;
                currentBlockId = bb.getShort(2);
                receivedList.clear();
                
                int sizeX = bb.getInt(24);
                int sizeY = bb.getInt(28);
                
                numPackets = (sizeX * sizeY) / (manager.packetSize - 8); // Number os packets in this block;
                
                if(debug) {
                    System.out.println("Leader of block ID: " + currentBlockId);
                    System.out.println("Payload type : " + bb.getShort(10));
                    System.out.println("Timestamp: " + (bb.getLong(12)));
                    System.out.println("Pixelformat: " + bb.getInt(20));
                    System.out.println("sx: " + bb.getInt(24));
                    System.out.println("sy: " + bb.getInt(28));
                    System.out.println("offx: " + bb.getInt(32));
                    System.out.println("offy: " + bb.getInt(36));
                    System.out.println("padx: " + bb.getShort(40));
                    System.out.println("pady: " + bb.getShort(42));
                    System.out.println("interleaving: " + (bb.get(8) & 0x0F));
                    System.out.println("Number of packets in this block: " + numPackets);
                }
                panel.repaint();
            }
            if (type == 2) {
//              if(debug)
                int pid = (bb.getInt(4) & 0x00FFFFFF);
                System.out.println("Trailer of block ID: " + bb.getShort(2) + " pId : " + pid);
                
                for(int i = 1; i < pid; i++) {
                    if(!receivedList.contains(i)) {
                        System.out.println("Missing packet : " + i);
                        manager.requestResend(i, i, currentBlockId);
                    }
                }
                trailerReceived = true;
            }
            if (type == 3) {
                int pID = (bb.getInt(4) & 0x00FFFFFF);
//              if(debug) {
//                  System.out.println("Payload packet ID: "
//                          + (bb.getInt(4) & 0x00FFFFFF) + " with size " + packet.getLength());
//              }
                
                if(receivedList.contains(pID)) {
                    System.out.println("Repeated packet ID!");
                    return;
                }
                if(bb.getShort(2) != currentBlockId) {
                    System.out.println("wrong block! " + bb.getShort(2) + " " + currentBlockId);
                    return;
                }

                int x = (manager.getPacketSize() - 8) * (pID - 1);
                
                for (int index = 8; index < packet.getLength(); index++) {
                    byte value = bb.get(index);
                    
                    ib[x] = value;
                    x++;
                }
                receivedList.add(pID);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        GigePanel panel = new GigePanel(null);
        
        panel.initSubPanel();
        frame.setLayout(new MigLayout());
        frame.setSize(800,600);
        frame.add(panel, "w 800, h 600");
        
        frame.setVisible(true);
    }
}
