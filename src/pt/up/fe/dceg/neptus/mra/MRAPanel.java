/*
 * Copyright (c) 2004-2013 Laboratório Mde Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Author: José Pinto
 * 2007/09/25
 */
package pt.up.fe.dceg.neptus.mra;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
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

import com.l2fprod.common.propertysheet.DefaultProperty;
import com.l2fprod.common.propertysheet.Property;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.InfiniteProgressPanel;
import pt.up.fe.dceg.neptus.gui.PropertiesProvider;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.IMCMessage;
import pt.up.fe.dceg.neptus.imc.lsf.LsfGenericIterator;
import pt.up.fe.dceg.neptus.mra.exporters.ImcTo837;
import pt.up.fe.dceg.neptus.mra.exporters.MraExporter;
import pt.up.fe.dceg.neptus.mra.exporters.PCDExporter;
import pt.up.fe.dceg.neptus.mra.importers.IMraLogGroup;
import pt.up.fe.dceg.neptus.mra.plots.LogMarkerListener;
import pt.up.fe.dceg.neptus.mra.replay.LogReplay;
import pt.up.fe.dceg.neptus.mra.visualizations.MRAVisualization;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.PluginsRepository;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.llf.LogTree;
import pt.up.fe.dceg.neptus.util.llf.LogUtils;
import pt.up.fe.dceg.neptus.util.llf.LsfTree;
import pt.up.fe.dceg.neptus.util.llf.LsfTreeMouseAdapter;
import pt.up.fe.dceg.neptus.util.llf.chart.MraChartFactory;
import pt.up.fe.dceg.neptus.util.llf.replay.LLFMsgReplay;

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
    private final LLFMsgReplay replayMsg;

    private final JPanel leftPanel = new JPanel(new MigLayout("ins 0"));
    private final JPanel mainPanel = new JPanel(new MigLayout());
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private final JScrollPane jspMessageTree;
    private final JScrollPane jspLogTree;

    private final LinkedHashMap<String, MRAVisualization> visualizationList = new LinkedHashMap<String, MRAVisualization>();
    private final LinkedHashMap<String, Component> openVisualizationList = new LinkedHashMap<String, Component>();

    private final ArrayList<LogMarker> logMarkers = new ArrayList<LogMarker>();
    private MRAVisualization shownViz = null;

    InfiniteProgressPanel loader = InfiniteProgressPanel.createInfinitePanelBeans("");

    private JMenu exporters;
    
    public MRAPanel(final IMraLogGroup source, NeptusMRA mra) {
        this.source = source;
        MRAVisualization[] automaticCharts = MraChartFactory.getAutomaticCharts(this);

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
                e1.printStackTrace();
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

        replayMsg = new LLFMsgReplay(this);
        loadVisualization(replayMsg, false);

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
        MraExporter exporterList[] = new MraExporter[] { 
                new ImcTo837(source),
                new PCDExporter(source)
        }; 
        
        // Check for existence of Exporters menu and remove on existence
        JMenuBar bar = mra.getMRAMenuBar();
        JMenu previousMenu = GuiUtils.getJMenuByName(bar, "Exporters");
        if(previousMenu != null) {
            bar.remove(previousMenu);
        }
        
        exporters = new JMenu("Exporters");
        for(final MraExporter exp : exporterList) {
            if(exp.canBeApplied(source)) {
                JMenuItem item = new JMenuItem(new AbstractAction(exp.getName()) {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        exp.process();
                    }
                });
                exporters.add(item);
            }
        }
        
        if(exporters.getItemCount() > 0) {
            bar.add(exporters);
        }
        
        monitor.close();
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

            marker.lat = loc.getLatitudeAsDoubleValueRads();
            marker.lon = loc.getLongitudeAsDoubleValueRads();
        }
        logTree.addMarker(marker);
        logMarkers.add(marker);

        // getTimestampsForMarker(marker, 2);

        for (MRAVisualization vis : visualizationList.values()) {
            if (vis instanceof LogMarkerListener) {
                ((LogMarkerListener) vis).addLogMarker(marker);
            }
        }
    }

    public void removeMarker(LogMarker marker) {
        removeTreeObject(marker);
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
                NeptusLog.pub().info("<###> "+marker.label + " --- " + state.getTimestampMillis());
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
            NeptusLog.pub().info("No markers for this log");
        }
    }

    public void saveMarkers() {
        try {
            ObjectOutputStream dos = new ObjectOutputStream(new FileOutputStream(source.getFile("Data.lsf").getParent()
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
            else {
                // Do the loading
                mainPanel.removeAll();
                mainPanel.repaint();
                mainPanel.add(loader, "w 100%, h 100%");

                loader.setText("Loading " + vis.getName());
                loader.start();

                c = vis.getComponent(source, NeptusMRA.defaultTimestep);
                openVisualizationList.put(vis.getName(), c);

                loader.stop();
            }

            if (shownViz != null)
                shownViz.onHide();

            shownViz = vis;
            vis.onShow();
            mainPanel.removeAll();
            mainPanel.add(c, "w 100%, h 100%");

            // Add markers
            // For every LogMarker just call the handler of the new visualization
            if (vis instanceof LogMarkerListener) {
                for (LogMarker marker : logMarkers) {
                    ((LogMarkerListener) vis).addLogMarker(marker);
                }
            }

            mainPanel.revalidate();
            mainPanel.repaint();
        }

    }
}
