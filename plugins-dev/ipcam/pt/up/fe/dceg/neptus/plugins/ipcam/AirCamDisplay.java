/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by jfortuna
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
