/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by 
 * 8/Mar/2005
 * $Id:: WorkspaceChecker.java 9616 2012-12-30 23:23:22Z pdias            $:
 */
package pt.up.fe.dceg.neptus.loader;

import java.io.File;

import javax.swing.JOptionPane;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.gui.Loader;
import pt.up.fe.dceg.neptus.util.ZipUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

/**
 * @author Paulo
 * 
 */
@Deprecated
public class WorkspaceChecker {
    private static String WORKSPACE_JAR = "workspace.jar";
    private static String WORKSPACE_DIR = ".neptus";

    /**
     * 
     */
    public static boolean checkWorkspace(Loader loader) {
        loader.setText("Checking workspace...");
        ConfigFetch.initialize();
        ConfigFetch cf = ConfigFetch.INSTANCE;

        if (null == ConfigFetch.resolvePath(ConfigFetch.getConfigFile())) {
            String fxSep = System.getProperty("file.separator", "/");
            String workspacePath = System.getProperty("user.home", ".") + fxSep + WORKSPACE_DIR;
            File wsDir = new File(workspacePath).getAbsoluteFile();
            if (!wsDir.exists()) {
                String fxWsPath = ConfigFetch.resolvePath(WORKSPACE_JAR);
                if (fxWsPath == null) {
                    fxWsPath = ConfigFetch.resolvePath("RM" + WORKSPACE_JAR);
                    if (fxWsPath == null) {
                        NeptusLog.pub().error("No workspace jar found!!");

                        JOptionPane.showMessageDialog(loader, "No workspace jar found!!");
                        System.exit(-1);
                    }
                }
                NeptusLog.pub().debug("Workspace jar found in: " + fxWsPath);
                loader.setText("Creating workspace...");
                wsDir.mkdirs();
                ZipUtils.unZip(fxWsPath, wsDir.getAbsolutePath());
                loader.setText("Workspace created.");
                cf.load();
                if (null == ConfigFetch.resolvePath(ConfigFetch.getConfigFile())) {
                    JOptionPane.showMessageDialog(loader, "The problem loading configuration still persists!\n"
                            + "Check the workspace for the configuration file.");
                    // System.exit(-1);
                }
            }
            else {
                loader.setText("Workspace already exists.");
            }
        }
        else {
            loader.setText("Workspace found.");
            // loader.set
            Object[] options = { "Yes", "No" };
            int option = JOptionPane.showOptionDialog(loader, "Click Yes to overwrite workspace", "Warning",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
            System.out.println(">>>>>>>> " + option);
            if (option == 0) {
                String fxSep = System.getProperty("file.separator", "/");
                String workspacePath = System.getProperty("user.home", ".") + fxSep + WORKSPACE_DIR;
                File wsDir = new File(workspacePath).getAbsoluteFile();
                // if (!wsDir.exists())
                // {
                String fxWsPath = ConfigFetch.resolvePath(WORKSPACE_JAR);
                if (fxWsPath == null) {
                    fxWsPath = ConfigFetch.resolvePath("RM" + WORKSPACE_JAR);
                    if (fxWsPath == null) {
                        NeptusLog.pub().error("No workspace jar found!!");
                        JOptionPane.showMessageDialog(loader, "No workspace jar found!!");
                        System.exit(-1);
                    }
                }
                NeptusLog.pub().debug("Workspace jar found in: " + fxWsPath);
                loader.setText("Re-creating workspace...");
                wsDir.mkdirs();
                ZipUtils.unZip(fxWsPath, wsDir.getAbsolutePath());
                loader.setText("Workspace re-created.");
                JOptionPane.showMessageDialog(loader, "Workspace re-created ok!!");
                cf.load();
                // }
            }
        }
        return true;
    }

}
