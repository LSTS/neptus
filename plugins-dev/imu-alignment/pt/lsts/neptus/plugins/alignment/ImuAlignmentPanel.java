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
 * Author: zp
 * Jun 27, 2013
 */
package pt.lsts.neptus.plugins.alignment;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pt.lsts.imc.AlignmentState;
import pt.lsts.imc.EntityActivationState;
import pt.lsts.imc.EntityParameter;
import pt.lsts.imc.EntityState;
import pt.lsts.imc.EstimatedState;
import pt.lsts.imc.QueryEntityActivationState;
import pt.lsts.imc.SetEntityParameters;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.console.notifications.Notification;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mp.Maneuver.SPEED_UNITS;
import pt.lsts.neptus.mp.ManeuverLocation;
import pt.lsts.neptus.mp.templates.PlanCreator;
import pt.lsts.neptus.plugins.NeptusProperty;
import pt.lsts.neptus.plugins.NeptusProperty.LEVEL;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;
import pt.lsts.neptus.plugins.update.IPeriodicUpdates;
import pt.lsts.neptus.types.coord.LocationType;
import pt.lsts.neptus.types.mission.plan.PlanType;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.speech.SpeechUtil;

import com.google.common.eventbus.Subscribe;

/**
 * This panel will allow monitoring and alignment of some IMUs
 * @author zp
 */
@PluginDescription(author = "ZP", name = "IMU Alignment")
@Popup(accelerator=KeyEvent.VK_I, pos=Popup.POSITION.CENTER, height=300, width=300, name="IMU Alignment")
public class ImuAlignmentPanel extends ConsolePanel implements IPeriodicUpdates {

    private static final long serialVersionUID = -1330079540844029305L;
    protected JToggleButton enableImu;
    protected JButton doAlignment;
    protected JEditorPane status;

    // used to activate and deactivate payload
    @NeptusProperty(name="IMU Entity Label", userLevel=LEVEL.ADVANCED)
    public String imuEntity = "IMU";

    @NeptusProperty(name="Navigation Entity Label", userLevel=LEVEL.ADVANCED)
    public String navEntity = "Navigation";

    @NeptusProperty(name="Square Side Length", userLevel=LEVEL.REGULAR)
    public double squareSideLength = 80;

    @NeptusProperty(name="Alignment Speed", userLevel=LEVEL.REGULAR)
    public double alignSpeed = 1.25;

    @NeptusProperty(name="Alignment Speed Units", userLevel=LEVEL.REGULAR)
    public SPEED_UNITS alignSpeedUnits = SPEED_UNITS.METERS_PS;
    
    @NeptusProperty(name="Use Error Notifications", userLevel = LEVEL.ADVANCED, description="Use an error instead of warning when navigation becomes not aligned")
    public boolean useErrorNotification = false;
    
    @NeptusProperty(name = "Use Audio Alerts", userLevel = LEVEL.REGULAR)
    public boolean useAudioAlerts = true;
    
    private boolean aligned = false;
    
    protected ImageIcon greenLed = ImageUtils.getIcon("pt/lsts/neptus/plugins/alignment/led_green.png");
    protected ImageIcon redLed = ImageUtils.getIcon("pt/lsts/neptus/plugins/alignment/led_red.png");
    protected ImageIcon grayLed = ImageUtils.getIcon("pt/lsts/neptus/plugins/alignment/led_none.png");

    public ImuAlignmentPanel(ConsoleLayout console) {
        super(console);        
        setLayout(new BorderLayout(3, 3));
        removeAll();
        JPanel top = new JPanel(new GridLayout(1, 0));
        enableImu = new JToggleButton(I18n.text("Enable IMU"), grayLed, false);
        enableImu.setToolTipText(I18n.text("Waiting for first IMU alignment state"));
        enableImu.setEnabled(false);
        top.add(enableImu);

        enableImu.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleImu(enableImu.isSelected());                
            }
        });
        
        doAlignment = new JButton(I18n.text("Do Alignment"));
        top.add(doAlignment);
        status = new JEditorPane("text/html", updateState());
        doAlignment.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doAlignment();
            }
        });
        add(top, BorderLayout.NORTH);
        add(status, BorderLayout.CENTER);
    }

    @Override
    public long millisBetweenUpdates() {        
        return 1000;
    }

    @Override
    public boolean update() {
        int imuId = EntitiesResolver.resolveId(getMainVehicleId(), imuEntity);
        if (imuId != -1) {
            QueryEntityActivationState qeas = new QueryEntityActivationState();
            qeas.setDstEnt(imuId);
            send(qeas);
        }
       
        status.setText(updateState());
        return true;
    }

    public String updateState() {
        AlignmentState alignState = getState().last(AlignmentState.class);
        EntityState imuState = (EntityState) getState().get(EntityState.ID_STATIC, EntitiesResolver.resolveId(getMainVehicleId(), imuEntity));
        EntityActivationState imuActivationState = (EntityActivationState) getState().get(EntityActivationState.ID_STATIC, EntitiesResolver.resolveId(getMainVehicleId(), imuEntity));
        
        if (imuState == null) { 
            enableImu.setEnabled(false);
            enableImu.setIcon(grayLed);
        }
        else {
            boolean active = false;
            if (imuActivationState != null) {
                switch (imuActivationState.getState()) {
                    case ACTIVE:
                        active = true;
                        break;
                    default:
                        active = false;
                        break;
                }
            }
            else {
                String activeStr = imuState.getDescription();
                if ("active".equalsIgnoreCase(activeStr) || I18n.text("active").equalsIgnoreCase(activeStr)) {
                    active = true;
                }
            }
            if (active) {
                enableImu.setSelected(true);
                enableImu.setIcon(greenLed);
                enableImu.setText(I18n.text("IMU Enabled"));
                enableImu.setToolTipText(null);
            }
            else {
                enableImu.setSelected(false);
                enableImu.setIcon(redLed);
                enableImu.setText(I18n.text("Enable IMU"));
                enableImu.setToolTipText(null);
                enableImu.setIcon(grayLed);
            }
        }

        if (alignState == null)
            return "<html><h1>"+I18n.text("Waiting for IMU alignment state")+"</h1></html>";
        switch(alignState.getState()) {
            case ALIGNED:
                // enableImu.setIcon(greenLed);
                enableImu.setToolTipText(I18n.text("IMU aligned. Vehicle can be used in dead-reckoning mode."));
                enableImu.setEnabled(true);
                return "<html><h1><font color='green'>"+I18n.text("IMU aligned")+"</font></h1>"
                +"<p>"+I18n.text("Vehicle can now be used to execute dead reckoning missions.")+"</p>"
                +"</html>";
            case NOT_ALIGNED:
                // enableImu.setIcon(redLed);
                enableImu.setEnabled(true);
                enableImu.setToolTipText(I18n.text("IMU is not aligned"));
                return "<html><h1><font color='red'>"+I18n.text("IMU not aligned")+"</font></h1>"
                +"<p>"+I18n.text("In order to execute dead reckoning missions, IMU must first be aligned.")+"</p>"                
                +"</html>";
            default:
                // enableImu.setIcon(grayLed);
                enableImu.setEnabled(false);
                enableImu.setToolTipText(I18n.textf("IMU cannot be aligned on %vehicle", alignState.getSourceName()));
                enableImu.setSelected(false);
                return "<html><h1><font color='red'>"+I18n.text("IMU cannot be aligned")+"</font></h1>"
                +"<p>"+I18n.text("This vehicle does not support IMU alignment.")+"</p>"                
                +"</html>";
        }
    }

    public void doAlignment() {

        int opt = GuiUtils.confirmDialog(getConsole(), I18n.text("Alignment Procedure"), "<html><h2>" + I18n.text("Alignment Procedure") + "</h2>"
                +I18n.text("To do IMU alignment, the vehicle must do straigth segments at the surface.") + "<br>"
                +"<b>" + I18n.text("Do you want me to create a plan at surface for you?") + "</b>");


        if (opt == JOptionPane.YES_OPTION) {
            EstimatedState lastState = getState().last(EstimatedState.class);

            PlanCreator pc = new PlanCreator(getConsole().getMission());
            if (lastState != null && lastState.getLat() != 0) {
                LocationType loc = new LocationType(
                        Math.toDegrees(lastState.getLat()), 
                        Math.toDegrees(lastState.getLon()));
                loc.translatePosition(lastState.getX(), lastState.getY(), 0);
                pc.setLocation(loc);
            }
            else {
                pc.setLocation(new LocationType(getConsole().getMission().getHomeRef()));
            }
            pc.setSpeed(alignSpeed, alignSpeedUnits);
            pc.setZ(0, ManeuverLocation.Z_UNITS.DEPTH);

            pc.addGoto(null);
            pc.move(squareSideLength, 0);
            pc.addGoto(null);
            pc.move(0, -squareSideLength);
            pc.addGoto(null);
            pc.move(-squareSideLength, 0);
            pc.addGoto(null);
            pc.move(0, squareSideLength);
            pc.addGoto(null);
            
            PlanType pt = pc.getPlan();
            pt.setId("alignment_template");
            pt.setMissionType(getConsole().getMission());
            
            pt.setVehicle(getConsole().getMainSystem());
            getConsole().getMission().addPlan(pt);
            getConsole().setPlan(pt);
            getConsole().warnMissionListeners();
            getConsole().getMission().save(true);                        
        }
    }

    public void toggleImu(boolean newState) {
        Vector<EntityParameter> params = new Vector<>();
        params.add(new EntityParameter("Active", "" + newState));
        SetEntityParameters m = new SetEntityParameters(imuEntity, params);
        send(m);
    }

    @Override
    public void initSubPanel() {
        
    }

    @Subscribe
    public void on(ConsoleEventMainSystemChange evt) {
        update();
    }
    
    @Subscribe
    public void on(EntityState entityState) {
        
        if (!entityState.getSourceName().equals(getMainVehicleId()))
            return;
        int eid = EntitiesResolver.resolveId(getMainVehicleId(), "Navigation");
        if (eid == -1)
            eid = EntitiesResolver.resolveId(getMainVehicleId(), I18n.text("Navigation"));
        if (entityState.getSrcEnt() != eid)
            return;
        
        boolean wasAligned = aligned;
        
        if (entityState.getDescription().equals("not aligned") || entityState.getDescription().equals(I18n.text("not aligned")))
            aligned = false;
        else
            aligned = true;
        
        if (!aligned && wasAligned) {
            if (useErrorNotification)
                post(Notification.error(I18n.text("Navigation"), I18n.text("IMU is not aligned")));
            else
                post(Notification.warning(I18n.text("Navigation"), I18n.text("IMU is not aligned")));
            if (useAudioAlerts)
                SpeechUtil.readSimpleText("I M U is not aligned");
        }
        
        if (aligned && !wasAligned) {
            post(Notification.info(I18n.text("Navigation"), I18n.text("IMU is ready")));
            if (useAudioAlerts && (entityState.getDescription().equals("aligned") || entityState.getDescription().equals(I18n.text("aligned"))))
                SpeechUtil.readSimpleText("I M U is ready");
        }
    }
    
    @Override
    public void cleanSubPanel() {
    }
}
