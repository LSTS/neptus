/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by Hugo
 * Oct 16, 2012
 * $Id:: ConsoleAction.java 9615 2012-12-30 23:08:28Z pdias                     $:
 */
package pt.up.fe.dceg.neptus.console.actions;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.KeyStroke;

/**
 * @author Hugo
 * 
 */
public abstract class ConsoleAction extends AbstractAction {

    private static final long serialVersionUID = 9126286288718307533L;

    public ConsoleAction(){
    }
    
    public ConsoleAction(String text) {
        super(text);
    }

    public ConsoleAction(String text, ImageIcon icon) {
        super(text, icon);
    }

    public ConsoleAction(String text, ImageIcon icon, String desc) {
        super(text, icon);
        putValue(Action.SHORT_DESCRIPTION, desc);
    }

    public ConsoleAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
        super(text, icon);
        putValue(Action.SHORT_DESCRIPTION, desc);
        putValue(Action.MNEMONIC_KEY, mnemonic);
    }

    public ConsoleAction(String text, ImageIcon icon, String desc, Integer mnemonic, KeyStroke accelerator) {
        super(text, icon);
        putValue(Action.SHORT_DESCRIPTION, desc);
        putValue(Action.MNEMONIC_KEY, mnemonic);
        putValue(Action.ACCELERATOR_KEY, accelerator);
    }

    public ConsoleAction(String text, ImageIcon icon, String desc, KeyStroke accelerator) {
        super(text, icon);
        putValue(Action.SHORT_DESCRIPTION, desc);
        putValue(Action.ACCELERATOR_KEY, accelerator);
    }

    public ConsoleAction(String text, ImageIcon icon, KeyStroke accelerator) {
        super(text, icon);
        putValue(Action.ACCELERATOR_KEY, accelerator);
    }

}
