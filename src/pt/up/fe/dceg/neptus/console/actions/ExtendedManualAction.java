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
 * Nov 10, 2012
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.notifications.Notification;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 * 
 */
@SuppressWarnings("serial")
public class ExtendedManualAction extends ConsoleAction {

    protected ConsoleLayout console;

    public ExtendedManualAction(ConsoleLayout console) {
        super(I18n.text("Extended Manual"), ImageUtils.createImageIcon("images/menus/info.png"), I18n.text("Extended Manual"));
        this.console = console;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            Desktop.getDesktop().browse(new File("doc/seacon/manual-seacon.html").toURI());
        }
        catch (IOException e1) {
            e1.printStackTrace();
            GuiUtils.errorMessage(I18n.text("Error opening Extended Manual"), e1.getMessage());
            console.post(Notification.error(I18n.text("Extended Manual"), 
                    I18n.textf("Error opening Extended Manual (%error)", e1.getMessage())));
        }
    }
}
