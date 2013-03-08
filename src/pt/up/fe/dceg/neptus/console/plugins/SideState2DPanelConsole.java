/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 20??/??/??
 * $Id:: SideState2DPanelConsole.java 9616 2012-12-30 23:23:22Z pdias     $:
 */
package pt.up.fe.dceg.neptus.console.plugins;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.plugins.ConfigurationListener;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.Popup.POSITION;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.comm.IMCUtils;

import com.rickyclarkson.java.awt.layout.PercentLayout;

@Popup(pos = POSITION.RIGHT, width = 500, height = 500)
@PluginDescription(name = "2D Side View", icon = "images/buttons/sensorstatus2dbutt.png")
public class SideState2DPanelConsole extends SimpleSubPanel implements ConfigurationListener, IPeriodicUpdates {
    private static final long serialVersionUID = -3122371180498520732L;

    @NeptusProperty(name="Up distance variable")
    public String upVar = "";
    @NeptusProperty(name="Down distance variable")
    public String downVar = "BottomDistance.DVL.value";
    @NeptusProperty(name="Back distance variable")
    public String leftVar = "";
    @NeptusProperty(name="Front distance variable")
    public String rightVar = "BottomDistance.EchoSounder.value";

    @NeptusProperty(name="Show up distance")
    public boolean up = true;
    @NeptusProperty(name="Show down distance")
    public boolean down = true;
    @NeptusProperty(name="Show back distance")
    public boolean back = true;
    @NeptusProperty(name="Show front distance")
    public boolean front = true;

    @NeptusProperty(name="Show sea level")
    public boolean sea = true;
    
    @NeptusProperty(name="Milliseconds between updates")
    public long updateMillis = 100;
    
    
    protected JPanel centerPanel;
    protected DistancePanel upDistancePanel;
    protected DistancePanel downDistancePanel;
    protected RollPitchVehiclePanel renderPanel;
    protected JPanel rightPanel;
    protected JPanel leftPanel;
    protected DistancePanel frontDistancePanel;
    protected DistancePanel backDistancePanel;
    protected JPanel insidePanel = new JPanel();

    @Override
    public long millisBetweenUpdates() {
        return updateMillis;
    }
    
    @Override
    public void initSubPanel() {
        renderPanel.setVehicle(getMainVehicleId());
    }
    
    @Override
    public void mainVehicleChangeNotification(String id) {
        renderPanel.setVehicle(id);
        update();
    }
    
    @Override
    public boolean update() {
    
        EstimatedState lastState = getState().lastEstimatedState();
        if (lastState == null)
            return true;
        
        renderPanel.setRoll((float)lastState.getPhi());
        
        if (!sea)
            renderPanel.setPitch((float)lastState.getTheta());        
        else
            renderPanel.setPitchDepth((float)lastState.getTheta(), (float)IMCUtils.getLocation(lastState).getAllZ());
        
        if(front)
            frontDistancePanel.setValue(getState().get(rightVar, Double.class));
        
        if(back)
            backDistancePanel.setValue(getState().get(leftVar, Double.class));
        
        if(up)
            upDistancePanel.setValue(getState().get(upVar, Double.class));
        
        if(down)
            downDistancePanel.setValue(getState().get(downVar, Double.class));
               
        repaint();
        return true;
    }
    
    public SideState2DPanelConsole(ConsoleLayout console) {
        super(console);
        removeAll();
        this.setSize(320, 240);
        setMinimumSize(new Dimension(100, 100));
        this.setResizable(true);
 
        this.setLayout(new BorderLayout());

        insidePanel.setLayout(new PercentLayout());

        createLeftPanel();
        insidePanel.add(leftPanel, new PercentLayout.Constraint(0, 0, 20, 100));

        createCenterPanel();
        insidePanel.add(centerPanel, new PercentLayout.Constraint(20, 0, 60, 100));

        createRightPanel();
        insidePanel.add(rightPanel, new PercentLayout.Constraint(80, 0, 20, 100));

        this.add(insidePanel, BorderLayout.CENTER);
    }

    public void reconfigure() {
        insidePanel.removeAll();
        insidePanel.setLayout(new PercentLayout());

        reconfigureCentral();

        if (front && back) {
            insidePanel.add(leftPanel, new PercentLayout.Constraint(0, 0, 20, 100));
            insidePanel.add(centerPanel, new PercentLayout.Constraint(20, 0, 60, 100));
            insidePanel.add(rightPanel, new PercentLayout.Constraint(80, 0, 20, 100));
        }

        if (front && !back) {
            insidePanel.add(centerPanel, new PercentLayout.Constraint(0, 0, 80, 100));
            insidePanel.add(rightPanel, new PercentLayout.Constraint(80, 0, 20, 100));
        }

        if (!front && back) {
            insidePanel.add(leftPanel, new PercentLayout.Constraint(0, 0, 20, 100));
            insidePanel.add(centerPanel, new PercentLayout.Constraint(20, 0, 80, 100));
        }

        if (!front && !back) {
            insidePanel.add(centerPanel, new PercentLayout.Constraint(0, 0, 100, 100));
        }

        renderPanel.setSea(sea);

        forceRepaint();       
    }

    public void reconfigureCentral() {

        centerPanel.removeAll();
        // leftPanel.setLayout(new PercentLayout());

        if (up && down) {
            centerPanel.setLayout(new GridLayout(3, 1));
            centerPanel.add(upDistancePanel);
            centerPanel.add(renderPanel);
            centerPanel.add(downDistancePanel);
        }
        if (!up && down) {
            centerPanel.setLayout(new GridLayout(2, 1));
            // centerPanel.add(upDistancePanel);
            centerPanel.add(renderPanel);
            centerPanel.add(downDistancePanel);
        }
        if (up && !down) {
            centerPanel.setLayout(new GridLayout(2, 1));
            centerPanel.add(upDistancePanel);
            centerPanel.add(renderPanel);
            // centerPanel.add(downDistancePanel);
        }
        if (!up && !down) {
            centerPanel.setLayout(new GridLayout(1, 1));
            // centerPanel.add(upDistancePanel);
            centerPanel.add(renderPanel);
            // centerPanel.add(downDistancePanel);
        }
    }

    public void forceRepaint() {
        this.doLayout();
        this.repaint();
        centerPanel.doLayout();
        centerPanel.repaint();

        leftPanel.doLayout();
        leftPanel.repaint();

        rightPanel.doLayout();
        rightPanel.repaint();

        upDistancePanel.doLayout();
        upDistancePanel.repaint();

        downDistancePanel.doLayout();
        downDistancePanel.repaint();

        frontDistancePanel.doLayout();
        frontDistancePanel.repaint();

        backDistancePanel.doLayout();
        backDistancePanel.repaint();

        renderPanel.doLayout();
        renderPanel.repaint();

    }

    public void createLeftPanel() {
        leftPanel = new JPanel();

        // leftPanel.setLayout(new PercentLayout());
        leftPanel.setLayout(new BorderLayout());

        createBackDistancePanel();

        leftPanel.add(backDistancePanel, BorderLayout.CENTER);

    }

    public void createCenterPanel() {
        centerPanel = new JPanel();
        // leftPanel.setLayout(new PercentLayout());
        centerPanel.setLayout(new GridLayout(3, 1));

        createUpDistancePanel();
        createRenderPanel();
        createDownDistancePanel();

        centerPanel.add(upDistancePanel);
        centerPanel.add(renderPanel);
        centerPanel.add(downDistancePanel);
    }

    public void createRightPanel() {
        rightPanel = new JPanel();
        // leftPanel.setLayout(new PercentLayout());
        rightPanel.setLayout(new BorderLayout());

        createFrontDistancePanel();

        rightPanel.add(frontDistancePanel, BorderLayout.CENTER);
    }

    public void createFrontDistancePanel() {
        frontDistancePanel = new DistancePanel();
        frontDistancePanel.setType(DistancePanel.HORIZONTAL_RIGHT);
    }

    public void createBackDistancePanel() {
        backDistancePanel = new DistancePanel();
        backDistancePanel.setType(DistancePanel.HORIZONTAL_LEFT);
    }

    public void createUpDistancePanel() {
        // novo Backgroung 2, 113, 171
        // Color3f bgColor = new Color3f(0.007843137254901961f,
        // 0.4431372549019608f, 0.6705882352941176f);

        upDistancePanel = new DistancePanel();
        upDistancePanel.setType(DistancePanel.VERTICAL_UP);
        // upDistancePanel.setBackground(new Color(2, 113, 171));
    }

    public void createDownDistancePanel() {
        // novo Backgroung 2, 113, 171
        // Color3f bgColor = new Color3f(0.007843137254901961f,
        // 0.4431372549019608f, 0.6705882352941176f);
        downDistancePanel = new DistancePanel();
        downDistancePanel.setType(DistancePanel.VERTICAL_DOWN);
        // downDistancePanel.setBackground(new Color(2, 113, 171));
    }

    public void createRenderPanel() {
        renderPanel = new RollPitchVehiclePanel();        
    }

    public void setVarUp(String varX) {
      
            this.upVar = varX;
    }

    public String getVarUp() {
        return this.upVar;
    }

    public void setVarDown(String varX) {
       
            this.downVar = varX;
    }

    public String getVarDown() {
        return this.downVar;
    }

    public void setVarLeft(String varX) {
       
            this.leftVar = varX;
    }

    public String getVarLeft() {
        return this.leftVar;
    }

    public void setVarRight(String varX) {
       
            this.rightVar = varX;
    }

    public String getVarRight() {
        return this.rightVar;
    }

   
    @Override
    public void propertiesChanged() {
        reconfigure();
    }
    
    public boolean isUp() {
        return up;
    }

    public void setUp(boolean up) {
        this.up = up;
    }

    public boolean isDown() {
        return down;
    }

    public void setDown(boolean down) {
        this.down = down;
    }

    public boolean isLeft() {
        return back;
    }

    public void setLeft(boolean left) {
        this.back = left;
    }

    public boolean isRight() {
        return front;
    }

    public void setRight(boolean right) {
        this.front = right;
    }

    public boolean isSea() {
        return sea;
    }

    public void setSea(boolean sea) {
        this.sea = sea;
    }
    
    public static void main(String[] args) {
        ConsoleParse.testSubPanel(SideState2DPanelConsole.class);
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
