package pt.lsts.neptus.plugins.logs.search;

import com.google.common.eventbus.Subscribe;
import jdk.nashorn.internal.scripts.JO;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.io.FileUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import pt.lsts.imc.lsf.LsfMerge;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.ftp.FtpDownloader;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.plugins.*;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import pt.lsts.neptus.plugins.Popup;

/**\
 * @author tsmarques
 * @date 3/14/17
 */
@PluginDescription(name = "Logs Searcher")
@Popup(width = 500, height = 650)
public class LogsSearcher extends ConsolePanel {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 600;

    enum DataOptionEnum {
        ANY("--any--"),
        MULTIBEAM("multibeam"),
        SIDESCAN("sidescan"),
        CAMERA("camera"),
        PH("ph"),
        CTD("ctd"),
        REDOX("redox"),
        DVL("dvl"),
        FLUORESCEIN("fluorescein"),
        RHODAMINE("rhodamine");

        private String dataStr;
        DataOptionEnum(String dataStr) {
            this.dataStr = dataStr;
        }

        @Override
        public String toString() {
            return this.dataStr;
        }
    }

    private static final File LOGS_DOWNLOAD_DIR = new File(System.getProperty("user.dir") + "/log/logs-searcher/");

    private final ConnConfig connConfig = new ConnConfig();
    private final LogsDbHandler db = new LogsDbHandler();

    private final LsfMerge logsMerger = new LsfMerge();

    private final JPanel mainPanel = new JPanel();
    private final MigLayout mainLayout = new MigLayout("ins 0, gap 0", "[][grow]", "[top][grow]");
    private final JPanel queryPanel = new JPanel();
    private final JPanel resultsPanel = new JPanel();
    private final LogsProgressMonitor logsProgressMonitor = new LogsProgressMonitor(mainPanel);
    private final JMenuBar menuBar = new JMenuBar();
    private final JMenu menu = new JMenu("Options");
    private final JMenuItem loginMenu = new JMenuItem("Login");
    private final JMenuItem clearCacheMenu = new JMenuItem("Clear cache");

    private final JComboBox<String> dataOptions = new JComboBox<>();
    private final JComboBox<String> yearOptions = new JComboBox<>();
    private final JComboBox<String> vehicleOptions = new JComboBox<>();

    private final HashSet<String> knownMapAreas = new HashSet<>();
    private final HashMap<String, String> logsPath = new HashMap<>();

    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private final JScrollPane scrollPane = new JScrollPane();

    private final JButton searchButton = new JButton("Search");
    private final JButton clearResultsButton = new JButton("Clear results");
    private final JButton selectAreaButton = new JButton("Search by Area");

    private FtpDownloader ftp = null;
    private FTPClient ftpClient;

    private AreaSelectionDialog areaSelectionDialog;

    /**
     * Open LogsDataSearcher from MRA
     * */
    public LogsSearcher(ConsoleLayout console) {
        super(console);

        if(!connConfig.isValid)
            GuiUtils.errorMessage("Error", "Configuration error. Fix it and restart the plugin");
        else
            buildGui();
    }

    @Override
    public void cleanSubPanel() {
        if(db != null)
            db.close();
    }

    @Override
    public void initSubPanel() {
    }

    private void showAuthenticationPanel() {
        JPanel authPanel = new JPanel();
        authPanel.setLayout(new GridLayout(2, 2));

        JTextField userName = new JTextField();
        JPasswordField pwField = new JPasswordField();

        JLabel userNameLabel = new JLabel("User: ");
        JLabel pwLabel = new JLabel("Password: ");

        authPanel.add(userNameLabel);
        authPanel.add(userName);
        authPanel.add(pwLabel);
        authPanel.add(pwField);

        JOptionPane.showMessageDialog(mainPanel, authPanel);

        char[] pw = pwField.getPassword();
        db.connect(connConfig.dbHost, connConfig.dbPort, userName.getText(), String.valueOf(pw));

        if(db.isConnected()) {
            pwField.setText("");
            userName.setText("");
            Arrays.fill(pw, '0');

            buildGui();
            if (!LOGS_DOWNLOAD_DIR.exists()) {
                NeptusLog.pub().info("Creating logs cache directory at " + LOGS_DOWNLOAD_DIR.getAbsolutePath());
                LOGS_DOWNLOAD_DIR.mkdirs();
            }

            areaSelectionDialog = new AreaSelectionDialog(mainPanel);
            initQueryOptions();
        }
        else
            GuiUtils.errorMessage("Error", "Computer says no...");
    }

    public boolean connectFtp() {
        try {
            if(ftp == null)
                ftp = new FtpDownloader(connConfig.ftpHost, connConfig.ftpPort);

            ftp.renewClient();
            ftpClient = ftp.getClient();

            if(ftpClient != null)
                return ftpClient.isConnected();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public void buildGui() {
        initMainPanel();
        initQueryPanel();
        initResultsPanel();

        loginMenu.addActionListener(e -> showAuthenticationPanel());
        clearCacheMenu.addActionListener(e -> clearCachedLogs());

        menu.add(loginMenu);
        menu.add(clearCacheMenu);
        menuBar.add(menu);
        mainPanel.add(menuBar, "w 90px, h 20px, spanx, wrap");
        mainPanel.add(queryPanel, "alignx center, w 47px, h 55px, spanx, wrap");
        mainPanel.add(resultsPanel, "w 100%, h 90%");

        this.add(mainPanel);
    }

    private void initMainPanel() {
        this.setSize(new Dimension(WIDTH, HEIGHT));
        mainPanel.setSize(new Dimension(this.getWidth(), this.getHeight()));
        mainPanel.setLayout(mainLayout);
    }

    private void initResultsPanel() {
        tableModel = new DefaultTableModel(new Object[]{"Data", "Date", "Vehicle", "Duration (m)", "Name"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class getColumnClass(int column) {
                switch (column) {
                    case 0:
                        return String.class;
                    case 1:
                        return Date.class;
                    case 2:
                        return String.class;
                    case 3:
                        return Double.class;
                    case 4:
                        return String.class;
                    default:
                        return String.class;
                }
            }
        };

        resultsTable = new JTable() {
            @Override
            public String getToolTipText(MouseEvent e) {
                String tip;
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);

                try {
                    tip = getValueAt(rowIndex, colIndex).toString();
                } catch (RuntimeException e1) {
                    return "";
                }

                return tip;
            }
        };

        resultsTable.setRowSelectionAllowed(true);
        resultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        TableRowSorter<TableModel> sorter
                = new TableRowSorter<>(tableModel);
        resultsTable.setRowSorter(sorter);

        resultsTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table =(JTable) me.getSource();
                int rowIndex = table.convertRowIndexToModel(table.getSelectedRow());
                if (me.getClickCount() == 2) {
                    if (rowIndex == -1) {
                        GuiUtils.errorMessage(mainPanel, "Log Selection Error", "No log selected");
                        return;
                    }

                    // get log path
                    Date logDate = (Date) tableModel.getValueAt(rowIndex, 1);
                    String logName = (String) tableModel.getValueAt(rowIndex, 4);
                    String dataType = (String) tableModel.getValueAt(rowIndex, 0);
                    double logDurationMin = (double) tableModel.getValueAt(rowIndex, 3);

                    if (logDate == null || logName == null) {
                        GuiUtils.errorMessage(mainPanel, "Log Selection Error", "Error while accessing column" + rowIndex);
                        return;
                    }

                    // fetch logPath
                    String logPath = logsPath.get(logName + dataType + logDurationMin);
                    new Thread(() -> openLog(logPath)).start();
                }
                else if (SwingUtilities.isRightMouseButton(me)) {
                    int[] selectedRows = table.getSelectedRows();

                    if(selectedRows.length < 2)
                        return;

                    int res = JOptionPane.showConfirmDialog(mainPanel, "Concatenate and open selected logs? This might take time",
                            "Multiple logs", JOptionPane.YES_NO_OPTION);

                    if(res != JOptionPane.OK_OPTION)
                        return;

                    new Thread(() -> {
                        try {
                            handleLogsConcatenation(selectedRows);

                        } catch (Exception e) {
                            GuiUtils.errorMessage(mainPanel, "Multiple Logs", "There's been an error, check logs!");
                            e.printStackTrace();
                        }

                        logsProgressMonitor.close();
                    }).start();
                }
            }
        });

        resultsTable.setModel(tableModel);

        scrollPane.setViewportView(resultsTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);


        resultsPanel.add(scrollPane);
    }

    private void handleLogsConcatenation(int[] selectedRows) throws Exception {
        ArrayList<File> files = new ArrayList<>(selectedRows.length);
        StringBuilder sb = new StringBuilder();
        sb.append(LOGS_DOWNLOAD_DIR + "/merged");

        setStatus(true, "Downloading logs");
        int i = 0;
        for(int row : selectedRows) {
            int rowIndex = resultsTable.convertRowIndexToModel(row);
            files.add(fetchLog(getLogPathAt(rowIndex)));

            String logName = (String) tableModel.getValueAt(rowIndex, 4);
            sb.append("-" + logName);
            i++;
        }

        setStatus(true, "Merging logs");
        File destLogParent = new File(sb.toString());
        File destLogPath = new File(sb.toString() + "/Data.lsf");
        if(!destLogPath.exists()) {
            destLogParent.mkdirs();
            try {
                logsMerger.merge(files, destLogPath, new ArrayList<>(), (s, i1) -> {});
                // copy IMC version
                FileUtil.copyFile(files.get(0).getParent() + "/IMC.xml.gz", destLogParent.getAbsolutePath() + "/IMC.xml.gz");
            } catch(Exception e) {
                e.printStackTrace();
                setStatus(false, "");
                GuiUtils.errorMessage(mainPanel, "Error", "Error while merging logs");

                return;
            }
        }

        setStatus(false, "");
        NeptusMRA.showApplication().getMraFilesHandler().openLog(destLogPath);
    }

    private String getLogPathAt(int rowIndex) {
        String logName = (String) tableModel.getValueAt(rowIndex, 4);
        String dataType = (String) tableModel.getValueAt(rowIndex, 0);
        double logDurationMin = (double) tableModel.getValueAt(rowIndex, 3);

        // fetch logPath
        return logsPath.get(logName + dataType + logDurationMin);
    }

    private void initQueryPanel() {
        queryPanel.setLayout(new GridLayout(2,3));
        queryPanel.add(dataOptions);
        queryPanel.add(vehicleOptions);
        queryPanel.add(yearOptions);

        queryPanel.add(selectAreaButton);
        queryPanel.add(searchButton);
        queryPanel.add(clearResultsButton);


        searchButton.addActionListener(e -> {
            String selectedDataTypeStr = String.valueOf(dataOptions.getSelectedItem());
            String selectedYearStr = String.valueOf(yearOptions.getSelectedItem());
            String selectedVehicleStr = String.valueOf(vehicleOptions.getSelectedItem());

            if(selectedDataTypeStr == null || selectedYearStr == null || selectedVehicleStr == null) {
                GuiUtils.errorMessage(mainPanel, "Log Selection Error", "Null option");
                return;
            }

            updateEntries(db.doQuery(selectedDataTypeStr, selectedYearStr, selectedVehicleStr));
        });

        clearResultsButton.addActionListener(e -> {
            tableModel.setRowCount(0);
            logsPath.clear();
        });

        selectAreaButton.addActionListener(e -> {
            int status = areaSelectionDialog.getInput();

            if(status == AreaSelectionDialog.INVALID_INPUT) {
                GuiUtils.errorMessage(this, "Error", "Invalid Coordinates");
                return;
            }

            if(status == AreaSelectionDialog.CLOSED)
                return;

            String selectedDataTypeStr = String.valueOf(dataOptions.getSelectedItem());
            String selectedYearStr = String.valueOf(yearOptions.getSelectedItem());
            String selectedVehicleStr = String.valueOf(vehicleOptions.getSelectedItem());

            if(selectedDataTypeStr == null || selectedYearStr == null || selectedVehicleStr == null) {
                GuiUtils.errorMessage(mainPanel, "Log Selection Error", "Null option");
                return;
            }

            double[] minCoordsRad = areaSelectionDialog.getMinCoordinatesRad();
            double[] maxCoordsRad = areaSelectionDialog.getMaxCoordinatesRad();
            ResultSet res = db.searchLogsByCoordinates(selectedDataTypeStr, selectedVehicleStr, selectedYearStr,
                    minCoordsRad, maxCoordsRad);

            updateEntries(res);
        });
    }

    private void initQueryOptions() {
        DefaultListCellRenderer dlcr = new DefaultListCellRenderer();
        dlcr.setHorizontalAlignment(DefaultListCellRenderer.CENTER);

        // data type options
        dataOptions.setRenderer(dlcr);
        ((DefaultComboBoxModel) dataOptions.getModel()).addElement("--any--");

        db.fetchAvailableDataType().stream()
                .forEach(opt -> ((DefaultComboBoxModel) dataOptions.getModel()).addElement(opt));

        // available years
        dlcr = new DefaultListCellRenderer();
        dlcr.setHorizontalAlignment(DefaultListCellRenderer.CENTER);
        yearOptions.setRenderer(dlcr);
        ((DefaultComboBoxModel) yearOptions.getModel()).addElement("--any--");

        db.fetchAvailableYears().stream()
                .forEach(y -> ((DefaultComboBoxModel) yearOptions.getModel()).addElement(y));

        // vehicles' ids
        dlcr = new DefaultListCellRenderer();
        dlcr.setHorizontalAlignment(DefaultListCellRenderer.CENTER);
        vehicleOptions.setRenderer(dlcr);

        ((DefaultComboBoxModel) vehicleOptions.getModel()).addElement("--any--");
        db.fetchAvailableVehicles().stream()
                .forEach(v -> ((DefaultComboBoxModel) vehicleOptions.getModel()).addElement(v));
    }

    /**
     * Parse query results and update results table
     * */
    private void updateEntries(ResultSet res) {
        if(res == null)
            return;

        try {
            boolean isEmpty = true;
            tableModel.setRowCount(0);
            logsPath.clear();

            while(res.next()) {
                isEmpty = false;
                String path = connConfig.ftpBaseDir + res.getString(LogsDbHandler.LogTableColumnName.PATH.toString());

                String logName = res.getString(LogsDbHandler.LogTableColumnName.LOG_NAME.toString());

                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(Long.parseLong(res.getString(LogsDbHandler.LogTableColumnName.DATE.toString())));
                Date d = new Date(Long.parseLong(res.getString(LogsDbHandler.LogTableColumnName.DATE.toString())));

                String vehicle = res.getString(LogsDbHandler.LogTableColumnName.VEHICLE_ID.toString());
                String dataType = res.getString(LogsDbHandler.LogTableColumnName.DATA_TYPE.toString());
                long durationMillis = Long.parseLong(res.getString(LogsDbHandler.LogTableColumnName.DURATION_MILLIS.toString()));
                double durationMin = Math.round((durationMillis / 1000.0 / 60.0) * 10.0) / 10.0;

                tableModel.addRow(new Object[]{dataType, d, vehicle, durationMin, logName});
                logsPath.put(logName + dataType + durationMin, path);
            }

            if(isEmpty)
                GuiUtils.infoMessage(mainPanel, "Query Results", "No results found");
        } catch (SQLException e) {
            GuiUtils.errorMessage(mainPanel, "Error", "Error while parsing results");
            e.printStackTrace();
        }
    }

    /**
     * Open a selected log in MRA
     * */
    private void openLog(String logAbsolutePathStr) {
        setStatus(true, "Downloading logs");

        // Fetch file from remote (FTP?)
        File logFile = null;
        try {
            logFile = fetchLog(logAbsolutePathStr);
            ftpClient.disconnect();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(logFile == null) {
            GuiUtils.errorMessage(mainPanel, "Log Selection Error", "Couldn't download file");
            logsProgressMonitor.close();
            return;
        }

        setStatus(false, "");
        NeptusMRA.showApplication().getMraFilesHandler().openLog(logFile);
    }

    /**
     * Fetch remote logs
     * */
    private File fetchLog(String logRemoteAbsolutePath) throws IOException {
        if(!connectFtp()) {
            GuiUtils.errorMessage(mainPanel, "Connection error", "No FTP connection");
            return null;
        }

        String logParentRemoteDir = logRemoteAbsolutePath.split("/Data.lsf.gz")[0];
        String logParentLocalDirStr = LOGS_DOWNLOAD_DIR + "/" + logParentRemoteDir.split(connConfig.ftpBaseDir)[1];
        File localLogAbsolutePath = new File(logParentLocalDirStr + "/Data.lsf.gz");

        NeptusLog.pub().info(logParentRemoteDir);
        NeptusLog.pub().info(logParentLocalDirStr);

        // log already exists, no need to re-download
        // not using checksum
        if(localLogAbsolutePath.exists()) {
            NeptusLog.pub().info("Log " + logParentLocalDirStr + " already exists");
            return localLogAbsolutePath;
        }

        File logParentLocalDir = new File(logParentLocalDirStr);
        logParentLocalDir.mkdirs();

        String ftpRootDir = ftpClient.printWorkingDirectory();
        String baseDir = logParentRemoteDir.split(connConfig.ftpBaseDir)[1];


        if(!ftpClient.changeWorkingDirectory(baseDir)) {
            NeptusLog.pub().error("Couldn't move to remote: " + baseDir);
            return null;
        }

        StringBuilder sb = null;

        for(FTPFile ftpFile : ftpClient.listFiles()) {
            if(ftpFile.getName().equals("mra"))
                continue;
            File downloadedFile = new File(LOGS_DOWNLOAD_DIR + "/" + baseDir + "/" + ftpFile.getName());
            OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadedFile));
            boolean success = ftpClient.retrieveFile(ftpFile.getName(), outputStream);
            outputStream.close();

            if(!success) {
                if(sb == null)
                    sb = new StringBuilder();
                sb.append(ftpFile.getName() + "\n");
            }
        }
        // reset ftp root dir
        ftpClient.changeWorkingDirectory(ftpRootDir);

        if(sb != null)
            GuiUtils.errorMessage(mainPanel, "Download Error", "The following files couldn't failed to download: \n"
                    + sb.toString());

        return localLogAbsolutePath;
    }

    /**
     * Remove all downloaded and merged logs
     * */
    private void clearCachedLogs() {
        try {
            FileUtils.cleanDirectory(LOGS_DOWNLOAD_DIR);
        } catch (IOException e) {
            GuiUtils.errorMessage("Error", e.getMessage());
        }
    }

    private void setStatus(boolean isActivated, String message) {
        if (isActivated) {
            logsProgressMonitor.open("Neptus: Logs Searcher status", message);
        }
        else
            logsProgressMonitor.close();
    }
}
