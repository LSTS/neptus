/*
 * Copyright (c) 2004-2020 Universidade do Porto - Faculdade de Engenharia
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
 * Author: pdias
 * 07/10/2016
 */
package pt.lsts.neptus.plugins.sidescan.console;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JComboBox;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.SonarData;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.console.plugins.SubPanelChangeEvent;
import pt.lsts.neptus.console.plugins.SubPanelChangeListener;
import pt.lsts.neptus.gui.model.ArrayListComboBoxModel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.SidescanLine;
import pt.lsts.neptus.mra.api.SidescanParameters;
import pt.lsts.neptus.mra.api.SidescanUtil;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.sidescan.gui.SidescanViewerPanel;
import pt.lsts.neptus.plugins.update.Periodic;

/**
 * @author pdias
 *
 */
@PluginDescription(author = "Paulo Dias", version = "0.4", name = "Sidescan Real-Time Waterfall")
@Popup(pos = POSITION.TOP_LEFT, width = 300, height = 500)
@SuppressWarnings("serial")
public class SidescanRealTimeWaterfall extends ConsolePanel
        implements MainVehicleChangeListener, ConfigurationListener, SubPanelChangeListener {

    // Parameters
    @NeptusProperty (name="Color map to use", category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private ColorMap colorMap = ColorMapFactory.createBronzeColormap();
    
    @NeptusProperty (name="Normalization factor", category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private double normalization = 0.2;
    
    @NeptusProperty (name="Time Variable Gain factor", category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private double tvgGain = 280;
    
    @NeptusProperty (name="Slant range correction", category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private boolean slantRangeCorrection = false;

    @NeptusProperty (name="Speed correction", category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private boolean speedCorrection = true;

    @NeptusProperty (name="Clean lines on vehicle change", category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private boolean cleanLinesOnVehicleChange = false;

    // GUI
    private SidescanViewerPanel ssViewer = null;
    private JComboBox<String> sssEntitiesComboBox;
    private ArrayListComboBoxModel<String> sssEntitiesComboBoxModel;
    private JComboBox<Long> subSystemsComboBox;
    private ArrayListComboBoxModel<Long> subSystemsComboBoxModel;

    // Data
    private SidescanParameters sidescanParams = new SidescanParameters(normalization, tvgGain);
    
    private EstimatedState curEstimatedState = null;
    
    private ExecutorService threadExecutor = Executors.newCachedThreadPool(new ThreadFactory() {
        String nameBase = new StringBuilder().append(SidescanRealTimeWaterfall.class.getSimpleName())
                .append("::").append(Integer.toHexString(SidescanRealTimeWaterfall.this.hashCode()))
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
    
    /**
     * @param console
     */
    public SidescanRealTimeWaterfall(ConsoleLayout console) {
        super(console);
        initialize();
    }

    private void initialize() {
        removeAll();
        
        ssViewer = new SidescanViewerPanel();
        updateViewerParameters();
        
        sssEntitiesComboBoxModel = new ArrayListComboBoxModel<>(new ArrayList<String>(), true);
        sssEntitiesComboBox = new JComboBox<>(sssEntitiesComboBoxModel);
        subSystemsComboBoxModel = new ArrayListComboBoxModel<>(new ArrayList<Long>(), true);
        subSystemsComboBox = new JComboBox<>(subSystemsComboBoxModel);
        
        setLayout(new MigLayout("ins 0, gap 5"));
        add(sssEntitiesComboBox, "sg 1, w :50%:50%");
        add(subSystemsComboBox, "sg 1, w :40%:40%, wrap");
        add(ssViewer, "w 100%, h 100%, spanx");
        
        ssViewer.addMouseListener(getMouseListener());
    }

    /**
     * @return
     */
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
                            ssViewer.clearLines();                            
                        }
                    });
                    popup.add(menu);
                    popup.show(me.getComponent(), me.getX(), me.getY());
                }
            }
        };
        return ma;
    }

    private void updateViewerParameters() {
        ssViewer.setColorMap(colorMap);
        ssViewer.setSlantRangeCorrection(slantRangeCorrection);
        ssViewer.setSpeedCorrection(speedCorrection);
        
        sidescanParams.setNormalization(normalization);
        sidescanParams.setTvgGain(tvgGain);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#initSubPanel()
     */
    @Override
    public void initSubPanel() {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        threadExecutor.shutdownNow();
    }

    @Subscribe
    public void mainVehicleChangeNotification(ConsoleEventMainSystemChange ev) {
        if (cleanLinesOnVehicleChange)
            ssViewer.clearLines();
        
        curEstimatedState = null;
        
        sssEntitiesComboBoxModel.clear();
        subSystemsComboBoxModel.clear();
        sssEntitiesComboBox.setSelectedItem(null);
        subSystemsComboBoxModel.setSelectedItem(null);
    }
    
    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.plugins.SubPanelChangeListener#subPanelChanged(pt.lsts.neptus.console.plugins.SubPanelChangeEvent)
     */
    @Override
    public void subPanelChanged(SubPanelChangeEvent panelChange) {
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.plugins.ConfigurationListener#propertiesChanged()
     */
    @Override
    public void propertiesChanged() {
        updateViewerParameters();
    }
    
    @Subscribe
    public void onEstimatedState(EstimatedState msg) {
        if (!msg.getSourceName().equals(getMainVehicleId()))
            return;

        curEstimatedState = msg;
    }
    
    @Subscribe
    public void onSidescanData(SonarData msg) {
        try {
            if (!msg.getSourceName().equals(getMainVehicleId()))
                return;
            if (msg.getType() != SonarData.TYPE.SIDESCAN)
                return;

            boolean firstEnt = sssEntitiesComboBoxModel.getSize() == 0;
            boolean firstSubSys = subSystemsComboBoxModel.getSize() == 0;
            sssEntitiesComboBoxModel.addValue(msg.getEntityName());
            subSystemsComboBoxModel.addValue(msg.getFrequency());
            if (firstEnt && sssEntitiesComboBoxModel.getSize() > 0)
                sssEntitiesComboBox.setSelectedItem(sssEntitiesComboBoxModel.getValue(0));
            if (firstSubSys && subSystemsComboBoxModel.getSize() > 0)
                subSystemsComboBox.setSelectedItem(subSystemsComboBoxModel.getValue(0));

            String selEnt = (String) sssEntitiesComboBoxModel.getSelectedItem();
            Long selSubSys = (Long) subSystemsComboBoxModel.getSelectedItem();

            if (selSubSys == null)
                return;

            if (selEnt != null && !selEnt.equals(msg.getEntityName()))
                return;
            if (!selSubSys.equals(msg.getFrequency()))
                return;

            SystemPositionAndAttitude pose;
            if (curEstimatedState == null
                    || Math.abs(curEstimatedState.getTimestampMillis() - msg.getTimestampMillis()) > 500) {
                pose = new SystemPositionAndAttitude();
                // return;
            }
            else {
                pose = new SystemPositionAndAttitude(curEstimatedState);
            }

            SidescanLine line = SidescanUtil.getSidescanLine(msg, pose, sidescanParams);
            ssViewer.addNewSidescanLine(line);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Periodic(millisBetweenUpdates = 200)
    private boolean periodicUpdaterSSImage() {
        try {
            threadExecutor.execute(() -> ssViewer.updateRequest());
        }
        catch (RejectedExecutionException e) {
            NeptusLog.pub().warn(e.getMessage());
        }
        return true;
    }
}
