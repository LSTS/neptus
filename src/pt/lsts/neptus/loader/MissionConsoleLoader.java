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
 */
package pt.lsts.neptus.loader;

import pt.lsts.neptus.gui.Loader;
import pt.lsts.neptus.mc.Workspace;
import pt.lsts.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo Dias
 */
@Deprecated
public class MissionConsoleLoader {

    public void run() {
        Loader loader = new Loader();
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
        @SuppressWarnings("unused")
        Workspace console = new Workspace();
        loader.end();
    }

    public static void main(String[] args) {
        if (args.length == 1 && args[0].startsWith("config=")) {
            @SuppressWarnings("unused")
            String configFile = args[0].substring(7);
        }
        new MissionConsoleLoader().run();
    }

}
