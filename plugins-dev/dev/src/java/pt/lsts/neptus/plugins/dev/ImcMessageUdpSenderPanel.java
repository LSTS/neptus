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
 * Author: pdias
 * Mar 27, 2019
 */
package pt.lsts.neptus.plugins.dev;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.util.Collection;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.sender.UIUtils;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMessageSenderPanel;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.util.ByteUtil;

/**
 * @author pdias
 * @author keila - changes on April 23rd
 */
@PluginDescription(name = "IMC Message UDP Sender",
    icon = "images/imc.png",
    description = "Send IMC messages using UDP. Use for test purposes")
@Popup(pos = POSITION.TOP_LEFT, width = 600, height = 300)
@SuppressWarnings("serial")
public class ImcMessageUdpSenderPanel extends ConsolePanel {

    private ImcMessageSenderPanel panel;
    //Custom buttons to be added at the bottom of the panel
    private JButton burstPublishButton,publishButton;
    /**
     * @param console
     */
    public ImcMessageUdpSenderPanel(ConsoleLayout console) {
        super(console);
    }

    /**
     * @param console
     * @param usedInsideAnotherConsolePanel
     */
    public ImcMessageUdpSenderPanel(ConsoleLayout console, boolean usedInsideAnotherConsolePanel) {
        super(console, usedInsideAnotherConsolePanel);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        this.removeAll();
        this.setLayout(new BorderLayout());
        this.setSize(600, 500);
        burstPublishButton = getBurstPublishButton();
        publishButton      = getPublishButton();
        panel = new ImcMessageSenderPanel(burstPublishButton,publishButton);
        this.add(panel);
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
    }
    /**
     * @return the burstPublishButton
     */
    private JButton getBurstPublishButton() {
        if (burstPublishButton == null) {
            burstPublishButton = new JButton();
            burstPublishButton.setText(I18n.text("Burst"));
            burstPublishButton.setPreferredSize(new Dimension(85, 26));
            burstPublishButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    burstPublishButton.setEnabled(false);
                    SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
                        @Override
                        protected Boolean doInBackground() throws Exception {
                            Collection<String> mtypes = IMCDefinition.getInstance().getMessageNames();
                            for (String mt : mtypes) {
                                String msgName = mt;
                                IMCMessage sMsg = panel.getOrCreateMessage(msgName);
                                sMsg.setTimestampMillis(System.currentTimeMillis());
                                sMsg.dump(System.out);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                IMCOutputStream ios = new IMCOutputStream(baos);
                                try {
                                    sMsg.serialize(ios);
                                    ByteUtil.dumpAsHex(msgName + " [size=" + baos.size() + "]", baos.toByteArray(),
                                            System.out);
                                    String msg = sendUdpMsg(baos.toByteArray(), baos.size());
                                    if (msg != null)
                                        UIUtils.exceptionDialog(panel, new Exception(msg), "Error sending message by UDP",
                                                "Validate message");
                                    else {
                                        JOptionPane.showMessageDialog(panel,
                                                "Message sent successfully by UDP.", "Message Sent",
                                                JOptionPane.INFORMATION_MESSAGE);
                                    }
                                }
                                catch (Exception e1) {
                                    NeptusLog.pub().error(e1);
                                    e1.printStackTrace();
                                }
                            }
                            return false;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            if (burstPublishButton != null)
                                burstPublishButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return burstPublishButton;
    }
    

    /**
     * This method initializes publishButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getPublishButton() {
        if (publishButton == null) {
            publishButton = new JButton();
            publishButton.setText(I18n.text("Publish"));
            publishButton.setPreferredSize(new Dimension(90, 26));
            publishButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String msg = null;
                    IMCMessage sMsg = panel.getMessage();
                    if (sMsg != null) {
                        try {
                            sMsg.setTimestampMillis(System.currentTimeMillis());
                            sMsg.dump(System.out);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            IMCOutputStream ios = new IMCOutputStream(baos);
                            sMsg.serialize(ios);

                            ByteUtil.dumpAsHex(sMsg.getAbbrev() + " [size=" + baos.size() + "]", baos.toByteArray(), System.out);
                            msg = sendUdpMsg(baos.toByteArray(), baos.size());
                            if (msg != null)
                                UIUtils.exceptionDialog(panel, new Exception(msg), "Error sending message by UDP",
                                        "Validate message");
                            else {
                                JOptionPane.showMessageDialog(panel,
                                        "Message sent successfully by UDP.", "Message Sent",
                                        JOptionPane.INFORMATION_MESSAGE);
                            }

                        }
                        catch (Exception e1) {
                            NeptusLog.pub().warn(e1.getMessage(), e1);
                        }
                    }
                }
            });
        }
        return publishButton;
    }
    
    public String sendUdpMsg(byte[] msg, int size) {

        try {
            DatagramSocket sock = null;
            if ("".equalsIgnoreCase(panel.getBindPort())) {
                sock = new DatagramSocket();
            }
            else {
                int bport = Integer.parseInt(panel.getBindPort());
                sock = new DatagramSocket(bport);
            }
            sock.connect(new InetSocketAddress(panel.getAddress(), Integer.parseInt(panel.getPort())));
            sock.send(new DatagramPacket(msg, size));
            sock.close();
            return null;
        }
        catch (Exception e) {
            NeptusLog.pub().error(e);
            return "Error sending the UDP message: " + e.getMessage();
        }
    }

}
