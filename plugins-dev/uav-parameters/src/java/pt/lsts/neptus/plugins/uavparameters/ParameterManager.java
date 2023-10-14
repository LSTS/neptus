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
 * Author: Manuel R.
 * 07/11/2016
 */
package pt.lsts.neptus.plugins.uavparameters;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableRowSorter;

import org.apache.commons.text.WordUtils;
import org.jdesktop.swingx.JXStatusBar;

import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.common.msg_param_value;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.gui.StatusLed;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.uavparameters.ParameterMetadata.Item;
import pt.lsts.neptus.plugins.uavparameters.connection.MAVLinkConnection;
import pt.lsts.neptus.plugins.uavparameters.connection.MAVLinkConnectionListener;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author Manuel R.
 *
 */

@SuppressWarnings("serial")
@PluginDescription(name = "UAV Parameter Configuration", icon = "images/settings2.png")
@Popup(name = "UAV Parameter Configuration Panel", pos = POSITION.CENTER, height = 500, width = 800, accelerator = '0')
public class ParameterManager extends ConsolePanel implements MAVLinkConnectionListener {
    private static final int TIMEOUT = 5000;
    private static final int RETRYS = 10;
    private static final InfiniteProgressPanel loader = InfiniteProgressPanel.createInfinitePanelBeans("", 100);
    private JTextField findTxtField;
    private JTable table;
    private TableRowSorter<ParameterTableModel> sorter;
    private MAVLinkConnection mavlink = null;
    private ParameterWriter writer = null;
    private ParameterReader reader = null;
    private boolean isFinished = false;
    private boolean writeWithSuccess = false;
    private int expectedParams;
    private HashMap<Integer, Parameter> parameters = new HashMap<Integer, Parameter>();
    private ArrayList<Parameter> parameterList = new ArrayList<Parameter>();
    private ParameterTableModel model = null;
    private JButton btnGetParams, btnWriteParams, btnSaveToFile, btnLoadFromFile, btnFind, btnConnect;
    private JXStatusBar statusBar = null;
    private JLabel messageBarLabel = null;
    private StatusLed statusLed = null;
    private boolean requestingWriting = false;

    public ParameterManager(ConsoleLayout console) {
        super(console);
    }

    @Override
    public void initSubPanel() {

        setupGUI();
        addListenersAndRenderer();
    }

    private void setupGUI() {
        setActivity("", StatusLed.LEVEL_OFF);
        setResizable(false);
        setLayout(new BorderLayout(0, 0));

        JPanel mainPanel = new JPanel();
        JPanel tablePanel = new JPanel();
        JPanel statusPanel = new JPanel();
        JScrollPane scrollPane = new JScrollPane();
        btnGetParams = new JButton("Load Parameters");
        btnWriteParams = new JButton("Write Parameters");
        btnSaveToFile = new JButton("Save to File");
        btnLoadFromFile = new JButton("Load from File");
        btnFind = new JButton("Find");
        findTxtField = new JTextField();
        btnConnect = new JButton("Connect");
        statusBar = new JXStatusBar();
        setBtnsEnabled(false);

        model = new ParameterTableModel(parameterList);
        table = new JTable(model) {
            @SuppressWarnings("unchecked")
            public TableCellEditor getCellEditor(int row, int column) {
                int modelColumn = convertColumnIndexToModel( column );
                int rowIndex = convertRowIndexToModel(row);
                Object value = model.getValue(rowIndex, true);

                if (modelColumn == ParameterTableModel.COLUMN_VALUE && value instanceof JComboBox) {
                    model.setEditedComboBox((JComboBox<Item>) value);
                    DefaultCellEditor editor = new DefaultCellEditor(model.getEditedComboBox());
                    editor.setClickCountToStart(2);

                    return editor;
                }
                else
                    return super.getCellEditor(rowIndex, column);
            }
        };

        sorter = new TableRowSorter<ParameterTableModel>(model);
        table.setRowSorter(sorter);

        List<RowSorter.SortKey> sortKeys = new ArrayList<>(1);
        sortKeys.add(new RowSorter.SortKey(0, SortOrder.ASCENDING));
        sorter.setSortKeys(sortKeys);

        mainPanel.setLayout(new BorderLayout(0, 0));
        tablePanel.setLayout(new MigLayout("", "[grow]", "[][][][][][][][][][]"));
        scrollPane.setViewportView(table);
        scrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.BLACK));
        statusPanel.setLayout(new BorderLayout(0, 0));
        statusPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Color.BLACK));
        findTxtField.setColumns(10);
        btnConnect.setFont(new Font("Dialog", Font.BOLD, 10));
        btnConnect.setMargin( new Insets(2, 2, 2, 2) );
        loader.setOpaque(false);
        loader.setVisible(false);
        loader.setBusy(false);

        mainPanel.add(tablePanel, BorderLayout.EAST);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        tablePanel.add(btnGetParams, "cell 0 0,growx");
        tablePanel.add(btnWriteParams, "cell 0 1,growx");
        tablePanel.add(btnSaveToFile, "cell 0 2,growx");
        tablePanel.add(btnLoadFromFile, "cell 0 3,growx");
        tablePanel.add(btnFind, "cell 0 6,growx");
        tablePanel.add(findTxtField, "cell 0 7,growx");
        tablePanel.add(loader, "cell 0 9,growx");

        statusPanel.add(statusBar);

        add(mainPanel);

        statusBar.add(getMessageBarLabel(), JXStatusBar.Constraint.ResizeBehavior.FILL);
        statusBar.add(btnConnect);
        statusBar.add(getStatusLed());
    }

    private void applyFilter() {
        RowFilter<ParameterTableModel, Object> rf = null;
        //If current expression doesn't parse, don't update.
        try {
            rf = RowFilter.regexFilter("(?i)" + findTxtField.getText(), 0);
        }
        catch (java.util.regex.PatternSyntaxException e) {
            return;
        }
        sorter.setRowFilter(rf);
    }

    private boolean updateParameters() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                if (model.getModifiedParams().isEmpty())
                    return null;

                requestingWriting = true;
                writeWithSuccess = false;
                long now = System.currentTimeMillis();

                setActivity("Updating "+ model.getModifiedParams().size() +" parameters...", StatusLed.LEVEL_1);

                for (Parameter param : model.getModifiedParams().values())
                    MAVLinkParameters.sendParameter(mavlink, param);

                while(((System.currentTimeMillis() - now) < TIMEOUT) && !writeWithSuccess && requestingWriting) {
                    Thread.sleep(1000);
                }

                if (writeWithSuccess) {
                    setActivity("All parameters updated successfully...", StatusLed.LEVEL_0);
                }
                else {
                    setActivity("Failed to update "+model.getModifiedParams().size() + " parameters...", StatusLed.LEVEL_2);
                    for (String n : model.getModifiedParams().keySet())
                        model.updateModified(n, Color.RED.darker());

                }
                requestingWriting = false;

                return null;
            }
        };

        worker.execute();

        return true;
    }

    private void addListenersAndRenderer() {

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

                        while (num_of_retries <= RETRYS && !isFinished) {
                            while(((System.currentTimeMillis() - now) < TIMEOUT) && !isFinished) {
                                Thread.sleep(1000);
                            }

                            // Didn't receive parameter count within TIMEOUT so we break loop
                            if (expectedParams == 0) {
                                setActivity("Failed to load parameters...", StatusLed.LEVEL_1);
                                num_of_retries = RETRYS + 1;
                            }

                            // Re-request missing parameters
                            if (!isFinished && expectedParams > 0)
                                reRequestMissingParams(expectedParams);

                            now = System.currentTimeMillis();
                            num_of_retries++;
                        }

                        if (isFinished){
                            setActivity("Parameters loaded successfully...", StatusLed.LEVEL_0);
                            updateTable();
                        }
                        else {
                            setActivity("Failed to load parameters...", StatusLed.LEVEL_0);
                            loader.setVisible(false);
                            loader.setBusy(false);
                            loader.setText("");
                        }
                        return null;
                    }

                };
                worker.execute();
            }
        });

        btnWriteParams.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateParameters();

            }
        });

        btnSaveToFile.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (parameterList.isEmpty())
                    return;

                writer = new ParameterWriter(parameterList);
                String path = System.getProperty("user.home");
                JFileChooser fc = GuiUtils.getFileChooser(path, "", ".param");

                if (fc.showOpenDialog(ParameterManager.this) == JFileChooser.APPROVE_OPTION) {
                    boolean saved = writer.saveParametersToFile(fc.getSelectedFile().getPath());
                    if (saved)
                        setActivity("Wrote "+ reader.getParameters().size() +" parameters to file...", StatusLed.LEVEL_0, "Ok!");
                }
            }
        });

        btnLoadFromFile.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                parameterList.clear();
                parameters.clear();
                model.clearModifiedParams();
                updateTable();

                reader = new ParameterReader();
                String path = System.getProperty("user.home");
                JFileChooser fc = GuiUtils.getFileChooser(path, "", ".param");

                if (fc.showOpenDialog(ParameterManager.this) == JFileChooser.APPROVE_OPTION) {
                    boolean f = reader.openFile(fc.getSelectedFile().getPath());
                    if (f) {
                        model.updateParamList((ArrayList<Parameter>) reader.getParameters(), mavlink.getSystemType());
                        setActivity("Loaded "+ reader.getParameters().size() +" parameters from file...", StatusLed.LEVEL_0, "Ok!");
                    }
                    else
                        setActivity("Failed to load "+ reader.getParameters().size() +" parameters from file...", StatusLed.LEVEL_0, "Fail!");
                }
            }
        });

        btnConnect.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                if (mavlink == null || (!mavlink.isMAVLinkConnected() && !mavlink.isToInitiateConnection())) {

                    HashMap<String, ImcSystem> validSystems = new HashMap<>();
                    ImcSystem[] syss = ImcSystemsHolder.lookupActiveSystemVehicles();
                    for (ImcSystem s : syss) {
                        String services = s.getServicesProvided();
                        if (services.contains(MAVLinkConnection.MAV_SCHEME))
                            validSystems.put(s.getName(), s);
                    }

                    if (validSystems.isEmpty()) {
                        Object[] options = {I18n.text("OK")};
                        JOptionPane.showOptionDialog(null, 
                                "No MAVLink compatible systems found.", 
                                "MAVLink Connection", JOptionPane.PLAIN_MESSAGE,
                                JOptionPane.QUESTION_MESSAGE,
                                null,
                                options,
                                options[0]);
                        return;
                    }

                    String system = (String) JOptionPane.showInputDialog(getConsole(), I18n.text("Connect to:"),
                            I18n.text("MAVLink Connection"), JOptionPane.QUESTION_MESSAGE, null,
                            validSystems.keySet().toArray(), validSystems.keySet().iterator().next());
                    if (system == null)
                        return;

                    ImcSystem sys = validSystems.get(system);
                    String[] listSer = sys.getServicesProvided().split(";");
                    String address = null;
                    int port = -1;
                    for (String rs : listSer) {
                        if (rs.trim().startsWith(MAVLinkConnection.MAV_SCHEME + ":")) {
                            URI url1 = URI.create(rs.trim());
                            address = url1.getHost();
                            port = url1.getPort();

                            break;
                        }
                    }

                    if (address != null && port != -1) {
                        beginMavConnection(address, 9999, system);
                        setActivity("Connecting...", StatusLed.LEVEL_1, "Connecting!");
                    }
                } 
                else {
                    if (mavlink != null) {
                        try {
                            mavlink.closeConnection();
                            setActivity("Not connected...", StatusLed.LEVEL_OFF, "Not connected!");
                            updateConnectMenuText();
                            setBtnsEnabled(false);
                        }
                        catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private static final long serialVersionUID = -4859420619704314087L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                    int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                setForeground(Color.WHITE);
                String param = model.getValueAt(table.convertRowIndexToModel(row), ParameterTableModel.COLUMN_PARAM_NAME).toString();

                setBackground(model.getRowColor(row, column, param));

                JLabel c = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == ParameterTableModel.COLUMN_DESCRIPTION || column == ParameterTableModel.COLUMN_UNITS) {
                    String desc = (String) model.getValueAt(table.convertRowIndexToModel(row), column);
                    
                    if (desc != null) {
                        if (!desc.isEmpty())
                            c.setToolTipText("<html>"+ WordUtils.wrap(desc, 40, "<br>", false) + "</html>");
                    }
                }
                else
                    c.setToolTipText(null);

                return this;
            }
        });

        findTxtField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                applyFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                applyFilter();
            }
        });
    }

    private void updateConnectMenuText() {
        if (mavlink.isMAVLinkConnected() || mavlink.isToInitiateConnection()) {
            btnConnect.setText(I18n.text("Disconnect"));
        }
        else {
            btnConnect.setText(I18n.text("Connect"));
        }
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

    private void beginMavConnection(String address, int port, String system) {
        if (mavlink == null)
            mavlink = new MAVLinkConnection(address, port, system);
        else
            mavlink.setAddressAndPort(address, port);
        mavlink.addMavLinkConnectionListener("ParameterManager", this);
        mavlink.connect();
    }

    private void setActivity(String message, short level, String tooltip) {
        getMessageBarLabel().setText(message);
        getStatusLed().setLevel(level, tooltip);
    }

    /**
     * Updates table model, notifying all listeners that table cell values 
     * in the table's rows may have changed
     * 
     * @param reParseMetadata : if true forces a metadata reparse, 
     *        else use same previously reparsed metadata
     */
    private void updateTable() {

        model.updateParamList(parameterList, mavlink.getSystemType());
    }

    /**
     * Request parameter list and updates table accordingly
     */
    private void requestParameters() {
        expectedParams = 0;
        parameters.clear();
        parameterList.clear();
        model.clearModifiedParams();
        isFinished = false;
        updateTable();

        if (mavlink != null)
            MAVLinkParameters.requestParametersList(mavlink);
    }

    /**
     * Validate incoming MavLinkMessage and process if it's a parameter message
     * @param msg the message to be validated/processed
     */

    private void validateMessage(MAVLinkMessage msg) {
        if (msg.msgid == msg_param_value.MAVLINK_MSG_ID_PARAM_VALUE) 
            processReceivedParam((msg_param_value) msg);
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


        //check if we're trying to write parameters
        if (requestingWriting) {

            model.checkAndUpdateParameter(param.name, param.getValue());

            // all parameters were successfuly updated
            if (model.getModifiedParams().isEmpty()) {
                requestingWriting = false;
                writeWithSuccess = true;
                model.fireTableDataChanged();
            }
        }

        //All parameters here!
        if (parameters.size() >= m_value.param_count) {
            parameterList.clear();
            for (int key : parameters.keySet()) {
                parameterList.add(parameters.get(key));
            }

            loader.setVisible(false);
            loader.setBusy(false);
            isFinished = true;
            parameters.clear();
        }
    }

    private void reRequestMissingParams(int howManyParams) {
        for (int i = 0; i < howManyParams; i++) {
            if (!parameters.containsKey(i)) {
                MAVLinkParameters.readParameter(mavlink, i);
            }
        }
    }

    @Override
    public void onReceiveMessage(MAVLinkMessage msg) {
        if (!btnGetParams.isEnabled()) {
            setActivity("Connected successfully...", StatusLed.LEVEL_0, "Connected!");
        }

        setBtnsEnabled(true);

        validateMessage(msg);
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
        setBtnsEnabled(true);
        updateConnectMenuText();
    }

    @Override
    public void onDisconnect() {
        if (mavlink.isToInitiateConnection())
            setActivity("No connection. Retrying...", StatusLed.LEVEL_2, "Not connected!");
        else
            setActivity("Not connected...", StatusLed.LEVEL_OFF, "Not connected!");

        setBtnsEnabled(false);
        updateConnectMenuText();

    }

    private void setBtnsEnabled(boolean state) {
        if (btnGetParams.isEnabled() == state)
            return;

        btnGetParams.setEnabled(state); 
        btnWriteParams.setEnabled(state);
        btnSaveToFile.setEnabled(state);
        btnLoadFromFile.setEnabled(state);
        btnFind.setEnabled(state);
    }

    @Override
    public void onComError(String errMsg) {
        setActivity(errMsg, StatusLed.LEVEL_1);
        setBtnsEnabled(false);
    }
}