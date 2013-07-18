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
 * Author: José Pinto and pdias
 * 2007/08/23
 */
package pt.up.fe.dceg.neptus.mc.lauvconsole;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import javax.swing.JFrame;

import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.actions.LayoutEditConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.OpenConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.RunChecklistConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.SaveAsConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.SaveConsoleAction;
import pt.up.fe.dceg.neptus.gui.Loader;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.loader.NeptusMain;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty.DistributionEnum;
import pt.up.fe.dceg.neptus.types.mission.MissionType;
import pt.up.fe.dceg.neptus.types.vehicle.VehicleType;
import pt.up.fe.dceg.neptus.types.vehicle.VehiclesHolder;
import pt.up.fe.dceg.neptus.util.ConsoleParse;
import pt.up.fe.dceg.neptus.util.Dom4JUtil;
import pt.up.fe.dceg.neptus.util.FileUtil;
import pt.up.fe.dceg.neptus.util.GuiUtils;
import pt.up.fe.dceg.neptus.util.conf.ConfigFetch;

@SuppressWarnings("serial")
public class LAUVConsole extends ConsoleLayout {
    public static final int CLOSE_ACTION = JFrame.EXIT_ON_CLOSE;
    private static String consoleURL = "conf/consoles/lauv.ncon";
    // "conf/consoles/seacon-light.ncon" "conf/consoles/seacon-console.ncon";
    public static String lauvVehicle = "lauv-dolphin-1";
    private static Loader loader = null;
    private static boolean editEnabled = false;

    @Override
    public void createMenuBar() {
        super.createMenuBar();

        // Let us remove the unwanted menus
        removeJMenuAction(OpenConsoleAction.class);
        removeJMenuAction(SaveConsoleAction.class);
        removeJMenuAction(SaveAsConsoleAction.class);
        if (!editEnabled)
            removeJMenuAction(LayoutEditConsoleAction.class);
        removeJMenuAction(RunChecklistConsoleAction.class);
        // removeJMenuAction(ManualAction.class);
        // removeJMenuAction(ExtendedManualAction.class);
    }

    @Override
    public void cleanup() {
        if (getMaximizedPanel() != null)
            minimizePanel(getMaximizedPanel());

        try {
            Node n = getXmlDoc().getRootElement().selectSingleNode("@mission-file");
            n.setText(FileUtil.relativizeFilePathAsURI(fileName.getAbsolutePath(), getMission().getMissionFile()
                    .getAbsolutePath()));
            // The Dom4JUtil is needed because when the Doc is already
            // in PrettyPrintFormated extra blank lines appears in the XML text.
            FileUtil.saveToFile(fileName.getAbsolutePath(),
                    FileUtil.getAsPrettyPrintFormatedXMLString(Dom4JUtil.documentToDocumentCleanFormating(getXmlDoc())));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // saveFile();
        super.cleanup();
    }

    public static ConsoleLayout create(String[] args) {

        ConfigFetch.setOnLockedMode(true);
        ConfigFetch.setDistributionType(DistributionEnum.CLIENT);

        // VehicleType vehicle = VehiclesHolder.getVehicleById(lauvVehicle);
        // if (vehicle == null) {
        // GuiUtils.errorMessage(loader, I18n.text("Loading Systems"), I18n.text("Error loading systems!"));
        // return null;
        // }
        loader.setText(I18n.text("Loading console..."));

        NeptusLog.pub().info("Loading " + LAUVConsole.class.getSimpleName() + ".");
        
        final LAUVConsole cls = new LAUVConsole();

        // ConsoleParse.loadConsole(cls, ConfigFetch.resolvePath(consoleURL));
        ConsoleParse.parseFile(ConfigFetch.resolvePath(consoleURL), cls);
        cls.setConsoleChanged(false);

        GuiUtils.leftTopScreen(cls);

        // handle mission file
        if (args.length > 0) {
            String filename = args[0];
            loader.setText(I18n.textf("Opening mission: %missionfile...", filename));
            // verify if file exists...
            File f = new File(filename);
            if (!f.canRead()) {
                GuiUtils.errorMessage(loader, I18n.text("Error opening mission file"),
                        I18n.textf("Unable to read the mission file '%missionfile'", filename));
            }
            else {
                try {
                    filename = f.getCanonicalPath();
                }
                catch (IOException e1) {
                    NeptusLog.pub().error(e1.getStackTrace());
                }
                String extension = FileUtil.getFileExtension(f).toLowerCase();
                if (FileUtil.FILE_TYPE_MISSION_COMPRESSED.equalsIgnoreCase(extension)
                        || FileUtil.FILE_TYPE_MISSION.equalsIgnoreCase(extension)) {
                    MissionType mission = new MissionType(f.getAbsolutePath());
                    if (mission.isLoadOk()) {
                        cls.setMission(mission);
                    }
                }
            }
        }
        // if (!LAUVConsole.editEnabled) {
        cls.getMainPanel().setRelayoutOnResize(true);
        cls.setResizable(true);
        // }
        
        // 20130122 This has to be not called other wise the main panel is not shown
        // try {
        //  cls.maximizePanel((SubPanel) cls.mainPanel.getComponent(0));
        // }
        // catch (Exception e1) {
        //  e1.printStackTrace();
        // }

        String mainSysStr = cls.getMainSystem();
        if (mainSysStr == null) {
            VehicleType vehicle = VehiclesHolder.getVehicleById(lauvVehicle);
            if (vehicle != null) {
                cls.setMainSystem(lauvVehicle);
            }
            else {
                LinkedHashMap<String, VehicleType> val = VehiclesHolder.getVehiclesList();
                VehicleType dVeh = null;
                for(VehicleType veh : val.values()) {
                    if (veh.getName().startsWith("lauv")) {
                        cls.setMainSystem(veh.getName());
                        dVeh = null;
                        break;
                    }
                    if (dVeh == null) {
                        if (veh.isOperationalActive() && "auv".equalsIgnoreCase(veh.getType()))
                            dVeh = veh;
                    }
                }
                if (dVeh != null)
                    cls.setMainSystem(dVeh.getName());
            }
        }
        
        cls.imcOn();
        cls.setVisible(true);
        return cls;
    }

    public static void setLoader(Loader loader) {
        LAUVConsole.loader = loader;
    }

    public static void main(String[] args) {
        ConfigFetch.initialize();
        GuiUtils.setLookAndFeel();

        loader = new Loader("images/neptus_loader_light.png");

        if (args.length == 1 && args[0].trim().equalsIgnoreCase("mra"))
            NeptusMain.launch(loader, args);
        else
            NeptusMain.launch(loader, new String[] { "la" });
    }
}
