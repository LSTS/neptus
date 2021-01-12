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
 * 3/3/2011
 */
package pt.lsts.neptus.plugins.alarms;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

import org.jdesktop.swingx.JXPanel;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.AlarmProviderOld;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.painters.SubPanelTitlePainter;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.util.DateTimeUtil;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@PluginDescription(name = "Last Error Panel", author = "Paulo Dias", version = "0.1", documentation = "entity-state/lasterror.html")
public class LastErrorPanel extends ConsolePanel implements MainVehicleChangeListener, IPeriodicUpdates,
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
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#millisBetweenUpdates()
     */
    @Override
    public long millisBetweenUpdates() {
        return 500;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.update.IPeriodicUpdates#update()
     */
    @Override
    public boolean update() {
        ImcSystem sys = ImcSystemsHolder.lookupSystemByName(getMainVehicleId());
        if (sys != null) {
            if (sys.isOnErrorState()) {
                // vehicleErrorsLabel.setText("<html><b><font color='red'>Yes");

                String dateStr = "";
                if (sys.getLastErrorStateReceived() != -1) {
                    dateStr = DateTimeUtil.timeFormatterNoMillis2.format(new Date(sys.getLastErrorStateReceived()));
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
                dateStr = DateTimeUtil.timeFormatterNoMillis2.format(new Date(lastErrorTime * 1000));
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
                    e.printStackTrace();
                }
            }
            catch (RuntimeException e) {
                e.printStackTrace();
            }
            oldState = alarmState;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.AlarmProvider#getAlarmState()
     */
    @Override
    public int getAlarmState() {
        return alarmState;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.AlarmProvider#getAlarmMessage()
     */
    @Override
    public String getAlarmMessage() {
        return "Alarm";
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.consolebase.AlarmProvider#sourceState()
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
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
        // TODO Auto-generated method stub
        
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
