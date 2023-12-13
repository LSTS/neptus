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
 * Author: José Correia
 * Oct 23, 2012
 */
package pt.lsts.neptus.plugins.sidescan;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicSliderUI;

import com.sun.codemodel.JForEach;
import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.gui.Timeline;
import pt.lsts.neptus.gui.TimelineChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mra.LogMarker;
import pt.lsts.neptus.mra.MRAPanel;
import pt.lsts.neptus.mra.api.SidescanHistogramNormalizer;
import pt.lsts.neptus.mra.api.SidescanParser;
import pt.lsts.neptus.mra.api.SidescanParserFactory;
import pt.lsts.neptus.mra.importers.IMraLogGroup;
import pt.lsts.neptus.mra.plots.LogMarkerListener;
import pt.lsts.neptus.mra.visualizations.MRAVisualization;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.llf.LogUtils;

/**
 * @author jqcorreia
 */
@PluginDescription(author = "jqcorreia", name = "Sidescan Analyzer", icon = "pt/lsts/neptus/plugins/echosounder/echosounder.png")
public class SidescanAnalyzer extends JPanel implements MRAVisualization, TimelineChangeListener, LogMarkerListener {
    private static final long serialVersionUID = 1L;

    protected MRAPanel mraPanel;
    private Timeline timeline;
    private long firstPingTime;
    private long lastPingTime;
    private long currentTime;
    private long lastUpdateTime;
    private Map<Integer, SidescanPanel> sidescanPanels = new HashMap<>();
    private Map<Integer, JToggleButton> panelButtons = new HashMap<>();
    private ArrayList<LogMarker> markerList = new ArrayList<>();
    private SidescanParser ssParser;
    private SidescanHistogramNormalizer histogram;
    private SidescanPanel currentPanel;

    public SidescanAnalyzer(MRAPanel panel) {
        this.mraPanel = panel;
    }

    public void initialize(IMraLogGroup source) {
        ssParser = SidescanParserFactory.build(source);
        histogram = SidescanHistogramNormalizer.create(source);

        firstPingTime = ssParser.firstPingTimestamp();
        lastPingTime = ssParser.lastPingTimestamp();

        lastUpdateTime = firstPingTime;

        timeline = new Timeline(0, (int) (lastPingTime - firstPingTime), 30, 1000, false);
        timeline.getSlider().setValue(0);
        timeline.addTimelineChangeListener(this);

        timeline.getSlider().setUI(new BasicSliderUI(timeline.getSlider()) {
            @Override
            public void paintTicks(Graphics g) {
                super.paintTicks(g);
                for (LogMarker m : markerList) {
                    long mtime = Double.valueOf(m.getTimestamp()).longValue();
                    g.drawLine(xPositionForValue((int) (mtime - firstPingTime)), 0,
                            xPositionForValue((int) (mtime - firstPingTime)), timeline.getSlider().getHeight() / 2);
                    // g.drawString(m.label, xPositionForValue((int)(mtime-firstPingTime))-10, 22);
                }
            }
        });


        for (Integer subsys : ssParser.getSubsystemList()) {
            sidescanPanels.put(subsys, new SidescanPanel(this, ssParser, subsys));
            JToggleButton button = new JToggleButton(subsys.toString());
            button.addActionListener(e -> {
                updateSidescanPanel(subsys);
            });
            panelButtons.put(subsys, button);
        }

        // Layout building
        setLayout(new MigLayout());

        JToolBar toolBar = new JToolBar();
        toolBar.setLayout(new GridLayout(0, ssParser.getSubsystemList().size()));
        ButtonGroup buttonGroup = new ButtonGroup();
        for(JToggleButton button: panelButtons.values()) {
            buttonGroup.add(button);
            toolBar.add(button);
        }
        add(toolBar, "dock north");
        panelButtons.get(ssParser.getSubsystemList().get(0)).setSelected(true);

        currentPanel = sidescanPanels.get(ssParser.getSubsystemList().get(0));

        add(currentPanel, "w 100%, h 100%, wrap");

        add(timeline, "w 100%, split");
    }

    private void updateSidescanPanel(int subsystem) {
        if (currentPanel == sidescanPanels.get(subsystem)) {
            return;
        }
        remove(currentPanel);
        remove(timeline);
        currentPanel = sidescanPanels.get(subsystem);
        add(currentPanel, "w 100%, h 100%, wrap");
        add(timeline, "w 100%, split");
    }

    @Override
    public Component getComponent(IMraLogGroup source, double timestep) {
        initialize(source);
        revalidate();
        repaint();

        return this;
    }

    public SidescanHistogramNormalizer getHistogram() {
        return histogram;
    }

    /**
     * @return the timeline
     */
    public Timeline getTimeline() {
        return timeline;
    }

    /**
     * @param timeline the timeline to set
     */
    public void setTimeline(Timeline timeline) {
        this.timeline = timeline;
    }

    @Override
    public void timelineChanged(int value) {
        try {
            if (!timeline.isRunning()) {
                return;
            }

            // This distinguishes between a drag and normal execution
            // If this is true but currentTime and lastTime as the same value
            if (Math.abs(value - currentTime) > 1000 / 15 * timeline.getSpeed()) {
                // this means it dragged
//                for (SidescanPanel p : sidescanPanels) {
//                    p.clearLines();
//                }
                currentPanel.clearLines();

                lastUpdateTime = value;
            }
            else {
                lastUpdateTime = currentTime;
            }

            currentTime = value;

            if (currentTime + firstPingTime >= lastPingTime) {
                timeline.pause();
            }

//            for (SidescanPanel p : sidescanPanels) {
//                try {
//                    p.updateImage(currentTime, lastUpdateTime);
//                    p.repaint();
//                }
//                catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
            try {
                currentPanel.updateImage(currentTime, lastUpdateTime);
                currentPanel.repaint();
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            timeline.setTime(firstPingTime + currentTime);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean canBeApplied(IMraLogGroup source) {
        return LogUtils.hasIMCSidescan(source) || SidescanParserFactory.existsSidescanParser(source)
                || source.getLog("SidescanPing") != null;
    }

    @Override
    public String getName() {
        return I18n.text("Sidescan Analyzer");
    }

    @Override
    public ImageIcon getIcon() {
        return ImageUtils.getScaledIcon("pt/lsts/neptus/plugins/echosounder/echosounder.png", 16, 16);
    }

    @Override
    public Double getDefaultTimeStep() {
        return 0.0;
    }

    @Override
    public boolean supportsVariableTimeSteps() {
        return false;
    }

    @Override
    public Type getType() {
        return Type.VISUALIZATION;
    }

    public ArrayList<LogMarker> getMarkerList() {
        return markerList;
    }

    @Override
    public void onCleanup() {
        if (timeline != null) {
            timeline.shutdown();
        }

        sidescanPanels.values().stream().forEach((p) -> p.clean());
        sidescanPanels.clear();
        removeAll();
        mraPanel = null;
        markerList.clear();
        if (ssParser != null) {
            ssParser.cleanup();
            ssParser = null;
        }
    }

    @Override
    public void onHide() {
        timeline.pause();
    }

    @Override
    public void onShow() {

    }

    @Override
    public void addLogMarker(LogMarker e) {
        markerList.add(e);
    }

    @Override
    public void removeLogMarker(LogMarker e) {
        markerList.remove(e);
    }

    @Override
    public void goToMarker(LogMarker marker) {

    }

}
