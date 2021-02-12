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
 * Author: José Pinto
 * Oct 16, 2012
 */
package pt.lsts.neptus.plugins.noptilus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;

import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.def.SpeedUnits;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.IMCUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.renderer2d.Renderer2DPainter;
import pt.lsts.neptus.renderer2d.StateRenderer2D;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;

/**
 * @author noptilus
 */
@PluginDescription(name = "Noptilus Plan Generation", author = "Noptilus")
public class NoptilusPlanGenerator extends ConsolePanel implements Renderer2DPainter {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name = "Multiplication factor")
    public double multFactor = 1.0;

    @NeptusProperty(name = "Addition factor")
    public double addFactor = 0.0;

    @NeptusProperty(name = "Use FollowPath maneuver")
    public boolean useFollowPath = false;

    @NeptusProperty(name = "Minimum distance between waypoints")
    public double minDistance = 0.0;

    @NeptusProperty(name = "Plan Speed")
    public double speed = 1000.0;

    @NeptusProperty(name = "Speed Units")
    public SpeedUnits units = SpeedUnits.RPM;

    @NeptusProperty(name = "Waypoints folder", editable = true)
    public String defaultFolder = ".";

    @NeptusProperty(name = "Show generated message")
    public boolean showMessage = false;

    @NeptusProperty(name = "Replay interval in milliseconds")
    public long replayTimestep = 500;

    protected Vector<LocationType> landmarkPositions = new Vector<LocationType>();
    protected Vector<Color> landmarkColors = new Vector<Color>();
    protected LandmarkReplay replay = null;

    public NoptilusPlanGenerator(ConsoleLayout console) {
        super(console);
        setVisibility(false);
    }

    @Override
    public void paint(Graphics2D g, StateRenderer2D renderer) {
        if (replay != null) {
            replay.paint(g, renderer);
        }
    }

    @Override
    public void initSubPanel() {
        addMenuItem("Noptilus>Plan Generation>Generate plan", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(defaultFolder);
                chooser.setDialogTitle("Open waypoints file");
                int option = chooser.showOpenDialog(getConsole());

                if (option == JFileChooser.APPROVE_OPTION) {
                    File selection = chooser.getSelectedFile();
                    defaultFolder = selection.getParent();
                    try {
                        Vector<double[]> waypoints = PlanUtils.loadWaypoints(selection);

                        PlanUtils.normalizeZ(waypoints, multFactor, addFactor);

                        if (minDistance > 0)
                            PlanUtils.filterShortDistances(waypoints, minDistance);

                        String plan_id = JOptionPane.showInputDialog(getConsole(), "Enter desired plan id",
                                "generated_plan");
                        PlanSpecification spec;

                        if (useFollowPath)
                            spec = PlanUtils.trajectoryPlan(plan_id, waypoints, speed,
                                    SpeedUnits.valueOf(units.toString()));
                        else
                            spec = PlanUtils.planFromWaypoints(plan_id, waypoints, speed, units);
                        if (showMessage) {
                            JFrame frm = GuiUtils.testFrame(new JScrollPane(new JLabel(IMCUtils.getAsHtml(spec))));
                            frm.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        }

                        PlanType plan = IMCUtils.parsePlanSpecification(getConsole().getMission(), spec);
                        plan.setVehicle(getConsole().getMainSystem());
                        getConsole().getMission().addPlan(plan);
                        getConsole().warnMissionListeners();
                        getConsole().setPlan(plan);
                        getConsole().getMission().save(false);
                    }
                    catch (Exception ex) {
                        GuiUtils.errorMessage(getConsole(), ex);
                        return;
                    }
                }
            }
        });

        addMenuItem("Noptilus>Plan Generation>Settings", null, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                PropertiesEditor.editProperties(NoptilusPlanGenerator.this, true);
                getConsole().saveFile();
            }
        });

        addMenuItem("Noptilus>Plan Generation>Load Landmarks", null, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser chooser = new JFileChooser(defaultFolder);
                chooser.setDialogTitle("Open landmarks file");
                int option = chooser.showOpenDialog(getConsole());

                if (option == JFileChooser.APPROVE_OPTION) {
                    File landmarksFile = chooser.getSelectedFile();
                    File replayFile = new File(chooser.getSelectedFile().getAbsolutePath()
                            .replace("_Positions", "_State"));

                    if (!replayFile.canRead())
                        replayFile = null;

                    if (replay != null)
                        replay.cleanup();

                    try {
                        replay = new LandmarkReplay(landmarksFile, replayFile, replayTimestep);

                    }

                    catch (Exception ex) {
                        GuiUtils.errorMessage(getConsole(), ex);
                    }
                    NeptusLog.pub().info("<###> " + replay);
                }
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        removeMenuItem("Noptilus>Plan Generation>Generate plan");
        removeMenuItem("Noptilus>Plan Generation>Settings");
        removeMenuItem("Noptilus>Plan Generation>Load Landmarks");
    }
}
