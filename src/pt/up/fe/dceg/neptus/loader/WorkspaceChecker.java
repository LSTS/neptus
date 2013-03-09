/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
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
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENCE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: 
 * 8/Mar/2005
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
