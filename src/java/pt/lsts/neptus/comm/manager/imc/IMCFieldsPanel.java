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
 * Author: keila
 * Dec 3, 2019
 */
package pt.lsts.neptus.comm.manager.imc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.Header;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.sender.UIUtils;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.BitmaskPanel;
import pt.lsts.neptus.gui.ImcCopyPastePanel;
import pt.lsts.neptus.gui.LocationCopyPastePanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.Bitmask;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author keila 3/12/19
 */
public class IMCFieldsPanel {

    private final int mid;
    private final List<String> m_fields;

    private JTextField srcId = new JTextField("");
    private JTextField dstId = new JTextField("");
    private JTextField srcEntId = new JTextField("");
    private JTextField dstEntId = new JTextField("");

    private JPanel holderHFields = null;
    private JPanel content, holderFields;
    private JScrollPane scrollable;

    private final IMCMessage msg;
    private final Header header;

    private Map<String, IMCMessage> inlineMsgs = Collections.synchronizedMap(new LinkedHashMap<>());
    private Map<String, List<IMCMessage>> msgList = Collections.synchronizedMap(new LinkedHashMap<>());
    private LinkedHashMap<String, BitmaskPanel> bitfields = new LinkedHashMap<>();
    private MigLayout bitFieldLayout = new MigLayout("wrap 3");

    IMCFieldsPanel parent;

    /**
     * @return the holder_fields
     */
    public JPanel getContents() {
        return this.content;
    }

    /***
     * Creates a new Panel for the @IMCMessage being edited. If the IMCMessage is not null, the fields are filled
     * accordingly.
     * 
     * @param p - parent panel
     * @param name - The @IMCMessage Abbrev Name
     * @param m - The @IMCMessage to use if feasible
     */
    public IMCFieldsPanel(IMCFieldsPanel p, String name, IMCMessage m) {
        this.parent = p;
        this.mid = IMCDefinition.getInstance().getMessageId(name);
        this.m_fields = new ArrayList<>();
        if (m == null)
            this.msg = IMCDefinition.getInstance().create(name);
        else {
            this.msg = m.cloneMessage();
            Arrays.stream(this.msg.getFieldNames()).forEach(item -> fillPanel(item));
        }
        this.header = this.msg.getHeader();
        this.m_fields.addAll(Arrays.asList(this.msg.getFieldNames()));
        this.initializePanel();

    }

    /**
     * Fills the current Panel with field values already defined for the message being edited.
     * 
     * @param item - field name
     * @return
     */
    private void fillPanel(String item) {
        switch (this.msg.getTypeOf(item)) {
            case "message":
                if (this.msg.getMessage(item) != null)
                    this.inlineMsgs.put(item, this.msg.getMessage(item));
                return;
            case "message-list":
                if (!this.msg.getMessageList(item).isEmpty())
                    this.msgList.put(item, this.msg.getMessageList(item));
                return;
        }
        String b = this.msg.getUnitsOf(item);
        if (b == null)
            return;
        boolean bitfield = b.equalsIgnoreCase("Bitfield");
        if (bitfield) {
            Bitmask bitmask = new Bitmask(this.msg.getIMCMessageType().getFieldPossibleValues(item),
                    this.msg.getLong(item));
            BitmaskPanel bitfieldPanel = BitmaskPanel.getBitmaskPanel(bitmask);
            this.bitfields.put(item, bitfieldPanel);
        }
    }

    @SuppressWarnings("serial")
    private void initializePanel() {
        JLabel title;
        holderHFields = new JPanel();

        // Panel with fields for main IMC Message -> requires header fields
        if (this.parent == null) {
            // Fixed components from IMC Headers
            GroupLayout layoutFields = new GroupLayout(holderHFields);
            holderHFields.setLayout(layoutFields);
            layoutFields.setAutoCreateGaps(true);
            layoutFields.setAutoCreateContainerGaps(true);
            holderHFields.setSize(new Dimension(450, 150));
            title = new JLabel("Edit " + this.getMessageName() + " Fields");
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));

            JLabel srcDstIdLabel = new JLabel("Source and Destination IMC IDs");
            JLabel srcDestEntLabel = new JLabel("Source and Destination Entities IDs (can be blanc)");
            
            if(this.header.get_src() >= 0){
                srcId.setText(String.valueOf(this.header.get_src()));
            }
            if(this.header.get_dst() >= 0){
                dstId.setText(String.valueOf(this.header.get_dst()));
            }
            if(this.header.get_dst_ent() >= 0){
                dstEntId.setText(String.valueOf(this.header.get_dst_ent()));
            }
            if(this.header.get_src_ent() >= 0){
                srcEntId.setText(String.valueOf(this.header.get_src_ent()));
            }

            layoutFields.setHorizontalGroup(layoutFields.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(title).addComponent(srcDstIdLabel)
                    .addGroup(layoutFields.createSequentialGroup().addComponent(srcId).addComponent(dstId))
                    .addComponent(srcDestEntLabel)
                    .addGroup(layoutFields.createSequentialGroup().addComponent(srcEntId).addComponent(dstEntId)));

            layoutFields.setVerticalGroup(
                    layoutFields.createSequentialGroup().addComponent(title).addComponent(srcDstIdLabel)
                            .addGroup(layoutFields.createParallelGroup(GroupLayout.Alignment.CENTER)
                                    .addComponent(srcId, 25, 25, 25).addComponent(dstId, 25, 25, 25))
                            .addComponent(srcDestEntLabel)
                            .addGroup(layoutFields.createParallelGroup(GroupLayout.Alignment.CENTER)
                                    .addComponent(srcEntId, 25, 25, 25).addComponent(dstEntId, 25, 25, 25)));
        }
        else {
            holderHFields.setSize(new Dimension(450, 50));
            title = new JLabel("Edit Inline " + this.getMessageName() + " Fields");
            title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
            holderHFields.add(title, Component.CENTER_ALIGNMENT);
        }

        holderFields = new JPanel();
        GroupLayout layout = new GroupLayout(holderFields);
        holderFields.setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        GroupLayout.ParallelGroup horizontal = layout.createParallelGroup(Alignment.CENTER);
        GroupLayout.SequentialGroup vertical = layout.createSequentialGroup();
        layout.setHorizontalGroup(layout.createSequentialGroup().addGroup(horizontal));
        layout.setVerticalGroup(vertical);

        JLabel previousLabel = null;
        for (int i = 0; i < m_fields.size(); i++) {
            String field = m_fields.get(i);
            boolean enumerated = false, bitfield = false;
            JLabel label = new JLabel(field);
            String dType = msg.getTypeOf(field);
            String units = msg.getUnitsOf(field);

            if (units != null) {
                String sufix = "";
                if (units.equalsIgnoreCase("rad"))
                    sufix = " (degrees)";
                else if (units.equalsIgnoreCase("rad/s"))
                    sufix = " (deg/s)";
                else if (units.equalsIgnoreCase("enumerated"))
                    enumerated = true;
                else if (units.equalsIgnoreCase("Bitfield"))
                    bitfield = true;
                else
                    sufix = " (" + units + ")";
                label = new JLabel(field + sufix);
            }

            if (dType.equalsIgnoreCase("message")) { // Inline Message
                JComboBox<String> messagesComboBox = null;
                messagesComboBox = getMessageComboBox(field, false);
                if (this.inlineMsgs.containsKey(field)) {
                    IMCMessage m = this.inlineMsgs.get(field);
                    String selectedItem = m.getAbbrev();
                    messagesComboBox.setSelectedItem(selectedItem);
                }
                else
                    messagesComboBox.setSelectedIndex(0);
                JButton edit;
                edit = getEditButtonForMsg(field, messagesComboBox, false, null, null);
                label.setLabelFor(messagesComboBox);
                horizontal.addGroup(layout.createSequentialGroup().addComponent(label).addComponent(messagesComboBox)
                        .addComponent(edit));
                vertical.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label)
                        .addComponent(messagesComboBox).addComponent(edit));
                if (i > 0) {
                    layout.linkSize(SwingConstants.HORIZONTAL, label, previousLabel);
                }
                previousLabel = label;
            }
            else if (dType.equalsIgnoreCase("message-list")) {
                JPanel inlineMsgPanel = new JPanel();
                JPanel msgHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
                inlineMsgPanel.setLayout(new BoxLayout(inlineMsgPanel, BoxLayout.Y_AXIS));
                // dynamic insertion all over again
                JComboBox<String> mListComboBox = getMessageComboBox(field, true);
                JButton plus = new JButton("+");
                ActionListener messageListAction = getMsgListAction(field, inlineMsgPanel, plus, mListComboBox);
                plus.addActionListener(messageListAction);
                msgHolder.add(mListComboBox);
                msgHolder.add(plus);
                inlineMsgPanel.add(msgHolder, 0);
                if (this.msgList.get(field) != null) {
                    if (!this.msgList.get(field).isEmpty()) {
                        for (IMCMessage m : this.msgList.get(field)) {
                            JPanel nMsgHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
                            inlineMsgPanel.setLayout(new BoxLayout(inlineMsgPanel, BoxLayout.Y_AXIS));
                            JComboBox<String> comboBox = getMessageComboBox(field, true);
                            comboBox.setSelectedItem(m.getAbbrev());
                            JButton minus = new JButton(" - ");
                            JButton edit = getEditButtonForMsg(field, comboBox, true, m, minus);
                            nMsgHolder.add(comboBox);
                            nMsgHolder.add(edit);
                            nMsgHolder.add(minus);
                            minus.addActionListener(new ActionListener() {
                                @Override
                                public void actionPerformed(ActionEvent e) {
                                    inlineMsgPanel.remove(nMsgHolder);
                                    if (IMCFieldsPanel.this.msgList.get(field) != null) {
                                        IMCFieldsPanel.this.msgList.get(field).remove(m);
                                    }
                                    inlineMsgPanel.repaint();
                                }
                            });
                            comboBox.setEnabled(false);
                            inlineMsgPanel.add(nMsgHolder);
                        }
                    }
                }

                label.setLabelFor(inlineMsgPanel);
                horizontal.addGroup(layout.createSequentialGroup().addComponent(label).addComponent(inlineMsgPanel));
                vertical.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label)
                        .addComponent(inlineMsgPanel));
                if (i > 0) {
                    layout.linkSize(SwingConstants.HORIZONTAL, label, previousLabel);
                }
                previousLabel = label;
            }
            else {
                if (enumerated) {
                    List<String> mList = new ArrayList<String>();
                    mList.addAll(msg.getIMCMessageType().getFieldPossibleValues(field).values());
                    JComboBox<String> enumComboBox = new JComboBox<String>(mList.toArray(new String[mList.size()]));
                    try {
                        String definedVal = String.valueOf(this.msg.getValue(field));
                        int index = Integer.parseInt(definedVal);
                        enumComboBox.setSelectedIndex(index);
                    }
                    catch (Exception e) {
                        enumComboBox.setSelectedIndex(0);
                    }
                    enumComboBox.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            IMCFieldsPanel.this.msg.setValue(field, enumComboBox.getSelectedIndex());
                        }
                    });

                    label.setLabelFor(enumComboBox);
                    horizontal.addGroup(layout.createSequentialGroup().addComponent(label).addComponent(enumComboBox));
                    vertical.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label)
                            .addComponent(enumComboBox));
                    if (i > 0) {
                        layout.linkSize(SwingConstants.HORIZONTAL, label, previousLabel);
                    }
                    previousLabel = label;
                }
                else if (bitfield) {
                    BitmaskPanel bitfieldPanel;
                    if (this.bitfields.containsKey(field))
                        bitfieldPanel = this.bitfields.get(field);
                    else {
                        Bitmask bitmask = new Bitmask(this.msg.getIMCMessageType().getFieldPossibleValues(field), 0);
                        bitfieldPanel = BitmaskPanel.getBitmaskPanel(bitmask);
                    }
                    this.bitfields.put(field, bitfieldPanel);
                    bitfieldPanel.setMainComponentLayout(bitFieldLayout);
                    label.setLabelFor(bitfieldPanel);
                    horizontal.addGroup(layout.createSequentialGroup().addComponent(label).addComponent(bitfieldPanel));
                    vertical.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label)
                            .addComponent(bitfieldPanel));
                    if (i > 0) {
                        layout.linkSize(SwingConstants.HORIZONTAL, label, previousLabel);
                    }
                    previousLabel = label;
                }
                else {
                    String default_value = String.valueOf(msg.getValue(field));
                    try {
                        if (units != null)
                            if (units.equalsIgnoreCase("rad") || units.equalsIgnoreCase("rad/s")) {
                                // Object val = IMCUtil.parseString(msg.getIMCMessageType().getFieldType(field),
                                // default_value);
                                // double deg = Math.toDegrees((double) val);
                                double deg = Math.toDegrees(Double.valueOf(default_value).doubleValue());
                                default_value = String.valueOf(deg);
                            }
                    }
                    catch (Exception e) {
                        NeptusLog.pub().warn(I18n.text("Unable to convert value: " + default_value), e);
                    }
                    JTextField tField = new JTextField(default_value);

                    tField.getDocument().addDocumentListener(new DocumentListener() {
                        @Override
                        public void insertUpdate(DocumentEvent e) {
                            setValue();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                            String value = String.valueOf(msg.getIMCMessageType().getDefaultValue(field));
                            setValue(value);
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                            setValue();
                        }

                        public void setValue() {
                            setValue(tField.getText());
                        }

                        public void setValue(String v) {
                            Object value = IMCUtil.parseString(msg.getIMCMessageType().getFieldType(field), v);
                            if (units != null) {
                                if (units.equalsIgnoreCase("rad") || units.equalsIgnoreCase("rad/s")) {
                                    value = Math.toRadians((double) value);
                                }
                            }
                            IMCFieldsPanel.this.msg.setValue(field, value);
                        }
                    });

                    label.setLabelFor(tField);
                    horizontal.addGroup(layout.createSequentialGroup().addComponent(label).addComponent(tField));
                    vertical.addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).addComponent(label)
                            .addComponent(tField));
                    if (i > 0) {
                        layout.linkSize(SwingConstants.HORIZONTAL, label, previousLabel);
                    }
                    previousLabel = label;
                }
            }

            // if (i > 0) {
            // layout.linkSize(SwingConstants.HORIZONTAL, label, previousLabel);
            // }
            // previousLabel = label;

        }
        // layout.linkSize(SwingConstants.HORIZONTAL, labels[i], labels[0]);

        this.content = new JPanel(new BorderLayout()) {

            @Override
            public Dimension getPreferredSize() {
                int height = IMCFieldsPanel.this.m_fields.size() < 2 ? 100 : IMCFieldsPanel.this.m_fields.size() * 5;
                                                                                                                     
                return new Dimension(500, height);
            }
        };
        this.scrollable = new JScrollPane(this.holderFields) {

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(450, 250);
            }
        };

        this.content.add(this.holderHFields, BorderLayout.NORTH);
        this.content.add(this.scrollable, BorderLayout.CENTER);
    }

    /**
     * @param field
     * @param mList
     * @return
     */
    private JComboBox<String> getMessageComboBox(String field, boolean messageListField) {
        List<String> mList = getMessagesFilter(msg, field);
        Collections.sort(mList);
        mList.add(0, "None");
        JComboBox<String> comboBox = new JComboBox<String>(mList.toArray(new String[mList.size()]));
        comboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                @SuppressWarnings("unchecked")
                String selectedItem = (String) ((JComboBox<String>) e.getSource()).getSelectedItem();
                IMCMessage newMsg = IMCDefinition.getInstance().create(selectedItem);
                // create methods for each action
                if (!messageListField) {
                    synchronized (IMCFieldsPanel.this.inlineMsgs) {
                        if (IMCFieldsPanel.this.inlineMsgs.containsKey(field)) {
                            IMCMessage m = IMCFieldsPanel.this.inlineMsgs.get(field);
                            String abbrev = m.getAbbrev();
                            if (!abbrev.equals(selectedItem)) {
                                IMCFieldsPanel.this.inlineMsgs.put(field, newMsg);
                            }
                        }
                        else {
                            if (!selectedItem.equalsIgnoreCase("None")) {
                                IMCFieldsPanel.this.inlineMsgs.put(field, newMsg);
                            }
                        }
                    }
                }
                else if (messageListField) {
                    synchronized (IMCFieldsPanel.this.msgList) {
                        if (!selectedItem.equalsIgnoreCase("None")) {

                        }
                    }
                }
            }
        });
        return comboBox;
    }

    private void fillHeader() {

        String srcId_txt = srcId.getText();
        String srcEnt_txt = srcEntId.getText();

        String dstId_txt = dstId.getText();
        String dstEnt_txt = dstEntId.getText();

        try {
            int value = getImcId(srcId_txt);
            if (value < 0 || srcId_txt.isEmpty())
                value = GeneralPreferences.imcCcuId.intValue();
            this.header.set_src(value);
        }
        catch (NumberFormatException ne) {
            this.header.set_src(GeneralPreferences.imcCcuId.intValue());
        }
        try {
            short value = Short.parseShort(srcEnt_txt);
            if (value < 0 || srcEnt_txt.isEmpty())
                value = (short) Header.DEFAULT_ENTITY_ID;
            this.header.set_src_ent(value);
        }
        catch (NumberFormatException ne) {
            this.header.set_src_ent((short) Header.DEFAULT_ENTITY_ID);
        }

        try {
            int value = getImcId(dstId_txt);
            if (value < 0 || dstId_txt.isEmpty())
                value = Header.DEFAULT_SYSTEM_ID;
            this.header.set_dst(value);
        }
        catch (NumberFormatException ne) {
            this.header.set_dst(Header.DEFAULT_SYSTEM_ID);
        }
        try {
            short value = Short.parseShort(dstEnt_txt);
            if (value < 0 || dstEnt_txt.isEmpty())
                value = (short) Header.DEFAULT_ENTITY_ID;
            this.header.set_dst_ent(value);
        }
        catch (NumberFormatException ne) {
            this.header.set_dst_ent((short) Header.DEFAULT_ENTITY_ID);
        }

    }

    /**
     * @param txt
     * @return
     */
    private int getImcId(String txt) {
        int id;
        try {
            id = (int) ImcId16.parseImcId16(txt);
        }
        catch (NumberFormatException e) {
            try {
                id = Integer.parseInt(txt);
            }
            catch (NumberFormatException e1) {
                try {
                    id = Integer.parseInt(txt, 16);
                }
                catch (NumberFormatException e2) {
                    throw e2;
                }
            }
        }
        return id;
    }

    /**
     * @param inceptionFields
     * @param dg
     * @param panelCeption
     * @return the locCopyPastPanel
     */
    private LocationCopyPastePanel getLocCopyPastPanel(IMCFieldsPanel inceptionFields, JDialog dg,
            JPanel panelCeption) {
        LocationCopyPastePanel cp = new LocationCopyPastePanel() {
            private static final long serialVersionUID = -4084168490231715332L;

            @Override
            public void setLocationType(LocationType locationType) {
                super.setLocationType(locationType);

                boolean res = inceptionFields.applyLocation(locationType);
                JPanel newPanel = inceptionFields.getContents();
                inceptionFields.content.repaint();
                if (dg != null && res) {
                    // dg.setVisible(false); - keep commented prevent from blinking
                    Component buttons = panelCeption.getComponent(panelCeption.getComponentCount() - 1);
                    newPanel.add(buttons, BorderLayout.SOUTH, -1);
                    dg.setContentPane(newPanel);
                    dg.revalidate();
                    // dg.setVisible(true); - keep commented prevent from blinking
                }
            }
        };
        cp.setPreferredSize(new Dimension(85, 26));
        cp.setMaximumSize(new Dimension(85, 26));
        // locCopyPastPanel.setBorder(null);
        cp.setToolTipText("Pastes to fields lat and lon");
        return cp;
    }

    private ImcCopyPastePanel getMsgCopyPastePanel(JDialog dg, JPanel panelCeption, String field, IMCMessage m, boolean isMsgList, JComboBox<String> messagesComboBox, JButton editButton, JButton removal) {
        @SuppressWarnings("serial")
        ImcCopyPastePanel msgCopyPastePanel = new ImcCopyPastePanel() {
            @Override
            public IMCMessage getMsg() {
                if(IMCFieldsPanel.this.inlineMsgs.containsKey(field) && !isMsgList) {
                    IMCMessage imcMessage = IMCFieldsPanel.this.inlineMsgs.get(field);
                    super.setMsg(imcMessage);
                    return imcMessage;
                }
                return m;
            }

            @Override
            public void setMsg(IMCMessage msg) {
                if (msgBelongToComboBox(messagesComboBox, msg.getAbbrev())) {
                    messagesComboBox.setSelectedItem(msg.getAbbrev());
                    super.setMsg(msg);
                    IMCFieldsPanel inceptionFields = new IMCFieldsPanel(IMCFieldsPanel.this, msg.getAbbrev(), msg);
                    if (!isMsgList) {
                        if (IMCFieldsPanel.this.inlineMsgs.containsKey(field)) {
                            IMCFieldsPanel.this.inlineMsgs.remove(field);
                        }
                        IMCFieldsPanel.this.inlineMsgs.put(field, msg);
                    }
                    else {
                        int index = -1;
                        if (IMCFieldsPanel.this.msgList.get(field) == null)
                            IMCFieldsPanel.this.msgList.put(field, new ArrayList<IMCMessage>());
                        else {
                            index = IMCFieldsPanel.this.msgList.get(field).indexOf(m);
                            IMCFieldsPanel.this.msgList.get(field).remove(m);
                        }
                        
                        if(index>=0)
                            IMCFieldsPanel.this.msgList.get(field).add(index,msg);
                        else
                            IMCFieldsPanel.this.msgList.get(field).add(msg);
                        
                        //Update buttons @ActionListener
                        if (removal != null) {
                            removal.removeActionListener(
                                    editButton.getActionListeners()[editButton.getActionListeners().length - 1]);
                            removal.addActionListener(getLogicRemovalAction(field, msg));
                        }
                        for(ActionListener l: editButton.getActionListeners())
                            editButton.removeActionListener(l);
                        editButton.addActionListener(getActionForMsg(field, messagesComboBox, isMsgList, msg, removal));
                    }
                    
                    JPanel newPanel = inceptionFields.getContents();
                    if (dg != null) {
                        JPanel buttons = (JPanel) panelCeption.getComponent(panelCeption.getComponentCount() - 1);
                        int buttonsN = buttons.getComponentCount();
                        if (buttonsN == 4) {
                            buttons.remove(buttons.getComponentCount() - 1);
                            buttons.remove(buttons.getComponentCount() - 1);
                            JButton validate = getValidateButtonFor(inceptionFields);
                            JButton insert = getInsertButtonFor(inceptionFields, field, validate, isMsgList, msg);
                            buttons.add(validate);
                            buttons.add(insert);
                        }
                        newPanel.add(buttons, BorderLayout.SOUTH, -1);
                        dg.setContentPane(newPanel);
                        dg.revalidate();
                        dg.repaint();
                    }
                }
            }

            private boolean msgBelongToComboBox(JComboBox<String> messagesComboBox, String abbrev) {
                for(int i=0;i<messagesComboBox.getItemCount();i++) {
                    String msgName = messagesComboBox.getItemAt(i);
                    if(msgName.equalsIgnoreCase(abbrev))
                        return true;
                }
                return false;
            }
        };
        return msgCopyPastePanel;
    }

    protected boolean applyLocation(LocationType locationType) {
        IMCMessage sMsg = this.getImcMessage();
        List<String> fieldNames = Arrays.asList(sMsg.getFieldNames());
        boolean hasLatLon = false, hasXY = false, hasDepthOrHeight = false;
        if (fieldNames.contains("lat") && fieldNames.contains("lon"))
            hasLatLon = true;
        if (fieldNames.contains("x") && fieldNames.contains("y"))
            hasXY = true;
        if (fieldNames.contains("depth") || fieldNames.contains("height"))
            hasDepthOrHeight = true;

        if (hasLatLon) {
            if (!hasXY)
                locationType = locationType.getNewAbsoluteLatLonDepth();

            sMsg.setValue("lat", locationType.getLatitudeRads());
            sMsg.setValue("lon", locationType.getLongitudeRads());

            if (fieldNames.contains("depth"))
                sMsg.setValue("depth", locationType.getDepth());

            if (fieldNames.contains("height"))
                sMsg.setValue("height", locationType.getHeight());

            double[] val = CoordinateUtil.sphericalToCartesianCoordinates(locationType.getOffsetDistance(),
                    locationType.getAzimuth(), locationType.getZenith());

            if (hasXY) {
                sMsg.setValue("x", locationType.getOffsetNorth() + val[0]);
                sMsg.setValue("y", locationType.getOffsetEast() + val[1]);
            }

            if (fieldNames.contains("z")) {

                if (!hasDepthOrHeight) {
                    sMsg.setValue("z", locationType.getAllZ());
                }
                else {
                    sMsg.setValue("z", locationType.getOffsetDown() + val[2]);
                }
            }
            this.msg.setValues(sMsg.getValues());
            this.msg.setHeader(sMsg.getHeader());
            this.msg.setTimestamp(sMsg.getTimestamp());
            this.initializePanel();
            return true;
        }
        return false;
    }

    /**
     * Creates edit button for inline and message-list fields, creating a new instance of @IMCFieldsPanel.
     * @param messagesComboBox
     * @param b
     * @param inlineMsg
     * @return
     */
    private JButton getEditButtonForMsg(String field, JComboBox<String> messagesComboBox, boolean b, IMCMessage m, JButton removal) {
        JButton edit = new JButton(I18n.text("Edit"));
        edit.addActionListener(getActionForMsg(field, messagesComboBox, b, m, removal));
        return edit;
    }

    /***
     * Creates the action for each edit @JButton associated to the message selected on the @JComboBox.
     * This button can be associated to a inline message field or a message-list field.
     * @param field - the field name to insert the @IMCMessage.
     * @param messagesComboBox - the @JComboBox used to select the @IMCMessage to edit.
     * @param msgList - if the buttons is used in a message-list field or not
     * @param m - initial message to fill the panel
     * @param removal - associated button to remove this message from the message-list, can be null.
     * @return - the action.
     */
    private ActionListener getActionForMsg(String field, JComboBox<String> messagesComboBox, boolean msgList,
            IMCMessage m, JButton removal) {
        // Add new action listener
        ActionListener action = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msgName = (String) messagesComboBox.getSelectedItem();
                if (msgName.equals("None")) {
                    return;
                }
                else {
                    IMCMessage init = IMCDefinition.getInstance().create(msgName);
                    if (!msgList) {
                        init = IMCFieldsPanel.this.inlineMsgs.get(field);
                        if (init != null) {
                            if (init.getAbbrev() != msgName) {
                                init = IMCDefinition.getInstance().create(msgName);
                            }
                        }
                    }
                    else if (msgList && m != null) {
                        if (msgName.equals(m.getAbbrev()))
                            init = m;
                        else {
                            boolean hasField = IMCFieldsPanel.this.msgList.get(field) != null;
                            if (hasField) {
                                IMCFieldsPanel.this.msgList.get(field).remove(m);
                                int index = IMCFieldsPanel.this.msgList.get(field).indexOf(m);
                                if(index >= 0)
                                    IMCFieldsPanel.this.msgList.get(field).add(index,init);
                                else
                                    IMCFieldsPanel.this.msgList.get(field).add(init);
                            }
                        }
                    }
                    IMCFieldsPanel inceptionFields = new IMCFieldsPanel(IMCFieldsPanel.this, msgName, init);
                    JPanel panelCeption = inceptionFields.getContents();
                    IMCFieldsPanel.this.inlineMsgs.put(field, init);
                    JDialog dg = new JDialog(SwingUtilities.getWindowAncestor(IMCFieldsPanel.this.getContents()),
                            ModalityType.DOCUMENT_MODAL);

                    JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
                    JButton validate = getValidateButtonFor(inceptionFields);
                    JButton insert = getInsertButtonFor(inceptionFields, field, validate, msgList, m);

                    ImcCopyPastePanel msgCopyPastePanel = getMsgCopyPastePanel(dg, panelCeption, field, init, msgList, messagesComboBox, (JButton) e.getSource(), removal);
                    LocationCopyPastePanel locCopyPastePanel = getLocCopyPastPanel(inceptionFields, dg, panelCeption);

                    buttons.add(msgCopyPastePanel);
                    buttons.add(locCopyPastePanel);
                    buttons.add(validate);
                    buttons.add(insert);
                    buttons.setSize(new Dimension(400, 20));
                    panelCeption.add(buttons, BorderLayout.SOUTH, -1);
                    // panelCeption.add(buttons, BorderLayout.SOUTH);
                    dg.setTitle("Edit Inline IMC Message - " + msgName);
                    dg.setContentPane(panelCeption);
                    dg.setSize(500, 500);
                    dg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    dg.getRootPane().registerKeyboardAction(ev -> { dg.dispose(); },
                            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
                    GuiUtils.centerParent(dg, SwingUtilities.getWindowAncestor(IMCFieldsPanel.this.getContents()));
                    dg.setVisible(true);
                }
            }
        };
        return action;
    }

    /**
     * @param inceptionFields
     * @param m 
     * @return
     */
    protected JButton getInsertButtonFor(IMCFieldsPanel inceptionFields, String field, JButton validate, boolean msgList, IMCMessage m) {
        JButton i = new JButton(I18n.text("Insert"));
        i.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                validate.doClick();
                IMCMessage value = inceptionFields.getImcMessage();
                if (!msgList) {
                    IMCFieldsPanel.this.inlineMsgs.put(field, value);
                }
                else if (msgList && m != null) {
                    boolean hasField = IMCFieldsPanel.this.msgList.get(field) != null;
                    if (!hasField)
                        IMCFieldsPanel.this.msgList.put(field, new ArrayList<IMCMessage>());
                    int index = IMCFieldsPanel.this.msgList.get(field).indexOf(m);
                    IMCFieldsPanel.this.msgList.get(field).remove(m);
                    m.setValues(value.getValues());
                 // add IMCMessage with updated fields
                    if(index >= 0)
                        IMCFieldsPanel.this.msgList.get(field).add(index, m);
                    else
                        IMCFieldsPanel.this.msgList.get(field).add(m);
                }
                JComponent comp = (JComponent) e.getSource();
                Window win = SwingUtilities.getWindowAncestor(comp);
                win.dispose();

            }
        });
        return i;
    }

    /**
     * @param inceptionFields
     * @return
     */
    protected JButton getValidateButtonFor(IMCFieldsPanel inceptionFields) {
        JButton v = new JButton(I18n.text("Validate"));

        v.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    IMCMessage imcMessage = inceptionFields.getImcMessage();
                    imcMessage.validate();
                    if (inceptionFields.parent != null)
                        JOptionPane.showMessageDialog(inceptionFields.parent.getContents(),
                                "Message "+ imcMessage.getAbbrev() + " parsed successfully.", "Validate message", JOptionPane.INFORMATION_MESSAGE);
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                    if (inceptionFields.parent != null)
                        UIUtils.exceptionDialog(inceptionFields.parent.getContents(), ex, "Error parsing message",
                                "Validate message");
                    return;
                }
            }
        });
        return v;

    }

    /***
     * 
     * @param field
     * @param inlineMsgPanel
     * @param more
     * @param mListComboBox
     * @return
     */
    private ActionListener getMsgListAction(String field, JPanel inlineMsgPanel, JButton more,
            JComboBox<String> mListComboBox) {
        ActionListener action = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String selectedItem = (String) mListComboBox.getSelectedItem();
                if (!selectedItem.equalsIgnoreCase("None")) {
                    IMCMessage newMsg = IMCDefinition.getInstance().create(selectedItem);
                    JPanel msgHolder = new JPanel(new FlowLayout(FlowLayout.LEFT));
                    inlineMsgPanel.setLayout(new BoxLayout(inlineMsgPanel, BoxLayout.Y_AXIS));
                    // dynamic insertion all over again
                    JComboBox<String> comboCeption = getMessageComboBox(field, true);
                    comboCeption.setSelectedItem(selectedItem);
                    JPanel c = (JPanel) inlineMsgPanel.getComponent(0);
                    JButton minus = new JButton(" - ");
                    ActionListener removalAction = getRemovalAction(field, inlineMsgPanel, newMsg, c);
                    ActionListener logicRemoval  = getLogicRemovalAction(field, newMsg);
                    minus.addActionListener(removalAction);
                    minus.addActionListener(logicRemoval);
                    JButton edit = getEditButtonForMsg(field, mListComboBox, true, newMsg, minus);
                    more.removeActionListener(this);
                    more.addActionListener(getMsgListAction(field, inlineMsgPanel, more, comboCeption));
                    msgHolder.add(comboCeption);
                    msgHolder.add(more);
                    c.remove(more);
                    c.add(edit);
                    c.add(minus);
                    mListComboBox.setEnabled(false);
                    // Add this message to Map
                    if (IMCFieldsPanel.this.msgList.get(field) == null)
                        IMCFieldsPanel.this.msgList.put(field, new ArrayList<IMCMessage>());
                    // getMessage from panel or update it
                    IMCFieldsPanel.this.msgList.get(field).add(newMsg);
                    inlineMsgPanel.add(msgHolder, 0);
                    inlineMsgPanel.revalidate();
                    inlineMsgPanel.repaint();
                }
            }
        };

        return action;
    }
    
    /**
     * Removes the message in the message-list field from the internal data structures from {@code} this class
     * @param field
     * @param imcMessage
     */
    private void removeFromMsgList(String field, IMCMessage imcMessage) {
        if (IMCFieldsPanel.this.msgList.get(field) != null) {
            IMCFieldsPanel.this.msgList.get(field).remove(imcMessage);
        }
    }

    /**
     * @param field 
     * @return
     */
    protected ActionListener getLogicRemovalAction(String field, IMCMessage imcMessage) {
        ActionListener action = new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                removeFromMsgList(field, imcMessage);
            }
        };

        return action;
    }

    /**
     * Get Removal button for each message inserted into the message-list panel
     * @param field
     * @param inlineMsgPanel
     * @param newMsg
     * @param c
     * @return
     */
    private ActionListener getRemovalAction(String field, JPanel inlineMsgPanel, IMCMessage newMsg, JPanel c) {
        return new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                inlineMsgPanel.remove(c);
                inlineMsgPanel.revalidate();
                inlineMsgPanel.repaint();

            }
        };
    }
    
    /**
     * @param msg
     * @return
     */
    private List<String> getMessagesFilter(IMCMessage msg, String field) {
        List<String> result = new ArrayList<>();
        String message_group = msg.getIMCMessageType().getFieldSubtype(field);
        for (String mt : IMCDefinition.getInstance().getMessageNames()) {
            IMCMessage mtg = IMCDefinition.getInstance().create(mt);
            if (message_group != null) {
                if (message_group.equals(mt)) { // NODE
                    result.add(mt);
                }
                else if (mtg.getIMCMessageType().getSupertype() != null) {

                    if (mtg.getIMCMessageType().getSupertype() != null) {
                        if (mtg.getIMCMessageType().getSupertype().getShortName().equals(message_group)) {
                            result.add(mt);
                        }
                    }
                }
            }
            else // If no constraints are imposed in the inline messages type
                result.add(mt);

        }
        return result;
    }

    public IMCMessage getImcMessage() {
        fillHeader();
        this.msg.setHeader(this.header);
        this.msg.setTimestampMillis(System.currentTimeMillis());
        if (!this.bitfields.isEmpty()) {
            for (Entry<String, BitmaskPanel> entry : this.bitfields.entrySet())
                this.msg.setValue(entry.getKey(), entry.getValue().getValue());
        }
        if (!this.inlineMsgs.isEmpty()) {
            for (Entry<String, IMCMessage> entry : this.inlineMsgs.entrySet())
                this.msg.setValue(entry.getKey(), entry.getValue());
        }

        for (Entry<String, List<IMCMessage>> msgs : this.msgList.entrySet()) {
            if (msgs.getValue() != null)
                this.msg.setValue(msgs.getKey(), msgs.getValue());
        }
        try {
            this.msg.validate();
            return this.msg;
        }
        catch (Exception e) {
            e.printStackTrace();
            UIUtils.exceptionDialog(IMCFieldsPanel.this.getContents(), e, "Error parsing message", "Validate message");
            return null;
        }
    }

    /**
     * @return
     */
    public String getMessageName() {
        return IMCDefinition.getInstance().getMessageName(mid);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("IMC Fields Tab");
        JPanel content = new IMCFieldsPanel(null, "TransmissionRequest", null).getContents();
        frame.setSize(500, 500);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setContentPane(content);
        frame.setVisible(true);
    }
}
