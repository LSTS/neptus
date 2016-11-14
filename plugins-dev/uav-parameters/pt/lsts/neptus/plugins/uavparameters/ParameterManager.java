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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_param_value;
import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
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
@Popup(name = "UAV Parameter Configuration Panel", pos = POSITION.CENTER, height = 500, width = 400, accelerator = '0')
public class ParameterManager extends ConsolePanel implements MainVehicleChangeListener, MAVLinkConnectionListener {
    private JTextField findTxtField;
    private JTable table;
    private MAVLinkConnection mavlink = null;
    private int expectedParams;
    private final HashMap<Integer, Parameter> parameters = new HashMap<Integer, Parameter>();
    public final ArrayList<Parameter> parameterList = new ArrayList<Parameter>();

    public ParameterManager(ConsoleLayout console) {
        super(console);
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
    public void initSubPanel() {
        mavlink = new MAVLinkConnection("10.0.20.125", 9999);
        mavlink.initiateConnection(true);
        mavlink.addMavLinkConnectionListener("ParameterManager", this);
        mavlink.connect();
        mavlink.send();
        
        setLayout(new BorderLayout(0, 0));

        JPanel mainPanel = new JPanel();
        add(mainPanel);
        mainPanel.setLayout(new BorderLayout(0, 0));

        JPanel tablePanel = new JPanel();
        mainPanel.add(tablePanel, BorderLayout.EAST);
        tablePanel.setLayout(new MigLayout("", "[grow]", "[][][][][][][][]"));

        JButton btnGetParams = new JButton("Load Parameters");
 
        btnGetParams.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        refreshParameters();
                        return null;
                    }
                };
                worker.execute();
            }
        }
        );
        tablePanel.add(btnGetParams, "cell 0 0,growx");

        JButton btnWriteParams = new JButton("Write Parameters");
        tablePanel.add(btnWriteParams, "cell 0 1,growx");

        JButton btnSaveToFile = new JButton("Save to File");
 
        tablePanel.add(btnSaveToFile, "cell 0 2,growx");

        JButton btnLoadFromFile = new JButton("Load from File");
        tablePanel.add(btnLoadFromFile, "cell 0 3,growx");

        JButton btnFind = new JButton("Find");
        tablePanel.add(btnFind, "cell 0 6,growx");

        findTxtField = new JTextField();
        tablePanel.add(findTxtField, "cell 0 7,growx");
        findTxtField.setColumns(10);

       btnSaveToFile.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                findTxtField.setText(parameters.size() +
                " " + parameters.size());
            }
        });
       
        JScrollPane scrollPane = new JScrollPane();
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        table = new JTable();
        table.setModel(new DefaultTableModel(
            new Object[][] {
            },
            new String[] {
                "Command", "Value"
            }
        ));
        scrollPane.setViewportView(table);
        setResizable(false);
    }

    
    public void refreshParameters() {
        parameters.clear();
        parameterList.clear();

        if (mavlink != null)
            MAVLinkParameters.requestParametersList(mavlink);
    }
    
    public List<Parameter> getParametersList(){
        return parameterList;
    }
    
    private boolean processMessage(MAVLinkMessage msg) {
        if (msg.msgid == msg_param_value.MAVLINK_MSG_ID_PARAM_VALUE) {
            processReceivedParam((msg_param_value) msg);
            return true;
        }
        return false;
    }

    private void processReceivedParam(msg_param_value m_value) {
        // collect params in parameter list
        Parameter param = new Parameter(m_value);
        parameters.put((int) m_value.param_index, param);

        expectedParams = m_value.param_count;

        // Are all parameters here? Notify the listener with the parameters
        if (parameters.size() >= m_value.param_count) {
            parameterList.clear();
            for (int key : parameters.keySet()) {
                parameterList.add(parameters.get(key));
            }
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
        //System.out.println("Receiving PARAMETERS");
        processMessage(msg);
    }

}