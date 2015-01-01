/*
 * Copyright (c) 2004-2015 Universidade do Porto - Faculdade de Engenharia
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
 * Author: Sérgio Ferreira
 * Apr 24, 2014
 */
package pt.lsts.neptus.plugins.uavs.panels;

import java.awt.Color;
import java.text.DecimalFormat;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import pt.lsts.imc.IMCDefinition;
import pt.lsts.imc.IMCMessage;
import pt.lsts.imc.IndicatedSpeed;
import pt.lsts.imc.Maneuver;
import pt.lsts.imc.PlanControl;
import pt.lsts.imc.PlanControlState;
import pt.lsts.imc.PlanControlState.STATE;
import pt.lsts.imc.PlanManeuver;
import pt.lsts.imc.PlanSpecification;
import pt.lsts.imc.TrueSpeed;
import pt.lsts.neptus.comm.IMCSendMessageUtils;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.plugins.MainVehicleChangeListener;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription.CATEGORY;
import pt.lsts.neptus.util.ImageUtils;

import com.google.common.eventbus.Subscribe;


/**
 * Neptus panel designed to show indicated speed and ground speed on the same frame. It allows for the setup of limits for maximum and
 * minimum acceptable velocity.
 * 
 * @author canastaman
 * @author jfortuna
 * @version 3.0
 * @category UavPanel  
 * 
 */
@PluginDescription(name = "Speed Indicator Panel", icon = "pt/lsts/neptus/plugins/uavs/speed.png", author = "canasta",  version = "3.0", category = CATEGORY.INTERFACE)
public class SpeedIndicatorPanel extends ConsolePanel implements MainVehicleChangeListener {

    private static final long serialVersionUID = 1L;

    @NeptusProperty(name="Minimum Speed", description="Speed below which the vehicle enters VStall (m/s)",  userLevel = LEVEL.REGULAR)
    public double minSpeed = 12.0;

    @NeptusProperty(name="Maximum Speed", description="Speed above which it's undesirable to fly (m/s)",  userLevel = LEVEL.REGULAR)
    public double maxSpeed = 25.0;

    //To be used if other speed units are desired
    static public final double MS_TO_KNOTS_CONV = 1.94384449244;

    //Illustrative icons to differentiate speeds
    private ImageIcon ICON_TSPEED;
    private ImageIcon ICON_GSPEED;
    private ImageIcon ICON_ASPEED;

    //indicates if the UAV as changed plan and needs to update it's command speed value
    private boolean samePlan = false;

    private String currentPlan = null;
    private String currentManeuver = null;

    private PlanSpecification planSpec = null;

    //various speeds
    private double aSpeed = 0.0;
    private double gSpeed = 0.0;
    private double tSpeed = 0.0;

    //sub panels used to better accommodate the information through the use of the layout manager
    private JPanel titlePanel = null;
    private JPanel topLabelPanel = null; 
    private JPanel speedPanel = null;
    private JPanel speedNumberPanel = null;
    private JPanel speedGraphPanel = null; 
    private JPanel bottomLabelPanel = null;

    private JProgressBar aSpeedBar = null;
    private JProgressBar gSpeedBar = null;

    private JLabel aSpeedLabel = null;
    private JLabel gSpeedLabel = null;
    private JLabel tSpeedLabel = null;
    private JLabel maxSpeedLabel = null;

    //display output formatter
    private DecimalFormat formatter = new DecimalFormat("0.0");

    public SpeedIndicatorPanel(ConsoleLayout console) {
        super(console);

        // clears all the unused initializations of the standard SimpleSubPanel
        removeAll();
    }

    //Listeners
    @Subscribe
    public void on(TrueSpeed msg) {
        if(msg.getSourceName().equals(getConsole().getMainSystem())) {
            gSpeed = msg.getValue();

            //speeds updated
            speedLabelPositionUpdate();
        }
    }
    
    @Subscribe
    public void on(IndicatedSpeed msg) {
        if(msg.getSourceName().equals(getConsole().getMainSystem())) {
            aSpeed = msg.getValue();

            //speeds updated
            speedLabelPositionUpdate();
        }
    }
    
    @Subscribe
    public void on(PlanControlState msg) {
        if(msg.getSourceName().equals(getConsole().getMainSystem())) {
            //if the vehicle is currently executing a plan we ask for that plan 
            //and then identify what maneuver is being executed                   
            if(msg.getAsNumber("state").longValue() == STATE.EXECUTING.value()){    

                if(!msg.getAsString("plan_id").equals(currentPlan))
                    samePlan = false;

                currentPlan = msg.getAsString("plan_id");
                currentManeuver = msg.getAsString("man_id");

                if(planSpec != null && samePlan){
                    for(PlanManeuver planMan: planSpec.getManeuvers()){
                        if(planMan.getManeuverId().equals(currentManeuver)){
                            Maneuver man = planMan.getData();
                            tSpeed = man.getAsNumber("speed").doubleValue();

                            //plan updated
                            tSpeedLabelPositionUpdate();
                        }
                    }
                }

                if(!samePlan){
                    IMCMessage planControlMessage = IMCDefinition.getInstance().create("PlanControl");
                    planControlMessage.setValue("type", 0);        
                    planControlMessage.setValue("op", "GET");
                    planControlMessage.setValue("request_id",IMCSendMessageUtils.getNextRequestId());

                    IMCSendMessageUtils.sendMessage(planControlMessage, I18n.text("Error requesting plan specificaion"),true, 
                            getConsole().getMainSystem());
                }
            }
            else{
                samePlan = false;
            }
        }
    }
    
    @Subscribe
    public void on(PlanControl msg) {
        if(msg.getSourceName().equals(getConsole().getMainSystem()) && !samePlan) {
            if(msg.getMessage("arg").getAbbrev().equals("PlanSpecification")){
                planSpec = (PlanSpecification) msg.getMessage("arg");
                samePlan = true;                      
            }                   
        }
    }
    
    @Override
    public void initSubPanel() {
        ICON_TSPEED = ImageUtils.createScaleImageIcon("pt/lsts/neptus/plugins/uavs/icons/target.png",(int)(this.getHeight()*0.2),(int)(this.getHeight()*0.2));
        ICON_GSPEED = ImageUtils.createScaleImageIcon("pt/lsts/neptus/plugins/uavs/icons/ground.png",(int)(this.getHeight()*0.2),(int)(this.getHeight()*0.2));
        ICON_ASPEED = ImageUtils.createScaleImageIcon("pt/lsts/neptus/plugins/uavs/icons/air.png",(int)(this.getHeight()*0.2),(int)(this.getHeight()*0.2));

        titlePanelSetup();
        topLabelPanelSetup();
        bottomLabelPanelSetup();
        speedPanelSetup();

        //panel general layout setup
        this.setLayout(new MigLayout("gap 0 0, ins 0"));
        this.add(titlePanel,"w 100%, h 15%!, wrap"); 
        this.add(topLabelPanel,"w 100%, h 15%!, wrap"); 
        this.add(speedPanel,"w 100%, h 50%!, wrap"); 
        this.add(bottomLabelPanel,"w 100%, h 20%!");
    }

    /**
     * 
     */
    private void titlePanelSetup() {
        titlePanel = new JPanel(new MigLayout("gap 0 0, ins 0"));
        titlePanel.add(new JLabel(I18n.text("Speed Indicator"),SwingConstants.CENTER), "w 100%, h 100%");
    }

    /**
     * 
     */
    private void topLabelPanelSetup() {
        maxSpeedLabel = new JLabel(formatter.format(maxSpeed),SwingConstants.RIGHT);    
        topLabelPanel = new JPanel(new MigLayout("gap 0 0, ins 0, rtl"));

        JPanel tempPanel2 = new JPanel(new MigLayout("ins 0"));
        tempPanel2.add(new JLabel("0.0",SwingConstants.LEFT), "w 50%, h 100%");                    
        tempPanel2.add(maxSpeedLabel, "w 50%, h 100%");

        topLabelPanel.add(tempPanel2, "w 80%, h 100%");
    }

    /**
     * 
     */
    private void speedPanelSetup() {
        speedPanel = new JPanel(new MigLayout("gap 0 0, ins 0"));

        speedNumberPanel = new JPanel(new MigLayout("ins 0"));
        aSpeedLabel = new JLabel(ICON_ASPEED);
        gSpeedLabel = new JLabel(ICON_GSPEED);
        speedNumberPanel.add(aSpeedLabel, "w 100%, h 50%, wrap");
        speedNumberPanel.add(gSpeedLabel, "w 100%, h 50%");

        speedGraphPanel = new JPanel(new MigLayout("ins 0, rtl"));
        
        aSpeedBar = new JProgressBar(0, (int)(maxSpeed * 10));
        aSpeedBar.setForeground(Color.cyan.darker());
        aSpeedBar.setBorderPainted(false);
        
        gSpeedBar = new JProgressBar(0, (int)(maxSpeed * 10));
        gSpeedBar.setForeground(Color.green.darker());
        gSpeedBar.setBorderPainted(false);
        
        speedGraphPanel.add(aSpeedBar, "w 100%, h 50%, wrap");
        speedGraphPanel.add(gSpeedBar, "w 100%, h 50%");

        speedLabelPositionUpdate();

        speedPanel.add(speedNumberPanel, "w 20%, h 100%");
        speedPanel.add(speedGraphPanel, "w 80%, h 100%");
    }

    /**
     * 
     */
    private void bottomLabelPanelSetup() {               
        bottomLabelPanel = new JPanel(new MigLayout("gap 0 0, ins 0, rtl"));      
        tSpeedLabel = new JLabel(ICON_TSPEED);
        tSpeedLabel.setHorizontalTextPosition(SwingConstants.LEADING);
        tSpeedLabel.setHorizontalAlignment(SwingConstants.LEFT);       

        tSpeedLabelPositionUpdate();
    }

    /**
     * 
     */
    private void speedLabelPositionUpdate() {
        //lazy workaround
        maxSpeedLabel.setText(formatter.format(maxSpeed));

        aSpeedBar.setValue((int)(aSpeed * 10));
        if (aSpeed < minSpeed) {
            aSpeedBar.setForeground(Color.red.darker());
        }
        else {
            aSpeedBar.setForeground(Color.cyan.darker());
        }
        aSpeedBar.setString(formatter.format(aSpeed));
        aSpeedBar.setStringPainted(true);
        gSpeedBar.setValue((int)(gSpeed * 10));
        gSpeedBar.setString(formatter.format(gSpeed));
        gSpeedBar.setStringPainted(true);

        revalidate();
    }

    /**
     * 
     */
    // TODO Fix position
    private void tSpeedLabelPositionUpdate() {
        tSpeedLabel.setText(formatter.format(tSpeed));
        int iconSizePercent = ICON_TSPEED.getIconWidth()*50/this.getWidth();
        int tPercent = (int)(tSpeed*80/maxSpeed) + tSpeedLabel.getMinimumSize().width*100/this.getWidth() + iconSizePercent; 

        bottomLabelPanel.remove(tSpeedLabel);
        bottomLabelPanel.add(tSpeedLabel, "w "+ (100-tPercent) +"%, h 100%");
    }

    /* (non-Javadoc)
     * @see pt.lsts.neptus.console.ConsolePanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
