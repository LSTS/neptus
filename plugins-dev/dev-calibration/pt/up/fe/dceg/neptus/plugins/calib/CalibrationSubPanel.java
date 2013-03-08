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
 * Jan 14, 2013
 * $Id:: CalibrationSubPanel.java 9832 2013-02-01 16:47:08Z pdias               $:
 */
package pt.up.fe.dceg.neptus.plugins.calib;

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

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventMainSystemChange;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.DevCalibrationControl;
import pt.up.fe.dceg.neptus.imc.DevCalibrationControl.OP;
import pt.up.fe.dceg.neptus.imc.DevCalibrationState;
import pt.up.fe.dceg.neptus.imc.EntityList;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.EntitiesResolver;

import com.google.common.eventbus.Subscribe;
import com.jogamp.newt.event.KeyEvent;

/**
 * @author zp
 *
 */
@Popup(accelerator=KeyEvent.VK_F10, width=400, height=250)
@PluginDescription(name="Device Calibration")
public class CalibrationSubPanel extends SimpleSubPanel {

    private static final long serialVersionUID = 1L;

    protected JButton btnStart, btnStop = new JButton(I18n.text("Cancel")), btnNext = new JButton(I18n.text("Next step >")), btnPrev = new JButton(I18n.text("< Previous Step"));
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
