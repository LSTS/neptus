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
 * Author: Paulo Dias
 * 17 de Dez de 2012
 */
package pt.lsts.neptus.console.actions;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.MonitorIMCComms;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;

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
