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
 * Author: Paulo Dias
 * 23/06/2011
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.Color;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCDefinition;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.PlanControlState;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 * 
 */
@PluginDescription(name = "Plan Control State", author = "Paulo Dias", version = "0.7", documentation = "plan-control/plan-control.html#PlanControlState")
public class PlanControlStatePanel extends SimpleSubPanel implements MainVehicleChangeListener, IPeriodicUpdates,
        NeptusMessageListener {
    private static final long serialVersionUID = 1L;

    // GUI
    private JLabel stateValueLabel;
    private JLabel stateLabel;
    private JLabel planIdValueLabel;
    private JLabel planIdLabel;
    private JLabel nodeIdValueLabel;
    private JLabel nodeIdLabel;
    private JLabel outcomeTitleLabel;
    private JLabel outcomeLabel;

    private PlanControlState.STATE state;
    private String planId = "";
    // private long planStarTimeMllisUTC = -1;
    private String nodeId = "";
    private String lastOutcome = "<html><font color='0x666666'>" + I18n.text("N/A") + "</font>";
    private int nodeTypeImcId = -1;
    private long nodeStarTimeMillisUTC = -1;
    private long nodeEtaSec = -1;
    private long lastUpdated = -1;

    private final String[] messagesToObserve = new String[] { "PlanControlState" };

    public PlanControlStatePanel(ConsoleLayout console) {
        super(console);
        removeAll();
        initialize();
    }

    private void initialize() {
        setSize(200, 200);
        this.setLayout(new MigLayout("ins 0"));
        stateValueLabel = new JLabel();
        stateValueLabel.setText("");
        stateLabel = new JLabel();
        stateLabel.setText("<html><b>" + I18n.text("State") + ": ");

        this.add(stateLabel, "");
        this.add(stateValueLabel, "wrap");

        planIdValueLabel = new JLabel();
        planIdValueLabel.setText("");
        planIdLabel = new JLabel();
        planIdLabel.setText("<html><b>" + I18n.text("Plan") + ": ");

        this.add(planIdLabel);
        this.add(planIdValueLabel, "wrap");

        nodeIdValueLabel = new JLabel();
        nodeIdValueLabel.setText("");
        nodeIdLabel = new JLabel();
        // / This is a plan node, keep it in one word.
        nodeIdLabel.setText("<html><b>" + I18n.textc("Man.", "Maneuver") + ": ");

        this.add(nodeIdLabel);
        this.add(nodeIdValueLabel, "wrap");

        outcomeTitleLabel = new JLabel("<html><b>" + I18n.text("Outcome") + ": ");
        outcomeTitleLabel.setHorizontalAlignment(SwingConstants.LEADING);

        outcomeLabel = new JLabel(lastOutcome);
        this.add(outcomeTitleLabel);
        this.add(outcomeLabel, "wrap");
    }

    @Override
    public String[] getObservedMessages() {
        return messagesToObserve;
    }

    public void consume(PlanControlState message) {
        try {
            state = message.getState();
            planId = message.getPlanId();
            nodeId = message.getManId();
            nodeTypeImcId = message.getManType();
            nodeEtaSec = message.getManEta();
            double progress = -1;
            switch (message.getState()) {
                case READY:
                case BLOCKED:
                    outcomeTitleLabel.setText("<html><b>" + I18n.text("Outcome") + ": ");
                    switch (message.getLastOutcome()) {
                        case SUCCESS:
                            lastOutcome = "<html><b><font color='#00CC00'>" + I18n.text("Success") + "</font></b>";
                            break;
                        case FAILURE:
                            lastOutcome = "<html><b><font color='#CC0000'>" + I18n.text("Failure") + "</font></b>";
                            break;
                        default:
                            lastOutcome = "<html><font color='#666666'>" + I18n.text("N/A") + "</font>";
                            break;
                    }
                    break;                    
                case EXECUTING:
                    progress = message.getPlanProgress();
                case INITIALIZING:                        
                    outcomeTitleLabel.setText("<html><b>" + I18n.text("Progress") + ": ");
                    if (progress != -1)
                        lastOutcome = GuiUtils.getNeptusDecimalFormat(0).format(message.getPlanProgress())+" %";    
                    else
                        lastOutcome = "<html><font color='#666666'>" + I18n.text("N/A") + "</font>";                            
                    break;
                default:
                    outcomeTitleLabel.setText("<html><b>" + I18n.text("Progress") + ": ");
                    lastOutcome = "Initializing";
                    break;
            }
            
            outcomeLabel.setText(lastOutcome);
            lastUpdated = System.currentTimeMillis();
            update();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage(), e);
        }
    }

    @Override
    public void messageArrived(IMCMessage message) {
        if (message.getMgid() == PlanControlState.ID_STATIC)
            consume((PlanControlState) message);
    }

    @Override
    public long millisBetweenUpdates() {
        return 1000;
    }

    @Override
    public boolean update() {
        if (state != null)
            stateValueLabel.setText(state.toString());

        String planTimeStr;

        planTimeStr = "";
        planIdValueLabel.setText(planId + planTimeStr);

        String nodeStr = nodeId;

        if (IMCDefinition.getInstance().getMessageName(nodeTypeImcId) != null)
            nodeStr += " <font color='#666666' size='3'>(" + IMCDefinition.getInstance().getMessageName(nodeTypeImcId)
                    + ")</font> ";
        String nodeTimeStr;
        if (nodeStarTimeMillisUTC > 0)
            nodeTimeStr = "@" + DateTimeUtil.timeFormaterNoMillis2.format(new Date(nodeStarTimeMillisUTC));
        else
            nodeTimeStr = "";
        String etaStr;
        if (nodeEtaSec >= 0 && !"".equalsIgnoreCase(nodeTimeStr) && nodeEtaSec != 0xFFFF)
            etaStr = " ETA " + DateTimeUtil.milliSecondsToFormatedString(nodeEtaSec * 1000);
        else
            etaStr = "";
        nodeIdValueLabel.setText("<html>" + nodeStr + etaStr);

        Color fColor = Color.BLACK;
        if ((System.currentTimeMillis() - (lastUpdated)) > 5000) {
            fColor = Color.RED.darker();
        }
        if (!stateLabel.getForeground().equals(fColor)) {
            stateLabel.setForeground(fColor);
            planIdLabel.setForeground(fColor);
            nodeIdLabel.setForeground(fColor);
            outcomeTitleLabel.setForeground(fColor);
        }
        return true;
    }

    @Override
    public void mainVehicleChangeNotification(String id) {
        state = PlanControlState.STATE.BLOCKED;
        planId = "";
        // planStarTimeMllisUTC = -1;
        nodeId = "";
        nodeStarTimeMillisUTC = -1;
        nodeTypeImcId = 0xFFFF;
    }

    @Override
    public void initSubPanel() {
    }

    @Override
    public void cleanSubPanel() {
    }
}
