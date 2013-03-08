/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 23/06/2011
 * $Id:: PlanControlStatePanel.java 10018 2013-02-22 11:19:58Z zepinto          $:
 */
package pt.up.fe.dceg.neptus.plugins.planning;

import java.awt.Color;
import java.util.Date;

import javax.swing.JLabel;

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

    private PlanControlState.STATE state;
    private String planId = "";
    //private long planStarTimeMllisUTC = -1;
    private String nodeId = "";
    private int nodeTypeImcId = -1;
    private long nodeStarTimeMillisUTC = -1;
    private long nodeEtaSec = -1;
    private String lastEvent = "";
    private long lastEventTimeMillisUTC = -1;
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

//        lastEventTitle = new JLabel("<html><b>" + I18n.text("Last event") + ": ");
//        lastEventTitle.setHorizontalAlignment(SwingConstants.LEADING);
//        lastEventTitle.setFont(font);
//        lastEventLabel = new JTextArea("");
//        lastEventLabel.setFont(font);
//        lastEventLabel.setLineWrap(true);
//        lastEventLabel.setWrapStyleWord(true);
//        lastEventLabel.setEditable(false);
//        lastEventLabel.setEnabled(false);
//        lastEventLabel.setOpaque(false);
//        lastEventScrollPane = new JScrollPane();
//        lastEventScrollPane.setSize(new Dimension(136, 40));
//        lastEventScrollPane.setViewportView(lastEventLabel);
//        lastEventScrollPane.setOpaque(false);
//        this.add(lastEventTitle, "wrap, span 2");
//        this.add(lastEventScrollPane, "w 100%, h 100%, span 2");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.NeptusMessageListener#getObservedMessages()
     */
    @Override
    public String[] getObservedMessages() {
        return messagesToObserve;
    }

    public void consume(PlanControlState message) {
        try {
            state = message.getState();
            planId = message.getPlanId();
            //planStarTimeMllisUTC = (long) (message. * 1000);
            nodeId = message.getManId();
            nodeTypeImcId = message.getManType();
            //nodeStarTimeMillisUTC = (long) (message.getManStime() * 1000);
            nodeEtaSec = message.getManEta();
            lastEvent = message.getLastOutcome().toString();
            
//            long ts = (long) (message.getLastEventTime() * 1000);
//            if (ts != lastEventTimeMillisUTC) {
//                post(Notification.info(
//                        getMainVehicleId(),
//                        I18n.textf("Message from system: %message",
//                                DateTimeUtil.timeFormaterNoMillis2.format(new Date(ts)) + " - " + lastEvent)));
//            }
//            lastEventTimeMillisUTC = ts;

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
//        if (planStarTimeMllisUTC > 0)
//            planTimeStr = "@" + DateTimeUtil.timeFormaterNoMillis2.format(new Date(planStarTimeMllisUTC));
//        else
            planTimeStr = "";
        planIdValueLabel.setText(planId + planTimeStr);

        String nodeStr = nodeId;
        
        if (IMCDefinition.getInstance().getMessageName(nodeTypeImcId) != null)
            nodeStr += " <font color='#666666' size='3'>(" + IMCDefinition.getInstance().getMessageName(nodeTypeImcId) + ")</font> ";
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
        nodeIdValueLabel.setText("<html>" +  nodeStr + etaStr);

//        if (lastEvent != null && !"".equalsIgnoreCase(lastEvent)) {
//            String dateStr = "";
//            if (lastEventTimeMillisUTC != -1) {
//                dateStr = DateTimeUtil.timeFormaterNoMillis2.format(new Date(lastEventTimeMillisUTC));
//                if ((System.currentTimeMillis() - (lastEventTimeMillisUTC)) > 5000)
//                    lastEventLabel.setEnabled(false);
//                else
//                    lastEventLabel.setEnabled(true);
//            }
//            lastEventLabel.setText(dateStr + " - " + lastEvent);
//        }
//        else {
//            lastEventLabel.setText("");
//        }

        Color fColor = Color.BLACK;
        if ((System.currentTimeMillis() - (lastUpdated)) > 5000) {
            fColor = Color.RED.darker();
        }
        if (!stateLabel.getForeground().equals(fColor)) {
            stateLabel.setForeground(fColor);
            planIdLabel.setForeground(fColor);
            nodeIdLabel.setForeground(fColor);
//            lastEventTitle.setForeground(fColor);
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
        lastEvent = "";
        lastEventTimeMillisUTC = -1;
    }

    @Override
    public void initSubPanel() {
    }

    @Override
    public void cleanSubPanel() {
    }
}
