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
 * Author: José Pinto
 * Mar 27, 2012
 */
package pt.lsts.neptus.plugins.help;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.ConsolePanel;
import pt.lsts.neptus.doc.DocumentationPanel;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.plugins.PluginDescription;
import pt.lsts.neptus.plugins.SimpleMenuAction;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "ZP", description = "Shows context-specific help", name = "Context help", icon = "pt/lsts/neptus/plugins/help/help.png")
public class ContextHelperPlugin extends SimpleMenuAction {

    private static final long serialVersionUID = 1L;
    protected Component oldGlassPane;
    protected JPanel glassPane = new JPanel();

    /**
     * @param console
     */
    public ContextHelperPlugin(ConsoleLayout console) {
        super(console);
    }

    @Override
    public String getMenuName() {
        return I18n.text("Help") + ">" + I18n.text("Context Help");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        oldGlassPane = getConsole().getGlassPane();
        glassPane.setOpaque(false);
        getConsole().setGlassPane(glassPane);

        glassPane.setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));

        glassPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);

                Component c = getConsole().getMainPanel().findComponentAt(e.getPoint());

                while (c != null && !(c instanceof ConsolePanel) && c != c.getParent()) {
                    c = c.getParent();
                }

                glassPane.removeMouseListener(this);
                MouseMotionListener[] ms = glassPane.getMouseMotionListeners();
                for (MouseMotionListener m : ms)
                    glassPane.removeMouseMotionListener(m);

                getConsole().setGlassPane(oldGlassPane);

                if (c != null)
                    DocumentationPanel.showDocumentation(c.getClass());
                else
                    DocumentationPanel.showDocumentation("start.html");
            }
        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
