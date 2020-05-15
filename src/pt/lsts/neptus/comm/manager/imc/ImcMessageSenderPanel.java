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
 * Author: Paulo Dias
 * 2008/04/13
 */
package pt.lsts.neptus.comm.manager.imc;

import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.gui.ImcCopyPastePanel;
import pt.lsts.neptus.gui.LocationCopyPastePanel;
import pt.lsts.neptus.gui.MessagePreviewer;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author pdias
 * @author keila - Changes on 2/12/2019
 *
 */
public class ImcMessageSenderPanel extends JPanel {

    private static final long serialVersionUID = 3776289592692060016L;

    private static ImageIcon ICON = new ImageIcon(
            ImageUtils.getImage("images/imc.png").getScaledInstance(16, 16, Image.SCALE_SMOOTH));
    private static ImageIcon ICON1 = new ImageIcon(
            ImageUtils.getImage("images/imc.png").getScaledInstance(48, 48, Image.SCALE_SMOOTH));

    private JComboBox<String> messagesComboBox = null;
    private JButton editMessageButton = null;
    private JButton previewButton = null;

    private LocationCopyPastePanel locCopyPastePanel = null;

    private ImcCopyPastePanel msgCopyPastePanel = null;

    private JTextField address = new JTextField("127.0.0.1");
    private NumberFormat nf = new DecimalFormat("#####");
    private JTextField port = null;
    private JTextField bindPort = new JTextField("");

    private MessagePreviewer msgPreview;

    private JTabbedPane tabs = null;

    private IMCFieldsPanel fields = null;

    private HashMap<String, IMCMessage> messagesPool = new HashMap<String, IMCMessage>();

    /**
     * Creates a @JPanel to construct a @IMCMessage.
     * By default the footer of the panel has an Edit and Preview button.
     * More buttons can be created adding them on this constructor method. 
     * The message created by this panel can be accessed in runtime events 
     * by using the {@link #getMessage()} or {@link #getOrCreateMessage(String abbrev)}.
     * @param custom list of custom buttons 
     */
    public ImcMessageSenderPanel(JButton... custom) {
        initialize(custom);
    }

    private void initialize(JButton... custom) {
        port = new JTextField(nf.format(GeneralPreferences.commsLocalPortUDP));

        // Main Tab Panel
        JPanel holderConfig = new JPanel();
        GroupLayout layoutConfig = new GroupLayout(holderConfig);
        holderConfig.setLayout(layoutConfig);
        layoutConfig.setAutoCreateGaps(true);
        layoutConfig.setAutoCreateContainerGaps(true);

        // Footer panel
        JPanel holderFooter = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // Main Tab components
        JLabel addressLabel = new JLabel("Address and Port to UDP send");
        JLabel localBindLabel = new JLabel("Local Port to bind (can be blanc)");
        JLabel msgNameLabel = new JLabel("Choose IMC Message");

        layoutConfig.setHorizontalGroup(layoutConfig.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addGroup(layoutConfig.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(localBindLabel)
                        .addComponent(bindPort))
                .addGroup(layoutConfig.createParallelGroup(GroupLayout.Alignment.CENTER).addComponent(addressLabel)
                        .addGroup(layoutConfig.createSequentialGroup().addComponent(address).addComponent(port)))
                .addGroup(layoutConfig.createParallelGroup(Alignment.CENTER).addComponent(msgNameLabel)
                        .addComponent(getMessagesComboBox())));

        layoutConfig.setVerticalGroup(layoutConfig.createSequentialGroup()
                .addGroup(layoutConfig.createSequentialGroup().addComponent(localBindLabel).addComponent(bindPort, 25,
                        25, 25))
                .addGroup(layoutConfig.createSequentialGroup().addComponent(addressLabel)
                        .addGroup(layoutConfig.createParallelGroup(GroupLayout.Alignment.CENTER)
                                .addComponent(address, 25, 25, 25).addComponent(port, 25, 25, 25)))
                .addGroup(layoutConfig.createSequentialGroup().addComponent(msgNameLabel)
                        .addComponent(getMessagesComboBox(), 25, 25, 25)));

        tabs = getTabedPane();
        String mgsName = (String) getMessagesComboBox().getSelectedItem();
        fields = new IMCFieldsPanel(null, mgsName, null);

        // Buttons container in the bottom
        holderFooter.add(getMsgCopyPastePanel());
        holderFooter.add(getLocCopyPastPanel());
        holderFooter.add(getEditMessageButton());
        holderFooter.add(getPreviewButton());
        //holder_footer.add(getCreateButton());
        for(JButton button: custom) {
            button.setPreferredSize(new Dimension(90, 26));
            button.setText(I18n.text(button.getText()));
            holderFooter.add(button);
        }
        
        @SuppressWarnings("serial")
        JScrollPane scrollFooter = new JScrollPane(holderFooter) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(700, 40);
            }
        };

        tabs.add("General Settings", holderConfig);
        tabs.add("Message Fields", fields.getContents());
        this.setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
        add(scrollFooter, BorderLayout.SOUTH);
    }

    /**
     * @return
     */
    private JTabbedPane getTabedPane() {
        if (tabs == null) {
            tabs = new JTabbedPane();
            ChangeListener changePane = new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    if (tabs.getSelectedIndex() == 1) { // fields
                        if (messagesComboBox == null) {
                            messagesComboBox = getMessagesComboBox();
                        }
                        // all components have been created
                        String mName = (String) getMessagesComboBox().getSelectedItem();
                        IMCMessage m = ImcMessageSenderPanel.this.messagesPool.get(mName);
                        if (ImcMessageSenderPanel.this.fields == null) {
                            fields = new IMCFieldsPanel(null, mName, m);
                            tabs.setComponentAt(1, fields.getContents());
                            tabs.repaint();
                        }
                        else if (!mName.equals(ImcMessageSenderPanel.this.fields.getMessageName())) {
                            fields = new IMCFieldsPanel(null, mName, m);
                            tabs.setComponentAt(1, fields.getContents());
                            tabs.repaint();
                        }
                    }
                }
            };
            tabs.addChangeListener(changePane);
        }
        return tabs;
    }

    private JComboBox<String> getMessagesComboBox() {
        if (messagesComboBox == null) {
            List<String> mList = new ArrayList<String>(IMCDefinition.getInstance().getMessageCount());
            for (String mt : IMCDefinition.getInstance().getMessageNames()) {
                mList.add(mt);
            }
            Collections.sort(mList);
            messagesComboBox = new JComboBox<String>(mList.toArray(new String[mList.size()]));
            messagesComboBox.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    String selectedItem = (String) messagesComboBox.getSelectedItem();
                    IMCMessage m = ImcMessageSenderPanel.this.messagesPool.get(selectedItem);
                    if (fields == null)
                        fields = new IMCFieldsPanel(null, selectedItem, m);
                    if (!(fields.getMessageName().equals(selectedItem))) {
                        IMCMessage toCache = fields.getImcMessage();
                        ImcMessageSenderPanel.this.messagesPool.put(fields.getMessageName(), toCache);
                        fields = new IMCFieldsPanel(null, selectedItem, m);
                        tabs.setComponentAt(1, fields.getContents());
                        tabs.repaint();
                    }
                }
            });
        }
        return messagesComboBox;
    }

    /**
     * This method initializes editMessageButton
     * 
     * @return javax.swing.JButton
     */
    private JButton getEditMessageButton() {
        if (editMessageButton == null) {
            editMessageButton = new JButton();
            editMessageButton.setText(I18n.text("Edit"));
            editMessageButton.setPreferredSize(new Dimension(90, 26));
            editMessageButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    tabs.setSelectedIndex(1);
                }
            });
        }
        return editMessageButton;
    }

    /**
     * This method associates a @pt.lsts.imc.sender.MessageEditor to the currently selected message on the panel
     * 
     * @return
     */
    private JButton getPreviewButton() {
        if (previewButton == null) {
            previewButton = new JButton();
            previewButton.setText(I18n.text("Preview"));
            previewButton.setPreferredSize(new Dimension(90, 26));
        }
        ActionListener previewAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String mName = (String) messagesComboBox.getSelectedItem();
                IMCMessage m = ImcMessageSenderPanel.this.messagesPool.get(mName);
                if (fields == null)
                    fields = new IMCFieldsPanel(null, mName, m);
                else if (!mName.equals(fields.getMessageName())) {
                    IMCMessage toCache = fields.getImcMessage();
                    ImcMessageSenderPanel.this.messagesPool.put(fields.getMessageName(), toCache);
                    fields = new IMCFieldsPanel(null, mName, m);
                }

                IMCMessage sMsg = fields.getImcMessage();
                messagesPool.put(mName, sMsg);
                msgPreview = new MessagePreviewer(sMsg, true, true, true);
                JDialog dg = new JDialog(SwingUtilities.getWindowAncestor(ImcMessageSenderPanel.this),
                        ModalityType.DOCUMENT_MODAL);
                dg.setContentPane(msgPreview);
                dg.setSize(500, 500);
                dg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                dg.getRootPane().registerKeyboardAction(ev -> { dg.dispose(); },
                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
                GuiUtils.centerParent(dg, (Window) dg.getParent());
                dg.setVisible(true);
            }
        };
        previewButton.addActionListener(previewAction);
        return previewButton;
    }

    /**
     * @return the locCopyPastPanel
     */
    private LocationCopyPastePanel getLocCopyPastPanel() {
        if (locCopyPastePanel == null) {
            locCopyPastePanel = new LocationCopyPastePanel() {
                private static final long serialVersionUID = 1809942752421373734L;

                @Override
                public void setLocationType(LocationType locationType) {
                    super.setLocationType(locationType);
                    boolean res = ImcMessageSenderPanel.this.fields
                            .applyLocation(getLocCopyPastPanel().getLocationType());
                    if (res) { // changes to be applied to panel
                        JPanel newPanel = ImcMessageSenderPanel.this.fields.getContents();
                        tabs.removeTabAt(1);
                        ImcMessageSenderPanel.this.tabs.add("Message Fields", newPanel);
                        ImcMessageSenderPanel.this.tabs.repaint();
                        ImcMessageSenderPanel.this.tabs.setSelectedIndex(1);
                    }
                }
            };
            locCopyPastePanel.setPreferredSize(new Dimension(85, 26));
            locCopyPastePanel.setMaximumSize(new Dimension(85, 26));
            locCopyPastePanel.setToolTipText(I18n.text("Pastes to lat and lon fields"));
        }
        return locCopyPastePanel;
    }

    @SuppressWarnings("serial")
    private ImcCopyPastePanel getMsgCopyPastePanel() {
        if (msgCopyPastePanel == null) {
            msgCopyPastePanel = new ImcCopyPastePanel() {

                @Override
                public IMCMessage getMsg() {
                    return ImcMessageSenderPanel.this.fields.getImcMessage();
                }

                @Override
                public void setMsg(IMCMessage msg) {
                    messagesPool.put(fields.getMessageName(), fields.getImcMessage());
                    String mgsName = msg.getAbbrev();
                    getMessagesComboBox().setSelectedItem(mgsName);
                    fields = new IMCFieldsPanel(null, mgsName, msg);
                    JPanel newPanel = ImcMessageSenderPanel.this.fields.getContents();
                    int selTab = tabs.getSelectedIndex();
                    tabs.removeTabAt(1);
                    ImcMessageSenderPanel.this.tabs.add("Message Fields", newPanel);
                    ImcMessageSenderPanel.this.tabs.repaint();
                    if (selTab < tabs.getTabCount())
                        tabs.setSelectedIndex(selTab);
                }
            };
        }
        this.msgCopyPastePanel.setPreferredSize(new Dimension(85, 26));
        this.msgCopyPastePanel.setMaximumSize(new Dimension(85, 26));
        return this.msgCopyPastePanel;
    }

    public IMCMessage getOrCreateMessage(String mName) {

        IMCMessage msg = ImcMessageSenderPanel.this.messagesPool.get(mName);
        if (fields == null)
            fields = new IMCFieldsPanel(null, mName, msg);
        else if (!mName.equals(fields.getMessageName())) {
            IMCMessage toCache = fields.getImcMessage();
            ImcMessageSenderPanel.this.messagesPool.put(fields.getMessageName(), toCache);
            fields = new IMCFieldsPanel(null, mName, msg);
        }

        IMCMessage sMsg = fields.getImcMessage();
        messagesPool.put(mName, sMsg);

        return msg;
    }
    
    public IMCMessage getMessage() {
        String mName = (String) getMessagesComboBox().getSelectedItem();
        if(mName == null)
            mName = "Abort";
        return this.getOrCreateMessage(mName);
    }
    
    public void addTemplate(IMCMessage m) {
        messagesPool.put(m.getAbbrev(), m);
    }

    public static JFrame getFrame() {
        JFrame frame = GuiUtils.testFrame(new ImcMessageSenderPanel(), "Teste 1,2", 500, 500);
        frame.setSize(600, 500);
        frame.setTitle("IMC Message Sender (by UDP)");
        ArrayList<Image> imageList = new ArrayList<Image>();
        imageList.add(ICON.getImage());
        imageList.add(ICON1.getImage());
        frame.setIconImages(imageList);
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        return frame;
    }

    /**
     * @param args
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        ConfigFetch.initialize();
        new ImcMessageSenderPanel().getFrame().setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    /**
     * @return the bindPort
     */
    public String getBindPort() {
        return bindPort.getText();
    }

    /**
     * @return the address to send this message
     */
    public String getAddress() {
        return address.getText();
    }

    /**
     * @return the port to send this message
     */
    public String getPort() {
        return port.getText();
    }
}
