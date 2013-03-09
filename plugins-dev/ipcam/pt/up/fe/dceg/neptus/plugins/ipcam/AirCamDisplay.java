/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
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
 * Author: jfortuna
 * Dec 5, 2012
 */
package pt.up.fe.dceg.neptus.plugins.ipcam;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.JOptionPane;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.actions.SimpleMenuAction;

/**
 * @author jfortuna
 *
 */
public class AirCamDisplay extends SimpleMenuAction {

    @NeptusProperty(name="Hostname")
    public String host = "10.0.20.19";
    /**
     * @param console
     */
    public AirCamDisplay(ConsoleLayout console) {
        super(console);
    }

    
    @Override
    public String getMenuName() {
        return I18n.text("Tools")+">"+I18n.text("Connect IP Camera");
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
       String option = JOptionPane.showInputDialog(getConsole(), I18n.text("Camera IP"), host);
       
       if (option == null)
           return;
       host = option;
       
       try {
           Runtime.getRuntime().exec("ffplay rtsp://" + host + ":554/live/ch01_0");
       }
       catch (IOException e1) {
           e1.printStackTrace();
       }
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
