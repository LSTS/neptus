package org.necsave.simulation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.apache.commons.io.IOUtils;

import net.miginfocom.swing.MigLayout;
import pt.lsts.neptus.NeptusLog;

public class SimulatorGUI {

    private HashMap<Integer, Simulator> platforms = new HashMap<>();
    private CustomJTabbedPane tabbedPane;
    private static String DUNE_BUILD_PATH = "/home/manuel/workspace/NECSAVE/dune/build/";
    private static String NECSAVE_BUILD_PATH = "/home/manuel/workspace/NECSAVE/build/";
    private static String XML_FILE;
    private static String LAUNCH_SCRIPT;
    private static String LAUNCH_DUNE_SCRIPT;
    private JDialog frame;
    private ConfigWindow confDiag;
    private ResultWindow resultDiag;
    private JButton runSimBtn;
    private File prevOpenFile;

    public SimulatorGUI() {
        copyTempFiles();
        
        //Create and set up the window.
        frame = new JDialog();
        frame.setTitle("Simulation Manager");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setResizable(false);
        tabbedPane = new CustomJTabbedPane();

        confDiag = new ConfigWindow();
        JMenuBar menuBar = new JMenuBar();

        //Build the first menu.
        JMenu menu = new JMenu("File");
        menuBar.add(menu);

        //a group of JMenuItems
        JMenuItem openMnItem = new JMenuItem("Open Config(s). (Ctrl+O)");

        ActionListener openAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                fileChooser();
            }
        };

        AbstractAction open = new AbstractAction() {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                openAction.actionPerformed(e);

            }
        };


        menuBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK), "Open");
        menuBar.getActionMap().put("Open", open);

        openMnItem.addActionListener(openAction);
        menu.add(openMnItem);

        JMenuItem confMnItem = new JMenuItem("Config Paths (Ctrl+C)");
        ActionListener confAction = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confDiag.setVisible(true);
                confDiag.pack();
            }
        };

        AbstractAction config = new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                confAction.actionPerformed(e);

            }
        };

        menuBar.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_MASK), "Config");
        menuBar.getActionMap().put("Config", config);

        confMnItem.addActionListener(confAction);
        menu.add(confMnItem);

        frame.setJMenuBar(menuBar);

        JPanel btnPanel = new JPanel();
        runSimBtn = new JButton("Run Simulation");
        runSimBtn.setEnabled(false);
        btnPanel.add(runSimBtn);
        runSimBtn.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                if (!confDiag.validDunePath(DUNE_BUILD_PATH) || !confDiag.validNecsavePath(NECSAVE_BUILD_PATH)) {
                    String path = !confDiag.validDunePath(DUNE_BUILD_PATH) ? "Dune" : "Necsave";
                    JOptionPane.showMessageDialog(frame, "Wrong build path for "+path+".", "Error", JOptionPane.ERROR_MESSAGE);
                } 


                if (resultDiag != null)
                    resultDiag.dispose();

                // Kill any open simulator
                new ExecThread("ps --no-headers axk comm o pid,args | awk '$2 ~ \"../build/\"{print $1}' | xargs kill -9 | echo 'Killing all necsave procs...'", false).start();

                resultDiag = new ResultWindow();

                javax.swing.SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        resultDiag.setVisible(true);
                    }
                });

                StringBuilder platformsArgs = new StringBuilder();
                StringBuilder duneArgs = new StringBuilder();

                for (Entry<Integer, Simulator> plat : platforms.entrySet()) {
                    platformsArgs.append(plat.getValue().getConfigPath()+ " ");
                    if (!plat.getValue().getPlatformType().equals("DummyPlatform")) {
                        duneArgs.append("vn_"+plat.getValue().getName()+" ");
                    }
                }

                new ExecThread(LAUNCH_SCRIPT+" "+NECSAVE_BUILD_PATH+" "+platformsArgs, true).start();

                System.out.println(duneArgs.toString());
                new ExecThread(LAUNCH_DUNE_SCRIPT+" "+DUNE_BUILD_PATH+" "+duneArgs, true).start();

                runSimBtn.setEnabled(false);

            }

        });

        //Add the tabbed pane to this panel.
        frame.getContentPane().add(tabbedPane);
        frame.getContentPane().add(btnPanel, BorderLayout.SOUTH);

        //Display the window.
        frame.setSize(553, 551);
        frame.setVisible(true);

    }

    private void copyTempFiles() {

        InputStream xmlStream = getClass().getResourceAsStream("../utils/schema.xml");
        InputStream necsaveScriptStream = getClass().getResourceAsStream("../utils/launch.sh");
        InputStream necsave2ScriptStream = getClass().getResourceAsStream("../utils/launch-one-platform.sh");
        InputStream duneScriptStream = getClass().getResourceAsStream("../utils/launch-dune.sh");

        Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxrwxr-x");
        FileAttribute<Set<PosixFilePermission>> fileAttributes = PosixFilePermissions.asFileAttribute(perms);

        Path temp = null;
        String launch = null;
        String launch_one = null;
        String launch_dune = null;
        String xml = null;
        
        try {
            temp = Files.createTempDirectory("necsave");

            launch = temp.toString()+"/launch.sh";
            launch_one = temp.toString()+"/launch-one-platform.sh";
            launch_dune = temp.toString()+"/launch-dune.sh";
            xml = temp.toString()+"/schema.xml";

            File nec1File = new File(launch);
            File nec2File = new File(launch_one);
            File duneFile = new File(launch_dune);
            File xmlFile = new File(xml);
            
            Path launchFile = Files.createFile(nec1File.toPath(), fileAttributes);
            Path launchNecFile = Files.createFile(nec2File.toPath(), fileAttributes);
            Path launchDuneFile = Files.createFile(duneFile.toPath(), fileAttributes);
            Path schemaFile = Files.createFile(xmlFile.toPath(), fileAttributes);

//            System.out.printf("Wrote text to temporary file %s%n", launchNecFile.toString());
//            System.out.printf("Wrote text to temporary file %s%n", launchFile.toString());
//            System.out.printf("Wrote text to temporary file %s%n", launchDuneFile.toString());
//            System.out.printf("Wrote text to temporary file %s%n", schemaFile.toString());
            
            LAUNCH_DUNE_SCRIPT = launchDuneFile.toString();
            LAUNCH_SCRIPT = launchFile.toString();
            XML_FILE = schemaFile.toString();
        }
        catch (IOException e1) {
            e1.printStackTrace();
        }

        if (xmlStream == null)
            NeptusLog.pub().error("../utils/schema.xml (resource not found)");

        if (necsaveScriptStream == null)
            NeptusLog.pub().error("../utils/launch.sh (resource not found)");

        if (necsave2ScriptStream == null)
            NeptusLog.pub().error("../utils/launch-one-platform.sh (resource not found)");

        
        if (duneScriptStream == null)
            NeptusLog.pub().error("../utils/launch-dune.sh (resource not found)");

        try {
            FileWriter launchWriter = new FileWriter(launch);
            FileWriter launchOneWriter = new FileWriter(launch_one);
            FileWriter launchDuneWriter = new FileWriter(launch_dune);
            FileWriter schemaWriter = new FileWriter(xml);
            
            IOUtils.copy(necsaveScriptStream, launchWriter, "UTF-8");
            IOUtils.copy(necsave2ScriptStream, launchOneWriter, "UTF-8");
            IOUtils.copy(duneScriptStream, launchDuneWriter, "UTF-8");
            IOUtils.copy(xmlStream, schemaWriter, "UTF-8");
            
            xmlStream.close();
            necsaveScriptStream.close();
            necsave2ScriptStream.close();
            duneScriptStream.close();
            launchWriter.close();
            launchOneWriter.close();
            launchDuneWriter.close();
            schemaWriter.close();
        }
        catch (IOException e1) {
            System.out.println("ERROR");
            e1.printStackTrace();
        }
    }
    
    private int fileChooser() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setCurrentDirectory(prevOpenFile);
        fileChooser.addChoosableFileFilter(new FileFilter() {

            public String getDescription() {
                return "Platform Config. Files (*.ini)";
            }

            public boolean accept(File f) {
                if (f.isDirectory()) {
                    return true;
                } else {
                    return f.getName().toLowerCase().endsWith(".ini");
                }
            }
        });

        int returnVal = fileChooser.showOpenDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files ) {
                prevOpenFile = file;
                System.out.println("Opening '" +file.getPath()+"'");
                Simulator newPlatfConfig = new Simulator(file.getPath(), XML_FILE);

                if (newPlatfConfig.isValidSimulator())
                {

                    int i = tabbedPane.addNewTab(newPlatfConfig);
                    platforms.put(i, newPlatfConfig);
                    runSimBtn.setEnabled(true);

                    frame.repaint();
                    frame.pack();
                } 
                else {
                    JOptionPane.showMessageDialog(frame, "Error opening "+file.getPath(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
            return 0;
        }
        return -1;
    }

    private class ConfigWindow extends JDialog {

        private static final long serialVersionUID = 1L;

        public ConfigWindow() {

            JPanel statusBar = new JPanel();
            JTextField duneTxtField, necsaveTxtField;
            JLabel msg = new JLabel();
            msg.setFont(new Font("DialogInput", Font.BOLD, 11));

            statusBar.setLayout(new BorderLayout());
            statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
            statusBar.add(msg, BorderLayout.WEST);

            setTitle("Configurations");
            setResizable(false);
            getContentPane().add(statusBar, BorderLayout.SOUTH);

            JPanel centerPanel = new JPanel();
            getContentPane().add(centerPanel, BorderLayout.CENTER);
            centerPanel.setLayout(new MigLayout("", "[114px,grow][5px][48px][5px][90px][5px][114px]", "[19px][35px][][][][][]"));

            JLabel lblDuneBuildPath = new JLabel("DUNE Build Path:");
            centerPanel.add(lblDuneBuildPath, "cell 0 0");

            duneTxtField = new JTextField(DUNE_BUILD_PATH);
            centerPanel.add(duneTxtField, "cell 0 1 7 1,growx");
            duneTxtField.setColumns(10);

            JLabel lblNecsaveBuildPath = new JLabel("NECSAVE Build Path:");
            centerPanel.add(lblNecsaveBuildPath, "cell 0 2");

            necsaveTxtField = new JTextField(NECSAVE_BUILD_PATH);
            centerPanel.add(necsaveTxtField, "cell 0 3 7 1,growx");
            necsaveTxtField.setColumns(10);

            JButton btnSave = new JButton("Save");
            btnSave.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    DUNE_BUILD_PATH = duneTxtField.getText();
                    NECSAVE_BUILD_PATH = necsaveTxtField.getText();
                    if (validDunePath(duneTxtField.getText())) {
                        msg.setText("DUNE Path: OK!");
                    } else
                        msg.setText("DUNE Path: ERROR!");
                    if (validNecsavePath(necsaveTxtField.getText())) {
                        msg.setText(msg.getText() + " | NECSAVE Path: OK!");
                    } else
                        msg.setText(msg.getText() + " | NECSAVE Path: ERROR!");


                }
            });

            addWindowListener(new WindowAdapter()
            {
                @Override
                public void windowClosing(WindowEvent e)
                {
                    btnSave.doClick();
                    e.getWindow().dispose();
                }
            });

            centerPanel.add(btnSave, "cell 2 5");
            pack();
            setVisible(false);

        }

        public boolean validDunePath(String path) {

            Path pathDir = FileSystems.getDefault().getPath(path, "dune");

            if (Files.exists(pathDir)) {
                if (Files.isExecutable(pathDir))
                    return true;
                else
                    return false;
            }

            return false;
        }

        public boolean validNecsavePath(String path) {

            Path pathDir = FileSystems.getDefault().getPath(path, "nec_perception");

            if (Files.exists(pathDir)) {
                if (Files.isExecutable(pathDir))
                    return true;
                else
                    return false;
            }

            return false;
        }
    }

    class ExecThread extends Thread {
        private String cmd;
        private boolean showOnTxtArea;

        public ExecThread(String cmd, boolean showOnTxtArea) {
            super(cmd);
            this.cmd = cmd;
            this.showOnTxtArea = showOnTxtArea;
        }

        public void run() {
            try {
                Process process = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c", cmd });

                BufferedReader buf = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = "";
                while ((line = buf.readLine()) != null) {
                    if (showOnTxtArea) {
                        resultDiag.setTxt(line.concat("\n"));
                        System.out.println("Response: " + line);
                    }
                    else
                        System.out.println("Exec response: " + line);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new SimulatorGUI();
            }
        });
    }

    private class ResultWindow extends JDialog {

        private static final long serialVersionUID = 1L;
        private final JPanel contentPanel = new JPanel();
        private JButton btnKillSim;
        private JTextArea txtArea;

        public ResultWindow() {
            setLocationRelativeTo(null);
            setResizable(false);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    btnKillSim.doClick();
                    if (platforms.size() > 0)
                        runSimBtn.setEnabled(true);
                    //dispose();
                }
            });
            setBounds(100, 100, 450, 280);
            getContentPane().setLayout(new BorderLayout());
            contentPanel.setLayout(new BorderLayout());
            contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            getContentPane().add(contentPanel, BorderLayout.CENTER);

            txtArea = new JTextArea();
            txtArea.setColumns(40);
            txtArea.setLineWrap(true);
            txtArea.setTabSize(20);
            txtArea.setRows(13);

            JScrollPane scrollPane = new JScrollPane(txtArea);
            contentPanel.add(scrollPane, BorderLayout.CENTER);

            JPanel panel = new JPanel();
            contentPanel.add(panel, BorderLayout.SOUTH);

            btnKillSim = new JButton("Kill Simulation");
            btnKillSim.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    new ExecThread("ps --no-headers axk comm o pid,args | awk '$2 ~ \"../build/\"{print $1}' | xargs kill -9 | echo 'Killing all necsave procs...'", true).start();
                    runSimBtn.setEnabled(true);
                }
            });
            panel.add(btnKillSim);
            pack();
            setLocation((Toolkit.getDefaultToolkit().getScreenSize().width)/2 - getWidth()/2, (Toolkit.getDefaultToolkit().getScreenSize().height)/2 - getHeight()/2);
            setVisible(false);
        }

        public void setTxt(String txt) {
            txtArea.append(txt);
        }
    }

    private class CustomJTabbedPane extends JTabbedPane {

        private static final long serialVersionUID = 1L;
        private int numTabs = 0;
        private JLabel initialLbl;

        public CustomJTabbedPane() {

            initialLbl = new JLabel("No Platforms to be simulated!");
            initialLbl.setHorizontalAlignment(SwingConstants.CENTER);
            initialLbl.setFont(new Font("Dialog", Font.BOLD, 20));

            addTab("Intro", null, initialLbl, null);

            // tab to add new tab when click
            add(new JPanel(), "+", 1);

            addChangeListener(changeListener);
        }

        ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {

                if ((numTabs > 0 && getSelectedIndex() == numTabs) || numTabs == 0 && getSelectedIndex() == 1) {
                    int res = fileChooser();
                    if (res == -1) {
                        int tab = numTabs != 0 ? numTabs - 1 : numTabs;
                        setSelectedIndex(tab);
                    }
                }
            }
        };

        private int addNewTab(Simulator sim) {
            if (numTabs == 0) 
                remove(initialLbl);

            int index = numTabs;
            removeChangeListener(changeListener);

            add(sim, "", index);
            setTabComponentAt(index, new CustomTab(this, sim.getName()));

            setSelectedIndex(index);
            numTabs++;
            addChangeListener(changeListener);

            return numTabs - 1;
        }

        public void removeTab(int index) {
            platforms.remove(index);
            remove(index);
            numTabs--;

            if (numTabs == 0) {
                removeChangeListener(changeListener);
                add(initialLbl, "Intro", 0);
                setSelectedIndex(0);
                runSimBtn.setEnabled(false);
                addChangeListener(changeListener);
            } else
                setSelectedIndex(index - 1);
        }

    }

    @SuppressWarnings("serial")
    public class CustomTab extends JPanel {

        private CustomJTabbedPane customJTabbedPane;

        public CustomTab(CustomJTabbedPane customJTabbedPane, String lbl) {
            this.customJTabbedPane = customJTabbedPane;
            setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
            setBorder(new EmptyBorder(5, 2, 2, 2));
            setOpaque(false);
            addLabel(lbl);
            add(new CustomButton("x"));
        }

        private void addLabel(String lblName) {
            JLabel label = new JLabel() {
                public String getText() {
                    int index = customJTabbedPane.indexOfTabComponent(CustomTab.this);
                    if (index != -1) {
                        return lblName;
                    }
                    return null;
                }
            };
            label.setBorder(new EmptyBorder(0, 0, 0, 8));
            add(label);
        }

        class CustomButton extends JButton implements MouseListener {
            public CustomButton(String text) {
                int size = 16;
                setText(text);
                setPreferredSize(new Dimension(size, size));
                setToolTipText("Remove this platform");
                setContentAreaFilled(false);
                setBorder(new EtchedBorder());
                setBorderPainted(false);
                setFocusable(false);
                addMouseListener(this);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int index = customJTabbedPane.indexOfTabComponent(CustomTab.this);
                if (index != -1)
                    customJTabbedPane.removeTab(index);
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setBorderPainted(true);
                setForeground(Color.BLUE);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setBorderPainted(false);
                setForeground(Color.BLACK);
            }
        }
    }

    public boolean isVisible() {
        // TODO Auto-generated method stub
        return frame.isVisible();
    }


}