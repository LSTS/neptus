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
 * Jan 14, 2013
 */
package pt.lsts.neptus.plugins.calib;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import com.google.common.eventbus.Subscribe;
import com.jogamp.newt.event.KeyEvent;

import pt.lsts.imc.DevCalibrationControl;
import pt.lsts.imc.DevCalibrationControl.OP;
import pt.lsts.imc.DevCalibrationState;
import pt.lsts.imc.EntityList;
import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.EntitiesResolver;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.console.events.ConsoleEventMainSystemChange;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.Popup;

/**
 * @author zp
 *
 */
@Popup(accelerator=KeyEvent.VK_F10, width=400, height=250)
@PluginDescription(name="Device Calibration", icon="pt/lsts/neptus/plugins/calib/calib.png")
public class CalibrationSubPanel extends ConsolePanel {

    private static final long serialVersionUID = 1L;

    protected JButton btnStart;
    protected JButton btnStop = new JButton(I18n.text("Cancel"));
    protected JButton btnNext = new JButton(I18n.text("Next Step") + " >"); 
    protected JButton btnPrev = new JButton("< " + I18n.text("Previous Step"));
    protected JEditorPane calibText;
    protected JPanel bottom;
    protected int calibratingEntity = -1;
    
    protected JComboBox<String> entitySelection = null;
    
    public CalibrationSubPanel(ConsoleLayout cl) {
        super(cl);
        initializeInterface();
    }
    
    @Subscribe
    public void on(EntityList list) {
        try {
            if (list.getSourceName() == null || !list.getSourceName().equals(getMainVehicleId()))
                return;
            if (entitySelection == null) {
                
                String[] entities = list.getList().keySet().toArray(new String[0]);
                Arrays.sort(entities);
                
                entitySelection = new JComboBox<String>(entities);
                entitySelection.addActionListener(new ActionListener() {
                    
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        DevCalibrationControl cmd = new DevCalibrationControl();
                        calibratingEntity = EntitiesResolver.resolveId(getMainVehicleId(), entitySelection.getSelectedItem()+""); 
                        cmd.setDstEnt(calibratingEntity);
                        cmd.setOp(OP.START);
                        send(cmd);
                    }
                });
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        add(entitySelection, BorderLayout.NORTH);
                        invalidate();
                        validate();
                        repaint();
                        calibText.setText(I18n.text("Select entity to be calibrated"));
                    }
                });            
            }
        }
        catch (Exception e) {
            // FIXME Temporary fix for null pointer
            NeptusLog.pub().fatal(e.getMessage() + " :: main system=" + getMainVehicleId(), e);
        }
    }
    
    @Subscribe 
    public void on(ConsoleEventMainSystemChange change) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                if (entitySelection != null)
                    remove(entitySelection);
                entitySelection = null;
                invalidate();
                validate();
                repaint();
                calibratingEntity = -1;
            }
        });        
    }
    
    @Subscribe
    public void on(DevCalibrationState state) {
        if (state.getSourceName().equals(getMainVehicleId()) && state.getSrcEnt() == calibratingEntity)
            calibText.setText(state.getStep());
        btnNext.setEnabled(((state.getFlags() & DevCalibrationState.DCS_NEXT_NOT_SUPPORTED) == 0));
        btnPrev.setEnabled(((state.getFlags() & DevCalibrationState.DCS_PREVIOUS_NOT_SUPPORTED) == 0));
        btnStop.setEnabled((state.getFlags() & DevCalibrationState.DCS_COMPLETED) == 0);
    }
    
    protected void initializeInterface() {
        setLayout(new BorderLayout());
        calibText = new JEditorPane();
        calibText.setEditable(false);
        calibText.setText(I18n.text("Waiting for vehicle..."));
        calibText.setFont(new Font("Arial", Font.BOLD, 16));
        calibText.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        add(calibText, BorderLayout.CENTER);
        bottom = new JPanel(new GridLayout(1,0));
        btnPrev.addActionListener(new ActionListener() {           
            @Override
            public void actionPerformed(ActionEvent e) {
                DevCalibrationControl cmd = new DevCalibrationControl();
                calibratingEntity = EntitiesResolver.resolveId(getMainVehicleId(), entitySelection.getSelectedItem()+""); 
                cmd.setDstEnt(calibratingEntity);
                cmd.setOp(OP.STEP_PREVIOUS);
                send(cmd);
            }
        });
        
        btnNext.addActionListener(new ActionListener() {           
            @Override
            public void actionPerformed(ActionEvent e) {
                DevCalibrationControl cmd = new DevCalibrationControl();
                calibratingEntity = EntitiesResolver.resolveId(getMainVehicleId(), entitySelection.getSelectedItem()+""); 
                cmd.setDstEnt(calibratingEntity);
                cmd.setOp(OP.STEP_NEXT);
                send(cmd);
            }
        });
        
        btnStop.addActionListener(new ActionListener() {           
            @Override
            public void actionPerformed(ActionEvent e) {
                DevCalibrationControl cmd = new DevCalibrationControl();
                calibratingEntity = EntitiesResolver.resolveId(getMainVehicleId(), entitySelection.getSelectedItem()+""); 
                cmd.setDstEnt(calibratingEntity);
                cmd.setOp(OP.STOP);
                send(cmd);
                calibratingEntity = -1;
                calibText.setText(I18n.text("Select entity to be calibrated"));
            }
        });
        
        bottom.add(btnPrev);
        bottom.add(btnStop);
        bottom.add(btnNext);        
        btnPrev.setEnabled(false);
        btnStop.setEnabled(false);
        btnNext.setEnabled(false);
        add(bottom, BorderLayout.SOUTH);
    }
    
    @Override
    public void initSubPanel() {
        
    }

    @Override
    public void cleanSubPanel() {
        
    }
}
