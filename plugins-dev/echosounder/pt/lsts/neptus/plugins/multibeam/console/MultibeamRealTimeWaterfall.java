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
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: tsm
 * 12 Oct 2016
 */
package pt.lsts.neptus.plugins.multibeam.console;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.IMCOutputStream;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.colormap.InterpolationColorMap;
import pt.lsts.neptus.comm.transports.udp.UDPTransport;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.gui.model.ArrayListComboBoxModel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.MultibeamUtil;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.multibeam.ui.MultibeamWaterfallViewer;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * This plugin receives sonar data through IMCSonarData,
 * transforms it into BathymetrySwath and paints it, in real-time
 * and waterfall manner
 * 
 * @author tsm
 * @author pdias
 */
@SuppressWarnings("serial")
@PluginDescription(author = "Tiago Marques", version = "0.1", name = "Multibeam Real-Time Waterfall Viewer")
@Popup(pos = POSITION.TOP_LEFT, width = 300, height = 500)
public class MultibeamRealTimeWaterfall extends ConsolePanel implements ConfigurationListener,
    MainVehicleChangeListener {

    // Parameters
    @NeptusProperty (name="Color map to use", category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private ColorMap colorMap = ColorMapFactory.createJetColorMap();

    @NeptusProperty (name="Max depth", description="Max depth used to normalize depth data", 
            category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private double maxDepth = 40;

    @NeptusProperty (name="Use adaptive max depth", description = "Use the highest value processed as max depth. Minimum value will be 'Max depth'",
            category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private boolean adaptativeMaxDepth = false;
    
    @NeptusProperty (name="Clean lines on vehicle change", category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private boolean cleanLinesOnVehicleChange = false;

    private ExecutorService threadExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        String nameBase = new StringBuilder().append(MultibeamRealTimeWaterfall.class.getSimpleName())
                .append("::").append(Integer.toHexString(MultibeamRealTimeWaterfall.this.hashCode()))
                .append("::").toString();
        ThreadGroup group = new ThreadGroup(nameBase);
        AtomicLong c = new AtomicLong(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread td = new Thread(group, r, nameBase + c.getAndIncrement());
            td.setDaemon(true);
            return td;
        }
    });

    // GUI
    private MultibeamWaterfallViewer mbViewer;
    private JComboBox<String> mbEntitiesComboBox;
    private ArrayListComboBoxModel<String> mbEntitiesComboBoxModel;
    private JComboBox<Long> subSystemsComboBox;
    private ArrayListComboBoxModel<Long> subSystemsComboBoxModel;
    
    private EstimatedState currentEstimatedState = null;

    public MultibeamRealTimeWaterfall(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        mbViewer = new MultibeamWaterfallViewer();
        setViewerProperties();

        mbEntitiesComboBoxModel = new ArrayListComboBoxModel<>(new ArrayList<String>(), true);
        mbEntitiesComboBox = new JComboBox<>(mbEntitiesComboBoxModel);
        subSystemsComboBoxModel = new ArrayListComboBoxModel<>(new ArrayList<Long>(), true);
        subSystemsComboBox = new JComboBox<>(subSystemsComboBoxModel);
        
        setLayout(new MigLayout("ins 0, gap 5"));
        add(mbEntitiesComboBox, "sg 1, w :50%:50%");
        add(subSystemsComboBox, "sg 1, w :40%:40%, wrap");
        add(mbViewer, "w 100%, h 100%, spanx");
        
        mbViewer.addMouseListener(getMouseListener());
    }

    private MouseListener getMouseListener() {
        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (SwingUtilities.isRightMouseButton(me)) {
                    JPopupMenu popup = new JPopupMenu();
                    JMenuItem menu = new JMenuItem(I18n.text("Clear"));
                    menu.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            mbViewer.clearLines();                            
                        }
                    });
                    popup.add(menu);
                    popup.show(me.getComponent(), me.getX(), me.getY());
                }
            }
        };
        return ma;
    }

    @Override
    public void cleanSubPanel() {
        threadExecutor.shutdownNow();
    }

    @Override
    public void initSubPanel() {
    }

    @Subscribe
    public void onEstimatedState(EstimatedState msg) {
        if (!msg.getSourceName().equals(getMainVehicleId()))
            return;

       currentEstimatedState = msg;
    }
    
    @Subscribe
    public void onSonarData(SonarData msg) {
        try {
            if (!msg.getSourceName().equals(getMainVehicleId()))
                return;
            // only interested in multibeam
            if(msg.getType() != SonarData.TYPE.MULTIBEAM)
                return;

            boolean firstEnt = mbEntitiesComboBoxModel.getSize() == 0;
            boolean firstSubSys = subSystemsComboBoxModel.getSize() == 0;
            mbEntitiesComboBoxModel.addValue(msg.getEntityName());
            subSystemsComboBoxModel.addValue(msg.getFrequency());
            if (firstEnt && mbEntitiesComboBoxModel.getSize() > 0)
                mbEntitiesComboBox.setSelectedItem(mbEntitiesComboBoxModel.getValue(0));
            if (firstSubSys && subSystemsComboBoxModel.getSize() > 0)
                subSystemsComboBox.setSelectedItem(subSystemsComboBoxModel.getValue(0));

            String selEnt = (String) mbEntitiesComboBoxModel.getSelectedItem();
            Long selSubSys = (Long) subSystemsComboBoxModel.getSelectedItem();

            if (selSubSys == null)
                return;

            if (selEnt != null && !selEnt.equals(msg.getEntityName()))
                return;
            if (!selSubSys.equals(msg.getFrequency()))
                return;

            SystemPositionAndAttitude pose = new SystemPositionAndAttitude();
            if (currentEstimatedState != null &&
                    Math.abs(currentEstimatedState.getTimestampMillis() - msg.getTimestampMillis()) < 500)
                pose = new SystemPositionAndAttitude(currentEstimatedState);

            BathymetrySwath swath = MultibeamUtil.getMultibeamSwath(msg, pose);
            if(swath != null)
                mbViewer.addNewData(swath);
            else
                NeptusLog.pub().warn("** Null Bathymetry swath!!!");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Periodic(millisBetweenUpdates = 200)
    public void update() {
        threadExecutor.execute(() -> mbViewer.updateRequest());
    }

    @Override
    public void propertiesChanged() {
        setViewerProperties();
    }

    private void setViewerProperties() {
        mbViewer.setColorMap(ColorMapFactory
                .createInvertedColorMap((InterpolationColorMap) colorMap));
        mbViewer.setMaxDepth(maxDepth);
        mbViewer.useAdaptiveMaxDepth(adaptativeMaxDepth);

        mbViewer.onViewerPropertiesUpdate();
    }
    
    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        currentEstimatedState = null;
        if (cleanLinesOnVehicleChange)
            mbViewer.clearLines();
        
        mbEntitiesComboBoxModel.clear();
        subSystemsComboBoxModel.clear();
        mbEntitiesComboBox.setSelectedItem(null);
        subSystemsComboBoxModel.setSelectedItem(null);
    }

    public static void main(String[] args) {
        String dataFile = (System.getProperty("user.dir") + "/" + "../log/maridan-multibeam/Data.lsf.gz");
        // dataFile = "D:\\REP15-Data\\to_upload_20150717\\lauv-noptilus-3\\20150717\\120741_horta-m01\\Data.lsf.gz";

        UDPTransport udp = new UDPTransport(6002);
        
        try {
            LsfLogSource source = new LsfLogSource(dataFile, null);
            DeltaTParser mbParser = new DeltaTParser(source);
            
            Collection<Integer> vehSrcs = source.getVehicleSources();
            if (vehSrcs.size() < 1)
                return;
            
            int mainVehSrc = vehSrcs.iterator().next();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IMCOutputStream imcOs = new IMCOutputStream(baos);

            BathymetrySwath swath = mbParser.nextSwath();
            while (swath != null) {
                SystemPositionAndAttitude pose = swath.getPose();
                EstimatedState currentEstimatedState = pose.toEstimatedState();
                SonarData sd = MultibeamUtil.swathToSonarData(swath, pose);
                
                currentEstimatedState.setSrc(mainVehSrc);
                sd.setSrc(mainVehSrc);
                
                currentEstimatedState.setTimestamp(sd.getTimestamp());

                baos.reset();
                currentEstimatedState.serialize(imcOs);
                udp.sendMessage("localhost", 6001, baos.toByteArray());
                baos.reset();
                sd.serialize(imcOs);
                udp.sendMessage("localhost", 6001, baos.toByteArray());
                
                Thread.sleep(50);
                
                swath = mbParser.nextSwath();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
