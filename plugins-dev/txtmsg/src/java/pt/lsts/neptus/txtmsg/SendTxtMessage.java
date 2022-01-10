/*
 * Copyright (c) 2004-2022 Universidade do Porto - Faculdade de Engenharia
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
 * Jul 14, 2016
 */
package pt.lsts.neptus.txtmsg;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.BadLocationException;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.AcousticOperation;
import pt.lsts.imc.AcousticOperation.OP;
import pt.lsts.imc.TextMessage;
import pt.lsts.imc.TransmissionRequest;
import pt.lsts.imc.TransmissionStatus;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.console.plugins.planning.SoundPlayer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Send Txt Message", description = "Allows to send small text messages throught acoustics.")
@Popup(accelerator = KeyEvent.VK_Q, width = 400, height = 200)
public class SendTxtMessage extends ConsolePanel {

    private static final String SOUNDS_NOTIFICATION_WAV = "/sounds/notification.wav";
    private static final String TEXT_SUBMIT = "text-submit";

    public enum CommMeanEnum {
        ACOUSTICS
    }

    private static final String TXT_PREFIX = "TXT";
    
    @NeptusProperty(name = "Maximum Number of Chars to Send")
    private int maxNumberOfChars = 65;
    
    @NeptusProperty(name = "Maximum Number of Chars in Control Box")
    private int maxRecvChars = 1000;

    @NeptusProperty(name = "Gateway", userLevel = LEVEL.REGULAR, description = "Set the sender modem (optional)")
    private String senderNode = "";

    @NeptusProperty(name = "Destination", userLevel = LEVEL.REGULAR, description = "Destination modem, use empty for broadcast")
    private String destinationNode = "";

    @NeptusProperty(name = "Play Sound on Message received", userLevel = LEVEL.REGULAR, description = "Play a sound on a message received")
    private boolean soundOnReceive = true;
    
    private ConcurrentHashMap<Integer, TransmissionRequest> transmissions = new ConcurrentHashMap<Integer, TransmissionRequest>();
    
    // GUI
    private JTextArea sendBox;
    private JTextArea recvBox;
    private JScrollPane sendBoxScroll;
    private JScrollPane recvBoxScroll;
    private JButton sendButton;
    private JButton clearButton;
    private JButton clearCtlButton;
    private JButton settingsButton;
    
    // Actions
    private AbstractAction sendAction;
    private AbstractAction clearAction;
    private AbstractAction clearCtlAction;
    private AbstractAction settingsAction;
    
    private String currentStrToSend = "";
    
    private SimpleDateFormat dateFormater = new SimpleDateFormat("HH:mm:ss");

    /**
     * @param console
     */
    public SendTxtMessage(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {
        initialize();
    }

    @Override
    public void cleanSubPanel() {
    }

    private void initialize() {
        initializeActions();
        
        sendBox = new JTextArea();
        sendBox.setRows(1);
        sendBox.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (sendBox.getText().length() > maxNumberOfChars) {
                    try {
                        sendBox.setText(sendBox.getText(0, Math.max(0, sendBox.getText().length() - 2)));
                    }
                    catch (BadLocationException e1) {
                        e1.printStackTrace();
                    }
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
        
        InputMap input = sendBox.getInputMap();
        KeyStroke shiftEnter = KeyStroke.getKeyStroke("control ENTER");
        input.put(shiftEnter, TEXT_SUBMIT);
        ActionMap actions = sendBox.getActionMap();
        actions.put(TEXT_SUBMIT, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendButton.doClick();
            }
        });
        
        recvBox = new JTextArea();
        recvBox.setEnabled(false);
        
        sendBoxScroll = new JScrollPane(sendBox);
        
        recvBoxScroll = new JScrollPane(recvBox);
        recvBoxScroll.setAutoscrolls(true);
        
        sendButton = new JButton(sendAction);
        GuiUtils.reactEnterKeyPress(sendButton);
        clearButton = new JButton(clearAction);
        clearCtlButton = new JButton(clearCtlAction);
        settingsButton = new JButton(settingsAction);
        
        removeAll();
        setLayout(new MigLayout());
        setPreferredSize(new Dimension(400, 200));
        setPreferredSize(new Dimension(200, 100));
        
        add(sendBoxScroll, "w 100%, h 40px:40px:40px, span, wrap");
        add(sendButton, "h 20px");
        add(clearButton, "h 20px");
        add(clearCtlButton, "h 20px");
        add(settingsButton, "h 20px, wrap");
        add(recvBoxScroll, "w 100%, h 100%, span");
    }

    private void initializeActions() {
        sendAction = new AbstractAction(I18n.text("send")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msgStr = sendBox.getText().trim();
                if (msgStr.isEmpty())
                    return;
                
                TextMessage msgTxt = new TextMessage();
                msgTxt.setOrigin(GeneralPreferences.imcCcuName.toLowerCase());
                msgTxt.setText(" " +TXT_PREFIX + ":" + msgStr);

                String destination = destinationNode.isEmpty() ? "broadcast" : destinationNode;
                
                AcousticOperation msg = new AcousticOperation();
                msg.setOp(AcousticOperation.OP.MSG);
                msg.setMsg(msgTxt);
                ImcSystem sender = null;
                if (!senderNode.isEmpty())
                    sender = ImcSystemsHolder.lookupSystemByName(senderNode);
                try {
                    ArrayList<TransmissionRequest> requests = IMCSendMessageUtils.sendMessageAcoustically(msgTxt,
                            destination, sender, false, 5);
                    requests.forEach(t -> {
                        transmissions.put(t.getReqId(), t);
                        updateRecvBoxTxt(t.getReqId()+"> Transmission requested.");
                    });                    
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    updateRecvBoxTxt("STA> Error sending message: "+ex.getMessage());
                }                    
            }
        };

        clearAction = new AbstractAction(I18n.text("clear")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendBox.setText("");
            }
        };
        clearCtlAction = new AbstractAction(I18n.text("clear ctl")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                synchronized (recvBox) {
                    recvBox.setText("");
                }
            }
        };
        settingsAction = new AbstractAction("settings") {
            @Override
            public void actionPerformed(ActionEvent e) {
                PluginUtils.editPluginProperties(SendTxtMessage.this, true);                
            }
        };
    }
    
    @Subscribe
    public void onTextMessage(TextMessage msg) {
        String origin = msg.getOrigin();
        String txt = msg.getText().trim();
        
        if (txt == null || txt.isEmpty() || !txt.trim().startsWith(TXT_PREFIX))
            return;
        
        
        String recMessage = "REC>" + origin + ": " + txt.replaceFirst(TXT_PREFIX + ":", "");
        updateRecvBoxTxt(recMessage);

        post(Notification.warning("Txt Message", recMessage).requireHumanAction(true));
        
        if (soundOnReceive)
            playNotifySound();
    }

    private void playNotifySound() {
        new SoundPlayer(FileUtil.getResourceAsStream(SOUNDS_NOTIFICATION_WAV)).start();
    }

    @Subscribe
    public void on(AcousticOperation msg) {
        if (msg.getOp() == OP.MSG_DONE || msg.getOp() == OP.MSG_FAILURE || msg.getOp() == OP.BUSY) {
            String recMessage = "RES> " + msg.getOpStr(); 
            updateRecvBoxTxt(recMessage);
            if (msg.getOp() == OP.MSG_DONE && currentStrToSend.equalsIgnoreCase(sendBox.getText()))
                sendBox.setText("");
        }
    }
    
   @Subscribe
   public void on(TextMessage msg) {
       updateRecvBoxTxt("TXT> "+msg.getOrigin()+" | "+msg.getText());
   }
   
   @Subscribe
   public void on(TransmissionStatus msg) {
       if (transmissions.containsKey(msg.getReqId())) {
           updateRecvBoxTxt(msg.getReqId()+"> "+msg.getStatusStr()+" | "+msg.getInfo());
       }
   }
    
    
    /**
     * @param recMessage
     */
    private void updateRecvBoxTxt(String recMessage) {
        synchronized (recvBox) {
            recvBox.append(dateFormater.format(new Date()) + " | " + recMessage + "\n");
            String t = recvBox.getText();
            if (t.length() > maxRecvChars) {
                t = t.substring(Math.min(t.length(), t.length() - maxRecvChars), t.length());
                recvBox.setText(t);
                recvBox.setCaretPosition(t.length());
            }
            recvBox.scrollRectToVisible(new Rectangle(0, recvBox.getHeight() + 22, 1, 1));
        }
    }
}
