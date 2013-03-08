/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by pdias
 * 17 de Dez de 2012
 * $Id:: OpenImcMonitorAction.java 9615 2012-12-30 23:08:28Z pdias              $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.ImcMsgManager;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.MonitorIMCComms;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class OpenImcMonitorAction extends ConsoleAction {

    private ConsoleLayout console = null;
    private JFrame imcMonitorFrame = null;
    
    public OpenImcMonitorAction(ConsoleLayout console) {
        super(I18n.text("IMC Monitor"), ImageUtils.createScaleImageIcon("images/imc.png", 16, 16), I18n
                .text("IMC Monitor"));
        this.console = console;
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (imcMonitorFrame == null) {
            final MonitorIMCComms imcPanel = new MonitorIMCComms(ImcMsgManager.getManager());
            imcMonitorFrame = new JFrame(I18n.text("IMC Monitor"));
            imcMonitorFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    imcPanel.cleanup();
                    console.removeWindowToOppenedList(imcMonitorFrame);
                    imcMonitorFrame = null;
                    super.windowClosing(e);
                }
            });
            imcMonitorFrame.setSize(new Dimension(imcPanel.getWidth() + 220, imcPanel.getHeight() + 220));
            imcMonitorFrame.setResizable(true);
            imcMonitorFrame.add(imcPanel);
            imcMonitorFrame.setIconImages(ConfigFetch.getIconImagesForFrames());
            imcMonitorFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            console.addWindowToOppenedList(imcMonitorFrame);
        }

        imcMonitorFrame.setVisible(true);
        imcMonitorFrame.requestFocus();
    }
}
