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
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;

import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mra.NeptusMRA;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author pdias
 *
 */
@SuppressWarnings("serial")
public class OpenMRAAction extends ConsoleAction {

    public OpenMRAAction() {
        super(I18n.text("MRA"), ImageUtils.createScaleImageIcon("images/mra-alt.png", 16, 16), I18n
                .text("Mission Review & Analysis"));
    }
    
    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        NeptusMRA.showApplication().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    }
}
