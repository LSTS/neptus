/*
 * Copyright 2005 (C) FEUP. All Rights Reserved.
 *
 * ====================================================================
 * Name: MissionPlanerLoader
 * Implementation-Name: Neptus
 * Specification-Vendor: LSTS (http://www.fe.up.pt/lsts)
 * Implementation-Vendor: GEDC (http://www.fe.up.pt/dceg)
 * Description: Starts the MissionPlanner application and shows a splash screen
 * while loading its components
 * ====================================================================
 *
 * For more information please see
 * <http://whale.fe.up.pt/neptus>.
 * ====================================================================
 * Created on 2/Mar/2005
 * $Id:: MissionPlannerLoader.java 8648 2012-10-10 13:56:20Z hugodias     $:
 */
package pt.up.fe.dceg.neptus.loader;

import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import pt.up.fe.dceg.neptus.gui.Loader;
import pt.up.fe.dceg.neptus.mp.MissionPlanner;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 */
@Deprecated
public class MissionPlannerLoader {

    public void run() {
        Loader loader = new Loader();
        loader.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        loader.start();
        ConfigFetch.initialize();
        run(loader);
    }

    /**
     * The main procedure of this class: Launches a new MissionPlanner application with an empty workspace (no missions
     * loaded)
     * 
     * @param args The command line arguments are ignored
     */
    public void run(Loader loader) {
        MissionPlanner planner = new MissionPlanner();
        ConfigFetch.setSuperParentFrame(planner);
        GuiUtils.centerOnScreen(planner);
        planner.setExtendedState(JFrame.MAXIMIZED_BOTH);
        planner.setVisible(true);
        planner.addWindowListener(new WindowAdapter() {
            public void windowClosed(WindowEvent e) {
                super.windowClosed(e);
                System.exit(0);
            }
        });
        loader.waitMoreAndEnd(1000);
        loader.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }

    public static void main(String[] args) {
        new MissionPlannerLoader().run();
    }

}
