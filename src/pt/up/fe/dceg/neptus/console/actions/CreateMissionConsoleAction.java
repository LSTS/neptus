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
 * Oct 17, 2012
 */
package pt.up.fe.dceg.neptus.console.actions;

import java.awt.event.ActionEvent;
import java.io.File;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.gui.LocationPanel;
import pt.up.fe.dceg.neptus.gui.swing.NeptusFileView;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.mystate.MyState;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.HomeReference;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Hugo
 *
 */
@SuppressWarnings("serial")
public class CreateMissionConsoleAction extends ConsoleAction{
    protected ConsoleLayout console;
    
    public CreateMissionConsoleAction(ConsoleLayout console){
        super(I18n.text("Create Mission"), new ImageIcon(ImageUtils.getImage("images/menus/new.png")), KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.Event.CTRL_MASK
                + java.awt.Event.ALT_MASK, true));
        this.console = console;
    }

    /* (non-Javadoc)
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        LocationType lt = new LocationType();
        HomeReference hRef = null;
        if (console.getMission() != null) {
            hRef = console.getMission().getHomeRef();
            if (hRef != null)
                lt.setLocation(hRef);
        }
        lt = LocationPanel.showLocationDialog(console, I18n.text("Set mission home"), lt, null, true);

        if (lt == null)
            return;

        JFileChooser chooser = new JFileChooser(ConfigFetch.getConfigFile());
        chooser.setFileView(new NeptusFileView());
        chooser.setFileFilter(GuiUtils.getCustomFileFilter(I18n.text("Mission Files ('nmisz')"),
                new String[] { "nmisz" }));
        int resp = chooser.showDialog(console, I18n.text("Save"));
        if (resp == JFileChooser.APPROVE_OPTION) {
            if (chooser.getSelectedFile().exists()) {
                resp = JOptionPane.showConfirmDialog(console,
                        I18n.text("Do you want to overwrite the existing file?"),
                        I18n.text("Save Mission As..."), JOptionPane.YES_NO_CANCEL_OPTION);
                if (resp != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            File dst = chooser.getSelectedFile();
            if (!dst.getAbsolutePath().endsWith(".nmisz")) {
                dst = new File(dst.getAbsolutePath() + ".nmisz");
            }
            MissionType tmp = MissionType.createZippedMission(dst);
            tmp.getHomeRef().setLocation(lt);
            console.setMission(tmp);
            console.setPlan(null);
            tmp.save(false);
            HomeReference newHr = tmp.getHomeRef();
            double dist = newHr.getDistanceInMeters(MyState.getLocation());
            if (dist > 1E3)
                MyState.setLocation(newHr);
            console.setConsoleChanged(true);
        }
    }
}
