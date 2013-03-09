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
 * 3/3/2011
 */
package pt.up.fe.dceg.neptus.plugins.alarms;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXPanel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.plugins.AlarmProviderOld;
import pt.up.fe.dceg.neptus.console.plugins.MainVehicleChangeListener;
import pt.up.fe.dceg.neptus.gui.painters.SubPanelTitlePainter;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.plugins.NeptusMessageListener;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.DateTimeUtil;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystem;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcSystemsHolder;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Last Error Panel", author = "Paulo Dias", version = "0.1", documentation = "entity-state/lasterror.html")
public class LastErrorPanel extends SimpleSubPanel implements MainVehicleChangeListener, IPeriodicUpdates,
        AlarmProviderOld, NeptusMessageListener {

    // GUI
    private JLabel entitiesInErrorTitle;
    private JTextArea entitiesInErrorLabel;
    private JScrollPane entitiesInErrorScrollPane;
    private JLabel lastErrorTitle;
    private JTextArea lastErrorLabel;
    private JScrollPane lastErrorScrollPane;

    private SubPanelTitlePainter backPainter;

    private Font font = new Font("Sans", 0, 12);

    private String lastError = "";
    private long lastErrorTime = -1;

    private int alarmState = AlarmProviderOld.LEVEL_0;

    /**
     * 
     */
    public LastErrorPanel(ConsoleLayout console) {
        super(console);
        initialize();
    }

    /**
     * 
     */
    private void initialize() {
        backPainter = new SubPanelTitlePainter("Error State");
        removeAll();
        setSize(180, 130);
        JXPanel holder = new JXPanel(new MigLayout());
        holder.setBackgroundPainter(backPainter);

        this.setLayout(new BorderLayout());
        this.add(holder);
        holder.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

        lastErrorTitle = new JLabel("<html><b>Last error: ");
        lastErrorTitle.setHorizontalAlignment(SwingConstants.LEADING);
        lastErrorTitle.setFont(font);

        lastErrorLabel = new JTextArea("");
        lastErrorLabel.setFont(font);
        lastErrorLabel.setLineWrap(true);
        lastErrorLabel.setWrapStyleWord(true);
        lastErrorLabel.setEditable(false);
        lastErrorLabel.setEnabled(false);
        lastErrorLabel.setOpaque(false);

        lastErrorScrollPane = new JScrollPane();
        // lastErrorScrollPane.setPreferredSize(new Dimension(136, 40));
        lastErrorScrollPane.setViewportView(lastErrorLabel);
        lastErrorScrollPane.setOpaque(false);

        entitiesInErrorTitle = new JLabel("<html><b>Error entities: ");
        // entitiesInErrorTitle.setHorizontalAlignment(SwingConstants.LEADING);
        entitiesInErrorTitle.setFont(font);

        entitiesInErrorLabel = new JTextArea("");
        entitiesInErrorLabel.setFont(font);
        entitiesInErrorLabel.setLineWrap(true);
        entitiesInErrorLabel.setWrapStyleWord(true);
        entitiesInErrorLabel.setEditable(false);
        entitiesInErrorLabel.setEnabled(false);
        entitiesInErrorLabel.setOpaque(false);

        entitiesInErrorScrollPane = new JScrollPane();
        // entitiesInErrorScrollPane.setPreferredSize(new Dimension(136, 40));
        entitiesInErrorScrollPane.setViewportView(entitiesInErrorLabel);
        entitiesInErrorScrollPane.setOpaque(false);

        // JPanel jp = new JPanel();
        // jp.setOpaque(false);
        // jp.setLayout(new BorderLayout());
        // jp.add(entitiesInErrorTitle, BorderLayout.WEST);
        // //jp.add(entitiesInErrorScrollPane, BorderLayout.CENTER);
        // holder.add(jp);
        // holder.add(entitiesInErrorScrollPane);
        //
        // jp = new JPanel();
        // jp.setOpaque(false);
        // jp.setLayout(new BorderLayout());
        // jp.add(lastErrorTitle, BorderLayout.WEST);
        // //jp.add(lastErrorScrollPane, BorderLayout.CENTER);
        // holder.add(jp);
        // holder.add(lastErrorScrollPane);
        holder.add(entitiesInErrorTitle, "wrap, gaptop 8");
        holder.add(entitiesInErrorScrollPane, "wrap ,h 45%, w 100%");
        holder.add(lastErrorTitle, "wrap");
        holder.add(lastErrorScrollPane, "wrap,h 45%, w 100%");
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
        if (sys != null) {
            if (sys.isOnErrorState()) {
                // vehicleErrorsLabel.setText("<html><b><font color='red'>Yes");

                String dateStr = "";
                if (sys.getLastErrorStateReceived() != -1) {
                    dateStr = DateTimeUtil.timeFormaterNoMillis2.format(new Date(sys.getLastErrorStateReceived()));
                    if ((System.currentTimeMillis() - sys.getLastErrorStateReceived()) > 5000)
                        entitiesInErrorLabel.setEnabled(false);
                    else
                        // @Override
                        // public String[] variablesToListen() {
                        // return new String[] { "VehicleState.last_error", "VehicleState.last_error_time" };
                        // }
                        //
                        entitiesInErrorLabel.setEnabled(true);
                }
                entitiesInErrorLabel.setText(dateStr + " - " + sys.getOnErrorStateStr());
            }
            else {
                // vehicleErrorsLabel.setText("<html>No");
                alarmState = AlarmProviderOld.LEVEL_0;
                warningAlarmChange();
                entitiesInErrorLabel.setText("");
            }
        }
        else {
            // vehicleErrorsLabel.setText("<html>?");
            entitiesInErrorLabel.setText("<html><b>?");
        }

        if (lastError != null && !"".equalsIgnoreCase(lastError)) {
            String dateStr = "";
            if (lastErrorTime != -1) {
                dateStr = DateTimeUtil.timeFormaterNoMillis2.format(new Date(lastErrorTime * 1000));
                if ((System.currentTimeMillis() - (lastErrorTime * 1000)) > 5000)
                    lastErrorLabel.setEnabled((entitiesInErrorLabel.isEnabled() && !""
                            .equalsIgnoreCase(entitiesInErrorLabel.getText())) ? true : false);
                else
                    lastErrorLabel.setEnabled(true);
            }
            lastErrorLabel.setText(dateStr + " - " + lastError);
        }
        else {
            alarmState = AlarmProviderOld.LEVEL_0;
            warningAlarmChange();
            lastErrorLabel.setText("");
        }

        return true;
    }

    int oldState = -100;

    private void warningAlarmChange() {
        if (oldState < 0 || oldState != alarmState) {
            try {
                try {
                    getMainpanel().getAlarmlistener().updateAlarmsListeners(this);
                }
                catch (RuntimeException e) {
                }
            }
            catch (RuntimeException e) {
            }
            oldState = alarmState;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.consolebase.AlarmProvider#getAlarmState()
     */
    @Override
    public int getAlarmState() {
        return alarmState;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.consolebase.AlarmProvider#getAlarmMessage()
     */
    @Override
    public String getAlarmMessage() {
        return "Alarm";
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.up.fe.dceg.neptus.consolebase.AlarmProvider#sourceState()
     */
    @Override
    public int sourceState() {
        return 0;
    }


    @Override
    public String[] getObservedMessages() {
        return new String[] { "VehicleState" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        String lastErrorTmp = message.getString("last_error");
        long lastErrorTimeTmp = message.getLong("last_error_time");

        if (!lastError.equals(lastErrorTmp)) {
            // FIXME comment this out for now, until settling with an alarm system (jqcorreia)
            // alarmState = AlarmProvider.LEVEL_2;
            // warningAlarmChange();

            lastError = lastErrorTmp;
            lastErrorTime = lastErrorTimeTmp;
        }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
