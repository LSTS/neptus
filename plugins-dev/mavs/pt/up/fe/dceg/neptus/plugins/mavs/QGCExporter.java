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
 * Apr 15, 2011
 */
package pt.up.fe.dceg.neptus.plugins.mavs;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JFileChooser;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.plugins.PluginDescription;
import pt.up.fe.dceg.neptus.plugins.PluginUtils;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;

/**
 * @author zp
 *
 */
@PluginDescription(name="QGroundControl interface")
public class QGCExporter extends SimpleSubPanel {

    /**
     * @param console
     */
    public QGCExporter(ConsoleLayout console) {
        super(console);
    }

    private static final long serialVersionUID = 1L;

    @Override
    public void initSubPanel() {
        addMenuItem("Tools>QGC>Export Waypoint List", ImageUtils.getIcon(PluginUtils.getPluginIcon(getClass())), new ActionListener() {            
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    PlanType plan = getConsole().getPlan();
                    if (plan == null)
                        throw new Exception("Please choose a main plan first");
                    String wptList = WaypointUtils.getAsQGCFormat(plan);
                    JFileChooser fchooser = new JFileChooser();
                    fchooser.setAcceptAllFileFilterUsed(true);
                    fchooser.setMultiSelectionEnabled(false);
                    int option = fchooser.showSaveDialog(getConsole());
                    if (option != JFileChooser.APPROVE_OPTION)
                        return;
                    File f = fchooser.getSelectedFile();
                    FileUtil.saveToFile(f.getAbsolutePath(), wptList);
                    GuiUtils.infoMessage(getConsole(), "Export Waypoint List", "Waypoint list successfully exported to '"+f.getName()+"'");
                }
                catch (Exception ex) {
                    GuiUtils.errorMessage(getConsole(), ex);
                }
                
            }
        });
    }

    /* (non-Javadoc)
     * @see pt.up.fe.dceg.neptus.plugins.SimpleSubPanel#cleanSubPanel()
     */
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub
        
    }
}
