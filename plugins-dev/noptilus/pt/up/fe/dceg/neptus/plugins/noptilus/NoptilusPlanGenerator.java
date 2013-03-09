/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Oct 16, 2012
 */
package pt.up.fe.dceg.neptus.plugins.noptilus;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.PropertiesEditor;
import pt.up.fe.dceg.neptus.imc.Goto;
import pt.up.fe.dceg.neptus.imc.Goto.SPEED_UNITS;
import pt.up.fe.dceg.neptus.imc.IMCUtil;
import pt.up.fe.dceg.neptus.imc.PlanSpecification;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.renderer2d.Renderer2DPainter;
import pt.up.fe.dceg.neptus.renderer2d.StateRenderer2D;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;

/**
 * @author noptilus
 */
@PluginDescription(name = "Noptilus Plan Generation", author="Noptilus")
public class NoptilusPlanGenerator extends SimpleSubPanel implements Renderer2DPainter {

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
    public Goto.SPEED_UNITS units = SPEED_UNITS.RPM;

    @NeptusProperty(name = "Waypoints folder", hidden = true)
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
                                    pt.up.fe.dceg.neptus.imc.FollowPath.SPEED_UNITS.valueOf(units.toString()));
                        else
                            spec = PlanUtils.planFromWaypoints(plan_id, waypoints, speed, units);
                        if (showMessage)
                            IMCUtil.debug(spec);

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
                    File replayFile = new File(chooser.getSelectedFile().getAbsolutePath().replace("_Positions", "_State"));
                    
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
                    System.out.println(replay);
                }
            }
        });
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
