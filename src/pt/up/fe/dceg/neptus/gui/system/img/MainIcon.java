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
 * 24/06/2011
 */
package pt.up.fe.dceg.neptus.gui.system.img;

import javax.swing.JButton;

import pt.up.fe.dceg.neptus.util.GuiUtils;

/**
 * @author pdias
 *
 */
public class MainIcon extends SelectionIcon {
    {
        isSelectionOrMain = false;
    }
    public MainIcon (int diameter, int margin) {
        super(diameter, margin);
    }

    public MainIcon (int diameter) {
        super(diameter);
    }
    
    public static void main(String[] args) {
        MainIcon icon = new MainIcon(100);
        JButton but = new JButton(icon);
        
        GuiUtils.testFrame(but);
    }
}
