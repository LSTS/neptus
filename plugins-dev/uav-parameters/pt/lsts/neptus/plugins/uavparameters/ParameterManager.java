/*
 * Copyright (c) 2004-2016 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * http://ec.europa.eu/idabc/eupl.html.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: Manuel R.
 * 07/11/2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.swingx.JXStatusBar;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_param_value;
import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.gui.StatusLed;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.uavparameters.connection.MAVLinkConnection;
import pt.lsts.neptus.plugins.uavparameters.connection.MAVLinkConnectionListener;

/**
 * @author Manuel R.
 *
 */

@SuppressWarnings("serial")
@PluginDescription(name = "UAV Parameter Configuration", icon = "images/settings2.png")
@Popup(name = "UAV Parameter Configuration Panel", pos = POSITION.CENTER, height = 500, width = 800, accelerator = '0')
public class ParameterManager extends ConsolePanel implements MainVehicleChangeListener, MAVLinkConnectionListener {
    private static final int TIMEOUT = 5000;
    private static final int RETRYS = 2;
    private JTextField findTxtField;
    private JTable table;
    private MAVLinkConnection mavlink = null;
    private boolean success = false;
    private int expectedParams;
    private HashMap<Integer, Parameter> parameters = new HashMap<Integer, Parameter>();
    private ArrayList<Parameter> parameterList = new ArrayList<Parameter>();
    private ParameterTableModel model = null;
    private static InfiniteProgressPanel loader = InfiniteProgressPanel.createInfinitePanelBeans("", 100);

    private JXStatusBar statusBar = null;
    private JLabel messageBarLabel = null;
    private StatusLed statusLed = null;

    public ParameterManager(ConsoleLayout console) {
        super(console);

        setActivity("", StatusLed.LEVEL_OFF);
        mavlink = new MAVLinkConnection("10.0.20.125", 9999);
        mavlink.initiateConnection(true);
        mavlink.addMavLinkConnectionListener("ParameterManager", this);
        mavlink.connect();

        setLayout(new BorderLayout(0, 0));
        JPanel mainPanel = new JPanel();
        JPanel tablePanel = new JPanel();
        JButton btnGetParams = new JButton("Load Parameters");
        JButton btnWriteParams = new JButton("Write Parameters");
        JButton btnSaveToFile = new JButton("Save to File");
        JButton btnLoadFromFile = new JButton("Load from File");
        JButton btnFind = new JButton("Find");
        findTxtField = new JTextField();
        JScrollPane scrollPane = new JScrollPane();

        add(mainPanel);

        mainPanel.setLayout(new BorderLayout(0, 0));
        mainPanel.add(tablePanel, BorderLayout.EAST);
        tablePanel.setLayout(new MigLayout("", "[grow]", "[][][][][][][][][][]"));

        tablePanel.add(btnGetParams, "cell 0 0,growx");
        tablePanel.add(btnWriteParams, "cell 0 1,growx");
        tablePanel.add(btnSaveToFile, "cell 0 2,growx");
        tablePanel.add(btnLoadFromFile, "cell 0 3,growx");
        tablePanel.add(btnFind, "cell 0 6,growx");
        tablePanel.add(findTxtField, "cell 0 7,growx");
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        findTxtField.setColumns(10);

        model = new ParameterTableModel(parameterList);

        table = new JTable(model);

        model.addTableModelListener(
                new TableModelListener() 
                {
                    public void tableChanged(TableModelEvent evt) 
                    {
                        if (!parameterList.isEmpty())
                            System.out.println("Something changed...");

                        //TODO
                    }
                });

        scrollPane.setViewportView(table);

        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.BLACK));
        setResizable(false);

        loader.setOpaque(false);
        loader.setVisible(false);
        loader.setBusy(false);
        tablePanel.add(loader, "cell 0 9,growx");

        JPanel statusPanel = new JPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);
        statusPanel.setLayout(new BorderLayout(0, 0));

        statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));

        statusBar = new JXStatusBar();
        statusBar.add(getMessageBarLabel(), JXStatusBar.Constraint.ResizeBehavior.FILL);
        statusBar.add(getStatusLed());

        statusPanel.add(statusBar);

        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = -4859420619704314087L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                    int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                setBackground(row % 2 == 0 ? Color.gray : Color.gray.darker());

                return this;
            }
        });

        btnGetParams.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

                    @Override
                    protected Void doInBackground() throws Exception {
                        setActivity("Loading parameters...", StatusLed.LEVEL_0);
                        loader.setText("");
                        loader.setVisible(true);
                        loader.setBusy(true);

                        int num_of_retries = 1;
                        long now = System.currentTimeMillis();
                        requestParameters();

                        while (num_of_retries <= RETRYS && !success) {
                            while(((System.currentTimeMillis() - now) < TIMEOUT) && !success)
                            {
                                Thread.sleep(1000);
                                System.out.println("...");

                            }
                            
                            // Didn't receive parameter count within TIMEOUT so we break loop
                            if (expectedParams == 0)
                                num_of_retries = RETRYS + 1;
                            
                            // Re-request missing parameters
                            if (!success && expectedParams > 0)
                                reRequestMissingParams(expectedParams);

                            now = System.currentTimeMillis();
                            num_of_retries++;
                        }

                        System.out.println(expectedParams + " " + getParametersList().size() );
                        if ((expectedParams != getParametersList().size()) || expectedParams == 0) {
                            setActivity("Failed to load parameters...", StatusLed.LEVEL_0);
                            loader.setVisible(false);
                            loader.setBusy(false);
                            loader.setText("");

                        }
                        else if (getParametersList().size() > 0){
                            setActivity("Parameters loaded successfully...", StatusLed.LEVEL_0);
                            updateTable();
                        }
                        return null;
                    }

                };
                worker.execute();
            }
        });

        btnSaveToFile.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                findTxtField.setText(parameters.size() +
                        " " + parameters.size());

                System.out.println("STATUS: " + mavlink.isConnected());
            }
        });
    }

    private JLabel getMessageBarLabel() {
        if (messageBarLabel == null) {
            messageBarLabel = new JLabel();
            messageBarLabel.setText("");
        }
        return messageBarLabel;
    }

    private StatusLed getStatusLed() {
        if (statusLed == null) {
            statusLed = new StatusLed();
            statusLed.setLevel(StatusLed.LEVEL_OFF);
        }
        return statusLed;

    }
    private void setActivity(String message, short level) {
        getMessageBarLabel().setText(message);
        getStatusLed().setLevel(level);
    }
            

    private void setActivity(String message, short level, String tooltip) {
        getMessageBarLabel().setText(message);
        getStatusLed().setLevel(level, tooltip);
    }

    @Override
    public void initSubPanel() {

    }

    private void updateTable() {
        model.updateParamList(parameterList);
    }

    private void requestParameters() {
        expectedParams = 0;
        parameters.clear();
        parameterList.clear();
        success = false;
        updateTable();

        if (mavlink != null)
            MAVLinkParameters.requestParametersList(mavlink);
    }


    private boolean processMessage(MAVLinkMessage msg) {
        if (msg.msgid == msg_param_value.MAVLINK_MSG_ID_PARAM_VALUE) {
            processReceivedParam((msg_param_value) msg);
            return true;
        }
        return false;
    }

    private void processReceivedParam(msg_param_value m_value) {
        Parameter param = new Parameter(m_value);
        parameters.put((int) m_value.param_index, param);

        expectedParams = m_value.param_count;

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                loader.setText(I18n.text(parameters.size()+ " of " + expectedParams));
                return null;
            }
        };
        worker.execute();

        //All parameters here!
        if (parameters.size() >= m_value.param_count) {
            parameterList.clear();
            for (int key : parameters.keySet()) {
                parameterList.add(parameters.get(key));
            }

            loader.setVisible(false);
            loader.setBusy(false);
            success = true;
        }
    }

    private void reRequestMissingParams(int howManyParams) {
        for (int i = 0; i < howManyParams; i++) {
            if (!parameters.containsKey(i)) {
                MAVLinkParameters.readParameter(mavlink, i);
            }
        }
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange e) {
        System.out.println("Vehicle changed "+ e.getCurrent() );
    }

    @Override
    public void onReceiveMessage(MAVLinkMessage msg) {
        processMessage(msg);
    }

    public List<Parameter> getParametersList(){
        return parameterList;
    }

    @Override
    public void cleanSubPanel() {
        if (mavlink != null) {
            try {
                mavlink.closeConnection();
            }
            catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @Override
    public void onConnect() {
        setActivity("Connected successfully...", StatusLed.LEVEL_0, "Connected!");
    }

    @Override
    public void onDisconnect() {
        setActivity("No connection. Retrying...", StatusLed.LEVEL_2, "Not connected!");

    }

    @Override
    public void onComError(String errMsg) {
        setActivity(errMsg, StatusLed.LEVEL_1);

    }
}