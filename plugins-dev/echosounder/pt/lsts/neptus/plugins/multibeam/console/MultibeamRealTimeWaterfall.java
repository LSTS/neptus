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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.eventbus.Subscribe;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.SonarData;
import pt.lsts.imc.lsf.LsfIndex;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.colormap.ColorMap;
import pt.lsts.neptus.colormap.ColorMapFactory;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.mp.SystemPositionAndAttitude;
import pt.lsts.neptus.mra.api.BathymetryPoint;
import pt.lsts.neptus.mra.api.BathymetrySwath;
import pt.lsts.neptus.mra.api.MultibeamUtil;
import pt.lsts.neptus.mra.importers.IMraLog;
import pt.lsts.neptus.mra.importers.deltat.DeltaTParser;
import pt.lsts.neptus.plugins.ConfigurationListener;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.Popup.POSITION;
import pt.lsts.neptus.plugins.interfaces.RealTimeWatefallViewer;
import pt.lsts.neptus.plugins.multibeam.ui.MultibeamWaterfallViewer;
import pt.lsts.neptus.plugins.update.Periodic;
import pt.lsts.neptus.util.llf.LsfLogSource;

/**
 * This plugin receives sonar data through IMCSonarData,
 * transforms it into BathymetrySwath and paints it, in real-time
 * and waterfall manner
 * @author tsm
 *
 */
@PluginDescription(author = "Tiago Marques", version = "0.1", name = "Multibeam Real-Time Waterfall Viewer")
@Popup(pos = POSITION.TOP_LEFT, width = 300, height = 500)
public class MultibeamRealTimeWaterfall extends ConsolePanel implements ConfigurationListener {

    // Parameters
    @NeptusProperty (name="Color map to use", category="Visualization parameters", userLevel = LEVEL.REGULAR)
    private ColorMap colorMap = ColorMapFactory.createBronzeColormap();

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

    private RealTimeWatefallViewer<BathymetrySwath> mbViewer;
    private EstimatedState currentEstimatedState = null;
    private DeltaTParser mbParser;


    public MultibeamRealTimeWaterfall(ConsoleLayout console) {
        super(console);
        initialize();
        testDataDisplay();
    }


    private void initialize() {
        mbViewer = new MultibeamWaterfallViewer();
        mbViewer.setColorMap(colorMap);

        setLayout(new MigLayout("ins 0, gap 5"));
        add(mbViewer, "w 100%, h 100%");
    }


    @Override
    public void cleanSubPanel() {
        threadExecutor.shutdownNow();
    }

    @Override
    public void initSubPanel() {

    }


    @Subscribe
    public void onSonarData(SonarData msg) {
        // only interested in multibeam
        if(msg.getType() != SonarData.TYPE.MULTIBEAM)
            return;

        SystemPositionAndAttitude pose;
        if (currentEstimatedState == null || Math.abs(currentEstimatedState.getTimestampMillis() - msg.getTimestampMillis()) > 500)
            pose = new SystemPositionAndAttitude();
        else
            pose = new SystemPositionAndAttitude(currentEstimatedState);

        BathymetrySwath swath = MultibeamUtil.getMultibeamSwath(msg, pose);
        if(swath != null)
            mbViewer.addNewData(swath);
        else
            NeptusLog.pub().warn("** Null Bathymetry swath!!!");
    }

    public void onBathymetrySwath(BathymetrySwath swath) {
        if(mbViewer == null)
            return;
        mbViewer.addNewData(swath);
    }

    private void testDataDisplay() {
        String dataFile = (System.getProperty("user.dir") + "/" + "../log/maridan-multibeam/Data.lsf.gz");

        try {
            LsfLogSource source = new LsfLogSource(dataFile, null);
            mbParser = new DeltaTParser(source);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Periodic(millisBetweenUpdates = 200)
    public void update() {
        if(mbViewer == null || mbParser == null)
            return;

        BathymetrySwath currSwath;
        if((currSwath = mbParser.nextSwath()) != null) {
            mbViewer.addNewData(currSwath);
            threadExecutor.execute(() -> mbViewer.updateRequest());
        }
        else
            System.out.println("Finished");
    }

    @Override
    public void propertiesChanged() {
        mbViewer.setColorMap(colorMap);
    }
}
