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
 * Author: Paulo Dias
 * 2005/03/06
 */
package pt.lsts.neptus.mc;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyVetoException;
import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;

import org.jdesktop.swingx.JXStatusBar;

import pt.lsts.neptus.NeptusLog;
import pt.lsts.neptus.comm.manager.imc.ImcMessageSenderPanel;
import pt.lsts.neptus.comm.manager.imc.ImcMsgManager;
import pt.lsts.neptus.comm.manager.imc.MonitorIMCComms;
import pt.lsts.neptus.console.ConsoleLayout;
import pt.lsts.neptus.console.actions.ExitAction;
import pt.lsts.neptus.events.NeptusEventHiddenMenus;
import pt.lsts.neptus.events.NeptusEvents;
import pt.lsts.neptus.gui.AboutPanel;
import pt.lsts.neptus.gui.ChronometerPanel;
import pt.lsts.neptus.gui.ConsoleFileChooser;
import pt.lsts.neptus.gui.DesktopIcon;
import pt.lsts.neptus.gui.HideMenusListener;
import pt.lsts.neptus.gui.IFrameOpener;
import pt.lsts.neptus.gui.LatLonConv;
import pt.lsts.neptus.gui.MenuScroller;
import pt.lsts.neptus.gui.PropertiesEditor;
import pt.lsts.neptus.gui.StatusLed;
import pt.lsts.neptus.gui.VehicleInfo;
import pt.lsts.neptus.gui.checklist.ChecklistFileChooser;
import pt.lsts.neptus.gui.checklist.ChecklistPanel;
import pt.lsts.neptus.gui.swing.NeptusFileView;
import pt.lsts.neptus.i18n.I18n;
import pt.lsts.neptus.loader.FileHandler;
import pt.lsts.neptus.mra.NeptusMRA;
import pt.lsts.neptus.types.checklist.ChecklistType;
import pt.lsts.neptus.types.vehicle.VehicleType;
import pt.lsts.neptus.types.vehicle.VehiclesHolder;
import pt.lsts.neptus.util.FileUtil;
import pt.lsts.neptus.util.GuiUtils;
import pt.lsts.neptus.util.ImageUtils;
import pt.lsts.neptus.util.RecentlyOpenedFilesUtil;
import pt.lsts.neptus.util.conf.ConfigFetch;
import pt.lsts.neptus.util.conf.GeneralPreferences;
import pt.lsts.neptus.util.editors.EditorLauncher;
import pt.lsts.neptus.util.gui.MouseRecorder;
import pt.lsts.neptus.util.output.OutputMonitor;
import pt.lsts.neptus.util.output.OutputPanel;
import pt.lsts.neptus.viewer3d.Viewer3D;

/**
 * @author Paulo Dias
 */
public class Workspace extends JFrame implements IFrameOpener, FileHandler {

    private static final long serialVersionUID = -6345952229234111612L;

    private final HashMap<String, ImageIcon> vehIconPool = new HashMap<String, ImageIcon>();

    private int numFrames = 0;
    private final HashMap<String, JInternalFrame> internalFrames = new HashMap<String, JInternalFrame>();

    private JFileChooser fileDialog = null;

    private final LinkedHashMap<JMenuItem, File> missionFilesOpened = new LinkedHashMap<JMenuItem, File>();
    private final LinkedHashMap<JMenuItem, File> miscFilesOpened = new LinkedHashMap<JMenuItem, File>();
    private final LinkedHashMap<JMenuItem, File> mapFilesOpened = new LinkedHashMap<JMenuItem, File>();
    private final LinkedHashMap<JMenuItem, File> checkFilesOpened = new LinkedHashMap<JMenuItem, File>();
    private final LinkedHashMap<JMenuItem, File> consoleFilesOpened = new LinkedHashMap<JMenuItem, File>();

    private JMenu recentlyOpenFilesMenu = null;
    private JMenu recentlyOpenMapMenu = null;
    private JMenu recentlyOpenedChecklistMenu = null;
    private JMenu recentlyOpenedConsoleMenu = null;

    public final static String RECENTLY_OPENED_MISSIONS = "conf/romissions.xml";
    public final static String RECENTLY_OPENED_MISC = "conf/romisc.xml";
    public final static String RECENTLY_OPENED_CHECKS = "conf/rocheck.xml";
    public final static String RECENTLY_OPENED_CONSOLES = "conf/roconsoles.xml";

    public final static short MISSIONS_FILES = 0;
    public final static short MISC_FILES = 1;
    public final static short MAPS_FILES = 2;
    public final static short CHECKS_FILES = 3;
    public final static short CONSOLES_FILES = 4;

    private static ImageIcon DISPLAY_ICON = ImageUtils.createImageIcon("images/menus/display.png");
    private static ImageIcon OPEN_ICON = ImageUtils.createImageIcon("images/menus/open.png");

    private static ImageIcon MRA_ICON = ImageUtils.createScaleImageIcon("images/mra-alt.png", 16, 16);
    private static ImageIcon GC_ICON = ImageUtils.createScaleImageIcon("images/buttons/gc.png", 16, 16);
    private static ImageIcon IMC_ICON = ImageUtils.createScaleImageIcon("images/imc.png", 16, 16);
    private static ImageIcon REC_MOUSE_ICON = ImageUtils.createScaleImageIcon("images/menus/recIcon16.png", 16, 16);

    private JPanel jContentPane = null;
    private JMenuBar jJMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu editMenu = null;
    private JMenu helpMenu = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem aboutMenuItem = null;
    private JMenuItem editFileMenuItem = null;
    private JMenu vehiclesMenu = null;
    private JMenu consolesMenu = null;
    private JMenu toolsMenu = null;
    private JMenu commsMenu = null;
    private JMenu reviewMenu = null;
    private JDesktopPane jDesktopPane = null;

    private JMenu checklistsMenu = null;
    private JMenuItem openChecklistMenuItem = null;
    private JMenuItem newChecklistMenuItem = null;
    //    private JMenuItem manualMenuItem = null;
    private JMenuItem viewer3DMenuItem = null;
    private JMenuItem dumpGeneralPropertiesDefaultsMenuItem = null;
    private JMenuItem snapShotMenuItem = null;

    private JXStatusBar statusBar = null;
    private JLabel messageBarLabel = null;
    private StatusLed statusLed = null;
    private JMenuItem openMRAMenuItem = null;
    private JMenuItem editGeneralPreferencesMenuItem = null;
    private JMenuItem callGcMenuItem = null;
    private JMenuItem showConsoleMenuItem = null;
    private JMenuItem latLonConvMenuItem = null;
    private JMenuItem chronometerPanelMenuItem = null;
    private JMenuItem imcMonitorMenuItem = null;

    private final MouseRecorder recorder = new MouseRecorder();
    private JCheckBoxMenuItem recordMouseMenuItem = null;

    private JMenuItem openConsoleMenuItem = null;
    private JMenuItem newConsoleMenuItem = null;

    private JPopupMenu.Separator hiddenMenuCommsSeparator = null;

    public static boolean STARTED = false;

    /**
     * Workspace constructor invisible by default
     * 
     * @throws HeadlessException
     */
    public Workspace() throws HeadlessException {
        super();
        STARTED = true;

        GuiUtils.setLookAndFeel();
        ConfigFetch.setSuperParentFrame(this);
        this.setTitle(I18n.textf("%Neptus Workspace", "Neptus"));
        this.setExtendedState(JFrame.MAXIMIZED_BOTH);
        this.setPreferredSize(new Dimension(800, 600));
        this.setIconImages(ConfigFetch.getIconImagesForFrames());
        this.setJMenuBar(getJJMenuBar());
        this.setContentPane(getJContentPane());

        this.pack();
        this.setLocationRelativeTo(null);

        loadRecentlyOpenedFiles(MISSIONS_FILES);
        loadRecentlyOpenedFiles(MISC_FILES);
        //loadRecentlyOpenedFiles(MAPS_FILES);
        loadRecentlyOpenedFiles(CHECKS_FILES);
        loadRecentlyOpenedFiles(CONSOLES_FILES);
        addDesktopIcons();

        this.initComponentListeners();
        this.setVisible(true);
    }

    @SuppressWarnings("serial")
    private void initComponentListeners() {
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowActivated(WindowEvent e) {
                ConfigFetch.setSuperParentFrameForced(Workspace.this);
            }

            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                JFrame frame = (JFrame) e.getComponent();
                OutputMonitor.end();
                frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

                for(JInternalFrame jif : internalFrames.values()) {
                    try {
                        jif.setClosed(true); // This calls each individual internalFrameClosing() method (see createFrame())
                    }
                    catch (PropertyVetoException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });

        //        this.addKeyListener(new KeyListener() {
        //            @Override
        //            public void keyTyped(KeyEvent e) {
        //            }
        //
        //            @Override
        //            public void keyReleased(KeyEvent e) {
        //            }
        //
        //            @Override
        //            public void keyPressed(KeyEvent e) {
        //                if (e.isShiftDown()) {
        //                    if (e.getKeyCode() == KeyEvent.VK_S) {
        //                        NeptusLog.pub().info("<###>shift S ---");
        //                        NeptusEvents.post(new NeptusEventHiddenMenus());
        //                    }
        //                }
        //            }
        //
        //        });

        JRootPane rootPane = this.getRootPane();
        InputMap globalInputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        // Hidden menus shift + S key binding
        globalInputMap.put(KeyStroke.getKeyStroke("shift S"), "pressed");
        rootPane.getActionMap().put("pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                NeptusEvents.post(new NeptusEventHiddenMenus());
            }
        });

    }

    /**
     * This method initializes jMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getVehiclesMenu() {
        if (vehiclesMenu == null) {
            vehiclesMenu = new JMenu();
            vehiclesMenu.setText(I18n.text("Systems"));
            loadVehiclesMenuItem(vehiclesMenu);
        }
        else {
            vehiclesMenu.removeAll();
            loadVehiclesMenuItem(vehiclesMenu);
        }
        return vehiclesMenu;
    }

    private void loadVehiclesMenuItem(JMenu jMenu) {
        JMenu notOpSys = new JMenu(I18n.text("More").toLowerCase());
        int notOpSysNumber = 0;
        ImageIcon vicon;
        for (VehicleType ve : VehiclesHolder.getVehiclesList().values()) {
            JMenuItem vMItem = new JMenuItem();
            vMItem.setText(ve.getName());
            vMItem.setActionCommand(ve.getId());

            if (vehIconPool.containsKey(ve.getId())) {
                vicon = vehIconPool.get(ve.getId());
            }
            else {
                String imgFile;
                if (!ve.getPresentationImageHref().equalsIgnoreCase(""))
                    imgFile = ve.getPresentationImageHref();
                else
                    imgFile = ve.getSideImageHref();

                Image vimg = new ImageIcon(imgFile).getImage();
                vicon = new ImageIcon(vimg.getScaledInstance(-1, 20, Image.SCALE_SMOOTH));
                // vicon = ImageUtils.createScaleImageIcon(imgFile, 40, -1);
                vehIconPool.put(ve.getId(), vicon);
            }
            vMItem.setIcon(vicon);

            vMItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    VehicleType ve = VehiclesHolder.getVehiclesList().get(e.getActionCommand());
                    openVehicleType(ve);
                }
            });

            if (ve.isOperationalActive()) {
                jMenu.add(vMItem);
            }
            else {
                notOpSys.add(vMItem);
                notOpSysNumber++;
            }
        }
        if (notOpSysNumber > 0) {
            MenuScroller.setScrollerFor(notOpSys, Workspace.this, 150, 0, 0);
            jMenu.add(notOpSys);
        }

        MenuScroller.setScrollerFor(jMenu, Workspace.this, 150, 0, 0);
    }

    /**
     * @param ve
     */
    protected void openVehicleType(VehicleType ve) {
        if (null != ve) {
            JInternalFrame ifrm = internalFrames.get("vehicle-" + ve.getId());
            if (ifrm != null) {
                ifrm.setVisible(true);
                try {
                    ifrm.setSelected(true);
                    ifrm.setIcon(false);
                }
                catch (PropertyVetoException e1) {
                    e1.printStackTrace();
                }
            }
            else {
                VehicleInfo vei = new VehicleInfo(ve);
                vei.setVehicleRefEditable(false);
                ifrm = createFrame(I18n.textf("%vehiclename - Vehicle Info", ve.getId()),
                        I18n.textf("vehicle-%vehicleid", ve.getId()), vei);
                internalFrames.put("vehicle-" + ve.getId(), ifrm);
                ifrm.setSize(new Dimension(vei.getWidth() + 20, vei.getHeight() + 230));
                ifrm.setResizable(false);
                ifrm.setMaximizable(false);
            }
        }
    }

    /**
     * @param fx
     */
    protected void openVehicleTypeFile(File fx) {
        if (fx != null) {
            VehicleType ve = new VehicleType(fx.getAbsolutePath());
            openVehicleType(ve);
        }
    }

    /**
     * This method initializes jMenu1
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getToolsMenu() {
        if (toolsMenu == null) {
            toolsMenu = new JMenu();
            toolsMenu.setText(I18n.text("Tools"));
            JSeparator separator1 = new JSeparator();
            toolsMenu.add(separator1);
            toolsMenu.add(getViewer3DMenuItem());
            toolsMenu.setName("tools menu");
            JSeparator separator2 = new JSeparator();
            toolsMenu.add(separator2);
            toolsMenu.add(getEditFileMenuItem());
            toolsMenu.add(getRecentlyOpenFilesMenu());
            toolsMenu.addSeparator();
            toolsMenu.add(getEditGeneralPreferencesMenuItem());
            toolsMenu.add(getDumpGeneralPropertiesDefaultsMenuItem());
            toolsMenu.addSeparator();
            toolsMenu.add(getSnapShotMenuItem());
            toolsMenu.add(getMouseRecorderMenuItem());
            toolsMenu.addSeparator();
            toolsMenu.add(getCallGcMenuItem());
            toolsMenu.addSeparator();
            toolsMenu.add(getShowConsoleMenuItem());
            toolsMenu.addSeparator();
            toolsMenu.add(getLatLonConvMenuItem());
            toolsMenu.add(getChronometerMenuItem());
            toolsMenu.addMenuListener(HideMenusListener.forge(new Component[] { separator1, separator2 },
                    new JMenuItem[] { getViewer3DMenuItem() }));
        }
        return toolsMenu;
    }

    private JMenu getCommsMenu() {
        if (commsMenu == null) {
            commsMenu = new JMenu();
            commsMenu.setText(I18n.text("Communications"));

            commsMenu.add(getImcMonitorMenuItem());
            JSeparator separator1 = new JSeparator();
            commsMenu.add(separator1);
            commsMenu.add(getHiddenMenuCommsSeparator());
            commsMenu.add(getImcMsgSender());
            //            commsMenu.addMenuListener(HideMenusListener.forge(new Component[] { getHiddenMenuCommsSeparator(),
            //                    separator1 }, new JMenuItem[] { getImcMsgSender() }));
        }
        return commsMenu;
    }

    private JPopupMenu.Separator getHiddenMenuCommsSeparator() {
        if (hiddenMenuCommsSeparator == null)
            hiddenMenuCommsSeparator = new JPopupMenu.Separator();

        return hiddenMenuCommsSeparator;
    }

    JMenuItem imcMsgSenderMenuItem = null;

    private JMenuItem getImcMsgSender() {
        if (imcMsgSenderMenuItem == null) {
            imcMsgSenderMenuItem = new JMenuItem();
            imcMsgSenderMenuItem.setIcon(IMC_ICON);
            imcMsgSenderMenuItem.setText(I18n.text("IMC Msg. Sender."));
            imcMsgSenderMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent arg0) {
                    ImcMessageSenderPanel.getFrame();
                }
            });
        }
        return imcMsgSenderMenuItem;
    }

    /**
     * This method initializes jDesktopPane
     * 
     * @return javax.swing.JDesktopPane
     */
    @SuppressWarnings("serial")
    private JDesktopPane getJDesktopPane() {
        if (jDesktopPane == null) {
            jDesktopPane = new JDesktopPane() {
                @Override
                public Component add(Component comp) {
                    if(getComponentCount() == 0)
                        return super.add(comp);
                    else
                        return super.add(comp, getComponentCount() - 1);
                }
            };
            jDesktopPane.setBackground(new Color(24, 58, 83));

            final ImageIcon icon = new ImageIcon(ImageUtils.getImage("images/lsts.png"));
            final JLabel lbl = new JLabel(icon) {
                @Override
                public void paint(Graphics g) {
                    super.paint(g);
                    String txt = "Neptus " + ConfigFetch.getNeptusVersion();
                    Rectangle2D bounds = g.getFontMetrics().getStringBounds(txt, g);
                    g.drawString(txt, (int) (this.getWidth() - bounds.getWidth() - 3), this.getHeight() - 6);
                }
            };

            lbl.setBounds(jDesktopPane.getWidth() - icon.getIconWidth(),
                    jDesktopPane.getHeight() - icon.getIconHeight(), icon.getIconWidth(), icon.getIconHeight());

            jDesktopPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    lbl.setBounds(e.getComponent().getWidth() - icon.getIconWidth(), e.getComponent().getHeight()
                            - icon.getIconHeight(), icon.getIconWidth(), icon.getIconHeight());
                }
            });

            jDesktopPane.add(lbl);
        }

        return jDesktopPane;
    }

    /**
     * This method initializes jFileChooser
     * 
     * @return javax.swing.JFileChooser
     */
    private JFileChooser getFileDialog() {
        if (fileDialog == null) {
            fileDialog = GuiUtils.getFileChooser((String) null);
        }
        return fileDialog;
    }

    /**
     * This method initializes jMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getRecentlyOpenFilesMenu() {
        if (recentlyOpenFilesMenu == null) {
            recentlyOpenFilesMenu = new JMenu();
            recentlyOpenFilesMenu.setText(I18n.text("Recently opened"));
            recentlyOpenFilesMenu.setIcon(new ImageIcon(this.getClass().getClassLoader()
                    .getResource("images/menus/edit.png")));
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, miscFilesOpened);
        }
        else {
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenFilesMenu, miscFilesOpened);
        }
        return recentlyOpenFilesMenu;
    }

    /**
     * This method initializes jMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getRecentlyOpenMapMenu() {
        if (recentlyOpenMapMenu == null) {
            recentlyOpenMapMenu = new JMenu();
            recentlyOpenMapMenu.setText(I18n.text("Recently opened"));
            recentlyOpenMapMenu.setIcon(new ImageIcon(this.getClass().getClassLoader()
                    .getResource("images/menus/mapeditor.png")));
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenMapMenu, mapFilesOpened);
        }
        else {
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenMapMenu, mapFilesOpened);
        }
        return recentlyOpenMapMenu;
    }

    /**
     * This method initializes checklistMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getChecklistsMenu() {
        if (checklistsMenu == null) {
            checklistsMenu = new JMenu();
            checklistsMenu.setText(I18n.text("Checklists"));
            checklistsMenu.add(getOpenChecklistMenuItem());
            checklistsMenu.add(getNewChecklistMenuItem());
            checklistsMenu.add(getRecentlyOpenedChecklistMenu());
        }
        return checklistsMenu;
    }

    /**
     * This method initializes openChecklistMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getOpenChecklistMenuItem() {
        if (openChecklistMenuItem == null) {
            openChecklistMenuItem = new JMenuItem();
            openChecklistMenuItem.setIcon(OPEN_ICON);
            openChecklistMenuItem.setText(I18n.text("Open"));
            openChecklistMenuItem.addActionListener(getActionOpenChecklist());
        }
        return openChecklistMenuItem;
    }

    private ActionListener getActionOpenChecklist() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    @Override
                    protected Void doInBackground() throws Exception {
                        File fx = ChecklistFileChooser.showOpenDialog(Workspace.this, null);
                        openChecklistTypeFile(fx);
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                        }
                        catch (Exception e) {
                            NeptusLog.pub().error(e);
                        }
                    }
                };
                worker.execute();
            }
        };
    }

    /**
     * This method initializes recentlyOpenedChecklistMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getRecentlyOpenedChecklistMenu() {
        if (recentlyOpenedChecklistMenu == null) {
            recentlyOpenedChecklistMenu = new JMenu();
            recentlyOpenedChecklistMenu.setText(I18n.text("Recently opened"));
            recentlyOpenedChecklistMenu.setIcon(new ImageIcon(this.getClass().getClassLoader()
                    .getResource("images/box_checked.png")));
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenedChecklistMenu, checkFilesOpened);
        }
        else {
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenedChecklistMenu, checkFilesOpened);
        }
        return recentlyOpenedChecklistMenu;
    }

    /**
     * This method initializes newChecklistMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getNewChecklistMenuItem() {
        if (newChecklistMenuItem == null) {
            newChecklistMenuItem = new JMenuItem();
            newChecklistMenuItem.setIcon(new ImageIcon(this.getClass().getClassLoader()
                    .getResource("images/menus/new.png")));
            newChecklistMenuItem.setText(I18n.text("New"));
            newChecklistMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // FIXME with SwingWorker
                    String inputValue = JOptionPane.showInputDialog(Workspace.this,
                            I18n.text("Please input a name for this checklist"));
                    if (inputValue != null && !inputValue.trim().equalsIgnoreCase("")) {
                        ChecklistType checklist = new ChecklistType();
                        checklist.setName(inputValue.trim());
                        openChecklistTypeFile(checklist);
                    }

                }
            });
        }
        return newChecklistMenuItem;
    }

    //    private JMenuItem getManualMenuItem() {
    //        if (manualMenuItem == null) {
    //            manualMenuItem = new JMenuItem();
    //            manualMenuItem.setText(I18n.text("Manual"));
    //            manualMenuItem
    //                    .setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/menus/info.png")));
    //            manualMenuItem.addActionListener(new ActionListener() {
    //                @Override
    //                public void actionPerformed(ActionEvent e) {
    //                    DocumentationPanel.showDocumentation("start.html");
    //                }
    //            });
    //        }
    //        return manualMenuItem;
    //    }

    /**
     * @param type
     */
    private void loadRecentlyOpenedFiles(short type) {
        String recentlyOpenedFiles;
        Method methodUpdate = null;
        //        if (type == MISSIONS_FILES) {
        //            recentlyOpenedFiles = ConfigFetch.resolvePath(RECENTLY_OPENED_MISSIONS);
        //            try {
        //                Class<?>[] params = { File.class };
        //                methodUpdate = this.getClass().getMethod("updateMissionFilesOpened", params);
        //            }
        //            catch (Exception e) {
        //                NeptusLog.pub().error(this + "loadRecentlyOpenedFiles", e);
        //                return;
        //            }
        //        }
        if (type == CHECKS_FILES) {
            recentlyOpenedFiles = ConfigFetch.resolvePath(RECENTLY_OPENED_CHECKS);
            try {
                Class<?>[] params = { File.class };
                methodUpdate = this.getClass().getMethod("updateChecklistFilesOpened", params);
            }
            catch (Exception e) {
                NeptusLog.pub().error(this + "loadRecentlyOpenedFiles", e);
                return;
            }
        }
        else if (type == CONSOLES_FILES) {
            recentlyOpenedFiles = ConfigFetch.resolvePath(RECENTLY_OPENED_CONSOLES);
            try {
                Class<?>[] params = { File.class };
                methodUpdate = this.getClass().getMethod("updateConsoleFilesOpened", params);
            }
            catch (Exception e) {
                NeptusLog.pub().error(this + "loadRecentlyOpenedFiles", e);
                return;
            }
        }
        else {
            recentlyOpenedFiles = ConfigFetch.resolvePath(RECENTLY_OPENED_MISC);
            try {
                Class<?>[] params = { File.class };
                methodUpdate = getClass().getMethod("updateMiscFilesOpened", params);
            }
            catch (Exception e) {
                NeptusLog.pub().error(this + "loadRecentlyOpenedFiles", e);
                return;
            }
        }
        if (recentlyOpenedFiles == null) {
            // JOptionPane.showInternalMessageDialog(this, "Cannot Load")
            return;
        }

        if (!new File(recentlyOpenedFiles).exists())
            return;

        RecentlyOpenedFilesUtil.loadRecentlyOpenedFiles(recentlyOpenedFiles, methodUpdate, this);
    }

    /**
     * @param type
     */
    private void storeRecentlyOpenedFiles(short type) {
        String recentlyOpenedFiles;
        LinkedHashMap<?, ?> hMap;
        String header;
        if (type == MISSIONS_FILES) {
            recentlyOpenedFiles = ConfigFetch.resolvePathBasedOnConfigFile(RECENTLY_OPENED_MISSIONS);
            hMap = missionFilesOpened;
            header = I18n.text("Recently opened mission files.");
        }
        else if (type == CHECKS_FILES) {
            recentlyOpenedFiles = ConfigFetch.resolvePathBasedOnConfigFile(RECENTLY_OPENED_CHECKS);
            hMap = checkFilesOpened;
            header = I18n.text("Recently opened checklist files.");
        }
        else if (type == CONSOLES_FILES) {
            recentlyOpenedFiles = ConfigFetch.resolvePathBasedOnConfigFile(RECENTLY_OPENED_CONSOLES);
            hMap = consoleFilesOpened;
            header = I18n.text("Recently opened consoles files.");
        }
        else {
            recentlyOpenedFiles = ConfigFetch.resolvePathBasedOnConfigFile(RECENTLY_OPENED_MISC);
            hMap = miscFilesOpened;
            header = I18n.text("Recently opened misc files.");
        }
        RecentlyOpenedFilesUtil.storeRecentlyOpenedFiles(recentlyOpenedFiles, hMap, header);
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            jContentPane = new JPanel();
            jContentPane.setLayout(new BorderLayout());
            jContentPane.add(getJDesktopPane(), BorderLayout.CENTER);
            jContentPane.add(getStatusBar(), BorderLayout.SOUTH);
        }
        return jContentPane;
    }

    /**
     * This method initializes jJMenuBar
     * 
     * @return javax.swing.JMenuBar
     */
    private JMenuBar getJJMenuBar() {
        if (jJMenuBar == null) {
            jJMenuBar = new JMenuBar();
            jJMenuBar.add(getFileMenu());
            jJMenuBar.add(getEditMenu());
            jJMenuBar.add(getVehiclesMenu());
            jJMenuBar.add(getConsolesMenu());
            //jJMenuBar.add(getMissionsMenu());
            //jJMenuBar.add(getMapsMenu());
            jJMenuBar.add(getChecklistsMenu());
            jJMenuBar.add(getReviewMenu());
            jJMenuBar.add(getCommsMenu());
            jJMenuBar.add(getToolsMenu());
            jJMenuBar.add(getHelpMenu());
        }
        return jJMenuBar;
    }

    /**
     * This method initializes jMenu
     * 
     * @return JMenu
     */
    private JMenu getFileMenu() {
        if (fileMenu == null) {
            fileMenu = new JMenu(I18n.text("File"));

            exitMenuItem = new JMenuItem();
            exitMenuItem.setAction(new ExitAction());
            fileMenu.add(exitMenuItem);
        }
        return fileMenu;
    }

    /**
     * This method initializes jMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getEditMenu() {
        if (editMenu == null) {
            editMenu = new JMenu();
            editMenu.setText(I18n.text("Edit"));
            editMenu.setVisible(false);
        }
        return editMenu;
    }

    /**
     * This method initializes jMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getHelpMenu() {
        if (helpMenu == null) {
            helpMenu = new JMenu();
            helpMenu.setText(I18n.text("Help"));
            //            helpMenu.add(getManualMenuItem());
            //            helpMenu.add(new AbstractAction(I18n.text("Extended Manual"), new ImageIcon(this.getClass()
            //                    .getClassLoader().getResource("images/menus/info.png"))) {
            //                @Override
            //                public void actionPerformed(ActionEvent e) {
            //                    try {
            //                        Desktop.getDesktop().browse(new File("doc/seacon/manual-seacon.html").toURI());
            //                    }
            //                    catch (IOException e1) {
            //                        e1.printStackTrace();
            //                        GuiUtils.errorMessage(I18n.text("Error opening Extended Manual"), e1.getMessage());
            //                    }
            //                }
            //            });
            helpMenu.add(getAboutMenuItem());
        }
        return helpMenu;
    }

    /**
     * This method initializes jMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getAboutMenuItem() {
        if (aboutMenuItem == null) {
            aboutMenuItem = new JMenuItem();
            aboutMenuItem.setText(I18n.text("About"));
            aboutMenuItem.setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/menus/info.png")));
            aboutMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    AboutPanel ap = new AboutPanel(Workspace.this);
                    ap.setVisible(true);
                }
            });
        }
        return aboutMenuItem;
    }

    /**
     * This method initializes jMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getEditFileMenuItem() {
        if (editFileMenuItem == null) {
            editFileMenuItem = new JMenuItem();
            editFileMenuItem.setText(I18n.text("File editor"));
            editFileMenuItem.setIcon(new ImageIcon(this.getClass().getClassLoader()
                    .getResource("images/menus/edit.png")));
            editFileMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK, true));
            editFileMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // FIXME with SwingWorker ?
                    // NeptusLog.pub().info("<###>actionPerformed()");
                    File fx = openFileAction();
                    openMiscTypeFile(fx);
                }
            });
        }
        return editFileMenuItem;
    }

    /**
     * Creates an internal frame with the given component and title
     * 
     * @param title The title to appear in the frame
     * @param newComponent The component to be shown in the new frame
     */
    @Override
    public JInternalFrame createFrame(String title, String name, final JComponent newComponent) {
        JInternalFrame jif = new JInternalFrame(title, true, true, true, true);
        jif.setName(name);

        if (newComponent instanceof ChecklistPanel) {
            jif.setFrameIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/images/box_checked.png"))));
            jif.setMaximizable(true);
        }
        else if (newComponent instanceof VehicleInfo) {
            VehicleType ve = ((VehicleInfo) newComponent).getVehicleMission().getVehicle();
            String imgFile;
            if (!ve.getPresentationImageHref().equalsIgnoreCase(""))
                imgFile = ve.getPresentationImageHref();
            else
                imgFile = ve.getSideImageHref();
            Image vimg = new ImageIcon(imgFile).getImage();
            ImageIcon vicon = new ImageIcon(vimg.getScaledInstance(22, -1, Image.SCALE_SMOOTH));
            jif.setFrameIcon(vicon);
        }
        else if (newComponent instanceof Viewer3D) {
            jif = new MyIFrame(title, true, true, true, true);
            jif.setName(name);
            jif.setFrameIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/images/menus/3d.png"))));
        }
        else {
            jif.setFrameIcon(new ImageIcon(Toolkit.getDefaultToolkit().getImage(
                    getClass().getResource("/images/neptus-icon.png"))));
        }

        jif.addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(InternalFrameEvent e) {
                JInternalFrame ifrm = (JInternalFrame) e.getSource();
                internalFrames.remove(ifrm.getName());
                ifrm.dispose();
                numFrames--;
                
                if (newComponent instanceof ChronometerPanel) {
                    ((ChronometerPanel) newComponent).stop();
                }

            }
        });

        jif.getContentPane().add(newComponent);
        jDesktopPane.add(jif);
        jif.setSize(400, 340);

        if (newComponent instanceof LatLonConv) {
            jif.setSize(419+35, 224);
        }

        if (newComponent instanceof ChronometerPanel) {
            jif.setSize(400, 204);
        }

        jif.setVisible(true);
        jif.setLocation(180 + numFrames * 20, 5 + numFrames * 20);
        numFrames++;

        return jif;
    }

    /**
     * This will not reset file filters.
     * 
     * @return
     */
    private File openFileActionWorker() {
        fileDialog = getFileDialog();
        fileDialog.setSelectedFile(new File(""));
        fileDialog.setCurrentDirectory(new File(ConfigFetch.getConfigFile()));
        fileDialog.setFileView(new NeptusFileView());
        int result = fileDialog.showOpenDialog(this);
        if (result == JFileChooser.CANCEL_OPTION)
            return null;
        return fileDialog.getSelectedFile();
    }

    /**
     * This will reset file filters to none.
     * 
     * @return
     */
    private File openFileAction() {
        fileDialog = getFileDialog();
        fileDialog.resetChoosableFileFilters();
        return openFileActionWorker();
    }

    /**
     * @param fx
     * @return
     */
    public boolean updateMiscFilesOpened(File fx) {
        // updateFilesOpenedWorker
        RecentlyOpenedFilesUtil.updateFilesOpenedMenuItems(fx, miscFilesOpened, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // FIXME with SwingWorker
                File fx;
                Object key = e.getSource();
                Object value = miscFilesOpened.get(key);
                if (value instanceof File) {
                    fx = (File) value;
                    openMiscTypeFile(fx);
                }
                else
                    return;
            }
        });
        getRecentlyOpenFilesMenu();
        storeRecentlyOpenedFiles(MISC_FILES);
        return true;
    }

    public boolean updateMapFilesOpened(File fx) {
        // updateFilesOpenedWorker
        RecentlyOpenedFilesUtil.updateFilesOpenedMenuItems(fx, mapFilesOpened, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // FIXME with SwingWorker
                File fx;
                Object key = e.getSource();
                Object value = mapFilesOpened.get(key);
                if (value instanceof File) {
                    fx = (File) value;
                    openMapTypeFile(fx);
                }
                else
                    return;
            }
        });
        getRecentlyOpenMapMenu();
        storeRecentlyOpenedFiles(MAPS_FILES);
        return true;
    }

    /**
     * @param fx
     * @return
     */
    public boolean updateChecklistFilesOpened(File fx) {
        RecentlyOpenedFilesUtil.updateFilesOpenedMenuItems(fx, checkFilesOpened, new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                final File fx;
                Object key = e.getSource();
                Object value = checkFilesOpened.get(key);
                if (value instanceof File) {
                    fx = (File) value;
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            openChecklistTypeFile(fx);
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                        }
                    };
                    worker.execute();
                }
                else
                    return;
            }
        });
        getRecentlyOpenedChecklistMenu();
        storeRecentlyOpenedFiles(CHECKS_FILES);
        return true;
    }

    protected void openConsoleTypeFile(File fx) {
        if (fx != null) {
            updateConsoleFilesOpened(fx);
            updateMiscFilesOpened(fx);
            startActivity(I18n.textf("Opening console (%consoleName)...", fx.getName()));
            final File fxfinal = fx;
            @SuppressWarnings("unused")
            ConsoleLayout cl = ConsoleLayout.forge(ConfigFetch.resolvePath(fxfinal.getAbsolutePath()));
        }
    }

    public boolean updateConsoleFilesOpened(File fx) {
        RecentlyOpenedFilesUtil.updateFilesOpenedMenuItems(fx, consoleFilesOpened, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final File fx;
                Object key = e.getSource();
                Object value = consoleFilesOpened.get(key);
                if (value instanceof File) {
                    fx = (File) value;
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            openConsoleTypeFile(fx);
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e, e);
                                GuiUtils.errorMessage(I18n.text("Error"),
                                        I18n.textf("Error openning console '%file'", fx.getName()));
                            }
                            endActivity("");
                        }
                    };
                    worker.execute();
                }
                else
                    return;
            }
        });
        getRecentlyOpenedConsoleMenu();
        storeRecentlyOpenedFiles(CONSOLES_FILES);
        return true;
    }

    /**
     * @param fx
     */
    protected void openMiscTypeFile(File fx) {
        if (fx == null)
            return;
        if (!fx.exists()) {
            JOptionPane.showMessageDialog(editFileMenuItem, I18n.text("The file doesn't exists!"));
            return;
        }
        if (fx.isDirectory()) {
            JOptionPane.showMessageDialog(editFileMenuItem, I18n.text("Cannot edit a directory!"));
            return;
        }
        updateMiscFilesOpened(fx);
        // EditorLauncher ed = new EditorLauncher();
        // short edType = ed.TEXT_EDITOR_TYPE;
        // ed.editFile(fx.getAbsolutePath(), edType, false);
        try {
            Desktop.getDesktop().edit(fx);
        }
        catch (Exception e) {
            EditorLauncher ed = new EditorLauncher();
            short edType = ed.TEXT_EDITOR_TYPE;
            ed.editFile(fx.getAbsolutePath(), edType, false);
            NeptusLog.pub().error(e.getStackTrace());
        }
    }

    protected void openMapTypeFile(File fx) {
        //        if (fx != null) {
        //            updateMapFilesOpened(fx);
        //            updateMiscFilesOpened(fx);
        //
        //            MapType map = new MapType(fx.getAbsolutePath());
        //            JInternalFrame ifrm = internalFrames.get("map-" + map.getId());
        //            if (ifrm != null) {
        //                // ifrm.setFocusable(true);
        //                ifrm.setVisible(true);
        //                try {
        //                    ifrm.setSelected(true);
        //                    ifrm.setIcon(false);
        //                }
        //                catch (PropertyVetoException e1) {
        //                    // e1.printStackTrace();
        //                    NeptusLog.pub().debug(this + " getOpenMapMenuItem", e1);
        //                }
        //                return;
        //            }
        //
        //            CoordinateSystem cs = new CoordinateSystem();
        //            Object ob = map.getAllElements().getFirst();
        //            if (ob instanceof LocationType) {
        //                // FIXME
        //                cs.setLocation((LocationType) ob);
        //            }
        //            final MissionMapEditor mme = new MissionMapEditor(map, cs, true);
        //            if (ob instanceof LocationType) {
        //                // FIXME
        //                // mp.centerOnLocation((AbstractLocationPoint) ob);
        //            }
        //            ifrm = createFrame(I18n.textf("%map - Map", map.getName()), "map-" + map.getId(), mme);
        //            internalFrames.put("map-" + map.getId(), ifrm);
        //            ifrm.setSize(new Dimension(mme.getWidth() + 190, mme.getHeight() + 50));
        //            // ifrm.setSize(new Dimension(mp.getWidth(), mp.getHeight()+120));
        //            ifrm.setResizable(true);
        //            ifrm.setMaximizable(true);
        //            mme.setFrameOpener(Workspace.this);
        //            /*
        //             * ifrm.addFocusListener(new FocusAdapter() { public void focusLost(java.awt.event.FocusEvent arg0) {
        //             * mme.switchTo2D(); }; });
        //             */
        //            ifrm.addInternalFrameListener(new InternalFrameAdapter() {
        //                @Override
        //                public void internalFrameDeactivated(javax.swing.event.InternalFrameEvent arg0) {
        //                    try {
        //
        //                        mme.switchTo2D();
        //                        // ((JInternalFrame) arg0.getSource()).setIcon(true);
        //                    }
        //                    catch (Exception e) {
        //                        NeptusLog.pub().error(this, e);
        //                    }
        //                }
        //            });
        //        }
    }

    protected void openChecklistTypeFile(File fx) {
        if (fx != null) {
            ChecklistType checklist = new ChecklistType(fx.getAbsolutePath());
            if (!checklist.isLoadOk()) {
                GuiUtils.errorMessage(this, I18n.text("Error Opening Checklist"), I18n.text("Not a valid checklist."));
                NeptusLog.pub().warn("Error Opening Checklist (" + fx.getAbsolutePath() + ")");
                return;
            }

            updateChecklistFilesOpened(fx);
            updateMiscFilesOpened(fx);

            JInternalFrame ifrm = internalFrames.get("checklist-" + checklist.getName());
            if (ifrm != null) {
                // ifrm.setFocusable(true);
                ifrm.setVisible(true);
                try {
                    ifrm.setSelected(true);
                    ifrm.setIcon(false);
                }
                catch (PropertyVetoException e1) {
                    // e1.printStackTrace();
                    NeptusLog.pub().debug(this + " getOpenMapMenuItem", e1);
                }
                return;
            }

            ChecklistPanel clPanel = new ChecklistPanel(checklist);
            clPanel.makeNameEditable();
            ifrm = createFrame(I18n.textf("%checklist - Checklist", checklist.getName()),
                    "checklist-" + checklist.getName(), clPanel);
            internalFrames.put("checklist-" + checklist.getName(), ifrm);
            ifrm.setSize(new Dimension(clPanel.getWidth() + 100, clPanel.getHeight() + 150));
            // ifrm.setSize(new Dimension(mp.getWidth(), mp.getHeight()+120));
            ifrm.setResizable(true);
            ifrm.setMaximizable(true);
            ifrm.repaint();
            clPanel.setJInternalFrame(ifrm);
            ifrm.validate();
        }
    }

    protected void openChecklistTypeFile(ChecklistType checklist) {
        if (checklist != null) {
            // updateChecklistFilesOpened(fx);
            // updateMiscFilesOpened(fx);

            // ChecklistType checklist = new
            // ChecklistType(fx.getAbsolutePath());
            JInternalFrame ifrm = internalFrames.get("checklist-" + checklist.getName());
            if (ifrm != null) {
                // FIXME controlar os IDs
            }
            else {
                ChecklistPanel clPanel = new ChecklistPanel(checklist);
                clPanel.makeNameEditable();
                ifrm = createFrame(I18n.textf("%checklist - Checklist", checklist.getName()),
                        "checklist-" + checklist.getName(), clPanel);
                clPanel.setJInternalFrame(ifrm);

                // FIXME ver melhor pq o ID mode mudar!!
                internalFrames.put("checklist-" + checklist.getName(), ifrm);

                ifrm.setSize(new Dimension(clPanel.getWidth() + 100, clPanel.getHeight() + 150));
                // ifrm.setSize(new Dimension(mp.getWidth(),
                // mp.getHeight()+120));
                ifrm.setResizable(true);
                ifrm.setMaximizable(false);
            }
        }
    }

    /**
     * This method initializes viewer3DMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getViewer3DMenuItem() {
        if (viewer3DMenuItem == null) {
            viewer3DMenuItem = new JMenuItem();
            viewer3DMenuItem.setText(I18n.text("3D Model Viewer"));
            viewer3DMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK, true));
            viewer3DMenuItem
            .setIcon(new ImageIcon(this.getClass().getClassLoader().getResource("images/menus/3d.png")));
            viewer3DMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    startActivity(I18n.text("Opening Viewer3D..."));
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            @SuppressWarnings("unused")
                            Viewer3D v3d = new Viewer3D();
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                            endActivity("");
                        }
                    };
                    worker.execute();

                    /*
                     * JInternalFrame ifrm = createFrame("3D Model Viewer", "3dv-", v3d); ifrm.setSize(new
                     * Dimension(500, 500)); ifrm.setIconifiable(false); ifrm.addInternalFrameListener(new
                     * InternalFrameAdapter() { public void
                     * internalFrameDeactivated(javax.swing.event.InternalFrameEvent arg0) { try {
                     * 
                     * //((MissionMapEditor)mmeComp).switchTo2D(); ((JInternalFrame) arg0.getSource()).setIcon(true); }
                     * catch (Exception e) { NeptusLog.pub().error(this, e); } }; });
                     */
                    // v3d.setFrameOpener(MissionConsole.this);
                    // v3d.show();
                }
            });
        }
        return viewer3DMenuItem;
    }

    class MyIFrame extends JInternalFrame {
        private static final long serialVersionUID = -7355044261933465004L;

        public MyIFrame(String title, boolean b, boolean c, boolean d, boolean e) {
            super(title, b, c, d, e);
        }

        /***********************************************************************
         * Para testar o repaint com o 3D public void repaint(){ System.err.println(">>>>>>>>>>> repaint");
         * 
         * try { Thread.sleep(2000); } catch (InterruptedException e) { // TODO Auto-generated catch block
         * e.printStackTrace(); } super.repaint(); }; /
         **********************************************************************/

        @Override
        public void paint(Graphics g) {
            System.err.println(g);
            if (g != null)
                super.paint(g);
        }

    }

    /**
     * This method initializes dumpGeneralPropertiesDefaultsMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getDumpGeneralPropertiesDefaultsMenuItem() {
        if (dumpGeneralPropertiesDefaultsMenuItem == null) {
            dumpGeneralPropertiesDefaultsMenuItem = new JMenuItem();
            dumpGeneralPropertiesDefaultsMenuItem.setText(I18n.text("Dump general preferences"));
            dumpGeneralPropertiesDefaultsMenuItem.setIcon(new ImageIcon(this.getClass().getClassLoader()
                    .getResource("images/menus/ark_extract.png")));
            dumpGeneralPropertiesDefaultsMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // int res = JOptionPane
                    // .showConfirmDialog(
                    // MissionConsole.this,
                    // "<html>The defaults of the general properties will "
                    // + "be dumped to <br><i>"
                    // + ConfigFetch.GENERAL_PROPERTIES_FILE
                    // +
                    // "</i> overwriting the ones already <br>modified in the "
                    // + "existing file!<br><br> Do you want to do this?",
                    // "Choose one",
                    // JOptionPane.YES_NO_OPTION);
                    // if (res == JOptionPane.YES_OPTION)
                    // GeneralPreferences.dumpGeneralPreferences();
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            // FIXME Modality
                            int res = JOptionPane.showConfirmDialog(Workspace.this, I18n.textf(
                                    "<html>The defaults of the general properties will " + "be dumped to <br><i>"
                                            + "%file" + "</i> overwriting the ones already <br>modified in the "
                                            + "existing file!<br><br> Do you want to do this?",
                                            GeneralPreferences.GENERAL_PROPERTIES_FILE), "Choose one", JOptionPane.YES_NO_OPTION);
                            if (res == JOptionPane.YES_OPTION) {
                                GeneralPreferences.dumpGeneralPreferences();
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                        }
                    };
                    worker.execute();
                }
            });
        }
        return dumpGeneralPropertiesDefaultsMenuItem;
    }

    /**
     * This method initializes snapShotMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getSnapShotMenuItem() {
        if (snapShotMenuItem == null) {
            snapShotMenuItem = new JMenuItem();
            snapShotMenuItem.setIcon(new ImageIcon(ImageUtils.getImage("images/menus/snapshot.png")));
            snapShotMenuItem.setText("Snapshot");
            // snapShotMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
            // java.awt.event.KeyEvent.VK_Q, java.awt.Event.CTRL_MASK, true));
            snapShotMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_PRINTSCREEN, Event.CTRL_MASK, true));
            snapShotMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            GuiUtils.takeSnapshot(Workspace.this, "MissionConsole");
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                NeptusLog.pub().error(e);
                            }
                        }
                    };
                    worker.execute();
                }
            });
        }
        return snapShotMenuItem;
    }

    /**
     * This method initializes snapShotMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getShowConsoleMenuItem() {
        if (showConsoleMenuItem == null) {
            showConsoleMenuItem = new JMenuItem();
            showConsoleMenuItem.setIcon(new ImageIcon(ImageUtils.getImage("images/menus/display.png")));
            showConsoleMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK, true));
            showConsoleMenuItem.setText(I18n.text("Show Console"));
            // snapShotMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(
            // java.awt.event.KeyEvent.VK_Q, java.awt.Event.CTRL_MASK, true));
            showConsoleMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    OutputPanel.showWindow();
                }
            });
        }
        return showConsoleMenuItem;
    }

    /**
     * This method initializes statusBar
     * 
     * @return
     */
    private JXStatusBar getStatusBar() {
        if (statusBar == null) {
            statusBar = new JXStatusBar();
            statusBar.add(getMessageBarLabel(), JXStatusBar.Constraint.ResizeBehavior.FILL);
            statusBar.add(getStatusLed());
        }
        return statusBar;
    }

    /**
     * This method initializes messageBarLabel
     * 
     * @return javax.swing.JLabel
     */
    private JLabel getMessageBarLabel() {
        if (messageBarLabel == null) {
            messageBarLabel = new JLabel();
            messageBarLabel.setText("");
        }
        return messageBarLabel;
    }

    /**
     * This method initializes statusLed
     * 
     * @return pt.lsts.neptus.gui.StatusLed
     */
    private StatusLed getStatusLed() {
        if (statusLed == null) {
            statusLed = new StatusLed();
            statusLed.setLevel(StatusLed.LEVEL_OFF);
        }
        return statusLed;
    }

    /**
     * @param message
     */
    private void startActivity(String message) {
        // System.err.println(">>>");
        setCursor(new Cursor(Cursor.WAIT_CURSOR));

        getMessageBarLabel().setText(message);
        getStatusLed().setLevel(StatusLed.LEVEL_1);
    }

    /**
     * @param message
     */
    private void endActivity(String message) {
        setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        getMessageBarLabel().setText(message);
        getStatusLed().setLevel(StatusLed.LEVEL_OFF);
    }

    /**
     * This method initializes openMRAMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getOpenMRAMenuItem() {
        if (openMRAMenuItem == null) {
            openMRAMenuItem = new JMenuItem();
            openMRAMenuItem.setText("MRA");
            openMRAMenuItem.setIcon(MRA_ICON);
            openMRAMenuItem.addActionListener(getActionOpenMRA());
        }
        return openMRAMenuItem;
    }

    private ActionListener getActionOpenMRA() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startActivity(I18n.text("Opening MRA..."));

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        JFrame mra = new NeptusMRA();
                        mra.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                        endActivity("");
                    }
                }, "MRAThread").run();
            }
        };
    }

    /**
     * This method initializes editGeneralPreferencesMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getEditGeneralPreferencesMenuItem() {
        if (editGeneralPreferencesMenuItem == null) {
            editGeneralPreferencesMenuItem = new JMenuItem();
            editGeneralPreferencesMenuItem.setText(I18n.text("Edit general preferences"));
            editGeneralPreferencesMenuItem.setIcon(new ImageIcon(this.getClass().getClassLoader()
                    .getResource("images/menus/settings.png")));
            editGeneralPreferencesMenuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {

                    PropertiesEditor.editProperties(new GeneralPreferences(), Workspace.this, true);
                    GeneralPreferences.saveProperties();
                }
            });
        }
        return editGeneralPreferencesMenuItem;
    }

    /**
     * This method initializes callGcMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getCallGcMenuItem() {
        if (callGcMenuItem == null) {
            callGcMenuItem = new JMenuItem();
            callGcMenuItem.setText(I18n.text("Invoke Garbage Collector"));
            callGcMenuItem.setIcon(GC_ICON);
            callGcMenuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    startActivity(I18n.text("Invoking Garbage Collector"));
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            long bytes = GuiUtils.callGC();
                            NeptusLog.pub().warn("Garbage Collector call freed " + bytes + " bytes");
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            endActivity("");
                        }
                    };
                    worker.execute();
                }
            });
        }
        return callGcMenuItem;
    }

    /**
     * This method initializes latLonConvMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getLatLonConvMenuItem() {
        if (latLonConvMenuItem == null) {
            latLonConvMenuItem = new JMenuItem();
            latLonConvMenuItem.setIcon(new ImageIcon(ImageUtils.getImage("images/menus/displaylatlon.png")));
            latLonConvMenuItem.setText(I18n.text("Lat/Lon Conv."));
            latLonConvMenuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    LatLonConv latLonConv = new LatLonConv();
                    createFrame(I18n.text("Lat/Lon Conv."), I18n.text("Lat/Lon Conv."), latLonConv);
                }
            });
        }
        return latLonConvMenuItem;
    }

    /**
     * @return
     */
    private JMenuItem getChronometerMenuItem() {
        if (chronometerPanelMenuItem == null) {
            chronometerPanelMenuItem = new JMenuItem();
            chronometerPanelMenuItem.setIcon(ImageUtils.getScaledIcon("images/buttons/clocksync2.png", 16, 16));
            chronometerPanelMenuItem.setText(I18n.text("Chronometer"));
            chronometerPanelMenuItem.addActionListener(new java.awt.event.ActionListener() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    ChronometerPanel chronometerPanel = new ChronometerPanel();
                    createFrame(I18n.text("Chronometer"), I18n.text("Chronometer"), chronometerPanel);
                }
            });
        }
        return chronometerPanelMenuItem;
    }

    /*
     * (non-Javadoc)
     * 
     * @see pt.lsts.neptus.loader.FileHandler#handleFile(java.io.File)
     */
    @Override
    public void handleFile(File f) {
        File fx = new File(ConfigFetch.resolvePath(f.getAbsolutePath()));
        String extension = FileUtil.getFileExtension(fx);
        //        if (extension.equalsIgnoreCase(FileUtil.FILE_TYPE_MISSION)
        //                || extension.equalsIgnoreCase(FileUtil.FILE_TYPE_MISSION_COMPRESSED)) {
        //            openMissionTypeFile(fx);
        //        }
        //        else 
        if (extension.equalsIgnoreCase(FileUtil.FILE_TYPE_MAP)) {
            openMapTypeFile(fx);
        }
        else if (extension.equalsIgnoreCase(FileUtil.FILE_TYPE_CHECKLIST)) {
            openChecklistTypeFile(fx);
        }
        else if (extension.equalsIgnoreCase(FileUtil.FILE_TYPE_VEHICLE)) {
            openVehicleTypeFile(fx);
        }
        else if (extension.equalsIgnoreCase(FileUtil.FILE_TYPE_CONSOLE)) {
            @SuppressWarnings("unused")
            ConsoleLayout console = ConsoleLayout.forge(ConfigFetch.resolvePath(fx.getAbsolutePath()));
        }
        else if (extension.equalsIgnoreCase(FileUtil.FILE_TYPE_WSN)) {
            openMiscTypeFile(fx);
        }
        else if (extension.equalsIgnoreCase(FileUtil.FILE_TYPE_CONFIG)) {
            openMiscTypeFile(fx);
        }
        else if (extension.equalsIgnoreCase(FileUtil.FILE_TYPE_INI)) {
            openMiscTypeFile(fx);
        }
        else {
            openMiscTypeFile(fx);
        }
    }

    private ActionListener getActionEmptyConsole() {
        return new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startActivity(I18n.text("Opening Empty Console..."));
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    ConsoleLayout empCon;
                    @Override
                    protected Void doInBackground() throws Exception {
                        empCon = ConsoleLayout.forge();
                        return null;
                    }

                    @Override
                    protected void done() {
                        try {
                            get();
                            empCon.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                            empCon.setVisible(true);
                        }
                        catch (Exception e) {
                            e.printStackTrace();
                        }
                        endActivity("");
                    }
                };
                worker.execute();
            }
        };
    }

    /**
     * This method initializes recordMouseMenuItem
     * 
     * @return javax.swing.JCheckBoxMenuItem
     */
    private JCheckBoxMenuItem getMouseRecorderMenuItem() {
        if (recordMouseMenuItem == null) {
            recordMouseMenuItem = new JCheckBoxMenuItem();
            recordMouseMenuItem.setIcon(REC_MOUSE_ICON);
            recordMouseMenuItem.setText(I18n.text("Record Mouse Actions"));

            recordMouseMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (recordMouseMenuItem.isSelected()) {
                        recorder.startRecording();
                    }
                    else {
                        recorder.stopRecording();
                    }
                }
            });
        }

        recordMouseMenuItem.setSelected(false);

        return recordMouseMenuItem;
    }

    /**
     * This method initializes imcMonitorMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getImcMonitorMenuItem() {
        if (imcMonitorMenuItem == null) {
            imcMonitorMenuItem = new JMenuItem();
            imcMonitorMenuItem.setIcon(IMC_ICON);
            imcMonitorMenuItem.setText(I18n.text("IMC Comm. Monitor"));
            imcMonitorMenuItem.addActionListener(getActionImcMonitor());
        }
        return imcMonitorMenuItem;
    }

    private ActionListener getActionImcMonitor() {
        return new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                startActivity(I18n.text("Opening IMC Monitor ..."));
                JInternalFrame ifrm = internalFrames.get("imc monitor");
                if (ifrm != null) {
                    ifrm.setVisible(true);
                    try {
                        ifrm.setSelected(true);
                        ifrm.setIcon(false);
                    }
                    catch (PropertyVetoException e1) {
                        NeptusLog.pub().debug(this + " open imc monitor", e1);
                    }
                    endActivity("");
                }
                else {
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            try {
                                final MonitorIMCComms imcPanel = new MonitorIMCComms(ImcMsgManager.getManager());
                                JInternalFrame ifrm = createFrame(I18n.text("IMC Comm. Monitor"),
                                        I18n.text("IMC Comm. Monitor"), imcPanel);
                                ifrm.addInternalFrameListener(new InternalFrameAdapter() {
                                    @Override
                                    public void internalFrameClosing(InternalFrameEvent e) {
                                        internalFrames.remove("imc monitor");
                                        imcPanel.cleanup();
                                    }
                                });
                                internalFrames.put("imc monitor", ifrm);
                                ifrm.setSize(new Dimension(imcPanel.getWidth() + 60, imcPanel.getHeight() + 140 + 140));
                                ifrm.setResizable(true);
                                ifrm.setMaximizable(true);
                            }
                            catch (Exception e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            endActivity("");
                        }
                    };
                    worker.execute();
                }
            }
        };
    }

    private JMenu getReviewMenu() {
        if (reviewMenu == null) {
            reviewMenu = new JMenu();
            reviewMenu.setText(I18n.text("Mission Review"));
            // reviewMenu.setMnemonic(java.awt.event.KeyEvent.VK_F);
            reviewMenu.add(getOpenMRAMenuItem());
        }
        return reviewMenu;
    }

    private JMenu getConsolesMenu() {
        if (consolesMenu == null) {
            consolesMenu = new JMenu();
            consolesMenu.setText(I18n.text("Consoles"));
            consolesMenu.add(getOpenConsoleMenuItem());
            consolesMenu.add(getNewConsoleMenuItem());
            consolesMenu.add(getRecentlyOpenedConsoleMenu());
        }
        return consolesMenu;
    }

    /**
     * This method initializes openConsoleMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getOpenConsoleMenuItem() {
        if (openConsoleMenuItem == null) {
            openConsoleMenuItem = new JMenuItem();
            openConsoleMenuItem.setIcon(OPEN_ICON);
            openConsoleMenuItem.setText(I18n.text("Open"));
            openConsoleMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        @Override
                        protected Void doInBackground() throws Exception {
                            File fx = ConsoleFileChooser.showOpenDialog(Workspace.this, null);
                            openConsoleTypeFile(fx);
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            // endActivity("");
                        }
                    };
                    worker.execute();
                }
            });
        }
        return openConsoleMenuItem;
    }

    /**
     * This method initializes recentlyOpenedChecklistMenu
     * 
     * @return javax.swing.JMenu
     */
    private JMenu getRecentlyOpenedConsoleMenu() {
        if (recentlyOpenedConsoleMenu == null) {
            recentlyOpenedConsoleMenu = new JMenu();
            recentlyOpenedConsoleMenu.setText(I18n.text("Recently opened"));
            recentlyOpenedConsoleMenu.setIcon(DISPLAY_ICON);
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenedConsoleMenu, consoleFilesOpened);
        }
        else {
            RecentlyOpenedFilesUtil.constructRecentlyFilesMenuItems(recentlyOpenedConsoleMenu, consoleFilesOpened);
        }
        return recentlyOpenedConsoleMenu;
    }

    /**
     * This method initializes newChecklistMenuItem
     * 
     * @return javax.swing.JMenuItem
     */
    private JMenuItem getNewConsoleMenuItem() {
        if (newConsoleMenuItem == null) {
            newConsoleMenuItem = new JMenuItem();
            newConsoleMenuItem.setIcon(new ImageIcon(this.getClass().getClassLoader()
                    .getResource("images/menus/new.png")));
            newConsoleMenuItem.setText(I18n.text("New"));
            // the same as openEmptyConsoleMenuItem
            newConsoleMenuItem.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    startActivity(I18n.text("Opening Empty Console..."));
                    SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                        ConsoleLayout empCon;
                        @Override
                        protected Void doInBackground() throws Exception {
                            empCon = ConsoleLayout.forge();
                            return null;
                        }

                        @Override
                        protected void done() {
                            try {
                                get();
                                empCon.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                                empCon.setVisible(true);
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }
                            endActivity("");
                        }
                    };
                    worker.execute();
                }
            });
        }
        return newConsoleMenuItem;
    }
    
    private void addDesktopIcons() {
        int iconSize = 48, iconSepSize = Math.min(iconSize + iconSize * 2 / 3, iconSize + 18);
        int posY = 30;
        DesktopIcon icon;

        //        icon = new DesktopIcon(new ImageIcon(ImageUtils.getImage("images/apps/NeptusApps-MP.png")
        //                .getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)), I18n.text("Planner"), getActionOpenMP());
        //        icon.setForeground(Color.WHITE);
        //        // icon.setToolTipText("View the current drifter positions");
        //        getJDesktopPane().add(icon);
        //        icon.setLocation(30, posY);
        //        posY += iconSepSize;

        icon = new DesktopIcon(new ImageIcon(ImageUtils.getImage("images/apps/NeptusApps-MRA.png").getScaledInstance(
                iconSize, iconSize, // "images/neptus-icon1.png"
                Image.SCALE_SMOOTH)), I18n.text("Review & Analysis"), getActionOpenMRA());
        icon.setForeground(Color.WHITE);
        // icon.setToolTipText("View the current drifter positions");
        getJDesktopPane().add(icon);
        icon.setLocation(30, posY);
        posY += iconSepSize;

        icon = new DesktopIcon(new ImageIcon(ImageUtils.getImage("images/apps/NeptusApps-Console.png")
                .getScaledInstance(iconSize, iconSize, Image.SCALE_SMOOTH)), I18n.text("Console Open/Builder"),
                getActionEmptyConsole());
        icon.setForeground(Color.WHITE);

        getJDesktopPane().add(icon);
        icon.setLocation(30, posY);
        posY += iconSepSize;

        icon = new DesktopIcon(new ImageIcon(ImageUtils.getImage("images/apps/NeptusApps-Check.png").getScaledInstance(
                iconSize, iconSize, Image.SCALE_SMOOTH)), I18n.text("Checklist"), getActionOpenChecklist());
        icon.setForeground(Color.WHITE);

        getJDesktopPane().add(icon);
        icon.setLocation(30, posY);
        posY += iconSepSize;

        icon = new DesktopIcon(new ImageIcon(ImageUtils.getImage("images/imc.png").getScaledInstance(iconSize,
                iconSize, Image.SCALE_SMOOTH)), I18n.text("IMC Monitor"), getActionImcMonitor());
        icon.setForeground(Color.WHITE);
        // icon.setToolTipText("View the current drifter positions");
        getJDesktopPane().add(icon);
        icon.setLocation(30, posY);
        posY += iconSepSize;

    }

    /**
     * @param args
     */
    // @SuppressWarnings("unused")
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ConfigFetch.initialize();
                Workspace application = new Workspace();
                application.setVisible(true);
            }
        });
    }
}
