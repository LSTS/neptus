/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zp
 * Mar 27, 2012
 */
package pt.up.fe.dceg.neptus.plugins.help;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JPanel;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.SubPanel;
import pt.up.fe.dceg.neptus.doc.DocumentationPanel;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.actions.SimpleMenuAction;

/**
 * @author zp
 * 
 */
@PluginDescription(author = "ZP", description = "Shows context-specific help", name = "Context help", icon = "pt/up/fe/dceg/neptus/plugins/help/help.png")
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

                while (c != null && !(c instanceof SubPanel) && c != c.getParent()) {
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
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }
}
