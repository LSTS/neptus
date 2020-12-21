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
 * Created in 2006/12/08
 * Reworked in 2008/12/01
 */
package pt.lsts.neptus.comm.manager.imc;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.SystemColor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.gui.ImcStatePanel;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.BaseAbstractCommsMonitorPanel;
import pt.lsts.neptus.comm.manager.MessageFrequencyCalculator;
import pt.lsts.neptus.console.plugins.SystemsList;
import pt.lsts.neptus.console.plugins.planning.plandb.PlanDBInfo;
import pt.lsts.neptus.gui.editor.ImcId16Editor;
import pt.lsts.neptus.gui.swing.JRoundButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.messages.MessageFilter;
import pt.lsts.neptus.messages.listener.MessageInfo;
import pt.lsts.neptus.messages.listener.MessageListener;
import pt.lsts.neptus.mystate.MyState;
import pt.lsts.neptus.types.coord.CoordinateUtil;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehicleType.SystemTypeEnum;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ByteUtil;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.MathMiscUtils;
import pt.lsts.neptus.util.StringUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;

/**
 * @author Paulo Dias
 */
public class MonitorIMCComms extends
        BaseAbstractCommsMonitorPanel<ImcMsgManager, SystemImcMsgCommInfo, IMCMessage, MessageInfo, ImcId16> {
    private static final long serialVersionUID = -7594653589671178640L;

    public static final ImageIcon ICON_ON = new ImageIcon(ImageUtils.getImage("images/imc.png").getScaledInstance(16,
            16, Image.SCALE_SMOOTH));
    public static final ImageIcon ICON_OFF = new ImageIcon(ImageUtils.getImage("images/buttons/noimc.png")
            .getScaledInstance(16, 16, Image.SCALE_SMOOTH));

    private JPanel addHolderSystemCommPanel = null;

    private JPanel addSystemCommPanel = null;
    private JPanel addNewSystemTextFieldPanel = null;
    private JRoundButton addNewSystemRoundButton = null;
    private ImcId16Editor imcId16Editor = null;

    private JPanel addVehicleCommPanel = null;
    private JTextField addNewVehTextField = null;
    private JRoundButton addNewVehRoundButton = null;

    private JPanel addConfigSystemPanel = null;
    private JPanel addImcLocalInfoPanel = null;
    private JLabel localInfoLabel = null;

    private JLabel configLabelTop = null;
    private JLabel configLabelMiddle = null;
    private JPanel configSystemControlPanel = null;

    private ImcSystemState commonSystemState = null;
    private ImcStatePanel commonSystemStatePanel = null;
    private JPanel addCommonImcMsgPanel = null;
    private JLabel commonImcMsgInfoLabel = null;
    private JScrollPane commonImcMsgScrollPane = null;

    private ImcSystemState selSystemState = null;
    private ImcStatePanel selSystemStatePanel = null;
    private JPanel addSystemImcMsgPanel = null;
    private JLabel systemImcMsgInfoLabel = null;
    private JScrollPane systemImcMsgScrollPane = null;

    private SystemsList systemsListPanel = null;

    private String imcCCUName;

    public MonitorIMCComms(ImcMsgManager imcMsgManager) {
        super(imcMsgManager);
        initialize();
    }

    private void initialize() {
        // addNewActivateCommPanel(getAddSystemCommPanel());
        addNewActivateCommPanel(getAddHolderSystemCommPanel());
        // addTopTab("IMC UDP Sender", ICON_ON, new ImcMessageSenderPanel(), null);
        // addMonitorTab("IMC UDP Sender", ICON_ON, new ImcMessageSenderPanel(), null);
        
        addMonitorTab(I18n.text("All Messages"), null, getAddCommonImcMsgPanel(), null);
        addMonitorTab(I18n.text("System Messages"), null, getAddSystemImcMsgPanel(), null);
        
        addMonitorTab(I18n.text("System Configurations"), null, getAddConfigSystemPanel(), null);
        addMonitorTab(I18n.text("Local Info"), null, getAddImcLocalInfoPanel(), null);
        addMonitorTab(I18n.text("Systems List"), null, getAddSystemsListPanel(), null);

//        imcCCUName = StringUtils.toImcName(GeneralPreferences.getProperty(GeneralPreferences.IMC_CCU_NAME));
        imcCCUName = StringUtils.toImcName(GeneralPreferences.imcCcuName);
    }

    private JPanel getAddCommonImcMsgPanel() {
        if (addCommonImcMsgPanel == null) {
            commonImcMsgInfoLabel = new JLabel();
            commonImcMsgInfoLabel.setText(I18n.text("All messages"));
            commonImcMsgInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            addCommonImcMsgPanel = new JPanel();
            addCommonImcMsgPanel.setLayout(new BorderLayout());
            commonImcMsgScrollPane = new JScrollPane();
            addCommonImcMsgPanel.add(commonImcMsgScrollPane, BorderLayout.CENTER);
            addCommonImcMsgPanel.add(commonImcMsgInfoLabel, BorderLayout.NORTH);
        }
        return addCommonImcMsgPanel;
    }

    private void updateCommonImcMsg() {
        ImcSystemState st = getCommManager().getImcState();
        if (st != commonSystemState) {
            if (commonSystemState != null)
                commonSystemState = null;
            if (commonSystemStatePanel != null) {
                commonSystemStatePanel.cleanup();
                commonSystemStatePanel = null;
            }

            commonSystemState = st;
            commonSystemStatePanel = new ImcStatePanel(commonSystemState);
            commonImcMsgScrollPane.setViewportView(commonSystemStatePanel);
        }
    }

    private JPanel getAddSystemImcMsgPanel() {
        if (addSystemImcMsgPanel == null) {
            systemImcMsgInfoLabel = new JLabel();
            systemImcMsgInfoLabel.setText(I18n.text("No system selected"));
            systemImcMsgInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);
            addSystemImcMsgPanel = new JPanel();
            addSystemImcMsgPanel.setLayout(new BorderLayout());
            systemImcMsgScrollPane = new JScrollPane();
            addSystemImcMsgPanel.add(systemImcMsgScrollPane, BorderLayout.CENTER);
            addSystemImcMsgPanel.add(systemImcMsgInfoLabel, BorderLayout.NORTH);
        }
        return addSystemImcMsgPanel;
    }

    private void updateSystemImcMsg() {
        if (selectedSystem == null)
            return;
        ImcId16 idImc = selectedSystem;
        if (idImc == null) {
            systemImcMsgInfoLabel.setText(I18n.text("No system selected"));
            systemImcMsgScrollPane.setViewportView(new JLabel());
            if (selSystemState != null)
                selSystemState = null;
            if (selSystemStatePanel != null) {
                selSystemStatePanel.cleanup();
                selSystemStatePanel = null;
            }
            return;
        }
            
//        changeSystemTree(((SystemImcMsgCommInfo)vci).getImcState());
        systemImcMsgInfoLabel.setText(I18n.text("System") + ": " + translateSystemIdToName(selectedSystem) + " ["
                + translateIdToStringId(selectedSystem) + "]" /* selectedSystem.toString() */);
 
        SystemImcMsgCommInfo ci = getCommManager().getCommInfoById(idImc);
        if (ci != null) {
            ImcSystemState st = ((SystemImcMsgCommInfo)ci).getImcState();
            if (st != selSystemState) {
                if (selSystemState != null)
                    selSystemState = null;
                if (selSystemStatePanel != null) {
                    selSystemStatePanel.cleanup();
                    selSystemStatePanel = null;
                }
                
                selSystemState = st;
                selSystemStatePanel = new ImcStatePanel(selSystemState);
                systemImcMsgScrollPane.setViewportView(selSystemStatePanel);
            }
        }
    }

    /**
     * @return the addImcLocalInfoPanel
     */
    private JPanel getAddImcLocalInfoPanel() {
        if (addImcLocalInfoPanel == null) {
            addImcLocalInfoPanel = new JPanel();
            addImcLocalInfoPanel.setLayout(new BorderLayout(20, 20));
            localInfoLabel = new JLabel();
            localInfoLabel.setVerticalAlignment(SwingConstants.TOP);
            JScrollPane jsp = new JScrollPane(localInfoLabel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jsp.setBorder(null);
            addImcLocalInfoPanel.add(jsp);
            addImcLocalInfoPanel.add(Box.createHorizontalGlue(), BorderLayout.WEST);
            addImcLocalInfoPanel.add(Box.createHorizontalGlue(), BorderLayout.EAST);
            addImcLocalInfoPanel.add(Box.createVerticalGlue(), BorderLayout.NORTH);
            addImcLocalInfoPanel.add(Box.createVerticalGlue(), BorderLayout.SOUTH);
        }
        return addImcLocalInfoPanel;
    }

    /**
     * @return
     */
    @SuppressWarnings("serial")
    private JPanel getAddConfigSystemPanel() {
        if (addConfigSystemPanel == null) {
            addConfigSystemPanel = new JPanel();
            addConfigSystemPanel.setLayout(new BorderLayout(20, 20));
            configLabelTop = new JLabel();
            configLabelTop.setText(I18n.text("No system selected"));
            configLabelTop.setHorizontalAlignment(SwingConstants.CENTER);
            configLabelTop.setVerticalAlignment(SwingConstants.TOP);

            configLabelMiddle = new JLabel();
            configLabelMiddle.setVerticalAlignment(SwingConstants.TOP);
            JScrollPane jsp = new JScrollPane(configLabelMiddle, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                    JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            jsp.setBorder(null);

            configSystemControlPanel = new JPanel();
            configSystemControlPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            configSystemControlPanel.add(new JButton(new AbstractAction(I18n.text("Clear Stored Data")) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (selectedSystem == null)
                        return;

                    ImcSystem sys = ImcSystemsHolder.lookupSystem(selectedSystem);
                    if (sys != null)
                        sys.clearStoredData();
                }
            }));

            addConfigSystemPanel.add(jsp);
            addConfigSystemPanel.add(Box.createHorizontalGlue(), BorderLayout.WEST);
            addConfigSystemPanel.add(Box.createHorizontalGlue(), BorderLayout.EAST);
            addConfigSystemPanel.add(configLabelTop, BorderLayout.NORTH);
            addConfigSystemPanel.add(configSystemControlPanel, BorderLayout.SOUTH);
        }
        return addConfigSystemPanel;
    }

    private void updateLocalConfigPanel() {
        ImcId16 idImc = getCommManager().getLocalId();

        String ret = "<html>";
        ret += "<b>" + I18n.text("Name:") + "</b> " + imcCCUName + "<br>";
        ret += "<b>" + I18n.text("ID:") + "</b> " + idImc.toPrettyString();
        if (getCommManager().is2IdErrorMode())
            ret += " <b color='red'>" + I18n.text("Another node with this ID detected!") + "</b>";
        ret += "<br>";
        ret += "<b>" + I18n.text("Services:") + "</b> "
                + getCommManager().getAllServicesString().replaceAll(";", ";<br>") + "<br><br>";

        ret += "<b>" + I18n.text("Comms info:") + "</b><br>" + getCommManager().getCommStatusAsHtmlFragment();

        if (System.currentTimeMillis() - getCommManager().getAnnounceLastArriveTime() > DateTimeUtil.MINUTE * 5) {
            ret += "<b color='red'>" + I18n.text("Announce not arriving for some time") + "</b><br>";
        }

        ret += "<br>";
        double freqS = getCommManager().getToSendMessagesFreqCalc().getMessageFreq();
        long countM = getCommManager().getToSendMessagesFreqCalc().getMsgCount();
        ret += "<b>" + I18n.text("To Send messages frequency:") + " </b> "
                + (freqS != -1 ? MathMiscUtils.parseToEngineeringNotation(freqS, 1) : "-") + " Hz" + " ("
                + I18n.textf("%number messages", countM) + ")" + "<br>";
        freqS = getCommManager().getSentMessagesFreqCalc().getMessageFreq();
        countM = getCommManager().getSentMessagesFreqCalc().getMsgCount();
        ret += "<b>" + I18n.text("Sent messages frequency:") + " </b> "
                + (freqS != -1 ? MathMiscUtils.parseToEngineeringNotation(freqS, 1) : "-") + " Hz" + " ("
                + I18n.textf("%number messages", countM) + ")" + "<br>";

        ret += "<br><b>" + I18n.text("Location:") + "</b> ";
        if (MyState.getLocation().isLocationEqual(LocationType.ABSOLUTE_ZERO))
            ret += "<span color='red'>" + I18n.text("Unknown location") + "</span><br>";
        else if (System.currentTimeMillis() - MyState.getLastLocationUpdateTimeMillis() > DateTimeUtil.SECOND * 10) {
            ret += "<span color='red'>"
                    + CoordinateUtil.latitudeAsPrettyString(
                            MathMiscUtils.round(MyState.getLocation().getLatitudeDegs(), 6))
                    + " "
                    + CoordinateUtil.longitudeAsPrettyString(
                            MathMiscUtils.round(MyState.getLocation().getLongitudeDegs(), 6))
                    + "</span><br>";
        }
        else
            ret += CoordinateUtil.latitudeAsPrettyString(
                    MathMiscUtils.round(MyState.getLocation().getLatitudeDegs(), 6))
                    + " "
                    + CoordinateUtil.longitudeAsPrettyString(
                            MathMiscUtils.round(MyState.getLocation().getLongitudeDegs(), 6)) + "<br>";

        ret += "<br><b>" + I18n.text("State Listeners:") + "</b><br>";
        ret += getCommManager().getStatusListenersAsHtmlFragment();
        ret += "<br><br><b>" + I18n.text("Root Messages Listeners:") + "</b><br>";
        ret += getCommManager().getListenersAsHtmlFragment();

        final String txtR = ret;
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    localInfoLabel.setText(txtR);
                }
            });
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getStackTrace());
        }
    }

    private void updateConfigPanel() {
        if (selectedSystem == null /* || "".equals(selectedSystem) */)
            return;
        ImcId16 idImc = selectedSystem; // translateStringIdToId(selectedSystem);
        if (idImc == null) {
            configLabelTop.setText(I18n.text("No system selected"));
            configLabelMiddle.setText("");
            configSystemControlPanel.setVisible(false);
            for (Component comp : configSystemControlPanel.getComponents()) {
                comp.setEnabled(false);
            }
            return;
        }

        ImcSystem sys = ImcSystemsHolder.lookupSystem(idImc);
        if (sys == null) {
            configLabelTop.setText(I18n.text("No system found"));
            configLabelMiddle.setText("");
            configSystemControlPanel.setVisible(false);
            for (Component comp : configSystemControlPanel.getComponents()) {
                comp.setEnabled(false);
            }
            return;
        }

        configSystemControlPanel.setVisible(true);
        for (Component comp : configSystemControlPanel.getComponents()) {
            comp.setEnabled(true);
        }

        configLabelTop.setText("<html><b>" + I18n.text("Name:") + " " + sys.getName() + "</b><br>" + "<b>"
                + I18n.text("ID:") + "</b> " + sys.getId().toPrettyString());

        String ret = "<html>";
        // ret += "<b>Name: " + sys.getName() + "</b><br>";
        // ret += "<b>ID:</b> " + sys.getId().toPrettyString() + "<br>";
        ret += "<b>" + I18n.text("Type:") + "</b> " + sys.getType() + "<br>";
        ret += "<b>" + I18n.text("IP:") + "</b> " + sys.getHostAddress() + "<br>";
        ret += "<b>" + I18n.text("UDP:") + "</b> " + (sys.isUDPOn() ? sys.getRemoteUDPPort() : "-") + "<br>";
        String tcpConnection = sys.isTCPOn() ? (getCommManager().isTCPConnectionEstablished(sys.getHostAddress(),
                sys.getRemoteTCPPort()) ? " (" + I18n.text("connected") + ")" : " (" + I18n.text("not connected") + ")")
                : "";
        ret += "<b>" + I18n.text("TCP:") + "</b> " + (sys.isTCPOn() ? sys.getRemoteTCPPort() + tcpConnection : "-")
                + "<br>";
        ret += "<b>" + I18n.text("Services:") + "</b> " + sys.getServicesProvided().replaceAll(";", ";<br>") + "<br>";

        ret += "<br>";
        MessageFrequencyCalculator mfc = getCommManager().getToSendMessagesFreqCalc(sys.getId());
        double freqS = mfc == null ? -1 : mfc.getMessageFreq();
        long countM = mfc == null ? 0 : mfc.getMsgCount();
        ret += "<b>" + I18n.text("To Send messages frequency:") + " </b> "
                + (freqS != -1 ? MathMiscUtils.parseToEngineeringNotation(freqS, 1) : "-") + " Hz" + " ("
                + I18n.textf("%number messages", countM) + ")" + "<br>";
        mfc = getCommManager().getSentMessagesFreqCalc(sys.getId());
        freqS = mfc == null ? -1 : mfc.getMessageFreq();
        countM = mfc == null ? 0 : mfc.getMsgCount();
        ret += "<b>" + I18n.text("Sent messages frequency:") + " </b> "
                + (freqS != -1 ? MathMiscUtils.parseToEngineeringNotation(freqS, 1) : "-") + " Hz" + " ("
                + I18n.textf("%number messages", countM) + ")" + "<br>";

        ret += "<br><b>" + I18n.text("Location:") + "</b> ";
        if (sys.getLocation().isLocationEqual(LocationType.ABSOLUTE_ZERO))
            ret += "<span color='red'>" + I18n.text("Unknown location") + "</span><br>";
        else if (System.currentTimeMillis() - sys.getLocationTimeMillis() > DateTimeUtil.SECOND * 10) {
            ret += "<span color='red'>"
                    + CoordinateUtil.latitudeAsPrettyString(
                            MathMiscUtils.round(sys.getLocation().getLatitudeDegs(), 6))
                    + " "
                    + CoordinateUtil.longitudeAsPrettyString(
                            MathMiscUtils.round(sys.getLocation().getLongitudeDegs(), 6))
                    + "</span><br>";
        }
        else
            ret += CoordinateUtil.latitudeAsPrettyString(
                    MathMiscUtils.round(sys.getLocation().getLatitudeDegs(), 6))
                    + " "
                    + CoordinateUtil.longitudeAsPrettyString(
                            MathMiscUtils.round(sys.getLocation().getLongitudeDegs(), 6)) + "<br>";

        ret += "<br><b>" + I18n.text("Entities:") + "</b>";
        Map<Integer, String> entLst = EntitiesResolver.getEntities(sys.getName());
        String txtEt = "";
        if (entLst != null) {
            boolean startEt = true;
            Integer[] es = entLst.keySet().toArray(new Integer[0]);
            for (Integer id : es) {
                if (startEt) {
                    txtEt += "<ul>";
                    startEt = false;
                }
                txtEt += "<li>" + id + " = " + entLst.get(id) + "</li>";
            }
            if (!startEt)
                txtEt += "</ul>";
            else
                txtEt += "<b color='red'> " + I18n.text("no info yet") + "</b>";
        }
        else
            txtEt += "<b color='red'> " + I18n.text("no info yet") + "</b>";
        txtEt += "<br>";
        ret += txtEt;

        ret += "<br><b>" + I18n.text("Stored Data:") + "</b><br>";
        for (String key : sys.getDataStorageKeys()) {
            Object obj = sys.retrieveData(key);
            long timeMillis = sys.retrieveDataTimeMillis(key);
            String age = timeMillis < 0 ? "" : " (\u2206t "
                    + DateTimeUtil.milliSecondsToFormatedString(System.currentTimeMillis() - timeMillis) + ")";
            String txt = "<p>";
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) obj;
                txt += "<i>" + key + age + "</i>:";
                boolean start = true;
                String[] ks = map.keySet().toArray(new String[0]);
                for (String keyItem : ks) {
                    if (keyItem == null)
                        continue;
                    if (start) {
                        txt += "<ul>";
                        start = false;
                    }
                    txt += "<li>" + keyItem + " = " + map.get(keyItem) + "</li>";
                    // txt += "<li>" + map.hashCode() + "</li>";
                }
                if (!start)
                    txt += "</ul>";
                txt += "<br>";
            }
            catch (Exception e) {
                txt += "<i>" + key + age + "</i> = " + obj + "<br>";
            }
            ret += txt;
            // ret += "<i>" + key + age + "</i> = " + obj + "<br>";
        }

        ret += "<br><b>" + I18n.text("Stored Plans:") + "</b><br>";
        String txt = "";
        try {
            Map<String, PlanDBInfo> sp = sys.getPlanDBControl().getRemoteState().getStoredPlans();
            boolean start = true;
            for (String planId : sp.keySet()) {
                if (start) {
                    txt += "<ul>";
                    start = false;
                }
                PlanDBInfo pdbi = sp.get(planId);
                txt += "<li>" + /* planId + " = " + */pdbi + " [" + I18n.text("MD5:") + " "
                        + ByteUtil.encodeAsString(pdbi.getMd5()) + "]</li>";
            }
            if (!start)
                txt += "</ul>";
        }
        catch (Exception e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        txt += "<br>";
        ret += txt;

        ret += "<br><b>" + I18n.text("Listeners:") + "</b><br>";
        ret += getCommManager().getListenersAsHtmlFragment(idImc);

        final String txtR = ret;
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    configLabelMiddle.setText(txtR);
                }
            });
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getStackTrace());
            // e.printStackTrace();
        }
    }

    private SystemsList getAddSystemsListPanel() {
        if (systemsListPanel == null) {
            systemsListPanel = new SystemsList(null);
            systemsListPanel.systemsFilter = SystemTypeEnum.ALL;
            systemsListPanel.setEnableSelection(false);
            systemsListPanel.setViewEnable(false);
            systemsListPanel.setIconsSize(20);
            systemsListPanel.setIndicatorsSize(20);
        }
        return systemsListPanel;
    }

    long time = -1;

    private void updateSystemsList() {
        if (systemsListPanel == null || !systemsListPanel.isShowing())
            return;
        long t1 = System.currentTimeMillis();
        if (t1 - time >= 600) {
            systemsListPanel.update();
            time = t1;
        }
    }

    @Override
    protected String getCommName() {
        return "IMC";
    }

    @Override
    public ImageIcon getOnIcon() {
        return ICON_ON;
    }

    @Override
    public ImageIcon getOffIcon() {
        return ICON_OFF;
    }

    @Override
    protected String translateSystemIdToName(ImcId16 id) {
        // FIXME verificar se não for veículo
        // return VehiclesHolder.getVehicleById(id).getName();
        VehicleType vehTmp = VehiclesHolder.getVehicleWithImc(id);
        ImcSystem sysImc = ImcSystemsHolder.lookupSystem(id);
        if (vehTmp != null)
            return vehTmp.getId();
        else if (sysImc != null)
            return sysImc.getName();
        return id.toPrettyString();
    }

    @Override
    protected ImcId16 translateStringIdToId(String id) {
        try {
            return ImcId16.valueOf(id);
        }
        catch (NumberFormatException e) {
            NeptusLog.pub().error(e.getMessage());
            return null;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.util.comm.manager.BaseAbstractCommsMonitorPanel#translateIdToStringId(java.lang.Object)
     */
    @Override
    protected String translateIdToStringId(ImcId16 id) {
        return id.toPrettyString();
    }

    /**
     * @return the imcId16Editor
     */
    private ImcId16Editor getImcId16Editor() {
        if (imcId16Editor == null) {
            imcId16Editor = new ImcId16Editor();
        }
        return imcId16Editor;
    }

    /**
     * This method initializes addNewVehTextField
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getAddNewSystemTextField() {
        if (addNewSystemTextFieldPanel == null) {
            addNewSystemTextFieldPanel = getImcId16Editor().imcId16Editor;
            addNewSystemTextFieldPanel.setPreferredSize(new Dimension(150, 19));
            addNewSystemTextFieldPanel.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        getAddNewSystemRoundButton().doClick(50);
                    }
                }
            });
        }
        return addNewSystemTextFieldPanel;
    }

    private JTextField getAddNewVehTextField() {
        if (addNewVehTextField == null) {
            addNewVehTextField = new JTextField();
            addNewVehTextField.setPreferredSize(new Dimension(150, 19));
            addNewVehTextField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        getAddNewVehRoundButton().doClick(50);
                    }
                }
            });
        }
        return addNewVehTextField;
    }

    /**
     * This method initializes addNewVehRoundButton
     * 
     * @return pt.lsts.neptus.gui.swing.JRoundButton
     */
    private JRoundButton getAddNewSystemRoundButton() {
        if (addNewSystemRoundButton == null) {
            addNewSystemRoundButton = new JRoundButton();
            addNewSystemRoundButton.setText(I18n.text("Add new system"));
            addNewSystemRoundButton.setPreferredSize(new Dimension(142, 34));
            addNewSystemRoundButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    addNewSystemRoundButton.setEnabled(false);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            if (!getCommManager().isRunning()) {
                                JOptionPane jop = new JOptionPane(I18n.text("IMC comm. manager is not running yet!"),
                                        JOptionPane.WARNING_MESSAGE);
                                JDialog dialog = jop.createDialog(MonitorIMCComms.this,
                                        I18n.text("Setup new system comms."));
                                dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                                dialog.setVisible(true);

                                return null;
                            }
                            ImcId16 newVehId = (ImcId16) getImcId16Editor().getValue();
                            getCommManager().initSystemCommInfo(newVehId, "");
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            addNewSystemRoundButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return addNewSystemRoundButton;
    }

    private JRoundButton getAddNewVehRoundButton() {
        if (addNewVehRoundButton == null) {
            addNewVehRoundButton = new JRoundButton();
            addNewVehRoundButton.setText(I18n.text("Add new vehicle"));
            addNewVehRoundButton.setPreferredSize(new Dimension(142, 34));
            addNewVehRoundButton.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    addNewVehRoundButton.setEnabled(false);
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            if (!getCommManager().isRunning()) {
                                JOptionPane jop = new JOptionPane(I18n.text("IMC comm. manager is not running yet!"),
                                        JOptionPane.WARNING_MESSAGE);
                                JDialog dialog = jop.createDialog(MonitorIMCComms.this,
                                        I18n.text("Setup new vehicle comms."));
                                dialog.setModalityType(ModalityType.DOCUMENT_MODAL);
                                dialog.setVisible(true);

                                return null;
                            }
                            String newVehId = getAddNewVehTextField().getText();
                            getCommManager().initVehicleCommInfo(newVehId, "");
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            addNewVehRoundButton.setEnabled(true);
                        }
                    };
                    worker.execute();
                }
            });
        }
        return addNewVehRoundButton;
    }

    private JPanel getAddHolderSystemCommPanel() {
        if (addHolderSystemCommPanel == null) {
            addHolderSystemCommPanel = new JPanel();
            GroupLayout layout = new GroupLayout(addHolderSystemCommPanel);
            addHolderSystemCommPanel.setLayout(layout);
            // addHolderSystemCommPanel.add(getAddSystemCommPanel(), null);
            // addHolderSystemCommPanel.add(getAddVehicleCommPanel(), null);
            layout.setAutoCreateGaps(true);
            layout.setAutoCreateContainerGaps(true);
            layout.setHorizontalGroup(layout.createSequentialGroup().addComponent(getAddSystemCommPanel())
                    .addComponent(getAddVehicleCommPanel()));
            layout.setVerticalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(getAddSystemCommPanel()).addComponent(getAddVehicleCommPanel()));

        }
        return addHolderSystemCommPanel;
    }

    /**
     * This method initializes jPanel1
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getAddSystemCommPanel() {
        if (addSystemCommPanel == null) {
            addSystemCommPanel = new JPanel();
            addSystemCommPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            addSystemCommPanel.setBorder(BorderFactory.createLineBorder(SystemColor.windowBorder, 1));
            addSystemCommPanel.setSize(new Dimension(355, 42));
            addSystemCommPanel.setLocation(new Point(15, 284));
            addSystemCommPanel.add(getAddNewSystemTextField(), null);
            addSystemCommPanel.add(getAddNewSystemRoundButton(), null);
        }
        return addSystemCommPanel;
    }

    private JPanel getAddVehicleCommPanel() {
        if (addVehicleCommPanel == null) {
            addVehicleCommPanel = new JPanel();
            addVehicleCommPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
            addVehicleCommPanel.setBorder(BorderFactory.createLineBorder(SystemColor.windowBorder, 1));
            addVehicleCommPanel.setSize(new Dimension(355, 42));
            addVehicleCommPanel.setLocation(new Point(15, 284));
            addVehicleCommPanel.add(getAddNewVehTextField(), null);
            addVehicleCommPanel.add(getAddNewVehRoundButton(), null);
        }
        return addVehicleCommPanel;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.util.comm.manager.BaseAbstractCommsMonitorPanel#updateVehicleCommDataPeriodicCall()
     */
    @Override
    protected void updateVehicleCommDataPeriodicCall() {
        updateLocalConfigPanel();
        updateConfigPanel();
        updateSystemsList();
        updateCommonImcMsg();
        updateSystemImcMsg();
    }

    /**
     * @param args
     * @throws MiddlewareException
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();

        ImcMsgManager imcMMsgManager = ImcMsgManager.getManager();

        JFrame frame = GuiUtils.testFrame(new MonitorIMCComms(imcMMsgManager), "Monitor IMC Comms");
        frame.setIconImage(ICON_ON.getImage());
        frame.setSize(396, 411 + 55);

        imcMMsgManager.start();

        MessageListener<MessageInfo, IMCMessage> mwl = new MessageListener<MessageInfo, IMCMessage>() {

            @Override
            public void onMessage(MessageInfo arg0, IMCMessage msg) {
                if (msg.getMessageType().getShortName().equalsIgnoreCase("EntityState")) {
                    for (String k : msg.getBitmask("flags").keySet())
                        NeptusLog.pub().info("<###> "+k + " " + msg.getBitmask("flags").get(k));
                }
            }

            @Override
            public String toString() {
                return "teste";
            }
        };
        MessageFilter<MessageInfo, IMCMessage> filter = new MessageFilter<MessageInfo, IMCMessage>() {
            @Override
            public boolean isMessageToListen(MessageInfo info, IMCMessage msg) {
                return true;
            }
        };
        imcMMsgManager.addListener(mwl, new ImcId16(0x0016), filter);

        ImcMessageSenderPanel.getFrame();
    }
}
