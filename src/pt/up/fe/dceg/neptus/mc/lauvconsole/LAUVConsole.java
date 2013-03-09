/*
 * Copyright (c) 2004-2013 Laboratório de Sistemas e Tecnologia Subaquática and Authors
 * All rights reserved.
 * Faculdade de Engenharia da Universidade do Porto
 * Departamento de Engenharia Electrotécnica e de Computadores
 * Rua Dr. Roberto Frias s/n, 4200-465 Porto, Portugal
 *
 * For more information please see <http://whale.fe.up.pt/neptus>.
 *
 * Created by zepinto and pdias
 * 2007/08/23
 */
package pt.up.fe.dceg.neptus.mc.lauvconsole;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;

import javax.swing.JFrame;

import org.dom4j.DocumentException;
import org.dom4j.Node;

import pt.up.fe.dceg.neptus.NeptusLog;
import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.actions.ExtendedManualAction;
import pt.up.fe.dceg.neptus.console.actions.LayoutEditConsoleAction;
import pt.up.fe.dceg.neptus.console.actions.ManualAction;
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
    public static String lauvVehicle = "lauv-seacon-1";
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
        removeJMenuAction(ManualAction.class);
        removeJMenuAction(ExtendedManualAction.class);
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

        final LAUVConsole cls = new LAUVConsole();

        // ConsoleParse.loadConsole(cls, ConfigFetch.resolvePath(consoleURL));
        try {
            ConsoleParse.parseFile(ConfigFetch.resolvePath(consoleURL), cls);
            cls.setConsoleChanged(false);
        }
        catch (DocumentException e2) {
            NeptusLog.pub().error(e2);
        }
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
        // loader.start();
        //
        // ConfigFetch.setSuperParentFrameForced(loader);
        //
        // loader.setText(I18n.text("LOADING_CONFIG", "Loading Configuration..."));
        // ConfigFetch.initialize();
        // ConfigFetch.setOnLockedMode(true);
        //
        // loader.setText(I18n.text("LOADING_PLUGINS", "Loading Plug-ins..."));
        // PluginClassLoader.install();
        //
        // loader.setText(I18n.text("LOADING_LNF", "Loading Look&Feel..."));
        // boolean nlf = false;
        // for (int i = 0; i < args.length; i++) {
        // if (args[i].equals("-nlf"))
        // nlf = true;
        // }
        //
        // UIDefaults defaults = UIManager.getDefaults();
        // defaults.put("Label.foreground", Color.black);
        // defaults.put("Label.disabledForeground", new Color(51,51,51));
        // defaults.put("TextArea.foreground", Color.black);
        // defaults.put("TextArea.inactiveForeground", new Color(51,51,51));
        //
        // UIManager.put("OptionPane.yesButtonText", I18n.text("YES","Yes"));
        // UIManager.put("OptionPane.noButtonText", I18n.text("NO","No"));
        // UIManager.put("OptionPane.cancelButtonText", I18n.text("CANCEL","Cancel"));
        // UIManager.put("OptionPane.titleText", I18n.text("OPTION_TITLE","Select an Option"));
        //
        //
        // if (nlf)
        // GuiUtils.setSystemLookAndFeel();
        // else
        // GuiUtils.setLookAndFeel();
        //
        // loader.setText(I18n.text("LOADING_SYSTEMS", "Loading Systems"));
        // boolean retV = VehiclesHolder.loadVehicles();
        // if (!retV) {
        // GuiUtils.errorMessage(loader, I18n.text("LOADING_SYSTEMS", "Loading systems"),
        // I18n.text("LOADING_SYSTEMS_ERROR", "Error loading systems!"));
        // System.exit(-1);
        // }
        //
        // retV = MiscSystemsHolder.loadMiscSystems();
        // if (!retV) {
        // GuiUtils.errorMessage(loader, I18n.text("LOADING_MISC_SYSTEMS", "Loading misc systems"),
        // I18n.text("LOADING_MISC_SYSTEMS_ERROR", "Error loading misc systems!"));
        // }
        //
        // if (args.length == 1 && args[0].trim().equalsIgnoreCase("mra")) {
        // NeptusMRA.showApplication().addWindowListener(new WindowAdapter() {
        // public void windowClosed(WindowEvent e) {
        // super.windowClosed(e);
        // OutputMonitor.end();
        // System.exit(0);
        // }
        // });
        // loader.waitMoreAndEnd(500);
        // }
        // else {
        // create(args);
        //
        // loader.waitMoreAndEnd(500);
        //
        // try {
        // ServerLoader.main(new String[0]);
        // }
        // catch (Exception e) {
        // NeptusLog.pub().warn(ReflectionUtil.getCallerStamp()
        // + "S57 Map Server error while starting", e);
        // }
        // catch (Error e) {
        // NeptusLog.pub().warn(ReflectionUtil.getCallerStamp()
        // + "S57 Map Server not available");
        // }
        // }

        if (args.length == 1 && args[0].trim().equalsIgnoreCase("mra"))
            NeptusMain.launch(loader, args);
        else
            NeptusMain.launch(loader, new String[] { "la" });
    }
}
