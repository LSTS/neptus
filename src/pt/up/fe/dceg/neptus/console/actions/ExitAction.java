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
 * Oct 4, 2012
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.Event;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;

import javax.swing.KeyStroke;

import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Hugo
 *
 */
/**
 * This class will create and dispatch a WINDOW_CLOSING event to the active frame. As a result a request to close the
 * frame will be made and any WindowListener that handles the windowClosing event will be executed. Since clicking on
 * the "Close" button of the frame or selecting the "Close" option from the system menu also invoke the WindowListener,
 * this will provide a common exit point for the application.
 */
public class ExitAction extends ConsoleAction {

    private static final long serialVersionUID = -6186014894869502822L;

    public ExitAction() {
        super(I18n.text("Exit"), ImageUtils.createImageIcon("images/menus/exit.png"), I18n.text("Exit Window"),
                KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK, true));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Find the active frame before creating and dispatching the event

        for (Frame frame : Frame.getFrames()) {
            if (frame.isActive()) {
                WindowEvent windowClosing = new WindowEvent(frame, WindowEvent.WINDOW_CLOSING);
                frame.dispatchEvent(windowClosing);
            }
        }
    }
}