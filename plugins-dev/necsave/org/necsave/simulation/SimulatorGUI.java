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
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map.Entry;

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

import net.miginfocom.swing.MigLayout;

public class SimulatorGUI {

	private HashMap<Integer, Simulator> platforms = new HashMap<>();
	private CustomJTabbedPane tabbedPane;
	private static String DUNE_BUILD_PATH = "/home/manuel/workspace/NECSAVE/dune/build/";
	private static String NECSAVE_BUILD_PATH = "/home/manuel/workspace/NECSAVE/build/";
	private final static String XML_FILE = "/home/manuel/workspace/Parser/conf/xml.xml";
	private final static String LAUNCH_SCRIPT = "/home/manuel/workspace/NECSAVE/launch.sh";
	private final static String LAUNCH_DUNE_SCRIPT = "/home/manuel/workspace/NECSAVE/launch-dune.sh";
	private JDialog frame;
	private JDialog confDiag;
	private ResultWindow resultDiag;
	private JButton runSimBtn;
	private File prevOpenFile;

	public SimulatorGUI() {
		//Create and set up the window.
		frame = new JDialog();
		frame.setTitle("Simulation Manager");
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setResizable(false);
		tabbedPane = new CustomJTabbedPane();

		setupConfDiag();
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

	private int fileChooser() {
	    String path = System.getProperty("user.home");
        JFileChooser fileChooser = new JFileChooser(path+"/workspace/NECSAVE/integration/2auvs_1cal/");
		fileChooser.setAcceptAllFileFilterUsed(false);
		fileChooser.setMultiSelectionEnabled(true);
		//fileChooser.setCurrentDirectory(prevOpenFile);
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

	private void setupConfDiag() {
		confDiag = new JDialog(frame, true);
		confDiag.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		JPanel statusBar = new JPanel();
		JTextField duneTxtField, necsaveTxtField;
		JLabel msg = new JLabel();
		msg.setFont(new Font("DialogInput", Font.BOLD, 11));

		statusBar.setLayout(new BorderLayout());
		statusBar.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		statusBar.add(msg, BorderLayout.WEST);

		confDiag.setTitle("Configurations");
		confDiag.setResizable(false);
		confDiag.getContentPane().add(statusBar, BorderLayout.SOUTH);

		JPanel centerPanel = new JPanel();
		confDiag.getContentPane().add(centerPanel, BorderLayout.CENTER);
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
		centerPanel.add(btnSave, "cell 2 5");
		confDiag.pack();
		confDiag.setVisible(false);

	}

	private boolean validDunePath(String path) {

		Path pathDir = FileSystems.getDefault().getPath(path, "dune");

		if (Files.exists(pathDir)) {
			if (Files.isExecutable(pathDir))
				return true;
			else
				return false;
		}

		return false;
	}

	private boolean validNecsavePath(String path) {

		Path pathDir = FileSystems.getDefault().getPath(path, "nec_perception");

		if (Files.exists(pathDir)) {
			if (Files.isExecutable(pathDir))
				return true;
			else
				return false;
		}

		return false;
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