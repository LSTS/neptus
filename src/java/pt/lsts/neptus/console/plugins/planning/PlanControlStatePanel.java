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
 * 23/06/2011
 */
package pt.lsts.neptus.console.plugins.planning;

import java.awt.Color;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCUtil;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.imc.PlanDB;
import pt.lsts.imc.PlanDB.OP;
import pt.lsts.imc.PlanDB.TYPE;
import pt.lsts.imc.StateReport;
import pt.lsts.imc.state.ImcSystemState;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author pdias
 * 
 */
@PluginDescription(name = "Plan Control State", author = "Paulo Dias", version = "0.7", documentation = "plan-control/plan-control.html#PlanControlState")
public class PlanControlStatePanel extends ConsolePanel {
    private static final long serialVersionUID = 1L;

    private static int count = 0;

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
    private String nodeId = "";
    private String lastOutcome = "<html><font color='0x666666'>" + I18n.text("N/A") + "</font>";
    private int nodeTypeImcId = -1;
    private long nodeStarTimeMillisUTC = -1;
    private long nodeEtaSec = -1;
    private long lastUpdated = -1;

    @NeptusProperty(name = "Request plans automatically", userLevel=LEVEL.ADVANCED, category="Planning", description = "Select if Neptus should ask the vehicle for plans it is executing but Neptus doesn't know about")
    public boolean requestPlans = false;

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

    @Subscribe
    public void on(PlanControlState msg) {
        if (!requestPlans || msg.getPlanId().isEmpty())
            return;

        if (!getConsole().getMission().getIndividualPlansList().containsKey(msg.getPlanId())) {
            PlanDB pdb = new PlanDB();
            pdb.setOp(OP.GET);
            pdb.setPlanId(msg.getPlanId());
            pdb.setType(TYPE.REQUEST);
            pdb.setRequestId(count++);
            send(msg.getSourceName(), pdb);
        }
    }

    @Subscribe
    public void consume(StateReport message) {
        if (!message.getSourceName().equals(getConsole().getMainSystem()))
            return;
        
        for (String plan : getConsole().getMission().getIndividualPlansList().keySet()) {
            byte[] bytes = plan.getBytes();
            if (IMCUtil.computeCrc16(bytes , 0, bytes.length) == message.getPlanChecksum()) {
                planId = plan + " (CS::" + message.getPlanChecksum() + ")";
                break;
            }            
        }
        
        int progress = -1;
        switch (message.getExecState()) {
            case -1:
                state = STATE.READY;    
                break;
            case -2:
            case -3:
                state = STATE.INITIALIZING;
                break;            
            case -4:
                state = STATE.BLOCKED;
                break;
            default:
                state = STATE.EXECUTING;
                progress = message.getExecState();
                break;
        }
        
        outcomeTitleLabel.setText("<html><b>" + I18n.text("Progress") + ": ");
        if (progress != -1)
            lastOutcome = GuiUtils.getNeptusDecimalFormat(0).format(progress) + " %";
        else
            lastOutcome = "<html><font color='#666666'>" + I18n.text("N/A") + "</font>";
    }
    
    @Subscribe
    public void consume(PlanControlState message) {
        if (!message.getSourceName().equals(getConsole().getMainSystem()))
            return;

        if (message.getPlanId().isEmpty()) {
            if (message.getPlanProgress() != -1) {
                outcomeTitleLabel.setText("<html><b>" + I18n.text("Progress") + ": ");
                outcomeLabel.setText(GuiUtils.getNeptusDecimalFormat(0).format(message.getPlanProgress()) + " %");
            }
            return;
        }
        
        try {
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
                        lastOutcome = GuiUtils.getNeptusDecimalFormat(0).format(message.getPlanProgress()) + " %";
                    else
                        lastOutcome = "<html><font color='#666666'>" + I18n.text("N/A") + "</font>";
                    break;
                default:
                    outcomeTitleLabel.setText("<html><b>" + I18n.text("Progress") + ": ");
                    lastOutcome = "Initializing";
                    break;
            }

            outcomeLabel.setText(lastOutcome);
            lastUpdated = message.getTimestampMillis();
            state = message.getState();
            update();
        }
        catch (Exception e) {
            NeptusLog.pub().error(e.getMessage(), e);
        }
    }

    @Periodic
    public void update() {
        if (state != null)
            stateValueLabel.setText(I18n.text(state.toString()));

        String planTimeStr;

        planTimeStr = "";
        planIdValueLabel.setText(planId + planTimeStr);

        String nodeStr = nodeId;

        if (IMCDefinition.getInstance().getMessageName(nodeTypeImcId) != null)
            nodeStr += " <font color='#666666' size='3'>(" + IMCDefinition.getInstance().getMessageName(nodeTypeImcId)
                    + ")</font> ";
        String nodeTimeStr;
        if (nodeStarTimeMillisUTC > 0)
            nodeTimeStr = "@" + DateTimeUtil.timeFormatterNoMillis2.format(new Date(nodeStarTimeMillisUTC));
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
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        state = PlanControlState.STATE.BLOCKED;
        planId = "";
        nodeId = "";
        nodeStarTimeMillisUTC = -1;
        nodeTypeImcId = 0xFFFF;
        try {
            ImcSystemState state = getConsole().getImcMsgManager().getState(getMainVehicleId());
            if (state != null) {
                PlanControlState pcsMsg = state.last(PlanControlState.class);
                StateReport srMsg = state.last(StateReport.class);
                
                if (pcsMsg != null && srMsg != null) {
                    if (srMsg.getAgeInSeconds() <= pcsMsg.getAgeInSeconds())
                        consume(srMsg); 
                    else
                        consume(pcsMsg); 
                }
                else if (pcsMsg != null) {
                    consume(pcsMsg); 
                }
                else if (srMsg != null) {
                    consume(srMsg); 
                }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        
    }

    @Override
    public void initSubPanel() {
    }

    @Override
    public void cleanSubPanel() {
    }
}
