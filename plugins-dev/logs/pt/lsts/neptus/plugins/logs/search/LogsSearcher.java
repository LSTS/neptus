package pt.lsts.neptus.plugins.logs.search;

import com.google.common.eventbus.Subscribe;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.ftp.FtpDownloader;
import pt.lsts.neptus.mp.MapChangeEvent;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.plugins.*;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.types.map.ParallelepipedElement;
import pt.lsts.neptus.util.GuiUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

import pt.lsts.neptus.plugins.Popup;

/**\
 * @author tsmarques
 * @date 3/14/17
 */
@PluginDescription(name = "Logs Searcher")
@Popup(width = 500, height = 600)
public class LogsSearcher extends ConsolePanel {
    private static final int WIDTH = 500;
    private static final int HEIGHT = 550;

    enum DataOptionEnum {
        ANY("--any--"),
        MULTIBEAM("multibeam"),
        SIDESCAN("sidescan"),
        CAMERA("camera"),
        PH("ph"),
        CTD("ctd"),
        REDOX("redox");

        private String dataStr;
        DataOptionEnum(String dataStr) {
            this.dataStr = dataStr;
        }

        @Override
        public String toString() {
            return this.dataStr;
        }
    }

    private final String FTP_HOST = "10.0.2.70";
    private final int FTP_PORT = 2121;
    private final String FTP_BASE_DIR = "/home/tsm/ws/lsts/";
    private static final File LOGS_DOWNLOAD_DIR = new File(System.getProperty("user.dir") + "/.cache/logs-searcher/");
    private final LogsDbHandler db = new LogsDbHandler();

    private final JPanel mainPanel = new JPanel();
    private final MigLayout mainLayout = new MigLayout("ins 0, gap 0", "[][grow]", "[top][grow]");
    private final JPanel queryPanel = new JPanel();
    private final JPanel resultsPanel = new JPanel();

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
    private boolean firstConnection = true;

    private AreaSelectionDialog areaSelectionDialog;

    /**
     * Open LogsDataSearcher from MRA
     * */
    public LogsSearcher(ConsoleLayout console) {
        super(console);
        buildGui();

        if(!LOGS_DOWNLOAD_DIR.exists()) {
            NeptusLog.pub().info("Creating logs cache directory at " + LOGS_DOWNLOAD_DIR.getAbsolutePath());
            LOGS_DOWNLOAD_DIR.mkdirs();
        }

       areaSelectionDialog = new AreaSelectionDialog(mainPanel);
    }

    @Override
    public void cleanSubPanel() {

    }

    @Override
    public void initSubPanel() {
        startConnections();
    }

    @Periodic(millisBetweenUpdates = 3000)
    public void onPeriodicCall() {
        if(ftp == null || db == null)
            return;

        if(db.isConnected() && ftpClient.isConnected() && firstConnection) {
            firstConnection = false;
            initQueryOptions();
        }


        if(db.isConnected())
            return;

        db.connect();

        if(ftpClient.isConnected())
            return;

        try {
            ftp.renewClient();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ftpClient = ftp.getClient();
    }

    private void startConnections() {
        new Thread(() -> {
            db.connect();
            connectFtp();
            initQueryOptions();
        }).start();
    }

    public void connectFtp() {
        try {
            if(ftp == null || !ftp.isConnected()) {
                ftp = new FtpDownloader(FTP_HOST, FTP_PORT);
                ftp.renewClient();
                ftpClient = ftp.getClient();
            }
        } catch (Exception e) {
            e.printStackTrace();
            ftp = null;
        }
    }

    public void buildGui() {
        initMainPanel();
        initQueryPanel();
        initResultsPanel();

        mainPanel.add(queryPanel, "alignx center, w 47px,h 55px, spanx, wrap");
        mainPanel.add(resultsPanel, "w 100%, h 100%");

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

        resultsTable.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent me) {
                JTable table =(JTable) me.getSource();
                Point p = me.getPoint();
                int rowIndex = table.rowAtPoint(p);
                if (me.getClickCount() == 2) {
                    if (rowIndex == -1) {
                        GuiUtils.errorMessage(mainPanel, "Log Selection Error", "No log selected");
                        return;
                    }

                    // get log path
                    String logDate = (String) tableModel.getValueAt(rowIndex, 1);
                    String logName = (String) tableModel.getValueAt(rowIndex, 4);
                    String logPath = logsPath.get(logDate + logName);
                    if (logDate == null || logName == null || logDate == null) {
                        GuiUtils.errorMessage(mainPanel, "Log Selection Error", "Error while accessing column" + rowIndex);
                        return;
                    }

                    openLog(logPath);
                }
            }
        });

        resultsTable.setModel(tableModel);

        scrollPane.setViewportView(resultsTable);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);


        resultsPanel.add(scrollPane);
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

            // perform query adn update resultsTable
            query(selectedDataTypeStr, selectedYearStr, selectedVehicleStr);
            //query(null, null, null);
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
            searchLogsByCoordinates(selectedDataTypeStr, selectedVehicleStr, selectedYearStr, minCoordsRad, maxCoordsRad);
        });
    }

    private void initQueryOptions() {
        DefaultListCellRenderer dlcr = new DefaultListCellRenderer();
        dlcr.setHorizontalAlignment(DefaultListCellRenderer.CENTER);

        // data type options
        dataOptions.setRenderer(dlcr);
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

    @Subscribe
    public void mapChanged(MapChangeEvent event) {
        if(event == null || event.getChangedObject() == null)
            return;

        // only care about areas
        if(!(event.getChangedObject() instanceof ParallelepipedElement))
            return;

        if(event.getEventType() == MapChangeEvent.OBJECT_ADDED) {
            String objectId = event.getChangedObject().getId();
            if(knownMapAreas.contains(objectId))
                return;

            knownMapAreas.add(objectId);
        }
    }

    private void query(String payload, String year, String vehicleId) {
        String query = buildQuery(payload, year, vehicleId);
        updateEntries(db.doQuery(query));
    }

    /**
     * Build query string based on user's selected options
     * */
    public String buildQuery(String payload, String year, String vehicleId) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM " + LogsDbHandler.DbTableName.LOGS.toString() + " WHERE ");
        sb.append(buildWhereStatement(payload, year, vehicleId));
        NeptusLog.pub().info(sb.toString());

        return sb.toString();
    }

    private String buildWhereStatement(String payload, String year, String vehicleId) {
        StringBuilder sb = new StringBuilder();

        boolean searchByPayload = false;
        boolean searchByYear = false;

        if(!payload.equals(LogsSearcher.DataOptionEnum.ANY.toString())) {
            sb.append(LogsDbHandler.LogTableColumnName.DATA_TYPE.toString() + "=" + "\"" + payload + "\"");
            searchByPayload = true;
        }

        if(!year.equals(LogsSearcher.DataOptionEnum.ANY.toString())) {
            if(searchByPayload)
                sb.append(" AND ");
            sb.append(LogsDbHandler.LogTableColumnName.YEAR.toString() + "=" + "\"" + year + "\"");
            searchByYear = true;
        }

        if(!vehicleId.equals(LogsSearcher.DataOptionEnum.ANY.toString())) {
            if(searchByPayload || searchByYear)
                sb.append(" AND ");
            sb.append(LogsDbHandler.LogTableColumnName.VEHICLE_ID.toString() + "=" + "\"" + vehicleId + "\"");
        }

        return sb.toString();
    }

    private void searchLogsByCoordinates(String selectedDataTypeStr, String selectedVehicleStr, String selectedYearStr,
                                         double[] minCoordinatesRad, double[] maxCoordinatesRad) {
        String query = "SELECT * FROM log WHERE (lat < " + maxCoordinatesRad[0] +
                " and lat > " + minCoordinatesRad[0] + " and " + " lon > " + minCoordinatesRad[1] + " and " +
                " lon < " + maxCoordinatesRad[1] + ")";

        String optionsQuery = buildWhereStatement(selectedDataTypeStr, selectedYearStr, selectedVehicleStr);

        String finalQuery;
        if(optionsQuery.isEmpty())
            finalQuery = query + ";";
        else
            finalQuery = query + " AND " + optionsQuery + ";";

        updateEntries(db.doQuery(finalQuery));
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
                String path = res.getString(LogsDbHandler.LogTableColumnName.PATH.toString());

                String logName = res.getString(LogsDbHandler.LogTableColumnName.LOG_NAME.toString());

                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(Long.parseLong(res.getString(LogsDbHandler.LogTableColumnName.DATE.toString())));
                String logDate = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);

                String vehicle = res.getString(LogsDbHandler.LogTableColumnName.VEHICLE_ID.toString());
                String dataType = res.getString(LogsDbHandler.LogTableColumnName.DATA_TYPE.toString());
                long durationMillis = Long.parseLong(res.getString(LogsDbHandler.LogTableColumnName.DURATION_MILLIS.toString()));
                double durationMin = Math.round((durationMillis / 1000.0 / 60.0) * 10.0) / 10.0;

                tableModel.addRow(new Object[]{dataType, logDate, vehicle, durationMin, logName});
                logsPath.put(logDate + logName, path);
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
        if(!ftp.isConnected())
            return;

        // Fetch file from remote (FTP?)
        File logFile = null;
        try {
            logFile = fetchLog(logAbsolutePathStr);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(logFile == null) {
            GuiUtils.errorMessage(mainPanel, "Log Selection Error", "Couldn't download file");
            return;
        }

        NeptusMRA.showApplication().getMraFilesHandler().openLog(logFile);
    }

    /**
     * Fetch remote logs
     * */
    private File fetchLog(String logRemoteAbsolutePath) throws IOException {
        File logParentRemoteDir = new File(new File(logRemoteAbsolutePath).getParent());
        File logParentLocalDir = new File(LOGS_DOWNLOAD_DIR + "/" + logParentRemoteDir.getAbsolutePath().split(FTP_BASE_DIR)[1]);
        File localLogAbsolutePath = new File(logParentLocalDir.getAbsolutePath() + "/Data.lsf.gz");

        // log already exists, no need to re-download
        // not using checksum
        if(localLogAbsolutePath.exists()) {
            NeptusLog.pub().info("Log " + logParentLocalDir.getAbsolutePath() + " already exists");
            return localLogAbsolutePath;
        }

        logParentLocalDir.mkdirs();

        String ftpRootDir = ftpClient.printWorkingDirectory();
        String baseDir = logParentRemoteDir.getAbsolutePath().split(FTP_BASE_DIR)[1];
        ftpClient.changeWorkingDirectory(baseDir);
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
}
