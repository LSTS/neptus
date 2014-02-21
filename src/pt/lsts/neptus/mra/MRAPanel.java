/*
 * Copyright (c) 2004-2014 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 2007/09/25
 */
package pt.lsts.neptus.mra;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.ProgressMonitor;

import net.miginfocom.swing.MigLayout;

import org.jdesktop.swingx.JXStatusBar;

import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.lsf.LsfGenericIterator;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.gui.InfiniteProgressPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.exporters.CSVExporter;
import pt.lsts.neptus.mra.exporters.ImcTo837;
import pt.lsts.neptus.mra.exporters.KMLExporter;
import pt.lsts.neptus.mra.exporters.MRAExporter;
import pt.lsts.neptus.mra.exporters.MatExporter;
import pt.lsts.neptus.mra.exporters.PCDExporter;
import pt.lsts.neptus.mra.exporters.XTFExporter;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.LogMarkerListener;
import pt.lsts.neptus.mra.replay.LogReplay;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.PluginUtils;
import pt.lsts.neptus.plugins.PluginsRepository;
import pt.lsts.neptus.plugins.noptilus.NoptilusMapExporter;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.LogTree;
import pt.lsts.neptus.util.llf.LogUtils;
import pt.lsts.neptus.util.llf.LsfTree;
import pt.lsts.neptus.util.llf.LsfTreeMouseAdapter;
import pt.lsts.neptus.util.llf.chart.MraChartFactory;

/**
 * @author ZP
 */
@SuppressWarnings("serial")
public class MRAPanel extends JPanel {

    private LsfTree tree;
    private LogTree logTree;

    private IMraLogGroup source = null;
    private final JXStatusBar statusBar = new JXStatusBar();

    private final LogReplay replay;

    private final JPanel leftPanel = new JPanel(new MigLayout("ins 0"));
    private final JPanel mainPanel = new JPanel(new MigLayout());
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private final JScrollPane jspMessageTree;
    private final JScrollPane jspLogTree;

    private final LinkedHashMap<String, MRAVisualization> visualizationList = new LinkedHashMap<String, MRAVisualization>();
    private final LinkedHashMap<String, Component> openVisualizationList = new LinkedHashMap<String, Component>();
    private final ArrayList<String> loadingVisualizations = new ArrayList<String>();

    private final ArrayList<LogMarker> logMarkers = new ArrayList<LogMarker>();
    private MRAVisualization shownViz = null;

    InfiniteProgressPanel loader = InfiniteProgressPanel.createInfinitePanelBeans("");

    private JMenu exporters;

    public MRAPanel(final IMraLogGroup source, NeptusMRA mra) {
        this.source = source;

        MRAVisualization[] automaticCharts = MraChartFactory.getAutomaticCharts(this);

        if (new File("conf/tides.txt").canRead() && source.getFile("tides.txt") == null) {
            FileUtil.copyFile("conf/tides.txt", new File(source.getFile("."), "tides.txt").getAbsolutePath());
        }

        // Setup interface
        tree = new LsfTree(source);
        logTree = new LogTree(source, this);

        jspMessageTree = new JScrollPane(tree);
        jspLogTree = new JScrollPane(logTree);

        tabbedPane.addTab(I18n.text("Visualizations"), jspLogTree);
        tabbedPane.addTab(I18n.text("Messages"), jspMessageTree);

        leftPanel.add(tabbedPane, "wrap, w 100%, h 100%");

        setLayout(new BorderLayout(3, 3));
        JSplitPane pane = new JSplitPane();

        VehicleType veh = LogUtils.getVehicle(source);
        Date startDate = LogUtils.getStartDate(source);
        String date = startDate != null ? " | <b>" + I18n.text("Date") + ":</b> "
                + new SimpleDateFormat("dd/MMM/yyyy").format(startDate) : "";

                statusBar.add(new JLabel("<html><b>" + I18n.text("Log") + ":</b> " + source.name() + date
                        + ((veh != null) ? " | <b>" + I18n.text("System") + ":</b> " + veh.getName() : "")));

                pane.setLeftComponent(leftPanel);
                pane.setRightComponent(mainPanel);

                pane.setDividerLocation(250);
                pane.setResizeWeight(0);

                final ProgressMonitor monitor = new ProgressMonitor(this, I18n.text("Loading") + "...",
                        I18n.text("Loading automatic charts"), 0, 100);
                monitor.setMillisToDecideToPopup(0);
                monitor.setNote(I18n.text("Loading tree"));

                // The LogTreeMouseAdapter class deals with all the plot instantiation
                tree.addMouseListener(new LsfTreeMouseAdapter(this));

                monitor.setProgress(10);
                int curProgress = 10;
                int increaseProgress = 65;
                if (automaticCharts.length > 0)
                    increaseProgress = 65 / automaticCharts.length;

                // Load Automatic Charts
                for (MRAVisualization chart : automaticCharts) {

                    if (!chart.canBeApplied(MRAPanel.this.source))
                        continue;

                    loadVisualization(chart, false);
                    curProgress += increaseProgress;
                    monitor.setNote(I18n.textf("loading %chartname", chart.getName()));
                    monitor.setProgress(curProgress);
                }

                // Load PluginVisualizations
                for (String visName : PluginsRepository.getMraVisualizations().keySet()) {
                    try {
                        Class<?> vis = PluginsRepository.getMraVisualizations().get(visName);

                        MRAVisualization visualization = (MRAVisualization) vis.getDeclaredConstructor(MRAPanel.class)
                                .newInstance(this);
                        PluginUtils.loadProperties(visualization, "mra");

                        if (visualization.canBeApplied(MRAPanel.this.source))
                            loadVisualization(visualization, false);
                    }
                    catch (Exception e1) {
                        // FIX (pdias) missing plugins jars or errors on load
                        NeptusLog.pub().error(
                                I18n.text("MRA Visualization not loading properly") + ": " + visName + "  [" + e1.getMessage()
                                + "]");
                    }
                    catch (Error e2) {
                        // FIX (pdias) missing plugins jars or errors on load
                        NeptusLog.pub().error(
                                I18n.text("MRA Visualization not loading properly") + ": " + visName + "  [" + e2.getMessage()
                                + "]");
                    }
                }

                monitor.setNote(I18n.text("Starting mission replay") + "...");

                replay = new LogReplay(this);
                loadVisualization(replay, false);

                monitor.setProgress(100);
                monitor.setNote(I18n.text("Done!"));

                add(pane, BorderLayout.CENTER);
                add(statusBar, BorderLayout.SOUTH);

                for (int i = 0; i < logTree.getRowCount(); i++) {
                    logTree.expandRow(i);
                }

                // Load markers
                loadMarkers();

                // Load exporters
                // Exporters list, this will be moved in the future
//                MRAExporter[] exporterList = new MRAExporter[] { new ImcTo837(source), new PCDExporter(source),
//                        new MatExporter(source), new KMLExporter(this, source), new CSVExporter(source),
//                        new XTFExporter(source), new NoptilusMapExporter(source) };
                //Ugly code but in the develop branch will be ok
                ArrayList<MRAExporter> exporterList = new ArrayList<>();
                createExportersAvailableList(source, exporterList);

                // Check for existence of Exporters menu and remove on existence (in case of opening a new log)
                JMenuBar bar = mra.getMRAMenuBar();
                JMenu previousMenu = GuiUtils.getJMenuByName(bar, I18n.text("Exporters"));
                if (previousMenu != null) {
                    bar.remove(previousMenu);
                }

                exporters = new JMenu(I18n.text("Exporters"));
                for (final MRAExporter exp : exporterList) {
                    if (exp.canBeApplied(source)) {
                        JMenuItem item = new JMenuItem(new AbstractAction(exp.getName()) {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                Thread t = new Thread(exp.getName() + " processing") {
                                    @Override
                                    public void run() {
                                        String res = exp.process();

                                        if (res != null)
                                            GuiUtils.infoMessage(MRAPanel.this, exp.getName(), res);
                                    };
                                };
                                t.setDaemon(true);
                                t.start();

                            }
                        });
                        exporters.add(item);
                    }
                }

                if (exporters.getItemCount() > 0) {
                    bar.add(exporters);
                }

                monitor.close();
    }

    /**
     * @param source
     * @param exporterList
     */
    private void createExportersAvailableList(final IMraLogGroup source, ArrayList<MRAExporter> exporterList) {
        try {
            MRAExporter exp = new ImcTo837(source);
            exporterList.add(exp);
        }
        catch (Exception e) {
            // Nothing to do.
        }
        catch (Error e) {
            // Nothing to do.
        }
        try {
            MRAExporter exp = new PCDExporter(source);
            exporterList.add(exp);
        }
        catch (Exception e) {
            // Nothing to do.
        }
        catch (Error e) {
            // Nothing to do.
        }
        try {
            MRAExporter exp = new MatExporter(source);
            exporterList.add(exp);
        }
        catch (Exception e) {
            // Nothing to do.
        }
        catch (Error e) {
            // Nothing to do.
        }
        try {
            MRAExporter exp = new KMLExporter(this, source);
            exporterList.add(exp);
        }
        catch (Exception e) {
            // Nothing to do.
        }
        catch (Error e) {
            // Nothing to do.
        }
        try {
            MRAExporter exp = new CSVExporter(source);
            exporterList.add(exp);
        }
        catch (Exception e) {
            // Nothing to do.
        }
        catch (Error e) {
            // Nothing to do.
        }
        try {
            MRAExporter exp = new XTFExporter(source);
            exporterList.add(exp);
        }
        catch (Exception e) {
            // Nothing to do.
        }
        catch (Error e) {
            // Nothing to do.
        }
        try {
            MRAExporter exp = new NoptilusMapExporter(source);
            exporterList.add(exp);
        }
        catch (Exception e) {
            // Nothing to do.
        }
        catch (Error e) {
            // Nothing to do.
        }
    }

    public void loadVisualization(MRAVisualization vis, boolean open) {
        // Doesnt exist already, load..
        if (!visualizationList.keySet().contains(vis.getName())) {
            ImageIcon icon = vis.getIcon();

            if (icon == null) {
                icon = ImageUtils.getIcon("images/menus/graph.png");
            }
            visualizationList.put(vis.getName(), vis);
            logTree.addVisualization(vis);

            if (open) {
                openVisualization(vis);
            }
        }
        else {
            if (open) {
                openVisualization(vis);
            }
        }
    }

    public void openVisualization(MRAVisualization viz) {
        new Thread(new LoadTask(viz)).start();
    }

    public void addMarker(LogMarker marker) {

        if (existsMark(marker))
            return;

        // Calculate marker location
        if (marker.lat == 0 && marker.lon == 0) {
            IMCMessage m = source.getLog("EstimatedState").getEntryAtOrAfter(new Double(marker.timestamp).longValue());
            LocationType loc = LogUtils.getLocation(m);

            marker.lat = loc.getLatitudeRads();
            marker.lon = loc.getLongitudeRads();
        }
        logTree.addMarker(marker);
        logMarkers.add(marker);

        // getTimestampsForMarker(marker, 2);

        for (MRAVisualization vis : visualizationList.values()) {
            if (vis instanceof LogMarkerListener) {
                ((LogMarkerListener) vis).addLogMarker(marker);
            }
        }
        
        saveMarkers();
    }

    public void removeMarker(LogMarker marker) {
        logTree.removeMarker(marker);
        logMarkers.remove(marker);
        for (MRAVisualization vis : visualizationList.values()) {
            if (vis instanceof LogMarkerListener) {
                try {
                    ((LogMarkerListener) vis).removeLogMarker(marker);
                }
                catch (Exception e) {
                    NeptusLog.pub().error(e);
                }
            }
        }
    }

    public void removeTreeObject(Object obj) {
        logTree.remove(obj);
    }

    public boolean existsMark(LogMarker marker) {
        for (LogMarker m : logMarkers) {
            if (m.label.equals(marker.label))
                return true;
        }
        return false;
    }

    public void getTimestampsForMarker(LogMarker marker, double distance) {
        LsfGenericIterator i = source.getLsfIndex().getIterator("EstimatedState");
        LocationType l = marker.getLocation();

        for (IMCMessage state = i.next(); i.hasNext(); state = i.next()) {
            LocationType loc = new LocationType(Math.toDegrees(state.getDouble("lat")), Math.toDegrees(state
                    .getDouble("lon")));
            loc.translatePosition(state.getDouble("x"), state.getDouble("y"), 0);

            if (loc.getDistanceInMeters(l) <= distance) {
                NeptusLog.pub().info("<###> " + marker.label + " --- " + state.getTimestampMillis());
            }
        }
    }

    public LogReplay getMissionReplay() {
        return replay;
    }

    /**
     * @return the tree
     */
    public LsfTree getTree() {
        return tree;
    }

    /**
     * @return the logTree
     */
    public final LogTree getLogTree() {
        return logTree;
    }

    /**
     * @return the source
     */
    public final IMraLogGroup getSource() {
        return source;
    }

    public ArrayList<LogMarker> getMarkers() {
        return logMarkers;
    }

    public InfiniteProgressPanel getLoader() {
        return loader;
    }

    public void cleanup() {
        NeptusLog.pub().info("MRA Cleanup");
        tree.removeAll();
        tree = null;

        logTree.removeAll();
        logTree = null;

        for (MRAVisualization vis : visualizationList.values()) {
            vis.onCleanup();
            vis = null;
        }

        openVisualizationList.clear();

        saveMarkers();

        source.cleanup();
        source = null;
    }

    @SuppressWarnings("unchecked")
    public void loadMarkers() {
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(source.getFile("Data.lsf").getParent()
                    + "/marks.dat"));
            for (LogMarker marker : (ArrayList<LogMarker>) ois.readObject()) {
                logMarkers.add(marker);
                logTree.addMarker(marker);
            }
            ois.close();
        }
        catch (Exception e) {
            NeptusLog.pub().info("No markers for this log, or erroneous mark file");
        }
    }

    public void saveMarkers() {
        try {
            ObjectOutputStream dos = new ObjectOutputStream(new FileOutputStream(source.getFile(".").getParent()
                    + "/marks.dat"));
            dos.writeObject(logMarkers);
            dos.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void synchVisualizations(LogMarker marker) {
        for (MRAVisualization v : visualizationList.values()) {
            if (v instanceof LogMarkerListener)
                ((LogMarkerListener) v).GotoMarker(marker);
        }
    }

    class LoadTask implements Runnable {
        MRAVisualization vis;

        public LoadTask(MRAVisualization vis) {
            this.vis = vis;
        }

        @Override
        public void run() {
            // Check for existence
            Component c;
            if (openVisualizationList.containsKey(vis.getName())) {
                c = openVisualizationList.get(vis.getName());
            }
            else if (loadingVisualizations.contains(vis.getName())) {
                loader.setText(I18n.textf("Loading %visName", vis.getName()));
                loader.start();
                c = loader;
            }
            else {
                loadingVisualizations.add(vis.getName());

                // Do the loading
                mainPanel.removeAll();
                mainPanel.repaint();
                mainPanel.add(loader, "w 100%, h 100%");

                loader.setText(I18n.textf("Loading %visName", vis.getName()));
                loader.start();

                c = vis.getComponent(source, NeptusMRA.defaultTimestep);
                openVisualizationList.put(vis.getName(), c);

                // Add markers
                // For every LogMarker just call the handler of the new visualization
                if (vis instanceof LogMarkerListener) {
                    for (LogMarker marker : logMarkers) {
                        ((LogMarkerListener) vis).addLogMarker(marker);
                    }
                }

                loader.stop();
                loadingVisualizations.remove(vis.getName());
            }

            if (shownViz != null)
                shownViz.onHide();

            shownViz = vis;
            vis.onShow();
            mainPanel.removeAll();
            mainPanel.add(c, "w 100%, h 100%");

            mainPanel.revalidate();
            mainPanel.repaint();
        }

    }
}
