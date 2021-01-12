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
 * 2009/09/19
 */
package pt.lsts.neptus.plugins.logs;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingWorker;

import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.painter.CompoundPainter;
import org.jdesktop.swingx.painter.GlossPainter;
import org.jdesktop.swingx.painter.Painter;

import com.google.common.eventbus.Subscribe;

import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.comm.manager.imc.ImcSystem;
import pt.lsts.neptus.comm.manager.imc.ImcSystemsHolder;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.ToolbarButton;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusMessageListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.util.DateTimeUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.NetworkInterfacesUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.logdownload.LogFileInfoList;
import pt.lsts.neptus.util.logdownload.LogFolderInfo;
import pt.lsts.neptus.util.logdownload.LogFolderInfo.State;
import pt.lsts.neptus.util.logdownload.LogsDownloaderWorker;

/**
 * @author pdias
 * 
 */
@SuppressWarnings("serial")
@Popup( pos = POSITION.RIGHT, width=300, height=100, accelerator='L')
@PluginDescription(author = "Paulo Dias and José Pinto", name = "Log Download", description = "", version = "3.0.0", icon = "pt/lsts/neptus/plugins/logs/log.png", documentation = "logs-downloader/logs-downloader.html#console")
public class LoggingDownloader extends ConsolePanel implements MainVehicleChangeListener, IPeriodicUpdates,
        ConfigurationListener, NeptusMessageListener {

    private final ImageIcon ICON = ImageUtils.getIcon("pt/lsts/neptus/plugins/logs/log.png");

    @NeptusProperty(name = "Log List Update Period in Seconds", userLevel = LEVEL.REGULAR)
    public short logListUpdatePeriodSeconds = 30;

    @NeptusProperty(name = "Automatic Update Log List from Remote Systems", userLevel = LEVEL.REGULAR)
    public boolean automaticUpdateLogsFromRemoteSystems = false;

    @NeptusProperty(name = "Change Log Name Enabled")
    public boolean changeLogNameEnabled = true;

    @NeptusProperty(name = "Component Update Period in Seconds", userLevel = LEVEL.ADVANCED)
    public short updatePeriodSeconds = 20;

    @NeptusProperty(name = "Auto Sync. Only Non Idle Logs", userLevel = LEVEL.ADVANCED, description = "If enable only logs with names (excluding 'idle') are synchronized.")
    public boolean autoSyncOnlyNonIdleLogs = false;

    protected boolean isUpdatedListFromServer = false;

    protected Operation curState = Operation.UNKNOWN;
    protected String lastName = "";

    protected String currentName = null;

    protected Vector<String> logFoldersList = new Vector<String>();

    // UI
    protected ToolbarButton button = null;
    protected ToolbarButton showDownloader = null, syncList = null;

    protected BarPainter barSyncValue = null;

    protected AbstractAction logCtrlAction, showDownloaderAction, syncListAction;

    private final String syncListToolTipText = I18n.text("Sync day Mission Logs");
    protected double valueSync = 0;

    private final LinkedHashMap<String, LogsDownloaderWorker> downloadWorkerList = new LinkedHashMap<String, LogsDownloaderWorker>();

    private Timer timer = null;
    private TimerTask ttaskUpdateLogList = null, ttaskUpdateLogName = null;

    private long timeRequestLogList = -1;

    private JFrame logsFrame;
    private JTabbedPane tabbledPane;

    public enum Operation {
        REQUEST_START(0),
        STARTED(1),
        REQUEST_STOP(2),
        STOPPED(3),
        REQUEST_CURRENT_NAME(4),
        CURRENT_NAME(5),
        UNKNOWN(-1);

        private final int c;

        Operation(int c) {
            this.c = c;
        }

        public int type() {
            return c;
        }
    }

    public LoggingDownloader(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        initializeActions();

        removeAll();

        button = new ToolbarButton(logCtrlAction); // ToolbarSwitch(ICON, "Log: (updating)", "logcontrol")
        // button.addActionListener(logCtrlAction);
        button.setEnabled(changeLogNameEnabled);

        syncList = new ToolbarButton(syncListAction);
        syncList.setToolTipText(syncListToolTipText);
        // / This should be a very short phrase. This is a button to sync day logs. The ideal size is to match the
        // letters number.
        syncList.setText(I18n.text("Sync Logs"));
        @SuppressWarnings("unchecked")
        Painter<JXButton> obp = syncList.getBackgroundPainter(); // this seam to be null
        barSyncValue = new BarPainter();
        if (obp != null)
            syncList.setBackgroundPainter(new CompoundPainter<JXButton>(obp, barSyncValue, new GlossPainter()));
        else
            syncList.setBackgroundPainter(new CompoundPainter<JXButton>(barSyncValue, new GlossPainter()));

        showDownloader = new ToolbarButton(showDownloaderAction);
        showDownloader.setToolTipText(I18n.text("Show Downloader"));

        setLayout(new BorderLayout());

        JXPanel dlpanel = new JXPanel();
        dlpanel.setLayout(new FlowLayout());

        dlpanel.add(button);
        dlpanel.add(syncList);
        dlpanel.add(showDownloader);

        add(dlpanel, BorderLayout.CENTER);

        tabbledPane = new JTabbedPane();
        if (getMainVehicleId() != null && !"".equalsIgnoreCase(getMainVehicleId())) {
            tabbledPane.setSelectedComponent(getDownloadWorker().getContentPanel());
        }

        logsFrame = new JFrame(I18n.text("Download Log Files"));
        logsFrame.setSize(900, 560);
        logsFrame.setIconImages(ConfigFetch.getIconImagesForFrames());
        logsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        logsFrame.setLayout(new BorderLayout());
        logsFrame.add(tabbledPane);
    }

    private void initializeActions() {
        logCtrlAction = new AbstractAction(I18n.text("Logging Control"), ICON) { // ICON_STOPPED
            @Override
            public void actionPerformed(ActionEvent e) {
                String name = JOptionPane.showInputDialog(getConsole(),
                        I18n.text("Please enter the desired log name (empty for default)"), lastName);
                if (name == null) {
                    return;
                }
                else {
                    lastName = name;
                }
                send(IMCDefinition.getInstance().create("LoggingControl", "op", Operation.REQUEST_START.type(), "name",
                        name));
            }
        };

        showDownloaderAction = new AbstractAction(I18n.text("Show Downloader"),
                LogsDownloaderWorker.getIcon()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                tabbledPane.setSelectedComponent(getDownloadWorker().getContentPanel());
                getDownloadWorker().setVisible(true);
            }
        };

        syncListAction = new AbstractAction(I18n.text("Sync Logs")) {
            @Override
            public void actionPerformed(ActionEvent e) {
                syncList.setEnabled(false);
                boolean noListRqst = true;
                if (!automaticUpdateLogsFromRemoteSystems) {
                    noListRqst = !scheduleDownloadListFromServer();
                }

                Timer timer = new Timer("Sync Logs");
                TimerTask ttask = new TimerTask() {
                    @Override
                    public void run() {
                        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                            @Override
                            protected Void doInBackground() throws Exception {
                                LogsDownloaderWorker dw = getDownloadWorker();
                                // getDownloadWorker().doUpdateListFromServer();
                                String[] listFolders = dw.doGiveListOfLogFolders();
                                Vector<String> ltdw = new Vector<String>();
                                for (String str : listFolders) {
                                    if (isMissionLogFromToday(str)) {
                                        ltdw.add(str);
                                    }
                                }

                                // Inverse sort
                                Collections.sort(ltdw, new Comparator<String>() {
                                    @Override
                                    public int compare(String o1, String o2) {
                                        return o2.compareTo(o1);
                                    }
                                });

                                dw.doDownloadLogFoldersFromServer(ltdw.toArray(new String[0]));

                                return null;
                            }

                            @Override
                            protected void done() {
                                try {
                                    get();
                                }
                                catch (Exception e) {
                                    NeptusLog.pub().error(e);
                                }
                                syncList.setEnabled(true);
                            };
                        };
                        worker.execute();
                        // worker.addPropertyChangeListener(new PropertyChangeListener() {
                        // @Override
                        // public void propertyChange(PropertyChangeEvent evt) {
                        // evt.get
                        // }
                        // });
                    }
                };
                timer.schedule(ttask, noListRqst ? 10 : 5000);
            }
        };
    }

    @Override
    public void initSubPanel() {
        if (getConsole().getMainSystem() == null) {
            button.setEnabled(false);
            button.setToolTipText(I18n.text("Log: (communications not started)"));
        }
        else {
            resetDownloaderForVehicle(getConsole().getMainSystem());

            send(IMCDefinition.getInstance().create("LoggingControl", "op", Operation.REQUEST_CURRENT_NAME.type()));
            button.setEnabled(changeLogNameEnabled);
            button.setToolTipText(I18n.text("Log: (updating...)"));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#clean()
     */
    @Override
    public void cleanSubPanel() {
        if (ttaskUpdateLogList != null) {
            ttaskUpdateLogList.cancel();
            ttaskUpdateLogList = null;
        }
        revokeScheduleCurLogFromVehicle();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        // if (downloadWorker != null) {
        // downloadWorker.cleanup();
        // }
        for (LogsDownloaderWorker downloadWorker : downloadWorkerList.values()) {
            downloadWorker.cleanup();
        }
        logsFrame.dispose();
//        if (logsFrame != null) {
//            logsFrame.setVisible(false);
//            SwingUtilities.invokeLater(new Runnable() {
//                
//                @Override
//                public void run() {
//                    logsFrame.dispose();
//                }
//            });            
//        }
    }

    /**
	 * 
	 */
    private void updateLogsState() {
        LogsDownloaderWorker dw = getDownloadWorker();
        if (dw == null)
            return;
        String[] listFolders;
        try {
            listFolders = dw.doGiveListOfLogFolders();
        }
        catch (Exception e) {
            NeptusLog.pub().warn(e);
            return;
        }
        long nTotal = 0, nDownloading = 0, nError = 0, nNew = 0, nIncomplete = 0, nSync = 0, nUnknown = 0;
        // NeptusLog.pub().info("<###>listFolders  filter: "+listFolders.length);
        for (String strLFd : listFolders) {
            if (!isMissionLogFromToday(strLFd))
                continue;
            LinkedHashMap<String, State> sfiles = dw.doGiveStateOfLogFolderFiles(strLFd);
            // NeptusLog.pub().info("<###> "+strLFd+"  filter: "+sfiles.size());
            for (String strFx : sfiles.keySet()) {
                if (sfiles.get(strFx) != LogFolderInfo.State.LOCAL) {
                    nTotal++;
                    if (sfiles.get(strFx) == LogFolderInfo.State.SYNC) {
                        nSync++;
                    }
                    else if (sfiles.get(strFx) == LogFolderInfo.State.DOWNLOADING) {
                        nDownloading++;
                    }
                    else if (sfiles.get(strFx) == LogFolderInfo.State.ERROR) {
                        nError++;
                    }
                    else if (sfiles.get(strFx) == LogFolderInfo.State.NEW) {
                        nNew++;
                    }
                    else if (sfiles.get(strFx) == LogFolderInfo.State.INCOMPLETE) {
                        nIncomplete++;
                    }
                    else if (sfiles.get(strFx) == LogFolderInfo.State.UNKNOWN) {
                        nUnknown++;
                    }
                }
            }
        }

        Icon icon = LogFileInfoList.ICON_UNKNOWN;
        if (nTotal == 0) {
            icon = LogFileInfoList.ICON_UNKNOWN;
        }
        else if (nDownloading > 0) {
            icon = LogFileInfoList.ICON_DOWN;
        }
        else if (nError > 0) {
            icon = LogFileInfoList.ICON_ERROR;
        }
        else if (nSync == nTotal) {
            icon = LogFileInfoList.ICON_SYNC;
        }
        else if (nNew/* +nLocal */== nTotal) {
            icon = LogFileInfoList.ICON_NEW;
        }
        else if (nSync + nIncomplete + nUnknown + nNew/* nLocal */== nTotal) {
            icon = LogFileInfoList.ICON_INCOMP;
        }
        // else if (nLocal == nTotal) {
        // logFolder.setState(LogFolderInfo.State.LOCAL);
        // }
        // else if (nNew == nTotal) {
        // logFolder.setState(LogFolderInfo.State.NEW);
        // }
        else {
            icon = LogFileInfoList.ICON_UNKNOWN;
        }

        barSyncValue.setValue(1.0 * nSync / nTotal);
        syncList.setToolTipText(syncListToolTipText + " (" + nSync + " / " + nTotal + ")");
        syncList.setIcon(icon);
        syncList.repaint();
    }

    /**
     * @param logStr
     * @return
     */
    private boolean isMissionLogFromToday(String logStr) {
        if (autoSyncOnlyNonIdleLogs) {
            int idx_ = logStr.indexOf('_');
            if (idx_ == -1)
                return false;
            else if (idx_ < 15)
                return false;

            String slstr = logStr.substring(idx_ + 1, logStr.length());
            if ("".equals(slstr))
                return false;
            else if ("idle".equals(slstr))
                return false;
        }

        String dateStr = DateTimeUtil.dateFormatterNoSpaces.format(new Date(System.currentTimeMillis()));
        if (logStr.startsWith(dateStr + "/"))
            return true;
        else
            return false;
    }

    /**
     * @return the downloadWorker
     */
    public LogsDownloaderWorker getDownloadWorker() {
        return getDownloadWorker(getMainVehicleId());
    }

    public synchronized LogsDownloaderWorker getDownloadWorker(String id) {
        if (id == null || id.length() == 0) {
            NeptusLog.pub().warn("Trying to get a downloader worker for a null id!");
            return null;
        }
        
        LogsDownloaderWorker downloadWorker = downloadWorkerList.get(id);
        if (downloadWorker == null) {
            downloadWorker = new LogsDownloaderWorker(logsFrame);
            downloadWorker.setHost("");
            // downloadWorker.setPort(sys.getRemoteUDPPort());
            downloadWorker.setLogLabel(id);
            downloadWorker.setEnableHost(true);
            downloadWorker.setEnablePort(false);
            downloadWorker.setEnableLogLabel(false);
            downloadWorkerList.put(id, downloadWorker);
            tabbledPane.addTab(downloadWorker.getLogLabel(), downloadWorker.getContentPanel());
            // downloadWorker.setVisible(false);
        }
        return downloadWorker;
    }

    /**
     * @param id
     */
    private void resetDownloaderForVehicle(String id) {
        LogsDownloaderWorker dw = getDownloadWorker(id);
        if (dw == null)
            return;
        
        String oldId = getDownloadWorker(id).getLogLabel();
        if (!id.equalsIgnoreCase(oldId)) {
            try {
                ImcSystem sys3 = ImcSystemsHolder.lookupSystemByName(id);
                getDownloadWorker(id).setHost(sys3.getHostAddress());
                getDownloadWorker(id).setLogLabel(id.toLowerCase());
                getDownloadWorker(id).doReset(false);
                scheduleDownloadListFromServer();
            }
            catch (Exception e) {
                NeptusLog.pub().error("Bad log downloader settings for '" + id + "'", e);
                getDownloadWorker(id).setHost("");
                getDownloadWorker(id).setLogLabel(id.toLowerCase());
                getDownloadWorker(id).doReset(false);
            }
        }
    }

    private void updateSettingsDownloaderForVehicle() {
        for (String id : downloadWorkerList.keySet().toArray(new String[0])) {
            try {
                LogsDownloaderWorker dw = getDownloadWorker(id);
                ImcSystem sys3 = ImcSystemsHolder.lookupSystemByName(id);
                if (dw == null || sys3 == null) {
                    NeptusLog.pub().warn("Not able to get IMC System for '" + id + "'");
                    continue;
                }
                dw.setHost(sys3.getHostAddress());
                dw.setLogLabel(id.toLowerCase());

                int idx = -1;
                for (int i = 0; i < tabbledPane.getTabCount(); i++) {
                    try {
                        if (tabbledPane.getComponentAt(i).equals(dw.getContentPanel())) {
                            idx = i;
                            break;
                        }
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (idx >= 0)
                    tabbledPane.setTitleAt(idx, dw.getLogLabel());

                //Vector<URI> sUri = sys3.getServiceProvided("http", "dune");
                Vector<URI> sUri = sys3.getServiceProvided("ftp", "");
                if (sUri.size() > 0) {
                    dw.setHost(sUri.get(0).getHost());
                    dw.setPort((sUri.get(0).getPort() <= 0) ? 21 : sUri.get(0).getPort());
                }
                if (sUri.size() > 1) {
                    for (URI uriT : sUri) {
                        if (NetworkInterfacesUtil.testForReachability(uriT.getHost(), uriT.getPort())) {
                            dw.setHost(uriT.getHost());
                            dw.setPort((uriT.getPort() <= 0) ? 21 : uriT.getPort());
                            break;
                        }
                    }
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String[] getObservedMessages() {
        return new String[] { "LoggingControl" };
    }

    @Override
    public void messageArrived(IMCMessage message) {
        String op = message.getString("op");
        if (op == null)
            return;
        Operation operation = Operation.valueOf(op);
        String name = message.getString("name");

        revokeScheduleCurLogFromVehicle();
        updateState(operation, name);
    }

    /**
     * @return
     */
    private TimerTask createTimerTaskUpdateLogList() {
        return new TimerTask() {
            @Override
            public void run() {
                for (String id : downloadWorkerList.keySet().toArray(new String[0])) {
                    LogsDownloaderWorker dw = getDownloadWorker(id);
                    if (dw != null && dw.validateConfiguration()) {
                        dw.doUpdateListFromServer();
                    }
                }
                ttaskUpdateLogList = null;
            }
        };
    }

    /**
     * @return
     */
    private TimerTask createTimerTaskUpdateLogName() {
        return new TimerTask() {
            @Override
            public void run() {
                if (getConsole().getMainSystem() != null /* && ! button.isEnabled() */)
                    send(IMCDefinition.getInstance().create("LoggingControl", "op",
                            Operation.REQUEST_CURRENT_NAME.type()));
                ttaskUpdateLogName = null;
            }
        };
    }

    /**
	 * 
	 */
    private boolean scheduleDownloadListFromServer() {
        // if (ttaskUpdateLogList == null) {
        if (System.currentTimeMillis() - timeRequestLogList > 800 /* logListUpdatePeriodSeconds * 1.1 * 1000 */) {
            timeRequestLogList = System.currentTimeMillis();
            if (timer == null)
                timer = new Timer(LoggingDownloader.class.getName() + ": ScheduleDownloadListFromServer");
            ttaskUpdateLogList = createTimerTaskUpdateLogList();
            timer.schedule(ttaskUpdateLogList, 500);
            return true;
        }
        return false;
    }

    private void scheduleCurLogFromVehicle() {
        if (ttaskUpdateLogName == null) {
            if (timer == null)
                timer = new Timer(LoggingDownloader.class.getName() + ": ScheduleCurLogFromVehicle");
            ttaskUpdateLogName = createTimerTaskUpdateLogName();
            timer.schedule(ttaskUpdateLogName, 1000);
        }
    }

    private void revokeScheduleCurLogFromVehicle() {
        if (ttaskUpdateLogName != null) {
            ttaskUpdateLogName.cancel();
            ttaskUpdateLogName = null;
        }
    }

    /**
     * @param operation
     * @param argument
     */
    protected void updateState(Operation operation, String argument) {
        switch (operation) {
            case STOPPED:
                button.setSelected(false);
                button.setEnabled(true);
                button.setToolTipText(I18n.text("Log: (stopped)"));
                break;
            case STARTED:
                if (argument != null && !argument.equals(currentName)) {
                    currentName = argument;
                }
                button.setSelected(true);
                button.setEnabled(true);
                button.setToolTipText(I18n.textf("Log: %logname", argument));
                break;
            case CURRENT_NAME:
                if (argument.equals("")) {
                    button.setSelected(false);
                    button.setEnabled(true);
                    button.setToolTipText(I18n.text("Log: (stopped)"));
                }
                else {
                    button.setSelected(true);
                    button.setEnabled(true);
                    button.setToolTipText(I18n.textf("Log: %logname", argument));
                }
                // if (getDownloadWorker().validateConfiguration()) {
                // getDownloadWorker().doUpdateListFromServer();
                // }
                break;
            default:
                // NeptusLog.pub().error("LoggingControl: "+operation.toString()+" operation is unknown.");
                break;
        }

        if (automaticUpdateLogsFromRemoteSystems)
            scheduleDownloadListFromServer();
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange evt) {
        button.setEnabled(false);
        button.setToolTipText(I18n.text("Log: (updating)"));

        tabbledPane.setSelectedComponent(getDownloadWorker().getContentPanel());

        resetDownloaderForVehicle(evt.getCurrent());
    }

    @Override
    public long millisBetweenUpdates() {
        return updatePeriodSeconds * 1000;
    }

    @Override
    public boolean update() {
        updateSettingsDownloaderForVehicle(); // To take into account IP change
        scheduleCurLogFromVehicle();
        updateLogsState();
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        button.setEnabled(changeLogNameEnabled);
    }

    /**
     * @author pdias
     * @author ZP
     */
    class BarPainter implements Painter<JXButton> {
        double value = 0.0;
        // ColorMap colormap = ColorMapFactory.createRedYellowGreenColorMap();
        ColorMap colormap = new InterpolationColorMap("RedYellowGreen", new double[] { 0.0, 0.9999999999, 1.0 },
                new Color[] { Color.red, Color.yellow, Color.green });

        /**
         * @return the value
         */
        public double getValue() {
            return value;
        }

        /**
         * @param value the value to set [0.0, 1.0]
         */
        public void setValue(double value) {
            if (value < 0.0)
                this.value = 0.0;
            else if (value > 1.0)
                this.value = 1;
            else
                this.value = value;
        }

        /**
         * @return the colormap
         */
        public ColorMap getColormap() {
            return colormap;
        }

        /**
         * @param colormap the colormap to set
         */
        public void setColormap(ColorMap colormap) {
            this.colormap = colormap;
        }

        @Override
        public void paint(Graphics2D g, JXButton b, int widthI, int heightI) {

            // arg0.fillRect(0, 0, getWidth(), getHeight());

            Color color = colormap.getColor(value);
            double max = b.getWidth() - 4;
            double width = max * value;

            if (value == 0) {
                width = max;
            }

            RoundRectangle2D rect = new RoundRectangle2D.Double(2, 2, width, b.getHeight() - 4, 4, 4);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g.setPaint(new LinearGradientPaint(0, 0, getWidth(), getHeight(), new float[] { 0f, 1f }, new Color[] {
                    color, color.darker() }));
            g.fill(rect);

            // rect = new RoundRectangle2D.Double(2,2,b.getWidth()-4,b.getHeight()-4, 4, 4);
            // g.draw(rect);

            // g.setColor(Color.black);
            // g.setStroke(new BasicStroke(1.5f));
            // g.setColor(Color.black);
            // g.draw(rect);
        }
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();
        IMCMessage msg = IMCDefinition.getInstance().create("LoggingControl", "op", Operation.STOPPED.type());

        Operation op = Operation.valueOf(msg.getString("op"));

        NeptusLog.pub().info("<###> "+op);

        GuiUtils.testFrame(new LoggingDownloader(null));
    }
}