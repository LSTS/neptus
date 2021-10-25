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
 * Author: José Pinto and pdias
 * 200?/??/??
 */
package pt.lsts.neptus.loader;

import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.Window.Type;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.fileeditor.RMFEditor;
import pt.lsts.neptus.gui.Loader;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.mc.Workspace;
import pt.lsts.neptus.mc.lauvconsole.LAUVConsole;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.params.ConfigurationManager;
import pt.lsts.neptus.plugins.PluginsLoader;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.ConsoleParse;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.editors.EditorLauncher;
import pt.lsts.neptus.util.output.OutputMonitor;

/**
 * This class launches the application received as an argument from the command line
 * 
 * @author ZP       
 * @author Paulo Dias
 */
public class NeptusMain {

    private static final LinkedHashMap<String, String> appNames = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Class<?>> fileHandlers = new LinkedHashMap<>();
    private static final List<Window> openAppWindows = new ArrayList<>();
    private static Loader loader;

    private static void init() {
        GeneralPreferences.initialize();

        // appNames.put("ws", I18n.text("Workspace"));
        appNames.put("auv", I18n.text("LAUV Console"));
        appNames.put("mra", I18n.text("Mission Review & Analysis"));
        appNames.put("la", I18n.text("LAUV SE Console"));
        appNames.put("uav", I18n.text("UAV Console"));
        appNames.put("cl", I18n.text("Empty Console"));

        // fileHandlers.put(FileUtil.FILE_TYPE_MISSION, Workspace.class);
        // fileHandlers.put(FileUtil.FILE_TYPE_MISSION_COMPRESSED, Workspace.class);
        fileHandlers.put(FileUtil.FILE_TYPE_CONFIG, EditorLauncher.class);
        fileHandlers.put(FileUtil.FILE_TYPE_CONSOLE, ConsoleParse.class);
        // fileHandlers.put(FileUtil.FILE_TYPE_VEHICLE, Workspace.class);
        fileHandlers.put(FileUtil.FILE_TYPE_CHECKLIST, Workspace.class);
        fileHandlers.put(FileUtil.FILE_TYPE_INI, EditorLauncher.class);
        fileHandlers.put(FileUtil.FILE_TYPE_RMF, RMFEditor.class);
        fileHandlers.put(FileUtil.FILE_TYPE_XML, EditorLauncher.class);

        fileHandlers.put(FileUtil.FILE_TYPE_LSF, NeptusMRA.class);
        fileHandlers.put(FileUtil.FILE_TYPE_LSF_COMPRESSED, NeptusMRA.class);
        fileHandlers.put(FileUtil.FILE_TYPE_LSF_COMPRESSED_BZIP2, NeptusMRA.class);
    }

    /**
     * @param appargs The commandline arguments for the program
     */
    public static void launch(String[] appargs) {
        ConfigFetch.initialize(); // Don't touch this, leave it as it his
        if (appNames.isEmpty())
            init();
        
        launch(new Loader(), appargs);
    }

    /**
     * @param loader A {@link Loader} to use on the opening of the program
     * @param appargs The commandline arguments for the program
     */
    public static void launch(Loader loader, String[] appargs) {
        ConfigFetch.initialize(); // Don't touch this, leave it as it his
        // benchmark
        long start = System.currentTimeMillis();
        NeptusMain.loader =  loader;
        
        if (appNames.isEmpty()) {
            init();
        }
        
        String app = appargs[0];
        loader.start();
        ConfigFetch.setSuperParentFrameForced(loader);

        loadPreRequirementsDataExceptConfigFetch(loader, true);

        // When loading one can type the application to start
        String typ = loader.getTypedString();
        if (!typ.equalsIgnoreCase("")) {
            if (typ.startsWith(" ") || typ.startsWith("" + ((char) KeyEvent.VK_ESCAPE))) {
                loader.setText(I18n.text("Choose Application..."));
                LinkedHashMap<String, String> chooseApp = new LinkedHashMap<>();
                for (String key : appNames.keySet()) {
                    String name = appNames.get(key);
                    chooseApp.put(name + " (" + key + ")", key);
                }
                String cApp = (String) JOptionPane.showInputDialog(loader,
                        I18n.text("Choose one of the available applications"), I18n.text("Select application"),
                        JOptionPane.QUESTION_MESSAGE, new ImageIcon(ImageUtils.getImage("images/neptus-icon.png")),
                        chooseApp.keySet().toArray(new String[] {}), chooseApp.keySet().iterator().next());
                if (cApp != null)
                    typ = chooseApp.get(cApp);
            }
            String appT = appNames.get(typ);
            if (appT != null)
                app = typ;
            else
                app = "auv";
        }
        else if (app.equalsIgnoreCase("-f") && appargs.length >= 2) {
            loader.setText(I18n.text("Opening file..."));
            handleFile(appargs[1]);
            loader.waitMoreAndEnd(1000);
            return;
        }

        String appName = appNames.get(app);
        loader.setText(I18n.textf("Starting %appname...", appName != null ? appName : ""));

        // Workspace 
        if (app.equalsIgnoreCase("ws") || app.equalsIgnoreCase("mc")) {
            Workspace ws = new Workspace();
            ws.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            wrapMainApplicationWindowWithCloseActionWindowAdapter(ws);
            NeptusLog.pub().info("workspace load finished in " + ((System.currentTimeMillis() - start) / 1E3) + "s ");
        }
        // MRA 
        else if (app.equalsIgnoreCase("mra")) {
            NeptusMRA mra = NeptusMRA.showApplication();
            wrapMainApplicationWindowWithCloseActionWindowAdapter(mra);
            
            if (appargs.length > 1) {
                mra.openLog(appargs[1]);
            }
        }
        // Empty Console
        else if (app.equalsIgnoreCase("cl")) {
            ConfigFetch.initialize();
            ConsoleLayout appC = ConsoleLayout.forge();
            // appC.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            appC.setVisible(true);
            wrapMainApplicationWindowWithCloseActionWindowAdapter(appC);
        }
        // LAUV Console
        else if (app.equalsIgnoreCase("auv")) {
            ConfigFetch.initialize();
            ConsoleLayout appC = ConsoleLayout.forge("conf/consoles/lauv.ncon", loader);
            // appC.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            appC.setVisible(true);
            wrapMainApplicationWindowWithCloseActionWindowAdapter(appC);
        }
        // LAUV Navy Console
        else if (app.equalsIgnoreCase("la")) {
            try {
                LAUVConsole.setLoader(loader);
                final ConsoleLayout cls = LAUVConsole.create(new String[0]);
                wrapMainApplicationWindowWithCloseActionWindowAdapter(Objects.requireNonNull(cls));
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        // UAV Console
        else if (app.equalsIgnoreCase("uav")) {
            ConfigFetch.initialize();
            ConsoleLayout appC = ConsoleLayout.forge("conf/consoles/uav-light.ncon", loader);
            // appC.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            appC.setVisible(true);
            wrapMainApplicationWindowWithCloseActionWindowAdapter(appC);
        }
        // File loading
        else {
            ConfigFetch.initialize();
            loader.setText(I18n.text("Opening file..."));
            handleFile(appargs[0]);
            loader.end();
            return;
        }
        loader.setText(I18n.text("Application started"));
        loader.end();
    }

    /**
     * @param loader A {@link Loader} to use on the opening of the program
     */
    public static void loadPreRequirementsDataExceptConfigFetch(Loader loader) {
        loadPreRequirementsDataExceptConfigFetch(loader, true);
    }

    /**
     * @param loader A {@link Loader} to use on the opening of the program
     * @param neptusLookAndFeelOrNative To indicate with look and feel to use (true for Neptus, false for Java native)
     */
    public static void loadPreRequirementsDataExceptConfigFetch(Loader loader, boolean neptusLookAndFeelOrNative) {
        loader.setText(I18n.text("Loading Plug-ins..."));
        PluginsLoader.load();

        loader.setText(I18n.text("Loading Look&Feel..."));

        if (!neptusLookAndFeelOrNative)
            GuiUtils.setSystemLookAndFeel();
        else
            GuiUtils.setLookAndFeel();

        loader.setText(I18n.text("Loading Systems") + "...");

        if (!VehiclesHolder.loadVehicles()) {
            GuiUtils.errorMessage(loader, I18n.text("Loading Systems"), I18n.text("Error loading systems!"));
        }

        loader.setText(I18n.text("Loading Systems Parameters Files..."));

        Thread bg = new Thread("System parameters files loader") {
            @Override
            public void run() {                
                ConfigurationManager.getInstance();                
            }
        };
        bg.setDaemon(true);
        bg.start();
    }

    /**
     * @param callingWindow The {@link Window} to add the {@link Window#addWindowListener(WindowListener)}
     */
    public static void wrapMainApplicationWindowWithCloseActionWindowAdapter(final Window callingWindow) {
        if (openAppWindows.contains(callingWindow)) {
            return;
        }

        openAppWindows.add(callingWindow);

        WindowAdapter wa = new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                openAppWindows.remove(callingWindow);
                if (!openAppWindows.isEmpty()) {
                    return;
                }

                Window[] openedWindows = Frame.getWindows();
                for (Window wdow : openedWindows) {
                    if (callingWindow == wdow)
                        continue;
                    if (wdow.getType() != Type.NORMAL)
                        continue;
                    if (wdow.isVisible()) {
                        WindowEvent wev = new WindowEvent(wdow, WindowEvent.WINDOW_CLOSING);
                        Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(wev);
                    }
                }

                Thread t = new Thread("Neptus Shutdown") {
                    @Override
                    public void run() {
                        try { Thread.sleep(10000); } catch (InterruptedException e1) { }
                        System.out.println("Force close !");
                        System.exit(0);
                    }
                };
                t.setDaemon(true);
                t.start();
                OutputMonitor.end();
            }
        };
        callingWindow.addWindowListener(wa);
    }

    private static void handleFile(String filename) {
        // verify if file exists...
        File f = new File(filename);
        if (!f.canRead()) {
            GuiUtils.errorMessage(loader, I18n.text("Error opening file"),
                    I18n.textf("Unable to read the file '%filename'", filename));
            System.exit(1);
        }
        else {
            try {
                filename = f.getCanonicalPath();
            }
            catch (IOException e1) {
                NeptusLog.pub().debug(e1);
            }

            String extension = FileUtil.getFileExtension(f).toLowerCase();
            // Lets us try to see compound extensions like 'lsf.gz'
            String preExtension = FileUtil.getFileExtension(FileUtil.getFileNameWithoutExtension(f).toLowerCase());
            if (!preExtension.isEmpty()) {
                extension = preExtension + "." + extension;
            }

            if (fileHandlers.containsKey(extension)) {
                try {
                    FileHandler fh = ((FileHandler) fileHandlers.get(extension).getDeclaredConstructor().newInstance());
                    loader.setText(I18n.textf("Starting %program %arg...", fh.getName(),f.getName()));
                    if (fh instanceof JFrame) {
                        if (!(fh instanceof ConsoleLayout)) {
                            ((JFrame) fh).setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        }
                        wrapMainApplicationWindowWithCloseActionWindowAdapter((JFrame) fh);
                    }
                    Window window = fh.handleFile(f);
                    if (window != null) {
                        wrapMainApplicationWindowWithCloseActionWindowAdapter(window);
                    }
                }
                catch (Exception e) {
                    GuiUtils.errorMessage(loader, e);
                    System.exit(1);
                }
            }
            else {
                GuiUtils.errorMessage(loader, I18n.text("Error opening file"),
                        I18n.textf("File '%filename' of type '.%extension' is not supported.", filename, extension));
                System.exit(1);
            }
        }
    }

    /**
     * @param args The first argument decides which application to launch - defaults to Mission Console
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            launch(new Loader(), new String[] { "auv" });
        }
        else {
            launch(new Loader(), args);
        }
    }
}
