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

import java.awt.event.ActionEvent;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.doc.DocumentationPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author Paulo Dias
 * 
 */
@SuppressWarnings("serial")
public class ManualAction extends ConsoleAction {

    protected ConsoleLayout console;

    public ManualAction(ConsoleLayout console) {
        super(I18n.text("Manual"), ImageUtils.createImageIcon("images/menus/info.png"), I18n.text("Manual"));
        this.console = console;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DocumentationPanel.showDocumentation("start.html");
    }
}
